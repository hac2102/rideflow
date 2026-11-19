package com.rideflow.server.service;

import com.rideflow.core.dispatch.CandidateScore;
import com.rideflow.core.dispatch.DispatchStrategy;
import com.rideflow.core.dispatch.RidePriorityQueue;
import com.rideflow.core.dispatch.WeightedFairDispatchStrategy;
import com.rideflow.core.enums.DriverStatus;
import com.rideflow.core.enums.EventType;
import com.rideflow.core.enums.RideStatus;
import com.rideflow.core.exception.DoubleBookingException;
import com.rideflow.core.model.Coordinate;
import com.rideflow.core.model.Driver;
import com.rideflow.core.model.RideAssignment;
import com.rideflow.core.model.RideRequest;
import com.rideflow.core.model.SimulationEvent;
import com.rideflow.core.spatial.DistanceCalculator;
import com.rideflow.core.spatial.EuclideanRoadDistanceCalculator;
import com.rideflow.server.dao.DriverDao;
import com.rideflow.server.dao.RideAssignmentDao;
import com.rideflow.server.dao.RideRequestDao;
import com.rideflow.server.dao.SimulationEventDao;
import com.rideflow.server.dao.ZoneDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Core dispatch coordinator implementing ACID transactional ride matching and anti-double-booking protection.
 */
@Service
public class DispatchService {

    private static final Logger log = LoggerFactory.getLogger(DispatchService.class);

    private final DataSource dataSource;
    private final DriverDao driverDao;
    private final RideRequestDao rideRequestDao;
    private final RideAssignmentDao rideAssignmentDao;
    private final SimulationEventDao simulationEventDao;
    private final ZoneDao zoneDao;

    private final DistanceCalculator distanceCalculator;
    private final DispatchStrategy dispatchStrategy;
    private final double maxPickupRadiusKm;

    // Cache of latest candidate explanation scores for explainable AI in dashboard
    private final Map<Long, List<CandidateScore>> candidateEvaluations = new ConcurrentHashMap<>();

    public DispatchService(DataSource dataSource,
                           DriverDao driverDao,
                           RideRequestDao rideRequestDao,
                           RideAssignmentDao rideAssignmentDao,
                           SimulationEventDao simulationEventDao,
                           ZoneDao zoneDao) {
        this.dataSource = dataSource;
        this.driverDao = driverDao;
        this.rideRequestDao = rideRequestDao;
        this.rideAssignmentDao = rideAssignmentDao;
        this.simulationEventDao = simulationEventDao;
        this.zoneDao = zoneDao;
        this.distanceCalculator = new EuclideanRoadDistanceCalculator();
        this.dispatchStrategy = new WeightedFairDispatchStrategy();
        this.maxPickupRadiusKm = 15.0; // 15 km maximum dispatch radius
    }

    /**
     * Executes an intelligent dispatch pass for all queued requests.
     */
    public synchronized List<RideAssignment> runDispatchCycle(long currentTick, long runId) {
        List<RideRequest> queued = rideRequestDao.findQueuedRequests();
        if (queued.isEmpty()) {
            return Collections.emptyList();
        }

        // Rebuild priority queue with updated aging
        RidePriorityQueue priorityQueue = new RidePriorityQueue();
        queued.forEach(priorityQueue::add);
        priorityQueue.reprioritize(LocalDateTime.now());

        List<Driver> availableDrivers = driverDao.findAvailableDrivers();
        Set<Long> reservedInThisCycle = new HashSet<>();
        List<RideAssignment> successfulAssignments = new ArrayList<>();

        while (!priorityQueue.isEmpty()) {
            RideRequest request = priorityQueue.poll().orElse(null);
            if (request == null) break;

            // Filter eligible candidate drivers within pickup radius and not already reserved
            List<Driver> eligible = new ArrayList<>();
            for (Driver d : availableDrivers) {
                if (!reservedInThisCycle.contains(d.getDriverId()) && d.getStatus() == DriverStatus.AVAILABLE) {
                    double dist = distanceCalculator.calculateDistanceKm(d.getCurrentCoordinate(), request.getPickupCoordinate());
                    if (dist <= maxPickupRadiusKm) {
                        eligible.add(d);
                    }
                }
            }

            if (eligible.isEmpty()) {
                log.debug("No eligible driver within radius for ride {}", request.getRequestId());
                continue;
            }

            double trafficMultiplier = zoneDao.findById(request.getPickupZoneId())
                    .map(z -> z.getCurrentTrafficMultiplier()).orElse(1.0);

            Optional<CandidateScore> bestCandidate = dispatchStrategy.selectBestDriver(
                    request, eligible, currentTick, distanceCalculator, trafficMultiplier
            );

            if (bestCandidate.isPresent()) {
                CandidateScore score = bestCandidate.get();
                Driver driver = score.getDriver();
                try {
                    RideAssignment assignment = atomicAssignRide(request.getRequestId(), driver.getDriverId(), currentTick, runId);
                    successfulAssignments.add(assignment);
                    reservedInThisCycle.add(driver.getDriverId());
                    candidateEvaluations.put(request.getRequestId(), List.of(score));
                    log.info("Dispatched Ride #{} to Driver #{} ({})", request.getRequestId(), driver.getDriverId(), score.getExplanation());
                } catch (DoubleBookingException e) {
                    log.warn("Double booking prevented during dispatch pass: {}", e.getMessage());
                } catch (Exception e) {
                    log.error("Failed to commit assignment for ride {}: {}", request.getRequestId(), e.getMessage());
                }
            }
        }

        return successfulAssignments;
    }

    /**
     * Executes atomic, thread-safe ride assignment with strict JDBC transaction rollback.
     */
    public RideAssignment atomicAssignRide(long requestId, long driverId, long currentTick, long runId) {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 1. Conditionally reserve driver
                boolean driverReserved = driverDao.updateStatusAndVersion(conn, driverId, DriverStatus.AVAILABLE, DriverStatus.RESERVED);
                if (!driverReserved) {
                    conn.rollback();
                    throw new DoubleBookingException("Driver " + driverId + " is no longer AVAILABLE. Assignment rolled back.");
                }

                // 2. Conditionally mark ride request as ASSIGNED with assigned driver
                rideRequestDao.markAssigned(conn, requestId, driverId, LocalDateTime.now());

                // 3. Insert assignment audit record
                RideAssignment assignment = new RideAssignment(null, requestId, driverId, currentTick, "ACTIVE");
                long assignmentId = rideAssignmentDao.save(conn, assignment);
                assignment.setAssignmentId(assignmentId);

                // Commit the transaction
                conn.commit();

                // 4. Log simulation event
                SimulationEvent event = new SimulationEvent(
                        null, runId, currentTick, EventType.RIDE_ASSIGNED, requestId,
                        String.format("{\"requestId\":%d,\"driverId\":%d,\"tick\":%d}", requestId, driverId, currentTick)
                );
                simulationEventDao.save(event);

                return assignment;
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            log.error("Database error during atomic assignment", e);
            throw new RuntimeException("Transactional assignment failed", e);
        }
    }

    public List<CandidateScore> getCandidateEvaluation(long requestId) {
        return candidateEvaluations.getOrDefault(requestId, Collections.emptyList());
    }
}

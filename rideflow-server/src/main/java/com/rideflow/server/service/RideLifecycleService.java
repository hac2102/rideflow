package com.rideflow.server.service;

import com.rideflow.core.enums.DriverStatus;
import com.rideflow.core.enums.EventType;
import com.rideflow.core.enums.RequestType;
import com.rideflow.core.enums.RideStatus;
import com.rideflow.core.exception.InvalidRideStateException;
import com.rideflow.core.exception.RideNotFoundException;
import com.rideflow.core.model.*;
import com.rideflow.core.spatial.ZonePartitionService;
import com.rideflow.server.dao.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class RideLifecycleService {

    private static final Logger log = LoggerFactory.getLogger(RideLifecycleService.class);

    private final DataSource dataSource;
    private final RideRequestDao rideRequestDao;
    private final DriverDao driverDao;
    private final RideAssignmentDao rideAssignmentDao;
    private final SimulationEventDao simulationEventDao;
    private final PricingService pricingService;
    private final ZonePartitionService zonePartitionService;

    public RideLifecycleService(DataSource dataSource,
                                RideRequestDao rideRequestDao,
                                DriverDao driverDao,
                                RideAssignmentDao rideAssignmentDao,
                                SimulationEventDao simulationEventDao,
                                PricingService pricingService) {
        this.dataSource = dataSource;
        this.rideRequestDao = rideRequestDao;
        this.driverDao = driverDao;
        this.rideAssignmentDao = rideAssignmentDao;
        this.simulationEventDao = simulationEventDao;
        this.pricingService = pricingService;
        this.zonePartitionService = new ZonePartitionService();
    }

    public RideRequest createAndQueueRide(long riderId, Coordinate pickup, Coordinate destination, RequestType type, long runId) {
        return createAndQueueRide(riderId, pickup, destination, type, runId, 0);
    }

    public RideRequest createAndQueueRide(long riderId, Coordinate pickup, Coordinate destination, RequestType type, long runId, long tick) {
        // Step 1: Calculate and persist locked fare quotation
        FareQuote quote = pricingService.generateAndSaveQuote(pickup, destination);

        int pickupZoneId = zonePartitionService.findZoneId(pickup);
        int destZoneId = zonePartitionService.findZoneId(destination);

        RideRequest request = new RideRequest(
                null,
                riderId,
                quote.getQuoteId(),
                pickup,
                pickupZoneId,
                destination,
                destZoneId,
                type,
                quote.getDistanceKm(),
                quote.getEstimatedDurationMin(),
                quote.getFinalQuotedFare(),
                quote.getSurgeMultiplier()
        );
        request.markQueued();

        long requestId = rideRequestDao.save(request);
        request.setRequestId(requestId);

        // Record simulation event
        SimulationEvent event = new SimulationEvent(
                null, runId, tick, EventType.RIDE_QUEUED, requestId,
                String.format("{\"requestId\":%d,\"riderId\":%d,\"type\":\"%s\",\"fare\":%.2f}",
                        requestId, riderId, type, quote.getFinalQuotedFare().getAmount())
        );
        simulationEventDao.save(event);

        log.info("Ride #{} created and placed in queue (Fare: {}, Surge: {}x)", requestId, quote.getFinalQuotedFare(), quote.getSurgeMultiplier());
        return request;
    }

    public void cancelRide(long requestId, long runId) {
        RideRequest request = rideRequestDao.findById(requestId)
                .orElseThrow(() -> new RideNotFoundException("Ride not found: " + requestId));

        if (!request.getStatus().canBeCancelled()) {
            throw new InvalidRideStateException("Ride #" + requestId + " cannot be cancelled from state " + request.getStatus());
        }

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // If a driver was already assigned, release the driver back to AVAILABLE
                if (request.getStatus() == RideStatus.ASSIGNED && request.getAssignedDriverId() != null) {
                    long driverId = request.getAssignedDriverId();
                    driverDao.updateStatusAndVersion(conn, driverId, DriverStatus.RESERVED, DriverStatus.AVAILABLE);
                    rideAssignmentDao.updateStatus(conn, requestId, driverId, "RELEASED_CANCELLED");
                }

                rideRequestDao.markCancelled(conn, requestId, LocalDateTime.now());
                conn.commit();

                simulationEventDao.save(new SimulationEvent(
                        null, runId, 0, EventType.RIDE_CANCELLED, requestId,
                        "{\"requestId\":" + requestId + ",\"reason\":\"USER_CANCELLED\"}"
                ));
                log.info("Ride #{} successfully cancelled and driver released", requestId);
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            log.error("Error cancelling ride: {}", requestId, e);
            throw new RuntimeException("Failed to cancel ride", e);
        }
    }

    public void startRide(long requestId, long runId) {
        startRide(requestId, runId, 0);
    }

    public void startRide(long requestId, long runId, long tick) {
        try (Connection conn = dataSource.getConnection()) {
            rideRequestDao.markStarted(conn, requestId, LocalDateTime.now());
            RideRequest req = rideRequestDao.findById(conn, requestId).orElseThrow();
            if (req.getAssignedDriverId() != null) {
                driverDao.updateStatusAndVersion(conn, req.getAssignedDriverId(), DriverStatus.RESERVED, DriverStatus.ON_TRIP);
            }
            simulationEventDao.save(new SimulationEvent(
                    null, runId, tick, EventType.RIDE_STARTED, requestId,
                    "{\"requestId\":" + requestId + "}"
            ));
        } catch (SQLException e) {
            log.error("Error starting ride: {}", requestId, e);
            throw new RuntimeException(e);
        }
    }

    public void completeRide(long requestId, long driverId, Coordinate dropoffPos, Money fareEarned, long runId) {
        completeRide(requestId, driverId, dropoffPos, fareEarned, runId, 0);
    }

    public void completeRide(long requestId, long driverId, Coordinate dropoffPos, Money fareEarned, long runId, long tick) {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int dropoffZoneId = zonePartitionService.findZoneId(dropoffPos);

                // Mark ride completed
                rideRequestDao.markCompleted(conn, requestId, LocalDateTime.now());

                // Release driver to AVAILABLE at dropoff location and credit earnings
                driverDao.updateTripCompletion(conn, driverId, dropoffPos, dropoffZoneId, fareEarned);

                // Update assignment record
                rideAssignmentDao.updateStatus(conn, requestId, driverId, "RELEASED_COMPLETED");

                conn.commit();

                simulationEventDao.save(new SimulationEvent(
                        null, runId, tick, EventType.RIDE_COMPLETED, requestId,
                        String.format("{\"requestId\":%d,\"driverId\":%d,\"fare\":%.2f}", requestId, driverId, fareEarned.getAmount())
                ));
                log.info("Ride #{} completed by Driver #{}. Fare {} credited.", requestId, driverId, fareEarned);
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            log.error("Error completing ride: {}", requestId, e);
            throw new RuntimeException(e);
        }
    }

    public List<RideRequest> getAllRides() {
        return rideRequestDao.findAll();
    }

    public List<RideRequest> getQueuedRides() {
        return rideRequestDao.findQueuedRequests();
    }

    public List<RideRequest> getActiveRides() {
        return rideRequestDao.findActiveTrips();
    }
}

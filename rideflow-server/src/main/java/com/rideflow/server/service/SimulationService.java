package com.rideflow.server.service;

import com.rideflow.core.enums.DriverStatus;
import com.rideflow.core.enums.EventType;
import com.rideflow.core.enums.RideStatus;
import com.rideflow.core.enums.SimulationStatus;
import com.rideflow.core.model.*;
import com.rideflow.core.simulation.ActiveTripProgress;
import com.rideflow.core.simulation.DemandGenerator;
import com.rideflow.core.simulation.SimulationClock;
import com.rideflow.core.spatial.ZonePartitionService;
import com.rideflow.server.dao.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class SimulationService {

    private static final Logger log = LoggerFactory.getLogger(SimulationService.class);

    private final SimulationClock clock;
    private final SimulationRunDao simulationRunDao;
    private final SimulationEventDao simulationEventDao;
    private final DriverDao driverDao;
    private final RideRequestDao rideRequestDao;
    private final FareQuoteDao fareQuoteDao;
    private final ZoneDao zoneDao;
    private final DispatchService dispatchService;
    private final RideLifecycleService rideLifecycleService;
    private final ZonePartitionService zonePartitionService;

    private SimulationRun currentRun;
    private DemandGenerator demandGenerator;
    private DemandGenerator.ScenarioType currentScenario = DemandGenerator.ScenarioType.NORMAL;
    private final AtomicLong nextGeneratedRequestId = new AtomicLong(1000);

    // Active trip progress models tracked in memory across ticks
    private final Map<Long, ActiveTripProgress> activeTripProgressMap = new ConcurrentHashMap<>();

    public SimulationService(SimulationRunDao simulationRunDao,
                             SimulationEventDao simulationEventDao,
                             DriverDao driverDao,
                             RideRequestDao rideRequestDao,
                             FareQuoteDao fareQuoteDao,
                             ZoneDao zoneDao,
                             DispatchService dispatchService,
                             RideLifecycleService rideLifecycleService) {
        this.simulationRunDao = simulationRunDao;
        this.simulationEventDao = simulationEventDao;
        this.driverDao = driverDao;
        this.rideRequestDao = rideRequestDao;
        this.fareQuoteDao = fareQuoteDao;
        this.zoneDao = zoneDao;
        this.dispatchService = dispatchService;
        this.rideLifecycleService = rideLifecycleService;
        this.zonePartitionService = new ZonePartitionService();
        this.clock = new SimulationClock();
        // Database initialization deferred until Flyway migrations complete
    }

    public synchronized SimulationRun getCurrentRun() {
        if (this.currentRun == null) {
            initializeRun("NORMAL_DEMAND", 42L);
        }
        return currentRun;
    }

    public synchronized void initializeRun(String scenarioName, long seed) {
        DemandGenerator.ScenarioType scenario = DemandGenerator.ScenarioType.NORMAL;
        try {
            scenario = DemandGenerator.ScenarioType.valueOf(scenarioName.toUpperCase());
        } catch (Exception ignored) {}
        this.currentScenario = scenario;
        this.demandGenerator = new DemandGenerator(seed, zonePartitionService);

        SimulationRun run = new SimulationRun(null, scenarioName, seed);
        long runId = simulationRunDao.save(run);
        run.setRunId(runId);
        this.currentRun = run;
        this.clock.reset();
        this.activeTripProgressMap.clear();

        log.info("Initialized Simulation Run #{} with scenario {} and seed {}", runId, scenarioName, seed);
    }

    public synchronized long advanceOneTick() {
        long tick = clock.step();
        getCurrentRun().setCurrentTick(tick);

        LocalDateTime currentTime = clock.getCurrentSimulatedTime();

        // 1. Advance Active Trips (linear interpolation along routes)
        processActiveTrips(tick);

        // 2. Check for newly assigned rides to initialize their progress trackers
        initializeNewTripTrackers();

        // 3. Expire rides waiting longer than 15 simulated minutes
        expireOverdueRides(currentTime);

        // 4. Generate new demand based on current scenario
        generateTickDemand(tick, currentTime);

        // 5. Execute Intelligent Dispatch Pass
        dispatchService.runDispatchCycle(tick, getCurrentRun().getRunId());

        // 6. Update Run Statistics
        updateRunStatistics();

        return tick;
    }

    private void processActiveTrips(long tick) {
        Iterator<Map.Entry<Long, ActiveTripProgress>> iter = activeTripProgressMap.entrySet().iterator();
        while (iter.hasNext()) {
            ActiveTripProgress progress = iter.next().getValue();
            Coordinate nextPos = progress.advanceOneTick();
            int zoneId = zonePartitionService.findZoneId(nextPos);

            Driver driver = driverDao.findById(progress.getDriverId()).orElse(null);
            if (driver == null) {
                iter.remove();
                continue;
            }

            if (progress.getStage() == ActiveTripProgress.Stage.ARRIVED_AT_PICKUP) {
                // Driver arrived at passenger pickup: begin trip
                rideLifecycleService.startRide(progress.getRequestId(), getCurrentRun().getRunId());
                driver.updatePosition(nextPos, zoneId);
                driver.setStatus(DriverStatus.ON_TRIP);
                driverDao.update(driver);
                log.info("Driver #{} picked up passenger for Ride #{}", driver.getDriverId(), progress.getRequestId());
            } else if (progress.getStage() == ActiveTripProgress.Stage.ARRIVED_AT_DROPOFF) {
                // Driver arrived at dropoff destination: complete trip
                RideRequest req = rideRequestDao.findById(progress.getRequestId()).orElse(null);
                Money fare = req != null ? req.getQuotedFare() : Money.of(150.0);
                rideLifecycleService.completeRide(progress.getRequestId(), progress.getDriverId(), nextPos, fare, getCurrentRun().getRunId());
                iter.remove();
            } else {
                // Driver en route: update interpolated position
                driver.updatePosition(nextPos, zoneId);
                driverDao.update(driver);
            }
        }
    }

    private void initializeNewTripTrackers() {
        List<RideRequest> assignedRides = rideRequestDao.findByStatus(RideStatus.ASSIGNED);
        for (RideRequest req : assignedRides) {
            if (!activeTripProgressMap.containsKey(req.getRequestId()) && req.getAssignedDriverId() != null) {
                Driver driver = driverDao.findById(req.getAssignedDriverId()).orElse(null);
                if (driver != null) {
                    int pickupDuration = Math.max(1, (int) Math.ceil(driver.getCurrentCoordinate().distanceKmTo(req.getPickupCoordinate()) / 0.5));
                    int tripDuration = Math.max(1, (int) Math.ceil(req.getEstimatedDistanceKm() / 0.5));
                    ActiveTripProgress progress = new ActiveTripProgress(driver, req, pickupDuration, tripDuration);
                    activeTripProgressMap.put(req.getRequestId(), progress);
                }
            }
        }
    }

    private void expireOverdueRides(LocalDateTime currentTime) {
        List<RideRequest> queued = rideRequestDao.findQueuedRequests();
        for (RideRequest req : queued) {
            if (req.getRequestedAt() != null) {
                long waitingMinutes = Duration.between(req.getRequestedAt(), currentTime).toMinutes();
                if (waitingMinutes >= 15) { // 15 tick maximum wait threshold
                    req.expire(currentTime);
                    rideRequestDao.update(req);
                    simulationEventDao.save(new SimulationEvent(
                            null, getCurrentRun().getRunId(), clock.getTick(), EventType.RIDE_EXPIRED, req.getRequestId(),
                            "{\"requestId\":" + req.getRequestId() + ",\"waitedMinutes\":" + waitingMinutes + "}"
                    ));
                    log.warn("Ride #{} expired after waiting {} ticks without an available driver", req.getRequestId(), waitingMinutes);
                }
            }
        }
    }

    private void generateTickDemand(long tick, LocalDateTime currentTime) {
        if (demandGenerator == null) {
            getCurrentRun();
        }
        List<RideRequest> generated = demandGenerator.generateBatchForTick(
                tick, currentTime, currentScenario, nextGeneratedRequestId.getAndAdd(10), 1L
        );
        for (RideRequest r : generated) {
            rideLifecycleService.createAndQueueRide(r.getRiderId(), r.getPickupCoordinate(), r.getDestinationCoordinate(), r.getRequestType(), getCurrentRun().getRunId());
        }
    }

    private void updateRunStatistics() {
        int total = (int) rideRequestDao.findAll().size();
        int completed = rideRequestDao.countByStatus(RideStatus.COMPLETED);
        int cancelled = rideRequestDao.countByStatus(RideStatus.CANCELLED);
        int expired = rideRequestDao.countByStatus(RideStatus.EXPIRED);

        SimulationRun run = getCurrentRun();
        run.setTotalRequests(total);
        run.setCompletedRequests(completed);
        run.setCancelledRequests(cancelled);
        run.setExpiredRequests(expired);

        simulationRunDao.update(run);
    }

    public void start() {
        clock.start();
        SimulationRun run = getCurrentRun();
        run.setStatus(SimulationStatus.RUNNING);
        simulationRunDao.update(run);
    }

    public void pause() {
        clock.pause();
        SimulationRun run = getCurrentRun();
        run.setStatus(SimulationStatus.PAUSED);
        simulationRunDao.update(run);
    }

    public void resume() {
        clock.resume();
        SimulationRun run = getCurrentRun();
        run.setStatus(SimulationStatus.RUNNING);
        simulationRunDao.update(run);
    }

    public void reset(String scenarioName, long seed) {
        initializeRun(scenarioName, seed);
    }

    public SimulationClock getClock() { return clock; }
    public DemandGenerator.ScenarioType getCurrentScenario() { return currentScenario; }
}

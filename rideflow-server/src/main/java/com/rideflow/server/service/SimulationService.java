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
import java.util.concurrent.*;
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

    // Automated Continuous Tick Scheduler
    private final ScheduledExecutorService autoTickExecutor = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> autoTickTask = null;
    private volatile long tickIntervalMs = 1000;

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
                rideLifecycleService.startRide(progress.getRequestId(), getCurrentRun().getRunId(), tick);
                driver.updatePosition(nextPos, zoneId);
                driver.setStatus(DriverStatus.ON_TRIP);
                driverDao.update(driver);
                log.info("Driver #{} picked up passenger for Ride #{}", driver.getDriverId(), progress.getRequestId());
            } else if (progress.getStage() == ActiveTripProgress.Stage.ARRIVED_AT_DROPOFF) {
                // Driver arrived at dropoff destination: complete trip
                RideRequest req = rideRequestDao.findById(progress.getRequestId()).orElse(null);
                Money fare = req != null ? req.getQuotedFare() : Money.of(150.0);
                rideLifecycleService.completeRide(progress.getRequestId(), progress.getDriverId(), nextPos, fare, getCurrentRun().getRunId(), tick);
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
            try {
                rideLifecycleService.createAndQueueRide(r.getRiderId(), r.getPickupCoordinate(), r.getDestinationCoordinate(), r.getRequestType(), getCurrentRun().getRunId(), tick);
            } catch (Exception e) {
                log.warn("Failed to queue ride request: {}", e.getMessage());
            }
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

    public synchronized void start() {
        clock.start();
        SimulationRun run = getCurrentRun();
        run.setStatus(SimulationStatus.RUNNING);
        simulationRunDao.update(run);
        startAutoTick();
    }

    public synchronized void pause() {
        clock.pause();
        SimulationRun run = getCurrentRun();
        run.setStatus(SimulationStatus.PAUSED);
        simulationRunDao.update(run);
        stopAutoTick();
    }

    public synchronized void resume() {
        clock.resume();
        SimulationRun run = getCurrentRun();
        run.setStatus(SimulationStatus.RUNNING);
        simulationRunDao.update(run);
        startAutoTick();
    }

    public synchronized void setTickSpeed(double speedMultiplier) {
        this.tickIntervalMs = Math.max(100, Math.min(5000, (long) (1000.0 / Math.max(0.1, speedMultiplier))));
        if (clock.isRunning()) {
            startAutoTick();
        }
    }

    public long getTickIntervalMs() {
        return tickIntervalMs;
    }

    private synchronized void startAutoTick() {
        stopAutoTick();
        autoTickTask = autoTickExecutor.scheduleWithFixedDelay(() -> {
            try {
                if (clock.isRunning()) {
                    advanceOneTick();
                }
            } catch (Exception ex) {
                log.error("Error during auto-tick execution", ex);
            }
        }, tickIntervalMs, tickIntervalMs, TimeUnit.MILLISECONDS);
        log.info("Auto-tick execution active with interval {} ms", tickIntervalMs);
    }

    private synchronized void stopAutoTick() {
        if (autoTickTask != null && !autoTickTask.isCancelled()) {
            autoTickTask.cancel(false);
            autoTickTask = null;
        }
    }

    // 1-Click Interactive Shock Scenarios
    public synchronized void injectRushHour(int zoneId, int requestCount) {
        long runId = getCurrentRun().getRunId();
        Random rng = new Random();
        for (int i = 0; i < requestCount; i++) {
            int row = (zoneId - 1) / 5;
            int col = (zoneId - 1) % 5;
            double px = col * 20.0 + 2.0 + rng.nextDouble() * 16.0;
            double py = row * 20.0 + 2.0 + rng.nextDouble() * 16.0;
            double dx = rng.nextDouble() * 100.0;
            double dy = rng.nextDouble() * 100.0;
            long riderId = 1L + (i % 5);
            rideLifecycleService.createAndQueueRide(riderId, new Coordinate(px, py), new Coordinate(dx, dy), com.rideflow.core.enums.RequestType.STANDARD, runId);
        }
        log.info("Injected Rush Hour spike with {} requests in Zone {}", requestCount, zoneId);
    }

    public synchronized void injectRainstorm() {
        long runId = getCurrentRun().getRunId();
        Random rng = new Random();
        for (int z = 1; z <= 25; z++) {
            int row = (z - 1) / 5;
            int col = (z - 1) % 5;
            double px = col * 20.0 + 2.0 + rng.nextDouble() * 16.0;
            double py = row * 20.0 + 2.0 + rng.nextDouble() * 16.0;
            double dx = rng.nextDouble() * 100.0;
            double dy = rng.nextDouble() * 100.0;
            long riderId = 1L + ((z - 1) % 5);
            rideLifecycleService.createAndQueueRide(riderId, new Coordinate(px, py), new Coordinate(dx, dy), com.rideflow.core.enums.RequestType.STANDARD, runId);
        }
        log.info("Injected Rainstorm city-wide demand shock");
    }

    public synchronized void mobilizeFleet(int count, int zoneId) {
        Random rng = new Random();
        int row = (zoneId - 1) / 5;
        int col = (zoneId - 1) % 5;
        for (int i = 0; i < count; i++) {
            double x = col * 20.0 + 2.0 + rng.nextDouble() * 16.0;
            double y = row * 20.0 + 2.0 + rng.nextDouble() * 16.0;
            Driver d = new Driver(null, "Reserve Driver #" + (500 + i), com.rideflow.core.enums.VehicleType.SEDAN,
                    "KA-01-MOB-" + (1000 + rng.nextInt(9000)), new Coordinate(x, y), zoneId);
            driverDao.save(d);
        }
        log.info("Mobilized {} reserve drivers to Zone {}", count, zoneId);
    }

    public void reset(String scenarioName, long seed) {
        stopAutoTick();
        initializeRun(scenarioName, seed);
    }

    public SimulationClock getClock() { return clock; }
    public DemandGenerator.ScenarioType getCurrentScenario() { return currentScenario; }
}

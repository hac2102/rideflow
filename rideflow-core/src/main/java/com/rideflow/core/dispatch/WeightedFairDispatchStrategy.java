package com.rideflow.core.dispatch;

import com.rideflow.core.model.Driver;
import com.rideflow.core.model.RideRequest;
import com.rideflow.core.spatial.DistanceCalculator;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Intelligent dispatch strategy balancing rapid passenger pickup (ETA) with driver fairness.
 *
 * Mathematical objective function:
 *   score = pickupETAminutes - min(driverIdleMinutes, maxIdleCreditMinutes) * idleWeight
 *
 * - Lower score is prioritized.
 * - Idle credit prevents driver starvation where a driver waiting 40 minutes gets overlooked
 *   for someone 200 meters closer.
 * - Deterministic tie-breaker: Driver ID ascending.
 */
public class WeightedFairDispatchStrategy implements DispatchStrategy {

    private final double idleWeight;
    private final double maxIdleCreditMinutes;

    public WeightedFairDispatchStrategy() {
        this(0.05, 30.0);
    }

    public WeightedFairDispatchStrategy(double idleWeight, double maxIdleCreditMinutes) {
        this.idleWeight = idleWeight;
        this.maxIdleCreditMinutes = maxIdleCreditMinutes;
    }

    @Override
    public String getName() {
        return "WeightedFairDispatchStrategy (Intelligent)";
    }

    @Override
    public Optional<CandidateScore> selectBestDriver(RideRequest request,
                                                     List<Driver> eligibleDrivers,
                                                     long currentTick,
                                                     DistanceCalculator distanceCalculator,
                                                     double trafficMultiplier) {
        if (eligibleDrivers == null || eligibleDrivers.isEmpty()) {
            return Optional.empty();
        }

        LocalDateTime now = LocalDateTime.now();

        return eligibleDrivers.stream()
                .filter(d -> d.getStatus().canBeAssigned())
                .map(driver -> {
                    double distKm = distanceCalculator.calculateDistanceKm(driver.getCurrentCoordinate(), request.getPickupCoordinate());
                    int etaMin = distanceCalculator.estimateDurationMinutes(driver.getCurrentCoordinate(), request.getPickupCoordinate(), trafficMultiplier);

                    double idleMinutes = 0.0;
                    if (driver.getLastAvailableAt() != null) {
                        long seconds = Math.max(0, Duration.between(driver.getLastAvailableAt(), now).toSeconds());
                        idleMinutes = seconds / 60.0;
                    }

                    double fairnessCredit = Math.min(idleMinutes, maxIdleCreditMinutes) * idleWeight;
                    double finalScore = (double) etaMin - fairnessCredit;

                    String explanation = String.format("ETA: %d min, Idle: %.1f min (Credit: -%.2f) => Score: %.3f",
                            etaMin, idleMinutes, fairnessCredit, finalScore);

                    return new CandidateScore(driver, distKm, etaMin, idleMinutes, finalScore, explanation);
                })
                .min(Comparator.naturalOrder());
    }

    public double getIdleWeight() {
        return idleWeight;
    }

    public double getMaxIdleCreditMinutes() {
        return maxIdleCreditMinutes;
    }
}

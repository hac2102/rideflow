package com.rideflow.core.dispatch;

import com.rideflow.core.model.Driver;
import com.rideflow.core.model.RideRequest;
import com.rideflow.core.spatial.DistanceCalculator;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Baseline dispatch strategy: selects the candidate driver strictly minimizing pickup distance.
 */
public class NearestDriverStrategy implements DispatchStrategy {

    @Override
    public String getName() {
        return "NearestDriverStrategy (Baseline)";
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

        return eligibleDrivers.stream()
                .filter(d -> d.getStatus().canBeAssigned())
                .map(driver -> {
                    double distKm = distanceCalculator.calculateDistanceKm(driver.getCurrentCoordinate(), request.getPickupCoordinate());
                    int etaMin = distanceCalculator.estimateDurationMinutes(driver.getCurrentCoordinate(), request.getPickupCoordinate(), trafficMultiplier);
                    double score = distKm;
                    String explanation = String.format("Pickup distance: %.2f km (ETA %d min)", distKm, etaMin);
                    return new CandidateScore(driver, distKm, etaMin, 0.0, score, explanation);
                })
                .min(Comparator.naturalOrder());
    }
}

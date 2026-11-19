package com.rideflow.core.pricing;

import com.rideflow.core.model.Coordinate;
import com.rideflow.core.model.FareQuote;
import com.rideflow.core.model.Money;
import com.rideflow.core.model.PricingConfig;
import com.rideflow.core.spatial.DistanceCalculator;
import com.rideflow.core.spatial.EuclideanRoadDistanceCalculator;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Thread-safe Dynamic Pricing Engine computing supply-demand dynamic surge pricing.
 */
public class PricingEngine {

    private final DistanceCalculator distanceCalculator;

    public PricingEngine() {
        this(new EuclideanRoadDistanceCalculator());
    }

    public PricingEngine(DistanceCalculator distanceCalculator) {
        this.distanceCalculator = Objects.requireNonNull(distanceCalculator, "distanceCalculator cannot be null");
    }

    /**
     * Computes the immutable FareQuote for a trip request.
     *
     * @param pickup                   Pickup coordinate
     * @param destination              Destination coordinate
     * @param zoneId                   Origin zone ID
     * @param trafficMultiplier        Zone traffic multiplier (e.g. 1.0, 1.3, 1.8)
     * @param queuedRequestsInZone     Current unassigned requests in this zone
     * @param availableDriversInZone   Current available drivers in this zone
     * @param config                   Active pricing configuration
     * @return Immutable FareQuote
     */
    public FareQuote generateQuote(Coordinate pickup,
                                  Coordinate destination,
                                  int zoneId,
                                  double trafficMultiplier,
                                  int queuedRequestsInZone,
                                  int availableDriversInZone,
                                  PricingConfig config) {
        Objects.requireNonNull(pickup, "pickup cannot be null");
        Objects.requireNonNull(destination, "destination cannot be null");
        Objects.requireNonNull(config, "config cannot be null");

        // Step 1: Physical trip estimation
        double distanceKm = distanceCalculator.calculateDistanceKm(pickup, destination);
        int durationMinutes = distanceCalculator.estimateDurationMinutes(pickup, destination, trafficMultiplier);

        // Step 2: Demand-Supply Ratio (including incoming request)
        int effectiveDemand = Math.max(1, queuedRequestsInZone + 1);
        int effectiveSupply = Math.max(1, availableDriversInZone); // Guard against division by zero
        double demandSupplyRatio = (double) effectiveDemand / (double) effectiveSupply;

        // Step 3: Surge Multiplier Calculation
        // surge = clamp(1 + sensitivity * max(0, ratio - 1), 1.0, maxSurgeCap)
        double excessRatio = Math.max(0.0, demandSupplyRatio - 1.0);
        double rawSurge = 1.0 + (config.getSurgeSensitivity() * excessRatio);
        double surgeMultiplier = Math.min(config.getMaxSurgeCap(), Math.max(1.0, rawSurge));
        // Round surge multiplier to 2 decimal places
        surgeMultiplier = Math.round(surgeMultiplier * 100.0) / 100.0;

        // Step 4: Base Trip Fare
        // baseTripFare = baseFare + (distanceKm * perKmRate) + (minutes * perMinuteRate)
        BigDecimal base = config.getBaseFare().getAmount();
        BigDecimal distanceCost = config.getPerKmRate().getAmount()
                .multiply(BigDecimal.valueOf(distanceKm));
        BigDecimal timeCost = config.getPerMinuteRate().getAmount()
                .multiply(BigDecimal.valueOf(durationMinutes));

        BigDecimal baseTripFareAmount = base.add(distanceCost).add(timeCost)
                .setScale(2, RoundingMode.HALF_UP);
        Money baseTripFare = Money.of(baseTripFareAmount);

        // Step 5: Final Quoted Fare
        BigDecimal finalFareAmount = baseTripFareAmount
                .multiply(BigDecimal.valueOf(surgeMultiplier))
                .setScale(2, RoundingMode.HALF_UP);
        Money finalQuotedFare = Money.of(finalFareAmount);

        return new FareQuote(
                null,
                zoneId,
                config.getVersion(),
                distanceKm,
                durationMinutes,
                baseTripFare,
                Math.round(demandSupplyRatio * 100.0) / 100.0,
                surgeMultiplier,
                finalQuotedFare,
                null
        );
    }
}

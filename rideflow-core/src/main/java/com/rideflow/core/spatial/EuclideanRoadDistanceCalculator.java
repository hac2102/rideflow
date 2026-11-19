package com.rideflow.core.spatial;

import com.rideflow.core.model.Coordinate;
import java.util.Objects;

/**
 * Calculates straight-line and synthetic road-network distances and driving durations.
 * 1 grid unit = 0.2 km.
 * Default road factor: 1.25 (simulates street curves and grid turns).
 * Default average speed: 30 km/h.
 */
public class EuclideanRoadDistanceCalculator implements DistanceCalculator {

    private final double roadFactor;
    private final double averageSpeedKmh;

    public EuclideanRoadDistanceCalculator() {
        this(1.25, 30.0);
    }

    public EuclideanRoadDistanceCalculator(double roadFactor, double averageSpeedKmh) {
        if (roadFactor < 1.0) {
            throw new IllegalArgumentException("roadFactor must be >= 1.0");
        }
        if (averageSpeedKmh <= 0.0) {
            throw new IllegalArgumentException("averageSpeedKmh must be > 0.0");
        }
        this.roadFactor = roadFactor;
        this.averageSpeedKmh = averageSpeedKmh;
    }

    @Override
    public double calculateDistanceKm(Coordinate from, Coordinate to) {
        Objects.requireNonNull(from, "from coordinate cannot be null");
        Objects.requireNonNull(to, "to coordinate cannot be null");
        double euclideanKm = from.distanceKmTo(to);
        double roadDistance = euclideanKm * roadFactor;
        return Math.round(roadDistance * 100.0) / 100.0;
    }

    @Override
    public int estimateDurationMinutes(Coordinate from, Coordinate to, double trafficMultiplier) {
        double distanceKm = calculateDistanceKm(from, to);
        if (distanceKm <= 0.01) {
            return 1;
        }
        double effectiveMultiplier = Math.max(1.0, trafficMultiplier);
        // Time in hours = distance / speed
        double hours = (distanceKm / averageSpeedKmh) * effectiveMultiplier;
        int minutes = (int) Math.ceil(hours * 60.0);
        return Math.max(1, minutes);
    }

    public double getRoadFactor() {
        return roadFactor;
    }

    public double getAverageSpeedKmh() {
        return averageSpeedKmh;
    }
}

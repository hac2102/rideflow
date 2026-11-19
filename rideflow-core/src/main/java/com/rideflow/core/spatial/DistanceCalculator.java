package com.rideflow.core.spatial;

import com.rideflow.core.model.Coordinate;

public interface DistanceCalculator {
    double calculateDistanceKm(Coordinate from, Coordinate to);
    int estimateDurationMinutes(Coordinate from, Coordinate to, double trafficMultiplier);
}

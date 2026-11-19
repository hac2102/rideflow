package com.rideflow.core.enums;

public enum TrafficLevel {
    NORMAL(1.00),
    MODERATE(1.30),
    HEAVY(1.80);

    private final double multiplier;

    TrafficLevel(double multiplier) {
        this.multiplier = multiplier;
    }

    public double getMultiplier() {
        return multiplier;
    }
}

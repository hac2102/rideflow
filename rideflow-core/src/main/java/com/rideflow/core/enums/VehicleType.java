package com.rideflow.core.enums;

public enum VehicleType {
    SEDAN("Sedan", 4),
    SUV("SUV", 6),
    HATCHBACK("Hatchback", 4),
    PREMIUM("Premium", 4);

    private final String displayName;
    private final int capacity;

    VehicleType(String displayName, int capacity) {
        this.displayName = displayName;
        this.capacity = capacity;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getCapacity() {
        return capacity;
    }
}

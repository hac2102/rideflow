package com.rideflow.core.enums;

public enum DriverStatus {
    OFFLINE,
    AVAILABLE,
    RESERVED,
    ON_TRIP;

    public boolean canBeAssigned() {
        return this == AVAILABLE;
    }

    public boolean canGoOffline() {
        return this == AVAILABLE || this == OFFLINE;
    }
}

package com.rideflow.core.enums;

public enum RideStatus {
    REQUESTED,
    QUEUED,
    ASSIGNED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED,
    EXPIRED;

    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED || this == EXPIRED;
    }

    public boolean canBeCancelled() {
        return this == REQUESTED || this == QUEUED || this == ASSIGNED;
    }

    public boolean canBeDispatched() {
        return this == QUEUED;
    }
}

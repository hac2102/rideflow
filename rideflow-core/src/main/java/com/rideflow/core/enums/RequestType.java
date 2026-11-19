package com.rideflow.core.enums;

public enum RequestType {
    STANDARD(1),
    PRIORITY(2);

    private final int priorityWeight;

    RequestType(int priorityWeight) {
        this.priorityWeight = priorityWeight;
    }

    public int getPriorityWeight() {
        return priorityWeight;
    }
}

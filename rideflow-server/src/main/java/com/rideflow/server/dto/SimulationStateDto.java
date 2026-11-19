package com.rideflow.server.dto;

import com.rideflow.core.enums.SimulationStatus;

public record SimulationStateDto(
        long runId,
        String scenarioName,
        long currentTick,
        String simulatedTime,
        SimulationStatus status,
        int totalRequests,
        int completedRequests,
        int cancelledRequests,
        int expiredRequests,
        String totalRevenue
) {}

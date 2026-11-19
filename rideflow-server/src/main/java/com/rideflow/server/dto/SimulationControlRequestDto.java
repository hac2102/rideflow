package com.rideflow.server.dto;

public record SimulationControlRequestDto(
        String scenarioName,
        Long seed
) {}

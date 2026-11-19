package com.rideflow.server.dto;

import com.rideflow.core.enums.DriverStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateDriverStatusDto(
        @NotNull(message = "Driver status is required")
        DriverStatus status
) {}

package com.rideflow.server.dto;

import com.rideflow.core.enums.VehicleType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateDriverRequestDto(
        @NotBlank(message = "Driver name is required")
        String name,

        @NotNull(message = "Vehicle type is required")
        VehicleType vehicleType,

        @NotBlank(message = "Vehicle number is required")
        String vehicleNumber,

        @Min(0) @Max(100)
        double initialX,

        @Min(0) @Max(100)
        double initialY
) {}

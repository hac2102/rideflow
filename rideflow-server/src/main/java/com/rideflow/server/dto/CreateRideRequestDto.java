package com.rideflow.server.dto;

import com.rideflow.core.enums.RequestType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateRideRequestDto(
        @NotNull(message = "Rider ID is required")
        Long riderId,

        @Min(0) @Max(100)
        double pickupX,

        @Min(0) @Max(100)
        double pickupY,

        @Min(0) @Max(100)
        double destinationX,

        @Min(0) @Max(100)
        double destinationY,

        @NotNull(message = "Request type is required")
        RequestType requestType
) {}

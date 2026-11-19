package com.rideflow.server.dto;

import com.rideflow.core.model.FareQuote;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record FareQuoteRequestDto(
        @Min(0) @Max(100) double pickupX,
        @Min(0) @Max(100) double pickupY,
        @Min(0) @Max(100) double destinationX,
        @Min(0) @Max(100) double destinationY
) {}

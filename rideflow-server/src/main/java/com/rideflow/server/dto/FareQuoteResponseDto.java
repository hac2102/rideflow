package com.rideflow.server.dto;

import com.rideflow.core.model.FareQuote;

public record FareQuoteResponseDto(
        Long quoteId,
        int zoneId,
        double distanceKm,
        int estimatedDurationMin,
        String baseTripFare,
        double demandSupplyRatio,
        double surgeMultiplier,
        String finalQuotedFare
) {
    public static FareQuoteResponseDto from(FareQuote q) {
        return new FareQuoteResponseDto(
                q.getQuoteId(),
                q.getZoneId(),
                q.getDistanceKm(),
                q.getEstimatedDurationMin(),
                q.getBaseTripFare().toString(),
                q.getDemandSupplyRatio(),
                q.getSurgeMultiplier(),
                q.getFinalQuotedFare().toString()
        );
    }
}

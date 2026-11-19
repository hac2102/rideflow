package com.rideflow.server.dto;

import com.rideflow.core.enums.RequestType;
import com.rideflow.core.enums.RideStatus;
import com.rideflow.core.model.RideRequest;

public record RideResponseDto(
        Long requestId,
        Long riderId,
        Long quoteId,
        double pickupX,
        double pickupY,
        int pickupZoneId,
        double destinationX,
        double destinationY,
        int destinationZoneId,
        RequestType requestType,
        RideStatus status,
        int priorityScore,
        String quotedFare,
        double surgeMultiplier,
        Long assignedDriverId,
        String requestedAt,
        String assignedAt
) {
    public static RideResponseDto from(RideRequest r) {
        return new RideResponseDto(
                r.getRequestId(),
                r.getRiderId(),
                r.getQuoteId(),
                r.getPickupCoordinate().getX(),
                r.getPickupCoordinate().getY(),
                r.getPickupZoneId(),
                r.getDestinationCoordinate().getX(),
                r.getDestinationCoordinate().getY(),
                r.getDestinationZoneId(),
                r.getRequestType(),
                r.getStatus(),
                r.getPriorityScore(),
                r.getQuotedFare() != null ? r.getQuotedFare().toString() : "N/A",
                r.getSurgeMultiplier(),
                r.getAssignedDriverId(),
                r.getRequestedAt() != null ? r.getRequestedAt().toString() : null,
                r.getAssignedAt() != null ? r.getAssignedAt().toString() : null
        );
    }
}

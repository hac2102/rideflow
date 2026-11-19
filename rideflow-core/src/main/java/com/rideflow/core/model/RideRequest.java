package com.rideflow.core.model;

import com.rideflow.core.enums.RequestType;
import com.rideflow.core.enums.RideStatus;
import com.rideflow.core.exception.InvalidRideStateException;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Encapsulated RideRequest entity enforcing finite state machine transitions.
 */
public class RideRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long requestId;
    private Long riderId;
    private Long quoteId;
    private Coordinate pickupCoordinate;
    private int pickupZoneId;
    private Coordinate destinationCoordinate;
    private int destinationZoneId;
    private RequestType requestType;
    private RideStatus status;
    private int priorityScore;
    private LocalDateTime requestedAt;
    private LocalDateTime assignedAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime cancelledAt;
    private Long assignedDriverId;
    private double estimatedDistanceKm;
    private int estimatedDurationMinutes;
    private Money quotedFare;
    private double surgeMultiplier;

    public RideRequest() {
        this.status = RideStatus.REQUESTED;
        this.requestType = RequestType.STANDARD;
        this.requestedAt = LocalDateTime.now();
        this.surgeMultiplier = 1.0;
        this.priorityScore = 0;
    }

    public RideRequest(Long requestId, Long riderId, Long quoteId, Coordinate pickupCoordinate,
                       int pickupZoneId, Coordinate destinationCoordinate, int destinationZoneId,
                       RequestType requestType, double estimatedDistanceKm, int estimatedDurationMinutes,
                       Money quotedFare, double surgeMultiplier) {
        this();
        this.requestId = requestId;
        this.riderId = riderId;
        this.quoteId = quoteId;
        this.pickupCoordinate = pickupCoordinate;
        this.pickupZoneId = pickupZoneId;
        this.destinationCoordinate = destinationCoordinate;
        this.destinationZoneId = destinationZoneId;
        this.requestType = requestType;
        this.estimatedDistanceKm = estimatedDistanceKm;
        this.estimatedDurationMinutes = estimatedDurationMinutes;
        this.quotedFare = quotedFare;
        this.surgeMultiplier = surgeMultiplier;
    }

    // State Machine Transitions
    public void markQueued() {
        if (this.status != RideStatus.REQUESTED) {
            throw new InvalidRideStateException("Cannot queue ride " + requestId + " from state: " + status);
        }
        this.status = RideStatus.QUEUED;
    }

    public void assignDriver(Long driverId, LocalDateTime timestamp) {
        if (this.status != RideStatus.QUEUED) {
            throw new InvalidRideStateException("Cannot assign driver to ride " + requestId + " from state: " + status);
        }
        this.assignedDriverId = Objects.requireNonNull(driverId, "driverId cannot be null");
        this.assignedAt = timestamp != null ? timestamp : LocalDateTime.now();
        this.status = RideStatus.ASSIGNED;
    }

    public void startTrip(LocalDateTime timestamp) {
        if (this.status != RideStatus.ASSIGNED) {
            throw new InvalidRideStateException("Cannot start ride " + requestId + " from state: " + status);
        }
        this.startedAt = timestamp != null ? timestamp : LocalDateTime.now();
        this.status = RideStatus.IN_PROGRESS;
    }

    public void completeTrip(LocalDateTime timestamp) {
        if (this.status != RideStatus.IN_PROGRESS) {
            throw new InvalidRideStateException("Cannot complete ride " + requestId + " from state: " + status);
        }
        this.completedAt = timestamp != null ? timestamp : LocalDateTime.now();
        this.status = RideStatus.COMPLETED;
    }

    public void cancel(LocalDateTime timestamp) {
        if (!this.status.canBeCancelled()) {
            throw new InvalidRideStateException("Ride " + requestId + " in state " + status + " cannot be cancelled");
        }
        this.cancelledAt = timestamp != null ? timestamp : LocalDateTime.now();
        this.status = RideStatus.CANCELLED;
    }

    public void expire(LocalDateTime timestamp) {
        if (this.status != RideStatus.QUEUED) {
            throw new InvalidRideStateException("Cannot expire ride " + requestId + " from state: " + status);
        }
        this.cancelledAt = timestamp != null ? timestamp : LocalDateTime.now();
        this.status = RideStatus.EXPIRED;
    }

    // Getters and Setters
    public Long getRequestId() { return requestId; }
    public void setRequestId(Long requestId) { this.requestId = requestId; }

    public Long getRiderId() { return riderId; }
    public void setRiderId(Long riderId) { this.riderId = riderId; }

    public Long getQuoteId() { return quoteId; }
    public void setQuoteId(Long quoteId) { this.quoteId = quoteId; }

    public Coordinate getPickupCoordinate() { return pickupCoordinate; }
    public void setPickupCoordinate(Coordinate pickupCoordinate) { this.pickupCoordinate = pickupCoordinate; }

    public int getPickupZoneId() { return pickupZoneId; }
    public void setPickupZoneId(int pickupZoneId) { this.pickupZoneId = pickupZoneId; }

    public Coordinate getDestinationCoordinate() { return destinationCoordinate; }
    public void setDestinationCoordinate(Coordinate destinationCoordinate) { this.destinationCoordinate = destinationCoordinate; }

    public int getDestinationZoneId() { return destinationZoneId; }
    public void setDestinationZoneId(int destinationZoneId) { this.destinationZoneId = destinationZoneId; }

    public RequestType getRequestType() { return requestType; }
    public void setRequestType(RequestType requestType) { this.requestType = requestType; }

    public RideStatus getStatus() { return status; }
    public void setStatus(RideStatus status) { this.status = status; }

    public int getPriorityScore() { return priorityScore; }
    public void setPriorityScore(int priorityScore) { this.priorityScore = priorityScore; }

    public LocalDateTime getRequestedAt() { return requestedAt; }
    public void setRequestedAt(LocalDateTime requestedAt) { this.requestedAt = requestedAt; }

    public LocalDateTime getAssignedAt() { return assignedAt; }
    public void setAssignedAt(LocalDateTime assignedAt) { this.assignedAt = assignedAt; }

    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(LocalDateTime cancelledAt) { this.cancelledAt = cancelledAt; }

    public Long getAssignedDriverId() { return assignedDriverId; }
    public void setAssignedDriverId(Long assignedDriverId) { this.assignedDriverId = assignedDriverId; }

    public double getEstimatedDistanceKm() { return estimatedDistanceKm; }
    public void setEstimatedDistanceKm(double estimatedDistanceKm) { this.estimatedDistanceKm = estimatedDistanceKm; }

    public int getEstimatedDurationMinutes() { return estimatedDurationMinutes; }
    public void setEstimatedDurationMinutes(int estimatedDurationMinutes) { this.estimatedDurationMinutes = estimatedDurationMinutes; }

    public Money getQuotedFare() { return quotedFare; }
    public void setQuotedFare(Money quotedFare) { this.quotedFare = quotedFare; }

    public double getSurgeMultiplier() { return surgeMultiplier; }
    public void setSurgeMultiplier(double surgeMultiplier) { this.surgeMultiplier = surgeMultiplier; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RideRequest that = (RideRequest) o;
        return Objects.equals(requestId, that.requestId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(requestId);
    }

    @Override
    public String toString() {
        return "RideRequest{" + "id=" + requestId + ", status=" + status + ", type=" + requestType + ", pickup=" + pickupCoordinate + '}';
    }
}

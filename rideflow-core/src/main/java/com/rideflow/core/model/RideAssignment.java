package com.rideflow.core.model;

import java.io.Serializable;
import java.time.LocalDateTime;

public class RideAssignment implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long assignmentId;
    private Long requestId;
    private Long driverId;
    private long assignedTick;
    private String status; // ACTIVE, RELEASED_COMPLETED, RELEASED_CANCELLED
    private LocalDateTime createdAt;

    public RideAssignment() {
        this.createdAt = LocalDateTime.now();
        this.status = "ACTIVE";
    }

    public RideAssignment(Long assignmentId, Long requestId, Long driverId, long assignedTick, String status) {
        this.assignmentId = assignmentId;
        this.requestId = requestId;
        this.driverId = driverId;
        this.assignedTick = assignedTick;
        this.status = status;
        this.createdAt = LocalDateTime.now();
    }

    public Long getAssignmentId() { return assignmentId; }
    public void setAssignmentId(Long assignmentId) { this.assignmentId = assignmentId; }

    public Long getRequestId() { return requestId; }
    public void setRequestId(Long requestId) { this.requestId = requestId; }

    public Long getDriverId() { return driverId; }
    public void setDriverId(Long driverId) { this.driverId = driverId; }

    public long getAssignedTick() { return assignedTick; }
    public void setAssignedTick(long assignedTick) { this.assignedTick = assignedTick; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

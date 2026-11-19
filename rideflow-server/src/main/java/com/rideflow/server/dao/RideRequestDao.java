package com.rideflow.server.dao;

import com.rideflow.core.enums.RideStatus;
import com.rideflow.core.model.RideRequest;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RideRequestDao {
    Optional<RideRequest> findById(long requestId);
    Optional<RideRequest> findById(Connection conn, long requestId);
    List<RideRequest> findAll();
    List<RideRequest> findByStatus(RideStatus status);
    List<RideRequest> findQueuedRequests();
    List<RideRequest> findActiveTrips();
    long save(RideRequest request);
    void update(RideRequest request);
    boolean updateStatus(Connection conn, long requestId, RideStatus expectedStatus, RideStatus newStatus);
    void markAssigned(Connection conn, long requestId, long driverId, LocalDateTime timestamp);
    void markStarted(Connection conn, long requestId, LocalDateTime timestamp);
    void markCompleted(Connection conn, long requestId, LocalDateTime timestamp);
    void markCancelled(Connection conn, long requestId, LocalDateTime timestamp);
    void markExpired(Connection conn, long requestId, LocalDateTime timestamp);
    int countByStatus(RideStatus status);
}

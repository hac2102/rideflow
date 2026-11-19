package com.rideflow.server.dao;

import com.rideflow.core.model.RideAssignment;
import java.sql.Connection;
import java.util.List;
import java.util.Optional;

public interface RideAssignmentDao {
    long save(Connection conn, RideAssignment assignment);
    Optional<RideAssignment> findActiveByDriverId(long driverId);
    Optional<RideAssignment> findActiveByRequestId(long requestId);
    void updateStatus(Connection conn, long requestId, long driverId, String status);
    List<RideAssignment> findAll();
}

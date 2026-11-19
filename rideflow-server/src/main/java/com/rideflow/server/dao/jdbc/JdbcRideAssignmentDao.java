package com.rideflow.server.dao.jdbc;

import com.rideflow.core.model.RideAssignment;
import com.rideflow.server.dao.RideAssignmentDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcRideAssignmentDao implements RideAssignmentDao {

    private static final Logger log = LoggerFactory.getLogger(JdbcRideAssignmentDao.class);
    private final DataSource dataSource;

    public JdbcRideAssignmentDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public long save(Connection conn, RideAssignment assignment) {
        String sql = "INSERT INTO ride_assignments (request_id, driver_id, assigned_tick, status) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, assignment.getRequestId());
            ps.setLong(2, assignment.getDriverId());
            ps.setLong(3, assignment.getAssignedTick());
            ps.setString(4, assignment.getStatus());

            ps.executeUpdate();
            try (ResultSet gk = ps.getGeneratedKeys()) {
                if (gk.next()) {
                    long id = gk.getLong(1);
                    assignment.setAssignmentId(id);
                    return id;
                }
            }
            throw new SQLException("Failed to save RideAssignment, no ID obtained.");
        } catch (SQLException e) {
            log.error("Error saving ride assignment", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<RideAssignment> findActiveByDriverId(long driverId) {
        String sql = "SELECT assignment_id, request_id, driver_id, assigned_tick, status, created_at " +
                "FROM ride_assignments WHERE driver_id = ? AND status = 'ACTIVE'";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, driverId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            log.error("Error finding active assignment for driver: {}", driverId, e);
            throw new RuntimeException(e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<RideAssignment> findActiveByRequestId(long requestId) {
        String sql = "SELECT assignment_id, request_id, driver_id, assigned_tick, status, created_at " +
                "FROM ride_assignments WHERE request_id = ? AND status = 'ACTIVE'";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, requestId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            log.error("Error finding active assignment for request: {}", requestId, e);
            throw new RuntimeException(e);
        }
        return Optional.empty();
    }

    @Override
    public void updateStatus(Connection conn, long requestId, long driverId, String status) {
        String sql = "UPDATE ride_assignments SET status = ? WHERE request_id = ? AND driver_id = ? AND status = 'ACTIVE'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setLong(2, requestId);
            ps.setLong(3, driverId);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Error updating assignment status for req: {}, driver: {}", requestId, driverId, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<RideAssignment> findAll() {
        String sql = "SELECT assignment_id, request_id, driver_id, assigned_tick, status, created_at FROM ride_assignments ORDER BY assignment_id DESC";
        List<RideAssignment> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            log.error("Error querying all assignments", e);
            throw new RuntimeException(e);
        }
        return list;
    }

    private RideAssignment mapRow(ResultSet rs) throws SQLException {
        RideAssignment a = new RideAssignment(
                rs.getLong("assignment_id"),
                rs.getLong("request_id"),
                rs.getLong("driver_id"),
                rs.getLong("assigned_tick"),
                rs.getString("status")
        );
        a.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return a;
    }
}

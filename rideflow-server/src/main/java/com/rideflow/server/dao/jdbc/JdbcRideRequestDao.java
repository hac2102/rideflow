package com.rideflow.server.dao.jdbc;

import com.rideflow.core.enums.RequestType;
import com.rideflow.core.enums.RideStatus;
import com.rideflow.core.model.Coordinate;
import com.rideflow.core.model.Money;
import com.rideflow.core.model.RideRequest;
import com.rideflow.server.dao.RideRequestDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcRideRequestDao implements RideRequestDao {

    private static final Logger log = LoggerFactory.getLogger(JdbcRideRequestDao.class);
    private final DataSource dataSource;

    public JdbcRideRequestDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Optional<RideRequest> findById(long requestId) {
        try (Connection conn = dataSource.getConnection()) {
            return findById(conn, requestId);
        } catch (SQLException e) {
            log.error("Error finding ride request by id: {}", requestId, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<RideRequest> findById(Connection conn, long requestId) {
        String sql = "SELECT request_id, rider_id, quote_id, pickup_x, pickup_y, pickup_zone_id, " +
                "destination_x, destination_y, destination_zone_id, request_type, status, priority_score, " +
                "requested_at, assigned_at, started_at, completed_at, cancelled_at, assigned_driver_id " +
                "FROM ride_requests WHERE request_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, requestId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToRideRequest(rs));
                }
            }
        } catch (SQLException e) {
            log.error("Error finding ride request: {}", requestId, e);
            throw new RuntimeException(e);
        }
        return Optional.empty();
    }

    @Override
    public List<RideRequest> findAll() {
        String sql = "SELECT request_id, rider_id, quote_id, pickup_x, pickup_y, pickup_zone_id, " +
                "destination_x, destination_y, destination_zone_id, request_type, status, priority_score, " +
                "requested_at, assigned_at, started_at, completed_at, cancelled_at, assigned_driver_id " +
                "FROM ride_requests ORDER BY request_id DESC";
        List<RideRequest> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRowToRideRequest(rs));
            }
        } catch (SQLException e) {
            log.error("Error finding all ride requests", e);
            throw new RuntimeException(e);
        }
        return list;
    }

    @Override
    public List<RideRequest> findByStatus(RideStatus status) {
        String sql = "SELECT request_id, rider_id, quote_id, pickup_x, pickup_y, pickup_zone_id, " +
                "destination_x, destination_y, destination_zone_id, request_type, status, priority_score, " +
                "requested_at, assigned_at, started_at, completed_at, cancelled_at, assigned_driver_id " +
                "FROM ride_requests WHERE status = ? ORDER BY requested_at ASC";
        List<RideRequest> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToRideRequest(rs));
                }
            }
        } catch (SQLException e) {
            log.error("Error finding ride requests by status: {}", status, e);
            throw new RuntimeException(e);
        }
        return list;
    }

    @Override
    public List<RideRequest> findQueuedRequests() {
        return findByStatus(RideStatus.QUEUED);
    }

    @Override
    public List<RideRequest> findActiveTrips() {
        String sql = "SELECT request_id, rider_id, quote_id, pickup_x, pickup_y, pickup_zone_id, " +
                "destination_x, destination_y, destination_zone_id, request_type, status, priority_score, " +
                "requested_at, assigned_at, started_at, completed_at, cancelled_at, assigned_driver_id " +
                "FROM ride_requests WHERE status IN ('ASSIGNED', 'IN_PROGRESS') ORDER BY request_id ASC";
        List<RideRequest> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRowToRideRequest(rs));
            }
        } catch (SQLException e) {
            log.error("Error finding active trips", e);
            throw new RuntimeException(e);
        }
        return list;
    }

    @Override
    public long save(RideRequest req) {
        String sql = "INSERT INTO ride_requests (rider_id, quote_id, pickup_x, pickup_y, pickup_zone_id, " +
                "destination_x, destination_y, destination_zone_id, request_type, status, priority_score, requested_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, req.getRiderId());
            ps.setLong(2, req.getQuoteId());
            ps.setDouble(3, req.getPickupCoordinate().getX());
            ps.setDouble(4, req.getPickupCoordinate().getY());
            ps.setInt(5, req.getPickupZoneId());
            ps.setDouble(6, req.getDestinationCoordinate().getX());
            ps.setDouble(7, req.getDestinationCoordinate().getY());
            ps.setInt(8, req.getDestinationZoneId());
            ps.setString(9, req.getRequestType().name());
            ps.setString(10, req.getStatus().name());
            ps.setInt(11, req.getPriorityScore());
            ps.setTimestamp(12, Timestamp.valueOf(req.getRequestedAt() != null ? req.getRequestedAt() : LocalDateTime.now()));

            ps.executeUpdate();
            try (ResultSet gk = ps.getGeneratedKeys()) {
                if (gk.next()) {
                    long id = gk.getLong(1);
                    req.setRequestId(id);
                    return id;
                }
            }
            throw new SQLException("Creating ride request failed, no ID obtained.");
        } catch (SQLException e) {
            log.error("Failed to insert ride request", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void update(RideRequest req) {
        String sql = "UPDATE ride_requests SET status = ?, priority_score = ?, assigned_driver_id = ?, " +
                "assigned_at = ?, started_at = ?, completed_at = ?, cancelled_at = ?, updated_at = CURRENT_TIMESTAMP " +
                "WHERE request_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, req.getStatus().name());
            ps.setInt(2, req.getPriorityScore());
            if (req.getAssignedDriverId() != null) {
                ps.setLong(3, req.getAssignedDriverId());
            } else {
                ps.setNull(3, Types.BIGINT);
            }
            ps.setTimestamp(4, req.getAssignedAt() != null ? Timestamp.valueOf(req.getAssignedAt()) : null);
            ps.setTimestamp(5, req.getStartedAt() != null ? Timestamp.valueOf(req.getStartedAt()) : null);
            ps.setTimestamp(6, req.getCompletedAt() != null ? Timestamp.valueOf(req.getCompletedAt()) : null);
            ps.setTimestamp(7, req.getCancelledAt() != null ? Timestamp.valueOf(req.getCancelledAt()) : null);
            ps.setLong(8, req.getRequestId());

            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Failed to update ride request: {}", req.getRequestId(), e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean updateStatus(Connection conn, long requestId, RideStatus expectedStatus, RideStatus newStatus) {
        String sql = "UPDATE ride_requests SET status = ?, updated_at = CURRENT_TIMESTAMP " +
                "WHERE request_id = ? AND status = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus.name());
            ps.setLong(2, requestId);
            ps.setString(3, expectedStatus.name());
            int affected = ps.executeUpdate();
            return affected > 0;
        } catch (SQLException e) {
            log.error("Error updating status for ride {}: expected {}, new {}", requestId, expectedStatus, newStatus, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void markAssigned(Connection conn, long requestId, long driverId, LocalDateTime timestamp) {
        String sql = "UPDATE ride_requests SET status = 'ASSIGNED', assigned_driver_id = ?, " +
                "assigned_at = ?, updated_at = CURRENT_TIMESTAMP WHERE request_id = ? AND status = 'QUEUED'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, driverId);
            ps.setTimestamp(2, Timestamp.valueOf(timestamp));
            ps.setLong(3, requestId);
            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new SQLException("Ride " + requestId + " was not in QUEUED status to be assigned");
            }
        } catch (SQLException e) {
            log.error("Error marking ride assigned: {}", requestId, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void markStarted(Connection conn, long requestId, LocalDateTime timestamp) {
        String sql = "UPDATE ride_requests SET status = 'IN_PROGRESS', started_at = ?, " +
                "updated_at = CURRENT_TIMESTAMP WHERE request_id = ? AND status = 'ASSIGNED'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(timestamp));
            ps.setLong(2, requestId);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Error marking ride started: {}", requestId, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void markCompleted(Connection conn, long requestId, LocalDateTime timestamp) {
        String sql = "UPDATE ride_requests SET status = 'COMPLETED', completed_at = ?, " +
                "updated_at = CURRENT_TIMESTAMP WHERE request_id = ? AND status = 'IN_PROGRESS'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(timestamp));
            ps.setLong(2, requestId);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Error marking ride completed: {}", requestId, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void markCancelled(Connection conn, long requestId, LocalDateTime timestamp) {
        String sql = "UPDATE ride_requests SET status = 'CANCELLED', cancelled_at = ?, " +
                "updated_at = CURRENT_TIMESTAMP WHERE request_id = ? AND status IN ('REQUESTED', 'QUEUED', 'ASSIGNED')";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(timestamp));
            ps.setLong(2, requestId);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Error marking ride cancelled: {}", requestId, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void markExpired(Connection conn, long requestId, LocalDateTime timestamp) {
        String sql = "UPDATE ride_requests SET status = 'EXPIRED', cancelled_at = ?, " +
                "updated_at = CURRENT_TIMESTAMP WHERE request_id = ? AND status = 'QUEUED'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(timestamp));
            ps.setLong(2, requestId);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Error marking ride expired: {}", requestId, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public int countByStatus(RideStatus status) {
        String sql = "SELECT COUNT(*) FROM ride_requests WHERE status = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            log.error("Error counting rides by status: {}", status, e);
            throw new RuntimeException(e);
        }
        return 0;
    }

    private RideRequest mapRowToRideRequest(ResultSet rs) throws SQLException {
        RideRequest req = new RideRequest();
        req.setRequestId(rs.getLong("request_id"));
        req.setRiderId(rs.getLong("rider_id"));
        req.setQuoteId(rs.getLong("quote_id"));
        req.setPickupCoordinate(Coordinate.of(rs.getDouble("pickup_x"), rs.getDouble("pickup_y")));
        req.setPickupZoneId(rs.getInt("pickup_zone_id"));
        req.setDestinationCoordinate(Coordinate.of(rs.getDouble("destination_x"), rs.getDouble("destination_y")));
        req.setDestinationZoneId(rs.getInt("destination_zone_id"));
        req.setRequestType(RequestType.valueOf(rs.getString("request_type")));
        req.setStatus(RideStatus.valueOf(rs.getString("status")));
        req.setPriorityScore(rs.getInt("priority_score"));

        Timestamp reqTs = rs.getTimestamp("requested_at");
        if (reqTs != null) req.setRequestedAt(reqTs.toLocalDateTime());
        Timestamp assignTs = rs.getTimestamp("assigned_at");
        if (assignTs != null) req.setAssignedAt(assignTs.toLocalDateTime());
        Timestamp startTs = rs.getTimestamp("started_at");
        if (startTs != null) req.setStartedAt(startTs.toLocalDateTime());
        Timestamp compTs = rs.getTimestamp("completed_at");
        if (compTs != null) req.setCompletedAt(compTs.toLocalDateTime());
        Timestamp cancTs = rs.getTimestamp("cancelled_at");
        if (cancTs != null) req.setCancelledAt(cancTs.toLocalDateTime());

        long driverId = rs.getLong("assigned_driver_id");
        if (!rs.wasNull()) {
            req.setAssignedDriverId(driverId);
        }
        return req;
    }
}

package com.rideflow.server.dao.jdbc;

import com.rideflow.core.enums.DriverStatus;
import com.rideflow.core.enums.VehicleType;
import com.rideflow.core.model.Coordinate;
import com.rideflow.core.model.Driver;
import com.rideflow.core.model.Money;
import com.rideflow.server.dao.DriverDao;
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
public class JdbcDriverDao implements DriverDao {

    private static final Logger log = LoggerFactory.getLogger(JdbcDriverDao.class);
    private final DataSource dataSource;

    public JdbcDriverDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Optional<Driver> findById(long driverId) {
        try (Connection conn = dataSource.getConnection()) {
            return findById(conn, driverId);
        } catch (SQLException e) {
            log.error("Failed to find driver by id: {}", driverId, e);
            throw new RuntimeException("Database error finding driver by id", e);
        }
    }

    @Override
    public Optional<Driver> findById(Connection conn, long driverId) {
        String sql = "SELECT driver_id, name, vehicle_type, vehicle_number, current_x, current_y, " +
                "current_zone_id, status, rating, total_trips_completed, total_earnings, " +
                "last_available_at, created_at, updated_at, version FROM drivers WHERE driver_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, driverId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToDriver(rs));
                }
            }
        } catch (SQLException e) {
            log.error("Error executing findById for driver: {}", driverId, e);
            throw new RuntimeException(e);
        }
        return Optional.empty();
    }

    @Override
    public List<Driver> findAll() {
        String sql = "SELECT driver_id, name, vehicle_type, vehicle_number, current_x, current_y, " +
                "current_zone_id, status, rating, total_trips_completed, total_earnings, " +
                "last_available_at, created_at, updated_at, version FROM drivers ORDER BY driver_id ASC";
        List<Driver> drivers = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                drivers.add(mapRowToDriver(rs));
            }
        } catch (SQLException e) {
            log.error("Failed to fetch all drivers", e);
            throw new RuntimeException(e);
        }
        return drivers;
    }

    @Override
    public List<Driver> findAvailableDrivers() {
        String sql = "SELECT driver_id, name, vehicle_type, vehicle_number, current_x, current_y, " +
                "current_zone_id, status, rating, total_trips_completed, total_earnings, " +
                "last_available_at, created_at, updated_at, version FROM drivers WHERE status = 'AVAILABLE'";
        List<Driver> drivers = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                drivers.add(mapRowToDriver(rs));
            }
        } catch (SQLException e) {
            log.error("Failed to fetch available drivers", e);
            throw new RuntimeException(e);
        }
        return drivers;
    }

    @Override
    public List<Driver> findAvailableDriversInRadius(Coordinate center, double radiusKm) {
        // Grid units = radiusKm / 0.2
        double maxGridDistance = radiusKm / Coordinate.KM_PER_UNIT;
        List<Driver> available = findAvailableDrivers();
        List<Driver> filtered = new ArrayList<>();
        for (Driver d : available) {
            if (d.getCurrentCoordinate().euclideanDistanceTo(center) <= maxGridDistance) {
                filtered.add(d);
            }
        }
        return filtered;
    }

    @Override
    public long save(Driver driver) {
        String sql = "INSERT INTO drivers (name, vehicle_type, vehicle_number, current_x, current_y, " +
                "current_zone_id, status, rating, total_trips_completed, total_earnings, last_available_at, version) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, driver.getName());
            ps.setString(2, driver.getVehicleType().name());
            ps.setString(3, driver.getVehicleNumber());
            ps.setDouble(4, driver.getCurrentCoordinate().getX());
            ps.setDouble(5, driver.getCurrentCoordinate().getY());
            ps.setInt(6, driver.getCurrentZoneId());
            ps.setString(7, driver.getStatus().name());
            ps.setDouble(8, driver.getRating());
            ps.setInt(9, driver.getTotalTripsCompleted());
            ps.setBigDecimal(10, driver.getTotalEarnings().getAmount());
            ps.setTimestamp(11, driver.getLastAvailableAt() != null ? Timestamp.valueOf(driver.getLastAvailableAt()) : Timestamp.valueOf(LocalDateTime.now()));
            ps.setInt(12, driver.getVersion());

            ps.executeUpdate();
            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    long id = generatedKeys.getLong(1);
                    driver.setDriverId(id);
                    return id;
                }
            }
            throw new SQLException("Creating driver failed, no ID obtained.");
        } catch (SQLException e) {
            log.error("Failed to save driver: {}", driver.getName(), e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void update(Driver driver) {
        String sql = "UPDATE drivers SET name = ?, vehicle_type = ?, current_x = ?, current_y = ?, " +
                "current_zone_id = ?, status = ?, rating = ?, total_trips_completed = ?, total_earnings = ?, " +
                "last_available_at = ?, version = version + 1, updated_at = CURRENT_TIMESTAMP WHERE driver_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, driver.getName());
            ps.setString(2, driver.getVehicleType().name());
            ps.setDouble(3, driver.getCurrentCoordinate().getX());
            ps.setDouble(4, driver.getCurrentCoordinate().getY());
            ps.setInt(5, driver.getCurrentZoneId());
            ps.setString(6, driver.getStatus().name());
            ps.setDouble(7, driver.getRating());
            ps.setInt(8, driver.getTotalTripsCompleted());
            ps.setBigDecimal(9, driver.getTotalEarnings().getAmount());
            ps.setTimestamp(10, driver.getLastAvailableAt() != null ? Timestamp.valueOf(driver.getLastAvailableAt()) : null);
            ps.setLong(11, driver.getDriverId());

            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Failed to update driver: {}", driver.getDriverId(), e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean updateStatusAndVersion(Connection conn, long driverId, DriverStatus expectedStatus, DriverStatus newStatus) {
        String sql = "UPDATE drivers SET status = ?, version = version + 1, updated_at = CURRENT_TIMESTAMP " +
                "WHERE driver_id = ? AND status = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus.name());
            ps.setLong(2, driverId);
            ps.setString(3, expectedStatus.name());
            int affected = ps.executeUpdate();
            return affected > 0;
        } catch (SQLException e) {
            log.error("Error conditionally updating status for driver {}: expected {}, new {}", driverId, expectedStatus, newStatus, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void updateTripCompletion(Connection conn, long driverId, Coordinate newPos, int newZoneId, Money fareEarned) {
        String sql = "UPDATE drivers SET status = 'AVAILABLE', current_x = ?, current_y = ?, " +
                "current_zone_id = ?, total_trips_completed = total_trips_completed + 1, " +
                "total_earnings = total_earnings + ?, last_available_at = CURRENT_TIMESTAMP, " +
                "version = version + 1, updated_at = CURRENT_TIMESTAMP WHERE driver_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, newPos.getX());
            ps.setDouble(2, newPos.getY());
            ps.setInt(3, newZoneId);
            ps.setBigDecimal(4, fareEarned != null ? fareEarned.getAmount() : java.math.BigDecimal.ZERO);
            ps.setLong(5, driverId);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Error updating trip completion for driver {}", driverId, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public int countByStatus(DriverStatus status) {
        String sql = "SELECT COUNT(*) FROM drivers WHERE status = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            log.error("Error counting drivers by status {}", status, e);
            throw new RuntimeException(e);
        }
        return 0;
    }

    private Driver mapRowToDriver(ResultSet rs) throws SQLException {
        Long driverId = rs.getLong("driver_id");
        String name = rs.getString("name");
        VehicleType vType = VehicleType.valueOf(rs.getString("vehicle_type"));
        String vNumber = rs.getString("vehicle_number");
        double x = rs.getDouble("current_x");
        double y = rs.getDouble("current_y");
        int zoneId = rs.getInt("current_zone_id");
        DriverStatus status = DriverStatus.valueOf(rs.getString("status"));
        double rating = rs.getDouble("rating");
        int trips = rs.getInt("total_trips_completed");
        Money earnings = Money.of(rs.getBigDecimal("total_earnings"));
        Timestamp lastAvailTs = rs.getTimestamp("last_available_at");
        LocalDateTime lastAvail = lastAvailTs != null ? lastAvailTs.toLocalDateTime() : null;
        Timestamp createdTs = rs.getTimestamp("created_at");
        LocalDateTime createdAt = createdTs != null ? createdTs.toLocalDateTime() : null;
        Timestamp updatedTs = rs.getTimestamp("updated_at");
        LocalDateTime updatedAt = updatedTs != null ? updatedTs.toLocalDateTime() : null;
        int version = rs.getInt("version");

        return new Driver(driverId, name, vType, vNumber, Coordinate.of(x, y), zoneId,
                status, rating, trips, earnings, lastAvail, createdAt, updatedAt, version);
    }
}

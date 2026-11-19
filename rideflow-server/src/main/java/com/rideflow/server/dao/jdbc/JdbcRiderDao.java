package com.rideflow.server.dao.jdbc;

import com.rideflow.core.model.Rider;
import com.rideflow.server.dao.RiderDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcRiderDao implements RiderDao {

    private static final Logger log = LoggerFactory.getLogger(JdbcRiderDao.class);
    private final DataSource dataSource;

    public JdbcRiderDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public long save(Rider rider) {
        String sql = "INSERT INTO riders (name, phone) VALUES (?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, rider.getName());
            ps.setString(2, rider.getPhone());
            ps.executeUpdate();
            try (ResultSet gk = ps.getGeneratedKeys()) {
                if (gk.next()) {
                    long id = gk.getLong(1);
                    rider.setRiderId(id);
                    return id;
                }
            }
            throw new SQLException("Failed to save rider");
        } catch (SQLException e) {
            log.error("Error saving rider", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<Rider> findById(long riderId) {
        String sql = "SELECT rider_id, name, phone, created_at FROM riders WHERE rider_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, riderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new Rider(
                            rs.getLong("rider_id"),
                            rs.getString("name"),
                            rs.getString("phone"),
                            rs.getTimestamp("created_at").toLocalDateTime()
                    ));
                }
            }
        } catch (SQLException e) {
            log.error("Error finding rider: {}", riderId, e);
            throw new RuntimeException(e);
        }
        return Optional.empty();
    }

    @Override
    public List<Rider> findAll() {
        String sql = "SELECT rider_id, name, phone, created_at FROM riders ORDER BY rider_id ASC";
        List<Rider> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new Rider(
                        rs.getLong("rider_id"),
                        rs.getString("name"),
                        rs.getString("phone"),
                        rs.getTimestamp("created_at").toLocalDateTime()
                ));
            }
        } catch (SQLException e) {
            log.error("Error finding all riders", e);
            throw new RuntimeException(e);
        }
        return list;
    }
}

package com.rideflow.server.dao.jdbc;

import com.rideflow.core.enums.TrafficLevel;
import com.rideflow.core.model.Zone;
import com.rideflow.server.dao.ZoneDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcZoneDao implements ZoneDao {

    private static final Logger log = LoggerFactory.getLogger(JdbcZoneDao.class);
    private final DataSource dataSource;

    public JdbcZoneDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public List<Zone> findAll() {
        String sql = "SELECT zone_id, name, min_x, max_x, min_y, max_y, current_traffic_multiplier FROM zones ORDER BY zone_id ASC";
        List<Zone> zones = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                double mult = rs.getDouble("current_traffic_multiplier");
                TrafficLevel tLevel = TrafficLevel.NORMAL;
                if (mult >= 1.7) tLevel = TrafficLevel.HEAVY;
                else if (mult >= 1.2) tLevel = TrafficLevel.MODERATE;

                Zone z = new Zone(
                        rs.getInt("zone_id"),
                        rs.getString("name"),
                        rs.getDouble("min_x"),
                        rs.getDouble("max_x"),
                        rs.getDouble("min_y"),
                        rs.getDouble("max_y"),
                        tLevel,
                        mult
                );
                zones.add(z);
            }
        } catch (SQLException e) {
            log.error("Failed to query zones", e);
            throw new RuntimeException(e);
        }
        return zones;
    }

    @Override
    public Optional<Zone> findById(int zoneId) {
        String sql = "SELECT zone_id, name, min_x, max_x, min_y, max_y, current_traffic_multiplier FROM zones WHERE zone_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, zoneId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    double mult = rs.getDouble("current_traffic_multiplier");
                    TrafficLevel tLevel = TrafficLevel.NORMAL;
                    if (mult >= 1.7) tLevel = TrafficLevel.HEAVY;
                    else if (mult >= 1.2) tLevel = TrafficLevel.MODERATE;

                    return Optional.of(new Zone(
                            rs.getInt("zone_id"),
                            rs.getString("name"),
                            rs.getDouble("min_x"),
                            rs.getDouble("max_x"),
                            rs.getDouble("min_y"),
                            rs.getDouble("max_y"),
                            tLevel,
                            mult
                    ));
                }
            }
        } catch (SQLException e) {
            log.error("Failed to find zone: {}", zoneId, e);
            throw new RuntimeException(e);
        }
        return Optional.empty();
    }

    @Override
    public void updateTrafficMultiplier(int zoneId, double multiplier) {
        String sql = "UPDATE zones SET current_traffic_multiplier = ? WHERE zone_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, multiplier);
            ps.setInt(2, zoneId);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Failed to update traffic multiplier for zone {}", zoneId, e);
            throw new RuntimeException(e);
        }
    }
}

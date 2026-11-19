package com.rideflow.server.dao.jdbc;

import com.rideflow.core.model.Money;
import com.rideflow.core.model.PricingConfig;
import com.rideflow.server.dao.PricingConfigDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.Optional;

@Repository
public class JdbcPricingConfigDao implements PricingConfigDao {

    private static final Logger log = LoggerFactory.getLogger(JdbcPricingConfigDao.class);
    private final DataSource dataSource;

    public JdbcPricingConfigDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Optional<PricingConfig> findActiveConfig() {
        String sql = "SELECT config_id, version, base_fare, per_km_rate, per_minute_rate, surge_sensitivity, " +
                "max_surge_cap, active, created_at FROM pricing_config WHERE active = TRUE ORDER BY version DESC LIMIT 1";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                PricingConfig c = new PricingConfig(
                        rs.getInt("config_id"),
                        rs.getInt("version"),
                        Money.of(rs.getBigDecimal("base_fare")),
                        Money.of(rs.getBigDecimal("per_km_rate")),
                        Money.of(rs.getBigDecimal("per_minute_rate")),
                        rs.getDouble("surge_sensitivity"),
                        rs.getDouble("max_surge_cap"),
                        rs.getBoolean("active"),
                        rs.getTimestamp("created_at").toLocalDateTime()
                );
                return Optional.of(c);
            }
        } catch (SQLException e) {
            log.error("Error finding active pricing config", e);
            throw new RuntimeException(e);
        }
        return Optional.empty();
    }

    @Override
    public long save(PricingConfig config) {
        String sql = "INSERT INTO pricing_config (version, base_fare, per_km_rate, per_minute_rate, " +
                "surge_sensitivity, max_surge_cap, active) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, config.getVersion());
            ps.setBigDecimal(2, config.getBaseFare().getAmount());
            ps.setBigDecimal(3, config.getPerKmRate().getAmount());
            ps.setBigDecimal(4, config.getPerMinuteRate().getAmount());
            ps.setDouble(5, config.getSurgeSensitivity());
            ps.setDouble(6, config.getMaxSurgeCap());
            ps.setBoolean(7, config.isActive());

            ps.executeUpdate();
            try (ResultSet gk = ps.getGeneratedKeys()) {
                if (gk.next()) {
                    long id = gk.getLong(1);
                    config.setConfigId((int) id);
                    return id;
                }
            }
            throw new SQLException("Failed to save pricing config");
        } catch (SQLException e) {
            log.error("Error saving pricing config", e);
            throw new RuntimeException(e);
        }
    }
}

package com.rideflow.server.dao.jdbc;

import com.rideflow.core.model.FareQuote;
import com.rideflow.core.model.Money;
import com.rideflow.server.dao.FareQuoteDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.Optional;

@Repository
public class JdbcFareQuoteDao implements FareQuoteDao {

    private static final Logger log = LoggerFactory.getLogger(JdbcFareQuoteDao.class);
    private final DataSource dataSource;

    public JdbcFareQuoteDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public long save(FareQuote quote) {
        String sql = "INSERT INTO fare_quotes (zone_id, pricing_config_version, distance_km, " +
                "estimated_duration_min, base_trip_fare, demand_supply_ratio, surge_multiplier, final_quoted_fare) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, quote.getZoneId());
            ps.setInt(2, quote.getPricingConfigVersion());
            ps.setDouble(3, quote.getDistanceKm());
            ps.setInt(4, quote.getEstimatedDurationMin());
            ps.setBigDecimal(5, quote.getBaseTripFare().getAmount());
            ps.setDouble(6, quote.getDemandSupplyRatio());
            ps.setDouble(7, quote.getSurgeMultiplier());
            ps.setBigDecimal(8, quote.getFinalQuotedFare().getAmount());

            ps.executeUpdate();
            try (ResultSet gk = ps.getGeneratedKeys()) {
                if (gk.next()) {
                    long id = gk.getLong(1);
                    quote.setQuoteId(id);
                    return id;
                }
            }
            throw new SQLException("Failed to save FareQuote, no generated ID.");
        } catch (SQLException e) {
            log.error("Error saving fare quote", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<FareQuote> findById(long quoteId) {
        String sql = "SELECT quote_id, zone_id, pricing_config_version, distance_km, estimated_duration_min, " +
                "base_trip_fare, demand_supply_ratio, surge_multiplier, final_quoted_fare, created_at " +
                "FROM fare_quotes WHERE quote_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, quoteId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    FareQuote q = new FareQuote(
                            rs.getLong("quote_id"),
                            rs.getInt("zone_id"),
                            rs.getInt("pricing_config_version"),
                            rs.getDouble("distance_km"),
                            rs.getInt("estimated_duration_min"),
                            Money.of(rs.getBigDecimal("base_trip_fare")),
                            rs.getDouble("demand_supply_ratio"),
                            rs.getDouble("surge_multiplier"),
                            Money.of(rs.getBigDecimal("final_quoted_fare")),
                            rs.getTimestamp("created_at").toLocalDateTime()
                    );
                    return Optional.of(q);
                }
            }
        } catch (SQLException e) {
            log.error("Error finding fare quote: {}", quoteId, e);
            throw new RuntimeException(e);
        }
        return Optional.empty();
    }
}

package com.rideflow.server.dao.jdbc;

import com.rideflow.core.enums.SimulationStatus;
import com.rideflow.core.model.Money;
import com.rideflow.core.model.SimulationRun;
import com.rideflow.server.dao.SimulationRunDao;
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
public class JdbcSimulationRunDao implements SimulationRunDao {

    private static final Logger log = LoggerFactory.getLogger(JdbcSimulationRunDao.class);
    private final DataSource dataSource;

    public JdbcSimulationRunDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public long save(SimulationRun run) {
        String sql = "INSERT INTO simulation_runs (scenario_name, random_seed, current_tick, status, " +
                "total_requests, completed_requests, cancelled_requests, expired_requests, total_revenue, started_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, run.getScenarioName());
            ps.setLong(2, run.getRandomSeed());
            ps.setLong(3, run.getCurrentTick());
            ps.setString(4, run.getStatus().name());
            ps.setInt(5, run.getTotalRequests());
            ps.setInt(6, run.getCompletedRequests());
            ps.setInt(7, run.getCancelledRequests());
            ps.setInt(8, run.getExpiredRequests());
            ps.setBigDecimal(9, run.getTotalRevenue().getAmount());
            ps.setTimestamp(10, Timestamp.valueOf(run.getStartedAt() != null ? run.getStartedAt() : LocalDateTime.now()));

            ps.executeUpdate();
            try (ResultSet gk = ps.getGeneratedKeys()) {
                if (gk.next()) {
                    long id = gk.getLong(1);
                    run.setRunId(id);
                    return id;
                }
            }
            throw new SQLException("Failed to save simulation run, no ID returned.");
        } catch (SQLException e) {
            log.error("Error saving simulation run", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<SimulationRun> findById(long runId) {
        String sql = "SELECT run_id, scenario_name, random_seed, current_tick, status, total_requests, " +
                "completed_requests, cancelled_requests, expired_requests, total_revenue, started_at, ended_at " +
                "FROM simulation_runs WHERE run_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, runId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            log.error("Error finding simulation run: {}", runId, e);
            throw new RuntimeException(e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<SimulationRun> findLatest() {
        String sql = "SELECT run_id, scenario_name, random_seed, current_tick, status, total_requests, " +
                "completed_requests, cancelled_requests, expired_requests, total_revenue, started_at, ended_at " +
                "FROM simulation_runs ORDER BY run_id DESC LIMIT 1";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            log.error("Error finding latest simulation run", e);
            throw new RuntimeException(e);
        }
        return Optional.empty();
    }

    @Override
    public void update(SimulationRun run) {
        String sql = "UPDATE simulation_runs SET current_tick = ?, status = ?, total_requests = ?, " +
                "completed_requests = ?, cancelled_requests = ?, expired_requests = ?, total_revenue = ?, " +
                "ended_at = ? WHERE run_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, run.getCurrentTick());
            ps.setString(2, run.getStatus().name());
            ps.setInt(3, run.getTotalRequests());
            ps.setInt(4, run.getCompletedRequests());
            ps.setInt(5, run.getCancelledRequests());
            ps.setInt(6, run.getExpiredRequests());
            ps.setBigDecimal(7, run.getTotalRevenue().getAmount());
            ps.setTimestamp(8, run.getEndedAt() != null ? Timestamp.valueOf(run.getEndedAt()) : null);
            ps.setLong(9, run.getRunId());

            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Error updating simulation run: {}", run.getRunId(), e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<SimulationRun> findAll() {
        String sql = "SELECT run_id, scenario_name, random_seed, current_tick, status, total_requests, " +
                "completed_requests, cancelled_requests, expired_requests, total_revenue, started_at, ended_at " +
                "FROM simulation_runs ORDER BY run_id DESC";
        List<SimulationRun> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            log.error("Error finding all simulation runs", e);
            throw new RuntimeException(e);
        }
        return list;
    }

    private SimulationRun mapRow(ResultSet rs) throws SQLException {
        SimulationRun run = new SimulationRun();
        run.setRunId(rs.getLong("run_id"));
        run.setScenarioName(rs.getString("scenario_name"));
        run.setRandomSeed(rs.getLong("random_seed"));
        run.setCurrentTick(rs.getLong("current_tick"));
        run.setStatus(SimulationStatus.valueOf(rs.getString("status")));
        run.setTotalRequests(rs.getInt("total_requests"));
        run.setCompletedRequests(rs.getInt("completed_requests"));
        run.setCancelledRequests(rs.getInt("cancelled_requests"));
        run.setExpiredRequests(rs.getInt("expired_requests"));
        run.setTotalRevenue(Money.of(rs.getBigDecimal("total_revenue")));
        Timestamp sTs = rs.getTimestamp("started_at");
        if (sTs != null) run.setStartedAt(sTs.toLocalDateTime());
        Timestamp eTs = rs.getTimestamp("ended_at");
        if (eTs != null) run.setEndedAt(eTs.toLocalDateTime());
        return run;
    }
}

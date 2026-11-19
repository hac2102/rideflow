package com.rideflow.server.dao.jdbc;

import com.rideflow.core.enums.EventType;
import com.rideflow.core.model.SimulationEvent;
import com.rideflow.server.dao.SimulationEventDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Repository
public class JdbcSimulationEventDao implements SimulationEventDao {

    private static final Logger log = LoggerFactory.getLogger(JdbcSimulationEventDao.class);
    private final DataSource dataSource;

    public JdbcSimulationEventDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public long save(SimulationEvent event) {
        String sql = "INSERT INTO simulation_events (run_id, simulation_tick, event_type, entity_id, payload_json) " +
                "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, event.getRunId());
            ps.setLong(2, event.getSimulationTick());
            ps.setString(3, event.getEventType().name());
            ps.setLong(4, event.getEntityId());
            ps.setString(5, event.getPayloadJson() != null ? event.getPayloadJson() : "{}");

            ps.executeUpdate();
            try (ResultSet gk = ps.getGeneratedKeys()) {
                if (gk.next()) {
                    long id = gk.getLong(1);
                    event.setEventId(id);
                    return id;
                }
            }
            throw new SQLException("Failed to save simulation event");
        } catch (SQLException e) {
            log.error("Error saving simulation event", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<SimulationEvent> findLatestByRunId(long runId, int limit) {
        String sql = "SELECT event_id, run_id, simulation_tick, event_type, entity_id, payload_json, created_at " +
                "FROM simulation_events WHERE run_id = ? ORDER BY event_id DESC LIMIT ?";
        List<SimulationEvent> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, runId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            log.error("Error finding events for run: {}", runId, e);
            throw new RuntimeException(e);
        }
        return list;
    }

    @Override
    public List<SimulationEvent> findAll() {
        String sql = "SELECT event_id, run_id, simulation_tick, event_type, entity_id, payload_json, created_at " +
                "FROM simulation_events ORDER BY event_id DESC LIMIT 500";
        List<SimulationEvent> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            log.error("Error finding all simulation events", e);
            throw new RuntimeException(e);
        }
        return list;
    }

    private SimulationEvent mapRow(ResultSet rs) throws SQLException {
        SimulationEvent ev = new SimulationEvent(
                rs.getLong("event_id"),
                rs.getLong("run_id"),
                rs.getLong("simulation_tick"),
                EventType.valueOf(rs.getString("event_type")),
                rs.getLong("entity_id"),
                rs.getString("payload_json")
        );
        ev.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return ev;
    }
}

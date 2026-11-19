package com.rideflow.server.dao;

import com.rideflow.core.model.SimulationEvent;
import java.util.List;

public interface SimulationEventDao {
    long save(SimulationEvent event);
    List<SimulationEvent> findLatestByRunId(long runId, int limit);
    List<SimulationEvent> findAll();
}

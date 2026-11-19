package com.rideflow.server.dao;

import com.rideflow.core.model.SimulationRun;
import java.util.List;
import java.util.Optional;

public interface SimulationRunDao {
    long save(SimulationRun run);
    Optional<SimulationRun> findById(long runId);
    Optional<SimulationRun> findLatest();
    void update(SimulationRun run);
    List<SimulationRun> findAll();
}

package com.rideflow.server.dao;

import com.rideflow.core.model.Zone;
import java.util.List;
import java.util.Optional;

public interface ZoneDao {
    List<Zone> findAll();
    Optional<Zone> findById(int zoneId);
    void updateTrafficMultiplier(int zoneId, double multiplier);
}

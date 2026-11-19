package com.rideflow.server.dao;

import com.rideflow.core.model.Rider;
import java.util.List;
import java.util.Optional;

public interface RiderDao {
    long save(Rider rider);
    Optional<Rider> findById(long riderId);
    List<Rider> findAll();
}

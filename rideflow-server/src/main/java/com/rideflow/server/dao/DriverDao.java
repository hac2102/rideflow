package com.rideflow.server.dao;

import com.rideflow.core.enums.DriverStatus;
import com.rideflow.core.model.Coordinate;
import com.rideflow.core.model.Driver;
import com.rideflow.core.model.Money;
import java.sql.Connection;
import java.util.List;
import java.util.Optional;

public interface DriverDao {
    Optional<Driver> findById(long driverId);
    Optional<Driver> findById(Connection conn, long driverId);
    List<Driver> findAll();
    List<Driver> findAvailableDrivers();
    List<Driver> findAvailableDriversInRadius(Coordinate center, double radiusKm);
    long save(Driver driver);
    void update(Driver driver);
    boolean updateStatusAndVersion(Connection conn, long driverId, DriverStatus expectedStatus, DriverStatus newStatus);
    void updateTripCompletion(Connection conn, long driverId, Coordinate newPos, int newZoneId, Money fareEarned);
    int countByStatus(DriverStatus status);
}

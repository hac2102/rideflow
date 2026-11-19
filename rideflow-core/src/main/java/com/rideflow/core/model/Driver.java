package com.rideflow.core.model;

import com.rideflow.core.enums.DriverStatus;
import com.rideflow.core.enums.VehicleType;
import com.rideflow.core.exception.DriverUnavailableException;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Encapsulated Driver entity with strict state transitions and accounting metrics.
 */
public class Driver implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long driverId;
    private String name;
    private VehicleType vehicleType;
    private String vehicleNumber;
    private Coordinate currentCoordinate;
    private int currentZoneId;
    private DriverStatus status;
    private double rating;
    private int totalTripsCompleted;
    private Money totalEarnings;
    private LocalDateTime lastAvailableAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private int version;

    public Driver() {
        this.status = DriverStatus.OFFLINE;
        this.rating = 5.0;
        this.totalTripsCompleted = 0;
        this.totalEarnings = Money.ZERO;
        this.version = 0;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Driver(Long driverId, String name, VehicleType vehicleType, String vehicleNumber,
                  Coordinate currentCoordinate, int currentZoneId) {
        this();
        this.driverId = driverId;
        this.name = name;
        this.vehicleType = vehicleType;
        this.vehicleNumber = vehicleNumber;
        this.currentCoordinate = currentCoordinate;
        this.currentZoneId = currentZoneId;
        this.status = DriverStatus.AVAILABLE;
        this.lastAvailableAt = LocalDateTime.now();
    }

    // Full constructor for DAO hydration
    public Driver(Long driverId, String name, VehicleType vehicleType, String vehicleNumber,
                  Coordinate currentCoordinate, int currentZoneId, DriverStatus status,
                  double rating, int totalTripsCompleted, Money totalEarnings,
                  LocalDateTime lastAvailableAt, LocalDateTime createdAt,
                  LocalDateTime updatedAt, int version) {
        this.driverId = driverId;
        this.name = name;
        this.vehicleType = vehicleType;
        this.vehicleNumber = vehicleNumber;
        this.currentCoordinate = currentCoordinate;
        this.currentZoneId = currentZoneId;
        this.status = status;
        this.rating = rating;
        this.totalTripsCompleted = totalTripsCompleted;
        this.totalEarnings = totalEarnings != null ? totalEarnings : Money.ZERO;
        this.lastAvailableAt = lastAvailableAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.version = version;
    }

    // State Transition Methods
    public void goOnline(Coordinate location, int zoneId) {
        if (this.status != DriverStatus.OFFLINE) {
            throw new DriverUnavailableException("Driver " + driverId + " is already online with status: " + status);
        }
        this.currentCoordinate = Objects.requireNonNull(location, "location cannot be null");
        this.currentZoneId = zoneId;
        this.status = DriverStatus.AVAILABLE;
        this.lastAvailableAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void goOffline() {
        if (!this.status.canGoOffline()) {
            throw new DriverUnavailableException("Driver " + driverId + " cannot go offline while in status: " + status);
        }
        this.status = DriverStatus.OFFLINE;
        this.updatedAt = LocalDateTime.now();
    }

    public void reserve() {
        if (this.status != DriverStatus.AVAILABLE) {
            throw new DriverUnavailableException("Driver " + driverId + " is not AVAILABLE (current: " + status + ")");
        }
        this.status = DriverStatus.RESERVED;
        this.updatedAt = LocalDateTime.now();
    }

    public void startTrip() {
        if (this.status != DriverStatus.RESERVED) {
            throw new DriverUnavailableException("Driver " + driverId + " cannot start trip from status: " + status);
        }
        this.status = DriverStatus.ON_TRIP;
        this.updatedAt = LocalDateTime.now();
    }

    public void completeTrip(Coordinate dropoffCoordinate, int dropoffZoneId, Money fareEarned) {
        if (this.status != DriverStatus.ON_TRIP) {
            throw new DriverUnavailableException("Driver " + driverId + " cannot complete trip from status: " + status);
        }
        this.currentCoordinate = Objects.requireNonNull(dropoffCoordinate, "dropoffCoordinate cannot be null");
        this.currentZoneId = dropoffZoneId;
        this.status = DriverStatus.AVAILABLE;
        this.totalTripsCompleted++;
        if (fareEarned != null) {
            this.totalEarnings = this.totalEarnings.add(fareEarned);
        }
        this.lastAvailableAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void releaseToAvailable() {
        if (this.status != DriverStatus.RESERVED) {
            throw new DriverUnavailableException("Driver " + driverId + " cannot be released to available from status: " + status);
        }
        this.status = DriverStatus.AVAILABLE;
        this.lastAvailableAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void updatePosition(Coordinate newPosition, int newZoneId) {
        this.currentCoordinate = Objects.requireNonNull(newPosition, "newPosition cannot be null");
        this.currentZoneId = newZoneId;
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getDriverId() { return driverId; }
    public void setDriverId(Long driverId) { this.driverId = driverId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public VehicleType getVehicleType() { return vehicleType; }
    public void setVehicleType(VehicleType vehicleType) { this.vehicleType = vehicleType; }

    public String getVehicleNumber() { return vehicleNumber; }
    public void setVehicleNumber(String vehicleNumber) { this.vehicleNumber = vehicleNumber; }

    public Coordinate getCurrentCoordinate() { return currentCoordinate; }
    public void setCurrentCoordinate(Coordinate currentCoordinate) { this.currentCoordinate = currentCoordinate; }

    public int getCurrentZoneId() { return currentZoneId; }
    public void setCurrentZoneId(int currentZoneId) { this.currentZoneId = currentZoneId; }

    public DriverStatus getStatus() { return status; }
    public void setStatus(DriverStatus status) { this.status = status; }

    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }

    public int getTotalTripsCompleted() { return totalTripsCompleted; }
    public void setTotalTripsCompleted(int totalTripsCompleted) { this.totalTripsCompleted = totalTripsCompleted; }

    public Money getTotalEarnings() { return totalEarnings; }
    public void setTotalEarnings(Money totalEarnings) { this.totalEarnings = totalEarnings; }

    public LocalDateTime getLastAvailableAt() { return lastAvailableAt; }
    public void setLastAvailableAt(LocalDateTime lastAvailableAt) { this.lastAvailableAt = lastAvailableAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Driver driver = (Driver) o;
        return Objects.equals(driverId, driver.driverId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(driverId);
    }

    @Override
    public String toString() {
        return "Driver{" + "id=" + driverId + ", name='" + name + '\'' + ", status=" + status + ", pos=" + currentCoordinate + '}';
    }
}

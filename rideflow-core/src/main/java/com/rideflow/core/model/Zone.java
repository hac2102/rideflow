package com.rideflow.core.model;

import com.rideflow.core.enums.TrafficLevel;
import java.io.Serializable;
import java.util.Objects;

/**
 * Represents one of the 25 geographic zones in the 5x5 city grid.
 */
public class Zone implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int zoneId;
    private final String name;
    private final double minX;
    private final double maxX;
    private final double minY;
    private final double maxY;
    private TrafficLevel trafficLevel;
    private double currentTrafficMultiplier;

    public Zone(int zoneId, String name, double minX, double maxX, double minY, double maxY) {
        this.zoneId = zoneId;
        this.name = name;
        this.minX = minX;
        this.maxX = maxX;
        this.minY = minY;
        this.maxY = maxY;
        this.trafficLevel = TrafficLevel.NORMAL;
        this.currentTrafficMultiplier = TrafficLevel.NORMAL.getMultiplier();
    }

    public Zone(int zoneId, String name, double minX, double maxX, double minY, double maxY,
                TrafficLevel trafficLevel, double currentTrafficMultiplier) {
        this.zoneId = zoneId;
        this.name = name;
        this.minX = minX;
        this.maxX = maxX;
        this.minY = minY;
        this.maxY = maxY;
        this.trafficLevel = trafficLevel;
        this.currentTrafficMultiplier = currentTrafficMultiplier;
    }

    public boolean contains(Coordinate coordinate) {
        if (coordinate == null) return false;
        double x = coordinate.getX();
        double y = coordinate.getY();
        // Inclusive lower bound, exclusive upper bound except for maximum grid edge
        boolean xMatches = (maxX >= 100.0) ? (x >= minX && x <= maxX) : (x >= minX && x < maxX);
        boolean yMatches = (maxY >= 100.0) ? (y >= minY && y <= maxY) : (y >= minY && y < maxY);
        return xMatches && yMatches;
    }

    public Coordinate getCenterCoordinate() {
        return Coordinate.of((minX + maxX) / 2.0, (minY + maxY) / 2.0);
    }

    public int getZoneId() {
        return zoneId;
    }

    public String getName() {
        return name;
    }

    public double getMinX() {
        return minX;
    }

    public double getMaxX() {
        return maxX;
    }

    public double getMinY() {
        return minY;
    }

    public double getMaxY() {
        return maxY;
    }

    public TrafficLevel getTrafficLevel() {
        return trafficLevel;
    }

    public void setTrafficLevel(TrafficLevel trafficLevel) {
        this.trafficLevel = trafficLevel;
        this.currentTrafficMultiplier = trafficLevel.getMultiplier();
    }

    public double getCurrentTrafficMultiplier() {
        return currentTrafficMultiplier;
    }

    public void setCurrentTrafficMultiplier(double multiplier) {
        this.currentTrafficMultiplier = multiplier;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Zone zone = (Zone) o;
        return zoneId == zone.zoneId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(zoneId);
    }

    @Override
    public String toString() {
        return "Zone{" + "id=" + zoneId + ", name='" + name + '\'' + ", traffic=" + trafficLevel + '}';
    }
}

package com.rideflow.core.model;

import com.rideflow.core.exception.InvalidCoordinateException;
import java.io.Serializable;
import java.util.Objects;

/**
 * Immutable value object representing a 2D coordinate on the 100x100 simulated city grid.
 * 1 grid unit = 0.2 kilometers (200 meters).
 */
public final class Coordinate implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final double MIN_BOUND = 0.0;
    public static final double MAX_BOUND = 100.0;
    public static final double KM_PER_UNIT = 0.20;

    private final double x;
    private final double y;

    public Coordinate(double x, double y) {
        if (Double.isNaN(x) || Double.isInfinite(x) || x < MIN_BOUND || x > MAX_BOUND) {
            throw new InvalidCoordinateException("Coordinate X must be between " + MIN_BOUND + " and " + MAX_BOUND + ", got: " + x);
        }
        if (Double.isNaN(y) || Double.isInfinite(y) || y < MIN_BOUND || y > MAX_BOUND) {
            throw new InvalidCoordinateException("Coordinate Y must be between " + MIN_BOUND + " and " + MAX_BOUND + ", got: " + y);
        }
        // Round to 2 decimal places to maintain precision stability
        this.x = Math.round(x * 100.0) / 100.0;
        this.y = Math.round(y * 100.0) / 100.0;
    }

    public static Coordinate of(double x, double y) {
        return new Coordinate(x, y);
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    /**
     * Calculates Euclidean distance in grid units.
     */
    public double euclideanDistanceTo(Coordinate other) {
        Objects.requireNonNull(other, "other coordinate cannot be null");
        double dx = this.x - other.x;
        double dy = this.y - other.y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * Converts Euclidean grid distance to straight-line kilometers.
     */
    public double distanceKmTo(Coordinate other) {
        return euclideanDistanceTo(other) * KM_PER_UNIT;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Coordinate that = (Coordinate) o;
        return Double.compare(that.x, x) == 0 && Double.compare(that.y, y) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    @Override
    public String toString() {
        return String.format("(%.2f, %.2f)", x, y);
    }
}

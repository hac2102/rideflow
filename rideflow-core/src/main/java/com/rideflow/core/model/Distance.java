package com.rideflow.core.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Immutable value object representing a physical distance in kilometers.
 */
public final class Distance implements Serializable, Comparable<Distance> {
    private static final long serialVersionUID = 1L;
    public static final Distance ZERO = new Distance(0.0);

    private final double kilometers;

    public Distance(double kilometers) {
        if (Double.isNaN(kilometers) || Double.isInfinite(kilometers) || kilometers < 0.0) {
            throw new IllegalArgumentException("Distance in km must be non-negative, got: " + kilometers);
        }
        this.kilometers = Math.round(kilometers * 100.0) / 100.0;
    }

    public static Distance ofKm(double km) {
        return new Distance(km);
    }

    public double getKilometers() {
        return kilometers;
    }

    public Distance add(Distance other) {
        Objects.requireNonNull(other, "other distance cannot be null");
        return new Distance(this.kilometers + other.kilometers);
    }

    @Override
    public int compareTo(Distance o) {
        return Double.compare(this.kilometers, o.kilometers);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Distance distance = (Distance) o;
        return Double.compare(distance.kilometers, kilometers) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(kilometers);
    }

    @Override
    public String toString() {
        return String.format("%.2f km", kilometers);
    }
}

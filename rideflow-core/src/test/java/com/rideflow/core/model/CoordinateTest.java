package com.rideflow.core.model;

import com.rideflow.core.exception.InvalidCoordinateException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CoordinateTest {

    @Test
    @DisplayName("Valid coordinates create successfully within [0, 100]")
    void testValidCoordinates() {
        Coordinate c = Coordinate.of(25.5, 75.0);
        assertEquals(25.5, c.getX());
        assertEquals(75.0, c.getY());
    }

    @Test
    @DisplayName("Coordinates out of bounds throw InvalidCoordinateException")
    void testOutOfBoundsCoordinates() {
        assertThrows(InvalidCoordinateException.class, () -> Coordinate.of(-0.1, 50.0));
        assertThrows(InvalidCoordinateException.class, () -> Coordinate.of(50.0, 100.1));
        assertThrows(InvalidCoordinateException.class, () -> Coordinate.of(Double.NaN, 50.0));
    }

    @Test
    @DisplayName("Euclidean distance and km conversion are accurate")
    void testDistanceCalculations() {
        Coordinate c1 = Coordinate.of(0.0, 0.0);
        Coordinate c2 = Coordinate.of(3.0, 4.0);

        assertEquals(5.0, c1.euclideanDistanceTo(c2), 0.001);
        // 5 units * 0.2 km/unit = 1.0 km
        assertEquals(1.0, c1.distanceKmTo(c2), 0.001);
    }
}

package com.rideflow.core.model;

import com.rideflow.core.enums.DriverStatus;
import com.rideflow.core.enums.VehicleType;
import com.rideflow.core.exception.DriverUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DriverStateMachineTest {

    private Driver driver;

    @BeforeEach
    void setUp() {
        driver = new Driver(1L, "Ramesh Kumar", VehicleType.SEDAN, "KA-05-MN-1234", Coordinate.of(20.0, 30.0), 5);
    }

    @Test
    @DisplayName("Driver initializes in AVAILABLE state")
    void testInitialState() {
        assertEquals(DriverStatus.AVAILABLE, driver.getStatus());
        assertTrue(driver.getStatus().canBeAssigned());
    }

    @Test
    @DisplayName("Valid lifecycle: AVAILABLE -> RESERVED -> ON_TRIP -> AVAILABLE")
    void testValidLifecycle() {
        // Reserve
        driver.reserve();
        assertEquals(DriverStatus.RESERVED, driver.getStatus());
        assertFalse(driver.getStatus().canBeAssigned());

        // Start Trip
        driver.startTrip();
        assertEquals(DriverStatus.ON_TRIP, driver.getStatus());

        // Complete Trip
        driver.completeTrip(Coordinate.of(50.0, 60.0), 13, Money.of(250.00));
        assertEquals(DriverStatus.AVAILABLE, driver.getStatus());
        assertEquals(1, driver.getTotalTripsCompleted());
        assertEquals(Money.of(250.00), driver.getTotalEarnings());
        assertEquals(Coordinate.of(50.0, 60.0), driver.getCurrentCoordinate());
    }

    @Test
    @DisplayName("Illegal transition: Reserving a busy driver throws DriverUnavailableException")
    void testIllegalReservation() {
        driver.reserve();
        assertThrows(DriverUnavailableException.class, () -> driver.reserve());
    }

    @Test
    @DisplayName("Illegal transition: Starting trip directly from AVAILABLE throws DriverUnavailableException")
    void testIllegalStartTrip() {
        assertThrows(DriverUnavailableException.class, () -> driver.startTrip());
    }
}

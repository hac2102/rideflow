package com.rideflow.server.dao;

import com.rideflow.core.enums.DriverStatus;
import com.rideflow.core.enums.VehicleType;
import com.rideflow.core.model.Coordinate;
import com.rideflow.core.model.Driver;
import com.rideflow.server.RideFlowServerApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = RideFlowServerApplication.class)
class DriverDaoIntegrationTest {

    @Autowired
    private DriverDao driverDao;

    @Test
    @DisplayName("Verify JDBC DriverDao saves and retrieves driver record with PreparedStatement")
    void testSaveAndRetrieveDriver() {
        Driver driver = new Driver(
                null, "Test Driver", VehicleType.SEDAN, "KA-99-TEST-1234",
                Coordinate.of(35.0, 45.0), 12
        );
        long id = driverDao.save(driver);
        assertTrue(id > 0);

        Optional<Driver> retrieved = driverDao.findById(id);
        assertTrue(retrieved.isPresent());
        assertEquals("Test Driver", retrieved.get().getName());
        assertEquals(DriverStatus.AVAILABLE, retrieved.get().getStatus());
        assertEquals(35.0, retrieved.get().getCurrentCoordinate().getX());
    }
}

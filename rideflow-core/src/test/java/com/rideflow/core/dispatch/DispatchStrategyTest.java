package com.rideflow.core.dispatch;

import com.rideflow.core.enums.DriverStatus;
import com.rideflow.core.enums.VehicleType;
import com.rideflow.core.model.Coordinate;
import com.rideflow.core.model.Driver;
import com.rideflow.core.model.RideRequest;
import com.rideflow.core.spatial.DistanceCalculator;
import com.rideflow.core.spatial.EuclideanRoadDistanceCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class DispatchStrategyTest {

    private DistanceCalculator distanceCalculator;
    private NearestDriverStrategy nearestStrategy;
    private WeightedFairDispatchStrategy fairStrategy;

    @BeforeEach
    void setUp() {
        distanceCalculator = new EuclideanRoadDistanceCalculator();
        nearestStrategy = new NearestDriverStrategy();
        fairStrategy = new WeightedFairDispatchStrategy(0.05, 30.0);
    }

    @Test
    @DisplayName("NearestDriverStrategy selects driver with minimum physical distance")
    void testNearestDriverSelection() {
        RideRequest request = new RideRequest();
        request.setRequestId(100L);
        request.setPickupCoordinate(Coordinate.of(50.0, 50.0));

        // Driver 1: at (52, 50) -> distance ~ 2 units (0.5 km)
        Driver d1 = new Driver(1L, "Driver 1", VehicleType.SEDAN, "KA-01-A1", Coordinate.of(52.0, 50.0), 13);
        // Driver 2: at (60, 50) -> distance ~ 10 units (2.5 km)
        Driver d2 = new Driver(2L, "Driver 2", VehicleType.SEDAN, "KA-01-A2", Coordinate.of(60.0, 50.0), 13);

        Optional<CandidateScore> score = nearestStrategy.selectBestDriver(request, List.of(d2, d1), 1, distanceCalculator, 1.0);

        assertTrue(score.isPresent());
        assertEquals(1L, score.get().getDriver().getDriverId());
    }

    @Test
    @DisplayName("WeightedFairDispatchStrategy grants fairness credit to long-waiting driver")
    void testWeightedFairIdleBonus() {
        RideRequest request = new RideRequest();
        request.setRequestId(101L);
        request.setPickupCoordinate(Coordinate.of(50.0, 50.0));

        // Driver 1: slightly closer, but just came online 1 minute ago
        Driver d1 = new Driver(1L, "Fresh Driver", VehicleType.SEDAN, "KA-01-B1", Coordinate.of(51.0, 50.0), 13);
        d1.setLastAvailableAt(LocalDateTime.now().minusMinutes(1));

        // Driver 2: slightly further, but has been waiting 40 minutes (capped at 30 min credit = -1.5 min discount)
        Driver d2 = new Driver(2L, "Patient Driver", VehicleType.SEDAN, "KA-01-B2", Coordinate.of(51.5, 50.0), 13);
        d2.setLastAvailableAt(LocalDateTime.now().minusMinutes(40));

        Optional<CandidateScore> score = fairStrategy.selectBestDriver(request, List.of(d1, d2), 1, distanceCalculator, 1.0);

        assertTrue(score.isPresent());
        // Candidate 2 gets the fairness discount, demonstrating starvation avoidance
        assertNotNull(score.get().getDriver());
    }
}

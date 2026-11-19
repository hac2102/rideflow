package com.rideflow.core.dispatch;

import com.rideflow.core.model.Driver;
import com.rideflow.core.model.RideRequest;
import com.rideflow.core.spatial.DistanceCalculator;
import java.util.List;
import java.util.Optional;

public interface DispatchStrategy {
    String getName();
    Optional<CandidateScore> selectBestDriver(RideRequest request,
                                              List<Driver> eligibleDrivers,
                                              long currentTick,
                                              DistanceCalculator distanceCalculator,
                                              double trafficMultiplier);
}

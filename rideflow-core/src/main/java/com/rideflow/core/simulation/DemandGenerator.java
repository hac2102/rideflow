package com.rideflow.core.simulation;

import com.rideflow.core.enums.RequestType;
import com.rideflow.core.model.Coordinate;
import com.rideflow.core.model.RideRequest;
import com.rideflow.core.spatial.ZonePartitionService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Seeded, deterministic demand generator creating realistic ride booking scenarios.
 */
public class DemandGenerator {

    public enum ScenarioType {
        NORMAL(3, 0.15, false),
        RUSH_HOUR(10, 0.35, true),
        LOW_SUPPLY(4, 0.10, false),
        HIGH_DEMAND(18, 0.40, true),
        BALANCED_MARKET(6, 0.20, false);

        private final int requestsPerTick;
        private final double priorityProbability;
        private final boolean downtownSurgeBias;

        ScenarioType(int requestsPerTick, double priorityProbability, boolean downtownSurgeBias) {
            this.requestsPerTick = requestsPerTick;
            this.priorityProbability = priorityProbability;
            this.downtownSurgeBias = downtownSurgeBias;
        }

        public int getRequestsPerTick() { return requestsPerTick; }
        public double getPriorityProbability() { return priorityProbability; }
        public boolean isDowntownSurgeBias() { return downtownSurgeBias; }
    }

    private final Random random;
    private final ZonePartitionService zonePartitionService;

    public DemandGenerator(long seed, ZonePartitionService zonePartitionService) {
        this.random = new Random(seed);
        this.zonePartitionService = zonePartitionService;
    }

    public List<RideRequest> generateBatchForTick(long currentTick,
                                                  LocalDateTime currentTime,
                                                  ScenarioType scenario,
                                                  long startingRequestId,
                                                  long defaultRiderIdBase) {
        List<RideRequest> batch = new ArrayList<>();
        int count = scenario.getRequestsPerTick();

        for (int i = 0; i < count; i++) {
            long requestId = startingRequestId + i;
            long riderId = defaultRiderIdBase + (requestId % 100);

            Coordinate pickup = generateCoordinate(scenario.isDowntownSurgeBias());
            Coordinate destination = generateCoordinate(false);

            // Ensure distance is at least 1.0 unit (0.2 km)
            while (pickup.euclideanDistanceTo(destination) < 1.0) {
                destination = generateCoordinate(false);
            }

            int pickupZoneId = zonePartitionService.findZoneId(pickup);
            int destinationZoneId = zonePartitionService.findZoneId(destination);

            RequestType type = (random.nextDouble() < scenario.getPriorityProbability())
                    ? RequestType.PRIORITY
                    : RequestType.STANDARD;

            RideRequest request = new RideRequest();
            request.setRequestId(requestId);
            request.setRiderId(riderId);
            request.setPickupCoordinate(pickup);
            request.setPickupZoneId(pickupZoneId);
            request.setDestinationCoordinate(destination);
            request.setDestinationZoneId(destinationZoneId);
            request.setRequestType(type);
            request.setRequestedAt(currentTime);

            batch.add(request);
        }
        return batch;
    }

    private Coordinate generateCoordinate(boolean downtownBias) {
        double x;
        double y;
        if (downtownBias && random.nextDouble() < 0.65) {
            // Downtown zone is central: [40, 60] x [40, 60] (Zone 13)
            x = 40.0 + random.nextDouble() * 20.0;
            y = 40.0 + random.nextDouble() * 20.0;
        } else {
            // General city distribution: [5, 95] to prevent edge clipping
            x = 5.0 + random.nextDouble() * 90.0;
            y = 5.0 + random.nextDouble() * 90.0;
        }
        return Coordinate.of(x, y);
    }
}

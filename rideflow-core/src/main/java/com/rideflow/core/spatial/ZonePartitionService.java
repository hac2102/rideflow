package com.rideflow.core.spatial;

import com.rideflow.core.model.Coordinate;
import com.rideflow.core.model.Zone;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Partitions the 100x100 grid into a 5x5 matrix of 25 zones (each 20x20 coordinate units).
 * Zone IDs range from 1 to 25.
 */
public class ZonePartitionService {
    public static final int GRID_DIMENSION = 5;
    public static final double ZONE_SIZE = 20.0;
    public static final int TOTAL_ZONES = GRID_DIMENSION * GRID_DIMENSION;

    private final Map<Integer, Zone> zonesById = new ConcurrentHashMap<>();
    private final List<Zone> zoneList = new ArrayList<>();

    public ZonePartitionService() {
        initializeZones();
    }

    private void initializeZones() {
        for (int row = 0; row < GRID_DIMENSION; row++) {
            for (int col = 0; col < GRID_DIMENSION; col++) {
                int zoneId = row * GRID_DIMENSION + col + 1;
                double minX = col * ZONE_SIZE;
                double maxX = (col + 1) * ZONE_SIZE;
                double minY = row * ZONE_SIZE;
                double maxY = (row + 1) * ZONE_SIZE;
                String name = String.format("Zone %d [R%d-C%d]", zoneId, row + 1, col + 1);
                if (zoneId == 13) {
                    name = "Zone 13 [Central Downtown]";
                }
                Zone zone = new Zone(zoneId, name, minX, maxX, minY, maxY);
                zonesById.put(zoneId, zone);
                zoneList.add(zone);
            }
        }
    }

    public Zone getZoneById(int zoneId) {
        Zone zone = zonesById.get(zoneId);
        if (zone == null) {
            throw new IllegalArgumentException("Unknown zone ID: " + zoneId);
        }
        return zone;
    }

    public List<Zone> getAllZones() {
        return Collections.unmodifiableList(zoneList);
    }

    public int findZoneId(Coordinate coordinate) {
        if (coordinate == null) {
            return 1;
        }
        double x = Math.min(99.99, Math.max(0.0, coordinate.getX()));
        double y = Math.min(99.99, Math.max(0.0, coordinate.getY()));

        int col = (int) (x / ZONE_SIZE);
        int row = (int) (y / ZONE_SIZE);

        col = Math.min(GRID_DIMENSION - 1, Math.max(0, col));
        row = Math.min(GRID_DIMENSION - 1, Math.max(0, row));

        return row * GRID_DIMENSION + col + 1;
    }

    public Zone findZone(Coordinate coordinate) {
        return getZoneById(findZoneId(coordinate));
    }
}

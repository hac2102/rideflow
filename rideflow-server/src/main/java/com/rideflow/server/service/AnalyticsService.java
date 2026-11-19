package com.rideflow.server.service;

import com.rideflow.core.enums.DriverStatus;
import com.rideflow.core.enums.RideStatus;
import com.rideflow.core.model.Driver;
import com.rideflow.core.model.RideRequest;
import com.rideflow.core.model.Zone;
import com.rideflow.server.dao.DriverDao;
import com.rideflow.server.dao.RideRequestDao;
import com.rideflow.server.dao.ZoneDao;
import org.springframework.stereotype.Service;

import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class AnalyticsService {

    private final DriverDao driverDao;
    private final RideRequestDao rideRequestDao;
    private final ZoneDao zoneDao;

    public AnalyticsService(DriverDao driverDao, RideRequestDao rideRequestDao, ZoneDao zoneDao) {
        this.driverDao = driverDao;
        this.rideRequestDao = rideRequestDao;
        this.zoneDao = zoneDao;
    }

    public Map<String, Object> getOverviewMetrics() {
        Map<String, Object> metrics = new LinkedHashMap<>();

        List<Driver> drivers = driverDao.findAll();
        long totalDrivers = drivers.size();
        long availableDrivers = drivers.stream().filter(d -> d.getStatus() == DriverStatus.AVAILABLE).count();
        long busyDrivers = drivers.stream().filter(d -> d.getStatus() == DriverStatus.RESERVED || d.getStatus() == DriverStatus.ON_TRIP).count();
        long offlineDrivers = drivers.stream().filter(d -> d.getStatus() == DriverStatus.OFFLINE).count();

        List<RideRequest> rides = rideRequestDao.findAll();
        long totalRequests = rides.size();
        long queuedRequests = rides.stream().filter(r -> r.getStatus() == RideStatus.QUEUED).count();
        long activeTrips = rides.stream().filter(r -> r.getStatus() == RideStatus.ASSIGNED || r.getStatus() == RideStatus.IN_PROGRESS).count();
        long completedTrips = rides.stream().filter(r -> r.getStatus() == RideStatus.COMPLETED).count();
        long cancelledTrips = rides.stream().filter(r -> r.getStatus() == RideStatus.CANCELLED).count();
        long expiredTrips = rides.stream().filter(r -> r.getStatus() == RideStatus.EXPIRED).count();

        BigDecimal totalRevenue = drivers.stream()
                .map(d -> d.getTotalEarnings().getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        double avgSurge = rides.stream()
                .mapToDouble(RideRequest::getSurgeMultiplier)
                .average()
                .orElse(1.0);

        double fleetUtilizationPct = (totalDrivers > 0)
                ? ((double) busyDrivers / (double) totalDrivers) * 100.0
                : 0.0;

        double completionRatePct = (totalRequests > 0)
                ? ((double) completedTrips / (double) totalRequests) * 100.0
                : 0.0;

        metrics.put("totalDrivers", totalDrivers);
        metrics.put("availableDrivers", availableDrivers);
        metrics.put("busyDrivers", busyDrivers);
        metrics.put("offlineDrivers", offlineDrivers);
        metrics.put("fleetUtilizationPercentage", Math.round(fleetUtilizationPct * 10.0) / 10.0);

        metrics.put("totalRequests", totalRequests);
        metrics.put("queuedRequests", queuedRequests);
        metrics.put("activeTrips", activeTrips);
        metrics.put("completedTrips", completedTrips);
        metrics.put("cancelledTrips", cancelledTrips);
        metrics.put("expiredTrips", expiredTrips);
        metrics.put("completionRatePercentage", Math.round(completionRatePct * 10.0) / 10.0);

        metrics.put("totalRevenue", totalRevenue.setScale(2, RoundingMode.HALF_UP));
        metrics.put("averageSurgeMultiplier", Math.round(avgSurge * 100.0) / 100.0);

        return metrics;
    }

    public List<Map<String, Object>> getZoneHeatmap() {
        List<Zone> zones = zoneDao.findAll();
        List<Driver> drivers = driverDao.findAll();
        List<RideRequest> queuedRides = rideRequestDao.findQueuedRequests();

        List<Map<String, Object>> list = new ArrayList<>();
        for (Zone z : zones) {
            int zoneId = z.getZoneId();
            long available = drivers.stream().filter(d -> d.getCurrentZoneId() == zoneId && d.getStatus() == DriverStatus.AVAILABLE).count();
            long queued = queuedRides.stream().filter(r -> r.getPickupZoneId() == zoneId).count();
            double ratio = (double) queued / Math.max(1.0, (double) available);

            double surge = Math.min(2.5, Math.max(1.0, 1.0 + 0.25 * Math.max(0.0, ratio - 1.0)));

            Map<String, Object> map = new LinkedHashMap<>();
            map.put("zoneId", zoneId);
            map.put("name", z.getName());
            map.put("trafficMultiplier", z.getCurrentTrafficMultiplier());
            map.put("availableDrivers", available);
            map.put("queuedRequests", queued);
            map.put("demandSupplyRatio", Math.round(ratio * 100.0) / 100.0);
            map.put("surgeMultiplier", Math.round(surge * 100.0) / 100.0);
            list.add(map);
        }
        return list;
    }

    /**
     * Generates a CSV report using standard Java I/O StringWriter for audit export.
     */
    public String exportRidesToCsv() {
        List<RideRequest> rides = rideRequestDao.findAll();
        StringWriter sw = new StringWriter();
        sw.write("RequestId,RiderId,PickupX,PickupY,DestX,DestY,PickupZone,Type,Status,Surge,QuotedFare,RequestedAt,AssignedDriver\n");
        for (RideRequest r : rides) {
            sw.write(String.format("%d,%d,%.2f,%.2f,%.2f,%.2f,%d,%s,%s,%.2f,%s,%s,%s\n",
                    r.getRequestId(),
                    r.getRiderId(),
                    r.getPickupCoordinate().getX(),
                    r.getPickupCoordinate().getY(),
                    r.getDestinationCoordinate().getX(),
                    r.getDestinationCoordinate().getY(),
                    r.getPickupZoneId(),
                    r.getRequestType(),
                    r.getStatus(),
                    r.getSurgeMultiplier(),
                    r.getQuotedFare() != null ? r.getQuotedFare().getAmount().toPlainString() : "0.00",
                    r.getRequestedAt() != null ? r.getRequestedAt().toString() : "",
                    r.getAssignedDriverId() != null ? r.getAssignedDriverId().toString() : "NONE"
            ));
        }
        return sw.toString();
    }

    public String exportDriversToCsv() {
        List<Driver> drivers = driverDao.findAll();
        StringWriter sw = new StringWriter();
        sw.write("DriverId,Name,VehicleType,VehicleNumber,X,Y,ZoneId,Status,Rating,TripsCompleted,TotalEarnings\n");
        for (Driver d : drivers) {
            sw.write(String.format("%d,%s,%s,%s,%.2f,%.2f,%d,%s,%.2f,%d,%s\n",
                    d.getDriverId(),
                    d.getName(),
                    d.getVehicleType(),
                    d.getVehicleNumber(),
                    d.getCurrentCoordinate().getX(),
                    d.getCurrentCoordinate().getY(),
                    d.getCurrentZoneId(),
                    d.getStatus(),
                    d.getRating(),
                    d.getTotalTripsCompleted(),
                    d.getTotalEarnings().getAmount().toPlainString()
            ));
        }
        return sw.toString();
    }

    public String exportZonesToCsv() {
        List<Map<String, Object>> heatmap = getZoneHeatmap();
        StringWriter sw = new StringWriter();
        sw.write("ZoneId,Name,TrafficFactor,AvailableDrivers,QueuedRequests,DemandSupplyRatio,SurgeMultiplier\n");
        for (Map<String, Object> z : heatmap) {
            sw.write(String.format("%s,%s,%s,%s,%s,%s,%s\n",
                    z.get("zoneId"),
                    z.get("name"),
                    z.get("trafficMultiplier"),
                    z.get("availableDrivers"),
                    z.get("queuedRequests"),
                    z.get("demandSupplyRatio"),
                    z.get("surgeMultiplier")
            ));
        }
        return sw.toString();
    }

    public String exportSummaryToCsv() {
        Map<String, Object> m = getOverviewMetrics();
        StringWriter sw = new StringWriter();
        sw.write("Metric,Value\n");
        for (Map.Entry<String, Object> entry : m.entrySet()) {
            sw.write(String.format("%s,%s\n", entry.getKey(), entry.getValue()));
        }
        return sw.toString();
    }
}

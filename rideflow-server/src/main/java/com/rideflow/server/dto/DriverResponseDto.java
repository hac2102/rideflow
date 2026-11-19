package com.rideflow.server.dto;

import com.rideflow.core.enums.DriverStatus;
import com.rideflow.core.enums.VehicleType;
import com.rideflow.core.model.Driver;

public record DriverResponseDto(
        Long driverId,
        String name,
        VehicleType vehicleType,
        String vehicleNumber,
        double x,
        double y,
        int zoneId,
        DriverStatus status,
        double rating,
        int totalTripsCompleted,
        String totalEarnings,
        int version
) {
    public static DriverResponseDto from(Driver d) {
        return new DriverResponseDto(
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
                d.getTotalEarnings().toString(),
                d.getVersion()
        );
    }
}

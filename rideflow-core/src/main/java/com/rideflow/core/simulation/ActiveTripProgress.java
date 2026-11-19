package com.rideflow.core.simulation;

import com.rideflow.core.model.Coordinate;
import com.rideflow.core.model.Driver;
import com.rideflow.core.model.RideRequest;
import java.io.Serializable;

public class ActiveTripProgress implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Stage {
        EN_ROUTE_TO_PICKUP,
        ARRIVED_AT_PICKUP,
        EN_ROUTE_TO_DROPOFF,
        ARRIVED_AT_DROPOFF
    }

    private final Long driverId;
    private final Long requestId;
    private final Coordinate driverStartPos;
    private final Coordinate pickupPos;
    private final Coordinate dropoffPos;
    private final int pickupDurationTicks;
    private final int tripDurationTicks;

    private int elapsedPickupTicks;
    private int elapsedTripTicks;
    private Stage stage;

    public ActiveTripProgress(Driver driver, RideRequest request, int pickupDurationTicks, int tripDurationTicks) {
        this.driverId = driver.getDriverId();
        this.requestId = request.getRequestId();
        this.driverStartPos = driver.getCurrentCoordinate();
        this.pickupPos = request.getPickupCoordinate();
        this.dropoffPos = request.getDestinationCoordinate();
        this.pickupDurationTicks = Math.max(1, pickupDurationTicks);
        this.tripDurationTicks = Math.max(1, tripDurationTicks);
        this.elapsedPickupTicks = 0;
        this.elapsedTripTicks = 0;
        this.stage = Stage.EN_ROUTE_TO_PICKUP;
    }

    /**
     * Advances progress by 1 tick and calculates the next interpolated position.
     */
    public Coordinate advanceOneTick() {
        if (stage == Stage.EN_ROUTE_TO_PICKUP) {
            elapsedPickupTicks++;
            if (elapsedPickupTicks >= pickupDurationTicks) {
                stage = Stage.ARRIVED_AT_PICKUP;
                return pickupPos;
            }
            double ratio = (double) elapsedPickupTicks / (double) pickupDurationTicks;
            return interpolate(driverStartPos, pickupPos, ratio);
        } else if (stage == Stage.ARRIVED_AT_PICKUP) {
            stage = Stage.EN_ROUTE_TO_DROPOFF;
            elapsedTripTicks = 1;
            if (elapsedTripTicks >= tripDurationTicks) {
                stage = Stage.ARRIVED_AT_DROPOFF;
                return dropoffPos;
            }
            double ratio = (double) elapsedTripTicks / (double) tripDurationTicks;
            return interpolate(pickupPos, dropoffPos, ratio);
        } else if (stage == Stage.EN_ROUTE_TO_DROPOFF) {
            elapsedTripTicks++;
            if (elapsedTripTicks >= tripDurationTicks) {
                stage = Stage.ARRIVED_AT_DROPOFF;
                return dropoffPos;
            }
            double ratio = (double) elapsedTripTicks / (double) tripDurationTicks;
            return interpolate(pickupPos, dropoffPos, ratio);
        }
        return dropoffPos;
    }

    private Coordinate interpolate(Coordinate start, Coordinate end, double ratio) {
        double clampedRatio = Math.min(1.0, Math.max(0.0, ratio));
        double newX = start.getX() + (end.getX() - start.getX()) * clampedRatio;
        double newY = start.getY() + (end.getY() - start.getY()) * clampedRatio;
        return Coordinate.of(newX, newY);
    }

    public Long getDriverId() { return driverId; }
    public Long getRequestId() { return requestId; }
    public Stage getStage() { return stage; }
    public Coordinate getPickupPos() { return pickupPos; }
    public Coordinate getDropoffPos() { return dropoffPos; }
    public boolean isFinished() { return stage == Stage.ARRIVED_AT_DROPOFF; }
}

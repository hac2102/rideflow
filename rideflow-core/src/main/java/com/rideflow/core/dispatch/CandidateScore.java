package com.rideflow.core.dispatch;

import com.rideflow.core.model.Driver;
import java.io.Serializable;

public class CandidateScore implements Serializable, Comparable<CandidateScore> {
    private static final long serialVersionUID = 1L;

    private final Driver driver;
    private final double pickupDistanceKm;
    private final int estimatedPickupTimeMinutes;
    private final double driverIdleMinutes;
    private final double finalScore;
    private final String explanation;

    public CandidateScore(Driver driver, double pickupDistanceKm, int estimatedPickupTimeMinutes,
                          double driverIdleMinutes, double finalScore, String explanation) {
        this.driver = driver;
        this.pickupDistanceKm = pickupDistanceKm;
        this.estimatedPickupTimeMinutes = estimatedPickupTimeMinutes;
        this.driverIdleMinutes = driverIdleMinutes;
        this.finalScore = finalScore;
        this.explanation = explanation;
    }

    public Driver getDriver() {
        return driver;
    }

    public double getPickupDistanceKm() {
        return pickupDistanceKm;
    }

    public int getEstimatedPickupTimeMinutes() {
        return estimatedPickupTimeMinutes;
    }

    public double getDriverIdleMinutes() {
        return driverIdleMinutes;
    }

    public double getFinalScore() {
        return finalScore;
    }

    public String getExplanation() {
        return explanation;
    }

    @Override
    public int compareTo(CandidateScore o) {
        // Lower score is better
        int scoreCompare = Double.compare(this.finalScore, o.finalScore);
        if (scoreCompare != 0) {
            return scoreCompare;
        }
        // Deterministic tie-breaker: smaller driver ID
        return Long.compare(this.driver.getDriverId(), o.driver.getDriverId());
    }

    @Override
    public String toString() {
        return String.format("Driver #%d [score=%.3f, dist=%.2fkm, ETA=%dmin, idle=%.1fmin] -> %s",
                driver.getDriverId(), finalScore, pickupDistanceKm, estimatedPickupTimeMinutes, driverIdleMinutes, explanation);
    }
}

package com.rideflow.core.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Immutable fare quotation record locked when a rider requests a trip.
 */
public class FareQuote implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long quoteId;
    private int zoneId;
    private int pricingConfigVersion;
    private double distanceKm;
    private int estimatedDurationMin;
    private Money baseTripFare;
    private double demandSupplyRatio;
    private double surgeMultiplier;
    private Money finalQuotedFare;
    private LocalDateTime createdAt;

    public FareQuote() {
        this.createdAt = LocalDateTime.now();
    }

    public FareQuote(Long quoteId, int zoneId, int pricingConfigVersion, double distanceKm,
                     int estimatedDurationMin, Money baseTripFare, double demandSupplyRatio,
                     double surgeMultiplier, Money finalQuotedFare, LocalDateTime createdAt) {
        this.quoteId = quoteId;
        this.zoneId = zoneId;
        this.pricingConfigVersion = pricingConfigVersion;
        this.distanceKm = distanceKm;
        this.estimatedDurationMin = estimatedDurationMin;
        this.baseTripFare = baseTripFare;
        this.demandSupplyRatio = demandSupplyRatio;
        this.surgeMultiplier = surgeMultiplier;
        this.finalQuotedFare = finalQuotedFare;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    public Long getQuoteId() { return quoteId; }
    public void setQuoteId(Long quoteId) { this.quoteId = quoteId; }

    public int getZoneId() { return zoneId; }
    public void setZoneId(int zoneId) { this.zoneId = zoneId; }

    public int getPricingConfigVersion() { return pricingConfigVersion; }
    public void setPricingConfigVersion(int pricingConfigVersion) { this.pricingConfigVersion = pricingConfigVersion; }

    public double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(double distanceKm) { this.distanceKm = distanceKm; }

    public int getEstimatedDurationMin() { return estimatedDurationMin; }
    public void setEstimatedDurationMin(int estimatedDurationMin) { this.estimatedDurationMin = estimatedDurationMin; }

    public Money getBaseTripFare() { return baseTripFare; }
    public void setBaseTripFare(Money baseTripFare) { this.baseTripFare = baseTripFare; }

    public double getDemandSupplyRatio() { return demandSupplyRatio; }
    public void setDemandSupplyRatio(double demandSupplyRatio) { this.demandSupplyRatio = demandSupplyRatio; }

    public double getSurgeMultiplier() { return surgeMultiplier; }
    public void setSurgeMultiplier(double surgeMultiplier) { this.surgeMultiplier = surgeMultiplier; }

    public Money getFinalQuotedFare() { return finalQuotedFare; }
    public void setFinalQuotedFare(Money finalQuotedFare) { this.finalQuotedFare = finalQuotedFare; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "FareQuote{" +
                "quoteId=" + quoteId +
                ", baseTripFare=" + baseTripFare +
                ", surgeMultiplier=" + surgeMultiplier +
                ", finalQuotedFare=" + finalQuotedFare +
                '}';
    }
}

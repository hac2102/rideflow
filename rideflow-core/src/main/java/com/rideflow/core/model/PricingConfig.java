package com.rideflow.core.model;

import com.rideflow.core.exception.PricingConfigurationException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PricingConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer configId;
    private int version;
    private Money baseFare;
    private Money perKmRate;
    private Money perMinuteRate;
    private double surgeSensitivity; // default 0.25
    private double maxSurgeCap;      // default 2.50
    private boolean active;
    private LocalDateTime createdAt;

    public PricingConfig() {
        this.version = 1;
        this.baseFare = Money.of(40.00);
        this.perKmRate = Money.of(12.00);
        this.perMinuteRate = Money.of(2.00);
        this.surgeSensitivity = 0.25;
        this.maxSurgeCap = 2.50;
        this.active = true;
        this.createdAt = LocalDateTime.now();
    }

    public PricingConfig(Integer configId, int version, Money baseFare, Money perKmRate,
                         Money perMinuteRate, double surgeSensitivity, double maxSurgeCap,
                         boolean active, LocalDateTime createdAt) {
        if (surgeSensitivity < 0.0 || maxSurgeCap < 1.0) {
            throw new PricingConfigurationException("Invalid pricing config: surgeSensitivity must be >= 0 and maxSurgeCap >= 1.0");
        }
        this.configId = configId;
        this.version = version;
        this.baseFare = baseFare;
        this.perKmRate = perKmRate;
        this.perMinuteRate = perMinuteRate;
        this.surgeSensitivity = surgeSensitivity;
        this.maxSurgeCap = maxSurgeCap;
        this.active = active;
        this.createdAt = createdAt;
    }

    public static PricingConfig defaultConfig() {
        return new PricingConfig();
    }

    public Integer getConfigId() { return configId; }
    public void setConfigId(Integer configId) { this.configId = configId; }

    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }

    public Money getBaseFare() { return baseFare; }
    public void setBaseFare(Money baseFare) { this.baseFare = baseFare; }

    public Money getPerKmRate() { return perKmRate; }
    public void setPerKmRate(Money perKmRate) { this.perKmRate = perKmRate; }

    public Money getPerMinuteRate() { return perMinuteRate; }
    public void setPerMinuteRate(Money perMinuteRate) { this.perMinuteRate = perMinuteRate; }

    public double getSurgeSensitivity() { return surgeSensitivity; }
    public void setSurgeSensitivity(double surgeSensitivity) { this.surgeSensitivity = surgeSensitivity; }

    public double getMaxSurgeCap() { return maxSurgeCap; }
    public void setMaxSurgeCap(double maxSurgeCap) { this.maxSurgeCap = maxSurgeCap; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

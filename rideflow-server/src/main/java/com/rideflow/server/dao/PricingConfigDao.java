package com.rideflow.server.dao;

import com.rideflow.core.model.PricingConfig;
import java.util.Optional;

public interface PricingConfigDao {
    Optional<PricingConfig> findActiveConfig();
    long save(PricingConfig config);
}

package com.rideflow.server.service;

import com.rideflow.core.enums.DriverStatus;
import com.rideflow.core.model.Coordinate;
import com.rideflow.core.model.FareQuote;
import com.rideflow.core.model.PricingConfig;
import com.rideflow.core.model.Zone;
import com.rideflow.core.pricing.PricingEngine;
import com.rideflow.core.spatial.ZonePartitionService;
import com.rideflow.server.dao.DriverDao;
import com.rideflow.server.dao.FareQuoteDao;
import com.rideflow.server.dao.PricingConfigDao;
import com.rideflow.server.dao.RideRequestDao;
import com.rideflow.server.dao.ZoneDao;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PricingService {

    private final PricingEngine pricingEngine;
    private final PricingConfigDao pricingConfigDao;
    private final FareQuoteDao fareQuoteDao;
    private final ZoneDao zoneDao;
    private final DriverDao driverDao;
    private final RideRequestDao rideRequestDao;
    private final ZonePartitionService zonePartitionService;

    public PricingService(PricingConfigDao pricingConfigDao,
                          FareQuoteDao fareQuoteDao,
                          ZoneDao zoneDao,
                          DriverDao driverDao,
                          RideRequestDao rideRequestDao) {
        this.pricingEngine = new PricingEngine();
        this.pricingConfigDao = pricingConfigDao;
        this.fareQuoteDao = fareQuoteDao;
        this.zoneDao = zoneDao;
        this.driverDao = driverDao;
        this.rideRequestDao = rideRequestDao;
        this.zonePartitionService = new ZonePartitionService();
    }

    public FareQuote generateAndSaveQuote(Coordinate pickup, Coordinate destination) {
        PricingConfig config = pricingConfigDao.findActiveConfig()
                .orElseGet(PricingConfig::defaultConfig);

        int zoneId = zonePartitionService.findZoneId(pickup);
        Zone zone = zoneDao.findById(zoneId)
                .orElseGet(() -> zonePartitionService.getZoneById(zoneId));

        double trafficMultiplier = zone.getCurrentTrafficMultiplier();

        // Calculate supply and demand in this zone
        long queuedInZone = rideRequestDao.findQueuedRequests().stream()
                .filter(r -> r.getPickupZoneId() == zoneId).count();

        long availableInZone = driverDao.findAvailableDrivers().stream()
                .filter(d -> d.getCurrentZoneId() == zoneId && d.getStatus() == DriverStatus.AVAILABLE).count();

        FareQuote quote = pricingEngine.generateQuote(
                pickup,
                destination,
                zoneId,
                trafficMultiplier,
                (int) queuedInZone,
                (int) availableInZone,
                config
        );

        long quoteId = fareQuoteDao.save(quote);
        quote.setQuoteId(quoteId);
        return quote;
    }

    public PricingConfig getActiveConfig() {
        return pricingConfigDao.findActiveConfig().orElseGet(PricingConfig::defaultConfig);
    }

    public PricingConfig updateConfig(PricingConfig newConfig) {
        PricingConfig current = getActiveConfig();
        PricingConfig next = new PricingConfig(
                null,
                current.getVersion() + 1,
                newConfig.getBaseFare() != null ? newConfig.getBaseFare() : current.getBaseFare(),
                newConfig.getPerKmRate() != null ? newConfig.getPerKmRate() : current.getPerKmRate(),
                newConfig.getPerMinuteRate() != null ? newConfig.getPerMinuteRate() : current.getPerMinuteRate(),
                newConfig.getSurgeSensitivity() > 0 ? newConfig.getSurgeSensitivity() : current.getSurgeSensitivity(),
                newConfig.getMaxSurgeCap() >= 1.0 ? newConfig.getMaxSurgeCap() : current.getMaxSurgeCap(),
                true,
                java.time.LocalDateTime.now()
        );
        long id = pricingConfigDao.save(next);
        next.setConfigId((int) id);
        return next;
    }

    public List<Zone> getAllZones() {
        return zoneDao.findAll();
    }
}

package com.rideflow.core.pricing;

import com.rideflow.core.model.Coordinate;
import com.rideflow.core.model.FareQuote;
import com.rideflow.core.model.PricingConfig;
import com.rideflow.core.spatial.EuclideanRoadDistanceCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class PricingEngineTest {

    private PricingEngine pricingEngine;
    private PricingConfig config;

    @BeforeEach
    void setUp() {
        pricingEngine = new PricingEngine(new EuclideanRoadDistanceCalculator());
        config = PricingConfig.defaultConfig();
    }

    @Test
    @DisplayName("Normal demand produces 1.0x surge multiplier")
    void testNormalDemandSurge() {
        Coordinate pickup = Coordinate.of(10.0, 10.0);
        Coordinate dest = Coordinate.of(20.0, 20.0);

        // 1 queued request + incoming = 2 demand, 10 available drivers -> ratio = 0.2 < 1.0
        FareQuote quote = pricingEngine.generateQuote(pickup, dest, 1, 1.0, 1, 10, config);

        assertNotNull(quote);
        assertEquals(1.00, quote.getSurgeMultiplier());
        assertTrue(quote.getBaseTripFare().compareTo(quote.getFinalQuotedFare()) == 0);
    }

    @Test
    @DisplayName("High demand produces calculated surge multiplier matching formula")
    void testHighDemandSurgeCalculation() {
        Coordinate pickup = Coordinate.of(10.0, 10.0);
        Coordinate dest = Coordinate.of(20.0, 20.0);

        // Queued = 11 + 1 incoming = 12 demand, 3 available drivers -> ratio = 4.0
        // surge = 1 + 0.25 * (4 - 1) = 1 + 0.75 = 1.75
        FareQuote quote = pricingEngine.generateQuote(pickup, dest, 1, 1.0, 11, 3, config);

        assertEquals(1.75, quote.getSurgeMultiplier());
        assertEquals(4.00, quote.getDemandSupplyRatio());
        // finalFare = baseTripFare * 1.75
        BigDecimal expectedFinal = quote.getBaseTripFare().getAmount().multiply(BigDecimal.valueOf(1.75)).setScale(2, java.math.RoundingMode.HALF_UP);
        assertEquals(expectedFinal, quote.getFinalQuotedFare().getAmount());
    }

    @Test
    @DisplayName("Extreme demand caps surge at configured maximum 2.5x")
    void testMaxSurgeCap() {
        Coordinate pickup = Coordinate.of(10.0, 10.0);
        Coordinate dest = Coordinate.of(20.0, 20.0);

        // 50 requests, 1 driver -> ratio = 51 / 1 = 51 -> raw surge > 10.0
        FareQuote quote = pricingEngine.generateQuote(pickup, dest, 1, 1.0, 50, 1, config);

        assertEquals(2.50, quote.getSurgeMultiplier());
    }

    @Test
    @DisplayName("Zero available drivers safely handles division by zero via max(S, 1)")
    void testZeroAvailableDriversSafeDivision() {
        Coordinate pickup = Coordinate.of(10.0, 10.0);
        Coordinate dest = Coordinate.of(20.0, 20.0);

        assertDoesNotThrow(() -> {
            FareQuote quote = pricingEngine.generateQuote(pickup, dest, 1, 1.0, 5, 0, config);
            assertNotNull(quote);
            assertTrue(quote.getSurgeMultiplier() > 1.0);
        });
    }
}

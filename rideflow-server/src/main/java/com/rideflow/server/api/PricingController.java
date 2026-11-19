package com.rideflow.server.api;

import com.rideflow.core.model.Coordinate;
import com.rideflow.core.model.FareQuote;
import com.rideflow.core.model.PricingConfig;
import com.rideflow.core.model.Zone;
import com.rideflow.server.dto.FareQuoteRequestDto;
import com.rideflow.server.dto.FareQuoteResponseDto;
import com.rideflow.server.service.PricingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/pricing")
@Tag(name = "Dynamic Pricing", description = "Endpoints for locked fare quotations, surge multipliers, and zone configs")
public class PricingController {

    private final PricingService pricingService;

    public PricingController(PricingService pricingService) {
        this.pricingService = pricingService;
    }

    @PostMapping("/quote")
    @Operation(summary = "Calculate an immutable dynamic surge fare quotation")
    public ResponseEntity<FareQuoteResponseDto> calculateQuote(@Valid @RequestBody FareQuoteRequestDto dto) {
        Coordinate pickup = Coordinate.of(dto.pickupX(), dto.pickupY());
        Coordinate dest = Coordinate.of(dto.destinationX(), dto.destinationY());
        FareQuote quote = pricingService.generateAndSaveQuote(pickup, dest);
        return ResponseEntity.ok(FareQuoteResponseDto.from(quote));
    }

    @GetMapping("/zones")
    @Operation(summary = "List all 25 city zones and their current traffic factors")
    public List<Zone> getAllZones() {
        return pricingService.getAllZones();
    }

    @GetMapping("/config")
    @Operation(summary = "Get the active pricing configuration")
    public PricingConfig getPricingConfig() {
        return pricingService.getActiveConfig();
    }
}

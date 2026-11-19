package com.rideflow.server.api;

import com.rideflow.core.enums.DriverStatus;
import com.rideflow.core.model.Coordinate;
import com.rideflow.core.model.Driver;
import com.rideflow.core.spatial.ZonePartitionService;
import com.rideflow.server.dao.DriverDao;
import com.rideflow.server.dto.CreateDriverRequestDto;
import com.rideflow.server.dto.DriverResponseDto;
import com.rideflow.server.dto.UpdateDriverStatusDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/drivers")
@Tag(name = "Driver Operations", description = "Endpoints for driver registration, status management, and fleet discovery")
public class DriverController {

    private final DriverDao driverDao;
    private final ZonePartitionService zonePartitionService;

    public DriverController(DriverDao driverDao) {
        this.driverDao = driverDao;
        this.zonePartitionService = new ZonePartitionService();
    }

    @PostMapping
    @Operation(summary = "Register a new driver")
    public ResponseEntity<DriverResponseDto> registerDriver(@Valid @RequestBody CreateDriverRequestDto dto) {
        Coordinate pos = Coordinate.of(dto.initialX(), dto.initialY());
        int zoneId = zonePartitionService.findZoneId(pos);
        Driver driver = new Driver(null, dto.name(), dto.vehicleType(), dto.vehicleNumber(), pos, zoneId);
        long id = driverDao.save(driver);
        driver.setDriverId(id);
        return ResponseEntity.status(HttpStatus.CREATED).body(DriverResponseDto.from(driver));
    }

    @GetMapping
    @Operation(summary = "List all drivers")
    public List<DriverResponseDto> getAllDrivers() {
        return driverDao.findAll().stream().map(DriverResponseDto::from).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get driver by ID")
    public ResponseEntity<DriverResponseDto> getDriverById(@PathVariable long id) {
        return driverDao.findById(id)
                .map(d -> ResponseEntity.ok(DriverResponseDto.from(d)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/available")
    @Operation(summary = "List all currently available drivers")
    public List<DriverResponseDto> getAvailableDrivers() {
        return driverDao.findAvailableDrivers().stream().map(DriverResponseDto::from).toList();
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update driver status (e.g. go online/offline)")
    public ResponseEntity<DriverResponseDto> updateDriverStatus(@PathVariable long id,
                                                               @Valid @RequestBody UpdateDriverStatusDto dto) {
        Driver driver = driverDao.findById(id).orElse(null);
        if (driver == null) {
            return ResponseEntity.notFound().build();
        }
        if (dto.status() == DriverStatus.OFFLINE) {
            driver.goOffline();
        } else if (dto.status() == DriverStatus.AVAILABLE) {
            driver.goOnline(driver.getCurrentCoordinate(), driver.getCurrentZoneId());
        } else {
            driver.setStatus(dto.status());
        }
        driverDao.update(driver);
        return ResponseEntity.ok(DriverResponseDto.from(driver));
    }
}

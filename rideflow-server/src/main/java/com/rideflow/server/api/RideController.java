package com.rideflow.server.api;

import com.rideflow.core.model.Coordinate;
import com.rideflow.core.model.RideRequest;
import com.rideflow.server.dao.RideRequestDao;
import com.rideflow.server.dto.CreateRideRequestDto;
import com.rideflow.server.dto.RideResponseDto;
import com.rideflow.server.service.RideLifecycleService;
import com.rideflow.server.service.SimulationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/rides")
@Tag(name = "Ride Operations", description = "Endpoints for booking, queueing, inspecting, and cancelling rides")
public class RideController {

    private final RideLifecycleService rideLifecycleService;
    private final RideRequestDao rideRequestDao;
    private final SimulationService simulationService;

    public RideController(RideLifecycleService rideLifecycleService,
                          RideRequestDao rideRequestDao,
                          SimulationService simulationService) {
        this.rideLifecycleService = rideLifecycleService;
        this.rideRequestDao = rideRequestDao;
        this.simulationService = simulationService;
    }

    @PostMapping
    @Operation(summary = "Create and queue a new ride request")
    public ResponseEntity<RideResponseDto> createRide(@Valid @RequestBody CreateRideRequestDto dto) {
        Coordinate pickup = Coordinate.of(dto.pickupX(), dto.pickupY());
        Coordinate dest = Coordinate.of(dto.destinationX(), dto.destinationY());
        long runId = simulationService.getCurrentRun().getRunId();

        RideRequest request = rideLifecycleService.createAndQueueRide(dto.riderId(), pickup, dest, dto.requestType(), runId);
        return ResponseEntity.status(HttpStatus.CREATED).body(RideResponseDto.from(request));
    }

    @GetMapping
    @Operation(summary = "List all ride requests")
    public List<RideResponseDto> getAllRides() {
        return rideLifecycleService.getAllRides().stream().map(RideResponseDto::from).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get ride request by ID")
    public ResponseEntity<RideResponseDto> getRideById(@PathVariable long id) {
        return rideRequestDao.findById(id)
                .map(r -> ResponseEntity.ok(RideResponseDto.from(r)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel an active or queued ride request")
    public ResponseEntity<Void> cancelRide(@PathVariable long id) {
        long runId = simulationService.getCurrentRun().getRunId();
        rideLifecycleService.cancelRide(id, runId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/queued")
    @Operation(summary = "List currently queued rides waiting for driver dispatch")
    public List<RideResponseDto> getQueuedRides() {
        return rideLifecycleService.getQueuedRides().stream().map(RideResponseDto::from).toList();
    }

    @GetMapping("/active")
    @Operation(summary = "List all active trips (assigned or in progress)")
    public List<RideResponseDto> getActiveRides() {
        return rideLifecycleService.getActiveRides().stream().map(RideResponseDto::from).toList();
    }
}

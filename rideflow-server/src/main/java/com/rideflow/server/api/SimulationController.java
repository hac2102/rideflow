package com.rideflow.server.api;

import com.rideflow.core.model.SimulationEvent;
import com.rideflow.core.model.SimulationRun;
import com.rideflow.server.dao.SimulationEventDao;
import com.rideflow.server.dto.SimulationControlRequestDto;
import com.rideflow.server.dto.SimulationStateDto;
import com.rideflow.server.service.SimulationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/simulation")
@Tag(name = "Simulation Controls", description = "Endpoints for starting, pausing, stepping, and resetting the simulation clock")
public class SimulationController {

    private final SimulationService simulationService;
    private final SimulationEventDao simulationEventDao;

    public SimulationController(SimulationService simulationService,
                                SimulationEventDao simulationEventDao) {
        this.simulationService = simulationService;
        this.simulationEventDao = simulationEventDao;
    }

    @GetMapping("/state")
    @Operation(summary = "Get current simulation run state and clock ticks")
    public SimulationStateDto getSimulationState() {
        SimulationRun run = simulationService.getCurrentRun();
        return new SimulationStateDto(
                run.getRunId(),
                run.getScenarioName(),
                run.getCurrentTick(),
                simulationService.getClock().getCurrentSimulatedTime().toString(),
                run.getStatus(),
                run.getTotalRequests(),
                run.getCompletedRequests(),
                run.getCancelledRequests(),
                run.getExpiredRequests(),
                run.getTotalRevenue().toString()
        );
    }

    @PostMapping("/start")
    @Operation(summary = "Start the simulation engine")
    public ResponseEntity<Void> startSimulation() {
        simulationService.start();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/pause")
    @Operation(summary = "Pause the simulation engine")
    public ResponseEntity<Void> pauseSimulation() {
        simulationService.pause();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/resume")
    @Operation(summary = "Resume the simulation engine")
    public ResponseEntity<Void> resumeSimulation() {
        simulationService.resume();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/tick")
    @Operation(summary = "Manually advance the simulation by exactly one tick (1 simulated minute)")
    public ResponseEntity<SimulationStateDto> advanceTick() {
        simulationService.advanceOneTick();
        return ResponseEntity.ok(getSimulationState());
    }

    @PostMapping("/reset")
    @Operation(summary = "Reset the simulation engine with a specific scenario and seed")
    public ResponseEntity<SimulationStateDto> resetSimulation(@RequestBody(required = false) SimulationControlRequestDto dto) {
        String scenario = (dto != null && dto.scenarioName() != null) ? dto.scenarioName() : "NORMAL";
        long seed = (dto != null && dto.seed() != null) ? dto.seed() : 42L;
        simulationService.reset(scenario, seed);
        return ResponseEntity.ok(getSimulationState());
    }

    @GetMapping("/events")
    @Operation(summary = "Get chronological stream of latest simulation events")
    public List<SimulationEvent> getLatestEvents(@RequestParam(defaultValue = "50") int limit) {
        long runId = simulationService.getCurrentRun().getRunId();
        return simulationEventDao.findLatestByRunId(runId, limit);
    }
}

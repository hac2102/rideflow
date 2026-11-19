package com.rideflow.server.api;

import com.rideflow.core.dispatch.CandidateScore;
import com.rideflow.server.service.AnalyticsService;
import com.rideflow.server.service.DispatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/analytics")
@Tag(name = "Analytics & Benchmarks", description = "Endpoints for KPIs, zone heatmaps, candidate explainability, and CSV exports")
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final DispatchService dispatchService;

    public AnalyticsController(AnalyticsService analyticsService, DispatchService dispatchService) {
        this.analyticsService = analyticsService;
        this.dispatchService = dispatchService;
    }

    @GetMapping("/overview")
    @Operation(summary = "Get high-level operations KPIs and fleet metrics")
    public Map<String, Object> getOverview() {
        return analyticsService.getOverviewMetrics();
    }

    @GetMapping("/heatmap")
    @Operation(summary = "Get 5x5 zone surge heatmap data and demand/supply ratios")
    public List<Map<String, Object>> getHeatmap() {
        return analyticsService.getZoneHeatmap();
    }

    @GetMapping("/dispatch/{requestId}")
    @Operation(summary = "Explainable AI: inspect candidate driver scoring decisions for a specific ride")
    public List<CandidateScore> getCandidateEvaluation(@PathVariable long requestId) {
        return dispatchService.getCandidateEvaluation(requestId);
    }

    @GetMapping(value = "/export/csv", produces = "text/csv")
    @Operation(summary = "Export complete ride historical data as CSV")
    public ResponseEntity<String> exportCsv() {
        String csv = analyticsService.exportRidesToCsv();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"rideflow_rides_report.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }
}

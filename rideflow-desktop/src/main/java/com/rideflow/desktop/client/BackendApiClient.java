package com.rideflow.desktop.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Asynchronous, non-blocking HTTP REST client for communicating with the RideFlow Spring Boot server.
 * Never blocks the JavaFX Application Thread.
 */
public class BackendApiClient {

    private static final Logger log = LoggerFactory.getLogger(BackendApiClient.class);

    private final String baseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public BackendApiClient() {
        this("http://localhost:8080");
    }

    public BackendApiClient(String baseUrl) {
        this.baseUrl = baseUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    // Generic GET helper
    private <T> CompletableFuture<T> getAsync(String endpoint, TypeReference<T> typeRef) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + endpoint))
                .timeout(Duration.ofSeconds(5))
                .header("Accept", "application/json")
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() >= 200 && response.statusCode() < 300) {
                        try {
                            return objectMapper.readValue(response.body(), typeRef);
                        } catch (Exception e) {
                            throw new RuntimeException("JSON parsing error on " + endpoint, e);
                        }
                    } else {
                        throw new RuntimeException("API error " + response.statusCode() + " on " + endpoint + ": " + response.body());
                    }
                });
    }

    // Generic POST helper
    private <T> CompletableFuture<T> postAsync(String endpoint, Object bodyObj, TypeReference<T> typeRef) {
        try {
            String jsonBody = bodyObj != null ? objectMapper.writeValueAsString(bodyObj) : "";
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + endpoint))
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json");

            if (bodyObj != null) {
                builder.POST(HttpRequest.BodyPublishers.ofString(jsonBody));
            } else {
                builder.POST(HttpRequest.BodyPublishers.noBody());
            }

            return httpClient.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> {
                        if (response.statusCode() >= 200 && response.statusCode() < 300) {
                            try {
                                if (typeRef == null || response.body().isBlank()) {
                                    return null;
                                }
                                return objectMapper.readValue(response.body(), typeRef);
                            } catch (Exception e) {
                                throw new RuntimeException("JSON parsing error on " + endpoint, e);
                            }
                        } else {
                            throw new RuntimeException("API error " + response.statusCode() + " on " + endpoint + ": " + response.body());
                        }
                    });
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    // API Calls
    public CompletableFuture<Map<String, Object>> getOverviewAsync() {
        return getAsync("/api/v1/analytics/overview", new TypeReference<>() {});
    }

    public CompletableFuture<List<Map<String, Object>>> getHeatmapAsync() {
        return getAsync("/api/v1/analytics/heatmap", new TypeReference<>() {});
    }

    public CompletableFuture<List<Map<String, Object>>> getAllDriversAsync() {
        return getAsync("/api/v1/drivers", new TypeReference<>() {});
    }

    public CompletableFuture<List<Map<String, Object>>> getAllRidesAsync() {
        return getAsync("/api/v1/rides", new TypeReference<>() {});
    }

    public CompletableFuture<List<Map<String, Object>>> getQueuedRidesAsync() {
        return getAsync("/api/v1/rides/queued", new TypeReference<>() {});
    }

    public CompletableFuture<List<Map<String, Object>>> getActiveRidesAsync() {
        return getAsync("/api/v1/rides/active", new TypeReference<>() {});
    }

    public CompletableFuture<Map<String, Object>> getSimulationStateAsync() {
        return getAsync("/api/v1/simulation/state", new TypeReference<>() {});
    }

    public CompletableFuture<List<Map<String, Object>>> getSimulationEventsAsync(int limit) {
        return getAsync("/api/v1/simulation/events?limit=" + limit, new TypeReference<>() {});
    }

    public CompletableFuture<Map<String, Object>> triggerTickAsync() {
        return postAsync("/api/v1/simulation/tick", null, new TypeReference<>() {});
    }

    public CompletableFuture<Void> startSimulationAsync() {
        return postAsync("/api/v1/simulation/start", null, null);
    }

    public CompletableFuture<Void> pauseSimulationAsync() {
        return postAsync("/api/v1/simulation/pause", null, null);
    }

    public CompletableFuture<Void> resumeSimulationAsync() {
        return postAsync("/api/v1/simulation/resume", null, null);
    }

    public CompletableFuture<Map<String, Object>> resetSimulationAsync(String scenario, long seed) {
        Map<String, Object> req = Map.of("scenarioName", scenario, "seed", seed);
        return postAsync("/api/v1/simulation/reset", req, new TypeReference<>() {});
    }

    public CompletableFuture<Map<String, Object>> createRideAsync(long riderId, double px, double py, double dx, double dy, String type) {
        Map<String, Object> body = Map.of(
                "riderId", riderId,
                "pickupX", px,
                "pickupY", py,
                "destinationX", dx,
                "destinationY", dy,
                "requestType", type
        );
        return postAsync("/api/v1/rides", body, new TypeReference<>() {});
    }

    public CompletableFuture<Void> cancelRideAsync(long requestId) {
        return postAsync("/api/v1/rides/" + requestId + "/cancel", null, null);
    }

    public CompletableFuture<Map<String, Object>> registerDriverAsync(String name, String vehicleType, String vehicleNumber, double x, double y) {
        Map<String, Object> body = Map.of(
                "name", name,
                "vehicleType", vehicleType,
                "vehicleNumber", vehicleNumber,
                "initialX", x,
                "initialY", y
        );
        return postAsync("/api/v1/drivers", body, new TypeReference<>() {});
    }

    public CompletableFuture<Map<String, Object>> toggleDriverStatusAsync(long driverId, String status) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/v1/drivers/" + driverId + "/status"))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString("{\"status\":\"" + status + "\"}"))
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(resp -> {
                    try {
                        return objectMapper.readValue(resp.body(), new TypeReference<Map<String, Object>>() {});
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    public CompletableFuture<String> exportCsvAsync() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/v1/analytics/export/csv"))
                .header("Accept", "text/csv")
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body);
    }

    public CompletableFuture<List<Map<String, Object>>> getCandidateEvaluationAsync(long requestId) {
        return getAsync("/api/v1/analytics/dispatch/" + requestId, new TypeReference<>() {});
    }

    public CompletableFuture<Void> setSpeedAsync(double multiplier) {
        return postAsync("/api/v1/simulation/speed?multiplier=" + multiplier, null, null);
    }

    public CompletableFuture<Void> injectRushHourAsync(int zoneId, int count) {
        return postAsync("/api/v1/simulation/inject/rush-hour?zoneId=" + zoneId + "&count=" + count, null, null);
    }

    public CompletableFuture<Void> injectRainstormAsync() {
        return postAsync("/api/v1/simulation/inject/rainstorm", null, null);
    }

    public CompletableFuture<Void> mobilizeFleetAsync(int zoneId, int count) {
        return postAsync("/api/v1/simulation/inject/mobilize-fleet?zoneId=" + zoneId + "&count=" + count, null, null);
    }

    public CompletableFuture<Map<String, Object>> stepSimulationAsync(int ticks) {
        return postAsync("/api/v1/simulation/step?ticks=" + ticks, null, new TypeReference<>() {});
    }

    public CompletableFuture<String> exportDriversCsvAsync() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/v1/analytics/export/drivers"))
                .header("Accept", "text/csv")
                .GET()
                .build();
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(HttpResponse::body);
    }

    public CompletableFuture<String> exportZonesCsvAsync() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/v1/analytics/export/zones"))
                .header("Accept", "text/csv")
                .GET()
                .build();
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(HttpResponse::body);
    }

    public CompletableFuture<String> exportSummaryCsvAsync() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/v1/analytics/export/summary"))
                .header("Accept", "text/csv")
                .GET()
                .build();
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(HttpResponse::body);
    }

    public CompletableFuture<Map<String, Object>> getPricingConfigAsync() {
        return getAsync("/api/v1/pricing/config", new TypeReference<>() {});
    }

    public CompletableFuture<Map<String, Object>> updatePricingConfigAsync(Map<String, Object> config) {
        try {
            String jsonBody = objectMapper.writeValueAsString(config);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/v1/pricing/config"))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();
            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(resp -> {
                        try {
                            return objectMapper.readValue(resp.body(), new TypeReference<Map<String, Object>>() {});
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    });
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }
}

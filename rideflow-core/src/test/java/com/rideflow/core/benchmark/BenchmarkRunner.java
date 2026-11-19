package com.rideflow.core.benchmark;

import com.rideflow.core.dispatch.CandidateScore;
import com.rideflow.core.dispatch.DispatchStrategy;
import com.rideflow.core.dispatch.NearestDriverStrategy;
import com.rideflow.core.dispatch.WeightedFairDispatchStrategy;
import com.rideflow.core.enums.DriverStatus;
import com.rideflow.core.enums.RequestType;
import com.rideflow.core.enums.VehicleType;
import com.rideflow.core.model.Coordinate;
import com.rideflow.core.model.Driver;
import com.rideflow.core.model.RideRequest;
import com.rideflow.core.spatial.DistanceCalculator;
import com.rideflow.core.spatial.EuclideanRoadDistanceCalculator;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Performance benchmarking suite executing deterministic matching benchmarks:
 * - Workload 1: 100 drivers / 500 requests
 * - Workload 2: 1,000 drivers / 5,000 requests
 * - Workload 3: 5,000 drivers / 20,000 requests
 *
 * Compares NearestDriverStrategy (O(N) baseline) vs WeightedFairDispatchStrategy.
 */
public class BenchmarkRunner {

    private static final DistanceCalculator distanceCalculator = new EuclideanRoadDistanceCalculator();

    public static void main(String[] args) {
        System.out.println("==========================================================================");
        System.out.println("          RideFlow Algorithmic Dispatch Engine Benchmark Suite            ");
        System.out.println("==========================================================================");

        // JVM Warmup
        warmUpJvm();

        // Workload 1: 100 drivers, 500 requests
        runBenchmarkScenario("Workload 1", 100, 500, 42L);

        // Workload 2: 1,000 drivers, 5,000 requests
        runBenchmarkScenario("Workload 2", 1000, 5000, 42L);

        // Workload 3: 5,000 drivers, 20,000 requests
        runBenchmarkScenario("Workload 3", 5000, 20000, 42L);

        System.out.println("\nAll benchmarks completed successfully.");
    }

    private static void warmUpJvm() {
        System.out.print("Warming up JVM JIT compiler... ");
        List<Driver> drivers = generateDrivers(100, new Random(1L));
        List<RideRequest> requests = generateRequests(500, new Random(2L));
        NearestDriverStrategy nearest = new NearestDriverStrategy();
        for (RideRequest r : requests) {
            nearest.selectBestDriver(r, drivers, 1, distanceCalculator, 1.0);
        }
        System.out.println("Done.");
    }

    private static void runBenchmarkScenario(String name, int driverCount, int requestCount, long seed) {
        System.out.printf("\n--------------------------------------------------------------------------\n");
        System.out.printf("Scenario: %s | Fleet: %d drivers | Requests: %d | Seed: %d\n", name, driverCount, requestCount, seed);
        System.out.printf("--------------------------------------------------------------------------\n");

        Random rng = new Random(seed);
        List<Driver> drivers = generateDrivers(driverCount, rng);
        List<RideRequest> requests = generateRequests(requestCount, rng);

        // Strategy 1: Nearest Driver
        DispatchStrategy nearestStrategy = new NearestDriverStrategy();
        BenchmarkResult nearestResult = executeStrategyBenchmark(nearestStrategy, drivers, requests);
        System.out.printf("  [Baseline]  %-35s -> Time: %6d ms | Throughput: %8.0f req/s | Avg Dist: %.2f km\n",
                nearestStrategy.getName(), nearestResult.elapsedMs, nearestResult.throughputReqSec, nearestResult.avgPickupDistanceKm);

        // Strategy 2: Weighted Fair Dispatch
        DispatchStrategy fairStrategy = new WeightedFairDispatchStrategy();
        BenchmarkResult fairResult = executeStrategyBenchmark(fairStrategy, drivers, requests);
        System.out.printf("  [Optimized] %-35s -> Time: %6d ms | Throughput: %8.0f req/s | Avg Dist: %.2f km\n",
                fairStrategy.getName(), fairResult.elapsedMs, fairResult.throughputReqSec, fairResult.avgPickupDistanceKm);
    }

    private static BenchmarkResult executeStrategyBenchmark(DispatchStrategy strategy, List<Driver> drivers, List<RideRequest> requests) {
        long startNanos = System.nanoTime();
        double totalDistance = 0.0;
        int matched = 0;

        for (RideRequest r : requests) {
            Optional<CandidateScore> best = strategy.selectBestDriver(r, drivers, 1, distanceCalculator, 1.0);
            if (best.isPresent()) {
                totalDistance += best.get().getPickupDistanceKm();
                matched++;
            }
        }

        long elapsedNanos = System.nanoTime() - startNanos;
        long elapsedMs = elapsedNanos / 1_000_000L;
        double throughput = (matched > 0 && elapsedNanos > 0) ? ((double) matched / (elapsedNanos / 1_000_000_000.0)) : 0.0;
        double avgDistance = matched > 0 ? (totalDistance / matched) : 0.0;

        return new BenchmarkResult(elapsedMs, throughput, avgDistance);
    }

    private static List<Driver> generateDrivers(int count, Random rng) {
        List<Driver> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            double x = rng.nextDouble() * 100.0;
            double y = rng.nextDouble() * 100.0;
            Driver d = new Driver((long) (i + 1), "Driver " + (i + 1), VehicleType.SEDAN, "DL-" + i, Coordinate.of(x, y), 13);
            d.setLastAvailableAt(LocalDateTime.now().minusMinutes(rng.nextInt(60)));
            list.add(d);
        }
        return list;
    }

    private static List<RideRequest> generateRequests(int count, Random rng) {
        List<RideRequest> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            double px = rng.nextDouble() * 100.0;
            double py = rng.nextDouble() * 100.0;
            double dx = rng.nextDouble() * 100.0;
            double dy = rng.nextDouble() * 100.0;
            RideRequest r = new RideRequest();
            r.setRequestId((long) (i + 1));
            r.setRiderId((long) ((i % 50) + 1));
            r.setPickupCoordinate(Coordinate.of(px, py));
            r.setDestinationCoordinate(Coordinate.of(dx, dy));
            r.setRequestType(rng.nextBoolean() ? RequestType.PRIORITY : RequestType.STANDARD);
            r.setRequestedAt(LocalDateTime.now().minusMinutes(rng.nextInt(20)));
            list.add(r);
        }
        return list;
    }

    private record BenchmarkResult(long elapsedMs, double throughputReqSec, double avgPickupDistanceKm) {}
}

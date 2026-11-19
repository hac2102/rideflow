# RideFlow — Testing & Verification Specification

RideFlow features comprehensive test suites spanning unit tests, integration tests, concurrency stress tests, and algorithmic benchmarks.

---

## 1. Running All Tests

Execute from the project root:
```cmd
.\scripts\test_all.cmd
```
Or via Maven:
```cmd
.\mvnw.cmd test
```

---

## 2. Test Suite Breakdown

### Core Domain & Algorithm Tests (`rideflow-core`)
- `CoordinateTest`: Validates boundary constraints (0–100), invalid coordinate exceptions, and Euclidean distance formulas.
- `DriverStateMachineTest`: Asserts legal transitions (`AVAILABLE` $\to$ `RESERVED` $\to$ `ON_TRIP` $\to$ `AVAILABLE`) and catches illegal states (`ON_TRIP` $\to$ `OFFLINE`).
- `PricingEngineTest`: Verifies base fare calculation, surge scaling, maximum surge clamp ($2.5\times$), and the exact Section 3.6 report case ($\text{₹}264.25$).
- `DispatchStrategyTest`: Tests proximity sorting, driver rating weights, idle starvation credits, and deterministic tie-breaking.
- `RideRequestComparatorTest`: Verifies PriorityQueue ordering and priority-aging calculations.

### Server Integration & Concurrency Tests (`rideflow-server`)
- `DriverDaoIntegrationTest`: Tests transactional database persistence, schema validation, PreparedStatement mapping, and optimistic version incrementing under raw JDBC.
- `ConcurrentDispatchTest`: Multi-threaded race condition test. Ten competing concurrent threads attempt to assign ten separate ride requests to a single available driver. Asserts that exactly one assignment succeeds and 9 fail with safe rollback, preserving database invariants.

---

## 3. Algorithmic Benchmarks (`rideflow-core`)

Run the benchmark suite:
```cmd
cd rideflow-core
..\mvnw.cmd test -Dtest=BenchmarkRunner
```
Evaluates throughput for 100, 1,000, and 5,000 drivers across 500 to 20,000 ride requests.

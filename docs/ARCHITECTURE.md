# RideFlow — System Architecture & Design Specification

## 1. Architectural Philosophy & Constraints

RideFlow is designed as a modular enterprise transportation operations simulator. It models how modern ride-hailing platforms (e.g., Uber, Lyft) dispatch vehicles, manage fleet availability, calculate dynamic surge prices, and ensure transactional consistency under high concurrency.

### Core Architectural Decisions:
1. **Separation of Concerns (Modular Monolith):**
   - `rideflow-core`: Pure Java domain invariants, mathematics, and algorithms. **Zero framework dependencies** (no Spring, no JDBC, no UI).
   - `rideflow-server`: Spring Boot REST services, JDBC persistence layer, connection pool management, and scheduled simulation clock.
   - `rideflow-desktop`: JavaFX 21 presentation tier communicating exclusively via non-blocking asynchronous REST HTTP (`BackendApiClient`).
2. **Persistence: Raw JDBC over JPA/Hibernate:**
   - Enforces explicit SQL query optimization, deterministic transaction boundaries, and direct composite index usage.
   - Eliminates ORM caching anomalies, lazy loading exceptions, and hidden N+1 queries.
3. **Optimistic Locking & Atomic Conditional Updates:**
   - Driver reservation uses `UPDATE drivers SET status = 'RESERVED', version = version + 1 WHERE driver_id = ? AND status = 'AVAILABLE';` to guarantee absolute immunity from double booking.

---

## 2. Multi-Module Structural Breakdown

```
rideflow/
├── rideflow-core/        --> Domain entities, value objects, PriorityQueue comparator, pricing math
├── rideflow-server/      --> Spring Boot 3.3.5, Raw JDBC DAOs, Flyway migrations, REST APIs
└── rideflow-desktop/     --> JavaFX 21, fixed sidebar, 9 operations screens, CityCanvasView
```

### Dependency Flow:
```
JavaFX Client ---> Asynchronous HTTP/JSON ---> Spring Boot REST Controller ---> Application Service ---> Domain Logic / Algorithmic Engine ---> Raw JDBC DAO ---> Relational Database
```

---

## 3. Mathematical & Algorithmic Formulations

### 3.1 Weighted Fair Dispatch Strategy
Given an unassigned ride request $R$, all available drivers $D_i$ within the maximum allowable pickup radius ($R_{\text{max}} = 10\text{ km}$) are scored:

$$\text{Score}(D_i) = \text{ETA}_{\text{minutes}}(D_i, R) - \text{FairnessCredit}(D_i)$$

Where:
$$\text{FairnessCredit}(D_i) = \min(\text{IdleMinutes}(D_i), 30) \times 0.05$$

The driver with the lowest composite score wins the assignment. In case of a tie, the lower `driverId` breaks the tie deterministically.

### 3.2 Dynamic Surge Pricing Formula
The city is partitioned into a $5 \times 5$ grid of 25 zones. In any zone $k$:
- $D_k$: Queued requests originating in zone $k$ (including incoming request).
- $S_k$: Available drivers currently positioned inside zone $k$.
- Demand-to-Supply Ratio: $R = \frac{D_k}{\max(S_k, 1)}$
- Surge Multiplier:
  $$M = \text{clamp}\left(1.0 + 0.25 \times \max(0, R - 1), 1.0, 2.5\right)$$
- Base Fare:
  $$\text{Base} = \text{₹}40.00 + (\text{₹}12.00/\text{km} \times \text{Distance}) + (\text{₹}2.00/\text{min} \times \text{Duration})$$
- Final Quoted Fare:
  $$\text{Final} = \text{Base} \times M$$

---

## 4. Concurrency & Transaction Management

A major threat to transport platforms is the double-booking anomaly. RideFlow eliminates this using atomic SQL conditional updates within a database transaction boundary (`@Transactional(isolation = Isolation.READ_COMMITTED)`):

```java
int updated = driverDao.updateStatusIfAvailable(driverId, DriverStatus.RESERVED);
if (updated == 0) {
    throw new DoubleBookingException("Driver #" + driverId + " was claimed by another thread.");
}
```
If two riders concurrently match with the same driver, exactly one transaction succeeds; the other transaction fails, triggers an automatic rollback, and keeps the competing ride safely in the priority queue for subsequent matching.

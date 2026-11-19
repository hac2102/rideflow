# CS553 Enterprise Web Application Coursework Traceability Matrix
## RideFlow: Intelligent Ride Dispatch, Fleet Management & Dynamic Pricing Simulator

This document provides a syllabus-to-implementation traceability matrix demonstrating compliance with all 5 units of the **CS553 Enterprise Web Application** curriculum.

---

### Unit 1: Java Foundations, OOP & Design Principles

| Curriculum Topic | Implementation File(s) | Concrete Working Feature in RideFlow |
| :--- | :--- | :--- |
| **Classes, Objects & Constructors** | `Driver.java`, `RideRequest.java`, `Zone.java` | Clean encapsulation with private state, overloaded constructors for fresh domain models and DAO hydration. |
| **Value Objects & Immutability** | `Coordinate.java`, `Money.java`, `Distance.java` | Strict immutability preventing coordinate mutation and floating-point financial corruption. |
| **Polymorphism & Strategy Pattern** | `DispatchStrategy.java`, `NearestDriverStrategy.java`, `WeightedFairDispatchStrategy.java` | Pluggable dispatch algorithms interchangeable via strategy interface. |
| **Encapsulation & State Machines** | `Driver.java` (`reserve()`, `startTrip()`, `completeTrip()`), `RideRequest.java` | State machines throwing checked exceptions on invalid transitions (`OFFLINE` to `RESERVED` forbidden). |
| **Packages & Clean Modular Design** | `com.rideflow.core.*`, `com.rideflow.server.*`, `com.rideflow.desktop.*` | Maven 3-tier multi-module architecture (`core`, `server`, `desktop`) with zero circular dependencies. |

---

### Unit 2: Collections Framework, Generics & Exception Handling

| Curriculum Topic | Implementation File(s) | Concrete Working Feature in RideFlow |
| :--- | :--- | :--- |
| **PriorityQueue & Custom Comparator** | `RideRequestComparator.java`, `RidePriorityQueue.java` | Custom comparator ordering requests by priority weight, aging waiting minutes, timestamp, and ID tie-breaker. |
| **Aging & Starvation Prevention** | `RidePriorityQueue.java` (`reprioritize()`) | Dynamic queue rebuilding at simulation ticks ensuring standard requests waiting 15+ mins outrank newly arrived priority requests. |
| **HashMap & Concurrent Collections** | `ZonePartitionService.java`, `DashboardController.java` | `ConcurrentHashMap` for active trip tracking and zone indexing; thread-safe state snapshots. |
| **Generics & Optional API** | `DriverDao.java`, `DispatchStrategy.java` (`Optional<CandidateScore>`) | Type-safe generic repository interfaces and non-null query return contracts. |
| **Custom Domain Exceptions** | `InvalidRideStateException.java`, `DriverUnavailableException.java`, `DoubleBookingException.java` | Granular error translation mapped to HTTP status codes (`400`, `404`, `409 Conflict`). |

---

### Unit 3: File Handling, Configuration & Logging

| Curriculum Topic | Implementation File(s) | Concrete Working Feature in RideFlow |
| :--- | :--- | :--- |
| **Java I/O & CSV Export** | `AnalyticsService.java` (`exportRidesToCsv()`), `DashboardController.java` | Standard Java I/O (`StringWriter`, `FileWriter`, `FileChooser`) exporting ride history audit logs to disk. |
| **Configuration (YAML & Properties)** | `application.yml`, `docker-compose.yml` | Hierarchical configuration separating H2 embedded dev profile from production MySQL 8 profile. |
| **Logging Framework** | `SLF4J` with `Logback` throughout backend & desktop | Structured log levels (`INFO`, `WARN`, `DEBUG`, `ERROR`) logging dispatch decisions and transactional events. |
| **Database Migrations** | `V1__initial_schema.sql`, `V2__seed_data.sql` (Flyway) | Scripted database versioning creating tables and seeding initial drivers across 25 zones. |

---

### Unit 4: JDBC & Persistence Management

| Curriculum Topic | Implementation File(s) | Concrete Working Feature in RideFlow |
| :--- | :--- | :--- |
| **Connection Pooling** | `HikariCP` configured in `application.yml` | Production connection pooling managing connections across concurrent worker threads. |
| **PreparedStatement & ResultSet** | `JdbcDriverDao.java`, `JdbcRideRequestDao.java` | 100% raw JDBC persistence; parameterized SQL preventing injection attacks; zero ORM/JPA abstractions. |
| **Transaction Management & Rollback** | `DispatchService.java` (`atomicAssignRide()`) | Manual JDBC transactions (`conn.setAutoCommit(false)`, `conn.commit()`, `conn.rollback()`). |
| **Anti-Double-Booking Concurrency** | `DispatchService.java`, `ConcurrentDispatchTest.java` | Conditional SQL update (`UPDATE drivers SET status = 'RESERVED' WHERE driver_id = ? AND status = 'AVAILABLE'`) with rollback on contention. |
| **Database Invariants & Indexing** | `V1__initial_schema.sql` | Composite indexes on `(status, current_zone_id)`, foreign key integrity, and DECIMAL financial fields. |

---

### Unit 5: JavaFX Desktop & GUI Engineering

| Curriculum Topic | Implementation File(s) | Concrete Working Feature in RideFlow |
| :--- | :--- | :--- |
| **Application Lifecycle & Stage/Scene** | `RideFlowDesktopApp.java` | Full desktop lifecycle implementation extending `javafx.application.Application`. |
| **Vector Canvas Animation** | `CityCanvasView.java` | Custom `Canvas` rendering 100x100 city grid, moving driver coordinates, trip vectors, and surge tints. |
| **Layout Panes & Controls** | `DashboardController.java` (`BorderPane`, `VBox`, `HBox`, `GridPane`, `TabPane`) | 6 full-featured operations screens including KPI cards, TableViews, and Heatmaps. |
| **Asynchronous Network Threading** | `BackendApiClient.java` (`java.net.http.HttpClient`) | Non-blocking asynchronous HTTP requests with `Platform.runLater()` ensuring the UI thread never freezes. |
| **Interactive Event Handling & Inspect** | `CityCanvasView.java` (`setOnMouseClicked()`), `DashboardController.java` | Click detection selecting canvas vehicles/rides and displaying properties in the inspector panel. |

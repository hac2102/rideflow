# RideFlow — Complete University Demonstration Walkthrough

This guide provides a script to demonstrate RideFlow during an academic viva, presentation, or recruiter technical interview.

---

## 30-Second Elevator Pitch
> *"RideFlow is a miniature ride-hailing operations simulator modeled after Uber's backend architecture. Built in pure Java 21 and Spring Boot with raw JDBC and JavaFX, it solves three core problems: multi-factor driver-rider matching using priority queues, dynamic surge pricing across 25 spatial zones, and double-booking concurrency prevention through atomic SQL transactions."*

---

## Demonstration Scenarios

### Demo A — Normal Dispatch & Visual Kinematics
1. Launch the platform using `scripts\run_all.cmd`.
2. Look at **Screen 1 (Operations Overview)**:
   - Notice the simulation clock ticking automatically at 1 tick/second.
   - Point out that vehicles (green dots) and ride requests (amber diamonds) are visible on the city canvas.
3. Switch to **Screen 2 (Live City)**:
   - Point out glowing route vectors connecting pickups to destinations.
   - Zoom in/out using the `[+ Zoom In]` and `[- Zoom Out]` controls.
   - Click on any vehicle to show the **Selected Entity Details** inspector (name, vehicle type, rating, completed trips, earnings).
   - Click on any zone to inspect that zone's live supply, demand, and traffic.

### Demo B — Dynamic Surge Pricing (Exact Section 3.6 Math)
1. Switch to **Screen 4 (Surge Pricing Monitor)**:
   - Show the 5x5 zone heatmap. Point out that zones with high demand turn yellow/orange/red.
   - Click **`Verify Section 3.6 Example (D=12, S=3)`**.
   - Show the modal dialog verifying the exact numbers from the academic report:
     - Ratio: $12 / 3 = 4.00$
     - Surge: $1.75\times$
     - Base Fare: $\text{₹}151.00$
     - Quoted Fare: $\text{₹}264.25$

### Demo C — 1-Click Interactive Shock Scenarios
1. On **Screen 2 (Live City)**, look at the bottom-right panel:
   - Click **`⚡ Rush Hour Shock (CBD Zone 13)`**.
   - **Observable Effect:** 12 ride requests appear immediately in Zone 13. The zone turns orange/red, the surge multiplier spikes to $2.20\times$, and available drivers reserve and turn orange as they steer toward the CBD.
2. Click **`🚗 Mobilize 10 Reserve Drivers`**:
   - **Observable Effect:** 10 new drivers appear in Zone 13. As supply increases, the surge rapidly cools back down toward $1.00\times$.

### Demo D — Explainable AI Candidate Evaluation
1. Switch to **Screen 3 (Dispatch Center)**:
   - Select any queued ride in the table.
   - Read the **Candidate Evaluation Scoring Breakdown** box:
     - Shows eligible drivers within the 10 km radius.
     - Displays pickup ETA, idle starvation credit, and final composite score.
     - Explains why the winning driver was chosen.

### Demo E — Concurrency & Double-Booking Prevention
1. Explain how `DispatchService.java` executes conditional updates:
   ```sql
   UPDATE drivers SET status = 'RESERVED', version = version + 1
   WHERE driver_id = ? AND status = 'AVAILABLE';
   ```
2. Mention that the integration test `ConcurrentDispatchTest` executes 10 concurrent threads against a single available driver, proving that exactly 1 thread claims the reservation while 9 threads roll back safely.

### Demo F — Persistence & Data Export
1. Switch to **Screen 7 (Analytics & Reports)**:
   - Click **`📥 Export Ride History (CSV)`** to save ride dispatch history to a local CSV file.
   - Point out the Benchmark Throughput Table showing sub-linear scaling across 100, 1,000, and 5,000 drivers.
2. Switch to **Screen 8 (Event Logs)**:
   - Search for specific events (e.g. `DRIVER_ASSIGNED` or `RIDE_COMPLETED`).
   - Click an event to inspect its structured JSON payload.

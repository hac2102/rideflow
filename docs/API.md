# RideFlow — Complete REST API Reference

All endpoints return JSON responses. Errors return a standardized `ErrorResponseDto`.

---

## 1. Simulation Controls (`/api/v1/simulation`)

### `GET /api/v1/simulation/state`
Returns the current simulation clock, run ID, scenario name, and state.
```json
{
  "runId": 1,
  "scenarioName": "NORMAL_DEMAND",
  "currentTick": 42,
  "simulatedTime": "2026-11-19T08:42:00",
  "status": "RUNNING",
  "totalRequests": 18,
  "completedRequests": 12,
  "cancelledRequests": 0,
  "expiredRequests": 1,
  "totalRevenue": "2850.50"
}
```

### `POST /api/v1/simulation/start`
Starts autonomous continuous ticking (default: 1 tick/sec).

### `POST /api/v1/simulation/pause`
Pauses the simulation clock.

### `POST /api/v1/simulation/resume`
Resumes continuous ticking.

### `POST /api/v1/simulation/step?ticks=1`
Advances the clock by $N$ ticks (minutes).

### `POST /api/v1/simulation/speed?multiplier=2.0`
Sets time dilation speed (e.g. 0.5x, 1x, 2x, 5x).

### `POST /api/v1/simulation/reset`
Resets the simulation run with a specified scenario and seed.
```json
{
  "scenarioName": "RUSH_HOUR",
  "seed": 42
}
```

### `POST /api/v1/simulation/inject/rush-hour?zoneId=13&count=12`
Injects an immediate demand spike into the specified zone.

### `POST /api/v1/simulation/inject/rainstorm`
Injects city-wide demand shock across all 25 zones.

### `POST /api/v1/simulation/inject/mobilize-fleet?zoneId=13&count=10`
Deploys available reserve vehicles to cool down high surge.

### `GET /api/v1/simulation/events?limit=50`
Returns chronological simulation events.

---

## 2. Driver Fleet Management (`/api/v1/drivers`)

### `GET /api/v1/drivers`
Returns all registered drivers in the fleet.

### `POST /api/v1/drivers`
Registers a new driver.
```json
{
  "name": "Ravi Shankar",
  "vehicleType": "SEDAN",
  "vehicleNumber": "KA-05-RS-1024",
  "initialX": 50.0,
  "initialY": 50.0
}
```

### `PATCH /api/v1/drivers/{id}/status`
Toggles driver availability (`AVAILABLE` or `OFFLINE`).

---

## 3. Ride Requests & Dispatch (`/api/v1/rides`)

### `POST /api/v1/rides`
Creates and queues a simulated ride booking.
```json
{
  "riderId": 1,
  "pickupX": 25.0,
  "pickupY": 30.0,
  "destinationX": 60.0,
  "destinationY": 70.0,
  "requestType": "STANDARD"
}
```

### `GET /api/v1/rides/queued`
Returns all rides waiting in the priority queue.

### `GET /api/v1/rides/active`
Returns all active trips currently in transit.

### `POST /api/v1/rides/{id}/cancel`
Cancels a queued ride request.

---

## 4. Analytics & Explainability (`/api/v1/analytics`)

### `GET /api/v1/analytics/overview`
Returns high-level operations KPIs.

### `GET /api/v1/analytics/heatmap`
Returns 5x5 zone surge heatmap data and demand/supply ratios.

### `GET /api/v1/analytics/dispatch/{requestId}`
Explainable AI: returns scored candidate evaluation for a specific ride.

### `GET /api/v1/analytics/export/csv`
Exports complete ride history as CSV.

### `GET /api/v1/analytics/export/drivers`
Exports driver operational summary as CSV.

### `GET /api/v1/analytics/export/zones`
Exports zone statistics as CSV.

---

## 5. Dynamic Pricing (`/api/v1/pricing`)

### `GET /api/v1/pricing/config`
Retrieves active pricing configuration.

### `PUT /api/v1/pricing/config`
Updates and versions the pricing configuration.

---

## 6. Standardized Error Format

```json
{
  "code": "INVALID_COORDINATE",
  "message": "Coordinate X must be between 0.0 and 100.0, got: 105.5",
  "timestamp": "2026-11-19T20:00:00Z",
  "path": "/api/v1/rides"
}
```
Supported domain error codes:
- `INVALID_COORDINATE` (HTTP 400)
- `INVALID_RIDE_STATE` (HTTP 400)
- `DRIVER_UNAVAILABLE` (HTTP 400)
- `RIDE_NOT_FOUND` (HTTP 404)
- `DOUBLE_BOOKING_PREVENTED` (HTTP 409)
- `PRICING_CONFIG_ERROR` (HTTP 400)

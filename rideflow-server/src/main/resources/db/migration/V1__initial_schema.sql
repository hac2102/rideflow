-- ============================================================================
-- V1__initial_schema.sql
-- RideFlow Database Schema
-- Compatible with MySQL 8.x and H2 (MySQL Mode)
-- ============================================================================

CREATE TABLE IF NOT EXISTS zones (
    zone_id INT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    min_x DECIMAL(5,2) NOT NULL,
    max_x DECIMAL(5,2) NOT NULL,
    min_y DECIMAL(5,2) NOT NULL,
    max_y DECIMAL(5,2) NOT NULL,
    current_traffic_multiplier DECIMAL(3,2) NOT NULL DEFAULT 1.00,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS drivers (
    driver_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    vehicle_type VARCHAR(20) NOT NULL,
    vehicle_number VARCHAR(30) NOT NULL UNIQUE,
    current_x DECIMAL(5,2) NOT NULL,
    current_y DECIMAL(5,2) NOT NULL,
    current_zone_id INT NOT NULL,
    status VARCHAR(20) NOT NULL,
    rating DECIMAL(3,2) NOT NULL DEFAULT 5.00,
    total_trips_completed INT NOT NULL DEFAULT 0,
    total_earnings DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    last_available_at TIMESTAMP NULL,
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_drivers_zone FOREIGN KEY (current_zone_id) REFERENCES zones(zone_id)
);

CREATE INDEX IF NOT EXISTS idx_drivers_status_zone ON drivers(status, current_zone_id);

CREATE TABLE IF NOT EXISTS riders (
    rider_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS pricing_config (
    config_id INT AUTO_INCREMENT PRIMARY KEY,
    version INT NOT NULL UNIQUE,
    base_fare DECIMAL(10,2) NOT NULL DEFAULT 40.00,
    per_km_rate DECIMAL(10,2) NOT NULL DEFAULT 12.00,
    per_minute_rate DECIMAL(10,2) NOT NULL DEFAULT 2.00,
    surge_sensitivity DECIMAL(3,2) NOT NULL DEFAULT 0.25,
    max_surge_cap DECIMAL(3,2) NOT NULL DEFAULT 2.50,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS fare_quotes (
    quote_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    zone_id INT NOT NULL,
    pricing_config_version INT NOT NULL,
    distance_km DECIMAL(6,2) NOT NULL,
    estimated_duration_min INT NOT NULL,
    base_trip_fare DECIMAL(10,2) NOT NULL,
    demand_supply_ratio DECIMAL(5,2) NOT NULL,
    surge_multiplier DECIMAL(3,2) NOT NULL,
    final_quoted_fare DECIMAL(10,2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_quotes_zone FOREIGN KEY (zone_id) REFERENCES zones(zone_id)
);

CREATE TABLE IF NOT EXISTS ride_requests (
    request_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rider_id BIGINT NOT NULL,
    quote_id BIGINT NOT NULL,
    pickup_x DECIMAL(5,2) NOT NULL,
    pickup_y DECIMAL(5,2) NOT NULL,
    pickup_zone_id INT NOT NULL,
    destination_x DECIMAL(5,2) NOT NULL,
    destination_y DECIMAL(5,2) NOT NULL,
    destination_zone_id INT NOT NULL,
    request_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    priority_score INT NOT NULL DEFAULT 0,
    requested_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    assigned_at TIMESTAMP NULL,
    started_at TIMESTAMP NULL,
    completed_at TIMESTAMP NULL,
    cancelled_at TIMESTAMP NULL,
    assigned_driver_id BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_rides_rider FOREIGN KEY (rider_id) REFERENCES riders(rider_id),
    CONSTRAINT fk_rides_quote FOREIGN KEY (quote_id) REFERENCES fare_quotes(quote_id),
    CONSTRAINT fk_rides_pzone FOREIGN KEY (pickup_zone_id) REFERENCES zones(zone_id),
    CONSTRAINT fk_rides_dzone FOREIGN KEY (destination_zone_id) REFERENCES zones(zone_id)
);

CREATE INDEX IF NOT EXISTS idx_rides_status ON ride_requests(status);

CREATE TABLE IF NOT EXISTS ride_assignments (
    assignment_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    request_id BIGINT NOT NULL,
    driver_id BIGINT NOT NULL,
    assigned_tick BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_assign_request FOREIGN KEY (request_id) REFERENCES ride_requests(request_id),
    CONSTRAINT fk_assign_driver FOREIGN KEY (driver_id) REFERENCES drivers(driver_id)
);

CREATE INDEX IF NOT EXISTS idx_assign_driver_status ON ride_assignments(driver_id, status);

CREATE TABLE IF NOT EXISTS simulation_runs (
    run_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    scenario_name VARCHAR(50) NOT NULL,
    random_seed BIGINT NOT NULL,
    current_tick BIGINT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL,
    total_requests INT NOT NULL DEFAULT 0,
    completed_requests INT NOT NULL DEFAULT 0,
    cancelled_requests INT NOT NULL DEFAULT 0,
    expired_requests INT NOT NULL DEFAULT 0,
    total_revenue DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ended_at TIMESTAMP NULL
);

CREATE TABLE IF NOT EXISTS simulation_events (
    event_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    run_id BIGINT NOT NULL,
    simulation_tick BIGINT NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    entity_id BIGINT NOT NULL,
    payload_json TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_events_run_tick ON simulation_events(run_id, simulation_tick);

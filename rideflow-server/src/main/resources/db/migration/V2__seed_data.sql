-- ============================================================================
-- V2__seed_data.sql
-- Seed initial zones (5x5 matrix, 25 zones), default pricing config, riders, and drivers
-- ============================================================================

-- 1. SEED 25 ZONES (5x5 Grid covering [0, 100] x [0, 100])
INSERT INTO zones (zone_id, name, min_x, max_x, min_y, max_y, current_traffic_multiplier) VALUES
(1,  'Zone 1 [R1-C1]',  0.0,  20.0,  0.0,  20.0, 1.00),
(2,  'Zone 2 [R1-C2]', 20.0,  40.0,  0.0,  20.0, 1.00),
(3,  'Zone 3 [R1-C3]', 40.0,  60.0,  0.0,  20.0, 1.00),
(4,  'Zone 4 [R1-C4]', 60.0,  80.0,  0.0,  20.0, 1.00),
(5,  'Zone 5 [R1-C5]', 80.0, 100.0,  0.0,  20.0, 1.00),

(6,  'Zone 6 [R2-C1]',  0.0,  20.0, 20.0,  40.0, 1.00),
(7,  'Zone 7 [R2-C2]', 20.0,  40.0, 20.0,  40.0, 1.00),
(8,  'Zone 8 [R2-C3]', 40.0,  60.0, 20.0,  40.0, 1.30),
(9,  'Zone 9 [R2-C4]', 60.0,  80.0, 20.0,  40.0, 1.00),
(10, 'Zone 10 [R2-C5]', 80.0, 100.0, 20.0,  40.0, 1.00),

(11, 'Zone 11 [R3-C1]',  0.0,  20.0, 40.0,  60.0, 1.00),
(12, 'Zone 12 [R3-C2]', 20.0,  40.0, 40.0,  60.0, 1.30),
(13, 'Zone 13 [Central Downtown]', 40.0,  60.0, 40.0,  60.0, 1.80),
(14, 'Zone 14 [R3-C4]', 60.0,  80.0, 40.0,  60.0, 1.30),
(15, 'Zone 15 [R3-C5]', 80.0, 100.0, 40.0,  60.0, 1.00),

(16, 'Zone 16 [R4-C1]',  0.0,  20.0, 60.0,  80.0, 1.00),
(17, 'Zone 17 [R4-C2]', 20.0,  40.0, 60.0,  80.0, 1.00),
(18, 'Zone 18 [R4-C3]', 40.0,  60.0, 60.0,  80.0, 1.30),
(19, 'Zone 19 [R4-C4]', 60.0,  80.0, 60.0,  80.0, 1.00),
(20, 'Zone 20 [R4-C5]', 80.0, 100.0, 60.0,  80.0, 1.00),

(21, 'Zone 21 [R5-C1]',  0.0,  20.0, 80.0, 100.0, 1.00),
(22, 'Zone 22 [R5-C2]', 20.0,  40.0, 80.0, 100.0, 1.00),
(23, 'Zone 23 [R5-C3]', 40.0,  60.0, 80.0, 100.0, 1.00),
(24, 'Zone 24 [R5-C4]', 60.0,  80.0, 80.0, 100.0, 1.00),
(25, 'Zone 25 [R5-C5]', 80.0, 100.0, 80.0, 100.0, 1.00);

-- 2. SEED DEFAULT PRICING CONFIG
INSERT INTO pricing_config (version, base_fare, per_km_rate, per_minute_rate, surge_sensitivity, max_surge_cap, active)
VALUES (1, 40.00, 12.00, 2.00, 0.25, 2.50, TRUE);

-- 3. SEED INITIAL RIDERS
INSERT INTO riders (rider_id, name, phone) VALUES
(1, 'Aarav Sharma', '+91-9876543210'),
(2, 'Priya Patel', '+91-9876543211'),
(3, 'Rohan Verma', '+91-9876543212'),
(4, 'Ananya Iyer', '+91-9876543213'),
(5, 'Vikram Singh', '+91-9876543214');

-- 4. SEED INITIAL FLEET OF DRIVERS ACROSS THE CITY
INSERT INTO drivers (driver_id, name, vehicle_type, vehicle_number, current_x, current_y, current_zone_id, status, rating, total_trips_completed, total_earnings, last_available_at) VALUES
(101, 'Rajesh Kumar', 'SEDAN', 'KA-01-AB-1001', 10.50, 12.00, 1, 'AVAILABLE', 4.90, 15, 3200.00, CURRENT_TIMESTAMP),
(102, 'Suresh Reddy', 'SUV', 'KA-01-AB-1002', 30.00, 15.00, 2, 'AVAILABLE', 4.85, 22, 4800.00, CURRENT_TIMESTAMP),
(103, 'Amitabh Sen', 'SEDAN', 'KA-01-AB-1003', 48.00, 18.00, 3, 'AVAILABLE', 4.95, 31, 6500.00, CURRENT_TIMESTAMP),
(104, 'Manoj Tiwari', 'HATCHBACK', 'KA-01-AB-1004', 72.00, 10.00, 4, 'AVAILABLE', 4.70, 8, 1750.00, CURRENT_TIMESTAMP),
(105, 'Deepak Joshi', 'PREMIUM', 'KA-01-AB-1005', 88.00, 14.00, 5, 'AVAILABLE', 5.00, 40, 9200.00, CURRENT_TIMESTAMP),

(106, 'Gopal Das', 'SEDAN', 'KA-02-CD-2001', 12.00, 32.00, 6, 'AVAILABLE', 4.80, 19, 4100.00, CURRENT_TIMESTAMP),
(107, 'Kiran Hegde', 'SEDAN', 'KA-02-CD-2002', 28.00, 28.00, 7, 'AVAILABLE', 4.92, 27, 5600.00, CURRENT_TIMESTAMP),
(108, 'Naveen Rao', 'SUV', 'KA-02-CD-2003', 52.00, 35.00, 8, 'AVAILABLE', 4.78, 14, 3100.00, CURRENT_TIMESTAMP),
(109, 'Prakash Nair', 'HATCHBACK', 'KA-02-CD-2004', 68.00, 30.00, 9, 'AVAILABLE', 4.65, 11, 2300.00, CURRENT_TIMESTAMP),
(110, 'Santosh Yadav', 'SEDAN', 'KA-02-CD-2005', 92.00, 38.00, 10, 'AVAILABLE', 4.88, 25, 5200.00, CURRENT_TIMESTAMP),

(111, 'Harish Gowda', 'PREMIUM', 'KA-03-EF-3001', 15.00, 50.00, 11, 'AVAILABLE', 4.96, 38, 8900.00, CURRENT_TIMESTAMP),
(112, 'Arun Menon', 'SEDAN', 'KA-03-EF-3002', 35.00, 48.00, 12, 'AVAILABLE', 4.82, 17, 3600.00, CURRENT_TIMESTAMP),
(113, 'Vijay Mallya', 'SUV', 'KA-03-EF-3003', 49.00, 51.00, 13, 'AVAILABLE', 4.89, 29, 6100.00, CURRENT_TIMESTAMP),
(114, 'Dinesh Karthik', 'SEDAN', 'KA-03-EF-3004', 53.00, 52.00, 13, 'AVAILABLE', 4.75, 12, 2600.00, CURRENT_TIMESTAMP),
(115, 'Rahul Dravid', 'SEDAN', 'KA-03-EF-3005', 70.00, 55.00, 14, 'AVAILABLE', 4.99, 45, 10200.00, CURRENT_TIMESTAMP),

(116, 'Anil Kumble', 'HATCHBACK', 'KA-04-GH-4001', 85.00, 52.00, 15, 'AVAILABLE', 4.81, 16, 3400.00, CURRENT_TIMESTAMP),
(117, 'Sachin Tendulkar', 'PREMIUM', 'KA-04-GH-4002', 25.00, 72.00, 17, 'AVAILABLE', 5.00, 50, 12500.00, CURRENT_TIMESTAMP),
(118, 'Sourav Ganguly', 'SUV', 'KA-04-GH-4003', 48.00, 75.00, 18, 'AVAILABLE', 4.91, 33, 7200.00, CURRENT_TIMESTAMP),
(119, 'VVS Laxman', 'SEDAN', 'KA-04-GH-4004', 50.00, 85.00, 23, 'AVAILABLE', 4.87, 21, 4600.00, CURRENT_TIMESTAMP),
(120, 'Javagal Srinath', 'HATCHBACK', 'KA-04-GH-4005', 82.00, 88.00, 25, 'AVAILABLE', 4.79, 13, 2800.00, CURRENT_TIMESTAMP);

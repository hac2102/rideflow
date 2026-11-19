-- ============================================================================
-- V3__seed_riders.sql
-- Seed additional riders to expand simulated passenger pool
-- ============================================================================

INSERT INTO riders (rider_id, name, phone) VALUES
(6, 'Kavya Nair', '+91-9876543215'),
(7, 'Aditya Joshi', '+91-9876543216'),
(8, 'Diya Kapoor', '+91-9876543217'),
(9, 'Rahul Sen', '+91-9876543218'),
(10, 'Ishaan Rao', '+91-9876543219'),
(11, 'Neha Gupta', '+91-9876543220'),
(12, 'Siddharth Roy', '+91-9876543221'),
(13, 'Tanvi Menon', '+91-9876543222'),
(14, 'Manish Tiwari', '+91-9876543223'),
(15, 'Sneha Mukherjee', '+91-9876543224'),
(16, 'Arjun Reddy', '+91-9876543225'),
(17, 'Meera Chawla', '+91-9876543226'),
(18, 'Kunal Bajaj', '+91-9876543227'),
(19, 'Ritu Agarwal', '+91-9876543228'),
(20, 'Harsh Vardhan', '+91-9876543229')
ON DUPLICATE KEY UPDATE name=VALUES(name);

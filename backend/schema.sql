-- RoutPilot PostgreSQL Schema (2026-2027)
-- Strictly enforces data_source ('LIVE_HARDWARE' or 'VIRTUAL_TEST') across all sensor and hazard tables.

CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(100) UNIQUE NOT NULL,
    role VARCHAR(50) DEFAULT 'DRIVER',
    preferred_vehicle VARCHAR(20) DEFAULT 'CAR',
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS devices (
    device_id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    road_reference VARCHAR(120) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'DISCONNECTED',
    battery_percent DOUBLE PRECISION DEFAULT 0,
    last_seen TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS sensor_readings (
    id BIGSERIAL PRIMARY KEY,
    device_id VARCHAR(64) NOT NULL,
    timestamp TIMESTAMPTZ NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    vibration DOUBLE PRECISION NOT NULL,
    tilt DOUBLE PRECISION NOT NULL,
    strain DOUBLE PRECISION NOT NULL,
    displacement DOUBLE PRECISION NOT NULL,
    water_level DOUBLE PRECISION NOT NULL,
    data_source VARCHAR(32) NOT NULL CHECK (data_source IN ('LIVE_HARDWARE', 'VIRTUAL_TEST')),
    validation_status VARCHAR(64) NOT NULL
);

CREATE TABLE IF NOT EXISTS hazards (
    hazard_id VARCHAR(64) PRIMARY KEY,
    source VARCHAR(64) NOT NULL,
    timestamp TIMESTAMPTZ NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    hazard_type VARCHAR(64) NOT NULL,
    severity VARCHAR(32) NOT NULL CHECK (severity IN ('SAFE', 'WARNING', 'CRITICAL', 'BLOCKED')),
    sensor_id VARCHAR(64) NOT NULL,
    road_reference VARCHAR(120) NOT NULL,
    status VARCHAR(32) NOT NULL,
    data_source VARCHAR(32) NOT NULL CHECK (data_source IN ('LIVE_HARDWARE', 'VIRTUAL_TEST')),
    verification_status VARCHAR(32) NOT NULL DEFAULT 'VERIFIED'
);

CREATE TABLE IF NOT EXISTS road_status (
    road_reference VARCHAR(120) PRIMARY KEY,
    status VARCHAR(32) NOT NULL CHECK (status IN ('OPEN', 'WARNING', 'RESTRICTED', 'BLOCKED')),
    hazard_id VARCHAR(64),
    source VARCHAR(64) NOT NULL,
    timestamp TIMESTAMPTZ NOT NULL,
    reason TEXT NOT NULL,
    expiry TIMESTAMPTZ,
    verification_status VARCHAR(32) NOT NULL,
    data_source VARCHAR(32) NOT NULL CHECK (data_source IN ('LIVE_HARDWARE', 'VIRTUAL_TEST'))
);

CREATE TABLE IF NOT EXISTS route_requests (
    id BIGSERIAL PRIMARY KEY,
    timestamp TIMESTAMPTZ DEFAULT NOW(),
    source_label VARCHAR(200) NOT NULL,
    source_lat DOUBLE PRECISION NOT NULL,
    source_lon DOUBLE PRECISION NOT NULL,
    destination_label VARCHAR(200) NOT NULL,
    destination_lat DOUBLE PRECISION NOT NULL,
    destination_lon DOUBLE PRECISION NOT NULL,
    vehicle_type VARCHAR(20) NOT NULL,
    algorithm VARCHAR(20) NOT NULL,
    distance_km DOUBLE PRECISION NOT NULL,
    duration_min DOUBLE PRECISION NOT NULL,
    total_cost DOUBLE PRECISION NOT NULL,
    sensor_mode VARCHAR(32) NOT NULL
);

CREATE TABLE IF NOT EXISTS route_events (
    id BIGSERIAL PRIMARY KEY,
    timestamp TIMESTAMPTZ DEFAULT NOW(),
    event_type VARCHAR(64) NOT NULL,
    affected_road VARCHAR(120) NOT NULL,
    reason TEXT NOT NULL,
    old_route TEXT NOT NULL,
    new_route TEXT NOT NULL,
    data_source VARCHAR(32) NOT NULL CHECK (data_source IN ('LIVE_HARDWARE', 'VIRTUAL_TEST'))
);

CREATE TABLE IF NOT EXISTS journeys (
    journey_id VARCHAR(64) PRIMARY KEY,
    started_at TIMESTAMPTZ NOT NULL,
    ended_at TIMESTAMPTZ,
    source_label VARCHAR(200) NOT NULL,
    destination_label VARCHAR(200) NOT NULL,
    vehicle_type VARCHAR(20) NOT NULL,
    distance_remaining_km DOUBLE PRECISION NOT NULL,
    eta_minutes DOUBLE PRECISION NOT NULL,
    status VARCHAR(32) NOT NULL,
    sensor_mode VARCHAR(32) NOT NULL
);

CREATE TABLE IF NOT EXISTS notifications (
    id BIGSERIAL PRIMARY KEY,
    timestamp TIMESTAMPTZ DEFAULT NOW(),
    title VARCHAR(200) NOT NULL,
    message TEXT NOT NULL,
    severity VARCHAR(32) NOT NULL,
    data_source VARCHAR(32) NOT NULL CHECK (data_source IN ('LIVE_HARDWARE', 'VIRTUAL_TEST')),
    acknowledged BOOLEAN DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS system_events (
    id BIGSERIAL PRIMARY KEY,
    timestamp TIMESTAMPTZ DEFAULT NOW(),
    event_type VARCHAR(64) NOT NULL,
    location VARCHAR(120) NOT NULL,
    source VARCHAR(64) NOT NULL,
    mode VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    details TEXT
);

package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sensor_readings")
data class SensorReadingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val deviceId: String,
    val timestamp: Long,
    val timestampIso: String,
    val latitude: Double,
    val longitude: Double,
    val vibration: Double,
    val tilt: Double,
    val strain: Double,
    val displacement: Double,
    val waterLevel: Double,
    val dataSource: String, // Strictly "LIVE_HARDWARE" or "VIRTUAL_TEST"
    val validationStatus: String
)

@Entity(tableName = "hazards")
data class HazardEntity(
    @PrimaryKey val hazardId: String,
    val source: String,
    val timestamp: Long,
    val timestampFormatted: String,
    val latitude: Double,
    val longitude: Double,
    val hazardType: String,
    val hazardTitle: String,
    val severity: String, // SAFE, WARNING, CRITICAL, BLOCKED
    val sensorId: String,
    val sensorTypesLabel: String,
    val sensorValuesLabel: String,
    val roadReference: String,
    val edgeId: String,
    val status: String, // ACTIVE, RESOLVED
    val roadStatus: String, // OPEN, WARNING, RESTRICTED, BLOCKED
    val dataSource: String, // LIVE_HARDWARE or VIRTUAL_TEST
    val verificationStatus: String
)

@Entity(tableName = "road_status")
data class RoadStatusEntity(
    @PrimaryKey val roadReference: String,
    val edgeId: String,
    val status: String, // OPEN, WARNING, RESTRICTED, BLOCKED
    val hazardId: String?,
    val source: String,
    val timestamp: Long,
    val reason: String,
    val expiry: Long?,
    val verificationStatus: String,
    val dataSource: String // LIVE_HARDWARE or VIRTUAL_TEST
)

@Entity(tableName = "route_requests")
data class RouteRequestEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val sourceLabel: String,
    val sourceLat: Double,
    val sourceLon: Double,
    val destinationLabel: String,
    val destinationLat: Double,
    val destinationLon: Double,
    val vehicleType: String,
    val algorithm: String,
    val pathSummary: String,
    val distanceKm: Double,
    val durationMin: Double,
    val totalCost: Double,
    val sensorMode: String,
    val dataSource: String
)

@Entity(tableName = "history_events")
data class HistoryEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val formattedDate: String,
    val category: String, // SENSOR_EVENT, HAZARD_EVENT, ROUTE_REQUEST, ROUTE_DIVERSION, JOURNEY
    val eventType: String,
    val location: String,
    val source: String,
    val mode: String, // EXTERNAL HARDWARE or VIRTUAL MODE
    val status: String, // SAFE, WARNING, CRITICAL, BLOCKED, COMPLETED, DIVERTED
    val dataSource: String, // LIVE_HARDWARE or VIRTUAL_TEST
    val details: String
)

@Entity(tableName = "journeys")
data class JourneyEntity(
    @PrimaryKey val journeyId: String,
    val startedAt: Long,
    val endedAt: Long?,
    val sourceLabel: String,
    val destinationLabel: String,
    val vehicleType: String,
    val currentPath: String,
    val distanceRemainingKm: Double,
    val etaMinutes: Double,
    val status: String, // MOVING, PAUSED, STOPPED, COMPLETED
    val sensorMode: String
)

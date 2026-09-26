package com.example.domain

import com.example.model.DataSource
import com.example.model.HazardEvaluation
import com.example.model.HazardSeverity
import com.example.model.HazardType
import com.example.model.RoadStatusType
import com.example.model.SensorPacket
import com.example.model.SensorThresholdRule
import com.example.model.ThresholdConfig
import com.example.model.ValidationResult
import java.util.Locale
import kotlin.math.abs
import kotlin.math.sqrt

class SensorValidatorAndHazardEngine(
    private val thresholds: ThresholdConfig
) {
    private var lastProcessedTimestampByDevice = mutableMapOf<String, Long>()

    /**
     * Validates missing values, NaN/Inf, valid ranges, timestamp, GPS coordinates,
     * duplicate packets, device ID, and stale data.
     * Invalid sensor data is NEVER used for hazard detection.
     */
    fun validate(packet: SensorPacket, nowMillis: Long = System.currentTimeMillis()): ValidationResult {
        if (packet.deviceId.isBlank()) {
            return ValidationResult(false, "INVALID_DEVICE_ID", "Missing deviceId")
        }
        if (packet.timestampIso.isBlank() || packet.epochMillis <= 0L) {
            return ValidationResult(false, "INVALID_TIMESTAMP", "Missing or invalid timestamp")
        }
        val values = listOf(
            packet.latitude,
            packet.longitude,
            packet.vibration,
            packet.tilt,
            packet.strain,
            packet.displacement,
            packet.waterLevel
        )
        if (values.any { it.isNaN() || it.isInfinite() }) {
            return ValidationResult(false, "NAN_DETECTED", "Sensor packet contains NaN or Infinite values")
        }
        if (packet.latitude !in -90.0..90.0 || packet.longitude !in -180.0..180.0) {
            return ValidationResult(false, "INVALID_GPS", "Latitude/Longitude out of valid geographic bounds")
        }
        if (!inValidRange(packet.vibration, thresholds.vibration) ||
            !inValidRange(packet.tilt, thresholds.tilt) ||
            !inValidRange(packet.strain, thresholds.strain) ||
            !inValidRange(packet.displacement, thresholds.displacement) ||
            !inValidRange(packet.waterLevel, thresholds.waterLevel)
        ) {
            return ValidationResult(false, "OUT_OF_RANGE", "One or more sensor values exceed physical valid range")
        }

        val prevTs = lastProcessedTimestampByDevice[packet.deviceId]
        if (prevTs != null && prevTs == packet.epochMillis) {
            return ValidationResult(false, "DUPLICATE_PACKET", "Duplicate timestamp packet ignored")
        }

        val ageSeconds = (nowMillis - packet.epochMillis) / 1000L
        if (ageSeconds > thresholds.staleTimeoutSeconds * 4L) {
            return ValidationResult(false, "STALE_DATA", "Packet timestamp is stale (${ageSeconds}s old)")
        }

        lastProcessedTimestampByDevice[packet.deviceId] = packet.epochMillis
        return ValidationResult(true, "VALID", "All validation checks passed")
    }

    private fun inValidRange(value: Double, rule: SensorThresholdRule): Boolean {
        return value >= rule.validMin && value <= rule.validMax
    }

    fun classifySingleSensor(value: Double, rule: SensorThresholdRule): HazardSeverity {
        val v = abs(value)
        return when {
            v >= rule.blocked -> HazardSeverity.BLOCKED
            v >= rule.critical -> HazardSeverity.CRITICAL
            v >= rule.warning -> HazardSeverity.WARNING
            else -> HazardSeverity.SAFE
        }
    }

    /**
     * Evaluates a validated SensorPacket against centralized thresholds (`hazard_thresholds.json`)
     * and returns full HazardEvaluation including HazardType, HazardSeverity, and Dynamic RoadStatusType.
     */
    fun evaluateHazard(packet: SensorPacket): HazardEvaluation {
        val vibSeverity = classifySingleSensor(packet.vibration, thresholds.vibration)
        val tiltSeverity = classifySingleSensor(packet.tilt, thresholds.tilt)
        val strainSeverity = classifySingleSensor(packet.strain, thresholds.strain)
        val dispSeverity = classifySingleSensor(packet.displacement, thresholds.displacement)
        val waterSeverity = classifySingleSensor(packet.waterLevel, thresholds.waterLevel)

        val triggered = mutableListOf<String>()
        val valueParts = mutableListOf<String>()

        if (vibSeverity != HazardSeverity.SAFE) {
            triggered.add("Vibration")
            valueParts.add(String.format(Locale.US, "%.2f g", packet.vibration))
        }
        if (tiltSeverity != HazardSeverity.SAFE) {
            triggered.add("Tilt")
            valueParts.add(String.format(Locale.US, "%.1f°", packet.tilt))
        }
        if (strainSeverity != HazardSeverity.SAFE) {
            triggered.add("Strain")
            valueParts.add(String.format(Locale.US, "%.0f µε", packet.strain))
        }
        if (dispSeverity != HazardSeverity.SAFE) {
            triggered.add("Displacement")
            valueParts.add(String.format(Locale.US, "%.1f mm", packet.displacement))
        }
        if (waterSeverity != HazardSeverity.SAFE) {
            triggered.add("Water Level")
            valueParts.add(String.format(Locale.US, "%.0f cm", packet.waterLevel))
        }
        if (packet.roadConstructionActive) {
            triggered.add("Road Work")
            valueParts.add("Active Zone")
        }

        val allSeverities = listOf(vibSeverity, tiltSeverity, strainSeverity, dispSeverity, waterSeverity)
        val maxIndividual = allSeverities.maxByOrNull { it.priority } ?: HazardSeverity.SAFE
        val criticalCount = allSeverities.count { it.priority >= HazardSeverity.CRITICAL.priority }
        val warningCount = allSeverities.count { it.priority >= HazardSeverity.WARNING.priority }

        val overallSeverity = when {
            maxIndividual == HazardSeverity.BLOCKED ||
                criticalCount >= thresholds.minCriticalSensorsForBlocked -> HazardSeverity.CRITICAL
            maxIndividual == HazardSeverity.CRITICAL ||
                warningCount >= thresholds.minWarningSensorsForCritical -> HazardSeverity.CRITICAL
            maxIndividual == HazardSeverity.WARNING || packet.roadConstructionActive -> HazardSeverity.WARNING
            else -> HazardSeverity.SAFE
        }

        if (overallSeverity == HazardSeverity.SAFE && !packet.roadConstructionActive) {
            return HazardEvaluation(
                hazardDetected = false,
                hazardType = HazardType.NONE,
                severity = HazardSeverity.SAFE,
                roadStatus = RoadStatusType.OPEN,
                triggeredSensors = emptyList(),
                formattedValues = String.format(
                    Locale.US,
                    "%.2f g, %.1f°, %.0f µε",
                    packet.vibration,
                    packet.tilt,
                    packet.strain
                ),
                affectedEdgeId = "B-C",
                roadReference = "Bridge B1 (B - C)",
                description = "All structural and road parameters within normal limits",
                dataSource = packet.dataSource
            )
        }

        val hazardType = when {
            vibSeverity != HazardSeverity.SAFE &&
                tiltSeverity != HazardSeverity.SAFE &&
                strainSeverity != HazardSeverity.SAFE &&
                packet.roadConstructionActive -> HazardType.COMBINED_HAZARD
            vibSeverity != HazardSeverity.SAFE &&
                tiltSeverity != HazardSeverity.SAFE &&
                strainSeverity != HazardSeverity.SAFE -> HazardType.BRIDGE_STRUCTURAL_WARNING
            triggered.size >= 2 -> HazardType.COMBINED_HAZARD
            packet.roadConstructionActive -> HazardType.ROAD_CONSTRUCTION
            vibSeverity != HazardSeverity.SAFE -> HazardType.HIGH_VIBRATION
            tiltSeverity != HazardSeverity.SAFE -> HazardType.ABNORMAL_TILT
            strainSeverity != HazardSeverity.SAFE -> HazardType.HIGH_STRAIN
            dispSeverity != HazardSeverity.SAFE -> HazardType.HIGH_DISPLACEMENT
            waterSeverity != HazardSeverity.SAFE -> HazardType.HIGH_WATER_LEVEL
            else -> HazardType.HIGH_VIBRATION
        }

        val roadStatus = when (overallSeverity) {
            HazardSeverity.BLOCKED, HazardSeverity.CRITICAL -> RoadStatusType.BLOCKED
            HazardSeverity.WARNING -> if (hazardType == HazardType.ROAD_CONSTRUCTION) {
                RoadStatusType.WARNING
            } else {
                RoadStatusType.RESTRICTED
            }
            HazardSeverity.SAFE -> RoadStatusType.OPEN
        }

        val affectedEdge = when (hazardType) {
            HazardType.ROAD_CONSTRUCTION -> "H-I"
            HazardType.HIGH_WATER_LEVEL -> "D-R"
            HazardType.HIGH_DISPLACEMENT -> "C-F"
            else -> "B-C"
        }

        val roadRef = when (affectedEdge) {
            "H-I" -> "Link H - I (Road Work)"
            "D-R" -> "River Causeway (D - R)"
            "C-F" -> "East Span (C - F)"
            else -> "Bridge B1 (B - C)"
        }

        val desc = when (hazardType) {
            HazardType.BRIDGE_STRUCTURAL_WARNING -> "Bridge B1 structural vibration high"
            HazardType.HIGH_VIBRATION -> "Bridge B1 deck vibration exceeding safe threshold"
            HazardType.ABNORMAL_TILT -> "Bridge B1 pier tilt inclination alert"
            HazardType.HIGH_STRAIN -> "Bridge B1 main girder microstrain alert"
            HazardType.HIGH_DISPLACEMENT -> "Bridge deck expansion joint displacement alert"
            HazardType.HIGH_WATER_LEVEL -> "High water level detected at river crossing"
            HazardType.ROAD_CONSTRUCTION -> "Road construction zone active on Link H - I"
            HazardType.COMBINED_HAZARD -> "Bridge B1 structural alert & corridor hazard active"
            HazardType.NONE -> "Normal road operation"
        }

        val finalTriggeredSensors = if (hazardType == HazardType.BRIDGE_STRUCTURAL_WARNING) {
            listOf("Vibration", "Tilt", "Strain")
        } else {
            triggered.ifEmpty { listOf("Vibration") }
        }
        val finalFormattedValues = if (hazardType == HazardType.BRIDGE_STRUCTURAL_WARNING) {
            String.format(Locale.US, "%.2f g, %.1f°, %.0f µε", packet.vibration, packet.tilt, packet.strain)
        } else {
            valueParts.joinToString(", ").ifBlank {
                String.format(Locale.US, "%.2f g, %.1f°, %.0f µε", packet.vibration, packet.tilt, packet.strain)
            }
        }

        return HazardEvaluation(
            hazardDetected = true,
            hazardType = hazardType,
            severity = overallSeverity,
            roadStatus = roadStatus,
            triggeredSensors = finalTriggeredSensors,
            formattedValues = finalFormattedValues,
            affectedEdgeId = affectedEdge,
            roadReference = roadRef,
            description = desc,
            dataSource = packet.dataSource
        )
    }

    /**
     * Local MATLAB-equivalent structural signal analysis (RMS, Peak, StdDev) used when remote
     * MATLAB Engine is offline, matching `matlab/analyzeSensorSignal.m`.
     */
    fun analyzeSignalSeries(series: List<Float>, dataSource: DataSource): Map<String, String> {
        if (series.isEmpty()) {
            return mapOf(
                "dataSource" to dataSource.dbValue,
                "rms" to "0.000",
                "peak" to "0.000",
                "stdDev" to "0.000"
            )
        }
        val n = series.size
        val mean = series.sum() / n
        val rms = sqrt(series.sumOf { (it * it).toDouble() } / n)
        val peak = series.maxOf { abs(it) }
        val variance = series.sumOf { ((it - mean) * (it - mean)).toDouble() } / n
        val stdDev = sqrt(variance)
        return mapOf(
            "dataSource" to dataSource.dbValue,
            "mean" to String.format(Locale.US, "%.3f", mean),
            "rms" to String.format(Locale.US, "%.3f", rms),
            "peak" to String.format(Locale.US, "%.3f", peak),
            "stdDev" to String.format(Locale.US, "%.3f", stdDev)
        )
    }
}

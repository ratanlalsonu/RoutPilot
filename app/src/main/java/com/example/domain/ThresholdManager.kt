package com.example.domain

import android.content.Context
import com.example.model.SensorThresholdRule
import com.example.model.ThresholdConfig
import org.json.JSONObject

object ThresholdManager {
    private var cachedConfig: ThresholdConfig? = null

    fun loadThresholds(context: Context): ThresholdConfig {
        cachedConfig?.let { return it }
        return try {
            val jsonText = context.assets.open("hazard_thresholds.json")
                .bufferedReader()
                .use { it.readText() }
            val root = JSONObject(jsonText)
            val combined = root.optJSONObject("combinedHazardRule")
            val parsed = ThresholdConfig(
                version = root.optString("version", "2026.1"),
                vibration = parseRule(root.getJSONObject("vibration"), "g"),
                tilt = parseRule(root.getJSONObject("tilt"), "°"),
                strain = parseRule(root.getJSONObject("strain"), "µε"),
                displacement = parseRule(root.getJSONObject("displacement"), "mm"),
                waterLevel = parseRule(root.getJSONObject("waterLevel"), "cm"),
                staleTimeoutSeconds = root.optInt("staleTimeoutSeconds", 15),
                minWarningSensorsForCritical = combined?.optInt("minWarningSensorsForCritical", 2) ?: 2,
                minCriticalSensorsForBlocked = combined?.optInt("minCriticalSensorsForBlocked", 2) ?: 2
            )
            cachedConfig = parsed
            parsed
        } catch (e: Exception) {
            ThresholdConfig().also { cachedConfig = it }
        }
    }

    private fun parseRule(obj: JSONObject, defaultUnit: String): SensorThresholdRule {
        val rangeArr = obj.optJSONArray("normalRange")
        val nMin = rangeArr?.optDouble(0, 0.0) ?: 0.0
        val nMax = rangeArr?.optDouble(1, obj.optDouble("warning", 1.0)) ?: 1.0
        val rawUnit = obj.optString("unit", defaultUnit)
        val displayUnit = when (rawUnit) {
            "deg" -> "°"
            "uE" -> "µε"
            else -> rawUnit
        }
        return SensorThresholdRule(
            unit = displayUnit,
            normalMin = nMin,
            normalMax = nMax,
            warning = obj.optDouble("warning", nMax),
            critical = obj.optDouble("critical", nMax * 1.5),
            blocked = obj.optDouble("blocked", nMax * 2.0),
            validMin = obj.optDouble("validMin", 0.0),
            validMax = obj.optDouble("validMax", 1000.0)
        )
    }
}

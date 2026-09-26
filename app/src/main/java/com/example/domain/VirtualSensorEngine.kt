package com.example.domain

import com.example.model.DataSource
import com.example.model.SensorPacket
import com.example.model.VirtualEngineState
import com.example.model.VirtualScenario
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.min
import kotlin.math.sin

class VirtualSensorEngine {
    var currentScenario: VirtualScenario = VirtualScenario.NORMAL
        private set

    var engineState: VirtualEngineState = VirtualEngineState.STOPPED
        private set

    private var stepIndex: Int = 0

    fun startScenario(scenario: VirtualScenario = currentScenario) {
        if (currentScenario != scenario) {
            stepIndex = 0
        }
        currentScenario = scenario
        engineState = VirtualEngineState.RUNNING
    }

    fun pauseScenario() {
        if (engineState == VirtualEngineState.RUNNING) {
            engineState = VirtualEngineState.PAUSED
        }
    }

    fun stopScenario() {
        engineState = VirtualEngineState.STOPPED
    }

    fun resetScenario() {
        stepIndex = 0
        engineState = VirtualEngineState.STOPPED
    }

    /**
     * Generates the next controlled, gradual test reading for the active VirtualScenario.
     * Always sets dataSource = DataSource.VIRTUAL_TEST ("TEST DATA — NOT LIVE").
     */
    fun nextVirtualPacket(baseLat: Double, baseLon: Double): SensorPacket {
        stepIndex++
        val progress = min(1.0, stepIndex / 8.0) // Smooth ramp over 8 ticks
        val harmonic = 0.02 * sin(stepIndex * 0.6)

        var vib = 0.24 + harmonic
        var tilt = 1.1 + harmonic * 2.0
        var strain = 115.0 + harmonic * 40.0
        var disp = 2.8 + harmonic * 4.0
        var water = 14.0 + harmonic * 10.0
        var construction = false

        when (currentScenario) {
            VirtualScenario.NORMAL -> {
                vib = 0.26 + harmonic
                tilt = 1.2 + harmonic * 2.0
                strain = 120.0 + harmonic * 30.0
                disp = 3.0 + harmonic * 3.0
                water = 16.0 + harmonic * 8.0
                construction = false
            }
            VirtualScenario.HIGH_VIBRATION -> {
                vib = 0.28 + progress * 0.57 + harmonic // Rises smoothly to ~0.85 g
                tilt = 1.3 + progress * 0.3
                strain = 130.0 + progress * 40.0
            }
            VirtualScenario.ABNORMAL_TILT -> {
                vib = 0.30 + progress * 0.10
                tilt = 1.2 + progress * 3.1 + harmonic // Rises smoothly to ~4.3°
                strain = 140.0 + progress * 45.0
            }
            VirtualScenario.HIGH_STRAIN -> {
                vib = 0.31 + progress * 0.08
                tilt = 1.4 + progress * 0.4
                strain = 120.0 + progress * 205.0 + harmonic * 20.0 // Rises smoothly to ~325 µε
            }
            VirtualScenario.HIGH_DISPLACEMENT -> {
                disp = 3.0 + progress * 6.4 + harmonic // Rises smoothly to ~9.4 mm
                strain = 135.0 + progress * 55.0
            }
            VirtualScenario.HIGH_WATER_LEVEL -> {
                water = 18.0 + progress * 30.0 + harmonic * 5.0 // Rises smoothly to ~48 cm
                vib = 0.29 + progress * 0.08
            }
            VirtualScenario.ROAD_CONSTRUCTION -> {
                vib = 0.38 + progress * 0.12 // Warning vibration near roadwork
                construction = true
            }
            VirtualScenario.BRIDGE_STRUCTURAL_WARNING -> {
                // Controlled structural hazard matching reference screen: 0.85 g, 4.2 deg, 320 uE
                vib = 0.85
                tilt = 4.2
                strain = 320.0
                disp = 3.0
                water = 25.0
                construction = true
            }
            VirtualScenario.COMBINED_HAZARD -> {
                vib = 0.30 + progress * 0.58
                tilt = 1.3 + progress * 3.1
                strain = 125.0 + progress * 205.0
                disp = 3.2 + progress * 5.2
                water = 22.0 + progress * 18.0
                construction = true
            }
        }

        val now = System.currentTimeMillis()
        val isoFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        return SensorPacket(
            deviceId = "VIRTUAL-ENGINE-01",
            timestampIso = isoFormatter.format(Date(now)),
            epochMillis = now,
            latitude = baseLat + 0.0045,
            longitude = baseLon - 0.0020,
            vibration = vib.coerceAtLeast(0.05),
            tilt = tilt.coerceAtLeast(0.1),
            strain = strain.coerceAtLeast(10.0),
            displacement = disp.coerceAtLeast(0.2),
            waterLevel = water.coerceAtLeast(1.0),
            roadConstructionActive = construction,
            battery = 100.0,
            dataSource = DataSource.VIRTUAL_TEST
        )
    }
}

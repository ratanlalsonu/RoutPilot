package com.example

import com.example.data.remote.RealNetworkProviders
import com.example.domain.RoutingAlgorithms
import com.example.domain.SensorValidatorAndHazardEngine
import com.example.domain.VirtualSensorEngine
import com.example.model.DataSource
import com.example.model.HardwareConfiguration
import com.example.model.HardwareConnectionType
import com.example.model.HazardSeverity
import com.example.model.RoadStatusType
import com.example.model.RouteCostWeights
import com.example.model.SensorMode
import com.example.model.SensorPacket
import com.example.model.ThresholdConfig
import com.example.model.VehicleType
import com.example.model.VirtualScenario
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun virtualSensorEngine_allScenariosTagTestDataNotLive() {
        val engine = VirtualSensorEngine()
        VirtualScenario.entries.forEach { scenario ->
            engine.startScenario(scenario)
            val packet = engine.nextVirtualPacket(28.6139, 77.2090)
            assertEquals(DataSource.VIRTUAL_TEST, packet.dataSource)
            assertEquals("TEST DATA — NOT LIVE", packet.dataSource.badgeLabel)
        }
    }

    @Test
    fun hardwareConfiguration_optionalEndpointsReturnNotConfiguredWithoutFakeValues() = runBlocking {
        val unconfigured = HardwareConfiguration(
            httpEndpoint = "",
            wsEndpoint = "",
            deviceId = "ESP32-001",
            connectionType = HardwareConnectionType.HTTP_AND_WS
        )
        assertFalse(unconfigured.isConfigured)

        val providers = RealNetworkProviders()
        val pollResult = providers.pollRealEsp32Packet("", "ESP32-001")
        assertNull(pollResult)

        val (testPacket, message) = providers.testEsp32HardwareConnection(unconfigured, "http://10.0.2.2:8000")
        assertNull(testPacket)
        assertEquals("ESP32 endpoint not configured.", message)
    }

    @Test
    fun sensorModes_haveExactDisplayTitles() {
        assertEquals("VIRTUAL MODE", SensorMode.VIRTUAL.displayTitle)
        assertEquals("EXTERNAL HARDWARE", SensorMode.EXTERNAL_HARDWARE.displayTitle)
    }

    @Test
    fun validatorAndHazardEngine_detectsBridgeStructuralCriticalAndRejectsNaN() {
        val engine = SensorValidatorAndHazardEngine(ThresholdConfig())
        val now = System.currentTimeMillis()
        val nanPacket = SensorPacket(
            deviceId = "ESP32-001",
            timestampIso = "2026-09-25T20:00:00Z",
            epochMillis = now,
            latitude = 28.61,
            longitude = 77.20,
            vibration = Double.NaN,
            tilt = 1.0,
            strain = 100.0,
            displacement = 2.0,
            waterLevel = 10.0,
            dataSource = DataSource.LIVE_HARDWARE
        )
        assertFalse(engine.validate(nanPacket, now).isValid)

        val criticalPacket = nanPacket.copy(
            vibration = 0.85,
            tilt = 4.2,
            strain = 320.0
        )
        assertTrue(engine.validate(criticalPacket, now).isValid)
        val eval = engine.evaluateHazard(criticalPacket)
        assertTrue(eval.hazardDetected)
        assertEquals(HazardSeverity.CRITICAL, eval.severity)
        assertEquals(RoadStatusType.BLOCKED, eval.roadStatus)
    }

    @Test
    fun aStarAndDijkstra_divertAroundBlockedBridgeAndAvoidHazards() {
        val overrides = mapOf(
            "B-C" to RoutingAlgorithms.RoadStatusInfo(
                status = RoadStatusType.BLOCKED,
                severity = HazardSeverity.CRITICAL,
                hazardId = "HZ-B-C",
                reason = "Bridge B1 Blocked",
                dataSource = DataSource.VIRTUAL_TEST
            )
        )
        val (nodes, edges) = RoutingAlgorithms.buildMonitoredGraph(
            anchorLat = 28.6139,
            anchorLon = 77.2090,
            roadStatusOverrides = overrides
        )
        val weights = RouteCostWeights()
        val aStar = RoutingAlgorithms.calculateRouteAStar(nodes, edges, "A", "T", VehicleType.CAR, weights)
        val dijkstra = RoutingAlgorithms.calculateRouteDijkstra(nodes, edges, "A", "T", VehicleType.CAR, weights)

        assertTrue(aStar.nodePath.isNotEmpty())
        assertTrue(dijkstra.nodePath.isNotEmpty())
        val usedBC = aStar.nodePath.zipWithNext().any { (u, v) -> (u == "B" && v == "C") || (u == "C" && v == "B") }
        assertFalse(usedBC)
        assertTrue(aStar.nodesEvaluated <= dijkstra.nodesEvaluated)
    }
}

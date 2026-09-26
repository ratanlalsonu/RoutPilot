package com.example.model

enum class SensorMode(val apiValue: String, val displayTitle: String) {
    EXTERNAL_HARDWARE("EXTERNAL_HARDWARE", "EXTERNAL HARDWARE"),
    VIRTUAL("VIRTUAL", "VIRTUAL MODE")
}

enum class DataSource(val dbValue: String, val badgeLabel: String) {
    LIVE_HARDWARE("LIVE_HARDWARE", "LIVE SENSOR DATA"),
    VIRTUAL_TEST("VIRTUAL_TEST", "TEST DATA — NOT LIVE"),
    CACHED("CACHED", "CACHED DATA"),
    CALCULATED("CALCULATED", "CALCULATED DATA"),
    EXTERNAL_API("EXTERNAL_API", "EXTERNAL API DATA")
}

enum class HardwareConnectionState(val label: String) {
    CONNECTED("CONNECTED"),
    DISCONNECTED("DISCONNECTED"),
    STALE("STALE"),
    ERROR("ERROR")
}

enum class VirtualEngineState(val label: String) {
    RUNNING("RUNNING"),
    PAUSED("PAUSED"),
    STOPPED("STOPPED")
}

enum class HazardSeverity(val label: String, val priority: Int) {
    SAFE("Normal", 0),
    WARNING("Warning", 1),
    CRITICAL("CRITICAL", 2),
    BLOCKED("BLOCKED", 3)
}

enum class RoadStatusType(val label: String) {
    OPEN("Safe"),
    WARNING("Warning"),
    RESTRICTED("Restricted"),
    BLOCKED("Blocked")
}

enum class HazardType(val code: String, val displayTitle: String, val defaultRoadRef: String) {
    NONE("NONE", "Normal Condition", "All Monitored Corridors"),
    HIGH_VIBRATION("HIGH_VIBRATION", "High Vibration", "Bridge B1 (B - C)"),
    ABNORMAL_TILT("ABNORMAL_TILT", "Abnormal Tilt", "Bridge B1 Pier 2 (B - C)"),
    HIGH_STRAIN("HIGH_STRAIN", "High Strain", "Main Span Girder (B - C)"),
    HIGH_DISPLACEMENT("HIGH_DISPLACEMENT", "High Displacement", "Expansion Joint (C - F)"),
    HIGH_WATER_LEVEL("HIGH_WATER_LEVEL", "High Water Level", "River Causeway (D - R)"),
    ROAD_CONSTRUCTION("ROAD_CONSTRUCTION", "Road Construction", "Link H - I (Road Work)"),
    BRIDGE_STRUCTURAL_WARNING("BRIDGE_STRUCTURAL_WARNING", "Bridge Structural", "Bridge B1 (B - C)"),
    COMBINED_HAZARD("COMBINED_HAZARD", "Combined Multi-Sensor Hazard", "Bridge B1 & Link H - I")
}

enum class VirtualScenario(
    val id: String,
    val title: String,
    val subtitle: String,
    val targetHazard: HazardType
) {
    NORMAL("NORMAL", "Normal Journey", "Baseline structural & road telemetry within safe limits", HazardType.NONE),
    ROAD_CONSTRUCTION("ROAD_CONSTRUCTION", "Road Construction", "Active lane work on H - I causing warning & restriction", HazardType.ROAD_CONSTRUCTION),
    HIGH_VIBRATION("HIGH_VIBRATION", "High Vibration", "Gradual rise in bridge deck acceleration exceeding 0.65 g", HazardType.HIGH_VIBRATION),
    ABNORMAL_TILT("ABNORMAL_TILT", "Abnormal Tilt", "Pier inclination drifting above 3.2° critical threshold", HazardType.ABNORMAL_TILT),
    HIGH_STRAIN("HIGH_STRAIN", "High Strain", "Structural microstrain rising past 260 µε on main girder", HazardType.HIGH_STRAIN),
    HIGH_DISPLACEMENT("HIGH_DISPLACEMENT", "High Displacement", "Deck lateral displacement exceeding 7.5 mm limit", HazardType.HIGH_DISPLACEMENT),
    HIGH_WATER_LEVEL("HIGH_WATER_LEVEL", "High Water Level", "Flood gauge rising above 35 cm near low-lying bridge", HazardType.HIGH_WATER_LEVEL),
    BRIDGE_STRUCTURAL_WARNING("BRIDGE_STRUCTURAL_WARNING", "Bridge Structural Warning", "Simultaneous high vibration (0.85 g), tilt (4.2°), and strain (320 µε) on Bridge B1", HazardType.BRIDGE_STRUCTURAL_WARNING),
    COMBINED_HAZARD("COMBINED_HAZARD", "Combined Hazard", "Bridge B1 structural critical alert + Link H-I construction warning", HazardType.COMBINED_HAZARD)
}

enum class VehicleType(
    val label: String,
    val osrmProfile: String,
    val weightTons: Double,
    val heightMeters: Double,
    val widthMeters: Double
) {
    CAR("Car", "driving", 1.8, 1.6, 1.9),
    BIKE("Bike", "cycling", 0.25, 1.2, 0.8),
    VAN("Van", "driving", 4.2, 3.1, 2.4)
}

enum class RoutingAlgorithm(val code: String, val displayName: String) {
    ASTAR("ASTAR", "A* (A-Star)"),
    DIJKSTRA("DIJKSTRA", "Dijkstra")
}

enum class AppRole(val displayName: String, val badgeText: String) {
    USER_PANEL("User / Driver Panel", "USER"),
    ADMIN_PANEL("Admin Authority Panel", "ADMIN")
}

enum class AppScreen {
    SPLASH,
    HOME,
    ADMIN_DASHBOARD,
    LIVE_MAP,
    SENSORS,
    ROUTE_PLANNER,
    HAZARD_DETECTION,
    ALGORITHM_COMPARISON,
    JOURNEY_TRACKING,
    TEST_SCENARIOS,
    SETTINGS,
    HISTORY,
    CONNECTIVITY,
    DATA_SOURCES
}

data class CitizenHazardReport(
    val id: String,
    val edgeId: String,
    val roadName: String,
    val hazardType: String,
    val severity: HazardSeverity,
    val description: String,
    val reportedBy: String = "Driver (User Panel)",
    val timestampLabel: String,
    val status: String = "PENDING_VERIFICATION" // PENDING_VERIFICATION, VERIFIED_ACTIVE, RESOLVED
)

data class SensorThresholdRule(
    val unit: String,
    val normalMin: Double,
    val normalMax: Double,
    val warning: Double,
    val critical: Double,
    val blocked: Double,
    val validMin: Double,
    val validMax: Double
)

data class ThresholdConfig(
    val version: String = "2026.1",
    val vibration: SensorThresholdRule = SensorThresholdRule("g", 0.0, 0.35, 0.35, 0.65, 0.80, 0.0, 10.0),
    val tilt: SensorThresholdRule = SensorThresholdRule("°", 0.0, 1.8, 1.8, 3.2, 4.0, -45.0, 45.0),
    val strain: SensorThresholdRule = SensorThresholdRule("µε", 0.0, 180.0, 180.0, 260.0, 300.0, 0.0, 2500.0),
    val displacement: SensorThresholdRule = SensorThresholdRule("mm", 0.0, 4.5, 4.5, 7.5, 9.0, 0.0, 200.0),
    val waterLevel: SensorThresholdRule = SensorThresholdRule("cm", 0.0, 20.0, 20.0, 35.0, 45.0, 0.0, 500.0),
    val staleTimeoutSeconds: Int = 15,
    val minWarningSensorsForCritical: Int = 2,
    val minCriticalSensorsForBlocked: Int = 2
)

data class SensorPacket(
    val deviceId: String,
    val timestampIso: String,
    val epochMillis: Long,
    val latitude: Double,
    val longitude: Double,
    val vibration: Double,
    val tilt: Double,
    val strain: Double,
    val displacement: Double,
    val waterLevel: Double,
    val roadConstructionActive: Boolean = false,
    val battery: Double = 98.0,
    val dataSource: DataSource
)

data class ValidationResult(
    val isValid: Boolean,
    val status: String,
    val reason: String
)

data class HazardEvaluation(
    val hazardDetected: Boolean,
    val hazardType: HazardType,
    val severity: HazardSeverity,
    val roadStatus: RoadStatusType,
    val triggeredSensors: List<String>,
    val formattedValues: String,
    val affectedEdgeId: String,
    val roadReference: String,
    val description: String,
    val dataSource: DataSource
)

data class RoadNode(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val normalizedX: Float,
    val normalizedY: Float,
    val isLandmark: Boolean = false
)

data class RoadEdge(
    val id: String,
    val fromNodeId: String,
    val toNodeId: String,
    val roadName: String,
    val distanceKm: Double,
    val baseTimeMin: Double,
    val trafficFactor: Double = 0.2,
    val isBridge: Boolean = false,
    val maxWeightTons: Double = 20.0,
    val maxHeightMeters: Double = 4.5,
    val minWidthMeters: Double = 3.5,
    val bikeAccessible: Boolean = true,
    val status: RoadStatusType = RoadStatusType.OPEN,
    val severity: HazardSeverity = HazardSeverity.SAFE,
    val activeHazardId: String? = null,
    val statusReason: String = "Normal flow",
    val dataSource: DataSource = DataSource.CALCULATED
)

data class RouteCostWeights(
    val distanceWeight: Float = 1.0f,
    val timeWeight: Float = 1.0f,
    val trafficWeight: Float = 0.5f,
    val warningPenalty: Float = 5.0f,
    val criticalPenalty: Float = 20.0f,
    val vehicleRestrictionPenalty: Float = 100.0f,
    val simulationSpeed: Float = 1.0f
)

data class AlgorithmRouteResult(
    val algorithm: RoutingAlgorithm,
    val nodePath: List<String>,
    val coordinateGeometry: List<Pair<Double, Double>>,
    val roadNames: List<String>,
    val turnSteps: List<String>,
    val distanceKm: Double,
    val estimatedTimeMin: Double,
    val totalCost: Double,
    val nodesEvaluated: Int,
    val hazardsAvoided: Int,
    val blockedRoadsAvoided: Int,
    val calculationTimeMs: Double,
    val providerName: String,
    val trafficAvailable: Boolean,
    val isDiverted: Boolean = false,
    val diversionReason: String? = null
)

data class AlgorithmComparison(
    val aStarResult: AlgorithmRouteResult?,
    val dijkstraResult: AlgorithmRouteResult?,
    val executedAtMillis: Long = System.currentTimeMillis(),
    val matlabVerified: Boolean = false
)

data class PlaceSearchResult(
    val displayName: String,
    val shortName: String,
    val category: String,
    val latitude: Double,
    val longitude: Double,
    val mappedNodeId: String? = null,
    val sourceProvider: String = "OpenStreetMap Nominatim"
)

data class GpsTelemetry(
    val isAvailable: Boolean = false,
    val hasPermission: Boolean = false,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val accuracyMeters: Float? = null,
    val speedKmh: Float? = null,
    val headingDegrees: Float? = null,
    val providerLabel: String = "Android Fused GPS",
    val lastUpdatedMillis: Long? = null
)

data class DiversionAlertState(
    val isActive: Boolean = false,
    val stage: String = "ROUTE AFFECTED", // ROUTE AFFECTED -> CHECKING ALTERNATIVE... -> ALTERNATE ROUTE FOUND
    val affectedRoad: String = "",
    val reason: String = "",
    val originalPathLabel: String = "",
    val alternatePathLabel: String = "",
    val newDistanceKm: Double = 0.0,
    val newTimeMin: Double = 0.0,
    val dataSource: DataSource = DataSource.VIRTUAL_TEST
)

data class HazardRouteNotificationState(
    val isVisible: Boolean = false,
    val notificationTitle: String = "Hazard Alert Ahead on Chosen Route",
    val locationLabel: String = "Bridge B1 (Main Span, Segment B → C)",
    val coordinatesLabel: String = "28.6403° N, 77.1995° E",
    val infrastructureType: String = "BRIDGE HAZARD (Bridge B1)",
    val hazardTypeSummary: String = "High Structural Vibration (0.85 g), Abnormal Tilt (4.2°) & High Strain (320 µε) — Bridge Blocked",
    val severity: HazardSeverity = HazardSeverity.CRITICAL,
    val affectedChosenPathLabel: String = "A → B → C → F → N → T",
    val primarySafeRoute: AlgorithmRouteResult? = null,
    val secondarySafeRoute: AlgorithmRouteResult? = null,
    val dataSource: DataSource = DataSource.VIRTUAL_TEST,
    val routeSwitchedConfirmation: String? = null
)

enum class HardwareConnectionType(val code: String, val label: String) {
    HTTP("HTTP", "Wi-Fi HTTP REST"),
    WEBSOCKET("WEBSOCKET", "WebSocket Stream"),
    HTTP_AND_WS("HTTP_AND_WS", "HTTP + WebSocket"),
    MQTT_FASTAPI("MQTT_FASTAPI", "MQTT via FastAPI")
}

data class HardwareConfiguration(
    val httpEndpoint: String = "",
    val wsEndpoint: String = "",
    val deviceId: String = "ESP32-001",
    val connectionType: HardwareConnectionType = HardwareConnectionType.HTTP_AND_WS
) {
    val isConfigured: Boolean
        get() = httpEndpoint.isNotBlank() || wsEndpoint.isNotBlank()
}

data class HardwareTestConnectionResult(
    val isTesting: Boolean = false,
    val tested: Boolean = false,
    val success: Boolean = false,
    val statusHeadline: String = "",
    val statusSubtext: String = "",
    val verifiedDeviceId: String? = null,
    val testedAtMillis: Long? = null
)

data class SystemConnectivityState(
    val internetOnline: Boolean = true,
    val gpsAvailable: Boolean = false,
    val fastApiConnected: Boolean = false,
    val fastApiUrl: String = "http://10.0.2.2:8000",
    val databaseStatus: String = "Room Active (PostgreSQL Sync Ready)",
    val esp32Configured: Boolean = false,
    val esp32State: HardwareConnectionState = HardwareConnectionState.DISCONNECTED,
    val esp32DeviceId: String = "ESP32-001",
    val esp32Endpoint: String = "",
    val esp32WsEndpoint: String = "",
    val esp32ConnectionType: HardwareConnectionType = HardwareConnectionType.HTTP_AND_WS,
    val esp32LastSeenSecondsAgo: Long? = null,
    val matlabConnected: Boolean = false,
    val matlabStatusMessage: String = "MATLAB unavailable — using local processing.",
    val routingProviderStatus: String = "OSRM Live + RoutPilot Graph Engine"
)


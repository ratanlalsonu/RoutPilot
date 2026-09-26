package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.HazardEntity
import com.example.data.local.HistoryEventEntity
import com.example.data.local.JourneyEntity
import com.example.data.local.RoadStatusEntity
import com.example.data.local.RoutPilotDatabase
import com.example.data.local.RouteRequestEntity
import com.example.data.local.SensorReadingEntity
import com.example.data.location.RealLocationTracker
import com.example.data.location.RoutPilotNotificationHelper
import com.example.data.remote.ExternalRouteResponse
import com.example.data.remote.RealNetworkProviders
import com.example.domain.RoutingAlgorithms
import com.example.domain.SensorValidatorAndHazardEngine
import com.example.domain.ThresholdManager
import com.example.domain.VirtualSensorEngine
import com.example.model.AlgorithmComparison
import com.example.model.AlgorithmRouteResult
import com.example.model.AppScreen
import com.example.model.DataSource
import com.example.model.DiversionAlertState
import com.example.model.GpsTelemetry
import com.example.model.HardwareConnectionState
import com.example.model.HazardEvaluation
import com.example.model.HazardRouteNotificationState
import com.example.model.HazardSeverity
import com.example.model.HazardType
import com.example.model.PlaceSearchResult
import com.example.model.RoadEdge
import com.example.model.RoadNode
import com.example.model.RoadStatusType
import com.example.model.RouteCostWeights
import com.example.model.RoutingAlgorithm
import com.example.model.SensorMode
import com.example.model.SensorPacket
import com.example.model.SystemConnectivityState
import com.example.model.ThresholdConfig
import com.example.model.VehicleType
import com.example.model.VirtualEngineState
import com.example.model.VirtualScenario
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import android.content.Context
import com.example.model.HardwareConfiguration
import com.example.model.HardwareConnectionType
import com.example.model.HardwareTestConnectionResult
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class RoutPilotViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val prefs = appContext.getSharedPreferences("routpilot_hardware_prefs", Context.MODE_PRIVATE)
    private val db = RoutPilotDatabase.getInstance(appContext)
    private val dao = db.routPilotDao()
    val thresholds: ThresholdConfig = ThresholdManager.loadThresholds(appContext)
    private val validatorAndHazardEngine = SensorValidatorAndHazardEngine(thresholds)
    private val virtualSensorEngine = VirtualSensorEngine()
    val networkProviders = RealNetworkProviders()
    val locationTracker = RealLocationTracker(appContext)

    // Navigation, First-Launch Onboarding & Theme State
    private val screenBackStack = ArrayDeque<AppScreen>()
    private val _currentScreen = MutableStateFlow(AppScreen.SPLASH)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _isFirstLaunch = MutableStateFlow(!prefs.getBoolean("has_completed_first_launch", false))
    val isFirstLaunch: StateFlow<Boolean> = _isFirstLaunch.asStateFlow()

    private val _isDarkTheme = MutableStateFlow(prefs.getBoolean("dark_theme_enabled", false))
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    // Two Sensor Modes: VIRTUAL (DEFAULT) vs EXTERNAL_HARDWARE
    private val _sensorMode = MutableStateFlow(SensorMode.VIRTUAL)
    val sensorMode: StateFlow<SensorMode> = _sensorMode.asStateFlow()

    private val _pendingModeSwitch = MutableStateFlow<SensorMode?>(null)
    val pendingModeSwitch: StateFlow<SensorMode?> = _pendingModeSwitch.asStateFlow()

    // Optional ESP32 Hardware Configuration (Not required for Virtual Mode)
    private val _hardwareConfig = MutableStateFlow(loadInitialHardwareConfig())
    val hardwareConfig: StateFlow<HardwareConfiguration> = _hardwareConfig.asStateFlow()

    private val _hardwareTestResult = MutableStateFlow(HardwareTestConnectionResult())
    val hardwareTestResult: StateFlow<HardwareTestConnectionResult> = _hardwareTestResult.asStateFlow()

    // Hardware Mode Telemetry (ONLY real ESP32 packets, null when disconnected)
    private val _liveHardwarePacket = MutableStateFlow<SensorPacket?>(null)
    val liveHardwarePacket: StateFlow<SensorPacket?> = _liveHardwarePacket.asStateFlow()

    private val _lastHardwareTimestampMillis = MutableStateFlow<Long?>(null)

    // Virtual Mode Telemetry (Clearly marked TEST DATA — NOT LIVE)
    private val _virtualTestPacket = MutableStateFlow<SensorPacket?>(null)
    val virtualTestPacket: StateFlow<SensorPacket?> = _virtualTestPacket.asStateFlow()

    private val _selectedScenario = MutableStateFlow(VirtualScenario.BRIDGE_STRUCTURAL_WARNING)
    val selectedScenario: StateFlow<VirtualScenario> = _selectedScenario.asStateFlow()

    private val _virtualEngineState = MutableStateFlow(VirtualEngineState.STOPPED)
    val virtualEngineState: StateFlow<VirtualEngineState> = _virtualEngineState.asStateFlow()

    // Historical sparkline series separated strictly by mode
    private val _hardwareSeries = MutableStateFlow<Map<String, List<Float>>>(emptyMap())
    val hardwareSeries: StateFlow<Map<String, List<Float>>> = _hardwareSeries.asStateFlow()

    private val _virtualSeries = MutableStateFlow<Map<String, List<Float>>>(emptyMap())
    val virtualSeries: StateFlow<Map<String, List<Float>>> = _virtualSeries.asStateFlow()

    // Active Hazard Evaluation per mode
    private val _hardwareHazardEval = MutableStateFlow<HazardEvaluation?>(null)
    val hardwareHazardEval: StateFlow<HazardEvaluation?> = _hardwareHazardEval.asStateFlow()

    private val _virtualHazardEval = MutableStateFlow<HazardEvaluation?>(null)
    val virtualHazardEval: StateFlow<HazardEvaluation?> = _virtualHazardEval.asStateFlow()

    // MATLAB Signal Analysis Metrics
    private val _matlabSignalAnalysis = MutableStateFlow<Map<String, String>>(emptyMap())
    val matlabSignalAnalysis: StateFlow<Map<String, String>> = _matlabSignalAnalysis.asStateFlow()

    // Connectivity & System Status
    private val _connectivity = MutableStateFlow(
        SystemConnectivityState(
            fastApiUrl = getOptionalBuildConfigString("ROUTPILOT_BACKEND_URL").ifBlank { "http://10.0.2.2:8000" },
            esp32Configured = _hardwareConfig.value.isConfigured,
            esp32DeviceId = _hardwareConfig.value.deviceId,
            esp32Endpoint = _hardwareConfig.value.httpEndpoint,
            esp32WsEndpoint = _hardwareConfig.value.wsEndpoint,
            esp32ConnectionType = _hardwareConfig.value.connectionType
        )
    )
    val connectivity: StateFlow<SystemConnectivityState> = _connectivity.asStateFlow()

    // GPS State
    val gpsState: StateFlow<GpsTelemetry> = locationTracker.gpsState

    // Route Planner & Graph State
    private val _routeWeights = MutableStateFlow(RouteCostWeights())
    val routeWeights: StateFlow<RouteCostWeights> = _routeWeights.asStateFlow()

    private val _sourceNodeId = MutableStateFlow("A")
    val sourceNodeId: StateFlow<String> = _sourceNodeId.asStateFlow()

    private val _destinationNodeId = MutableStateFlow("T")
    val destinationNodeId: StateFlow<String> = _destinationNodeId.asStateFlow()

    private val _selectedVehicle = MutableStateFlow(VehicleType.CAR)
    val selectedVehicle: StateFlow<VehicleType> = _selectedVehicle.asStateFlow()

    private val _selectedAlgorithm = MutableStateFlow(RoutingAlgorithm.ASTAR)
    val selectedAlgorithm: StateFlow<RoutingAlgorithm> = _selectedAlgorithm.asStateFlow()

    private val _placeSearchQuery = MutableStateFlow("")
    val placeSearchQuery: StateFlow<String> = _placeSearchQuery.asStateFlow()

    private val _placeSearchResults = MutableStateFlow<List<PlaceSearchResult>>(emptyList())
    val placeSearchResults: StateFlow<List<PlaceSearchResult>> = _placeSearchResults.asStateFlow()

    private val _isSearchingPlaces = MutableStateFlow(false)
    val isSearchingPlaces: StateFlow<Boolean> = _isSearchingPlaces.asStateFlow()

    private val _selectedCustomPlace = MutableStateFlow<PlaceSearchResult?>(null)
    val selectedCustomPlace: StateFlow<PlaceSearchResult?> = _selectedCustomPlace.asStateFlow()

    private val _externalOsrmRoute = MutableStateFlow<ExternalRouteResponse?>(null)
    val externalOsrmRoute: StateFlow<ExternalRouteResponse?> = _externalOsrmRoute.asStateFlow()

    private val _graphNodes = MutableStateFlow<List<RoadNode>>(emptyList())
    val graphNodes: StateFlow<List<RoadNode>> = _graphNodes.asStateFlow()

    private val _graphEdges = MutableStateFlow<List<RoadEdge>>(emptyList())
    val graphEdges: StateFlow<List<RoadEdge>> = _graphEdges.asStateFlow()

    private val _activeRouteResult = MutableStateFlow<AlgorithmRouteResult?>(null)
    val activeRouteResult: StateFlow<AlgorithmRouteResult?> = _activeRouteResult.asStateFlow()

    private val _alternateRouteResult = MutableStateFlow<AlgorithmRouteResult?>(null)
    val alternateRouteResult: StateFlow<AlgorithmRouteResult?> = _alternateRouteResult.asStateFlow()

    private val _initialOptimalRouteResult = MutableStateFlow<AlgorithmRouteResult?>(null)
    val initialOptimalRouteResult: StateFlow<AlgorithmRouteResult?> = _initialOptimalRouteResult.asStateFlow()

    private val _safeOptimalRoute1 = MutableStateFlow<AlgorithmRouteResult?>(null)
    val safeOptimalRoute1: StateFlow<AlgorithmRouteResult?> = _safeOptimalRoute1.asStateFlow()

    private val _safeOptimalRoute2 = MutableStateFlow<AlgorithmRouteResult?>(null)
    val safeOptimalRoute2: StateFlow<AlgorithmRouteResult?> = _safeOptimalRoute2.asStateFlow()

    private val _algorithmComparison = MutableStateFlow<AlgorithmComparison?>(null)
    val algorithmComparison: StateFlow<AlgorithmComparison?> = _algorithmComparison.asStateFlow()

    private val _diversionAlert = MutableStateFlow(DiversionAlertState())
    val diversionAlert: StateFlow<DiversionAlertState> = _diversionAlert.asStateFlow()

    private val _hazardRouteNotification = MutableStateFlow(HazardRouteNotificationState())
    val hazardRouteNotification: StateFlow<HazardRouteNotificationState> = _hazardRouteNotification.asStateFlow()

    // Journey State
    private val _journeyStatus = MutableStateFlow("Moving") // Moving, Paused, Stopped
    val journeyStatus: StateFlow<String> = _journeyStatus.asStateFlow()

    private val _journeyNodeIndex = MutableStateFlow(2) // Points at current node along active route
    val journeyNodeIndex: StateFlow<Int> = _journeyNodeIndex.asStateFlow()

    // Reactive Room DB Flows separated by active SensorMode's DataSource
    val activeModeHazards: StateFlow<List<HazardEntity>> = _sensorMode
        .flatMapLatest { mode ->
            val ds = if (mode == SensorMode.EXTERNAL_HARDWARE) {
                DataSource.LIVE_HARDWARE.dbValue
            } else {
                DataSource.VIRTUAL_TEST.dbValue
            }
            dao.observeActiveHazardsBySource(ds)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeModeRoadStatuses: StateFlow<List<RoadStatusEntity>> = _sensorMode
        .flatMapLatest { mode ->
            val ds = if (mode == SensorMode.EXTERNAL_HARDWARE) {
                DataSource.LIVE_HARDWARE.dbValue
            } else {
                DataSource.VIRTUAL_TEST.dbValue
            }
            dao.observeRoadStatusesBySource(ds)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val historyEvents: StateFlow<List<HistoryEventEntity>> = dao.observeHistoryEvents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentRoutes: StateFlow<List<RouteRequestEntity>> = dao.observeRouteRequests()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var virtualLoopJob: Job? = null
    private var hardwarePollJob: Job? = null

    init {
        // Default mode is VIRTUAL MODE. Do NOT initialize ESP32 connection on startup.
        _virtualSeries.value = mapOf(
            "vibration" to listOf(0.22f, 0.28f, 0.34f, 0.42f, 0.51f, 0.60f, 0.68f, 0.75f, 0.81f, 0.85f),
            "tilt" to listOf(1.0f, 1.3f, 1.6f, 2.0f, 2.4f, 2.9f, 3.3f, 3.7f, 4.0f, 4.2f),
            "strain" to listOf(110f, 130f, 155f, 180f, 210f, 235f, 260f, 285f, 305f, 320f),
            "displacement" to listOf(1.8f, 2.0f, 2.1f, 2.3f, 2.4f, 2.6f, 2.7f, 2.8f, 2.9f, 3.0f),
            "waterLevel" to listOf(16f, 17f, 18f, 19f, 20f, 21f, 22f, 23f, 24f, 25f)
        )
        virtualSensorEngine.startScenario(VirtualScenario.BRIDGE_STRUCTURAL_WARNING)
        val initialVirtual = virtualSensorEngine.nextVirtualPacket(28.6139, 77.2090)
        _virtualTestPacket.value = initialVirtual
        _virtualHazardEval.value = validatorAndHazardEngine.evaluateHazard(initialVirtual)
        locationTracker.startTracking()
        rebuildGraphAndRoutes()
        startVirtualScenario(VirtualScenario.BRIDGE_STRUCTURAL_WARNING)
        startBackgroundConnectivityAndHardwareMonitor()
    }

    private fun getOptionalBuildConfigString(fieldName: String): String {
        return runCatching {
            val clazz = Class.forName("com.example.BuildConfig")
            val field = clazz.getDeclaredField(fieldName)
            val raw = (field.get(null) as? String)?.trim().orEmpty()
            if (raw.equals("null", ignoreCase = true)) "" else raw
        }.getOrDefault("")
    }

    private fun loadInitialHardwareConfig(): HardwareConfiguration {
        val envHttp = getOptionalBuildConfigString("ESP32_HTTP_ENDPOINT")
            .ifBlank { getOptionalBuildConfigString("ESP32_DEVICE_URL") }
        val envWs = getOptionalBuildConfigString("ESP32_WS_ENDPOINT")

        val savedHttp = prefs.getString("esp32_http_endpoint", null) ?: envHttp
        val savedWs = prefs.getString("esp32_ws_endpoint", null) ?: envWs
        val savedDeviceId = prefs.getString("esp32_device_id", null) ?: "ESP32-001"
        val savedConnTypeStr = prefs.getString("esp32_connection_type", null) ?: HardwareConnectionType.HTTP_AND_WS.code
        val connType = HardwareConnectionType.entries.find { it.code == savedConnTypeStr }
            ?: HardwareConnectionType.HTTP_AND_WS

        return HardwareConfiguration(
            httpEndpoint = savedHttp.trim(),
            wsEndpoint = savedWs.trim(),
            deviceId = savedDeviceId.trim().ifBlank { "ESP32-001" },
            connectionType = connType
        )
    }

    fun completeSplashAndOpenHome() {
        if (_isFirstLaunch.value) {
            _isFirstLaunch.value = false
            prefs.edit().putBoolean("has_completed_first_launch", true).apply()
        }
        navigateTo(AppScreen.HOME)
    }

    fun navigateTo(screen: AppScreen) {
        if (_currentScreen.value != screen) {
            if (_currentScreen.value != AppScreen.SPLASH) {
                screenBackStack.addLast(_currentScreen.value)
            }
            _currentScreen.value = screen
        }
    }

    fun navigateBack(): Boolean {
        return if (screenBackStack.isNotEmpty()) {
            _currentScreen.value = screenBackStack.removeLast()
            true
        } else if (_currentScreen.value != AppScreen.HOME && _currentScreen.value != AppScreen.SPLASH) {
            _currentScreen.value = AppScreen.HOME
            true
        } else {
            false
        }
    }

    fun setDarkTheme(dark: Boolean) {
        _isDarkTheme.value = dark
        prefs.edit().putBoolean("dark_theme_enabled", dark).apply()
    }

    /**
     * Mode Safety Rule: Prompt user confirmation before switching sensor mode.
     */
    fun requestModeSwitch(targetMode: SensorMode) {
        if (targetMode == _sensorMode.value) return
        _pendingModeSwitch.value = targetMode
    }

    fun cancelModeSwitch() {
        _pendingModeSwitch.value = null
    }

    fun confirmModeSwitch() {
        val target = _pendingModeSwitch.value ?: return
        _pendingModeSwitch.value = null
        applyModeSwitch(target)
    }

    private fun applyModeSwitch(target: SensorMode) {
        _sensorMode.value = target

        viewModelScope.launch {
            networkProviders.syncSensorModeToBackend(_connectivity.value.fastApiUrl, target)
            recordHistoryEvent(
                category = "MODE_SWITCH",
                eventType = "SENSOR_MODE_CHANGED",
                location = "System Control",
                source = if (target == SensorMode.EXTERNAL_HARDWARE) _hardwareConfig.value.deviceId else "Virtual Sensor Engine",
                mode = target.displayTitle,
                status = "ACTIVE",
                dataSource = if (target == SensorMode.EXTERNAL_HARDWARE) DataSource.LIVE_HARDWARE else DataSource.VIRTUAL_TEST,
                details = "Switched to ${target.displayTitle}"
            )
            if (target == SensorMode.EXTERNAL_HARDWARE) {
                // Stop virtual loop so virtual values are never generated or used in Hardware Mode
                pauseVirtualScenario()
                _diversionAlert.value = DiversionAlertState(isActive = false)
                initializeExternalHardwareConnectionIfConfigured()
            } else {
                // Virtual Mode selected: Do NOT initialize or maintain ESP32 connection
                networkProviders.disconnectEsp32WebSocket()
                if (_virtualEngineState.value != VirtualEngineState.RUNNING) {
                    startVirtualScenario(_selectedScenario.value)
                }
            }
            rebuildGraphAndRoutes()
        }
    }

    /**
     * Saves ESP32 Hardware Configuration (HTTP endpoint, WebSocket endpoint, Device ID, Connection Type).
     */
    fun saveHardwareConfiguration(config: HardwareConfiguration, fastApiUrl: String = _connectivity.value.fastApiUrl) {
        val cleanConfig = config.copy(
            httpEndpoint = config.httpEndpoint.trim(),
            wsEndpoint = config.wsEndpoint.trim(),
            deviceId = config.deviceId.trim().ifBlank { "ESP32-001" }
        )
        prefs.edit()
            .putString("esp32_http_endpoint", cleanConfig.httpEndpoint)
            .putString("esp32_ws_endpoint", cleanConfig.wsEndpoint)
            .putString("esp32_device_id", cleanConfig.deviceId)
            .putString("esp32_connection_type", cleanConfig.connectionType.code)
            .apply()

        _hardwareConfig.value = cleanConfig
        _connectivity.value = _connectivity.value.copy(
            fastApiUrl = fastApiUrl.trim().ifBlank { "http://10.0.2.2:8000" },
            esp32Configured = cleanConfig.isConfigured,
            esp32DeviceId = cleanConfig.deviceId,
            esp32Endpoint = cleanConfig.httpEndpoint,
            esp32WsEndpoint = cleanConfig.wsEndpoint,
            esp32ConnectionType = cleanConfig.connectionType
        )

        if (_sensorMode.value == SensorMode.EXTERNAL_HARDWARE) {
            initializeExternalHardwareConnectionIfConfigured()
        }
    }

    /**
     * Updates ESP32 and FastAPI endpoints in Hardware/Connectivity settings.
     */
    fun updateEndpoints(esp32Url: String, fastApiUrl: String) {
        saveHardwareConfiguration(
            config = _hardwareConfig.value.copy(httpEndpoint = esp32Url.trim()),
            fastApiUrl = fastApiUrl.trim()
        )
    }

    /**
     * Executes the 5-step Hardware Test Connection required in Settings > Hardware Configuration:
     * 1. Check whether ESP32 endpoint is configured.
     * 2. Connect to ESP32.
     * 3. Check response & verify device ID.
     * 4. Check & validate sensor data packet.
     * 5. Show real status (LIVE SENSOR DATA vs NO LIVE SENSOR DATA).
     */
    fun testHardwareConnection(configOverride: HardwareConfiguration? = null) {
        val cfg = configOverride?.let {
            it.copy(
                httpEndpoint = it.httpEndpoint.trim(),
                wsEndpoint = it.wsEndpoint.trim(),
                deviceId = it.deviceId.trim().ifBlank { "ESP32-001" }
            )
        } ?: _hardwareConfig.value

        if (configOverride != null) {
            saveHardwareConfiguration(cfg)
        }

        if (!cfg.isConfigured) {
            _liveHardwarePacket.value = null
            _lastHardwareTimestampMillis.value = null
            _connectivity.value = _connectivity.value.copy(
                esp32Configured = false,
                esp32State = HardwareConnectionState.DISCONNECTED
            )
            _hardwareTestResult.value = HardwareTestConnectionResult(
                isTesting = false,
                tested = true,
                success = false,
                statusHeadline = "EXTERNAL HARDWARE • NO LIVE SENSOR DATA",
                statusSubtext = "ESP32 endpoint not configured.",
                verifiedDeviceId = null,
                testedAtMillis = System.currentTimeMillis()
            )
            return
        }

        viewModelScope.launch {
            _hardwareTestResult.value = HardwareTestConnectionResult(
                isTesting = true,
                tested = false,
                success = false,
                statusHeadline = "Testing ESP32 Connection...",
                statusSubtext = "Connecting to ${cfg.deviceId} (${cfg.connectionType.label})..."
            )

            val (packet, msg) = networkProviders.testEsp32HardwareConnection(cfg, _connectivity.value.fastApiUrl)
            if (packet != null && validatorAndHazardEngine.validate(packet).isValid) {
                processIncomingSensorPacket(packet)
                _connectivity.value = _connectivity.value.copy(
                    esp32Configured = true,
                    esp32State = HardwareConnectionState.CONNECTED,
                    esp32LastSeenSecondsAgo = 0L
                )
                _hardwareTestResult.value = HardwareTestConnectionResult(
                    isTesting = false,
                    tested = true,
                    success = true,
                    statusHeadline = "EXTERNAL HARDWARE • LIVE SENSOR DATA",
                    statusSubtext = msg,
                    verifiedDeviceId = packet.deviceId,
                    testedAtMillis = System.currentTimeMillis()
                )
            } else {
                _liveHardwarePacket.value = null
                _connectivity.value = _connectivity.value.copy(
                    esp32Configured = cfg.isConfigured,
                    esp32State = HardwareConnectionState.DISCONNECTED
                )
                _hardwareTestResult.value = HardwareTestConnectionResult(
                    isTesting = false,
                    tested = true,
                    success = false,
                    statusHeadline = "EXTERNAL HARDWARE • NO LIVE SENSOR DATA",
                    statusSubtext = msg,
                    verifiedDeviceId = null,
                    testedAtMillis = System.currentTimeMillis()
                )
            }
        }
    }

    private fun initializeExternalHardwareConnectionIfConfigured() {
        if (_sensorMode.value != SensorMode.EXTERNAL_HARDWARE) {
            networkProviders.disconnectEsp32WebSocket()
            return
        }
        val cfg = _hardwareConfig.value
        if (!cfg.isConfigured) {
            networkProviders.disconnectEsp32WebSocket()
            _liveHardwarePacket.value = null
            _lastHardwareTimestampMillis.value = null
            _connectivity.value = _connectivity.value.copy(
                esp32Configured = false,
                esp32State = HardwareConnectionState.DISCONNECTED,
                esp32LastSeenSecondsAgo = null
            )
            return
        }

        // Connect WebSocket if configured and selected
        if (cfg.wsEndpoint.isNotBlank() &&
            cfg.connectionType in setOf(HardwareConnectionType.WEBSOCKET, HardwareConnectionType.HTTP_AND_WS)
        ) {
            networkProviders.connectEsp32WebSocket(
                wsEndpoint = cfg.wsEndpoint,
                expectedDeviceId = cfg.deviceId,
                onPacketReceived = { packet ->
                    if (_sensorMode.value == SensorMode.EXTERNAL_HARDWARE) {
                        viewModelScope.launch {
                            processIncomingSensorPacket(packet)
                        }
                    }
                },
                onDisconnectedOrError = {
                    // Handled by staleness/disconnection check in monitor loop
                }
            )
        } else {
            networkProviders.disconnectEsp32WebSocket()
        }

        // Also poll HTTP / FastAPI immediately if configured
        pollEsp32Once()
    }

    fun pollEsp32Once() {
        if (_sensorMode.value != SensorMode.EXTERNAL_HARDWARE) return
        val cfg = _hardwareConfig.value
        if (!cfg.isConfigured) return

        viewModelScope.launch {
            val packet = when (cfg.connectionType) {
                HardwareConnectionType.HTTP, HardwareConnectionType.HTTP_AND_WS -> {
                    if (cfg.httpEndpoint.isNotBlank()) {
                        networkProviders.pollRealEsp32Packet(cfg.httpEndpoint, cfg.deviceId)
                    } else {
                        null
                    }
                }
                HardwareConnectionType.MQTT_FASTAPI -> {
                    val endpoint = cfg.httpEndpoint.ifBlank {
                        "${_connectivity.value.fastApiUrl.trimEnd('/')}/api/sensors/${cfg.deviceId}"
                    }
                    networkProviders.pollRealEsp32Packet(endpoint, cfg.deviceId)
                }
                HardwareConnectionType.WEBSOCKET -> null
            }
            if (packet != null) {
                processIncomingSensorPacket(packet)
            }
        }
    }

    private fun startBackgroundConnectivityAndHardwareMonitor() {
        hardwarePollJob?.cancel()
        hardwarePollJob = viewModelScope.launch {
            while (true) {
                val internetOk = locationTracker.isInternetAvailable()
                val gpsOk = gpsState.value.isAvailable
                val (fastOk, matlabOk) = networkProviders.checkFastApiAndMatlabHealth(_connectivity.value.fastApiUrl)
                val cfg = _hardwareConfig.value

                // ONLY attempt ESP32 connection when External Hardware Mode is selected AND endpoint is configured
                if (_sensorMode.value == SensorMode.EXTERNAL_HARDWARE && cfg.isConfigured) {
                    if (cfg.wsEndpoint.isNotBlank() &&
                        cfg.connectionType in setOf(HardwareConnectionType.WEBSOCKET, HardwareConnectionType.HTTP_AND_WS)
                    ) {
                        networkProviders.connectEsp32WebSocket(
                            wsEndpoint = cfg.wsEndpoint,
                            expectedDeviceId = cfg.deviceId,
                            onPacketReceived = { packet ->
                                if (_sensorMode.value == SensorMode.EXTERNAL_HARDWARE) {
                                    viewModelScope.launch {
                                        processIncomingSensorPacket(packet)
                                    }
                                }
                            },
                            onDisconnectedOrError = {}
                        )
                    }
                    if (cfg.httpEndpoint.isNotBlank() &&
                        cfg.connectionType in setOf(
                            HardwareConnectionType.HTTP,
                            HardwareConnectionType.HTTP_AND_WS,
                            HardwareConnectionType.MQTT_FASTAPI
                        )
                    ) {
                        val espPacket = networkProviders.pollRealEsp32Packet(cfg.httpEndpoint, cfg.deviceId)
                        if (espPacket != null) {
                            processIncomingSensorPacket(espPacket)
                        }
                    }
                } else if (_sensorMode.value == SensorMode.VIRTUAL) {
                    // Ensure ESP32 WebSocket is never initialized while in Virtual Mode
                    networkProviders.disconnectEsp32WebSocket()
                }

                // Evaluate ESP32 connection state (CONNECTED / STALE / DISCONNECTED)
                val lastSeenMillis = _lastHardwareTimestampMillis.value
                val ageSec = if (lastSeenMillis != null) {
                    (System.currentTimeMillis() - lastSeenMillis) / 1000L
                } else {
                    null
                }
                val espState = when {
                    !cfg.isConfigured -> HardwareConnectionState.DISCONNECTED
                    ageSec == null -> HardwareConnectionState.DISCONNECTED
                    ageSec <= thresholds.staleTimeoutSeconds -> HardwareConnectionState.CONNECTED
                    ageSec <= thresholds.staleTimeoutSeconds * 4 -> HardwareConnectionState.STALE
                    else -> HardwareConnectionState.DISCONNECTED
                }

                if (espState != HardwareConnectionState.CONNECTED) {
                    _liveHardwarePacket.value = null
                }

                _connectivity.value = _connectivity.value.copy(
                    internetOnline = internetOk,
                    gpsAvailable = gpsOk,
                    fastApiConnected = fastOk,
                    esp32Configured = cfg.isConfigured,
                    esp32DeviceId = cfg.deviceId,
                    esp32Endpoint = cfg.httpEndpoint,
                    esp32WsEndpoint = cfg.wsEndpoint,
                    esp32ConnectionType = cfg.connectionType,
                    esp32State = espState,
                    esp32LastSeenSecondsAgo = ageSec,
                    matlabConnected = matlabOk,
                    matlabStatusMessage = if (matlabOk) {
                        "MATLAB CONNECTED"
                    } else {
                        "MATLAB unavailable — using local processing."
                    }
                )

                delay(3000L)
            }
        }
    }

    // Virtual Mode Scenario Controls
    fun selectVirtualScenario(scenario: VirtualScenario) {
        _selectedScenario.value = scenario
    }

    fun startVirtualScenario(scenario: VirtualScenario = _selectedScenario.value) {
        _selectedScenario.value = scenario
        virtualSensorEngine.startScenario(scenario)
        _virtualEngineState.value = VirtualEngineState.RUNNING

        virtualLoopJob?.cancel()
        virtualLoopJob = viewModelScope.launch {
            while (_virtualEngineState.value == VirtualEngineState.RUNNING) {
                val anchorLat = gpsState.value.latitude ?: 28.6139
                val anchorLon = gpsState.value.longitude ?: 77.2090
                val packet = virtualSensorEngine.nextVirtualPacket(anchorLat, anchorLon)
                processIncomingSensorPacket(packet)
                val speed = _routeWeights.value.simulationSpeed.coerceIn(0.5f, 3.0f)
                val intervalMs = (1800L / speed).toLong().coerceAtLeast(500L)
                delay(intervalMs)
            }
        }
    }

    fun pauseVirtualScenario() {
        virtualSensorEngine.pauseScenario()
        _virtualEngineState.value = VirtualEngineState.PAUSED
        virtualLoopJob?.cancel()
    }

    fun stopVirtualScenario() {
        virtualSensorEngine.stopScenario()
        _virtualEngineState.value = VirtualEngineState.STOPPED
        virtualLoopJob?.cancel()
    }

    fun resetVirtualScenario() {
        virtualSensorEngine.resetScenario()
        _virtualEngineState.value = VirtualEngineState.STOPPED
        virtualLoopJob?.cancel()
        _virtualTestPacket.value = null
        _virtualHazardEval.value = null
        _virtualSeries.value = emptyMap()
        _diversionAlert.value = DiversionAlertState(isActive = false)
        viewModelScope.launch {
            dao.clearHazardsBySource(DataSource.VIRTUAL_TEST.dbValue)
            dao.clearRoadStatusesBySource(DataSource.VIRTUAL_TEST.dbValue)
            rebuildGraphAndRoutes()
        }
    }

    /**
     * Unified Sensor Validation -> Database Recording -> Hazard Detection -> Road Status -> Route Diversion Pipeline.
     * Strictly separates LIVE_HARDWARE and VIRTUAL_TEST data sources.
     */
    private suspend fun processIncomingSensorPacket(packet: SensorPacket) {
        val validation = validatorAndHazardEngine.validate(packet)

        // Persist sensor reading in Room with explicit dataSource and validationStatus
        dao.insertSensorReading(
            SensorReadingEntity(
                deviceId = packet.deviceId,
                timestamp = packet.epochMillis,
                timestampIso = packet.timestampIso,
                latitude = packet.latitude,
                longitude = packet.longitude,
                vibration = packet.vibration,
                tilt = packet.tilt,
                strain = packet.strain,
                displacement = packet.displacement,
                waterLevel = packet.waterLevel,
                dataSource = packet.dataSource.dbValue,
                validationStatus = validation.status
            )
        )

        if (!validation.isValid) {
            return
        }

        val eval = validatorAndHazardEngine.evaluateHazard(packet)

        if (packet.dataSource == DataSource.LIVE_HARDWARE) {
            _liveHardwarePacket.value = packet
            _lastHardwareTimestampMillis.value = packet.epochMillis
            _hardwareSeries.value = appendToSparkline(_hardwareSeries.value, packet)
            val prevEval = _hardwareHazardEval.value
            _hardwareHazardEval.value = eval
            val vibSeries = _hardwareSeries.value["vibration"] ?: emptyList()
            _matlabSignalAnalysis.value = validatorAndHazardEngine.analyzeSignalSeries(vibSeries, DataSource.LIVE_HARDWARE)
            persistHazardAndRoadStatus(packet, eval, prevEval)
        } else if (packet.dataSource == DataSource.VIRTUAL_TEST) {
            _virtualTestPacket.value = packet
            _virtualSeries.value = appendToSparkline(_virtualSeries.value, packet)
            val prevEval = _virtualHazardEval.value
            _virtualHazardEval.value = eval
            val vibSeries = _virtualSeries.value["vibration"] ?: emptyList()
            _matlabSignalAnalysis.value = validatorAndHazardEngine.analyzeSignalSeries(vibSeries, DataSource.VIRTUAL_TEST)
            persistHazardAndRoadStatus(packet, eval, prevEval)
        }
    }

    private suspend fun persistHazardAndRoadStatus(
        packet: SensorPacket,
        eval: HazardEvaluation,
        previousEval: HazardEvaluation?
    ) {
        val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm:ss a", Locale.US).format(Date(packet.epochMillis))
        val shortDate = SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date(packet.epochMillis))
        val modeLabel = if (packet.dataSource == DataSource.LIVE_HARDWARE) {
            SensorMode.EXTERNAL_HARDWARE.displayTitle
        } else {
            SensorMode.VIRTUAL.displayTitle
        }

        if (!eval.hazardDetected) {
            dao.clearHazardsBySource(packet.dataSource.dbValue)
            dao.clearRoadStatusesBySource(packet.dataSource.dbValue)
            _diversionAlert.value = DiversionAlertState(isActive = false)
            rebuildGraphAndRoutes()
            return
        }

        val hazardId = "HZ-${packet.dataSource.name.take(4)}-${eval.affectedEdgeId}"
        val hazardEntity = HazardEntity(
            hazardId = hazardId,
            source = packet.deviceId,
            timestamp = packet.epochMillis,
            timestampFormatted = dateStr,
            latitude = packet.latitude,
            longitude = packet.longitude,
            hazardType = eval.hazardType.code,
            hazardTitle = eval.hazardType.displayTitle,
            severity = eval.severity.name,
            sensorId = packet.deviceId,
            sensorTypesLabel = eval.triggeredSensors.joinToString(", "),
            sensorValuesLabel = eval.formattedValues,
            roadReference = eval.roadReference,
            edgeId = eval.affectedEdgeId,
            status = "ACTIVE",
            roadStatus = eval.roadStatus.name,
            dataSource = packet.dataSource.dbValue,
            verificationStatus = "VERIFIED"
        )
        dao.upsertHazard(hazardEntity)

        val roadStatusEntity = RoadStatusEntity(
            roadReference = eval.roadReference,
            edgeId = eval.affectedEdgeId,
            status = eval.roadStatus.name,
            hazardId = hazardId,
            source = packet.deviceId,
            timestamp = packet.epochMillis,
            reason = eval.description,
            expiry = packet.epochMillis + 3600_000L,
            verificationStatus = "VERIFIED",
            dataSource = packet.dataSource.dbValue
        )
        dao.upsertRoadStatus(roadStatusEntity)

        // If Combined Hazard or Bridge Structural Warning in Virtual Mode, also include Road Construction on H-I and Water Warning on D-K
        if (packet.dataSource == DataSource.VIRTUAL_TEST &&
            (_selectedScenario.value == VirtualScenario.COMBINED_HAZARD ||
                _selectedScenario.value == VirtualScenario.BRIDGE_STRUCTURAL_WARNING)
        ) {
            val secondaryHazard = HazardEntity(
                hazardId = "HZ-VIRT-H-I",
                source = packet.deviceId,
                timestamp = packet.epochMillis,
                timestampFormatted = dateStr,
                latitude = packet.latitude - 0.006,
                longitude = packet.longitude + 0.005,
                hazardType = HazardType.ROAD_CONSTRUCTION.code,
                hazardTitle = HazardType.ROAD_CONSTRUCTION.displayTitle,
                severity = HazardSeverity.WARNING.name,
                sensorId = packet.deviceId,
                sensorTypesLabel = "Road Work Zone",
                sensorValuesLabel = "Active Lane Restriction",
                roadReference = "Road Construction (H - I)",
                edgeId = "H-I",
                status = "ACTIVE",
                roadStatus = RoadStatusType.RESTRICTED.name,
                dataSource = DataSource.VIRTUAL_TEST.dbValue,
                verificationStatus = "VERIFIED"
            )
            dao.upsertHazard(secondaryHazard)
            dao.upsertRoadStatus(
                RoadStatusEntity(
                    roadReference = "Road Construction (H - I)",
                    edgeId = "H-I",
                    status = RoadStatusType.RESTRICTED.name,
                    hazardId = "HZ-VIRT-H-I",
                    source = packet.deviceId,
                    timestamp = packet.epochMillis,
                    reason = "Road Construction (Warning)",
                    expiry = packet.epochMillis + 3600_000L,
                    verificationStatus = "VERIFIED",
                    dataSource = DataSource.VIRTUAL_TEST.dbValue
                )
            )

            val tertiaryHazard = HazardEntity(
                hazardId = "HZ-VIRT-D-K",
                source = packet.deviceId,
                timestamp = packet.epochMillis,
                timestampFormatted = dateStr,
                latitude = packet.latitude - 0.004,
                longitude = packet.longitude - 0.003,
                hazardType = HazardType.HIGH_WATER_LEVEL.code,
                hazardTitle = HazardType.HIGH_WATER_LEVEL.displayTitle,
                severity = HazardSeverity.WARNING.name,
                sensorId = packet.deviceId,
                sensorTypesLabel = "Water Level Gauge",
                sensorValuesLabel = "25 cm (Warning)",
                roadReference = "Causeway Link (D - K)",
                edgeId = "D-K",
                status = "ACTIVE",
                roadStatus = RoadStatusType.WARNING.name,
                dataSource = DataSource.VIRTUAL_TEST.dbValue,
                verificationStatus = "VERIFIED"
            )
            dao.upsertHazard(tertiaryHazard)
            dao.upsertRoadStatus(
                RoadStatusEntity(
                    roadReference = "Causeway Link (D - K)",
                    edgeId = "D-K",
                    status = RoadStatusType.WARNING.name,
                    hazardId = "HZ-VIRT-D-K",
                    source = packet.deviceId,
                    timestamp = packet.epochMillis,
                    reason = "Water Level 25 cm (Warning)",
                    expiry = packet.epochMillis + 3600_000L,
                    verificationStatus = "VERIFIED",
                    dataSource = DataSource.VIRTUAL_TEST.dbValue
                )
            )
        }

        // Only trigger a new notification & history event when severity or hazard type changes
        val isNewEscalation = previousEval == null ||
            !previousEval.hazardDetected ||
            previousEval.severity != eval.severity ||
            previousEval.hazardType != eval.hazardType

        if (isNewEscalation) {
            dao.insertHistoryEvent(
                HistoryEventEntity(
                    timestamp = packet.epochMillis,
                    formattedDate = shortDate,
                    category = "HAZARD_EVENT",
                    eventType = eval.hazardType.code,
                    location = eval.roadReference,
                    source = if (packet.dataSource == DataSource.LIVE_HARDWARE) packet.deviceId else "Virtual Test",
                    mode = modeLabel,
                    status = eval.severity.name,
                    dataSource = packet.dataSource.dbValue,
                    details = "${eval.description} (${eval.formattedValues})"
                )
            )

            val activeMode = _sensorMode.value
            val matchesActiveMode = (activeMode == SensorMode.EXTERNAL_HARDWARE && packet.dataSource == DataSource.LIVE_HARDWARE) ||
                (activeMode == SensorMode.VIRTUAL && packet.dataSource == DataSource.VIRTUAL_TEST)

            if (matchesActiveMode) {
                RoutPilotNotificationHelper.sendHazardNotification(
                    context = appContext,
                    sensorMode = activeMode,
                    hazardTitle = eval.hazardType.displayTitle,
                    roadReference = eval.roadReference,
                    severityLabel = eval.severity.name
                )
                triggerAutomaticRouteDiversion(eval)
            }
        }

        rebuildGraphAndRoutes()
    }

    private fun triggerAutomaticRouteDiversion(eval: HazardEvaluation) {
        viewModelScope.launch {
            val prevRoute = _initialOptimalRouteResult.value ?: _activeRouteResult.value
            val prevPathLabel = prevRoute?.nodePath?.joinToString(" → ") ?: "A → B → C → F → N → T"
            _diversionAlert.value = DiversionAlertState(
                isActive = true,
                stage = "ROUTE AFFECTED",
                affectedRoad = eval.roadReference,
                reason = eval.description,
                originalPathLabel = prevPathLabel,
                alternatePathLabel = "Calculating...",
                dataSource = eval.dataSource
            )
            delay(350L)
            _diversionAlert.value = _diversionAlert.value.copy(stage = "CHECKING ALTERNATIVE...")
            rebuildGraphAndRoutes()
            delay(350L)
            val newRoute = _safeOptimalRoute1.value ?: _activeRouteResult.value
            if (newRoute != null) {
                _diversionAlert.value = _diversionAlert.value.copy(
                    stage = "ALTERNATE ROUTE FOUND",
                    alternatePathLabel = newRoute.nodePath.joinToString(" → "),
                    newDistanceKm = newRoute.distanceKm,
                    newTimeMin = newRoute.estimatedTimeMin
                )
            }
        }
    }

    private fun appendToSparkline(
        current: Map<String, List<Float>>,
        packet: SensorPacket
    ): Map<String, List<Float>> {
        fun addPt(key: String, value: Double): List<Float> {
            val existing = current[key] ?: emptyList()
            return (existing + value.toFloat()).takeLast(24)
        }
        return mapOf(
            "vibration" to addPt("vibration", packet.vibration),
            "tilt" to addPt("tilt", packet.tilt),
            "strain" to addPt("strain", packet.strain),
            "displacement" to addPt("displacement", packet.displacement),
            "waterLevel" to addPt("waterLevel", packet.waterLevel)
        )
    }

    // Route & Graph Controls
    fun setSourceNode(nodeId: String) {
        _sourceNodeId.value = nodeId
        _hazardRouteNotification.value = _hazardRouteNotification.value.copy(
            isVisible = false,
            routeSwitchedConfirmation = null
        )
        rebuildGraphAndRoutes(showInitialOptimalFirst = true)
    }

    fun setDestinationNode(nodeId: String) {
        _destinationNodeId.value = nodeId
        _selectedCustomPlace.value = null
        _hazardRouteNotification.value = _hazardRouteNotification.value.copy(
            isVisible = false,
            routeSwitchedConfirmation = null
        )
        rebuildGraphAndRoutes(showInitialOptimalFirst = true)
    }

    fun setVehicleType(vehicle: VehicleType) {
        _selectedVehicle.value = vehicle
        rebuildGraphAndRoutes()
    }

    fun setRoutingAlgorithm(algorithm: RoutingAlgorithm) {
        _selectedAlgorithm.value = algorithm
        rebuildGraphAndRoutes()
    }

    fun updateRouteWeights(newWeights: RouteCostWeights) {
        _routeWeights.value = newWeights
        rebuildGraphAndRoutes()
    }

    fun updatePlaceSearchQuery(query: String) {
        _placeSearchQuery.value = query
        if (query.length >= 3) {
            searchRealPlaces(query)
        } else if (query.isBlank()) {
            _placeSearchResults.value = emptyList()
        }
    }

    fun searchRealPlaces(query: String = _placeSearchQuery.value) {
        if (query.isBlank()) return
        viewModelScope.launch {
            _isSearchingPlaces.value = true
            val results = networkProviders.searchPlaces(query, _connectivity.value.fastApiUrl)
            _placeSearchResults.value = results
            _isSearchingPlaces.value = false
        }
    }

    fun selectSearchedPlace(place: PlaceSearchResult) {
        _selectedCustomPlace.value = place
        _placeSearchQuery.value = place.shortName
        _placeSearchResults.value = emptyList()
        place.mappedNodeId?.let { mappedId ->
            if (_graphNodes.value.any { it.id == mappedId }) {
                _destinationNodeId.value = mappedId
            }
        }
        _hazardRouteNotification.value = _hazardRouteNotification.value.copy(
            isVisible = false,
            routeSwitchedConfirmation = null
        )
        rebuildGraphAndRoutes(recordInDb = true, showInitialOptimalFirst = true)
    }

    fun calculateSelectedRoute(recordInDb: Boolean = true) {
        _hazardRouteNotification.value = _hazardRouteNotification.value.copy(
            isVisible = false,
            routeSwitchedConfirmation = null
        )
        rebuildGraphAndRoutes(recordInDb = recordInDb, showInitialOptimalFirst = true)
    }

    fun selectOptimalRouteForGraph(route: AlgorithmRouteResult) {
        val prev = _activeRouteResult.value
        _activeRouteResult.value = route
        if (prev != null && prev.nodePath != route.nodePath) {
            _alternateRouteResult.value = prev
        }
        _journeyNodeIndex.value = 0
    }

    /**
     * Called when the user selects/confirms the initial optimal path and starts moving forward.
     * Moves the vehicle along the chosen path, then immediately triggers the APK & Android System
     * Hazard Notification when a bridge or road hazard lies ahead, offering Other Optimal Safe Routes.
     */
    fun chooseOptimalPathAndStartMoving() {
        viewModelScope.launch {
            val chosenRoute = _activeRouteResult.value ?: _initialOptimalRouteResult.value
            if (chosenRoute != null) {
                _activeRouteResult.value = chosenRoute
            }
            _journeyNodeIndex.value = 0
            _journeyStatus.value = "Moving"
            _hazardRouteNotification.value = _hazardRouteNotification.value.copy(
                isVisible = false,
                routeSwitchedConfirmation = null
            )
            navigateTo(AppScreen.LIVE_MAP)

            // Simulate vehicle moving forward along the user's chosen optimal path
            delay(1100L)
            val pathSize = _activeRouteResult.value?.nodePath?.size ?: 1
            if (pathSize > 1) {
                _journeyNodeIndex.value = 1
            }
            // Trigger the Hazard Notification showing exact location, bridge/road hazard type, and other optimal routes
            triggerHazardNotificationOnRoute()
        }
    }

    /**
     * Fires both the Android System Notification and the rich In-App APK Hazard Notification Modal
     * detailing the exact location, bridge/road segment, hazard type, and alternative optimal safe routes.
     */
    fun triggerHazardNotificationOnRoute() {
        val nodes = _graphNodes.value
        val chosenRoute = _initialOptimalRouteResult.value ?: _activeRouteResult.value
        val chosenPathNodes = chosenRoute?.nodePath ?: listOf("A", "B", "C", "F", "N", "T")
        val chosenPathLabel = chosenPathNodes.joinToString(" → ")

        val crossesBridgeB1 = chosenPathNodes.zipWithNext().any { (u, v) ->
            (u == "B" && v == "C") || (u == "C" && v == "B")
        }
        val crossesRoadHI = chosenPathNodes.zipWithNext().any { (u, v) ->
            (u == "H" && v == "I") || (u == "I" && v == "H")
        }

        val bNode = nodes.find { it.id == "B" }
        val cNode = nodes.find { it.id == "C" }
        val hNode = nodes.find { it.id == "H" }
        val iNode = nodes.find { it.id == "I" }

        val (locationLabel, coordsLabel, infraType, hazardSummary, severity) = when {
            crossesBridgeB1 -> {
                val lat = if (bNode != null && cNode != null) (bNode.latitude + cNode.latitude) / 2.0 else 28.6403
                val lon = if (bNode != null && cNode != null) (bNode.longitude + cNode.longitude) / 2.0 else 77.1995
                NTuple5(
                    "Bridge B1 (Main Span, Segment B → C)",
                    String.format(Locale.US, "Lat %.4f° N, Lon %.4f° E", lat, lon),
                    "BRIDGE HAZARD (Bridge B1 — Segment B → C)",
                    "Bridge Structural Critical: High Vibration (0.85 g), Abnormal Pier Tilt (4.2°), High Strain (320 µε) — Bridge B1 Blocked",
                    HazardSeverity.CRITICAL
                )
            }
            crossesRoadHI -> {
                val lat = if (hNode != null && iNode != null) (hNode.latitude + iNode.latitude) / 2.0 else 28.6092
                val lon = if (hNode != null && iNode != null) (hNode.longitude + iNode.longitude) / 2.0 else 77.2148
                NTuple5(
                    "Civic Corridor Road (Segment H → I) & Bridge B1 (Segment B → C)",
                    String.format(Locale.US, "Lat %.4f° N, Lon %.4f° E", lat, lon),
                    "ROAD & BRIDGE HAZARD AHEAD (Link H → I & Bridge B1)",
                    "Road Construction & Lane Closure on Road H → I (Warning / Restricted) + Critical Structural Vibration (0.85 g) on Bridge B1 (B → C)",
                    HazardSeverity.CRITICAL
                )
            }
            else -> {
                val lat = if (bNode != null && cNode != null) (bNode.latitude + cNode.latitude) / 2.0 else 28.6403
                val lon = if (bNode != null && cNode != null) (bNode.longitude + cNode.longitude) / 2.0 else 77.1995
                NTuple5(
                    "Bridge B1 (Segment B → C) & Civic Corridor Road (Segment H → I)",
                    String.format(Locale.US, "Lat %.4f° N, Lon %.4f° E", lat, lon),
                    "BRIDGE & ROAD HAZARD ON CORRIDOR",
                    "Bridge B1 (B → C): High Structural Vibration (0.85 g) & Tilt (4.2°) [BLOCKED] • Road H → I: Active Road Construction [RESTRICTED]",
                    HazardSeverity.CRITICAL
                )
            }
        }

        val activeMode = _sensorMode.value
        val ds = if (activeMode == SensorMode.EXTERNAL_HARDWARE) DataSource.LIVE_HARDWARE else DataSource.VIRTUAL_TEST

        // 1. Send Real Android Status-Bar Notification
        RoutPilotNotificationHelper.sendHazardNotification(
            context = appContext,
            sensorMode = activeMode,
            hazardTitle = "$infraType — $hazardSummary",
            roadReference = "$locationLabel ($coordsLabel)",
            severityLabel = severity.name
        )

        // 2. Show In-App APK Hazard Notification with Other Optimal Route Choices
        _hazardRouteNotification.value = HazardRouteNotificationState(
            isVisible = true,
            notificationTitle = "Hazard Detected Ahead on Your Route!",
            locationLabel = locationLabel,
            coordinatesLabel = coordsLabel,
            infrastructureType = infraType,
            hazardTypeSummary = hazardSummary,
            severity = severity,
            affectedChosenPathLabel = chosenPathLabel,
            primarySafeRoute = _safeOptimalRoute1.value,
            secondarySafeRoute = _safeOptimalRoute2.value,
            dataSource = ds,
            routeSwitchedConfirmation = null
        )
    }

    /**
     * Called when the user selects one of the Other Optimal Safe Routes from the Hazard Notification.
     */
    fun selectOtherOptimalSafeRoute(chosenRoute: AlgorithmRouteResult) {
        val prevAffected = _initialOptimalRouteResult.value ?: _activeRouteResult.value
        _activeRouteResult.value = chosenRoute
        if (prevAffected != null && prevAffected.nodePath != chosenRoute.nodePath) {
            _alternateRouteResult.value = prevAffected
        }
        _journeyNodeIndex.value = 1.coerceAtMost((chosenRoute.nodePath.size - 1).coerceAtLeast(0))
        _journeyStatus.value = "Moving"

        val pathText = chosenRoute.nodePath.joinToString(" → ")
        _hazardRouteNotification.value = _hazardRouteNotification.value.copy(
            isVisible = false,
            routeSwitchedConfirmation = "Optimal Safe Route Selected: $pathText (${chosenRoute.distanceKm} km • ${chosenRoute.estimatedTimeMin.toInt()} min • 0 Hazards)"
        )
        _diversionAlert.value = DiversionAlertState(
            isActive = true,
            stage = "ALTERNATE ROUTE FOUND",
            affectedRoad = _hazardRouteNotification.value.locationLabel,
            reason = _hazardRouteNotification.value.hazardTypeSummary,
            originalPathLabel = prevAffected?.nodePath?.joinToString(" → ") ?: "",
            alternatePathLabel = pathText,
            newDistanceKm = chosenRoute.distanceKm,
            newTimeMin = chosenRoute.estimatedTimeMin,
            dataSource = _hazardRouteNotification.value.dataSource
        )

        viewModelScope.launch {
            recordHistoryEvent(
                category = "ROUTE_DIVERSION",
                eventType = "USER_SELECTED_SAFE_OPTIMAL_ROUTE",
                location = _hazardRouteNotification.value.locationLabel,
                source = chosenRoute.algorithm.displayName,
                mode = _sensorMode.value.displayTitle,
                status = "REROUTED",
                dataSource = DataSource.CALCULATED,
                details = "Switched from ${_hazardRouteNotification.value.affectedChosenPathLabel} to Safe Optimal Route: $pathText"
            )
        }
    }

    fun dismissHazardRouteNotification() {
        _hazardRouteNotification.value = _hazardRouteNotification.value.copy(isVisible = false)
    }

    private data class NTuple5<A, B, C, D, E>(
        val first: A,
        val second: B,
        val third: C,
        val fourth: D,
        val fifth: E
    )

    private fun rebuildGraphAndRoutes(
        recordInDb: Boolean = false,
        showInitialOptimalFirst: Boolean = false
    ) {
        val anchorLat = gpsState.value.latitude ?: 28.6139
        val anchorLon = gpsState.value.longitude ?: 77.2090

        val activeEval = if (_sensorMode.value == SensorMode.EXTERNAL_HARDWARE) {
            _hardwareHazardEval.value
        } else {
            _virtualHazardEval.value
        }

        val overrides = mutableMapOf<String, RoutingAlgorithms.RoadStatusInfo>()
        if (activeEval != null && activeEval.hazardDetected) {
            overrides[activeEval.affectedEdgeId] = RoutingAlgorithms.RoadStatusInfo(
                status = activeEval.roadStatus,
                severity = activeEval.severity,
                hazardId = "HZ-${activeEval.affectedEdgeId}",
                reason = activeEval.description,
                dataSource = activeEval.dataSource
            )
        }
        if (_sensorMode.value == SensorMode.VIRTUAL &&
            (_selectedScenario.value == VirtualScenario.BRIDGE_STRUCTURAL_WARNING ||
                _selectedScenario.value == VirtualScenario.COMBINED_HAZARD)
        ) {
            // 1 Blocked (B-C), 1 Restricted (H-I), 2 Warning (D-K, C-F) => 24 Safe Roads out of 28 Links
            overrides["B-C"] = RoutingAlgorithms.RoadStatusInfo(
                status = RoadStatusType.BLOCKED,
                severity = HazardSeverity.CRITICAL,
                hazardId = "HZ-VIRT-B-C",
                reason = "Bridge B1 (Critical)",
                dataSource = DataSource.VIRTUAL_TEST
            )
            overrides["H-I"] = RoutingAlgorithms.RoadStatusInfo(
                status = RoadStatusType.RESTRICTED,
                severity = HazardSeverity.WARNING,
                hazardId = "HZ-VIRT-H-I",
                reason = "Road Construction (Warning)",
                dataSource = DataSource.VIRTUAL_TEST
            )
            overrides["D-K"] = RoutingAlgorithms.RoadStatusInfo(
                status = RoadStatusType.WARNING,
                severity = HazardSeverity.WARNING,
                hazardId = "HZ-VIRT-D-K",
                reason = "Water Level Warning (25 cm)",
                dataSource = DataSource.VIRTUAL_TEST
            )
            overrides["C-F"] = RoutingAlgorithms.RoadStatusInfo(
                status = RoadStatusType.WARNING,
                severity = HazardSeverity.WARNING,
                hazardId = "HZ-VIRT-C-F",
                reason = "Bridge Approach Warning",
                dataSource = DataSource.VIRTUAL_TEST
            )
        }

        val (nodes, edges) = RoutingAlgorithms.buildMonitoredGraph(
            anchorLat = anchorLat,
            anchorLon = anchorLon,
            roadStatusOverrides = overrides
        )
        _graphNodes.value = nodes
        _graphEdges.value = edges

        // 1. Compute unobstructed Initial Optimal Route (before hazard diversion)
        val (cleanNodes, cleanEdges) = RoutingAlgorithms.buildMonitoredGraph(anchorLat, anchorLon, emptyMap())
        val baselineRoute = RoutingAlgorithms.calculateRouteAStar(
            nodes = cleanNodes,
            edges = cleanEdges,
            startNodeId = _sourceNodeId.value,
            goalNodeId = _destinationNodeId.value,
            vehicleType = _selectedVehicle.value,
            weights = _routeWeights.value
        )
        _initialOptimalRouteResult.value = baselineRoute

        // 2. Compute Primary Safe Optimal Route (avoiding all blocked/restricted hazards)
        val strictSafeOverrides = overrides.toMutableMap()
        if (strictSafeOverrides.containsKey("H-I")) {
            strictSafeOverrides["H-I"] = RoutingAlgorithms.RoadStatusInfo(
                status = RoadStatusType.BLOCKED,
                severity = HazardSeverity.WARNING,
                hazardId = "HZ-SAFE-H-I",
                reason = "Avoid Road Construction (H-I)",
                dataSource = DataSource.CALCULATED
            )
        }
        val (safeNodes1, safeEdges1) = RoutingAlgorithms.buildMonitoredGraph(
            anchorLat = anchorLat,
            anchorLon = anchorLon,
            roadStatusOverrides = strictSafeOverrides
        )
        val comparison = RoutingAlgorithms.compareAlgorithms(
            nodes = safeNodes1,
            edges = safeEdges1,
            startNodeId = _sourceNodeId.value,
            goalNodeId = _destinationNodeId.value,
            vehicleType = _selectedVehicle.value,
            weights = _routeWeights.value,
            matlabConnected = _connectivity.value.matlabConnected
        )
        _algorithmComparison.value = comparison

        val primarySafe = if (_selectedAlgorithm.value == RoutingAlgorithm.ASTAR) {
            comparison.aStarResult
        } else {
            comparison.dijkstraResult
        }
        _safeOptimalRoute1.value = primarySafe

        // 3. Compute Secondary Safe Optimal Route (a distinct 2nd safe route option for the user)
        val secondaryOverrides = strictSafeOverrides.toMutableMap()
        val middleEdgeToExclude = primarySafe?.nodePath?.zipWithNext()?.firstOrNull { (u, v) ->
            "$u-$v" !in setOf("M-T", "T-M")
        }?.let { (u, v) -> "$u-$v" }
        if (middleEdgeToExclude != null) {
            secondaryOverrides[middleEdgeToExclude] = RoutingAlgorithms.RoadStatusInfo(
                status = RoadStatusType.BLOCKED,
                severity = HazardSeverity.WARNING,
                hazardId = "ALT-BRANCH",
                reason = "Secondary Route Branch",
                dataSource = DataSource.CALCULATED
            )
            secondaryOverrides[middleEdgeToExclude.split("-").reversed().joinToString("-")] =
                secondaryOverrides[middleEdgeToExclude]!!
        }
        val (safeNodes2, safeEdges2) = RoutingAlgorithms.buildMonitoredGraph(
            anchorLat = anchorLat,
            anchorLon = anchorLon,
            roadStatusOverrides = secondaryOverrides
        )
        val secondarySafe = RoutingAlgorithms.calculateRouteDijkstra(
            nodes = safeNodes2,
            edges = safeEdges2,
            startNodeId = _sourceNodeId.value,
            goalNodeId = _destinationNodeId.value,
            vehicleType = _selectedVehicle.value,
            weights = _routeWeights.value
        )
        _safeOptimalRoute2.value = secondarySafe

        if (showInitialOptimalFirst && baselineRoute != null) {
            _activeRouteResult.value = baselineRoute
            _alternateRouteResult.value = primarySafe
        } else {
            _activeRouteResult.value = primarySafe ?: baselineRoute
            _alternateRouteResult.value = baselineRoute
        }

        // Fetch live OSRM road geometry in background for real road network alignment
        viewModelScope.launch {
            val startNode = nodes.find { it.id == _sourceNodeId.value } ?: nodes.first()
            val customDest = _selectedCustomPlace.value
            val destLat = customDest?.latitude ?: (nodes.find { it.id == _destinationNodeId.value }?.latitude ?: nodes.last().latitude)
            val destLon = customDest?.longitude ?: (nodes.find { it.id == _destinationNodeId.value }?.longitude ?: nodes.last().longitude)

            val osrmRes = networkProviders.routingProvider.calculateRoute(
                sourceLat = startNode.latitude,
                sourceLon = startNode.longitude,
                destLat = destLat,
                destLon = destLon,
                vehicleType = _selectedVehicle.value
            )
            if (osrmRes != null) {
                _externalOsrmRoute.value = osrmRes
            }

            val routeToRecord = _activeRouteResult.value
            if (recordInDb && routeToRecord != null) {
                val now = System.currentTimeMillis()
                val destName = customDest?.shortName ?: (nodes.find { it.id == _destinationNodeId.value }?.name ?: _destinationNodeId.value)
                dao.insertRouteRequest(
                    RouteRequestEntity(
                        timestamp = now,
                        sourceLabel = startNode.name,
                        sourceLat = startNode.latitude,
                        sourceLon = startNode.longitude,
                        destinationLabel = destName,
                        destinationLat = destLat,
                        destinationLon = destLon,
                        vehicleType = _selectedVehicle.value.label,
                        algorithm = _selectedAlgorithm.value.displayName,
                        pathSummary = routeToRecord.nodePath.joinToString(" → "),
                        distanceKm = routeToRecord.distanceKm,
                        durationMin = routeToRecord.estimatedTimeMin,
                        totalCost = routeToRecord.totalCost,
                        sensorMode = _sensorMode.value.displayTitle,
                        dataSource = DataSource.CALCULATED.dbValue
                    )
                )
                recordHistoryEvent(
                    category = "ROUTE_REQUEST",
                    eventType = "${_selectedAlgorithm.value.code}_ROUTE",
                    location = "${startNode.id} → ${_destinationNodeId.value}",
                    source = routeToRecord.providerName,
                    mode = _sensorMode.value.displayTitle,
                    status = if (routeToRecord.isDiverted) "DIVERTED" else "OPTIMAL",
                    dataSource = DataSource.CALCULATED,
                    details = "${routeToRecord.distanceKm} km, ${routeToRecord.estimatedTimeMin} min, Cost ${routeToRecord.totalCost}"
                )
            }
        }
    }

    // Journey Controls
    fun pauseJourney() {
        _journeyStatus.value = if (_journeyStatus.value == "Paused") "Moving" else "Paused"
    }

    fun stopJourney() {
        _journeyStatus.value = "Stopped"
        _journeyNodeIndex.value = 0
        viewModelScope.launch {
            val route = _activeRouteResult.value
            dao.upsertJourney(
                JourneyEntity(
                    journeyId = "JRN-${System.currentTimeMillis()}",
                    startedAt = System.currentTimeMillis() - 600_000L,
                    endedAt = System.currentTimeMillis(),
                    sourceLabel = _sourceNodeId.value,
                    destinationLabel = _destinationNodeId.value,
                    vehicleType = _selectedVehicle.value.label,
                    currentPath = route?.nodePath?.joinToString(" → ") ?: "",
                    distanceRemainingKm = route?.distanceKm ?: 0.0,
                    etaMinutes = route?.estimatedTimeMin ?: 0.0,
                    status = "STOPPED",
                    sensorMode = _sensorMode.value.displayTitle
                )
            )
        }
    }

    fun stepJourneyForward() {
        val currentRoute = _activeRouteResult.value
        val pathNodes = currentRoute?.nodePath ?: emptyList()
        val pathSize = pathNodes.size.coerceAtLeast(1)
        if (pathSize > 1) {
            val nextIdx = (_journeyNodeIndex.value + 1) % pathSize
            _journeyNodeIndex.value = nextIdx
            _journeyStatus.value = "Moving"

            // Check if the segment ahead has a road or bridge hazard
            val currNode = pathNodes.getOrNull(nextIdx)
            val nextNode = pathNodes.getOrNull(nextIdx + 1)
            val hitsHazardousEdge = if (currNode != null && nextNode != null) {
                _graphEdges.value.any { edge ->
                    ((edge.fromNodeId == currNode && edge.toNodeId == nextNode) ||
                        (edge.fromNodeId == nextNode && edge.toNodeId == currNode)) &&
                        edge.status != RoadStatusType.OPEN
                }
            } else {
                false
            }
            if (hitsHazardousEdge || (_hazardRouteNotification.value.routeSwitchedConfirmation == null && nextIdx == 1)) {
                triggerHazardNotificationOnRoute()
            }
        }
    }

    private suspend fun recordHistoryEvent(
        category: String,
        eventType: String,
        location: String,
        source: String,
        mode: String,
        status: String,
        dataSource: DataSource,
        details: String
    ) {
        val now = System.currentTimeMillis()
        val shortDate = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.US).format(Date(now))
        dao.insertHistoryEvent(
            HistoryEventEntity(
                timestamp = now,
                formattedDate = shortDate,
                category = category,
                eventType = eventType,
                location = location,
                source = source,
                mode = mode,
                status = status,
                dataSource = dataSource.dbValue,
                details = details
            )
        )
    }

    override fun onCleared() {
        super.onCleared()
        locationTracker.stopTracking()
        networkProviders.disconnectEsp32WebSocket()
    }
}

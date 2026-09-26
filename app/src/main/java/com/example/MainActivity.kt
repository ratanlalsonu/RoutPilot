package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.AppRole
import com.example.model.AppScreen
import com.example.model.SensorMode
import com.example.ui.components.AutomaticDiversionBanner
import com.example.ui.components.HazardRouteNotificationModal
import com.example.ui.components.ModeSwitchConfirmDialog
import com.example.ui.components.PersistentModeBanner
import com.example.ui.components.RoutPilotBottomNavBar
import com.example.ui.components.RoutPilotTopBar
import com.example.ui.components.RouteSwitchedConfirmationBanner
import com.example.ui.screens.AdminControlPanelScreen
import com.example.ui.screens.AdminLoginDialog
import com.example.ui.screens.AlgorithmComparisonScreen
import com.example.ui.screens.CitizenHazardReportDialog
import com.example.ui.screens.ConnectivityAndDataSourcesScreen
import com.example.ui.screens.HazardDetectionScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeDashboardScreen
import com.example.ui.screens.JourneyTrackingScreen
import com.example.ui.screens.LiveMapScreen
import com.example.ui.screens.RoutePlannerScreen
import com.example.ui.screens.SensorMonitoringScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.TestScenariosScreen
import com.example.ui.theme.RoutPilotTheme
import com.example.ui.theme.RpPrimaryBlue
import com.example.ui.theme.RpPurpleAccent
import com.example.ui.viewmodel.RoutPilotViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: RoutPilotViewModel = viewModel()
            val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()

            RoutPilotTheme(darkTheme = isDarkTheme) {
                RoutPilotRootApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun RoutPilotRootApp(viewModel: RoutPilotViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val isFirstLaunch by viewModel.isFirstLaunch.collectAsStateWithLifecycle()
    val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()
    val activeRole by viewModel.activeRole.collectAsStateWithLifecycle()
    val showAdminLoginModal by viewModel.showAdminLoginModal.collectAsStateWithLifecycle()
    val adminEmergencyBroadcast by viewModel.adminEmergencyBroadcast.collectAsStateWithLifecycle()
    val citizenHazardReports by viewModel.citizenHazardReports.collectAsStateWithLifecycle()

    val sensorMode by viewModel.sensorMode.collectAsStateWithLifecycle()
    val pendingModeSwitch by viewModel.pendingModeSwitch.collectAsStateWithLifecycle()

    val connectivity by viewModel.connectivity.collectAsStateWithLifecycle()
    val gpsState by viewModel.gpsState.collectAsStateWithLifecycle()
    val hardwareConfig by viewModel.hardwareConfig.collectAsStateWithLifecycle()
    val hardwareTestResult by viewModel.hardwareTestResult.collectAsStateWithLifecycle()

    val liveHardwarePacket by viewModel.liveHardwarePacket.collectAsStateWithLifecycle()
    val virtualTestPacket by viewModel.virtualTestPacket.collectAsStateWithLifecycle()
    val virtualEngineState by viewModel.virtualEngineState.collectAsStateWithLifecycle()
    val selectedScenario by viewModel.selectedScenario.collectAsStateWithLifecycle()

    val hardwareSeries by viewModel.hardwareSeries.collectAsStateWithLifecycle()
    val virtualSeries by viewModel.virtualSeries.collectAsStateWithLifecycle()

    val hardwareHazardEval by viewModel.hardwareHazardEval.collectAsStateWithLifecycle()
    val virtualHazardEval by viewModel.virtualHazardEval.collectAsStateWithLifecycle()
    val activeModeHazards by viewModel.activeModeHazards.collectAsStateWithLifecycle()

    val graphNodes by viewModel.graphNodes.collectAsStateWithLifecycle()
    val graphEdges by viewModel.graphEdges.collectAsStateWithLifecycle()
    val activeRouteResult by viewModel.activeRouteResult.collectAsStateWithLifecycle()
    val alternateRouteResult by viewModel.alternateRouteResult.collectAsStateWithLifecycle()
    val initialOptimalRouteResult by viewModel.initialOptimalRouteResult.collectAsStateWithLifecycle()
    val safeOptimalRoute1 by viewModel.safeOptimalRoute1.collectAsStateWithLifecycle()
    val safeOptimalRoute2 by viewModel.safeOptimalRoute2.collectAsStateWithLifecycle()
    val algorithmComparison by viewModel.algorithmComparison.collectAsStateWithLifecycle()
    val externalOsrmRoute by viewModel.externalOsrmRoute.collectAsStateWithLifecycle()

    val sourceNodeId by viewModel.sourceNodeId.collectAsStateWithLifecycle()
    val destinationNodeId by viewModel.destinationNodeId.collectAsStateWithLifecycle()
    val selectedVehicle by viewModel.selectedVehicle.collectAsStateWithLifecycle()
    val selectedAlgorithm by viewModel.selectedAlgorithm.collectAsStateWithLifecycle()
    val routeWeights by viewModel.routeWeights.collectAsStateWithLifecycle()

    val placeSearchQuery by viewModel.placeSearchQuery.collectAsStateWithLifecycle()
    val placeSearchResults by viewModel.placeSearchResults.collectAsStateWithLifecycle()
    val isSearchingPlaces by viewModel.isSearchingPlaces.collectAsStateWithLifecycle()
    val selectedCustomPlace by viewModel.selectedCustomPlace.collectAsStateWithLifecycle()

    val diversionAlert by viewModel.diversionAlert.collectAsStateWithLifecycle()
    val hazardRouteNotification by viewModel.hazardRouteNotification.collectAsStateWithLifecycle()
    val journeyStatus by viewModel.journeyStatus.collectAsStateWithLifecycle()
    val journeyNodeIndex by viewModel.journeyNodeIndex.collectAsStateWithLifecycle()
    val historyEvents by viewModel.historyEvents.collectAsStateWithLifecycle()
    val matlabSignalAnalysis by viewModel.matlabSignalAnalysis.collectAsStateWithLifecycle()

    var showCitizenReportDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.locationTracker.startTracking()
    }

    LaunchedEffect(Unit) {
        val perms = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(perms.toTypedArray())
    }

    val isRootScreen = currentScreen == AppScreen.SPLASH ||
        currentScreen == AppScreen.HOME ||
        currentScreen == AppScreen.ADMIN_DASHBOARD

    BackHandler(enabled = !isRootScreen) {
        viewModel.navigateBack()
    }

    // Admin Authority Login Modal
    AdminLoginDialog(
        visible = showAdminLoginModal,
        onAuthenticate = { adminId, pin -> viewModel.authenticateAdmin(adminId, pin) },
        onQuickDemoLogin = { viewModel.quickDemoAdminLogin() },
        onDismiss = { viewModel.dismissAdminLoginModal() }
    )

    // Citizen Hazard Reporting Modal (From User Panel -> Admin Panel)
    CitizenHazardReportDialog(
        visible = showCitizenReportDialog,
        edges = graphEdges,
        onSubmitReport = { edgeId, roadName, hazardType, severity, description ->
            viewModel.submitCitizenHazardReport(edgeId, roadName, hazardType, severity, description)
        },
        onDismiss = { showCitizenReportDialog = false }
    )

    // Mode Switch Safety Confirmation Dialog
    ModeSwitchConfirmDialog(
        targetMode = pendingModeSwitch,
        onConfirm = { viewModel.confirmModeSwitch() },
        onDismiss = { viewModel.cancelModeSwitch() }
    )

    val isAdminRole = activeRole == AppRole.ADMIN_PANEL

    // In-App APK Hazard Notification Modal with Other Optimal Safe Route Options
    HazardRouteNotificationModal(
        state = hazardRouteNotification,
        isAdminMode = isAdminRole,
        onSelectSafeRoute = { chosenSafeRoute ->
            viewModel.selectOtherOptimalSafeRoute(chosenSafeRoute)
        },
        onDismiss = {
            viewModel.dismissHazardRouteNotification()
        }
    )

    if (currentScreen == AppScreen.SPLASH) {
        SplashScreen(
            isFirstLaunch = isFirstLaunch,
            isDarkTheme = isDarkTheme,
            onToggleTheme = { viewModel.setDarkTheme(it) },
            onGetStarted = { viewModel.completeSplashAndOpenHome() },
            onAdminPortalClick = { viewModel.requestAdminPanelFromSplashOrApp() },
            onSkip = { viewModel.completeSplashAndOpenHome() }
        )
        return
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val drawerItems = if (isAdminRole) {
        listOf(
            Triple("Admin Control Center", Icons.Default.AdminPanelSettings, AppScreen.ADMIN_DASHBOARD),
            Triple("Live Network Map", Icons.Default.Map, AppScreen.LIVE_MAP),
            Triple("Sensor Telemetry (ESP32)", Icons.Default.Sensors, AppScreen.SENSORS),
            Triple("Virtual Test Scenarios", Icons.Default.PlayCircleFilled, AppScreen.TEST_SCENARIOS),
            Triple("A* vs Dijkstra Lab", Icons.Default.CompareArrows, AppScreen.ALGORITHM_COMPARISON),
            Triple("Connectivity & MATLAB", Icons.Default.Wifi, AppScreen.CONNECTIVITY),
            Triple("Data Sources & Storage", Icons.Default.Storage, AppScreen.DATA_SOURCES),
            Triple("Admin Threshold Settings", Icons.Default.Settings, AppScreen.SETTINGS)
        )
    } else {
        listOf(
            Triple("Home", Icons.Default.Home, AppScreen.HOME),
            Triple("Live Map", Icons.Default.Map, AppScreen.LIVE_MAP),
            Triple("Find Route", Icons.Default.AltRoute, AppScreen.ROUTE_PLANNER),
            Triple("Start Navigation", Icons.Default.Navigation, AppScreen.JOURNEY_TRACKING),
            Triple("Road & Bridge Alerts", Icons.Default.Warning, AppScreen.HAZARD_DETECTION),
            Triple("Recent Trips", Icons.Default.History, AppScreen.HISTORY),
            Triple("Settings", Icons.Default.Settings, AppScreen.SETTINGS)
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = currentScreen == AppScreen.HOME || currentScreen == AppScreen.ADMIN_DASHBOARD,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.fillMaxWidth(0.78f)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "RoutePilot",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isAdminRole) RpPurpleAccent else RpPrimaryBlue
                    )
                    Text(
                        text = if (isAdminRole) {
                            "🛡️ Traffic Authority Admin Panel"
                        } else {
                            "Smart Road & Bridge Navigation"
                        },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isAdminRole) RpPurpleAccent else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (isAdminRole) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Mode: ${sensorMode.displayTitle}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                HorizontalDivider()
                Spacer(modifier = Modifier.height(6.dp))
                drawerItems.forEach { (label, icon, screen) ->
                    NavigationDrawerItem(
                        label = { Text(label, fontWeight = FontWeight.SemiBold, fontSize = 13.sp) },
                        selected = currentScreen == screen,
                        onClick = {
                            scope.launch { drawerState.close() }
                            viewModel.navigateTo(screen)
                        },
                        icon = { Icon(icon, contentDescription = label) },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }

                if (isAdminRole) {
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider()
                    Column(modifier = Modifier.padding(12.dp)) {
                        Button(
                            onClick = {
                                scope.launch { drawerState.close() }
                                viewModel.switchToUserPanel()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = RpPrimaryBlue),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Return to Driver App",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    ) {
        val screenTitle = when (currentScreen) {
            AppScreen.HOME -> "RoutePilot"
            AppScreen.ADMIN_DASHBOARD -> "Admin Control Panel"
            AppScreen.LIVE_MAP -> "Live Map"
            AppScreen.ROUTE_PLANNER -> if (isAdminRole) "Route Planner" else "Find Safe Route"
            AppScreen.SENSORS -> "Sensor Monitoring"
            AppScreen.HAZARD_DETECTION -> if (isAdminRole) "Hazard Alerts" else "Road & Bridge Alerts"
            AppScreen.ALGORITHM_COMPARISON -> "A* vs Dijkstra"
            AppScreen.JOURNEY_TRACKING -> if (isAdminRole) "Journey Tracking" else "Live Navigation"
            AppScreen.TEST_SCENARIOS -> "Test Scenarios"
            AppScreen.SETTINGS -> if (isAdminRole) "Admin Settings" else "Settings"
            AppScreen.HISTORY -> if (isAdminRole) "Event History" else "Recent Trips"
            AppScreen.CONNECTIVITY -> "Connectivity & System"
            AppScreen.DATA_SOURCES -> "Data Sources & MATLAB"
            AppScreen.SPLASH -> "RoutePilot"
        }

        val showHamburgerIcon = currentScreen == AppScreen.HOME || currentScreen == AppScreen.ADMIN_DASHBOARD

        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                Column {
                    RoutPilotTopBar(
                        title = screenTitle,
                        isHome = showHamburgerIcon,
                        activeHazardCount = activeModeHazards.size,
                        activeRole = activeRole,
                        isDarkTheme = isDarkTheme,
                        onToggleTheme = { viewModel.setDarkTheme(!isDarkTheme) },
                        onToggleRoleClick = {
                            if (activeRole == AppRole.ADMIN_PANEL) {
                                viewModel.switchToUserPanel()
                            }
                        },
                        onMenuOrBackClick = {
                            if (showHamburgerIcon) {
                                scope.launch { drawerState.open() }
                            } else {
                                viewModel.navigateBack()
                            }
                        },
                        onBellClick = {
                            viewModel.triggerHazardNotificationOnRoute()
                        }
                    )
                    if (isAdminRole && currentScreen != AppScreen.LIVE_MAP) {
                        PersistentModeBanner(
                            sensorMode = sensorMode,
                            esp32State = connectivity.esp32State,
                            esp32LastSeenSec = connectivity.esp32LastSeenSecondsAgo,
                            virtualEngineState = virtualEngineState,
                            internetOnline = connectivity.internetOnline,
                            isDarkTheme = isDarkTheme,
                            onSwitchModePrompt = { targetMode ->
                                viewModel.requestModeSwitch(targetMode)
                            }
                        )
                    }
                    if (currentScreen != AppScreen.LIVE_MAP) {
                        RouteSwitchedConfirmationBanner(
                            notificationState = hazardRouteNotification,
                            onReopenRouteChoices = { viewModel.triggerHazardNotificationOnRoute() }
                        )
                    }
                    if (currentScreen in setOf(AppScreen.ROUTE_PLANNER, AppScreen.JOURNEY_TRACKING)) {
                        AutomaticDiversionBanner(
                            diversion = diversionAlert,
                            isAdminMode = isAdminRole,
                            onViewMapClick = { viewModel.navigateTo(AppScreen.LIVE_MAP) }
                        )
                    }
                }
            },
            bottomBar = {
                RoutPilotBottomNavBar(
                    currentScreen = currentScreen,
                    activeRole = activeRole,
                    onNavigate = { target -> viewModel.navigateTo(target) }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                val activeEval = if (sensorMode == SensorMode.EXTERNAL_HARDWARE) {
                    hardwareHazardEval
                } else {
                    virtualHazardEval
                }
                val activeLivePacket = if (sensorMode == SensorMode.EXTERNAL_HARDWARE) {
                    liveHardwarePacket
                } else {
                    virtualTestPacket
                }
                val currentVehicleNode = activeRouteResult?.nodePath?.getOrElse(
                    journeyNodeIndex.coerceIn(0, (activeRouteResult?.nodePath?.size ?: 1) - 1)
                ) { "H" } ?: "H"

                when (currentScreen) {
                    AppScreen.HOME -> {
                        HomeDashboardScreen(
                            sensorMode = sensorMode,
                            esp32State = connectivity.esp32State,
                            virtualEngineState = virtualEngineState,
                            nodes = graphNodes,
                            edges = graphEdges,
                            activeHazards = activeModeHazards,
                            activeRoute = activeRouteResult,
                            gpsState = gpsState,
                            isDarkTheme = isDarkTheme,
                            emergencyBroadcast = adminEmergencyBroadcast,
                            onOpenCitizenReport = { showCitizenReportDialog = true },
                            onOpenAdminPanel = { viewModel.requestAdminPanelFromSplashOrApp() },
                            onQuickSelectDestination = { destNodeId ->
                                viewModel.setDestinationNode(destNodeId)
                                viewModel.calculateSelectedRoute(recordInDb = true)
                            },
                            onNavigate = { viewModel.navigateTo(it) },
                            onRequestModeSwitch = { viewModel.requestModeSwitch(it) }
                        )
                    }
                    AppScreen.ADMIN_DASHBOARD -> {
                        AdminControlPanelScreen(
                            edges = graphEdges,
                            activeRoute = activeRouteResult,
                            sensorMode = sensorMode,
                            emergencyBroadcast = adminEmergencyBroadcast,
                            citizenReports = citizenHazardReports,
                            onSetRoadStatusOverride = { edgeId, status, reason ->
                                viewModel.setAdminRoadStatusOverride(edgeId, status, reason)
                            },
                            onResetAllRoadOverrides = { viewModel.clearAllAdminRoadOverrides() },
                            onUpdateEmergencyBroadcast = { viewModel.updateAdminEmergencyBroadcast(it) },
                            onVerifyCitizenReport = { viewModel.verifyAndBlockCitizenReport(it) },
                            onResolveCitizenReport = { viewModel.resolveCitizenReport(it) },
                            onSwitchToUserPanel = { viewModel.switchToUserPanel() },
                            onLogoutAdmin = { viewModel.logoutAdmin() },
                            onNavigate = { viewModel.navigateTo(it) }
                        )
                    }
                    AppScreen.LIVE_MAP -> {
                        LiveMapScreen(
                            nodes = graphNodes,
                            edges = graphEdges,
                            activeRoute = activeRouteResult,
                            alternateRoute = alternateRouteResult,
                            activeHazards = activeModeHazards,
                            gpsState = gpsState,
                            sensorMode = sensorMode,
                            livePacket = activeLivePacket,
                            externalOsrmGeometry = externalOsrmRoute?.geometry ?: emptyList(),
                            currentVehicleNodeId = currentVehicleNode,
                            isDarkTheme = isDarkTheme,
                            isAdminMode = isAdminRole,
                            onRequestLocationPermission = {
                                permissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            },
                            onNavigateToHazardDetail = { viewModel.navigateTo(AppScreen.HAZARD_DETECTION) },
                            onNavigateToRoutePlanner = { viewModel.navigateTo(AppScreen.ROUTE_PLANNER) },
                            onTriggerHazardNotification = { viewModel.triggerHazardNotificationOnRoute() }
                        )
                    }
                    AppScreen.ROUTE_PLANNER -> {
                        RoutePlannerScreen(
                            nodes = graphNodes,
                            edges = graphEdges,
                            sourceNodeId = sourceNodeId,
                            destinationNodeId = destinationNodeId,
                            selectedVehicle = selectedVehicle,
                            selectedAlgorithm = selectedAlgorithm,
                            activeRoute = activeRouteResult,
                            initialOptimalRoute = initialOptimalRouteResult,
                            safeOptimalRoute1 = safeOptimalRoute1,
                            safeOptimalRoute2 = safeOptimalRoute2,
                            livePacket = activeLivePacket,
                            currentVehicleNodeId = currentVehicleNode,
                            externalOsrmRoute = externalOsrmRoute,
                            placeSearchQuery = placeSearchQuery,
                            placeSearchResults = placeSearchResults,
                            isSearchingPlaces = isSearchingPlaces,
                            selectedCustomPlace = selectedCustomPlace,
                            isAdminMode = isAdminRole,
                            onSelectSource = { viewModel.setSourceNode(it) },
                            onSelectDestination = { viewModel.setDestinationNode(it) },
                            onSelectVehicle = { viewModel.setVehicleType(it) },
                            onSelectAlgorithm = { viewModel.setRoutingAlgorithm(it) },
                            onUpdateSearchQuery = { viewModel.updatePlaceSearchQuery(it) },
                            onTriggerPlaceSearch = { viewModel.searchRealPlaces() },
                            onSelectPlaceResult = { viewModel.selectSearchedPlace(it) },
                            onCalculateRoute = { viewModel.calculateSelectedRoute(recordInDb = true) },
                            onSelectOptimalRouteForGraph = { viewModel.selectOptimalRouteForGraph(it) },
                            onChooseOptimalPathAndProceed = { viewModel.chooseOptimalPathAndStartMoving() },
                            onViewRouteOnMap = { viewModel.navigateTo(AppScreen.LIVE_MAP) },
                            onNavigateToComparison = { viewModel.navigateTo(AppScreen.ALGORITHM_COMPARISON) },
                            onNavigateToJourney = { viewModel.navigateTo(AppScreen.JOURNEY_TRACKING) }
                        )
                    }
                    AppScreen.SENSORS -> {
                        SensorMonitoringScreen(
                            sensorMode = sensorMode,
                            esp32State = connectivity.esp32State,
                            esp32LastSeenSec = connectivity.esp32LastSeenSecondsAgo,
                            hardwareConfig = hardwareConfig,
                            hardwareTestResult = hardwareTestResult,
                            liveHardwarePacket = liveHardwarePacket,
                            virtualTestPacket = virtualTestPacket,
                            virtualEngineState = virtualEngineState,
                            selectedScenario = selectedScenario,
                            hardwareSeries = hardwareSeries,
                            virtualSeries = virtualSeries,
                            thresholds = viewModel.thresholds,
                            onRequestModeSwitch = { viewModel.requestModeSwitch(it) },
                            onStartVirtual = { viewModel.startVirtualScenario(it) },
                            onPauseVirtual = { viewModel.pauseVirtualScenario() },
                            onStopVirtual = { viewModel.stopVirtualScenario() },
                            onResetVirtual = { viewModel.resetVirtualScenario() },
                            onTestHardwareConnection = { viewModel.testHardwareConnection() },
                            onConfigureHardware = { viewModel.navigateTo(AppScreen.SETTINGS) },
                            onOpenHazardScreen = { viewModel.navigateTo(AppScreen.HAZARD_DETECTION) }
                        )
                    }
                    AppScreen.HAZARD_DETECTION -> {
                        HazardDetectionScreen(
                            sensorMode = sensorMode,
                            activeHazards = activeModeHazards,
                            activeEval = activeEval,
                            isAdminMode = isAdminRole,
                            onViewOnMap = { viewModel.navigateTo(AppScreen.LIVE_MAP) }
                        )
                    }
                    AppScreen.ALGORITHM_COMPARISON -> {
                        AlgorithmComparisonScreen(
                            comparison = algorithmComparison,
                            matlabStatusMessage = connectivity.matlabStatusMessage,
                            onRecalculateBoth = { viewModel.calculateSelectedRoute(recordInDb = true) },
                            onViewRoutesOnMap = { viewModel.navigateTo(AppScreen.LIVE_MAP) }
                        )
                    }
                    AppScreen.JOURNEY_TRACKING -> {
                        JourneyTrackingScreen(
                            nodes = graphNodes,
                            edges = graphEdges,
                            activeRoute = activeRouteResult,
                            alternateRoute = alternateRouteResult,
                            activeHazards = activeModeHazards,
                            gpsState = gpsState,
                            sensorMode = sensorMode,
                            livePacket = activeLivePacket,
                            journeyStatus = journeyStatus,
                            journeyNodeIndex = journeyNodeIndex,
                            isDarkTheme = isDarkTheme,
                            isAdminMode = isAdminRole,
                            onPauseResumeJourney = { viewModel.pauseJourney() },
                            onStopJourney = { viewModel.stopJourney() },
                            onAdvanceCheckpoint = { viewModel.stepJourneyForward() },
                            onOpenHazardDetail = { viewModel.navigateTo(AppScreen.HAZARD_DETECTION) }
                        )
                    }
                    AppScreen.TEST_SCENARIOS -> {
                        TestScenariosScreen(
                            sensorMode = sensorMode,
                            selectedScenario = selectedScenario,
                            virtualEngineState = virtualEngineState,
                            onSelectScenario = { viewModel.selectVirtualScenario(it) },
                            onRunScenario = { viewModel.startVirtualScenario(it) },
                            onRequestModeSwitch = { viewModel.requestModeSwitch(it) },
                            onNavigate = { viewModel.navigateTo(it) }
                        )
                    }
                    AppScreen.SETTINGS -> {
                        SettingsScreen(
                            weights = routeWeights,
                            sensorMode = sensorMode,
                            esp32State = connectivity.esp32State,
                            hardwareConfig = hardwareConfig,
                            hardwareTestResult = hardwareTestResult,
                            isDarkTheme = isDarkTheme,
                            isAdminMode = isAdminRole,
                            selectedVehicle = selectedVehicle,
                            onSelectVehicle = { viewModel.setVehicleType(it) },
                            onOpenCitizenReport = { showCitizenReportDialog = true },
                            onOpenHistory = { viewModel.navigateTo(AppScreen.HISTORY) },
                            onOpenAdminLogin = { viewModel.requestAdminPanelFromSplashOrApp() },
                            onExitAdminMode = { viewModel.switchToUserPanel() },
                            onUpdateWeights = { viewModel.updateRouteWeights(it) },
                            onSaveHardwareConfig = { viewModel.saveHardwareConfiguration(it) },
                            onTestHardwareConnection = { viewModel.testHardwareConnection(it) },
                            onRequestModeSwitch = { viewModel.requestModeSwitch(it) },
                            onToggleDarkTheme = { viewModel.setDarkTheme(it) },
                            onSaveAndReturn = { viewModel.navigateTo(AppScreen.ADMIN_DASHBOARD) }
                        )
                    }
                    AppScreen.HISTORY -> {
                        HistoryScreen(
                            events = historyEvents,
                            isAdminMode = isAdminRole,
                            onNavigate = { viewModel.navigateTo(it) }
                        )
                    }
                    AppScreen.CONNECTIVITY, AppScreen.DATA_SOURCES -> {
                        ConnectivityAndDataSourcesScreen(
                            connectivity = connectivity,
                            sensorMode = sensorMode,
                            matlabAnalysis = matlabSignalAnalysis,
                            onNavigate = { viewModel.navigateTo(it) }
                        )
                    }
                    AppScreen.SPLASH -> Unit
                }
            }
        }
    }
}

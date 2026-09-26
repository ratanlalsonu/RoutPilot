package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.HistoryEventEntity
import com.example.model.AppScreen
import com.example.model.DataSource
import com.example.model.HardwareConfiguration
import com.example.model.HardwareConnectionState
import com.example.model.HardwareConnectionType
import com.example.model.HardwareTestConnectionResult
import com.example.model.RouteCostWeights
import com.example.model.SensorMode
import com.example.model.SystemConnectivityState
import com.example.model.VehicleType
import com.example.model.VirtualEngineState
import com.example.model.VirtualScenario
import com.example.ui.components.SensorModeSelectorCard
import com.example.ui.theme.RpBlockedRed
import com.example.ui.theme.RpBlockedRedBg
import com.example.ui.theme.RpCriticalOrange
import com.example.ui.theme.RpPrimaryBlue
import com.example.ui.theme.RpPurpleAccent
import com.example.ui.theme.RpPurpleBg
import com.example.ui.theme.RpSafeGreen
import com.example.ui.theme.RpSafeGreenBg
import com.example.ui.theme.RpWarningYellow
import com.example.ui.theme.RpWarningYellowBg
import java.util.Locale

@Composable
fun TestScenariosScreen(
    sensorMode: SensorMode,
    selectedScenario: VirtualScenario,
    virtualEngineState: VirtualEngineState,
    onSelectScenario: (VirtualScenario) -> Unit,
    onRunScenario: (VirtualScenario) -> Unit,
    onRequestModeSwitch: (SensorMode) -> Unit,
    onNavigate: (AppScreen) -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Sub-navigation Pills for More section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            MoreSubNavButton("Test Mode", true, { onNavigate(AppScreen.TEST_SCENARIOS) }, Modifier.weight(1f))
            MoreSubNavButton("History", false, { onNavigate(AppScreen.HISTORY) }, Modifier.weight(1f))
            MoreSubNavButton("System", false, { onNavigate(AppScreen.CONNECTIVITY) }, Modifier.weight(1f))
            MoreSubNavButton("Settings", false, { onNavigate(AppScreen.SETTINGS) }, Modifier.weight(1f))
        }

        // Prominent Virtual Mode & Test Data Warning Banner
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = RpPurpleAccent.copy(alpha = 0.14f)),
            border = BorderStroke(1.5.dp, RpPurpleAccent),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "VIRTUAL MODE • TEST DATA — NOT LIVE",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = RpPurpleAccent
                    )
                    Text(
                        text = "Tests the complete RoutPilot validation, hazard, road-status, and A*/Dijkstra route-diversion pipeline without physical ESP32 sensors.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                if (sensorMode != SensorMode.VIRTUAL) {
                    Button(
                        onClick = { onRequestModeSwitch(SensorMode.VIRTUAL) },
                        colors = ButtonDefaults.buttonColors(containerColor = RpPurpleAccent),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Enable Virtual", fontSize = 11.sp)
                    }
                }
            }
        }

        // List of All 9 Selectable Scenarios matching Reference Image
        VirtualScenario.entries.forEach { scenario ->
            val isSelected = scenario == selectedScenario
            val icon = scenarioIcon(scenario)
            val iconColor = scenarioColor(scenario)

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onSelectScenario(scenario) }
                    .testTag("scenario_item_${scenario.id.lowercase()}"),
                color = if (isSelected) RpPrimaryBlue.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) RpPrimaryBlue else MaterialTheme.colorScheme.outline
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = scenario.title,
                        tint = iconColor,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = scenario.title,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = scenario.subtitle,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Button(
            onClick = {
                if (sensorMode != SensorMode.VIRTUAL) {
                    onRequestModeSwitch(SensorMode.VIRTUAL)
                } else {
                    onRunScenario(selectedScenario)
                    onNavigate(AppScreen.SENSORS)
                }
            },
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = RpSafeGreen),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("run_scenario_button")
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Run Scenario (${selectedScenario.title})",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

private fun scenarioIcon(scenario: VirtualScenario): ImageVector {
    return when (scenario) {
        VirtualScenario.NORMAL -> Icons.Default.DirectionsCar
        VirtualScenario.ROAD_CONSTRUCTION -> Icons.Default.Construction
        VirtualScenario.HIGH_VIBRATION -> Icons.Default.GraphicEq
        VirtualScenario.ABNORMAL_TILT -> Icons.Default.RotateRight
        VirtualScenario.HIGH_STRAIN -> Icons.Default.Compress
        VirtualScenario.HIGH_DISPLACEMENT -> Icons.Default.CompareArrows
        VirtualScenario.HIGH_WATER_LEVEL -> Icons.Default.WaterDrop
        VirtualScenario.BRIDGE_STRUCTURAL_WARNING -> Icons.Default.Warning
        VirtualScenario.COMBINED_HAZARD -> Icons.Default.Warning
    }
}

private fun scenarioColor(scenario: VirtualScenario): Color {
    return when (scenario) {
        VirtualScenario.NORMAL -> RpPrimaryBlue
        VirtualScenario.ROAD_CONSTRUCTION -> Color(0xFFD97706)
        VirtualScenario.HIGH_VIBRATION -> RpPrimaryBlue
        VirtualScenario.ABNORMAL_TILT -> Color(0xFFD97706)
        VirtualScenario.HIGH_STRAIN -> RpSafeGreen
        VirtualScenario.HIGH_DISPLACEMENT -> RpBlockedRed
        VirtualScenario.HIGH_WATER_LEVEL -> Color(0xFF0284C7)
        VirtualScenario.BRIDGE_STRUCTURAL_WARNING -> RpCriticalOrange
        VirtualScenario.COMBINED_HAZARD -> RpBlockedRed
    }
}

@Composable
fun SettingsScreen(
    weights: RouteCostWeights,
    sensorMode: SensorMode,
    esp32State: HardwareConnectionState,
    hardwareConfig: HardwareConfiguration,
    hardwareTestResult: HardwareTestConnectionResult,
    isDarkTheme: Boolean,
    isAdminMode: Boolean = false,
    selectedVehicle: VehicleType = VehicleType.CAR,
    onSelectVehicle: (VehicleType) -> Unit = {},
    onOpenCitizenReport: () -> Unit = {},
    onOpenHistory: () -> Unit = {},
    onOpenAdminLogin: () -> Unit = {},
    onExitAdminMode: () -> Unit = {},
    onUpdateWeights: (RouteCostWeights) -> Unit,
    onSaveHardwareConfig: (HardwareConfiguration) -> Unit,
    onTestHardwareConnection: (HardwareConfiguration) -> Unit,
    onRequestModeSwitch: (SensorMode) -> Unit,
    onToggleDarkTheme: (Boolean) -> Unit,
    onSaveAndReturn: () -> Unit
) {
    val scrollState = rememberScrollState()
    var distanceW by remember(weights) { mutableStateOf(weights.distanceWeight) }
    var timeW by remember(weights) { mutableStateOf(weights.timeWeight) }
    var trafficW by remember(weights) { mutableStateOf(weights.trafficWeight) }
    var warningP by remember(weights) { mutableStateOf(weights.warningPenalty) }
    var criticalP by remember(weights) { mutableStateOf(weights.criticalPenalty) }
    var vehicleP by remember(weights) { mutableStateOf(weights.vehicleRestrictionPenalty) }
    var simSpeed by remember(weights) { mutableStateOf(weights.simulationSpeed) }

    // Hardware Configuration editable fields
    var httpEndpointInput by remember(hardwareConfig.httpEndpoint) { mutableStateOf(hardwareConfig.httpEndpoint) }
    var wsEndpointInput by remember(hardwareConfig.wsEndpoint) { mutableStateOf(hardwareConfig.wsEndpoint) }
    var deviceIdInput by remember(hardwareConfig.deviceId) { mutableStateOf(hardwareConfig.deviceId) }
    var selectedConnType by remember(hardwareConfig.connectionType) { mutableStateOf(hardwareConfig.connectionType) }
    var configSavedFeedback by remember { mutableStateOf(false) }

    val draftHardwareConfig = HardwareConfiguration(
        httpEndpoint = httpEndpointInput.trim(),
        wsEndpoint = wsEndpointInput.trim(),
        deviceId = deviceIdInput.trim().ifBlank { "ESP32-001" },
        connectionType = selectedConnType
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (!isAdminMode) {
            // =========================================================================
            // USER MODE SETTINGS: Ultra-Simple Google Maps-Style Preferences
            // (Zero algorithm, cost weight, or hardware sensor jargon)
            // =========================================================================

            // 1. Display & Map Appearance (Light / Dark)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "App Appearance",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Choose Day (Light) or Night (Dark) screen style",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ThemeChoicePill(
                            label = "Day (Light)",
                            icon = Icons.Default.LightMode,
                            selected = !isDarkTheme,
                            onClick = { onToggleDarkTheme(false) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("theme_light_button")
                        )
                        ThemeChoicePill(
                            label = "Night (Dark)",
                            icon = Icons.Default.DarkMode,
                            selected = isDarkTheme,
                            onClick = { onToggleDarkTheme(true) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("theme_dark_button")
                        )
                    }
                }
            }

            // 2. Your Vehicle Type (Simple 1-Tap Choice like Google Maps)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Your Vehicle",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Select your vehicle so heavy trucks and buses automatically avoid weak bridges.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        VehicleType.entries.forEach { vehicle ->
                            val selected = selectedVehicle == vehicle
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (selected) RpPrimaryBlue.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(1.5.dp, if (selected) RpPrimaryBlue else MaterialTheme.colorScheme.outline),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onSelectVehicle(vehicle) }
                                    .testTag("settings_vehicle_${vehicle.name.lowercase()}")
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 4.dp)) {
                                    Text(
                                        text = vehicle.label,
                                        fontSize = 12.sp,
                                        fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                        color = if (selected) RpPrimaryBlue else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Road Safety & Driver Help
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Road Safety & Help",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = RpSafeGreenBg,
                        border = BorderStroke(1.dp, RpSafeGreen.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(RpSafeGreen)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Automatic Closed-Bridge Rerouting: ON",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = RpSafeGreen
                                )
                                Text(
                                    text = "Automatically guides you onto safe open roads when a bridge or road ahead is closed.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF065F46)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onOpenCitizenReport,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("settings_report_issue_button")
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = RpCriticalOrange, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Report Road Issue", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onOpenHistory,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("settings_recent_trips_button")
                        ) {
                            Icon(Icons.Default.History, contentDescription = null, tint = RpPrimaryBlue, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Recent Trips", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 4. Discreet "About App" Card at the very bottom (Hidden Admin Access inside Settings)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "About RoutePilot",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Live Road & Bridge Navigation • Map Data 2026",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    // Discreet row at the very bottom of Settings for authorized Admin entry
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onOpenAdminLogin() }
                            .testTag("settings_admin_access_button")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Version 2.4.0 (Build 2026.09)",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "System Console",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                            )
                        }
                    }
                }
            }
        } else {
            // =========================================================================
            // ADMIN MODE SETTINGS: Full Engineering, Algorithm Weights & ESP32 Config
            // =========================================================================

            // Admin Active Banner with quick return to Driver App
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = RpPurpleAccent.copy(alpha = 0.12f)),
                border = BorderStroke(1.5.dp, RpPurpleAccent),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Admin Engineering & Calibration Settings",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = RpPurpleAccent
                    )
                    Text(
                        text = "Configure A* / Dijkstra route cost weights, switch between Virtual and ESP32 Hardware sensor modes, and manage endpoints.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onSaveAndReturn,
                            colors = ButtonDefaults.buttonColors(containerColor = RpPurpleAccent),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Admin Control Center", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        OutlinedButton(
                            onClick = onExitAdminMode,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, RpPrimaryBlue),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings_exit_admin_button")
                        ) {
                            Text("Return to Driver App", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = RpPrimaryBlue)
                        }
                    }
                }
            }

            // 1. Route Cost Weights & System Settings Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Route Cost Weights (A* / Dijkstra)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    SettingSliderRow(
                        label = "Distance Weight",
                        value = distanceW,
                        range = 0.2f..3.0f,
                        valueText = String.format(Locale.US, "%.1f", distanceW),
                        onValueChange = { distanceW = it }
                    )
                    SettingSliderRow(
                        label = "Travel Time Weight",
                        value = timeW,
                        range = 0.2f..3.0f,
                        valueText = String.format(Locale.US, "%.1f", timeW),
                        onValueChange = { timeW = it }
                    )
                    SettingSliderRow(
                        label = "Traffic Weight",
                        value = trafficW,
                        range = 0.0f..2.5f,
                        valueText = String.format(Locale.US, "%.1f", trafficW),
                        onValueChange = { trafficW = it }
                    )
                    SettingSliderRow(
                        label = "Warning Penalty",
                        value = warningP,
                        range = 1.0f..25.0f,
                        valueText = String.format(Locale.US, "%.1f", warningP),
                        onValueChange = { warningP = it }
                    )
                    SettingSliderRow(
                        label = "Critical Penalty",
                        value = criticalP,
                        range = 5.0f..60.0f,
                        valueText = String.format(Locale.US, "%.1f", criticalP),
                        onValueChange = { criticalP = it }
                    )
                    SettingSliderRow(
                        label = "Vehicle Restriction Penalty",
                        value = vehicleP,
                        range = 20.0f..200.0f,
                        valueText = String.format(Locale.US, "%.1f", vehicleP),
                        onValueChange = { vehicleP = it }
                    )
                    SettingSliderRow(
                        label = "Simulation Speed",
                        value = simSpeed,
                        range = 0.5f..3.0f,
                        valueText = String.format(Locale.US, "%.1fx", simSpeed),
                        onValueChange = { simSpeed = it }
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Theme Toggle (Light / Dark)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Theme",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ThemeChoicePill(
                                label = "Light",
                                icon = Icons.Default.LightMode,
                                selected = !isDarkTheme,
                                onClick = { onToggleDarkTheme(false) },
                                modifier = Modifier.testTag("theme_light_button")
                            )
                            ThemeChoicePill(
                                label = "Dark",
                                icon = Icons.Default.DarkMode,
                                selected = isDarkTheme,
                                onClick = { onToggleDarkTheme(true) },
                                modifier = Modifier.testTag("theme_dark_button")
                            )
                        }
                    }
                }
            }

            Button(
                onClick = {
                    onSaveHardwareConfig(draftHardwareConfig)
                    onUpdateWeights(
                        RouteCostWeights(
                            distanceWeight = distanceW,
                            timeWeight = timeW,
                            trafficWeight = trafficW,
                            warningPenalty = warningP,
                            criticalPenalty = criticalP,
                            vehicleRestrictionPenalty = vehicleP,
                            simulationSpeed = simSpeed
                        )
                    )
                    onSaveAndReturn()
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RpPrimaryBlue),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_settings_button")
            ) {
                Icon(Icons.Default.Save, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Save Admin Settings",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // 2. Sensor Mode Selector
            SensorModeSelectorCard(
                currentMode = sensorMode,
                esp32State = esp32State,
                onSelectMode = onRequestModeSwitch
            )

            // 3. Hardware Configuration Card (ESP32_HTTP_ENDPOINT, ESP32_WS_ENDPOINT, Device ID, Connection Type)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("hardware_configuration_card")
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.DeveloperBoard,
                                contentDescription = "Hardware Configuration",
                                tint = RpPrimaryBlue,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Hardware Configuration",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "ESP32 & Physical Sensors (Optional for Virtual Mode)",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            color = if (draftHardwareConfig.isConfigured) RpSafeGreenBg else RpWarningYellowBg,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (draftHardwareConfig.isConfigured) "CONFIGURED" else "OPTIONAL",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (draftHardwareConfig.isConfigured) RpSafeGreen else Color(0xFFB45309),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    if (!draftHardwareConfig.isConfigured) {
                        Surface(
                            color = RpBlockedRed.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, RpBlockedRed.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "ESP32 endpoint not configured.",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = RpBlockedRed
                                )
                                Text(
                                    text = "Virtual Mode works completely without ESP32 endpoints. Enter your ESP32 HTTP or WebSocket endpoint below when ready to test physical sensors in External Hardware Mode.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = httpEndpointInput,
                        onValueChange = {
                            httpEndpointInput = it
                            configSavedFeedback = false
                        },
                        label = { Text("ESP32 HTTP Endpoint (ESP32_HTTP_ENDPOINT)", fontSize = 11.sp) },
                        placeholder = { Text("http://192.168.1.50/sensors", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("esp32_http_endpoint_input")
                    )

                    OutlinedTextField(
                        value = wsEndpointInput,
                        onValueChange = {
                            wsEndpointInput = it
                            configSavedFeedback = false
                        },
                        label = { Text("ESP32 WebSocket Endpoint (ESP32_WS_ENDPOINT)", fontSize = 11.sp) },
                        placeholder = { Text("ws://192.168.1.50:81/ws", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("esp32_ws_endpoint_input")
                    )

                    OutlinedTextField(
                        value = deviceIdInput,
                        onValueChange = {
                            deviceIdInput = it
                            configSavedFeedback = false
                        },
                        label = { Text("Device ID", fontSize = 11.sp) },
                        placeholder = { Text("ESP32-001", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("esp32_device_id_input")
                    )

                    Text(
                        text = "Connection Type",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        HardwareConnectionType.entries.forEach { connType ->
                            val selected = selectedConnType == connType
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        selectedConnType = connType
                                        configSavedFeedback = false
                                    }
                                    .testTag("conn_type_${connType.code.lowercase()}"),
                                color = if (selected) RpPrimaryBlue.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, if (selected) RpPrimaryBlue else MaterialTheme.colorScheme.outline)
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 4.dp)) {
                                    Text(
                                        text = when (connType) {
                                            HardwareConnectionType.HTTP -> "HTTP"
                                            HardwareConnectionType.WEBSOCKET -> "WebSocket"
                                            HardwareConnectionType.HTTP_AND_WS -> "HTTP+WS"
                                            HardwareConnectionType.MQTT_FASTAPI -> "MQTT"
                                        },
                                        fontSize = 10.sp,
                                        fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium,
                                        color = if (selected) RpPrimaryBlue else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    // Hardware Test Connection Result Banner
                    if (hardwareTestResult.isTesting || hardwareTestResult.tested) {
                        val resultColor = when {
                            hardwareTestResult.isTesting -> Color(0xFFD97706)
                            hardwareTestResult.success -> RpSafeGreen
                            else -> RpBlockedRed
                        }
                        Surface(
                            color = resultColor.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, resultColor.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("hardware_test_result_banner")
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = hardwareTestResult.statusHeadline,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = resultColor
                                )
                                Text(
                                    text = hardwareTestResult.statusSubtext,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    if (configSavedFeedback) {
                        Text(
                            text = "✓ Hardware Configuration saved.",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = RpSafeGreen
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                onSaveHardwareConfig(draftHardwareConfig)
                                configSavedFeedback = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = RpPrimaryBlue),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("save_hardware_config_button")
                        ) {
                            Text("Save Configuration", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                onTestHardwareConnection(draftHardwareConfig)
                            },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.5.dp, RpPrimaryBlue),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("test_hardware_connection_button")
                        ) {
                            Text(
                                text = if (hardwareTestResult.isTesting) "Testing..." else "Test Connection",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = RpPrimaryBlue
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            httpEndpointInput = ""
                            wsEndpointInput = ""
                            deviceIdInput = "ESP32-001"
                            selectedConnType = HardwareConnectionType.HTTP_AND_WS
                            onSaveHardwareConfig(
                                HardwareConfiguration(
                                    httpEndpoint = "",
                                    wsEndpoint = "",
                                    deviceId = "ESP32-001",
                                    connectionType = HardwareConnectionType.HTTP_AND_WS
                                )
                            )
                            configSavedFeedback = false
                        },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .testTag("clear_hardware_config_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Clear Configuration",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Clear Configuration",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingSliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    valueText: String,
    onValueChange: (Float) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1.1f)
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = RpPrimaryBlue,
                activeTrackColor = RpPrimaryBlue
            ),
            modifier = Modifier.weight(1.2f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = valueText,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(42.dp)
        )
    }
}

@Composable
private fun ThemeChoicePill(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        color = if (selected) RpPrimaryBlue.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.5.dp, if (selected) RpPrimaryBlue else MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) RpPrimaryBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) RpPrimaryBlue else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun HistoryScreen(
    events: List<HistoryEventEntity>,
    isAdminMode: Boolean = false,
    onNavigate: (AppScreen) -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (isAdminMode) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MoreSubNavButton("Test Mode", false, { onNavigate(AppScreen.TEST_SCENARIOS) }, Modifier.weight(1f))
                MoreSubNavButton("History", true, { onNavigate(AppScreen.HISTORY) }, Modifier.weight(1f))
                MoreSubNavButton("System", false, { onNavigate(AppScreen.CONNECTIVITY) }, Modifier.weight(1f))
                MoreSubNavButton("Settings", false, { onNavigate(AppScreen.SETTINGS) }, Modifier.weight(1f))
            }
        }

        Text(
            text = if (isAdminMode) "Event & Route History (Room Local DB)" else "Recent Trips & Road Alerts",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        if (events.isEmpty()) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No recent trips recorded yet.",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        } else {
            events.forEach { ev ->
                val isLive = ev.dataSource == DataSource.LIVE_HARDWARE.dbValue
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${ev.formattedDate} • ${ev.eventType}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (isAdminMode) {
                                Surface(
                                    color = if (isLive) RpSafeGreenBg else RpPurpleBg,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = ev.mode,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isLive) RpSafeGreen else RpPurpleAccent,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = if (isAdminMode) {
                                "Location: ${ev.location} • Source: ${ev.source} • Status: ${ev.status}"
                            } else {
                                "${ev.location} • ${ev.status}"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = RpPrimaryBlue
                        )
                        if (isAdminMode) {
                            Text(
                                text = ev.details,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConnectivityAndDataSourcesScreen(
    connectivity: SystemConnectivityState,
    sensorMode: SensorMode,
    matlabAnalysis: Map<String, String>,
    onNavigate: (AppScreen) -> Unit
) {
    val scrollState = rememberScrollState()
    var showMatlabFiles by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            MoreSubNavButton("Test Mode", false, { onNavigate(AppScreen.TEST_SCENARIOS) }, Modifier.weight(1f))
            MoreSubNavButton("History", false, { onNavigate(AppScreen.HISTORY) }, Modifier.weight(1f))
            MoreSubNavButton("System", true, { onNavigate(AppScreen.CONNECTIVITY) }, Modifier.weight(1f))
            MoreSubNavButton("Settings", false, { onNavigate(AppScreen.SETTINGS) }, Modifier.weight(1f))
        }

        // Connectivity Status Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "System Connectivity Status",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                ConnectivityStatusRow("Sensor Mode", sensorMode.displayTitle, true)
                ConnectivityStatusRow("Internet", if (connectivity.internetOnline) "ONLINE" else "OFFLINE", connectivity.internetOnline)
                ConnectivityStatusRow("Android GPS", if (connectivity.gpsAvailable) "ACTIVE (Fused Location)" else "GPS UNAVAILABLE", connectivity.gpsAvailable)
                ConnectivityStatusRow("FastAPI Backend", if (connectivity.fastApiConnected) "CONNECTED (${connectivity.fastApiUrl})" else "LOCAL AUTONOMOUS (${connectivity.fastApiUrl})", connectivity.fastApiConnected)
                ConnectivityStatusRow("Database", connectivity.databaseStatus, true)
                ConnectivityStatusRow(
                    "ESP32 Hardware",
                    if (!connectivity.esp32Configured) {
                        "ESP32 endpoint not configured."
                    } else if (connectivity.esp32State == HardwareConnectionState.CONNECTED) {
                        "${connectivity.esp32DeviceId} • LIVE SENSOR DATA"
                    } else {
                        "${connectivity.esp32DeviceId} • NO LIVE SENSOR DATA (${connectivity.esp32State.label})"
                    },
                    connectivity.esp32State == HardwareConnectionState.CONNECTED
                )
                ConnectivityStatusRow(
                    "ESP32 HTTP",
                    connectivity.esp32Endpoint.ifBlank { "Optional (Not set)" },
                    connectivity.esp32Endpoint.isNotBlank()
                )
                ConnectivityStatusRow(
                    "ESP32 WebSocket",
                    connectivity.esp32WsEndpoint.ifBlank { "Optional (Not set)" },
                    connectivity.esp32WsEndpoint.isNotBlank()
                )
                ConnectivityStatusRow(
                    "MATLAB Layer",
                    connectivity.matlabStatusMessage,
                    connectivity.matlabConnected
                )
                ConnectivityStatusRow("Routing Provider", connectivity.routingProviderStatus, true)

                OutlinedButton(
                    onClick = { onNavigate(AppScreen.SETTINGS) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Configure Hardware", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Data Sources Card (matching prompt specification)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "DATA SOURCES",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                ConnectivityStatusRow("Map", "OpenStreetMap Slippy Tiles + RoutPilot Layer", true)
                ConnectivityStatusRow("Geocoding", "OpenStreetMap Nominatim + FastAPI", true)
                ConnectivityStatusRow("Routing", "OSRM Live Road Network + A* / Dijkstra", true)
                ConnectivityStatusRow("Traffic", "TRAFFIC DATA UNAVAILABLE (No Fake Traffic)", false)
                ConnectivityStatusRow("GPS", "Android Fused Location Provider", connectivity.gpsAvailable)
                ConnectivityStatusRow(
                    "Sensor",
                    if (sensorMode == SensorMode.EXTERNAL_HARDWARE) "ESP32 — Hardware Mode" else "Virtual Sensor Engine — Virtual Mode (TEST DATA)",
                    true
                )
                ConnectivityStatusRow("Database", "Room Local Cache + PostgreSQL Schema", true)
                ConnectivityStatusRow("Analysis", "MATLAB Engineering Suite (12 .m modules)", true)
            }
        }

        // MATLAB Signal Analysis & 12 .m Engineering Files Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "MATLAB Engineering Signal Analysis",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Data Source: ${matlabAnalysis["dataSource"] ?: sensorMode.apiValue} • RMS: ${matlabAnalysis["rms"] ?: "0.000"} g • Peak: ${matlabAnalysis["peak"] ?: "0.000"} g • StdDev: ${matlabAnalysis["stdDev"] ?: "0.000"}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = RpPrimaryBlue
                )
                OutlinedButton(
                    onClick = { showMatlabFiles = !showMatlabFiles },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (showMatlabFiles) "Hide MATLAB /matlab/*.m Modules" else "View 12 MATLAB Engineering Modules (/matlab/)")
                }
                if (showMatlabFiles) {
                    val mFiles = listOf(
                        "matlab/validateSensorData.m",
                        "matlab/detectHazard.m",
                        "matlab/calculateSeverity.m",
                        "matlab/buildRoadGraph.m",
                        "matlab/calculateRouteAStar.m",
                        "matlab/calculateRouteDijkstra.m",
                        "matlab/calculateRouteCost.m",
                        "matlab/applyVehicleRestrictions.m",
                        "matlab/applyHazardPenalty.m",
                        "matlab/analyzeSensorSignal.m",
                        "matlab/compareAlgorithms.m",
                        "matlab/analyzeVirtualScenario.m"
                    )
                    mFiles.forEach { file ->
                        Text(
                            text = "✓ $file",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = RpSafeGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConnectivityStatusRow(
    label: String,
    value: String,
    isPositive: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.9f)
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (isPositive) RpSafeGreen else RpCriticalOrange,
            modifier = Modifier.weight(1.3f)
        )
    }
}

@Composable
private fun MoreSubNavButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        color = if (selected) RpPrimaryBlue else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, if (selected) RpPrimaryBlue else MaterialTheme.colorScheme.outline)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

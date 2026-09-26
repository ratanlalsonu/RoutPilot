package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.AlgorithmRouteResult
import com.example.model.AppRole
import com.example.model.AppScreen
import com.example.model.DataSource
import com.example.model.DiversionAlertState
import com.example.model.HardwareConnectionState
import com.example.model.HazardRouteNotificationState
import com.example.model.HazardSeverity
import com.example.model.SensorMode
import com.example.model.VirtualEngineState
import com.example.ui.theme.RpBlockedRed
import com.example.ui.theme.RpBlockedRedBg
import com.example.ui.theme.RpCriticalOrange
import com.example.ui.theme.RpCriticalOrangeBg
import com.example.ui.theme.RpPrimaryBlue
import com.example.ui.theme.RpPurpleAccent
import com.example.ui.theme.RpPurpleBg
import com.example.ui.theme.RpSafeGreen
import com.example.ui.theme.RpSafeGreenBg
import com.example.ui.theme.RpWarningYellow
import com.example.ui.theme.RpWarningYellowBg

@Composable
fun RoutPilotTopBar(
    title: String,
    isHome: Boolean,
    activeHazardCount: Int,
    activeRole: AppRole = AppRole.USER_PANEL,
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
    onToggleRoleClick: () -> Unit = {},
    onMenuOrBackClick: () -> Unit,
    onBellClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onMenuOrBackClick,
                modifier = Modifier
                    .size(48.dp)
                    .testTag(if (isHome) "menu_button" else "back_button")
            ) {
                Icon(
                    imageVector = if (isHome) Icons.Default.Menu else Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = if (isHome) "Open navigation menu" else "Navigate back",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            if (isHome) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(
                            SpanStyle(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.ExtraBold
                            )
                        ) {
                            append("Route")
                        }
                        withStyle(
                            SpanStyle(
                                color = RpPrimaryBlue,
                                fontWeight = FontWeight.ExtraBold
                            )
                        ) {
                            append("Pilot")
                        }
                    },
                    fontSize = 21.sp
                )
            } else {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Show "Exit Admin" pill ONLY when inside Admin Panel; normal users never see a mode/role badge
                if (activeRole == AppRole.ADMIN_PANEL) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = RpPurpleAccent.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, RpPurpleAccent),
                        modifier = Modifier
                            .clickable { onToggleRoleClick() }
                            .testTag("topbar_role_switch_pill")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = "Exit Admin Panel",
                                tint = RpPurpleAccent,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Exit Admin",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = RpPurpleAccent
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onToggleTheme,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("topbar_theme_toggle")
                ) {
                    Icon(
                        imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = if (isDarkTheme) "Switch to Light Mode" else "Switch to Dark Mode",
                        tint = if (isDarkTheme) Color(0xFFFBBF24) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(contentAlignment = Alignment.TopEnd) {
                    IconButton(
                        onClick = onBellClick,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("notification_bell_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "View active hazard notifications",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (activeHazardCount > 0) {
                        Box(
                            modifier = Modifier
                                .padding(top = 8.dp, end = 8.dp)
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(RpBlockedRed),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = activeHazardCount.toString(),
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PersistentModeBanner(
    sensorMode: SensorMode,
    esp32State: HardwareConnectionState,
    esp32LastSeenSec: Long?,
    virtualEngineState: VirtualEngineState,
    internetOnline: Boolean,
    isDarkTheme: Boolean = false,
    onSwitchModePrompt: (SensorMode) -> Unit
) {
    val isHardware = sensorMode == SensorMode.EXTERNAL_HARDWARE
    val isConnectedHardware = isHardware && esp32State == HardwareConnectionState.CONNECTED

    val bgColor = if (isDarkTheme) {
        when {
            !internetOnline -> Color(0xFF2E210F)
            isHardware && isConnectedHardware -> Color(0xFF0B281E)
            isHardware && esp32State == HardwareConnectionState.STALE -> Color(0xFF2E210F)
            isHardware -> Color(0xFF2D1219)
            else -> Color(0xFF131D34)
        }
    } else {
        when {
            !internetOnline -> RpWarningYellowBg
            isHardware && isConnectedHardware -> RpSafeGreenBg
            isHardware && esp32State == HardwareConnectionState.STALE -> RpWarningYellowBg
            isHardware -> RpBlockedRedBg
            else -> RpPurpleBg
        }
    }

    val accentColor = when {
        !internetOnline -> RpCriticalOrange
        isHardware && isConnectedHardware -> RpSafeGreen
        isHardware && esp32State == HardwareConnectionState.STALE -> RpCriticalOrange
        isHardware -> RpBlockedRed
        isDarkTheme -> Color(0xFFA855F7)
        else -> RpPurpleAccent
    }

    val primaryText = if (isHardware) {
        "EXTERNAL HARDWARE"
    } else {
        "VIRTUAL MODE"
    }

    val statusSubtext = when {
        isHardware && isConnectedHardware -> {
            val sec = esp32LastSeenSec ?: 1L
            "LIVE SENSOR DATA • Connected (${sec}s ago)"
        }
        isHardware && esp32State == HardwareConnectionState.STALE -> {
            val sec = esp32LastSeenSec ?: 47L
            "NO LIVE SENSOR DATA • Stale (${sec}s ago)"
        }
        isHardware -> {
            "NO LIVE SENSOR DATA • Disconnected"
        }
        else -> {
            "TEST DATA — NOT LIVE • (${virtualEngineState.label})"
        }
    }

    Surface(
        color = bgColor,
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                val target = if (isHardware) SensorMode.VIRTUAL else SensorMode.EXTERNAL_HARDWARE
                onSwitchModePrompt(target)
            }
            .testTag("persistent_mode_banner")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = primaryText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = accentColor
                        )
                        if (!internetOnline) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "[OFFLINE CACHE]",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = RpBlockedRed
                            )
                        }
                    }
                    Text(
                        text = statusSubtext,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkTheme) Color(0xFFE2E8F0) else Color(0xFF1E293B),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Surface(
                color = if (isDarkTheme) Color(0xFF1E293B) else Color.White.copy(alpha = 0.85f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.45f))
            ) {
                Text(
                    text = "Switch Mode",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = accentColor,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun SensorModeSelectorCard(
    currentMode: SensorMode,
    esp32State: HardwareConnectionState = HardwareConnectionState.DISCONNECTED,
    onSelectMode: (SensorMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val isHardware = currentMode == SensorMode.EXTERNAL_HARDWARE
    val isConnected = isHardware && esp32State == HardwareConnectionState.CONNECTED

    val statusTitle = if (isHardware) "EXTERNAL HARDWARE" else "VIRTUAL MODE"
    val statusDetail = when {
        isHardware && isConnected -> "LIVE SENSOR DATA"
        isHardware -> "NO LIVE SENSOR DATA"
        else -> "TEST DATA — NOT LIVE"
    }
    val statusColor = when {
        isHardware && isConnected -> RpSafeGreen
        isHardware -> RpBlockedRed
        else -> RpPurpleAccent
    }
    val statusBg = statusColor.copy(alpha = 0.14f)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SENSOR DATA MODE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Default: VIRTUAL MODE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ModeOptionButton(
                    title = "VIRTUAL MODE",
                    selected = currentMode == SensorMode.VIRTUAL,
                    activeColor = RpPurpleAccent,
                    onClick = { onSelectMode(SensorMode.VIRTUAL) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("mode_btn_virtual")
                )
                ModeOptionButton(
                    title = "EXTERNAL HARDWARE",
                    selected = currentMode == SensorMode.EXTERNAL_HARDWARE,
                    activeColor = RpPrimaryBlue,
                    onClick = { onSelectMode(SensorMode.EXTERNAL_HARDWARE) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("mode_btn_external_hardware")
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                color = statusBg,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, statusColor.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sensor_mode_status_box")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = statusTitle,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = statusColor
                    )
                    Text(
                        text = statusDetail,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = statusColor
                    )
                }
            }
        }
    }
}

@Composable
private fun ModeOptionButton(
    title: String,
    selected: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        color = if (selected) activeColor.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) activeColor else MaterialTheme.colorScheme.outline
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (selected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                contentDescription = title,
                tint = if (selected) activeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                color = if (selected) activeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ModeSwitchConfirmDialog(
    targetMode: SensorMode?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (targetMode == null) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Switch sensor data mode?",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Target Mode: ${targetMode.displayTitle}",
                    fontWeight = FontWeight.Bold,
                    color = RpPrimaryBlue
                )
                Text(
                    text = "• EXTERNAL HARDWARE:\nUses live ESP32 data only. Never generates simulated values.",
                    fontSize = 13.sp
                )
                Text(
                    text = "• VIRTUAL:\nUses controlled test/simulated sensor data labeled TEST DATA — NOT LIVE.",
                    fontSize = 13.sp
                )
                Text(
                    text = "RoutPilot strictly separates live hardware and virtual test data sources.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = RpPrimaryBlue),
                modifier = Modifier.testTag("confirm_mode_switch_button")
            ) {
                Text("Confirm Switch")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_mode_switch_button")
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AutomaticDiversionBanner(
    diversion: DiversionAlertState,
    isAdminMode: Boolean = false,
    onViewMapClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(visible = diversion.isActive) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp)
                .clickable { onViewMapClick() },
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = RpCriticalOrangeBg),
            border = BorderStroke(1.5.dp, RpCriticalOrange)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Route Diversion Alert",
                            tint = RpBlockedRed,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isAdminMode) diversion.stage else "SAFE REROUTE ACTIVE",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            color = RpBlockedRed
                        )
                    }
                    if (isAdminMode) {
                        Surface(
                            color = if (diversion.dataSource == DataSource.LIVE_HARDWARE) RpSafeGreenBg else RpPurpleBg,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = if (diversion.dataSource == DataSource.LIVE_HARDWARE) "LIVE EVENT" else "VIRTUAL TEST EVENT",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (diversion.dataSource == DataSource.LIVE_HARDWARE) RpSafeGreen else RpPurpleAccent,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Closed Ahead: ${diversion.affectedRoad}",
                    fontSize = 12.sp,
                    color = Color(0xFF1E293B),
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Safe Route: ${diversion.newTimeMin} mins • ${diversion.newDistanceKm} km",
                    fontSize = 12.sp,
                    color = RpPrimaryBlue,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun SeverityStatusBadge(severity: HazardSeverity) {
    val (bg, fg, text) = when (severity) {
        HazardSeverity.SAFE -> Triple(RpSafeGreenBg, RpSafeGreen, "Normal")
        HazardSeverity.WARNING -> Triple(RpWarningYellowBg, Color(0xFFB45309), "Warning")
        HazardSeverity.CRITICAL -> Triple(RpBlockedRed, Color.White, "CRITICAL")
        HazardSeverity.BLOCKED -> Triple(RpBlockedRed, Color.White, "BLOCKED")
    }
    Surface(
        color = bg,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = text,
            color = fg,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun RoutPilotBottomNavBar(
    currentScreen: AppScreen,
    activeRole: AppRole = AppRole.USER_PANEL,
    onNavigate: (AppScreen) -> Unit
) {
    val isAdmin = activeRole == AppRole.ADMIN_PANEL
    val items = if (isAdmin) {
        listOf(
            Triple("Admin", Icons.Default.AdminPanelSettings, AppScreen.ADMIN_DASHBOARD),
            Triple("Sensors", Icons.Default.Sensors, AppScreen.SENSORS),
            Triple("A* Lab", Icons.Default.CompareArrows, AppScreen.ALGORITHM_COMPARISON),
            Triple("Scenarios", Icons.Default.MoreHoriz, AppScreen.TEST_SCENARIOS),
            Triple("Config", Icons.Default.Settings, AppScreen.SETTINGS)
        )
    } else {
        listOf(
            Triple("Home", Icons.Default.Home, AppScreen.HOME),
            Triple("Map", Icons.Default.Map, AppScreen.LIVE_MAP),
            Triple("Routes", Icons.Default.AltRoute, AppScreen.ROUTE_PLANNER),
            Triple("Navigate", Icons.Default.Navigation, AppScreen.JOURNEY_TRACKING),
            Triple("Settings", Icons.Default.Settings, AppScreen.SETTINGS)
        )
    }

    val activeAccent = if (isAdmin) RpPurpleAccent else RpPrimaryBlue

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        items.forEach { (label, icon, targetScreen) ->
            val selected = currentScreen == targetScreen

            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(targetScreen) },
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = label
                    )
                },
                label = {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = activeAccent,
                    selectedTextColor = activeAccent,
                    indicatorColor = activeAccent.copy(alpha = 0.14f),
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.testTag("bottom_nav_${label.lowercase()}")
            )
        }
    }
}

/**
 * Top banner shown after the user selects an alternative optimal safe route from the Hazard Notification.
 */
@Composable
fun RouteSwitchedConfirmationBanner(
    notificationState: HazardRouteNotificationState,
    onReopenRouteChoices: () -> Unit
) {
    val confirmation = notificationState.routeSwitchedConfirmation
    AnimatedVisibility(visible = !confirmation.isNullOrBlank()) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .clickable { onReopenRouteChoices() }
                .testTag("route_switched_confirmation_banner"),
            shape = RoundedCornerShape(12.dp),
            color = RpSafeGreenBg,
            border = BorderStroke(1.5.dp, RpSafeGreen)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = RpSafeGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = confirmation ?: "",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF065F46)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    color = RpSafeGreen,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "Change Route",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

/**
 * In-App APK Hazard Notification Dialog that triggers when the user chooses an optimal route
 * and starts moving forward, showing:
 * 1. Exact Location & Coordinates of the Bridge or Road
 * 2. Infrastructure Type (Bridge Hazard vs Road Hazard) & Exact Hazard Type + Sensor Values
 * 3. Option to Choose Other Optimal Safe Routes (Primary A* Safe Route & Secondary Safe Detour Route)
 */
@Composable
fun HazardRouteNotificationModal(
    state: HazardRouteNotificationState,
    isAdminMode: Boolean = false,
    onSelectSafeRoute: (AlgorithmRouteResult) -> Unit,
    onDismiss: () -> Unit
) {
    if (!state.isVisible) return

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 12.dp)
                .testTag("apk_hazard_route_notification_dialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(2.dp, RpBlockedRed),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. Alert Header
                Surface(
                    color = RpBlockedRed,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Road Alert",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isAdminMode) "APK HAZARD NOTIFICATION" else "ROAD SAFETY ALERT",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                            Text(
                                text = if (isAdminMode) state.notificationTitle else "Bridge Closed Ahead — Safe Route Ready",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                    }
                }

                // 2. Location & Reason Card
                Surface(
                    color = RpBlockedRedBg,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, RpBlockedRed.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (isAdminMode) state.infrastructureType else "CLOSED FOR SAFETY",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = RpBlockedRed
                            )
                            SeverityStatusBadge(severity = state.severity)
                        }

                        Text(
                            text = "Location: ${state.locationLabel}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0F172A)
                        )
                        if (isAdminMode) {
                            Text(
                                text = "GPS Coordinates: ${state.coordinatesLabel}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF475569)
                            )
                            Text(
                                text = "Hazard Type: ${state.hazardTypeSummary}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF7F1D1D)
                            )
                            Text(
                                text = "Your Current Path (Affected): ${state.affectedChosenPathLabel}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = RpBlockedRed
                            )
                        } else {
                            Text(
                                text = "This bridge/road is unsafe right now. Please take the safe bypass road below.",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF7F1D1D)
                            )
                        }
                    }
                }

                // 3. Safe Route Options
                Text(
                    text = if (isAdminMode) "Choose Other Optimal Safe Route:" else "Select Safe Bypass Road:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                state.primarySafeRoute?.let { safe1 ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = RpSafeGreenBg,
                        border = BorderStroke(1.5.dp, RpSafeGreen),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isAdminMode) {
                                        "Option 1: Recommended Optimal Safe Route (${safe1.algorithm.displayName})"
                                    } else {
                                        "Best Safe Route (Recommended)"
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF065F46),
                                    modifier = Modifier.weight(1f)
                                )
                                Surface(
                                    color = RpSafeGreen,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "100% SAFE",
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            if (isAdminMode) {
                                Text(
                                    text = "Path: ${safe1.nodePath.joinToString(" → ")}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = "Distance: ${safe1.distanceKm} km • Est. Time: ${safe1.estimatedTimeMin.toInt()} min • Cost: ${safe1.totalCost}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF334155)
                                )
                            } else {
                                Text(
                                    text = "${safe1.estimatedTimeMin.toInt()} mins (${safe1.distanceKm} km) • Avoids Closed Bridge",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF0F172A)
                                )
                            }
                            Button(
                                onClick = { onSelectSafeRoute(safe1) },
                                colors = ButtonDefaults.buttonColors(containerColor = RpSafeGreen),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .testTag("select_safe_optimal_route_1_button")
                            ) {
                                Icon(Icons.Default.AltRoute, contentDescription = null, tint = Color.White, modifier = Modifier.size(17.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isAdminMode) "Choose This Optimal Safe Route →" else "Take Safe Route →",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                state.secondarySafeRoute?.takeIf { it.nodePath != state.primarySafeRoute?.nodePath }?.let { safe2 ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, RpPrimaryBlue),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Alternate Safe Route 2",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = RpPrimaryBlue
                            )
                            if (isAdminMode) {
                                Text(
                                    text = "Path: ${safe2.nodePath.joinToString(" → ")}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Distance: ${safe2.distanceKm} km • Est. Time: ${safe2.estimatedTimeMin.toInt()} min • Cost: ${safe2.totalCost}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Text(
                                    text = "${safe2.estimatedTimeMin.toInt()} mins (${safe2.distanceKm} km) • Safe Bypass Road",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Button(
                                onClick = { onSelectSafeRoute(safe2) },
                                colors = ButtonDefaults.buttonColors(containerColor = RpPrimaryBlue),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp)
                                    .testTag("select_safe_optimal_route_2_button")
                            ) {
                                Text(
                                    text = "Take Alternate Route 2 →",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close", fontSize = 12.sp)
                }
            }
        }
    }
}


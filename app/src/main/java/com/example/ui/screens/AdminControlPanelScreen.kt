package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.AlgorithmRouteResult
import com.example.model.AppScreen
import com.example.model.CitizenHazardReport
import com.example.model.HazardSeverity
import com.example.model.RoadEdge
import com.example.model.RoadStatusType
import com.example.model.SensorMode
import com.example.model.VehicleType
import com.example.ui.theme.RpBlockedRed
import com.example.ui.theme.RpBlockedRedBg
import com.example.ui.theme.RpCriticalOrange
import com.example.ui.theme.RpPrimaryBlue
import com.example.ui.theme.RpPurpleAccent
import com.example.ui.theme.RpSafeGreen
import com.example.ui.theme.RpSafeGreenBg
import com.example.ui.theme.RpWarningYellow
import com.example.ui.theme.RpWarningYellowBg

@Composable
fun AdminLoginDialog(
    visible: Boolean,
    onAuthenticate: (String, String) -> Boolean,
    onQuickDemoLogin: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!visible) return

    var adminId by remember { mutableStateOf("admin@routepilot.gov") }
    var pinCode by remember { mutableStateOf("1234") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.5.dp, RpPurpleAccent)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(RpPurpleAccent.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = "Admin Portal",
                            tint = RpPurpleAccent,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Traffic Authority Admin Login",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Authorized Road & Bridge Control Panel",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider()

                OutlinedTextField(
                    value = adminId,
                    onValueChange = { adminId = it },
                    label = { Text("Authority Admin ID") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_id_input")
                )

                OutlinedTextField(
                    value = pinCode,
                    onValueChange = {
                        pinCode = it
                        errorMessage = null
                    },
                    label = { Text("Security Access PIN (Default: 1234)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_pin_input")
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = RpBlockedRed
                    )
                }

                Button(
                    onClick = {
                        val ok = onAuthenticate(adminId, pinCode)
                        if (!ok) {
                            errorMessage = "Invalid PIN. Use default PIN: 1234"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RpPurpleAccent),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("admin_login_submit_button")
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Login to Admin Panel →",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }

                OutlinedButton(
                    onClick = onQuickDemoLogin,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, RpPrimaryBlue),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("admin_quick_demo_login_button")
                ) {
                    Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = RpPrimaryBlue, modifier = Modifier.size(17.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "1-Tap Viva / Demo Admin Login",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = RpPrimaryBlue
                    )
                }

                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Stay in User / Driver Panel", fontSize = 12.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CitizenHazardReportDialog(
    visible: Boolean,
    edges: List<RoadEdge>,
    onSubmitReport: (edgeId: String, roadName: String, hazardType: String, severity: HazardSeverity, description: String) -> Unit,
    onDismiss: () -> Unit
) {
    if (!visible) return

    val keyCorridors = remember(edges) {
        val preferredIds = listOf("B-C", "H-I", "H-P", "C-F", "D-K", "Q-R")
        val found = edges.filter { it.id in preferredIds }
        if (found.isNotEmpty()) found else edges.take(6)
    }

    var selectedEdge by remember(keyCorridors) {
        mutableStateOf(keyCorridors.firstOrNull())
    }
    var selectedHazardType by remember { mutableStateOf("Bridge / Road Surface Hazard") }
    var selectedSeverity by remember { mutableStateOf(HazardSeverity.WARNING) }
    var notes by remember { mutableStateOf("") }

    val hazardPresets = listOf(
        "Bridge Deck Vibration / Crack",
        "Road Construction Barricade",
        "Waterlogging / Flood on Causeway",
        "Lane Obstruction / Accident"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.5.dp, RpCriticalOrange)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ReportProblem,
                        contentDescription = null,
                        tint = RpCriticalOrange,
                        modifier = Modifier.size(26.dp)
                    )
                    Column {
                        Text(
                            text = "Report Hazard to Admin Panel",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Driver Citizen Report • Sent to Traffic Control Authority",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider()

                Text(
                    text = "1. Select Road or Bridge Segment:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    keyCorridors.forEach { edge ->
                        val isSelected = selectedEdge?.id == edge.id
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) RpPrimaryBlue else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable { selectedEdge = edge }
                        ) {
                            Text(
                                text = "${edge.id} (${edge.roadName.take(16)})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Text(
                    text = "2. Select Hazard Type:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    hazardPresets.forEach { preset ->
                        val isSelected = selectedHazardType == preset
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) RpCriticalOrange else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable { selectedHazardType = preset }
                        ) {
                            Text(
                                text = preset,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Text(
                    text = "3. Severity Level:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(HazardSeverity.WARNING, HazardSeverity.CRITICAL).forEach { sev ->
                        val isSelected = selectedSeverity == sev
                        val chipColor = if (sev == HazardSeverity.CRITICAL) RpBlockedRed else RpWarningYellow
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) chipColor else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable { selectedSeverity = sev }
                        ) {
                            Text(
                                text = sev.label.uppercase(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Observation / Location Notes (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            val edge = selectedEdge
                            if (edge != null) {
                                onSubmitReport(
                                    edge.id,
                                    edge.roadName,
                                    selectedHazardType,
                                    selectedSeverity,
                                    notes
                                )
                            }
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RpCriticalOrange),
                        modifier = Modifier
                            .weight(1.4f)
                            .testTag("submit_citizen_report_button")
                    ) {
                        Text("Send to Admin", fontWeight = FontWeight.ExtraBold, color = Color.White)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminControlPanelScreen(
    edges: List<RoadEdge>,
    activeRoute: AlgorithmRouteResult?,
    sensorMode: SensorMode,
    emergencyBroadcast: String?,
    citizenReports: List<CitizenHazardReport>,
    onSetRoadStatusOverride: (edgeId: String, status: RoadStatusType, reason: String) -> Unit,
    onResetAllRoadOverrides: () -> Unit,
    onUpdateEmergencyBroadcast: (String?) -> Unit,
    onVerifyCitizenReport: (String) -> Unit,
    onResolveCitizenReport: (String) -> Unit,
    onSwitchToUserPanel: () -> Unit,
    onLogoutAdmin: () -> Unit,
    onNavigate: (AppScreen) -> Unit
) {
    val scrollState = rememberScrollState()
    var broadcastInput by remember(emergencyBroadcast) {
        mutableStateOf(
            emergencyBroadcast
                ?: "TRAFFIC AUTHORITY ADVISORY: Bridge B1 (B-C) under structural vibration alert. Follow Safe Optimal Route A → D → H → P → Q → M → T."
        )
    }

    val safeCount = edges.count { it.status == RoadStatusType.OPEN }
    val warnRestrictedCount = edges.count {
        it.status == RoadStatusType.WARNING || it.status == RoadStatusType.RESTRICTED
    }
    val blockedCount = edges.count { it.status == RoadStatusType.BLOCKED }
    val pendingReportCount = citizenReports.count { it.status == "PENDING_VERIFICATION" }

    val monitoredCorridors = remember(edges) {
        val priorityIds = listOf("B-C", "H-I", "H-P", "C-F", "D-K", "Q-R", "A-B", "P-Q")
        val priorityEdges = priorityIds.mapNotNull { id -> edges.find { it.id == id } }
        if (priorityEdges.isNotEmpty()) priorityEdges else edges.take(8)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Traffic Authority Admin Header Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B)),
            border = BorderStroke(1.5.dp, RpPurpleAccent)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(RpPurpleAccent.copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = "Admin Authority",
                                tint = Color(0xFFC4B5FD),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "TRAFFIC AUTHORITY ADMIN PANEL",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Text(
                                text = "Central Bridge & Road Network Control • ID: ADMIN-DEL-01",
                                fontSize = 11.sp,
                                color = Color(0xFFCBD5E1)
                            )
                        }
                    }

                    Surface(
                        color = RpSafeGreen,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "AUTHORIZED",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // KPI Grid inside Admin Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AdminKpiBox(
                        value = "$safeCount/${edges.size}",
                        label = "Safe Links",
                        color = Color(0xFF34D399),
                        modifier = Modifier.weight(1f)
                    )
                    AdminKpiBox(
                        value = "$warnRestrictedCount",
                        label = "Warn / Restr.",
                        color = Color(0xFFFBBF24),
                        modifier = Modifier.weight(1f)
                    )
                    AdminKpiBox(
                        value = "$blockedCount",
                        label = "Blocked",
                        color = Color(0xFFF87171),
                        modifier = Modifier.weight(1f)
                    )
                    AdminKpiBox(
                        value = "$pendingReportCount",
                        label = "User Reports",
                        color = Color(0xFF38BDF8),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onSwitchToUserPanel,
                        colors = ButtonDefaults.buttonColors(containerColor = RpPrimaryBlue),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1.4f)
                            .height(42.dp)
                            .testTag("admin_switch_to_user_panel_button")
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Switch to User Panel",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }

                    OutlinedButton(
                        onClick = onLogoutAdmin,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFF94A3B8)),
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Logout Admin", fontSize = 12.sp, color = Color.White)
                    }
                }
            }
        }

        // 2. Active User Route Impact Banner (Shows how Admin changes affect User Panel Optimal Route)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, RpPrimaryBlue)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "LIVE USER PANEL OPTIMAL PATH PREVIEW",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = RpPrimaryBlue
                    )
                    Text(
                        text = "Mode: ${sensorMode.displayTitle}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = activeRoute?.nodePath?.joinToString(" → ") ?: "A → D → H → P → Q → M → T",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Distance: ${activeRoute?.distanceKm ?: 14.5} km • Est. Time: ${activeRoute?.estimatedTimeMin?.toInt() ?: 21} min • Cost: ${activeRoute?.totalCost ?: 17.8}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 3. Live Bridge & Road Network Status Control (Admin Override)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Bridge & Road Network Control (Admin Only)",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Block, Restrict, or Open any Bridge/Road — User Panel routes update instantly",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedButton(
                        onClick = onResetAllRoadOverrides,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset", fontSize = 11.sp)
                    }
                }

                HorizontalDivider()

                monitoredCorridors.forEach { edge ->
                    val statusColor = when (edge.status) {
                        RoadStatusType.OPEN -> RpSafeGreen
                        RoadStatusType.WARNING -> RpWarningYellow
                        RoadStatusType.RESTRICTED -> RpCriticalOrange
                        RoadStatusType.BLOCKED -> RpBlockedRed
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.55f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${if (edge.isBridge) "🌉" else "🛣️"} ${edge.roadName} (${edge.id})",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = edge.statusReason.ifBlank { "Normal traffic flow" },
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Surface(
                                    color = statusColor,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = edge.status.label.uppercase(),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    RoadStatusType.OPEN to RpSafeGreen,
                                    RoadStatusType.WARNING to RpWarningYellow,
                                    RoadStatusType.RESTRICTED to RpCriticalOrange,
                                    RoadStatusType.BLOCKED to RpBlockedRed
                                ).forEach { (targetStatus, btnColor) ->
                                    val isCurrent = edge.status == targetStatus
                                    Surface(
                                        shape = RoundedCornerShape(7.dp),
                                        color = if (isCurrent) btnColor else MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(1.dp, btnColor),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                val reason = when (targetStatus) {
                                                    RoadStatusType.OPEN -> "Cleared & Opened by Admin Authority"
                                                    RoadStatusType.WARNING -> "Admin Caution Advisory on ${edge.id}"
                                                    RoadStatusType.RESTRICTED -> "Admin Lane Restriction on ${edge.id}"
                                                    RoadStatusType.BLOCKED -> "Closed by Traffic Authority (${edge.id})"
                                                }
                                                onSetRoadStatusOverride(edge.id, targetStatus, reason)
                                            }
                                            .testTag("admin_edge_${edge.id}_${targetStatus.name.lowercase()}")
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.padding(vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = targetStatus.label,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (isCurrent) Color.White else btnColor
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Emergency Broadcast Controller (Pushes live alert banner to User Panel)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Campaign,
                        contentDescription = null,
                        tint = RpCriticalOrange,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Emergency Broadcast to User Panel",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Broadcast real-time safety advisories to all connected drivers",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                OutlinedTextField(
                    value = broadcastInput,
                    onValueChange = { broadcastInput = it },
                    label = { Text("Emergency Advisory Message") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onUpdateEmergencyBroadcast(broadcastInput) },
                        colors = ButtonDefaults.buttonColors(containerColor = RpCriticalOrange),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1.4f)
                    ) {
                        Text("Broadcast to Users", fontWeight = FontWeight.ExtraBold, color = Color.White, fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = { onUpdateEmergencyBroadcast(null) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Clear Broadcast", fontSize = 12.sp)
                    }
                }
            }
        }

        // 5. Citizen Hazard Reports Queue (Sent from User Panel)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Citizen Driver Reports (From User Panel)",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                citizenReports.forEach { report ->
                    val isVerified = report.status == "VERIFIED_ACTIVE"
                    val isResolved = report.status == "RESOLVED"
                    val badgeBg = when {
                        isResolved -> RpSafeGreenBg
                        isVerified -> RpBlockedRedBg
                        else -> RpWarningYellowBg
                    }
                    val badgeFg = when {
                        isResolved -> RpSafeGreen
                        isVerified -> RpBlockedRed
                        else -> Color(0xFFB45309)
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
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
                                    text = "${report.id} • ${report.roadName}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Surface(color = badgeBg, shape = RoundedCornerShape(6.dp)) {
                                    Text(
                                        text = report.status.replace("_", " "),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = badgeFg,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                            }
                            Text(
                                text = "${report.hazardType} (${report.severity.label})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = RpBlockedRed
                            )
                            Text(
                                text = report.description,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Reported by: ${report.reportedBy} • ${report.timestampLabel}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { onVerifyCitizenReport(report.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = RpBlockedRed),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(36.dp)
                                ) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Verify & Block Road", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                                OutlinedButton(
                                    onClick = { onResolveCitizenReport(report.id) },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(36.dp)
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = RpSafeGreen, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Resolve & Open", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RpSafeGreen)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 6. Admin Engineering & Telemetry Suite Shortcuts
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Admin Telemetry, Simulation & Algorithm Lab",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AdminQuickToolButton(
                        title = "Sensor Telemetry",
                        subtitle = "ESP32 & Virtual",
                        icon = Icons.Default.Sensors,
                        color = RpPrimaryBlue,
                        onClick = { onNavigate(AppScreen.SENSORS) },
                        modifier = Modifier.weight(1f)
                    )
                    AdminQuickToolButton(
                        title = "Test Scenarios",
                        subtitle = "Inject Hazards",
                        icon = Icons.Default.PlayCircleFilled,
                        color = RpPurpleAccent,
                        onClick = { onNavigate(AppScreen.TEST_SCENARIOS) },
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AdminQuickToolButton(
                        title = "A* vs Dijkstra",
                        subtitle = "Algorithm Lab",
                        icon = Icons.Default.CompareArrows,
                        color = RpSafeGreen,
                        onClick = { onNavigate(AppScreen.ALGORITHM_COMPARISON) },
                        modifier = Modifier.weight(1f)
                    )
                    AdminQuickToolButton(
                        title = "System & MATLAB",
                        subtitle = "Connectivity",
                        icon = Icons.Default.Wifi,
                        color = RpCriticalOrange,
                        onClick = { onNavigate(AppScreen.CONNECTIVITY) },
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AdminQuickToolButton(
                        title = "Live Network Map",
                        subtitle = "Inspect Corridors",
                        icon = Icons.Default.Map,
                        color = RpPrimaryBlue,
                        onClick = { onNavigate(AppScreen.LIVE_MAP) },
                        modifier = Modifier.weight(1f)
                    )
                    AdminQuickToolButton(
                        title = "Thresholds & ESP32",
                        subtitle = "Admin Settings",
                        icon = Icons.Default.Settings,
                        color = RpPurpleAccent,
                        onClick = { onNavigate(AppScreen.SETTINGS) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminKpiBox(
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = Color.White.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
            Text(
                text = label,
                fontSize = 10.sp,
                color = Color(0xFFE2E8F0)
            )
        }
    }
}

@Composable
private fun AdminQuickToolButton(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(imageVector = icon, contentDescription = title, tint = color, modifier = Modifier.size(22.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun UserSettingsScreen(
    selectedVehicle: VehicleType,
    isDarkTheme: Boolean,
    onSelectVehicle: (VehicleType) -> Unit,
    onToggleDarkTheme: (Boolean) -> Unit,
    onOpenCitizenReport: () -> Unit,
    onOpenSavedTrips: () -> Unit,
    onOpenHiddenAdminLogin: () -> Unit
) {
    val scrollState = rememberScrollState()
    var autoRerouteEnabled by remember { mutableStateOf(true) }
    var bridgeWarningPopupEnabled by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Default Vehicle Selection
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
                    text = "My Vehicle",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Select your vehicle so narrow or weight-restricted bridges are avoided automatically",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf(
                        Triple(VehicleType.CAR, "Car", Icons.Default.DirectionsCar),
                        Triple(VehicleType.BIKE, "Bike", Icons.Default.DirectionsBike),
                        Triple(VehicleType.VAN, "Van / Truck", Icons.Default.LocalShipping)
                    ).forEach { (vType, label, icon) ->
                        val isSelected = selectedVehicle == vType
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) RpPrimaryBlue else MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) RpPrimaryBlue else MaterialTheme.colorScheme.outline
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onSelectVehicle(vType) }
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = label,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Display & Safety Alert Preferences
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
                    text = "Display & Safety Preferences",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
                            contentDescription = null,
                            tint = RpPrimaryBlue,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Dark Night Theme",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Comfortable dark colors for night driving",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = isDarkTheme,
                        onCheckedChange = onToggleDarkTheme
                    )
                }

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Automatic Safe Rerouting",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Automatically switch to safe road when a bridge or road closes ahead",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = autoRerouteEnabled,
                        onCheckedChange = { autoRerouteEnabled = it }
                    )
                }

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Road & Bridge Warning Popups",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Show instant alert popup when approaching road work or unsafe bridge",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = bridgeWarningPopupEnabled,
                        onCheckedChange = { bridgeWarningPopupEnabled = it }
                    )
                }
            }
        }

        // 3. Trip History & Road Issue Reporting
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
                    text = "Driver Support",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                OutlinedButton(
                    onClick = onOpenSavedTrips,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                ) {
                    Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("View Saved Trips & Route History", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onOpenCitizenReport,
                    colors = ButtonDefaults.buttonColors(containerColor = RpCriticalOrange),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                ) {
                    Icon(Icons.Default.ReportProblem, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Report Road Block or Bridge Issue", fontWeight = FontWeight.ExtraBold, color = Color.White)
                }
            }
        }

        // 4. Discreet App Info Footer (Tapping this opens the PIN-protected Admin Panel)
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenHiddenAdminLogin() }
                .testTag("settings_hidden_admin_access_card")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "RoutePilot Navigation v1.0.4",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Build 2026.09 • System Diagnostics",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                    )
                }
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "System Diagnostics Access",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

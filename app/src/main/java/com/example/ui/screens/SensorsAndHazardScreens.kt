package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.HazardEntity
import com.example.model.DataSource
import com.example.model.HardwareConfiguration
import com.example.model.HardwareConnectionState
import com.example.model.HardwareTestConnectionResult
import com.example.model.HazardEvaluation
import com.example.model.HazardSeverity
import com.example.model.SensorMode
import com.example.model.SensorPacket
import com.example.model.SensorThresholdRule
import com.example.model.ThresholdConfig
import com.example.model.VirtualEngineState
import com.example.model.VirtualScenario
import com.example.ui.components.SensorModeSelectorCard
import com.example.ui.components.SeverityStatusBadge
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@Composable
fun SensorMonitoringScreen(
    sensorMode: SensorMode,
    esp32State: HardwareConnectionState,
    esp32LastSeenSec: Long?,
    hardwareConfig: HardwareConfiguration,
    hardwareTestResult: HardwareTestConnectionResult,
    liveHardwarePacket: SensorPacket?,
    virtualTestPacket: SensorPacket?,
    virtualEngineState: VirtualEngineState,
    selectedScenario: VirtualScenario,
    hardwareSeries: Map<String, List<Float>>,
    virtualSeries: Map<String, List<Float>>,
    thresholds: ThresholdConfig,
    onRequestModeSwitch: (SensorMode) -> Unit,
    onStartVirtual: (VirtualScenario) -> Unit,
    onPauseVirtual: () -> Unit,
    onStopVirtual: () -> Unit,
    onResetVirtual: () -> Unit,
    onTestHardwareConnection: () -> Unit,
    onConfigureHardware: () -> Unit,
    onOpenHazardScreen: () -> Unit
) {
    val scrollState = rememberScrollState()
    val isHardware = sensorMode == SensorMode.EXTERNAL_HARDWARE
    val activePacket = if (isHardware) liveHardwarePacket else virtualTestPacket
    val activeSeries = if (isHardware) hardwareSeries else virtualSeries
    val isConnectedHardware = isHardware && esp32State == HardwareConnectionState.CONNECTED && liveHardwarePacket != null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. Prominent Mode Selector at Top
        SensorModeSelectorCard(
            currentMode = sensorMode,
            esp32State = esp32State,
            onSelectMode = onRequestModeSwitch
        )

        // 2. Device / Simulation Status Header Card
        if (isHardware) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isConnectedHardware) {
                        RpSafeGreenBg.copy(alpha = 0.55f)
                    } else {
                        RpBlockedRedBg.copy(alpha = 0.65f)
                    }
                ),
                border = BorderStroke(
                    1.dp,
                    if (isConnectedHardware) RpSafeGreen else RpBlockedRed
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.DeveloperBoard,
                                contentDescription = "ESP32 Hardware",
                                tint = if (isConnectedHardware) RpSafeGreen else RpBlockedRed,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "EXTERNAL HARDWARE (${hardwareConfig.deviceId})",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = if (isConnectedHardware) {
                                        "LIVE SENSOR DATA • Last update: ${esp32LastSeenSec ?: 1}s ago"
                                    } else {
                                        "NO LIVE SENSOR DATA • ${esp32State.label}"
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isConnectedHardware) RpSafeGreen else RpBlockedRed
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = onConfigureHardware,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("configure_hardware_button")
                        ) {
                            Text("Configure Hardware", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (!hardwareConfig.isConfigured) {
                        // Mandatory message when ESP32 endpoint is not configured
                        Surface(
                            color = Color.White.copy(alpha = 0.94f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, RpBlockedRed.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("esp32_not_configured_banner")
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "ESP32 endpoint not configured.",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = RpBlockedRed
                                )
                                Text(
                                    text = "EXTERNAL HARDWARE • NO LIVE SENSOR DATA",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = "Configure ESP32_HTTP_ENDPOINT or ESP32_WS_ENDPOINT in Hardware Configuration to connect your physical ESP32 and sensors.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF334155)
                                )
                                Button(
                                    onClick = onConfigureHardware,
                                    colors = ButtonDefaults.buttonColors(containerColor = RpPrimaryBlue),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("configure_hardware_cta_button")
                                ) {
                                    Text("Configure Hardware", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else if (!isConnectedHardware) {
                        // Endpoint is configured, but ESP32 is not currently responding with live packets
                        Surface(
                            color = Color.White.copy(alpha = 0.94f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, RpBlockedRed.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("no_live_sensor_data_banner")
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "EXTERNAL HARDWARE • NO LIVE SENSOR DATA",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = RpBlockedRed
                                )
                                Text(
                                    text = "Endpoint (${hardwareConfig.connectionType.label}): ${hardwareConfig.httpEndpoint.ifBlank { hardwareConfig.wsEndpoint }}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF1E293B)
                                )
                                if (hardwareTestResult.tested || hardwareTestResult.isTesting) {
                                    Text(
                                        text = hardwareTestResult.statusSubtext,
                                        fontSize = 11.sp,
                                        color = Color(0xFF475569)
                                    )
                                } else {
                                    Text(
                                        text = "Waiting for live ESP32 sensor packets. No replacement or virtual values are generated in External Hardware Mode.",
                                        fontSize = 11.sp,
                                        color = Color(0xFF334155)
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = onTestHardwareConnection,
                                        colors = ButtonDefaults.buttonColors(containerColor = RpPrimaryBlue),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("sensor_screen_test_connection_button")
                                    ) {
                                        Text("Test Connection", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    OutlinedButton(
                                        onClick = onConfigureHardware,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Configure Hardware", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    } else if (liveHardwarePacket != null) {
                        // Connected & receiving live ESP32 sensor packets
                        Surface(
                            color = Color.White.copy(alpha = 0.92f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, RpSafeGreen.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "EXTERNAL HARDWARE • LIVE SENSOR DATA",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = RpSafeGreen
                                    )
                                    Text(
                                        text = String.format(
                                            Locale.US,
                                            "GPS: %.4f, %.4f • Battery: %.0f%% • %s",
                                            liveHardwarePacket.latitude,
                                            liveHardwarePacket.longitude,
                                            liveHardwarePacket.battery,
                                            hardwareConfig.connectionType.label
                                        ),
                                        fontSize = 11.sp,
                                        color = Color(0xFF1E293B)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Virtual Mode Header Bar matching Reference Image
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = RpSafeGreenBg,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeveloperBoard,
                                        contentDescription = null,
                                        tint = RpSafeGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "VIRTUAL MODE",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF065F46)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = if (virtualEngineState == VirtualEngineState.RUNNING) RpSafeGreenBg else RpWarningYellowBg,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (virtualEngineState == VirtualEngineState.RUNNING) RpSafeGreen else RpCriticalOrange
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = virtualEngineState.label,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A)
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = {
                                if (virtualEngineState == VirtualEngineState.RUNNING) {
                                    onStopVirtual()
                                } else {
                                    onStartVirtual(selectedScenario)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (virtualEngineState == VirtualEngineState.RUNNING) RpBlockedRed else RpSafeGreen
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(
                                imageVector = if (virtualEngineState == VirtualEngineState.RUNNING) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (virtualEngineState == VirtualEngineState.RUNNING) "Stop" else "Start",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Scenario: ${selectedScenario.title}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                            color = RpPurpleBg,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "TEST DATA — NOT LIVE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = RpPurpleAccent,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        }

        // 3. The 5 Physical / Virtual Sensor Cards + Road Construction Card matching Reference Image
        val dataSourceLabel = if (isHardware) {
            if (isConnectedHardware) "LIVE SENSOR DATA" else "NO LIVE SENSOR DATA"
        } else {
            "TEST DATA — NOT LIVE"
        }

        SensorTelemetryCard(
            title = "Vibration",
            valueText = activePacket?.let { String.format(Locale.US, "%.2f g", it.vibration) } ?: "-- g",
            rangeText = "(0 – ${thresholds.vibration.warning} g normal)",
            icon = Icons.Default.GraphicEq,
            iconTint = RpPrimaryBlue,
            series = activeSeries["vibration"] ?: emptyList(),
            severity = activePacket?.let { classifyValue(it.vibration, thresholds.vibration) } ?: HazardSeverity.SAFE,
            dataSourceBadge = dataSourceLabel,
            timestampText = activePacket?.timestampIso?.substringAfter("T")?.removeSuffix("Z") ?: "--:--:--"
        )

        SensorTelemetryCard(
            title = "Tilt",
            valueText = activePacket?.let { String.format(Locale.US, "%.1f °", it.tilt) } ?: "-- °",
            rangeText = "(0 – ${thresholds.tilt.warning}° normal)",
            icon = Icons.Default.RotateRight,
            iconTint = Color(0xFFD97706),
            series = activeSeries["tilt"] ?: emptyList(),
            severity = activePacket?.let { classifyValue(it.tilt, thresholds.tilt) } ?: HazardSeverity.SAFE,
            dataSourceBadge = dataSourceLabel,
            timestampText = activePacket?.timestampIso?.substringAfter("T")?.removeSuffix("Z") ?: "--:--:--"
        )

        SensorTelemetryCard(
            title = "Strain",
            valueText = activePacket?.let { String.format(Locale.US, "%.0f µε", it.strain) } ?: "-- µε",
            rangeText = "(0 – ${thresholds.strain.warning.toInt()} µε normal)",
            icon = Icons.Default.Compress,
            iconTint = RpSafeGreen,
            series = activeSeries["strain"] ?: emptyList(),
            severity = activePacket?.let { classifyValue(it.strain, thresholds.strain) } ?: HazardSeverity.SAFE,
            dataSourceBadge = dataSourceLabel,
            timestampText = activePacket?.timestampIso?.substringAfter("T")?.removeSuffix("Z") ?: "--:--:--"
        )

        SensorTelemetryCard(
            title = "Displacement",
            valueText = activePacket?.let { String.format(Locale.US, "%.1f mm", it.displacement) } ?: "-- mm",
            rangeText = "(0 – ${thresholds.displacement.warning} mm normal)",
            icon = Icons.Default.CompareArrows,
            iconTint = RpBlockedRed,
            series = activeSeries["displacement"] ?: emptyList(),
            severity = activePacket?.let { classifyValue(it.displacement, thresholds.displacement) } ?: HazardSeverity.SAFE,
            dataSourceBadge = dataSourceLabel,
            timestampText = activePacket?.timestampIso?.substringAfter("T")?.removeSuffix("Z") ?: "--:--:--"
        )

        SensorTelemetryCard(
            title = "Water Level",
            valueText = activePacket?.let { String.format(Locale.US, "%.0f cm", it.waterLevel) } ?: "-- cm",
            rangeText = "(0 – ${thresholds.waterLevel.warning.toInt()} cm normal)",
            icon = Icons.Default.WaterDrop,
            iconTint = Color(0xFF0284C7),
            series = activeSeries["waterLevel"] ?: emptyList(),
            severity = activePacket?.let { classifyValue(it.waterLevel, thresholds.waterLevel) } ?: HazardSeverity.SAFE,
            dataSourceBadge = dataSourceLabel,
            timestampText = activePacket?.timestampIso?.substringAfter("T")?.removeSuffix("Z") ?: "--:--:--"
        )

        // Road Construction Status Card matching Reference Image
        val constructionActive = activePacket?.roadConstructionActive == true
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenHazardScreen() }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(RpPurpleBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Construction,
                            contentDescription = "Road Construction",
                            tint = RpPurpleAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Road Construction",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (activePacket == null) {
                                "NO LIVE SENSOR DATA"
                            } else if (constructionActive) {
                                "Active (Link H - I)"
                            } else {
                                "Inactive"
                            },
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                SeverityStatusBadge(
                    severity = if (constructionActive) HazardSeverity.WARNING else HazardSeverity.SAFE
                )
            }
        }

        // Real-Time Multi-Sensor Graph Card matching Dark Mode Reference Image (Vibration, Tilt, Strain)
        RealTimeMultiSensorGraphCard(
            vibrationSeries = activeSeries["vibration"] ?: emptyList(),
            tiltSeries = activeSeries["tilt"] ?: emptyList(),
            strainSeries = activeSeries["strain"] ?: emptyList()
        )

        // Virtual Mode Quick Scenario & Simulation Controls at Bottom matching Reference Image
        if (!isHardware) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickScenarioPill(
                    title = "Normal",
                    selected = selectedScenario == VirtualScenario.NORMAL,
                    onClick = { onStartVirtual(VirtualScenario.NORMAL) },
                    modifier = Modifier.weight(1f)
                )
                QuickScenarioPill(
                    title = "High Vibration",
                    selected = selectedScenario == VirtualScenario.HIGH_VIBRATION,
                    onClick = { onStartVirtual(VirtualScenario.HIGH_VIBRATION) },
                    modifier = Modifier.weight(1f)
                )
                QuickScenarioPill(
                    title = "High Water",
                    selected = selectedScenario == VirtualScenario.HIGH_WATER_LEVEL,
                    onClick = { onStartVirtual(VirtualScenario.HIGH_WATER_LEVEL) },
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onStartVirtual(selectedScenario) },
                    colors = ButtonDefaults.buttonColors(containerColor = RpSafeGreen),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Start", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onPauseVirtual,
                    colors = ButtonDefaults.buttonColors(containerColor = RpWarningYellow),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Pause, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Pause", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                }
                Button(
                    onClick = onResetVirtual,
                    colors = ButtonDefaults.buttonColors(containerColor = RpPrimaryBlue),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reset", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun RealTimeMultiSensorGraphCard(
    vibrationSeries: List<Float>,
    tiltSeries: List<Float>,
    strainSeries: List<Float>
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Real-Time Sensor Graph",
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                    .padding(10.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val gridColor = Color(0xFF64748B).copy(alpha = 0.22f)
                    for (r in 1..3) {
                        val y = size.height * (r / 4f)
                        drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.5f)
                    }

                    fun drawNormalizedSeries(pts: List<Float>, maxRef: Float, color: Color) {
                        if (pts.size < 2) return
                        val p = Path()
                        pts.forEachIndexed { idx, v ->
                            val x = (idx.toFloat() / (pts.size - 1).coerceAtLeast(1)) * size.width
                            val normY = (v / maxRef).coerceIn(0.05f, 0.95f)
                            val y = size.height - (normY * size.height)
                            if (idx == 0) p.moveTo(x, y) else p.lineTo(x, y)
                        }
                        drawPath(p, color = color, style = Stroke(width = 4f, cap = StrokeCap.Round))
                    }

                    drawNormalizedSeries(vibrationSeries, 1.1f, RpBlockedRed)
                    drawNormalizedSeries(tiltSeries, 6.0f, Color(0xFFF59E0B))
                    drawNormalizedSeries(strainSeries, 450f, Color(0xFF38BDF8))
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                GraphLegendDot("Vibration", RpBlockedRed)
                GraphLegendDot("Tilt", Color(0xFFF59E0B))
                GraphLegendDot("Strain", Color(0xFF38BDF8))
            }
        }
    }
}

@Composable
private fun GraphLegendDot(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SensorTelemetryCard(
    title: String,
    valueText: String,
    rangeText: String,
    icon: ImageVector,
    iconTint: Color,
    series: List<Float>,
    severity: HazardSeverity,
    dataSourceBadge: String,
    timestampText: String
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1.1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(iconTint.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = valueText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "$rangeText • $timestampText",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Live Sparkline Graph
            Box(
                modifier = Modifier
                    .width(96.dp)
                    .height(42.dp)
                    .padding(horizontal = 6.dp)
            ) {
                SparklineGraph(
                    points = series,
                    lineColor = iconTint
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                SeverityStatusBadge(severity = severity)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = dataSourceBadge,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        dataSourceBadge.contains("TEST DATA") -> RpPurpleAccent
                        dataSourceBadge.contains("LIVE SENSOR DATA") && !dataSourceBadge.contains("NO ") -> RpSafeGreen
                        else -> RpBlockedRed
                    }
                )
            }
        }
    }
}

@Composable
private fun SparklineGraph(
    points: List<Float>,
    lineColor: Color
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        if (points.size < 2) {
            drawLine(
                color = lineColor.copy(alpha = 0.35f),
                start = Offset(0f, size.height / 2f),
                end = Offset(size.width, size.height / 2f),
                strokeWidth = 3f
            )
            return@Canvas
        }
        val minVal = (points.minOrNull() ?: 0f) * 0.9f
        val maxVal = ((points.maxOrNull() ?: 1f) * 1.1f).coerceAtLeast(minVal + 0.01f)
        val path = Path()
        val fillPath = Path()

        points.forEachIndexed { index, v ->
            val x = (index.toFloat() / (points.size - 1).coerceAtLeast(1)) * size.width
            val normY = ((v - minVal) / (maxVal - minVal)).coerceIn(0f, 1f)
            val y = size.height - (normY * (size.height * 0.8f) + size.height * 0.1f)
            if (index == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, size.height)
                fillPath.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }
        fillPath.lineTo(size.width, size.height)
        fillPath.close()

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(lineColor.copy(alpha = 0.28f), Color.Transparent)
            )
        )
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 4.5f, cap = StrokeCap.Round)
        )
    }
}

@Composable
private fun QuickScenarioPill(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        color = if (selected) RpPrimaryBlue.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, if (selected) RpPrimaryBlue else MaterialTheme.colorScheme.outline)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) RpPrimaryBlue else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private fun classifyValue(value: Double, rule: SensorThresholdRule): HazardSeverity {
    val v = abs(value)
    return when {
        v >= rule.critical -> HazardSeverity.CRITICAL
        v >= rule.warning -> HazardSeverity.WARNING
        else -> HazardSeverity.SAFE
    }
}

@Composable
fun HazardDetectionScreen(
    sensorMode: SensorMode,
    activeHazards: List<HazardEntity>,
    activeEval: HazardEvaluation?,
    onViewOnMap: () -> Unit
) {
    val scrollState = rememberScrollState()
    val primaryHazard = activeHazards.firstOrNull()
    val isHardware = sensorMode == SensorMode.EXTERNAL_HARDWARE
    val nowFormatted = SimpleDateFormat("dd MMM yyyy, hh:mm:ss a", Locale.US).format(Date())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Red/Orange Alert Card matching Reference Image (Light & Dark Mode)
        val hasHazard = primaryHazard != null || (activeEval?.hazardDetected == true)
        val alertBgColor = if (hasHazard) {
            RpBlockedRed.copy(alpha = 0.14f)
        } else {
            RpSafeGreen.copy(alpha = 0.14f)
        }
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = alertBgColor
            ),
            border = BorderStroke(1.5.dp, if (hasHazard) RpBlockedRed else RpSafeGreen),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(if (hasHazard) RpBlockedRed else RpSafeGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Hazard Status",
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = if (hasHazard) "Hazard Detected" else "No Active Hazards",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (hasHazard) RpBlockedRed else RpSafeGreen
                    )
                    Text(
                        text = if (hasHazard) {
                            activeEval?.description ?: primaryHazard?.hazardTitle ?: "Bridge B1 structural vibration high"
                        } else if (isHardware) {
                            "No live hardware hazard detected (Connect ESP32)"
                        } else {
                            "All monitored corridors operating within safe thresholds"
                        },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = primaryHazard?.timestampFormatted ?: nowFormatted,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Detailed Hazard Attributes Table matching Reference Image
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
                HazardDetailRow(
                    label = "Hazard Type",
                    value = if (hasHazard) {
                        primaryHazard?.hazardTitle ?: activeEval?.hazardType?.displayTitle ?: "Bridge Structural"
                    } else {
                        "None (Normal)"
                    }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Severity",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(110.dp)
                    )
                    Text(text = ":  ", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    SeverityStatusBadge(
                        severity = if (hasHazard) {
                            activeEval?.severity ?: HazardSeverity.CRITICAL
                        } else {
                            HazardSeverity.SAFE
                        }
                    )
                }

                HazardDetailRow(
                    label = "Sensor",
                    value = if (hasHazard) {
                        primaryHazard?.sensorTypesLabel ?: activeEval?.triggeredSensors?.joinToString(", ") ?: "Vibration, Tilt, Strain"
                    } else {
                        if (isHardware) "ESP32-001 (No Live Data)" else "Virtual Sensor Array"
                    }
                )

                HazardDetailRow(
                    label = "Values",
                    value = if (hasHazard) {
                        primaryHazard?.sensorValuesLabel ?: activeEval?.formattedValues ?: "0.85 g, 4.2°, 320 µε"
                    } else {
                        if (isHardware) "NO LIVE SENSOR DATA" else "Within Normal Limits"
                    }
                )

                HazardDetailRow(
                    label = "Location",
                    value = if (hasHazard) {
                        primaryHazard?.roadReference ?: activeEval?.roadReference ?: "Bridge B1 (B - C)"
                    } else {
                        "All Corridors Clear"
                    }
                )

                HazardDetailRow(
                    label = "Road Status",
                    value = if (hasHazard) {
                        primaryHazard?.roadStatus ?: activeEval?.roadStatus?.label ?: "Blocked"
                    } else {
                        "Open / Safe"
                    },
                    valueColor = if (hasHazard) RpBlockedRed else RpSafeGreen
                )

                HazardDetailRow(
                    label = "Data Source",
                    value = if (isHardware) {
                        "LIVE_HARDWARE (ESP32)"
                    } else {
                        "VIRTUAL_TEST (TEST DATA — NOT LIVE)"
                    },
                    valueColor = if (isHardware) RpPrimaryBlue else RpPurpleAccent
                )
            }
        }

        // Active Hazards Summary List matching Dark Mode Reference Image
        if (hasHazard && !isHardware) {
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
                        text = "Active Hazards",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    ActiveHazardSummaryItem(
                        title = "Bridge B1 (Critical)",
                        badgeText = "Blocked",
                        badgeColor = RpBlockedRed,
                        icon = Icons.Default.Warning
                    )
                    ActiveHazardSummaryItem(
                        title = "Road Construction (Warning)",
                        badgeText = "Restricted",
                        badgeColor = Color(0xFFD97706),
                        icon = Icons.Default.Construction
                    )
                    ActiveHazardSummaryItem(
                        title = "Water Level (Warning)",
                        badgeText = "25 cm",
                        badgeColor = Color(0xFFD97706),
                        icon = Icons.Default.WaterDrop
                    )
                }
            }
        }

        // Bridge Structural Visual Asset matching Reference Image
        Card(
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier
                .fillMaxWidth()
                .height(155.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_bridge_structural_1790392059599),
                contentDescription = "Monitored Highway Bridge Structure",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Button(
            onClick = onViewOnMap,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = RpPrimaryBlue),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("hazard_view_on_map_button")
        ) {
            Icon(
                imageVector = Icons.Default.Map,
                contentDescription = null,
                tint = Color.White
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "View on Map",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
private fun ActiveHazardSummaryItem(
    title: String,
    badgeText: String,
    badgeColor: Color,
    icon: ImageVector
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = badgeColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Surface(
            color = badgeColor,
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = badgeText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun HazardDetailRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(110.dp)
        )
        Text(
            text = ":  ",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
    }
}

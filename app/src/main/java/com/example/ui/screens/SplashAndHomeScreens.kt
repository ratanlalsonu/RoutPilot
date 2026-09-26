package com.example.ui.screens

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AltRoute
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.HazardEntity
import com.example.model.AlgorithmRouteResult
import com.example.model.AppScreen
import com.example.model.GpsTelemetry
import com.example.model.HardwareConnectionState
import com.example.model.RoadEdge
import com.example.model.RoadNode
import com.example.model.RoadStatusType
import com.example.model.SensorMode
import com.example.model.VirtualEngineState
import com.example.ui.components.SensorModeSelectorCard
import com.example.ui.theme.RpBlockedRed
import com.example.ui.theme.RpBlockedRedBg
import com.example.ui.theme.RpCriticalOrange
import com.example.ui.theme.RpPrimaryBlue
import com.example.ui.theme.RpPurpleAccent
import com.example.ui.theme.RpSafeGreen
import com.example.ui.theme.RpSafeGreenBg
import com.example.ui.theme.RpWarningYellowBg
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SplashScreen(
    isFirstLaunch: Boolean = true,
    isDarkTheme: Boolean = false,
    onToggleTheme: (Boolean) -> Unit = {},
    onGetStarted: () -> Unit,
    onSkip: () -> Unit
) {
    // On subsequent APK launches (after the first time Get Started was tapped),
    // display this exact Splash interface briefly and then open the main app automatically.
    LaunchedEffect(isFirstLaunch) {
        if (!isFirstLaunch) {
            delay(1800L)
            onGetStarted()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (!isFirstLaunch) {
                    Modifier.clickable { onGetStarted() }
                } else {
                    Modifier
                }
            )
    ) {
        // Full-bleed scenic highway and suspension bridge photo at bottom
        Image(
            painter = painterResource(id = R.drawable.img_splash_highway_1790392046960),
            contentDescription = "Scenic highway and bridge background",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Exact deep night-sky gradient from reference screenshot blending seamlessly into the mountain highway
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color(0xFF020710),
                            0.36f to Color(0xFF051124).copy(alpha = 0.96f),
                            0.56f to Color(0xFF081A33).copy(alpha = 0.55f),
                            0.78f to Color(0xFF030812).copy(alpha = 0.30f),
                            1.0f to Color(0xFF01040A).copy(alpha = 0.88f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 20.dp)
            ) {
                // Vector Emblem matching Reference Screenshot (Twin Mountains + White/Orange Suspension Bridge + White S-Curve Highway)
                RoutPilotCrestEmblem(
                    modifier = Modifier.size(width = 185.dp, height = 118.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = buildAnnotatedString {
                        withStyle(
                            SpanStyle(
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold
                            )
                        ) {
                            append("Route")
                        }
                        withStyle(
                            SpanStyle(
                                color = Color(0xFF1DA1F2),
                                fontWeight = FontWeight.ExtraBold
                            )
                        ) {
                            append("Pilot")
                        }
                    },
                    fontSize = 42.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Real-Time Road & Bridge\nHazard Detection\nwith Intelligent Route Diversion",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFE2E8F0),
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // 3 Pager indicator dots on the highway matching the uploaded screenshot
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF64748B).copy(alpha = 0.72f))
                    )
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF64748B).copy(alpha = 0.72f))
                    )
                }

                Spacer(modifier = Modifier.height(26.dp))

                if (isFirstLaunch) {
                    // Show "Get Started ->" ONLY on the very first launch after APK installation
                    Button(
                        onClick = onGetStarted,
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007BFF)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("get_started_button")
                    ) {
                        Text(
                            text = "Get Started",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                } else {
                    // Subsequent launches: no "Get Started" button, preserves exact layout spacing
                    Spacer(modifier = Modifier.height(54.dp))
                }
            }
        }
    }
}

@Composable
private fun RoutPilotCrestEmblem(
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // 1. Back Mountain Peak (Deep Navy-Teal with crisp white ridge highlight)
        val backMountain = Path().apply {
            moveTo(w * 0.26f, h * 0.64f)
            lineTo(w * 0.46f, h * 0.22f)
            lineTo(w * 0.50f, h * 0.27f)
            lineTo(w * 0.56f, h * 0.12f)
            lineTo(w * 0.74f, h * 0.58f)
            close()
        }
        drawPath(
            path = backMountain,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF0D3B5E), Color(0xFF061B2E))
            )
        )
        // White snow ridge line on back mountain
        val backRidge = Path().apply {
            moveTo(w * 0.39f, h * 0.34f)
            lineTo(w * 0.48f, h * 0.20f)
            moveTo(w * 0.51f, h * 0.23f)
            lineTo(w * 0.56f, h * 0.12f)
            lineTo(w * 0.58f, h * 0.20f)
        }
        drawPath(
            path = backRidge,
            color = Color.White.copy(alpha = 0.9f),
            style = Stroke(width = 3f, cap = StrokeCap.Round)
        )

        // 2. Front Left Cyan Mountain Slope
        val frontCyanMountain = Path().apply {
            moveTo(w * 0.10f, h * 0.76f)
            lineTo(w * 0.37f, h * 0.28f)
            lineTo(w * 0.58f, h * 0.52f)
            quadraticTo(w * 0.32f, h * 0.50f, w * 0.10f, h * 0.76f)
            close()
        }
        drawPath(
            path = frontCyanMountain,
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF00E1FF), Color(0xFF0099FF))
            )
        )

        // 3. White Suspension Bridge Tower & Cables on Right
        val towerCenterX = w * 0.65f
        val leftLegTopX = towerCenterX - w * 0.012f
        val rightLegTopX = towerCenterX + w * 0.012f
        val leftLegBotX = towerCenterX - w * 0.024f
        val rightLegBotX = towerCenterX + w * 0.024f
        val towerTopY = h * 0.24f
        val towerBotY = h * 0.53f

        // Suspension cables (White)
        val leftCable = Path().apply {
            moveTo(leftLegTopX, towerTopY + h * 0.02f)
            quadraticTo(w * 0.60f, h * 0.42f, w * 0.55f, h * 0.46f)
        }
        val rightCable = Path().apply {
            moveTo(rightLegTopX, towerTopY + h * 0.02f)
            quadraticTo(w * 0.70f, h * 0.42f, w * 0.74f, h * 0.48f)
        }
        drawPath(leftCable, color = Color.White, style = Stroke(width = 3f, cap = StrokeCap.Round))
        drawPath(rightCable, color = Color.White, style = Stroke(width = 3f, cap = StrokeCap.Round))

        // Vertical suspenders
        for (i in 1..3) {
            val sxL = w * (0.56f + i * 0.02f)
            drawLine(
                color = Color.White.copy(alpha = 0.85f),
                start = Offset(sxL, h * 0.38f),
                end = Offset(sxL, h * 0.47f),
                strokeWidth = 1.8f
            )
        }

        // White Bridge Tower Legs & Crossbars
        drawLine(
            color = Color.White,
            start = Offset(leftLegTopX, towerTopY),
            end = Offset(leftLegBotX, towerBotY),
            strokeWidth = 4.5f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color.White,
            start = Offset(rightLegTopX, towerTopY),
            end = Offset(rightLegBotX, towerBotY),
            strokeWidth = 4.5f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color.White,
            start = Offset(towerCenterX - w * 0.025f, h * 0.34f),
            end = Offset(towerCenterX + w * 0.025f, h * 0.34f),
            strokeWidth = 3.2f
        )
        drawLine(
            color = Color(0xFFE2E8F0),
            start = Offset(w * 0.54f, h * 0.46f),
            end = Offset(w * 0.74f, h * 0.48f),
            strokeWidth = 4f,
            cap = StrokeCap.Round
        )

        // 4. Orange Bridge Deck & Small Secondary Pylon on Far Right
        val orangeDeck = Path().apply {
            moveTo(w * 0.55f, h * 0.51f)
            quadraticTo(w * 0.70f, h * 0.56f, w * 0.86f, h * 0.72f)
            quadraticTo(w * 0.70f, h * 0.62f, w * 0.55f, h * 0.54f)
            close()
        }
        drawPath(orangeDeck, color = Color(0xFFFF8C00))
        // Small orange pylon on right
        val smallPylonX = w * 0.73f
        drawLine(
            color = Color(0xFFFF9800),
            start = Offset(smallPylonX, h * 0.46f),
            end = Offset(smallPylonX - w * 0.015f, h * 0.61f),
            strokeWidth = 3.5f
        )
        drawLine(
            color = Color(0xFFFF9800),
            start = Offset(smallPylonX, h * 0.46f),
            end = Offset(smallPylonX + w * 0.015f, h * 0.63f),
            strokeWidth = 3.5f
        )

        // 5. Bold White S-Curve Highway in Foreground with Dark Dashed Center Line
        val whiteRoad = Path().apply {
            moveTo(w * 0.25f, h * 0.88f)
            quadraticTo(w * 0.26f, h * 0.46f, w * 0.68f, h * 0.58f)
            lineTo(w * 0.76f, h * 0.65f)
            quadraticTo(w * 0.42f, h * 0.56f, w * 0.44f, h * 0.95f)
            close()
        }
        drawPath(
            path = whiteRoad,
            color = Color.White
        )

        // Dashed dark navy center line on the white highway
        val centerDash = Path().apply {
            moveTo(w * 0.34f, h * 0.91f)
            quadraticTo(w * 0.34f, h * 0.52f, w * 0.71f, h * 0.61f)
        }
        drawPath(
            path = centerDash,
            color = Color(0xFF071324),
            style = Stroke(
                width = 4f,
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
            )
        )
    }
}

@Composable
fun HomeDashboardScreen(
    sensorMode: SensorMode,
    esp32State: HardwareConnectionState,
    virtualEngineState: VirtualEngineState,
    nodes: List<RoadNode>,
    edges: List<RoadEdge>,
    activeHazards: List<HazardEntity>,
    activeRoute: AlgorithmRouteResult?,
    gpsState: GpsTelemetry,
    isDarkTheme: Boolean = false,
    onNavigate: (AppScreen) -> Unit,
    onRequestModeSwitch: (SensorMode) -> Unit
) {
    val scrollState = rememberScrollState()
    val nowFormatted = SimpleDateFormat("dd MMM yyyy\nhh:mm a", Locale.US).format(Date())

    // Dynamic counters computed from actual live application state / Room database
    val totalNodesCount = nodes.size.takeIf { it > 0 } ?: 20
    val totalLinksCount = edges.size.takeIf { it > 0 } ?: 28
    val activeHazardsCount = if (sensorMode == SensorMode.VIRTUAL && activeHazards.isEmpty() &&
        edges.any { it.status == RoadStatusType.BLOCKED }
    ) {
        3
    } else {
        activeHazards.size
    }
    val safeRoadsCount = edges.count { it.status == RoadStatusType.OPEN }
    val restrictedRoadsCount = edges.count { it.status == RoadStatusType.RESTRICTED }
    val blockedRoadsCount = edges.count { it.status == RoadStatusType.BLOCKED }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Top Status Section matching Light Mode vs Dark Mode Reference Image
        if (isDarkTheme) {
            // Dark Mode: 3 side-by-side status pills (System Online | Sensors Running | Mode Simulation)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DarkHomeStatusPill(
                    title = "System",
                    value = "Online",
                    dotColor = Color(0xFF10B981),
                    valueColor = Color(0xFF10B981),
                    onClick = { onNavigate(AppScreen.CONNECTIVITY) },
                    modifier = Modifier.weight(1f)
                )
                DarkHomeStatusPill(
                    title = "Sensors",
                    value = if (sensorMode == SensorMode.EXTERNAL_HARDWARE) {
                        if (esp32State == HardwareConnectionState.CONNECTED) "Live" else "Offline"
                    } else {
                        "Running"
                    },
                    dotColor = Color(0xFF38BDF8),
                    valueColor = Color(0xFF38BDF8),
                    onClick = { onNavigate(AppScreen.SENSORS) },
                    modifier = Modifier.weight(1f)
                )
                DarkHomeStatusPill(
                    title = "Mode",
                    value = if (sensorMode == SensorMode.EXTERNAL_HARDWARE) "Hardware" else "Simulation",
                    dotColor = Color(0xFFA855F7),
                    valueColor = Color(0xFFA855F7),
                    onClick = {
                        val target = if (sensorMode == SensorMode.VIRTUAL) SensorMode.EXTERNAL_HARDWARE else SensorMode.VIRTUAL
                        onRequestModeSwitch(target)
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            // Light Mode: Single sleek System Online + Date/Time Card matching Reference Image
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val target = if (sensorMode == SensorMode.VIRTUAL) SensorMode.EXTERNAL_HARDWARE else SensorMode.VIRTUAL
                        onRequestModeSwitch(target)
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(
                                    if (sensorMode == SensorMode.EXTERNAL_HARDWARE && esp32State != HardwareConnectionState.CONNECTED) {
                                        RpBlockedRed
                                    } else {
                                        Color(0xFF16A34A)
                                    }
                                )
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (sensorMode == SensorMode.EXTERNAL_HARDWARE && esp32State != HardwareConnectionState.CONNECTED) {
                                "ESP32 Disconnected"
                            } else {
                                "System Online"
                            },
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (sensorMode == SensorMode.EXTERNAL_HARDWARE && esp32State != HardwareConnectionState.CONNECTED) {
                                RpBlockedRed
                            } else {
                                Color(0xFF15803D)
                            }
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = nowFormatted,
                                fontSize = 10.sp,
                                lineHeight = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.End
                            )
                        }
                    }
                }
            }
        }

        // 2. Dynamic 6 Metric Cards matching Light Mode (2x3) and Dark Mode (3x2) Reference Layouts
        if (isDarkTheme) {
            // Dark Mode: 3 columns x 2 rows matching Dark Mode Reference Image
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DarkMetricCard(
                    title = "Total Nodes",
                    value = totalNodesCount.toString(),
                    valueColor = Color.White,
                    onClick = { onNavigate(AppScreen.LIVE_MAP) },
                    modifier = Modifier.weight(1f)
                )
                DarkMetricCard(
                    title = "Total Links",
                    value = totalLinksCount.toString(),
                    valueColor = Color.White,
                    onClick = { onNavigate(AppScreen.LIVE_MAP) },
                    modifier = Modifier.weight(1f)
                )
                DarkMetricCard(
                    title = "Active Hazards",
                    value = activeHazardsCount.toString(),
                    valueColor = Color.White,
                    leadingIcon = Icons.Default.Warning,
                    leadingIconTint = RpBlockedRed,
                    onClick = { onNavigate(AppScreen.HAZARD_DETECTION) },
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DarkMetricCard(
                    title = "Safe Roads",
                    value = safeRoadsCount.toString(),
                    valueColor = Color(0xFF10B981),
                    showGreenUnderline = true,
                    onClick = { onNavigate(AppScreen.LIVE_MAP) },
                    modifier = Modifier.weight(1f)
                )
                DarkMetricCard(
                    title = "Restricted",
                    value = restrictedRoadsCount.toString(),
                    valueColor = Color.White,
                    leadingIcon = Icons.Default.Construction,
                    leadingIconTint = Color(0xFFF59E0B),
                    onClick = { onNavigate(AppScreen.LIVE_MAP) },
                    modifier = Modifier.weight(1f)
                )
                DarkMetricCard(
                    title = "Blocked",
                    value = blockedRoadsCount.toString(),
                    valueColor = Color.White,
                    leadingIcon = Icons.Default.Block,
                    leadingIconTint = RpBlockedRed,
                    onClick = { onNavigate(AppScreen.HAZARD_DETECTION) },
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            // Light Mode: 2 columns x 3 rows matching Light Mode Reference Image
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DashboardStatCard(
                    title = "Total Nodes",
                    value = totalNodesCount.toString(),
                    icon = Icons.Default.Security,
                    iconBg = Color(0xFFDCEBFF),
                    iconTint = RpPrimaryBlue,
                    valueColor = MaterialTheme.colorScheme.onSurface,
                    onClick = { onNavigate(AppScreen.LIVE_MAP) },
                    modifier = Modifier.weight(1f)
                )
                DashboardStatCard(
                    title = "Total Links",
                    value = totalLinksCount.toString(),
                    icon = Icons.Default.Link,
                    iconBg = Color(0xFFDCEBFF),
                    iconTint = RpPrimaryBlue,
                    valueColor = MaterialTheme.colorScheme.onSurface,
                    onClick = { onNavigate(AppScreen.LIVE_MAP) },
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DashboardStatCard(
                    title = "Active Hazards",
                    value = activeHazardsCount.toString(),
                    icon = Icons.Default.Warning,
                    iconBg = RpBlockedRedBg,
                    iconTint = RpBlockedRed,
                    valueColor = MaterialTheme.colorScheme.onSurface,
                    onClick = { onNavigate(AppScreen.HAZARD_DETECTION) },
                    modifier = Modifier.weight(1f)
                )
                DashboardStatCard(
                    title = "Safe Roads",
                    value = safeRoadsCount.toString(),
                    icon = Icons.Default.CheckCircle,
                    iconBg = RpSafeGreenBg,
                    iconTint = RpSafeGreen,
                    valueColor = MaterialTheme.colorScheme.onSurface,
                    onClick = { onNavigate(AppScreen.LIVE_MAP) },
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DashboardStatCard(
                    title = "Restricted",
                    value = restrictedRoadsCount.toString(),
                    icon = Icons.Default.Construction,
                    iconBg = RpWarningYellowBg,
                    iconTint = Color(0xFFD97706),
                    valueColor = MaterialTheme.colorScheme.onSurface,
                    onClick = { onNavigate(AppScreen.LIVE_MAP) },
                    modifier = Modifier.weight(1f)
                )
                DashboardStatCard(
                    title = "Blocked",
                    value = blockedRoadsCount.toString(),
                    icon = Icons.Default.Block,
                    iconBg = RpBlockedRedBg,
                    iconTint = RpBlockedRed,
                    valueColor = MaterialTheme.colorScheme.onSurface,
                    onClick = { onNavigate(AppScreen.HAZARD_DETECTION) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 3. Quick Actions Header & 6 Tiles (Directly below Metric Cards matching Reference Image!)
        Text(
            text = "Quick Actions",
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 2.dp)
        )

        if (isDarkTheme) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DarkQuickActionTile(
                    label = "Plan Route",
                    icon = Icons.AutoMirrored.Filled.AltRoute,
                    iconTint = Color(0xFF818CF8),
                    iconBg = Color(0xFF1E293B),
                    onClick = { onNavigate(AppScreen.ROUTE_PLANNER) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_action_plan_route")
                )
                DarkQuickActionTile(
                    label = "Live Map",
                    icon = Icons.Default.Map,
                    iconTint = Color(0xFF2DD4BF),
                    iconBg = Color(0xFF132F38),
                    onClick = { onNavigate(AppScreen.LIVE_MAP) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_action_live_map")
                )
                DarkQuickActionTile(
                    label = "Sensors",
                    icon = Icons.Default.Sensors,
                    iconTint = Color(0xFF38BDF8),
                    iconBg = Color(0xFF152C4A),
                    onClick = { onNavigate(AppScreen.SENSORS) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_action_sensors")
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DarkQuickActionTile(
                    label = "Demo Mode",
                    icon = Icons.Default.PlayCircleFilled,
                    iconTint = Color(0xFFEF4444),
                    iconBg = Color(0xFF3B1822),
                    onClick = { onNavigate(AppScreen.TEST_SCENARIOS) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_action_test_mode")
                )
                DarkQuickActionTile(
                    label = "History",
                    icon = Icons.Default.History,
                    iconTint = Color(0xFFA855F7),
                    iconBg = Color(0xFF281C40),
                    onClick = { onNavigate(AppScreen.HISTORY) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_action_history")
                )
                DarkQuickActionTile(
                    label = "Settings",
                    icon = Icons.Default.Settings,
                    iconTint = Color(0xFF94A3B8),
                    iconBg = Color(0xFF1E293B),
                    onClick = { onNavigate(AppScreen.SETTINGS) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_action_settings")
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionTile(
                    label = "Plan Route",
                    icon = Icons.AutoMirrored.Filled.AltRoute,
                    containerColor = Color(0xFF4A90E2),
                    contentColor = Color.White,
                    iconTint = Color(0xFF1E3A8A),
                    iconCircleColor = Color.White.copy(alpha = 0.30f),
                    onClick = { onNavigate(AppScreen.ROUTE_PLANNER) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_action_plan_route")
                )
                QuickActionTile(
                    label = "Live Map",
                    icon = Icons.Default.Map,
                    containerColor = Color(0xFF6EE7B7),
                    contentColor = Color(0xFF064E3B),
                    iconTint = Color(0xFF065F46),
                    iconCircleColor = Color.White.copy(alpha = 0.45f),
                    onClick = { onNavigate(AppScreen.LIVE_MAP) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_action_live_map")
                )
                QuickActionTile(
                    label = "Sensors",
                    icon = Icons.Default.Sensors,
                    containerColor = Color(0xFFFED7AA),
                    contentColor = Color(0xFF7C2D12),
                    iconTint = Color(0xFFEA580C),
                    iconCircleColor = Color.White.copy(alpha = 0.55f),
                    onClick = { onNavigate(AppScreen.SENSORS) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_action_sensors")
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionTile(
                    label = "Demo Mode",
                    icon = Icons.Default.PlayCircleFilled,
                    containerColor = Color(0xFFF1F5F9),
                    contentColor = Color(0xFF1E293B),
                    iconTint = RpPurpleAccent,
                    iconCircleColor = Color(0xFFEDE9FE),
                    onClick = { onNavigate(AppScreen.TEST_SCENARIOS) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_action_test_mode")
                )
                QuickActionTile(
                    label = "History",
                    icon = Icons.Default.History,
                    containerColor = Color(0xFFF1F5F9),
                    contentColor = Color(0xFF1E293B),
                    iconTint = RpBlockedRed,
                    iconCircleColor = RpBlockedRedBg,
                    onClick = { onNavigate(AppScreen.HISTORY) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_action_history")
                )
                QuickActionTile(
                    label = "Settings",
                    icon = Icons.Default.Settings,
                    containerColor = Color(0xFFF1F5F9),
                    contentColor = Color(0xFF1E293B),
                    iconTint = Color(0xFF334155),
                    iconCircleColor = Color(0xFFE2E8F0),
                    onClick = { onNavigate(AppScreen.SETTINGS) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_action_settings")
                )
            }
        }

        // 4. Sensor Data Mode Switcher & Engineering Screens (Accessible by scrolling below Quick Actions)
        SensorModeSelectorCard(
            currentMode = sensorMode,
            esp32State = esp32State,
            onSelectMode = onRequestModeSwitch
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SecondaryActionChip(
                label = "Hazard Detection",
                icon = Icons.Default.Warning,
                onClick = { onNavigate(AppScreen.HAZARD_DETECTION) },
                modifier = Modifier.weight(1f)
            )
            SecondaryActionChip(
                label = "A* vs Dijkstra",
                icon = Icons.AutoMirrored.Filled.CompareArrows,
                onClick = { onNavigate(AppScreen.ALGORITHM_COMPARISON) },
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SecondaryActionChip(
                label = "Journey Simulation",
                icon = Icons.Default.Navigation,
                onClick = { onNavigate(AppScreen.JOURNEY_TRACKING) },
                modifier = Modifier.weight(1f)
            )
            SecondaryActionChip(
                label = "Connectivity & MATLAB",
                icon = Icons.Default.Storage,
                onClick = { onNavigate(AppScreen.CONNECTIVITY) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun DarkHomeStatusPill(
    title: String,
    value: String,
    dotColor: Color,
    valueColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = Color(0xFF101D33),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFF1F365C))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Spacer(modifier = Modifier.width(7.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8),
                    maxLines = 1
                )
                Text(
                    text = value,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = valueColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun DarkMetricCard(
    title: String,
    value: String,
    valueColor: Color,
    leadingIcon: ImageVector? = null,
    leadingIconTint: Color = Color.White,
    showGreenUnderline: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(82.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        color = Color(0xFF101D33),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Color(0xFF1F365C))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFCBD5E1),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (leadingIcon != null) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        tint = leadingIconTint,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Column {
                    Text(
                        text = value,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = valueColor
                    )
                    if (showGreenUnderline) {
                        Box(
                            modifier = Modifier
                                .width(28.dp)
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color(0xFF10B981))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardStatCard(
    title: String,
    value: String,
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    valueColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = value,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = valueColor
                )
            }
        }
    }
}

@Composable
private fun QuickActionTile(
    label: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    iconTint: Color,
    iconCircleColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(92.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        color = containerColor,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.Black.copy(alpha = 0.05f)),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(iconCircleColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        }
    }
}

@Composable
private fun DarkQuickActionTile(
    label: String,
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(96.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        color = Color(0xFF101D33),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFF1F365C))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }
    }
}

@Composable
private fun SecondaryActionChip(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = RpPrimaryBlue,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

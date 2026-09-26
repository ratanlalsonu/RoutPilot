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
    onAdminPortalClick: () -> Unit = {},
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
    emergencyBroadcast: String? = null,
    onQuickSelectDestination: (String) -> Unit = {},
    onOpenCitizenReport: () -> Unit = {},
    onOpenAdminPanel: () -> Unit = {},
    onNavigate: (AppScreen) -> Unit,
    onRequestModeSwitch: (SensorMode) -> Unit
) {
    val scrollState = rememberScrollState()
    val safeRoadsCount = edges.count { it.status == RoadStatusType.OPEN }
    val slowRoadsCount = edges.count {
        it.status == RoadStatusType.WARNING || it.status == RoadStatusType.RESTRICTED
    }
    val closedRoadsCount = edges.count { it.status == RoadStatusType.BLOCKED }

    val startName = nodes.firstOrNull { it.id == (activeRoute?.nodePath?.firstOrNull() ?: "A") }?.name
        ?.substringAfter(" - ")
        ?: "College Gate (My Location)"
    val currentDestId = activeRoute?.nodePath?.lastOrNull() ?: "T"
    val endName = nodes.firstOrNull { it.id == currentDestId }?.name
        ?.substringAfter(" - ")
        ?: "City Hospital"
    val distKm = activeRoute?.distanceKm ?: 14.5
    val timeMin = activeRoute?.estimatedTimeMin?.toInt() ?: 21

    val closedOrSlowEdges = edges.filter { it.status != RoadStatusType.OPEN }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Live Traffic Safety Advisory Banner (Simple, clear warning for drivers)
        if (!emergencyBroadcast.isNullOrBlank()) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDarkTheme) Color(0xFF3B1219) else RpBlockedRedBg
                ),
                border = BorderStroke(1.5.dp, RpBlockedRed),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigate(AppScreen.LIVE_MAP) }
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(RpBlockedRed),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Road Alert",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "⚠️ Bridge B1 Closed Ahead",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDarkTheme) Color(0xFFFCA5A5) else RpBlockedRed
                        )
                        Text(
                            text = "Safe alternate road is automatically selected for your trip.",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDarkTheme) Color(0xFFFECACA) else Color(0xFF7F1D1D)
                        )
                    }
                }
            }
        }

        // 2. Google Maps-Style Main Navigation Card ("Where to?")
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.5.dp, RpPrimaryBlue.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Where to?",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Safe route avoiding closed bridges",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        color = RpSafeGreenBg,
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, RpSafeGreen)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = RpSafeGreen,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "100% SAFE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = RpSafeGreen
                            )
                        }
                    }
                }

                // 1-Tap Quick Destination Buttons (Like Google Maps shortcuts — zero typing needed!)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val quickPlaces = listOf(
                        Triple("🏥 Hospital", "T", currentDestId == "T"),
                        Triple("🚉 Station", "N", currentDestId == "N"),
                        Triple("🛍️ Market", "I", currentDestId == "I"),
                        Triple("🎓 College", "A", currentDestId == "A")
                    )
                    quickPlaces.forEach { (label, nodeId, isSelected) ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) RpPrimaryBlue else MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) RpPrimaryBlue else MaterialTheme.colorScheme.outline
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onQuickSelectDestination(nodeId) }
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                // Start -> Destination Box (Tap to search or change location)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate(AppScreen.ROUTE_PLANNER) }
                        .testTag("quick_action_plan_route")
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(RpPrimaryBlue)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "From (Your Location)",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = startName,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = "Change",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = RpPrimaryBlue
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(RpSafeGreen)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "To (Destination)",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = endName,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = "Search Place",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = RpPrimaryBlue
                            )
                        }
                    }
                }

                // Big Travel Time & Distance Summary
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = RpSafeGreen.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, RpSafeGreen.copy(alpha = 0.4f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "$timeMin mins",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = RpSafeGreen
                            )
                            Text(
                                text = "Travel Time",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = RpPrimaryBlue.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, RpPrimaryBlue.copy(alpha = 0.4f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "$distKm km",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = RpPrimaryBlue
                            )
                            Text(
                                text = "Distance",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Primary Action Buttons: Start Go & View Live Map
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { onNavigate(AppScreen.JOURNEY_TRACKING) },
                        colors = ButtonDefaults.buttonColors(containerColor = RpSafeGreen),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1.2f)
                            .height(50.dp)
                            .testTag("start_navigation_home_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Navigation,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Start",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }

                    Button(
                        onClick = { onNavigate(AppScreen.LIVE_MAP) },
                        colors = ButtonDefaults.buttonColors(containerColor = RpPrimaryBlue),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("quick_action_live_map")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Map,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "View Map",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // 3. Simple 3-Box Road Condition Summary (Easy to understand for any driver)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DashboardStatCard(
                title = "Open Roads",
                value = safeRoadsCount.toString(),
                icon = Icons.Default.CheckCircle,
                iconBg = RpSafeGreenBg,
                iconTint = RpSafeGreen,
                valueColor = RpSafeGreen,
                onClick = { onNavigate(AppScreen.LIVE_MAP) },
                modifier = Modifier.weight(1f)
            )
            DashboardStatCard(
                title = "Road Work",
                value = slowRoadsCount.toString(),
                icon = Icons.Default.Construction,
                iconBg = RpWarningYellowBg,
                iconTint = Color(0xFFD97706),
                valueColor = Color(0xFFD97706),
                onClick = { onNavigate(AppScreen.LIVE_MAP) },
                modifier = Modifier.weight(1f)
            )
            DashboardStatCard(
                title = "Closed Bridge",
                value = closedRoadsCount.toString(),
                icon = Icons.Default.Block,
                iconBg = RpBlockedRedBg,
                iconTint = RpBlockedRed,
                valueColor = RpBlockedRed,
                onClick = { onNavigate(AppScreen.HAZARD_DETECTION) },
                modifier = Modifier.weight(1f)
            )
        }

        // 4. Live Road & Bridge Warnings Nearby (Plain, easy-to-read cards)
        if (closedOrSlowEdges.isNotEmpty()) {
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
                        text = "Road & Bridge Status Ahead",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    closedOrSlowEdges.take(3).forEach { edge ->
                        val isClosed = edge.status == RoadStatusType.BLOCKED
                        val badgeColor = if (isClosed) RpBlockedRed else RpCriticalOrange
                        val statusText = if (isClosed) "CLOSED" else "ROAD WORK"
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigate(AppScreen.LIVE_MAP) }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${if (edge.isBridge) "🌉 Bridge:" else "🛣️ Road:"} ${edge.roadName}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (isClosed) {
                                            "Closed for safety. Safe bypass road is selected."
                                        } else {
                                            "Slow traffic / road work ahead."
                                        },
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = badgeColor,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = statusText,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Simple Driver Help Actions (Report Road Problem / Saved Trips)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onOpenCitizenReport,
                colors = ButtonDefaults.buttonColors(containerColor = RpCriticalOrange),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("home_report_hazard_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Report Road Issue",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }

            SecondaryActionChip(
                label = "Recent Trips",
                icon = Icons.Default.History,
                onClick = { onNavigate(AppScreen.HISTORY) },
                modifier = Modifier
                    .weight(0.8f)
                    .height(48.dp)
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

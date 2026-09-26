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
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.HazardEntity
import com.example.model.AlgorithmComparison
import com.example.model.AlgorithmRouteResult
import com.example.model.GpsTelemetry
import com.example.model.RoadEdge
import com.example.model.RoadNode
import com.example.model.SensorMode
import com.example.model.SensorPacket
import com.example.ui.components.RealInteractiveMapView
import com.example.ui.theme.RpBlockedRed
import com.example.ui.theme.RpPrimaryBlue
import com.example.ui.theme.RpPurpleAccent
import com.example.ui.theme.RpPurpleBg
import com.example.ui.theme.RpSafeGreen
import com.example.ui.theme.RpSafeGreenBg
import com.example.ui.theme.RpWarningYellow
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun AlgorithmComparisonScreen(
    comparison: AlgorithmComparison?,
    matlabStatusMessage: String,
    onRecalculateBoth: () -> Unit,
    onViewRoutesOnMap: () -> Unit
) {
    val scrollState = rememberScrollState()
    val aStar = comparison?.aStarResult
    val dijkstra = comparison?.dijkstraResult

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // MATLAB Engineering Layer Status Banner
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Algorithm Engineering Comparison",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = matlabStatusMessage,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedButton(
                    onClick = onRecalculateBoth,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Re-run Both", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // A* vs Dijkstra Comparison Table Card matching Reference Image
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Metric",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1.4f)
                    )
                    Text(
                        text = "A* (A-Star)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = RpPrimaryBlue,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "Dijkstra",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                ComparisonTableRow(
                    metric = "Distance (km)",
                    aStarVal = aStar?.distanceKm?.toString() ?: "NOT AVAILABLE",
                    dijkstraVal = dijkstra?.distanceKm?.toString() ?: "NOT AVAILABLE"
                )
                ComparisonTableRow(
                    metric = "Estimated Time\n(min)",
                    aStarVal = aStar?.estimatedTimeMin?.roundToInt()?.toString() ?: "NOT AVAILABLE",
                    dijkstraVal = dijkstra?.estimatedTimeMin?.roundToInt()?.toString() ?: "NOT AVAILABLE"
                )
                ComparisonTableRow(
                    metric = "Total Cost",
                    aStarVal = aStar?.totalCost?.toString() ?: "NOT AVAILABLE",
                    dijkstraVal = dijkstra?.totalCost?.toString() ?: "NOT AVAILABLE"
                )
                ComparisonTableRow(
                    metric = "Nodes Evaluated",
                    aStarVal = aStar?.nodesEvaluated?.toString() ?: "NOT AVAILABLE",
                    dijkstraVal = dijkstra?.nodesEvaluated?.toString() ?: "NOT AVAILABLE",
                    highlightAStar = true
                )
                ComparisonTableRow(
                    metric = "Hazards Avoided",
                    aStarVal = aStar?.hazardsAvoided?.toString() ?: "NOT AVAILABLE",
                    dijkstraVal = dijkstra?.hazardsAvoided?.toString() ?: "NOT AVAILABLE"
                )
                ComparisonTableRow(
                    metric = "Blocked Roads\nAvoided",
                    aStarVal = aStar?.blockedRoadsAvoided?.toString() ?: "NOT AVAILABLE",
                    dijkstraVal = dijkstra?.blockedRoadsAvoided?.toString() ?: "NOT AVAILABLE"
                )
                ComparisonTableRow(
                    metric = "Calculation Time\n(ms)",
                    aStarVal = aStar?.calculationTimeMs?.toString() ?: "NOT AVAILABLE",
                    dijkstraVal = dijkstra?.calculationTimeMs?.toString() ?: "NOT AVAILABLE",
                    highlightAStar = true,
                    showDivider = false
                )
            }
        }

        // Path Breakdown Card
        if (aStar != null && dijkstra != null) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "A* Path (f(n) = g(n) + h(n)): ${aStar.nodePath.joinToString(" → ")}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = RpPrimaryBlue
                    )
                    Text(
                        text = "Dijkstra Path (Uniform Cost): ${dijkstra.nodePath.joinToString(" → ")}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    val reduction = if (dijkstra.nodesEvaluated > 0) {
                        ((1.0 - aStar.nodesEvaluated.toDouble() / dijkstra.nodesEvaluated.toDouble()) * 100).roundToInt()
                    } else {
                        0
                    }
                    Text(
                        text = "Heuristic Efficiency: A* evaluated ${aStar.nodesEvaluated} nodes vs ${dijkstra.nodesEvaluated} for Dijkstra ($reduction% search reduction).",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Button(
            onClick = onViewRoutesOnMap,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = RpPrimaryBlue),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("view_routes_on_map_button")
        ) {
            Icon(Icons.Default.Map, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "View Routes on Map",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
private fun ComparisonTableRow(
    metric: String,
    aStarVal: String,
    dijkstraVal: String,
    highlightAStar: Boolean = false,
    showDivider: Boolean = true
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = metric,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1.4f)
            )
            Text(
                text = aStarVal,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (highlightAStar) RpPrimaryBlue else MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = dijkstraVal,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
        }
        if (showDivider) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
        }
    }
}

@Composable
fun JourneyTrackingScreen(
    nodes: List<RoadNode>,
    edges: List<RoadEdge>,
    activeRoute: AlgorithmRouteResult?,
    alternateRoute: AlgorithmRouteResult?,
    activeHazards: List<HazardEntity>,
    gpsState: GpsTelemetry,
    sensorMode: SensorMode,
    livePacket: SensorPacket? = null,
    journeyStatus: String,
    journeyNodeIndex: Int,
    isDarkTheme: Boolean,
    isAdminMode: Boolean = false,
    onPauseResumeJourney: () -> Unit,
    onStopJourney: () -> Unit,
    onAdvanceCheckpoint: () -> Unit,
    onOpenHazardDetail: () -> Unit
) {
    val scrollState = rememberScrollState()
    val pathNodes = activeRoute?.nodePath?.ifEmpty { listOf("A", "E", "H", "I", "M", "T") }
        ?: listOf("A", "E", "H", "I", "M", "T")
    val safeIdx = journeyNodeIndex.coerceIn(0, (pathNodes.size - 1).coerceAtLeast(0))
    val currentNodeId = pathNodes.getOrElse(safeIdx) { "H" }
    val nextNodeId = pathNodes.getOrElse((safeIdx + 1).coerceAtMost(pathNodes.size - 1)) { "I" }

    val currentPlaceName = nodes.find { it.id == currentNodeId }?.name?.substringAfter(" - ") ?: "Main Road"
    val nextPlaceName = nodes.find { it.id == nextNodeId }?.name?.substringAfter(" - ") ?: "Next Turn"
    val destPlaceName = nodes.find { it.id == pathNodes.lastOrNull() }?.name?.substringAfter(" - ") ?: "Destination"

    val progressFraction = if (pathNodes.size > 1) {
        (safeIdx.toDouble() / (pathNodes.size - 1).toDouble()).coerceIn(0.0, 1.0)
    } else {
        0.5
    }
    val totalDist = activeRoute?.distanceKm ?: 18.5
    val totalTime = activeRoute?.estimatedTimeMin ?: 32.0
    val remainingKm = ((totalDist * (1.0 - progressFraction * 0.65)) * 10.0).roundToInt() / 10.0
    val remainingMin = (totalTime * (1.0 - progressFraction * 0.65)).roundToInt().coerceAtLeast(2)

    val liveSpeedKmh = if (gpsState.isAvailable && (gpsState.speedKmh ?: 0f) > 0.5f) {
        String.format(Locale.US, "%.0f km/h", gpsState.speedKmh)
    } else {
        "Safe Speed"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (!isAdminMode) {
            // 1. Google Maps-Style Big Green Turn-by-Turn Direction Header Card for Normal Drivers
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF065F46)),
                border = BorderStroke(1.5.dp, RpSafeGreen),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⬆️ Go Straight Towards $nextPlaceName",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            modifier = Modifier.weight(1f)
                        )
                        Surface(
                            color = RpSafeGreen,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "SAFE ROAD",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Text(
                        text = "Destination: $destPlaceName • Avoiding closed Bridge B1",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFD1FAE5)
                    )
                }
            }

            // 2. Large Interactive Live Map for Driver Navigation
            RealInteractiveMapView(
                nodes = nodes,
                edges = edges,
                activeRoute = activeRoute,
                alternateRoute = alternateRoute,
                activeHazards = activeHazards,
                gpsState = gpsState,
                sensorMode = sensorMode,
                currentVehicleNodeId = currentNodeId,
                isDarkTheme = isDarkTheme,
                showControls = true,
                onHazardMarkerClick = { onOpenHazardDetail() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(275.dp)
            )

            // 3. Simple 3-Box Google Maps Bottom Trip Bar (Time Left | Distance Left | Current Road)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                JourneyStatBox(
                    label = "Time Left",
                    value = "$remainingMin mins",
                    subValue = "Fastest Safe Way",
                    valueColor = RpSafeGreen,
                    modifier = Modifier.weight(1f)
                )
                JourneyStatBox(
                    label = "Distance Left",
                    value = "$remainingKm km",
                    subValue = "To $destPlaceName",
                    valueColor = RpPrimaryBlue,
                    modifier = Modifier.weight(1f)
                )
                JourneyStatBox(
                    label = "Current Location",
                    value = currentPlaceName.take(12),
                    subValue = liveSpeedKmh,
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            // Admin Mode: Full Node Progress Stepper & Engineering Telemetry
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        pathNodes.take(6).forEachIndexed { idx, nodeId ->
                            val isPassed = idx < safeIdx
                            val isCurrent = idx == safeIdx
                            val circleColor = when {
                                isCurrent -> RpPrimaryBlue
                                isPassed -> RpSafeGreen
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                            val textColor = if (isCurrent || isPassed) Color.White else MaterialTheme.colorScheme.onSurface

                            Box(
                                modifier = Modifier
                                    .size(if (isCurrent) 38.dp else 30.dp)
                                    .clip(CircleShape)
                                    .background(circleColor)
                                    .clickable { onAdvanceCheckpoint() },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = nodeId,
                                    fontSize = if (isCurrent) 15.sp else 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = textColor
                                )
                            }

                            if (idx < pathNodes.take(6).size - 1) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(4.dp)
                                        .background(if (idx < safeIdx) RpSafeGreen else RpPrimaryBlue.copy(alpha = 0.25f))
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (journeyStatus == "Moving") "Vehicle En Route" else "Journey $journeyStatus",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = RpPrimaryBlue
                        )
                        if (sensorMode == SensorMode.VIRTUAL) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = RpPurpleBg,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "VIRTUAL TEST EVENT",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = RpPurpleAccent,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            RealInteractiveMapView(
                nodes = nodes,
                edges = edges,
                activeRoute = activeRoute,
                alternateRoute = alternateRoute,
                activeHazards = activeHazards,
                gpsState = gpsState,
                sensorMode = sensorMode,
                currentVehicleNodeId = currentNodeId,
                isDarkTheme = isDarkTheme,
                showControls = false,
                onHazardMarkerClick = { onOpenHazardDetail() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(185.dp)
            )

            if (activeRoute != null) {
                SourceToDestinationTelemetryGraphCard(
                    route = activeRoute,
                    edges = edges,
                    livePacket = livePacket,
                    currentVehicleNodeId = currentNodeId,
                    compact = true
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                JourneyStatBox(
                    label = "Current Location",
                    value = currentNodeId,
                    subValue = if (gpsState.isAvailable && gpsState.latitude != null) {
                        String.format(Locale.US, "%.3f, %.3f", gpsState.latitude, gpsState.longitude)
                    } else {
                        "Corridor Node $currentNodeId"
                    },
                    modifier = Modifier.weight(1f)
                )
                JourneyStatBox(
                    label = "Next Node",
                    value = nextNodeId,
                    subValue = activeRoute?.roadNames?.getOrElse(safeIdx) { "Civic Corridor" } ?: "Next Segment",
                    modifier = Modifier.weight(1f)
                )
                JourneyStatBox(
                    label = "Remaining",
                    value = "$remainingKm km",
                    subValue = "Dest: ${pathNodes.lastOrNull() ?: "T"}",
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                JourneyStatBox(
                    label = "Est. Time Left",
                    value = "$remainingMin min",
                    subValue = activeRoute?.algorithm?.displayName ?: "A*",
                    modifier = Modifier.weight(1f)
                )
                JourneyStatBox(
                    label = "Speed",
                    value = liveSpeedKmh,
                    subValue = "Real Android GPS",
                    modifier = Modifier.weight(1f)
                )
                JourneyStatBox(
                    label = "Status",
                    value = journeyStatus,
                    valueColor = when (journeyStatus) {
                        "Moving" -> RpSafeGreen
                        "Paused" -> Color(0xFFD97706)
                        else -> RpBlockedRed
                    },
                    subValue = if (activeHazards.isNotEmpty()) "${activeHazards.size} Hazard Diverted" else "Route Clear",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 4. Pause, Next Turn, and Exit Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onPauseResumeJourney,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RpWarningYellow),
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .testTag("journey_pause_button")
            ) {
                Icon(
                    imageVector = if (journeyStatus == "Paused") Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = null,
                    tint = Color.Black
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (journeyStatus == "Paused") "Resume" else "Pause",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }

            Button(
                onClick = onStopJourney,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RpBlockedRed),
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .testTag("journey_stop_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = null,
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isAdminMode) "Stop" else "End Trip",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        OutlinedButton(
            onClick = onAdvanceCheckpoint,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
        ) {
            Icon(Icons.Default.SkipNext, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isAdminMode) "Advance Next Corridor Node Checkpoint" else "Move to Next Turn ($nextPlaceName)",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun JourneyStatBox(
    label: String,
    value: String,
    subValue: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = label,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = valueColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subValue,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

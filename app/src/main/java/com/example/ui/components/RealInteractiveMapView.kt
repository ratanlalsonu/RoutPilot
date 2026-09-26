package com.example.ui.components

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Point
import android.graphics.RectF
import android.graphics.Typeface
import android.view.MotionEvent
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.data.local.HazardEntity
import com.example.model.AlgorithmRouteResult
import com.example.model.GpsTelemetry
import com.example.model.HazardSeverity
import com.example.model.RoadEdge
import com.example.model.RoadNode
import com.example.model.RoadStatusType
import com.example.model.SensorMode
import com.example.ui.theme.RpBlockedRed
import com.example.ui.theme.RpCriticalOrange
import com.example.ui.theme.RpPrimaryBlue
import com.example.ui.theme.RpRestrictedGray
import com.example.ui.theme.RpSafeGreen
import com.example.ui.theme.RpWarningYellow
import java.io.File
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay

/**
 * Full Native Interactive OpenStreetMap (`org.osmdroid.views.MapView`) Component.
 *
 * - Renders a clean, unobstructed OpenStreetMap viewport.
 * - Highlights the selected Optimal Path from Source to Destination with bold casing and directional arrows.
 * - Geo-anchors every Bridge, Road Construction site, and Hazard directly at its exact GPS latitude/longitude
 *   location on the map.
 */
@Composable
fun RealInteractiveMapView(
    nodes: List<RoadNode>,
    edges: List<RoadEdge>,
    activeRoute: AlgorithmRouteResult?,
    alternateRoute: AlgorithmRouteResult?,
    activeHazards: List<HazardEntity>,
    gpsState: GpsTelemetry,
    sensorMode: SensorMode,
    externalOsrmGeometry: List<Pair<Double, Double>> = emptyList(),
    currentVehicleNodeId: String = "H",
    isDarkTheme: Boolean = false,
    showControls: Boolean = true,
    onHazardMarkerClick: (HazardEntity?) -> Unit = {},
    onNodeClick: (RoadNode) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    remember(context) {
        val osmConfig = Configuration.getInstance()
        osmConfig.userAgentValue = "${context.packageName}/RoutPilot-OSM-2026"
        val baseDir = File(context.cacheDir, "osmdroid").apply { mkdirs() }
        val tileDir = File(baseDir, "tiles").apply { mkdirs() }
        osmConfig.osmdroidBasePath = baseDir
        osmConfig.osmdroidTileCache = tileDir
        true
    }

    var showRoadLayer by remember { mutableStateOf(true) }
    var currentZoomLevel by remember { mutableDoubleStateOf(13.3) }
    val mapViewRef = remember { mutableStateOf<MapView?>(null) }

    // Calculate center of the active Optimal Path so the full Source-to-Destination path & hazards are centered
    val routeNodes = remember(activeRoute, nodes) {
        val pathIds = activeRoute?.nodePath ?: emptyList()
        if (pathIds.isNotEmpty()) {
            nodes.filter { it.id in pathIds }
        } else {
            nodes
        }
    }

    val defaultCenterLat = if (routeNodes.isNotEmpty()) {
        routeNodes.map { it.latitude }.average()
    } else {
        gpsState.latitude ?: 28.6139
    }
    val defaultCenterLon = if (routeNodes.isNotEmpty()) {
        routeNodes.map { it.longitude }.average()
    } else {
        gpsState.longitude ?: 77.2090
    }

    val geoOverlay = remember {
        RoutPilotOsmGeoOverlay()
    }

    geoOverlay.nodes = nodes
    geoOverlay.edges = edges
    geoOverlay.activeRoute = activeRoute
    geoOverlay.alternateRoute = alternateRoute
    geoOverlay.activeHazards = activeHazards
    geoOverlay.gpsState = gpsState
    geoOverlay.sensorMode = sensorMode
    geoOverlay.externalOsrmGeometry = externalOsrmGeometry
    geoOverlay.currentVehicleNodeId = currentVehicleNodeId
    geoOverlay.showRoadLayer = showRoadLayer
    geoOverlay.isDarkTheme = isDarkTheme
    geoOverlay.onNodeClick = onNodeClick
    geoOverlay.onHazardClick = onHazardMarkerClick

    // Automatically center on the active optimal path whenever the selected optimal route changes
    LaunchedEffect(activeRoute?.nodePath) {
        if (routeNodes.isNotEmpty()) {
            val avgLat = routeNodes.map { it.latitude }.average()
            val avgLon = routeNodes.map { it.longitude }.average()
            mapViewRef.value?.controller?.animateTo(GeoPoint(avgLat, avgLon))
            mapViewRef.value?.invalidate()
        }
    }

    LaunchedEffect(externalOsrmGeometry) {
        if (externalOsrmGeometry.size >= 2) {
            val mid = externalOsrmGeometry[externalOsrmGeometry.size / 2]
            mapViewRef.value?.controller?.animateTo(GeoPoint(mid.first, mid.second))
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapViewRef.value?.onResume()
                Lifecycle.Event.ON_PAUSE -> mapViewRef.value?.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapViewRef.value?.onDetach()
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clipToBounds()
            .background(if (isDarkTheme) Color(0xFF0B1526) else Color(0xFFE8F1E5))
    ) {
        // 1. Full-Screen Native Interactive osmdroid MapView
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    isTilesScaledToDpi = true
                    minZoomLevel = 4.0
                    maxZoomLevel = 20.0
                    zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                    controller.setZoom(currentZoomLevel)
                    controller.setCenter(GeoPoint(defaultCenterLat, defaultCenterLon))

                    if (!overlays.contains(geoOverlay)) {
                        overlays.add(geoOverlay)
                    }

                    addMapListener(object : MapListener {
                        override fun onScroll(event: ScrollEvent?): Boolean = false
                        override fun onZoom(event: ZoomEvent?): Boolean {
                            if (event != null) {
                                currentZoomLevel = event.zoomLevel
                            }
                            return false
                        }
                    })

                    mapViewRef.value = this
                }
            },
            update = { mapView ->
                if (isDarkTheme) {
                    val darkMatrix = ColorMatrix(
                        floatArrayOf(
                            -0.85f, 0f, 0f, 0f, 225f,
                            0f, -0.82f, 0f, 0f, 235f,
                            0f, 0f, -0.72f, 0f, 250f,
                            0f, 0f, 0f, 1f, 0f
                        )
                    )
                    mapView.overlayManager.tilesOverlay.setColorFilter(ColorMatrixColorFilter(darkMatrix))
                } else {
                    mapView.overlayManager.tilesOverlay.setColorFilter(null)
                }

                if (!mapView.overlays.contains(geoOverlay)) {
                    mapView.overlays.add(geoOverlay)
                }
                mapView.invalidate()
            }
        )

        // 2. Compact Bottom-Left Map Controls (Zoom +/-, Recenter on Optimal Path, Layer Toggle)
        if (showControls) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White.copy(alpha = 0.94f),
                    shadowElevation = 4.dp,
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                ) {
                    Column {
                        IconButton(
                            onClick = { mapViewRef.value?.controller?.zoomIn() },
                            modifier = Modifier
                                .size(38.dp)
                                .testTag("map_zoom_in_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Zoom in", tint = Color(0xFF0F172A))
                        }
                        IconButton(
                            onClick = { mapViewRef.value?.controller?.zoomOut() },
                            modifier = Modifier
                                .size(38.dp)
                                .testTag("map_zoom_out_button")
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Zoom out", tint = Color(0xFF0F172A))
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White.copy(alpha = 0.94f),
                    shadowElevation = 4.dp,
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                ) {
                    Column {
                        IconButton(
                            onClick = {
                                mapViewRef.value?.controller?.setZoom(13.3)
                                mapViewRef.value?.controller?.animateTo(GeoPoint(defaultCenterLat, defaultCenterLon))
                            },
                            modifier = Modifier
                                .size(38.dp)
                                .testTag("map_recenter_button")
                        ) {
                            Icon(Icons.Default.MyLocation, contentDescription = "Center Optimal Path", tint = RpPrimaryBlue)
                        }
                        IconButton(
                            onClick = {
                                showRoadLayer = !showRoadLayer
                                mapViewRef.value?.invalidate()
                            },
                            modifier = Modifier
                                .size(38.dp)
                                .testTag("map_layer_toggle_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Layers,
                                contentDescription = "Toggle Background Network Layer",
                                tint = if (showRoadLayer) RpPrimaryBlue else Color(0xFF64748B)
                            )
                        }
                    }
                }
            }

            // 3. Compact Bottom-Right Legend & Quick-Pan Chips (Tap to center on Bridge B1 or Road Construction)
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(10.dp),
                shape = RoundedCornerShape(10.dp),
                color = Color.White.copy(alpha = 0.94f),
                shadowElevation = 4.dp,
                border = BorderStroke(1.dp, Color(0xFFCBD5E1))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable {
                                val bNode = nodes.find { it.id == "B" }
                                val cNode = nodes.find { it.id == "C" }
                                if (bNode != null && cNode != null) {
                                    mapViewRef.value?.controller?.animateTo(
                                        GeoPoint(
                                            (bNode.latitude + cNode.latitude) / 2.0,
                                            (bNode.longitude + cNode.longitude) / 2.0
                                        )
                                    )
                                }
                                onHazardMarkerClick(activeHazards.firstOrNull())
                            }
                            .testTag("map_hazard_callout_bridge")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(RpBlockedRed)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Bridge Hazard",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = RpBlockedRed
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable {
                                val hNode = nodes.find { it.id == "H" }
                                val iNode = nodes.find { it.id == "I" }
                                if (hNode != null && iNode != null) {
                                    mapViewRef.value?.controller?.animateTo(
                                        GeoPoint(
                                            (hNode.latitude + iNode.latitude) / 2.0,
                                            (hNode.longitude + iNode.longitude) / 2.0
                                        )
                                    )
                                }
                                onHazardMarkerClick(activeHazards.lastOrNull())
                            }
                            .testTag("map_hazard_callout_construction")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(RpWarningYellow)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Road Work",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFB45309)
                        )
                    }

                    val scaleKm = (40000.0 / 2.0.pow(currentZoomLevel) * 0.15).coerceAtLeast(0.1)
                    val scaleLabel = if (scaleKm >= 1.0) "${scaleKm.roundToInt()} km" else "${(scaleKm * 1000).roundToInt()} m"
                    Text(
                        text = "• $scaleLabel",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF475569)
                    )
                }
            }
        }
    }
}

/**
 * Custom `org.osmdroid.views.overlay.Overlay` that draws:
 * 1. The active Optimal Path from Source to Destination with high-contrast white casing, bold route line,
 *    and directional arrows (`➤`).
 * 2. Exact Geo-Anchored Markers & Pointer Callout Badges at the exact GPS location (`GeoPoint`) of:
 *    - Any Bridge on the Optimal Path or corridor (`🌉 Bridge B1`, `🌉 South Bridge H-P`, `🌉 East Viaduct C-F`, `🌉 Causeway Q-R`)
 *    - Any Road Construction site on the Optimal Path or corridor (`🚧 Road Construction H-I`)
 *    - Any Active Hazard on the Optimal Path or corridor (`⚠ CRITICAL HAZARD`, `⚠ WARNING`)
 *    - Source (`🟢 START`) and Destination (`🏁 DEST`) pins
 */
private class RoutPilotOsmGeoOverlay : Overlay() {
    var nodes: List<RoadNode> = emptyList()
    var edges: List<RoadEdge> = emptyList()
    var activeRoute: AlgorithmRouteResult? = null
    var alternateRoute: AlgorithmRouteResult? = null
    var activeHazards: List<HazardEntity> = emptyList()
    var gpsState: GpsTelemetry = GpsTelemetry()
    var sensorMode: SensorMode = SensorMode.VIRTUAL
    var externalOsrmGeometry: List<Pair<Double, Double>> = emptyList()
    var currentVehicleNodeId: String = "H"
    var showRoadLayer: Boolean = true
    var isDarkTheme: Boolean = false
    var onNodeClick: (RoadNode) -> Unit = {}
    var onHazardClick: (HazardEntity?) -> Unit = {}

    private val bgEdgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val routeCasingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = android.graphics.Color.WHITE
        strokeWidth = 22f
    }

    private val routePrimaryPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = android.graphics.Color.rgb(37, 99, 235) // RpPrimaryBlue
        strokeWidth = 13f
    }

    private val routeHazardSegmentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        strokeWidth = 15f
    }

    private val routeAlternatePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = android.graphics.Color.argb(170, 100, 116, 139)
        strokeWidth = 8f
        pathEffect = DashPathEffect(floatArrayOf(18f, 14f), 0f)
    }

    private val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = android.graphics.Color.WHITE
    }

    private val osrmPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = android.graphics.Color.argb(190, 14, 165, 233)
        strokeWidth = 12f
    }

    private val nodeFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val nodeBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    private val nodeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = 22f
    }

    private val gpsHaloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = android.graphics.Color.argb(60, 37, 99, 235)
    }

    private val gpsDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = android.graphics.Color.rgb(37, 99, 235)
    }

    private val hazardPulsePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val calloutBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = android.graphics.Color.argb(248, 255, 255, 255)
    }

    private val calloutBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    private val calloutTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = 20f
    }

    private val calloutSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = 16f
    }

    override fun draw(canvas: android.graphics.Canvas, mapView: MapView, shadow: Boolean) {
        if (shadow) return
        val projection = mapView.projection ?: return
        val nodeMap = nodes.associateBy { it.id }
        val pt1 = Point()
        val pt2 = Point()

        val primaryPath = activeRoute?.nodePath ?: emptyList()
        // Collect the set of undirected edge keys on the active Optimal Path
        val activePathEdgePairs = mutableSetOf<Pair<String, String>>()
        for (i in 0 until primaryPath.size - 1) {
            activePathEdgePairs.add(primaryPath[i] to primaryPath[i + 1])
            activePathEdgePairs.add(primaryPath[i + 1] to primaryPath[i])
        }

        // 1. Draw Live OSRM Real Road-Network Polyline (if computed from OSRM API)
        if (externalOsrmGeometry.size >= 2) {
            val osrmPath = Path()
            externalOsrmGeometry.forEachIndexed { index, (lat, lon) ->
                projection.toPixels(GeoPoint(lat, lon), pt1)
                if (index == 0) {
                    osrmPath.moveTo(pt1.x.toFloat(), pt1.y.toFloat())
                } else {
                    osrmPath.lineTo(pt1.x.toFloat(), pt1.y.toFloat())
                }
            }
            canvas.drawPath(osrmPath, osrmPaint)
        }

        // 2. Draw Road & Bridge Network Links (Subtle for non-route safe edges, Bold for Hazard / Construction / Bridge edges)
        edges.forEach { edge ->
            val nFrom = nodeMap[edge.fromNodeId] ?: return@forEach
            val nTo = nodeMap[edge.toNodeId] ?: return@forEach
            val isOnOptimalPath = (edge.fromNodeId to edge.toNodeId) in activePathEdgePairs
            val isBlockedOrCritical = edge.status == RoadStatusType.BLOCKED ||
                edge.severity == HazardSeverity.CRITICAL ||
                edge.severity == HazardSeverity.BLOCKED ||
                activeHazards.any {
                    it.edgeId == edge.id && (it.severity == HazardSeverity.CRITICAL.name || it.severity == HazardSeverity.BLOCKED.name)
                }
            val isWarningOrConstruction = edge.status == RoadStatusType.WARNING ||
                edge.severity == HazardSeverity.WARNING ||
                edge.id == "H-I" ||
                activeHazards.any { it.edgeId == edge.id && it.severity == HazardSeverity.WARNING.name }

            if (!showRoadLayer && !isOnOptimalPath && !isBlockedOrCritical && !isWarningOrConstruction && !edge.isBridge) {
                return@forEach
            }

            projection.toPixels(GeoPoint(nFrom.latitude, nFrom.longitude), pt1)
            projection.toPixels(GeoPoint(nTo.latitude, nTo.longitude), pt2)

            when {
                isBlockedOrCritical -> {
                    // Draw white casing + bold red hazard line at exact location
                    bgEdgePaint.color = android.graphics.Color.WHITE
                    bgEdgePaint.strokeWidth = 16f
                    bgEdgePaint.pathEffect = null
                    canvas.drawLine(pt1.x.toFloat(), pt1.y.toFloat(), pt2.x.toFloat(), pt2.y.toFloat(), bgEdgePaint)

                    bgEdgePaint.color = android.graphics.Color.rgb(220, 38, 38) // Blocked Red
                    bgEdgePaint.strokeWidth = 11f
                    bgEdgePaint.pathEffect = null
                    canvas.drawLine(pt1.x.toFloat(), pt1.y.toFloat(), pt2.x.toFloat(), pt2.y.toFloat(), bgEdgePaint)
                }
                isWarningOrConstruction -> {
                    // Draw white casing + bold amber construction line at exact location
                    bgEdgePaint.color = android.graphics.Color.WHITE
                    bgEdgePaint.strokeWidth = 15f
                    bgEdgePaint.pathEffect = null
                    canvas.drawLine(pt1.x.toFloat(), pt1.y.toFloat(), pt2.x.toFloat(), pt2.y.toFloat(), bgEdgePaint)

                    bgEdgePaint.color = android.graphics.Color.rgb(245, 158, 11) // Warning / Construction Amber
                    bgEdgePaint.strokeWidth = 10f
                    bgEdgePaint.pathEffect = DashPathEffect(floatArrayOf(20f, 10f), 0f)
                    canvas.drawLine(pt1.x.toFloat(), pt1.y.toFloat(), pt2.x.toFloat(), pt2.y.toFloat(), bgEdgePaint)
                }
                edge.status == RoadStatusType.RESTRICTED -> {
                    bgEdgePaint.color = android.graphics.Color.argb(160, 100, 116, 139)
                    bgEdgePaint.strokeWidth = 6f
                    bgEdgePaint.pathEffect = DashPathEffect(floatArrayOf(14f, 10f), 0f)
                    canvas.drawLine(pt1.x.toFloat(), pt1.y.toFloat(), pt2.x.toFloat(), pt2.y.toFloat(), bgEdgePaint)
                }
                else -> {
                    // Subtle green background road link so the OpenStreetMap streets stay crystal clear
                    bgEdgePaint.color = android.graphics.Color.argb(135, 16, 185, 129)
                    bgEdgePaint.strokeWidth = 5f
                    bgEdgePaint.pathEffect = null
                    canvas.drawLine(pt1.x.toFloat(), pt1.y.toFloat(), pt2.x.toFloat(), pt2.y.toFloat(), bgEdgePaint)
                }
            }
        }

        // 3. Draw Alternate Route (dashed line) if different from active Optimal Path
        val altPath = alternateRoute?.nodePath ?: emptyList()
        if (altPath.size >= 2 && altPath != primaryPath) {
            for (i in 0 until altPath.size - 1) {
                val n1 = nodeMap[altPath[i]] ?: continue
                val n2 = nodeMap[altPath[i + 1]] ?: continue
                projection.toPixels(GeoPoint(n1.latitude, n1.longitude), pt1)
                projection.toPixels(GeoPoint(n2.latitude, n2.longitude), pt2)
                canvas.drawLine(
                    pt1.x.toFloat(),
                    pt1.y.toFloat(),
                    pt2.x.toFloat(),
                    pt2.y.toFloat(),
                    routeAlternatePaint
                )
            }
        }

        // 4. Draw Primary Active OPTIMAL PATH (Bold White Casing + Vibrant Optimal Route Line + Directional Arrows)
        if (primaryPath.size >= 2) {
            for (i in 0 until primaryPath.size - 1) {
                val uId = primaryPath[i]
                val vId = primaryPath[i + 1]
                val n1 = nodeMap[uId] ?: continue
                val n2 = nodeMap[vId] ?: continue
                projection.toPixels(GeoPoint(n1.latitude, n1.longitude), pt1)
                projection.toPixels(GeoPoint(n2.latitude, n2.longitude), pt2)

                val x1 = pt1.x.toFloat()
                val y1 = pt1.y.toFloat()
                val x2 = pt2.x.toFloat()
                val y2 = pt2.y.toFloat()

                // Check if this specific segment on the Optimal Path has a Hazard, Bridge, or Road Construction
                val segmentEdge = edges.find {
                    (it.fromNodeId == uId && it.toNodeId == vId) || (it.fromNodeId == vId && it.toNodeId == uId)
                }
                val segBlocked = segmentEdge != null && (
                    segmentEdge.status == RoadStatusType.BLOCKED ||
                        segmentEdge.severity == HazardSeverity.CRITICAL ||
                        segmentEdge.severity == HazardSeverity.BLOCKED ||
                        activeHazards.any { it.edgeId == segmentEdge.id && it.severity == HazardSeverity.CRITICAL.name }
                    )
                val segWarning = segmentEdge != null && (
                    segmentEdge.status == RoadStatusType.WARNING ||
                        segmentEdge.severity == HazardSeverity.WARNING ||
                        segmentEdge.id == "H-I" ||
                        activeHazards.any { it.edgeId == segmentEdge.id }
                    )

                // White outer casing for the Optimal Path
                canvas.drawLine(x1, y1, x2, y2, routeCasingPaint)

                // Colored inner line (Red if Critical Hazard on Optimal Path, Amber if Construction on Optimal Path, Royal Blue if Safe Optimal Path)
                when {
                    segBlocked -> {
                        routeHazardSegmentPaint.color = android.graphics.Color.rgb(220, 38, 38)
                        canvas.drawLine(x1, y1, x2, y2, routeHazardSegmentPaint)
                    }
                    segWarning -> {
                        routeHazardSegmentPaint.color = android.graphics.Color.rgb(217, 119, 6)
                        canvas.drawLine(x1, y1, x2, y2, routeHazardSegmentPaint)
                    }
                    else -> {
                        canvas.drawLine(x1, y1, x2, y2, routePrimaryPaint)
                    }
                }

                // Draw directional chevron arrow at segment midpoint showing Optimal Path flow
                val midX = (x1 + x2) / 2f
                val midY = (y1 + y2) / 2f
                val angle = atan2((y2 - y1).toDouble(), (x2 - x1).toDouble())
                drawDirectionArrow(canvas, midX, midY, angle)
            }
        }

        // 5. Draw Nodes (Full circles for Optimal Path nodes & Landmarks, small dots for background nodes)
        nodes.forEach { node ->
            val isSource = node.id == primaryPath.firstOrNull()
            val isDest = node.id == primaryPath.lastOrNull()
            val isInRoute = node.id in primaryPath
            if (!showRoadLayer && !isInRoute && !node.isLandmark) return@forEach

            projection.toPixels(GeoPoint(node.latitude, node.longitude), pt1)
            val cx = pt1.x.toFloat()
            val cy = pt1.y.toFloat()
            val radius = when {
                isSource || isDest -> 24f
                isInRoute -> 19f
                else -> 13f
            }

            nodeFillPaint.color = when {
                isSource -> android.graphics.Color.rgb(16, 185, 129) // Green Source
                isDest -> android.graphics.Color.rgb(220, 38, 38) // Red Destination
                isInRoute -> android.graphics.Color.rgb(37, 99, 235) // Blue Optimal Path Node
                else -> android.graphics.Color.argb(220, 255, 255, 255)
            }
            nodeBorderPaint.color = when {
                isSource || isDest || isInRoute -> android.graphics.Color.WHITE
                else -> android.graphics.Color.rgb(100, 116, 139)
            }

            canvas.drawCircle(cx, cy, radius, nodeFillPaint)
            canvas.drawCircle(cx, cy, radius, nodeBorderPaint)

            nodeTextPaint.color = if (isSource || isDest || isInRoute) {
                android.graphics.Color.WHITE
            } else {
                android.graphics.Color.rgb(30, 41, 59)
            }
            nodeTextPaint.textSize = when {
                isSource || isDest -> 22f
                isInRoute -> 18f
                else -> 15f
            }
            val textOffset = (nodeTextPaint.descent() + nodeTextPaint.ascent()) / 2f
            canvas.drawText(node.id, cx, cy - textOffset, nodeTextPaint)
        }

        // 6. Draw Geo-Anchored Source & Destination Labels at their exact locations
        primaryPath.firstOrNull()?.let { startId ->
            nodeMap[startId]?.let { startNode ->
                projection.toPixels(GeoPoint(startNode.latitude, startNode.longitude), pt1)
                drawLocationCallout(
                    canvas = canvas,
                    anchorX = pt1.x.toFloat(),
                    anchorY = pt1.y.toFloat(),
                    title = "START: ${startNode.id} (Source)",
                    subtitle = startNode.name.substringAfter("- ").trim(),
                    borderColor = android.graphics.Color.rgb(16, 185, 129),
                    titleColor = android.graphics.Color.rgb(6, 95, 70),
                    placeAbove = true,
                    verticalOffset = 30f
                )
            }
        }

        primaryPath.lastOrNull()?.takeIf { it != primaryPath.firstOrNull() }?.let { endId ->
            nodeMap[endId]?.let { endNode ->
                projection.toPixels(GeoPoint(endNode.latitude, endNode.longitude), pt1)
                drawLocationCallout(
                    canvas = canvas,
                    anchorX = pt1.x.toFloat(),
                    anchorY = pt1.y.toFloat(),
                    title = "DEST: ${endNode.id} (Goal)",
                    subtitle = endNode.name.substringAfter("- ").trim(),
                    borderColor = android.graphics.Color.rgb(220, 38, 38),
                    titleColor = android.graphics.Color.rgb(153, 27, 27),
                    placeAbove = false,
                    verticalOffset = 30f
                )
            }
        }

        // 7. Draw Geo-Anchored Bridges, Road Construction Sites & Hazards at their EXACT GPS Location on the Map!
        edges.forEach { edge ->
            val nFrom = nodeMap[edge.fromNodeId] ?: return@forEach
            val nTo = nodeMap[edge.toNodeId] ?: return@forEach
            val isOnOptimalPath = (edge.fromNodeId to edge.toNodeId) in activePathEdgePairs

            val matchingHazard = activeHazards.find { it.edgeId == edge.id }
            val isBridgeEdge = edge.isBridge || edge.roadName.contains("Bridge", ignoreCase = true) || edge.id == "B-C"
            val isConstructionEdge = edge.id == "H-I" ||
                edge.roadName.contains("Construction", ignoreCase = true) ||
                matchingHazard?.hazardType?.contains("CONSTRUCTION", ignoreCase = true) == true
            val isCriticalOrBlocked = edge.status == RoadStatusType.BLOCKED ||
                edge.severity == HazardSeverity.CRITICAL ||
                edge.severity == HazardSeverity.BLOCKED ||
                matchingHazard?.severity == HazardSeverity.CRITICAL.name ||
                matchingHazard?.severity == HazardSeverity.BLOCKED.name
            val isWarningHazard = edge.status == RoadStatusType.WARNING ||
                edge.severity == HazardSeverity.WARNING ||
                matchingHazard != null

            // Show indicator at exact location if:
            // (a) It is any Bridge or Construction or Hazard on the active Optimal Path, OR
            // (b) It is an active Critical/Warning Hazard or Bridge B1 (B-C) or Road Construction (H-I)
            val shouldIndicateAtLocation = (isOnOptimalPath && (isBridgeEdge || isConstructionEdge || isCriticalOrBlocked || isWarningHazard)) ||
                isCriticalOrBlocked ||
                isConstructionEdge ||
                (edge.id == "B-C" && activeHazards.isNotEmpty())

            if (shouldIndicateAtLocation) {
                val midLat = (nFrom.latitude + nTo.latitude) / 2.0
                val midLon = (nFrom.longitude + nTo.longitude) / 2.0
                projection.toPixels(GeoPoint(midLat, midLon), pt1)
                val mx = pt1.x.toFloat()
                val my = pt1.y.toFloat()

                when {
                    isCriticalOrBlocked -> {
                        // Pulsing Red Hazard Beacon at exact GPS midpoint of Bridge/Road
                        hazardPulsePaint.color = android.graphics.Color.argb(85, 239, 68, 68)
                        canvas.drawCircle(mx, my, 34f, hazardPulsePaint)
                        nodeFillPaint.color = android.graphics.Color.rgb(220, 38, 38)
                        nodeBorderPaint.color = android.graphics.Color.WHITE
                        canvas.drawCircle(mx, my, 15f, nodeFillPaint)
                        canvas.drawCircle(mx, my, 15f, nodeBorderPaint)

                        val pathTag = if (isOnOptimalPath) "ON OPTIMAL PATH" else "BYPASSED BY OPTIMAL PATH"
                        val titleText = if (isBridgeEdge) "⚠ Bridge B1 Hazard (${edge.id})" else "⚠ Road Hazard (${edge.id})"
                        drawLocationCallout(
                            canvas = canvas,
                            anchorX = mx,
                            anchorY = my,
                            title = titleText,
                            subtitle = "CRITICAL • $pathTag",
                            borderColor = android.graphics.Color.rgb(220, 38, 38),
                            titleColor = android.graphics.Color.rgb(185, 28, 28),
                            placeAbove = true,
                            verticalOffset = 20f
                        )
                    }
                    isConstructionEdge || isWarningHazard -> {
                        // Pulsing Amber Construction Beacon at exact GPS midpoint of Road H-I
                        hazardPulsePaint.color = android.graphics.Color.argb(85, 245, 158, 11)
                        canvas.drawCircle(mx, my, 32f, hazardPulsePaint)
                        nodeFillPaint.color = android.graphics.Color.rgb(245, 158, 11)
                        nodeBorderPaint.color = android.graphics.Color.WHITE
                        canvas.drawCircle(mx, my, 14f, nodeFillPaint)
                        canvas.drawCircle(mx, my, 14f, nodeBorderPaint)

                        val pathTag = if (isOnOptimalPath) "ON OPTIMAL PATH" else "AVOIDED AT NODE H"
                        val titleText = if (isConstructionEdge) "🚧 Road Construction (${edge.id})" else "⚠ Hazard Warning (${edge.id})"
                        drawLocationCallout(
                            canvas = canvas,
                            anchorX = mx,
                            anchorY = my,
                            title = titleText,
                            subtitle = "WARNING • $pathTag",
                            borderColor = android.graphics.Color.rgb(245, 158, 11),
                            titleColor = android.graphics.Color.rgb(180, 83, 9),
                            placeAbove = false,
                            verticalOffset = 20f
                        )
                    }
                    isBridgeEdge && isOnOptimalPath -> {
                        // Safe Bridge directly on the chosen Optimal Path (e.g., H-P South Bridge Approach)
                        hazardPulsePaint.color = android.graphics.Color.argb(65, 37, 99, 235)
                        canvas.drawCircle(mx, my, 26f, hazardPulsePaint)
                        nodeFillPaint.color = android.graphics.Color.rgb(37, 99, 235)
                        nodeBorderPaint.color = android.graphics.Color.WHITE
                        canvas.drawCircle(mx, my, 12f, nodeFillPaint)
                        canvas.drawCircle(mx, my, 12f, nodeBorderPaint)

                        drawLocationCallout(
                            canvas = canvas,
                            anchorX = mx,
                            anchorY = my,
                            title = "🌉 ${edge.roadName} (${edge.id})",
                            subtitle = "SAFE BRIDGE ON OPTIMAL PATH",
                            borderColor = android.graphics.Color.rgb(37, 99, 235),
                            titleColor = android.graphics.Color.rgb(30, 64, 175),
                            placeAbove = false,
                            verticalOffset = 18f
                        )
                    }
                }
            }
        }

        // 8. Draw Live GPS / Vehicle Position Beacon on the Optimal Path
        val gpsLat = gpsState.latitude ?: nodeMap[currentVehicleNodeId]?.latitude
        val gpsLon = gpsState.longitude ?: nodeMap[currentVehicleNodeId]?.longitude
        if (gpsLat != null && gpsLon != null) {
            projection.toPixels(GeoPoint(gpsLat, gpsLon), pt1)
            val gx = pt1.x.toFloat()
            val gy = pt1.y.toFloat()
            canvas.drawCircle(gx, gy, 34f, gpsHaloPaint)
            canvas.drawCircle(gx, gy, 12f, gpsDotPaint)
            nodeBorderPaint.color = android.graphics.Color.WHITE
            canvas.drawCircle(gx, gy, 12f, nodeBorderPaint)
        }
    }

    private fun drawDirectionArrow(
        canvas: android.graphics.Canvas,
        cx: Float,
        cy: Float,
        angleRad: Double
    ) {
        val size = 10f
        val path = Path()
        val tipX = cx + (size * cos(angleRad)).toFloat()
        val tipY = cy + (size * sin(angleRad)).toFloat()
        val leftX = cx + (size * cos(angleRad + 2.4)).toFloat()
        val leftY = cy + (size * sin(angleRad + 2.4)).toFloat()
        val rightX = cx + (size * cos(angleRad - 2.4)).toFloat()
        val rightY = cy + (size * sin(angleRad - 2.4)).toFloat()
        path.moveTo(tipX, tipY)
        path.lineTo(leftX, leftY)
        path.lineTo(rightX, rightY)
        path.close()
        canvas.drawPath(path, arrowPaint)
    }

    private fun drawLocationCallout(
        canvas: android.graphics.Canvas,
        anchorX: Float,
        anchorY: Float,
        title: String,
        subtitle: String,
        borderColor: Int,
        titleColor: Int,
        placeAbove: Boolean,
        verticalOffset: Float
    ) {
        calloutTitlePaint.textSize = 18f
        calloutSubPaint.textSize = 14f
        val titleWidth = calloutTitlePaint.measureText(title)
        val subWidth = calloutSubPaint.measureText(subtitle)
        val boxWidth = maxOf(titleWidth, subWidth) + 28f
        val boxHeight = 46f

        val left = anchorX - boxWidth / 2f
        val right = anchorX + boxWidth / 2f
        val top = if (placeAbove) anchorY - verticalOffset - boxHeight else anchorY + verticalOffset
        val bottom = top + boxHeight

        // Draw pointer triangle connecting the callout box to the exact GeoPoint coordinate
        val pointerPath = Path()
        if (placeAbove) {
            pointerPath.moveTo(anchorX - 9f, bottom)
            pointerPath.lineTo(anchorX + 9f, bottom)
            pointerPath.lineTo(anchorX, anchorY - verticalOffset + 4f)
        } else {
            pointerPath.moveTo(anchorX - 9f, top)
            pointerPath.lineTo(anchorX + 9f, top)
            pointerPath.lineTo(anchorX, anchorY + verticalOffset - 4f)
        }
        pointerPath.close()

        calloutBorderPaint.color = borderColor
        canvas.drawPath(pointerPath, calloutBgPaint)

        val rect = RectF(left, top, right, bottom)
        canvas.drawRoundRect(rect, 12f, 12f, calloutBgPaint)
        canvas.drawRoundRect(rect, 12f, 12f, calloutBorderPaint)

        calloutTitlePaint.color = titleColor
        canvas.drawText(title, anchorX, top + 20f, calloutTitlePaint)

        calloutSubPaint.color = android.graphics.Color.rgb(71, 85, 105)
        canvas.drawText(subtitle, anchorX, top + 38f, calloutSubPaint)
    }

    override fun onSingleTapConfirmed(e: MotionEvent?, mapView: MapView?): Boolean {
        if (e == null || mapView == null) return false
        val projection = mapView.projection ?: return false
        val nodeMap = nodes.associateBy { it.id }
        val pt = Point()
        val tapX = e.x
        val tapY = e.y

        // 1. Check if user tapped on a Hazard / Bridge / Road Construction midpoint marker on the map
        for (edge in edges) {
            val isHazardOrBridge = edge.isBridge ||
                edge.id in setOf("B-C", "H-I") ||
                edge.status != RoadStatusType.OPEN ||
                edge.severity != HazardSeverity.SAFE ||
                activeHazards.any { it.edgeId == edge.id }
            if (!isHazardOrBridge) continue

            val nFrom = nodeMap[edge.fromNodeId] ?: continue
            val nTo = nodeMap[edge.toNodeId] ?: continue
            val midLat = (nFrom.latitude + nTo.latitude) / 2.0
            val midLon = (nFrom.longitude + nTo.longitude) / 2.0
            projection.toPixels(GeoPoint(midLat, midLon), pt)
            val distSq = (pt.x - tapX).pow(2) + (pt.y - tapY).pow(2)
            if (distSq <= 4200f) {
                val matchedHazard = activeHazards.find { it.edgeId == edge.id } ?: activeHazards.firstOrNull()
                onHazardClick(matchedHazard)
                return true
            }
        }

        // 2. Check if user tapped on a RoadNode (A..T)
        val tappedNode = nodes.minByOrNull { node ->
            projection.toPixels(GeoPoint(node.latitude, node.longitude), pt)
            (pt.x - tapX).pow(2) + (pt.y - tapY).pow(2)
        }
        if (tappedNode != null) {
            projection.toPixels(GeoPoint(tappedNode.latitude, tappedNode.longitude), pt)
            val distSq = (pt.x - tapX).pow(2) + (pt.y - tapY).pow(2)
            if (distSq <= 2500f) {
                onNodeClick(tappedNode)
                return true
            }
        }
        return false
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MapStatusLegendCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LegendChip(color = RpSafeGreen, label = "Safe", isPill = true)
            LegendChip(color = RpWarningYellow, label = "Warning", isPill = false)
            LegendChip(color = RpCriticalOrange, label = "Critical", isPill = true)
            LegendChip(color = RpBlockedRed, label = "Blocked", isPill = true)
            LegendChip(color = RpPrimaryBlue, label = "Route", isPill = false)
            LegendChip(color = RpPrimaryBlue, label = "Alternate Route", isDashed = true)
            LegendChip(color = RpRestrictedGray, label = "Restricted", isPill = false)
        }
    }
}

@Composable
private fun LegendChip(
    color: Color,
    label: String,
    isPill: Boolean = false,
    isDashed: Boolean = false
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (isPill) {
            Box(
                modifier = Modifier
                    .size(width = 16.dp, height = 9.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
            )
        } else if (isDashed) {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .size(width = 4.dp, height = 3.dp)
                            .background(color)
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .size(width = 16.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color)
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

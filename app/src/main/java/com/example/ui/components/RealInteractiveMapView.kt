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
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay

enum class MapVisualStyle(val label: String) {
    STREET("Street"),
    DARK("Dark"),
    SATELLITE("Satellite")
}

private val CARTO_DARK_TILE_SOURCE: OnlineTileSourceBase = XYTileSource(
    "CartoDarkMatter",
    1,
    20,
    256,
    ".png",
    arrayOf(
        "https://a.basemaps.cartocdn.com/dark_all/",
        "https://b.basemaps.cartocdn.com/dark_all/",
        "https://c.basemaps.cartocdn.com/dark_all/"
    )
)

private val ESRI_SATELLITE_TILE_SOURCE: OnlineTileSourceBase = object : OnlineTileSourceBase(
    "EsriWorldImagery",
    1,
    19,
    256,
    ".jpg",
    arrayOf("https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/")
) {
    override fun getTileURLString(pMapTileIndex: Long): String {
        val zoom = MapTileIndex.getZoom(pMapTileIndex)
        val y = MapTileIndex.getY(pMapTileIndex)
        val x = MapTileIndex.getX(pMapTileIndex)
        return "$baseUrl$zoom/$y/$x"
    }
}

/**
 * Full Native Interactive OpenStreetMap (`org.osmdroid.views.MapView`) Component with
 * authentic Street, Dark Night, and Satellite Imagery modes.
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
    var mapStyle by remember { mutableStateOf(if (isDarkTheme) MapVisualStyle.DARK else MapVisualStyle.STREET) }
    val mapViewRef = remember { mutableStateOf<MapView?>(null) }

    // Automatically sync map style when the user toggles the app's Dark Mode button
    LaunchedEffect(isDarkTheme) {
        if (isDarkTheme && mapStyle == MapVisualStyle.STREET) {
            mapStyle = MapVisualStyle.DARK
        } else if (!isDarkTheme && mapStyle == MapVisualStyle.DARK) {
            mapStyle = MapVisualStyle.STREET
        }
    }

    val isNightOrSatellite = mapStyle == MapVisualStyle.DARK || mapStyle == MapVisualStyle.SATELLITE || isDarkTheme

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
    geoOverlay.isDarkTheme = isNightOrSatellite
    geoOverlay.mapVisualStyle = mapStyle
    geoOverlay.onNodeClick = onNodeClick
    geoOverlay.onHazardClick = onHazardMarkerClick

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

    val mapBgColor = when (mapStyle) {
        MapVisualStyle.DARK -> Color(0xFF070E1B)
        MapVisualStyle.SATELLITE -> Color(0xFF0A1914)
        MapVisualStyle.STREET -> if (isDarkTheme) Color(0xFF070E1B) else Color(0xFFE8F1E5)
    }

    val controlSurfaceColor = if (isNightOrSatellite) {
        Color(0xFF0F172A).copy(alpha = 0.94f)
    } else {
        Color.White.copy(alpha = 0.94f)
    }
    val controlBorderColor = if (isNightOrSatellite) {
        Color(0xFF334155)
    } else {
        Color(0xFFCBD5E1)
    }
    val controlIconTint = if (isNightOrSatellite) {
        Color(0xFFF8FAFC)
    } else {
        Color(0xFF0F172A)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clipToBounds()
            .background(mapBgColor)
    ) {
        // 1. Full-Screen Native Interactive osmdroid MapView
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(
                        when (mapStyle) {
                            MapVisualStyle.SATELLITE -> ESRI_SATELLITE_TILE_SOURCE
                            MapVisualStyle.DARK -> CARTO_DARK_TILE_SOURCE
                            MapVisualStyle.STREET -> TileSourceFactory.MAPNIK
                        }
                    )
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
                val desiredSource = when (mapStyle) {
                    MapVisualStyle.SATELLITE -> ESRI_SATELLITE_TILE_SOURCE
                    MapVisualStyle.DARK -> CARTO_DARK_TILE_SOURCE
                    MapVisualStyle.STREET -> TileSourceFactory.MAPNIK
                }
                if (mapView.tileProvider.tileSource.name() != desiredSource.name()) {
                    mapView.setTileSource(desiredSource)
                }

                // Configure tile loading background & color filter so Dark and Satellite modes look deep & authentic
                when (mapStyle) {
                    MapVisualStyle.DARK -> {
                        mapView.overlayManager.tilesOverlay.loadingBackgroundColor =
                            android.graphics.Color.rgb(7, 14, 27)
                        mapView.overlayManager.tilesOverlay.loadingLineColor =
                            android.graphics.Color.rgb(18, 31, 53)
                        // Slight contrast boost for Carto Dark Matter tiles
                        val nightContrastMatrix = ColorMatrix(
                            floatArrayOf(
                                1.12f, 0f, 0f, 0f, -4f,
                                0f, 1.15f, 0f, 0f, 2f,
                                0f, 0f, 1.25f, 0f, 10f,
                                0f, 0f, 0f, 1f, 0f
                            )
                        )
                        mapView.overlayManager.tilesOverlay.setColorFilter(
                            ColorMatrixColorFilter(nightContrastMatrix)
                        )
                    }
                    MapVisualStyle.SATELLITE -> {
                        mapView.overlayManager.tilesOverlay.loadingBackgroundColor =
                            android.graphics.Color.rgb(10, 24, 19)
                        mapView.overlayManager.tilesOverlay.loadingLineColor =
                            android.graphics.Color.rgb(20, 44, 36)
                        // Rich satellite imagery contrast enhancement
                        val satMatrix = ColorMatrix(
                            floatArrayOf(
                                1.08f, 0f, 0f, 0f, -6f,
                                0f, 1.10f, 0f, 0f, -4f,
                                0f, 0f, 1.08f, 0f, -4f,
                                0f, 0f, 0f, 1f, 0f
                            )
                        )
                        mapView.overlayManager.tilesOverlay.setColorFilter(
                            ColorMatrixColorFilter(satMatrix)
                        )
                    }
                    MapVisualStyle.STREET -> {
                        if (isDarkTheme) {
                            mapView.overlayManager.tilesOverlay.loadingBackgroundColor =
                                android.graphics.Color.rgb(7, 14, 27)
                            mapView.overlayManager.tilesOverlay.loadingLineColor =
                                android.graphics.Color.rgb(18, 31, 53)
                            val darkStreetMatrix = ColorMatrix(
                                floatArrayOf(
                                    -0.75f, 0f, 0f, 0f, 200f,
                                    0f, -0.72f, 0f, 0f, 212f,
                                    0f, 0f, -0.58f, 0f, 232f,
                                    0f, 0f, 0f, 1f, 0f
                                )
                            )
                            mapView.overlayManager.tilesOverlay.setColorFilter(
                                ColorMatrixColorFilter(darkStreetMatrix)
                            )
                        } else {
                            mapView.overlayManager.tilesOverlay.loadingBackgroundColor =
                                android.graphics.Color.rgb(232, 241, 229)
                            mapView.overlayManager.tilesOverlay.loadingLineColor =
                                android.graphics.Color.rgb(203, 213, 225)
                            mapView.overlayManager.tilesOverlay.setColorFilter(null)
                        }
                    }
                }

                if (!mapView.overlays.contains(geoOverlay)) {
                    mapView.overlays.add(geoOverlay)
                }
                mapView.invalidate()
            }
        )

        if (showControls) {
            // 2. Compact Map Mode Switcher (Street | Dark | Satellite) at Top-End below the path bar
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 58.dp, end = 10.dp),
                shape = RoundedCornerShape(10.dp),
                color = controlSurfaceColor,
                shadowElevation = 5.dp,
                border = BorderStroke(1.dp, controlBorderColor)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MapVisualStyle.entries.forEach { style ->
                        val selected = mapStyle == style
                        Surface(
                            shape = RoundedCornerShape(7.dp),
                            color = if (selected) RpPrimaryBlue else Color.Transparent,
                            modifier = Modifier
                                .clickable {
                                    mapStyle = style
                                    mapViewRef.value?.invalidate()
                                }
                                .testTag("map_style_${style.name.lowercase()}")
                        ) {
                            Text(
                                text = style.label,
                                fontSize = 10.sp,
                                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                color = if (selected) Color.White else controlIconTint,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // 3. Compact Bottom-Left Map Controls (Zoom +/-, Recenter on Optimal Path, Layer Toggle)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = controlSurfaceColor,
                    shadowElevation = 4.dp,
                    border = BorderStroke(1.dp, controlBorderColor)
                ) {
                    Column {
                        IconButton(
                            onClick = { mapViewRef.value?.controller?.zoomIn() },
                            modifier = Modifier
                                .size(38.dp)
                                .testTag("map_zoom_in_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Zoom in", tint = controlIconTint)
                        }
                        IconButton(
                            onClick = { mapViewRef.value?.controller?.zoomOut() },
                            modifier = Modifier
                                .size(38.dp)
                                .testTag("map_zoom_out_button")
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Zoom out", tint = controlIconTint)
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = controlSurfaceColor,
                    shadowElevation = 4.dp,
                    border = BorderStroke(1.dp, controlBorderColor)
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
                            Icon(
                                Icons.Default.MyLocation,
                                contentDescription = "Center Optimal Path",
                                tint = if (isNightOrSatellite) Color(0xFF38BDF8) else RpPrimaryBlue
                            )
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
                                tint = if (showRoadLayer) {
                                    if (isNightOrSatellite) Color(0xFF38BDF8) else RpPrimaryBlue
                                } else {
                                    Color(0xFF64748B)
                                }
                            )
                        }
                    }
                }
            }

            // 4. Compact Bottom-Right Legend & Quick-Pan Chips (Tap to center on Bridge B1 or Road Construction)
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(10.dp),
                shape = RoundedCornerShape(10.dp),
                color = controlSurfaceColor,
                shadowElevation = 4.dp,
                border = BorderStroke(1.dp, controlBorderColor)
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
                            color = if (isNightOrSatellite) Color(0xFFFCA5A5) else RpBlockedRed
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
                            color = if (isNightOrSatellite) Color(0xFFFCD34D) else Color(0xFFB45309)
                        )
                    }

                    val scaleKm = (40000.0 / 2.0.pow(currentZoomLevel) * 0.15).coerceAtLeast(0.1)
                    val scaleLabel = if (scaleKm >= 1.0) "${scaleKm.roundToInt()} km" else "${(scaleKm * 1000).roundToInt()}" + " m"
                    Text(
                        text = "• $scaleLabel",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isNightOrSatellite) Color(0xFF94A3B8) else Color(0xFF475569)
                    )
                }
            }
        }
    }
}

/**
 * Custom `org.osmdroid.views.overlay.Overlay` that renders:
 * 1. Authentic Dark / Satellite waterway & terrain corridor cues when in Dark or Satellite mode.
 * 2. The active Optimal Path from Source to Destination with glowing dark/satellite halo, bold route line,
 *    and directional arrows (`➤`).
 * 3. Exact Geo-Anchored Markers & Pointer Callout Badges at the exact GPS location (`GeoPoint`) of:
 *    - Any Bridge on the Optimal Path or corridor (`🌉 Bridge B1`, `🌉 South Plaza Bridge H-P`, `🌉 East Viaduct C-F`, `🌉 Causeway Q-R`)
 *    - Any Road Construction site on the Optimal Path or corridor (`🚧 Road Construction H-I`)
 *    - Any Active Hazard on the Optimal Path or corridor (`⚠ CRITICAL HAZARD`, `⚠ WARNING`)
 *    - Source (`START`) and Destination (`DEST`) pins
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
    var mapVisualStyle: MapVisualStyle = MapVisualStyle.STREET
    var onNodeClick: (RoadNode) -> Unit = {}
    var onHazardClick: (HazardEntity?) -> Unit = {}

    private val riverWaterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val riverBankPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val bgEdgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val routeGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        strokeWidth = 30f
    }

    private val routeCasingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        strokeWidth = 21f
    }

    private val routePrimaryPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
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
    }

    private val gpsDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val hazardPulsePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val calloutBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val calloutBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3.5f
    }

    private val calloutTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = 19f
    }

    private val calloutSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = 14f
    }

    override fun draw(canvas: android.graphics.Canvas, mapView: MapView, shadow: Boolean) {
        if (shadow) return
        val projection = mapView.projection ?: return
        val nodeMap = nodes.associateBy { it.id }
        val pt1 = Point()
        val pt2 = Point()

        val isNightOrSat = isDarkTheme || mapVisualStyle == MapVisualStyle.DARK || mapVisualStyle == MapVisualStyle.SATELLITE

        // 0. Draw subtle geo-anchored River Channel passing under the bridges (B-C, H-P, Q-R) so Bridges are visually unmistakable in Dark & Satellite modes
        val bNode = nodeMap["B"]
        val cNode = nodeMap["C"]
        val hNode = nodeMap["H"]
        val pNode = nodeMap["P"]
        val qNode = nodeMap["Q"]
        val rNode = nodeMap["R"]
        if (bNode != null && cNode != null && hNode != null && pNode != null && qNode != null && rNode != null) {
            val riverPath = Path()
            val northLat = (bNode.latitude + cNode.latitude) / 2.0 + 0.015
            val northLon = (bNode.longitude + cNode.longitude) / 2.0 - 0.004
            val bcLat = (bNode.latitude + cNode.latitude) / 2.0
            val bcLon = (bNode.longitude + cNode.longitude) / 2.0
            val hpLat = (hNode.latitude + pNode.latitude) / 2.0
            val hpLon = (hNode.longitude + pNode.longitude) / 2.0
            val qrLat = (qNode.latitude + rNode.latitude) / 2.0
            val qrLon = (qNode.longitude + rNode.longitude) / 2.0

            projection.toPixels(GeoPoint(northLat, northLon), pt1)
            riverPath.moveTo(pt1.x.toFloat(), pt1.y.toFloat())
            projection.toPixels(GeoPoint(bcLat, bcLon), pt1)
            riverPath.lineTo(pt1.x.toFloat(), pt1.y.toFloat())
            projection.toPixels(GeoPoint(hpLat, hpLon), pt1)
            riverPath.lineTo(pt1.x.toFloat(), pt1.y.toFloat())
            projection.toPixels(GeoPoint(qrLat, qrLon), pt1)
            riverPath.lineTo(pt1.x.toFloat(), pt1.y.toFloat())

            riverBankPaint.strokeWidth = 42f
            riverBankPaint.color = when {
                mapVisualStyle == MapVisualStyle.SATELLITE -> android.graphics.Color.argb(110, 6, 78, 59)
                isNightOrSat -> android.graphics.Color.argb(120, 14, 116, 144)
                else -> android.graphics.Color.argb(95, 56, 189, 248)
            }
            canvas.drawPath(riverPath, riverBankPaint)

            riverWaterPaint.strokeWidth = 28f
            riverWaterPaint.color = when {
                mapVisualStyle == MapVisualStyle.SATELLITE -> android.graphics.Color.argb(175, 8, 47, 73)
                isNightOrSat -> android.graphics.Color.argb(185, 12, 74, 110)
                else -> android.graphics.Color.argb(145, 14, 165, 233)
            }
            canvas.drawPath(riverPath, riverWaterPaint)
        }

        val primaryPath = activeRoute?.nodePath ?: emptyList()
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

        // 2. Draw Road & Bridge Network Links
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
            val x1 = pt1.x.toFloat()
            val y1 = pt1.y.toFloat()
            val x2 = pt2.x.toFloat()
            val y2 = pt2.y.toFloat()

            when {
                isBlockedOrCritical -> {
                    // Glowing Red Halo + Bold Red Hazard Line at exact location
                    bgEdgePaint.color = if (isNightOrSat) {
                        android.graphics.Color.argb(120, 239, 68, 68)
                    } else {
                        android.graphics.Color.WHITE
                    }
                    bgEdgePaint.strokeWidth = if (isNightOrSat) 22f else 16f
                    bgEdgePaint.pathEffect = null
                    canvas.drawLine(x1, y1, x2, y2, bgEdgePaint)

                    bgEdgePaint.color = android.graphics.Color.rgb(239, 68, 68)
                    bgEdgePaint.strokeWidth = 11f
                    bgEdgePaint.pathEffect = null
                    canvas.drawLine(x1, y1, x2, y2, bgEdgePaint)
                }
                isWarningOrConstruction -> {
                    // Glowing Amber Halo + Dashed Construction Line at exact location
                    bgEdgePaint.color = if (isNightOrSat) {
                        android.graphics.Color.argb(110, 245, 158, 11)
                    } else {
                        android.graphics.Color.WHITE
                    }
                    bgEdgePaint.strokeWidth = if (isNightOrSat) 20f else 15f
                    bgEdgePaint.pathEffect = null
                    canvas.drawLine(x1, y1, x2, y2, bgEdgePaint)

                    bgEdgePaint.color = android.graphics.Color.rgb(245, 158, 11)
                    bgEdgePaint.strokeWidth = 10f
                    bgEdgePaint.pathEffect = DashPathEffect(floatArrayOf(20f, 10f), 0f)
                    canvas.drawLine(x1, y1, x2, y2, bgEdgePaint)
                }
                edge.status == RoadStatusType.RESTRICTED -> {
                    bgEdgePaint.color = if (isNightOrSat) {
                        android.graphics.Color.argb(170, 148, 163, 184)
                    } else {
                        android.graphics.Color.argb(160, 100, 116, 139)
                    }
                    bgEdgePaint.strokeWidth = 6f
                    bgEdgePaint.pathEffect = DashPathEffect(floatArrayOf(14f, 10f), 0f)
                    canvas.drawLine(x1, y1, x2, y2, bgEdgePaint)
                }
                else -> {
                    bgEdgePaint.color = if (isNightOrSat) {
                        android.graphics.Color.argb(130, 52, 211, 153)
                    } else {
                        android.graphics.Color.argb(135, 16, 185, 129)
                    }
                    bgEdgePaint.strokeWidth = 5f
                    bgEdgePaint.pathEffect = null
                    canvas.drawLine(x1, y1, x2, y2, bgEdgePaint)
                }
            }
        }

        // 3. Draw Alternate Route (dashed line) if different from active Optimal Path
        val altPath = alternateRoute?.nodePath ?: emptyList()
        if (altPath.size >= 2 && altPath != primaryPath) {
            routeAlternatePaint.color = if (isNightOrSat) {
                android.graphics.Color.argb(190, 148, 163, 184)
            } else {
                android.graphics.Color.argb(170, 100, 116, 139)
            }
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

        // 4. Draw Primary Active OPTIMAL PATH (Glowing Halo in Dark/Sat Mode + High-Contrast Casing + Vibrant Optimal Route Line + Directional Arrows)
        if (primaryPath.size >= 2) {
            routeGlowPaint.color = if (isNightOrSat) {
                android.graphics.Color.argb(95, 56, 189, 248) // Glowing Cyan-Blue Halo in Dark/Satellite mode
            } else {
                android.graphics.Color.argb(55, 37, 99, 235)
            }
            routeCasingPaint.color = if (isNightOrSat) {
                android.graphics.Color.rgb(15, 23, 42)
            } else {
                android.graphics.Color.WHITE
            }
            routePrimaryPaint.color = if (isNightOrSat) {
                android.graphics.Color.rgb(56, 189, 248) // Bright Electric Sky-Blue for Dark/Satellite clarity
            } else {
                android.graphics.Color.rgb(37, 99, 235)
            }

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

                canvas.drawLine(x1, y1, x2, y2, routeGlowPaint)
                canvas.drawLine(x1, y1, x2, y2, routeCasingPaint)

                when {
                    segBlocked -> {
                        routeHazardSegmentPaint.color = android.graphics.Color.rgb(239, 68, 68)
                        canvas.drawLine(x1, y1, x2, y2, routeHazardSegmentPaint)
                    }
                    segWarning -> {
                        routeHazardSegmentPaint.color = android.graphics.Color.rgb(245, 158, 11)
                        canvas.drawLine(x1, y1, x2, y2, routeHazardSegmentPaint)
                    }
                    else -> {
                        canvas.drawLine(x1, y1, x2, y2, routePrimaryPaint)
                    }
                }

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
                isSource -> android.graphics.Color.rgb(16, 185, 129)
                isDest -> android.graphics.Color.rgb(239, 68, 68)
                isInRoute -> android.graphics.Color.rgb(37, 99, 235)
                isNightOrSat -> android.graphics.Color.rgb(15, 23, 42)
                else -> android.graphics.Color.argb(230, 255, 255, 255)
            }
            nodeBorderPaint.color = when {
                isSource || isDest || isInRoute -> android.graphics.Color.WHITE
                isNightOrSat -> android.graphics.Color.rgb(148, 163, 184)
                else -> android.graphics.Color.rgb(100, 116, 139)
            }

            canvas.drawCircle(cx, cy, radius, nodeFillPaint)
            canvas.drawCircle(cx, cy, radius, nodeBorderPaint)

            nodeTextPaint.color = when {
                isSource || isDest || isInRoute -> android.graphics.Color.WHITE
                isNightOrSat -> android.graphics.Color.rgb(241, 245, 249)
                else -> android.graphics.Color.rgb(30, 41, 59)
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
                    titleColor = if (isNightOrSat) {
                        android.graphics.Color.rgb(110, 231, 183)
                    } else {
                        android.graphics.Color.rgb(6, 95, 70)
                    },
                    isDarkCallout = isNightOrSat,
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
                    borderColor = android.graphics.Color.rgb(239, 68, 68),
                    titleColor = if (isNightOrSat) {
                        android.graphics.Color.rgb(252, 165, 165)
                    } else {
                        android.graphics.Color.rgb(153, 27, 27)
                    },
                    isDarkCallout = isNightOrSat,
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
                        hazardPulsePaint.color = android.graphics.Color.argb(105, 239, 68, 68)
                        canvas.drawCircle(mx, my, 36f, hazardPulsePaint)
                        nodeFillPaint.color = android.graphics.Color.rgb(239, 68, 68)
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
                            borderColor = android.graphics.Color.rgb(239, 68, 68),
                            titleColor = if (isNightOrSat) {
                                android.graphics.Color.rgb(252, 165, 165)
                            } else {
                                android.graphics.Color.rgb(185, 28, 28)
                            },
                            isDarkCallout = isNightOrSat,
                            placeAbove = true,
                            verticalOffset = 20f
                        )
                    }
                    isConstructionEdge || isWarningHazard -> {
                        hazardPulsePaint.color = android.graphics.Color.argb(105, 245, 158, 11)
                        canvas.drawCircle(mx, my, 34f, hazardPulsePaint)
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
                            titleColor = if (isNightOrSat) {
                                android.graphics.Color.rgb(253, 224, 71)
                            } else {
                                android.graphics.Color.rgb(180, 83, 9)
                            },
                            isDarkCallout = isNightOrSat,
                            placeAbove = false,
                            verticalOffset = 20f
                        )
                    }
                    isBridgeEdge && isOnOptimalPath -> {
                        hazardPulsePaint.color = android.graphics.Color.argb(80, 56, 189, 248)
                        canvas.drawCircle(mx, my, 28f, hazardPulsePaint)
                        nodeFillPaint.color = android.graphics.Color.rgb(14, 165, 233)
                        nodeBorderPaint.color = android.graphics.Color.WHITE
                        canvas.drawCircle(mx, my, 13f, nodeFillPaint)
                        canvas.drawCircle(mx, my, 13f, nodeBorderPaint)

                        drawLocationCallout(
                            canvas = canvas,
                            anchorX = mx,
                            anchorY = my,
                            title = "🌉 ${edge.roadName} (${edge.id})",
                            subtitle = "SAFE BRIDGE ON OPTIMAL PATH",
                            borderColor = android.graphics.Color.rgb(56, 189, 248),
                            titleColor = if (isNightOrSat) {
                                android.graphics.Color.rgb(125, 211, 252)
                            } else {
                                android.graphics.Color.rgb(30, 64, 175)
                            },
                            isDarkCallout = isNightOrSat,
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
            gpsHaloPaint.color = if (isNightOrSat) {
                android.graphics.Color.argb(85, 56, 189, 248)
            } else {
                android.graphics.Color.argb(60, 37, 99, 235)
            }
            gpsDotPaint.color = if (isNightOrSat) {
                android.graphics.Color.rgb(56, 189, 248)
            } else {
                android.graphics.Color.rgb(37, 99, 235)
            }
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
        isDarkCallout: Boolean,
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

        calloutBgPaint.color = if (isDarkCallout) {
            android.graphics.Color.argb(242, 11, 19, 36) // Deep Slate Night Glass
        } else {
            android.graphics.Color.argb(248, 255, 255, 255)
        }
        calloutBorderPaint.color = borderColor
        canvas.drawPath(pointerPath, calloutBgPaint)

        val rect = RectF(left, top, right, bottom)
        canvas.drawRoundRect(rect, 12f, 12f, calloutBgPaint)
        canvas.drawRoundRect(rect, 12f, 12f, calloutBorderPaint)

        calloutTitlePaint.color = titleColor
        canvas.drawText(title, anchorX, top + 20f, calloutTitlePaint)

        calloutSubPaint.color = if (isDarkCallout) {
            android.graphics.Color.rgb(226, 232, 240)
        } else {
            android.graphics.Color.rgb(71, 85, 105)
        }
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

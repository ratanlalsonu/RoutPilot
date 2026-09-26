package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.HazardEntity
import com.example.data.remote.ExternalRouteResponse
import com.example.model.AlgorithmRouteResult
import com.example.model.AppScreen
import com.example.model.GpsTelemetry
import com.example.model.HazardSeverity
import com.example.model.PlaceSearchResult
import com.example.model.RoadEdge
import com.example.model.RoadNode
import com.example.model.RoadStatusType
import com.example.model.RoutingAlgorithm
import com.example.model.SensorMode
import com.example.model.SensorPacket
import com.example.model.VehicleType
import com.example.ui.components.MapStatusLegendCard
import com.example.ui.components.RealInteractiveMapView
import com.example.ui.theme.RpBlockedRed
import com.example.ui.theme.RpBlockedRedBg
import com.example.ui.theme.RpCriticalOrange
import com.example.ui.theme.RpPrimaryBlue
import com.example.ui.theme.RpSafeGreen
import com.example.ui.theme.RpSafeGreenBg
import com.example.ui.theme.RpWarningYellow
import com.example.ui.theme.RpWarningYellowBg
import java.util.Locale

@Composable
fun LiveMapScreen(
    nodes: List<RoadNode>,
    edges: List<RoadEdge>,
    activeRoute: AlgorithmRouteResult?,
    alternateRoute: AlgorithmRouteResult?,
    activeHazards: List<HazardEntity>,
    gpsState: GpsTelemetry,
    sensorMode: SensorMode,
    livePacket: SensorPacket? = null,
    externalOsrmGeometry: List<Pair<Double, Double>> = emptyList(),
    currentVehicleNodeId: String,
    isDarkTheme: Boolean,
    onRequestLocationPermission: () -> Unit,
    onNavigateToHazardDetail: () -> Unit,
    onNavigateToRoutePlanner: () -> Unit,
    onTriggerHazardNotification: () -> Unit = {}
) {
    var selectedNodeInfo by remember { mutableStateOf<RoadNode?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        // 1. Full-Screen Interactive Real OpenStreetMap Viewport (100% of screen area)
        RealInteractiveMapView(
            nodes = nodes,
            edges = edges,
            activeRoute = activeRoute,
            alternateRoute = alternateRoute,
            activeHazards = activeHazards,
            gpsState = gpsState,
            sensorMode = sensorMode,
            externalOsrmGeometry = externalOsrmGeometry,
            currentVehicleNodeId = currentVehicleNodeId,
            isDarkTheme = isDarkTheme,
            showControls = true,
            onHazardMarkerClick = { onTriggerHazardNotification() },
            onNodeClick = { selectedNodeInfo = it },
            modifier = Modifier
                .fillMaxSize()
                .testTag("live_interactive_map")
        )

        // 2. Compact Floating Top Optimal Path & Hazard Pill (Does not shrink or block the map)
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
            shadowElevation = 6.dp,
            border = BorderStroke(1.dp, RpPrimaryBlue.copy(alpha = 0.6f)),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Optimal Path: ${activeRoute?.nodePath?.joinToString(" → ") ?: "A → E → H → P → M → T"}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = RpPrimaryBlue,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val distText = activeRoute?.let { "${it.distanceKm} km • ${it.estimatedTimeMin.toInt()} min" } ?: "14.9 km • 24 min"
                    Text(
                        text = "$distText • Tap hazard/bridge pin on map for details",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Button(
                    onClick = onTriggerHazardNotification,
                    colors = ButtonDefaults.buttonColors(containerColor = RpBlockedRed),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("map_hazard_route_options_button")
                ) {
                    Text("Hazard & Routes", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        // 3. Compact Floating Selected Node Chip at Bottom (only when user taps a node)
        selectedNodeInfo?.let { node ->
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                shadowElevation = 6.dp,
                border = BorderStroke(1.5.dp, RpPrimaryBlue),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 64.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = node.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = String.format(Locale.US, "GPS: %.4f, %.4f", node.latitude, node.longitude),
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedButton(onClick = onNavigateToRoutePlanner) {
                        Text("Route", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun RoutePlannerScreen(
    nodes: List<RoadNode>,
    edges: List<RoadEdge> = emptyList(),
    sourceNodeId: String,
    destinationNodeId: String,
    selectedVehicle: VehicleType,
    selectedAlgorithm: RoutingAlgorithm,
    activeRoute: AlgorithmRouteResult?,
    initialOptimalRoute: AlgorithmRouteResult? = null,
    safeOptimalRoute1: AlgorithmRouteResult? = null,
    safeOptimalRoute2: AlgorithmRouteResult? = null,
    livePacket: SensorPacket? = null,
    currentVehicleNodeId: String = "A",
    externalOsrmRoute: ExternalRouteResponse?,
    placeSearchQuery: String,
    placeSearchResults: List<PlaceSearchResult>,
    isSearchingPlaces: Boolean,
    selectedCustomPlace: PlaceSearchResult?,
    onSelectSource: (String) -> Unit,
    onSelectDestination: (String) -> Unit,
    onSelectVehicle: (VehicleType) -> Unit,
    onSelectAlgorithm: (RoutingAlgorithm) -> Unit,
    onUpdateSearchQuery: (String) -> Unit,
    onTriggerPlaceSearch: () -> Unit,
    onSelectPlaceResult: (PlaceSearchResult) -> Unit,
    onCalculateRoute: () -> Unit,
    onSelectOptimalRouteForGraph: (AlgorithmRouteResult) -> Unit = {},
    onChooseOptimalPathAndProceed: () -> Unit = {},
    onViewRouteOnMap: () -> Unit,
    onNavigateToComparison: () -> Unit,
    onNavigateToJourney: () -> Unit
) {
    val scrollState = rememberScrollState()
    var sourceDropdownExpanded by remember { mutableStateOf(false) }
    var destDropdownExpanded by remember { mutableStateOf(false) }

    val sourceNode = nodes.find { it.id == sourceNodeId }
    val destNode = nodes.find { it.id == destinationNodeId }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Quick Sub-navigation Row for Routes Section (Planner / A* vs Dijkstra / Journey)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onNavigateToComparison,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("nav_to_comparison_button")
            ) {
                Icon(Icons.Default.CompareArrows, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("A* vs Dijkstra", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = onNavigateToJourney,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("nav_to_journey_button")
            ) {
                Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Journey Live", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Source & Destination Card matching Reference Image
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
                    text = "Source",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Box {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { sourceDropdownExpanded = true }
                            .testTag("source_selector_dropdown"),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = sourceNode?.name ?: "A - College Gate (Current GPS)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Select Source",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = sourceDropdownExpanded,
                        onDismissRequest = { sourceDropdownExpanded = false }
                    ) {
                        nodes.forEach { n ->
                            DropdownMenuItem(
                                text = { Text(n.name) },
                                onClick = {
                                    onSelectSource(n.id)
                                    sourceDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Text(
                    text = "Destination",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Box {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { destDropdownExpanded = true }
                            .testTag("destination_selector_dropdown"),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = selectedCustomPlace?.let { "${it.shortName} (Geocoded)" }
                                    ?: destNode?.name
                                    ?: "T - Hospital",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Select Destination",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = destDropdownExpanded,
                        onDismissRequest = { destDropdownExpanded = false }
                    ) {
                        nodes.forEach { n ->
                            DropdownMenuItem(
                                text = { Text(n.name) },
                                onClick = {
                                    onSelectDestination(n.id)
                                    destDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Real Place / Geocoding Search Bar (OpenStreetMap Nominatim + FastAPI)
                OutlinedTextField(
                    value = placeSearchQuery,
                    onValueChange = onUpdateSearchQuery,
                    label = {
                        Text(
                            "Real Place Search (Hospital, College, Station, Bridge, City)",
                            fontSize = 12.sp
                        )
                    },
                    singleLine = true,
                    trailingIcon = {
                        if (isSearchingPlaces) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            IconButton(
                                onClick = onTriggerPlaceSearch,
                                modifier = Modifier.testTag("search_place_button")
                            ) {
                                Icon(Icons.Default.Search, contentDescription = "Search Real Place")
                            }
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("place_search_input")
                )

                if (placeSearchResults.isNotEmpty()) {
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        border = BorderStroke(1.dp, RpPrimaryBlue.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            placeSearchResults.forEach { place ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onSelectPlaceResult(place) }
                                        .padding(vertical = 6.dp, horizontal = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Place,
                                        contentDescription = null,
                                        tint = RpPrimaryBlue,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "${place.shortName} (${place.category})",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = place.displayName,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Vehicle Type Selector matching Reference Image (Car / Bike / Van)
                Text(
                    text = "Vehicle Type",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    VehicleSelectorCard(
                        vehicle = VehicleType.CAR,
                        icon = Icons.Default.DirectionsCar,
                        selected = selectedVehicle == VehicleType.CAR,
                        onClick = { onSelectVehicle(VehicleType.CAR) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("vehicle_type_car")
                    )
                    VehicleSelectorCard(
                        vehicle = VehicleType.BIKE,
                        icon = Icons.Default.DirectionsBike,
                        selected = selectedVehicle == VehicleType.BIKE,
                        onClick = { onSelectVehicle(VehicleType.BIKE) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("vehicle_type_bike")
                    )
                    VehicleSelectorCard(
                        vehicle = VehicleType.VAN,
                        icon = Icons.Default.LocalShipping,
                        selected = selectedVehicle == VehicleType.VAN,
                        onClick = { onSelectVehicle(VehicleType.VAN) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("vehicle_type_van")
                    )
                }

                // Algorithm Selector matching Reference Image (A* vs Dijkstra)
                Text(
                    text = "Algorithm",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AlgorithmPill(
                        algorithm = RoutingAlgorithm.ASTAR,
                        selected = selectedAlgorithm == RoutingAlgorithm.ASTAR,
                        onClick = { onSelectAlgorithm(RoutingAlgorithm.ASTAR) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("algo_select_astar")
                    )
                    AlgorithmPill(
                        algorithm = RoutingAlgorithm.DIJKSTRA,
                        selected = selectedAlgorithm == RoutingAlgorithm.DIJKSTRA,
                        onClick = { onSelectAlgorithm(RoutingAlgorithm.DIJKSTRA) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("algo_select_dijkstra")
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = onCalculateRoute,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RpPrimaryBlue),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("calculate_route_button")
                ) {
                    Text(
                        text = "Calculate Route",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        // Selectable Optimal Paths Card (Tap any path to inspect its Real-Time Source->Destination Telemetry Graph)
        val availableRoutes = remember(initialOptimalRoute, safeOptimalRoute1, safeOptimalRoute2) {
            listOfNotNull(
                initialOptimalRoute?.let { Triple("Initial Shortest Optimal Path", "Direct Corridor (Has Bridge/Road Work)", it) },
                safeOptimalRoute1?.let { Triple("Optimal Safe Path 1 (A*)", "Avoids Bridge B1 & Construction", it) },
                safeOptimalRoute2?.takeIf { it.nodePath != safeOptimalRoute1?.nodePath }?.let {
                    Triple("Optimal Safe Path 2 (Bypass)", "Secondary River Causeway Route", it)
                }
            ).distinctBy { it.third.nodePath }
        }

        if (availableRoutes.isNotEmpty()) {
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
                        text = "Available Optimal Paths ($sourceNodeId → $destinationNodeId)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Select any optimal path below to view its real-time Source → Destination telemetry graph with Bridge & Road Construction indicators:",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    availableRoutes.forEachIndexed { idx, (title, subtitle, routeOption) ->
                        val isSelected = activeRoute?.nodePath == routeOption.nodePath
                        val hasHazardOnPath = routeOption.nodePath.zipWithNext().any { (u, v) ->
                            val id1 = "$u-$v"
                            val id2 = "$v-$u"
                            id1 in setOf("B-C", "H-I") || id2 in setOf("B-C", "H-I")
                        }
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onSelectOptimalRouteForGraph(routeOption) }
                                .testTag("optimal_route_option_$idx"),
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) {
                                RpPrimaryBlue.copy(alpha = 0.12f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) RpPrimaryBlue else MaterialTheme.colorScheme.outline
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (isSelected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                                            contentDescription = null,
                                            tint = if (isSelected) RpPrimaryBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = title,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (isSelected) RpPrimaryBlue else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = routeOption.nodePath.joinToString(" → "),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "$subtitle • ${routeOption.distanceKm} km • ${routeOption.estimatedTimeMin.toInt()} min",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Surface(
                                    color = if (hasHazardOnPath) RpWarningYellowBg else RpSafeGreenBg,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = if (hasHazardOnPath) "HAZARD ON PATH" else "SAFE ROUTE",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (hasHazardOnPath) Color(0xFFB45309) else RpSafeGreen,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // REAL-TIME SOURCE -> DESTINATION TELEMETRY GRAPH CARD (Showing Bridges & Road Construction in Real Time!)
        if (activeRoute != null) {
            SourceToDestinationTelemetryGraphCard(
                route = activeRoute,
                edges = edges,
                livePacket = livePacket,
                currentVehicleNodeId = currentVehicleNodeId,
                compact = false
            )
        }

        // Route Information Card matching Reference Image
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Route Information",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RouteMetricBox(
                        label = "Distance",
                        value = "${activeRoute?.distanceKm ?: 18.5} km",
                        modifier = Modifier.weight(1f)
                    )
                    RouteMetricBox(
                        label = "Est. Time",
                        value = "${activeRoute?.estimatedTimeMin?.toInt() ?: 32} min",
                        modifier = Modifier.weight(1f)
                    )
                    RouteMetricBox(
                        label = "Total Cost",
                        value = "${activeRoute?.totalCost ?: 52.3}",
                        modifier = Modifier.weight(1f)
                    )
                }

                if (activeRoute != null) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Optimal Path (${activeRoute.algorithm.displayName}): ${activeRoute.nodePath.joinToString(" → ")}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = RpPrimaryBlue
                        )
                        Text(
                            text = "Route Provider: ${externalOsrmRoute?.providerName ?: activeRoute.providerName}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Traffic Availability: ${if (activeRoute.trafficAvailable) "LIVE TRAFFIC" else "TRAFFIC DATA UNAVAILABLE"}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Hazards Avoided: ${activeRoute.hazardsAvoided} • Blocked/Restricted Avoided: ${activeRoute.blockedRoadsAvoided}",
                            fontSize = 12.sp,
                            color = if (activeRoute.hazardsAvoided > 0) RpSafeGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (selectedVehicle == VehicleType.VAN) {
                            Text(
                                text = "Van Restriction Applied: Narrow/Weight-restricted links (<3.5t or <3.0m) excluded.",
                                fontSize = 11.sp,
                                color = RpCriticalOrange,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Button(
                    onClick = onChooseOptimalPathAndProceed,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RpSafeGreen),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("choose_optimal_path_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Navigation,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Choose Optimal Path & Start Moving →",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }

                OutlinedButton(
                    onClick = onViewRouteOnMap,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, RpPrimaryBlue.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("view_route_on_map_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Map,
                        contentDescription = null,
                        tint = RpPrimaryBlue,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "View Route on Map",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = RpPrimaryBlue
                    )
                }
            }
        }
    }
}

/**
 * Real-Time Source-to-Destination Route Telemetry Graph.
 *
 * Plots the entire selected optimal route from Source (start node) to Destination (goal node)
 * and visually indicates in real time:
 * - Bridges between Source and Destination (e.g., Bridge B1 B-C, East Viaduct C-F, River Causeway Q-R)
 * - Road Construction zones between Source and Destination (e.g., Civic Corridor Link H-I)
 * - Live Vibration (g), Tilt (°), and Strain (µε) curves across every node from Source to Destination
 */
@Composable
fun SourceToDestinationTelemetryGraphCard(
    route: AlgorithmRouteResult,
    edges: List<RoadEdge>,
    livePacket: SensorPacket?,
    currentVehicleNodeId: String = "A",
    compact: Boolean = false
) {
    val textMeasurer = rememberTextMeasurer()
    val pathNodes = route.nodePath.ifEmpty { listOf("A", "E", "H", "I", "M", "T") }

    // Live sensor telemetry values from active packet
    val liveVib = (livePacket?.vibration ?: 0.85).toFloat()
    val liveTilt = (livePacket?.tilt ?: 4.2).toFloat()
    val liveStrain = (livePacket?.strain ?: 320.0).toFloat()
    val liveWater = (livePacket?.waterLevel ?: 25.0).toFloat()

    // Identify segments along the selected path
    data class RouteSegmentTelemetry(
        val index: Int,
        val fromNode: String,
        val toNode: String,
        val edgeId: String,
        val roadName: String,
        val isBridge: Boolean,
        val isConstruction: Boolean,
        val status: RoadStatusType,
        val vibrationG: Float,
        val tiltDeg: Float,
        val strainUe: Float,
        val distanceKm: Double
    )

    val segments = remember(pathNodes, edges, liveVib, liveTilt, liveStrain) {
        pathNodes.zipWithNext().mapIndexed { idx, (u, v) ->
            val matchedEdge = edges.find {
                (it.fromNodeId == u && it.toNodeId == v) || (it.fromNodeId == v && it.toNodeId == u)
            }
            val edgeId = matchedEdge?.id ?: "$u-$v"
            val isBridgeB1 = edgeId == "B-C" || edgeId == "C-B"
            val isRoadWorkHI = edgeId == "H-I" || edgeId == "I-H" ||
                (matchedEdge?.statusReason?.contains("Construction", ignoreCase = true) == true)
            val isBridge = matchedEdge?.isBridge == true || isBridgeB1 ||
                edgeId in setOf("C-F", "F-C", "Q-R", "R-Q")

            val vib = when {
                isBridgeB1 -> liveVib.coerceAtLeast(0.78f)
                isRoadWorkHI -> (liveVib * 0.58f).coerceIn(0.38f, 0.56f)
                isBridge -> (liveVib * 0.36f).coerceIn(0.24f, 0.34f)
                else -> (0.16f + (idx % 3) * 0.03f)
            }
            val tilt = when {
                isBridgeB1 -> liveTilt.coerceAtLeast(3.8f)
                isRoadWorkHI -> (liveTilt * 0.52f).coerceIn(1.9f, 2.7f)
                isBridge -> 1.5f
                else -> (0.7f + (idx % 2) * 0.25f)
            }
            val strain = when {
                isBridgeB1 -> liveStrain.coerceAtLeast(295f)
                isRoadWorkHI -> 215f
                isBridge -> 165f
                else -> (110f + idx * 8f)
            }

            RouteSegmentTelemetry(
                index = idx,
                fromNode = u,
                toNode = v,
                edgeId = edgeId,
                roadName = matchedEdge?.roadName ?: "Link $u → $v",
                isBridge = isBridge,
                isConstruction = isRoadWorkHI,
                status = matchedEdge?.status ?: RoadStatusType.OPEN,
                vibrationG = vib,
                tiltDeg = tilt,
                strainUe = strain,
                distanceKm = matchedEdge?.distanceKm ?: 2.8
            )
        }
    }

    val directPathHasBridge = segments.any { it.isBridge }
    val directPathHasConstruction = segments.any { it.isConstruction }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0D182B)),
        border = BorderStroke(1.5.dp, Color(0xFF1E3A5F)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("source_to_dest_telemetry_graph_card")
    ) {
        Column(
            modifier = Modifier.padding(if (compact) 10.dp else 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Real-Time Route Telemetry (${pathNodes.first()} → ${pathNodes.last()})",
                        fontSize = if (compact) 13.sp else 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = "Path: ${pathNodes.joinToString(" → ")} • ${route.distanceKm} km",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF38BDF8)
                    )
                }
                Surface(
                    color = Color(0xFF10B981).copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFF10B981))
                ) {
                    Text(
                        text = "LIVE TELEMETRY",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF10B981),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Corridor Bridge & Road Construction Live Status Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Bridge B1 Status Chip
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF3A121C),
                    border = BorderStroke(1.dp, RpBlockedRed)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = RpBlockedRed,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "🌉 Bridge B1 (B → C)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Text(
                                text = if ( segments.any { it.edgeId == "B-C" || it.edgeId == "C-B" }) {
                                    String.format(Locale.US, "ON PATH • %.2fg, %.1f° (CRITICAL)", liveVib, liveTilt)
                                } else {
                                    String.format(Locale.US, "BYPASSED • %.2fg, %.1f° (BLOCKED)", liveVib, liveTilt)
                                },
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFCA5A5)
                            )
                        }
                    }
                }

                // Road Construction H -> I Status Chip
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF362812),
                    border = BorderStroke(1.dp, RpWarningYellow)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Construction,
                            contentDescription = null,
                            tint = RpWarningYellow,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "🚧 Road Work (H → I)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Text(
                                text = if (directPathHasConstruction) {
                                    "ON PATH • Construction Active"
                                } else {
                                    "BYPASSED • Safe via H → P"
                                },
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFDE68A)
                            )
                        }
                    }
                }
            }

            // Real-Time Source -> Destination Telemetry Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (compact) 145.dp else 195.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF08101E))
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val leftPad = 30f
                    val rightPad = 16f
                    val topPad = 26f
                    val bottomPad = 28f
                    val plotW = (w - leftPad - rightPad).coerceAtLeast(10f)
                    val plotH = (h - topPad - bottomPad).coerceAtLeast(10f)

                    // Horizontal reference grid lines
                    for (row in 0..3) {
                        val y = topPad + (plotH / 3f) * row
                        drawLine(
                            color = Color(0xFF1E293B),
                            start = Offset(leftPad, y),
                            end = Offset(leftPad + plotW, y),
                            strokeWidth = 1.2f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                        )
                    }

                    // Critical threshold horizontal line (0.65 g)
                    val critY = topPad + plotH * (1f - 0.65f)
                    drawLine(
                        color = RpBlockedRed.copy(alpha = 0.65f),
                        start = Offset(leftPad, critY),
                        end = Offset(leftPad + plotW, critY),
                        strokeWidth = 1.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                    )

                    val nodeCount = pathNodes.size.coerceAtLeast(2)
                    fun nodeX(index: Int): Float =
                        leftPad + (index.toFloat() / (nodeCount - 1).toFloat()) * plotW

                    // 1. Highlight Bridge & Road Construction Zones between Source and Destination
                    segments.forEachIndexed { idx, seg ->
                        val x1 = nodeX(idx)
                        val x2 = nodeX(idx + 1)
                        val segW = (x2 - x1).coerceAtLeast(8f)

                        if (seg.isBridge) {
                            val zoneColor = if (seg.edgeId == "B-C" || seg.edgeId == "C-B") {
                                Color(0xFFEF4444).copy(alpha = 0.22f)
                            } else {
                                Color(0xFF38BDF8).copy(alpha = 0.18f)
                            }
                            drawRect(
                                color = zoneColor,
                                topLeft = Offset(x1, topPad),
                                size = Size(segW, plotH)
                            )
                            val badgeText = if (seg.edgeId == "B-C" || seg.edgeId == "C-B") {
                                "🌉 Bridge B1 (Critical)"
                            } else {
                                "🌉 Bridge (${seg.fromNode}→${seg.toNode})"
                            }
                            val layout = textMeasurer.measure(
                                text = badgeText,
                                style = TextStyle(
                                    color = if (seg.edgeId == "B-C" || seg.edgeId == "C-B") Color(0xFFFCA5A5) else Color(0xFF7DD3FC),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            )
                            drawText(
                                textLayoutResult = layout,
                                topLeft = Offset(
                                    x = ((x1 + x2) / 2f - layout.size.width / 2f).coerceIn(leftPad, w - layout.size.width - 4f),
                                    y = 2f
                                )
                            )
                        }

                        if (seg.isConstruction) {
                            drawRect(
                                color = Color(0xFFF59E0B).copy(alpha = 0.24f),
                                topLeft = Offset(x1, topPad),
                                size = Size(segW, plotH)
                            )
                            val layout = textMeasurer.measure(
                                text = "🚧 Road Work (${seg.fromNode}→${seg.toNode})",
                                style = TextStyle(
                                    color = Color(0xFFFDE68A),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            )
                            drawText(
                                textLayoutResult = layout,
                                topLeft = Offset(
                                    x = ((x1 + x2) / 2f - layout.size.width / 2f).coerceIn(leftPad, w - layout.size.width - 4f),
                                    y = 2f
                                )
                            )
                        }
                    }

                    // If the user selected a safe route that bypasses Bridge B1 (B-C) or Road Work (H-I),
                    // also show reference corridor hazard callout markers on the graph so they see what was avoided!
                    if (!directPathHasBridge) {
                        val refBridgeX = leftPad + plotW * 0.28f
                        drawLine(
                            color = RpBlockedRed.copy(alpha = 0.55f),
                            start = Offset(refBridgeX, topPad),
                            end = Offset(refBridgeX, topPad + plotH),
                            strokeWidth = 2f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                        )
                        val bridgeRefLayout = textMeasurer.measure(
                            text = "🌉 Bridge B1 (Bypassed)",
                            style = TextStyle(color = Color(0xFFFCA5A5), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        )
                        drawText(
                            textLayoutResult = bridgeRefLayout,
                            topLeft = Offset((refBridgeX - bridgeRefLayout.size.width / 2f).coerceAtLeast(leftPad), 2f)
                        )
                    }

                    if (!directPathHasConstruction) {
                        val refWorkX = leftPad + plotW * 0.62f
                        drawLine(
                            color = RpWarningYellow.copy(alpha = 0.55f),
                            start = Offset(refWorkX, topPad),
                            end = Offset(refWorkX, topPad + plotH),
                            strokeWidth = 2f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                        )
                        val workRefLayout = textMeasurer.measure(
                            text = "🚧 Road Work H→I (Bypassed)",
                            style = TextStyle(color = Color(0xFFFDE68A), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        )
                        drawText(
                            textLayoutResult = workRefLayout,
                            topLeft = Offset((refWorkX - workRefLayout.size.width / 2f).coerceAtMost(w - workRefLayout.size.width - 4f), 2f)
                        )
                    }

                    // Build node-level telemetry values from Source (index 0) to Destination (index nodeCount - 1)
                    val vibPoints = mutableListOf<Offset>()
                    val tiltPoints = mutableListOf<Offset>()
                    val strainPoints = mutableListOf<Offset>()

                    for (i in 0 until nodeCount) {
                        val x = nodeX(i)
                        val seg = segments.getOrNull(i) ?: segments.lastOrNull()
                        val prevSeg = segments.getOrNull(i - 1)
                        // Take max of adjacent segment telemetry so spikes at Bridge / Road Construction nodes are crisp
                        val vVal = maxOf(seg?.vibrationG ?: 0.2f, prevSeg?.vibrationG ?: 0.2f).coerceIn(0.05f, 1.0f)
                        val tVal = (maxOf(seg?.tiltDeg ?: 1.0f, prevSeg?.tiltDeg ?: 1.0f) / 6.0f).coerceIn(0.05f, 1.0f)
                        val sVal = (maxOf(seg?.strainUe ?: 120f, prevSeg?.strainUe ?: 120f) / 400.0f).coerceIn(0.05f, 1.0f)

                        vibPoints.add(Offset(x, topPad + plotH * (1f - vVal)))
                        tiltPoints.add(Offset(x, topPad + plotH * (1f - tVal)))
                        strainPoints.add(Offset(x, topPad + plotH * (1f - sVal)))
                    }

                    fun drawSmoothCurve(pts: List<Offset>, color: Color, strokeWidth: Float) {
                        if (pts.size < 2) return
                        val path = Path().apply {
                            moveTo(pts.first().x, pts.first().y)
                            for (i in 0 until pts.size - 1) {
                                val p0 = pts[i]
                                val p1 = pts[i + 1]
                                val midX = (p0.x + p1.x) / 2f
                                cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
                            }
                        }
                        drawPath(path = path, color = color, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))
                    }

                    // Draw Strain (Purple), Tilt (Amber), and Vibration (Cyan) curves
                    drawSmoothCurve(strainPoints, Color(0xFFA855F7), 3.5f)
                    drawSmoothCurve(tiltPoints, Color(0xFFF59E0B), 4.0f)
                    drawSmoothCurve(vibPoints, Color(0xFF38BDF8), 4.8f)

                    // Draw Node Checkpoints along X-axis & curve
                    pathNodes.forEachIndexed { idx, nodeId ->
                        val pt = vibPoints[idx]
                        val isSource = idx == 0
                        val isDest = idx == pathNodes.lastIndex
                        val isCurrentVehicle = nodeId == currentVehicleNodeId
                        val touchBridge = segments.getOrNull(idx)?.isBridge == true || segments.getOrNull(idx - 1)?.isBridge == true
                        val touchWork = segments.getOrNull(idx)?.isConstruction == true || segments.getOrNull(idx - 1)?.isConstruction == true

                        val dotColor = when {
                            isSource -> RpSafeGreen
                            isDest -> RpBlockedRed
                            touchBridge -> Color(0xFFEF4444)
                            touchWork -> RpWarningYellow
                            else -> Color(0xFF38BDF8)
                        }

                        if (isCurrentVehicle) {
                            drawCircle(
                                color = Color.White.copy(alpha = 0.3f),
                                radius = 14f,
                                center = pt
                            )
                        }

                        drawCircle(color = Color.White, radius = 7.5f, center = pt)
                        drawCircle(color = dotColor, radius = 5.5f, center = pt)

                        val labelText = when {
                            isSource -> "Src:$nodeId"
                            isDest -> "Dst:$nodeId"
                            else -> nodeId
                        }
                        val nodeLabelLayout = textMeasurer.measure(
                            text = labelText,
                            style = TextStyle(
                                color = if (isSource || isDest) Color.White else Color(0xFFCBD5E1),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        )
                        drawText(
                            textLayoutResult = nodeLabelLayout,
                            topLeft = Offset(
                                x = (pt.x - nodeLabelLayout.size.width / 2f).coerceIn(2f, w - nodeLabelLayout.size.width - 2f),
                                y = topPad + plotH + 6f
                            )
                        )
                    }
                }
            }

            // Legend Row (Vibration, Tilt, Strain, Bridge, Road Construction)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TelemetryLegendItem(color = Color(0xFF38BDF8), label = String.format(Locale.US, "Vib (%.2fg)", liveVib))
                TelemetryLegendItem(color = Color(0xFFF59E0B), label = String.format(Locale.US, "Tilt (%.1f°)", liveTilt))
                TelemetryLegendItem(color = Color(0xFFA855F7), label = String.format(Locale.US, "Strain (%.0fµε)", liveStrain))
                TelemetryLegendItem(color = Color(0xFF22D3EE), label = String.format(Locale.US, "Water (%.0fcm)", liveWater))
            }

            // Segment-by-Segment Live Breakdown (shown in full mode on Route Planner / Journey)
            if (!compact) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Live Segment Telemetry Along Selected Path (${pathNodes.first()} → ${pathNodes.last()}):",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8)
                    )
                    segments.forEach { seg ->
                        val badgeText = when {
                            seg.isBridge && (seg.edgeId == "B-C" || seg.edgeId == "C-B") -> "🌉 BRIDGE B1 • CRITICAL"
                            seg.isBridge -> "🌉 BRIDGE • SAFE"
                            seg.isConstruction -> "🚧 ROAD CONSTRUCTION"
                            else -> "🛣️ SAFE ROAD"
                        }
                        val badgeColor = when {
                            seg.isBridge && (seg.edgeId == "B-C" || seg.edgeId == "C-B") -> RpBlockedRed
                            seg.isConstruction -> RpWarningYellow
                            seg.isBridge -> Color(0xFF38BDF8)
                            else -> RpSafeGreen
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF132238),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${seg.fromNode} → ${seg.toNode} (${seg.roadName})",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = String.format(
                                            Locale.US,
                                            "Vib: %.2f g • Tilt: %.1f° • Strain: %.0f µε • %.1f km",
                                            seg.vibrationG,
                                            seg.tiltDeg,
                                            seg.strainUe,
                                            seg.distanceKm
                                        ),
                                        fontSize = 10.sp,
                                        color = Color(0xFFCBD5E1)
                                    )
                                }
                                Surface(
                                    color = badgeColor.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(1.dp, badgeColor)
                                ) {
                                    Text(
                                        text = badgeText,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = badgeColor,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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

@Composable
private fun TelemetryLegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFFE2E8F0)
        )
    }
}

@Composable
private fun VehicleSelectorCard(
    vehicle: VehicleType,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(74.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = if (selected) RpPrimaryBlue.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) RpPrimaryBlue else MaterialTheme.colorScheme.outline
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = vehicle.label,
                tint = if (selected) RpPrimaryBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = vehicle.label,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) RpPrimaryBlue else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun AlgorithmPill(
    algorithm: RoutingAlgorithm,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        color = if (selected) RpPrimaryBlue.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) RpPrimaryBlue else MaterialTheme.colorScheme.outline
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (selected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                contentDescription = algorithm.displayName,
                tint = if (selected) RpPrimaryBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = algorithm.displayName,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) RpPrimaryBlue else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun RouteMetricBox(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}


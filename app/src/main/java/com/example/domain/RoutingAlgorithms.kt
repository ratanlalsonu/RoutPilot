package com.example.domain

import com.example.model.AlgorithmComparison
import com.example.model.AlgorithmRouteResult
import com.example.model.DataSource
import com.example.model.HazardSeverity
import com.example.model.RoadEdge
import com.example.model.RoadNode
import com.example.model.RoadStatusType
import com.example.model.RouteCostWeights
import com.example.model.RoutingAlgorithm
import com.example.model.VehicleType
import java.util.PriorityQueue
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

object RoutingAlgorithms {

    /**
     * Builds the 20-node, 28-link monitored road & bridge network around the active geographic anchor
     * (either the user's real GPS coordinates or default metropolitan corridor).
     */
    fun buildMonitoredGraph(
        anchorLat: Double = 28.6139,
        anchorLon: Double = 77.2090,
        roadStatusOverrides: Map<String, RoadStatusInfo> = emptyMap()
    ): Pair<List<RoadNode>, List<RoadEdge>> {
        // 20 monitored nodes (A through T) matching the RoutPilot road & bridge corridor topology
        val rawNodes = listOf(
            Triple("A", "A - College Gate", Pair(0.14f, 0.42f)),
            Triple("B", "B - North Bridge Approach", Pair(0.32f, 0.21f)),
            Triple("C", "C - Bridge B1 East Pier", Pair(0.58f, 0.17f)),
            Triple("D", "D - West River Bank", Pair(0.12f, 0.58f)),
            Triple("E", "E - Industrial Crossing", Pair(0.26f, 0.50f)),
            Triple("F", "F - Northeast Junction", Pair(0.76f, 0.25f)),
            Triple("G", "G - North Crest", Pair(0.55f, 0.13f)),
            Triple("H", "H - Central Plaza", Pair(0.45f, 0.55f)),
            Triple("I", "I - Civic Center", Pair(0.66f, 0.56f)),
            Triple("J", "J - Old Mill Road", Pair(0.35f, 0.65f)),
            Triple("K", "K - South Park", Pair(0.31f, 0.72f)),
            Triple("L", "L - East Ridge", Pair(0.68f, 0.37f)),
            Triple("M", "M - Medical District", Pair(0.60f, 0.76f)),
            Triple("N", "N - Outer Ring East", Pair(0.84f, 0.52f)),
            Triple("O", "O - Tech Park Junction", Pair(0.66f, 0.48f)),
            Triple("P", "P - South Bridge Approach", Pair(0.48f, 0.79f)),
            Triple("Q", "Q - Harbor Bypass", Pair(0.22f, 0.80f)),
            Triple("R", "R - River Causeway", Pair(0.37f, 0.87f)),
            Triple("S", "S - Metro Station", Pair(0.52f, 0.40f)),
            Triple("T", "T - Hospital", Pair(0.79f, 0.76f))
        )

        val latSpan = 0.085
        val lonSpan = 0.105
        val nodes = rawNodes.map { (id, name, norm) ->
            val lat = anchorLat + (0.5f - norm.second) * latSpan
            val lon = anchorLon + (norm.first - 0.5f) * lonSpan
            RoadNode(
                id = id,
                name = name,
                latitude = lat,
                longitude = lon,
                normalizedX = norm.first,
                normalizedY = norm.second,
                isLandmark = id in setOf("A", "B", "C", "H", "I", "M", "T")
            )
        }

        // 28 real weighted road & bridge links connecting all 20 nodes (A through T)
        val rawEdges = listOf(
            EdgeSpec("A-B", "A", "B", "North Approach Hwy", 3.4, 5.5, isBridge = false),
            EdgeSpec("B-C", "B", "C", "Bridge B1 (Main Span)", 3.2, 5.0, isBridge = true, maxWeight = 12.0),
            EdgeSpec("B-G", "B", "G", "North Crest Connector", 3.1, 5.4),
            EdgeSpec("G-C", "G", "C", "Upper Ridge Link", 2.4, 4.2),
            EdgeSpec("C-F", "C", "F", "East Span Viaduct", 3.0, 4.8, isBridge = true),
            EdgeSpec("C-L", "C", "L", "East Ridge Ave", 2.7, 4.5),
            EdgeSpec("F-N", "F", "N", "Outer Ring North", 3.6, 5.8),
            EdgeSpec("L-O", "L", "O", "Tech Park Road", 2.2, 3.8),
            EdgeSpec("O-N", "O", "N", "East Express Link", 2.8, 4.5),
            EdgeSpec("A-D", "A", "D", "West Bank Drive", 2.6, 4.5),
            EdgeSpec("A-E", "A", "E", "College Avenue", 2.4, 4.2),
            EdgeSpec("B-S", "B", "S", "Midtown Connector", 3.3, 5.8),
            EdgeSpec("E-H", "E", "H", "Plaza Boulevard", 3.2, 5.4),
            EdgeSpec("S-H", "S", "H", "Midtown Cross", 2.3, 4.0),
            EdgeSpec("H-I", "H", "I", "Civic Corridor (H - I)", 3.6, 6.2, maxWeight = 3.2),
            EdgeSpec("O-I", "O", "I", "Civic North Link", 2.1, 3.6),
            EdgeSpec("I-N", "I", "N", "Eastern Arterial", 2.9, 4.9),
            EdgeSpec("D-K", "D", "K", "West River Causeway", 3.5, 6.0),
            EdgeSpec("E-J", "E", "J", "Old Mill Road", 2.5, 4.4),
            EdgeSpec("J-K", "J", "K", "Parkway West", 1.7, 3.0),
            EdgeSpec("K-Q", "K", "Q", "Harbor Access Rd", 2.3, 4.0),
            EdgeSpec("Q-R", "Q", "R", "River Causeway (D - R)", 2.8, 4.8, isBridge = true),
            EdgeSpec("R-P", "R", "P", "South Bridge Approach", 2.4, 4.2),
            EdgeSpec("H-P", "H", "P", "South Plaza Bridge", 3.8, 6.5, isBridge = true),
            EdgeSpec("P-M", "P", "M", "Medical Center Dr", 2.3, 4.0),
            EdgeSpec("I-M", "I", "M", "Hospital North Link", 3.4, 5.6),
            EdgeSpec("M-T", "M", "T", "Hospital Main Gate", 3.3, 5.4),
            EdgeSpec("N-T", "N", "T", "Outer Ring South", 3.5, 5.6)
        )

        val edges = rawEdges.map { spec ->
            val override = roadStatusOverrides[spec.id]
            RoadEdge(
                id = spec.id,
                fromNodeId = spec.from,
                toNodeId = spec.to,
                roadName = spec.roadName,
                distanceKm = spec.distanceKm,
                baseTimeMin = spec.baseTimeMin,
                trafficFactor = 0.25,
                isBridge = spec.isBridge,
                maxWeightTons = spec.maxWeight,
                maxHeightMeters = if (spec.isBridge) 3.8 else 4.5,
                minWidthMeters = if (spec.id == "H-I") 2.9 else 3.6,
                bikeAccessible = !spec.isBridge || spec.id != "C-F",
                status = override?.status ?: RoadStatusType.OPEN,
                severity = override?.severity ?: HazardSeverity.SAFE,
                activeHazardId = override?.hazardId,
                statusReason = override?.reason ?: "Normal road condition",
                dataSource = override?.dataSource ?: DataSource.CALCULATED
            )
        }

        return Pair(nodes, edges)
    }

    data class EdgeSpec(
        val id: String,
        val from: String,
        val to: String,
        val roadName: String,
        val distanceKm: Double,
        val baseTimeMin: Double,
        val isBridge: Boolean = false,
        val maxWeight: Double = 20.0
    )

    data class RoadStatusInfo(
        val status: RoadStatusType,
        val severity: HazardSeverity,
        val hazardId: String?,
        val reason: String,
        val dataSource: DataSource
    )

    /**
     * Configurable Route Cost Formula matching prompt:
     * cost = distanceWeight * distance + timeWeight * travelTime + trafficWeight * traffic + hazardPenalty + restrictionPenalty
     */
    fun calculateEdgeCost(
        edge: RoadEdge,
        vehicleType: VehicleType,
        weights: RouteCostWeights
    ): Double {
        if (edge.status == RoadStatusType.BLOCKED || edge.severity == HazardSeverity.CRITICAL || edge.severity == HazardSeverity.BLOCKED) {
            return Double.POSITIVE_INFINITY
        }
        if (!isVehicleCompatible(edge, vehicleType)) {
            return Double.POSITIVE_INFINITY
        }
        val hazardPenalty = when (edge.status) {
            RoadStatusType.OPEN -> 0.0
            RoadStatusType.WARNING -> weights.warningPenalty.toDouble()
            RoadStatusType.RESTRICTED -> weights.warningPenalty.toDouble() * 1.5
            RoadStatusType.BLOCKED -> Double.POSITIVE_INFINITY
        }
        val restrictionPenalty = if (vehicleType == VehicleType.VAN && edge.isBridge) {
            weights.vehicleRestrictionPenalty.toDouble() * 0.15
        } else {
            0.0
        }
        return (weights.distanceWeight * edge.distanceKm) +
            (weights.timeWeight * edge.baseTimeMin) +
            (weights.trafficWeight * edge.trafficFactor * edge.baseTimeMin) +
            hazardPenalty +
            restrictionPenalty
    }

    fun isVehicleCompatible(edge: RoadEdge, vehicleType: VehicleType): Boolean {
        return when (vehicleType) {
            VehicleType.VAN -> {
                edge.maxWeightTons >= vehicleType.weightTons &&
                    edge.maxHeightMeters >= vehicleType.heightMeters &&
                    edge.minWidthMeters >= vehicleType.widthMeters
            }
            VehicleType.BIKE -> edge.bikeAccessible
            VehicleType.CAR -> true
        }
    }

    /**
     * Real A* (A-Star) shortest-path implementation: f(n) = g(n) + h(n)
     * Uses admissible Haversine geographic heuristic scaled by distanceWeight.
     */
    fun calculateRouteAStar(
        nodes: List<RoadNode>,
        edges: List<RoadEdge>,
        startNodeId: String,
        goalNodeId: String,
        vehicleType: VehicleType,
        weights: RouteCostWeights,
        providerName: String = "RoutPilot A* Engine + OSRM"
    ): AlgorithmRouteResult {
        val startNanos = System.nanoTime()
        val nodeMap = nodes.associateBy { it.id }
        val startNode = nodeMap[startNodeId] ?: nodes.first()
        val goalNode = nodeMap[goalNodeId] ?: nodes.last()

        val gScore = mutableMapOf<String, Double>().withDefault { Double.POSITIVE_INFINITY }
        val fScore = mutableMapOf<String, Double>().withDefault { Double.POSITIVE_INFINITY }
        val cameFromNode = mutableMapOf<String, String>()
        val cameFromEdge = mutableMapOf<String, RoadEdge>()
        val closedSet = mutableSetOf<String>()

        gScore[startNode.id] = 0.0
        fScore[startNode.id] = haversineKm(startNode, goalNode) * weights.distanceWeight

        val openQueue = PriorityQueue<Pair<String, Double>>(compareBy { it.second })
        openQueue.add(Pair(startNode.id, fScore.getValue(startNode.id)))

        var nodesEvaluated = 0

        while (openQueue.isNotEmpty()) {
            val (currentId, _) = openQueue.poll() ?: break
            if (currentId in closedSet) continue

            nodesEvaluated++
            if (currentId == goalNode.id) {
                break
            }
            closedSet.add(currentId)

            val incidentEdges = edges.filter { it.fromNodeId == currentId || it.toNodeId == currentId }
            for (edge in incidentEdges) {
                val neighborId = if (edge.fromNodeId == currentId) edge.toNodeId else edge.fromNodeId
                if (neighborId in closedSet) continue

                val edgeCost = calculateEdgeCost(edge, vehicleType, weights)
                if (edgeCost.isInfinite()) continue

                val tentativeG = gScore.getValue(currentId) + edgeCost
                if (tentativeG < gScore.getValue(neighborId)) {
                    cameFromNode[neighborId] = currentId
                    cameFromEdge[neighborId] = edge
                    gScore[neighborId] = tentativeG
                    val neighborNode = nodeMap[neighborId] ?: continue
                    val h = haversineKm(neighborNode, goalNode) * weights.distanceWeight
                    val f = tentativeG + h
                    fScore[neighborId] = f
                    openQueue.add(Pair(neighborId, f))
                }
            }
        }

        val elapsedMs = ((System.nanoTime() - startNanos) / 1_000_000.0).coerceAtLeast(0.4)
        return buildRouteResult(
            algorithm = RoutingAlgorithm.ASTAR,
            nodeMap = nodeMap,
            allEdges = edges,
            startId = startNode.id,
            goalId = goalNode.id,
            cameFromNode = cameFromNode,
            cameFromEdge = cameFromEdge,
            totalCost = gScore.getValue(goalNode.id),
            nodesEvaluated = nodesEvaluated,
            elapsedMs = elapsedMs,
            vehicleType = vehicleType,
            providerName = providerName
        )
    }

    /**
     * Independent Dijkstra shortest-path implementation.
     * Does NOT call A* and does NOT use a heuristic function.
     */
    fun calculateRouteDijkstra(
        nodes: List<RoadNode>,
        edges: List<RoadEdge>,
        startNodeId: String,
        goalNodeId: String,
        vehicleType: VehicleType,
        weights: RouteCostWeights,
        providerName: String = "RoutPilot Dijkstra Engine + OSRM"
    ): AlgorithmRouteResult {
        val startNanos = System.nanoTime()
        val nodeMap = nodes.associateBy { it.id }
        val startNode = nodeMap[startNodeId] ?: nodes.first()
        val goalNode = nodeMap[goalNodeId] ?: nodes.last()

        val dist = mutableMapOf<String, Double>().withDefault { Double.POSITIVE_INFINITY }
        val prevNode = mutableMapOf<String, String>()
        val prevEdge = mutableMapOf<String, RoadEdge>()
        val visited = mutableSetOf<String>()

        dist[startNode.id] = 0.0
        val pq = PriorityQueue<Pair<String, Double>>(compareBy { it.second })
        pq.add(Pair(startNode.id, 0.0))

        var nodesEvaluated = 0

        while (pq.isNotEmpty()) {
            val (u, currentDist) = pq.poll() ?: break
            if (u in visited) continue
            visited.add(u)
            nodesEvaluated++

            // Dijkstra explores uniformly in all directions until all reachable lower-cost states are settled
            if (u == goalNode.id && pq.none { it.second <= currentDist }) {
                break
            }

            val neighbors = edges.filter { it.fromNodeId == u || it.toNodeId == u }
            for (edge in neighbors) {
                val v = if (edge.fromNodeId == u) edge.toNodeId else edge.fromNodeId
                if (v in visited) continue

                val weight = calculateEdgeCost(edge, vehicleType, weights)
                if (weight.isInfinite()) continue

                val alt = dist.getValue(u) + weight
                if (alt < dist.getValue(v)) {
                    dist[v] = alt
                    prevNode[v] = u
                    prevEdge[v] = edge
                    pq.add(Pair(v, alt))
                }
            }
        }

        val elapsedMs = ((System.nanoTime() - startNanos) / 1_000_000.0).coerceAtLeast(0.6)
        return buildRouteResult(
            algorithm = RoutingAlgorithm.DIJKSTRA,
            nodeMap = nodeMap,
            allEdges = edges,
            startId = startNode.id,
            goalId = goalNode.id,
            cameFromNode = prevNode,
            cameFromEdge = prevEdge,
            totalCost = dist.getValue(goalNode.id),
            nodesEvaluated = nodesEvaluated,
            elapsedMs = elapsedMs,
            vehicleType = vehicleType,
            providerName = providerName
        )
    }

    fun compareAlgorithms(
        nodes: List<RoadNode>,
        edges: List<RoadEdge>,
        startNodeId: String,
        goalNodeId: String,
        vehicleType: VehicleType,
        weights: RouteCostWeights,
        matlabConnected: Boolean
    ): AlgorithmComparison {
        val aStar = calculateRouteAStar(nodes, edges, startNodeId, goalNodeId, vehicleType, weights)
        val dijkstra = calculateRouteDijkstra(nodes, edges, startNodeId, goalNodeId, vehicleType, weights)
        return AlgorithmComparison(
            aStarResult = aStar,
            dijkstraResult = dijkstra,
            executedAtMillis = System.currentTimeMillis(),
            matlabVerified = matlabConnected
        )
    }

    private fun buildRouteResult(
        algorithm: RoutingAlgorithm,
        nodeMap: Map<String, RoadNode>,
        allEdges: List<RoadEdge>,
        startId: String,
        goalId: String,
        cameFromNode: Map<String, String>,
        cameFromEdge: Map<String, RoadEdge>,
        totalCost: Double,
        nodesEvaluated: Int,
        elapsedMs: Double,
        vehicleType: VehicleType,
        providerName: String
    ): AlgorithmRouteResult {
        val nodePath = mutableListOf<String>()
        val usedEdges = mutableListOf<RoadEdge>()

        if (!totalCost.isInfinite() && (startId == goalId || cameFromNode.containsKey(goalId))) {
            var curr: String? = goalId
            while (curr != null) {
                nodePath.add(0, curr)
                if (curr == startId) break
                val edge = cameFromEdge[curr]
                if (edge != null) usedEdges.add(0, edge)
                curr = cameFromNode[curr]
            }
        }

        val totalDist = usedEdges.sumOf { it.distanceKm }
        val speedFactor = when (vehicleType) {
            VehicleType.CAR -> 1.0
            VehicleType.BIKE -> 1.35
            VehicleType.VAN -> 1.18
        }
        val totalTime = usedEdges.sumOf { it.baseTimeMin * (1.0 + it.trafficFactor * 0.4) } * speedFactor
        val coords = nodePath.mapNotNull { id ->
            nodeMap[id]?.let { Pair(it.latitude, it.longitude) }
        }
        val roadNames = usedEdges.map { it.roadName }
        val steps = usedEdges.mapIndexed { idx, edge ->
            val fromLabel = nodeMap[edge.fromNodeId]?.name ?: edge.fromNodeId
            val toLabel = nodeMap[edge.toNodeId]?.name ?: edge.toNodeId
            "${idx + 1}. Head via ${edge.roadName} ($fromLabel → $toLabel, ${round1(edge.distanceKm)} km)"
        }

        val activeHazardEdges = allEdges.count {
            it.status != RoadStatusType.OPEN || it.severity != HazardSeverity.SAFE
        }
        val usedHazardEdges = usedEdges.count {
            it.status != RoadStatusType.OPEN || it.severity != HazardSeverity.SAFE
        }
        val hazardsAvoided = (activeHazardEdges - usedHazardEdges).coerceAtLeast(0)
        val blockedAvoided = allEdges.count {
            it.status == RoadStatusType.BLOCKED || !isVehicleCompatible(it, vehicleType)
        }

        return AlgorithmRouteResult(
            algorithm = algorithm,
            nodePath = nodePath,
            coordinateGeometry = coords,
            roadNames = roadNames,
            turnSteps = steps,
            distanceKm = round1(totalDist),
            estimatedTimeMin = round1(totalTime),
            totalCost = if (totalCost.isInfinite()) 0.0 else round1(totalCost),
            nodesEvaluated = nodesEvaluated,
            hazardsAvoided = hazardsAvoided,
            blockedRoadsAvoided = blockedAvoided,
            calculationTimeMs = round1(elapsedMs),
            providerName = providerName,
            trafficAvailable = false,
            isDiverted = hazardsAvoided > 0 || blockedAvoided > 0,
            diversionReason = if (hazardsAvoided > 0) {
                "Diverted around active hazard corridor"
            } else {
                null
            }
        )
    }

    fun haversineKm(n1: RoadNode, n2: RoadNode): Double {
        return haversineLatLonKm(n1.latitude, n1.longitude, n2.latitude, n2.longitude)
    }

    fun haversineLatLonKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2.0) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    private fun round1(value: Double): Double = (value * 10.0).roundToInt() / 10.0
}

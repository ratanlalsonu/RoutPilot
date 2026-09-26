package com.example.data.remote

import com.example.model.DataSource
import com.example.model.HardwareConfiguration
import com.example.model.HardwareConnectionType
import com.example.model.PlaceSearchResult
import com.example.model.SensorMode
import com.example.model.SensorPacket
import com.example.model.VehicleType
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

data class ExternalRouteResponse(
    val geometry: List<Pair<Double, Double>>,
    val distanceKm: Double,
    val durationMin: Double,
    val steps: List<String>,
    val roadNames: List<String>,
    val alternatives: List<List<Pair<Double, Double>>>,
    val providerName: String
)

interface RoutingProvider {
    suspend fun calculateRoute(
        sourceLat: Double,
        sourceLon: Double,
        destLat: Double,
        destLon: Double,
        vehicleType: VehicleType
    ): ExternalRouteResponse?

    suspend fun calculateAlternativeRoutes(
        sourceLat: Double,
        sourceLon: Double,
        destLat: Double,
        destLon: Double,
        vehicleType: VehicleType
    ): List<ExternalRouteResponse>
}

class RealNetworkProviders {
    val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .writeTimeout(6, TimeUnit.SECONDS)
        .build()

    private var activeEsp32WebSocket: WebSocket? = null
    private var activeWsUrl: String = ""

    val routingProvider: RoutingProvider = object : RoutingProvider {
        override suspend fun calculateRoute(
            sourceLat: Double,
            sourceLon: Double,
            destLat: Double,
            destLon: Double,
            vehicleType: VehicleType
        ): ExternalRouteResponse? = withContext(Dispatchers.IO) {
            fetchOsrmRoutes(sourceLat, sourceLon, destLat, destLon, vehicleType).firstOrNull()
        }

        override suspend fun calculateAlternativeRoutes(
            sourceLat: Double,
            sourceLon: Double,
            destLat: Double,
            destLon: Double,
            vehicleType: VehicleType
        ): List<ExternalRouteResponse> = withContext(Dispatchers.IO) {
            fetchOsrmRoutes(sourceLat, sourceLon, destLat, destLon, vehicleType)
        }
    }

    private fun fetchOsrmRoutes(
        sourceLat: Double,
        sourceLon: Double,
        destLat: Double,
        destLon: Double,
        vehicleType: VehicleType
    ): List<ExternalRouteResponse> {
        return try {
            val profile = vehicleType.osrmProfile
            val url = "https://router.project-osrm.org/route/v1/$profile/" +
                "$sourceLon,$sourceLat;$destLon,$destLat" +
                "?overview=full&geometries=geojson&steps=true&alternatives=true"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "RoutPilot-Android/2026.1")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return emptyList()
                val bodyStr = response.body?.string() ?: return emptyList()
                val json = JSONObject(bodyStr)
                val routesArr = json.optJSONArray("routes") ?: return emptyList()
                val results = mutableListOf<ExternalRouteResponse>()
                val allGeometries = mutableListOf<List<Pair<Double, Double>>>()

                for (i in 0 until routesArr.length()) {
                    val rObj = routesArr.getJSONObject(i)
                    val geomObj = rObj.optJSONObject("geometry")
                    val coordsArr = geomObj?.optJSONArray("coordinates")
                    val coords = mutableListOf<Pair<Double, Double>>()
                    if (coordsArr != null) {
                        for (j in 0 until coordsArr.length()) {
                            val pt = coordsArr.getJSONArray(j)
                            val lon = pt.getDouble(0)
                            val lat = pt.getDouble(1)
                            coords.add(Pair(lat, lon))
                        }
                    }
                    allGeometries.add(coords)
                }

                for (i in 0 until routesArr.length()) {
                    val rObj = routesArr.getJSONObject(i)
                    val distKm = ((rObj.optDouble("distance", 0.0) / 1000.0) * 10.0).roundToInt() / 10.0
                    val durMin = ((rObj.optDouble("duration", 0.0) / 60.0) * 10.0).roundToInt() / 10.0
                    val stepsList = mutableListOf<String>()
                    val roadsSet = linkedSetOf<String>()

                    val legsArr = rObj.optJSONArray("legs")
                    if (legsArr != null && legsArr.length() > 0) {
                        val stepsArr = legsArr.getJSONObject(0).optJSONArray("steps")
                        if (stepsArr != null) {
                            for (k in 0 until stepsArr.length()) {
                                val stepObj = stepsArr.getJSONObject(k)
                                val name = stepObj.optString("name", "").trim()
                                val stepDist = stepObj.optDouble("distance", 0.0)
                                val maneuver = stepObj.optJSONObject("maneuver")?.optString("type", "continue") ?: "continue"
                                if (name.isNotEmpty()) {
                                    roadsSet.add(name)
                                    stepsList.add("${stepsList.size + 1}. ${maneuver.replaceFirstChar { it.uppercase() }} on $name (${stepDist.roundToInt()} m)")
                                }
                            }
                        }
                    }

                    val altGeoms = allGeometries.filterIndexed { index, _ -> index != i }
                    results.add(
                        ExternalRouteResponse(
                            geometry = allGeometries.getOrElse(i) { emptyList() },
                            distanceKm = distKm,
                            durationMin = durMin,
                            steps = stepsList,
                            roadNames = roadsSet.toList(),
                            alternatives = altGeoms,
                            providerName = "OSRM Live Road Network"
                        )
                    )
                }
                results
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Real Geocoding / Place Search via FastAPI `/api/geocode/search` with automatic fallback
     * to OpenStreetMap Nominatim (`https://nominatim.openstreetmap.org/search`).
     */
    suspend fun searchPlaces(query: String, fastApiBaseUrl: String): List<PlaceSearchResult> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val encoded = URLEncoder.encode(query.trim(), "UTF-8")

        // 1. Try FastAPI backend first if reachable
        try {
            val fastUrl = "${fastApiBaseUrl.trimEnd('/')}/api/geocode/search?q=$encoded"
            val req = Request.Builder()
                .url(fastUrl)
                .header("User-Agent", "RoutPilot-Android/2026.1")
                .get()
                .build()
            httpClient.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string()
                    if (!body.isNullOrBlank()) {
                        return@withContext parseNominatimArray(JSONArray(body), "FastAPI + Nominatim")
                    }
                }
            }
        } catch (_: Exception) {
            // Fall through to direct OpenStreetMap Nominatim query
        }

        // 2. Direct OpenStreetMap Nominatim query
        try {
            val nomUrl = "https://nominatim.openstreetmap.org/search?format=json&q=$encoded&limit=6&addressdetails=1"
            val req = Request.Builder()
                .url(nomUrl)
                .header("User-Agent", "RoutPilot-Android/2026.1")
                .header("Accept-Language", "en")
                .get()
                .build()
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext emptyList()
                val body = resp.body?.string() ?: return@withContext emptyList()
                parseNominatimArray(JSONArray(body), "OpenStreetMap Nominatim")
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseNominatimArray(arr: JSONArray, providerLabel: String): List<PlaceSearchResult> {
        val list = mutableListOf<PlaceSearchResult>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val display = obj.optString("display_name", "")
            val lat = obj.optString("lat", "").toDoubleOrNull() ?: continue
            val lon = obj.optString("lon", "").toDoubleOrNull() ?: continue
            val type = obj.optString("type", "location").replace("_", " ").replaceFirstChar { it.uppercase() }
            val shortName = display.substringBefore(",").ifBlank { display.take(32) }
            list.add(
                PlaceSearchResult(
                    displayName = display,
                    shortName = shortName,
                    category = type,
                    latitude = lat,
                    longitude = lon,
                    mappedNodeId = null,
                    sourceProvider = providerLabel
                )
            )
        }
        return list
    }

    /**
     * Polls a real ESP32 hardware endpoint (or FastAPI `/api/sensors/{deviceId}`) for live JSON:
     * { "deviceId": "ESP32-001", "timestamp": "ISO-8601", "latitude": 0, "longitude": 0,
     *   "vibration": 0, "tilt": 0, "strain": 0, "displacement": 0, "waterLevel": 0, "battery": 0 }
     * Never invents or fabricates values if the endpoint is blank or unreachable.
     */
    suspend fun pollRealEsp32Packet(endpointUrl: String, expectedDeviceId: String = ""): SensorPacket? = withContext(Dispatchers.IO) {
        val cleanUrl = endpointUrl.trim()
        if (cleanUrl.isBlank()) return@withContext null
        try {
            val req = Request.Builder()
                .url(cleanUrl)
                .header("Accept", "application/json")
                .get()
                .build()
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext null
                val rawJson = resp.body?.string() ?: return@withContext null
                val packet = parseEsp32JsonPacket(rawJson) ?: return@withContext null
                if (expectedDeviceId.isNotBlank() && !packet.deviceId.equals(expectedDeviceId.trim(), ignoreCase = true)) {
                    return@withContext null
                }
                packet
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Connects to a real ESP32 WebSocket endpoint (ESP32_WS_ENDPOINT) ONLY when External Hardware Mode
     * is active and a WebSocket endpoint is configured.
     */
    fun connectEsp32WebSocket(
        wsEndpoint: String,
        expectedDeviceId: String,
        onPacketReceived: (SensorPacket) -> Unit,
        onDisconnectedOrError: () -> Unit
    ) {
        val cleanWs = wsEndpoint.trim()
        if (cleanWs.isBlank()) {
            disconnectEsp32WebSocket()
            return
        }
        if (activeEsp32WebSocket != null && activeWsUrl == cleanWs) {
            return
        }
        disconnectEsp32WebSocket()
        try {
            val request = Request.Builder()
                .url(cleanWs)
                .build()
            activeWsUrl = cleanWs
            activeEsp32WebSocket = httpClient.newWebSocket(
                request,
                object : WebSocketListener() {
                    override fun onMessage(webSocket: WebSocket, text: String) {
                        val packet = parseEsp32JsonPacket(text) ?: return
                        if (expectedDeviceId.isBlank() || packet.deviceId.equals(expectedDeviceId.trim(), ignoreCase = true)) {
                            onPacketReceived(packet)
                        }
                    }

                    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                        activeEsp32WebSocket = null
                        activeWsUrl = ""
                        onDisconnectedOrError()
                    }

                    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                        activeEsp32WebSocket = null
                        activeWsUrl = ""
                        onDisconnectedOrError()
                    }
                }
            )
        } catch (_: Exception) {
            activeEsp32WebSocket = null
            activeWsUrl = ""
            onDisconnectedOrError()
        }
    }

    fun disconnectEsp32WebSocket() {
        try {
            activeEsp32WebSocket?.close(1000, "Mode switched or disconnected")
        } catch (_: Exception) {
        } finally {
            activeEsp32WebSocket = null
            activeWsUrl = ""
        }
    }

    /**
     * Performs the 5-step Hardware Test Connection required by Settings > Hardware Configuration:
     * 1. Connect to ESP32 (HTTP / WebSocket / FastAPI MQTT bridge).
     * 2. Check response.
     * 3. Verify device ID.
     * 4. Check sensor data.
     * 5. Return verified packet or null on failure (never fabricates data).
     */
    suspend fun testEsp32HardwareConnection(
        config: HardwareConfiguration,
        fastApiBaseUrl: String
    ): Pair<SensorPacket?, String> = withContext(Dispatchers.IO) {
        if (!config.isConfigured && config.connectionType != HardwareConnectionType.MQTT_FASTAPI) {
            return@withContext Pair(null, "ESP32 endpoint not configured.")
        }

        // 1. Try HTTP Endpoint if configured and selected
        if (config.httpEndpoint.isNotBlank() &&
            config.connectionType in setOf(HardwareConnectionType.HTTP, HardwareConnectionType.HTTP_AND_WS)
        ) {
            val packet = pollRealEsp32Packet(config.httpEndpoint, config.deviceId)
            if (packet != null) {
                return@withContext Pair(
                    packet,
                    "Verified ${packet.deviceId} via HTTP (${config.httpEndpoint})"
                )
            }
        }

        // 2. Try FastAPI MQTT/Sensor Bridge if MQTT_FASTAPI or fallback
        if (config.connectionType == HardwareConnectionType.MQTT_FASTAPI || config.httpEndpoint.isNotBlank()) {
            val bridgeUrl = "${fastApiBaseUrl.trimEnd('/')}/api/sensors/${config.deviceId.trim().ifBlank { "ESP32-001" }}"
            val bridgePacket = pollRealEsp32Packet(bridgeUrl, config.deviceId)
            if (bridgePacket != null) {
                return@withContext Pair(
                    bridgePacket,
                    "Verified ${bridgePacket.deviceId} via FastAPI MQTT Bridge"
                )
            }
        }

        // 3. Try WebSocket Endpoint if configured
        if (config.wsEndpoint.isNotBlank() &&
            config.connectionType in setOf(HardwareConnectionType.WEBSOCKET, HardwareConnectionType.HTTP_AND_WS)
        ) {
            val deferred = CompletableDeferred<SensorPacket?>()
            var tempWs: WebSocket? = null
            try {
                val req = Request.Builder().url(config.wsEndpoint.trim()).build()
                tempWs = httpClient.newWebSocket(
                    req,
                    object : WebSocketListener() {
                        override fun onMessage(webSocket: WebSocket, text: String) {
                            val p = parseEsp32JsonPacket(text)
                            if (p != null && (config.deviceId.isBlank() || p.deviceId.equals(config.deviceId.trim(), ignoreCase = true))) {
                                if (!deferred.isCompleted) deferred.complete(p)
                            }
                        }

                        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                            if (!deferred.isCompleted) deferred.complete(null)
                        }
                    }
                )
                val wsPacket = withTimeoutOrNull(4500L) { deferred.await() }
                tempWs.close(1000, "Test complete")
                if (wsPacket != null) {
                    return@withContext Pair(
                        wsPacket,
                        "Verified ${wsPacket.deviceId} via WebSocket (${config.wsEndpoint})"
                    )
                }
            } catch (_: Exception) {
                try {
                    tempWs?.close(1000, "Error")
                } catch (_: Exception) {
                }
            }
        }

        Pair(null, "No response from ESP32 (${config.deviceId}). Ensure ESP32 is powered on and reachable.")
    }

    fun parseEsp32JsonPacket(rawJson: String): SensorPacket? {
        return try {
            val trimmed = rawJson.trim()
            val obj = if (trimmed.startsWith("{")) {
                JSONObject(trimmed)
            } else {
                return null
            }
            // Support direct ESP32 payload or FastAPI /api/sensors/{deviceId} wrapper
            val sourceObj = obj.optJSONObject("latestReading") ?: obj
            val deviceId = sourceObj.optString("deviceId", obj.optString("deviceId", "")).trim()
            if (deviceId.isBlank()) return null

            if (!sourceObj.has("vibration") || !sourceObj.has("tilt") || !sourceObj.has("strain")) {
                return null
            }

            val tsIso = sourceObj.optString("timestamp", "").ifBlank {
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }.format(Date())
            }
            val lat = sourceObj.optDouble("latitude", 0.0)
            val lon = sourceObj.optDouble("longitude", 0.0)
            val vib = sourceObj.getDouble("vibration")
            val tilt = sourceObj.getDouble("tilt")
            val strain = sourceObj.getDouble("strain")
            val disp = sourceObj.optDouble("displacement", 0.0)
            val water = sourceObj.optDouble("waterLevel", 0.0)
            val battery = sourceObj.optDouble("battery", 100.0)

            SensorPacket(
                deviceId = deviceId,
                timestampIso = tsIso,
                epochMillis = System.currentTimeMillis(),
                latitude = lat,
                longitude = lon,
                vibration = vib,
                tilt = tilt,
                strain = strain,
                displacement = disp,
                waterLevel = water,
                roadConstructionActive = sourceObj.optBoolean("roadConstruction", false),
                battery = battery,
                dataSource = DataSource.LIVE_HARDWARE
            )
        } catch (_: Exception) {
            null
        }
    }

    suspend fun checkFastApiAndMatlabHealth(baseUrl: String): Pair<Boolean, Boolean> = withContext(Dispatchers.IO) {
        var fastApiOk = false
        var matlabOk = false
        try {
            val healthReq = Request.Builder()
                .url("${baseUrl.trimEnd('/')}/api/health")
                .get()
                .build()
            httpClient.newCall(healthReq).execute().use { resp ->
                fastApiOk = resp.isSuccessful
            }
            if (fastApiOk) {
                val matlabReq = Request.Builder()
                    .url("${baseUrl.trimEnd('/')}/api/matlab/status")
                    .get()
                    .build()
                httpClient.newCall(matlabReq).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val body = resp.body?.string().orEmpty()
                        matlabOk = JSONObject(body).optBoolean("connected", false)
                    }
                }
            }
        } catch (_: Exception) {
            fastApiOk = false
            matlabOk = false
        }
        Pair(fastApiOk, matlabOk)
    }

    suspend fun syncSensorModeToBackend(baseUrl: String, mode: SensorMode): Boolean = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().put("mode", mode.apiValue).toString()
            val req = Request.Builder()
                .url("${baseUrl.trimEnd('/')}/api/sensor-mode")
                .post(payload.toRequestBody("application/json".toMediaType()))
                .build()
            httpClient.newCall(req).execute().use { it.isSuccessful }
        } catch (_: Exception) {
            false
        }
    }
}


package com.example.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.model.GpsTelemetry
import com.example.model.SensorMode
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class RealLocationTracker(private val context: Context) {

    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    private val locationManager: LocationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private val _gpsState = MutableStateFlow(GpsTelemetry())
    val gpsState: StateFlow<GpsTelemetry> = _gpsState.asStateFlow()

    private var isTracking = false

    private val fusedCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val loc = result.lastLocation ?: return
            updateFromAndroidLocation(loc, "Android Fused Location Provider")
        }
    }

    private val fallbackListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            updateFromAndroidLocation(location, "Android GPS / Network Provider")
        }
    }

    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    @SuppressLint("MissingPermission")
    fun startTracking() {
        if (!hasLocationPermission()) {
            _gpsState.value = GpsTelemetry(
                isAvailable = false,
                hasPermission = false,
                providerLabel = "GPS UNAVAILABLE (Permission Required)"
            )
            return
        }

        _gpsState.value = _gpsState.value.copy(hasPermission = true)
        if (isTracking) return
        isTracking = true

        try {
            fusedClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) {
                    updateFromAndroidLocation(loc, "Android Fused Location Provider")
                }
            }

            val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000L)
                .setMinUpdateIntervalMillis(1500L)
                .build()
            fusedClient.requestLocationUpdates(request, fusedCallback, Looper.getMainLooper())
        } catch (_: Exception) {
            // Fallback to LocationManager if Play Services is unavailable
        }

        try {
            val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            for (provider in providers) {
                if (locationManager.isProviderEnabled(provider)) {
                    val lastKnown = locationManager.getLastKnownLocation(provider)
                    if (lastKnown != null) {
                        updateFromAndroidLocation(lastKnown, "Android $provider")
                    }
                    locationManager.requestLocationUpdates(
                        provider,
                        3000L,
                        2f,
                        fallbackListener,
                        Looper.getMainLooper()
                    )
                }
            }
        } catch (_: Exception) {
            // Ignore if provider not present on device
        }
    }

    fun stopTracking() {
        if (!isTracking) return
        isTracking = false
        try {
            fusedClient.removeLocationUpdates(fusedCallback)
            locationManager.removeUpdates(fallbackListener)
        } catch (_: Exception) {
        }
    }

    private fun updateFromAndroidLocation(loc: Location, providerName: String) {
        val speedKmh = if (loc.hasSpeed()) loc.speed * 3.6f else 0.0f
        val heading = if (loc.hasBearing()) loc.bearing else 0.0f
        val accuracy = if (loc.hasAccuracy()) loc.accuracy else null
        _gpsState.value = GpsTelemetry(
            isAvailable = true,
            hasPermission = true,
            latitude = loc.latitude,
            longitude = loc.longitude,
            accuracyMeters = accuracy,
            speedKmh = speedKmh,
            headingDegrees = heading,
            providerLabel = providerName,
            lastUpdatedMillis = System.currentTimeMillis()
        )
    }

    fun isInternetAvailable(): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (_: Exception) {
            true
        }
    }
}

object RoutPilotNotificationHelper {
    private const val CHANNEL_ID = "routpilot_hazard_alerts"

    fun sendHazardNotification(
        context: Context,
        sensorMode: SensorMode,
        hazardTitle: String,
        roadReference: String,
        severityLabel: String
    ) {
        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "RoutPilot Hazard & Diversion Alerts",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Distinguishes Live Hardware vs Virtual Test road hazard alerts"
                }
                nm.createNotificationChannel(channel)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val granted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
                if (!granted) return
            }

            // Strictly enforce the notification rule:
            // Hardware Mode -> "Live road hazard detected."
            // Virtual Mode -> "Virtual test hazard detected."
            val headline = when (sensorMode) {
                SensorMode.EXTERNAL_HARDWARE -> "Live road hazard detected."
                SensorMode.VIRTUAL -> "Virtual test hazard detected."
            }
            val sourceTag = when (sensorMode) {
                SensorMode.EXTERNAL_HARDWARE -> "[EXTERNAL HARDWARE — LIVE]"
                SensorMode.VIRTUAL -> "[VIRTUAL MODE — TEST DATA]"
            }

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle(headline)
                .setContentText("$sourceTag $hazardTitle on $roadReference ($severityLabel)")
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText("$headline\n$sourceTag\nHazard: $hazardTitle ($severityLabel)\nLocation: $roadReference")
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()

            nm.notify((System.currentTimeMillis() % 10000).toInt(), notification)
        } catch (_: Exception) {
        }
    }
}

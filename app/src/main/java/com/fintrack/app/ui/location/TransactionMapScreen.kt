package com.fintrack.app.ui.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Handler
import android.os.Looper
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

@Composable
fun TransactionMapScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var location by remember { mutableStateOf<Location?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun findLocation() {
        loading = true
        error = null
        requestCurrentLocation(context) { result ->
            loading = false
            result.onSuccess { location = it }
                .onFailure { error = it.message ?: "Could not get your location." }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.any { it }) {
            findLocation()
        } else {
            error = "Location permission is needed to show the map."
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Nearby map", style = MaterialTheme.typography.headlineMedium)
        Text(
            "View your current position on OpenStreetMap. FinTrack does not save it; the map provider receives your coordinates to display the map.",
            style = MaterialTheme.typography.bodyMedium
        )
        Button(
            onClick = {
                val hasLocationPermission =
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED ||
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                if (hasLocationPermission) {
                    findLocation()
                } else {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        )
                    )
                }
            },
            enabled = !loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (loading) "Finding location…" else "Find my location")
        }
        if (loading) CircularProgressIndicator()
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        location?.let { currentLocation ->
            Text(
                String.format(
                    Locale.US,
                    "Current position: %.5f, %.5f",
                    currentLocation.latitude,
                    currentLocation.longitude
                ),
                style = MaterialTheme.typography.bodySmall
            )
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(500.dp),
                factory = { viewContext ->
                    WebView(viewContext).apply {
                        settings.javaScriptEnabled = false
                        settings.domStorageEnabled = false
                        webViewClient = WebViewClient()
                    }
                },
                update = { webView ->
                    webView.loadUrl(openStreetMapUrl(currentLocation.latitude, currentLocation.longitude))
                }
            )
        }
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Back to transactions")
        }
    }
}

@SuppressLint("MissingPermission")
private fun requestCurrentLocation(
    context: Context,
    onResult: (Result<Location>) -> Unit
) {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val provider = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)
        .firstOrNull { locationManager.isProviderEnabled(it) }
    if (provider == null) {
        onResult(Result.failure(IllegalStateException("Turn on location services and try again.")))
        return
    }

    val completed = AtomicBoolean(false)
    val handler = Handler(Looper.getMainLooper())
    lateinit var listener: LocationListener
    fun finish(result: Result<Location>) {
        if (completed.compareAndSet(false, true)) {
            handler.removeCallbacksAndMessages(null)
            runCatching { locationManager.removeUpdates(listener) }
            onResult(result)
        }
    }

    listener = LocationListener { location -> finish(Result.success(location)) }
    try {
        locationManager.requestLocationUpdates(provider, 0L, 0f, listener, Looper.getMainLooper())
        handler.postDelayed(
            {
                finish(
                    Result.failure(
                        IllegalStateException("Location is unavailable. Check GPS and try again.")
                    )
                )
            },
            LOCATION_TIMEOUT_MILLIS
        )
        locationManager.getLastKnownLocation(provider)?.let { finish(Result.success(it)) }
    } catch (_: SecurityException) {
        finish(Result.failure(IllegalStateException("Location permission was not granted.")))
    } catch (_: IllegalArgumentException) {
        finish(Result.failure(IllegalStateException("Location provider is unavailable.")))
    }
}

private fun openStreetMapUrl(latitude: Double, longitude: Double): String {
    val offset = 0.01
    val bounds = listOf(
        longitude - offset,
        latitude - offset,
        longitude + offset,
        latitude + offset
    ).joinToString(",") { String.format(Locale.US, "%.6f", it) }
    val marker = "${String.format(Locale.US, "%.6f", latitude)}%2C${String.format(Locale.US, "%.6f", longitude)}"
    return "https://www.openstreetmap.org/export/embed.html?bbox=$bounds&layer=mapnik&marker=$marker"
}

private const val LOCATION_TIMEOUT_MILLIS = 15_000L

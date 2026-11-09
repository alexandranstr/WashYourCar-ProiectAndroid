package com.example.washyourcar.ui.screens

import android.content.Context
import android.location.Geocoder
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.util.MapTileIndex
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import java.util.Locale

/** Result of picking a spot: coordinates plus a best-effort street address and city. */
data class PickedLocation(val latitude: Double, val longitude: Double, val address: String?, val city: String?)

private val DefaultCenter = GeoPoint(45.6427, 25.5887) // Brașov

/**
 * Esri World Street Map: free street tiles, no API key.
 * (tile.openstreetmap.org returns "403 Access blocked" for this app, so its tiles can't be used.)
 * Esri's URLs are .../tile/{z}/{y}/{x}, hence the custom URL builder.
 */
private val EsriStreetMap = object : OnlineTileSourceBase(
    "EsriWorldStreetMap", 0, 19, 256, "",
    arrayOf("https://server.arcgisonline.com/ArcGIS/rest/services/World_Street_Map/MapServer/tile/"),
    "Tiles © Esri — Sources: Esri, HERE, Garmin, OpenStreetMap contributors"
) {
    override fun getTileURLString(pMapTileIndex: Long): String =
        baseUrl + MapTileIndex.getZoom(pMapTileIndex) + "/" +
            MapTileIndex.getY(pMapTileIndex) + "/" + MapTileIndex.getX(pMapTileIndex)
}

/**
 * Full-screen map (free, no API key): tap to drop the pin on the car wash, then confirm.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapPickerDialog(
    initialLatitude: Double?,
    initialLongitude: Double?,
    onDismiss: () -> Unit,
    onPicked: (PickedLocation) -> Unit
) {
    val context = LocalContext.current
    val initial = if (initialLatitude != null && initialLongitude != null) GeoPoint(initialLatitude, initialLongitude) else null
    var pin by remember { mutableStateOf(initial) }
    var resolving by remember { mutableStateOf(false) }

    // Tile servers want an identifying User-Agent.
    remember {
        Configuration.getInstance().apply {
            load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
            userAgentValue = "WashYourCar/1.0 (Android)"
        }
    }

    val mapView = remember {
        MapView(context).apply {
            setTileSource(EsriStreetMap)
            setMultiTouchControls(true)
            controller.setZoom(if (initial != null) 17.0 else 13.0)
            controller.setCenter(initial ?: DefaultCenter)
        }
    }
    val marker = remember { Marker(mapView).apply { setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM); title = "Car wash" } }

    // Tap on the map moves the pin.
    DisposableEffect(mapView) {
        val tapOverlay = MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean { pin = p; return true }
            override fun longPressHelper(p: GeoPoint): Boolean { pin = p; return true }
        })
        mapView.overlays.add(0, tapOverlay)
        onDispose { mapView.overlays.remove(tapOverlay) }
    }

    // MapView must follow the screen lifecycle (tile loading, cache).
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> {}
            }
        }
        lifecycle.addObserver(observer)
        mapView.onResume()
        onDispose {
            lifecycle.removeObserver(observer)
            mapView.onPause()
            mapView.onDetach()
        }
    }

    // Reverse-geocode after the user confirms (runs off the main thread).
    var confirmRequested by remember { mutableStateOf(false) }
    LaunchedEffect(confirmRequested) {
        val p = pin
        if (!confirmRequested || p == null) return@LaunchedEffect
        resolving = true
        val (address, city) = reverseGeocode(context, p)
        resolving = false
        confirmRequested = false
        onPicked(PickedLocation(p.latitude, p.longitude, address, city))
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Place your car wash") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                    }
                )
            },
            bottomBar = {
                Surface(tonalElevation = 3.dp) {
                    Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            pin?.let { "Pin at %.5f, %.5f".format(Locale.US, it.latitude, it.longitude) }
                                ?: "Tap the map where your car wash is.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text("Tiles © Esri — Esri, HERE, Garmin, OpenStreetMap contributors", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Button(
                            onClick = { confirmRequested = true },
                            enabled = pin != null && !resolving,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (resolving) {
                                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                            }
                            Text("Use this location")
                        }
                    }
                }
            }
        ) { padding ->
            AndroidView(
                factory = { mapView },
                modifier = Modifier.fillMaxSize().padding(padding),
                update = { view ->
                    val p = pin
                    if (p == null) {
                        view.overlays.remove(marker)
                    } else {
                        marker.position = p
                        if (marker !in view.overlays) view.overlays.add(marker)
                    }
                    view.invalidate()
                }
            )
        }
    }
}

@Suppress("DEPRECATION") // The callback API needs API 33; the blocking one is fine off the main thread.
private suspend fun reverseGeocode(context: Context, point: GeoPoint): Pair<String?, String?> = withContext(Dispatchers.IO) {
    try {
        val result = Geocoder(context, Locale.getDefault()).getFromLocation(point.latitude, point.longitude, 1)?.firstOrNull()
        val street = listOfNotNull(result?.thoroughfare, result?.subThoroughfare).joinToString(" ").ifBlank { null }
        street to (result?.locality ?: result?.subAdminArea)
    } catch (_: Exception) {
        null to null
    }
}

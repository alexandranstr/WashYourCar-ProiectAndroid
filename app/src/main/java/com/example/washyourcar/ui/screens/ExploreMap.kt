package com.example.washyourcar.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.GradientDrawable
import android.location.Location
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.washyourcar.data.entities.WashType
import com.example.washyourcar.models.BayStatus
import com.example.washyourcar.models.WashListItem
import com.example.washyourcar.models.WashMatcher
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

/** Client position, or null while unknown / permission denied. */
data class UserPosition(val latitude: Double, val longitude: Double)

/**
 * Asks for location permission once and returns the current position (Android's built-in location
 * service, no Google library or key). [refresh] re-reads it, e.g. from the "my location" button.
 */
@Composable
fun rememberUserPosition(): Pair<UserPosition?, () -> Unit> {
    val context = LocalContext.current
    var position by remember { mutableStateOf<UserPosition?>(null) }
    var requestCount by remember { mutableIntStateOf(0) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        if (granted.values.any { it }) requestCount++
    }
    LaunchedEffect(Unit) {
        if (hasLocationPermission(context)) requestCount++
        else launcher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }
    LaunchedEffect(requestCount) {
        if (requestCount > 0 && hasLocationPermission(context)) {
            readLocation(context) { loc -> if (loc != null) position = UserPosition(loc.latitude, loc.longitude) }
        }
    }
    return position to { requestCount++ }
}

private fun hasLocationPermission(context: Context) =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

@SuppressLint("MissingPermission") // checked by hasLocationPermission() before calling
private fun readLocation(context: Context, onResult: (Location?) -> Unit) {
    val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER).filter { lm.isProviderEnabled(it) }
    // Show the last known fix immediately, then refine with a fresh one.
    providers.mapNotNull { lm.getLastKnownLocation(it) }.maxByOrNull { it.time }?.let(onResult)
    providers.firstOrNull()?.let { provider ->
        LocationManagerCompat.getCurrentLocation(
            lm, provider, null as androidx.core.os.CancellationSignal?, ContextCompat.getMainExecutor(context),
            androidx.core.util.Consumer<Location?> { loc -> if (loc != null) onResult(loc) }
        )
    }
}

fun distanceKm(from: UserPosition?, wash: WashListItem): Double? {
    if (from == null || wash.latitude == null || wash.longitude == null) return null
    val out = FloatArray(1)
    Location.distanceBetween(from.latitude, from.longitude, wash.latitude, wash.longitude, out)
    return out[0] / 1000.0
}

/** Pin color: green = free now, amber = short wait, red = long wait, purple = classic (booked by appointment). */
private fun pinColor(wash: WashListItem, bays: List<BayStatus>): Int {
    if (wash.type == WashType.NORMAL || bays.isEmpty()) return 0xFF7B1FA2.toInt()
    val wait = bays.minOf { it.waitMinutes }
    return when {
        bays.any { it.isFree } -> 0xFF2E7D32.toInt()
        wait <= 15 -> 0xFFF9A825.toInt()
        else -> 0xFFC62828.toInt()
    }
}

private fun dot(color: Int, sizePx: Int, strokeColor: Int = 0xFFFFFFFF.toInt()) = GradientDrawable().apply {
    shape = GradientDrawable.OVAL
    setColor(color)
    setStroke(sizePx / 6, strokeColor)
    setSize(sizePx, sizePx)
}

/**
 * Client Explore screen: map with every wash placed on the app map, the client's position, a pull-up
 * list sorted by distance, and the prominent "Find my wash" button.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreMapScreen(
    washes: List<WashListItem>,
    bayStatusesFor: (WashListItem) -> List<BayStatus>,
    ratings: Map<String, Pair<Float, Int>>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    selectedTypes: Set<WashType>,
    onToggleType: (WashType) -> Unit,
    position: UserPosition?,
    onRefreshPosition: () -> Unit,
    highlighted: List<WashListItem>,
    onWashClick: (WashListItem) -> Unit,
    onFindMyWash: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val mapped = washes.filter { it.latitude != null && it.longitude != null }
    val sorted = remember(mapped, position) { mapped.sortedBy { distanceKm(position, it) ?: Double.MAX_VALUE } }
    var selected by remember { mutableStateOf<WashListItem?>(null) }
    val density = context.resources.displayMetrics.density

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
            zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER)
            controller.setZoom(13.0)
            controller.setCenter(DefaultCenter)
        }
    }

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, mapView) {
        val observer = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_RESUME) mapView.onResume()
            if (e == Lifecycle.Event.ON_PAUSE) mapView.onPause()
        }
        lifecycle.addObserver(observer)
        mapView.onResume()
        onDispose { lifecycle.removeObserver(observer); mapView.onPause(); mapView.onDetach() }
    }

    // Recenter on the client when the position first arrives or they actually moved (>500 m),
    // e.g. a stale last-known fix replaced by the real one.
    var centeredOn by remember { mutableStateOf<UserPosition?>(null) }
    LaunchedEffect(position) {
        val p = position ?: return@LaunchedEffect
        val last = centeredOn
        val moved = last == null || FloatArray(1).also {
            Location.distanceBetween(last.latitude, last.longitude, p.latitude, p.longitude, it)
        }[0] > 500f
        if (moved) {
            mapView.controller.animateTo(GeoPoint(p.latitude, p.longitude), 14.0, 800L)
            centeredOn = p
        }
    }
    // Zoom to the recommended washes when results arrive.
    LaunchedEffect(highlighted) {
        highlighted.firstOrNull()?.let { mapView.controller.animateTo(GeoPoint(it.latitude!!, it.longitude!!), 15.0, 800L) }
    }

    // Redraw pins + "you are here" whenever any of their inputs change.
    val pinStates = mapped.map { it.key to pinColor(it, bayStatusesFor(it)) }
    val highlightedKeys = highlighted.map { it.key }.toSet()
    LaunchedEffect(pinStates, position, highlightedKeys) {
        mapView.overlays.removeAll { it is Marker }
        mapped.forEach { wash ->
            val isHighlighted = wash.key in highlightedKeys
            mapView.overlays.add(Marker(mapView).apply {
                this.position = GeoPoint(wash.latitude!!, wash.longitude!!)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                icon = dot(
                    pinColor(wash, bayStatusesFor(wash)), ((if (isHighlighted) 34 else 24) * density).toInt(),
                    if (isHighlighted) 0xFFFFC94D.toInt() else 0xFFFFFFFF.toInt()
                )
                title = wash.name
                setOnMarkerClickListener { _, _ -> selected = wash; true }
            })
        }
        position?.let { p ->
            mapView.overlays.add(Marker(mapView).apply {
                this.position = GeoPoint(p.latitude, p.longitude)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                icon = dot(0xFF1A73E8.toInt(), (18 * density).toInt())
                title = "You are here"
                setInfoWindow(null)
            })
        }
        mapView.invalidate()
    }

    var showList by remember { mutableStateOf(false) }
    if (showList) {
        ModalBottomSheet(onDismissRequest = { showList = false }) {
            NearbySheet(
                sorted, position, ratings, bayStatusesFor, searchQuery, onSearchChange, selectedTypes, onToggleType,
                onWashClick = { wash ->
                    showList = false
                    selected = wash
                    mapView.controller.animateTo(GeoPoint(wash.latitude!!, wash.longitude!!), 16.0, 600L)
                },
                totalWashes = washes.size
            )
        }
    }

    Box(modifier) {
        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())

        // Selected wash: quick card on top of the map.
        selected?.let { wash ->
            SelectedWashCard(
                wash = wash,
                distance = distanceKm(position, wash),
                bays = bayStatusesFor(wash),
                rating = ratings[wash.key],
                onOpen = { onWashClick(wash) },
                onClose = { selected = null },
                modifier = Modifier.align(Alignment.TopCenter).padding(12.dp)
            )
        }

        Text(
            "Tiles © Esri",
            fontSize = 9.sp,
            color = Color.DarkGray,
            modifier = Modifier.align(Alignment.TopStart).padding(6.dp)
        )

        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // "Near you" list, sorted by distance.
                ExtendedFloatingActionButton(
                    onClick = { showList = true },
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    icon = { Icon(Icons.Default.Search, null) },
                    text = { Text(if (position != null) "Near you (${sorted.size})" else "All washes (${sorted.size})") }
                )
                Spacer(Modifier.weight(1f))
                SmallFloatingActionButton(
                    onClick = {
                        onRefreshPosition()
                        position?.let { mapView.controller.animateTo(GeoPoint(it.latitude, it.longitude), 15.0, 600L) }
                    },
                    containerColor = MaterialTheme.colorScheme.surface
                ) { Icon(Icons.Default.MyLocation, contentDescription = "My location", tint = MaterialTheme.colorScheme.primary) }
            }

            // Prominent blue "Find my wash" button at the bottom of Home.
            Button(
                onClick = onFindMyWash,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E5BB8), contentColor = Color.White),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .height(56.dp)
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null)
                Spacer(Modifier.width(10.dp))
                Text("Find my wash", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SelectedWashCard(
    wash: WashListItem,
    distance: Double?,
    bays: List<BayStatus>,
    rating: Pair<Float, Int>?,
    onOpen: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(wash.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        listOfNotNull(wash.type.label, distance?.let { WashMatcher.formatDistance(it) + " away" },
                            rating?.let { "%.1f★ (%d)".format(it.first, it.second) }).joinToString(" · "),
                        fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                NavigateToWashButton(wash.latitude, wash.longitude, "${wash.address}, ${wash.city}")
                IconButton(onClick = onClose) { Icon(Icons.Default.Close, contentDescription = "Close") }
            }
            AvailabilityLine(bays)
            FilledTonalButton(onClick = onOpen, modifier = Modifier.fillMaxWidth()) { Text("View & book") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NearbySheet(
    washes: List<WashListItem>,
    position: UserPosition?,
    ratings: Map<String, Pair<Float, Int>>,
    bayStatusesFor: (WashListItem) -> List<BayStatus>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    selectedTypes: Set<WashType>,
    onToggleType: (WashType) -> Unit,
    onWashClick: (WashListItem) -> Unit,
    totalWashes: Int
) {
    Column(Modifier.fillMaxWidth().fillMaxHeight(0.75f).padding(horizontal = 16.dp)) {
        Text(
            if (position != null) "Near you · ${washes.size} car washes" else "${washes.size} car washes on the map",
            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold
        )
        if (totalWashes > washes.size) {
            Text("${totalWashes - washes.size} not shown: not placed on the map yet", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Search car wash…") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 8.dp)) {
            items(WashType.entries) { type ->
                FilterChip(selected = type in selectedTypes, onClick = { onToggleType(type) }, label = { Text(type.label) })
            }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
            items(washes, key = { it.key }) { wash ->
                NearbyRow(wash, distanceKm(position, wash), ratings[wash.key], bayStatusesFor(wash)) { onWashClick(wash) }
            }
        }
    }
}

@Composable
private fun NearbyRow(wash: WashListItem, distance: Double?, rating: Pair<Float, Int>?, bays: List<BayStatus>, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(wash.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${wash.type.label} · ${wash.address}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (bays.isNotEmpty()) AvailabilityLine(bays)
            }
            Column(horizontalAlignment = Alignment.End) {
                distance?.let { Text(WashMatcher.formatDistance(it), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) }
                rating?.let {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, null, tint = Color(0xFFFFB300), modifier = Modifier.size(14.dp))
                        Text("%.1f".format(it.first), fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

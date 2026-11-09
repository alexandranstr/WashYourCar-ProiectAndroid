package com.example.washyourcar.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.washyourcar.data.entities.AppointmentWithDetails
import com.example.washyourcar.models.BayAvailability
import com.example.washyourcar.models.BayKind
import com.example.washyourcar.models.BayQueueSimulator
import com.example.washyourcar.models.BayStatus
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Re-runs [onTick] every [periodMillis] while on screen (queues are simulated per 5-minute window). */
@Composable
fun RefreshEvery(periodMillis: Long = 30_000L, onTick: () -> Unit) {
    LaunchedEffect(Unit) {
        while (true) {
            delay(periodMillis)
            onTick()
        }
    }
}

fun availabilityText(a: BayAvailability): String = when {
    a.totalBays == 0 -> "No bays listed"
    a.freeBays > 0 -> "${a.freeBays} of ${a.totalBays} bays free now"
    else -> "All bays busy · you can wash in ~${a.shortestWaitMinutes} min"
}

/** One-line availability summary with a colored dot. */
@Composable
fun AvailabilityLine(statuses: List<BayStatus>, modifier: Modifier = Modifier) {
    if (statuses.isEmpty()) return
    val a = BayQueueSimulator.availability(statuses)
    val color = if (a.freeBays > 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Surface(color = color, shape = RoundedCornerShape(50), modifier = Modifier.size(8.dp)) {}
        Spacer(Modifier.width(8.dp))
        Text(availabilityText(a), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
    }
}

/** Grid of bays with queue length and wait time for each. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BaysSection(statuses: List<BayStatus>, title: String = "Bays", modifier: Modifier = Modifier) {
    if (statuses.isEmpty()) return
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        AvailabilityLine(statuses)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            maxItemsInEachRow = 2
        ) {
            statuses.forEach { BayTile(it, Modifier.weight(1f)) }
        }
        Text(
            "Queues are estimated and refresh automatically.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun BayTile(bay: BayStatus, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val (container, content) = if (bay.isFree) scheme.tertiaryContainer to scheme.onTertiaryContainer
    else scheme.surfaceVariant to scheme.onSurfaceVariant
    val time = SimpleDateFormat("HH:mm", Locale.getDefault())
    Card(
        colors = CardDefaults.cardColors(containerColor = container, contentColor = content),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Bay ${bay.number}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(bay.kind.label + if (bay.kind.bookable) " · bookable" else "", fontSize = 12.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                when {
                    bay.isFree -> "Free now"
                    bay.bookedNow && bay.carsInLine <= 1 -> "Booked · free in ~${bay.waitMinutes} min"
                    else -> "${bay.carsInLine} car${if (bay.carsInLine != 1) "s" else ""} · ~${bay.waitMinutes} min"
                },
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
            bay.nextBookingStart?.let {
                Text("Next booking ${time.format(Date(it))}", fontSize = 11.sp)
            }
        }
    }
}

/** Upcoming bookings for premium self-service or automatic bays (owner view). */
@Composable
fun BookingsList(bookings: List<AppointmentWithDetails>, bayOffset: Int, modifier: Modifier = Modifier) {
    val fmt = SimpleDateFormat("EEE d MMM, HH:mm", Locale.getDefault())
    val endFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.EventAvailable, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Text("Scheduled appointments", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        }
        if (bookings.isEmpty()) {
            Text("No upcoming bookings.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        bookings.forEach { b ->
            val a = b.appointment
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("${fmt.format(Date(a.startTime))} – ${endFmt.format(Date(a.endTime))}", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        val name = listOfNotNull(b.customerFirstName, b.customerLastName).joinToString(" ").ifBlank { "Customer" }
                        Text("$name · ${a.licensePlate}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    a.bayNumber?.let {
                        AssistChip(onClick = {}, label = { Text("Bay ${it + bayOffset}") })
                    }
                }
            }
        }
    }
}

// ---- Navigation to the wash (Google Maps / Waze) ----

/** Location icon that lets the client open the wash in Google Maps or Waze. */
@Composable
fun NavigateToWashButton(
    latitude: Double?,
    longitude: Double?,
    address: String,
    modifier: Modifier = Modifier,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary
) {
    val context = LocalContext.current
    var showChooser by remember { mutableStateOf(false) }
    IconButton(onClick = { showChooser = true }, modifier = modifier) {
        Icon(Icons.Default.LocationOn, contentDescription = "Navigate to car wash", tint = tint)
    }
    if (showChooser) {
        AlertDialog(
            onDismissRequest = { showChooser = false },
            icon = { Icon(Icons.Default.Directions, null) },
            title = { Text("Navigate there") },
            text = { Text(address) },
            confirmButton = {
                TextButton(onClick = { showChooser = false; openGoogleMaps(context, latitude, longitude, address) }) {
                    Icon(Icons.Default.Map, null); Spacer(Modifier.width(6.dp)); Text("Google Maps")
                }
            },
            dismissButton = {
                TextButton(onClick = { showChooser = false; openWaze(context, latitude, longitude, address) }) { Text("Waze") }
            }
        )
    }
}

private fun openGoogleMaps(context: Context, lat: Double?, lng: Double?, address: String) {
    val destination = if (lat != null && lng != null) "$lat,$lng" else Uri.encode(address)
    val app = Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=$destination")).setPackage("com.google.android.apps.maps")
    val web = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$destination"))
    launchFirst(context, app, web)
}

private fun openWaze(context: Context, lat: Double?, lng: Double?, address: String) {
    val url = if (lat != null && lng != null) "https://waze.com/ul?ll=$lat,$lng&navigate=yes"
    else "https://waze.com/ul?q=${Uri.encode(address)}&navigate=yes"
    // The Waze link opens the app when installed, otherwise the browser.
    launchFirst(context, Intent(Intent.ACTION_VIEW, Uri.parse(url)))
}

private fun launchFirst(context: Context, vararg intents: Intent) {
    for (intent in intents) {
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return
        } catch (_: ActivityNotFoundException) { }
    }
    Toast.makeText(context, "No maps app found", Toast.LENGTH_SHORT).show()
}

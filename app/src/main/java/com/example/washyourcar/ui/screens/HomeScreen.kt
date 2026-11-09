package com.example.washyourcar.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.washyourcar.R
import com.example.washyourcar.data.entities.WashType
import com.example.washyourcar.data.entities.washKey
import com.example.washyourcar.models.WashListItem
import com.example.washyourcar.network.DeviceWeather
import com.example.washyourcar.network.DeviceWeatherApi
import com.example.washyourcar.ui.viewmodel.ClientHomeViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    firebaseUid: String,
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit,
    onLogout: () -> Unit,
    onWashClick: (WashType, Int) -> Unit,
    viewModel: ClientHomeViewModel = viewModel()
) {
    var selectedTab by rememberSaveable { mutableStateOf("Explore") }
    val washes by viewModel.washes.collectAsState()
    val cities by viewModel.cities.collectAsState()
    val selectedCity by viewModel.selectedCity.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedTypes by viewModel.selectedTypes.collectAsState()
    val profileLoading by viewModel.profileLoading.collectAsState()
    val profileMessage by viewModel.profileMessage.collectAsState()
    val signedInEmail by viewModel.signedInEmail.collectAsState()
    val currentCustomer by viewModel.currentCustomer.collectAsState()
    val customerCar by viewModel.customerCar.collectAsState()
    val activeAppointment by viewModel.activeAppointment.collectAsState()
    val carWashRatings by viewModel.carWashRatings.collectAsState()
    val runningBookings by viewModel.runningBookings.collectAsState()
    // Ticks so the simulated queues on the cards stay current.
    var nowTick by remember { mutableLongStateOf(System.currentTimeMillis()) }
    RefreshEvery {
        nowTick = System.currentTimeMillis()
        viewModel.refreshAvailability()
    }
    var showCancelConfirmationDialog by remember { mutableStateOf(false) }

    // Map + "Find my wash"
    val (userPosition, refreshPosition) = rememberUserPosition()
    val needs by viewModel.needs.collectAsState()
    val matches by viewModel.matches.collectAsState()
    val matchBusy by viewModel.matchBusy.collectAsState()
    val aiNote by viewModel.aiNote.collectAsState()
    var showFindMyWash by remember { mutableStateOf(false) }

    if (showFindMyWash) {
        FindMyWashSheet(
            needs = needs,
            onNeedsChange = viewModel::updateNeeds,
            busy = matchBusy,
            aiNote = aiNote,
            matches = matches,
            hasLocation = userPosition != null,
            onAskAi = viewModel::understandRequest,
            onFind = { viewModel.findMatches(userPosition?.latitude, userPosition?.longitude) },
            onBackToNeeds = viewModel::clearMatches,
            onOpenWash = { m -> showFindMyWash = false; onWashClick(m.candidate.item.type, m.candidate.item.id) },
            onDismiss = { showFindMyWash = false }
        )
    }

    LaunchedEffect(firebaseUid) {
        if (firebaseUid.isNotEmpty()) {
            viewModel.loadCustomerData(firebaseUid)
            viewModel.loadActiveAppointment(firebaseUid)
        }
    }

    // Weather for the header: location comes from the device's IP, refreshed every 30 minutes.
    var weather by remember { mutableStateOf<DeviceWeather?>(null) }
    LaunchedEffect(Unit) {
        while (true) {
            weather = DeviceWeatherApi.fetch() ?: weather
            delay(30 * 60 * 1000L)
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.refreshRatings()
        }
    }

    if (showCancelConfirmationDialog && activeAppointment != null) {
        AlertDialog(
            onDismissRequest = { showCancelConfirmationDialog = false },
            title = { Text("Cancel Appointment?", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error) },
            text = { Text("Are you sure you want to cancel your upcoming reservation? This action cannot be undone.", color = MaterialTheme.colorScheme.onSurface) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.cancelAppointment(activeAppointment!!.appointmentId, firebaseUid)
                        showCancelConfirmationDialog = false
                    }
                ) {
                    Text("Yes, Cancel", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelConfirmationDialog = false }) {
                    Text("No, Keep it", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                listOf(
                    "Explore" to Icons.Default.Search,
                    "Appointments" to Icons.Default.Event,
                    "Account" to Icons.Default.Person
                ).forEach { (label, icon) ->
                    NavigationBarItem(
                        selected = selectedTab == label,
                        onClick = {
                            selectedTab = label
                            if (label == "Account") viewModel.loadCustomerData(firebaseUid)
                            if (label == "Appointments") viewModel.loadActiveAppointment(firebaseUid)
                        },
                        icon = { Icon(icon, null) },
                        label = { Text(label) }
                    )
                }
            }
        },
        topBar = {
            TopAppBar(
                modifier = Modifier.washYourCarHeader(),
                title = { Text("Wash Your Car!", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold) },
                actions = { WeatherHeaderBadge(weather) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        if (selectedTab != "Explore") {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    if (selectedTab == "Appointments") "Your next appointment" else "Account details",
                    style = MaterialTheme.typography.headlineSmall
                )
                if (selectedTab == "Appointments") {
                    if (activeAppointment != null) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(3.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val dateFormatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                                val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
                                val dateStr = dateFormatter.format(Date(activeAppointment!!.startTime))
                                val timeStr = timeFormatter.format(Date(activeAppointment!!.startTime))

                                Text(
                                    text = "YOUR NEXT SLOT",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )

                                val sharedPrefs = LocalContext.current.getSharedPreferences(
                                    "WashYourCarPrefs",
                                    android.content.Context.MODE_PRIVATE
                                )
                                val savedWashName = sharedPrefs.getString("last_booked_wash_name", "Car Wash Station")

                                Text(
                                    text = "📍 Location: $savedWashName",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Date: $dateStr",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Time: $timeStr",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Plate: ${activeAppointment!!.licensePlate}",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                OutlinedButton(
                                    onClick = { showCancelConfirmationDialog = true },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    contentPadding = PaddingValues(vertical = 4.dp)
                                ) {
                                    Text("Cancel Appointment", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "No upcoming appointments.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                } else {
                    Text(
                        currentCustomer?.let { "${it.firstName} ${it.lastName}" } ?: "Your account",
                        style = MaterialTheme.typography.titleLarge
                    )
                    if (profileLoading) LoadingCardSkeleton(lines = 4)
                    profileMessage?.let { message ->
                        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedButton(onClick = { viewModel.loadCustomerData(firebaseUid) }) {
                            Text("Retry loading details")
                        }
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(8.dp),
                        elevation = CardDefaults.cardElevation(2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Email: ${currentCustomer?.email ?: signedInEmail ?: "Not available"}",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Phone: ${currentCustomer?.phoneNumber ?: "Not stored on this device"}",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (customerCar != null) {
                                Text(
                                    text = "Car: ${customerCar!!.model} [${customerCar!!.licensePlate}]",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            } else {
                                Text(
                                    text = "No car registered",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }

                    LoyaltyCardSection(firebaseUid = firebaseUid)

                    OutlinedButton(
                        onClick = onThemeToggle,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                    ) {
                        Icon(if (isDarkTheme) Icons.Default.WbSunny else Icons.Default.DarkMode, null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (isDarkTheme) "Light mode" else "Dark mode")
                    }
                    Button(
                        onClick = onLogout,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB3261E), contentColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Sign out")
                    }
                }
            }
        } else {
            // Explore = map of car washes + pull-up "near you" list + "Find my wash" button.
            ExploreMapScreen(
                washes = washes,
                bayStatusesFor = { wash ->
                    if (wash.type == WashType.NORMAL) emptyList() else {
                        val washKey = wash.type.washKey(wash.id)
                        com.example.washyourcar.models.BayQueueSimulator.statuses(
                            washKey, wash.normalBays, wash.premiumBays, wash.automaticBays,
                            runningBookings[washKey].orEmpty(), nowTick
                        )
                    }
                },
                ratings = carWashRatings,
                searchQuery = searchQuery,
                onSearchChange = { viewModel.updateSearchQuery(it) },
                selectedTypes = selectedTypes,
                onToggleType = { viewModel.toggleType(it) },
                position = userPosition,
                onRefreshPosition = refreshPosition,
                highlighted = matches.orEmpty().take(3).map { it.candidate.item },
                onWashClick = { wash -> onWashClick(wash.type, wash.id) },
                onFindMyWash = { showFindMyWash = true },
                modifier = Modifier.fillMaxSize().padding(paddingValues)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarWashCard(
    wash: WashListItem,
    rating: Float?,
    reviewCount: Int,
    bayStatuses: List<com.example.washyourcar.models.BayStatus> = emptyList(),
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.spalatorie),
                    contentDescription = "Car Wash Image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Text(
                    text = wash.type.label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }

            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = wash.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    NavigateToWashButton(wash.latitude, wash.longitude, "${wash.address}, ${wash.city}")

                    if (rating != null && reviewCount > 0) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(1.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val starsCount = rating.toInt().coerceIn(0, 5)
                            repeat(starsCount) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(16.dp))
                            }
                            repeat(5 - starsCount) {
                                Icon(Icons.Default.StarBorder, contentDescription = null, tint = Color(0xFFFFB300).copy(alpha = 0.4f), modifier = Modifier.size(16.dp))
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = String.format(Locale.getDefault(), "%.1f", rating),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = " ($reviewCount)",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    } else {
                        Text(
                            text = "No reviews",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }

                Text(
                    text = "Mon–Sun  ${formatTime(wash.openTime)} - ${formatTime(wash.closeTime)}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )

                Text(
                    text = "${wash.address}, ${wash.city}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )

                // Self-service / automatic: free bays now, or how long until you can wash.
                AvailabilityLine(bayStatuses, Modifier.padding(top = 4.dp))
            }
        }
    }
}

@Composable
private fun WeatherHeaderBadge(weather: DeviceWeather?) {
    Row(
        modifier = Modifier.padding(end = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (weather == null) {
            Text(
                text = "--°C",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
            )
        } else {
            Text(text = weather.emoji, fontSize = 28.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${weather.temperatureC}°C",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Text(
                    text = weather.city?.let { "${weather.description} · $it" } ?: weather.description,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                    maxLines = 1
                )
            }
        }
    }
}

private fun formatTime(time: Long): String {
    val hours = time / 100
    val minutes = time % 100
    return String.format(Locale.getDefault(), "%02d:%02d", hours, minutes)
}
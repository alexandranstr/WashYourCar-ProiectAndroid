package com.example.washyourcar.ui.screens

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.washyourcar.ui.viewmodel.CarWashDetailViewModel
import com.example.washyourcar.ui.viewmodel.WeatherViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarWashDetailScreen(
    carWashId: Int,
    firebaseUid: String,
    onBack: () -> Unit,
    viewModel: CarWashDetailViewModel,
    weatherViewModel: WeatherViewModel = viewModel()
) {
    val carWash by viewModel.carWash.collectAsState()
    val availableSlots by viewModel.availableSlots.collectAsState()
    val customerCar by viewModel.customerCar.collectAsState()
    val isSuccess by viewModel.isSuccess.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val randomTip by viewModel.randomTip.collectAsState()
    val carWashServiceNames by viewModel.carWashServiceNames.collectAsState()

    var showBottomSheet by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    var selectedDateMillis by remember { mutableStateOf<Long?>(null) }
    var selectedTimeRange by remember { mutableStateOf("Morning") }
    var selectedServiceId by remember { mutableStateOf<Int?>(null) }
    var selectedServiceDuration by remember { mutableStateOf(0) }
    var selectedSlotStartTime by remember { mutableStateOf<Long?>(null) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("WashYourCarPrefs", android.content.Context.MODE_PRIVATE) }
    val lastBookedWash = remember { sharedPrefs.getString("last_booked_wash_name", null) }

    val datePickerState = rememberDatePickerState(
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                return utcTimeMillis >= System.currentTimeMillis() - 24L * 60L * 60L * 1000L
            }
        }
    )

    LaunchedEffect(Unit) {
        weatherViewModel.fetchWeather("Brasov", "09981e4b7d1730200bcc5f422d1f6f85")
    }

    LaunchedEffect(carWashId, firebaseUid) {
        viewModel.loadCarWashDetails(carWashId, firebaseUid)
    }

    LaunchedEffect(isSuccess) {
        if (isSuccess) {
            showBottomSheet = false
            showSuccessDialog = true
            viewModel.resetSuccess()
        }
    }

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = {
                showSuccessDialog = false
                onBack()
            },
            title = { Text("Appointment Confirmed!", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
            text = { Text("Your car wash slot has been successfully reserved. You can review or cancel it in your appointments section.", color = MaterialTheme.colorScheme.onSurface) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSuccessDialog = false
                        onBack()
                    }
                ) {
                    Text("OK", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(carWash?.name ?: "Details", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        carWash?.let { wash ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                randomTip?.let { tip ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = tip.title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = tip.message,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                lastBookedWash?.let { washName ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Last time you scheduled at: ",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = washName,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }

                Card(
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(6.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.secondary
                                    )
                                )
                            )
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Open daily: ${wash.openTime / 100}:00 - ${wash.closeTime / 100}:00",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${wash.address}, ${wash.city}",
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                            fontSize = 14.sp
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 4.dp))

                        Text(text = "Premium Services Available: ", color = MaterialTheme.colorScheme.onPrimary, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(4.dp))

                        if (carWashServiceNames.isEmpty()) {
                            Text(text = "No custom services listed.", color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f), fontSize = 12.sp)
                        } else {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                carWashServiceNames.chunked(2).forEach { rowServices ->
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        rowServices.forEach { serviceName ->
                                            Surface(
                                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier.padding(horizontal = 2.dp)
                                            ) {
                                                Text(
                                                    text = serviceName,
                                                    color = MaterialTheme.colorScheme.onPrimary,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = { showBottomSheet = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text("Schedule appointment", color = MaterialTheme.colorScheme.onPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("Schedule appointment", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                Text("Select date", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                DatePicker(
                    state = datePickerState,
                    title = null,
                    headline = null,
                    showModeToggle = false,
                    colors = DatePickerDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        headlineContentColor = MaterialTheme.colorScheme.onSurface,
                        weekdayContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        subheadContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        yearContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        currentYearContentColor = MaterialTheme.colorScheme.primary,
                        selectedYearContentColor = MaterialTheme.colorScheme.onPrimary,
                        selectedYearContainerColor = MaterialTheme.colorScheme.primary,
                        dayContentColor = MaterialTheme.colorScheme.onSurface,
                        selectedDayContentColor = MaterialTheme.colorScheme.onPrimary,
                        selectedDayContainerColor = MaterialTheme.colorScheme.primary,
                        todayContentColor = MaterialTheme.colorScheme.primary,
                        todayDateBorderColor = MaterialTheme.colorScheme.primary
                    )
                )

                selectedDateMillis = datePickerState.selectedDateMillis

                Text("Select service", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                val masterServices = listOf(
                    Triple(1, "Exterior Wash", 15),
                    Triple(2, "Interior Cleaning", 20),
                    Triple(3, "Detailing", 60),
                    Triple(4, "Polish", 90)
                )

                val availableLocalServices = masterServices.filter { (_, name, _) ->
                    carWashServiceNames.any { it.equals(name, ignoreCase = true) }
                }

                if (availableLocalServices.isEmpty()) {
                    Text("No services available for this car wash.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                } else {
                    availableLocalServices.forEach { (id, name, duration) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedServiceId = id
                                    selectedServiceDuration = duration
                                }
                        ) {
                            RadioButton(
                                selected = selectedServiceId == id,
                                onClick = {
                                    selectedServiceId = id
                                    selectedServiceDuration = duration
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                            )
                            Text("$name ($duration min)", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }

                Text("Total duration: $selectedServiceDuration minutes", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)

                Text("Select time range", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                var dropdownExpanded by remember { mutableStateOf(false) }

                Box {
                    Button(
                        onClick = { dropdownExpanded = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Text(selectedTimeRange, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    DropdownMenu(expanded = dropdownExpanded, onDismissRequest = { dropdownExpanded = false }) {
                        listOf("Morning", "Afternoon", "Evening").forEach { range ->
                            DropdownMenuItem(
                                text = { Text(range, color = MaterialTheme.colorScheme.onSurface) },
                                onClick = {
                                    selectedTimeRange = range
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        selectedDateMillis?.let {
                            viewModel.generateSlots(carWashId, it, selectedServiceDuration, selectedTimeRange)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text("Check availability", color = MaterialTheme.colorScheme.onSecondary)
                }

                errorMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }

                if (availableSlots.isNotEmpty()) {
                    Text("Available time slots", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.height(100.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(availableSlots) { timeMs ->
                            val timeString = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timeMs))
                            val isSelected = selectedSlotStartTime == timeMs

                            Button(
                                onClick = { selectedSlotStartTime = timeMs },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Text(timeString, color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        val plate = customerCar?.licensePlate ?: "Test"
                        if (selectedSlotStartTime != null && selectedServiceId != null) {
                            viewModel.confirmAppointment(
                                context = context,
                                customerId = firebaseUid,
                                carWashId = carWashId,
                                serviceId = selectedServiceId!!,
                                licensePlate = plate,
                                startTime = selectedSlotStartTime!!,
                                durationMinutes = selectedServiceDuration
                            )
                        }
                    },
                    enabled = selectedSlotStartTime != null,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Text("Confirm appointment", color = MaterialTheme.colorScheme.onPrimary)
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
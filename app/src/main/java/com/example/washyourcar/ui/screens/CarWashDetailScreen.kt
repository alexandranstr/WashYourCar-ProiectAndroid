package com.example.washyourcar.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.washyourcar.ui.viewmodel.CarWashDetailViewModel
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarWashDetailScreen(
    carWashId: Int,
    firebaseUid: String,
    onBack: () -> Unit,
    viewModel: CarWashDetailViewModel
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
                return utcTimeMillis >= System.currentTimeMillis() - 24 * 60 * 60 * 1000
            }
        }
    )

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
            title = { Text("Appointment Confirmed!", fontWeight = FontWeight.Bold, color = Color(0xFF6A1B9A)) },
            text = { Text("Your car wash slot has been successfully reserved. You can review or cancel it in your appointments section.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSuccessDialog = false
                        onBack()
                    }
                ) {
                    Text("OK", fontWeight = FontWeight.Bold, color = Color(0xFF6A1B9A))
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = Color.White
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(carWash?.name ?: "Details", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF6A1B9A))
            )
        }
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
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9C4)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "${tip.title}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF57F17)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = tip.message,
                                fontSize = 13.sp,
                                color = Color.Black
                            )
                        }
                    }
                }

                lastBookedWash?.let { washName ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)), // Verde deschis plăcut
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "↩Last time you scheduled at: ",
                                fontSize = 12.sp,
                                color = Color.Black
                            )
                            Text(
                                text = washName,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
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
                                    colors = listOf(Color(0xFF5E1680), Color(0xFF7B1FA2), Color(0xFF8E24AA))
                                )
                            )
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Open daily: ${wash.openTime / 100}:00 - ${wash.closeTime / 100}:00",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${wash.address}, ${wash.city}",
                            color = Color(0xFFEDE7F6),
                            fontSize = 14.sp
                        )

                        HorizontalDivider(color = Color(0xFFD1C4E9), modifier = Modifier.padding(vertical = 4.dp))

                        Text(text = "Premium Services Available: ", color = Color.White, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        if (carWashServiceNames.isEmpty()) {
                            Text(text = "No custom services listed.", color = Color(0xFFE1D5F5), fontSize = 12.sp)
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
                                                color = Color.White.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier.padding(horizontal = 2.dp)
                                            ) {
                                                Text(
                                                    text = serviceName,
                                                    color = Color.White,
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
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6A1B9A)),
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text("Schedule appointment", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("Schedule appointment", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6A1B9A))

                Text("Select date", fontWeight = FontWeight.Bold, color = Color(0xFF6A1B9A))
                DatePicker(state = datePickerState, title = null, headline = null, showModeToggle = false)

                selectedDateMillis = datePickerState.selectedDateMillis

                Text("Select service", fontWeight = FontWeight.Bold, color = Color(0xFF6A1B9A))

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
                    Text("No services available for this car wash.", color = Color.Gray, fontSize = 14.sp)
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
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF6A1B9A))
                            )
                            Text("$name ($duration min)", fontSize = 14.sp)
                        }
                    }
                }

                Text("Total duration: $selectedServiceDuration minutes", color = Color(0xFF4A148C), fontWeight = FontWeight.Bold)

                Text("Select time range", fontWeight = FontWeight.Bold, color = Color(0xFF6A1B9A))
                var dropdownExpanded by remember { mutableStateOf(false) }

                Box {
                    Button(onClick = { dropdownExpanded = true }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEDE7F6))) {
                        Text(selectedTimeRange, color = Color(0xFF6A1B9A))
                    }
                    DropdownMenu(expanded = dropdownExpanded, onDismissRequest = { dropdownExpanded = false }) {
                        listOf("Morning", "Afternoon", "Evening").forEach { range ->
                            DropdownMenuItem(text = { Text(range) }, onClick = {
                                selectedTimeRange = range
                                dropdownExpanded = false
                            })
                        }
                    }
                }

                Button(
                    onClick = {
                        selectedDateMillis?.let {
                            viewModel.generateSlots(carWashId, it, selectedServiceDuration, selectedTimeRange)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFAB47BC)),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text("Check availability", color = Color.White)
                }

                errorMessage?.let {
                    Text(it, color = Color.Red, fontSize = 12.sp)
                }

                if (availableSlots.isNotEmpty()) {
                    Text("Available time slots", fontWeight = FontWeight.Bold, color = Color(0xFF6A1B9A))

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
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
                                    containerColor = if (isSelected) Color(0xFF6A1B9A) else Color(0xFFE1D5F5)
                                )
                            ) {
                                Text(timeString, color = if (isSelected) Color.White else Color.Black, fontSize = 11.sp)
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
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6A1B9A)),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Text("Confirm appointment", color = Color.White)
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
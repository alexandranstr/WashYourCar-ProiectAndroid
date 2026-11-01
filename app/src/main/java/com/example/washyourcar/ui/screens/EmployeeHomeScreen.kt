package com.example.washyourcar.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Person
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.washyourcar.data.entities.AppointmentWithDetails
import com.example.washyourcar.ui.viewmodel.EmployeeHomeViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeHomeScreen(
    firebaseUid: String,
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit,
    onLogout: () -> Unit,
    viewModel: EmployeeHomeViewModel
) {
    var selectedTab by rememberSaveable { mutableStateOf("Appointments") }
    val appointmentsListState = rememberLazyListState()
    BackHandler(enabled = selectedTab == "Account") { selectedTab = "Appointments" }

    val selectedDate by viewModel.selectedDate.collectAsState()
    val appointments by viewModel.appointments.collectAsState()
    val carWashName by viewModel.carWashName.collectAsState()
    val appointmentServiceNames by viewModel.appointmentServiceNames.collectAsState()

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate.timeInMillis)

    val dateFormatter = SimpleDateFormat("EEE, dd MMM", Locale.getDefault())
    val displayedDate = dateFormatter.format(selectedDate.time)

    LaunchedEffect(firebaseUid) {
        viewModel.initEmployee(firebaseUid)
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    showDatePicker = false
                    datePickerState.selectedDateMillis?.let { millis ->
                        val newDate = Calendar.getInstance().apply { timeInMillis = millis }
                        viewModel.updateSelectedDate(newDate)
                    }
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
    val currentEmployee by viewModel.currentEmployee.collectAsState()

    val accountDetails: @Composable () -> Unit = {

                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(8.dp),
                                elevation = CardDefaults.cardElevation(2.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "Email: ${currentEmployee?.email ?: "N/A"}",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Phone: ${currentEmployee?.phoneNumber ?: "N/A"}",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Token: ${currentEmployee?.accessToken ?: "N/A"}",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    if (carWashName != null) {
                                        Text(
                                            text = "Car Wash: $carWashName",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    } else if (currentEmployee?.carWashId != null) {
                                        Text(
                                            text = "Assigned Car Wash: #${currentEmployee!!.carWashId}",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    } else {
                                        Text(
                                            text = "No car wash assigned",
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }

    }

    Scaffold(
            bottomBar = {
                NavigationBar {
                    listOf("Appointments" to Icons.Default.Event, "Account" to Icons.Default.Person).forEach { (label, icon) ->
                        NavigationBarItem(
                            selected = selectedTab == label,
                            onClick = { selectedTab = label },
                            icon = { Icon(icon, null) },
                            label = { Text(label) }
                        )
                    }
                }
            },
            topBar = {
                TopAppBar(
                    modifier = Modifier.washYourCarHeader(),
                    title = {
                        if (selectedTab == "Account") Text("Account", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                        else Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showDatePicker = true }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = displayedDate,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = "Select Date",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    },
                    actions = {
                        if (selectedTab == "Appointments") {
                        IconButton(onClick = { viewModel.changeDateByDays(-1) }) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Prev", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(32.dp))
                        }
                        IconButton(onClick = { viewModel.changeDateByDays(1) }) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(32.dp))
                        }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            if (selectedTab == "Account") {
                Column(
                    Modifier.fillMaxSize().padding(paddingValues).verticalScroll(rememberScrollState()).padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(currentEmployee?.let { "${it.firstName} ${it.lastName}" } ?: "Employee account",
                        style = MaterialTheme.typography.headlineSmall)
                    accountDetails()
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
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB3261E), contentColor = Color.White),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Sign out")
                    }
                }
            } else Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                if (appointments.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No appointments for this day.", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                    }
                } else {
                    LazyColumn(
                        state = appointmentsListState,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize().padding(top = 8.dp)
                    ) {
                        items(appointments) { details ->
                            val serviceNames = appointmentServiceNames[details.appointment.appointmentId] ?: emptyList()
                            EmployeeAppointmentCard(
                                details = details,
                                serviceNames = serviceNames,
                                onDoneClick = { viewModel.markAppointmentAsDone(details.appointment.appointmentId) }
                            )
                        }
                }
            }
        }
    }
}

@Composable
fun EmployeeAppointmentCard(
    details: AppointmentWithDetails,
    serviceNames: List<String>,
    onDoneClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    val startTime = timeFormatter.format(Date(details.appointment.startTime))
    val endTime = timeFormatter.format(Date(details.appointment.endTime))

    val customerName = "${details.customerFirstName ?: ""} ${details.customerLastName ?: ""}".trim()
    val status = details.appointment.status

    val isDark = MaterialTheme.colorScheme.background == Color.Black ||
            MaterialTheme.colorScheme.background == Color(0xFF121212)

    val (statusLabel, statusColor, statusBackgroundColor) = when (status) {
        "CONFIRMED" -> {
            if (isDark) Triple("Pending", Color.White, Color(0xFFF57F17))
            else Triple("Pending", Color.White, Color(0xFFFFA000))
        }
        "IN_PROGRESS" -> {
            if (isDark) Triple("In Progress", Color.White, Color(0xFFE65100))
            else Triple("In Progress", Color.White, Color(0xFFD84315))
        }
        "DONE" -> {
            if (isDark) Triple("Done", Color.White, Color(0xFF2E7D32))
            else Triple("Done", Color.White, Color(0xFF388E3C))
        }
        "CANCELED" -> {
            if (isDark) Triple("Cancelled", Color.White, Color(0xFFC62828))
            else Triple("Cancelled", Color.White, Color(0xFFD32F2F))
        }
        else -> Triple(status, Color.White, Color.Gray)
    }

    val canMarkDone = status == "CONFIRMED" || status == "IN_PROGRESS"

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$startTime - $endTime",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.width(115.dp)
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(text = customerName, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    Text(text = "Car: ${details.appointment.licensePlate}", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusBackgroundColor)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = statusLabel,
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { expanded = !expanded },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp).weight(1f)
                ) {
                    Text("Details", color = MaterialTheme.colorScheme.onPrimary, fontSize = 12.sp)
                }

                if (canMarkDone) {
                    Button(
                        onClick = onDoneClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp).weight(1f)
                    ) {
                        Text("Mark Done", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            AnimatedVisibility(visible = expanded) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (serviceNames.isNotEmpty()) {
                            Text(
                                text = "Services: ${serviceNames.joinToString(", ")}",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                text = "Services: Unknown",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "Phone: ${details.customerPhone ?: "N/A"}",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
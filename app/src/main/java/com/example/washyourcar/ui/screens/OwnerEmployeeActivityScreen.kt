package com.example.washyourcar.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.washyourcar.data.entities.AppointmentWithFullDetails
import com.example.washyourcar.ui.viewmodel.EmployeeActivityViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerEmployeeActivityScreen(
    onBack: () -> Unit,
    viewModel: EmployeeActivityViewModel,
    bottomBar: @Composable () -> Unit = {}
) {
    val selectedDate by viewModel.selectedDate.collectAsState()
    val appointments by viewModel.appointments.collectAsState()

    val dateFormatter = SimpleDateFormat("EEE, dd MMM", Locale.getDefault())
    val displayedDate = dateFormatter.format(selectedDate.time)

    Scaffold(
            bottomBar = bottomBar,
        topBar = {
            TopAppBar(
                modifier = Modifier.washYourCarHeader(),
                title = { Text("Employee Activity", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold) },
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
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.changeDateByDays(-1) }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Prev Day", modifier = Modifier.size(32.dp))
                }
                Text(
                    text = displayedDate,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                IconButton(onClick = { viewModel.changeDateByDays(1) }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next Day", modifier = Modifier.size(32.dp))
                }
            }

            if (appointments.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No activity for this day.", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(appointments) { item ->
                        ActivityCard(item)
                    }
                }
            }
        }
    }
}

@Composable
fun ActivityCard(item: AppointmentWithFullDetails) {
    val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    val startTime = timeFormatter.format(Date(item.appointment.startTime))
    val endTime = timeFormatter.format(Date(item.appointment.endTime))

    val statusColor = when (item.appointment.status) {
        "DONE" -> Color(0xFF4CAF50)
        "IN_PROGRESS" -> Color(0xFFFF9800)
        "CANCELED" -> Color(0xFFF44336)
        else -> MaterialTheme.colorScheme.primary
    }

    val customerName = if (item.customerFirstName != null || item.customerLastName != null) {
        "${item.customerFirstName ?: ""} ${item.customerLastName ?: ""}".trim()
    } else {
        "Unknown"
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$startTime - $endTime",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Box(
                    modifier = Modifier.background(statusColor, RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(item.appointment.status, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = "Customer: $customerName", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                if (item.customerPhone != null) {
                    Text(text = "Phone: ${item.customerPhone}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = "Car: ${item.appointment.licensePlate}", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(text = "Employee: ${item.employeeName ?: "Unassigned"}", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            if (!item.serviceNames.isNullOrEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                Text(text = "Services: ${item.serviceNames}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
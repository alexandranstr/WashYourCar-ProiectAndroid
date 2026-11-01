package com.example.washyourcar.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.washyourcar.data.entities.CarWashServiceDetails
import com.example.washyourcar.data.entities.Service
import com.example.washyourcar.ui.viewmodel.CarWashMonitoringViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerCarWashMonitoringScreen(
    onBack: () -> Unit,
    viewModel: CarWashMonitoringViewModel,
    bottomBar: @Composable () -> Unit = {}
) {
    val activeServices by viewModel.activeServices.collectAsState()
    val availableServices by viewModel.availableServices.collectAsState()
    val context = LocalContext.current

    var showAddDialog by remember { mutableStateOf(false) }
    var serviceToEdit by remember { mutableStateOf<CarWashServiceDetails?>(null) }

    Scaffold(
            bottomBar = bottomBar,
        topBar = {
            TopAppBar(
                modifier = Modifier.washYourCarHeader(),
                title = { Text("Car Wash Monitoring", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.secondary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Service", tint = Color.White)
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            Button(
                onClick = { Toast.makeText(context, "Connecting to live cameras...", Toast.LENGTH_SHORT).show() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Icon(Icons.Default.Videocam, contentDescription = "Live Camera", tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("View Live Cameras", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Text(
                text = "Active Services",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            if (activeServices.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No active services. Add one using the button below.", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(activeServices) { serviceDetail ->
                        ServiceMonitoringCard(
                            details = serviceDetail,
                            onEdit = { serviceToEdit = serviceDetail },
                            onDelete = { viewModel.removeService(serviceDetail.carWashService.serviceId) }
                        )
                    }
                }
            }
        }
    }

    if (serviceToEdit != null) {
        EditServiceDialog(
            details = serviceToEdit!!,
            onDismiss = { serviceToEdit = null },
            onConfirm = { newPrice, newDuration ->
                viewModel.updateService(serviceToEdit!!.carWashService.serviceId, newPrice, newDuration)
                serviceToEdit = null
            }
        )
    }

    if (showAddDialog) {
        AddServiceDialog(
            availableServices = availableServices,
            onDismiss = { showAddDialog = false },
            onConfirm = { serviceId, price, duration ->
                viewModel.addService(serviceId, price, duration)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun ServiceMonitoringCard(
    details: CarWashServiceDetails,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = details.serviceName, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = "Price: ${details.carWashService.price} RON | Duration: ${details.carWashService.duration} min", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun EditServiceDialog(
    details: CarWashServiceDetails,
    onDismiss: () -> Unit,
    onConfirm: (Float, Int) -> Unit
) {
    var priceText by remember { mutableStateOf(details.carWashService.price.toString()) }
    var durationText by remember { mutableStateOf(details.carWashService.duration.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit ${details.serviceName}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text("Price (RON)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = durationText,
                    onValueChange = { durationText = it },
                    label = { Text("Duration (minutes)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val price = priceText.toFloatOrNull() ?: details.carWashService.price
                val duration = durationText.toIntOrNull() ?: details.carWashService.duration
                onConfirm(price, duration)
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddServiceDialog(
    availableServices: List<Service>,
    onDismiss: () -> Unit,
    onConfirm: (Int, Float, Int) -> Unit
) {
    var selectedService by remember { mutableStateOf<Service?>(availableServices.firstOrNull()) }
    var priceText by remember { mutableStateOf("") }
    var durationText by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Service") },
        text = {
            if (availableServices.isEmpty()) {
                Text("All available services are already active.")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = selectedService?.name ?: "Select Service",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            availableServices.forEach { service ->
                                DropdownMenuItem(
                                    text = { Text(service.name) },
                                    onClick = {
                                        selectedService = service
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it },
                        label = { Text("Price (RON)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = durationText,
                        onValueChange = { durationText = it },
                        label = { Text("Duration (minutes)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val price = priceText.toFloatOrNull() ?: 0f
                    val duration = durationText.toIntOrNull() ?: 0
                    selectedService?.let { onConfirm(it.serviceId, price, duration) }
                },
                enabled = availableServices.isNotEmpty() && selectedService != null
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
package com.example.washyourcar.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.washyourcar.data.entities.WashType
import com.example.washyourcar.models.ServiceOption
import com.example.washyourcar.viewmodel.AuthViewModel

@Composable
fun RegisterWashServicesScreen(
    viewModel: AuthViewModel,
    washType: WashType,
    washId: Int,
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit,
    onRegisterSuccess: () -> Unit,
    onBackClick: () -> Unit
) {
    var services by remember { mutableStateOf<List<ServiceOption>>(emptyList()) }
    val serviceSelected = remember { mutableStateMapOf<Int, Boolean>() }
    val servicePrices = remember { mutableStateMapOf<Int, String>() }
    val serviceDurations = remember { mutableStateMapOf<Int, String>() }
    var localErrorMessage by remember { mutableStateOf<String?>(null) }

    val authState by viewModel.authState.collectAsState()

    LaunchedEffect(washType) {
        viewModel.clearError()
        services = viewModel.getServiceOptions(washType)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            AuthHeader(isDarkTheme = isDarkTheme, onThemeToggle = onThemeToggle, onBackClick = onBackClick)
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "Services Offered",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Select the services your ${washType.label.lowercase()} car wash provides",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                modifier = Modifier.padding(horizontal = 40.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (services.isEmpty()) {
            item {
                Text(
                    "No services available for this type.",
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    modifier = Modifier.padding(horizontal = 40.dp, vertical = 16.dp)
                )
            }
        }

        items(services, key = { it.id }) { service ->
            Card(
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 40.dp, vertical = 6.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = serviceSelected[service.id] ?: false,
                            onCheckedChange = { serviceSelected[service.id] = it },
                            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                service.name,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                service.description,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                        }
                    }
                    if (serviceSelected[service.id] == true) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AuthTextField(
                                label = "Price (RON)",
                                value = servicePrices[service.id] ?: "",
                                onValueChange = { servicePrices[service.id] = it },
                                modifier = Modifier.weight(1f)
                            )
                            AuthTextField(
                                label = "Duration (min)",
                                value = serviceDurations[service.id] ?: "",
                                onValueChange = { serviceDurations[service.id] = it },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        item {
            val displayError = localErrorMessage ?: authState.errorMessage
            if (displayError != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    displayError,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 40.dp)
                )
            }
            Spacer(modifier = Modifier.height(20.dp))

            AuthButton(
                text = "FINISH",
                loadingText = "SAVING...",
                isLoading = authState.isLoading,
                onClick = {
                    val selected = services.filter { serviceSelected[it.id] == true }
                    if (selected.isEmpty()) {
                        localErrorMessage = "Please select at least one service."
                        return@AuthButton
                    }
                    for (svc in selected) {
                        val price = servicePrices[svc.id]
                        val duration = serviceDurations[svc.id]
                        if (price.isNullOrBlank() || duration.isNullOrBlank()) {
                            localErrorMessage = "Please fill in price and duration for ${svc.name}."
                            return@AuthButton
                        }
                        if (price.toFloatOrNull() == null || price.toFloat() <= 0f) {
                            localErrorMessage = "Invalid price for ${svc.name}."
                            return@AuthButton
                        }
                        if (duration.toIntOrNull() == null || duration.toInt() <= 0) {
                            localErrorMessage = "Invalid duration for ${svc.name}."
                            return@AuthButton
                        }
                    }
                    localErrorMessage = null

                    val serviceData = selected.map { svc ->
                        Triple(svc.id, servicePrices[svc.id]!!.toFloat(), serviceDurations[svc.id]!!.toInt())
                    }
                    viewModel.saveWashServices(washType, washId, serviceData, onRegisterSuccess)
                },
                modifier = Modifier.padding(horizontal = 40.dp)
            )
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
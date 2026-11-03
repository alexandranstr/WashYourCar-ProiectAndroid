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
import com.example.washyourcar.data.entities.Service
import com.example.washyourcar.viewmodel.AuthViewModel

@Composable
fun RegisterWashDetailsScreen(
    viewModel: AuthViewModel,
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit,
    onRegisterSuccess: () -> Unit,
    onBackClick: () -> Unit,
    availableServices: List<Service>
) {
    var washName by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var openHour by remember { mutableStateOf("08") }
    var openMinute by remember { mutableStateOf("00") }
    var closeHour by remember { mutableStateOf("20") }
    var closeMinute by remember { mutableStateOf("00") }

    var servicePrices by remember { mutableStateOf(mutableMapOf<Int, String>()) }
    var serviceDurations by remember { mutableStateOf(mutableMapOf<Int, String>()) }
    var serviceSelected by remember { mutableStateOf(mutableMapOf<Int, Boolean>()) }

    LaunchedEffect(availableServices) {
        availableServices.forEach { svc ->
            if (!serviceSelected.containsKey(svc.serviceId)) {
                serviceSelected.toMutableMap().put(svc.serviceId, false)
                serviceSelected = serviceSelected.toMutableMap()
            }
            if (!servicePrices.containsKey(svc.serviceId)) {
                servicePrices = servicePrices.toMutableMap().apply { this.put(svc.serviceId, "") }
            }
            if (!serviceDurations.containsKey(svc.serviceId)) {
                serviceDurations = serviceDurations.toMutableMap().apply { this.put(svc.serviceId, "") }
            }
        }
        serviceSelected = serviceSelected.mapValues { _ -> false }.toMutableMap()
    }

    var localErrorMessage by remember { mutableStateOf<String?>(null) }

    val authState by viewModel.authState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.clearError()
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
                    text = "Car Wash Details",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Tell us about your car wash business",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(20.dp))
            }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AuthTextField(label = "Car Wash Name", value = washName, onValueChange = { washName = it })
                AuthTextField(label = "Address", value = address, onValueChange = { address = it })
                AuthTextField(label = "City", value = city, onValueChange = { city = it })
                AuthTextField(label = "Phone Number", value = phone, onValueChange = { phone = it })

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Opening Hours",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AuthTextField(
                                label = "Opens",
                                value = openHour,
                                onValueChange = { openHour = it },
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = ":",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            AuthTextField(
                                label = "Minute",
                                value = openMinute,
                                onValueChange = { openMinute = it },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AuthTextField(
                                label = "Closes",
                                value = closeHour,
                                onValueChange = { closeHour = it },
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = ":",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            AuthTextField(
                                label = "Minute",
                                value = closeMinute,
                                onValueChange = { closeMinute = it },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            Text(
                text = "Services Offered",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(horizontal = 40.dp, vertical = 8.dp)
            )
            Text(
                text = "Select the services your car wash provides",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                modifier = Modifier
                    .padding(horizontal = 40.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(availableServices) { service ->
            Card(
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 40.dp, vertical = 6.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = serviceSelected[service.serviceId] ?: false,
                            onCheckedChange = { checked ->
                                serviceSelected = serviceSelected.toMutableMap().apply {
                                    this[service.serviceId] = checked
                                }
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = service.name,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = service.description,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                        }
                    }

                    if (serviceSelected[service.serviceId] == true) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AuthTextField(
                                label = "Price (RON)",
                                value = servicePrices[service.serviceId] ?: "",
                                onValueChange = {
                                    servicePrices = servicePrices.toMutableMap().apply {
                                        this[service.serviceId] = it
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                            AuthTextField(
                                label = "Duration (min)",
                                value = serviceDurations[service.serviceId] ?: "",
                                onValueChange = {
                                    serviceDurations = serviceDurations.toMutableMap().apply {
                                        this[service.serviceId] = it
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        item {
            val displayError = localErrorMessage ?: authState.errorMessage
            if (displayError != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = displayError,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 40.dp)
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        item {
            Spacer(modifier = Modifier.height(10.dp))
            AuthButton(
                text = "SAVE & CONTINUE",
                loadingText = "SAVING...",
                isLoading = authState.isLoading,
                onClick = {
                    if (washName.isBlank() || address.isBlank() || city.isBlank() || phone.isBlank()) {
                        localErrorMessage = "Please fill in all car wash details."
                        return@AuthButton
                    }

                    val openH = openHour.toIntOrNull()
                    val openM = openMinute.toIntOrNull()
                    val closeH = closeHour.toIntOrNull()
                    val closeM = closeMinute.toIntOrNull()

                    if (openH == null || openM == null || closeH == null || closeM == null) {
                        localErrorMessage = "Please enter valid opening hours."
                        return@AuthButton
                    }
                    if (openH !in 0..23 || openM !in 0..59 || closeH !in 0..23 || closeM !in 0..59) {
                        localErrorMessage = "Hours must be between 00:00 and 23:59."
                        return@AuthButton
                    }

                    val selectedServices = availableServices.filter { serviceSelected[it.serviceId] == true }
                    if (selectedServices.isEmpty()) {
                        localErrorMessage = "Please select at least one service."
                        return@AuthButton
                    }

                    var hasError = false
                    for (svc in selectedServices) {
                        val price = servicePrices[svc.serviceId]
                        val duration = serviceDurations[svc.serviceId]
                        if (price.isNullOrBlank() || duration.isNullOrBlank()) {
                            localErrorMessage = "Please fill in price and duration for ${svc.name}."
                            hasError = true
                            break
                        }
                        if (price.toFloatOrNull() == null || price.toFloat() <= 0f) {
                            localErrorMessage = "Invalid price for ${svc.name}."
                            hasError = true
                            break
                        }
                        if (duration.toIntOrNull() == null || duration.toInt() <= 0) {
                            localErrorMessage = "Invalid duration for ${svc.name}."
                            hasError = true
                            break
                        }
                    }
                    if (hasError) return@AuthButton

                    localErrorMessage = null

                    val serviceData = selectedServices.map { svc ->
                        Triple(
                            svc.serviceId,
                            (servicePrices[svc.serviceId] ?: "0").toFloat(),
                            (serviceDurations[svc.serviceId] ?: "0").toInt()
                        )
                    }

                    val openTime = openH * 100 + openM
                    val closeTime = closeH * 100 + closeM

                    viewModel.registerWashDetails(
                        washName = washName,
                        address = address,
                        city = city,
                        phone = phone,
                        openTime = openTime.toLong(),
                        closeTime = closeTime.toLong(),
                        serviceData = serviceData,
                        onAccessCodeGenerated = { },
                        onSuccess = onRegisterSuccess
                    )
                },
                modifier = Modifier.padding(horizontal = 40.dp)
            )

            Spacer(modifier = Modifier.height(40.dp))
        }
        }
    }
}

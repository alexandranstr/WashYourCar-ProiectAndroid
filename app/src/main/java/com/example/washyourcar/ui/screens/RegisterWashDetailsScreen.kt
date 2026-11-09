package com.example.washyourcar.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.washyourcar.data.entities.WashType
import com.example.washyourcar.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterWashDetailsScreen(
    viewModel: AuthViewModel,
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit,
    onContinue: (WashType, Int) -> Unit,
    onBackClick: () -> Unit
) {
    var washType by remember { mutableStateOf(WashType.NORMAL) }
    var typeMenuOpen by remember { mutableStateOf(false) }
    var washName by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var openHour by remember { mutableStateOf("08") }
    var openMinute by remember { mutableStateOf("00") }
    var closeHour by remember { mutableStateOf("20") }
    var closeMinute by remember { mutableStateOf("00") }
    var normalBays by remember { mutableStateOf("") }
    var premiumBays by remember { mutableStateOf("") }
    var automaticBays by remember { mutableStateOf("") }
    var localErrorMessage by remember { mutableStateOf<String?>(null) }
    var picked by remember { mutableStateOf<PickedLocation?>(null) }
    var showMap by remember { mutableStateOf(false) }

    if (showMap) {
        MapPickerDialog(
            initialLatitude = picked?.latitude,
            initialLongitude = picked?.longitude,
            onDismiss = { showMap = false },
            onPicked = { location ->
                picked = location
                // Pre-fill from the map, but don't overwrite what the owner already typed.
                if (address.isBlank()) location.address?.let { address = it }
                if (city.isBlank()) location.city?.let { city = it }
                showMap = false
            }
        )
    }

    val authState by viewModel.authState.collectAsState()
    LaunchedEffect(Unit) { viewModel.clearError() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AuthHeader(isDarkTheme = isDarkTheme, onThemeToggle = onThemeToggle, onBackClick = onBackClick)
        Spacer(modifier = Modifier.height(24.dp))
        Text("Car Wash Details", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Tell us about your car wash business",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(20.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ExposedDropdownMenuBox(
                expanded = typeMenuOpen,
                onExpandedChange = { typeMenuOpen = it }
            ) {
                OutlinedTextField(
                    value = washType.label,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Car wash type") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeMenuOpen) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = typeMenuOpen, onDismissRequest = { typeMenuOpen = false }) {
                    WashType.entries.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.label) },
                            onClick = {
                                washType = type
                                typeMenuOpen = false
                            }
                        )
                    }
                }
            }

            AuthTextField(label = "Car Wash Name", value = washName, onValueChange = { washName = it })
            OutlinedButton(onClick = { showMap = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(androidx.compose.material.icons.Icons.Default.Place, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(picked?.let { "Location set · change on map" } ?: "Place your car wash on the map")
            }
            AuthTextField(label = "Address", value = address, onValueChange = { address = it })
            AuthTextField(label = "City", value = city, onValueChange = { city = it })
            AuthTextField(label = "Phone Number", value = phone, onValueChange = { phone = it })

            when (washType) {
                WashType.NORMAL -> {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text("Opening Hours", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                AuthTextField(label = "Opens", value = openHour, onValueChange = { openHour = it }, modifier = Modifier.weight(1f))
                                Text(":", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                AuthTextField(label = "Minute", value = openMinute, onValueChange = { openMinute = it }, modifier = Modifier.weight(1f))
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                AuthTextField(label = "Closes", value = closeHour, onValueChange = { closeHour = it }, modifier = Modifier.weight(1f))
                                Text(":", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                AuthTextField(label = "Minute", value = closeMinute, onValueChange = { closeMinute = it }, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
                WashType.SELF -> {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Bays", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                AuthTextField(
                                    label = "Normal bays",
                                    value = normalBays,
                                    onValueChange = { normalBays = it.filter(Char::isDigit) },
                                    modifier = Modifier.weight(1f)
                                )
                                AuthTextField(
                                    label = "Premium bays",
                                    value = premiumBays,
                                    onValueChange = { premiumBays = it.filter(Char::isDigit) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
                WashType.AUTOMATIC -> {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Bays", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                            AuthTextField(
                                label = "Number of automatic bays",
                                value = automaticBays,
                                onValueChange = { automaticBays = it.filter(Char::isDigit) }
                            )
                        }
                    }
                }
            }

            val displayError = localErrorMessage ?: authState.errorMessage
            if (displayError != null) {
                Text(displayError, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.height(8.dp))

            AuthButton(
                text = "CONTINUE",
                loadingText = "SAVING...",
                isLoading = authState.isLoading,
                onClick = {
                    if (washName.isBlank() || address.isBlank() || city.isBlank() || phone.isBlank()) {
                        localErrorMessage = "Please fill in all car wash details."
                        return@AuthButton
                    }

                    var openTime = 0
                    var closeTime = 2359
                    if (washType == WashType.NORMAL) {
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
                        openTime = openH * 100 + openM
                        closeTime = closeH * 100 + closeM
                    }

                    val normal = normalBays.toIntOrNull() ?: 0
                    val premium = premiumBays.toIntOrNull() ?: 0
                    val automatic = automaticBays.toIntOrNull() ?: 0
                    if (washType == WashType.SELF && normal + premium <= 0) {
                        localErrorMessage = "Please enter the number of bays."
                        return@AuthButton
                    }
                    if (washType == WashType.AUTOMATIC && automatic <= 0) {
                        localErrorMessage = "Please enter the number of automatic bays."
                        return@AuthButton
                    }

                    localErrorMessage = null

                    viewModel.registerWashBasics(
                        washType = washType,
                        washName = washName,
                        address = address,
                        city = city,
                        phone = phone,
                        openTime = openTime.toLong(),
                        closeTime = closeTime.toLong(),
                        normalBays = normal,
                        premiumBays = premium,
                        automaticBays = automatic,
                        latitude = picked?.latitude,
                        longitude = picked?.longitude,
                        onSuccess = { washId -> onContinue(washType, washId) }
                    )
                }
            )

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
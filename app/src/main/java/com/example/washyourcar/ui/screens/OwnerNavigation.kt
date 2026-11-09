package com.example.washyourcar.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.washyourcar.data.dao.OwnerDao
import com.example.washyourcar.data.entities.Owner
import kotlinx.coroutines.CancellationException

@Composable
fun OwnerBottomBar(selectedRoute: String, onNavigate: (String) -> Unit, showActivity: Boolean = true) {
    NavigationBar {
        listOfNotNull(
            Triple("owner_home", "Overview", Icons.Default.Home),
            // Employee activity only exists for classic washes; self-service/automatic have no employees.
            if (showActivity) Triple("employee_activity", "Activity", Icons.Default.Groups) else null,
            Triple("car_wash_monitoring", "Car wash", Icons.Default.LocalCarWash),
            Triple("manage_account", "Account", Icons.Default.Person)
        ).forEach { (route, label, icon) ->
            NavigationBarItem(selected = selectedRoute == route, onClick = { onNavigate(route) },
                icon = { Icon(icon, null) }, label = { Text(label) })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerAccountScreen(uid: String, ownerDao: OwnerDao, isDarkTheme: Boolean,
    onThemeToggle: () -> Unit, onLogout: () -> Unit, bottomBar: @Composable () -> Unit) {
    var owner by remember(uid) { mutableStateOf<Owner?>(null) }
    var loading by remember(uid) { mutableStateOf(true) }
    var message by remember(uid) { mutableStateOf<String?>(null) }
    var refresh by remember { mutableIntStateOf(0) }
    LaunchedEffect(uid, refresh) {
        loading = true
        message = null
        try {
            owner = ownerDao.getById(uid)
            if (owner == null) message = "No owner profile is stored on this device."
        } catch (e: CancellationException) { throw e
        } catch (_: Exception) { message = "Couldn’t load account details. Please try again."
        } finally { loading = false }
    }
    Scaffold(
        topBar = { CenterAlignedTopAppBar(modifier = Modifier.washYourCarHeader(), title = { Text("Account") }) },
        bottomBar = bottomBar
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            owner?.let {
                Text("${it.firstName} ${it.lastName}", style = MaterialTheme.typography.headlineSmall)
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Owner account", style = MaterialTheme.typography.titleMedium)
                        Text("Email: ${it.email}")
                        Text("Phone: ${it.phoneNumber}")
                    }
                }
            }
            message?.let {
                Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedButton(onClick = { refresh++ }) { Text("Retry") }
            }
            OutlinedButton(onClick = onThemeToggle, modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)) {
                Icon(if (isDarkTheme) Icons.Default.WbSunny else Icons.Default.DarkMode, null)
                Spacer(Modifier.width(8.dp))
                Text(if (isDarkTheme) "Light mode" else "Dark mode")
            }
            Button(onClick = onLogout, modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB3261E), contentColor = Color.White),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, null)
                Spacer(Modifier.width(8.dp))
                Text("Sign out")
            }
        }
    }
}

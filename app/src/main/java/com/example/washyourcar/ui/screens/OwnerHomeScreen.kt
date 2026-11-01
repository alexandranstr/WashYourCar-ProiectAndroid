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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.washyourcar.ui.viewmodel.OwnerHomeViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerHomeScreen(
    firebaseUid: String,
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit,
    onLogout: () -> Unit,
    onNavigateToEmployeeActivity: () -> Unit,
    onNavigateToCarWashMonitoring: () -> Unit,
    onNavigateToManageAccount: () -> Unit,
    viewModel: OwnerHomeViewModel,
    bottomBar: @Composable () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val currentOwner by viewModel.currentOwner.collectAsState()
    val services by viewModel.services.collectAsState()

    LaunchedEffect(firebaseUid) {
        viewModel.initOwner(firebaseUid)
    }

    val menuTitleColor = if (isDarkTheme) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.background,
                modifier = Modifier.width(300.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "${currentOwner?.firstName ?: "Owner"} ${currentOwner?.lastName ?: ""}",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = menuTitleColor
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(text = "Email: ${currentOwner?.email ?: "N/A"}", fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground)
                            Text(text = "Phone: ${currentOwner?.phoneNumber ?: "N/A"}", fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground)
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))

                    Column(
                        modifier = Modifier.padding(vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        DrawerMenuItem(text = "Employee activity", onClick = {
                            scope.launch { drawerState.close() }
                            onNavigateToEmployeeActivity()
                        })
                        DrawerMenuItem(text = "Car wash monitoring", onClick = {
                            scope.launch { drawerState.close() }
                            onNavigateToCarWashMonitoring()
                        })
                        DrawerMenuItem(text = "Manage account", onClick = {
                            scope.launch { drawerState.close() }
                            onNavigateToManageAccount()
                        })
                    }

                    Spacer(modifier = Modifier.weight(1f))
                    DrawerMenuItem(text = "Logout", onClick = onLogout, textColor = MaterialTheme.colorScheme.error)
                }
            }
        }
    ) {
        Scaffold(
            bottomBar = bottomBar,
            topBar = {
                TopAppBar(
                    modifier = Modifier.washYourCarHeader(),
                    title = {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text(
                                text = "My Car Wash",
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp,
                                modifier = Modifier.padding(end = 48.dp)
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(32.dp))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)))
                            .padding(24.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Car Wash Details", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                            Spacer(modifier = Modifier.height(4.dp))

                            Text(text = "City: Galati", fontSize = 14.sp, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f))
                            Text(text = "Open: 08:00 - 22:00", fontSize = 14.sp, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f))

                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.4f))

                            if (services.isEmpty()) {
                                Text("No services active.", color = MaterialTheme.colorScheme.onPrimary)
                            } else {
                                services.forEach { detail ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = "• ", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                                        Text(text = detail.serviceName, fontSize = 14.sp, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f))
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DrawerMenuItem(text: String, onClick: () -> Unit, textColor: Color = MaterialTheme.colorScheme.onBackground) {
    Text(
        text = text,
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        color = textColor,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 24.dp, vertical = 16.dp)
    )
}
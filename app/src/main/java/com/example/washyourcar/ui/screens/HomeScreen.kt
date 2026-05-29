package com.example.washyourcar.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.washyourcar.data.entities.CarWash
import com.example.washyourcar.ui.viewmodel.ClientHomeViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    firebaseUid: String,
    onLogout: () -> Unit,
    onCarWashClick: (Int) -> Unit,
    viewModel: ClientHomeViewModel = viewModel()
) {
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val carWashes by viewModel.carWashes.collectAsState()
    val cities by viewModel.cities.collectAsState()
    val selectedCity by viewModel.selectedCity.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedFilters by viewModel.selectedFilters.collectAsState()
    val currentCustomer by viewModel.currentCustomer.collectAsState()
    val customerCar by viewModel.customerCar.collectAsState()

    var filterBoxVisible by remember { mutableStateOf(false) }
    var accountBoxExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(firebaseUid) {
        if (firebaseUid.isNotEmpty()) {
            viewModel.loadCustomerData(firebaseUid)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color(0xFFEDE7F6),
                modifier = Modifier.width(280.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            text = "Hello, ${currentCustomer?.firstName ?: "Client"}!",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF6A1B9A),
                            modifier = Modifier.padding(top = 16.dp)
                        )

                        HorizontalDivider(color = Color(0xFFD1C4E9))

                        Text(
                            text = "Account Details",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { accountBoxExpanded = !accountBoxExpanded }
                                .padding(vertical = 8.dp)
                        )

                        AnimatedVisibility(visible = accountBoxExpanded) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(8.dp),
                                elevation = CardDefaults.cardElevation(2.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(text = "Email: ${currentCustomer?.email ?: "N/A"}", fontSize = 14.sp)
                                    Text(text = "Phone: ${currentCustomer?.phoneNumber ?: "N/A"}", fontSize = 14.sp)
                                    if (customerCar != null) {
                                        Text(
                                            text = "Car: ${customerCar!!.model} [${customerCar!!.licensePlate}]",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    } else {
                                        Text(text = "No car registered", fontSize = 14.sp, color = Color.Gray)
                                    }
                                }
                            }
                        }

                        Text(
                            text = "My Appointments",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { }
                                .padding(vertical = 8.dp)
                        )
                    }

                    Button(
                        onClick = onLogout,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Logout", tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Logout", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Wash Your Car", color = Color.White, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF6A1B9A))
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Select City",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FilterChip(
                            selected = selectedCity == null,
                            onClick = { viewModel.loadAllCarWashes() },
                            label = { Text("All Cities") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF6A1B9A),
                                selectedLabelColor = Color.White
                            )
                        )

                        cities.forEach { city ->
                            FilterChip(
                                selected = selectedCity == city,
                                onClick = { viewModel.selectCity(city) },
                                label = { Text(city) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF6A1B9A),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.updateSearchQuery(it) },
                        placeholder = { Text("Search car wash...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF6A1B9A),
                            unfocusedBorderColor = Color(0xFFB28BFF)
                        ),
                        singleLine = true,
                        enabled = true
                    )

                    IconButton(
                        onClick = { filterBoxVisible = !filterBoxVisible },
                        enabled = true
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Filters",
                            tint = if (selectedFilters.isNotEmpty()) Color(0xFFFFB300) else Color(0xFF6A1B9A)
                        )
                    }
                }

                AnimatedVisibility(visible = filterBoxVisible) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            val filterOptions = listOf(
                                "Exterior Wash",
                                "Interior Cleaning",
                                "Detailing",
                                "Polish"
                            )

                            filterOptions.forEach { filter ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.toggleFilter(filter) }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = selectedFilters.contains(filter),
                                        onCheckedChange = { _ -> viewModel.toggleFilter(filter) },
                                        colors = CheckboxDefaults.colors(checkedColor = Color(0xFF6A1B9A))
                                    )
                                    Text(
                                        text = filter,
                                        fontSize = 14.sp,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                if (carWashes.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No car washes matched your criteria.",
                            color = Color.Gray,
                            fontSize = 15.sp
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(carWashes) { wash ->
                            CarWashCard(carWash = wash, onClick = { onCarWashClick(wash.carWashId) })
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarWashCard(carWash: CarWash, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5EDFF)),
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = com.example.washyourcar.R.drawable.spalatorie),
                    contentDescription = "Car Wash Image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            }

            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = carWash.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        val starsCount = carWash.rating.toInt()
                        repeat(starsCount) {
                            Text("★", color = Color(0xFFFFB300), fontSize = 14.sp)
                        }
                        repeat(5 - starsCount) {
                            Text("☆", color = Color(0xFFFFB300), fontSize = 14.sp)
                        }
                    }
                }

                Text(
                    text = "Mon–Sun  ${formatTime(carWash.openTime)} - ${formatTime(carWash.closeTime)}",
                    fontSize = 13.sp,
                    color = Color.DarkGray
                )

                Text(
                    text = "${carWash.address}, ${carWash.city}",
                    fontSize = 13.sp,
                    color = Color.DarkGray
                )
            }
        }
    }
}

private fun formatTime(time: Long): String {
    val hours = time / 100
    val minutes = time % 100
    return String.format("%02d:%02d", hours, minutes)
}

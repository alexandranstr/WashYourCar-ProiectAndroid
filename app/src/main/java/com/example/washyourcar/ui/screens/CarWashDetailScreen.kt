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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CarRental
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.washyourcar.data.entities.Review
import com.example.washyourcar.ui.viewmodel.CarWashDetailViewModel
import com.example.washyourcar.ui.viewmodel.WeatherViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarWashDetailScreen(
    carWashId: Int,
    firebaseUid: String,
    onBack: () -> Unit,
    viewModel: CarWashDetailViewModel,
    weatherViewModel: WeatherViewModel = viewModel()
) {
    val carWash by viewModel.carWash.collectAsState()
    val availableSlots by viewModel.availableSlots.collectAsState()
    val customerCar by viewModel.customerCar.collectAsState()
    val isSuccess by viewModel.isSuccess.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val randomTip by viewModel.randomTip.collectAsState()
    val carWashServiceNames by viewModel.carWashServiceNames.collectAsState()
    val availableServices by viewModel.availableServices.collectAsState()

    val reviews by viewModel.reviews.collectAsState()
    val computedRating by viewModel.computedRating.collectAsState()
    val reviewCount by viewModel.reviewCount.collectAsState()
    val customerExistingReview by viewModel.customerExistingReview.collectAsState()

    var bookingStep by remember { mutableStateOf(0) }
    var checkedSelection by remember { mutableStateOf<String?>(null) }
    var showBottomSheet by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var showReviewDialog by remember { mutableStateOf(false) }

    var selectedTimeRange by remember { mutableStateOf("Morning") }
    var selectedServiceIds by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var selectedSlotStartTime by remember { mutableStateOf<Long?>(null) }

    var reviewRating by remember { mutableStateOf(customerExistingReview?.rating ?: 0) }
    var reviewComment by remember { mutableStateOf(customerExistingReview?.comment ?: "") }
    var reviewSubmitted by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("WashYourCarPrefs", android.content.Context.MODE_PRIVATE) }
    val lastBookedWash = remember { sharedPrefs.getString("last_booked_wash_name", null) }

    val datePickerState = rememberDatePickerState(
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                return utcTimeMillis >= System.currentTimeMillis() - 24L * 60L * 60L * 1000L
            }
        }
    )

    val selectedDateMillis = datePickerState.selectedDateMillis
    val selectionKey = "${selectedServiceIds.sorted()}:$selectedDateMillis:$selectedTimeRange"
    LaunchedEffect(selectionKey) {
        selectedSlotStartTime = null
        checkedSelection = null
        viewModel.clearAvailableSlots()
    }

    LaunchedEffect(Unit) {
        weatherViewModel.fetchWeather("Brasov", "09981e4b7d1730200bcc5f422d1f6f85")
    }

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
            title = { Text("Appointment Confirmed!", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
            text = { Text("Your car wash slot has been successfully reserved. You can review or cancel it in your appointments section.", color = MaterialTheme.colorScheme.onSurface) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSuccessDialog = false
                        onBack()
                    }
                ) {
                    Text("OK", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.washYourCarHeader(),
                title = { Text(carWash?.name ?: "Details", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold) },
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
        if (carWash == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                LoadingCardSkeleton(lines = 4)
                LoadingCardSkeleton(lines = 3)
            }
        } else carWash?.let { wash ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                randomTip?.let { tip ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = tip.title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = tip.message,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                lastBookedWash?.let { washName ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Last time you scheduled at: ",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = washName,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }

                Card(
                    shape = WashYourCarCardShape,
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(6.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.secondary
                                    )
                                )
                            )
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Open daily: ${wash.openTime / 100}:00 - ${wash.closeTime / 100}:00",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${wash.address}, ${wash.city}",
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                            fontSize = 14.sp
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 4.dp))

                        Text(text = "Premium Services Available: ", color = MaterialTheme.colorScheme.onPrimary, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(4.dp))

                        if (carWashServiceNames.isEmpty()) {
                            Text(text = "No custom services listed.", color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f), fontSize = 12.sp)
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
                                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier.padding(horizontal = 2.dp)
                                            ) {
                                                Text(
                                                    text = serviceName,
                                                    color = MaterialTheme.colorScheme.onPrimary,
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

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Reviews", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text("($reviewCount)", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        TextButton(
                            onClick = {
                                reviewRating = customerExistingReview?.rating ?: 0
                                reviewComment = customerExistingReview?.comment ?: ""
                                showReviewDialog = true
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (customerExistingReview != null) "Update review" else "Write a review")
                        }
                    }

                    if (computedRating != null && reviewCount > 0) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = String.format("%.1f", computedRating!!),
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                                        val fullStars = computedRating!!.toInt()
                                        repeat(fullStars) {
                                            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(16.dp))
                                        }
                                        repeat(5 - fullStars) {
                                            Icon(Icons.Default.StarBorder, contentDescription = null, tint = Color(0xFFFFB300).copy(alpha = 0.4f), modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                                VerticalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f), modifier = Modifier.height(40.dp))
                                Text(
                                    text = "$reviewCount review${if (reviewCount != 1) "s" else ""}",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (reviews.isEmpty()) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.StarBorder, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("No reviews yet", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Be the first to share your experience!", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                            }
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            lazyItems(reviews) { review ->
                                ReviewCard(review = review)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    elevation = washYourCarButtonElevation(),
                    onClick = { bookingStep = 0; showBottomSheet = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = WashYourCarButtonShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text("Schedule appointment", color = MaterialTheme.colorScheme.onPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showReviewDialog) {
        AlertDialog(
            onDismissRequest = { showReviewDialog = false },
            title = {
                Column {
                    Text("Rate this car wash", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text(carWash?.name ?: "", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Your rating", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        repeat(5) { index ->
                            IconButton(onClick = { reviewRating = index + 1 }) {
                                Icon(
                                    imageVector = if (index < reviewRating) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = "${index + 1} star${if (index != 0) "s" else ""}",
                                    tint = Color(0xFFFFB300),
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = reviewComment,
                        onValueChange = { reviewComment = it },
                        placeholder = { Text("Share your experience (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                    )
                    if (reviewSubmitted) {
                        Text("Review submitted successfully!", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (reviewRating > 0) {
                            viewModel.submitReview(carWashId, firebaseUid, reviewRating, reviewComment)
                            reviewSubmitted = true
                            showReviewDialog = false
                        }
                    },
                    enabled = reviewRating > 0,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(if (customerExistingReview != null) "Update" else "Submit", color = MaterialTheme.colorScheme.onPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showReviewDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    val bookingScroll = rememberScrollState()
    LaunchedEffect(bookingStep) { bookingScroll.scrollTo(0) }
    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
                    .verticalScroll(bookingScroll),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("Schedule appointment", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)


                val totalDuration = availableServices
                    .filter { selectedServiceIds.contains(it.carWashService.serviceId) }
                    .sumOf { it.carWashService.duration }
                val totalPrice = availableServices
                    .filter { selectedServiceIds.contains(it.carWashService.serviceId) }
                    .sumOf { it.carWashService.price.toDouble() }


                BookingStepIndicator(
                    currentStep = bookingStep,
                    canOpenDate = selectedServiceIds.isNotEmpty(),
                    canOpenReview = selectedSlotStartTime != null && checkedSelection == selectionKey,
                    onStepSelected = { bookingStep = it }
                )
                if (bookingStep == 0) {
                Text("Select services", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                if (availableServices.isEmpty()) {
                    Text("No services available for this car wash.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                } else {
                    availableServices.forEach { serviceDetails ->
                        val serviceId = serviceDetails.carWashService.serviceId
                        val name = serviceDetails.serviceName
                        val duration = serviceDetails.carWashService.duration
                        val price = serviceDetails.carWashService.price

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedServiceIds = if (selectedServiceIds.contains(serviceId))
                                        selectedServiceIds - serviceId
                                    else
                                        selectedServiceIds + serviceId
                                }
                        ) {
                            Checkbox(
                                checked = selectedServiceIds.contains(serviceId),
                                onCheckedChange = { checked ->
                                    selectedServiceIds = if (checked)
                                        selectedServiceIds + serviceId
                                    else
                                        selectedServiceIds - serviceId
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = MaterialTheme.colorScheme.primary,
                                    checkmarkColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                            Column(modifier = Modifier.padding(start = 4.dp)) {
                                Text("$name ($duration min)", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                                Text("${price} RON", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                if (selectedServiceIds.isNotEmpty()) {
                    Text("Total duration: $totalDuration minutes", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                    Text("Total price: ${String.format("%.2f", totalPrice)} RON", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                }


                    Button(onClick = { bookingStep = 1 }, enabled = selectedServiceIds.isNotEmpty(), modifier = Modifier.fillMaxWidth()) { Text("Choose date and time") }
                }
                if (bookingStep == 1) {
                Text("Select date", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                DatePicker(
                    state = datePickerState,
                    title = null,
                    headline = null,
                    showModeToggle = false,
                    colors = DatePickerDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        headlineContentColor = MaterialTheme.colorScheme.onSurface,
                        weekdayContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        subheadContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        yearContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        currentYearContentColor = MaterialTheme.colorScheme.primary,
                        selectedYearContentColor = MaterialTheme.colorScheme.onPrimary,
                        selectedYearContainerColor = MaterialTheme.colorScheme.primary,
                        dayContentColor = MaterialTheme.colorScheme.onSurface,
                        selectedDayContentColor = MaterialTheme.colorScheme.onPrimary,
                        selectedDayContainerColor = MaterialTheme.colorScheme.primary,
                        todayContentColor = MaterialTheme.colorScheme.primary,
                        todayDateBorderColor = MaterialTheme.colorScheme.primary
                    )
                )




                Text("Select time range", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                var dropdownExpanded by remember { mutableStateOf(false) }

                Box {
                    Button(
                        elevation = washYourCarButtonElevation(),
                        onClick = { dropdownExpanded = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Text(selectedTimeRange, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    DropdownMenu(expanded = dropdownExpanded, onDismissRequest = { dropdownExpanded = false }) {
                        listOf("Morning", "Afternoon", "Evening").forEach { range ->
                            DropdownMenuItem(
                                text = { Text(range, color = MaterialTheme.colorScheme.onSurface) },
                                onClick = {
                                    selectedTimeRange = range
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Button(
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 1.dp, disabledElevation = 0.dp),
                    enabled = selectedDateMillis != null && selectedServiceIds.isNotEmpty(),
                    onClick = {
                        selectedDateMillis?.let {
                            val totalDur = availableServices
                                .filter { svc -> selectedServiceIds.contains(svc.carWashService.serviceId) }
                                .sumOf { svc -> svc.carWashService.duration }
                            selectedSlotStartTime = null
                            checkedSelection = selectionKey
                            viewModel.generateSlots(carWashId, it, totalDur, selectedTimeRange)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text("Check availability", color = MaterialTheme.colorScheme.onSecondary)
                }

                errorMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }

                if (availableSlots.isNotEmpty() && checkedSelection == selectionKey) {
                    Text("Available time slots", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.height(100.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(availableSlots) { timeMs ->
                            val timeString = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timeMs))
                            val isSelected = selectedSlotStartTime == timeMs

                            Button(
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 1.dp, disabledElevation = 0.dp),
                                onClick = { selectedSlotStartTime = timeMs },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Text(timeString, color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                            }
                        }
                    }
                }


                    Button(onClick = { bookingStep = 2 }, enabled = selectedSlotStartTime != null && checkedSelection == selectionKey, modifier = Modifier.fillMaxWidth()) { Text("Review appointment") }
                    TextButton(onClick = { bookingStep = 0 }) { Text("Back to services") }
                }
                if (bookingStep == 2) {
                    Text("Review your appointment", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(Icons.Default.Business, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                                Column {
                                    Text("Location", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(carWash?.name ?: "Car wash", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))

                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(Icons.Default.Category, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                                Column {
                                    Text("Services", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    availableServices.filter { selectedServiceIds.contains(it.carWashService.serviceId) }.forEach {
                                        Text(it.serviceName, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))

                            selectedSlotStartTime?.let {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Icon(Icons.Default.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                                    Column {
                                        Text("Date & Time", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(SimpleDateFormat("EEE, d MMM yyyy · HH:mm", Locale.getDefault()).format(Date(it)), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))

                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(Icons.Default.AccessTime, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                                Column {
                                    Text("Duration", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("$totalDuration minutes", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))

                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(Icons.Default.Category, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(22.dp))
                                Column {
                                    Text("Total Price", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${String.format("%.2f", totalPrice)} RON", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))

                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(Icons.Default.CarRental, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                                Column {
                                    Text("Vehicle", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(customerCar?.licensePlate ?: "Not available", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }

                    errorMessage?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                Button(
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 1.dp, disabledElevation = 0.dp),
                    onClick = {
                        val plate = customerCar?.licensePlate ?: "Test"
                        if (selectedSlotStartTime != null && selectedServiceIds.isNotEmpty()) {
                            val totalDur = availableServices
                                .filter { svc -> selectedServiceIds.contains(svc.carWashService.serviceId) }
                                .sumOf { svc -> svc.carWashService.duration }
                            viewModel.confirmAppointmentWithServices(
                                context = context,
                                customerId = firebaseUid,
                                carWashId = carWashId,
                                serviceIds = selectedServiceIds.toList(),
                                licensePlate = plate,
                                startTime = selectedSlotStartTime!!,
                                durationMinutes = totalDur
                            )
                        }
                    },
                    enabled = selectedSlotStartTime != null && selectedServiceIds.isNotEmpty() && checkedSelection == selectionKey,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth(),
                    shape = WashYourCarButtonShape
                ) {
                    Text("Confirm appointment", color = MaterialTheme.colorScheme.onPrimary)
                }

                    TextButton(onClick = { bookingStep = 1 }) { Text("Change date or time") }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun ReviewCard(review: Review) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(review.customerName, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                        repeat(review.rating) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(12.dp))
                        }
                        repeat(5 - review.rating) {
                            Icon(Icons.Default.StarBorder, contentDescription = null, tint = Color(0xFFFFB300).copy(alpha = 0.4f), modifier = Modifier.size(12.dp))
                        }
                    }
                }
                Text(
                    text = SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(review.timestamp)),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (review.comment.isNotEmpty()) {
                Text(review.comment, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}


package com.example.washyourcar.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items as ownerLazyItems
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.washyourcar.data.entities.Review
import com.example.washyourcar.ui.viewmodel.OwnerHomeViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    val currentOwner by viewModel.currentOwner.collectAsState()
    val carWash by viewModel.carWash.collectAsState()
    val services by viewModel.services.collectAsState()
    val reviews by viewModel.reviews.collectAsState()
    val computedRating by viewModel.computedRating.collectAsState()
    val reviewCount by viewModel.reviewCount.collectAsState()

    LaunchedEffect(firebaseUid) {
        viewModel.initOwner(firebaseUid)
    }

    val openTimeFormatted = carWash?.openTime?.let { time ->
        val h = time / 100
        val m = time % 100
        String.format("%02d:%02d", h, m)
    } ?: "N/A"
    val closeTimeFormatted = carWash?.closeTime?.let { time ->
        val h = time / 100
        val m = time % 100
        String.format("%02d:%02d", h, m)
    } ?: "N/A"

    Scaffold(
        bottomBar = bottomBar,
        topBar = {
            TopAppBar(
                modifier = Modifier.washYourCarHeader(),
                title = {
                    Text(
                        text = "My Car Wash",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)))
                            .padding(24.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = carWash?.name ?: "No Car Wash Yet",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))

                            if (carWash != null) {
                                Text(
                                    text = "Address: ${carWash!!.address}, ${carWash!!.city}",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                                )
                                Text(
                                    text = "Phone: ${carWash!!.phoneNumber}",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                                )
                                Text(
                                    text = "Open: $openTimeFormatted - $closeTimeFormatted",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                                )
                                Text(
                                    text = "Access Code: ${carWash!!.accessCode}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                                )
                            } else {
                                Text(
                                    text = "Complete your car wash setup to see details here.",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                                )
                            }

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

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Customer Reviews", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Text("($reviewCount)", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                            fontSize = 28.sp,
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
                                    modifier = Modifier.padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("No reviews yet", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                ownerLazyItems(reviews) { review ->
                                    OwnerReviewCard(review = review)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OwnerReviewCard(review: Review) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                    text = SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(review.timestamp)),
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

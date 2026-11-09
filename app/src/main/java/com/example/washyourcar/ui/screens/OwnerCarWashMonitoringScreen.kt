package com.example.washyourcar.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.view.Surface
import android.view.TextureView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
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

    var showAddDialog by remember { mutableStateOf(false) }
    var serviceToEdit by remember { mutableStateOf<CarWashServiceDetails?>(null) }
    var showLiveCameras by remember { mutableStateOf(false) }

    if (showLiveCameras) {
        AutomatedLiveCamerasOverlay(onDismiss = { showLiveCameras = false })
    } else {
        Scaffold(
            bottomBar = bottomBar,
            topBar = {
                TopAppBar(
                    modifier = Modifier.washYourCarHeader(),
                    title = { Text("Automated Car Wash Monitoring", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold) },
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
                    onClick = { showLiveCameras = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Icon(Icons.Default.Videocam, contentDescription = "Live Camera", tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("View Automated Live Cameras", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
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
}

private fun Context.findActivity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}

@Composable
fun AutomatedLiveCamerasOverlay(onDismiss: () -> Unit) {
    // MOCKUP: the second value is the NAME of a video file in app/src/main/res/raw (without .mp4).
    // Later this can come from the database (a stream URL) instead. Empty = no camera linked.
    val cameraList = listOf(
        Pair("Automated Bay 1", "cam_sydney"),
        Pair("Automated Bay 2", "cam_surveillance"),
        Pair("Automated Bay 3", "cam_tunnel"),
        Pair("Automated Bay 4", "") // no camera connected
    )

    var zoomedCameraData by remember { mutableStateOf<Pair<String, String>?>(null) }
    val isZoomed = zoomedCameraData != null

    // Android back button: leave fullscreen first, then leave the camera screen.
    BackHandler {
        if (zoomedCameraData != null) zoomedCameraData = null else onDismiss()
    }

    // Hide the status bar / navigation bar while a camera is fullscreen.
    val view = LocalView.current
    DisposableEffect(isZoomed) {
        val window = view.context.findActivity()?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        if (isZoomed) {
            controller?.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller?.hide(WindowInsetsCompat.Type.systemBars())
        }
        onDispose { controller?.show(WindowInsetsCompat.Type.systemBars()) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val zoomed = zoomedCameraData
        if (zoomed != null) {
            // ---------- FULLSCREEN CAMERA ----------
            if (zoomed.second.isNotEmpty()) {
                LocalVideoPlayer(videoName = zoomed.second, isMuted = false)
            } else {
                ConnectionLostView()
            }

            // Top bar drawn on top of the video
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopStart)
                    .background(Color.Black.copy(alpha = 0.55f))
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { zoomedCameraData = null }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text(
                    text = zoomed.first,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (zoomed.second.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .background(Color.Red.copy(alpha = 0.85f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text("LIVE", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }
        } else {
            // ---------- GRID OF CAMERAS ----------
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E1E1E))
                        .statusBarsPadding()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Automated Live Cameras",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize().padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(cameraList) { camera ->
                        Box(
                            modifier = Modifier
                                .aspectRatio(16f / 9f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.DarkGray)
                        ) {
                            if (camera.second.isNotEmpty()) {
                                LocalVideoPlayer(videoName = camera.second, isMuted = true)

                                Box(
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .background(Color.Red.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                        .align(Alignment.TopStart)
                                ) {
                                    Text("LIVE", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }

                                Box(
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                        .align(Alignment.BottomStart)
                                ) {
                                    Text(camera.first, color = Color.White, fontSize = 10.sp)
                                }
                            } else {
                                ConnectionLostView(compact = true)
                            }

                            // Transparent layer on top of everything: tap = open fullscreen
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clickable { zoomedCameraData = camera }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConnectionLostView(compact: Boolean = false) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.DarkGray),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Warning,
                contentDescription = "Connection lost",
                tint = Color.Yellow,
                modifier = Modifier.size(if (compact) 28.dp else 56.dp)
            )
            Spacer(modifier = Modifier.height(if (compact) 4.dp else 10.dp))
            Text(
                "CONNECTION LOST",
                color = Color.White,
                fontSize = if (compact) 12.sp else 22.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "No camera connected",
                color = Color.LightGray,
                fontSize = if (compact) 10.sp else 15.sp
            )
        }
    }
}

/**
 * Plays app/src/main/res/raw/<videoName>.mp4 in a loop, keeping the video's own proportions
 * (black bars if needed). Uses TextureView + MediaPlayer, which composes reliably inside Compose
 * (unlike VideoView, which could show a black screen after switching to fullscreen).
 */
@Composable
fun LocalVideoPlayer(videoName: String, isMuted: Boolean) {
    val context = LocalContext.current
    // Looks up the file in res/raw. 0 means the file is missing.
    val resId = remember(videoName) {
        context.resources.getIdentifier(videoName, "raw", context.packageName)
    }

    if (resId == 0) {
        ConnectionLostView()
        return
    }

    var videoRatio by remember(videoName) { mutableStateOf(16f / 9f) }
    val playerHolder = remember(videoName) { arrayOfNulls<MediaPlayer>(1) }

    DisposableEffect(videoName) {
        onDispose {
            playerHolder[0]?.release()
            playerHolder[0] = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { ctx ->
                TextureView(ctx).apply {
                    surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                        private var surface: Surface? = null

                        override fun onSurfaceTextureAvailable(st: SurfaceTexture, w: Int, h: Int) {
                            val mp = MediaPlayer()
                            try {
                                val afd = ctx.resources.openRawResourceFd(resId)
                                mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                                afd.close()
                                surface = Surface(st)
                                mp.setSurface(surface)
                                mp.isLooping = true
                                val volume = if (isMuted) 0f else 1f
                                mp.setVolume(volume, volume)
                                mp.setOnVideoSizeChangedListener { _, vw, vh ->
                                    if (vw > 0 && vh > 0) videoRatio = vw.toFloat() / vh.toFloat()
                                }
                                mp.setOnPreparedListener { it.start() }
                                mp.setOnErrorListener { _, _, _ -> true } // never crash on a bad video
                                mp.prepareAsync()
                                playerHolder[0] = mp
                            } catch (e: Exception) {
                                mp.release()
                            }
                        }

                        override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, w: Int, h: Int) {}

                        override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
                            playerHolder[0]?.release()
                            playerHolder[0] = null
                            surface?.release()
                            surface = null
                            return true
                        }

                        override fun onSurfaceTextureUpdated(st: SurfaceTexture) {}
                    }
                }
            },
            update = {
                val volume = if (isMuted) 0f else 1f
                try {
                    playerHolder[0]?.setVolume(volume, volume)
                } catch (_: IllegalStateException) {
                }
            },
            modifier = Modifier.aspectRatio(videoRatio)
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
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
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
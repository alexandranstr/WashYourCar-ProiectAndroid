package com.example.washyourcar.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import kotlin.math.sin

@Composable
fun RoleSelectionScreen(
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit,
    onClientSelected: () -> Unit,
    onEmployeeSelected: () -> Unit,
    onOwnerSelected: () -> Unit,
    onRegisterClientSelected: () -> Unit,
    onRegisterOwnerSelected: () -> Unit,
    onRegisterEmployeeSelected: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Box(modifier = Modifier.fillMaxSize()) {
        FloatingBubblesBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                AuthWaveHeader()

                Text(
                    text = "Wash Your Car!",
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.5f),
                            offset = Offset(2f, 4f),
                            blurRadius = 10f
                        )
                    ),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = 10.dp)
                )
            }

            if (isLandscape) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 30.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    RoleButton("Client", Icons.Default.DirectionsCar) { onClientSelected() }
                    RoleButton("Owner", Icons.Default.Person) { onOwnerSelected() }
                    RoleButton("Employee", Icons.Default.Settings) { onEmployeeSelected() }
                }
                Spacer(modifier = Modifier.weight(1f))
            } else {
                Spacer(modifier = Modifier.weight(0.5f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    RoleButton("Client", Icons.Default.DirectionsCar) { onClientSelected() }
                    RoleButton("Owner", Icons.Default.Person) { onOwnerSelected() }
                }

                Spacer(modifier = Modifier.height(30.dp))

                RoleButton("Employee", Icons.Default.Settings) { onEmployeeSelected() }

                Spacer(modifier = Modifier.weight(1f))
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = if (isLandscape) 16.dp else 60.dp)
            ) {
                Text(
                    "Create your account as:",
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Row(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            CircleShape
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    FooterLink("Client") { onRegisterClientSelected() }
                    Text("  •  ", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f), fontSize = 12.sp)
                    FooterLink("Owner") { onRegisterOwnerSelected() }
                    Text("  •  ", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f), fontSize = 12.sp)
                    FooterLink("Employee")  { onRegisterEmployeeSelected() }
                }
            }
        }

        androidx.compose.material3.IconButton(
            onClick = onThemeToggle,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 16.dp, top = 40.dp)
                .size(44.dp)
                .shadow(8.dp, CircleShape)
                .background(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                    CircleShape
                )
        ) {
            Icon(
                imageVector = if (isDarkTheme) Icons.Default.WbSunny else Icons.Default.DarkMode,
                contentDescription = "Schimbă tema",
                tint = Color.White,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}

val BUBBLE_DATA = listOf(
    BubbleData(0.1f, 0f, 8f, 12000),
    BubbleData(0.2f, 2f, 5f, 15000),
    BubbleData(0.35f, 1f, 10f, 10000),
    BubbleData(0.5f, 3f, 7f, 13000),
    BubbleData(0.65f, 0.5f, 9f, 11000),
    BubbleData(0.75f, 2.5f, 6f, 14000),
    BubbleData(0.85f, 1.5f, 11f, 9000),
    BubbleData(0.95f, 3.5f, 4f, 16000),
    BubbleData(0.05f, 4f, 7f, 12500),
    BubbleData(0.45f, 0.8f, 8f, 11500),
    BubbleData(0.55f, 2.2f, 5f, 13500),
    BubbleData(0.7f, 3.8f, 6f, 10500)
)

@Composable
fun FloatingBubblesBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "bubbles")
    val secondaryColor = MaterialTheme.colorScheme.secondary

    val bubblePhases = BUBBLE_DATA.mapIndexed { index, bubble ->
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(bubble.duration, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ), label = "bubble$index"
        ).value
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        BUBBLE_DATA.forEachIndexed { index, bubble ->
            val bubblePhase = bubblePhases[index]
            val x = bubble.xRatio * w + sin(bubblePhase * 4f + bubble.wobblePhase) * 20f
            val y = h - (bubblePhase * h)
            val radius = bubble.radius
            val alpha = (1f - bubblePhase) * 0.15f

            drawCircle(
                color = secondaryColor.copy(alpha = alpha),
                radius = radius,
                center = Offset(x, y)
            )

            drawCircle(
                color = Color.White.copy(alpha = alpha * 0.5f),
                radius = radius * 0.4f,
                center = Offset(x - radius * 0.2f, y - radius * 0.2f)
            )
        }
    }
}

data class BubbleData(
    val xRatio: Float,
    val wobblePhase: Float,
    val radius: Float,
    val duration: Int
)

@Composable
fun RoleButton(label: String, icon: ImageVector, onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "alpha"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp * pulseScale)
                    .clip(CircleShape)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = pulseAlpha)
                    )
            )

            Box(
                modifier = Modifier
                    .size(110.dp)
                    .shadow(12.dp, CircleShape, clip = false)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.secondary,
                                MaterialTheme.colorScheme.primary
                            )
                        )
                    )
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(50.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
        Text(
            text = label,
            modifier = Modifier.padding(top = 10.dp),
            style = TextStyle(
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
        )
    }
}

@Composable
fun FooterLink(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.primary,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.clickable { onClick() }
    )
}
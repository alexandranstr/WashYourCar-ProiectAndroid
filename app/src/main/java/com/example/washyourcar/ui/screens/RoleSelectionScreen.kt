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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration

@Composable
fun RoleSelectionScreen(
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit,
    onClientSelected: () -> Unit,
    onEmployeeSelected: () -> Unit,
    onOwnerSelected: () -> Unit,
    onRegisterClientSelected: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(MaterialTheme.colorScheme.primary)
        ) {
            GlowingOrb()

            Text(
                text = "Wash Your Car!",
                style = TextStyle(
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 35.sp,
                    fontWeight = FontWeight.Bold,
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.5f),
                        offset = Offset(2f, 2f),
                        blurRadius = 4f
                    )
                ),
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = 8.dp)
            )

            androidx.compose.material3.IconButton(
                onClick = onThemeToggle,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp)
            ) {
                Icon(
                    imageVector = if (isDarkTheme) Icons.Default.WbSunny else Icons.Default.DarkMode,
                    contentDescription = "Schimbă tema",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }

        Spacer(
            modifier = Modifier
                .height(2.dp)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f))
        )

        if (isLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                RoleButton("Client", Icons.Default.DirectionsCar) { onClientSelected() }
                RoleButton("Owner", Icons.Default.Person) { onOwnerSelected() }
                RoleButton("Employee", Icons.Default.Settings) { onEmployeeSelected() }
            }
            Spacer(modifier = Modifier.weight(1f))
        } else {
            Spacer(modifier = Modifier.weight(0.8f))

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
            modifier = Modifier.padding(bottom = if (isLandscape) 16.dp else 80.dp) // Pe landscape micșorăm distanța de jos ca să nu iasă din ecran
        ) {
            Text(
                "Create your account as:",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                fontSize = 14.sp
            )
            Row(modifier = Modifier.padding(top = 8.dp)) {
                FooterLink("Client") { onRegisterClientSelected() }
                Text(" • ", color = MaterialTheme.colorScheme.onBackground)
                FooterLink("Owner") { }
                Text(" • ", color = MaterialTheme.colorScheme.onBackground)
                FooterLink("Employee") { }
            }
        }
    }
}

@Composable
fun GlowingOrb() {
    val infiniteTransition = rememberInfiniteTransition(label = "particles")

    val animProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "progress"
    )

    val color1 = MaterialTheme.colorScheme.secondary
    val color2 = MaterialTheme.colorScheme.tertiary
    val color3 = MaterialTheme.colorScheme.primary

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        drawGlowingOrb(
            centerOffset = Offset(w * 0.2f + (animProgress * 100), h * 0.3f),
            radius = 150f,
            color = color1
        )

        drawGlowingOrb(
            centerOffset = Offset(w * 0.8f - (animProgress * 150), h * 0.6f),
            radius = 200f,
            color = color2
        )

        drawGlowingOrb(
            centerOffset = Offset(w * 0.5f, h * 0.2f + (animProgress * 80)),
            radius = 120f,
            color = Color.White
        )

        drawGlowingOrb(
            centerOffset = Offset(w * 0.1f, h * 0.8f),
            radius = 180f,
            color = color3
        )
    }
}

fun DrawScope.drawGlowingOrb(centerOffset: Offset, radius: Float, color: Color) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = 0.4f), Color.Transparent),
            center = centerOffset,
            radius = radius
        ),
        center = centerOffset,
        radius = radius
    )
}

@Composable
fun RoleButton(label: String, icon: ImageVector, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(110.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.secondary,
                            MaterialTheme.colorScheme.primary
                        )
                    )
                )
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(50.dp),
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
        Text(
            text = label,
            modifier = Modifier.padding(top = 8.dp),
            style = TextStyle(
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
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
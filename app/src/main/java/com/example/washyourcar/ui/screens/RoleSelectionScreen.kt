package com.example.washyourcar.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
@Composable
fun RoleSelectionScreen(onClientSelected: () -> Unit,
                         onEmployeeSelected: () -> Unit,
                         onOwnerSelected: () -> Unit,
                         onRegisterClientSelected: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(Color(0xFF6A1B9A)),
            contentAlignment = Alignment.Center
        ) {
            GlowingOrb()

            Text(
                text = "Wash Your Car!",
                style = TextStyle(
                    color = Color.White,
                    fontSize = 35.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }

        Spacer(modifier = Modifier.height(2.dp).fillMaxWidth().background(Color.Black))

        Spacer(modifier = Modifier.weight(0.8f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            RoleButton("Client", Icons.Default.DirectionsCar) {
                onClientSelected()
            }
            RoleButton("Owner", Icons.Default.Person) {
                onOwnerSelected()
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        RoleButton("Employee", Icons.Default.Settings) {
            onEmployeeSelected()
        }

        Spacer(modifier = Modifier.weight(1f))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(bottom = 24.dp)
        ) {
            Text("Create your account as:", color = Color.Gray, fontSize = 14.sp)
            Row(modifier = Modifier.padding(top = 8.dp)) {
                FooterLink("Client") { onRegisterClientSelected() }
                Text(" • ", color = Color.Black)
                FooterLink("Owner") {  }
                Text(" • ", color = Color.Black)
                FooterLink("Employee") {  }
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

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        drawGlowingOrb(
            centerOffset = Offset(w * 0.2f + (animProgress * 100), h * 0.3f),
            radius = 150f,
            color = Color(0xFF00CED1)
        )

        drawGlowingOrb(
            centerOffset = Offset(w * 0.8f - (animProgress * 150), h * 0.6f),
            radius = 200f,
            color = Color(0xFFB28BFF)
        )

        drawGlowingOrb(
            centerOffset = Offset(w * 0.5f, h * 0.2f + (animProgress * 80)),
            radius = 120f,
            color = Color.White
        )

        drawGlowingOrb(
            centerOffset = Offset(w * 0.1f, h * 0.8f),
            radius = 180f,
            color = Color(0xFFD0A8FF)
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
                        colors = listOf(Color(0xFFB28BFF), Color(0xFF5A0F84))
                    )
                )
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(50.dp),
                tint = Color.White
            )
        }
        Text(
            text = label,
            modifier = Modifier.padding(top = 8.dp),
            style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium)
        )
    }
}

@Composable
fun FooterLink(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        color = Color(0xFF6A1B9A),
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.clickable { onClick() }
    )
}

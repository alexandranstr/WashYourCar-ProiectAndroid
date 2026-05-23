package com.example.washyourcar.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import com.example.washyourcar.viewmodel.AuthViewModel

@Composable
fun LogInClientScreen(
    viewModel: AuthViewModel,
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val authState by viewModel.authState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .background(Color(0xFF6A1B9A)),
            contentAlignment = Alignment.Center
        ) {
            LoginGlowingOrb()
            Text(
                text = "Wash Your Car!",
                style = TextStyle(
                    color = Color.White,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }


        Spacer(modifier = Modifier.height(2.dp).fillMaxWidth().background(Color.Black))
        Spacer(modifier = Modifier.height(50.dp))

        Text(
            text = "Client Login",
            style = TextStyle(
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF6A1B9A)
            )
        )

        Spacer(modifier = Modifier.height(30.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 50.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ClearTextField(
                label = "Email Address",
                value = email,
                onValueChange = { email = it }
            )

            ClearTextField(
                label = "Password",
                value = password,
                onValueChange = { password = it },
                isPasswordInput = true
            )
        }

        if (authState.errorMessage != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = authState.errorMessage ?: "Unknown error",
                color = Color.Red,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 50.dp)
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        Button(
            onClick = {
                viewModel.login(email, password, onLoginSuccess)
            },
            enabled = !authState.isLoading,
            modifier = Modifier
                .fillMaxWidth(0.75f)
                .height(50.dp)
                .clip(RoundedCornerShape(25.dp)),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            contentPadding = PaddingValues()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color(0xFFB28BFF), Color(0xFF5A0F84)))),
                contentAlignment = Alignment.Center
            ) {
                if (authState.isLoading) {
                    Text("LOADING...", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                } else {
                    Text("LOGIN", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(modifier = Modifier.height(60.dp))

        Text(
            text = "Don't have an account? Sign up",
            color = Color.Gray,
            fontSize = 13.sp,
            modifier = Modifier
                .clickable { onNavigateToRegister() }
                .padding(bottom = 20.dp)
        )
    }
}

@Composable
fun ClearTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    isPasswordInput: Boolean = false
) {
    var passwordVisible by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp),
        placeholder = { Text(text = label, color = Color.LightGray) },
        shape = RoundedCornerShape(12.dp),
        singleLine = true,

        visualTransformation = if (isPasswordInput && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        trailingIcon = {
            if (isPasswordInput) {
                val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                val description = if (passwordVisible) "Hide password" else "Show password"

                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(imageVector = image, contentDescription = description, tint = Color(0xFF6A1B9A))
                }
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFF6A1B9A),
            unfocusedBorderColor = Color(0xFFB28BFF),
            focusedContainerColor = Color(0xFFF8F4FF),
            unfocusedContainerColor = Color.White,
            cursorColor = Color(0xFF6A1B9A)
        ),
        textStyle = TextStyle(fontSize = 16.sp, color = Color.Black)
    )
}


@Composable
fun LoginGlowingOrb() {
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

        drawLoginGlowingOrb(
            centerOffset = Offset(w * 0.2f + (animProgress * 100), h * 0.3f),
            radius = 150f,
            color = Color(0xFF00CED1)
        )

        drawLoginGlowingOrb(
            centerOffset = Offset(w * 0.8f - (animProgress * 150), h * 0.6f),
            radius = 200f,
            color = Color(0xFFB28BFF)
        )

        drawLoginGlowingOrb(
            centerOffset = Offset(w * 0.5f, h * 0.2f + (animProgress * 80)),
            radius = 120f,
            color = Color.White
        )

        drawLoginGlowingOrb(
            centerOffset = Offset(w * 0.1f, h * 0.8f),
            radius = 180f,
            color = Color(0xFFD0A8FF)
        )
    }
}

fun DrawScope.drawLoginGlowingOrb(centerOffset: Offset, radius: Float, color: Color) {
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
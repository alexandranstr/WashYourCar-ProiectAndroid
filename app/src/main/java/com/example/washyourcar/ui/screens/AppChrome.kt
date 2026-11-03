package com.example.washyourcar.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.sin

@Composable
fun Modifier.washYourCarHeader(): Modifier {
    return if (isSystemInDarkTheme()) this else shadow(
        elevation = 4.dp,
        clip = false
    )
}

@Composable
fun AuthHeader(
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit,
    onBackClick: () -> Unit
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

        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 16.dp, top = 40.dp)
                .size(40.dp)
                .shadow(4.dp, CircleShape)
                .background(Color.White.copy(alpha = 0.2f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Înapoi",
                tint = Color.White,
                modifier = Modifier.padding(6.dp)
            )
        }

        IconButton(
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

@Composable
fun AuthWaveHeader() {
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val TWO_PI = (2 * Math.PI).toFloat()
    val wavePhase1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = TWO_PI,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "wave1"
    )
    val wavePhase2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = TWO_PI,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "wave2"
    )
    val wavePhase3 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = TWO_PI,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "wave3"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    primaryColor.copy(alpha = 0.95f),
                    secondaryColor.copy(alpha = 0.9f)
                )
            )
        )

        drawAuthWave(
            phase = wavePhase1,
            amplitude = 20f,
            frequency = 3f,
            baseY = h * 0.75f,
            color = tertiaryColor.copy(alpha = 0.3f),
            w = w, h = h
        )

        drawAuthWave(
            phase = wavePhase2,
            amplitude = 25f,
            frequency = 2.5f,
            baseY = h * 0.82f,
            color = Color.White.copy(alpha = 0.2f),
            w = w, h = h
        )

        drawAuthWave(
            phase = wavePhase3,
            amplitude = 15f,
            frequency = 4f,
            baseY = h * 0.9f,
            color = primaryColor.copy(alpha = 0.4f),
            w = w, h = h
        )

        drawAuthWave(
            phase = wavePhase1 + Math.PI.toFloat(),
            amplitude = 10f,
            frequency = 5f,
            baseY = h * 0.95f,
            color = Color(0xFF000001).copy(alpha = 0.5f),
            w = w, h = h
        )
    }
}

fun DrawScope.drawAuthWave(
    phase: Float,
    amplitude: Float,
    frequency: Float,
    baseY: Float,
    color: Color,
    w: Float,
    h: Float
) {
    val path = Path().apply {
        moveTo(0f, baseY)
        val twoPi = (2 * Math.PI).toFloat()
        for (x in 0..w.toInt()) {
            val y = baseY + sin(x.toFloat() * twoPi * frequency / w + phase) * amplitude
            lineTo(x.toFloat(), y)
        }
        lineTo(w, h)
        lineTo(0f, h)
        close()
    }
    drawPath(path, color = color)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    isPasswordInput: Boolean = false,
    modifier: Modifier = Modifier
) {
    var passwordVisible by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp),
        placeholder = {
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                fontSize = 15.sp
            )
        },
        shape = RoundedCornerShape(16.dp),
        singleLine = true,
        visualTransformation = if (isPasswordInput && !passwordVisible) {
            androidx.compose.ui.text.input.PasswordVisualTransformation()
        } else {
            androidx.compose.ui.text.input.VisualTransformation.None
        },
        trailingIcon = {
            if (isPasswordInput) {
                val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = image,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = Color.Gray,
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            cursorColor = MaterialTheme.colorScheme.primary,
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            unfocusedLabelColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        ),
        textStyle = TextStyle(fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground)
    )
}

@Composable
fun AuthButton(
    text: String,
    loadingText: String,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = !isLoading,
        modifier = modifier
            .fillMaxWidth()
            .height(55.dp)
            .shadow(8.dp, RoundedCornerShape(18.dp), clip = false)
            .clip(RoundedCornerShape(18.dp)),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        contentPadding = PaddingValues()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary,
                            MaterialTheme.colorScheme.primary
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isLoading) loadingText else text,
                color = MaterialTheme.colorScheme.onPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
fun LoadingCardSkeleton(lines: Int = 3) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            repeat(lines) { index ->
                val width = if (index == 0) 0.62f else 0.9f
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .fillMaxWidth(width)
                        .height(if (index == 0) 20.dp else 14.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(6.dp)
                        )
                )
            }
        }
    }
}

val WashYourCarCardShape = RoundedCornerShape(12.dp)
val WashYourCarButtonShape = RoundedCornerShape(14.dp)

@Composable
fun washYourCarButtonElevation() = ButtonDefaults.buttonElevation(
    defaultElevation = 3.dp,
    pressedElevation = 1.dp,
    disabledElevation = 0.dp
)

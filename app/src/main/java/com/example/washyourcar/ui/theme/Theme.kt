package com.example.washyourcar.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = DarkWaterPrimary,
    secondary = DarkWaterSecondary,
    tertiary = DarkWaterTertiary,
    background = DarkWaterBackground,
    surface = DarkWaterSurface,
    onPrimary = Color.White,
    onBackground = DarkWaterText,
    onSurface = DarkWaterText
)

private val LightColorScheme = lightColorScheme(
    primary = WaterPrimary,
    secondary = WaterSecondary,
    tertiary = WaterTertiary,
    background = WaterBackground,
    surface = Color.White,
    onPrimary = Color.White,
    onBackground = WaterText,
    onSurface = WaterText
)

@Composable
fun WashYourCarTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
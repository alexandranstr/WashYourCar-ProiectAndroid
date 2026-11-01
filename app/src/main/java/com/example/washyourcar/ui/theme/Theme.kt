package com.example.washyourcar.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp
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
    onSecondary = Color.White, onTertiary = Color.White,
    primaryContainer = Color(0xFF283B52), onPrimaryContainer = Color(0xFFD9E9FF),
    secondaryContainer = Color(0xFF30363E), onSecondaryContainer = Color(0xFFE0E8F3),
    tertiaryContainer = Color(0xFF343434), onTertiaryContainer = Color(0xFFEAEAEA),
    surfaceVariant = Color(0xFF303030), onSurfaceVariant = Color(0xFFD0D0D0),
    outline = Color(0xFF929292), outlineVariant = Color(0xFF484848),
    surfaceTint = Color.Transparent,
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
    onSecondary = Color.White, onTertiary = Color(0xFF102B4B),
    primaryContainer = Color(0xFFDDEAFF), onPrimaryContainer = Color(0xFF173C6B),
    secondaryContainer = Color(0xFFE5EDF8), onSecondaryContainer = Color(0xFF263E5E),
    tertiaryContainer = Color(0xFFEDF0F5), onTertiaryContainer = Color(0xFF354359),
    surfaceVariant = Color(0xFFEBEFF5), onSurfaceVariant = Color(0xFF424D5C),
    outline = Color(0xFF747E8C), outlineVariant = Color(0xFFCBD2DC),
    surfaceTint = Color.Transparent,
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
        shapes = Shapes(small = RoundedCornerShape(10.dp), medium = RoundedCornerShape(16.dp), large = RoundedCornerShape(22.dp)),
        content = content
    )
}
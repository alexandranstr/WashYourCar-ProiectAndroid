package com.example.washyourcar.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp

@Composable
fun Modifier.washYourCarHeader(): Modifier {
    return if (isSystemInDarkTheme()) this else shadow(
        elevation = 4.dp,
        clip = false
    )
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

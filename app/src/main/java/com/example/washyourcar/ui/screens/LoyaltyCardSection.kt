package com.example.washyourcar.ui.screens

import AppDatabase
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CardMembership
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.washyourcar.data.entities.LoyaltyCard
import kotlinx.coroutines.launch

private val topUpPresets = listOf(50.0, 100.0, 200.0)

/**
 * Loyalty card shown in the client's Account tab. Top-up currently only updates the
 * local balance (test mode) – the real payment will go through Stripe later.
 */
@Composable
fun LoyaltyCardSection(firebaseUid: String) {
    if (firebaseUid.isEmpty()) return
    val dao = AppDatabase.getDatabase(LocalContext.current).loyaltyCardDao()
    val card by remember(firebaseUid) { dao.observeCard(firebaseUid) }.collectAsState(initial = null)
    val scope = rememberCoroutineScope()
    var showTopUp by remember { mutableStateOf(false) }

    val current = card
    if (current == null) {
        OutlinedButton(
            onClick = {
                scope.launch {
                    val number = (1..4).joinToString(" ") { (1000..9999).random().toString() }
                    dao.insert(LoyaltyCard(customerUid = firebaseUid, cardNumber = number))
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.CardMembership, null)
            Spacer(Modifier.width(8.dp))
            Text("Get a loyalty card")
        }
        return
    }

    Card(shape = RoundedCornerShape(20.dp), elevation = CardDefaults.cardElevation(6.dp), modifier = Modifier.fillMaxWidth()) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val onCard = MaterialTheme.colorScheme.onPrimary
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("LOYALTY CARD", color = onCard, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(1f))
                Icon(Icons.Default.CardMembership, null, tint = onCard)
            }
            Text(current.cardNumber, color = onCard, fontFamily = FontFamily.Monospace, fontSize = 18.sp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Column {
                    Text("Balance", color = onCard.copy(alpha = 0.8f), fontSize = 12.sp)
                    Text(String.format("%.2f RON", current.balance), color = onCard, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Points", color = onCard.copy(alpha = 0.8f), fontSize = 12.sp)
                    Text("${current.points}", color = onCard, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }
            Button(
                onClick = { showTopUp = true },
                colors = ButtonDefaults.buttonColors(containerColor = onCard, contentColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(8.dp))
                Text("Top up")
            }
        }
    }

    if (showTopUp) {
        TopUpDialog(
            onDismiss = { showTopUp = false },
            onConfirm = { amount ->
                scope.launch { dao.topUp(firebaseUid, amount) }
                showTopUp = false
            }
        )
    }
}

@Composable
private fun TopUpDialog(onDismiss: () -> Unit, onConfirm: (Double) -> Unit) {
    var amountText by remember { mutableStateOf("") }
    val amount = amountText.replace(',', '.').toDoubleOrNull()
    val valid = amount != null && amount >= 10.0 && amount <= 2000.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Top up loyalty card") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    topUpPresets.forEach { preset ->
                        FilterChip(
                            selected = amount == preset,
                            onClick = { amountText = preset.toInt().toString() },
                            label = { Text("${preset.toInt()} RON") }
                        )
                    }
                }
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' || c == ',' } },
                    label = { Text("Amount (RON)") },
                    supportingText = { Text("Between 10 and 2000 RON. Test mode – no real payment.") },
                    isError = amountText.isNotEmpty() && !valid,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = { amount?.let(onConfirm) }, enabled = valid) { Text("Pay") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

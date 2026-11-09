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

    // Top-up is paid with Google Pay (TEST environment); the balance is only credited after success.
    var pendingAmount by remember { mutableStateOf<Double?>(null) }
    var paymentMessage by remember { mutableStateOf<String?>(null) }
    val googlePay = com.example.washyourcar.payments.rememberGooglePay { result ->
        val amount = pendingAmount
        when (result) {
            is com.example.washyourcar.payments.GooglePay.Result.Success -> if (amount != null) {
                scope.launch { dao.topUp(firebaseUid, amount) }
                paymentMessage = String.format("Added %.2f RON to your card, paid with %s (test payment).", amount, result.cardDescription)
                showTopUp = false
            }
            com.example.washyourcar.payments.GooglePay.Result.Canceled -> Unit
            is com.example.washyourcar.payments.GooglePay.Result.Error -> paymentMessage = "Payment failed: ${result.message}"
        }
        pendingAmount = null
    }

    if (showTopUp) {
        TopUpDialog(
            googlePayAvailable = googlePay.available,
            onDismiss = { showTopUp = false },
            onConfirm = { amount ->
                pendingAmount = amount
                googlePay.pay(amount, "Loyalty card top-up")
            }
        )
    }

    paymentMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { paymentMessage = null },
            title = { Text(if (msg.startsWith("Payment failed")) "Payment failed" else "Top-up successful") },
            text = { Text(msg) },
            confirmButton = { TextButton(onClick = { paymentMessage = null }) { Text("OK") } }
        )
    }
}

@Composable
private fun TopUpDialog(googlePayAvailable: Boolean?, onDismiss: () -> Unit, onConfirm: (Double) -> Unit) {
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
                    supportingText = { Text("Between 10 and 2000 RON. Google Pay test mode – no real money.") },
                    isError = amountText.isNotEmpty() && !valid,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                when (googlePayAvailable) {
                    null -> Text("Checking Google Pay…")
                    false -> Text(
                        "Google Pay isn't available on this device. Sign in to a Google account and try again.",
                        color = MaterialTheme.colorScheme.error
                    )
                    true -> com.example.washyourcar.payments.GooglePayButton(
                        onClick = { amount?.let(onConfirm) },
                        enabled = valid,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

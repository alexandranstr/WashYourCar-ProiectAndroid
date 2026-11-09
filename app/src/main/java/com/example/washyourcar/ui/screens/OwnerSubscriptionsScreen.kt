package com.example.washyourcar.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalCarWash
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.washyourcar.data.entities.WashType

enum class BillingPeriod(val label: String, val suffix: String) {
    MONTHLY("Monthly", "/ month"),
    YEARLY("Yearly", "/ year")
}

/**
 * Subscription plan shown in the UI. [stripeMonthlyPriceId] / [stripeYearlyPriceId]
 * are placeholders to be replaced with real Stripe Price IDs when Stripe is wired in.
 */
data class SubscriptionPlan(
    val id: String,
    val name: String,
    val description: String,
    val features: List<String>,
    val monthlyPrice: Double,
    val yearlyPrice: Double,
    val icon: ImageVector = Icons.Default.LocalCarWash,
    val stripeMonthlyPriceId: String = "price_test_${id}_monthly",
    val stripeYearlyPriceId: String = "price_test_${id}_yearly"
) {
    fun priceFor(period: BillingPeriod) = if (period == BillingPeriod.MONTHLY) monthlyPrice else yearlyPrice
}

// Test prices (RON) – UI only, not connected to Stripe yet. Each wash type has a Normal and a Premium plan.
val testSubscriptionPlans: Map<WashType, List<SubscriptionPlan>> = mapOf(
    WashType.NORMAL to listOf(
        SubscriptionPlan(
            id = "classic_normal", name = "Classic – Normal",
            description = "List your classic hand-wash station in the app.",
            features = listOf("Online appointments", "Up to 5 employees", "Reviews & ratings"),
            monthlyPrice = 149.99, yearlyPrice = 1499.99
        ),
        SubscriptionPlan(
            id = "classic_premium", name = "Classic – Premium",
            description = "Everything in Normal, for bigger teams.",
            features = listOf("Unlimited employees", "Featured in search", "Employee activity reports"),
            monthlyPrice = 249.99, yearlyPrice = 2499.99
        )
    ),
    WashType.SELF to listOf(
        SubscriptionPlan(
            id = "self_normal", name = "Self-service – Normal",
            description = "List your self-service bays in the app.",
            features = listOf("Live queue per bay", "Up to 6 bays", "Reviews & ratings"),
            monthlyPrice = 79.99, yearlyPrice = 799.99
        ),
        SubscriptionPlan(
            id = "self_premium", name = "Self-service – Premium",
            description = "Everything in Normal, plus bookable premium bays.",
            features = listOf("Premium bay bookings", "Unlimited bays", "Featured in search"),
            monthlyPrice = 129.99, yearlyPrice = 1299.99
        )
    ),
    WashType.AUTOMATIC to listOf(
        SubscriptionPlan(
            id = "auto_normal", name = "Automatic – Normal",
            description = "List your automatic wash in the app.",
            features = listOf("Live queue per bay", "Online slot bookings", "Reviews & ratings"),
            monthlyPrice = 119.99, yearlyPrice = 1199.99
        ),
        SubscriptionPlan(
            id = "auto_premium", name = "Automatic – Premium",
            description = "Everything in Normal, for busy tunnels.",
            features = listOf("Unlimited bays", "Featured in search", "Booking reports"),
            monthlyPrice = 189.99, yearlyPrice = 1899.99
        )
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerSubscriptionsScreen(
    washType: WashType?,
    onBack: () -> Unit,
    // Only the plans for the owner's own wash type are shown.
    plans: List<SubscriptionPlan> = washType?.let { testSubscriptionPlans[it] }.orEmpty(),
    // Will be replaced with the Stripe checkout flow later.
    onSubscribe: (SubscriptionPlan, BillingPeriod) -> Unit = { _, _ -> }
) {
    var period by remember { mutableStateOf(BillingPeriod.MONTHLY) }
    var selectedPlanId by remember { mutableStateOf<String?>(null) }
    var confirmPlan by remember { mutableStateOf<SubscriptionPlan?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.washYourCarHeader(),
                title = { Text("Subscriptions", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Choose a plan", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                when (washType) {
                    null -> "Set up your car wash first to see the plans for it."
                    else -> "Plans for your ${washType.label.lowercase()} car wash"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                BillingPeriod.entries.forEachIndexed { index, p ->
                    SegmentedButton(
                        selected = period == p,
                        onClick = { period = p },
                        shape = SegmentedButtonDefaults.itemShape(index, BillingPeriod.entries.size)
                    ) { Text(p.label) }
                }
            }

            plans.forEach { plan ->
                SubscriptionPlanCard(
                    plan = plan,
                    period = period,
                    selected = selectedPlanId == plan.id,
                    onSelect = { selectedPlanId = plan.id },
                    onSubscribe = { confirmPlan = plan }
                )
            }

            Text(
                "Test mode – payments are not processed yet.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    confirmPlan?.let { plan ->
        AlertDialog(
            onDismissRequest = { confirmPlan = null },
            icon = { Icon(Icons.Default.Payment, null) },
            title = { Text("Confirm subscription") },
            text = {
                Text("${plan.name} – ${formatRon(plan.priceFor(period))} ${period.suffix}\n\nStripe payment will be available soon.")
            },
            confirmButton = {
                Button(onClick = {
                    onSubscribe(plan, period)
                    confirmPlan = null
                }) { Text("Pay") }
            },
            dismissButton = { TextButton(onClick = { confirmPlan = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun SubscriptionPlanCard(
    plan: SubscriptionPlan,
    period: BillingPeriod,
    selected: Boolean,
    onSelect: () -> Unit,
    onSubscribe: () -> Unit
) {
    Card(
        onClick = onSelect,
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(if (selected) 8.dp else 2.dp),
        border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(plan.icon, null, tint = MaterialTheme.colorScheme.primary)
                Text(plan.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            }
            Text(plan.description, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(formatRon(plan.priceFor(period)), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(6.dp))
                Text(period.suffix, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 3.dp))
            }
            plan.features.forEach { feature ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                    Text(feature, fontSize = 14.sp)
                }
            }
            Button(onClick = onSubscribe, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Text("Subscribe")
            }
        }
    }
}

private fun formatRon(value: Double) = String.format("%.2f RON", value)

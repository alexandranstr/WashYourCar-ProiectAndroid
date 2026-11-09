package com.example.washyourcar.payments

import android.app.Activity
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.wallet.IsReadyToPayRequest
import com.google.android.gms.wallet.PaymentData
import com.google.android.gms.wallet.PaymentDataRequest
import com.google.android.gms.wallet.PaymentsClient
import com.google.android.gms.wallet.Wallet
import com.google.android.gms.wallet.WalletConstants
import com.google.android.gms.wallet.contract.ApiTaskResult
import com.google.android.gms.wallet.contract.TaskResultContracts
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

/**
 * Google Pay API in the TEST environment: shows the real Google Pay sheet with test cards,
 * no money moves and no merchant account or server is needed.
 *
 * The "example" gateway returns a dummy token. To take real payments you'd switch to
 * ENVIRONMENT_PRODUCTION, register in the Google Pay & Wallet Console, and use a real
 * gateway (e.g. Stripe) whose server charges the token.
 */
object GooglePay {

    sealed interface Result {
        /** [cardDescription] e.g. "Visa •••• 1111" from the test card the user picked. */
        data class Success(val cardDescription: String) : Result
        data object Canceled : Result
        data class Error(val message: String) : Result
    }

    fun client(context: Context): PaymentsClient = Wallet.getPaymentsClient(
        context,
        Wallet.WalletOptions.Builder().setEnvironment(WalletConstants.ENVIRONMENT_TEST).build()
    )

    private val cardMethod: JSONObject
        get() = JSONObject()
            .put("type", "CARD")
            .put(
                "parameters", JSONObject()
                    .put("allowedAuthMethods", JSONArray(listOf("PAN_ONLY", "CRYPTOGRAM_3DS")))
                    .put("allowedCardNetworks", JSONArray(listOf("VISA", "MASTERCARD", "AMEX")))
            )
            .put(
                "tokenizationSpecification", JSONObject()
                    .put("type", "PAYMENT_GATEWAY")
                    .put(
                        "parameters", JSONObject()
                            .put("gateway", "example")
                            .put("gatewayMerchantId", "exampleGatewayMerchantId")
                    )
            )

    private fun baseRequest() = JSONObject().put("apiVersion", 2).put("apiVersionMinor", 0)

    fun isReadyToPayRequest(): IsReadyToPayRequest = IsReadyToPayRequest.fromJson(
        baseRequest().put("allowedPaymentMethods", JSONArray().put(cardMethod)).toString()
    )

    fun paymentRequest(amountRon: Double, label: String): PaymentDataRequest = PaymentDataRequest.fromJson(
        baseRequest()
            .put("allowedPaymentMethods", JSONArray().put(cardMethod))
            .put(
                "transactionInfo", JSONObject()
                    .put("totalPrice", String.format(Locale.US, "%.2f", amountRon))
                    .put("totalPriceStatus", "FINAL")
                    .put("totalPriceLabel", label)
                    .put("currencyCode", "RON")
                    .put("countryCode", "RO")
            )
            .put("merchantInfo", JSONObject().put("merchantName", "WashYourCar (test)"))
            .toString()
    )

    fun parse(result: ApiTaskResult<PaymentData>): Result = when (result.status.statusCode) {
        CommonStatusCodes.SUCCESS -> {
            val info = result.result?.toJson()?.let {
                JSONObject(it).getJSONObject("paymentMethodData").optJSONObject("info")
            }
            val network = info?.optString("cardNetwork").orEmpty()
            val last4 = info?.optString("cardDetails").orEmpty()
            Result.Success(listOf(network, if (last4.isNotEmpty()) "•••• $last4" else "").filter { it.isNotEmpty() }.joinToString(" ").ifEmpty { "Google Pay" })
        }
        CommonStatusCodes.CANCELED, Activity.RESULT_CANCELED -> Result.Canceled
        else -> Result.Error(result.status.statusMessage ?: "Google Pay error (${result.status.statusCode})")
    }
}

/**
 * Google Pay state for a screen: whether it's available on this device, and a pay() function
 * that opens the Google Pay sheet and reports back through [onResult].
 */
class GooglePayLauncher(val available: Boolean?, val pay: (amountRon: Double, label: String) -> Unit)

@Composable
fun rememberGooglePay(onResult: (GooglePay.Result) -> Unit): GooglePayLauncher {
    val context = LocalContext.current
    val client = remember { GooglePay.client(context) }
    var available by remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(client) {
        client.isReadyToPay(GooglePay.isReadyToPayRequest())
            .addOnCompleteListener { task -> available = task.isSuccessful && task.result == true }
    }
    val currentOnResult by rememberUpdatedState(onResult)
    val launcher = rememberLauncherForActivityResult(TaskResultContracts.GetPaymentDataResult()) { result ->
        currentOnResult(GooglePay.parse(result))
    }
    return remember(available) {
        GooglePayLauncher(available) { amount, label -> launcher.launch(client.loadPaymentData(GooglePay.paymentRequest(amount, label))) }
    }
}

/** Black "Pay with Google Pay" button following Google's button style. */
@Composable
fun GooglePayButton(onClick: () -> Unit, enabled: Boolean = true, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(100.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Black, contentColor = Color.White),
        modifier = modifier.height(48.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Pay with ", fontSize = 15.sp)
            Text("G", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4285F4))
            Text(" Pay", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

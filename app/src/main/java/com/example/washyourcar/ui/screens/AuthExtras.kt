package com.example.washyourcar.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.washyourcar.viewmodel.AuthViewModel

/** "Forgot password?" link for the login screens, plus the confirmation once the reset email is sent. */
@Composable
fun ForgotPasswordRow(email: String, viewModel: AuthViewModel, infoMessage: String?) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalAlignment = Alignment.End) {
        TextButton(onClick = { viewModel.sendPasswordReset(email) }) { Text("Forgot password?") }
        infoMessage?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

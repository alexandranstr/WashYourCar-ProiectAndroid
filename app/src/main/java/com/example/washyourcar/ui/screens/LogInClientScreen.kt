package com.example.washyourcar.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.washyourcar.viewmodel.AuthViewModel

@Composable
fun LogInClientScreen(
    viewModel: AuthViewModel,
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit,
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onBackClick: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var localErrorMessage by remember { mutableStateOf<String?>(null) }

    val authState by viewModel.authState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.clearError()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AuthHeader(isDarkTheme = isDarkTheme, onThemeToggle = onThemeToggle, onBackClick = onBackClick)

            Spacer(modifier = Modifier.height(50.dp))

            Text(
                text = "Client Login",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(30.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 50.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AuthTextField(
                    label = "Email Address",
                    value = email,
                    onValueChange = { email = it }
                )

                AuthTextField(
                    label = "Password",
                    value = password,
                    onValueChange = { password = it },
                    isPasswordInput = true
                )
            }

            val displayError = localErrorMessage ?: authState.errorMessage

            if (displayError != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = displayError,
                    color = Color.Red,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 50.dp)
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            AuthButton(
                text = "LOGIN",
                loadingText = "LOADING...",
                isLoading = authState.isLoading,
                onClick = {
                    if (email.isBlank() || password.isBlank()) {
                        localErrorMessage = "Please enter both email and password."
                    } else {
                        localErrorMessage = null
                        viewModel.loginClient(email, password, onLoginSuccess)
                    }
                },
                modifier = Modifier.fillMaxWidth(0.75f)
            )
            Spacer(modifier = Modifier.height(60.dp))

            Text(
                text = "Don't have an account? Sign up",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clickable { onNavigateToRegister() }
                    .padding(bottom = 20.dp)
            )
        }
    }
}
package com.example.washyourcar.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
fun RegisterOwnerScreen(
    viewModel: AuthViewModel,
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit,
    onRegisterSuccess: () -> Unit,
    onBackClick: () -> Unit
) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var localErrorMessage by remember { mutableStateOf<String?>(null) }

    val scrollState = rememberScrollState()
    val authState by viewModel.authState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.clearError()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AuthHeader(isDarkTheme = isDarkTheme, onThemeToggle = onThemeToggle, onBackClick = onBackClick)

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Create Owner Account",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(20.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AuthTextField(label = "First Name", value = firstName, onValueChange = { firstName = it })
                AuthTextField(label = "Last Name", value = lastName, onValueChange = { lastName = it })
                AuthTextField(label = "Email Address", value = email, onValueChange = { email = it })
                AuthTextField(label = "Phone Number", value = phone, onValueChange = { phone = it })
                AuthTextField(label = "Password", value = password, onValueChange = { password = it }, isPasswordInput = true)

                val displayError = localErrorMessage ?: authState.errorMessage

                if (displayError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = displayError,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                AuthButton(
                    text = "CREATE",
                    loadingText = "CREATING...",
                    isLoading = authState.isLoading,
                    onClick = {
                        if (firstName.isBlank() || lastName.isBlank() || email.isBlank() || phone.isBlank() || password.isBlank()) {
                            localErrorMessage = "All fields are required! Please fill in everything."
                        } else {
                            localErrorMessage = null
                            viewModel.registerOwner(
                                firstName = firstName,
                                lastName = lastName,
                                email = email,
                                phone = phone,
                                password = password,
                                onSuccess = onRegisterSuccess
                            )
                        }
                    }
                )

                Text(
                    text = "Already have an account? Sign In",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clickable { onBackClick() }
                        .padding(vertical = 16.dp)
                )

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}
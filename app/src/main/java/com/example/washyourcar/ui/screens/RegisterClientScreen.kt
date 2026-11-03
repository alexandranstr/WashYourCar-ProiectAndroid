package com.example.washyourcar.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
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
fun RegisterClientScreen(
    viewModel: AuthViewModel,
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit,
    onRegisterSuccess: () -> Unit,
    onBackClick: () -> Unit
) {
    var familyName by remember { mutableStateOf("") }
    var givenName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var carPlate by remember { mutableStateOf("") }
    var carModel by remember { mutableStateOf("") }
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
                text = "Create your account",
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
                AuthTextField(label = "Family Name", value = familyName, onValueChange = { familyName = it })
                AuthTextField(label = "Given Name", value = givenName, onValueChange = { givenName = it })
                AuthTextField(label = "Email Address", value = email, onValueChange = { email = it })
                AuthTextField(label = "Phone Number", value = phone, onValueChange = { phone = it })
                AuthTextField(label = "Password", value = password, onValueChange = { password = it }, isPasswordInput = true)

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
                Text(text = "Car Information", fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f), fontWeight = FontWeight.Bold)

                AuthTextField(label = "License Plate", value = carPlate, onValueChange = { carPlate = it })
                AuthTextField(label = "Car Model", value = carModel, onValueChange = { carModel = it })

                val displayError = localErrorMessage ?: authState.errorMessage

                if (displayError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = displayError,
                        color = Color.Red,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                AuthButton(
                    text = "CREATE",
                    loadingText = "CREATING...",
                    isLoading = authState.isLoading,
                    onClick = {
                        if (familyName.isBlank() || givenName.isBlank() || email.isBlank() ||
                            phone.isBlank() || password.isBlank() || carPlate.isBlank() || carModel.isBlank()) {
                            localErrorMessage = "All fields are required! Please fill in everything."
                        } else {
                            localErrorMessage = null
                            viewModel.register(
                                nume = familyName,
                                prenume = givenName,
                                email = email,
                                telefon = phone,
                                parola = password,
                                numarInmatriculare = carPlate,
                                modelMasina = carModel,
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
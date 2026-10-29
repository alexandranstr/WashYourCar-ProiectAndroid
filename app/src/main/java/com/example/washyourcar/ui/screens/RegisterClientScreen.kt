package com.example.washyourcar.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.washyourcar.viewmodel.AuthViewModel

@Composable
fun RegisterClientScreen(
    viewModel: AuthViewModel,
    isDarkTheme: Boolean,           // Adăugat parametrul temei
    onThemeToggle: () -> Unit,      // Adăugat funcția de toggle a temei
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header-ul simetric
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp) // Setat la 120dp pentru consistență
                .background(MaterialTheme.colorScheme.primary)
        ) {
            RegGlowingOrb()

            // Titlul centrat
            Text(
                text = "Wash Your Car!",
                style = TextStyle(
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 35.sp, // Setat la 35sp
                    fontWeight = FontWeight.Bold,
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.5f),
                        offset = Offset(2f, 2f),
                        blurRadius = 4f
                    )
                ),
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = 8.dp)
            )

            // Butonul de BACK (Stânga)
            androidx.compose.material3.IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Înapoi",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }

            // Butonul de TEMĂ (Dreapta)
            androidx.compose.material3.IconButton(
                onClick = onThemeToggle,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp)
            ) {
                Icon(
                    imageVector = if (isDarkTheme) Icons.Default.WbSunny else Icons.Default.DarkMode,
                    contentDescription = "Schimbă tema",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }

        Spacer(
            modifier = Modifier
                .height(2.dp)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f))
        )
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
            RegClearTextField(label = "Family Name", value = familyName, onValueChange = { familyName = it })
            RegClearTextField(label = "Given Name", value = givenName, onValueChange = { givenName = it })
            RegClearTextField(label = "Email Address", value = email, onValueChange = { email = it })
            RegClearTextField(label = "Phone Number", value = phone, onValueChange = { phone = it })
            RegClearTextField(label = "Password", value = password, onValueChange = { password = it }, isPasswordInput = true)

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
            Text(text = "Car Information", fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f), fontWeight = FontWeight.Bold)

            RegClearTextField(label = "License Plate", value = carPlate, onValueChange = { carPlate = it })
            RegClearTextField(label = "Car Model", value = carModel, onValueChange = { carModel = it })

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

            Button(
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
                },
                enabled = !authState.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp)
                    .clip(RoundedCornerShape(30.dp)),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.secondary,
                                    MaterialTheme.colorScheme.primary
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (authState.isLoading) {
                        Text("CREATING...", color = MaterialTheme.colorScheme.onPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Text("CREATE", color = MaterialTheme.colorScheme.onPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Text(
                text = "Already have an account? Sign In",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                fontSize = 13.sp,
                modifier = Modifier
                    .clickable { onBackClick() }
                    .padding(vertical = 16.dp)
            )

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegClearTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    isPasswordInput: Boolean = false
) {
    var passwordVisible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth().height(55.dp),
        placeholder = { Text(text = label, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f), fontSize = 11.sp) },
        shape = RoundedCornerShape(12.dp),
        singleLine = true,
        visualTransformation = if (isPasswordInput && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        trailingIcon = {
            if (isPasswordInput) {
                val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(imageVector = image, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.secondary,
            focusedContainerColor = MaterialTheme.colorScheme.background,
            unfocusedContainerColor = MaterialTheme.colorScheme.background,
            cursorColor = MaterialTheme.colorScheme.primary
        ),
        textStyle = TextStyle(fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground)
    )
}

@Composable
fun RegGlowingOrb() {
    val infiniteTransition = rememberInfiniteTransition(label = "orbs")
    val animProgress by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(8000, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
        label = "progress"
    )

    val color1 = MaterialTheme.colorScheme.secondary
    val color2 = MaterialTheme.colorScheme.tertiary

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        drawRegGlowingOrb(Offset(w * 0.2f + (animProgress * 80), h * 0.3f), 140f, color1)
        drawRegGlowingOrb(Offset(w * 0.8f - (animProgress * 100), h * 0.6f), 180f, color2) // Corectat: adăugat "-"
        drawRegGlowingOrb(Offset(w * 0.5f, h * 0.2f + (animProgress * 60)), 110f, Color.White)
    }
}

fun DrawScope.drawRegGlowingOrb(centerOffset: Offset, radius: Float, color: Color) {
    drawCircle(
        brush = Brush.radialGradient(colors = listOf(color.copy(alpha = 0.35f), Color.Transparent), center = centerOffset, radius = radius),
        center = centerOffset,
        radius = radius
    )
}
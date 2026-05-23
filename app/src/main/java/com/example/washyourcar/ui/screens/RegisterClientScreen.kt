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
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.washyourcar.viewmodel.AuthViewModel

@Composable
fun RegisterClientScreen(
    viewModel: AuthViewModel,
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

    val scrollState = rememberScrollState()
    val authState by viewModel.authState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .background(Color(0xFF6A1B9A)),
            contentAlignment = Alignment.Center
        ) {
            RegGlowingOrb()
            Text(text = "Wash your car!", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text(text = "Create your account", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF6A1B9A))
        Spacer(modifier = Modifier.height(20.dp))

        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RegClearTextField(label = "Family Name", value = familyName, onValueChange = { familyName = it })
            RegClearTextField(label = "Given Name", value = givenName, onValueChange = { givenName = it })
            RegClearTextField(label = "Email Address", value = email, onValueChange = { email = it })
            RegClearTextField(label = "Phone Number", value = phone, onValueChange = { phone = it })
            RegClearTextField(label = "Password", value = password, onValueChange = { password = it }, isPasswordInput = true)

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(thickness = 1.dp, color = Color(0xFFF0E6FF))
            Text(text = "Car Information", fontSize = 14.sp, color = Color.Gray, fontWeight = FontWeight.Bold)

            RegClearTextField(label = "License Plate", value = carPlate, onValueChange = { carPlate = it })
            RegClearTextField(label = "Car Model", value = carModel, onValueChange = { carModel = it })

            if (authState.errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = authState.errorMessage ?: "Error",
                    color = Color.Red,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
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
                        .background(Brush.verticalGradient(listOf(Color(0xFFB28BFF), Color(0xFF5A0F84)))),
                    contentAlignment = Alignment.Center
                ) {
                    if (authState.isLoading) {
                        Text("CREATING...", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Text("CREATE", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Text(
                text = "Already have an account? Sign In",
                color = Color.Gray,
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
        placeholder = { Text(text = label, color = Color.LightGray, fontSize = 11.sp) },
        shape = RoundedCornerShape(12.dp),
        singleLine = true,
        visualTransformation = if (isPasswordInput && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        trailingIcon = {
            if (isPasswordInput) {
                val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(imageVector = image, contentDescription = null, tint = Color(0xFF6A1B9A))
                }
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFF6A1B9A),
            unfocusedBorderColor = Color(0xFFB28BFF),
            focusedContainerColor = Color(0xFFF8F4FF),
            unfocusedContainerColor = Color.White
        )
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
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        drawRegGlowingOrb(Offset(w * 0.2f + (animProgress * 80), h * 0.3f), 140f, Color(0xFF00CED1))
        drawRegGlowingOrb(Offset(w * 0.8f - (animProgress * 100), h * 0.6f), 180f, Color(0xFFB28BFF))
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
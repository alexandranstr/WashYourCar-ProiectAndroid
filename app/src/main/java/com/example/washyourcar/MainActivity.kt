package com.example.washyourcar

import AppDatabase
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.washyourcar.ui.navigation.AuthenticationNavigation
import com.example.washyourcar.ui.theme.WashYourCarTheme
import com.example.washyourcar.viewmodel.AuthViewModel

class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Mirror local data to Cloud Firestore so it's permanent and shared across devices.
        com.example.washyourcar.data.CloudSync.start(AppDatabase.getDatabase(applicationContext))

        setContent {
            WashYourCarTheme {
                AuthenticationNavigation(authViewModel = authViewModel)
            }
        }
    }
}
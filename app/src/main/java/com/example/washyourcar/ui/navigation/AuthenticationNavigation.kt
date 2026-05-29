package com.example.washyourcar.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.washyourcar.viewmodel.AuthViewModel
import com.example.washyourcar.ui.screens.*
sealed class Screen(val route: String) {
    object RoleSelection : Screen("role_selection")
    object LoginClient : Screen("login_client")
    object RegisterClient : Screen("register_client")
    object Home : Screen("home")
}

@Composable
fun AuthenticationNavigation(authViewModel: AuthViewModel) {
    val navController = rememberNavController()

    val startDestination = if (authViewModel.isLoggedIn) Screen.Home.route else Screen.RoleSelection.route

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.RoleSelection.route) {
            RoleSelectionScreen(
                onClientSelected = {
                    navController.navigate(Screen.LoginClient.route)
                },
                onEmployeeSelected = {  },
                onOwnerSelected = {  },
                onRegisterClientSelected = {
                    navController.navigate(Screen.RegisterClient.route)
                }
            )
        }

        composable(Screen.LoginClient.route) {
            LogInClientScreen(
                viewModel = authViewModel,
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.RoleSelection.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(Screen.RegisterClient.route)
                }
            )
        }

        composable(Screen.RegisterClient.route) {
            RegisterClientScreen(
                viewModel = authViewModel,
                onRegisterSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.RoleSelection.route) { inclusive = true }
                    }
                },
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Home.route) {
            val currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""

            HomeScreen(
                firebaseUid = currentUid,
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Screen.RoleSelection.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                onCarWashClick = { carWashId ->

                }
            )
        }
    }
}
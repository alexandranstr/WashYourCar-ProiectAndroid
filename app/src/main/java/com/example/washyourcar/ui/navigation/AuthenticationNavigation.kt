package com.example.washyourcar.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.washyourcar.data.AppDatabase
import com.example.washyourcar.ui.screens.*
import com.example.washyourcar.ui.viewmodel.CarWashDetailViewModel
import com.example.washyourcar.viewmodel.*

sealed class Screen(val route: String) {
    object RoleSelection : Screen("role_selection")
    object LoginClient : Screen("login_client")
    object RegisterClient : Screen("register_client")
    object Home : Screen("home")
    object CarWashDetail : Screen("car_wash_detail/{carWashId}")
}

@Composable
fun AuthenticationNavigation(authViewModel: AuthViewModel) {
    val navController = rememberNavController()

    val context = LocalContext.current
    val database = AppDatabase.getDatabase(context)

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
                    navController.navigate("car_wash_detail/$carWashId")
                }
            )
        }

        composable(
            route = Screen.CarWashDetail.route,
            arguments = listOf(navArgument("carWashId") { type = NavType.IntType })
        ) { backStackEntry ->
            val carWashId = backStackEntry.arguments?.getInt("carWashId") ?: 0
            val currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""

            val detailViewModel: CarWashDetailViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return CarWashDetailViewModel(
                            carWashDao = database.carWashDao(),
                            appointmentDao = database.appointmentDao(),
                            carDao = database.carDao()
                        ) as T
                    }
                }
            )

            CarWashDetailScreen(
                carWashId = carWashId,
                firebaseUid = currentUid,
                onBack = { navController.popBackStack() },
                viewModel = detailViewModel
            )
        }
    }
}
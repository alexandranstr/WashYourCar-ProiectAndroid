package com.example.washyourcar.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.example.washyourcar.ui.theme.WashYourCarTheme
import com.example.washyourcar.ui.viewmodel.CarWashDetailViewModel
import com.example.washyourcar.viewmodel.*

sealed class Screen(val route: String) {
    object RoleSelection : Screen("role_selection")
    object LoginClient : Screen("login_client")
    object RegisterClient : Screen("register_client")
    object Home : Screen("home")
    object CarWashDetail : Screen("car_wash_detail/{carWashId}")

    object LoginOwner : Screen("login_owner")
    object RegisterOwner : Screen("register_owner")
    object OwnerHome : Screen("owner_home")
    object EmployeeActivity : Screen("employee_activity")
    object CarWashMonitoring : Screen("car_wash_monitoring")
    object ManageAccount : Screen("manage_account")

    object LoginEmployee : Screen("login_employee")
    object RegisterEmployee : Screen("register_employee")
    object EmployeeHome : Screen("employee_home")
}

@Composable
fun AuthenticationNavigation(authViewModel: AuthViewModel) {
    val defaultTheme = isSystemInDarkTheme()
    var isDarkTheme by remember { mutableStateOf(defaultTheme) }

    WashYourCarTheme(
        darkTheme = isDarkTheme,
        dynamicColor = false
    ) {
        val navController = rememberNavController()
        val context = LocalContext.current
        val database = AppDatabase.getDatabase(context)

        val startDestination = if (authViewModel.isLoggedIn) Screen.Home.route else Screen.RoleSelection.route

        NavHost(
            navController = navController,
            startDestination = startDestination,
            enterTransition = {
                slideInHorizontally(animationSpec = tween(400), initialOffsetX = { it }) + fadeIn(tween(400))
            },
            exitTransition = {
                slideOutHorizontally(animationSpec = tween(400), targetOffsetX = { -it }) + fadeOut(tween(400))
            },
            popEnterTransition = {
                slideInHorizontally(animationSpec = tween(400), initialOffsetX = { -it }) + fadeIn(tween(400))
            },
            popExitTransition = {
                slideOutHorizontally(animationSpec = tween(400), targetOffsetX = { it }) + fadeOut(tween(400))
            }
        ) {
            composable(Screen.RoleSelection.route) {
                RoleSelectionScreen(
                    isDarkTheme = isDarkTheme,
                    onThemeToggle = { isDarkTheme = !isDarkTheme },
                    onClientSelected = { navController.navigate(Screen.LoginClient.route) },
                    onEmployeeSelected = { navController.navigate(Screen.LoginEmployee.route) },
                    onOwnerSelected = { navController.navigate(Screen.LoginOwner.route) },
                    onRegisterClientSelected = { navController.navigate(Screen.RegisterClient.route) },
                    onRegisterOwnerSelected = { navController.navigate(Screen.RegisterOwner.route) },
                    onRegisterEmployeeSelected = { navController.navigate(Screen.RegisterEmployee.route) }
                )
            }

            composable(Screen.LoginClient.route) {
                LogInClientScreen(
                    viewModel = authViewModel,
                    isDarkTheme = isDarkTheme,
                    onThemeToggle = { isDarkTheme = !isDarkTheme },
                    onLoginSuccess = {
                        navController.navigate(Screen.Home.route) { popUpTo(Screen.RoleSelection.route) { inclusive = true } }
                    },
                    onNavigateToRegister = { navController.navigate(Screen.RegisterClient.route) },
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(Screen.RegisterClient.route) {
                RegisterClientScreen(
                    viewModel = authViewModel,
                    isDarkTheme = isDarkTheme,
                    onThemeToggle = { isDarkTheme = !isDarkTheme },
                    onRegisterSuccess = {
                        navController.navigate(Screen.Home.route) { popUpTo(Screen.RoleSelection.route) { inclusive = true } }
                    },
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(Screen.LoginOwner.route) {
                LogInOwnerScreen(
                    viewModel = authViewModel,
                    isDarkTheme = isDarkTheme,
                    onThemeToggle = { isDarkTheme = !isDarkTheme },
                    onLoginSuccess = {
                        navController.navigate(Screen.OwnerHome.route) { popUpTo(Screen.RoleSelection.route) { inclusive = true } }
                    },
                    onNavigateToRegister = { navController.navigate(Screen.RegisterOwner.route) },
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(Screen.RegisterOwner.route) {
                RegisterOwnerScreen(
                    viewModel = authViewModel,
                    isDarkTheme = isDarkTheme,
                    onThemeToggle = { isDarkTheme = !isDarkTheme },
                    onRegisterSuccess = {
                        navController.navigate(Screen.OwnerHome.route) { popUpTo(Screen.RoleSelection.route) { inclusive = true } }
                    },
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(Screen.OwnerHome.route) {
                val currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""

                val ownerHomeViewModel: com.example.washyourcar.ui.viewmodel.OwnerHomeViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return com.example.washyourcar.ui.viewmodel.OwnerHomeViewModel(
                                ownerDao = database.ownerDao(),
                                carWashServiceDao = database.carWashServiceDao()
                            ) as T
                        }
                    }
                )

                OwnerHomeScreen(
                    firebaseUid = currentUid,
                    isDarkTheme = isDarkTheme,
                    onThemeToggle = { isDarkTheme = !isDarkTheme },
                    onLogout = {
                        authViewModel.logout()
                        navController.navigate(Screen.RoleSelection.route) { popUpTo(Screen.OwnerHome.route) { inclusive = true } }
                    },
                    onNavigateToEmployeeActivity = { navController.navigate(Screen.EmployeeActivity.route) },
                    onNavigateToCarWashMonitoring = { navController.navigate(Screen.CarWashMonitoring.route) },
                    onNavigateToManageAccount = { navController.navigate(Screen.ManageAccount.route) },
                    viewModel = ownerHomeViewModel
                )
            }

            composable(Screen.EmployeeActivity.route) {
                val activityViewModel: com.example.washyourcar.ui.viewmodel.EmployeeActivityViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return com.example.washyourcar.ui.viewmodel.EmployeeActivityViewModel(
                                appointmentDao = database.appointmentDao()
                            ) as T
                        }
                    }
                )

                OwnerEmployeeActivityScreen(
                    onBack = { navController.popBackStack() },
                    viewModel = activityViewModel
                )
            }

            composable(Screen.CarWashMonitoring.route) {
                val monitoringViewModel: com.example.washyourcar.ui.viewmodel.CarWashMonitoringViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return com.example.washyourcar.ui.viewmodel.CarWashMonitoringViewModel(
                                carWashServiceDao = database.carWashServiceDao()
                            ) as T
                        }
                    }
                )

                OwnerCarWashMonitoringScreen(
                    onBack = { navController.popBackStack() },
                    viewModel = monitoringViewModel
                )
            }

            composable(Screen.ManageAccount.route) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Manage Account Screen")
                }
            }

            composable(Screen.Home.route) {
                val currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""

                HomeScreen(
                    firebaseUid = currentUid,
                    isDarkTheme = isDarkTheme,
                    onThemeToggle = { isDarkTheme = !isDarkTheme },
                    onLogout = {
                        authViewModel.logout()
                        navController.navigate(Screen.RoleSelection.route) { popUpTo(Screen.Home.route) { inclusive = true } }
                    },
                    onCarWashClick = { carWashId -> navController.navigate("car_wash_detail/$carWashId") }
                )
            }

            composable(Screen.EmployeeHome.route) {
                val currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""

                val employeeHomeViewModel: com.example.washyourcar.ui.viewmodel.EmployeeHomeViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return com.example.washyourcar.ui.viewmodel.EmployeeHomeViewModel(
                                employeeDao = database.employeeDao(),
                                appointmentDao = database.appointmentDao()
                            ) as T
                        }
                    }
                )

                EmployeeHomeScreen(
                    firebaseUid = currentUid,
                    isDarkTheme = isDarkTheme,
                    onThemeToggle = { isDarkTheme = !isDarkTheme },
                    onLogout = {
                        authViewModel.logout()
                        navController.navigate(Screen.RoleSelection.route) { popUpTo(Screen.EmployeeHome.route) { inclusive = true } }
                    },
                    viewModel = employeeHomeViewModel
                )
            }

            composable(Screen.LoginEmployee.route) {
                LogInEmployeeScreen(
                    viewModel = authViewModel,
                    isDarkTheme = isDarkTheme,
                    onThemeToggle = { isDarkTheme = !isDarkTheme },
                    onLoginSuccess = {
                        navController.navigate(Screen.EmployeeHome.route) { popUpTo(Screen.RoleSelection.route) { inclusive = true } }
                    },
                    onNavigateToRegister = { navController.navigate(Screen.RegisterEmployee.route) },
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(Screen.RegisterEmployee.route) {
                RegisterEmployeeScreen(
                    viewModel = authViewModel,
                    isDarkTheme = isDarkTheme,
                    onThemeToggle = { isDarkTheme = !isDarkTheme },
                    onRegisterSuccess = {
                        navController.navigate(Screen.EmployeeHome.route) { popUpTo(Screen.RoleSelection.route) { inclusive = true } }
                    },
                    onBackClick = { navController.popBackStack() }
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
                                carDao = database.carDao(),
                                carWashServiceDao = database.carWashServiceDao()
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
}
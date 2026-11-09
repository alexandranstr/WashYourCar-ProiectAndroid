package com.example.washyourcar.ui.navigation

import AppDatabase
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.washyourcar.data.entities.WashType
import com.example.washyourcar.ui.screens.CarWashDetailScreen
import com.example.washyourcar.ui.screens.EmployeeHomeScreen
import com.example.washyourcar.ui.screens.HomeScreen
import com.example.washyourcar.ui.screens.LogInClientScreen
import com.example.washyourcar.ui.screens.LogInEmployeeScreen
import com.example.washyourcar.ui.screens.LogInOwnerScreen
import com.example.washyourcar.ui.screens.OwnerAccountScreen
import com.example.washyourcar.ui.screens.OwnerBottomBar
import com.example.washyourcar.ui.screens.OwnerCarWashMonitoringScreen
import com.example.washyourcar.ui.screens.OwnerEmployeeActivityScreen
import com.example.washyourcar.ui.screens.OwnerHomeScreen
import com.example.washyourcar.ui.screens.RegisterClientScreen
import com.example.washyourcar.ui.screens.RegisterEmployeeScreen
import com.example.washyourcar.ui.screens.RegisterOwnerScreen
import com.example.washyourcar.ui.screens.RegisterWashDetailsScreen
import com.example.washyourcar.ui.screens.RegisterWashServicesScreen
import com.example.washyourcar.ui.screens.RoleSelectionScreen
import com.example.washyourcar.ui.theme.WashYourCarTheme
import com.example.washyourcar.ui.viewmodel.CarWashDetailViewModel
import com.example.washyourcar.viewmodel.AuthViewModel
import kotlinx.coroutines.CancellationException

sealed class Screen(val route: String) {
    object RoleSelection : Screen("role_selection")
    object LoginClient : Screen("login_client")
    object RegisterClient : Screen("register_client")
    object Home : Screen("home")
    object CarWashDetail : Screen("car_wash_detail/{carWashId}?washType={washType}")

    object LoginOwner : Screen("login_owner")
    object RegisterOwner : Screen("register_owner")
    object RegisterWashDetails : Screen("register_wash_details")
    object OwnerHome : Screen("owner_home")
    object EmployeeActivity : Screen("employee_activity")
    object CarWashMonitoring : Screen("car_wash_monitoring")
    object ManageAccount : Screen("manage_account")
    object OwnerSubscriptions : Screen("owner_subscriptions")

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
        val database = remember(context) { AppDatabase.getDatabase(context) }
        val navigateOwnerTab: (String) -> Unit = { route ->
            if (route == Screen.OwnerHome.route) {
                navController.popBackStack(Screen.OwnerHome.route, false)
            } else {
                navController.navigate(route) {
                    popUpTo(Screen.OwnerHome.route) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }

        // Type of the logged-in owner's wash (null until known / not an owner). Self-service and
        // automatic washes have no employees, so their owners don't get the Activity tab.
        var ownerWashType by remember { mutableStateOf<WashType?>(null) }
        val ownerHasEmployees = ownerWashType == null || ownerWashType == WashType.NORMAL
        val refreshOwnerWashType: suspend () -> Unit = {
            val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            ownerWashType = when {
                uid == null -> null
                database.carWashDao().getCarWashByOwnerId(uid) != null -> WashType.NORMAL
                database.selfWashDao().getSelfWashByOwnerId(uid) != null -> WashType.SELF
                database.autoWashDao().getAutoWashByOwnerId(uid) != null -> WashType.AUTOMATIC
                else -> null
            }
        }

        // null = still checking who is logged in. The NavHost is only created AFTER we know the
        // start destination, because changing startDestination later does NOT move the user.
        var startDestination by remember { mutableStateOf<String?>(null) }

        LaunchedEffect(Unit) {
            startDestination = try {
                when (authViewModel.getUserRole()) {
                    is AuthViewModel.UserRole.Client -> Screen.Home.route
                    is AuthViewModel.UserRole.Owner -> Screen.OwnerHome.route
                    is AuthViewModel.UserRole.Employee -> Screen.EmployeeHome.route
                    is AuthViewModel.UserRole.Unknown -> Screen.RoleSelection.route
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Never leave the app stuck on a blank screen if the role check fails.
                Screen.RoleSelection.route
            }
        }

        val start = startDestination

        if (start == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            NavHost(
                navController = navController,
                startDestination = start,
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
                            navController.navigate(Screen.RegisterWashDetails.route) { popUpTo(Screen.RegisterOwner.route) { inclusive = true } }
                        },
                        onBackClick = { navController.popBackStack() }
                    )
                }

                composable(Screen.RegisterWashDetails.route) {
                    RegisterWashDetailsScreen(
                        viewModel = authViewModel,
                        isDarkTheme = isDarkTheme,
                        onThemeToggle = { isDarkTheme = !isDarkTheme },
                        onContinue = { type, washId ->
                            navController.navigate("register_wash_services/${type.name}/$washId")
                        },
                        onBackClick = { navController.popBackStack() }
                    )
                }

                composable(
                    route = "register_wash_services/{washType}/{washId}",
                    arguments = listOf(
                        navArgument("washType") { type = NavType.StringType },
                        navArgument("washId") { type = NavType.IntType }
                    )
                ) { backStackEntry ->
                    val washType = WashType.valueOf(
                        backStackEntry.arguments?.getString("washType") ?: WashType.NORMAL.name
                    )
                    val washId = backStackEntry.arguments?.getInt("washId") ?: 0

                    RegisterWashServicesScreen(
                        viewModel = authViewModel,
                        washType = washType,
                        washId = washId,
                        isDarkTheme = isDarkTheme,
                        onThemeToggle = { isDarkTheme = !isDarkTheme },
                        onRegisterSuccess = {
                            navController.navigate(Screen.OwnerHome.route) {
                                popUpTo(Screen.RegisterWashDetails.route) { inclusive = true }
                            }
                        },
                        onBackClick = { navController.popBackStack() }
                    )
                }

                composable(Screen.OwnerHome.route) {
                    LaunchedEffect(Unit) { refreshOwnerWashType() }
                    val currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""

                    val ownerHomeViewModel: com.example.washyourcar.ui.viewmodel.OwnerHomeViewModel = viewModel(
                        factory = object : ViewModelProvider.Factory {
                            @Suppress("UNCHECKED_CAST")
                            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                return com.example.washyourcar.ui.viewmodel.OwnerHomeViewModel(
                                    ownerDao = database.ownerDao(),
                                    carWashDao = database.carWashDao(),
                                    autoWashDao = database.autoWashDao(),
                                    selfWashDao = database.selfWashDao(),
                                    carWashServiceDao = database.carWashServiceDao(),
                                    autoWashServiceDao = database.autoWashServiceDao(),
                                    selfWashServiceDao = database.selfWashServiceDao(),
                                    reviewDao = database.reviewDao(),
                                    appointmentDao = database.appointmentDao()
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
                        onNavigateToEmployeeActivity = { navigateOwnerTab(Screen.EmployeeActivity.route) },
                        onNavigateToCarWashMonitoring = { navigateOwnerTab(Screen.CarWashMonitoring.route) },
                        onNavigateToManageAccount = { navigateOwnerTab(Screen.ManageAccount.route) },
                        bottomBar = { OwnerBottomBar(Screen.OwnerHome.route, navigateOwnerTab, showActivity = ownerHasEmployees) },
                        viewModel = ownerHomeViewModel,
                        onNavigateToSubscriptions = { navController.navigate(Screen.OwnerSubscriptions.route) },
                        onSetupCarWash = { navController.navigate(Screen.RegisterWashDetails.route) }
                    )
                }

                composable(Screen.OwnerSubscriptions.route) {
                    LaunchedEffect(Unit) { if (ownerWashType == null) refreshOwnerWashType() }
                    com.example.washyourcar.ui.screens.OwnerSubscriptionsScreen(
                        washType = ownerWashType,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.EmployeeActivity.route) {
                    val currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""

                    val activityViewModel: com.example.washyourcar.ui.viewmodel.EmployeeActivityViewModel = viewModel(
                        factory = object : ViewModelProvider.Factory {
                            @Suppress("UNCHECKED_CAST")
                            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                return com.example.washyourcar.ui.viewmodel.EmployeeActivityViewModel(
                                    appointmentDao = database.appointmentDao(),
                                    carWashDao = database.carWashDao()
                                ) as T
                            }
                        }
                    )

                    LaunchedEffect(currentUid) {
                        activityViewModel.initOwner(currentUid)
                    }

                    OwnerEmployeeActivityScreen(
                        onBack = { navController.popBackStack() },
                        bottomBar = { OwnerBottomBar(Screen.EmployeeActivity.route, navigateOwnerTab, showActivity = ownerHasEmployees) },
                        viewModel = activityViewModel
                    )
                }

                composable(Screen.CarWashMonitoring.route) {
                    val currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""

                    val monitoringViewModel: com.example.washyourcar.ui.viewmodel.CarWashMonitoringViewModel = viewModel(
                        factory = object : ViewModelProvider.Factory {
                            @Suppress("UNCHECKED_CAST")
                            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                return com.example.washyourcar.ui.viewmodel.CarWashMonitoringViewModel(
                                    carWashServiceDao = database.carWashServiceDao(),
                                    carWashDao = database.carWashDao(),
                                    selfWashDao = database.selfWashDao(),
                                    selfWashServiceDao = database.selfWashServiceDao(),
                                    autoWashDao = database.autoWashDao(),
                                    autoWashServiceDao = database.autoWashServiceDao()
                                ) as T
                            }
                        }
                    )

                    LaunchedEffect(currentUid) {
                        monitoringViewModel.initOwner(currentUid)
                    }

                    OwnerCarWashMonitoringScreen(
                        onBack = { navController.popBackStack() },
                        bottomBar = { OwnerBottomBar(Screen.CarWashMonitoring.route, navigateOwnerTab, showActivity = ownerHasEmployees) },
                        viewModel = monitoringViewModel
                    )
                }

                composable(Screen.ManageAccount.route) {
                    OwnerAccountScreen(
                        uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "",
                        ownerDao = database.ownerDao(),
                        isDarkTheme = isDarkTheme,
                        onThemeToggle = { isDarkTheme = !isDarkTheme },
                        onLogout = {
                            authViewModel.logout()
                            navController.navigate(Screen.RoleSelection.route) {
                                popUpTo(Screen.OwnerHome.route) { inclusive = true }
                            }
                        },
                        bottomBar = { OwnerBottomBar(Screen.ManageAccount.route, navigateOwnerTab, showActivity = ownerHasEmployees) }
                    )
                }

                composable(Screen.Home.route)  {
                    val currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""

                    HomeScreen(
                        firebaseUid = currentUid,
                        isDarkTheme = isDarkTheme,
                        onThemeToggle = { isDarkTheme = !isDarkTheme },
                        onLogout = {
                            authViewModel.logout()
                            navController.navigate(Screen.RoleSelection.route) { popUpTo(Screen.Home.route) { inclusive = true } }
                        },
                        onWashClick = { type, id -> navController.navigate("car_wash_detail/$id?washType=${type.name}") }
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
                                    appointmentDao = database.appointmentDao(),
                                    carWashDao = database.carWashDao(),
                                    carWashServiceDao = database.carWashServiceDao()
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
                    arguments = listOf(
                        navArgument("carWashId") { type = NavType.IntType },
                        navArgument("washType") { type = NavType.StringType; defaultValue = WashType.NORMAL.name }
                    )
                ) { backStackEntry ->
                    val carWashId = backStackEntry.arguments?.getInt("carWashId") ?: 0
                    val washType = runCatching {
                        WashType.valueOf(backStackEntry.arguments?.getString("washType") ?: WashType.NORMAL.name)
                    }.getOrDefault(WashType.NORMAL)
                    val currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""

                    val detailViewModel: CarWashDetailViewModel = viewModel(
                        factory = object : ViewModelProvider.Factory {
                            @Suppress("UNCHECKED_CAST")
                            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                return CarWashDetailViewModel(
                                    carWashDao = database.carWashDao(),
                                    appointmentDao = database.appointmentDao(),
                                    carDao = database.carDao(),
                                    carWashServiceDao = database.carWashServiceDao(),
                                    reviewDao = database.reviewDao(),
                                    customerDao = database.customerDao(),
                                    selfWashDao = database.selfWashDao(),
                                    selfWashServiceDao = database.selfWashServiceDao(),
                                    autoWashDao = database.autoWashDao(),
                                    autoWashServiceDao = database.autoWashServiceDao()
                                ) as T
                            }
                        }
                    )

                    CarWashDetailScreen(
                        carWashId = carWashId,
                        firebaseUid = currentUid,
                        onBack = { navController.popBackStack() },
                        viewModel = detailViewModel,
                        washType = washType
                    )
                }
            }
        }
    }
}
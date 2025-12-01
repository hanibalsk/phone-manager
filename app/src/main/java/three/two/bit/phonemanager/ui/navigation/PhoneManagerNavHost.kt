package three.two.bit.phonemanager.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.delay
import three.two.bit.phonemanager.ui.alerts.AlertsScreen
import three.two.bit.phonemanager.ui.alerts.CreateAlertScreen
import three.two.bit.phonemanager.ui.auth.ForgotPasswordScreen
import three.two.bit.phonemanager.ui.auth.LoginScreen
import three.two.bit.phonemanager.ui.auth.RegisterScreen
import three.two.bit.phonemanager.ui.geofences.CreateGeofenceScreen
import three.two.bit.phonemanager.ui.geofences.GeofencesScreen
import three.two.bit.phonemanager.ui.group.GroupMembersScreen
import three.two.bit.phonemanager.ui.history.HistoryScreen
import three.two.bit.phonemanager.ui.home.HomeScreen
import three.two.bit.phonemanager.ui.home.HomeViewModel
import three.two.bit.phonemanager.ui.map.MapScreen
import three.two.bit.phonemanager.ui.movementevents.MovementEventsScreen
import three.two.bit.phonemanager.ui.permissions.PermissionViewModel
import three.two.bit.phonemanager.ui.registration.RegistrationScreen
import three.two.bit.phonemanager.ui.settings.SettingsScreen
import three.two.bit.phonemanager.ui.devices.DeviceDetailScreen
import three.two.bit.phonemanager.ui.devices.DeviceListScreen
import three.two.bit.phonemanager.ui.tripdetail.TripDetailScreen
import three.two.bit.phonemanager.ui.triphistory.TripHistoryScreen
import three.two.bit.phonemanager.ui.weather.WeatherScreen
import three.two.bit.phonemanager.ui.webhooks.CreateWebhookScreen
import three.two.bit.phonemanager.ui.webhooks.WebhooksScreen

sealed class Screen(val route: String) {
    // Story E9.11: Authentication screens
    object Login : Screen("login")
    object Register : Screen("register")
    object ForgotPassword : Screen("forgot_password")

    // Legacy registration (to be replaced by Login/Register flow)
    object Registration : Screen("registration")

    object Home : Screen("home")
    object GroupMembers : Screen("group_members")
    object Settings : Screen("settings")
    object Map : Screen("map")
    object History : Screen("history")
    object Alerts : Screen("alerts")
    object CreateAlert : Screen("create_alert")
    object Geofences : Screen("geofences")
    object CreateGeofence : Screen("create_geofence")
    object Webhooks : Screen("webhooks")
    object CreateWebhook : Screen("create_webhook")
    object Weather : Screen("weather")
    object TripHistory : Screen("trip_history")
    object TripDetail : Screen("trip_detail/{tripId}") {
        fun createRoute(tripId: String) = "trip_detail/$tripId"
    }
    object MovementEvents : Screen("movement_events")

    // Story E10.6: Device Management screens
    object DeviceList : Screen("device_list")
    object DeviceDetail : Screen("device_detail")
}

/**
 * Protects a route from being accessed in secret mode.
 * Redirects to Home screen when secret mode is active.
 */
@Composable
private fun SecretModeProtectedRoute(
    isSecretMode: Boolean,
    navController: NavController,
    content: @Composable () -> Unit,
) {
    if (isSecretMode) {
        LaunchedEffect(Unit) {
            navController.navigate(Screen.Home.route) {
                popUpTo(Screen.Home.route) { inclusive = true }
            }
        }
    } else {
        content()
    }
}

@Composable
fun PhoneManagerNavHost(
    permissionViewModel: PermissionViewModel,
    onRequestLocationPermission: () -> Unit,
    onRequestBackgroundPermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    isRegistered: Boolean,
    initialDestination: String? = null,
) {
    val navController = rememberNavController()

    // Secret mode state for navigation protection
    val homeViewModel: HomeViewModel = hiltViewModel()
    val isSecretMode by homeViewModel.isSecretModeEnabled.collectAsState()

    // Determine start destination based on registration status
    val startDestination = if (isRegistered) Screen.Home.route else Screen.Registration.route

    // Story E7.2: Handle deep link navigation from notification (AC E7.2.4)
    // Weather screen is top-level - back button closes app (hides notification)
    LaunchedEffect(initialDestination) {
        if (initialDestination != null && isRegistered) {
            // Wait for NavHost to be ready
            delay(100)
            navController.navigate(initialDestination) {
                // Clear back stack - weather is top screen, back closes app
                popUpTo(navController.graph.startDestinationId) {
                    inclusive = true
                }
                launchSingleTop = true
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        // Story E9.11: Authentication screens
        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                },
                onNavigateToForgotPassword = {
                    navController.navigate(Screen.ForgotPassword.route)
                },
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                onNavigateToLogin = {
                    navController.popBackStack()
                },
                onRegisterSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.ForgotPassword.route) {
            ForgotPasswordScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Legacy registration screen (will be removed when auth is integrated)
        composable(Screen.Registration.route) {
            RegistrationScreen(
                onRegistrationComplete = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Registration.route) { inclusive = true }
                    }
                },
            )
        }
        composable(Screen.Home.route) {
            HomeScreen(
                permissionViewModel = permissionViewModel,
                onRequestLocationPermission = onRequestLocationPermission,
                onRequestBackgroundPermission = onRequestBackgroundPermission,
                onRequestNotificationPermission = onRequestNotificationPermission,
                onNavigateToGroupMembers = {
                    navController.navigate(Screen.GroupMembers.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToMap = {
                    navController.navigate(Screen.Map.route)
                },
                onNavigateToHistory = {
                    navController.navigate(Screen.History.route)
                },
                onNavigateToAlerts = {
                    navController.navigate(Screen.Alerts.route)
                },
                onNavigateToGeofences = {
                    navController.navigate(Screen.Geofences.route)
                },
                onNavigateToWebhooks = {
                    navController.navigate(Screen.Webhooks.route)
                },
                onNavigateToWeather = {
                    navController.navigate(Screen.Weather.route)
                },
                onNavigateToTripHistory = {
                    navController.navigate(Screen.TripHistory.route)
                },
            )
        }
        composable(Screen.GroupMembers.route) {
            SecretModeProtectedRoute(isSecretMode, navController) {
                GroupMembersScreen(
                    onNavigateBack = { navController.popBackStack() },
                )
            }
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToTripHistory = {
                    navController.navigate(Screen.TripHistory.route)
                },
                onNavigateToMovementEvents = {
                    navController.navigate(Screen.MovementEvents.route)
                },
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route)
                },
                onNavigateToGroups = {
                    // TODO: Navigate to groups screen when E11.8 is implemented
                    navController.navigate(Screen.GroupMembers.route)
                },
                onNavigateToMyDevices = {
                    navController.navigate(Screen.DeviceList.route)
                }
            )
        }
        composable(Screen.Map.route) {
            SecretModeProtectedRoute(isSecretMode, navController) {
                MapScreen(
                    onNavigateBack = { navController.popBackStack() },
                )
            }
        }
        composable(Screen.History.route) {
            SecretModeProtectedRoute(isSecretMode, navController) {
                HistoryScreen(
                    onNavigateBack = { navController.popBackStack() },
                )
            }
        }
        composable(Screen.Alerts.route) {
            AlertsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCreate = {
                    navController.navigate(Screen.CreateAlert.route)
                },
            )
        }
        composable(Screen.CreateAlert.route) {
            CreateAlertScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable(Screen.Geofences.route) {
            GeofencesScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCreate = {
                    navController.navigate(Screen.CreateGeofence.route)
                },
            )
        }
        composable(Screen.CreateGeofence.route) {
            CreateGeofenceScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable(Screen.Webhooks.route) {
            WebhooksScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCreateWebhook = {
                    navController.navigate(Screen.CreateWebhook.route)
                },
            )
        }
        composable(Screen.CreateWebhook.route) {
            CreateWebhookScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable(Screen.Weather.route) {
            WeatherScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable(Screen.TripHistory.route) {
            SecretModeProtectedRoute(isSecretMode, navController) {
                TripHistoryScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onTripClick = { tripId ->
                        navController.navigate(Screen.TripDetail.createRoute(tripId))
                    },
                )
            }
        }
        composable(Screen.TripDetail.route) {
            SecretModeProtectedRoute(isSecretMode, navController) {
                TripDetailScreen(
                    onNavigateBack = { navController.popBackStack() },
                )
            }
        }
        composable(Screen.MovementEvents.route) {
            SecretModeProtectedRoute(isSecretMode, navController) {
                MovementEventsScreen(
                    onNavigateBack = { navController.popBackStack() },
                )
            }
        }

        // Story E10.6: Device Management screens (AC E10.6.1, E10.6.3)
        composable(Screen.DeviceList.route) {
            DeviceListScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDeviceDetail = {
                    navController.navigate(Screen.DeviceDetail.route)
                },
            )
        }
        composable(Screen.DeviceDetail.route) {
            DeviceDetailScreen(
                onNavigateBack = { navController.popBackStack() },
                onDeviceUnlinked = {
                    // If current device was unlinked, go back to home
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
            )
        }
    }
}

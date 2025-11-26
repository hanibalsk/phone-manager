package three.two.bit.phonemanager.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import three.two.bit.phonemanager.ui.alerts.AlertsScreen
import three.two.bit.phonemanager.ui.alerts.CreateAlertScreen
import three.two.bit.phonemanager.ui.group.GroupMembersScreen
import three.two.bit.phonemanager.ui.history.HistoryScreen
import three.two.bit.phonemanager.ui.home.HomeScreen
import three.two.bit.phonemanager.ui.map.MapScreen
import three.two.bit.phonemanager.ui.permissions.PermissionViewModel
import three.two.bit.phonemanager.ui.registration.RegistrationScreen
import three.two.bit.phonemanager.ui.settings.SettingsScreen

sealed class Screen(val route: String) {
    object Registration : Screen("registration")
    object Home : Screen("home")
    object GroupMembers : Screen("group_members")
    object Settings : Screen("settings")
    object Map : Screen("map")
    object History : Screen("history")
    object Alerts : Screen("alerts")
    object CreateAlert : Screen("create_alert")
}

@Composable
fun PhoneManagerNavHost(
    permissionViewModel: PermissionViewModel,
    onRequestLocationPermission: () -> Unit,
    onRequestBackgroundPermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    isRegistered: Boolean,
) {
    val navController = rememberNavController()

    // Determine start destination based on registration status
    val startDestination = if (isRegistered) Screen.Home.route else Screen.Registration.route

    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
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
            )
        }
        composable(Screen.GroupMembers.route) {
            GroupMembersScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable(Screen.Map.route) {
            MapScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable(Screen.History.route) {
            HistoryScreen(
                onNavigateBack = { navController.popBackStack() },
            )
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
    }
}

package com.phonemanager.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.phonemanager.ui.home.HomeScreen
import com.phonemanager.ui.permissions.PermissionViewModel

sealed class Screen(val route: String) {
    object Home : Screen("home")
    // Future: Add Settings screen
}

@Composable
fun PhoneManagerNavHost(
    permissionViewModel: PermissionViewModel,
    onRequestLocationPermission: () -> Unit,
    onRequestBackgroundPermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                permissionViewModel = permissionViewModel,
                onRequestLocationPermission = onRequestLocationPermission,
                onRequestBackgroundPermission = onRequestBackgroundPermission,
                onRequestNotificationPermission = onRequestNotificationPermission,
            )
        }
        // Future: Add Settings screen route
    }
}

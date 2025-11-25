package three.two.bit.phonemanager.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import three.two.bit.phonemanager.ui.group.GroupMembersScreen
import three.two.bit.phonemanager.ui.home.HomeScreen
import three.two.bit.phonemanager.ui.permissions.PermissionViewModel
import three.two.bit.phonemanager.ui.registration.RegistrationScreen

sealed class Screen(val route: String) {
    object Registration : Screen("registration")
    object Home : Screen("home")
    object GroupMembers : Screen("group_members")
    // Future: Add Settings screen
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
            )
        }
        composable(Screen.GroupMembers.route) {
            GroupMembersScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }
        // Future: Add Settings screen route
    }
}

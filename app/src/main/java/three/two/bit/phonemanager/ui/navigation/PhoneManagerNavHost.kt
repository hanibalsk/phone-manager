package three.two.bit.phonemanager.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.delay
import three.two.bit.phonemanager.ui.admin.AdminGeofenceScreen
import three.two.bit.phonemanager.ui.admin.AdminUsersScreen
import three.two.bit.phonemanager.ui.admin.BulkSettingsScreen
import three.two.bit.phonemanager.ui.admin.DeviceSettingsScreen
import three.two.bit.phonemanager.ui.admin.MemberDevicesScreen
import three.two.bit.phonemanager.ui.admin.SettingsHistoryScreen
import three.two.bit.phonemanager.ui.admin.SettingsTemplateScreen
import three.two.bit.phonemanager.ui.admin.UserHomeScreen
import three.two.bit.phonemanager.ui.admin.UserLocationMapScreen
import three.two.bit.phonemanager.ui.alerts.AlertsScreen
import three.two.bit.phonemanager.ui.alerts.CreateAlertScreen
import three.two.bit.phonemanager.ui.auth.ForgotPasswordScreen
import three.two.bit.phonemanager.ui.auth.LoginScreen
import three.two.bit.phonemanager.ui.auth.RegisterScreen
import three.two.bit.phonemanager.ui.devices.DeviceDetailScreen
import three.two.bit.phonemanager.ui.devices.DeviceListScreen
import three.two.bit.phonemanager.ui.enrollment.EnrollmentQRScannerScreen
import three.two.bit.phonemanager.ui.enrollment.EnrollmentScreen
import three.two.bit.phonemanager.ui.enrollment.EnrollmentSuccessScreen
import three.two.bit.phonemanager.ui.enrollment.SetupScreen
import three.two.bit.phonemanager.ui.geofences.CreateGeofenceScreen
import three.two.bit.phonemanager.ui.geofences.GeofencesScreen
import three.two.bit.phonemanager.ui.group.GroupMembersScreen
import three.two.bit.phonemanager.ui.groups.GroupDetailScreen
import three.two.bit.phonemanager.ui.groups.GroupListScreen
import three.two.bit.phonemanager.ui.groups.GroupMigrationScreen
import three.two.bit.phonemanager.ui.groups.InviteMembersScreen
import three.two.bit.phonemanager.ui.groups.JoinGroupScreen
import three.two.bit.phonemanager.ui.groups.ManageMembersScreen
import three.two.bit.phonemanager.ui.groups.PendingInvitesScreen
import three.two.bit.phonemanager.ui.groups.QRScannerScreen
import three.two.bit.phonemanager.ui.history.HistoryScreen
import three.two.bit.phonemanager.ui.home.HomeScreen
import three.two.bit.phonemanager.ui.home.HomeViewModel
import three.two.bit.phonemanager.ui.map.MapScreen
import three.two.bit.phonemanager.ui.movementevents.MovementEventsScreen
import three.two.bit.phonemanager.ui.permissions.PermissionViewModel
import three.two.bit.phonemanager.ui.registration.RegistrationScreen
import three.two.bit.phonemanager.ui.settings.SettingsScreen
import three.two.bit.phonemanager.ui.tripdetail.TripDetailScreen
import three.two.bit.phonemanager.ui.triphistory.TripHistoryScreen
import three.two.bit.phonemanager.ui.unlock.UnlockRequestsScreen
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
    object DeviceDetail : Screen("device_detail/{deviceId}") {
        fun createRoute(deviceId: String) = "device_detail/$deviceId"
    }

    // Story E11.8: Group Management screens
    object GroupList : Screen("group_list")
    object GroupDetail : Screen("group_detail/{groupId}") {
        fun createRoute(groupId: String) = "group_detail/$groupId"
    }
    object ManageMembers : Screen("manage_members/{groupId}") {
        fun createRoute(groupId: String) = "manage_members/$groupId"
    }

    // Story E11.9: Invite screens
    object InviteMembers : Screen("invite_members/{groupId}") {
        fun createRoute(groupId: String) = "invite_members/$groupId"
    }
    object PendingInvites : Screen("pending_invites/{groupId}") {
        fun createRoute(groupId: String) = "pending_invites/$groupId"
    }
    object JoinGroup : Screen("join_group?code={code}") {
        fun createRoute(code: String? = null) = if (code != null) "join_group?code=$code" else "join_group"
    }
    object QRScanner : Screen("qr_scanner")

    // Story UGM-4.3: Group Migration screen
    object GroupMigration : Screen("group_migration/{groupId}") {
        fun createRoute(groupId: String) = "group_migration/$groupId"
    }

    // Story E12.7: Admin Settings Management screens
    object MemberDevices : Screen("member_devices/{groupId}") {
        fun createRoute(groupId: String) = "member_devices/$groupId"
    }
    object AdminDeviceSettings : Screen("admin_device_settings/{deviceId}") {
        fun createRoute(deviceId: String) = "admin_device_settings/$deviceId"
    }
    object AdminSettingsHistory : Screen("admin_settings_history/{deviceId}") {
        fun createRoute(deviceId: String) = "admin_settings_history/$deviceId"
    }
    object BulkSettings : Screen("bulk_settings/{deviceIds}") {
        fun createRoute(deviceIds: List<String>) = "bulk_settings/${deviceIds.joinToString(",")}"
    }
    object SettingsTemplates : Screen("settings_templates")

    // Story E12.8: Unlock Request UI screens
    object UnlockRequests : Screen("unlock_requests/{deviceId}") {
        fun createRoute(deviceId: String) = "unlock_requests/$deviceId"
    }

    // Story E9.3: Admin Users Management screens
    object AdminUsers : Screen("admin_users")
    object UserHome : Screen("user_home/{groupId}/{userId}") {
        fun createRoute(groupId: String, userId: String) = "user_home/$groupId/$userId"
    }
    object UserLocationMap : Screen("user_location/{groupId}/{deviceId}") {
        fun createRoute(groupId: String, deviceId: String) = "user_location/$groupId/$deviceId"
    }

    // Story E9.4: Admin Geofence Management screen
    object AdminGeofence : Screen("admin_geofence/{groupId}/{deviceId}") {
        fun createRoute(groupId: String, deviceId: String) = "admin_geofence/$groupId/$deviceId"
    }

    // Story E13.10: Enterprise Enrollment screens
    object Setup : Screen("setup")
    object Enrollment : Screen("enrollment?token={token}") {
        fun createRoute(token: String? = null) = if (token != null) "enrollment?token=$token" else "enrollment"
    }
    object EnrollmentQRScanner : Screen("enrollment_qr_scanner")
    object EnrollmentSuccess : Screen("enrollment_success")
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
    // Story E11.9: Deep link invite code support (AC E11.9.8)
    pendingInviteCode: String? = null,
    onInviteCodeConsumed: () -> Unit = {},
    // Story E13.10: Deep link enrollment token support (AC E13.10.3)
    pendingEnrollmentToken: String? = null,
    onEnrollmentTokenConsumed: () -> Unit = {},
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

    // Story E11.9: Handle deep link invite code navigation (AC E11.9.8)
    LaunchedEffect(pendingInviteCode) {
        if (pendingInviteCode != null) {
            // Wait for NavHost to be ready
            delay(100)
            navController.navigate(Screen.JoinGroup.createRoute(pendingInviteCode)) {
                launchSingleTop = true
            }
            onInviteCodeConsumed()
        }
    }

    // Story E13.10: Handle deep link enrollment token navigation (AC E13.10.3)
    LaunchedEffect(pendingEnrollmentToken) {
        if (pendingEnrollmentToken != null) {
            // Wait for NavHost to be ready
            delay(100)
            navController.navigate(Screen.Enrollment.createRoute(pendingEnrollmentToken)) {
                launchSingleTop = true
            }
            onEnrollmentTokenConsumed()
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
                },
                // Story UGM-4.2: Navigate to migration screen
                onNavigateToMigration = { groupId ->
                    navController.navigate(Screen.GroupMigration.createRoute(groupId)) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
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
                },
                // Story UGM-4.2: Navigate to migration screen
                onNavigateToMigration = { groupId ->
                    navController.navigate(Screen.GroupMigration.createRoute(groupId)) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                },
            )
        }

        composable(Screen.ForgotPassword.route) {
            ForgotPasswordScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
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
                onNavigateToAdminUsers = {
                    navController.navigate(Screen.AdminUsers.route)
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
                    navController.navigate(Screen.GroupList.route)
                },
                onNavigateToMyDevices = {
                    navController.navigate(Screen.DeviceList.route)
                },
                // Story E12.8: Navigate to unlock requests screen
                onNavigateToUnlockRequests = { deviceId ->
                    navController.navigate(Screen.UnlockRequests.createRoute(deviceId))
                },
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
                onNavigateToDeviceDetail = { device ->
                    navController.navigate(Screen.DeviceDetail.createRoute(device.deviceUuid))
                },
            )
        }
        composable(
            route = Screen.DeviceDetail.route,
            arguments = listOf(
                navArgument("deviceId") { type = NavType.StringType },
            ),
        ) {
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

        // Story E11.8: Group Management screens (AC E11.8.1, E11.8.3, E11.8.4)
        composable(Screen.GroupList.route) {
            GroupListScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToGroupDetail = { group ->
                    navController.navigate(Screen.GroupDetail.createRoute(group.id))
                },
                // Story E11.9: Navigate to join group screen
                onNavigateToJoinGroup = {
                    navController.navigate(Screen.JoinGroup.createRoute())
                },
            )
        }
        composable(Screen.GroupDetail.route) {
            GroupDetailScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToMembers = { groupId ->
                    navController.navigate(Screen.ManageMembers.createRoute(groupId))
                },
                // Story E11.9: Navigate to invite members screen
                onNavigateToInvite = { groupId ->
                    navController.navigate(Screen.InviteMembers.createRoute(groupId))
                },
                // Story E12.7: Navigate to member devices settings screen
                onNavigateToMemberDevices = { groupId ->
                    navController.navigate(Screen.MemberDevices.createRoute(groupId))
                },
                onGroupDeleted = {
                    navController.navigate(Screen.GroupList.route) {
                        popUpTo(Screen.GroupList.route) { inclusive = true }
                    }
                },
                onLeftGroup = {
                    navController.navigate(Screen.GroupList.route) {
                        popUpTo(Screen.GroupList.route) { inclusive = true }
                    }
                },
            )
        }
        composable(Screen.ManageMembers.route) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: ""
            ManageMembersScreen(
                onNavigateBack = { navController.popBackStack() },
                onInviteMember = {
                    navController.navigate(Screen.InviteMembers.createRoute(groupId))
                },
                // Story UGM-2.2: Navigate to member's device details
                onNavigateToMemberDetails = { navGroupId, userId ->
                    navController.navigate(Screen.UserHome.createRoute(navGroupId, userId))
                },
            )
        }

        // Story E11.9: Invite screens (AC E11.9.1-E11.9.8)
        composable(Screen.InviteMembers.route) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: ""
            InviteMembersScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPendingInvites = {
                    navController.navigate(Screen.PendingInvites.createRoute(groupId))
                },
            )
        }

        composable(Screen.PendingInvites.route) {
            PendingInvitesScreen(
                onNavigateBack = { navController.popBackStack() },
                onInviteClick = { invite ->
                    // Could navigate to invite detail if needed
                },
            )
        }

        composable(
            route = Screen.JoinGroup.route,
            arguments = listOf(
                navArgument("code") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) { backStackEntry ->
            val code = backStackEntry.arguments?.getString("code")
            JoinGroupScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToQrScanner = {
                    navController.navigate(Screen.QRScanner.route)
                },
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route)
                },
                onJoinSuccess = { groupId ->
                    // Navigate to the group detail after joining
                    navController.navigate(Screen.GroupDetail.createRoute(groupId)) {
                        popUpTo(Screen.JoinGroup.route) { inclusive = true }
                    }
                },
            )
        }

        composable(Screen.QRScanner.route) {
            QRScannerScreen(
                onNavigateBack = { navController.popBackStack() },
                onCodeScanned = { scannedCode ->
                    // Pop back to JoinGroup screen with the scanned code
                    navController.popBackStack()
                    // Navigate to JoinGroup with the code
                    navController.navigate(Screen.JoinGroup.createRoute(scannedCode)) {
                        launchSingleTop = true
                    }
                },
            )
        }

        // Story UGM-4.3: Group Migration screen
        composable(
            route = Screen.GroupMigration.route,
            arguments = listOf(
                navArgument("groupId") { type = NavType.StringType },
            ),
        ) {
            GroupMigrationScreen(
                onNavigateBack = { navController.popBackStack() },
                onMigrationSuccess = { newGroupId ->
                    // Navigate to new group detail after migration
                    navController.navigate(Screen.GroupDetail.createRoute(newGroupId)) {
                        popUpTo(Screen.GroupMigration.route) { inclusive = true }
                    }
                },
            )
        }

        // Story E12.7: Admin Settings Management screens (AC E12.7.1-E12.7.8)
        composable(
            route = Screen.MemberDevices.route,
            arguments = listOf(
                navArgument("groupId") { type = NavType.StringType },
            ),
        ) {
            MemberDevicesScreen(
                onNavigateBack = { navController.popBackStack() },
                onDeviceClick = { deviceId ->
                    navController.navigate(Screen.AdminDeviceSettings.createRoute(deviceId))
                },
                onBulkEditClick = { deviceIds ->
                    navController.navigate(Screen.BulkSettings.createRoute(deviceIds))
                },
            )
        }

        composable(
            route = Screen.AdminDeviceSettings.route,
            arguments = listOf(
                navArgument("deviceId") { type = NavType.StringType },
            ),
        ) {
            DeviceSettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onViewHistory = { deviceId ->
                    navController.navigate(Screen.AdminSettingsHistory.createRoute(deviceId))
                },
            )
        }

        composable(
            route = Screen.AdminSettingsHistory.route,
            arguments = listOf(
                navArgument("deviceId") { type = NavType.StringType },
            ),
        ) {
            SettingsHistoryScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(
            route = Screen.BulkSettings.route,
            arguments = listOf(
                navArgument("deviceIds") { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            val deviceIdsString = backStackEntry.arguments?.getString("deviceIds") ?: ""
            val deviceIds = deviceIdsString.split(",").filter { it.isNotEmpty() }
            BulkSettingsScreen(
                deviceIds = deviceIds,
                onNavigateBack = { navController.popBackStack() },
                onComplete = {
                    navController.popBackStack()
                },
            )
        }

        composable(Screen.SettingsTemplates.route) {
            SettingsTemplateScreen(
                onNavigateBack = { navController.popBackStack() },
                onApplyTemplate = { templateId ->
                    // Template application is handled within the screen via ViewModel
                    // This callback could navigate to device selection if needed
                },
            )
        }

        // Story E12.8: Unlock Request UI screens (AC E12.8.1-E12.8.10)
        composable(
            route = Screen.UnlockRequests.route,
            arguments = listOf(
                navArgument("deviceId") { type = NavType.StringType },
            ),
        ) {
            UnlockRequestsScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }

        // Story E9.3: Admin Users Management screens (AC E9.3.1-E9.3.6)
        composable(Screen.AdminUsers.route) {
            SecretModeProtectedRoute(isSecretMode, navController) {
                AdminUsersScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToUserLocation = { groupId, userId ->
                        navController.navigate(Screen.UserHome.createRoute(groupId, userId))
                    },
                )
            }
        }

        // Story E9.3: User Home Screen (view another user's home data)
        composable(
            route = Screen.UserHome.route,
            arguments = listOf(
                navArgument("groupId") { type = NavType.StringType },
                navArgument("userId") { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: ""
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            SecretModeProtectedRoute(isSecretMode, navController) {
                UserHomeScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToMap = { latitude, longitude ->
                        // Navigate to map with the user's location
                        navController.navigate(Screen.Map.route)
                    },
                    onNavigateToGeofences = {
                        // Get deviceId from the current state if available
                        // For now, just navigate back since we need deviceId for geofences
                        navController.popBackStack()
                    },
                )
            }
        }

        composable(
            route = Screen.UserLocationMap.route,
            arguments = listOf(
                navArgument("groupId") { type = NavType.StringType },
                navArgument("deviceId") { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: ""
            val deviceId = backStackEntry.arguments?.getString("deviceId") ?: ""
            SecretModeProtectedRoute(isSecretMode, navController) {
                UserLocationMapScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToGeofences = {
                        navController.navigate(Screen.AdminGeofence.createRoute(groupId, deviceId))
                    },
                )
            }
        }

        // Story E9.4: Admin Geofence Management screen (AC E9.4.1-E9.4.4)
        composable(
            route = Screen.AdminGeofence.route,
            arguments = listOf(
                navArgument("groupId") { type = NavType.StringType },
                navArgument("deviceId") { type = NavType.StringType },
            ),
        ) {
            SecretModeProtectedRoute(isSecretMode, navController) {
                AdminGeofenceScreen(
                    onNavigateBack = { navController.popBackStack() },
                )
            }
        }

        // Story E13.10: Enterprise Enrollment screens (AC E13.10.1-E13.10.9)
        composable(Screen.Setup.route) {
            SetupScreen(
                onPersonalSetup = {
                    // Navigate to registration or home for personal setup
                    navController.navigate(Screen.Registration.route) {
                        popUpTo(Screen.Setup.route) { inclusive = true }
                    }
                },
                onEnterpriseSetup = {
                    // Navigate to enrollment screen
                    navController.navigate(Screen.Enrollment.createRoute())
                },
                onScanQrCode = {
                    // Navigate to QR scanner for quick enrollment
                    navController.navigate(Screen.EnrollmentQRScanner.route)
                },
            )
        }

        composable(
            route = Screen.Enrollment.route,
            arguments = listOf(
                navArgument("token") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) { backStackEntry ->
            val token = backStackEntry.arguments?.getString("token")
            EnrollmentScreen(
                onNavigateBack = { navController.popBackStack() },
                onEnrollmentSuccess = {
                    navController.navigate(Screen.EnrollmentSuccess.route) {
                        popUpTo(Screen.Enrollment.route) { inclusive = true }
                    }
                },
                onNavigateToQRScanner = {
                    navController.navigate(Screen.EnrollmentQRScanner.route)
                },
                initialToken = token,
            )
        }

        composable(Screen.EnrollmentQRScanner.route) {
            EnrollmentQRScannerScreen(
                onNavigateBack = { navController.popBackStack() },
                onCodeScanned = { scannedToken ->
                    // Pop back to enrollment screen with the scanned token
                    navController.popBackStack()
                    navController.navigate(Screen.Enrollment.createRoute(scannedToken)) {
                        launchSingleTop = true
                    }
                },
            )
        }

        composable(Screen.EnrollmentSuccess.route) {
            EnrollmentSuccessScreen(
                onGetStarted = {
                    // Navigate to home after successful enrollment
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.EnrollmentSuccess.route) { inclusive = true }
                    }
                },
            )
        }
    }
}

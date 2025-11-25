package three.two.bit.phonemanager.ui.home

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import three.two.bit.phonemanager.ui.components.LocationStatsCard
import three.two.bit.phonemanager.ui.components.LocationTrackingToggle
import three.two.bit.phonemanager.ui.components.ServiceStatusCard
import three.two.bit.phonemanager.ui.main.LocationTrackingViewModel
import three.two.bit.phonemanager.ui.permissions.PermissionRationaleDialog
import three.two.bit.phonemanager.ui.permissions.PermissionStatusCard
import three.two.bit.phonemanager.ui.permissions.PermissionType
import three.two.bit.phonemanager.ui.permissions.PermissionViewModel

/**
 * HomeScreen - Main screen with permission management and tracking toggle
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    permissionViewModel: PermissionViewModel,
    onRequestLocationPermission: () -> Unit,
    onRequestBackgroundPermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onNavigateToGroupMembers: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    modifier: Modifier = Modifier,
    homeViewModel: HomeViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val permissionState by permissionViewModel.permissionState.collectAsState()
    val showLocationRationale by permissionViewModel.showLocationRationale.collectAsState()
    val showBackgroundRationale by permissionViewModel.showBackgroundRationale.collectAsState()
    val showNotificationRationale by permissionViewModel.showNotificationRationale.collectAsState()

    // Story E2.1: Tap counter for version text (AC E2.1.3)
    var tapCount by remember { mutableIntStateOf(0) }
    var lastTapTime by remember { mutableLongStateOf(0L) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    // Story E2.1: Long-press gesture on title to toggle secret mode (AC E2.1.2)
                    Text(
                        "Phone Manager",
                        modifier =
                        Modifier.pointerInput(Unit) {
                            detectTapGestures(
                                onLongPress = {
                                    homeViewModel.toggleSecretMode()
                                },
                            )
                        },
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, "Settings")
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier =
            modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Location Tracking",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // Permission status card
            PermissionStatusCard(
                permissionState = permissionState,
                onGrantPermissions = {
                    permissionViewModel.requestLocationPermission(context as Activity)
                },
                onOpenSettings = {
                    permissionViewModel.openAppSettings(context)
                },
                modifier = Modifier.fillMaxWidth(),
            )

            // Location tracking toggle (Story 1.1)
            LocationTrackingToggle(
                modifier = Modifier.fillMaxWidth(),
            )

            // Story 1.3: Service status and location statistics
            val trackingViewModel: LocationTrackingViewModel = hiltViewModel()
            val serviceState by trackingViewModel.serviceState.collectAsState()
            val locationStats by trackingViewModel.locationStats.collectAsState()

            // Service status card (Story 1.3)
            ServiceStatusCard(
                serviceState = serviceState,
                modifier = Modifier.fillMaxWidth(),
            )

            // Location stats card (Story 1.3)
            LocationStatsCard(
                locationStats = locationStats,
                modifier = Modifier.fillMaxWidth(),
            )

            // Navigate to Group Members screen (Story E1.2)
            Button(
                onClick = onNavigateToGroupMembers,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("View Group Members")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Story E2.1: Version text with tap gesture to toggle secret mode (AC E2.1.3)
            Text(
                text = "v1.0.0",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier =
                Modifier.pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            val now = System.currentTimeMillis()
                            if (now - lastTapTime < 500) {
                                tapCount++
                            } else {
                                tapCount = 1
                            }
                            lastTapTime = now
                            if (tapCount >= 5) {
                                homeViewModel.toggleSecretMode()
                                tapCount = 0
                            }
                        },
                    )
                },
            )
        }
    }

    // Location permission rationale dialog
    if (showLocationRationale) {
        PermissionRationaleDialog(
            type = PermissionType.LOCATION,
            onAccept = {
                permissionViewModel.onLocationRationaleAccepted()
                onRequestLocationPermission()
            },
            onDismiss = {
                permissionViewModel.onLocationRationaleDismissed()
            },
        )
    }

    // Background permission rationale dialog
    if (showBackgroundRationale && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        PermissionRationaleDialog(
            type = PermissionType.BACKGROUND,
            onAccept = {
                permissionViewModel.onBackgroundRationaleAccepted()
                onRequestBackgroundPermission()
            },
            onDismiss = {
                permissionViewModel.onBackgroundRationaleDismissed()
            },
        )
    }

    // Notification permission rationale dialog
    if (showNotificationRationale && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        PermissionRationaleDialog(
            type = PermissionType.NOTIFICATION,
            onAccept = {
                permissionViewModel.onNotificationRationaleAccepted()
                onRequestNotificationPermission()
            },
            onDismiss = {
                permissionViewModel.onNotificationRationaleDismissed()
            },
        )
    }
}

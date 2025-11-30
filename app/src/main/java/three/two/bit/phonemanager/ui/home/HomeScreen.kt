package three.two.bit.phonemanager.ui.home

import android.app.Activity
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.ShareLocation
import androidx.compose.material.icons.rounded.Webhook
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import three.two.bit.phonemanager.R
import three.two.bit.phonemanager.permission.PermissionState
import three.two.bit.phonemanager.ui.components.ActiveTripCard
import three.two.bit.phonemanager.ui.components.DailySummaryCard
import three.two.bit.phonemanager.ui.components.LocationStatsCard
import three.two.bit.phonemanager.ui.components.LocationTrackingToggle
import three.two.bit.phonemanager.ui.components.QuickActionCard
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
    modifier: Modifier = Modifier,
    onNavigateToGroupMembers: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToMap: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    onNavigateToAlerts: () -> Unit = {},
    onNavigateToGeofences: () -> Unit = {},
    onNavigateToWebhooks: () -> Unit = {},
    onNavigateToWeather: () -> Unit = {},
    onNavigateToTripHistory: () -> Unit = {},
    homeViewModel: HomeViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val permissionState by permissionViewModel.permissionState.collectAsState()
    val showLocationRationale by permissionViewModel.showLocationRationale.collectAsState()
    val showBackgroundRationale by permissionViewModel.showBackgroundRationale.collectAsState()
    val showNotificationRationale by permissionViewModel.showNotificationRationale.collectAsState()

    // Secret mode state and haptic feedback
    val isSecretMode by homeViewModel.isSecretModeEnabled.collectAsState()
    val hapticFeedback = LocalHapticFeedback.current

    // Story E8.13: Active trip and daily stats (AC E8.13.4, E8.13.5)
    val activeTrip by homeViewModel.activeTrip.collectAsState()
    val todayStats by homeViewModel.todayStats.collectAsState()
    val tripEndedEvent by homeViewModel.tripEndedEvent.collectAsState()

    // Snackbar state for trip ended feedback
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val tripEndedMessage = stringResource(R.string.trip_ended_message)

    // Show snackbar when trip ends
    LaunchedEffect(tripEndedEvent) {
        if (tripEndedEvent) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(tripEndedMessage)
                homeViewModel.clearTripEndedEvent()
            }
        }
    }

    // Toggle secret mode with haptic feedback
    val toggleWithHaptic = {
        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
        homeViewModel.toggleSecretMode()
    }

    // Story E2.1: Tap counter for version text (AC E2.1.3)
    var tapCount by remember { mutableIntStateOf(0) }
    var lastTapTime by remember { mutableLongStateOf(0L) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    // Story E2.1: Long-press gesture on title to toggle secret mode (AC E2.1.2)
                    Text(
                        text = stringResource(
                            if (isSecretMode) R.string.app_name_secret else R.string.app_name,
                        ),
                        modifier =
                        Modifier.pointerInput(Unit) {
                            detectTapGestures(
                                onLongPress = { toggleWithHaptic() },
                            )
                        },
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, stringResource(R.string.settings))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        Column(
            modifier =
            modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Hide tracking-related UI in secret mode
            AnimatedVisibility(
                visible = !isSecretMode,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(R.string.home_location_tracking),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    // Permission status card
                    PermissionStatusCard(
                        permissionState = permissionState,
                        onGrantPermissions = {
                            when (permissionState) {
                                is PermissionState.LocationDenied -> {
                                    permissionViewModel.requestLocationPermission(context as Activity)
                                }

                                is PermissionState.BackgroundDenied -> {
                                    onRequestBackgroundPermission()
                                }

                                is PermissionState.NotificationDenied -> {
                                    permissionViewModel.requestNotificationPermission()
                                }

                                else -> {
                                    // For any other state, default to location permission
                                    permissionViewModel.requestLocationPermission(context as Activity)
                                }
                            }
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

                    // Story E8.13: Active trip card (AC E8.13.1, E8.13.4, E8.13.5)
                    activeTrip?.let { trip ->
                        ActiveTripCard(
                            trip = trip,
                            onEndTrip = { homeViewModel.endActiveTrip() },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    // Story E8.13: Daily summary card (AC E8.13.2)
                    DailySummaryCard(
                        stats = todayStats,
                        onViewHistory = onNavigateToTripHistory,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            // Quick Actions Grid - Row 1: Group Members + Map (hidden in secret mode)
            AnimatedVisibility(
                visible = !isSecretMode,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    QuickActionCard(
                        icon = Icons.Rounded.Groups,
                        title = stringResource(R.string.home_quick_group),
                        onClick = onNavigateToGroupMembers,
                        modifier = Modifier.weight(1f),
                    )
                    QuickActionCard(
                        icon = Icons.Rounded.Map,
                        title = stringResource(R.string.home_quick_map),
                        onClick = onNavigateToMap,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // Quick Actions Grid - Row 2: History (hidden) + Alerts (visible)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // History - hidden in secret mode
                AnimatedVisibility(
                    visible = !isSecretMode,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                    modifier = Modifier.weight(1f),
                ) {
                    QuickActionCard(
                        icon = Icons.Rounded.History,
                        title = stringResource(R.string.home_quick_history),
                        onClick = onNavigateToHistory,
                    )
                }
                // Alerts - always visible
                QuickActionCard(
                    icon = Icons.Rounded.NotificationsActive,
                    title = stringResource(R.string.home_quick_alerts),
                    onClick = onNavigateToAlerts,
                    modifier = if (isSecretMode) Modifier.fillMaxWidth() else Modifier.weight(1f),
                )
            }

            // Quick Actions Grid - Row 3: Geofences + Webhooks (always visible)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                QuickActionCard(
                    icon = Icons.Rounded.ShareLocation,
                    title = stringResource(R.string.home_quick_geofences),
                    onClick = onNavigateToGeofences,
                    modifier = Modifier.weight(1f),
                )
                QuickActionCard(
                    icon = Icons.Rounded.Webhook,
                    title = stringResource(R.string.home_quick_webhooks),
                    onClick = onNavigateToWebhooks,
                    modifier = Modifier.weight(1f),
                )
            }

            // Quick Actions Grid - Row 4: Trips (hidden in secret mode)
            AnimatedVisibility(
                visible = !isSecretMode,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    QuickActionCard(
                        icon = Icons.Rounded.Route,
                        title = stringResource(R.string.home_quick_trips),
                        onClick = onNavigateToTripHistory,
                        modifier = Modifier.weight(1f),
                    )
                    // Placeholder for future feature
                    Spacer(modifier = Modifier.weight(1f))
                }
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
                                toggleWithHaptic()
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

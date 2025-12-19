@file:Suppress("DEPRECATION") // hiltViewModel() deprecation - using stable API

package three.two.bit.phonemanager.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.ShareLocation
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import three.two.bit.phonemanager.R
import three.two.bit.phonemanager.domain.model.GroupRole
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/**
 * Story E9.3: User Home Screen
 *
 * Displays another user's home screen data including:
 * - User info and role
 * - Device status and location
 * - Tracking controls (for admins)
 *
 * AC E9.3.1-E9.3.6: View User Location
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserHomeScreen(
    viewModel: UserHomeViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToMap: (latitude: Double, longitude: Double) -> Unit = { _, _ -> },
    onNavigateToGeofences: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Snackbar messages
    val trackingEnabledMsg = stringResource(R.string.admin_tracking_enabled)
    val trackingDisabledMsg = stringResource(R.string.admin_tracking_disabled)

    // Show snackbar on tracking toggle success
    LaunchedEffect(uiState.trackingToggleSuccess) {
        if (uiState.trackingToggleSuccess) {
            val message = if (uiState.trackingEnabled == true) trackingEnabledMsg else trackingDisabledMsg
            coroutineScope.launch {
                snackbarHostState.showSnackbar(message)
            }
            viewModel.clearTrackingToggleSuccess()
        }
    }

    // Show snackbar on tracking toggle error
    LaunchedEffect(uiState.trackingToggleError) {
        uiState.trackingToggleError?.let { error ->
            coroutineScope.launch {
                snackbarHostState.showSnackbar(error)
            }
            viewModel.clearTrackingToggleError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.member?.displayName ?: stringResource(R.string.user_home_title),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshLocation() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.refresh),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }

                uiState.error != null -> {
                    ErrorState(
                        message = uiState.error!!,
                        onRetry = { viewModel.retry() },
                    )
                }

                uiState.noDevicesRegistered -> {
                    NoDevicesState(userName = uiState.member?.displayName ?: "User")
                }

                else -> {
                    UserHomeContent(
                        uiState = uiState,
                        onToggleTracking = { viewModel.toggleTracking() },
                        onNavigateToMap = {
                            uiState.location?.let { location ->
                                onNavigateToMap(location.latitude, location.longitude)
                            }
                        },
                        onNavigateToGeofences = onNavigateToGeofences,
                    )
                }
            }
        }
    }
}

@Composable
private fun UserHomeContent(
    uiState: UserHomeUiState,
    onToggleTracking: () -> Unit,
    onNavigateToMap: () -> Unit,
    onNavigateToGeofences: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // User Info Card
        UserInfoCard(
            displayName = uiState.member?.displayName ?: "Unknown",
            role = uiState.member?.role ?: GroupRole.MEMBER,
            deviceCount = uiState.devices.size,
        )

        // Location Status Card
        LocationStatusCard(
            hasLocation = uiState.location != null,
            lastSeenAt = uiState.lastSeenAt,
            deviceName = uiState.primaryDevice?.displayName,
            onViewOnMap = onNavigateToMap,
        )

        // Tracking Control Card
        TrackingControlCard(
            trackingEnabled = uiState.trackingEnabled,
            isToggling = uiState.isTogglingTracking,
            onToggle = onToggleTracking,
        )

        // Device Info Card
        uiState.primaryDevice?.let { device ->
            DeviceInfoCard(device = device)
        }

        // Quick Actions
        QuickActionsCard(
            hasLocation = uiState.location != null,
            onViewOnMap = onNavigateToMap,
            onManageGeofences = onNavigateToGeofences,
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun UserInfoCard(
    displayName: String,
    role: GroupRole,
    deviceCount: Int,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary,
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Medium,
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RoleBadge(role = role)

                    Text(
                        text = "$deviceCount ${if (deviceCount == 1) "device" else "devices"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun RoleBadge(role: GroupRole) {
    val (text, containerColor) = when (role) {
        GroupRole.OWNER -> stringResource(R.string.admin_role_owner) to MaterialTheme.colorScheme.primary
        GroupRole.ADMIN -> stringResource(R.string.admin_role_admin) to MaterialTheme.colorScheme.secondary
        GroupRole.MEMBER -> stringResource(R.string.admin_role_member) to MaterialTheme.colorScheme.surfaceVariant
    }

    val contentColor = when (role) {
        GroupRole.OWNER -> MaterialTheme.colorScheme.onPrimary
        GroupRole.ADMIN -> MaterialTheme.colorScheme.onSecondary
        GroupRole.MEMBER -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        shape = MaterialTheme.shapes.small,
        color = containerColor,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun LocationStatusCard(
    hasLocation: Boolean,
    lastSeenAt: Instant?,
    deviceName: String?,
    onViewOnMap: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (hasLocation) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.errorContainer
            }
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (hasLocation) Icons.Default.LocationOn else Icons.Default.LocationOff,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = if (hasLocation) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onErrorContainer
                },
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (hasLocation) {
                        stringResource(R.string.user_home_location_available)
                    } else {
                        stringResource(R.string.user_home_location_unavailable)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (hasLocation) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onErrorContainer
                    },
                )

                lastSeenAt?.let {
                    Text(
                        text = stringResource(R.string.last_seen, formatRelativeTime(it)),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (hasLocation) {
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                        },
                    )
                }

                deviceName?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (hasLocation) {
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                        },
                    )
                }
            }

            if (hasLocation) {
                IconButton(onClick = onViewOnMap) {
                    Icon(
                        imageVector = Icons.Rounded.Map,
                        contentDescription = stringResource(R.string.admin_view_location),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }
    }
}

@Composable
private fun TrackingControlCard(
    trackingEnabled: Boolean?,
    isToggling: Boolean,
    onToggle: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.ShareLocation,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = if (trackingEnabled == true) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.user_home_tracking_control),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                )

                Text(
                    text = when (trackingEnabled) {
                        true -> stringResource(R.string.user_home_tracking_enabled)
                        false -> stringResource(R.string.user_home_tracking_disabled)
                        null -> stringResource(R.string.user_home_tracking_unknown)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (isToggling) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                )
            } else {
                Switch(
                    checked = trackingEnabled ?: false,
                    onCheckedChange = { onToggle() },
                    enabled = trackingEnabled != null,
                )
            }
        }
    }
}

@Composable
private fun DeviceInfoCard(device: three.two.bit.phonemanager.domain.model.Device) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Smartphone,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary,
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                )

                Text(
                    text = "ID: ${device.deviceId.take(8)}...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                device.lastLocation?.let { location ->
                    Text(
                        text = "%.4f, %.4f".format(location.latitude, location.longitude),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickActionsCard(
    hasLocation: Boolean,
    onViewOnMap: () -> Unit,
    onManageGeofences: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text(
                text = stringResource(R.string.user_home_quick_actions),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = onViewOnMap,
                    enabled = hasLocation,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Map,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.user_home_view_map))
                }

                Button(
                    onClick = onManageGeofences,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ShareLocation,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.user_home_geofences))
                }
            }
        }
    }
}

@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
            )

            Button(onClick = onRetry) {
                Text(stringResource(R.string.retry))
            }
        }
    }
}

@Composable
private fun NoDevicesState(userName: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                imageVector = Icons.Default.PersonOff,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text(
                text = stringResource(R.string.user_home_no_devices, userName),
                style = MaterialTheme.typography.titleMedium,
            )

            Text(
                text = stringResource(R.string.user_home_no_devices_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun formatRelativeTime(instant: Instant): String {
    val now = Clock.System.now()
    val duration = now - instant
    return when {
        duration < 1.minutes -> stringResource(R.string.time_just_now)
        duration < 1.hours -> stringResource(R.string.time_minutes_ago, duration.inWholeMinutes.toInt())
        duration < 1.days -> stringResource(R.string.time_hours_ago, duration.inWholeHours.toInt())
        duration < 7.days -> stringResource(R.string.time_days_ago, duration.inWholeDays.toInt())
        else -> instant.toString().take(10)
    }
}

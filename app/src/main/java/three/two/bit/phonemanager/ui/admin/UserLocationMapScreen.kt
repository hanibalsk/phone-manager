package three.two.bit.phonemanager.ui.admin

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.remember
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import three.two.bit.phonemanager.R

/**
 * Story E9.3: User Location Map Screen
 * Story E9.5: Remote Tracking Control
 *
 * Displays a single user's location on a Google Map.
 * Reuses UI patterns from MapScreen (Story E3.1/E3.2/E3.3).
 *
 * ACs: E9.3.4, E9.3.5, E9.3.6, E9.5.1-E9.5.5
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserLocationMapScreen(
    viewModel: UserLocationMapViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToGeofences: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Get localized strings for snackbar messages
    val trackingEnabledMessage = stringResource(R.string.admin_tracking_enabled)
    val trackingDisabledMessage = stringResource(R.string.admin_tracking_disabled)

    // Start/stop polling based on lifecycle
    DisposableEffect(Unit) {
        viewModel.startPolling()
        onDispose {
            viewModel.stopPolling()
        }
    }

    // Story E9.5: Show success/error messages for tracking toggle
    // AC E9.5.3: Visual confirmation of tracking state change
    LaunchedEffect(uiState.trackingToggleSuccess) {
        if (uiState.trackingToggleSuccess) {
            val message = if (uiState.trackingEnabled == true) {
                trackingEnabledMessage
            } else {
                trackingDisabledMessage
            }
            snackbarHostState.showSnackbar(message)
            viewModel.clearTrackingToggleSuccess()
        }
    }

    LaunchedEffect(uiState.trackingToggleError) {
        uiState.trackingToggleError?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearTrackingToggleError()
        }
    }

    // Camera position with zoom level 15
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            uiState.location ?: LatLng(0.0, 0.0),
            15f,
        )
    }

    // Center map on location when it changes
    LaunchedEffect(uiState.location) {
        uiState.location?.let { location ->
            cameraPositionState.position = CameraPosition.fromLatLngZoom(location, 15f)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.device?.displayName?.let {
                            stringResource(R.string.admin_user_location_title, it)
                        } ?: stringResource(R.string.admin_view_location),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToGeofences,
                containerColor = MaterialTheme.colorScheme.primary,
            ) {
                Icon(Icons.Default.Place, contentDescription = stringResource(R.string.admin_manage_geofences))
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            when {
                // Loading state
                uiState.isLoading && uiState.location == null -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
                // Error state
                uiState.error != null && uiState.location == null -> {
                    ErrorContent(
                        message = uiState.error!!,
                        onRetry = { viewModel.loadDeviceLocation() },
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
                // No location available
                uiState.location == null && !uiState.isLoading -> {
                    NoLocationContent(
                        deviceName = uiState.device?.displayName ?: "",
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
                // Show map with location
                else -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Map taking most of the screen
                        GoogleMap(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            cameraPositionState = cameraPositionState,
                            properties = MapProperties(
                                isMyLocationEnabled = false,
                            ),
                            uiSettings = MapUiSettings(
                                zoomControlsEnabled = true,
                                compassEnabled = true,
                                rotationGesturesEnabled = true,
                                scrollGesturesEnabled = true,
                                zoomGesturesEnabled = true,
                            ),
                        ) {
                            // User location marker
                            uiState.location?.let { location ->
                                val markerSnippet = uiState.lastSeenAt?.let { lastSeen ->
                                    stringResource(R.string.last_seen, formatRelativeTime(lastSeen))
                                } ?: ""

                                Marker(
                                    state = MarkerState(position = location),
                                    title = uiState.device?.displayName ?: "",
                                    snippet = markerSnippet,
                                    icon = BitmapDescriptorFactory.defaultMarker(
                                        BitmapDescriptorFactory.HUE_ORANGE,
                                    ),
                                )
                            }
                        }

                        // Info card at bottom with tracking toggle
                        LocationInfoCard(
                            deviceName = uiState.device?.displayName ?: "",
                            lastSeenAt = uiState.lastSeenAt,
                            trackingEnabled = uiState.trackingEnabled,
                            isTogglingTracking = uiState.isTogglingTracking,
                            onToggleTracking = { enabled -> viewModel.toggleTracking(enabled) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Location info card shown at bottom of map
 *
 * AC E9.3.5: Displays last update timestamp
 * AC E9.3.6: Shows user's display name and device info
 * AC E9.5.1: Toggle control for tracking
 * AC E9.5.5: Current tracking state reflected in UI
 */
@Composable
private fun LocationInfoCard(
    deviceName: String,
    lastSeenAt: Instant?,
    trackingEnabled: Boolean?,
    isTogglingTracking: Boolean,
    onToggleTracking: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text(
                text = deviceName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            lastSeenAt?.let { timestamp ->
                Text(
                    text = stringResource(
                        R.string.admin_user_location_subtitle,
                        formatRelativeTime(timestamp),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Story E9.5: Tracking toggle
            // AC E9.5.1: Toggle control on user detail screen
            if (trackingEnabled != null) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.admin_tracking_label),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )

                    if (isTogglingTracking) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Switch(
                            checked = trackingEnabled,
                            onCheckedChange = onToggleTracking,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Error content with retry button
 */
@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
        )

        Button(
            onClick = onRetry,
            modifier = Modifier.padding(top = 16.dp),
        ) {
            Text(stringResource(R.string.retry))
        }
    }
}

/**
 * Content shown when no location is available
 */
@Composable
private fun NoLocationContent(
    deviceName: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.admin_no_location),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )

        if (deviceName.isNotEmpty()) {
            Text(
                text = deviceName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

/**
 * Format timestamp as relative time
 */
@Composable
private fun formatRelativeTime(instant: Instant): String {
    val now = Clock.System.now()
    val duration = now - instant
    return when {
        duration < 1.minutes -> stringResource(R.string.time_just_now)
        duration < 1.hours -> stringResource(R.string.time_minutes_ago, duration.inWholeMinutes.toInt())
        duration < 1.days -> stringResource(R.string.time_hours_ago, duration.inWholeHours.toInt())
        duration < 7.days -> stringResource(R.string.time_days_ago, duration.inWholeDays.toInt())
        else -> {
            instant.toLocalDateTime(TimeZone.currentSystemDefault())
                .date.toString()
        }
    }
}

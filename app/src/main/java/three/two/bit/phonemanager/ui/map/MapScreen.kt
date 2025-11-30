package three.two.bit.phonemanager.ui.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import three.two.bit.phonemanager.R
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/**
 * Story E3.1/E3.2/E3.3: Map Screen
 *
 * Story E3.1: Displays Google Map with current device location
 * Story E3.2: Displays group member locations on map
 * Story E3.3: Real-time polling for location updates
 * ACs: E3.1.1, E3.1.2, E3.1.3, E3.1.4, E3.2.1, E3.2.2, E3.2.3, E3.2.4, E3.2.5, E3.3.1, E3.3.6
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(viewModel: MapViewModel = hiltViewModel(), onNavigateBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Story E3.3: Lifecycle-aware polling (AC E3.3.6)
    DisposableEffect(Unit) {
        viewModel.startPolling()
        onDispose {
            viewModel.stopPolling()
        }
    }

    // AC E3.1.3: Camera position with zoom level 15
    val cameraPositionState =
        rememberCameraPositionState {
            position =
                CameraPosition.fromLatLngZoom(
                    uiState.currentLocation ?: LatLng(0.0, 0.0),
                    15f,
                )
        }

    // AC E3.1.3: Center map on current location when available
    LaunchedEffect(uiState.currentLocation) {
        uiState.currentLocation?.let { location ->
            cameraPositionState.position =
                CameraPosition.fromLatLngZoom(
                    location,
                    15f,
                )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.map_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
            )
        },
    ) { paddingValues ->
        Box(
            modifier =
            Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            when {
                // Show loading state
                uiState.isLoading && uiState.currentLocation == null -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
                // Show error state
                uiState.error != null -> {
                    Text(
                        text = uiState.error!!,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
                // Show map (AC E3.1.1)
                else -> {
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState,
                        properties =
                        MapProperties(
                            isMyLocationEnabled = false, // We use custom marker
                        ),
                        uiSettings =
                        MapUiSettings(
                            zoomControlsEnabled = true, // AC E3.1.4: Zoom
                            compassEnabled = true,
                            rotationGesturesEnabled = true, // AC E3.1.4: Rotate
                            scrollGesturesEnabled = true, // AC E3.1.4: Pan
                            zoomGesturesEnabled = true, // AC E3.1.4: Pinch zoom
                        ),
                    ) {
                        // AC E3.1.2, E3.2.3: Current location marker with distinctive blue color
                        uiState.currentLocation?.let { location ->
                            Marker(
                                state = MarkerState(position = location),
                                title = stringResource(R.string.map_marker_you),
                                snippet = stringResource(R.string.marker_current_location),
                                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE),
                            )
                        }

                        // Story E3.2: Group member markers (AC E3.2.1, E3.2.2, E3.2.3, E3.2.4, E3.2.5)
                        uiState.groupMembers.forEach { member ->
                            // AC E3.2.5: Filter out members with null lastLocation
                            member.lastLocation?.let { location ->
                                // Precompute snippet for localization
                                val memberSnippet = member.lastSeenAt?.let { lastSeen ->
                                    stringResource(R.string.last_seen, formatRelativeTime(lastSeen))
                                } ?: stringResource(R.string.loading)

                                Marker(
                                    state =
                                    MarkerState(
                                        position =
                                        LatLng(
                                            location.latitude,
                                            location.longitude,
                                        ),
                                    ),
                                    // AC E3.2.2: Display name as title
                                    title = member.displayName,
                                    // AC E3.2.4: Info window with last update time
                                    snippet = memberSnippet,
                                    // AC E3.2.3: Visual distinction - orange for group members
                                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Format timestamp as relative time (AC E3.2.4)
 * Examples: "Just now", "2 min ago", "3h ago", "2d ago"
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
            // For older dates, show the actual date
            instant.toLocalDateTime(TimeZone.currentSystemDefault())
                .date.toString()
        }
    }
}

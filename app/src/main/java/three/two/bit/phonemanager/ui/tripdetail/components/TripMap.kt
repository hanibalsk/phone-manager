package three.two.bit.phonemanager.ui.tripdetail.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import three.two.bit.phonemanager.R
import three.two.bit.phonemanager.data.model.LocationEntity
import three.two.bit.phonemanager.domain.model.LatLng as DomainLatLng

/**
 * Story E8.10: TripMap Component
 *
 * Displays trip route on Google Map with start/end markers.
 * AC E8.10.2: Map with route polyline
 * AC E8.10.3: Raw vs corrected path toggle
 */
@Composable
fun TripMap(
    locations: List<LocationEntity>,
    showCorrectedPath: Boolean,
    correctedPath: List<DomainLatLng>,
    hasCorrectedPath: Boolean,
    selectedLocationIndex: Int?,
    onTogglePathView: () -> Unit,
    onLocationSelected: (Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cameraPositionState = rememberCameraPositionState()

    // Fit bounds to show all locations
    LaunchedEffect(locations) {
        if (locations.isNotEmpty()) {
            val boundsBuilder = LatLngBounds.builder()
            locations.forEach { location ->
                boundsBuilder.include(LatLng(location.latitude, location.longitude))
            }
            val bounds = boundsBuilder.build()
            val padding = 100 // pixels
            cameraPositionState.move(CameraUpdateFactory.newLatLngBounds(bounds, padding))
        }
    }

    Box(modifier = modifier) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            uiSettings = MapUiSettings(
                zoomControlsEnabled = true,
                compassEnabled = true,
                rotationGesturesEnabled = true,
                scrollGesturesEnabled = true,
                zoomGesturesEnabled = true,
            ),
        ) {
            if (locations.isNotEmpty()) {
                val rawPoints = locations.map { LatLng(it.latitude, it.longitude) }
                val correctedPoints = correctedPath.map { LatLng(it.latitude, it.longitude) }

                // Determine which path to display
                val displayCorrected = showCorrectedPath && hasCorrectedPath && correctedPoints.isNotEmpty()
                val displayPoints = if (displayCorrected) correctedPoints else rawPoints

                // Route polyline (AC E8.10.2, E8.10.3)
                Polyline(
                    points = displayPoints,
                    color = if (displayCorrected) {
                        Color(0xFF1976D2) // Blue for corrected (solid appearance)
                    } else {
                        Color(0xFF4CAF50) // Green for raw
                    },
                    width = 8f,
                    geodesic = true,
                )

                // Start marker (green circle) (AC E8.10.2)
                val startLocation = locations.first()
                Circle(
                    center = LatLng(startLocation.latitude, startLocation.longitude),
                    radius = 20.0,
                    fillColor = Color(0xFF4CAF50).copy(alpha = 0.7f),
                    strokeColor = Color(0xFF2E7D32),
                    strokeWidth = 3f,
                )
                Marker(
                    state = MarkerState(
                        position = LatLng(startLocation.latitude, startLocation.longitude),
                    ),
                    title = stringResource(R.string.trip_detail_start),
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN),
                )

                // End marker (red circle) (AC E8.10.2)
                val endLocation = locations.last()
                if (locations.size > 1) {
                    Circle(
                        center = LatLng(endLocation.latitude, endLocation.longitude),
                        radius = 20.0,
                        fillColor = Color(0xFFF44336).copy(alpha = 0.7f),
                        strokeColor = Color(0xFFC62828),
                        strokeWidth = 3f,
                    )
                    Marker(
                        state = MarkerState(
                            position = LatLng(endLocation.latitude, endLocation.longitude),
                        ),
                        title = stringResource(R.string.trip_detail_end),
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED),
                    )
                }

                // Selected location marker (AC E8.10.10)
                selectedLocationIndex?.let { index ->
                    if (index in locations.indices) {
                        val selectedLocation = locations[index]
                        Marker(
                            state = MarkerState(
                                position = LatLng(
                                    selectedLocation.latitude,
                                    selectedLocation.longitude,
                                ),
                            ),
                            title = "Point ${index + 1}",
                            snippet = selectedLocation.speed?.let {
                                "Speed: ${String.format("%.1f", it * 3.6)} km/h"
                            },
                            icon = BitmapDescriptorFactory.defaultMarker(
                                BitmapDescriptorFactory.HUE_ORANGE,
                            ),
                        )
                    }
                }
            }
        }

        // Path toggle chip (AC E8.10.3) - positioned at bottom
        // Only enabled when corrected path is available
        FilterChip(
            selected = showCorrectedPath && hasCorrectedPath,
            onClick = onTogglePathView,
            enabled = hasCorrectedPath,
            label = {
                Text(
                    if (showCorrectedPath && hasCorrectedPath) {
                        stringResource(R.string.trip_map_corrected)
                    } else {
                        stringResource(R.string.trip_map_raw)
                    },
                )
            },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
        )
    }
}

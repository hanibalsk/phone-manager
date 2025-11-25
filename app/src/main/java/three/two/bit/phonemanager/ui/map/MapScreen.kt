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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

/**
 * Story E3.1: Map Screen
 *
 * Displays Google Map with current device location
 * ACs: E3.1.1, E3.1.2, E3.1.3, E3.1.4
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(viewModel: MapViewModel = hiltViewModel(), onNavigateBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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
                title = { Text("Map") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
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
                        // AC E3.1.2: Current location marker with distinctive blue color
                        uiState.currentLocation?.let { location ->
                            Marker(
                                state = MarkerState(position = location),
                                title = "You",
                                snippet = "Current Location",
                                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE),
                            )
                        }
                    }
                }
            }
        }
    }
}

package three.two.bit.phonemanager.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import three.two.bit.phonemanager.location.LocationManager
import javax.inject.Inject

/**
 * Story E3.1: MapViewModel
 *
 * Manages map state and current location
 * ACs: E3.1.2, E3.1.3, E3.1.5
 */
@HiltViewModel
class MapViewModel
@Inject
constructor(private val locationManager: LocationManager) : ViewModel() {
    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    init {
        loadCurrentLocation()
    }

    /**
     * Load current device location (AC E3.1.2, E3.1.3)
     */
    fun loadCurrentLocation() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val result = locationManager.getCurrentLocation()

            result.fold(
                onSuccess = { locationEntity ->
                    if (locationEntity != null) {
                        val currentLocation =
                            LatLng(
                                locationEntity.latitude,
                                locationEntity.longitude,
                            )
                        _uiState.update {
                            it.copy(
                                currentLocation = currentLocation,
                                isLoading = false,
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = "Unable to get current location",
                            )
                        }
                    }
                },
                onFailure = { exception ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = exception.message ?: "Failed to load location",
                        )
                    }
                },
            )
        }
    }

    /**
     * Refresh location
     */
    fun refresh() {
        loadCurrentLocation()
    }
}

/**
 * UI State for Map Screen
 *
 * @property currentLocation Current device location for map display
 * @property isLoading True when fetching location
 * @property error Error message if location fetch failed
 */
data class MapUiState(val currentLocation: LatLng? = null, val isLoading: Boolean = true, val error: String? = null)

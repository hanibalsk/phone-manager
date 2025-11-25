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
import three.two.bit.phonemanager.data.repository.DeviceRepository
import three.two.bit.phonemanager.domain.model.Device
import three.two.bit.phonemanager.location.LocationManager
import javax.inject.Inject

/**
 * Story E3.1/E3.2: MapViewModel
 *
 * Story E3.1: Manages map state and current location
 * Story E3.2: Manages group member locations on map
 * ACs: E3.1.2, E3.1.3, E3.1.5, E3.2.1
 */
@HiltViewModel
class MapViewModel
@Inject
constructor(
    private val locationManager: LocationManager,
    private val deviceRepository: DeviceRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    init {
        loadCurrentLocation()
        loadGroupMembers()
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
     * Story E3.2: Load group members with locations (AC E3.2.1, E3.2.5)
     */
    private fun loadGroupMembers() {
        viewModelScope.launch {
            val result = deviceRepository.getGroupMembers()

            result.fold(
                onSuccess = { devices ->
                    _uiState.update {
                        it.copy(groupMembers = devices)
                    }
                },
                onFailure = { exception ->
                    // Log error but don't block map display
                    _uiState.update {
                        it.copy(
                            error = exception.message ?: "Failed to load group members",
                        )
                    }
                },
            )
        }
    }

    /**
     * Refresh location and group members
     */
    fun refresh() {
        loadCurrentLocation()
        loadGroupMembers()
    }
}

/**
 * UI State for Map Screen
 *
 * @property currentLocation Current device location for map display
 * @property groupMembers List of group member devices with locations (Story E3.2)
 * @property isLoading True when fetching location
 * @property error Error message if location fetch failed
 */
data class MapUiState(
    val currentLocation: LatLng? = null,
    val groupMembers: List<Device> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

package three.two.bit.phonemanager.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import three.two.bit.phonemanager.data.preferences.PreferencesRepository
import three.two.bit.phonemanager.data.repository.DeviceRepository
import three.two.bit.phonemanager.domain.model.Device
import three.two.bit.phonemanager.location.LocationManager
import timber.log.Timber
import javax.inject.Inject

/**
 * Story E3.1/E3.2/E3.3: MapViewModel
 *
 * Story E3.1: Manages map state and current location
 * Story E3.2: Manages group member locations on map
 * Story E3.3: Implements real-time location polling
 * ACs: E3.1.2, E3.1.3, E3.1.5, E3.2.1, E3.3.1, E3.3.3, E3.3.4, E3.3.5, E3.3.6
 */
@HiltViewModel
class MapViewModel
@Inject
constructor(
    private val locationManager: LocationManager,
    private val deviceRepository: DeviceRepository,
    private val preferencesRepository: PreferencesRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null

    init {
        loadCurrentLocation()
        loadGroupMembers()
    }

    /**
     * Story E3.3: Start periodic polling for group member locations (AC E3.3.1, E3.3.6)
     * Should be called when Map screen becomes visible
     */
    fun startPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (isActive) {
                val intervalSeconds = preferencesRepository.mapPollingIntervalSeconds.first()
                delay(intervalSeconds * 1000L)
                fetchGroupMembersForPolling()
            }
        }
        Timber.d("Map polling started")
    }

    /**
     * Story E3.3: Stop polling (AC E3.3.6)
     * Should be called when Map screen is hidden
     */
    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
        Timber.d("Map polling stopped")
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
                        it.copy(
                            groupMembers = devices,
                            lastPolledAt = Clock.System.now(),
                        )
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
     * Story E3.3: Fetch group members during polling (AC E3.3.1, E3.3.2, E3.3.3, E3.3.4)
     */
    private suspend fun fetchGroupMembersForPolling() {
        val result = deviceRepository.getGroupMembers()

        result.fold(
            onSuccess = { devices ->
                _uiState.update {
                    it.copy(
                        groupMembers = devices,
                        lastPolledAt = Clock.System.now(), // AC E3.3.3: Track last update time
                    )
                }
            },
            onFailure = { exception ->
                // AC E3.3.4: Fail gracefully, continue polling
                Timber.e(exception, "Failed to poll group members")
                // Don't update state - keep showing last known positions
            },
        )
    }

    /**
     * Refresh location and group members (manual refresh)
     */
    fun refresh() {
        loadCurrentLocation()
        loadGroupMembers()
    }

    override fun onCleared() {
        stopPolling()
        super.onCleared()
    }
}

/**
 * UI State for Map Screen
 *
 * @property currentLocation Current device location for map display
 * @property groupMembers List of group member devices with locations (Story E3.2)
 * @property lastPolledAt Timestamp of last successful poll (Story E3.3)
 * @property isLoading True when fetching location
 * @property error Error message if location fetch failed
 */
data class MapUiState(
    val currentLocation: LatLng? = null,
    val groupMembers: List<Device> = emptyList(),
    val lastPolledAt: Instant? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
)

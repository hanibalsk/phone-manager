package three.two.bit.phonemanager.ui.admin

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import three.two.bit.phonemanager.data.repository.DeviceRepository
import three.two.bit.phonemanager.domain.model.Device
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.Instant

/**
 * Story E9.3: User Location Map ViewModel
 * Story E9.5: Remote Tracking Control
 *
 * Manages UI state for viewing a single user's location on a map.
 * Supports real-time polling for location updates and tracking toggle.
 *
 * ACs: E9.3.4, E9.3.5, E9.3.6, E9.5.1-E9.5.5
 */
@HiltViewModel
class UserLocationMapViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val deviceRepository: DeviceRepository,
) : ViewModel() {

    private val groupId: String = savedStateHandle.get<String>("groupId") ?: ""
    private val deviceId: String = savedStateHandle.get<String>("deviceId") ?: ""

    private val _uiState = MutableStateFlow(UserLocationMapUiState())
    val uiState: StateFlow<UserLocationMapUiState> = _uiState.asStateFlow()

    private var isPolling = false

    init {
        if (groupId.isNotEmpty() && deviceId.isNotEmpty()) {
            loadDeviceLocation()
            loadTrackingState()
        } else {
            _uiState.update { it.copy(error = "Invalid device or group ID") }
        }
    }

    /**
     * Story E9.5: Load current tracking state
     *
     * AC E9.5.5: Current tracking state reflected in UI
     */
    private fun loadTrackingState() {
        viewModelScope.launch {
            deviceRepository.getDeviceTrackingSettings(deviceId)
                .onSuccess { settings ->
                    Timber.d("Device $deviceId tracking enabled: ${settings.trackingEnabled}")
                    _uiState.update {
                        it.copy(trackingEnabled = settings.trackingEnabled)
                    }
                }
                .onFailure { error ->
                    Timber.w(error, "Failed to load tracking state")
                    // Don't show error, just leave tracking state unknown
                }
        }
    }

    /**
     * Story E9.5: Toggle device tracking
     *
     * AC E9.5.1: Toggle control on user detail screen
     * AC E9.5.2: Backend API to update tracking state
     * AC E9.5.3: Visual confirmation of tracking state change
     */
    fun toggleTracking(enabled: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isTogglingTracking = true) }

            deviceRepository.toggleDeviceTracking(deviceId, enabled)
                .onSuccess {
                    Timber.i("Tracking toggled for device $deviceId: enabled=$enabled")
                    _uiState.update {
                        it.copy(
                            isTogglingTracking = false,
                            trackingEnabled = enabled,
                            trackingToggleSuccess = true,
                        )
                    }
                }
                .onFailure { error ->
                    Timber.e(error, "Failed to toggle tracking for device $deviceId")
                    _uiState.update {
                        it.copy(
                            isTogglingTracking = false,
                            trackingToggleError = error.message ?: "Failed to update tracking",
                        )
                    }
                }
        }
    }

    /**
     * Clear tracking toggle success state
     */
    fun clearTrackingToggleSuccess() {
        _uiState.update { it.copy(trackingToggleSuccess = false) }
    }

    /**
     * Clear tracking toggle error
     */
    fun clearTrackingToggleError() {
        _uiState.update { it.copy(trackingToggleError = null) }
    }

    /**
     * Load device location from repository
     *
     * AC E9.3.4: Same UI as "My Device" screen (reused map component)
     */
    fun loadDeviceLocation() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            deviceRepository.getGroupDevices(groupId)
                .onSuccess { devices ->
                    val device = devices.find { it.deviceId == deviceId }
                    if (device != null) {
                        Timber.d("Found device: ${device.displayName}")
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                device = device,
                                location = device.lastLocation?.let { loc ->
                                    LatLng(loc.latitude, loc.longitude)
                                },
                                lastSeenAt = device.lastSeenAt,
                            )
                        }
                    } else {
                        Timber.w("Device $deviceId not found in group $groupId")
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = "Device not found",
                            )
                        }
                    }
                }
                .onFailure { error ->
                    Timber.w(error, "Failed to load device location")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Failed to load location",
                        )
                    }
                }
        }
    }

    /**
     * Start polling for location updates
     *
     * AC E9.3.4: Real-time polling for location updates (like E3.3)
     */
    fun startPolling() {
        if (isPolling) return
        isPolling = true

        viewModelScope.launch {
            while (isActive && isPolling) {
                delay(POLLING_INTERVAL_MS)
                if (isPolling) {
                    loadDeviceLocation()
                }
            }
        }
    }

    /**
     * Stop polling for location updates
     */
    fun stopPolling() {
        isPolling = false
    }

    companion object {
        private const val POLLING_INTERVAL_MS = 30_000L // 30 seconds
    }
}

/**
 * UI State for User Location Map screen
 *
 * @property device The device being viewed
 * @property location Current location as LatLng
 * @property lastSeenAt Last seen timestamp
 * @property isLoading True when loading data
 * @property error Error message if any
 * @property trackingEnabled Whether tracking is enabled for this device
 * @property isTogglingTracking True when tracking toggle is in progress
 * @property trackingToggleSuccess True when tracking was successfully toggled
 * @property trackingToggleError Error message if tracking toggle failed
 */
data class UserLocationMapUiState(
    val device: Device? = null,
    val location: LatLng? = null,
    val lastSeenAt: Instant? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val trackingEnabled: Boolean? = null,
    val isTogglingTracking: Boolean = false,
    val trackingToggleSuccess: Boolean = false,
    val trackingToggleError: String? = null,
)

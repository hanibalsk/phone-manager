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
import three.two.bit.phonemanager.data.repository.GroupRepository
import three.two.bit.phonemanager.domain.model.Device
import three.two.bit.phonemanager.domain.model.GroupMembership
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.Instant

/**
 * Story E9.3: User Home ViewModel
 *
 * Manages UI state for viewing another user's home screen data.
 * Fetches user's device info, location, and provides admin controls.
 *
 * ACs: E9.3.1-E9.3.6 (View User Location)
 */
@HiltViewModel
class UserHomeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val groupRepository: GroupRepository,
    private val deviceRepository: DeviceRepository,
) : ViewModel() {

    private val groupId: String = savedStateHandle.get<String>("groupId") ?: ""
    private val userId: String = savedStateHandle.get<String>("userId") ?: ""

    private val _uiState = MutableStateFlow(UserHomeUiState())
    val uiState: StateFlow<UserHomeUiState> = _uiState.asStateFlow()

    private var isPolling = false

    init {
        Timber.d("UserHomeViewModel init: groupId=$groupId, userId=$userId")
        if (groupId.isNotEmpty() && userId.isNotEmpty()) {
            loadUserData()
        } else {
            _uiState.update { it.copy(error = "Invalid user or group ID") }
        }
    }

    /**
     * Load user's member info and their device(s)
     */
    private fun loadUserData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            // First, get the user's membership info from the group
            groupRepository.getGroupMembers(groupId)
                .onSuccess { members ->
                    val member = members.find { it.userId == userId }
                    if (member != null) {
                        Timber.d("Found member: ${member.displayName}")
                        _uiState.update { it.copy(member = member) }

                        // Now load the user's device(s)
                        loadUserDevices()
                    } else {
                        Timber.w("User $userId not found in group $groupId")
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = "User not found in this group"
                            )
                        }
                    }
                }
                .onFailure { error ->
                    Timber.e(error, "Failed to load group members")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Failed to load user data"
                        )
                    }
                }
        }
    }

    /**
     * Load user's devices from the group
     */
    private fun loadUserDevices() {
        viewModelScope.launch {
            // Get all devices in the group
            deviceRepository.getGroupDevices(groupId)
                .onSuccess { devices ->
                    // Filter devices owned by this user
                    val userDevices = devices.filter { it.ownerId == userId }
                    Timber.d("Found ${userDevices.size} devices for user $userId")

                    if (userDevices.isNotEmpty()) {
                        val primaryDevice = userDevices.first()
                        val location = primaryDevice.lastLocation?.let {
                            LatLng(it.latitude, it.longitude)
                        }

                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                devices = userDevices,
                                primaryDevice = primaryDevice,
                                location = location,
                                lastSeenAt = primaryDevice.lastSeenAt,
                                error = null,
                            )
                        }

                        // Load tracking settings for the device
                        loadTrackingSettings(primaryDevice.deviceId)

                        // Start polling for location updates
                        startLocationPolling()
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                noDevicesRegistered = true,
                                error = null,
                            )
                        }
                    }
                }
                .onFailure { error ->
                    Timber.e(error, "Failed to load user devices")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Failed to load user devices"
                        )
                    }
                }
        }
    }

    /**
     * Load tracking settings for a device
     */
    private fun loadTrackingSettings(deviceId: String) {
        viewModelScope.launch {
            deviceRepository.getDeviceTrackingSettings(deviceId)
                .onSuccess { settings ->
                    Timber.d("Loaded tracking settings for device $deviceId: enabled=${settings.trackingEnabled}")
                    _uiState.update {
                        it.copy(trackingEnabled = settings.trackingEnabled)
                    }
                }
                .onFailure { error ->
                    Timber.w(error, "Failed to load tracking settings for device $deviceId")
                    // Don't show error, just leave tracking as unknown
                }
        }
    }

    /**
     * Start polling for location updates
     */
    private fun startLocationPolling() {
        if (isPolling) return
        isPolling = true

        viewModelScope.launch {
            while (isActive && isPolling) {
                delay(30_000) // Poll every 30 seconds
                refreshLocation()
            }
        }
    }

    /**
     * Stop polling for location updates
     */
    fun stopPolling() {
        isPolling = false
    }

    /**
     * Refresh user's location
     */
    fun refreshLocation() {
        val deviceId = _uiState.value.primaryDevice?.deviceId ?: return

        viewModelScope.launch {
            deviceRepository.getGroupDevices(groupId)
                .onSuccess { devices ->
                    val device = devices.find { it.deviceId == deviceId }
                    if (device != null) {
                        val location = device.lastLocation?.let {
                            LatLng(it.latitude, it.longitude)
                        }
                        _uiState.update {
                            it.copy(
                                primaryDevice = device,
                                location = location,
                                lastSeenAt = device.lastSeenAt,
                            )
                        }
                    }
                }
                .onFailure { error ->
                    Timber.w(error, "Failed to refresh location")
                }
        }
    }

    /**
     * Story E9.5: Toggle tracking for the user's device
     */
    fun toggleTracking() {
        val device = _uiState.value.primaryDevice ?: return
        val newState = !(_uiState.value.trackingEnabled ?: true)

        viewModelScope.launch {
            _uiState.update { it.copy(isTogglingTracking = true) }

            deviceRepository.toggleDeviceTracking(device.deviceId, newState)
                .onSuccess {
                    Timber.i("Tracking ${if (newState) "enabled" else "disabled"} for device ${device.deviceId}")
                    _uiState.update {
                        it.copy(
                            trackingEnabled = newState,
                            isTogglingTracking = false,
                            trackingToggleSuccess = true,
                        )
                    }
                }
                .onFailure { error ->
                    Timber.e(error, "Failed to toggle tracking")
                    _uiState.update {
                        it.copy(
                            isTogglingTracking = false,
                            trackingToggleError = error.message ?: "Failed to toggle tracking",
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
     * Clear tracking toggle error state
     */
    fun clearTrackingToggleError() {
        _uiState.update { it.copy(trackingToggleError = null) }
    }

    /**
     * Retry loading data
     */
    fun retry() {
        loadUserData()
    }

    override fun onCleared() {
        super.onCleared()
        stopPolling()
    }
}

/**
 * UI State for User Home Screen
 */
data class UserHomeUiState(
    val member: GroupMembership? = null,
    val devices: List<Device> = emptyList(),
    val primaryDevice: Device? = null,
    val location: LatLng? = null,
    val lastSeenAt: Instant? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val noDevicesRegistered: Boolean = false,
    val trackingEnabled: Boolean? = null,
    val isTogglingTracking: Boolean = false,
    val trackingToggleSuccess: Boolean = false,
    val trackingToggleError: String? = null,
)

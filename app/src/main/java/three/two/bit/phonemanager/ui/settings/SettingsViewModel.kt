package three.two.bit.phonemanager.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import three.two.bit.phonemanager.data.preferences.PreferencesRepository
import three.two.bit.phonemanager.data.repository.AuthRepository
import three.two.bit.phonemanager.data.repository.DeviceRepository
import three.two.bit.phonemanager.domain.auth.User
import three.two.bit.phonemanager.permission.PermissionManager
import javax.inject.Inject

/**
 * Story E1.3/E3.3/E7.4/E9.11: SettingsViewModel
 *
 * Manages device settings (displayName, groupId) and handles re-registration
 * Story E3.3: Also manages map polling interval setting (AC E3.3.5)
 * Story E7.4: Also manages weather notification toggle (AC E7.4.5)
 * Story E9.11: Also manages authentication state and logout (AC E9.11.6, E9.11.8)
 * Movement detection: Also manages movement detection settings and permissions
 * ACs: E1.3.2, E1.3.3, E1.3.4, E3.3.5, E7.4.5, E9.11.6, E9.11.8
 */
@HiltViewModel
class SettingsViewModel
@Inject
constructor(
    private val deviceRepository: DeviceRepository,
    private val preferencesRepository: PreferencesRepository,
    private val permissionManager: PermissionManager,
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    val deviceId: String = deviceRepository.getDeviceId()

    // Story E7.4: Weather notification toggle state (AC E7.4.5)
    val showWeatherInNotification: StateFlow<Boolean> =
        preferencesRepository.showWeatherInNotification
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = true,
            )

    // Movement detection settings
    val isMovementDetectionEnabled: StateFlow<Boolean> =
        preferencesRepository.isMovementDetectionEnabled
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = false,
            )

    val isActivityRecognitionEnabled: StateFlow<Boolean> =
        preferencesRepository.isActivityRecognitionEnabled
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = true,
            )

    val isBluetoothCarDetectionEnabled: StateFlow<Boolean> =
        preferencesRepository.isBluetoothCarDetectionEnabled
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = true,
            )

    val isAndroidAutoDetectionEnabled: StateFlow<Boolean> =
        preferencesRepository.isAndroidAutoDetectionEnabled
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = true,
            )

    val vehicleIntervalMultiplier: StateFlow<Float> =
        preferencesRepository.vehicleIntervalMultiplier
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = 0.55f,
            )

    val defaultIntervalMultiplier: StateFlow<Float> =
        preferencesRepository.defaultIntervalMultiplier
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = 1.0f,
            )

    // Story E8.12: Trip Detection settings
    val isTripDetectionEnabled: StateFlow<Boolean> =
        preferencesRepository.isTripDetectionEnabled
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = true,
            )

    val tripStationaryThreshold: StateFlow<Int> =
        preferencesRepository.tripStationaryThresholdMinutes
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = 5,
            )

    val tripMinimumDuration: StateFlow<Int> =
        preferencesRepository.tripMinimumDurationMinutes
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = 2,
            )

    val tripMinimumDistance: StateFlow<Int> =
        preferencesRepository.tripMinimumDistanceMeters
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = 100,
            )

    val isTripAutoMergeEnabled: StateFlow<Boolean> =
        preferencesRepository.isTripAutoMergeEnabled
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = true,
            )

    // Story E9.11: Authentication state (AC E9.11.6, E9.11.8)
    val currentUser: StateFlow<User?> =
        authRepository.currentUser
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = null,
            )

    val isLoggedIn: Boolean
        get() = authRepository.isLoggedIn()

    // Movement detection permission states
    private val _movementPermissionState = MutableStateFlow(MovementPermissionState())
    val movementPermissionState: StateFlow<MovementPermissionState> = _movementPermissionState.asStateFlow()

    private var originalDisplayName: String = ""
    private var originalGroupId: String = ""

    init {
        loadCurrentSettings()
        loadPollingInterval()
        updateMovementPermissionState()
    }

    /**
     * Load current settings from SecureStorage (AC E1.3.4)
     */
    private fun loadCurrentSettings() {
        viewModelScope.launch {
            val displayName = deviceRepository.getDisplayName() ?: ""
            val groupId = deviceRepository.getGroupId() ?: ""

            originalDisplayName = displayName
            originalGroupId = groupId

            _uiState.update {
                it.copy(
                    displayName = displayName,
                    groupId = groupId,
                    isLoading = false,
                )
            }
        }
    }

    /**
     * Load map polling interval from preferences (AC E3.3.5)
     */
    private fun loadPollingInterval() {
        viewModelScope.launch {
            val interval = preferencesRepository.mapPollingIntervalSeconds.first()
            _uiState.update { it.copy(mapPollingIntervalSeconds = interval) }
        }
    }

    /**
     * Handle polling interval change (AC E3.3.5)
     * Saves immediately since this is an independent setting
     */
    fun onPollingIntervalChanged(seconds: Int) {
        viewModelScope.launch {
            preferencesRepository.setMapPollingIntervalSeconds(seconds)
            _uiState.update { it.copy(mapPollingIntervalSeconds = seconds) }
        }
    }

    /**
     * Handle weather notification toggle change (AC E7.4.5)
     * Saves immediately and triggers notification update in LocationTrackingService
     */
    fun setShowWeatherInNotification(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setShowWeatherInNotification(enabled)
        }
    }

    // Movement detection methods

    /**
     * Update movement detection permission state
     * Call this when permissions may have changed (e.g., returning from permission request)
     */
    fun updateMovementPermissionState() {
        _movementPermissionState.value = MovementPermissionState(
            hasActivityRecognitionPermission = permissionManager.hasActivityRecognitionPermission(),
            hasBluetoothConnectPermission = permissionManager.hasBluetoothConnectPermission(),
        )
    }

    /**
     * Toggle movement detection on/off
     * Returns true if permissions need to be requested before enabling
     */
    fun setMovementDetectionEnabled(enabled: Boolean): Boolean {
        if (enabled) {
            // Check if we have the necessary permissions
            val hasActivityPermission = permissionManager.hasActivityRecognitionPermission()
            val hasBluetoothPermission = permissionManager.hasBluetoothConnectPermission()

            if (!hasActivityPermission || !hasBluetoothPermission) {
                // Signal that permissions need to be requested
                return true
            }
        }

        viewModelScope.launch {
            preferencesRepository.setMovementDetectionEnabled(enabled)
        }
        return false
    }

    /**
     * Enable movement detection after permissions are granted
     */
    fun enableMovementDetectionAfterPermission() {
        viewModelScope.launch {
            preferencesRepository.setMovementDetectionEnabled(true)
        }
        updateMovementPermissionState()
    }

    /**
     * Toggle activity recognition detection
     */
    fun setActivityRecognitionEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setActivityRecognitionEnabled(enabled)
        }
    }

    /**
     * Toggle Bluetooth car detection
     */
    fun setBluetoothCarDetectionEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setBluetoothCarDetectionEnabled(enabled)
        }
    }

    /**
     * Toggle Android Auto detection
     */
    fun setAndroidAutoDetectionEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setAndroidAutoDetectionEnabled(enabled)
        }
    }

    /**
     * Set vehicle interval multiplier (0.1 to 1.0)
     */
    fun setVehicleIntervalMultiplier(multiplier: Float) {
        viewModelScope.launch {
            preferencesRepository.setVehicleIntervalMultiplier(multiplier.coerceIn(0.1f, 1.0f))
        }
    }

    /**
     * Set default interval multiplier (0.1 to 2.0)
     */
    fun setDefaultIntervalMultiplier(multiplier: Float) {
        viewModelScope.launch {
            preferencesRepository.setDefaultIntervalMultiplier(multiplier.coerceIn(0.1f, 2.0f))
        }
    }

    // Story E8.12: Trip Detection setting methods

    /**
     * Toggle trip detection on/off (AC E8.12.2)
     */
    fun setTripDetectionEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setTripDetectionEnabled(enabled)
        }
    }

    /**
     * Set stationary threshold for trip end (AC E8.12.3)
     */
    fun setTripStationaryThreshold(minutes: Int) {
        viewModelScope.launch {
            preferencesRepository.setTripStationaryThresholdMinutes(minutes)
        }
    }

    /**
     * Set minimum trip duration (AC E8.12.4)
     */
    fun setTripMinimumDuration(minutes: Int) {
        viewModelScope.launch {
            preferencesRepository.setTripMinimumDurationMinutes(minutes.coerceIn(1, 10))
        }
    }

    /**
     * Set minimum trip distance (AC E8.12.5)
     */
    fun setTripMinimumDistance(meters: Int) {
        viewModelScope.launch {
            preferencesRepository.setTripMinimumDistanceMeters(meters.coerceIn(50, 500))
        }
    }

    /**
     * Toggle trip auto-merge (AC E8.12.6)
     */
    fun setTripAutoMergeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setTripAutoMergeEnabled(enabled)
        }
    }

    /**
     * Handle display name text change
     */
    fun onDisplayNameChanged(newName: String) {
        _uiState.update {
            it.copy(
                displayName = newName,
                displayNameError = null,
                hasChanges = newName != originalDisplayName || it.groupId != originalGroupId,
            )
        }
        validateForm()
    }

    /**
     * Handle group ID text change
     */
    fun onGroupIdChanged(newGroupId: String) {
        _uiState.update {
            it.copy(
                groupId = newGroupId,
                groupIdError = null,
                hasChanges = it.displayName != originalDisplayName || newGroupId != originalGroupId,
            )
        }
        validateForm()
    }

    /**
     * Save settings and re-register with server (AC E1.3.2, E1.3.3)
     */
    fun onSaveClicked() {
        if (!validateForm()) return

        val currentState = _uiState.value

        // Check if group ID changed - show confirmation dialog
        if (currentState.groupId.trim() != originalGroupId) {
            _uiState.update { it.copy(showGroupChangeConfirmation = true) }
            return
        }

        performSave()
    }

    /**
     * Dismiss group change confirmation dialog
     */
    fun onDismissGroupChangeConfirmation() {
        _uiState.update { it.copy(showGroupChangeConfirmation = false) }
    }

    /**
     * Confirm group change and proceed with save
     */
    fun onConfirmGroupChange() {
        _uiState.update { it.copy(showGroupChangeConfirmation = false) }
        performSave()
    }

    /**
     * Perform the actual save operation
     */
    private fun performSave() {
        val currentState = _uiState.value

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            // Re-register with new settings (AC E1.3.2, E1.3.3)
            val result =
                deviceRepository.registerDevice(
                    displayName = currentState.displayName.trim(),
                    groupId = currentState.groupId.trim(),
                )

            result.fold(
                onSuccess = {
                    // Settings persisted to SecureStorage by repository (AC E1.3.4)
                    originalDisplayName = currentState.displayName.trim()
                    originalGroupId = currentState.groupId.trim()

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            saveSuccess = true,
                            hasChanges = false,
                        )
                    }
                },
                onFailure = { exception ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = exception.message ?: "Failed to save settings",
                        )
                    }
                },
            )
        }
    }

    /**
     * Validate form fields (matches RegistrationViewModel validation)
     * @return true if form is valid
     */
    private fun validateForm(): Boolean {
        val state = _uiState.value
        var isValid = true

        // Validate display name (matching RegistrationViewModel.kt:88-93)
        val displayNameError = when {
            state.displayName.isBlank() -> "Display name is required"
            state.displayName.trim().length < 2 -> "Display name must be at least 2 characters"
            state.displayName.trim().length > 50 -> "Display name must be 50 characters or less"
            else -> null
        }

        // Validate group ID (matching RegistrationViewModel.kt:96-104)
        val groupIdRegex = Regex("^[a-zA-Z0-9-]+$")
        val groupIdError = when {
            state.groupId.isBlank() -> "Group ID is required"
            state.groupId.trim().length < 2 -> "Group ID must be at least 2 characters"
            state.groupId.trim().length > 50 -> "Group ID must be 50 characters or less"
            !state.groupId.trim().matches(groupIdRegex) ->
                "Group ID can only contain letters, numbers, and hyphens"

            else -> null
        }

        if (displayNameError != null || groupIdError != null) {
            isValid = false
        }

        _uiState.update {
            it.copy(
                displayNameError = displayNameError,
                groupIdError = groupIdError,
                isFormValid = isValid,
            )
        }

        return isValid
    }

    /**
     * Story E9.11: Logout user (AC E9.11.6)
     *
     * Clears tokens and session state.
     */
    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }
}

/**
 * UI State for Settings Screen
 *
 * @property displayName Current display name value
 * @property groupId Current group ID value
 * @property displayNameError Validation error for display name
 * @property groupIdError Validation error for group ID
 * @property isFormValid True when all validation passes
 * @property isLoading True when saving settings
 * @property error Error message if save failed, null otherwise
 * @property saveSuccess True when settings saved successfully
 * @property hasChanges True when values differ from original
 * @property showGroupChangeConfirmation True when group ID change confirmation needed
 * @property mapPollingIntervalSeconds Map polling interval in seconds (AC E3.3.5)
 */
data class SettingsUiState(
    val displayName: String = "",
    val groupId: String = "",
    val displayNameError: String? = null,
    val groupIdError: String? = null,
    val isFormValid: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null,
    val saveSuccess: Boolean = false,
    val hasChanges: Boolean = false,
    val showGroupChangeConfirmation: Boolean = false,
    val mapPollingIntervalSeconds: Int = 15,
)

/**
 * State for movement detection permissions
 */
data class MovementPermissionState(
    val hasActivityRecognitionPermission: Boolean = true,
    val hasBluetoothConnectPermission: Boolean = true,
) {
    val hasBothPermissions: Boolean
        get() = hasActivityRecognitionPermission && hasBluetoothConnectPermission
}

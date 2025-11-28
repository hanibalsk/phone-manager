package three.two.bit.phonemanager.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import three.two.bit.phonemanager.data.preferences.PreferencesRepository
import three.two.bit.phonemanager.data.repository.DeviceRepository
import javax.inject.Inject

/**
 * Story E1.3/E3.3: SettingsViewModel
 *
 * Manages device settings (displayName, groupId) and handles re-registration
 * Story E3.3: Also manages map polling interval setting (AC E3.3.5)
 * ACs: E1.3.2, E1.3.3, E1.3.4, E3.3.5
 */
@HiltViewModel
class SettingsViewModel
@Inject
constructor(
    private val deviceRepository: DeviceRepository,
    private val preferencesRepository: PreferencesRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    val deviceId: String = deviceRepository.getDeviceId()

    private var originalDisplayName: String = ""
    private var originalGroupId: String = ""

    init {
        loadCurrentSettings()
        loadPollingInterval()
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

package three.two.bit.phonemanager.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import three.two.bit.phonemanager.data.repository.DeviceRepository
import javax.inject.Inject

/**
 * Story E1.3: SettingsViewModel
 *
 * Manages device settings (displayName, groupId) and handles re-registration
 * ACs: E1.3.2, E1.3.3, E1.3.4
 */
@HiltViewModel
class SettingsViewModel
@Inject
constructor(private val deviceRepository: DeviceRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private var originalDisplayName: String = ""
    private var originalGroupId: String = ""

    init {
        loadCurrentSettings()
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
     * Handle display name text change
     */
    fun onDisplayNameChanged(newName: String) {
        _uiState.update {
            it.copy(
                displayName = newName,
                hasChanges = newName != originalDisplayName || it.groupId != originalGroupId,
            )
        }
    }

    /**
     * Handle group ID text change
     */
    fun onGroupIdChanged(newGroupId: String) {
        _uiState.update {
            it.copy(
                groupId = newGroupId,
                hasChanges = it.displayName != originalDisplayName || newGroupId != originalGroupId,
            )
        }
    }

    /**
     * Save settings and re-register with server (AC E1.3.2, E1.3.3)
     */
    fun onSaveClicked() {
        val currentState = _uiState.value

        // Validation
        if (currentState.displayName.isBlank()) {
            _uiState.update { it.copy(error = "Display name cannot be empty") }
            return
        }

        if (currentState.groupId.isBlank()) {
            _uiState.update { it.copy(error = "Group ID cannot be empty") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            // Re-register with new settings (AC E1.3.2, E1.3.3)
            val result =
                deviceRepository.registerDevice(
                    displayName = currentState.displayName,
                    groupId = currentState.groupId,
                )

            result.fold(
                onSuccess = {
                    // Settings persisted to SecureStorage by repository (AC E1.3.4)
                    originalDisplayName = currentState.displayName
                    originalGroupId = currentState.groupId

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
}

/**
 * UI State for Settings Screen
 *
 * @property displayName Current display name value
 * @property groupId Current group ID value
 * @property isLoading True when saving settings
 * @property error Error message if save failed, null otherwise
 * @property saveSuccess True when settings saved successfully
 * @property hasChanges True when values differ from original
 */
data class SettingsUiState(
    val displayName: String = "",
    val groupId: String = "",
    val isLoading: Boolean = true,
    val error: String? = null,
    val saveSuccess: Boolean = false,
    val hasChanges: Boolean = false,
)

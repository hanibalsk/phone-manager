package three.two.bit.phonemanager.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import three.two.bit.phonemanager.data.repository.AdminSettingsRepository
import three.two.bit.phonemanager.domain.model.BulkSettingsResult
import three.two.bit.phonemanager.domain.model.SettingDefinition
import timber.log.Timber
import javax.inject.Inject

/**
 * Story E12.7: ViewModel for Bulk Settings Screen
 *
 * Manages bulk settings updates across multiple devices.
 *
 * AC E12.7.6: Bulk Settings Application
 */
@HiltViewModel
class BulkSettingsViewModel @Inject constructor(
    private val adminSettingsRepository: AdminSettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BulkSettingsUiState())
    val uiState: StateFlow<BulkSettingsUiState> = _uiState.asStateFlow()

    /**
     * Set the selected devices for bulk update.
     */
    fun setSelectedDevices(deviceIds: List<String>) {
        _uiState.update { it.copy(selectedDeviceIds = deviceIds) }
    }

    /**
     * Toggle a setting selection.
     */
    fun toggleSetting(key: String) {
        _uiState.update { state ->
            val selectedSettings = state.selectedSettings.toMutableSet()
            val settingValues = state.settingValues.toMutableMap()

            if (selectedSettings.contains(key)) {
                selectedSettings.remove(key)
                settingValues.remove(key)
            } else {
                selectedSettings.add(key)
                // Set default value if not already set
                if (!settingValues.containsKey(key)) {
                    val definition = SettingDefinition.forKey(key)
                    if (definition != null) {
                        settingValues[key] = definition.defaultValue
                    }
                }
            }

            state.copy(
                selectedSettings = selectedSettings,
                settingValues = settingValues,
            )
        }
    }

    /**
     * Set a setting value.
     */
    fun setSettingValue(key: String, value: Any) {
        _uiState.update { state ->
            state.copy(
                settingValues = state.settingValues + (key to value),
            )
        }
    }

    /**
     * Toggle whether a setting should be locked.
     */
    fun toggleLock(key: String) {
        _uiState.update { state ->
            val lockedSettings = state.lockedSettings.toMutableSet()
            if (lockedSettings.contains(key)) {
                lockedSettings.remove(key)
            } else {
                lockedSettings.add(key)
            }
            state.copy(lockedSettings = lockedSettings)
        }
    }

    /**
     * Set notify users preference.
     */
    fun setNotifyUsers(notify: Boolean) {
        _uiState.update { it.copy(notifyUsers = notify) }
    }

    /**
     * Apply settings to selected devices.
     */
    fun applySettings() {
        val state = _uiState.value
        if (state.selectedSettings.isEmpty() || state.selectedDeviceIds.isEmpty()) {
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isApplying = true, error = null) }

            // Build settings map from selected settings
            val settings = state.selectedSettings.associateWith { key ->
                state.settingValues[key] ?: SettingDefinition.forKey(key)?.defaultValue ?: false
            }

            // Get locks list
            val locks = state.lockedSettings.toList().ifEmpty { null }

            adminSettingsRepository.bulkUpdateDevices(
                deviceIds = state.selectedDeviceIds,
                settings = settings,
                locks = locks,
                notifyUsers = state.notifyUsers,
            ).fold(
                onSuccess = { result ->
                    _uiState.update {
                        it.copy(
                            isApplying = false,
                            result = result,
                            applyComplete = true,
                        )
                    }
                    Timber.i(
                        "Bulk update complete: ${result.successCount} success, " +
                            "${result.failureCount} failed",
                    )
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isApplying = false,
                            error = error.message ?: "Failed to apply settings",
                        )
                    }
                    Timber.e(error, "Failed to apply bulk settings")
                },
            )
        }
    }

    /**
     * Clear error message.
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Reset state for new operation.
     */
    fun reset() {
        _uiState.update {
            BulkSettingsUiState(selectedDeviceIds = it.selectedDeviceIds)
        }
    }
}

/**
 * UI state for Bulk Settings screen.
 */
data class BulkSettingsUiState(
    val selectedDeviceIds: List<String> = emptyList(),
    val selectedSettings: Set<String> = emptySet(),
    val settingValues: Map<String, Any> = emptyMap(),
    val lockedSettings: Set<String> = emptySet(),
    val notifyUsers: Boolean = true,
    val isApplying: Boolean = false,
    val error: String? = null,
    val result: BulkSettingsResult? = null,
    val applyComplete: Boolean = false,
) {
    val deviceCount: Int get() = selectedDeviceIds.size
}

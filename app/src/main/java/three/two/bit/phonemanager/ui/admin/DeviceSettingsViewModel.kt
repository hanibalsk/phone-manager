package three.two.bit.phonemanager.ui.admin

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import three.two.bit.phonemanager.data.repository.AdminSettingsRepository
import three.two.bit.phonemanager.domain.model.MemberDeviceSettings
import three.two.bit.phonemanager.domain.model.SettingCategory
import three.two.bit.phonemanager.domain.model.SettingChange
import three.two.bit.phonemanager.domain.model.SettingDefinition
import three.two.bit.phonemanager.domain.model.validateSettingValue
import timber.log.Timber
import javax.inject.Inject

/**
 * Story E12.7: ViewModel for Device Settings Screen
 *
 * Manages viewing and modifying settings for a specific device.
 *
 * AC E12.7.2: Settings View (grouped by category)
 * AC E12.7.3: View Remote Settings
 * AC E12.7.4: Modify Remote Settings
 * AC E12.7.5: Lock/Unlock Settings
 * AC E12.7.8: Audit Trail
 */
@HiltViewModel
class DeviceSettingsViewModel @Inject constructor(
    private val adminSettingsRepository: AdminSettingsRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val deviceId: String = savedStateHandle.get<String>("deviceId")
        ?: throw IllegalArgumentException("deviceId is required")

    private val _uiState = MutableStateFlow(DeviceSettingsUiState())
    val uiState: StateFlow<DeviceSettingsUiState> = _uiState.asStateFlow()

    init {
        loadDeviceSettings()
    }

    /**
     * Load device settings.
     * AC E12.7.3: View Remote Settings
     */
    fun loadDeviceSettings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            adminSettingsRepository.getDeviceSettings(deviceId).fold(
                onSuccess = { settings ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            deviceSettings = settings,
                            settingsByCategory = groupSettingsByCategory(settings),
                        )
                    }
                    Timber.i("Loaded settings for device $deviceId")
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Failed to load settings",
                        )
                    }
                    Timber.e(error, "Failed to load device settings")
                },
            )
        }
    }

    /**
     * Update a setting value.
     * AC E12.7.4: Modify Remote Settings
     */
    fun updateSetting(key: String, value: Any) {
        // Validate first
        val validationError = validateSettingValue(key, value)
        if (validationError != null) {
            _uiState.update { it.copy(error = validationError) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }

            adminSettingsRepository.updateDeviceSettings(
                deviceId = deviceId,
                changes = mapOf(key to value),
                notifyUser = _uiState.value.notifyUserOnChange,
            ).fold(
                onSuccess = { applied ->
                    _uiState.update { state ->
                        val updatedSettings = state.deviceSettings?.copy(
                            settings = state.deviceSettings.settings + applied,
                        )
                        state.copy(
                            isSaving = false,
                            deviceSettings = updatedSettings,
                            settingsByCategory = updatedSettings?.let { groupSettingsByCategory(it) }
                                ?: emptyMap(),
                            successMessage = "Setting updated successfully",
                        )
                    }
                    Timber.i("Updated setting $key for device $deviceId")
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            error = error.message ?: "Failed to update setting",
                        )
                    }
                    Timber.e(error, "Failed to update setting $key")
                },
            )
        }
    }

    /**
     * Lock a setting.
     * AC E12.7.5: Lock Settings
     */
    fun lockSetting(key: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }

            adminSettingsRepository.lockSettings(deviceId, listOf(key)).fold(
                onSuccess = { count ->
                    // Reload to get updated lock state
                    loadDeviceSettings()
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            successMessage = "Setting locked",
                        )
                    }
                    Timber.i("Locked setting $key for device $deviceId")
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            error = error.message ?: "Failed to lock setting",
                        )
                    }
                    Timber.e(error, "Failed to lock setting $key")
                },
            )
        }
    }

    /**
     * Unlock a setting.
     * AC E12.7.5: Unlock Settings
     */
    fun unlockSetting(key: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }

            adminSettingsRepository.unlockSettings(deviceId, listOf(key)).fold(
                onSuccess = { count ->
                    // Reload to get updated lock state
                    loadDeviceSettings()
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            successMessage = "Setting unlocked",
                        )
                    }
                    Timber.i("Unlocked setting $key for device $deviceId")
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            error = error.message ?: "Failed to unlock setting",
                        )
                    }
                    Timber.e(error, "Failed to unlock setting $key")
                },
            )
        }
    }

    /**
     * Toggle lock state for a setting.
     */
    fun toggleLock(key: String) {
        val isLocked = _uiState.value.deviceSettings?.isLocked(key) ?: false
        if (isLocked) {
            unlockSetting(key)
        } else {
            lockSetting(key)
        }
    }

    /**
     * Lock multiple settings at once.
     */
    fun lockSettings(keys: List<String>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }

            adminSettingsRepository.lockSettings(deviceId, keys).fold(
                onSuccess = { count ->
                    loadDeviceSettings()
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            successMessage = "$count settings locked",
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            error = error.message ?: "Failed to lock settings",
                        )
                    }
                },
            )
        }
    }

    /**
     * Unlock multiple settings at once.
     */
    fun unlockSettings(keys: List<String>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }

            adminSettingsRepository.unlockSettings(deviceId, keys).fold(
                onSuccess = { count ->
                    loadDeviceSettings()
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            successMessage = "$count settings unlocked",
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            error = error.message ?: "Failed to unlock settings",
                        )
                    }
                },
            )
        }
    }

    /**
     * Load settings history.
     * AC E12.7.8: Audit Trail
     */
    fun loadHistory() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingHistory = true) }

            adminSettingsRepository.getSettingsHistory(deviceId).fold(
                onSuccess = { history ->
                    _uiState.update {
                        it.copy(
                            isLoadingHistory = false,
                            history = history,
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoadingHistory = false,
                            error = error.message ?: "Failed to load history",
                        )
                    }
                },
            )
        }
    }

    /**
     * Toggle notify user on change option.
     */
    fun setNotifyUserOnChange(notify: Boolean) {
        _uiState.update { it.copy(notifyUserOnChange = notify) }
    }

    /**
     * Expand/collapse a category.
     */
    fun toggleCategory(category: SettingCategory) {
        _uiState.update { state ->
            val expandedCategories = state.expandedCategories.toMutableSet()
            if (expandedCategories.contains(category)) {
                expandedCategories.remove(category)
            } else {
                expandedCategories.add(category)
            }
            state.copy(expandedCategories = expandedCategories)
        }
    }

    /**
     * Show setting edit dialog.
     */
    fun showEditDialog(key: String) {
        val definition = SettingDefinition.forKey(key)
        val currentValue = _uiState.value.deviceSettings?.getValue(key, definition?.defaultValue)
        _uiState.update {
            it.copy(
                editingSettingKey = key,
                editingSettingValue = currentValue,
            )
        }
    }

    /**
     * Hide setting edit dialog.
     */
    fun hideEditDialog() {
        _uiState.update {
            it.copy(
                editingSettingKey = null,
                editingSettingValue = null,
            )
        }
    }

    /**
     * Show lock confirmation dialog.
     */
    fun showLockConfirmation(key: String, lock: Boolean) {
        _uiState.update {
            it.copy(
                confirmLockSettingKey = key,
                confirmLockAction = lock,
            )
        }
    }

    /**
     * Hide lock confirmation dialog.
     */
    fun hideLockConfirmation() {
        _uiState.update {
            it.copy(
                confirmLockSettingKey = null,
                confirmLockAction = null,
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
     * Clear success message.
     */
    fun clearSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }

    /**
     * Group settings by category with their current values.
     */
    private fun groupSettingsByCategory(
        settings: MemberDeviceSettings,
    ): Map<SettingCategory, List<SettingWithValue>> {
        return SettingDefinition.ALL_SETTINGS.map { definition ->
            val value = settings.getValue(definition.key, definition.defaultValue)
            val lock = settings.getLock(definition.key)
            SettingWithValue(
                definition = definition,
                value = value,
                isLocked = lock?.isLocked ?: false,
                lockedBy = lock?.lockedBy,
            )
        }.groupBy { it.definition.category }
    }
}

/**
 * Setting with its current value and lock state.
 */
data class SettingWithValue(
    val definition: SettingDefinition,
    val value: Any,
    val isLocked: Boolean,
    val lockedBy: String?,
)

/**
 * UI state for Device Settings screen.
 */
data class DeviceSettingsUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isLoadingHistory: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val deviceSettings: MemberDeviceSettings? = null,
    val settingsByCategory: Map<SettingCategory, List<SettingWithValue>> = emptyMap(),
    val history: List<SettingChange> = emptyList(),
    val expandedCategories: Set<SettingCategory> = SettingCategory.entries.toSet(),
    val notifyUserOnChange: Boolean = true,
    val editingSettingKey: String? = null,
    val editingSettingValue: Any? = null,
    val confirmLockSettingKey: String? = null,
    val confirmLockAction: Boolean? = null, // true = lock, false = unlock
) {
    val isEditing: Boolean get() = editingSettingKey != null
    val isConfirmingLock: Boolean get() = confirmLockSettingKey != null
    val deviceName: String get() = deviceSettings?.deviceName ?: ""
    val ownerName: String get() = deviceSettings?.ownerName ?: ""
    val isOnline: Boolean get() = deviceSettings?.isOnline ?: false
    val lockedCount: Int get() = deviceSettings?.lockedCount() ?: 0
}

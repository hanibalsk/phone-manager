package three.two.bit.phonemanager.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import three.two.bit.phonemanager.data.repository.SettingsSyncRepository
import three.two.bit.phonemanager.domain.model.DeviceSettings
import three.two.bit.phonemanager.domain.model.ManagedDeviceStatus
import three.two.bit.phonemanager.domain.model.SettingLock
import three.two.bit.phonemanager.domain.model.SettingsSyncStatus
import timber.log.Timber
import javax.inject.Inject

/**
 * Story E12.6: ViewModel for settings sync functionality.
 *
 * Manages settings synchronization, lock state display, and
 * push notification handling.
 *
 * AC E12.6.2: Setting sync on app start
 * AC E12.6.5: Push notification handling
 * AC E12.6.6: Settings status section
 */
@HiltViewModel
class SettingsSyncViewModel @Inject constructor(
    private val settingsSyncRepository: SettingsSyncRepository,
) : ViewModel() {

    /** Current sync status */
    val syncStatus: StateFlow<SettingsSyncStatus> = settingsSyncRepository.syncStatus

    /** Server settings with lock information */
    val serverSettings: StateFlow<DeviceSettings?> = settingsSyncRepository.serverSettings

    /** Managed device status for display */
    val managedStatus: StateFlow<ManagedDeviceStatus> = settingsSyncRepository.managedStatus
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ManagedDeviceStatus(isManaged = false),
        )

    /** Map of setting keys to their lock states */
    private val _lockStates = MutableStateFlow<Map<String, SettingLock>>(emptyMap())
    val lockStates: StateFlow<Map<String, SettingLock>> = _lockStates.asStateFlow()

    /** UI state for settings sync screen */
    private val _uiState = MutableStateFlow(SettingsSyncUiState())
    val uiState: StateFlow<SettingsSyncUiState> = _uiState.asStateFlow()

    init {
        // Load settings on init
        syncSettings()
    }

    /**
     * Sync settings from server.
     * AC E12.6.2: Setting sync on app start
     */
    fun syncSettings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true) }

            settingsSyncRepository.syncAllSettings().fold(
                onSuccess = {
                    val settings = settingsSyncRepository.serverSettings.value
                    settings?.let {
                        _lockStates.value = it.locks
                    }
                    _uiState.update { state ->
                        state.copy(
                            isSyncing = false,
                            lastSyncError = null,
                        )
                    }
                    Timber.i("Settings synced successfully")
                },
                onFailure = { error ->
                    _uiState.update { state ->
                        state.copy(
                            isSyncing = false,
                            lastSyncError = error.message ?: "Sync failed",
                        )
                    }
                    Timber.e(error, "Settings sync failed")
                },
            )
        }
    }

    /**
     * Check if a specific setting is locked.
     * AC E12.6.1: Lock indicator display
     */
    fun isSettingLocked(key: String): Boolean {
        return _lockStates.value[key]?.isLocked == true
    }

    /**
     * Get lock information for a specific setting.
     * AC E12.6.1: Lock indicator display
     */
    fun getSettingLock(key: String): SettingLock? {
        return _lockStates.value[key]
    }

    /**
     * Get who locked a specific setting.
     * AC E12.6.1: Show "Managed by [admin name]"
     */
    fun getLockedBy(key: String): String? {
        return _lockStates.value[key]?.lockedBy
    }

    /**
     * Handle push notification with settings update.
     * AC E12.6.5: Push notification for settings changes
     *
     * @param updatedSettings Map of setting keys to new values
     * @param updatedBy Who made the changes (admin email/name)
     */
    fun handlePushNotification(
        updatedSettings: Map<String, Any>,
        updatedBy: String,
    ) {
        viewModelScope.launch {
            Timber.i("Handling settings push from $updatedBy")
            settingsSyncRepository.handleSettingsUpdatePush(updatedSettings, updatedBy)

            // Update UI state with notification info
            _uiState.update { state ->
                state.copy(
                    lastPushUpdate = PushUpdateInfo(
                        updatedBy = updatedBy,
                        settingsChanged = updatedSettings.keys.toList(),
                    ),
                )
            }
        }
    }

    /**
     * Dismiss the push update notification.
     */
    fun dismissPushNotification() {
        _uiState.update { it.copy(lastPushUpdate = null) }
    }

    /**
     * Update a setting value (checks lock before allowing).
     * AC E12.6.3: Lock enforcement
     * AC E12.6.4: Unlocked setting interaction
     *
     * @param key Setting key
     * @param value New value
     * @return true if update was allowed, false if blocked by lock
     */
    suspend fun updateSetting(key: String, value: Any): Boolean {
        if (isSettingLocked(key)) {
            val lockedBy = getLockedBy(key)
            _uiState.update { state ->
                state.copy(
                    showLockedDialog = true,
                    lockedSettingKey = key,
                    lockedBy = lockedBy,
                )
            }
            return false
        }

        val result = settingsSyncRepository.updateServerSetting(key, value)
        return result.fold(
            onSuccess = { updateResult ->
                if (updateResult.wasLocked) {
                    _uiState.update { state ->
                        state.copy(
                            showLockedDialog = true,
                            lockedSettingKey = key,
                            lockedBy = updateResult.error,
                        )
                    }
                    false
                } else if (!updateResult.success) {
                    _uiState.update { state ->
                        state.copy(lastSyncError = updateResult.error)
                    }
                    false
                } else {
                    true
                }
            },
            onFailure = {
                _uiState.update { state ->
                    state.copy(lastSyncError = it.message)
                }
                false
            },
        )
    }

    /**
     * Dismiss the locked setting dialog.
     */
    fun dismissLockedDialog() {
        _uiState.update { state ->
            state.copy(
                showLockedDialog = false,
                lockedSettingKey = null,
                lockedBy = null,
            )
        }
    }

    /**
     * Clear any error state.
     */
    fun clearError() {
        _uiState.update { it.copy(lastSyncError = null) }
    }
}

/**
 * UI state for settings sync.
 */
data class SettingsSyncUiState(
    val isSyncing: Boolean = false,
    val lastSyncError: String? = null,
    val showLockedDialog: Boolean = false,
    val lockedSettingKey: String? = null,
    val lockedBy: String? = null,
    val lastPushUpdate: PushUpdateInfo? = null,
)

/**
 * Information about a push notification update.
 */
data class PushUpdateInfo(
    val updatedBy: String,
    val settingsChanged: List<String>,
)

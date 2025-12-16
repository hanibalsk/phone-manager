package three.two.bit.phonemanager.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.time.Clock
import kotlin.time.Instant
import three.two.bit.phonemanager.domain.model.BulkSettingsResult
import three.two.bit.phonemanager.domain.model.DeviceSettingsResult
import three.two.bit.phonemanager.domain.model.MemberDeviceSettings
import three.two.bit.phonemanager.domain.model.SettingChange
import three.two.bit.phonemanager.domain.model.SettingChangeType
import three.two.bit.phonemanager.domain.model.SettingLock
import three.two.bit.phonemanager.domain.model.SettingsTemplate
import three.two.bit.phonemanager.network.DeviceApiService
import three.two.bit.phonemanager.network.models.SettingLockResponse
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Story E12.7: Admin Settings Management Repository
 *
 * Repository for admin-side device settings management.
 * Provides methods for viewing, modifying, and locking remote device settings.
 *
 * AC E12.7.1: Device Settings List Screen
 * AC E12.7.3: View Remote Settings
 * AC E12.7.4: Modify Remote Settings
 * AC E12.7.5: Lock/Unlock Settings
 * AC E12.7.6: Bulk Settings Application
 * AC E12.7.7: Settings Templates
 * AC E12.7.8: Audit Trail
 */
interface AdminSettingsRepository {
    /** Currently loaded device settings */
    val currentDeviceSettings: StateFlow<MemberDeviceSettings?>

    /** Currently loaded member devices for the group */
    val memberDevices: StateFlow<List<MemberDeviceSettings>>

    /** Currently loaded templates */
    val templates: StateFlow<List<SettingsTemplate>>

    /** Loading state */
    val isLoading: StateFlow<Boolean>

    /** Error state */
    val error: StateFlow<String?>

    /**
     * Get member devices for a group.
     * AC E12.7.1: Device Settings List Screen
     */
    suspend fun getMemberDevices(groupId: String): Result<List<MemberDeviceSettings>>

    /**
     * Get settings for a specific device.
     * AC E12.7.3: View Remote Settings
     */
    suspend fun getDeviceSettings(deviceId: String): Result<MemberDeviceSettings>

    /**
     * Update settings for a device.
     * AC E12.7.4: Modify Remote Settings
     */
    suspend fun updateDeviceSettings(
        deviceId: String,
        changes: Map<String, Any>,
        notifyUser: Boolean = true,
    ): Result<Map<String, Any>>

    /**
     * Lock settings for a device.
     * AC E12.7.5: Lock/Unlock Settings
     */
    suspend fun lockSettings(
        deviceId: String,
        settingKeys: List<String>,
    ): Result<Int>

    /**
     * Unlock settings for a device.
     * AC E12.7.5: Lock/Unlock Settings
     */
    suspend fun unlockSettings(
        deviceId: String,
        settingKeys: List<String>,
    ): Result<Int>

    /**
     * Get settings change history for a device.
     * AC E12.7.8: Audit Trail
     */
    suspend fun getSettingsHistory(
        deviceId: String,
        limit: Int = 50,
        offset: Int = 0,
    ): Result<List<SettingChange>>

    /**
     * Apply settings to multiple devices.
     * AC E12.7.6: Bulk Settings Application
     */
    suspend fun bulkUpdateDevices(
        deviceIds: List<String>,
        settings: Map<String, Any>,
        locks: List<String>? = null,
        notifyUsers: Boolean = true,
    ): Result<BulkSettingsResult>

    /**
     * Get all settings templates.
     * AC E12.7.7: Settings Templates
     */
    suspend fun getTemplates(): Result<List<SettingsTemplate>>

    /**
     * Save a settings template.
     * AC E12.7.7: Settings Templates
     */
    suspend fun saveTemplate(template: SettingsTemplate): Result<SettingsTemplate>

    /**
     * Delete a settings template.
     * AC E12.7.7: Settings Templates
     */
    suspend fun deleteTemplate(templateId: String): Result<Unit>

    /**
     * Apply a template to devices.
     * AC E12.7.7: Settings Templates
     */
    suspend fun applyTemplate(
        templateId: String,
        deviceIds: List<String>,
        notifyUsers: Boolean = true,
    ): Result<BulkSettingsResult>

    /**
     * Clear current device settings state.
     */
    fun clearCurrentDevice()

    /**
     * Clear error state.
     */
    fun clearError()
}

@Singleton
class AdminSettingsRepositoryImpl @Inject constructor(
    private val deviceApiService: DeviceApiService,
    private val authRepository: AuthRepository,
) : AdminSettingsRepository {

    private val _currentDeviceSettings = MutableStateFlow<MemberDeviceSettings?>(null)
    override val currentDeviceSettings: StateFlow<MemberDeviceSettings?> =
        _currentDeviceSettings.asStateFlow()

    private val _memberDevices = MutableStateFlow<List<MemberDeviceSettings>>(emptyList())
    override val memberDevices: StateFlow<List<MemberDeviceSettings>> =
        _memberDevices.asStateFlow()

    private val _templates = MutableStateFlow<List<SettingsTemplate>>(emptyList())
    override val templates: StateFlow<List<SettingsTemplate>> = _templates.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    override val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    override val error: StateFlow<String?> = _error.asStateFlow()

    override suspend fun getMemberDevices(groupId: String): Result<List<MemberDeviceSettings>> {
        val accessToken = authRepository.getAccessToken()
            ?: return Result.failure(IllegalStateException("Not authenticated"))

        _isLoading.value = true
        _error.value = null

        return try {
            val result = deviceApiService.getGroupMemberDevices(groupId, accessToken)

            result.fold(
                onSuccess = { response ->
                    val devices = response.devices.map { device ->
                        MemberDeviceSettings(
                            deviceId = device.deviceId,
                            deviceName = device.displayName,
                            ownerUserId = "", // Not provided by this endpoint
                            ownerName = "", // Not provided by this endpoint
                            ownerEmail = "", // Not provided by this endpoint
                            isOnline = device.lastSeenAt != null, // Infer from last seen
                            lastSeen = device.lastSeenAt?.let { parseInstant(it) },
                            settings = emptyMap(), // Basic info only, load full settings on demand
                            locks = emptyMap(),
                            lastSyncedAt = null,
                            lastModifiedBy = null,
                        )
                    }
                    _memberDevices.value = devices
                    Timber.i("Fetched ${devices.size} member devices for group $groupId")
                    Result.success(devices)
                },
                onFailure = { error ->
                    _error.value = error.message
                    Timber.e(error, "Failed to fetch member devices")
                    Result.failure(error)
                },
            )
        } finally {
            _isLoading.value = false
        }
    }

    override suspend fun getDeviceSettings(deviceId: String): Result<MemberDeviceSettings> {
        val accessToken = authRepository.getAccessToken()
            ?: return Result.failure(IllegalStateException("Not authenticated"))

        _isLoading.value = true
        _error.value = null

        return try {
            val result = deviceApiService.getAdminDeviceSettings(deviceId, accessToken)

            result.fold(
                onSuccess = { response ->
                    // Extract values and locks from the nested setting objects
                    val settingsValues = response.getSettingsValues()
                    val settingsLocks = response.getSettingsLocks()

                    Timber.d("Backend settings keys: ${settingsValues.keys}")
                    Timber.d("Backend settings values: $settingsValues")

                    // Try to get device info from cached member devices list
                    // (the settings endpoint doesn't return device name or online status)
                    val cachedDevice = _memberDevices.value.find { it.deviceId == deviceId }

                    val settings = MemberDeviceSettings(
                        deviceId = response.deviceId,
                        // Use cached device name if backend doesn't provide one
                        deviceName = response.deviceName.ifEmpty { cachedDevice?.deviceName ?: "" },
                        ownerUserId = response.ownerUserId,
                        ownerName = response.ownerName,
                        ownerEmail = response.ownerEmail,
                        // Use cached online status if backend doesn't provide it
                        isOnline = if (response.isOnline) true else cachedDevice?.isOnline ?: false,
                        lastSeen = response.lastSeen?.let { parseInstant(it) }
                            ?: cachedDevice?.lastSeen,
                        settings = settingsValues,
                        locks = (response.locks + settingsLocks).mapValues { (key, lock) -> lock.toDomain(key) },
                        lastSyncedAt = response.lastSyncedAt?.let { parseInstant(it) },
                        lastModifiedBy = response.lastModifiedBy,
                    )
                    _currentDeviceSettings.value = settings
                    Timber.i("Fetched settings for device $deviceId (deviceName='${settings.deviceName}')")
                    Result.success(settings)
                },
                onFailure = { error ->
                    _error.value = error.message
                    Timber.e(error, "Failed to fetch device settings")
                    Result.failure(error)
                },
            )
        } finally {
            _isLoading.value = false
        }
    }

    override suspend fun updateDeviceSettings(
        deviceId: String,
        changes: Map<String, Any>,
        notifyUser: Boolean,
    ): Result<Map<String, Any>> {
        val accessToken = authRepository.getAccessToken()
            ?: return Result.failure(IllegalStateException("Not authenticated"))

        _isLoading.value = true
        _error.value = null

        return try {
            val result = deviceApiService.updateAdminDeviceSettings(
                deviceId = deviceId,
                changes = changes,
                notifyUser = notifyUser,
                accessToken = accessToken,
            )

            result.fold(
                onSuccess = { response ->
                    if (response.success) {
                        val applied = response.appliedSettings ?: changes
                        // Update cached settings
                        _currentDeviceSettings.value?.let { current ->
                            _currentDeviceSettings.value = current.copy(
                                settings = current.settings + applied,
                            )
                        }
                        Timber.i("Updated ${applied.size} settings for device $deviceId")
                        Result.success(applied)
                    } else {
                        _error.value = response.error
                        Result.failure(Exception(response.error ?: "Update failed"))
                    }
                },
                onFailure = { error ->
                    _error.value = error.message
                    Timber.e(error, "Failed to update device settings")
                    Result.failure(error)
                },
            )
        } finally {
            _isLoading.value = false
        }
    }

    override suspend fun lockSettings(
        deviceId: String,
        settingKeys: List<String>,
    ): Result<Int> {
        val accessToken = authRepository.getAccessToken()
            ?: return Result.failure(IllegalStateException("Not authenticated"))

        _isLoading.value = true
        _error.value = null

        return try {
            val result = deviceApiService.lockDeviceSettings(
                deviceId = deviceId,
                settingKeys = settingKeys,
                lock = true,
                accessToken = accessToken,
            )

            result.fold(
                onSuccess = { response ->
                    if (response.success) {
                        // Update cached locks
                        _currentDeviceSettings.value?.let { current ->
                            val updatedLocks = current.locks.toMutableMap()
                            settingKeys.forEach { key ->
                                updatedLocks[key] = SettingLock(
                                    settingKey = key,
                                    isLocked = true,
                                    lockedBy = "Current User", // Local cache only - actual value from server
                                    lockedAt = Clock.System.now(),
                                )
                            }
                            _currentDeviceSettings.value = current.copy(locks = updatedLocks)
                        }
                        Timber.i("Locked ${response.lockedCount} settings for device $deviceId")
                        Result.success(response.lockedCount)
                    } else {
                        _error.value = response.error
                        Result.failure(Exception(response.error ?: "Lock failed"))
                    }
                },
                onFailure = { error ->
                    _error.value = error.message
                    Timber.e(error, "Failed to lock settings")
                    Result.failure(error)
                },
            )
        } finally {
            _isLoading.value = false
        }
    }

    override suspend fun unlockSettings(
        deviceId: String,
        settingKeys: List<String>,
    ): Result<Int> {
        val accessToken = authRepository.getAccessToken()
            ?: return Result.failure(IllegalStateException("Not authenticated"))

        _isLoading.value = true
        _error.value = null

        return try {
            val result = deviceApiService.lockDeviceSettings(
                deviceId = deviceId,
                settingKeys = settingKeys,
                lock = false,
                accessToken = accessToken,
            )

            result.fold(
                onSuccess = { response ->
                    if (response.success) {
                        // Update cached locks
                        _currentDeviceSettings.value?.let { current ->
                            val updatedLocks = current.locks.toMutableMap()
                            settingKeys.forEach { key ->
                                updatedLocks[key] = SettingLock(
                                    settingKey = key,
                                    isLocked = false,
                                )
                            }
                            _currentDeviceSettings.value = current.copy(locks = updatedLocks)
                        }
                        Timber.i("Unlocked ${response.unlockedCount} settings for device $deviceId")
                        Result.success(response.unlockedCount)
                    } else {
                        _error.value = response.error
                        Result.failure(Exception(response.error ?: "Unlock failed"))
                    }
                },
                onFailure = { error ->
                    _error.value = error.message
                    Timber.e(error, "Failed to unlock settings")
                    Result.failure(error)
                },
            )
        } finally {
            _isLoading.value = false
        }
    }

    override suspend fun getSettingsHistory(
        deviceId: String,
        limit: Int,
        offset: Int,
    ): Result<List<SettingChange>> {
        val accessToken = authRepository.getAccessToken()
            ?: return Result.failure(IllegalStateException("Not authenticated"))

        _isLoading.value = true
        _error.value = null

        return try {
            val result = deviceApiService.getSettingsHistory(
                deviceId = deviceId,
                limit = limit,
                offset = offset,
                accessToken = accessToken,
            )

            result.fold(
                onSuccess = { response ->
                    val history = response.changes.map { change ->
                        SettingChange(
                            id = change.id,
                            settingKey = change.settingKey,
                            oldValue = change.oldValue,
                            newValue = change.newValue,
                            changedBy = change.changedBy,
                            changedByName = change.changedByName,
                            changedAt = parseInstant(change.changedAt)
                                ?: Clock.System.now(),
                            changeType = parseChangeType(change.changeType),
                            deviceId = deviceId,
                        )
                    }
                    Timber.i("Fetched ${history.size} history entries for device $deviceId")
                    Result.success(history)
                },
                onFailure = { error ->
                    _error.value = error.message
                    Timber.e(error, "Failed to fetch settings history")
                    Result.failure(error)
                },
            )
        } finally {
            _isLoading.value = false
        }
    }

    override suspend fun bulkUpdateDevices(
        deviceIds: List<String>,
        settings: Map<String, Any>,
        locks: List<String>?,
        notifyUsers: Boolean,
    ): Result<BulkSettingsResult> {
        val accessToken = authRepository.getAccessToken()
            ?: return Result.failure(IllegalStateException("Not authenticated"))

        _isLoading.value = true
        _error.value = null

        return try {
            val result = deviceApiService.bulkUpdateSettings(
                deviceIds = deviceIds,
                settings = settings,
                locks = locks,
                notifyUsers = notifyUsers,
                accessToken = accessToken,
            )

            result.fold(
                onSuccess = { response ->
                    val bulkResult = BulkSettingsResult(
                        successful = response.successful.map { device ->
                            DeviceSettingsResult(
                                deviceId = device.deviceId,
                                deviceName = device.deviceName,
                                success = true,
                                appliedSettings = device.appliedSettings,
                            )
                        },
                        failed = response.failed.map { device ->
                            DeviceSettingsResult(
                                deviceId = device.deviceId,
                                deviceName = device.deviceName,
                                success = false,
                                error = device.error,
                            )
                        },
                    )
                    Timber.i(
                        "Bulk update: ${bulkResult.successCount} successful, " +
                            "${bulkResult.failureCount} failed",
                    )
                    Result.success(bulkResult)
                },
                onFailure = { error ->
                    _error.value = error.message
                    Timber.e(error, "Failed to bulk update devices")
                    Result.failure(error)
                },
            )
        } finally {
            _isLoading.value = false
        }
    }

    override suspend fun getTemplates(): Result<List<SettingsTemplate>> {
        val accessToken = authRepository.getAccessToken()
            ?: return Result.failure(IllegalStateException("Not authenticated"))

        _isLoading.value = true
        _error.value = null

        return try {
            val result = deviceApiService.getSettingsTemplates(accessToken)

            result.fold(
                onSuccess = { response ->
                    val templateList = response.templates.map { template ->
                        SettingsTemplate(
                            id = template.id,
                            name = template.name,
                            description = template.description,
                            settings = template.settings,
                            lockedSettings = template.lockedSettings.toSet(),
                            createdBy = template.createdBy,
                            createdByName = template.createdByName,
                            createdAt = parseInstant(template.createdAt)
                                ?: Clock.System.now(),
                            updatedAt = template.updatedAt?.let { parseInstant(it) },
                            isShared = template.isShared,
                        )
                    }
                    _templates.value = templateList
                    Timber.i("Fetched ${templateList.size} settings templates")
                    Result.success(templateList)
                },
                onFailure = { error ->
                    _error.value = error.message
                    Timber.e(error, "Failed to fetch templates")
                    Result.failure(error)
                },
            )
        } finally {
            _isLoading.value = false
        }
    }

    override suspend fun saveTemplate(template: SettingsTemplate): Result<SettingsTemplate> {
        val accessToken = authRepository.getAccessToken()
            ?: return Result.failure(IllegalStateException("Not authenticated"))

        _isLoading.value = true
        _error.value = null

        return try {
            val result = deviceApiService.saveSettingsTemplate(
                name = template.name,
                description = template.description,
                settings = template.settings,
                lockedSettings = template.lockedSettings.toList(),
                isShared = template.isShared,
                accessToken = accessToken,
            )

            result.fold(
                onSuccess = { response ->
                    if (response.success && response.template != null) {
                        val savedTemplate = SettingsTemplate(
                            id = response.template.id,
                            name = response.template.name,
                            description = response.template.description,
                            settings = response.template.settings,
                            lockedSettings = response.template.lockedSettings.toSet(),
                            createdBy = response.template.createdBy,
                            createdByName = response.template.createdByName,
                            createdAt = parseInstant(response.template.createdAt)
                                ?: Clock.System.now(),
                            updatedAt = response.template.updatedAt?.let { parseInstant(it) },
                            isShared = response.template.isShared,
                        )
                        // Update templates list
                        val currentTemplates = _templates.value.toMutableList()
                        val existingIndex = currentTemplates.indexOfFirst { it.id == savedTemplate.id }
                        if (existingIndex >= 0) {
                            currentTemplates[existingIndex] = savedTemplate
                        } else {
                            currentTemplates.add(savedTemplate)
                        }
                        _templates.value = currentTemplates
                        Timber.i("Saved template: ${savedTemplate.name}")
                        Result.success(savedTemplate)
                    } else {
                        _error.value = response.error
                        Result.failure(Exception(response.error ?: "Save failed"))
                    }
                },
                onFailure = { error ->
                    _error.value = error.message
                    Timber.e(error, "Failed to save template")
                    Result.failure(error)
                },
            )
        } finally {
            _isLoading.value = false
        }
    }

    override suspend fun deleteTemplate(templateId: String): Result<Unit> {
        val accessToken = authRepository.getAccessToken()
            ?: return Result.failure(IllegalStateException("Not authenticated"))

        _isLoading.value = true
        _error.value = null

        return try {
            val result = deviceApiService.deleteSettingsTemplate(templateId, accessToken)

            result.fold(
                onSuccess = {
                    // Remove from templates list
                    _templates.value = _templates.value.filter { it.id != templateId }
                    Timber.i("Deleted template: $templateId")
                    Result.success(Unit)
                },
                onFailure = { error ->
                    _error.value = error.message
                    Timber.e(error, "Failed to delete template")
                    Result.failure(error)
                },
            )
        } finally {
            _isLoading.value = false
        }
    }

    override suspend fun applyTemplate(
        templateId: String,
        deviceIds: List<String>,
        notifyUsers: Boolean,
    ): Result<BulkSettingsResult> {
        // Find the template
        val template = _templates.value.find { it.id == templateId }
            ?: return Result.failure(IllegalArgumentException("Template not found: $templateId"))

        // Apply as bulk update
        return bulkUpdateDevices(
            deviceIds = deviceIds,
            settings = template.settings,
            locks = template.lockedSettings.toList(),
            notifyUsers = notifyUsers,
        )
    }

    override fun clearCurrentDevice() {
        _currentDeviceSettings.value = null
    }

    override fun clearError() {
        _error.value = null
    }

    private fun parseInstant(isoString: String): Instant? {
        return try {
            Instant.parse(isoString)
        } catch (e: Exception) {
            Timber.w(e, "Failed to parse instant: $isoString")
            null
        }
    }

    private fun parseChangeType(type: String): SettingChangeType {
        return when (type.uppercase()) {
            "VALUE_CHANGED", "CHANGED" -> SettingChangeType.VALUE_CHANGED
            "LOCKED" -> SettingChangeType.LOCKED
            "UNLOCKED" -> SettingChangeType.UNLOCKED
            "RESET" -> SettingChangeType.RESET
            else -> SettingChangeType.VALUE_CHANGED
        }
    }

    private fun SettingLockResponse.toDomain(key: String): SettingLock {
        return SettingLock(
            settingKey = key,
            isLocked = isLocked,
            lockedBy = lockedBy,
            lockedAt = lockedAt?.let { parseInstant(it) },
        )
    }
}

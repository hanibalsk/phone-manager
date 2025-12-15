package three.two.bit.phonemanager.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlin.time.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import three.two.bit.phonemanager.domain.model.DeviceSettings
import three.two.bit.phonemanager.domain.model.ManagedDeviceStatus
import three.two.bit.phonemanager.domain.model.SettingLock
import three.two.bit.phonemanager.domain.model.SettingUpdateResult
import three.two.bit.phonemanager.domain.model.SettingsSyncStatus
import three.two.bit.phonemanager.network.DeviceApiService
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Story E12.6: Repository for syncing device settings with server.
 *
 * Manages settings synchronization, lock state caching, and
 * offline handling.
 *
 * AC E12.6.2: Setting sync on app start
 * AC E12.6.3: Lock enforcement
 * AC E12.6.4: Unlocked setting interaction
 * AC E12.6.8: Offline handling
 */
interface SettingsSyncRepository {
    /** Current sync status */
    val syncStatus: StateFlow<SettingsSyncStatus>

    /** Current device settings from server */
    val serverSettings: StateFlow<DeviceSettings?>

    /** Managed device status */
    val managedStatus: Flow<ManagedDeviceStatus>

    /**
     * Fetch settings from server.
     * AC E12.6.2: Setting sync on app start
     */
    suspend fun fetchServerSettings(): Result<DeviceSettings>

    /**
     * Update a setting on the server.
     * AC E12.6.4: Unlocked setting interaction
     * @return Result with update status, including if blocked by lock
     */
    suspend fun updateServerSetting(key: String, value: Any): Result<SettingUpdateResult>

    /**
     * Get lock status for a specific setting.
     * AC E12.6.1: Lock indicator display
     */
    suspend fun getSettingLockStatus(key: String): SettingLock?

    /**
     * Check if a setting is locked.
     * AC E12.6.3: Lock enforcement
     */
    suspend fun isSettingLocked(key: String): Boolean

    /**
     * Sync all settings from server.
     * Called on app start and periodically.
     */
    suspend fun syncAllSettings(): Result<Unit>

    /**
     * Get who locked a specific setting.
     * AC E12.6.1: Show "Managed by [admin name]"
     */
    suspend fun getLockedBy(key: String): String?

    /**
     * Handle push notification with settings update.
     * AC E12.6.5: Push notification for settings changes
     */
    suspend fun handleSettingsUpdatePush(updatedSettings: Map<String, Any>, updatedBy: String)

    /**
     * Handle push notification for setting lock/unlock.
     * AC E12.6.5: Push notification for lock state changes
     */
    suspend fun handleSettingLockPush(settingKey: String, isLocked: Boolean, adminName: String?)

    /**
     * Register FCM token with server.
     * AC E12.6.5: Enable push notifications
     */
    suspend fun registerFcmToken(token: String)

    /**
     * Clear cached settings (e.g., on logout).
     */
    suspend fun clearCache()
}

private val Context.settingsSyncDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "settings_sync",
)

@Singleton
class SettingsSyncRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val deviceApiService: DeviceApiService,
    private val deviceRepository: DeviceRepository,
    private val authRepository: AuthRepository,
) : SettingsSyncRepository {

    private val json = Json { ignoreUnknownKeys = true }

    private val _syncStatus = MutableStateFlow(SettingsSyncStatus.SYNCED)
    override val syncStatus: StateFlow<SettingsSyncStatus> = _syncStatus.asStateFlow()

    private val _serverSettings = MutableStateFlow<DeviceSettings?>(null)
    override val serverSettings: StateFlow<DeviceSettings?> = _serverSettings.asStateFlow()

    override val managedStatus: Flow<ManagedDeviceStatus> = context.settingsSyncDataStore.data
        .map { preferences ->
            val isManaged = preferences[PreferenceKeys.IS_MANAGED] ?: false
            val groupName = preferences[PreferenceKeys.GROUP_NAME]
            val groupId = preferences[PreferenceKeys.GROUP_ID]
            val lockedCount = preferences[PreferenceKeys.LOCKED_COUNT]?.toIntOrNull() ?: 0
            val lastSynced = preferences[PreferenceKeys.LAST_SYNCED]?.let {
                try {
                    Instant.parse(it)
                } catch (e: Exception) {
                    null
                }
            }

            ManagedDeviceStatus(
                isManaged = isManaged,
                groupName = groupName,
                groupId = groupId,
                lockedSettingsCount = lockedCount,
                lastSyncedAt = lastSynced,
            )
        }

    private object PreferenceKeys {
        val IS_MANAGED = booleanPreferencesKey("is_managed")
        val GROUP_NAME = stringPreferencesKey("group_name")
        val GROUP_ID = stringPreferencesKey("group_id")
        val LOCKED_COUNT = stringPreferencesKey("locked_count")
        val LAST_SYNCED = stringPreferencesKey("last_synced")
        val CACHED_LOCKS = stringPreferencesKey("cached_locks")
        val CACHED_SETTINGS = stringPreferencesKey("cached_settings")
    }

    override suspend fun fetchServerSettings(): Result<DeviceSettings> {
        val deviceId = deviceRepository.getDeviceId()
        val accessToken = authRepository.getAccessToken()

        if (accessToken == null) {
            Timber.d("No access token, cannot fetch server settings")
            _syncStatus.value = SettingsSyncStatus.NOT_AUTHENTICATED
            return Result.failure(IllegalStateException("Not authenticated"))
        }

        _syncStatus.value = SettingsSyncStatus.SYNCING

        return try {
            val result = deviceApiService.getDeviceSettings(deviceId, accessToken)

            result.fold(
                onSuccess = { settings ->
                    _serverSettings.value = settings
                    cacheSettings(settings)
                    _syncStatus.value = SettingsSyncStatus.SYNCED
                    Timber.i("Fetched server settings: ${settings.lockedCount()} locked")
                    Result.success(settings)
                },
                onFailure = { error ->
                    Timber.e(error, "Failed to fetch server settings")
                    _syncStatus.value = SettingsSyncStatus.ERROR
                    // Try to load from cache
                    loadCachedSettings()
                    Result.failure(error)
                },
            )
        } catch (e: Exception) {
            Timber.e(e, "Exception fetching server settings")
            _syncStatus.value = SettingsSyncStatus.ERROR
            loadCachedSettings()
            Result.failure(e)
        }
    }

    override suspend fun updateServerSetting(key: String, value: Any): Result<SettingUpdateResult> {
        // Check if locked first
        if (isSettingLocked(key)) {
            val lockedBy = getLockedBy(key)
            Timber.w("Cannot update $key: locked by $lockedBy")
            return Result.success(
                SettingUpdateResult(
                    success = false,
                    error = "Setting is locked by $lockedBy",
                    wasLocked = true,
                ),
            )
        }

        val deviceId = deviceRepository.getDeviceId()
        val accessToken = authRepository.getAccessToken()

        if (accessToken == null) {
            _syncStatus.value = SettingsSyncStatus.PENDING
            return Result.success(
                SettingUpdateResult(
                    success = false,
                    error = "Not authenticated, change queued",
                ),
            )
        }

        return try {
            val result = deviceApiService.updateDeviceSetting(
                deviceId = deviceId,
                key = key,
                value = value.toString(),
                accessToken = accessToken,
            )

            result.fold(
                onSuccess = { response ->
                    if (response.success) {
                        Timber.i("Successfully updated setting $key")
                        Result.success(SettingUpdateResult(success = true))
                    } else {
                        Timber.w("Failed to update setting $key: ${response.error}")
                        Result.success(
                            SettingUpdateResult(
                                success = false,
                                error = response.error,
                                wasLocked = response.isLocked,
                            ),
                        )
                    }
                },
                onFailure = { error ->
                    Timber.e(error, "Failed to update setting $key")
                    Result.success(
                        SettingUpdateResult(
                            success = false,
                            error = error.message,
                        ),
                    )
                },
            )
        } catch (e: Exception) {
            Timber.e(e, "Exception updating setting $key")
            Result.success(
                SettingUpdateResult(
                    success = false,
                    error = e.message,
                ),
            )
        }
    }

    override suspend fun getSettingLockStatus(key: String): SettingLock? {
        return _serverSettings.value?.getLock(key) ?: loadCachedLock(key)
    }

    override suspend fun isSettingLocked(key: String): Boolean {
        return _serverSettings.value?.isLocked(key)
            ?: loadCachedLock(key)?.isLocked
            ?: false
    }

    override suspend fun syncAllSettings(): Result<Unit> {
        return fetchServerSettings().map { }
    }

    override suspend fun getLockedBy(key: String): String? {
        return _serverSettings.value?.getLockedBy(key)
            ?: loadCachedLock(key)?.lockedBy
    }

    override suspend fun handleSettingsUpdatePush(
        updatedSettings: Map<String, Any>,
        updatedBy: String,
    ) {
        Timber.i("Received settings update push from $updatedBy: $updatedSettings")

        // Refresh settings from server to get accurate lock states
        fetchServerSettings()
    }

    override suspend fun handleSettingLockPush(
        settingKey: String,
        isLocked: Boolean,
        adminName: String?,
    ) {
        Timber.i("Received setting lock push: $settingKey -> locked=$isLocked by $adminName")

        // Update cached lock state immediately
        val currentSettings = _serverSettings.value
        if (currentSettings != null) {
            val updatedLocks = currentSettings.locks.toMutableMap()
            updatedLocks[settingKey] = SettingLock(
                settingKey = settingKey,
                isLocked = isLocked,
                lockedBy = adminName,
                lockedAt = if (isLocked) Clock.System.now() else null,
            )
            _serverSettings.value = currentSettings.copy(locks = updatedLocks)
            cacheSettings(_serverSettings.value!!)
        }

        // Also refresh from server to ensure consistency
        fetchServerSettings()
    }

    override suspend fun registerFcmToken(token: String) {
        Timber.d("Registering FCM token with server")

        val deviceId = deviceRepository.getDeviceId()
        val groupId = deviceRepository.getGroupId()

        // Only register FCM token if device has been properly set up with a group
        if (groupId == null) {
            Timber.w("Cannot register FCM token: device has no group ID yet")
            return
        }

        // Use device registration endpoint which already supports fcm_token
        // This will upsert the device and update the FCM token
        try {
            val request = three.two.bit.phonemanager.network.models.DeviceRegistrationRequest(
                deviceId = deviceId,
                displayName = android.os.Build.MODEL,
                groupId = groupId,
                platform = "android",
                fcmToken = token,
            )

            val result = deviceApiService.registerDevice(request)
            result.fold(
                onSuccess = {
                    Timber.i("FCM token registered successfully")
                },
                onFailure = { error ->
                    Timber.e(error, "Failed to register FCM token")
                },
            )
        } catch (e: Exception) {
            Timber.e(e, "Exception registering FCM token")
        }
    }

    override suspend fun clearCache() {
        context.settingsSyncDataStore.edit { preferences ->
            preferences.clear()
        }
        _serverSettings.value = null
        _syncStatus.value = SettingsSyncStatus.SYNCED
        Timber.d("Settings sync cache cleared")
    }

    private suspend fun cacheSettings(settings: DeviceSettings) {
        val locksJson = json.encodeToString(
            settings.locks.map { (key, lock) ->
                CachedLock(
                    settingKey = key,
                    isLocked = lock.isLocked,
                    lockedBy = lock.lockedBy,
                    lockedAt = lock.lockedAt?.toString(),
                )
            },
        )

        context.settingsSyncDataStore.edit { preferences ->
            preferences[PreferenceKeys.CACHED_LOCKS] = locksJson
            preferences[PreferenceKeys.LOCKED_COUNT] = settings.lockedCount().toString()
            preferences[PreferenceKeys.LAST_SYNCED] = Clock.System.now().toString()
            preferences[PreferenceKeys.IS_MANAGED] = settings.lockedCount() > 0
        }
    }

    private suspend fun loadCachedSettings() {
        val preferences = context.settingsSyncDataStore.data.first()
        val locksJson = preferences[PreferenceKeys.CACHED_LOCKS]

        if (locksJson != null) {
            try {
                val cachedLocks = json.decodeFromString<List<CachedLock>>(locksJson)
                val lockMap = cachedLocks.associate { cached ->
                    cached.settingKey to SettingLock(
                        settingKey = cached.settingKey,
                        isLocked = cached.isLocked,
                        lockedBy = cached.lockedBy,
                        lockedAt = cached.lockedAt?.let { Instant.parse(it) },
                    )
                }

                // Create settings with cached locks
                _serverSettings.value = DeviceSettings(locks = lockMap)
                _syncStatus.value = SettingsSyncStatus.OFFLINE
                Timber.d("Loaded cached locks: ${lockMap.size} entries")
            } catch (e: Exception) {
                Timber.e(e, "Failed to load cached locks")
            }
        }
    }

    private suspend fun loadCachedLock(key: String): SettingLock? {
        val preferences = context.settingsSyncDataStore.data.first()
        val locksJson = preferences[PreferenceKeys.CACHED_LOCKS] ?: return null

        return try {
            val cachedLocks = json.decodeFromString<List<CachedLock>>(locksJson)
            cachedLocks.find { it.settingKey == key }?.let { cached ->
                SettingLock(
                    settingKey = cached.settingKey,
                    isLocked = cached.isLocked,
                    lockedBy = cached.lockedBy,
                    lockedAt = cached.lockedAt?.let { Instant.parse(it) },
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to load cached lock for $key")
            null
        }
    }
}

@kotlinx.serialization.Serializable
private data class CachedLock(
    val settingKey: String,
    val isLocked: Boolean,
    val lockedBy: String? = null,
    val lockedAt: String? = null,
)

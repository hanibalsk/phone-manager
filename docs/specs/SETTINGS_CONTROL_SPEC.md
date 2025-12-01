# Settings Control Specification

## Phone Manager Android - Remote Settings Management & Locks

**Version:** 1.0.0
**Status:** Design Specification
**Last Updated:** 2025-12-01

---

## 1. Overview

This document specifies the Android implementation for device settings synchronization, setting locks enforced by administrators, unlock request workflows, and the user interface for viewing and managing locked settings.

---

## 2. Domain Models

### 2.1 Device Setting Model

```kotlin
// domain/model/DeviceSetting.kt
data class DeviceSetting(
    val key: String,
    val value: SettingValue,
    val isLocked: Boolean,
    val lockInfo: SettingLockInfo?,
    val definition: SettingDefinition?,
    val updatedAt: Instant,
    val updatedBy: String?
)

sealed class SettingValue {
    data class BooleanValue(val value: Boolean) : SettingValue()
    data class IntValue(val value: Int) : SettingValue()
    data class LongValue(val value: Long) : SettingValue()
    data class StringValue(val value: String) : SettingValue()
    data class StringListValue(val value: List<String>) : SettingValue()

    fun asBoolean(): Boolean = (this as BooleanValue).value
    fun asInt(): Int = (this as IntValue).value
    fun asLong(): Long = (this as LongValue).value
    fun asString(): String = (this as StringValue).value
    fun asStringList(): List<String> = (this as StringListValue).value
}

data class SettingLockInfo(
    val lockedBy: String,
    val lockedByName: String?,
    val lockedAt: Instant,
    val reason: String?
)

data class SettingDefinition(
    val key: String,
    val displayName: String,
    val description: String?,
    val dataType: SettingDataType,
    val defaultValue: SettingValue,
    val isLockable: Boolean,
    val category: SettingCategory,
    val validationRules: SettingValidation?
)

enum class SettingDataType {
    BOOLEAN,
    INTEGER,
    LONG,
    STRING,
    STRING_LIST
}

enum class SettingCategory {
    TRACKING,
    PRIVACY,
    NOTIFICATIONS,
    BATTERY,
    SYNC,
    DISPLAY,
    SECURITY
}

data class SettingValidation(
    val minValue: Number? = null,
    val maxValue: Number? = null,
    val allowedValues: List<String>? = null,
    val pattern: String? = null
)
```

### 2.2 Settings State

```kotlin
// domain/model/DeviceSettingsState.kt
data class DeviceSettingsState(
    val deviceId: String,
    val settings: Map<String, DeviceSetting>,
    val policyId: String?,
    val managedByGroupId: String?,
    val lastSyncedAt: Instant
) {
    fun getSetting(key: String): DeviceSetting? = settings[key]

    fun isLocked(key: String): Boolean = settings[key]?.isLocked == true

    val lockedSettings: List<DeviceSetting>
        get() = settings.values.filter { it.isLocked }

    val unlockedSettings: List<DeviceSetting>
        get() = settings.values.filter { !it.isLocked }

    val lockedCount: Int get() = lockedSettings.size

    fun settingsByCategory(): Map<SettingCategory, List<DeviceSetting>> {
        return settings.values.groupBy { it.definition?.category ?: SettingCategory.TRACKING }
    }
}
```

### 2.3 Unlock Request Model

```kotlin
// domain/model/UnlockRequest.kt
data class UnlockRequest(
    val id: String,
    val deviceId: String,
    val settingKey: String,
    val settingDisplayName: String?,
    val status: UnlockRequestStatus,
    val reason: String,
    val requestedBy: String,
    val requestedByName: String?,
    val decisionNote: String?,
    val decidedBy: String?,
    val decidedByName: String?,
    val decidedAt: Instant?,
    val createdAt: Instant
)

enum class UnlockRequestStatus {
    PENDING,
    APPROVED,
    DENIED,
    CANCELLED,
    EXPIRED
}
```

---

## 3. Repository Layer

### 3.1 Settings Repository Interface

```kotlin
// domain/repository/DeviceSettingsRepository.kt
interface DeviceSettingsRepository {
    // Settings CRUD
    suspend fun getSettings(deviceId: String): Result<DeviceSettingsState>
    suspend fun getSetting(deviceId: String, key: String): Result<DeviceSetting>
    suspend fun updateSetting(deviceId: String, key: String, value: SettingValue): Result<DeviceSetting>
    suspend fun updateSettings(deviceId: String, settings: Map<String, SettingValue>): Result<SettingsUpdateResult>

    // Settings sync
    suspend fun syncSettings(deviceId: String): Result<DeviceSettingsState>

    // Lock management (admin)
    suspend fun getLocks(deviceId: String): Result<List<DeviceSetting>>
    suspend fun lockSetting(deviceId: String, key: String, value: SettingValue?, reason: String?): Result<DeviceSetting>
    suspend fun unlockSetting(deviceId: String, key: String): Result<DeviceSetting>
    suspend fun updateLocks(deviceId: String, locks: Map<String, Boolean>, reason: String?): Result<List<DeviceSetting>>

    // Unlock requests (user)
    suspend fun requestUnlock(deviceId: String, key: String, reason: String): Result<UnlockRequest>
    suspend fun getMyUnlockRequests(): Result<List<UnlockRequest>>
    suspend fun cancelUnlockRequest(requestId: String): Result<Unit>

    // Unlock requests (admin)
    suspend fun getGroupUnlockRequests(groupId: String): Result<List<UnlockRequest>>
    suspend fun respondToUnlockRequest(requestId: String, approved: Boolean, note: String?): Result<UnlockRequest>

    // Reactive streams
    fun observeSettings(deviceId: String): Flow<DeviceSettingsState>
    fun observeSetting(deviceId: String, key: String): Flow<DeviceSetting?>
    fun observeMyUnlockRequests(): Flow<List<UnlockRequest>>
    fun observeGroupUnlockRequests(groupId: String): Flow<List<UnlockRequest>>

    // Setting definitions
    suspend fun getSettingDefinitions(): Result<List<SettingDefinition>>
}

data class SettingsUpdateResult(
    val updated: List<String>,
    val locked: List<String>,
    val settings: Map<String, DeviceSetting>
)
```

### 3.2 Repository Implementation

```kotlin
// data/repository/DeviceSettingsRepositoryImpl.kt
class DeviceSettingsRepositoryImpl @Inject constructor(
    private val settingsApi: DeviceSettingsApi,
    private val settingsDao: DeviceSettingsDao,
    private val unlockRequestDao: UnlockRequestDao,
    private val settingDefinitionsCache: SettingDefinitionsCache
) : DeviceSettingsRepository {

    override suspend fun getSettings(deviceId: String): Result<DeviceSettingsState> = runCatching {
        val response = settingsApi.getSettings(deviceId, includeDefinitions = true)
        val state = response.toDomain()

        // Cache locally
        settingsDao.insertAll(state.settings.values.map { it.toEntity(deviceId) })

        // Cache definitions
        response.definitions?.let { defs ->
            settingDefinitionsCache.cache(defs.map { it.toDomain() })
        }

        state
    }

    override suspend fun updateSetting(
        deviceId: String,
        key: String,
        value: SettingValue
    ): Result<DeviceSetting> = runCatching {
        val request = UpdateSettingRequest(value = value.toDto())
        val response = settingsApi.updateSetting(deviceId, key, request)

        if (response.isLocked) {
            throw SettingLockedException(key, response.error ?: "Setting is locked")
        }

        val setting = response.toDomain()
        settingsDao.insert(setting.toEntity(deviceId))

        setting
    }

    override suspend fun syncSettings(deviceId: String): Result<DeviceSettingsState> = runCatching {
        val response = settingsApi.syncSettings(deviceId)
        val state = response.toDomain()

        // Apply changes locally
        settingsDao.deleteByDeviceId(deviceId)
        settingsDao.insertAll(state.settings.values.map { it.toEntity(deviceId) })

        // Notify about changes
        response.changesApplied.forEach { change ->
            onSettingChangedRemotely(deviceId, change)
        }

        state
    }

    override fun observeSettings(deviceId: String): Flow<DeviceSettingsState> {
        return settingsDao.observeByDeviceId(deviceId)
            .map { entities ->
                DeviceSettingsState(
                    deviceId = deviceId,
                    settings = entities.associate { it.key to it.toDomain() },
                    policyId = null,
                    managedByGroupId = null,
                    lastSyncedAt = Instant.now()
                )
            }
    }

    private fun onSettingChangedRemotely(deviceId: String, change: SettingChangeDto) {
        // Emit event for observers
        // This could trigger UI updates, setting application, etc.
    }

    // ... other implementations
}

class SettingLockedException(
    val settingKey: String,
    message: String
) : Exception(message)
```

---

## 4. Local Database

### 4.1 Room Entities

```kotlin
// data/local/entity/DeviceSettingEntity.kt
@Entity(
    tableName = "device_settings",
    primaryKeys = ["deviceId", "key"]
)
data class DeviceSettingEntity(
    val deviceId: String,
    val key: String,
    val valueType: String,
    val valueJson: String,
    val isLocked: Boolean,
    val lockedBy: String?,
    val lockedByName: String?,
    val lockedAt: Long?,
    val lockReason: String?,
    val updatedAt: Long,
    val updatedBy: String?,
    val lastSyncedAt: Long = System.currentTimeMillis()
)

// data/local/entity/SettingDefinitionEntity.kt
@Entity(tableName = "setting_definitions")
data class SettingDefinitionEntity(
    @PrimaryKey val key: String,
    val displayName: String,
    val description: String?,
    val dataType: String,
    val defaultValueJson: String,
    val isLockable: Boolean,
    val category: String,
    val validationJson: String?
)

// data/local/entity/UnlockRequestEntity.kt
@Entity(tableName = "unlock_requests")
data class UnlockRequestEntity(
    @PrimaryKey val id: String,
    val deviceId: String,
    val settingKey: String,
    val settingDisplayName: String?,
    val status: String,
    val reason: String,
    val requestedBy: String,
    val requestedByName: String?,
    val decisionNote: String?,
    val decidedBy: String?,
    val decidedByName: String?,
    val decidedAt: Long?,
    val createdAt: Long
)
```

### 4.2 DAOs

```kotlin
// data/local/dao/DeviceSettingsDao.kt
@Dao
interface DeviceSettingsDao {
    @Query("SELECT * FROM device_settings WHERE deviceId = :deviceId")
    fun observeByDeviceId(deviceId: String): Flow<List<DeviceSettingEntity>>

    @Query("SELECT * FROM device_settings WHERE deviceId = :deviceId AND key = :key")
    fun observeBySetting(deviceId: String, key: String): Flow<DeviceSettingEntity?>

    @Query("SELECT * FROM device_settings WHERE deviceId = :deviceId AND isLocked = 1")
    fun observeLockedSettings(deviceId: String): Flow<List<DeviceSettingEntity>>

    @Query("SELECT * FROM device_settings WHERE deviceId = :deviceId AND key = :key")
    suspend fun getSetting(deviceId: String, key: String): DeviceSettingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(setting: DeviceSettingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(settings: List<DeviceSettingEntity>)

    @Query("DELETE FROM device_settings WHERE deviceId = :deviceId")
    suspend fun deleteByDeviceId(deviceId: String)

    @Query("UPDATE device_settings SET isLocked = :isLocked, lockedBy = :lockedBy, lockedByName = :lockedByName, lockedAt = :lockedAt, lockReason = :reason WHERE deviceId = :deviceId AND key = :key")
    suspend fun updateLock(
        deviceId: String,
        key: String,
        isLocked: Boolean,
        lockedBy: String?,
        lockedByName: String?,
        lockedAt: Long?,
        reason: String?
    )
}

// data/local/dao/UnlockRequestDao.kt
@Dao
interface UnlockRequestDao {
    @Query("SELECT * FROM unlock_requests WHERE requestedBy = :userId ORDER BY createdAt DESC")
    fun observeByRequester(userId: String): Flow<List<UnlockRequestEntity>>

    @Query("SELECT * FROM unlock_requests WHERE status = 'PENDING' ORDER BY createdAt DESC")
    fun observePending(): Flow<List<UnlockRequestEntity>>

    @Query("SELECT COUNT(*) FROM unlock_requests WHERE status = 'PENDING'")
    fun observePendingCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(request: UnlockRequestEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(requests: List<UnlockRequestEntity>)

    @Query("DELETE FROM unlock_requests WHERE id = :requestId")
    suspend fun deleteById(requestId: String)

    @Query("UPDATE unlock_requests SET status = :status, decisionNote = :note, decidedBy = :decidedBy, decidedByName = :decidedByName, decidedAt = :decidedAt WHERE id = :requestId")
    suspend fun updateDecision(
        requestId: String,
        status: String,
        note: String?,
        decidedBy: String?,
        decidedByName: String?,
        decidedAt: Long?
    )
}
```

---

## 5. Network Layer

### 5.1 API Interface

```kotlin
// data/remote/api/DeviceSettingsApi.kt
interface DeviceSettingsApi {
    // Settings
    suspend fun getSettings(deviceId: String, includeDefinitions: Boolean = false): DeviceSettingsResponseDto
    suspend fun getSetting(deviceId: String, key: String): DeviceSettingDto
    suspend fun updateSetting(deviceId: String, key: String, request: UpdateSettingRequest): UpdateSettingResponseDto
    suspend fun updateSettings(deviceId: String, request: UpdateSettingsRequest): UpdateSettingsResponseDto
    suspend fun syncSettings(deviceId: String): SyncSettingsResponseDto

    // Locks (admin)
    suspend fun getLocks(deviceId: String): LocksResponseDto
    suspend fun lockSetting(deviceId: String, key: String, request: LockSettingRequest): DeviceSettingDto
    suspend fun unlockSetting(deviceId: String, key: String): DeviceSettingDto
    suspend fun updateLocks(deviceId: String, request: UpdateLocksRequest): UpdateLocksResponseDto

    // Unlock requests (user)
    suspend fun requestUnlock(deviceId: String, key: String, request: UnlockRequestDto): UnlockRequestResponseDto
    suspend fun getMyUnlockRequests(): UnlockRequestListResponseDto
    suspend fun cancelUnlockRequest(requestId: String)

    // Unlock requests (admin)
    suspend fun getGroupUnlockRequests(groupId: String, status: String? = null): UnlockRequestListResponseDto
    suspend fun respondToUnlockRequest(requestId: String, request: RespondToUnlockRequest): UnlockRequestResponseDto

    // Definitions
    suspend fun getSettingDefinitions(): SettingDefinitionListResponseDto
}
```

### 5.2 DTOs

```kotlin
// data/remote/dto/DeviceSettingsDto.kt
@Serializable
data class DeviceSettingsResponseDto(
    @SerialName("device_id") val deviceId: String,
    val settings: Map<String, DeviceSettingDto>,
    @SerialName("policy_id") val policyId: String? = null,
    @SerialName("managed_by_group_id") val managedByGroupId: String? = null,
    @SerialName("last_synced_at") val lastSyncedAt: String,
    val definitions: List<SettingDefinitionDto>? = null
)

@Serializable
data class DeviceSettingDto(
    val key: String,
    val value: JsonElement,
    @SerialName("is_locked") val isLocked: Boolean,
    @SerialName("locked_by") val lockedBy: String? = null,
    @SerialName("locked_by_name") val lockedByName: String? = null,
    @SerialName("locked_at") val lockedAt: String? = null,
    @SerialName("lock_reason") val lockReason: String? = null,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("updated_by") val updatedBy: String? = null,
    val definition: SettingDefinitionDto? = null,
    val error: String? = null
)

@Serializable
data class SettingDefinitionDto(
    val key: String,
    @SerialName("display_name") val displayName: String,
    val description: String? = null,
    @SerialName("data_type") val dataType: String,
    @SerialName("default_value") val defaultValue: JsonElement,
    @SerialName("is_lockable") val isLockable: Boolean,
    val category: String,
    val validation: SettingValidationDto? = null
)

@Serializable
data class SettingValidationDto(
    @SerialName("min_value") val minValue: Double? = null,
    @SerialName("max_value") val maxValue: Double? = null,
    @SerialName("allowed_values") val allowedValues: List<String>? = null,
    val pattern: String? = null
)

// Request DTOs
@Serializable
data class UpdateSettingRequest(
    val value: JsonElement
)

@Serializable
data class UpdateSettingsRequest(
    val settings: Map<String, JsonElement>
)

@Serializable
data class LockSettingRequest(
    val reason: String? = null,
    val value: JsonElement? = null,
    @SerialName("notify_user") val notifyUser: Boolean = true
)

@Serializable
data class UpdateLocksRequest(
    val locks: Map<String, Boolean>,
    val reason: String? = null,
    @SerialName("notify_user") val notifyUser: Boolean = true
)

@Serializable
data class UnlockRequestDto(
    val reason: String
)

@Serializable
data class RespondToUnlockRequest(
    val status: String,  // "approved" or "denied"
    val note: String? = null
)

// Response DTOs
@Serializable
data class UpdateSettingResponseDto(
    val key: String,
    val value: JsonElement,
    @SerialName("is_locked") val isLocked: Boolean,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("updated_by") val updatedBy: String? = null,
    val error: String? = null
)

@Serializable
data class UpdateSettingsResponseDto(
    val updated: List<String>,
    val locked: List<String>,
    val settings: Map<String, DeviceSettingDto>
)

@Serializable
data class SyncSettingsResponseDto(
    @SerialName("synced_at") val syncedAt: String,
    val settings: Map<String, DeviceSettingDto>,
    @SerialName("changes_applied") val changesApplied: List<SettingChangeDto>
)

@Serializable
data class SettingChangeDto(
    val key: String,
    @SerialName("old_value") val oldValue: JsonElement? = null,
    @SerialName("new_value") val newValue: JsonElement,
    val reason: String? = null
)

@Serializable
data class UnlockRequestResponseDto(
    val id: String,
    @SerialName("device_id") val deviceId: String,
    @SerialName("setting_key") val settingKey: String,
    @SerialName("setting_display_name") val settingDisplayName: String? = null,
    val status: String,
    val reason: String,
    @SerialName("requested_by") val requestedBy: RequestedByDto,
    @SerialName("decision_note") val decisionNote: String? = null,
    @SerialName("decided_by") val decidedBy: DecidedByDto? = null,
    @SerialName("decided_at") val decidedAt: String? = null,
    @SerialName("setting_unlocked") val settingUnlocked: Boolean? = null,
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class RequestedByDto(
    val id: String,
    @SerialName("display_name") val displayName: String
)

@Serializable
data class DecidedByDto(
    val id: String,
    @SerialName("display_name") val displayName: String
)
```

---

## 6. Use Cases

### 6.1 Settings Use Cases

```kotlin
// domain/usecase/settings/GetDeviceSettingsUseCase.kt
class GetDeviceSettingsUseCase @Inject constructor(
    private val settingsRepository: DeviceSettingsRepository,
    private val deviceIdentifier: DeviceIdentifier
) {
    suspend operator fun invoke(deviceId: String? = null): Result<DeviceSettingsState> {
        val targetDeviceId = deviceId ?: getCurrentDeviceId()
        return settingsRepository.getSettings(targetDeviceId)
    }

    fun observe(deviceId: String? = null): Flow<DeviceSettingsState> {
        val targetDeviceId = deviceId ?: getCurrentDeviceId()
        return settingsRepository.observeSettings(targetDeviceId)
    }

    private fun getCurrentDeviceId(): String {
        // Get current device ID from local storage
        return deviceIdentifier.getDeviceId()
    }
}

// domain/usecase/settings/UpdateSettingUseCase.kt
class UpdateSettingUseCase @Inject constructor(
    private val settingsRepository: DeviceSettingsRepository
) {
    suspend operator fun invoke(
        deviceId: String,
        key: String,
        value: SettingValue
    ): Result<DeviceSetting> {
        return settingsRepository.updateSetting(deviceId, key, value)
    }
}

// domain/usecase/settings/SyncSettingsUseCase.kt
class SyncSettingsUseCase @Inject constructor(
    private val settingsRepository: DeviceSettingsRepository
) {
    suspend operator fun invoke(deviceId: String): Result<DeviceSettingsState> {
        return settingsRepository.syncSettings(deviceId)
    }
}
```

### 6.2 Lock Management Use Cases (Admin)

```kotlin
// domain/usecase/settings/LockSettingUseCase.kt
class LockSettingUseCase @Inject constructor(
    private val settingsRepository: DeviceSettingsRepository
) {
    suspend operator fun invoke(
        deviceId: String,
        key: String,
        value: SettingValue? = null,
        reason: String? = null
    ): Result<DeviceSetting> {
        return settingsRepository.lockSetting(deviceId, key, value, reason)
    }
}

// domain/usecase/settings/UnlockSettingUseCase.kt
class UnlockSettingUseCase @Inject constructor(
    private val settingsRepository: DeviceSettingsRepository
) {
    suspend operator fun invoke(deviceId: String, key: String): Result<DeviceSetting> {
        return settingsRepository.unlockSetting(deviceId, key)
    }
}

// domain/usecase/settings/BulkUpdateLocksUseCase.kt
class BulkUpdateLocksUseCase @Inject constructor(
    private val settingsRepository: DeviceSettingsRepository
) {
    suspend operator fun invoke(
        deviceId: String,
        locks: Map<String, Boolean>,
        reason: String? = null
    ): Result<List<DeviceSetting>> {
        return settingsRepository.updateLocks(deviceId, locks, reason)
    }
}
```

### 6.3 Unlock Request Use Cases

```kotlin
// domain/usecase/settings/RequestUnlockUseCase.kt
class RequestUnlockUseCase @Inject constructor(
    private val settingsRepository: DeviceSettingsRepository
) {
    suspend operator fun invoke(
        deviceId: String,
        settingKey: String,
        reason: String
    ): Result<UnlockRequest> {
        require(reason.isNotBlank()) { "Reason is required" }
        require(reason.length <= 500) { "Reason too long" }

        return settingsRepository.requestUnlock(deviceId, settingKey, reason)
    }
}

// domain/usecase/settings/GetMyUnlockRequestsUseCase.kt
class GetMyUnlockRequestsUseCase @Inject constructor(
    private val settingsRepository: DeviceSettingsRepository
) {
    suspend operator fun invoke(): Result<List<UnlockRequest>> {
        return settingsRepository.getMyUnlockRequests()
    }

    fun observe(): Flow<List<UnlockRequest>> {
        return settingsRepository.observeMyUnlockRequests()
    }
}

// domain/usecase/settings/RespondToUnlockRequestUseCase.kt
class RespondToUnlockRequestUseCase @Inject constructor(
    private val settingsRepository: DeviceSettingsRepository
) {
    suspend fun approve(requestId: String, note: String? = null): Result<UnlockRequest> {
        return settingsRepository.respondToUnlockRequest(requestId, approved = true, note = note)
    }

    suspend fun deny(requestId: String, note: String? = null): Result<UnlockRequest> {
        return settingsRepository.respondToUnlockRequest(requestId, approved = false, note = note)
    }
}
```

---

## 7. ViewModels

### 7.1 Device Settings ViewModel

```kotlin
// presentation/viewmodel/DeviceSettingsViewModel.kt
@HiltViewModel
class DeviceSettingsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getDeviceSettingsUseCase: GetDeviceSettingsUseCase,
    private val updateSettingUseCase: UpdateSettingUseCase,
    private val syncSettingsUseCase: SyncSettingsUseCase,
    private val requestUnlockUseCase: RequestUnlockUseCase
) : ViewModel() {

    private val deviceId: String = savedStateHandle.get<String>("deviceId")
        ?: throw IllegalArgumentException("deviceId required")

    private val _uiState = MutableStateFlow(DeviceSettingsUiState())
    val uiState: StateFlow<DeviceSettingsUiState> = _uiState.asStateFlow()

    private val _events = Channel<DeviceSettingsEvent>(Channel.BUFFERED)
    val events: Flow<DeviceSettingsEvent> = _events.receiveAsFlow()

    init {
        observeSettings()
        loadSettings()
    }

    private fun observeSettings() {
        viewModelScope.launch {
            getDeviceSettingsUseCase.observe(deviceId).collect { state ->
                _uiState.update {
                    it.copy(
                        settingsState = state,
                        settingsByCategory = state.settingsByCategory()
                    )
                }
            }
        }
    }

    fun loadSettings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            getDeviceSettingsUseCase(deviceId)
                .onSuccess { state ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            settingsState = state,
                            settingsByCategory = state.settingsByCategory()
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    fun updateSetting(key: String, value: SettingValue) {
        viewModelScope.launch {
            _uiState.update { it.copy(updatingKey = key) }

            updateSettingUseCase(deviceId, key, value)
                .onSuccess {
                    _uiState.update { it.copy(updatingKey = null) }
                    _events.send(DeviceSettingsEvent.SettingUpdated(key))
                }
                .onFailure { error ->
                    _uiState.update { it.copy(updatingKey = null) }

                    if (error is SettingLockedException) {
                        _events.send(DeviceSettingsEvent.SettingLocked(key, error.message ?: ""))
                    } else {
                        _events.send(DeviceSettingsEvent.Error(error.message ?: "Update failed"))
                    }
                }
        }
    }

    fun syncSettings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true) }

            syncSettingsUseCase(deviceId)
                .onSuccess {
                    _uiState.update { it.copy(isSyncing = false) }
                    _events.send(DeviceSettingsEvent.SettingsSynced)
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isSyncing = false) }
                    _events.send(DeviceSettingsEvent.Error(error.message ?: "Sync failed"))
                }
        }
    }

    fun requestUnlock(key: String, reason: String) {
        viewModelScope.launch {
            requestUnlockUseCase(deviceId, key, reason)
                .onSuccess { request ->
                    _events.send(DeviceSettingsEvent.UnlockRequested(request))
                }
                .onFailure { error ->
                    _events.send(DeviceSettingsEvent.Error(error.message ?: "Request failed"))
                }
        }
    }
}

data class DeviceSettingsUiState(
    val isLoading: Boolean = false,
    val isSyncing: Boolean = false,
    val updatingKey: String? = null,
    val settingsState: DeviceSettingsState? = null,
    val settingsByCategory: Map<SettingCategory, List<DeviceSetting>> = emptyMap(),
    val error: String? = null
) {
    val hasLockedSettings: Boolean
        get() = settingsState?.lockedCount ?: 0 > 0
}

sealed class DeviceSettingsEvent {
    data class SettingUpdated(val key: String) : DeviceSettingsEvent()
    data class SettingLocked(val key: String, val message: String) : DeviceSettingsEvent()
    data class UnlockRequested(val request: UnlockRequest) : DeviceSettingsEvent()
    object SettingsSynced : DeviceSettingsEvent()
    data class Error(val message: String) : DeviceSettingsEvent()
}
```

### 7.2 Admin Lock Management ViewModel

```kotlin
// presentation/viewmodel/AdminLockManagementViewModel.kt
@HiltViewModel
class AdminLockManagementViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getDeviceSettingsUseCase: GetDeviceSettingsUseCase,
    private val lockSettingUseCase: LockSettingUseCase,
    private val unlockSettingUseCase: UnlockSettingUseCase,
    private val bulkUpdateLocksUseCase: BulkUpdateLocksUseCase
) : ViewModel() {

    private val deviceId: String = savedStateHandle.get<String>("deviceId")!!

    private val _uiState = MutableStateFlow(AdminLockUiState())
    val uiState: StateFlow<AdminLockUiState> = _uiState.asStateFlow()

    private val _events = Channel<AdminLockEvent>(Channel.BUFFERED)
    val events: Flow<AdminLockEvent> = _events.receiveAsFlow()

    // Track pending lock changes for bulk operations
    private val pendingLockChanges = mutableMapOf<String, Boolean>()

    init {
        observeSettings()
        loadSettings()
    }

    private fun observeSettings() {
        viewModelScope.launch {
            getDeviceSettingsUseCase.observe(deviceId).collect { state ->
                _uiState.update {
                    it.copy(
                        settings = state.settings.values.toList(),
                        lockedCount = state.lockedCount
                    )
                }
            }
        }
    }

    fun loadSettings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            getDeviceSettingsUseCase(deviceId)
                .onSuccess { state ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            settings = state.settings.values.toList(),
                            lockedCount = state.lockedCount
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false) }
                    _events.send(AdminLockEvent.Error(error.message ?: "Failed to load"))
                }
        }
    }

    fun toggleLock(key: String) {
        val currentSetting = _uiState.value.settings.find { it.key == key } ?: return
        val newLockState = !currentSetting.isLocked

        pendingLockChanges[key] = newLockState
        _uiState.update { it.copy(pendingChanges = pendingLockChanges.toMap()) }
    }

    fun lockSetting(key: String, value: SettingValue?, reason: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(processingKey = key) }

            lockSettingUseCase(deviceId, key, value, reason)
                .onSuccess {
                    _uiState.update { it.copy(processingKey = null) }
                    _events.send(AdminLockEvent.SettingLocked(key))
                }
                .onFailure { error ->
                    _uiState.update { it.copy(processingKey = null) }
                    _events.send(AdminLockEvent.Error(error.message ?: "Failed to lock"))
                }
        }
    }

    fun unlockSetting(key: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(processingKey = key) }

            unlockSettingUseCase(deviceId, key)
                .onSuccess {
                    _uiState.update { it.copy(processingKey = null) }
                    _events.send(AdminLockEvent.SettingUnlocked(key))
                }
                .onFailure { error ->
                    _uiState.update { it.copy(processingKey = null) }
                    _events.send(AdminLockEvent.Error(error.message ?: "Failed to unlock"))
                }
        }
    }

    fun applyPendingChanges(reason: String?) {
        if (pendingLockChanges.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isApplyingBulk = true) }

            bulkUpdateLocksUseCase(deviceId, pendingLockChanges.toMap(), reason)
                .onSuccess {
                    pendingLockChanges.clear()
                    _uiState.update {
                        it.copy(isApplyingBulk = false, pendingChanges = emptyMap())
                    }
                    _events.send(AdminLockEvent.BulkLocksUpdated)
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isApplyingBulk = false) }
                    _events.send(AdminLockEvent.Error(error.message ?: "Failed to update"))
                }
        }
    }

    fun discardPendingChanges() {
        pendingLockChanges.clear()
        _uiState.update { it.copy(pendingChanges = emptyMap()) }
    }
}

data class AdminLockUiState(
    val isLoading: Boolean = false,
    val isApplyingBulk: Boolean = false,
    val processingKey: String? = null,
    val settings: List<DeviceSetting> = emptyList(),
    val lockedCount: Int = 0,
    val pendingChanges: Map<String, Boolean> = emptyMap()
) {
    val hasPendingChanges: Boolean get() = pendingChanges.isNotEmpty()

    val lockableSettings: List<DeviceSetting>
        get() = settings.filter { it.definition?.isLockable == true }
}

sealed class AdminLockEvent {
    data class SettingLocked(val key: String) : AdminLockEvent()
    data class SettingUnlocked(val key: String) : AdminLockEvent()
    object BulkLocksUpdated : AdminLockEvent()
    data class Error(val message: String) : AdminLockEvent()
}
```

### 7.3 Unlock Requests ViewModel (Admin)

```kotlin
// presentation/viewmodel/UnlockRequestsViewModel.kt
@HiltViewModel
class UnlockRequestsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val settingsRepository: DeviceSettingsRepository,
    private val respondToUnlockRequestUseCase: RespondToUnlockRequestUseCase
) : ViewModel() {

    private val groupId: String = savedStateHandle.get<String>("groupId")!!

    private val _uiState = MutableStateFlow(UnlockRequestsUiState())
    val uiState: StateFlow<UnlockRequestsUiState> = _uiState.asStateFlow()

    private val _events = Channel<UnlockRequestsEvent>(Channel.BUFFERED)
    val events: Flow<UnlockRequestsEvent> = _events.receiveAsFlow()

    init {
        observeRequests()
        loadRequests()
    }

    private fun observeRequests() {
        viewModelScope.launch {
            settingsRepository.observeGroupUnlockRequests(groupId).collect { requests ->
                _uiState.update { it.copy(requests = requests) }
            }
        }
    }

    fun loadRequests() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            settingsRepository.getGroupUnlockRequests(groupId)
                .onSuccess { requests ->
                    _uiState.update { it.copy(isLoading = false, requests = requests) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false) }
                    _events.send(UnlockRequestsEvent.Error(error.message ?: "Failed to load"))
                }
        }
    }

    fun approveRequest(requestId: String, note: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(processingRequestId = requestId) }

            respondToUnlockRequestUseCase.approve(requestId, note)
                .onSuccess {
                    _uiState.update { it.copy(processingRequestId = null) }
                    _events.send(UnlockRequestsEvent.RequestApproved)
                }
                .onFailure { error ->
                    _uiState.update { it.copy(processingRequestId = null) }
                    _events.send(UnlockRequestsEvent.Error(error.message ?: "Failed"))
                }
        }
    }

    fun denyRequest(requestId: String, note: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(processingRequestId = requestId) }

            respondToUnlockRequestUseCase.deny(requestId, note)
                .onSuccess {
                    _uiState.update { it.copy(processingRequestId = null) }
                    _events.send(UnlockRequestsEvent.RequestDenied)
                }
                .onFailure { error ->
                    _uiState.update { it.copy(processingRequestId = null) }
                    _events.send(UnlockRequestsEvent.Error(error.message ?: "Failed"))
                }
        }
    }

    fun setFilter(status: UnlockRequestStatus?) {
        _uiState.update { it.copy(filterStatus = status) }
    }
}

data class UnlockRequestsUiState(
    val isLoading: Boolean = false,
    val processingRequestId: String? = null,
    val requests: List<UnlockRequest> = emptyList(),
    val filterStatus: UnlockRequestStatus? = UnlockRequestStatus.PENDING
) {
    val filteredRequests: List<UnlockRequest>
        get() = filterStatus?.let { status ->
            requests.filter { it.status == status }
        } ?: requests

    val pendingCount: Int
        get() = requests.count { it.status == UnlockRequestStatus.PENDING }
}

sealed class UnlockRequestsEvent {
    object RequestApproved : UnlockRequestsEvent()
    object RequestDenied : UnlockRequestsEvent()
    data class Error(val message: String) : UnlockRequestsEvent()
}
```

---

## 8. Settings Synchronization

### 8.1 Settings Sync Worker

```kotlin
// worker/SettingsSyncWorker.kt
@HiltWorker
class SettingsSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val settingsRepository: DeviceSettingsRepository,
    private val deviceIdentifier: DeviceIdentifier,
    private val settingsApplicator: SettingsApplicator
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val deviceId = inputData.getString(KEY_DEVICE_ID)
            ?: deviceIdentifier.getDeviceId()

        return try {
            val settingsState = settingsRepository.syncSettings(deviceId).getOrThrow()

            // Apply settings to device
            settingsApplicator.applySettings(settingsState)

            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < MAX_RETRIES) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    companion object {
        const val KEY_DEVICE_ID = "device_id"
        private const val MAX_RETRIES = 3

        fun createOneTimeRequest(deviceId: String? = null): OneTimeWorkRequest {
            return OneTimeWorkRequestBuilder<SettingsSyncWorker>()
                .setInputData(workDataOf(KEY_DEVICE_ID to deviceId))
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()
        }

        fun createPeriodicRequest(): PeriodicWorkRequest {
            return PeriodicWorkRequestBuilder<SettingsSyncWorker>(
                15, TimeUnit.MINUTES,
                5, TimeUnit.MINUTES
            )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()
        }
    }
}
```

### 8.2 Settings Applicator

```kotlin
// service/SettingsApplicator.kt
@Singleton
class SettingsApplicator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val trackingManager: TrackingManager,
    private val notificationManager: AppNotificationManager,
    private val privacyManager: PrivacyManager
) {
    suspend fun applySettings(settingsState: DeviceSettingsState) {
        settingsState.settings.forEach { (key, setting) ->
            applySetting(key, setting.value)
        }
    }

    suspend fun applySetting(key: String, value: SettingValue) {
        when (key) {
            // Tracking settings
            "tracking_enabled" -> trackingManager.setEnabled(value.asBoolean())
            "tracking_interval_minutes" -> trackingManager.setInterval(value.asInt())
            "movement_detection_enabled" -> trackingManager.setMovementDetection(value.asBoolean())
            "movement_threshold_meters" -> trackingManager.setMovementThreshold(value.asInt())

            // Privacy settings
            "secret_mode_enabled" -> privacyManager.setSecretMode(value.asBoolean())
            "location_blur_enabled" -> privacyManager.setLocationBlur(value.asBoolean())
            "location_blur_radius_meters" -> privacyManager.setBlurRadius(value.asInt())

            // Notification settings
            "notifications_enabled" -> notificationManager.setEnabled(value.asBoolean())
            "notification_sound_enabled" -> notificationManager.setSoundEnabled(value.asBoolean())

            // Battery settings
            "battery_optimization_enabled" -> {
                // This requires user interaction, just log intent
            }

            // Other settings can be handled here
            else -> {
                // Unknown setting - log for debugging
            }
        }
    }
}
```

---

## 9. Push Notification Handling

### 9.1 Settings Notification Handler

```kotlin
// service/SettingsNotificationHandler.kt
class SettingsNotificationHandler @Inject constructor(
    private val settingsDao: DeviceSettingsDao,
    private val unlockRequestDao: UnlockRequestDao,
    private val settingsApplicator: SettingsApplicator,
    private val notificationManager: NotificationManager,
    private val workManager: WorkManager
) {
    suspend fun handleNotification(data: Map<String, String>) {
        when (data["type"]) {
            "settings_changed" -> handleSettingsChanged(data)
            "setting_locked" -> handleSettingLocked(data)
            "setting_unlocked" -> handleSettingUnlocked(data)
            "unlock_request_response" -> handleUnlockResponse(data)
            "unlock_request_received" -> handleUnlockRequestReceived(data)
        }
    }

    private suspend fun handleSettingsChanged(data: Map<String, String>) {
        val deviceId = data["device_id"] ?: return
        val changesJson = data["changes"] ?: return

        // Trigger sync to get latest settings
        workManager.enqueueUniqueWork(
            "settings_sync_$deviceId",
            ExistingWorkPolicy.REPLACE,
            SettingsSyncWorker.createOneTimeRequest(deviceId)
        )

        // Show notification
        val changedBy = data["changed_by"] ?: "Admin"
        notificationManager.showSettingsChanged(changedBy)
    }

    private suspend fun handleSettingLocked(data: Map<String, String>) {
        val deviceId = data["device_id"] ?: return
        val key = data["key"] ?: return
        val newValue = data["new_value"]
        val reason = data["reason"]
        val lockedBy = data["locked_by"]
        val lockedByName = data["locked_by_name"]

        // Update local database
        settingsDao.updateLock(
            deviceId = deviceId,
            key = key,
            isLocked = true,
            lockedBy = lockedBy,
            lockedByName = lockedByName,
            lockedAt = System.currentTimeMillis(),
            reason = reason
        )

        // Apply new value if provided
        newValue?.let {
            val settingValue = parseSettingValue(key, it)
            settingsApplicator.applySetting(key, settingValue)
        }

        // Show notification
        notificationManager.showSettingLocked(
            settingName = getSettingDisplayName(key),
            reason = reason,
            lockedBy = lockedByName
        )
    }

    private suspend fun handleUnlockResponse(data: Map<String, String>) {
        val requestId = data["request_id"] ?: return
        val status = data["status"] ?: return
        val note = data["note"]
        val decidedBy = data["decided_by"]
        val decidedByName = data["decided_by_name"]

        // Update local database
        unlockRequestDao.updateDecision(
            requestId = requestId,
            status = status,
            note = note,
            decidedBy = decidedBy,
            decidedByName = decidedByName,
            decidedAt = System.currentTimeMillis()
        )

        // Show notification
        val approved = status == "approved"
        notificationManager.showUnlockRequestResponse(
            settingName = data["setting_key"] ?: "Setting",
            approved = approved,
            note = note
        )

        // If approved, trigger settings sync
        if (approved) {
            val deviceId = data["device_id"]
            workManager.enqueue(SettingsSyncWorker.createOneTimeRequest(deviceId))
        }
    }

    private suspend fun handleUnlockRequestReceived(data: Map<String, String>) {
        // For admins - new unlock request from a user
        val requestId = data["request_id"] ?: return
        val deviceName = data["device_name"] ?: "Device"
        val settingName = data["setting_display_name"] ?: "Setting"
        val requestedByName = data["requested_by_name"] ?: "User"
        val reason = data["reason"]

        notificationManager.showUnlockRequestReceived(
            requestId = requestId,
            deviceName = deviceName,
            settingName = settingName,
            requestedBy = requestedByName,
            reason = reason
        )
    }

    // ... helper methods
}
```

---

## 10. Available Settings

### 10.1 Setting Keys

```kotlin
// domain/model/SettingKeys.kt
object SettingKeys {
    // Tracking
    const val TRACKING_ENABLED = "tracking_enabled"
    const val TRACKING_INTERVAL_MINUTES = "tracking_interval_minutes"
    const val MOVEMENT_DETECTION_ENABLED = "movement_detection_enabled"
    const val MOVEMENT_THRESHOLD_METERS = "movement_threshold_meters"
    const val HIGH_ACCURACY_MODE = "high_accuracy_mode"

    // Privacy
    const val SECRET_MODE_ENABLED = "secret_mode_enabled"
    const val LOCATION_BLUR_ENABLED = "location_blur_enabled"
    const val LOCATION_BLUR_RADIUS_METERS = "location_blur_radius_meters"
    const val HIDE_FROM_MAP = "hide_from_map"

    // Notifications
    const val NOTIFICATIONS_ENABLED = "notifications_enabled"
    const val NOTIFICATION_SOUND_ENABLED = "notification_sound_enabled"
    const val GEOFENCE_ALERTS_ENABLED = "geofence_alerts_enabled"
    const val LOW_BATTERY_ALERTS_ENABLED = "low_battery_alerts_enabled"

    // Battery
    const val BATTERY_SAVER_MODE = "battery_saver_mode"
    const val ADAPTIVE_TRACKING = "adaptive_tracking"

    // Sync
    const val SYNC_WIFI_ONLY = "sync_wifi_only"
    const val BACKGROUND_SYNC_ENABLED = "background_sync_enabled"
    const val SYNC_FREQUENCY_MINUTES = "sync_frequency_minutes"

    // Display
    const val DISTANCE_UNIT = "distance_unit"
    const val MAP_TYPE = "map_type"
    const val DARK_MODE = "dark_mode"

    // Security
    const val REQUIRE_PIN = "require_pin"
    const val BIOMETRIC_ENABLED = "biometric_enabled"
    const val AUTO_LOCK_MINUTES = "auto_lock_minutes"
}
```

---

## 11. Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0.0 | 2025-12-01 | Initial specification |

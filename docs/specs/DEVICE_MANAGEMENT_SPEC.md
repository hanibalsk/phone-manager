# Device Management Specification

## Phone Manager Android - Device Ownership & Control

**Version:** 1.0.0
**Status:** Design Specification
**Last Updated:** 2025-12-01

---

## 1. Overview

This document specifies the Android implementation for device registration with user binding, device ownership management, linking devices to groups, and viewing devices within groups.

---

## 2. Domain Models

### 2.1 Device Model

```kotlin
// domain/model/Device.kt
data class Device(
    val id: String,
    val deviceUuid: String,
    val displayName: String,
    val platform: DevicePlatform,
    val ownerId: String?,
    val ownerName: String?,
    val organizationId: String?,
    val isManaged: Boolean,
    val enrollmentStatus: EnrollmentStatus,
    val isActive: Boolean,
    val lastSeenAt: Instant?,
    val deviceInfo: DeviceInfo?,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    val isThisDevice: Boolean get() = deviceUuid == DeviceIdentifier.getUuid()
    val isOnline: Boolean get() = lastSeenAt?.let {
        Duration.between(it, Instant.now()).toMinutes() < 15
    } ?: false
}

enum class DevicePlatform {
    ANDROID,
    IOS
}

enum class EnrollmentStatus {
    PENDING,
    ENROLLED,
    SUSPENDED,
    RETIRED
}

data class DeviceInfo(
    val manufacturer: String?,
    val model: String?,
    val osVersion: String?,
    val appVersion: String?
)
```

### 2.2 Device-Group Relationship

```kotlin
// domain/model/DeviceGroup.kt
data class DeviceGroupAssignment(
    val deviceId: String,
    val groupId: String,
    val groupName: String,
    val assignedAt: Instant,
    val assignedBy: String?
)

// Device with group information
data class DeviceWithGroups(
    val device: Device,
    val groups: List<DeviceGroupAssignment>
)

// Group device listing
data class GroupDevice(
    val device: Device,
    val ownerName: String?,
    val assignedAt: Instant,
    val settingsLockCount: Int
)
```

### 2.3 Device Registration

```kotlin
// domain/model/DeviceRegistration.kt
data class DeviceRegistrationRequest(
    val deviceUuid: String,
    val displayName: String,
    val platform: DevicePlatform,
    val deviceInfo: DeviceInfo?,
    val fcmToken: String?,
    val groupId: String? = null  // Legacy compatibility
)

data class DeviceRegistrationResponse(
    val device: Device,
    val isNewDevice: Boolean,
    val deviceToken: String?  // For device-based auth
)
```

---

## 3. Repository Layer

### 3.1 Device Repository Interface

```kotlin
// domain/repository/DeviceRepository.kt
interface DeviceRepository {
    // Registration
    suspend fun registerDevice(request: DeviceRegistrationRequest): Result<DeviceRegistrationResponse>
    suspend fun updateFcmToken(fcmToken: String): Result<Unit>

    // Current device
    suspend fun getCurrentDevice(): Result<Device>
    suspend fun updateDeviceName(name: String): Result<Device>

    // My devices (owned)
    suspend fun getMyDevices(): Result<List<Device>>
    suspend fun linkDeviceToUser(deviceId: String): Result<Device>
    suspend fun unlinkDevice(deviceId: String): Result<Unit>
    suspend fun transferDevice(deviceId: String, newOwnerId: String): Result<Device>

    // Device-group management
    suspend fun assignDeviceToGroup(deviceId: String, groupId: String): Result<DeviceGroupAssignment>
    suspend fun removeDeviceFromGroup(deviceId: String, groupId: String): Result<Unit>
    suspend fun getDeviceGroups(deviceId: String): Result<List<DeviceGroupAssignment>>

    // Group devices (admin view)
    suspend fun getGroupDevices(groupId: String): Result<List<GroupDevice>>

    // Reactive streams
    fun observeCurrentDevice(): Flow<Device?>
    fun observeMyDevices(): Flow<List<Device>>
    fun observeGroupDevices(groupId: String): Flow<List<GroupDevice>>
}
```

### 3.2 Repository Implementation

```kotlin
// data/repository/DeviceRepositoryImpl.kt
class DeviceRepositoryImpl @Inject constructor(
    private val deviceApi: DeviceApi,
    private val deviceDao: DeviceDao,
    private val deviceGroupDao: DeviceGroupDao,
    private val deviceIdentifier: DeviceIdentifier,
    private val secureStorage: SecureTokenStorage
) : DeviceRepository {

    override suspend fun registerDevice(
        request: DeviceRegistrationRequest
    ): Result<DeviceRegistrationResponse> = runCatching {
        val response = deviceApi.registerDevice(request.toDto())
        val device = response.toDomain()

        // Store device token if provided
        response.deviceToken?.let { token ->
            secureStorage.saveDeviceToken(token)
        }

        // Cache device locally
        deviceDao.insert(device.device.toEntity())

        device
    }

    override suspend fun getMyDevices(): Result<List<Device>> = runCatching {
        val response = deviceApi.getMyDevices()
        val devices = response.data.map { it.toDomain() }

        // Cache locally
        deviceDao.insertAll(devices.map { it.toEntity() })

        devices
    }

    override fun observeMyDevices(): Flow<List<Device>> {
        return deviceDao.observeMyDevices()
            .map { entities -> entities.map { it.toDomain() } }
    }

    override suspend fun assignDeviceToGroup(
        deviceId: String,
        groupId: String
    ): Result<DeviceGroupAssignment> = runCatching {
        val response = deviceApi.assignToGroup(deviceId, AssignToGroupRequest(groupId))
        val assignment = response.toDomain()

        deviceGroupDao.insert(assignment.toEntity())

        assignment
    }

    // ... other implementations
}
```

---

## 4. Local Database

### 4.1 Room Entities

```kotlin
// data/local/entity/DeviceEntity.kt
@Entity(tableName = "devices")
data class DeviceEntity(
    @PrimaryKey val id: String,
    val deviceUuid: String,
    val displayName: String,
    val platform: String,
    val ownerId: String?,
    val ownerName: String?,
    val organizationId: String?,
    val isManaged: Boolean,
    val enrollmentStatus: String,
    val isActive: Boolean,
    val lastSeenAt: Long?,
    val manufacturer: String?,
    val model: String?,
    val osVersion: String?,
    val appVersion: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val isCurrentDevice: Boolean = false,
    val lastSyncedAt: Long = System.currentTimeMillis()
)

// data/local/entity/DeviceGroupAssignmentEntity.kt
@Entity(
    tableName = "device_group_assignments",
    primaryKeys = ["deviceId", "groupId"],
    foreignKeys = [
        ForeignKey(
            entity = DeviceEntity::class,
            parentColumns = ["id"],
            childColumns = ["deviceId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = GroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("deviceId"), Index("groupId")]
)
data class DeviceGroupAssignmentEntity(
    val deviceId: String,
    val groupId: String,
    val groupName: String,
    val assignedAt: Long,
    val assignedBy: String?
)
```

### 4.2 DAOs

```kotlin
// data/local/dao/DeviceDao.kt
@Dao
interface DeviceDao {
    @Query("SELECT * FROM devices WHERE isCurrentDevice = 1 LIMIT 1")
    fun observeCurrentDevice(): Flow<DeviceEntity?>

    @Query("SELECT * FROM devices WHERE ownerId = :userId ORDER BY displayName ASC")
    fun observeByOwner(userId: String): Flow<List<DeviceEntity>>

    @Query("SELECT * FROM devices WHERE isCurrentDevice = 1 LIMIT 1")
    suspend fun getCurrentDevice(): DeviceEntity?

    @Query("SELECT * FROM devices WHERE id = :deviceId")
    suspend fun getById(deviceId: String): DeviceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(device: DeviceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(devices: List<DeviceEntity>)

    @Query("UPDATE devices SET displayName = :name, updatedAt = :updatedAt WHERE id = :deviceId")
    suspend fun updateName(deviceId: String, name: String, updatedAt: Long)

    @Query("UPDATE devices SET ownerId = :ownerId, ownerName = :ownerName WHERE id = :deviceId")
    suspend fun updateOwner(deviceId: String, ownerId: String?, ownerName: String?)

    @Query("UPDATE devices SET isCurrentDevice = 1 WHERE deviceUuid = :uuid")
    suspend fun markAsCurrent(uuid: String)

    @Query("DELETE FROM devices WHERE id = :deviceId")
    suspend fun deleteById(deviceId: String)
}

// data/local/dao/DeviceGroupDao.kt
@Dao
interface DeviceGroupDao {
    @Query("SELECT * FROM device_group_assignments WHERE deviceId = :deviceId")
    fun observeByDevice(deviceId: String): Flow<List<DeviceGroupAssignmentEntity>>

    @Query("""
        SELECT d.*, dga.assignedAt,
               (SELECT COUNT(*) FROM device_settings_locks WHERE deviceId = d.id) as lockCount
        FROM devices d
        INNER JOIN device_group_assignments dga ON d.id = dga.deviceId
        WHERE dga.groupId = :groupId
        ORDER BY d.displayName ASC
    """)
    fun observeGroupDevices(groupId: String): Flow<List<GroupDeviceWithLocks>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(assignment: DeviceGroupAssignmentEntity)

    @Query("DELETE FROM device_group_assignments WHERE deviceId = :deviceId AND groupId = :groupId")
    suspend fun delete(deviceId: String, groupId: String)

    @Query("DELETE FROM device_group_assignments WHERE deviceId = :deviceId")
    suspend fun deleteAllForDevice(deviceId: String)
}

data class GroupDeviceWithLocks(
    @Embedded val device: DeviceEntity,
    val assignedAt: Long,
    val lockCount: Int
)
```

---

## 5. Network Layer

### 5.1 API Interface

```kotlin
// data/remote/api/DeviceApi.kt
interface DeviceApi {
    // Registration
    suspend fun registerDevice(request: RegisterDeviceDto): DeviceRegistrationResponseDto
    suspend fun updateFcmToken(request: UpdateFcmTokenRequest)

    // Current device
    suspend fun getCurrentDevice(): DeviceDto
    suspend fun updateDevice(deviceId: String, request: UpdateDeviceRequest): DeviceDto

    // My devices
    suspend fun getMyDevices(): DeviceListResponse
    suspend fun linkDevice(deviceId: String): DeviceDto
    suspend fun unlinkDevice(deviceId: String)
    suspend fun transferDevice(deviceId: String, request: TransferDeviceRequest): DeviceDto

    // Group assignments
    suspend fun assignToGroup(deviceId: String, request: AssignToGroupRequest): DeviceGroupAssignmentDto
    suspend fun removeFromGroup(deviceId: String, groupId: String)
    suspend fun getDeviceGroups(deviceId: String): DeviceGroupListResponse

    // Group devices
    suspend fun getGroupDevices(groupId: String): GroupDeviceListResponse
}
```

### 5.2 Ktor Implementation

```kotlin
// data/remote/api/DeviceApiImpl.kt
class DeviceApiImpl @Inject constructor(
    private val client: HttpClient,
    private val baseUrl: String
) : DeviceApi {

    override suspend fun registerDevice(request: RegisterDeviceDto): DeviceRegistrationResponseDto {
        return client.post("$baseUrl/api/v1/devices/register") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    override suspend fun getMyDevices(): DeviceListResponse {
        return client.get("$baseUrl/api/v1/users/me/devices").body()
    }

    override suspend fun assignToGroup(
        deviceId: String,
        request: AssignToGroupRequest
    ): DeviceGroupAssignmentDto {
        return client.post("$baseUrl/api/v1/devices/$deviceId/groups") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    override suspend fun getGroupDevices(groupId: String): GroupDeviceListResponse {
        return client.get("$baseUrl/api/v1/groups/$groupId/devices").body()
    }

    // ... other implementations
}
```

### 5.3 DTOs

```kotlin
// data/remote/dto/DeviceDto.kt
@Serializable
data class DeviceDto(
    val id: String,
    @SerialName("device_uuid") val deviceUuid: String,
    @SerialName("display_name") val displayName: String,
    val platform: String,
    @SerialName("owner_id") val ownerId: String? = null,
    @SerialName("owner_name") val ownerName: String? = null,
    @SerialName("organization_id") val organizationId: String? = null,
    @SerialName("is_managed") val isManaged: Boolean,
    @SerialName("enrollment_status") val enrollmentStatus: String,
    @SerialName("is_active") val isActive: Boolean,
    @SerialName("last_seen_at") val lastSeenAt: String? = null,
    @SerialName("device_info") val deviceInfo: DeviceInfoDto? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String
)

@Serializable
data class DeviceInfoDto(
    val manufacturer: String? = null,
    val model: String? = null,
    @SerialName("os_version") val osVersion: String? = null,
    @SerialName("app_version") val appVersion: String? = null
)

@Serializable
data class RegisterDeviceDto(
    @SerialName("device_uuid") val deviceUuid: String,
    @SerialName("display_name") val displayName: String,
    val platform: String,
    @SerialName("device_info") val deviceInfo: DeviceInfoDto? = null,
    @SerialName("fcm_token") val fcmToken: String? = null,
    @SerialName("group_id") val groupId: String? = null
)

@Serializable
data class DeviceRegistrationResponseDto(
    val id: String,
    @SerialName("device_uuid") val deviceUuid: String,
    @SerialName("display_name") val displayName: String,
    val platform: String,
    @SerialName("owner_user_id") val ownerUserId: String? = null,
    @SerialName("organization_id") val organizationId: String? = null,
    @SerialName("is_managed") val isManaged: Boolean,
    @SerialName("enrollment_status") val enrollmentStatus: String,
    @SerialName("is_active") val isActive: Boolean,
    @SerialName("device_token") val deviceToken: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String
)

@Serializable
data class DeviceGroupAssignmentDto(
    @SerialName("device_id") val deviceId: String,
    @SerialName("group_id") val groupId: String,
    @SerialName("group_name") val groupName: String,
    @SerialName("assigned_at") val assignedAt: String,
    @SerialName("assigned_by") val assignedBy: String? = null
)

// Request DTOs
@Serializable
data class UpdateDeviceRequest(
    @SerialName("display_name") val displayName: String? = null
)

@Serializable
data class UpdateFcmTokenRequest(
    @SerialName("fcm_token") val fcmToken: String
)

@Serializable
data class AssignToGroupRequest(
    @SerialName("group_id") val groupId: String
)

@Serializable
data class TransferDeviceRequest(
    @SerialName("new_owner_id") val newOwnerId: String
)

// Response DTOs
@Serializable
data class DeviceListResponse(
    val data: List<DeviceDto>,
    val pagination: PaginationDto? = null
)

@Serializable
data class GroupDeviceListResponse(
    val data: List<GroupDeviceDto>,
    val pagination: PaginationDto? = null
)

@Serializable
data class GroupDeviceDto(
    val device: DeviceDto,
    @SerialName("owner_name") val ownerName: String? = null,
    @SerialName("assigned_at") val assignedAt: String,
    @SerialName("settings_lock_count") val settingsLockCount: Int
)
```

---

## 6. Use Cases

### 6.1 Device Registration Use Cases

```kotlin
// domain/usecase/device/RegisterDeviceUseCase.kt
class RegisterDeviceUseCase @Inject constructor(
    private val deviceRepository: DeviceRepository,
    private val deviceIdentifier: DeviceIdentifier,
    private val deviceInfoProvider: DeviceInfoProvider,
    private val fcmTokenProvider: FcmTokenProvider
) {
    suspend operator fun invoke(displayName: String? = null): Result<DeviceRegistrationResponse> {
        val uuid = deviceIdentifier.getUuid()
        val deviceInfo = deviceInfoProvider.getDeviceInfo()
        val fcmToken = fcmTokenProvider.getToken()

        val request = DeviceRegistrationRequest(
            deviceUuid = uuid,
            displayName = displayName ?: deviceInfoProvider.getDefaultName(),
            platform = DevicePlatform.ANDROID,
            deviceInfo = deviceInfo,
            fcmToken = fcmToken
        )

        return deviceRepository.registerDevice(request)
    }
}

// domain/usecase/device/UpdateDeviceNameUseCase.kt
class UpdateDeviceNameUseCase @Inject constructor(
    private val deviceRepository: DeviceRepository
) {
    suspend operator fun invoke(name: String): Result<Device> {
        require(name.isNotBlank()) { "Device name cannot be empty" }
        require(name.length <= 100) { "Device name too long" }

        return deviceRepository.updateDeviceName(name)
    }
}
```

### 6.2 Device Ownership Use Cases

```kotlin
// domain/usecase/device/GetMyDevicesUseCase.kt
class GetMyDevicesUseCase @Inject constructor(
    private val deviceRepository: DeviceRepository
) {
    suspend operator fun invoke(): Result<List<Device>> {
        return deviceRepository.getMyDevices()
    }

    fun observe(): Flow<List<Device>> = deviceRepository.observeMyDevices()
}

// domain/usecase/device/LinkDeviceUseCase.kt
class LinkDeviceUseCase @Inject constructor(
    private val deviceRepository: DeviceRepository
) {
    suspend operator fun invoke(deviceId: String): Result<Device> {
        return deviceRepository.linkDeviceToUser(deviceId)
    }
}

// domain/usecase/device/UnlinkDeviceUseCase.kt
class UnlinkDeviceUseCase @Inject constructor(
    private val deviceRepository: DeviceRepository
) {
    suspend operator fun invoke(deviceId: String): Result<Unit> {
        return deviceRepository.unlinkDevice(deviceId)
    }
}

// domain/usecase/device/TransferDeviceUseCase.kt
class TransferDeviceUseCase @Inject constructor(
    private val deviceRepository: DeviceRepository
) {
    suspend operator fun invoke(deviceId: String, newOwnerId: String): Result<Device> {
        return deviceRepository.transferDevice(deviceId, newOwnerId)
    }
}
```

### 6.3 Device-Group Assignment Use Cases

```kotlin
// domain/usecase/device/AssignDeviceToGroupUseCase.kt
class AssignDeviceToGroupUseCase @Inject constructor(
    private val deviceRepository: DeviceRepository
) {
    suspend operator fun invoke(deviceId: String, groupId: String): Result<DeviceGroupAssignment> {
        return deviceRepository.assignDeviceToGroup(deviceId, groupId)
    }
}

// domain/usecase/device/RemoveDeviceFromGroupUseCase.kt
class RemoveDeviceFromGroupUseCase @Inject constructor(
    private val deviceRepository: DeviceRepository
) {
    suspend operator fun invoke(deviceId: String, groupId: String): Result<Unit> {
        return deviceRepository.removeDeviceFromGroup(deviceId, groupId)
    }
}

// domain/usecase/device/GetGroupDevicesUseCase.kt
class GetGroupDevicesUseCase @Inject constructor(
    private val deviceRepository: DeviceRepository
) {
    suspend operator fun invoke(groupId: String): Result<List<GroupDevice>> {
        return deviceRepository.getGroupDevices(groupId)
    }

    fun observe(groupId: String): Flow<List<GroupDevice>> {
        return deviceRepository.observeGroupDevices(groupId)
    }
}
```

---

## 7. ViewModels

### 7.1 Current Device ViewModel

```kotlin
// presentation/viewmodel/CurrentDeviceViewModel.kt
@HiltViewModel
class CurrentDeviceViewModel @Inject constructor(
    private val deviceRepository: DeviceRepository,
    private val registerDeviceUseCase: RegisterDeviceUseCase,
    private val updateDeviceNameUseCase: UpdateDeviceNameUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CurrentDeviceUiState())
    val uiState: StateFlow<CurrentDeviceUiState> = _uiState.asStateFlow()

    private val _events = Channel<CurrentDeviceEvent>(Channel.BUFFERED)
    val events: Flow<CurrentDeviceEvent> = _events.receiveAsFlow()

    init {
        observeCurrentDevice()
        loadOrRegisterDevice()
    }

    private fun observeCurrentDevice() {
        viewModelScope.launch {
            deviceRepository.observeCurrentDevice().collect { device ->
                _uiState.update { it.copy(device = device) }
            }
        }
    }

    private fun loadOrRegisterDevice() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            deviceRepository.getCurrentDevice()
                .onSuccess { device ->
                    _uiState.update { it.copy(isLoading = false, device = device) }
                }
                .onFailure {
                    // Device not registered, register now
                    registerDevice()
                }
        }
    }

    private suspend fun registerDevice() {
        registerDeviceUseCase()
            .onSuccess { response ->
                _uiState.update { it.copy(isLoading = false, device = response.device) }
            }
            .onFailure { error ->
                _uiState.update { it.copy(isLoading = false, error = error.message) }
            }
    }

    fun updateDeviceName(name: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdating = true) }

            updateDeviceNameUseCase(name)
                .onSuccess { device ->
                    _uiState.update { it.copy(isUpdating = false, device = device) }
                    _events.send(CurrentDeviceEvent.NameUpdated)
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isUpdating = false) }
                    _events.send(CurrentDeviceEvent.Error(error.message ?: "Failed to update"))
                }
        }
    }
}

data class CurrentDeviceUiState(
    val isLoading: Boolean = true,
    val isUpdating: Boolean = false,
    val device: Device? = null,
    val error: String? = null
)

sealed class CurrentDeviceEvent {
    object NameUpdated : CurrentDeviceEvent()
    data class Error(val message: String) : CurrentDeviceEvent()
}
```

### 7.2 My Devices ViewModel

```kotlin
// presentation/viewmodel/MyDevicesViewModel.kt
@HiltViewModel
class MyDevicesViewModel @Inject constructor(
    private val getMyDevicesUseCase: GetMyDevicesUseCase,
    private val unlinkDeviceUseCase: UnlinkDeviceUseCase,
    private val transferDeviceUseCase: TransferDeviceUseCase,
    private val assignDeviceToGroupUseCase: AssignDeviceToGroupUseCase,
    private val removeDeviceFromGroupUseCase: RemoveDeviceFromGroupUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyDevicesUiState())
    val uiState: StateFlow<MyDevicesUiState> = _uiState.asStateFlow()

    private val _events = Channel<MyDevicesEvent>(Channel.BUFFERED)
    val events: Flow<MyDevicesEvent> = _events.receiveAsFlow()

    init {
        observeDevices()
        loadDevices()
    }

    private fun observeDevices() {
        viewModelScope.launch {
            getMyDevicesUseCase.observe().collect { devices ->
                _uiState.update { it.copy(devices = devices) }
            }
        }
    }

    fun loadDevices() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            getMyDevicesUseCase()
                .onSuccess { devices ->
                    _uiState.update { it.copy(isLoading = false, devices = devices) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    fun unlinkDevice(deviceId: String) {
        viewModelScope.launch {
            unlinkDeviceUseCase(deviceId)
                .onSuccess {
                    _events.send(MyDevicesEvent.DeviceUnlinked)
                }
                .onFailure { error ->
                    _events.send(MyDevicesEvent.Error(error.message ?: "Failed to unlink"))
                }
        }
    }

    fun transferDevice(deviceId: String, newOwnerId: String) {
        viewModelScope.launch {
            transferDeviceUseCase(deviceId, newOwnerId)
                .onSuccess {
                    _events.send(MyDevicesEvent.DeviceTransferred)
                }
                .onFailure { error ->
                    _events.send(MyDevicesEvent.Error(error.message ?: "Failed to transfer"))
                }
        }
    }

    fun assignToGroup(deviceId: String, groupId: String) {
        viewModelScope.launch {
            assignDeviceToGroupUseCase(deviceId, groupId)
                .onSuccess {
                    _events.send(MyDevicesEvent.AssignedToGroup)
                }
                .onFailure { error ->
                    _events.send(MyDevicesEvent.Error(error.message ?: "Failed to assign"))
                }
        }
    }

    fun removeFromGroup(deviceId: String, groupId: String) {
        viewModelScope.launch {
            removeDeviceFromGroupUseCase(deviceId, groupId)
                .onSuccess {
                    _events.send(MyDevicesEvent.RemovedFromGroup)
                }
                .onFailure { error ->
                    _events.send(MyDevicesEvent.Error(error.message ?: "Failed to remove"))
                }
        }
    }
}

data class MyDevicesUiState(
    val isLoading: Boolean = false,
    val devices: List<Device> = emptyList(),
    val error: String? = null
) {
    val currentDevice: Device? get() = devices.find { it.isThisDevice }
    val otherDevices: List<Device> get() = devices.filter { !it.isThisDevice }
}

sealed class MyDevicesEvent {
    object DeviceUnlinked : MyDevicesEvent()
    object DeviceTransferred : MyDevicesEvent()
    object AssignedToGroup : MyDevicesEvent()
    object RemovedFromGroup : MyDevicesEvent()
    data class Error(val message: String) : MyDevicesEvent()
}
```

### 7.3 Group Devices ViewModel

```kotlin
// presentation/viewmodel/GroupDevicesViewModel.kt
@HiltViewModel
class GroupDevicesViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getGroupDevicesUseCase: GetGroupDevicesUseCase,
    private val removeDeviceFromGroupUseCase: RemoveDeviceFromGroupUseCase,
    private val groupRepository: GroupRepository
) : ViewModel() {

    private val groupId: String = savedStateHandle.get<String>("groupId")!!

    private val _uiState = MutableStateFlow(GroupDevicesUiState())
    val uiState: StateFlow<GroupDevicesUiState> = _uiState.asStateFlow()

    private val _events = Channel<GroupDevicesEvent>(Channel.BUFFERED)
    val events: Flow<GroupDevicesEvent> = _events.receiveAsFlow()

    init {
        observeGroup()
        observeDevices()
        loadDevices()
    }

    private fun observeGroup() {
        viewModelScope.launch {
            groupRepository.observeGroup(groupId).collect { group ->
                _uiState.update { it.copy(group = group) }
            }
        }
    }

    private fun observeDevices() {
        viewModelScope.launch {
            getGroupDevicesUseCase.observe(groupId).collect { devices ->
                _uiState.update { it.copy(devices = devices) }
            }
        }
    }

    fun loadDevices() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            getGroupDevicesUseCase(groupId)
                .onSuccess { devices ->
                    _uiState.update { it.copy(isLoading = false, devices = devices) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    fun removeDevice(deviceId: String) {
        viewModelScope.launch {
            removeDeviceFromGroupUseCase(deviceId, groupId)
                .onSuccess {
                    _events.send(GroupDevicesEvent.DeviceRemoved)
                }
                .onFailure { error ->
                    _events.send(GroupDevicesEvent.Error(error.message ?: "Failed to remove"))
                }
        }
    }
}

data class GroupDevicesUiState(
    val isLoading: Boolean = false,
    val group: Group? = null,
    val devices: List<GroupDevice> = emptyList(),
    val error: String? = null
) {
    val canManageDevices: Boolean get() = group?.myRole?.canManageMembers() == true
    val onlineCount: Int get() = devices.count { it.device.isOnline }
    val lockedSettingsCount: Int get() = devices.sumOf { it.settingsLockCount }
}

sealed class GroupDevicesEvent {
    object DeviceRemoved : GroupDevicesEvent()
    data class Error(val message: String) : GroupDevicesEvent()
}
```

---

## 8. Device Identifier & Info

### 8.1 Device Identifier

```kotlin
// util/DeviceIdentifier.kt
@Singleton
class DeviceIdentifier @Inject constructor(
    @ApplicationContext private val context: Context,
    private val secureStorage: SecureTokenStorage
) {
    private val deviceUuidKey = "device_uuid"

    /**
     * Get or generate a stable device UUID.
     * This UUID persists across app reinstalls if possible.
     */
    fun getUuid(): String {
        // Try to get existing UUID
        secureStorage.getString(deviceUuidKey)?.let { return it }

        // Generate new UUID
        val uuid = generateDeviceUuid()
        secureStorage.saveString(deviceUuidKey, uuid)
        return uuid
    }

    private fun generateDeviceUuid(): String {
        // Try to use Android ID as seed for consistency
        val androidId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        )

        return if (androidId != null && androidId != "9774d56d682e549c") {
            // Use Android ID to create deterministic UUID
            UUID.nameUUIDFromBytes(androidId.toByteArray()).toString()
        } else {
            // Fallback to random UUID
            UUID.randomUUID().toString()
        }
    }
}
```

### 8.2 Device Info Provider

```kotlin
// util/DeviceInfoProvider.kt
@Singleton
class DeviceInfoProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun getDeviceInfo(): DeviceInfo {
        return DeviceInfo(
            manufacturer = Build.MANUFACTURER,
            model = Build.MODEL,
            osVersion = "Android ${Build.VERSION.RELEASE}",
            appVersion = getAppVersion()
        )
    }

    fun getDefaultName(): String {
        val manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
        val model = Build.MODEL

        return if (model.startsWith(manufacturer, ignoreCase = true)) {
            model
        } else {
            "$manufacturer $model"
        }
    }

    private fun getAppVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "Unknown"
        } catch (e: PackageManager.NameNotFoundException) {
            "Unknown"
        }
    }
}
```

---

## 9. FCM Token Management

### 9.1 Token Provider

```kotlin
// service/FcmTokenProvider.kt
@Singleton
class FcmTokenProvider @Inject constructor() {

    private val _token = MutableStateFlow<String?>(null)
    val token: StateFlow<String?> = _token.asStateFlow()

    suspend fun getToken(): String? {
        return try {
            val token = FirebaseMessaging.getInstance().token.await()
            _token.value = token
            token
        } catch (e: Exception) {
            null
        }
    }

    fun onNewToken(token: String) {
        _token.value = token
    }
}

// service/PhoneManagerFirebaseMessagingService.kt
class PhoneManagerFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var fcmTokenProvider: FcmTokenProvider

    @Inject
    lateinit var deviceRepository: DeviceRepository

    override fun onCreate() {
        super.onCreate()
        // Inject dependencies
        (applicationContext as PhoneManagerApplication).appComponent.inject(this)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)

        fcmTokenProvider.onNewToken(token)

        // Update token on server
        CoroutineScope(Dispatchers.IO).launch {
            deviceRepository.updateFcmToken(token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        // Handle push notifications
        // ... notification handling
    }
}
```

---

## 10. Backward Compatibility

### 10.1 Legacy Device Support

```kotlin
// data/migration/LegacyDeviceMigration.kt
class LegacyDeviceMigration @Inject constructor(
    private val legacyPreferences: SharedPreferences,
    private val deviceRepository: DeviceRepository,
    private val sessionManager: SessionManager
) {
    private val migrationCompletedKey = "legacy_device_migration_completed"

    suspend fun migrateIfNeeded() {
        if (legacyPreferences.getBoolean(migrationCompletedKey, false)) {
            return
        }

        // Check for legacy API key
        val legacyApiKey = legacyPreferences.getString("api_key", null)
        val legacyGroupId = legacyPreferences.getString("group_id", null)

        if (legacyApiKey != null) {
            // Device was already registered with legacy system
            // Re-register with new system to get proper device record
            deviceRepository.registerDevice(
                DeviceRegistrationRequest(
                    deviceUuid = DeviceIdentifier.getUuid(),
                    displayName = Build.MODEL,
                    platform = DevicePlatform.ANDROID,
                    deviceInfo = DeviceInfoProvider.getDeviceInfo(),
                    fcmToken = null,
                    groupId = legacyGroupId  // Preserve group assignment
                )
            )
        }

        legacyPreferences.edit().putBoolean(migrationCompletedKey, true).apply()
    }
}
```

### 10.2 Dual Auth Support

```kotlin
// data/remote/interceptor/DualAuthInterceptor.kt
class DualAuthInterceptor @Inject constructor(
    private val sessionManager: SessionManager,
    private val legacyApiKeyProvider: LegacyApiKeyProvider
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        val authenticatedRequest = when {
            // Use JWT if logged in
            sessionManager.isLoggedIn() -> {
                val token = sessionManager.accessToken
                request.newBuilder()
                    .header("Authorization", "Bearer $token")
                    .build()
            }
            // Fall back to legacy API key
            legacyApiKeyProvider.hasApiKey() -> {
                val apiKey = legacyApiKeyProvider.getApiKey()
                request.newBuilder()
                    .header("X-API-Key", apiKey)
                    .build()
            }
            // No auth
            else -> request
        }

        return chain.proceed(authenticatedRequest)
    }
}
```

---

## 11. Push Notification Handling

### 11.1 Device Notifications

```kotlin
// domain/model/DeviceNotification.kt
sealed class DeviceNotification {
    data class DeviceLinked(
        val deviceId: String,
        val deviceName: String,
        val linkedByName: String
    ) : DeviceNotification()

    data class DeviceUnlinked(
        val deviceId: String,
        val deviceName: String
    ) : DeviceNotification()

    data class DeviceAddedToGroup(
        val deviceId: String,
        val deviceName: String,
        val groupId: String,
        val groupName: String
    ) : DeviceNotification()

    data class DeviceRemovedFromGroup(
        val deviceId: String,
        val deviceName: String,
        val groupId: String,
        val groupName: String
    ) : DeviceNotification()

    data class SettingsChanged(
        val deviceId: String,
        val changes: List<SettingChange>,
        val changedByName: String
    ) : DeviceNotification()
}

data class SettingChange(
    val key: String,
    val action: SettingChangeAction,
    val newValue: Any?
)

enum class SettingChangeAction {
    UPDATED,
    LOCKED,
    UNLOCKED
}
```

---

## 12. Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0.0 | 2025-12-01 | Initial specification |

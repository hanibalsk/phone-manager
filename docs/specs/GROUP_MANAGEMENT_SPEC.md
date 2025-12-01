# Group Management Specification

## Phone Manager Android - Group & Membership Features

**Version:** 1.0.0
**Status:** Design Specification
**Last Updated:** 2025-12-01

---

## 1. Overview

This document specifies the Android implementation for group management, including creating groups, managing memberships, handling invitations, and role-based permissions within groups.

---

## 2. Domain Models

### 2.1 Group Model

```kotlin
// domain/model/Group.kt
data class Group(
    val id: String,
    val name: String,
    val description: String?,
    val ownerId: String,
    val ownerName: String?,
    val memberCount: Int,
    val deviceCount: Int,
    val myRole: GroupRole?,
    val joinedAt: Instant?,
    val createdAt: Instant,
    val updatedAt: Instant
)

enum class GroupRole {
    OWNER,
    ADMIN,
    MEMBER;

    fun canManageMembers(): Boolean = this in listOf(OWNER, ADMIN)
    fun canManageSettings(): Boolean = this in listOf(OWNER, ADMIN)
    fun canInviteMembers(): Boolean = this in listOf(OWNER, ADMIN)
    fun canRemoveMembers(): Boolean = this in listOf(OWNER, ADMIN)
    fun canDeleteGroup(): Boolean = this == OWNER
    fun canTransferOwnership(): Boolean = this == OWNER
}
```

### 2.2 Group Membership Model

```kotlin
// domain/model/GroupMembership.kt
data class GroupMembership(
    val id: String,
    val groupId: String,
    val userId: String,
    val userDisplayName: String,
    val userEmail: String?,
    val role: GroupRole,
    val deviceCount: Int,
    val joinedAt: Instant,
    val invitedBy: String?
)
```

### 2.3 Group Invitation Model

```kotlin
// domain/model/GroupInvite.kt
data class GroupInvite(
    val id: String,
    val groupId: String,
    val groupName: String,
    val inviteCode: String,
    val inviteType: InviteType,
    val maxUses: Int?,
    val useCount: Int,
    val expiresAt: Instant?,
    val isActive: Boolean,
    val createdBy: String,
    val createdAt: Instant
)

enum class InviteType {
    SINGLE_USE,
    MULTI_USE,
    UNLIMITED
}

// Pending invitation (received by user)
data class PendingInvitation(
    val id: String,
    val groupId: String,
    val groupName: String,
    val groupDescription: String?,
    val invitedBy: String,
    val invitedByName: String,
    val memberCount: Int,
    val expiresAt: Instant?,
    val createdAt: Instant
)
```

---

## 3. Repository Layer

### 3.1 Group Repository Interface

```kotlin
// domain/repository/GroupRepository.kt
interface GroupRepository {
    // Group CRUD
    suspend fun getMyGroups(): Result<List<Group>>
    suspend fun getGroup(groupId: String): Result<Group>
    suspend fun createGroup(name: String, description: String?): Result<Group>
    suspend fun updateGroup(groupId: String, name: String?, description: String?): Result<Group>
    suspend fun deleteGroup(groupId: String): Result<Unit>
    suspend fun leaveGroup(groupId: String): Result<Unit>

    // Membership management
    suspend fun getGroupMembers(groupId: String): Result<List<GroupMembership>>
    suspend fun updateMemberRole(groupId: String, userId: String, role: GroupRole): Result<GroupMembership>
    suspend fun removeMember(groupId: String, userId: String): Result<Unit>
    suspend fun transferOwnership(groupId: String, newOwnerId: String): Result<Group>

    // Invitations (admin)
    suspend fun createInvite(
        groupId: String,
        type: InviteType,
        maxUses: Int?,
        expiresInHours: Int?
    ): Result<GroupInvite>
    suspend fun getGroupInvites(groupId: String): Result<List<GroupInvite>>
    suspend fun revokeInvite(groupId: String, inviteId: String): Result<Unit>

    // Invitations (user)
    suspend fun getPendingInvitations(): Result<List<PendingInvitation>>
    suspend fun joinWithCode(inviteCode: String): Result<Group>
    suspend fun acceptInvitation(inviteId: String): Result<Group>
    suspend fun declineInvitation(inviteId: String): Result<Unit>

    // Group devices
    suspend fun getGroupDevices(groupId: String): Result<List<Device>>

    // Reactive streams
    fun observeMyGroups(): Flow<List<Group>>
    fun observeGroup(groupId: String): Flow<Group?>
    fun observeGroupMembers(groupId: String): Flow<List<GroupMembership>>
    fun observePendingInvitations(): Flow<List<PendingInvitation>>
}
```

### 3.2 Repository Implementation

```kotlin
// data/repository/GroupRepositoryImpl.kt
class GroupRepositoryImpl @Inject constructor(
    private val groupApi: GroupApi,
    private val groupDao: GroupDao,
    private val membershipDao: GroupMembershipDao,
    private val inviteDao: GroupInviteDao
) : GroupRepository {

    override suspend fun getMyGroups(): Result<List<Group>> = runCatching {
        val response = groupApi.getMyGroups()
        val groups = response.data.map { it.toDomain() }

        // Cache locally
        groupDao.insertAll(groups.map { it.toEntity() })

        groups
    }

    override fun observeMyGroups(): Flow<List<Group>> {
        return groupDao.observeAll()
            .map { entities -> entities.map { it.toDomain() } }
    }

    override suspend fun createGroup(name: String, description: String?): Result<Group> = runCatching {
        val request = CreateGroupRequest(name = name, description = description)
        val response = groupApi.createGroup(request)
        val group = response.toDomain()

        groupDao.insert(group.toEntity())

        group
    }

    override suspend fun joinWithCode(inviteCode: String): Result<Group> = runCatching {
        val request = JoinGroupRequest(inviteCode = inviteCode)
        val response = groupApi.joinWithCode(request)
        val group = response.group.toDomain()

        groupDao.insert(group.toEntity())

        group
    }

    // ... other implementations
}
```

---

## 4. Local Database

### 4.1 Room Entities

```kotlin
// data/local/entity/GroupEntity.kt
@Entity(tableName = "groups")
data class GroupEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String?,
    val ownerId: String,
    val ownerName: String?,
    val memberCount: Int,
    val deviceCount: Int,
    val myRole: String?,
    val joinedAt: Long?,
    val createdAt: Long,
    val updatedAt: Long,
    val lastSyncedAt: Long = System.currentTimeMillis()
)

// data/local/entity/GroupMembershipEntity.kt
@Entity(
    tableName = "group_memberships",
    foreignKeys = [
        ForeignKey(
            entity = GroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("groupId")]
)
data class GroupMembershipEntity(
    @PrimaryKey val id: String,
    val groupId: String,
    val userId: String,
    val userDisplayName: String,
    val userEmail: String?,
    val role: String,
    val deviceCount: Int,
    val joinedAt: Long,
    val invitedBy: String?
)

// data/local/entity/PendingInvitationEntity.kt
@Entity(tableName = "pending_invitations")
data class PendingInvitationEntity(
    @PrimaryKey val id: String,
    val groupId: String,
    val groupName: String,
    val groupDescription: String?,
    val invitedBy: String,
    val invitedByName: String,
    val memberCount: Int,
    val expiresAt: Long?,
    val createdAt: Long
)
```

### 4.2 DAOs

```kotlin
// data/local/dao/GroupDao.kt
@Dao
interface GroupDao {
    @Query("SELECT * FROM groups ORDER BY name ASC")
    fun observeAll(): Flow<List<GroupEntity>>

    @Query("SELECT * FROM groups WHERE id = :groupId")
    fun observeById(groupId: String): Flow<GroupEntity?>

    @Query("SELECT * FROM groups WHERE id = :groupId")
    suspend fun getById(groupId: String): GroupEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(group: GroupEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(groups: List<GroupEntity>)

    @Query("DELETE FROM groups WHERE id = :groupId")
    suspend fun deleteById(groupId: String)

    @Query("DELETE FROM groups")
    suspend fun deleteAll()

    @Query("UPDATE groups SET myRole = null, joinedAt = null WHERE id = :groupId")
    suspend fun markAsLeft(groupId: String)
}

// data/local/dao/GroupMembershipDao.kt
@Dao
interface GroupMembershipDao {
    @Query("SELECT * FROM group_memberships WHERE groupId = :groupId ORDER BY role ASC, userDisplayName ASC")
    fun observeByGroupId(groupId: String): Flow<List<GroupMembershipEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(memberships: List<GroupMembershipEntity>)

    @Query("DELETE FROM group_memberships WHERE groupId = :groupId")
    suspend fun deleteByGroupId(groupId: String)

    @Query("DELETE FROM group_memberships WHERE groupId = :groupId AND userId = :userId")
    suspend fun deleteByGroupAndUser(groupId: String, userId: String)
}

// data/local/dao/PendingInvitationDao.kt
@Dao
interface PendingInvitationDao {
    @Query("SELECT * FROM pending_invitations ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<PendingInvitationEntity>>

    @Query("SELECT COUNT(*) FROM pending_invitations")
    fun observeCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(invitations: List<PendingInvitationEntity>)

    @Query("DELETE FROM pending_invitations WHERE id = :inviteId")
    suspend fun deleteById(inviteId: String)

    @Query("DELETE FROM pending_invitations")
    suspend fun deleteAll()
}
```

---

## 5. Network Layer

### 5.1 API Interface

```kotlin
// data/remote/api/GroupApi.kt
interface GroupApi {
    // Groups
    suspend fun getMyGroups(): GroupListResponse
    suspend fun getGroup(groupId: String): GroupResponse
    suspend fun createGroup(request: CreateGroupRequest): GroupResponse
    suspend fun updateGroup(groupId: String, request: UpdateGroupRequest): GroupResponse
    suspend fun deleteGroup(groupId: String)
    suspend fun leaveGroup(groupId: String)

    // Members
    suspend fun getGroupMembers(groupId: String): MemberListResponse
    suspend fun updateMemberRole(groupId: String, userId: String, request: UpdateRoleRequest): MemberResponse
    suspend fun removeMember(groupId: String, userId: String)
    suspend fun transferOwnership(groupId: String, request: TransferOwnershipRequest): GroupResponse

    // Invites (admin)
    suspend fun createInvite(groupId: String, request: CreateInviteRequest): InviteResponse
    suspend fun getGroupInvites(groupId: String): InviteListResponse
    suspend fun revokeInvite(groupId: String, inviteId: String)

    // Invites (user)
    suspend fun getPendingInvitations(): PendingInvitationListResponse
    suspend fun joinWithCode(request: JoinGroupRequest): JoinGroupResponse
    suspend fun acceptInvitation(inviteId: String): JoinGroupResponse
    suspend fun declineInvitation(inviteId: String)

    // Group devices
    suspend fun getGroupDevices(groupId: String): DeviceListResponse
}
```

### 5.2 Ktor Implementation

```kotlin
// data/remote/api/GroupApiImpl.kt
class GroupApiImpl @Inject constructor(
    private val client: HttpClient,
    private val baseUrl: String
) : GroupApi {

    override suspend fun getMyGroups(): GroupListResponse {
        return client.get("$baseUrl/api/v1/groups").body()
    }

    override suspend fun createGroup(request: CreateGroupRequest): GroupResponse {
        return client.post("$baseUrl/api/v1/groups") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    override suspend fun joinWithCode(request: JoinGroupRequest): JoinGroupResponse {
        return client.post("$baseUrl/api/v1/groups/join") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    override suspend fun getGroupMembers(groupId: String): MemberListResponse {
        return client.get("$baseUrl/api/v1/groups/$groupId/members").body()
    }

    override suspend fun createInvite(groupId: String, request: CreateInviteRequest): InviteResponse {
        return client.post("$baseUrl/api/v1/groups/$groupId/invites") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    // ... other implementations
}
```

### 5.3 DTOs

```kotlin
// data/remote/dto/GroupDto.kt
@Serializable
data class GroupDto(
    val id: String,
    val name: String,
    val description: String? = null,
    @SerialName("owner_id") val ownerId: String,
    @SerialName("owner_name") val ownerName: String? = null,
    @SerialName("member_count") val memberCount: Int,
    @SerialName("device_count") val deviceCount: Int,
    @SerialName("my_role") val myRole: String? = null,
    @SerialName("joined_at") val joinedAt: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String
)

@Serializable
data class GroupMemberDto(
    val id: String,
    @SerialName("group_id") val groupId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("display_name") val displayName: String,
    val email: String? = null,
    val role: String,
    @SerialName("device_count") val deviceCount: Int,
    @SerialName("joined_at") val joinedAt: String,
    @SerialName("invited_by") val invitedBy: String? = null
)

@Serializable
data class GroupInviteDto(
    val id: String,
    @SerialName("group_id") val groupId: String,
    @SerialName("invite_code") val inviteCode: String,
    @SerialName("invite_type") val inviteType: String,
    @SerialName("max_uses") val maxUses: Int? = null,
    @SerialName("use_count") val useCount: Int,
    @SerialName("expires_at") val expiresAt: String? = null,
    @SerialName("is_active") val isActive: Boolean,
    @SerialName("created_by") val createdBy: String,
    @SerialName("created_at") val createdAt: String
)

// Request DTOs
@Serializable
data class CreateGroupRequest(
    val name: String,
    val description: String? = null
)

@Serializable
data class UpdateGroupRequest(
    val name: String? = null,
    val description: String? = null
)

@Serializable
data class JoinGroupRequest(
    @SerialName("invite_code") val inviteCode: String
)

@Serializable
data class CreateInviteRequest(
    @SerialName("invite_type") val inviteType: String,
    @SerialName("max_uses") val maxUses: Int? = null,
    @SerialName("expires_in_hours") val expiresInHours: Int? = null
)

@Serializable
data class UpdateRoleRequest(
    val role: String
)

@Serializable
data class TransferOwnershipRequest(
    @SerialName("new_owner_id") val newOwnerId: String
)

// Response DTOs
@Serializable
data class GroupListResponse(
    val data: List<GroupDto>,
    val pagination: PaginationDto? = null
)

@Serializable
data class GroupResponse(
    val id: String,
    val name: String,
    val description: String? = null,
    // ... same as GroupDto
)

@Serializable
data class JoinGroupResponse(
    val group: GroupDto,
    val membership: GroupMemberDto
)
```

---

## 6. Use Cases

### 6.1 Group Management Use Cases

```kotlin
// domain/usecase/group/GetMyGroupsUseCase.kt
class GetMyGroupsUseCase @Inject constructor(
    private val groupRepository: GroupRepository
) {
    suspend operator fun invoke(forceRefresh: Boolean = false): Result<List<Group>> {
        return groupRepository.getMyGroups()
    }

    fun observe(): Flow<List<Group>> = groupRepository.observeMyGroups()
}

// domain/usecase/group/CreateGroupUseCase.kt
class CreateGroupUseCase @Inject constructor(
    private val groupRepository: GroupRepository
) {
    suspend operator fun invoke(name: String, description: String?): Result<Group> {
        // Validation
        require(name.isNotBlank()) { "Group name cannot be empty" }
        require(name.length <= 100) { "Group name too long" }

        return groupRepository.createGroup(name, description)
    }
}

// domain/usecase/group/LeaveGroupUseCase.kt
class LeaveGroupUseCase @Inject constructor(
    private val groupRepository: GroupRepository
) {
    suspend operator fun invoke(groupId: String): Result<Unit> {
        // Note: Cannot leave if you're the owner - must transfer ownership first
        return groupRepository.leaveGroup(groupId)
    }
}

// domain/usecase/group/DeleteGroupUseCase.kt
class DeleteGroupUseCase @Inject constructor(
    private val groupRepository: GroupRepository
) {
    suspend operator fun invoke(groupId: String): Result<Unit> {
        // Only owner can delete
        return groupRepository.deleteGroup(groupId)
    }
}
```

### 6.2 Membership Use Cases

```kotlin
// domain/usecase/group/GetGroupMembersUseCase.kt
class GetGroupMembersUseCase @Inject constructor(
    private val groupRepository: GroupRepository
) {
    suspend operator fun invoke(groupId: String): Result<List<GroupMembership>> {
        return groupRepository.getGroupMembers(groupId)
    }

    fun observe(groupId: String): Flow<List<GroupMembership>> {
        return groupRepository.observeGroupMembers(groupId)
    }
}

// domain/usecase/group/UpdateMemberRoleUseCase.kt
class UpdateMemberRoleUseCase @Inject constructor(
    private val groupRepository: GroupRepository
) {
    suspend operator fun invoke(
        groupId: String,
        userId: String,
        newRole: GroupRole
    ): Result<GroupMembership> {
        // Cannot change owner role - use transfer ownership instead
        require(newRole != GroupRole.OWNER) { "Use transfer ownership to change owner" }

        return groupRepository.updateMemberRole(groupId, userId, newRole)
    }
}

// domain/usecase/group/RemoveMemberUseCase.kt
class RemoveMemberUseCase @Inject constructor(
    private val groupRepository: GroupRepository
) {
    suspend operator fun invoke(groupId: String, userId: String): Result<Unit> {
        return groupRepository.removeMember(groupId, userId)
    }
}

// domain/usecase/group/TransferOwnershipUseCase.kt
class TransferOwnershipUseCase @Inject constructor(
    private val groupRepository: GroupRepository
) {
    suspend operator fun invoke(groupId: String, newOwnerId: String): Result<Group> {
        return groupRepository.transferOwnership(groupId, newOwnerId)
    }
}
```

### 6.3 Invitation Use Cases

```kotlin
// domain/usecase/invite/CreateGroupInviteUseCase.kt
class CreateGroupInviteUseCase @Inject constructor(
    private val groupRepository: GroupRepository
) {
    suspend operator fun invoke(
        groupId: String,
        type: InviteType = InviteType.SINGLE_USE,
        maxUses: Int? = null,
        expiresInHours: Int? = 72
    ): Result<GroupInvite> {
        return groupRepository.createInvite(groupId, type, maxUses, expiresInHours)
    }
}

// domain/usecase/invite/JoinGroupWithCodeUseCase.kt
class JoinGroupWithCodeUseCase @Inject constructor(
    private val groupRepository: GroupRepository
) {
    suspend operator fun invoke(inviteCode: String): Result<Group> {
        require(inviteCode.isNotBlank()) { "Invite code cannot be empty" }

        // Normalize code (uppercase, remove spaces/dashes)
        val normalizedCode = inviteCode
            .uppercase()
            .replace(Regex("[\\s-]"), "")

        return groupRepository.joinWithCode(normalizedCode)
    }
}

// domain/usecase/invite/GetPendingInvitationsUseCase.kt
class GetPendingInvitationsUseCase @Inject constructor(
    private val groupRepository: GroupRepository
) {
    suspend operator fun invoke(): Result<List<PendingInvitation>> {
        return groupRepository.getPendingInvitations()
    }

    fun observe(): Flow<List<PendingInvitation>> {
        return groupRepository.observePendingInvitations()
    }

    fun observeCount(): Flow<Int> {
        return observe().map { it.size }
    }
}

// domain/usecase/invite/RespondToInvitationUseCase.kt
class RespondToInvitationUseCase @Inject constructor(
    private val groupRepository: GroupRepository
) {
    suspend fun accept(inviteId: String): Result<Group> {
        return groupRepository.acceptInvitation(inviteId)
    }

    suspend fun decline(inviteId: String): Result<Unit> {
        return groupRepository.declineInvitation(inviteId)
    }
}
```

---

## 7. ViewModels

### 7.1 Group List ViewModel

```kotlin
// presentation/viewmodel/GroupListViewModel.kt
@HiltViewModel
class GroupListViewModel @Inject constructor(
    private val getMyGroupsUseCase: GetMyGroupsUseCase,
    private val getPendingInvitationsUseCase: GetPendingInvitationsUseCase,
    private val createGroupUseCase: CreateGroupUseCase,
    private val joinGroupWithCodeUseCase: JoinGroupWithCodeUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(GroupListUiState())
    val uiState: StateFlow<GroupListUiState> = _uiState.asStateFlow()

    private val _events = Channel<GroupListEvent>(Channel.BUFFERED)
    val events: Flow<GroupListEvent> = _events.receiveAsFlow()

    init {
        loadGroups()
        observeGroups()
        observePendingInvitations()
    }

    private fun observeGroups() {
        viewModelScope.launch {
            getMyGroupsUseCase.observe().collect { groups ->
                _uiState.update { it.copy(groups = groups) }
            }
        }
    }

    private fun observePendingInvitations() {
        viewModelScope.launch {
            getPendingInvitationsUseCase.observeCount().collect { count ->
                _uiState.update { it.copy(pendingInvitationCount = count) }
            }
        }
    }

    fun loadGroups() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            getMyGroupsUseCase(forceRefresh = true)
                .onSuccess { groups ->
                    _uiState.update { it.copy(isLoading = false, groups = groups) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    fun createGroup(name: String, description: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCreating = true) }

            createGroupUseCase(name, description)
                .onSuccess { group ->
                    _uiState.update { it.copy(isCreating = false) }
                    _events.send(GroupListEvent.GroupCreated(group))
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isCreating = false) }
                    _events.send(GroupListEvent.Error(error.message ?: "Failed to create group"))
                }
        }
    }

    fun joinWithCode(inviteCode: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isJoining = true) }

            joinGroupWithCodeUseCase(inviteCode)
                .onSuccess { group ->
                    _uiState.update { it.copy(isJoining = false) }
                    _events.send(GroupListEvent.JoinedGroup(group))
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isJoining = false) }
                    _events.send(GroupListEvent.Error(error.message ?: "Invalid invite code"))
                }
        }
    }
}

data class GroupListUiState(
    val isLoading: Boolean = false,
    val isCreating: Boolean = false,
    val isJoining: Boolean = false,
    val groups: List<Group> = emptyList(),
    val pendingInvitationCount: Int = 0,
    val error: String? = null
) {
    val ownedGroups: List<Group> get() = groups.filter { it.myRole == GroupRole.OWNER }
    val adminGroups: List<Group> get() = groups.filter { it.myRole == GroupRole.ADMIN }
    val memberGroups: List<Group> get() = groups.filter { it.myRole == GroupRole.MEMBER }
}

sealed class GroupListEvent {
    data class GroupCreated(val group: Group) : GroupListEvent()
    data class JoinedGroup(val group: Group) : GroupListEvent()
    data class Error(val message: String) : GroupListEvent()
}
```

### 7.2 Group Detail ViewModel

```kotlin
// presentation/viewmodel/GroupDetailViewModel.kt
@HiltViewModel
class GroupDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getGroupMembersUseCase: GetGroupMembersUseCase,
    private val updateMemberRoleUseCase: UpdateMemberRoleUseCase,
    private val removeMemberUseCase: RemoveMemberUseCase,
    private val leaveGroupUseCase: LeaveGroupUseCase,
    private val deleteGroupUseCase: DeleteGroupUseCase,
    private val transferOwnershipUseCase: TransferOwnershipUseCase,
    private val createGroupInviteUseCase: CreateGroupInviteUseCase,
    private val groupRepository: GroupRepository
) : ViewModel() {

    private val groupId: String = savedStateHandle.get<String>("groupId")!!

    private val _uiState = MutableStateFlow(GroupDetailUiState())
    val uiState: StateFlow<GroupDetailUiState> = _uiState.asStateFlow()

    private val _events = Channel<GroupDetailEvent>(Channel.BUFFERED)
    val events: Flow<GroupDetailEvent> = _events.receiveAsFlow()

    init {
        observeGroup()
        observeMembers()
        loadMembers()
    }

    private fun observeGroup() {
        viewModelScope.launch {
            groupRepository.observeGroup(groupId).collect { group ->
                _uiState.update { it.copy(group = group) }
            }
        }
    }

    private fun observeMembers() {
        viewModelScope.launch {
            getGroupMembersUseCase.observe(groupId).collect { members ->
                _uiState.update { it.copy(members = members) }
            }
        }
    }

    fun loadMembers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMembers = true) }

            getGroupMembersUseCase(groupId)
                .onSuccess { members ->
                    _uiState.update { it.copy(isLoadingMembers = false, members = members) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoadingMembers = false) }
                    _events.send(GroupDetailEvent.Error(error.message ?: "Failed to load members"))
                }
        }
    }

    fun updateMemberRole(userId: String, newRole: GroupRole) {
        viewModelScope.launch {
            updateMemberRoleUseCase(groupId, userId, newRole)
                .onSuccess {
                    _events.send(GroupDetailEvent.MemberRoleUpdated)
                }
                .onFailure { error ->
                    _events.send(GroupDetailEvent.Error(error.message ?: "Failed to update role"))
                }
        }
    }

    fun removeMember(userId: String) {
        viewModelScope.launch {
            removeMemberUseCase(groupId, userId)
                .onSuccess {
                    _events.send(GroupDetailEvent.MemberRemoved)
                }
                .onFailure { error ->
                    _events.send(GroupDetailEvent.Error(error.message ?: "Failed to remove member"))
                }
        }
    }

    fun leaveGroup() {
        viewModelScope.launch {
            leaveGroupUseCase(groupId)
                .onSuccess {
                    _events.send(GroupDetailEvent.LeftGroup)
                }
                .onFailure { error ->
                    _events.send(GroupDetailEvent.Error(error.message ?: "Failed to leave group"))
                }
        }
    }

    fun deleteGroup() {
        viewModelScope.launch {
            deleteGroupUseCase(groupId)
                .onSuccess {
                    _events.send(GroupDetailEvent.GroupDeleted)
                }
                .onFailure { error ->
                    _events.send(GroupDetailEvent.Error(error.message ?: "Failed to delete group"))
                }
        }
    }

    fun transferOwnership(newOwnerId: String) {
        viewModelScope.launch {
            transferOwnershipUseCase(groupId, newOwnerId)
                .onSuccess {
                    _events.send(GroupDetailEvent.OwnershipTransferred)
                }
                .onFailure { error ->
                    _events.send(GroupDetailEvent.Error(error.message ?: "Failed to transfer ownership"))
                }
        }
    }

    fun createInvite(type: InviteType, maxUses: Int?, expiresInHours: Int?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCreatingInvite = true) }

            createGroupInviteUseCase(groupId, type, maxUses, expiresInHours)
                .onSuccess { invite ->
                    _uiState.update { it.copy(isCreatingInvite = false, createdInvite = invite) }
                    _events.send(GroupDetailEvent.InviteCreated(invite))
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isCreatingInvite = false) }
                    _events.send(GroupDetailEvent.Error(error.message ?: "Failed to create invite"))
                }
        }
    }
}

data class GroupDetailUiState(
    val group: Group? = null,
    val members: List<GroupMembership> = emptyList(),
    val isLoadingMembers: Boolean = false,
    val isCreatingInvite: Boolean = false,
    val createdInvite: GroupInvite? = null
) {
    val canManageMembers: Boolean get() = group?.myRole?.canManageMembers() == true
    val canDeleteGroup: Boolean get() = group?.myRole?.canDeleteGroup() == true
    val canLeaveGroup: Boolean get() = group?.myRole != GroupRole.OWNER
    val canInviteMembers: Boolean get() = group?.myRole?.canInviteMembers() == true
}

sealed class GroupDetailEvent {
    object MemberRoleUpdated : GroupDetailEvent()
    object MemberRemoved : GroupDetailEvent()
    object LeftGroup : GroupDetailEvent()
    object GroupDeleted : GroupDetailEvent()
    object OwnershipTransferred : GroupDetailEvent()
    data class InviteCreated(val invite: GroupInvite) : GroupDetailEvent()
    data class Error(val message: String) : GroupDetailEvent()
}
```

### 7.3 Pending Invitations ViewModel

```kotlin
// presentation/viewmodel/PendingInvitationsViewModel.kt
@HiltViewModel
class PendingInvitationsViewModel @Inject constructor(
    private val getPendingInvitationsUseCase: GetPendingInvitationsUseCase,
    private val respondToInvitationUseCase: RespondToInvitationUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(PendingInvitationsUiState())
    val uiState: StateFlow<PendingInvitationsUiState> = _uiState.asStateFlow()

    private val _events = Channel<PendingInvitationsEvent>(Channel.BUFFERED)
    val events: Flow<PendingInvitationsEvent> = _events.receiveAsFlow()

    init {
        observeInvitations()
        loadInvitations()
    }

    private fun observeInvitations() {
        viewModelScope.launch {
            getPendingInvitationsUseCase.observe().collect { invitations ->
                _uiState.update { it.copy(invitations = invitations) }
            }
        }
    }

    fun loadInvitations() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            getPendingInvitationsUseCase()
                .onSuccess { invitations ->
                    _uiState.update { it.copy(isLoading = false, invitations = invitations) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false) }
                    _events.send(PendingInvitationsEvent.Error(error.message ?: "Failed to load invitations"))
                }
        }
    }

    fun acceptInvitation(inviteId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(processingInviteId = inviteId) }

            respondToInvitationUseCase.accept(inviteId)
                .onSuccess { group ->
                    _uiState.update { it.copy(processingInviteId = null) }
                    _events.send(PendingInvitationsEvent.InvitationAccepted(group))
                }
                .onFailure { error ->
                    _uiState.update { it.copy(processingInviteId = null) }
                    _events.send(PendingInvitationsEvent.Error(error.message ?: "Failed to accept"))
                }
        }
    }

    fun declineInvitation(inviteId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(processingInviteId = inviteId) }

            respondToInvitationUseCase.decline(inviteId)
                .onSuccess {
                    _uiState.update { it.copy(processingInviteId = null) }
                    _events.send(PendingInvitationsEvent.InvitationDeclined)
                }
                .onFailure { error ->
                    _uiState.update { it.copy(processingInviteId = null) }
                    _events.send(PendingInvitationsEvent.Error(error.message ?: "Failed to decline"))
                }
        }
    }
}

data class PendingInvitationsUiState(
    val isLoading: Boolean = false,
    val invitations: List<PendingInvitation> = emptyList(),
    val processingInviteId: String? = null
)

sealed class PendingInvitationsEvent {
    data class InvitationAccepted(val group: Group) : PendingInvitationsEvent()
    object InvitationDeclined : PendingInvitationsEvent()
    data class Error(val message: String) : PendingInvitationsEvent()
}
```

---

## 8. Push Notification Handling

### 8.1 Group Notification Types

```kotlin
// domain/model/GroupNotification.kt
sealed class GroupNotification {
    data class InvitationReceived(
        val inviteId: String,
        val groupId: String,
        val groupName: String,
        val invitedByName: String
    ) : GroupNotification()

    data class RoleChanged(
        val groupId: String,
        val groupName: String,
        val newRole: GroupRole,
        val changedByName: String
    ) : GroupNotification()

    data class RemovedFromGroup(
        val groupId: String,
        val groupName: String,
        val removedByName: String
    ) : GroupNotification()

    data class GroupDeleted(
        val groupId: String,
        val groupName: String
    ) : GroupNotification()

    data class NewMemberJoined(
        val groupId: String,
        val groupName: String,
        val memberName: String
    ) : GroupNotification()

    data class MemberLeft(
        val groupId: String,
        val groupName: String,
        val memberName: String
    ) : GroupNotification()
}
```

### 8.2 Notification Handler

```kotlin
// service/GroupNotificationHandler.kt
class GroupNotificationHandler @Inject constructor(
    private val groupDao: GroupDao,
    private val pendingInvitationDao: PendingInvitationDao,
    private val membershipDao: GroupMembershipDao,
    private val notificationManager: NotificationManager
) {
    suspend fun handleNotification(data: Map<String, String>) {
        when (data["type"]) {
            "group_invitation" -> handleInvitation(data)
            "role_changed" -> handleRoleChange(data)
            "removed_from_group" -> handleRemoval(data)
            "group_deleted" -> handleGroupDeletion(data)
            "member_joined" -> handleMemberJoined(data)
            "member_left" -> handleMemberLeft(data)
        }
    }

    private suspend fun handleInvitation(data: Map<String, String>) {
        val invitation = PendingInvitationEntity(
            id = data["invite_id"]!!,
            groupId = data["group_id"]!!,
            groupName = data["group_name"]!!,
            groupDescription = data["group_description"],
            invitedBy = data["invited_by"]!!,
            invitedByName = data["invited_by_name"]!!,
            memberCount = data["member_count"]?.toIntOrNull() ?: 0,
            expiresAt = data["expires_at"]?.let { Instant.parse(it).toEpochMilli() },
            createdAt = System.currentTimeMillis()
        )

        pendingInvitationDao.insert(invitation)

        notificationManager.showGroupInvitation(
            inviteId = invitation.id,
            groupName = invitation.groupName,
            invitedBy = invitation.invitedByName
        )
    }

    private suspend fun handleRoleChange(data: Map<String, String>) {
        val groupId = data["group_id"]!!
        val newRole = data["new_role"]!!

        // Update local cache
        groupDao.updateMyRole(groupId, newRole)

        notificationManager.showRoleChange(
            groupName = data["group_name"]!!,
            newRole = newRole,
            changedBy = data["changed_by_name"]!!
        )
    }

    private suspend fun handleRemoval(data: Map<String, String>) {
        val groupId = data["group_id"]!!

        // Remove from local cache
        groupDao.deleteById(groupId)
        membershipDao.deleteByGroupId(groupId)

        notificationManager.showRemovalFromGroup(
            groupName = data["group_name"]!!,
            removedBy = data["removed_by_name"]!!
        )
    }

    private suspend fun handleGroupDeletion(data: Map<String, String>) {
        val groupId = data["group_id"]!!

        // Remove from local cache
        groupDao.deleteById(groupId)
        membershipDao.deleteByGroupId(groupId)

        notificationManager.showGroupDeleted(
            groupName = data["group_name"]!!
        )
    }

    // ... other handlers
}
```

---

## 9. Invite Sharing

### 9.1 Share Intent Builder

```kotlin
// util/InviteShareUtils.kt
object InviteShareUtils {

    fun createShareIntent(invite: GroupInvite, groupName: String): Intent {
        val shareText = buildShareText(invite, groupName)

        return Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Join $groupName on Phone Manager")
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
    }

    private fun buildShareText(invite: GroupInvite, groupName: String): String {
        return buildString {
            appendLine("Join my group \"$groupName\" on Phone Manager!")
            appendLine()
            appendLine("Use this invite code: ${formatCode(invite.inviteCode)}")
            appendLine()
            appendLine("Or open this link:")
            appendLine("https://phonemanager.app/join/${invite.inviteCode}")

            invite.expiresAt?.let { expiresAt ->
                appendLine()
                appendLine("This invite expires on ${formatDate(expiresAt)}")
            }
        }
    }

    fun formatCode(code: String): String {
        // Format as XXXX-XXXX for readability
        return code.chunked(4).joinToString("-")
    }

    private fun formatDate(instant: Instant): String {
        val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
            .withZone(ZoneId.systemDefault())
        return formatter.format(instant)
    }
}
```

### 9.2 Deep Link Handling

```kotlin
// navigation/DeepLinkHandler.kt
class DeepLinkHandler @Inject constructor(
    private val joinGroupWithCodeUseCase: JoinGroupWithCodeUseCase,
    private val sessionManager: SessionManager
) {
    suspend fun handleDeepLink(uri: Uri): DeepLinkResult {
        return when {
            uri.path?.startsWith("/join/") == true -> {
                val inviteCode = uri.lastPathSegment ?: return DeepLinkResult.Invalid
                handleJoinLink(inviteCode)
            }
            else -> DeepLinkResult.Invalid
        }
    }

    private suspend fun handleJoinLink(inviteCode: String): DeepLinkResult {
        // Check if user is logged in
        if (!sessionManager.isLoggedIn()) {
            return DeepLinkResult.RequiresAuth(
                pendingAction = PendingAction.JoinGroup(inviteCode)
            )
        }

        return joinGroupWithCodeUseCase(inviteCode)
            .map { group -> DeepLinkResult.JoinedGroup(group) }
            .getOrElse { error -> DeepLinkResult.Error(error.message ?: "Invalid invite") }
    }
}

sealed class DeepLinkResult {
    data class JoinedGroup(val group: Group) : DeepLinkResult()
    data class RequiresAuth(val pendingAction: PendingAction) : DeepLinkResult()
    data class Error(val message: String) : DeepLinkResult()
    object Invalid : DeepLinkResult()
}

sealed class PendingAction {
    data class JoinGroup(val inviteCode: String) : PendingAction()
}
```

---

## 10. Permissions & Authorization

### 10.1 Permission Checker

```kotlin
// domain/util/GroupPermissionChecker.kt
class GroupPermissionChecker @Inject constructor(
    private val sessionManager: SessionManager
) {
    fun canManageMembers(group: Group): Boolean {
        return group.myRole?.canManageMembers() == true
    }

    fun canUpdateMemberRole(group: Group, targetMembership: GroupMembership): Boolean {
        val myRole = group.myRole ?: return false

        // Cannot change own role
        if (targetMembership.userId == sessionManager.currentUserId) return false

        // Cannot change owner's role
        if (targetMembership.role == GroupRole.OWNER) return false

        // Must be admin or owner
        return myRole.canManageMembers()
    }

    fun canRemoveMember(group: Group, targetMembership: GroupMembership): Boolean {
        val myRole = group.myRole ?: return false

        // Cannot remove self (use leave instead)
        if (targetMembership.userId == sessionManager.currentUserId) return false

        // Cannot remove owner
        if (targetMembership.role == GroupRole.OWNER) return false

        // Owners can remove anyone, admins can remove members
        return when (myRole) {
            GroupRole.OWNER -> true
            GroupRole.ADMIN -> targetMembership.role == GroupRole.MEMBER
            GroupRole.MEMBER -> false
        }
    }

    fun canInviteMembers(group: Group): Boolean {
        return group.myRole?.canInviteMembers() == true
    }

    fun canDeleteGroup(group: Group): Boolean {
        return group.myRole?.canDeleteGroup() == true
    }

    fun canLeaveGroup(group: Group): Boolean {
        // Owner cannot leave - must transfer ownership first
        return group.myRole != GroupRole.OWNER
    }

    fun canTransferOwnership(group: Group): Boolean {
        return group.myRole?.canTransferOwnership() == true
    }
}
```

---

## 11. Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0.0 | 2025-12-01 | Initial specification |

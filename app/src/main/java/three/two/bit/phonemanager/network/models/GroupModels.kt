package three.two.bit.phonemanager.network.models

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import three.two.bit.phonemanager.domain.model.Group
import three.two.bit.phonemanager.domain.model.GroupInvite
import three.two.bit.phonemanager.domain.model.GroupMembership
import three.two.bit.phonemanager.domain.model.GroupPreview
import three.two.bit.phonemanager.domain.model.GroupRole
import three.two.bit.phonemanager.domain.model.InviteStatus
import three.two.bit.phonemanager.domain.model.InviteValidationResult
import three.two.bit.phonemanager.domain.model.JoinGroupResult

// ============================================================================
// Story E11.8: Group Management API Models
// ============================================================================

/**
 * Story E11.8 Task 2: Request body for creating a new group
 * POST /groups
 */
@Serializable
data class CreateGroupRequest(
    val name: String,
    val description: String? = null,
)

/**
 * Story E11.8 Task 2: Response for create group operation
 */
@Serializable
data class CreateGroupResponse(
    val id: String,
    val name: String,
    val description: String?,
    val ownerId: String,
    val memberCount: Int,
    val createdAt: String,
)

/**
 * Story E11.8 Task 2: Request body for updating a group
 * PUT /groups/{groupId}
 */
@Serializable
data class UpdateGroupRequest(
    val name: String? = null,
    val description: String? = null,
)

/**
 * Story E11.8 Task 2: Response for list user groups
 * GET /groups
 */
@Serializable
data class ListGroupsResponse(
    val groups: List<GroupDto>,
    val count: Int,
)

/**
 * Story E11.8 Task 2: Group DTO in API responses
 */
@Serializable
data class GroupDto(
    val id: String,
    val name: String,
    val description: String? = null,
    val ownerId: String,
    val memberCount: Int,
    val userRole: String, // "owner", "admin", "member"
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

/**
 * Story E11.8 Task 2: Response for get group details
 * GET /groups/{groupId}
 */
@Serializable
data class GroupDetailResponse(
    val group: GroupDto,
)

/**
 * Story E11.8 Task 2: Response for list group members
 * GET /groups/{groupId}/members
 */
@Serializable
data class ListMembersResponse(
    val members: List<GroupMemberDto>,
    val count: Int,
)

/**
 * Story E11.8 Task 2: Group member DTO in API responses
 */
@Serializable
data class GroupMemberDto(
    val userId: String,
    val email: String,
    val displayName: String,
    val role: String, // "owner", "admin", "member"
    val deviceCount: Int,
    val joinedAt: String?,
    val lastActiveAt: String?,
)

/**
 * Story E11.8 Task 2: Request body for updating member role
 * PUT /groups/{groupId}/members/{userId}/role
 */
@Serializable
data class UpdateMemberRoleRequest(
    val role: String, // "admin" or "member"
)

/**
 * Story E11.8 Task 2: Generic success response
 */
@Serializable
data class GroupOperationResponse(
    val success: Boolean,
    val message: String? = null,
)

/**
 * Story E11.8 Task 2: Request body for transferring ownership
 * POST /groups/{groupId}/transfer-ownership
 */
@Serializable
data class TransferOwnershipRequest(
    val newOwnerId: String,
)

// ============================================================================
// Mapping Functions
// ============================================================================

/**
 * Maps GroupDto from API response to Group domain model
 */
fun GroupDto.toDomain(): Group = Group(
    id = id,
    name = name,
    description = description,
    ownerId = ownerId,
    memberCount = memberCount,
    userRole = GroupRole.fromString(userRole),
    createdAt = createdAt?.let { Instant.parse(it) },
    updatedAt = updatedAt?.let { Instant.parse(it) },
)

/**
 * Maps GroupMemberDto from API response to GroupMembership domain model
 */
fun GroupMemberDto.toDomain(groupId: String): GroupMembership = GroupMembership(
    userId = userId,
    groupId = groupId,
    email = email,
    displayName = displayName,
    role = GroupRole.fromString(role),
    deviceCount = deviceCount,
    joinedAt = joinedAt?.let { Instant.parse(it) },
    lastActiveAt = lastActiveAt?.let { Instant.parse(it) },
)

/**
 * Maps CreateGroupResponse to Group domain model
 */
fun CreateGroupResponse.toDomain(): Group = Group(
    id = id,
    name = name,
    description = description,
    ownerId = ownerId,
    memberCount = memberCount,
    userRole = GroupRole.OWNER, // Creator is always owner
    createdAt = Instant.parse(createdAt),
)

// ============================================================================
// Story E11.9: Group Invite API Models
// ============================================================================

/**
 * Story E11.9 Task 2: Request body for creating a new invite
 * POST /groups/{groupId}/invites
 */
@Serializable
data class CreateInviteRequest(
    val expiryDays: Int = 7,
    val maxUses: Int = 1, // -1 for unlimited
)

/**
 * Story E11.9 Task 2: Response for create invite operation
 */
@Serializable
data class CreateInviteResponse(
    val id: String,
    val code: String,
    val groupId: String,
    val groupName: String? = null,
    val createdBy: String,
    val createdAt: String,
    val expiresAt: String,
    val maxUses: Int,
    val usesRemaining: Int,
    val status: String,
)

/**
 * Story E11.9 Task 2: Response for list group invites
 * GET /groups/{groupId}/invites
 */
@Serializable
data class ListInvitesResponse(
    val invites: List<InviteDto>,
    val count: Int,
)

/**
 * Story E11.9 Task 2: Invite DTO in API responses
 */
@Serializable
data class InviteDto(
    val id: String,
    val code: String,
    val groupId: String,
    val groupName: String? = null,
    val createdBy: String,
    val createdAt: String,
    val expiresAt: String,
    val maxUses: Int,
    val usesRemaining: Int,
    val status: String, // "active", "expired", "revoked", "used"
)

/**
 * Story E11.9 Task 2: Request body for validating invite code
 * POST /invites/{code}/validate
 */
@Serializable
data class ValidateInviteRequest(
    val code: String,
)

/**
 * Story E11.9 Task 2: Response for validate invite code
 */
@Serializable
data class ValidateInviteResponse(
    val valid: Boolean,
    val group: GroupPreviewDto? = null,
    val error: String? = null,
)

/**
 * Story E11.9 Task 2: Group preview DTO for invite validation
 */
@Serializable
data class GroupPreviewDto(
    val id: String,
    val name: String,
    val memberCount: Int,
)

/**
 * Story E11.9 Task 2: Response for joining a group with invite code
 * POST /invites/{code}/join
 */
@Serializable
data class JoinGroupResponse(
    val groupId: String,
    val role: String, // "member", "admin", "owner"
    val joinedAt: String,
)

// ============================================================================
// Story E11.9: Invite Mapping Functions
// ============================================================================

/**
 * Maps InviteDto from API response to GroupInvite domain model
 */
fun InviteDto.toDomain(): GroupInvite = GroupInvite(
    id = id,
    groupId = groupId,
    groupName = groupName,
    code = code,
    createdBy = createdBy,
    createdAt = Instant.parse(createdAt),
    expiresAt = Instant.parse(expiresAt),
    maxUses = maxUses,
    usesRemaining = usesRemaining,
    status = InviteStatus.fromString(status),
)

/**
 * Maps CreateInviteResponse to GroupInvite domain model
 */
fun CreateInviteResponse.toDomain(): GroupInvite = GroupInvite(
    id = id,
    groupId = groupId,
    groupName = groupName,
    code = code,
    createdBy = createdBy,
    createdAt = Instant.parse(createdAt),
    expiresAt = Instant.parse(expiresAt),
    maxUses = maxUses,
    usesRemaining = usesRemaining,
    status = InviteStatus.fromString(status),
)

/**
 * Maps GroupPreviewDto to GroupPreview domain model
 */
fun GroupPreviewDto.toDomain(): GroupPreview = GroupPreview(
    id = id,
    name = name,
    memberCount = memberCount,
)

/**
 * Maps ValidateInviteResponse to InviteValidationResult domain model
 */
fun ValidateInviteResponse.toDomain(): InviteValidationResult = InviteValidationResult(
    valid = valid,
    group = group?.toDomain(),
    error = error,
)

/**
 * Maps JoinGroupResponse to JoinGroupResult domain model
 */
fun JoinGroupResponse.toDomain(): JoinGroupResult = JoinGroupResult(
    groupId = groupId,
    role = GroupRole.fromString(role),
    joinedAt = Instant.parse(joinedAt),
)

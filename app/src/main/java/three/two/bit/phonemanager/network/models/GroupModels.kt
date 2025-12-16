package three.two.bit.phonemanager.network.models

import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.serialization.SerialName
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
 *
 * Backend returns snake_case fields with some optional values.
 */
@Serializable
data class CreateGroupResponse(
    val id: String,
    val name: String,
    val slug: String? = null,
    val description: String? = null,
    @SerialName("icon_emoji") val iconEmoji: String? = null,
    @SerialName("owner_id") val ownerId: String? = null,
    @SerialName("member_count") val memberCount: Int = 1,
    @SerialName("device_count") val deviceCount: Int = 0,
    @SerialName("your_role") val userRole: String = "owner",
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
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
 *
 * Backend returns snake_case: { "data": [...], "count": N }
 */
@Serializable
data class ListGroupsResponse(
    @SerialName("data") val groups: List<GroupDto>,
    val count: Int,
)

/**
 * Story E11.8 Task 2: Group DTO in API responses
 *
 * Backend GroupSummary uses snake_case field names.
 */
@Serializable
data class GroupDto(
    val id: String,
    val name: String,
    val slug: String? = null,
    val description: String? = null,
    @SerialName("icon_emoji") val iconEmoji: String? = null,
    @SerialName("owner_id") val ownerId: String? = null,
    @SerialName("member_count") val memberCount: Int = 0,
    @SerialName("device_count") val deviceCount: Int = 0,
    @SerialName("your_role") val userRole: String = "member", // "owner", "admin", "member"
    @SerialName("joined_at") val joinedAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

/**
 * Story E11.8 Task 2: Response for get group details
 * GET /groups/{groupId}
 *
 * Backend returns the group object directly at the root level,
 * so this is a type alias for GroupDto.
 */
typealias GroupDetailResponse = GroupDto

/**
 * Story E11.8 Task 2: Response for list group members
 * GET /groups/{groupId}/members
 *
 * Backend returns snake_case: { "data": [...], "pagination": {...} }
 */
@Serializable
data class ListMembersResponse(
    @SerialName("data") val members: List<GroupMemberDto>,
    val pagination: PaginationDto? = null,
)

/**
 * Pagination info from backend responses
 */
@Serializable
data class PaginationDto(
    val page: Int = 1,
    @SerialName("page_size") val pageSize: Int = 20,
    @SerialName("total_count") val totalCount: Int = 0,
    @SerialName("total_pages") val totalPages: Int = 0,
)

/**
 * Story E11.8 Task 2: Group member DTO in API responses
 *
 * Backend MemberResponse has nested user object.
 */
@Serializable
data class GroupMemberDto(
    val id: String,
    val user: UserPublicDto,
    val role: String, // "owner", "admin", "member"
    @SerialName("joined_at") val joinedAt: String? = null,
    @SerialName("invited_by") val invitedBy: String? = null,
    val devices: List<MemberDeviceDto>? = null,
)

/**
 * Public user info (no sensitive data)
 */
@Serializable
data class UserPublicDto(
    val id: String,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
)

/**
 * Device info for member listing
 */
@Serializable
data class MemberDeviceDto(
    val id: String? = null,
    @SerialName("device_id") val deviceId: String,
    val name: String? = null,
    @SerialName("is_online") val isOnline: Boolean = false,
    @SerialName("last_location") val lastLocation: MemberDeviceLocationDto? = null,
)

/**
 * Last location info for device
 */
@Serializable
data class MemberDeviceLocationDto(
    val latitude: Double,
    val longitude: Double,
    val timestamp: String,
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
    ownerId = ownerId ?: "",
    memberCount = memberCount,
    userRole = GroupRole.fromString(userRole),
    createdAt = (createdAt ?: joinedAt)?.let { Instant.parse(it) },
    updatedAt = updatedAt?.let { Instant.parse(it) },
)

/**
 * Maps GroupMemberDto from API response to GroupMembership domain model
 */
fun GroupMemberDto.toDomain(groupId: String): GroupMembership = GroupMembership(
    userId = user.id,
    groupId = groupId,
    email = "", // Not provided by backend in public user info
    displayName = user.displayName ?: "Unknown",
    role = GroupRole.fromString(role),
    deviceCount = devices?.size ?: 0,
    joinedAt = joinedAt?.let { Instant.parse(it) },
    lastActiveAt = null, // Not provided by backend
)

/**
 * Maps CreateGroupResponse to Group domain model
 */
fun CreateGroupResponse.toDomain(): Group = Group(
    id = id,
    name = name,
    description = description,
    ownerId = ownerId ?: "",
    memberCount = memberCount,
    userRole = GroupRole.fromString(userRole),
    createdAt = createdAt?.let { Instant.parse(it) },
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
 * Uses snake_case field names matching backend response.
 */
@Serializable
data class CreateInviteResponse(
    val id: String,
    val code: String,
    @SerialName("group_id") val groupId: String? = null,
    @SerialName("preset_role") val presetRole: String? = null,
    @SerialName("max_uses") val maxUses: Int,
    @SerialName("current_uses") val currentUses: Int,
    @SerialName("expires_at") val expiresAt: String,
    @SerialName("created_by") val createdBy: String,
    @SerialName("created_at") val createdAt: String,
)

/**
 * Story E11.9 Task 2: Response for list group invites
 * GET /groups/{groupId}/invites
 *
 * Backend returns { data: [...] } with InviteSummary format.
 */
@Serializable
data class ListInvitesResponse(
    val data: List<InviteSummaryDto>,
) {
    val count: Int get() = data.size
}

/**
 * Invite summary from backend list response.
 * Uses snake_case field names matching backend InviteSummary.
 */
@Serializable
data class InviteSummaryDto(
    val id: String,
    val code: String,
    @SerialName("preset_role") val presetRole: String,
    @SerialName("max_uses") val maxUses: Int,
    @SerialName("current_uses") val currentUses: Int,
    @SerialName("expires_at") val expiresAt: String,
    @SerialName("created_by") val createdBy: InviteCreatorDto,
    @SerialName("created_at") val createdAt: String,
)

/**
 * Creator info in invite summary.
 */
@Serializable
data class InviteCreatorDto(
    val id: String,
    @SerialName("display_name") val displayName: String? = null,
)

/**
 * Story E11.9 Task 2: Invite DTO in API responses
 * Used for create invite response and other contexts.
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
 * Note: id and memberCount are optional as the server may not always return them
 */
@Serializable
data class GroupPreviewDto(
    val id: String? = null,
    val name: String,
    @SerialName("member_count")
    val memberCount: Int? = null,
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
 * Maps InviteSummaryDto from API list response to GroupInvite domain model.
 * Backend list endpoint returns InviteSummary format without groupId/groupName.
 */
fun InviteSummaryDto.toDomain(groupId: String): GroupInvite {
    // Calculate uses remaining from maxUses and currentUses
    // maxUses of -1 means unlimited
    val remaining = if (maxUses == -1) Int.MAX_VALUE else (maxUses - currentUses).coerceAtLeast(0)

    // Determine status based on expiry and uses
    val expiresInstant = Instant.parse(expiresAt)
    val now = Clock.System.now()
    val status = when {
        remaining <= 0 && maxUses != -1 -> InviteStatus.USED
        expiresInstant <= now -> InviteStatus.EXPIRED
        else -> InviteStatus.ACTIVE
    }

    return GroupInvite(
        id = id,
        groupId = groupId,
        groupName = null, // Not provided in summary format
        code = code,
        createdBy = createdBy.id,
        createdAt = Instant.parse(createdAt),
        expiresAt = expiresInstant,
        maxUses = maxUses,
        usesRemaining = remaining,
        status = status,
    )
}

/**
 * Maps CreateInviteResponse to GroupInvite domain model
 */
fun CreateInviteResponse.toDomain(groupIdFallback: String = ""): GroupInvite {
    val usesRemaining = if (maxUses < 0) -1 else maxUses - currentUses
    val expiresAtInstant = Instant.parse(expiresAt)
    val isExpired = expiresAtInstant < Clock.System.now()
    val status = when {
        isExpired -> InviteStatus.EXPIRED
        maxUses >= 0 && usesRemaining <= 0 -> InviteStatus.USED
        else -> InviteStatus.ACTIVE
    }
    return GroupInvite(
        id = id,
        groupId = groupId ?: groupIdFallback,
        groupName = null,
        code = code,
        createdBy = createdBy,
        createdAt = Instant.parse(createdAt),
        expiresAt = expiresAtInstant,
        maxUses = maxUses,
        usesRemaining = usesRemaining,
        status = status,
    )
}

/**
 * Maps GroupPreviewDto to GroupPreview domain model
 * Provides default values for optional fields that may not be returned by the server
 */
fun GroupPreviewDto.toDomain(): GroupPreview = GroupPreview(
    id = id ?: "",
    name = name,
    memberCount = memberCount ?: 0,
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

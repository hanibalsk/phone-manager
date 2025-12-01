package three.two.bit.phonemanager.network.models

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import three.two.bit.phonemanager.domain.model.Group
import three.two.bit.phonemanager.domain.model.GroupMembership
import three.two.bit.phonemanager.domain.model.GroupRole

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

package three.two.bit.phonemanager.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import three.two.bit.phonemanager.domain.model.Group
import three.two.bit.phonemanager.domain.model.GroupInvite
import three.two.bit.phonemanager.domain.model.GroupMembership
import three.two.bit.phonemanager.domain.model.InviteValidationResult
import three.two.bit.phonemanager.domain.model.JoinGroupResult
import three.two.bit.phonemanager.network.models.CreateGroupRequest
import three.two.bit.phonemanager.network.models.CreateGroupResponse
import three.two.bit.phonemanager.network.models.CreateInviteRequest
import three.two.bit.phonemanager.network.models.CreateInviteResponse
import three.two.bit.phonemanager.network.models.GroupDetailResponse
import three.two.bit.phonemanager.network.models.GroupOperationResponse
import three.two.bit.phonemanager.network.models.JoinGroupRequest
import three.two.bit.phonemanager.network.models.JoinGroupResponse
import three.two.bit.phonemanager.network.models.ListGroupsResponse
import three.two.bit.phonemanager.network.models.ListInvitesResponse
import three.two.bit.phonemanager.network.models.ListMembersResponse
import three.two.bit.phonemanager.network.models.TransferOwnershipRequest
import three.two.bit.phonemanager.network.models.UpdateGroupRequest
import three.two.bit.phonemanager.network.models.UpdateMemberRoleRequest
import three.two.bit.phonemanager.network.models.ValidateInviteResponse
import three.two.bit.phonemanager.network.models.toDomain
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Story E11.8: GroupApiService - HTTP client for group management operations
 *
 * Provides methods to create, read, update, delete groups and manage members.
 */
interface GroupApiService {

    /**
     * AC E11.8.2: Create a new group
     * POST /groups
     *
     * @param name Group name (required)
     * @param description Group description (optional)
     * @param accessToken JWT access token
     * @return Result with created group on success
     */
    suspend fun createGroup(name: String, description: String?, accessToken: String): Result<Group>

    /**
     * AC E11.8.1: Get all groups the user belongs to
     * GET /groups
     *
     * @param accessToken JWT access token
     * @return Result with list of groups on success
     */
    suspend fun getUserGroups(accessToken: String): Result<List<Group>>

    /**
     * AC E11.8.3: Get group details
     * GET /groups/{groupId}
     *
     * @param groupId The group's ID
     * @param accessToken JWT access token
     * @return Result with group details on success
     */
    suspend fun getGroupDetails(groupId: String, accessToken: String): Result<Group>

    /**
     * AC E11.8.8: Update group information
     * PUT /groups/{groupId}
     *
     * @param groupId The group's ID
     * @param name New group name (optional)
     * @param description New description (optional)
     * @param accessToken JWT access token
     * @return Result with success status
     */
    suspend fun updateGroup(groupId: String, name: String?, description: String?, accessToken: String): Result<Unit>

    /**
     * AC E11.8.7: Delete a group
     * DELETE /groups/{groupId}
     *
     * @param groupId The group's ID
     * @param accessToken JWT access token
     * @return Result with success status
     */
    suspend fun deleteGroup(groupId: String, accessToken: String): Result<Unit>

    /**
     * AC E11.8.4: Get group members
     * GET /groups/{groupId}/members
     *
     * @param groupId The group's ID
     * @param accessToken JWT access token
     * @return Result with list of members on success
     */
    suspend fun getGroupMembers(groupId: String, accessToken: String): Result<List<GroupMembership>>

    /**
     * AC E11.8.5: Update member role
     * PUT /groups/{groupId}/members/{userId}/role
     *
     * @param groupId The group's ID
     * @param userId The member's user ID
     * @param role New role ("admin" or "member")
     * @param accessToken JWT access token
     * @return Result with success status
     */
    suspend fun updateMemberRole(groupId: String, userId: String, role: String, accessToken: String): Result<Unit>

    /**
     * AC E11.8.5: Remove member from group
     * DELETE /groups/{groupId}/members/{userId}
     *
     * @param groupId The group's ID
     * @param userId The member's user ID to remove
     * @param accessToken JWT access token
     * @return Result with success status
     */
    suspend fun removeMember(groupId: String, userId: String, accessToken: String): Result<Unit>

    /**
     * AC E11.8.6: Leave a group
     * POST /groups/{groupId}/leave
     *
     * @param groupId The group's ID
     * @param accessToken JWT access token
     * @return Result with success status
     */
    suspend fun leaveGroup(groupId: String, accessToken: String): Result<Unit>

    /**
     * AC E11.8.5: Transfer group ownership
     * POST /groups/{groupId}/transfer-ownership
     *
     * @param groupId The group's ID
     * @param newOwnerId The new owner's user ID
     * @param accessToken JWT access token
     * @return Result with success status
     */
    suspend fun transferOwnership(groupId: String, newOwnerId: String, accessToken: String): Result<Unit>

    // ==========================================================================
    // Story E11.9: Group Invite Endpoints
    // ==========================================================================

    /**
     * AC E11.9.1: Create a new invite for a group
     * POST /groups/{groupId}/invites
     *
     * @param groupId The group's ID
     * @param expiryDays Number of days until the invite expires (default: 7)
     * @param maxUses Maximum number of uses (-1 for unlimited)
     * @param accessToken JWT access token
     * @return Result with created invite on success
     */
    suspend fun createInvite(groupId: String, expiryDays: Int, maxUses: Int, accessToken: String): Result<GroupInvite>

    /**
     * AC E11.9.6: Get all invites for a group
     * GET /groups/{groupId}/invites
     *
     * @param groupId The group's ID
     * @param accessToken JWT access token
     * @return Result with list of invites on success
     */
    suspend fun getGroupInvites(groupId: String, accessToken: String): Result<List<GroupInvite>>

    /**
     * AC E11.9.7: Revoke an invite
     * DELETE /groups/{groupId}/invites/{inviteId}
     *
     * @param groupId The group's ID
     * @param inviteId The invite's ID
     * @param accessToken JWT access token
     * @return Result with success status
     */
    suspend fun revokeInvite(groupId: String, inviteId: String, accessToken: String): Result<Unit>

    /**
     * AC E11.9.4: Validate an invite code
     * POST /invites/{code}/validate
     *
     * @param code The 8-character invite code
     * @return Result with validation result including group preview
     */
    suspend fun validateInviteCode(code: String): Result<InviteValidationResult>

    /**
     * AC E11.9.4: Join a group using an invite code
     * POST /invites/{code}/join
     *
     * @param code The 8-character invite code
     * @param accessToken JWT access token
     * @return Result with join result including role assigned
     */
    suspend fun joinWithInvite(code: String, accessToken: String): Result<JoinGroupResult>
}

@Singleton
class GroupApiServiceImpl @Inject constructor(
    private val httpClient: HttpClient,
    private val apiConfig: ApiConfiguration,
) : GroupApiService {

    /**
     * Story E11.8 Task 2: Create a new group
     * POST /groups
     */
    override suspend fun createGroup(name: String, description: String?, accessToken: String): Result<Group> = try {
        Timber.d("Creating group: name=$name")

        val response: CreateGroupResponse = httpClient.post("${apiConfig.baseUrl}/api/v1/groups") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $accessToken")
            setBody(CreateGroupRequest(name = name, description = description))
        }.body()

        Timber.i("Group created successfully: ${response.id}")
        Result.success(response.toDomain())
    } catch (e: Exception) {
        Timber.e(e, "Failed to create group")
        Result.failure(e)
    }

    /**
     * Story E11.8 Task 2: Get all groups the user belongs to
     * GET /groups
     */
    override suspend fun getUserGroups(accessToken: String): Result<List<Group>> = try {
        Timber.d("Fetching user groups")

        val response: ListGroupsResponse = httpClient.get("${apiConfig.baseUrl}/api/v1/groups") {
            header("Authorization", "Bearer $accessToken")
        }.body()

        val groups = response.groups.map { it.toDomain() }
        Timber.i("Fetched ${groups.size} groups")
        Result.success(groups)
    } catch (e: Exception) {
        Timber.e(e, "Failed to fetch user groups")
        Result.failure(e)
    }

    /**
     * Story E11.8 Task 2: Get group details
     * GET /groups/{groupId}
     */
    override suspend fun getGroupDetails(groupId: String, accessToken: String): Result<Group> = try {
        Timber.d("Fetching group details: groupId=$groupId")

        val response: GroupDetailResponse = httpClient.get("${apiConfig.baseUrl}/api/v1/groups/$groupId") {
            header("Authorization", "Bearer $accessToken")
        }.body()

        Timber.i("Fetched group details: ${response.name}")
        Result.success(response.toDomain())
    } catch (e: Exception) {
        Timber.e(e, "Failed to fetch group details: groupId=$groupId")
        Result.failure(e)
    }

    /**
     * Story E11.8 Task 2: Update group information
     * PUT /groups/{groupId}
     */
    override suspend fun updateGroup(
        groupId: String,
        name: String?,
        description: String?,
        accessToken: String,
    ): Result<Unit> = try {
        Timber.d("Updating group: groupId=$groupId")

        httpClient.put("${apiConfig.baseUrl}/api/v1/groups/$groupId") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $accessToken")
            setBody(UpdateGroupRequest(name = name, description = description))
        }

        Timber.i("Group updated: $groupId")
        Result.success(Unit)
    } catch (e: Exception) {
        Timber.e(e, "Failed to update group: groupId=$groupId")
        Result.failure(e)
    }

    /**
     * Story E11.8 Task 2: Delete a group
     * DELETE /groups/{groupId}
     */
    override suspend fun deleteGroup(groupId: String, accessToken: String): Result<Unit> = try {
        Timber.d("Deleting group: groupId=$groupId")

        httpClient.delete("${apiConfig.baseUrl}/api/v1/groups/$groupId") {
            header("Authorization", "Bearer $accessToken")
        }

        Timber.i("Group deleted: $groupId")
        Result.success(Unit)
    } catch (e: Exception) {
        Timber.e(e, "Failed to delete group: groupId=$groupId")
        Result.failure(e)
    }

    /**
     * Story E11.8 Task 2: Get group members
     * GET /groups/{groupId}/members
     */
    override suspend fun getGroupMembers(groupId: String, accessToken: String): Result<List<GroupMembership>> = try {
        Timber.d("Fetching group members: groupId=$groupId, url=${apiConfig.baseUrl}/api/v1/groups/$groupId/members")

        val response: ListMembersResponse = httpClient.get(
            "${apiConfig.baseUrl}/api/v1/groups/$groupId/members",
        ) {
            header("Authorization", "Bearer $accessToken")
        }.body()

        Timber.d("Members API response: ${response.members.size} members, pagination=${response.pagination}")
        val members = response.members.map { it.toDomain(groupId) }
        Timber.i("Fetched ${members.size} members for group: $groupId")
        Result.success(members)
    } catch (e: Exception) {
        Timber.e(e, "Failed to fetch group members: groupId=$groupId")
        Result.failure(e)
    }

    /**
     * Story E11.8 Task 2: Update member role
     * PUT /groups/{groupId}/members/{userId}/role
     */
    override suspend fun updateMemberRole(
        groupId: String,
        userId: String,
        role: String,
        accessToken: String,
    ): Result<Unit> = try {
        Timber.d("Updating member role: groupId=$groupId, userId=$userId, role=$role")

        val response: GroupOperationResponse = httpClient.put(
            "${apiConfig.baseUrl}/api/v1/groups/$groupId/members/$userId/role",
        ) {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $accessToken")
            setBody(UpdateMemberRoleRequest(role = role))
        }.body()

        if (response.success) {
            Timber.i("Member role updated: $userId in group $groupId")
            Result.success(Unit)
        } else {
            Timber.e("Failed to update member role: ${response.message}")
            Result.failure(Exception(response.message ?: "Failed to update member role"))
        }
    } catch (e: Exception) {
        Timber.e(e, "Failed to update member role: groupId=$groupId, userId=$userId")
        Result.failure(e)
    }

    /**
     * Story E11.8 Task 2: Remove member from group
     * DELETE /groups/{groupId}/members/{userId}
     */
    override suspend fun removeMember(groupId: String, userId: String, accessToken: String): Result<Unit> = try {
        Timber.d("Removing member: groupId=$groupId, userId=$userId")

        httpClient.delete("${apiConfig.baseUrl}/api/v1/groups/$groupId/members/$userId") {
            header("Authorization", "Bearer $accessToken")
        }

        Timber.i("Member removed: $userId from group $groupId")
        Result.success(Unit)
    } catch (e: Exception) {
        Timber.e(e, "Failed to remove member: groupId=$groupId, userId=$userId")
        Result.failure(e)
    }

    /**
     * Story E11.8 Task 2: Leave a group
     * POST /groups/{groupId}/leave
     */
    override suspend fun leaveGroup(groupId: String, accessToken: String): Result<Unit> = try {
        Timber.d("Leaving group: groupId=$groupId")

        httpClient.post("${apiConfig.baseUrl}/api/v1/groups/$groupId/leave") {
            header("Authorization", "Bearer $accessToken")
        }

        Timber.i("Left group: $groupId")
        Result.success(Unit)
    } catch (e: Exception) {
        Timber.e(e, "Failed to leave group: groupId=$groupId")
        Result.failure(e)
    }

    /**
     * Story E11.8 Task 2: Transfer group ownership
     * POST /groups/{groupId}/transfer-ownership
     */
    override suspend fun transferOwnership(groupId: String, newOwnerId: String, accessToken: String): Result<Unit> =
        try {
            Timber.d("Transferring ownership: groupId=$groupId to newOwnerId=$newOwnerId")

            val response: GroupOperationResponse = httpClient.post(
                "${apiConfig.baseUrl}/api/v1/groups/$groupId/transfer-ownership",
            ) {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer $accessToken")
                setBody(TransferOwnershipRequest(newOwnerId = newOwnerId))
            }.body()

            if (response.success) {
                Timber.i("Ownership transferred: $groupId to $newOwnerId")
                Result.success(Unit)
            } else {
                Timber.e("Failed to transfer ownership: ${response.message}")
                Result.failure(Exception(response.message ?: "Failed to transfer ownership"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to transfer ownership: groupId=$groupId")
            Result.failure(e)
        }

    // ==========================================================================
    // Story E11.9: Group Invite Endpoints Implementation
    // ==========================================================================

    /**
     * Story E11.9 Task 2: Create a new invite for a group
     * POST /groups/{groupId}/invites
     */
    override suspend fun createInvite(
        groupId: String,
        expiryDays: Int,
        maxUses: Int,
        accessToken: String,
    ): Result<GroupInvite> = try {
        Timber.d("Creating invite: groupId=$groupId, expiryDays=$expiryDays, maxUses=$maxUses")

        val response: CreateInviteResponse = httpClient.post(
            "${apiConfig.baseUrl}/api/v1/groups/$groupId/invites",
        ) {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $accessToken")
            setBody(CreateInviteRequest(expiryDays = expiryDays, maxUses = maxUses))
        }.body()

        Timber.i("Invite created: code=${response.code} for group=$groupId")
        Result.success(response.toDomain(groupIdFallback = groupId))
    } catch (e: Exception) {
        Timber.e(e, "Failed to create invite: groupId=$groupId")
        Result.failure(e)
    }

    /**
     * Story E11.9 Task 2: Get all invites for a group
     * GET /groups/{groupId}/invites
     */
    override suspend fun getGroupInvites(groupId: String, accessToken: String): Result<List<GroupInvite>> = try {
        Timber.d("Fetching invites: groupId=$groupId")

        val response: ListInvitesResponse = httpClient.get(
            "${apiConfig.baseUrl}/api/v1/groups/$groupId/invites",
        ) {
            header("Authorization", "Bearer $accessToken")
        }.body()

        val invites = response.data.map { it.toDomain(groupId) }
        Timber.i("Fetched ${invites.size} invites for group=$groupId")
        Result.success(invites)
    } catch (e: Exception) {
        Timber.e(e, "Failed to fetch invites: groupId=$groupId")
        Result.failure(e)
    }

    /**
     * Story E11.9 Task 2: Revoke an invite
     * DELETE /groups/{groupId}/invites/{inviteId}
     */
    override suspend fun revokeInvite(groupId: String, inviteId: String, accessToken: String): Result<Unit> = try {
        Timber.d("Revoking invite: inviteId=$inviteId in group=$groupId")

        httpClient.delete("${apiConfig.baseUrl}/api/v1/groups/$groupId/invites/$inviteId") {
            header("Authorization", "Bearer $accessToken")
        }

        Timber.i("Invite revoked: $inviteId")
        Result.success(Unit)
    } catch (e: Exception) {
        Timber.e(e, "Failed to revoke invite: inviteId=$inviteId")
        Result.failure(e)
    }

    /**
     * Story E11.9 Task 2: Validate an invite code
     * GET /invites/{code}
     */
    override suspend fun validateInviteCode(code: String): Result<InviteValidationResult> = try {
        Timber.d("Validating invite code: $code")

        val response: ValidateInviteResponse = httpClient.get(
            "${apiConfig.baseUrl}/api/v1/invites/$code",
        ).body()

        Timber.i("Invite code validated: valid=${response.isValid}")
        Result.success(response.toDomain())
    } catch (e: Exception) {
        Timber.e(e, "Failed to validate invite code: $code")
        Result.failure(e)
    }

    /**
     * Story E11.9 Task 2: Join a group using an invite code
     * POST /groups/join
     */
    override suspend fun joinWithInvite(code: String, accessToken: String): Result<JoinGroupResult> = try {
        Timber.d("Joining group with invite code: $code")

        val response: JoinGroupResponse = httpClient.post(
            "${apiConfig.baseUrl}/api/v1/groups/join",
        ) {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $accessToken")
            setBody(JoinGroupRequest(code = code))
        }.body()

        Timber.i("Joined group: ${response.group.id} with role=${response.membership.role}")
        Result.success(response.toDomain())
    } catch (e: Exception) {
        Timber.e(e, "Failed to join group with invite code: $code")
        Result.failure(e)
    }
}

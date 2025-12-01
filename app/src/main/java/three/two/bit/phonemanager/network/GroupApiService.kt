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
import three.two.bit.phonemanager.domain.model.GroupMembership
import three.two.bit.phonemanager.network.models.CreateGroupRequest
import three.two.bit.phonemanager.network.models.CreateGroupResponse
import three.two.bit.phonemanager.network.models.GroupDetailResponse
import three.two.bit.phonemanager.network.models.GroupOperationResponse
import three.two.bit.phonemanager.network.models.ListGroupsResponse
import three.two.bit.phonemanager.network.models.ListMembersResponse
import three.two.bit.phonemanager.network.models.TransferOwnershipRequest
import three.two.bit.phonemanager.network.models.UpdateGroupRequest
import three.two.bit.phonemanager.network.models.UpdateMemberRoleRequest
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
    suspend fun createGroup(
        name: String,
        description: String?,
        accessToken: String,
    ): Result<Group>

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
    suspend fun getGroupDetails(
        groupId: String,
        accessToken: String,
    ): Result<Group>

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
    suspend fun updateGroup(
        groupId: String,
        name: String?,
        description: String?,
        accessToken: String,
    ): Result<Unit>

    /**
     * AC E11.8.7: Delete a group
     * DELETE /groups/{groupId}
     *
     * @param groupId The group's ID
     * @param accessToken JWT access token
     * @return Result with success status
     */
    suspend fun deleteGroup(
        groupId: String,
        accessToken: String,
    ): Result<Unit>

    /**
     * AC E11.8.4: Get group members
     * GET /groups/{groupId}/members
     *
     * @param groupId The group's ID
     * @param accessToken JWT access token
     * @return Result with list of members on success
     */
    suspend fun getGroupMembers(
        groupId: String,
        accessToken: String,
    ): Result<List<GroupMembership>>

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
    suspend fun updateMemberRole(
        groupId: String,
        userId: String,
        role: String,
        accessToken: String,
    ): Result<Unit>

    /**
     * AC E11.8.5: Remove member from group
     * DELETE /groups/{groupId}/members/{userId}
     *
     * @param groupId The group's ID
     * @param userId The member's user ID to remove
     * @param accessToken JWT access token
     * @return Result with success status
     */
    suspend fun removeMember(
        groupId: String,
        userId: String,
        accessToken: String,
    ): Result<Unit>

    /**
     * AC E11.8.6: Leave a group
     * POST /groups/{groupId}/leave
     *
     * @param groupId The group's ID
     * @param accessToken JWT access token
     * @return Result with success status
     */
    suspend fun leaveGroup(
        groupId: String,
        accessToken: String,
    ): Result<Unit>

    /**
     * AC E11.8.5: Transfer group ownership
     * POST /groups/{groupId}/transfer-ownership
     *
     * @param groupId The group's ID
     * @param newOwnerId The new owner's user ID
     * @param accessToken JWT access token
     * @return Result with success status
     */
    suspend fun transferOwnership(
        groupId: String,
        newOwnerId: String,
        accessToken: String,
    ): Result<Unit>
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
    override suspend fun createGroup(
        name: String,
        description: String?,
        accessToken: String,
    ): Result<Group> = try {
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
    override suspend fun getGroupDetails(
        groupId: String,
        accessToken: String,
    ): Result<Group> = try {
        Timber.d("Fetching group details: groupId=$groupId")

        val response: GroupDetailResponse = httpClient.get("${apiConfig.baseUrl}/api/v1/groups/$groupId") {
            header("Authorization", "Bearer $accessToken")
        }.body()

        Timber.i("Fetched group details: ${response.group.name}")
        Result.success(response.group.toDomain())
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
    override suspend fun deleteGroup(
        groupId: String,
        accessToken: String,
    ): Result<Unit> = try {
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
    override suspend fun getGroupMembers(
        groupId: String,
        accessToken: String,
    ): Result<List<GroupMembership>> = try {
        Timber.d("Fetching group members: groupId=$groupId")

        val response: ListMembersResponse = httpClient.get(
            "${apiConfig.baseUrl}/api/v1/groups/$groupId/members"
        ) {
            header("Authorization", "Bearer $accessToken")
        }.body()

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
            "${apiConfig.baseUrl}/api/v1/groups/$groupId/members/$userId/role"
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
    override suspend fun removeMember(
        groupId: String,
        userId: String,
        accessToken: String,
    ): Result<Unit> = try {
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
    override suspend fun leaveGroup(
        groupId: String,
        accessToken: String,
    ): Result<Unit> = try {
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
    override suspend fun transferOwnership(
        groupId: String,
        newOwnerId: String,
        accessToken: String,
    ): Result<Unit> = try {
        Timber.d("Transferring ownership: groupId=$groupId to newOwnerId=$newOwnerId")

        val response: GroupOperationResponse = httpClient.post(
            "${apiConfig.baseUrl}/api/v1/groups/$groupId/transfer-ownership"
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
}

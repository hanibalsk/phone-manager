package three.two.bit.phonemanager.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import three.two.bit.phonemanager.domain.model.Group
import three.two.bit.phonemanager.domain.model.GroupMembership
import three.two.bit.phonemanager.network.GroupApiService
import three.two.bit.phonemanager.security.SecureStorage
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Story E11.8: Group Repository
 *
 * Handles group management operations including CRUD, member management, and caching.
 *
 * Dependencies: E10.6 (Device Management), Backend E11.1-E11.7
 */
@Singleton
class GroupRepository @Inject constructor(
    private val groupApiService: GroupApiService,
    private val secureStorage: SecureStorage,
) {
    // Cached groups for offline access and quick display
    private val _cachedGroups = MutableStateFlow<List<Group>>(emptyList())
    val cachedGroups: Flow<List<Group>> = _cachedGroups.asStateFlow()

    /**
     * AC E11.8.2: Create a new group
     *
     * @param name Group name (required, max 50 characters)
     * @param description Optional description
     * @return Result with created Group on success
     */
    suspend fun createGroup(name: String, description: String?): Result<Group> {
        val accessToken = secureStorage.getAccessToken()
            ?: return Result.failure(IllegalStateException("Authentication required"))

        return groupApiService.createGroup(
            name = name,
            description = description,
            accessToken = accessToken
        ).also { result ->
            result.onSuccess { group ->
                Timber.i("Group created: ${group.id}")
                // Update cache with new group
                _cachedGroups.value = _cachedGroups.value + group
            }
        }
    }

    /**
     * AC E11.8.1: Get all groups the user belongs to
     *
     * @return Result with list of groups on success
     */
    suspend fun getUserGroups(): Result<List<Group>> {
        val accessToken = secureStorage.getAccessToken()
            ?: return Result.failure(IllegalStateException("Authentication required"))

        return groupApiService.getUserGroups(accessToken).also { result ->
            result.onSuccess { groups ->
                Timber.i("Loaded ${groups.size} groups")
                // Update cache
                _cachedGroups.value = groups
            }
        }
    }

    /**
     * AC E11.8.3: Get group details
     *
     * @param groupId The group's ID
     * @return Result with group details on success
     */
    suspend fun getGroupDetails(groupId: String): Result<Group> {
        val accessToken = secureStorage.getAccessToken()
            ?: return Result.failure(IllegalStateException("Authentication required"))

        return groupApiService.getGroupDetails(
            groupId = groupId,
            accessToken = accessToken
        ).also { result ->
            result.onSuccess { group ->
                Timber.i("Loaded group details: ${group.name}")
                // Update cached group if exists
                _cachedGroups.value = _cachedGroups.value.map {
                    if (it.id == groupId) group else it
                }
            }
        }
    }

    /**
     * AC E11.8.8: Update group information
     *
     * @param groupId The group's ID
     * @param name New group name (optional)
     * @param description New description (optional)
     * @return Result with success status
     */
    suspend fun updateGroup(
        groupId: String,
        name: String?,
        description: String?,
    ): Result<Unit> {
        val accessToken = secureStorage.getAccessToken()
            ?: return Result.failure(IllegalStateException("Authentication required"))

        return groupApiService.updateGroup(
            groupId = groupId,
            name = name,
            description = description,
            accessToken = accessToken
        ).also { result ->
            result.onSuccess {
                Timber.i("Group updated: $groupId")
                // Update cache with new values
                _cachedGroups.value = _cachedGroups.value.map { group ->
                    if (group.id == groupId) {
                        group.copy(
                            name = name ?: group.name,
                            description = description ?: group.description
                        )
                    } else {
                        group
                    }
                }
            }
        }
    }

    /**
     * AC E11.8.7: Delete a group
     *
     * @param groupId The group's ID
     * @return Result with success status
     */
    suspend fun deleteGroup(groupId: String): Result<Unit> {
        val accessToken = secureStorage.getAccessToken()
            ?: return Result.failure(IllegalStateException("Authentication required"))

        return groupApiService.deleteGroup(
            groupId = groupId,
            accessToken = accessToken
        ).also { result ->
            result.onSuccess {
                Timber.i("Group deleted: $groupId")
                // Remove from cache
                _cachedGroups.value = _cachedGroups.value.filter { it.id != groupId }
            }
        }
    }

    /**
     * AC E11.8.4: Get group members
     *
     * @param groupId The group's ID
     * @return Result with list of members on success
     */
    suspend fun getGroupMembers(groupId: String): Result<List<GroupMembership>> {
        val accessToken = secureStorage.getAccessToken()
            ?: return Result.failure(IllegalStateException("Authentication required"))

        return groupApiService.getGroupMembers(
            groupId = groupId,
            accessToken = accessToken
        ).also { result ->
            result.onSuccess { members ->
                Timber.i("Loaded ${members.size} members for group $groupId")
            }
        }
    }

    /**
     * AC E11.8.5: Update member role
     *
     * @param groupId The group's ID
     * @param userId The member's user ID
     * @param role New role ("admin" or "member")
     * @return Result with success status
     */
    suspend fun updateMemberRole(
        groupId: String,
        userId: String,
        role: String,
    ): Result<Unit> {
        val accessToken = secureStorage.getAccessToken()
            ?: return Result.failure(IllegalStateException("Authentication required"))

        return groupApiService.updateMemberRole(
            groupId = groupId,
            userId = userId,
            role = role,
            accessToken = accessToken
        ).also { result ->
            result.onSuccess {
                Timber.i("Member role updated: $userId to $role in group $groupId")
            }
        }
    }

    /**
     * AC E11.8.5: Remove member from group
     *
     * @param groupId The group's ID
     * @param userId The member's user ID to remove
     * @return Result with success status
     */
    suspend fun removeMember(groupId: String, userId: String): Result<Unit> {
        val accessToken = secureStorage.getAccessToken()
            ?: return Result.failure(IllegalStateException("Authentication required"))

        return groupApiService.removeMember(
            groupId = groupId,
            userId = userId,
            accessToken = accessToken
        ).also { result ->
            result.onSuccess {
                Timber.i("Member removed: $userId from group $groupId")
                // Update member count in cache
                _cachedGroups.value = _cachedGroups.value.map { group ->
                    if (group.id == groupId) {
                        group.copy(memberCount = group.memberCount - 1)
                    } else {
                        group
                    }
                }
            }
        }
    }

    /**
     * AC E11.8.6: Leave a group
     *
     * @param groupId The group's ID
     * @return Result with success status
     */
    suspend fun leaveGroup(groupId: String): Result<Unit> {
        val accessToken = secureStorage.getAccessToken()
            ?: return Result.failure(IllegalStateException("Authentication required"))

        return groupApiService.leaveGroup(
            groupId = groupId,
            accessToken = accessToken
        ).also { result ->
            result.onSuccess {
                Timber.i("Left group: $groupId")
                // Remove from cache
                _cachedGroups.value = _cachedGroups.value.filter { it.id != groupId }
            }
        }
    }

    /**
     * AC E11.8.5: Transfer group ownership
     *
     * @param groupId The group's ID
     * @param newOwnerId The new owner's user ID
     * @return Result with success status
     */
    suspend fun transferOwnership(groupId: String, newOwnerId: String): Result<Unit> {
        val accessToken = secureStorage.getAccessToken()
            ?: return Result.failure(IllegalStateException("Authentication required"))

        return groupApiService.transferOwnership(
            groupId = groupId,
            newOwnerId = newOwnerId,
            accessToken = accessToken
        ).also { result ->
            result.onSuccess {
                Timber.i("Ownership transferred: $groupId to $newOwnerId")
                // Refresh groups to update roles
                getUserGroups()
            }
        }
    }

    /**
     * Get cached group by ID
     *
     * @param groupId The group's ID
     * @return Group from cache or null if not found
     */
    fun getCachedGroup(groupId: String): Group? {
        return _cachedGroups.value.find { it.id == groupId }
    }

    /**
     * Get cached group count
     *
     * @return Number of groups the user belongs to
     */
    fun getGroupCount(): Int = _cachedGroups.value.size

    /**
     * Clear cached groups (e.g., on logout)
     */
    fun clearCache() {
        _cachedGroups.value = emptyList()
        Timber.d("Group cache cleared")
    }
}

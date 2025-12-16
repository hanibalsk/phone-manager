package three.two.bit.phonemanager.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import three.two.bit.phonemanager.domain.model.Group
import three.two.bit.phonemanager.domain.model.GroupInvite
import three.two.bit.phonemanager.domain.model.GroupMembership
import three.two.bit.phonemanager.domain.model.InviteValidationResult
import three.two.bit.phonemanager.domain.model.JoinGroupResult
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
    private val settingsSyncRepository: SettingsSyncRepository,
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
        _cachedInvites.value = emptyList()
        Timber.d("Group and invite cache cleared")
    }

    // ==========================================================================
    // Story E11.9: Group Invite Functions
    // ==========================================================================

    // Cached invites for quick display
    private val _cachedInvites = MutableStateFlow<List<GroupInvite>>(emptyList())
    val cachedInvites: Flow<List<GroupInvite>> = _cachedInvites.asStateFlow()

    /**
     * AC E11.9.1: Create a new invite for a group
     *
     * @param groupId The group's ID
     * @param expiryDays Number of days until the invite expires (default: 7)
     * @param maxUses Maximum number of uses (-1 for unlimited, default: 1)
     * @return Result with created GroupInvite on success
     */
    suspend fun createInvite(
        groupId: String,
        expiryDays: Int = 7,
        maxUses: Int = 1,
    ): Result<GroupInvite> {
        val accessToken = secureStorage.getAccessToken()
            ?: return Result.failure(IllegalStateException("Authentication required"))

        return groupApiService.createInvite(
            groupId = groupId,
            expiryDays = expiryDays,
            maxUses = maxUses,
            accessToken = accessToken
        ).also { result ->
            result.onSuccess { invite ->
                Timber.i("Invite created: ${invite.code} for group $groupId")
                // Add to cache
                _cachedInvites.value = _cachedInvites.value + invite
            }
        }
    }

    /**
     * AC E11.9.6: Get all invites for a group
     *
     * @param groupId The group's ID
     * @return Result with list of invites on success
     */
    suspend fun getGroupInvites(groupId: String): Result<List<GroupInvite>> {
        val accessToken = secureStorage.getAccessToken()
            ?: return Result.failure(IllegalStateException("Authentication required"))

        return groupApiService.getGroupInvites(
            groupId = groupId,
            accessToken = accessToken
        ).also { result ->
            result.onSuccess { invites ->
                Timber.i("Loaded ${invites.size} invites for group $groupId")
                // Update cache for this group
                _cachedInvites.value = _cachedInvites.value
                    .filter { it.groupId != groupId } + invites
            }
        }
    }

    /**
     * AC E11.9.7: Revoke an invite
     *
     * @param groupId The group's ID
     * @param inviteId The invite's ID
     * @return Result with success status
     */
    suspend fun revokeInvite(groupId: String, inviteId: String): Result<Unit> {
        val accessToken = secureStorage.getAccessToken()
            ?: return Result.failure(IllegalStateException("Authentication required"))

        return groupApiService.revokeInvite(
            groupId = groupId,
            inviteId = inviteId,
            accessToken = accessToken
        ).also { result ->
            result.onSuccess {
                Timber.i("Invite revoked: $inviteId")
                // Remove from cache
                _cachedInvites.value = _cachedInvites.value.filter { it.id != inviteId }
            }
        }
    }

    /**
     * AC E11.9.4: Validate an invite code
     *
     * No authentication required for validation (allows preview before sign-in)
     *
     * @param code The 8-character invite code
     * @return Result with validation result including group preview
     */
    suspend fun validateInviteCode(code: String): Result<InviteValidationResult> {
        return groupApiService.validateInviteCode(code).also { result ->
            result.onSuccess { validation ->
                if (validation.valid) {
                    Timber.i("Invite code valid: group=${validation.group?.name}")
                } else {
                    Timber.w("Invite code invalid: ${validation.error}")
                }
            }
        }
    }

    /**
     * AC E11.9.4: Join a group using an invite code
     *
     * @param code The 8-character invite code
     * @return Result with join result including role assigned
     */
    suspend fun joinWithInvite(code: String): Result<JoinGroupResult> {
        val accessToken = secureStorage.getAccessToken()
            ?: return Result.failure(IllegalStateException("Authentication required"))

        return groupApiService.joinWithInvite(
            code = code,
            accessToken = accessToken
        ).also { result ->
            result.onSuccess { joinResult ->
                Timber.i("Joined group: ${joinResult.groupId} with role=${joinResult.role}")
                // Refresh groups to include new group
                getUserGroups()
                // Sync settings from server after joining group
                // This ensures the device adopts group/admin-managed settings
                settingsSyncRepository.syncAllSettings().onSuccess {
                    Timber.i("Settings synced after joining group ${joinResult.groupId}")
                }.onFailure { error ->
                    Timber.w(error, "Failed to sync settings after joining group")
                }
            }
        }
    }

    /**
     * Get cached invites for a specific group
     *
     * @param groupId The group's ID
     * @return List of cached invites for the group
     */
    fun getCachedInvitesForGroup(groupId: String): List<GroupInvite> {
        return _cachedInvites.value.filter { it.groupId == groupId }
    }

    /**
     * Get cached invite by code
     *
     * @param code The 8-character invite code
     * @return Invite from cache or null if not found
     */
    fun getCachedInviteByCode(code: String): GroupInvite? {
        return _cachedInvites.value.find { it.code == code }
    }

    /**
     * Clear invites cache for a specific group
     *
     * @param groupId The group's ID
     */
    fun clearInvitesCache(groupId: String) {
        _cachedInvites.value = _cachedInvites.value.filter { it.groupId != groupId }
        Timber.d("Invites cache cleared for group $groupId")
    }

    /**
     * Quick add current device to a group (for owners/admins)
     *
     * This creates a temporary single-use invite and immediately joins the group,
     * providing a one-click experience for owners to add their current device.
     *
     * @param groupId The group's ID to add the device to
     * @return Result with join result on success
     */
    suspend fun addCurrentDeviceToGroup(groupId: String): Result<JoinGroupResult> {
        Timber.i("Quick adding current device to group: $groupId")

        // Step 1: Create a single-use, short-lived invite
        val inviteResult = createInvite(
            groupId = groupId,
            expiryDays = 1, // Short expiry since we use it immediately
            maxUses = 1     // Single use
        )

        val invite = inviteResult.getOrElse { error ->
            Timber.e(error, "Failed to create invite for quick add")
            return Result.failure(error)
        }

        // Step 2: Join using the invite code
        val joinResult = joinWithInvite(invite.code)

        joinResult.onSuccess {
            Timber.i("Successfully added current device to group $groupId")
        }.onFailure { error ->
            Timber.e(error, "Failed to join group with generated invite")
        }

        return joinResult
    }
}

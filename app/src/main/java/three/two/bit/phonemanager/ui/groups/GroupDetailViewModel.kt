package three.two.bit.phonemanager.ui.groups

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import three.two.bit.phonemanager.data.repository.AuthRepository
import three.two.bit.phonemanager.data.repository.GroupRepository
import three.two.bit.phonemanager.domain.model.Group
import three.two.bit.phonemanager.domain.model.GroupMembership
import three.two.bit.phonemanager.domain.model.GroupRole
import timber.log.Timber
import javax.inject.Inject

/**
 * Story E11.8 Task 5: Group Detail ViewModel
 *
 * Handles group detail operations including:
 * - Viewing group details (AC E11.8.3)
 * - Viewing members (AC E11.8.4)
 * - Role management (AC E11.8.5)
 * - Leave group (AC E11.8.6)
 * - Delete group (AC E11.8.7)
 * - Group settings (AC E11.8.8)
 *
 * Dependencies:
 * - GroupRepository for API calls
 * - AuthRepository for current user info
 */
@HiltViewModel
class GroupDetailViewModel @Inject constructor(
    private val groupRepository: GroupRepository,
    private val authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    // Get groupId from navigation arguments
    private val groupId: String = savedStateHandle.get<String>("groupId") ?: ""

    private val _uiState = MutableStateFlow<GroupDetailUiState>(GroupDetailUiState.Loading)
    val uiState: StateFlow<GroupDetailUiState> = _uiState.asStateFlow()

    private val _members = MutableStateFlow<List<GroupMembership>>(emptyList())
    val members: StateFlow<List<GroupMembership>> = _members.asStateFlow()

    private val _operationResult = MutableStateFlow<GroupOperationResult>(GroupOperationResult.Idle)
    val operationResult: StateFlow<GroupOperationResult> = _operationResult.asStateFlow()

    // Current user ID for permission checks
    private val currentUserId: String?
        get() = authRepository.getCurrentUser()?.userId

    init {
        if (groupId.isNotBlank()) {
            loadGroupDetails()
        } else {
            _uiState.value = GroupDetailUiState.Error(
                message = "No group ID provided",
                errorCode = "invalid_group_id"
            )
        }
    }

    /**
     * AC E11.8.3: Load group details
     */
    fun loadGroupDetails() {
        if (groupId.isBlank()) return

        viewModelScope.launch {
            _uiState.value = GroupDetailUiState.Loading

            val result = groupRepository.getGroupDetails(groupId)

            result.fold(
                onSuccess = { group ->
                    Timber.i("Loaded group: ${group.name}")
                    _uiState.value = GroupDetailUiState.Success(
                        group = group,
                        canManageMembers = group.canManageMembers(),
                        canDelete = group.canDelete(),
                        canLeave = group.canLeave()
                    )
                    // Also load members
                    loadMembers()
                },
                onFailure = { error ->
                    Timber.e(error, "Failed to load group details")
                    _uiState.value = GroupDetailUiState.Error(
                        message = getErrorMessage(error),
                        errorCode = getErrorCode(error)
                    )
                }
            )
        }
    }

    /**
     * AC E11.8.4: Load group members
     */
    fun loadMembers() {
        viewModelScope.launch {
            val result = groupRepository.getGroupMembers(groupId)

            result.fold(
                onSuccess = { memberList ->
                    Timber.i("Loaded ${memberList.size} members")
                    _members.value = memberList
                },
                onFailure = { error ->
                    Timber.e(error, "Failed to load members")
                    // Don't change UI state, just log the error
                }
            )
        }
    }

    /**
     * AC E11.8.8: Update group name
     *
     * @param name New group name
     */
    fun updateGroupName(name: String) {
        if (name.isBlank()) {
            _operationResult.value = GroupOperationResult.Error(
                message = "Group name cannot be empty"
            )
            return
        }

        viewModelScope.launch {
            _operationResult.value = GroupOperationResult.Loading

            val result = groupRepository.updateGroup(
                groupId = groupId,
                name = name.trim(),
                description = null
            )

            result.fold(
                onSuccess = {
                    Timber.i("Group name updated")
                    _operationResult.value = GroupOperationResult.Success(
                        message = "Group name updated"
                    )
                    loadGroupDetails() // Refresh
                },
                onFailure = { error ->
                    Timber.e(error, "Failed to update group name")
                    _operationResult.value = GroupOperationResult.Error(
                        message = getErrorMessage(error)
                    )
                }
            )
        }
    }

    /**
     * AC E11.8.8: Update group description
     *
     * @param description New description
     */
    fun updateGroupDescription(description: String?) {
        viewModelScope.launch {
            _operationResult.value = GroupOperationResult.Loading

            val result = groupRepository.updateGroup(
                groupId = groupId,
                name = null,
                description = description?.trim()
            )

            result.fold(
                onSuccess = {
                    Timber.i("Group description updated")
                    _operationResult.value = GroupOperationResult.Success(
                        message = "Group description updated"
                    )
                    loadGroupDetails() // Refresh
                },
                onFailure = { error ->
                    Timber.e(error, "Failed to update group description")
                    _operationResult.value = GroupOperationResult.Error(
                        message = getErrorMessage(error)
                    )
                }
            )
        }
    }

    /**
     * AC E11.8.7: Delete group (owner only)
     *
     * @return true if deletion was initiated (check operationResult for outcome)
     */
    fun deleteGroup(): Boolean {
        val state = _uiState.value
        if (state !is GroupDetailUiState.Success || !state.canDelete) {
            _operationResult.value = GroupOperationResult.Error(
                message = "You don't have permission to delete this group"
            )
            return false
        }

        viewModelScope.launch {
            _operationResult.value = GroupOperationResult.Loading

            val result = groupRepository.deleteGroup(groupId)

            result.fold(
                onSuccess = {
                    Timber.i("Group deleted: $groupId")
                    _operationResult.value = GroupOperationResult.GroupDeleted
                },
                onFailure = { error ->
                    Timber.e(error, "Failed to delete group")
                    _operationResult.value = GroupOperationResult.Error(
                        message = getErrorMessage(error)
                    )
                }
            )
        }

        return true
    }

    /**
     * AC E11.8.6: Leave group (non-owners only)
     *
     * @return true if leave was initiated (check operationResult for outcome)
     */
    fun leaveGroup(): Boolean {
        val state = _uiState.value
        if (state !is GroupDetailUiState.Success || !state.canLeave) {
            _operationResult.value = GroupOperationResult.Error(
                message = "You cannot leave this group. Transfer ownership first."
            )
            return false
        }

        viewModelScope.launch {
            _operationResult.value = GroupOperationResult.Loading

            val result = groupRepository.leaveGroup(groupId)

            result.fold(
                onSuccess = {
                    Timber.i("Left group: $groupId")
                    _operationResult.value = GroupOperationResult.LeftGroup
                },
                onFailure = { error ->
                    Timber.e(error, "Failed to leave group")
                    _operationResult.value = GroupOperationResult.Error(
                        message = getErrorMessage(error)
                    )
                }
            )
        }

        return true
    }

    /**
     * AC E11.8.5: Update member role
     *
     * @param userId The member's user ID
     * @param newRole New role (admin or member)
     */
    fun updateMemberRole(userId: String, newRole: GroupRole) {
        // Cannot change own role
        if (userId == currentUserId) {
            _operationResult.value = GroupOperationResult.Error(
                message = "You cannot change your own role"
            )
            return
        }

        // Cannot set someone as owner via this method
        if (newRole == GroupRole.OWNER) {
            _operationResult.value = GroupOperationResult.Error(
                message = "Use transfer ownership to make someone an owner"
            )
            return
        }

        viewModelScope.launch {
            _operationResult.value = GroupOperationResult.Loading

            val roleString = when (newRole) {
                GroupRole.ADMIN -> "admin"
                else -> "member"
            }

            val result = groupRepository.updateMemberRole(
                groupId = groupId,
                userId = userId,
                role = roleString
            )

            result.fold(
                onSuccess = {
                    Timber.i("Member role updated: $userId to $roleString")
                    _operationResult.value = GroupOperationResult.Success(
                        message = "Member role updated"
                    )
                    loadMembers() // Refresh member list
                },
                onFailure = { error ->
                    Timber.e(error, "Failed to update member role")
                    _operationResult.value = GroupOperationResult.Error(
                        message = getErrorMessage(error)
                    )
                }
            )
        }
    }

    /**
     * AC E11.8.5: Remove member from group
     *
     * @param userId The member's user ID to remove
     */
    fun removeMember(userId: String) {
        // Cannot remove self
        if (userId == currentUserId) {
            _operationResult.value = GroupOperationResult.Error(
                message = "Use 'Leave Group' to remove yourself"
            )
            return
        }

        viewModelScope.launch {
            _operationResult.value = GroupOperationResult.Loading

            val result = groupRepository.removeMember(
                groupId = groupId,
                userId = userId
            )

            result.fold(
                onSuccess = {
                    Timber.i("Member removed: $userId")
                    _operationResult.value = GroupOperationResult.Success(
                        message = "Member removed from group"
                    )
                    loadMembers() // Refresh member list
                    loadGroupDetails() // Refresh member count
                },
                onFailure = { error ->
                    Timber.e(error, "Failed to remove member")
                    _operationResult.value = GroupOperationResult.Error(
                        message = getErrorMessage(error)
                    )
                }
            )
        }
    }

    /**
     * AC E11.8.5: Transfer group ownership
     *
     * @param newOwnerId The new owner's user ID
     */
    fun transferOwnership(newOwnerId: String) {
        val state = _uiState.value
        if (state !is GroupDetailUiState.Success || !state.group.isOwner()) {
            _operationResult.value = GroupOperationResult.Error(
                message = "Only the owner can transfer ownership"
            )
            return
        }

        if (newOwnerId == currentUserId) {
            _operationResult.value = GroupOperationResult.Error(
                message = "You are already the owner"
            )
            return
        }

        viewModelScope.launch {
            _operationResult.value = GroupOperationResult.Loading

            val result = groupRepository.transferOwnership(
                groupId = groupId,
                newOwnerId = newOwnerId
            )

            result.fold(
                onSuccess = {
                    Timber.i("Ownership transferred to: $newOwnerId")
                    _operationResult.value = GroupOperationResult.Success(
                        message = "Ownership transferred successfully"
                    )
                    loadGroupDetails() // Refresh to show new role
                    loadMembers() // Refresh member roles
                },
                onFailure = { error ->
                    Timber.e(error, "Failed to transfer ownership")
                    _operationResult.value = GroupOperationResult.Error(
                        message = getErrorMessage(error)
                    )
                }
            )
        }
    }

    /**
     * Clear operation result state
     */
    fun clearOperationResult() {
        _operationResult.value = GroupOperationResult.Idle
    }

    /**
     * Quick add current device to this group (for owners/admins).
     *
     * Creates a single-use invite and immediately joins the group,
     * providing a one-click experience for owners to add their current device.
     */
    fun addCurrentDeviceToGroup() {
        val state = _uiState.value
        if (state !is GroupDetailUiState.Success || !state.canManageMembers) {
            _operationResult.value = GroupOperationResult.Error(
                message = "You don't have permission to add devices to this group"
            )
            return
        }

        viewModelScope.launch {
            _operationResult.value = GroupOperationResult.Loading

            val result = groupRepository.addCurrentDeviceToGroup(groupId)

            result.fold(
                onSuccess = { joinResult ->
                    Timber.i("Device added to group: ${joinResult.groupId}")
                    _operationResult.value = GroupOperationResult.DeviceAdded
                    loadGroupDetails() // Refresh member count
                    loadMembers() // Refresh member list
                },
                onFailure = { error ->
                    Timber.e(error, "Failed to add device to group")
                    _operationResult.value = GroupOperationResult.Error(
                        message = getErrorMessage(error)
                    )
                }
            )
        }
    }

    /**
     * Check if current user can manage this member
     */
    fun canManageMember(member: GroupMembership): Boolean {
        val state = _uiState.value
        if (state !is GroupDetailUiState.Success) return false

        // Cannot manage self
        if (member.userId == currentUserId) return false

        // Only admins and owners can manage members
        if (!state.canManageMembers) return false

        // Admins cannot manage other admins or owners
        if (state.group.userRole == GroupRole.ADMIN && member.role != GroupRole.MEMBER) {
            return false
        }

        return true
    }

    /**
     * Convert exception to user-friendly error message
     */
    private fun getErrorMessage(exception: Throwable): String {
        val message = exception.message ?: ""
        return when {
            message.contains("401") || message.contains("unauthorized", ignoreCase = true) ->
                "Session expired. Please sign in again."
            message.contains("403") || message.contains("forbidden", ignoreCase = true) ->
                "You don't have permission for this action."
            message.contains("404") || message.contains("not found", ignoreCase = true) ->
                "Group not found."
            message.contains("network", ignoreCase = true) ||
                message.contains("connection", ignoreCase = true) ->
                "Network error. Please check your connection."
            else -> "Something went wrong. Please try again."
        }
    }

    /**
     * Extract error code from exception
     */
    private fun getErrorCode(exception: Throwable): String? {
        val message = exception.message ?: ""
        return when {
            message.contains("401") -> "unauthorized"
            message.contains("403") -> "forbidden"
            message.contains("404") -> "not_found"
            message.contains("network", ignoreCase = true) -> "network_error"
            else -> null
        }
    }
}

/**
 * Story E11.8: UI State for Group Detail Screen
 */
sealed interface GroupDetailUiState {
    /**
     * Loading group details
     */
    data object Loading : GroupDetailUiState

    /**
     * Successfully loaded group details
     *
     * @property group The group details
     * @property canManageMembers Whether user can manage members
     * @property canDelete Whether user can delete the group
     * @property canLeave Whether user can leave the group
     */
    data class Success(
        val group: Group,
        val canManageMembers: Boolean,
        val canDelete: Boolean,
        val canLeave: Boolean,
    ) : GroupDetailUiState

    /**
     * Error loading group details
     */
    data class Error(
        val message: String,
        val errorCode: String? = null,
    ) : GroupDetailUiState
}

/**
 * Story E11.8: Result state for group operations
 */
sealed interface GroupOperationResult {
    /**
     * No operation in progress
     */
    data object Idle : GroupOperationResult

    /**
     * Operation in progress
     */
    data object Loading : GroupOperationResult

    /**
     * Operation succeeded
     *
     * @property message Success message
     */
    data class Success(val message: String) : GroupOperationResult

    /**
     * Operation failed
     *
     * @property message Error message
     */
    data class Error(val message: String) : GroupOperationResult

    /**
     * Group was deleted - navigate back to list
     */
    data object GroupDeleted : GroupOperationResult

    /**
     * User left the group - navigate back to list
     */
    data object LeftGroup : GroupOperationResult

    /**
     * Device was added to the group
     */
    data object DeviceAdded : GroupOperationResult
}

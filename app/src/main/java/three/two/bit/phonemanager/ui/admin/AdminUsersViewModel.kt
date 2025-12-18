package three.two.bit.phonemanager.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import three.two.bit.phonemanager.data.repository.GroupRepository
import three.two.bit.phonemanager.domain.model.Group
import three.two.bit.phonemanager.domain.model.GroupMembership
import three.two.bit.phonemanager.security.SecureStorage
import timber.log.Timber
import javax.inject.Inject

/**
 * Story E9.3: Admin Users ViewModel
 * Story E9.6: Remove User from Managed List
 *
 * Manages UI state for viewing managed users across admin groups.
 * Shows groups where user is admin/owner and their members.
 *
 * ACs: E9.3.1, E9.3.2, E9.3.3, E9.6.1-E9.6.6
 */
@HiltViewModel
class AdminUsersViewModel @Inject constructor(
    private val groupRepository: GroupRepository,
    private val secureStorage: SecureStorage,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminUsersUiState())
    val uiState: StateFlow<AdminUsersUiState> = _uiState.asStateFlow()

    init {
        loadAdminGroups()
    }

    /**
     * Load groups where user has admin/owner access
     */
    fun loadAdminGroups() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            groupRepository.getUserGroups()
                .onSuccess { groups ->
                    val adminGroups = groups.filter { it.isOwner() || it.isAdmin() }
                    Timber.d("Loaded ${adminGroups.size} admin groups")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            adminGroups = adminGroups,
                            isEmpty = adminGroups.isEmpty(),
                        )
                    }
                }
                .onFailure { error ->
                    Timber.w(error, "Failed to load admin groups")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Failed to load groups",
                        )
                    }
                }
        }
    }

    /**
     * Select a group to view its members
     *
     * AC E9.3.2: Show list of managed devices/users
     */
    fun selectGroup(group: Group) {
        _uiState.update {
            it.copy(
                selectedGroup = group,
                groupMembers = emptyList(),
                isMembersLoading = true,
            )
        }
        loadGroupMembers(group.id)
    }

    /**
     * Clear selected group and go back to group list
     */
    fun clearSelectedGroup() {
        _uiState.update {
            it.copy(
                selectedGroup = null,
                groupMembers = emptyList(),
            )
        }
    }

    /**
     * Load members for a specific group
     *
     * AC E9.3.3: Select user from list navigates to device detail screen
     */
    private fun loadGroupMembers(groupId: String) {
        viewModelScope.launch {
            groupRepository.getGroupMembers(groupId)
                .onSuccess { members ->
                    Timber.d("Loaded ${members.size} members for group $groupId")
                    _uiState.update {
                        it.copy(
                            groupMembers = members,
                            isMembersLoading = false,
                            membersError = null,
                        )
                    }
                }
                .onFailure { error ->
                    Timber.w(error, "Failed to load group members for $groupId")
                    _uiState.update {
                        it.copy(
                            isMembersLoading = false,
                            membersError = error.message ?: "Failed to load members",
                        )
                    }
                }
        }
    }

    /**
     * Refresh the current view
     */
    fun refresh() {
        val selectedGroup = _uiState.value.selectedGroup
        if (selectedGroup != null) {
            _uiState.update { it.copy(isMembersLoading = true, membersError = null) }
            loadGroupMembers(selectedGroup.id)
        } else {
            loadAdminGroups()
        }
    }

    /**
     * Story E9.6: Check if a member is the current user
     *
     * AC E9.6.6: Cannot remove self from list (validation)
     *
     * @param member The member to check
     * @return true if the member is the current user
     */
    fun isCurrentUser(member: GroupMembership): Boolean {
        val currentUserId = secureStorage.getUserId()
        return member.userId == currentUserId
    }

    /**
     * Story E9.6: Show confirmation dialog for removing a user
     *
     * AC E9.6.2: Confirmation dialog before removal
     *
     * @param member The member to remove
     */
    fun showRemoveConfirmation(member: GroupMembership) {
        _uiState.update { it.copy(memberToRemove = member) }
    }

    /**
     * Story E9.6: Cancel the remove confirmation dialog
     */
    fun cancelRemoveConfirmation() {
        _uiState.update { it.copy(memberToRemove = null) }
    }

    /**
     * Story E9.6: Remove a user from the managed group
     *
     * AC E9.6.1: Remove action accessible from users list
     * AC E9.6.3: Backend API to revoke management relationship
     * AC E9.6.4: User removed from list immediately after successful API call
     * AC E9.6.5: Removed user's device continues to function independently
     */
    fun removeUser() {
        val member = _uiState.value.memberToRemove ?: return
        val groupId = _uiState.value.selectedGroup?.id ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isRemoving = true) }

            groupRepository.removeMember(groupId, member.userId)
                .onSuccess {
                    Timber.i("Successfully removed user ${member.userId} from group $groupId")
                    // AC E9.6.4: User removed from list immediately after successful API call
                    _uiState.update {
                        it.copy(
                            isRemoving = false,
                            memberToRemove = null,
                            groupMembers = it.groupMembers.filter { m -> m.userId != member.userId },
                            removeSuccess = member.displayName,
                        )
                    }
                }
                .onFailure { error ->
                    Timber.e(error, "Failed to remove user ${member.userId} from group $groupId")
                    _uiState.update {
                        it.copy(
                            isRemoving = false,
                            memberToRemove = null,
                            removeError = error.message ?: "Failed to remove user",
                        )
                    }
                }
        }
    }

    /**
     * Story E9.6: Clear the remove success message
     */
    fun clearRemoveSuccess() {
        _uiState.update { it.copy(removeSuccess = null) }
    }

    /**
     * Story E9.6: Clear the remove error message
     */
    fun clearRemoveError() {
        _uiState.update { it.copy(removeError = null) }
    }
}

/**
 * UI State for Admin Users screen
 *
 * @property adminGroups Groups where user is admin/owner
 * @property selectedGroup Currently selected group (null = showing group list)
 * @property groupMembers Members in the selected group
 * @property isLoading True when loading groups
 * @property isMembersLoading True when loading group members
 * @property error Error message for group loading
 * @property membersError Error message for members loading
 * @property isEmpty True when no admin groups exist
 * @property memberToRemove Member pending removal confirmation (Story E9.6)
 * @property isRemoving True when removal is in progress (Story E9.6)
 * @property removeSuccess Display name of successfully removed user (Story E9.6)
 * @property removeError Error message for failed removal (Story E9.6)
 */
data class AdminUsersUiState(
    val adminGroups: List<Group> = emptyList(),
    val selectedGroup: Group? = null,
    val groupMembers: List<GroupMembership> = emptyList(),
    val isLoading: Boolean = false,
    val isMembersLoading: Boolean = false,
    val error: String? = null,
    val membersError: String? = null,
    val isEmpty: Boolean = false,
    val memberToRemove: GroupMembership? = null,
    val isRemoving: Boolean = false,
    val removeSuccess: String? = null,
    val removeError: String? = null,
)

package three.two.bit.phonemanager.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import three.two.bit.phonemanager.data.repository.DeviceRepository
import three.two.bit.phonemanager.data.repository.GroupRepository
import three.two.bit.phonemanager.domain.model.Device
import three.two.bit.phonemanager.domain.model.Group
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
    private val deviceRepository: DeviceRepository,
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
     * Load members/devices for a specific group
     *
     * AC E9.3.3: Select user from list navigates to device detail screen
     */
    private fun loadGroupMembers(groupId: String) {
        viewModelScope.launch {
            deviceRepository.getGroupDevices(groupId)
                .onSuccess { devices ->
                    Timber.d("Loaded ${devices.size} devices for group $groupId")
                    _uiState.update {
                        it.copy(
                            groupMembers = devices,
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
     * Story E9.6: Check if a device belongs to the current user
     *
     * AC E9.6.6: Cannot remove self from list (validation)
     *
     * @param device The device to check
     * @return true if the device belongs to the current user
     */
    fun isCurrentUserDevice(device: Device): Boolean {
        val currentUserId = secureStorage.getUserId()
        return device.ownerId != null && device.ownerId == currentUserId
    }

    /**
     * Story E9.6: Show confirmation dialog for removing a user
     *
     * AC E9.6.2: Confirmation dialog before removal
     *
     * @param device The device/user to remove
     */
    fun showRemoveConfirmation(device: Device) {
        _uiState.update { it.copy(deviceToRemove = device) }
    }

    /**
     * Story E9.6: Cancel the remove confirmation dialog
     */
    fun cancelRemoveConfirmation() {
        _uiState.update { it.copy(deviceToRemove = null) }
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
        val device = _uiState.value.deviceToRemove ?: return
        val groupId = _uiState.value.selectedGroup?.id ?: return
        val userId = device.ownerId

        if (userId == null) {
            Timber.w("Cannot remove device ${device.deviceId} - no owner ID")
            _uiState.update {
                it.copy(
                    deviceToRemove = null,
                    removeError = "Cannot remove device: not linked to a user account",
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isRemoving = true) }

            groupRepository.removeMember(groupId, userId)
                .onSuccess {
                    Timber.i("Successfully removed user $userId from group $groupId")
                    // AC E9.6.4: User removed from list immediately after successful API call
                    _uiState.update {
                        it.copy(
                            isRemoving = false,
                            deviceToRemove = null,
                            groupMembers = it.groupMembers.filter { member -> member.ownerId != userId },
                            removeSuccess = device.displayName,
                        )
                    }
                }
                .onFailure { error ->
                    Timber.e(error, "Failed to remove user $userId from group $groupId")
                    _uiState.update {
                        it.copy(
                            isRemoving = false,
                            deviceToRemove = null,
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
 * @property groupMembers Devices/users in the selected group
 * @property isLoading True when loading groups
 * @property isMembersLoading True when loading group members
 * @property error Error message for group loading
 * @property membersError Error message for members loading
 * @property isEmpty True when no admin groups exist
 * @property deviceToRemove Device pending removal confirmation (Story E9.6)
 * @property isRemoving True when removal is in progress (Story E9.6)
 * @property removeSuccess Display name of successfully removed user (Story E9.6)
 * @property removeError Error message for failed removal (Story E9.6)
 */
data class AdminUsersUiState(
    val adminGroups: List<Group> = emptyList(),
    val selectedGroup: Group? = null,
    val groupMembers: List<Device> = emptyList(),
    val isLoading: Boolean = false,
    val isMembersLoading: Boolean = false,
    val error: String? = null,
    val membersError: String? = null,
    val isEmpty: Boolean = false,
    val deviceToRemove: Device? = null,
    val isRemoving: Boolean = false,
    val removeSuccess: String? = null,
    val removeError: String? = null,
)

package three.two.bit.phonemanager.ui.groups

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import three.two.bit.phonemanager.data.repository.GroupRepository
import three.two.bit.phonemanager.domain.model.Group
import three.two.bit.phonemanager.domain.model.GroupInvite
import three.two.bit.phonemanager.domain.model.InviteStatus
import timber.log.Timber
import javax.inject.Inject

/**
 * Story E11.9: InviteViewModel
 *
 * Handles invite creation, management, and sharing for group admins/owners.
 *
 * AC E11.9.1: Create invite code
 * AC E11.9.2: Display invite code and QR
 * AC E11.9.3: Share invite
 * AC E11.9.6: View pending invites
 * AC E11.9.7: Revoke invite
 */
@HiltViewModel
class InviteViewModel @Inject constructor(
    private val groupRepository: GroupRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val groupId: String = savedStateHandle.get<String>("groupId") ?: ""

    private val _uiState = MutableStateFlow<InviteUiState>(InviteUiState.Loading)
    val uiState: StateFlow<InviteUiState> = _uiState.asStateFlow()

    private val _currentInvite = MutableStateFlow<GroupInvite?>(null)
    val currentInvite: StateFlow<GroupInvite?> = _currentInvite.asStateFlow()

    private val _invites = MutableStateFlow<List<GroupInvite>>(emptyList())
    val invites: StateFlow<List<GroupInvite>> = _invites.asStateFlow()

    private val _group = MutableStateFlow<Group?>(null)
    val group: StateFlow<Group?> = _group.asStateFlow()

    private val _operationResult = MutableStateFlow<InviteOperationResult>(InviteOperationResult.Idle)
    val operationResult: StateFlow<InviteOperationResult> = _operationResult.asStateFlow()

    init {
        if (groupId.isNotBlank()) {
            loadGroupAndInvites()
        } else {
            _uiState.value = InviteUiState.Error("invalid_group_id", "Invalid group ID")
        }
    }

    /**
     * Load group details and existing invites
     */
    private fun loadGroupAndInvites() {
        viewModelScope.launch {
            _uiState.value = InviteUiState.Loading

            // Load group details
            val groupResult = groupRepository.getGroupDetails(groupId)
            groupResult.onSuccess { loadedGroup ->
                _group.value = loadedGroup
            }.onFailure { error ->
                Timber.e(error, "Failed to load group details")
                _uiState.value = InviteUiState.Error("load_failed", error.message ?: "Failed to load group")
                return@launch
            }

            // Load existing invites
            loadInvites()
        }
    }

    /**
     * AC E11.9.6: Load pending invites for the group
     */
    fun loadInvites() {
        viewModelScope.launch {
            groupRepository.getGroupInvites(groupId).fold(
                onSuccess = { loadedInvites ->
                    // Filter to only show active (non-expired, non-revoked) invites
                    val activeInvites = loadedInvites.filter {
                        it.status == InviteStatus.ACTIVE && !it.isExpired()
                    }
                    _invites.value = activeInvites
                    _uiState.value = InviteUiState.Success
                },
                onFailure = { error ->
                    Timber.e(error, "Failed to load invites")
                    _uiState.value =
                        InviteUiState.Error("load_invites_failed", error.message ?: "Failed to load invites")
                },
            )
        }
    }

    /**
     * AC E11.9.1: Create a new invite for the group
     *
     * @param expiryDays Number of days until expiry (default: 7)
     * @param maxUses Maximum number of uses (-1 for unlimited, default: 1)
     */
    fun createInvite(expiryDays: Int = 7, maxUses: Int = 1) {
        viewModelScope.launch {
            _operationResult.value = InviteOperationResult.Creating

            groupRepository.createInvite(
                groupId = groupId,
                expiryDays = expiryDays,
                maxUses = maxUses,
            ).fold(
                onSuccess = { invite ->
                    _currentInvite.value = invite
                    _invites.value = listOf(invite) + _invites.value
                    _operationResult.value = InviteOperationResult.InviteCreated(invite)
                    Timber.i("Invite created: ${invite.code}")
                },
                onFailure = { error ->
                    Timber.e(error, "Failed to create invite")
                    _operationResult.value = InviteOperationResult.Error(
                        error.message ?: "Failed to create invite",
                    )
                },
            )
        }
    }

    /**
     * AC E11.9.7: Revoke an existing invite
     *
     * @param inviteId The invite's ID to revoke
     */
    fun revokeInvite(inviteId: String) {
        viewModelScope.launch {
            _operationResult.value = InviteOperationResult.Revoking

            groupRepository.revokeInvite(groupId, inviteId).fold(
                onSuccess = {
                    _invites.value = _invites.value.filter { it.id != inviteId }
                    // Clear current invite if it was the one revoked
                    if (_currentInvite.value?.id == inviteId) {
                        _currentInvite.value = null
                    }
                    _operationResult.value = InviteOperationResult.InviteRevoked
                    Timber.i("Invite revoked: $inviteId")
                },
                onFailure = { error ->
                    Timber.e(error, "Failed to revoke invite")
                    _operationResult.value = InviteOperationResult.Error(
                        error.message ?: "Failed to revoke invite",
                    )
                },
            )
        }
    }

    /**
     * Set the current invite for display (e.g., when selecting from list)
     *
     * @param invite The invite to display
     */
    fun setCurrentInvite(invite: GroupInvite) {
        _currentInvite.value = invite
    }

    /**
     * Clear the current invite
     */
    fun clearCurrentInvite() {
        _currentInvite.value = null
    }

    /**
     * AC E11.9.3: Get share content for an invite
     *
     * @param invite The invite to share
     * @return ShareContent with formatted message and deep link
     */
    fun getShareContent(invite: GroupInvite): ShareContent {
        val groupName = _group.value?.name ?: "a group"
        return ShareContent(
            message = "Join my Phone Manager group '$groupName' with code: ${invite.code}",
            deepLink = invite.getDeepLink(),
            code = invite.code,
            groupName = groupName,
        )
    }

    /**
     * Clear operation result state
     */
    fun clearOperationResult() {
        _operationResult.value = InviteOperationResult.Idle
    }

    /**
     * Refresh all data
     */
    fun refresh() {
        loadGroupAndInvites()
    }
}

/**
 * UI state for invite screens
 */
sealed interface InviteUiState {
    object Loading : InviteUiState
    object Success : InviteUiState
    data class Error(val errorCode: String, val message: String) : InviteUiState
}

/**
 * Result of invite operations
 */
sealed interface InviteOperationResult {
    object Idle : InviteOperationResult
    object Creating : InviteOperationResult
    object Revoking : InviteOperationResult
    data class InviteCreated(val invite: GroupInvite) : InviteOperationResult
    object InviteRevoked : InviteOperationResult
    data class Error(val message: String) : InviteOperationResult
}

/**
 * Content for sharing an invite
 */
data class ShareContent(val message: String, val deepLink: String, val code: String, val groupName: String)

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
import three.two.bit.phonemanager.domain.model.GroupPreview
import three.two.bit.phonemanager.domain.model.GroupRole
import three.two.bit.phonemanager.util.InviteCodeUtils
import timber.log.Timber
import javax.inject.Inject

/**
 * Story E11.9: JoinGroupViewModel
 *
 * Handles joining a group using an invite code, including validation,
 * QR code parsing, and deep link handling.
 *
 * AC E11.9.4: Join with invite code
 * AC E11.9.5: QR code scanning
 * AC E11.9.8: Deep link handling
 */
@HiltViewModel
class JoinGroupViewModel @Inject constructor(
    private val groupRepository: GroupRepository,
    private val authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    // Initial code from deep link or QR scan
    private val initialCode: String? = savedStateHandle.get<String>("code")

    private val _inviteCode = MutableStateFlow("")
    val inviteCode: StateFlow<String> = _inviteCode.asStateFlow()

    private val _groupPreview = MutableStateFlow<GroupPreview?>(null)
    val groupPreview: StateFlow<GroupPreview?> = _groupPreview.asStateFlow()

    private val _uiState = MutableStateFlow<JoinGroupUiState>(JoinGroupUiState.EnterCode)
    val uiState: StateFlow<JoinGroupUiState> = _uiState.asStateFlow()

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _joinResult = MutableStateFlow<JoinResult>(JoinResult.Idle)
    val joinResult: StateFlow<JoinResult> = _joinResult.asStateFlow()

    init {
        // Check authentication status
        _isAuthenticated.value = authRepository.isLoggedIn()

        // If we have an initial code from deep link, set and validate it
        if (!initialCode.isNullOrBlank()) {
            setInviteCode(initialCode)
            validateCode()
        }
    }

    /**
     * Update the invite code (auto-uppercase, max 8 characters)
     *
     * @param code The new invite code
     */
    fun setInviteCode(code: String) {
        // Use shared utility for normalization
        val normalizedCode = InviteCodeUtils.normalizeCode(code)

        _inviteCode.value = normalizedCode

        // Clear previous preview if code changes
        if (normalizedCode.length < 8) {
            _groupPreview.value = null
            if (_uiState.value !is JoinGroupUiState.Error) {
                _uiState.value = JoinGroupUiState.EnterCode
            }
        }
    }

    /**
     * AC E11.9.4, E11.9.5: Validate the invite code and show group preview
     */
    fun validateCode() {
        val code = _inviteCode.value

        if (code.length != 8) {
            _uiState.value = JoinGroupUiState.Error("Code must be 8 characters")
            return
        }

        viewModelScope.launch {
            _uiState.value = JoinGroupUiState.Validating

            groupRepository.validateInviteCode(code).fold(
                onSuccess = { result ->
                    if (result.valid && result.group != null) {
                        _groupPreview.value = result.group
                        _uiState.value = JoinGroupUiState.PreviewReady(result.group)
                        Timber.i("Code validated: group=${result.group.name}")
                    } else {
                        _groupPreview.value = null
                        _uiState.value = JoinGroupUiState.Error(
                            result.error ?: "Invalid or expired invite code"
                        )
                        Timber.w("Code validation failed: ${result.error}")
                    }
                },
                onFailure = { error ->
                    _groupPreview.value = null
                    _uiState.value = JoinGroupUiState.Error(
                        error.message ?: "Failed to validate code"
                    )
                    Timber.e(error, "Code validation error")
                }
            )
        }
    }

    /**
     * AC E11.9.4: Join the group using the validated code
     */
    fun joinGroup() {
        val code = _inviteCode.value

        if (!_isAuthenticated.value) {
            _joinResult.value = JoinResult.AuthenticationRequired
            return
        }

        if (code.length != 8) {
            _joinResult.value = JoinResult.Error("Invalid invite code")
            return
        }

        viewModelScope.launch {
            _joinResult.value = JoinResult.Joining

            groupRepository.joinWithInvite(code).fold(
                onSuccess = { result ->
                    _joinResult.value = JoinResult.Success(result.groupId, result.role)
                    Timber.i("Joined group: ${result.groupId}")
                },
                onFailure = { error ->
                    val errorMessage = when {
                        error.message?.contains("already_member", ignoreCase = true) == true ->
                            "You are already a member of this group"
                        error.message?.contains("expired", ignoreCase = true) == true ->
                            "This invite has expired"
                        error.message?.contains("invalid", ignoreCase = true) == true ->
                            "Invalid invite code"
                        error.message?.contains("used", ignoreCase = true) == true ->
                            "This invite has already been used"
                        else -> error.message ?: "Failed to join group"
                    }
                    _joinResult.value = JoinResult.Error(errorMessage)
                    Timber.e(error, "Failed to join group")
                }
            )
        }
    }

    /**
     * AC E11.9.5: Handle QR code scan result
     * Parses the scanned content to extract invite code
     *
     * @param scannedContent The raw content from QR scan (could be deep link or plain code)
     */
    fun handleQrCodeScan(scannedContent: String) {
        val code = InviteCodeUtils.extractInviteCode(scannedContent)
        if (code != null) {
            setInviteCode(code)
            validateCode()
        } else {
            _uiState.value = JoinGroupUiState.Error("Invalid QR code format")
        }
    }

    /**
     * AC E11.9.8: Handle deep link navigation
     * Extract code from deep link URL format: phonemanager://join/{code}
     *
     * @param deepLink The deep link URL
     */
    fun handleDeepLink(deepLink: String) {
        val code = InviteCodeUtils.extractInviteCode(deepLink)
        if (code != null) {
            setInviteCode(code)
            validateCode()
        }
    }

    /**
     * Clear the current state to start over
     */
    fun clearState() {
        _inviteCode.value = ""
        _groupPreview.value = null
        _uiState.value = JoinGroupUiState.EnterCode
        _joinResult.value = JoinResult.Idle
    }

    /**
     * Clear join result
     */
    fun clearJoinResult() {
        _joinResult.value = JoinResult.Idle
    }

    /**
     * Refresh authentication status
     */
    fun refreshAuthStatus() {
        _isAuthenticated.value = authRepository.isLoggedIn()
    }
}

/**
 * UI state for join group flow
 */
sealed interface JoinGroupUiState {
    object EnterCode : JoinGroupUiState
    object Validating : JoinGroupUiState
    data class PreviewReady(val group: GroupPreview) : JoinGroupUiState
    data class Error(val message: String) : JoinGroupUiState
}

/**
 * Result of join operation
 */
sealed interface JoinResult {
    object Idle : JoinResult
    object Joining : JoinResult
    object AuthenticationRequired : JoinResult
    data class Success(val groupId: String, val role: GroupRole) : JoinResult
    data class Error(val message: String) : JoinResult
}

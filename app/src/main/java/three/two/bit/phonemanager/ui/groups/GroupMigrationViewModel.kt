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
import timber.log.Timber
import javax.inject.Inject

/**
 * Story UGM-4.3: Group Migration ViewModel
 *
 * Handles group migration operations:
 * - AC 1: Pre-fill group name with registration group info
 * - AC 2: Validate group name (3-50 chars)
 * - AC 3: Call migration API
 * - AC 4: Set user as OWNER of new group
 * - AC 5: Delete registration group after migration
 * - AC 6: Show progress indicator
 *
 * Dependencies:
 * - GroupRepository for migration API calls
 */
@HiltViewModel
class GroupMigrationViewModel @Inject constructor(
    private val groupRepository: GroupRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    // Get groupId from navigation arguments
    private val registrationGroupId: String = savedStateHandle.get<String>("groupId") ?: ""

    private val _uiState = MutableStateFlow<MigrationUiState>(MigrationUiState.Idle)
    val uiState: StateFlow<MigrationUiState> = _uiState.asStateFlow()

    // Group name input
    private val _groupName = MutableStateFlow("")
    val groupName: StateFlow<String> = _groupName.asStateFlow()

    // Validation error for group name
    private val _nameError = MutableStateFlow<String?>(null)
    val nameError: StateFlow<String?> = _nameError.asStateFlow()

    // Device count for display
    private val _deviceCount = MutableStateFlow(0)
    val deviceCount: StateFlow<Int> = _deviceCount.asStateFlow()

    init {
        Timber.d("GroupMigrationViewModel init with groupId=$registrationGroupId")
        if (registrationGroupId.isNotBlank()) {
            loadRegistrationGroupInfo()
        } else {
            Timber.w("GroupMigrationViewModel: No groupId provided")
            _uiState.value = MigrationUiState.Error(
                message = "No group ID provided",
            )
        }
    }

    /**
     * Load registration group info to pre-fill the form (AC 1)
     */
    private fun loadRegistrationGroupInfo() {
        viewModelScope.launch {
            val result = groupRepository.checkRegistrationGroup()

            result.fold(
                onSuccess = { groupInfo ->
                    if (groupInfo != null && groupInfo.groupId == registrationGroupId) {
                        Timber.i("Loaded registration group info: ${groupInfo.groupName}")
                        _groupName.value = groupInfo.groupName
                        _deviceCount.value = groupInfo.deviceCount
                    } else {
                        // Use group ID as fallback name
                        _groupName.value = registrationGroupId.take(20)
                    }
                },
                onFailure = { error ->
                    Timber.e(error, "Failed to load registration group info")
                    // Use group ID as fallback name
                    _groupName.value = registrationGroupId.take(20)
                },
            )
        }
    }

    /**
     * Update group name input
     */
    fun updateGroupName(name: String) {
        _groupName.value = name
        validateGroupName(name)
    }

    /**
     * Validate group name (AC 2: 3-50 chars, alphanumeric and spaces)
     */
    private fun validateGroupName(name: String): Boolean {
        val trimmedName = name.trim()

        _nameError.value = when {
            trimmedName.length < 3 -> "Group name must be at least 3 characters"
            trimmedName.length > 50 -> "Group name must be 50 characters or less"
            !trimmedName.matches(Regex("^[a-zA-Z0-9 ]+$")) ->
                "Group name can only contain letters, numbers, and spaces"
            else -> null
        }

        return _nameError.value == null
    }

    /**
     * Check if migration can proceed
     */
    fun canMigrate(): Boolean {
        return validateGroupName(_groupName.value) && _uiState.value !is MigrationUiState.Loading
    }

    /**
     * Execute migration (AC 3, 4, 5, 6)
     */
    fun migrate() {
        val name = _groupName.value.trim()

        if (!validateGroupName(name)) {
            return
        }

        viewModelScope.launch {
            _uiState.value = MigrationUiState.Loading

            Timber.i("Starting migration: $registrationGroupId -> $name")

            val result = groupRepository.migrateRegistrationGroup(
                groupId = registrationGroupId,
                newName = name,
            )

            result.fold(
                onSuccess = { newGroup ->
                    Timber.i("Migration successful: ${newGroup.id}")
                    _uiState.value = MigrationUiState.Success(newGroup)
                },
                onFailure = { error ->
                    Timber.e(error, "Migration failed")
                    _uiState.value = MigrationUiState.Error(
                        message = getErrorMessage(error),
                    )
                },
            )
        }
    }

    /**
     * Clear error state to retry
     */
    fun clearError() {
        if (_uiState.value is MigrationUiState.Error) {
            _uiState.value = MigrationUiState.Idle
        }
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
                "Registration group not found."
            message.contains("409") || message.contains("conflict", ignoreCase = true) ->
                "A group with this name already exists."
            message.contains("network", ignoreCase = true) ||
                message.contains("connection", ignoreCase = true) ->
                "Network error. Please check your connection and try again."
            else -> "Migration failed. Please try again."
        }
    }
}

/**
 * Story UGM-4.3: UI State for Migration Screen
 */
sealed interface MigrationUiState {
    /**
     * Initial state - ready to migrate
     */
    data object Idle : MigrationUiState

    /**
     * Migration in progress (AC 6)
     */
    data object Loading : MigrationUiState

    /**
     * Migration successful (AC 4)
     *
     * @property newGroup The newly created authenticated group
     */
    data class Success(val newGroup: Group) : MigrationUiState

    /**
     * Migration failed
     *
     * @property message Error message to display
     */
    data class Error(val message: String) : MigrationUiState
}

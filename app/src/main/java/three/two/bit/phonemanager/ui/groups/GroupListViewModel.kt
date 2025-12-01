package three.two.bit.phonemanager.ui.groups

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
import timber.log.Timber
import javax.inject.Inject

/**
 * Story E11.8 Task 4: Group List ViewModel
 *
 * Handles group list operations including:
 * - Listing user's groups (AC E11.8.1)
 * - Creating groups (AC E11.8.2)
 *
 * Dependencies:
 * - GroupRepository for API calls
 * - AuthRepository for authentication state
 */
@HiltViewModel
class GroupListViewModel @Inject constructor(
    private val groupRepository: GroupRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<GroupListUiState>(GroupListUiState.Loading)
    val uiState: StateFlow<GroupListUiState> = _uiState.asStateFlow()

    private val _createGroupResult = MutableStateFlow<CreateGroupResult>(CreateGroupResult.Idle)
    val createGroupResult: StateFlow<CreateGroupResult> = _createGroupResult.asStateFlow()

    init {
        // Auto-load groups when ViewModel is created
        if (authRepository.isLoggedIn()) {
            refreshGroups()
        } else {
            _uiState.value = GroupListUiState.Error(
                message = "Please sign in to view your groups",
                errorCode = "not_authenticated"
            )
        }
    }

    /**
     * AC E11.8.1: Refresh the group list
     *
     * Fetches all groups the user belongs to.
     */
    fun refreshGroups() {
        if (!authRepository.isLoggedIn()) {
            _uiState.value = GroupListUiState.Error(
                message = "Please sign in to view your groups",
                errorCode = "not_authenticated"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = GroupListUiState.Loading

            val result = groupRepository.getUserGroups()

            result.fold(
                onSuccess = { groups ->
                    Timber.i("Loaded ${groups.size} groups")
                    _uiState.value = if (groups.isEmpty()) {
                        GroupListUiState.Empty
                    } else {
                        GroupListUiState.Success(groups = groups)
                    }
                },
                onFailure = { error ->
                    Timber.e(error, "Failed to load groups")
                    _uiState.value = GroupListUiState.Error(
                        message = getErrorMessage(error),
                        errorCode = getErrorCode(error)
                    )
                }
            )
        }
    }

    /**
     * AC E11.8.2: Create a new group
     *
     * @param name Group name (required)
     * @param description Optional description
     */
    fun createGroup(name: String, description: String?) {
        if (name.isBlank()) {
            _createGroupResult.value = CreateGroupResult.Error(
                message = "Group name is required"
            )
            return
        }

        if (name.length > 50) {
            _createGroupResult.value = CreateGroupResult.Error(
                message = "Group name must be 50 characters or less"
            )
            return
        }

        viewModelScope.launch {
            _createGroupResult.value = CreateGroupResult.Loading

            val result = groupRepository.createGroup(
                name = name.trim(),
                description = description?.trim()?.takeIf { it.isNotBlank() }
            )

            result.fold(
                onSuccess = { group ->
                    Timber.i("Group created: ${group.id}")
                    _createGroupResult.value = CreateGroupResult.Success(group = group)
                    // Refresh the list to show the new group
                    refreshGroups()
                },
                onFailure = { error ->
                    Timber.e(error, "Failed to create group")
                    _createGroupResult.value = CreateGroupResult.Error(
                        message = getErrorMessage(error)
                    )
                }
            )
        }
    }

    /**
     * Clear create group result state
     */
    fun clearCreateGroupResult() {
        _createGroupResult.value = CreateGroupResult.Idle
    }

    /**
     * Get the number of groups
     */
    fun getGroupCount(): Int = groupRepository.getGroupCount()

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
            message.contains("409") || message.contains("conflict", ignoreCase = true) ->
                "A group with this name already exists."
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
            message.contains("409") -> "conflict"
            message.contains("network", ignoreCase = true) -> "network_error"
            else -> null
        }
    }
}

/**
 * Story E11.8: UI State for Group List Screen
 */
sealed interface GroupListUiState {
    /**
     * Loading groups
     */
    data object Loading : GroupListUiState

    /**
     * Successfully loaded groups
     *
     * @property groups List of user's groups
     */
    data class Success(
        val groups: List<Group>,
    ) : GroupListUiState

    /**
     * Error loading groups
     *
     * @property message User-friendly error message
     * @property errorCode Optional error code for debugging
     */
    data class Error(
        val message: String,
        val errorCode: String? = null,
    ) : GroupListUiState

    /**
     * Empty state - user has no groups
     */
    data object Empty : GroupListUiState
}

/**
 * Story E11.8: Result state for create group operation
 */
sealed interface CreateGroupResult {
    /**
     * No operation in progress
     */
    data object Idle : CreateGroupResult

    /**
     * Creating group in progress
     */
    data object Loading : CreateGroupResult

    /**
     * Group created successfully
     *
     * @property group The created group
     */
    data class Success(val group: Group) : CreateGroupResult

    /**
     * Failed to create group
     *
     * @property message Error message
     */
    data class Error(val message: String) : CreateGroupResult
}

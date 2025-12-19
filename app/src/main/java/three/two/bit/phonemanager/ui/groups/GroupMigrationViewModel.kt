package three.two.bit.phonemanager.ui.groups

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import three.two.bit.phonemanager.data.repository.GroupRepository
import three.two.bit.phonemanager.domain.model.Group
import three.two.bit.phonemanager.network.ConnectivityMonitor
import three.two.bit.phonemanager.util.ValidationConstants
import timber.log.Timber
import javax.inject.Inject

/**
 * Story UGM-4.3: Group Migration ViewModel
 * Story UGM-4.4: Handle Migration Errors and Offline
 *
 * Handles group migration operations:
 * - AC 1: Pre-fill group name with registration group info
 * - AC 2: Validate group name (3-50 chars)
 * - AC 3: Call migration API
 * - AC 4: Set user as OWNER of new group
 * - AC 5: Delete registration group after migration
 * - AC 6: Show progress indicator
 *
 * Error handling (UGM-4.4):
 * - AC 1: Network error handling with specific message
 * - AC 2: Retry option for failed migrations
 * - AC 3: Offline detection before migration
 * - AC 4: No offline queue (requires connection)
 * - AC 5: Server error handling
 * - AC 6: Retry functionality
 *
 * Dependencies:
 * - GroupRepository for migration API calls
 * - ConnectivityMonitor for network state
 */
@HiltViewModel
class GroupMigrationViewModel @Inject constructor(
    private val groupRepository: GroupRepository,
    private val connectivityMonitor: ConnectivityMonitor,
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
    private val _nameError = MutableStateFlow<NameValidationError?>(null)
    val nameError: StateFlow<NameValidationError?> = _nameError.asStateFlow()

    // Device count for display
    private val _deviceCount = MutableStateFlow(0)
    val deviceCount: StateFlow<Int> = _deviceCount.asStateFlow()

    // Story UGM-4.4: Network connectivity state (AC 3, 4)
    val isOnline: StateFlow<Boolean> = connectivityMonitor.observeConnectivity()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = connectivityMonitor.isNetworkAvailable(),
        )

    init {
        Timber.d("GroupMigrationViewModel init with groupId=$registrationGroupId")
        if (registrationGroupId.isNotBlank()) {
            loadRegistrationGroupInfo()
        } else {
            Timber.w("GroupMigrationViewModel: No groupId provided")
            _uiState.value = MigrationUiState.Error(
                errorType = MigrationErrorType.NotFound,
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
     * Validate group name (AC 2: 3-50 chars, letters, numbers and spaces)
     *
     * Supports Unicode letters for international names (e.g., "Семья Иванова", "田中家族").
     * Uses \\p{L} for Unicode letters and \\p{N} for Unicode numbers.
     */
    private fun validateGroupName(name: String): Boolean {
        val trimmedName = name.trim()

        _nameError.value = when {
            trimmedName.length < ValidationConstants.MIN_GROUP_NAME_LENGTH -> NameValidationError.TooShort
            trimmedName.length > ValidationConstants.MAX_GROUP_NAME_LENGTH -> NameValidationError.TooLong
            // Allow Unicode letters (\p{L}), Unicode numbers (\p{N}), and spaces
            !trimmedName.matches(Regex("^[\\p{L}\\p{N} ]+$")) -> NameValidationError.InvalidCharacters
            else -> null
        }

        return _nameError.value == null
    }

    /**
     * Check if migration can proceed
     * Story UGM-4.4 AC 3, 4: Requires network connection
     */
    fun canMigrate(): Boolean {
        return validateGroupName(_groupName.value) &&
            _uiState.value !is MigrationUiState.Loading &&
            isOnline.value
    }

    /**
     * Execute migration (AC 3, 4, 5, 6)
     * Story UGM-4.4 AC 1, 3, 5: Handle offline and errors
     */
    fun migrate() {
        val name = _groupName.value.trim()

        if (!validateGroupName(name)) {
            return
        }

        // Story UGM-4.4 AC 3, 4: Check connectivity before migration
        if (!isOnline.value) {
            Timber.w("Migration attempted while offline")
            _uiState.value = MigrationUiState.Offline
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
                        errorType = getErrorType(error),
                        isRetryable = isRetryableError(error),
                    )
                },
            )
        }
    }

    /**
     * Story UGM-4.4 AC 2, 6: Retry migration after failure
     */
    fun retry() {
        Timber.d("Retrying migration")
        clearError()
        migrate()
    }

    /**
     * Clear error state to retry
     */
    fun clearError() {
        if (_uiState.value is MigrationUiState.Error || _uiState.value is MigrationUiState.Offline) {
            _uiState.value = MigrationUiState.Idle
        }
    }

    /**
     * Story UGM-4.4 AC 1, 5: Convert exception to error type for UI string resolution
     *
     * Parses HTTP status codes and error messages to determine the appropriate error type.
     * Uses regex patterns for reliable status code extraction.
     */
    private fun getErrorType(exception: Throwable): MigrationErrorType {
        val message = exception.message ?: ""

        // Try to extract HTTP status code using regex pattern (e.g., "HTTP 401", "status: 404", "code 500")
        val statusCodePattern = Regex("""(?:HTTP|status|code)[:\s]*(\d{3})""", RegexOption.IGNORE_CASE)
        val statusCodeMatch = statusCodePattern.find(message)
        val statusCode = statusCodeMatch?.groupValues?.getOrNull(1)?.toIntOrNull()

        // Also check for standalone 3-digit codes that look like HTTP status codes
        val standaloneCodePattern = Regex("""\b([45]\d{2})\b""")
        val standaloneCode = if (statusCode == null) {
            standaloneCodePattern.find(message)?.groupValues?.getOrNull(1)?.toIntOrNull()
        } else null

        val httpCode = statusCode ?: standaloneCode

        return when {
            // Check HTTP status codes first (most reliable)
            httpCode == 401 -> MigrationErrorType.Unauthorized
            httpCode == 403 -> MigrationErrorType.Forbidden
            httpCode == 404 -> MigrationErrorType.NotFound
            httpCode == 409 -> MigrationErrorType.Conflict
            httpCode != null && httpCode in 500..599 -> MigrationErrorType.Server

            // Fall back to keyword matching for non-HTTP errors
            message.contains("unauthorized", ignoreCase = true) -> MigrationErrorType.Unauthorized
            message.contains("forbidden", ignoreCase = true) -> MigrationErrorType.Forbidden
            message.contains("not found", ignoreCase = true) -> MigrationErrorType.NotFound
            message.contains("conflict", ignoreCase = true) ||
                message.contains("already exists", ignoreCase = true) -> MigrationErrorType.Conflict

            // Network-related errors
            message.contains("network", ignoreCase = true) ||
                message.contains("connection", ignoreCase = true) ||
                message.contains("timeout", ignoreCase = true) ||
                message.contains("unable to resolve", ignoreCase = true) ||
                message.contains("no internet", ignoreCase = true) ||
                message.contains("ConnectException", ignoreCase = true) ||
                message.contains("SocketException", ignoreCase = true) ||
                message.contains("UnknownHostException", ignoreCase = true) ->
                MigrationErrorType.Network

            else -> MigrationErrorType.Generic
        }
    }

    /**
     * Story UGM-4.4: Determine if error is retryable
     */
    private fun isRetryableError(exception: Throwable): Boolean {
        val message = exception.message ?: ""
        // Not retryable: auth errors, not found, conflict
        return when {
            message.contains("401") -> false
            message.contains("403") -> false
            message.contains("404") -> false
            message.contains("409") -> false
            else -> true // Network errors, server errors, etc. are retryable
        }
    }
}

/**
 * Story UGM-4.3: UI State for Migration Screen
 * Story UGM-4.4: Enhanced error states
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
     * Story UGM-4.4 AC 3: Device is offline
     */
    data object Offline : MigrationUiState

    /**
     * Migration failed
     * Story UGM-4.4 AC 1, 2, 5: Error with retry option
     *
     * @property errorType The type of error for UI string resolution
     * @property isRetryable Whether retry button should be shown
     */
    data class Error(val errorType: MigrationErrorType, val isRetryable: Boolean = true) : MigrationUiState
}

/**
 * Story UGM-4.3 AC 2: Group name validation errors
 *
 * Sealed interface for validation errors that can be resolved to string resources in the UI.
 */
sealed interface NameValidationError {
    /** Group name is less than 3 characters */
    data object TooShort : NameValidationError

    /** Group name is more than 50 characters */
    data object TooLong : NameValidationError

    /** Group name contains invalid characters (only letters, numbers, spaces allowed) */
    data object InvalidCharacters : NameValidationError
}

/**
 * Story UGM-4.4: Migration API error types
 *
 * Sealed interface for API errors that can be resolved to string resources in the UI.
 */
sealed interface MigrationErrorType {
    /** Network/connection error */
    data object Network : MigrationErrorType

    /** Server error (5xx) */
    data object Server : MigrationErrorType

    /** Session expired (401) */
    data object Unauthorized : MigrationErrorType

    /** Permission denied (403) */
    data object Forbidden : MigrationErrorType

    /** Registration group not found (404) */
    data object NotFound : MigrationErrorType

    /** Group name conflict (409) */
    data object Conflict : MigrationErrorType

    /** Generic/unknown error */
    data object Generic : MigrationErrorType
}

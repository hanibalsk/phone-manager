package three.two.bit.phonemanager.ui.registration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import three.two.bit.phonemanager.data.repository.DeviceRepository
import three.two.bit.phonemanager.network.NetworkException
import timber.log.Timber
import javax.inject.Inject

/**
 * Story E1.1: RegistrationViewModel - Handles device registration UI state and logic
 */
@HiltViewModel
class RegistrationViewModel @Inject constructor(private val deviceRepository: DeviceRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(RegistrationUiState())
    val uiState: StateFlow<RegistrationUiState> = _uiState.asStateFlow()

    /**
     * Update display name and validate
     */
    fun updateDisplayName(name: String) {
        _uiState.update { it.copy(displayName = name, displayNameError = null) }
        validateForm()
    }

    /**
     * Update group ID and validate
     */
    fun updateGroupId(groupId: String) {
        _uiState.update { it.copy(groupId = groupId, groupIdError = null) }
        validateForm()
    }

    /**
     * Submit registration
     */
    fun register() {
        if (!validateForm()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val result = deviceRepository.registerDevice(
                displayName = _uiState.value.displayName.trim(),
                groupId = _uiState.value.groupId.trim(),
            )

            result.fold(
                onSuccess = {
                    Timber.i("Registration successful")
                    _uiState.update { it.copy(isLoading = false, isRegistered = true) }
                },
                onFailure = { e ->
                    Timber.e(e, "Registration failed")
                    val errorMessage = when (e) {
                        is NetworkException -> "No internet connection. Please check your network."
                        else -> e.message ?: "Registration failed. Please try again."
                    }
                    _uiState.update { it.copy(isLoading = false, error = errorMessage) }
                },
            )
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Validate form fields and update state
     * @return true if form is valid
     */
    private fun validateForm(): Boolean {
        val state = _uiState.value
        var isValid = true

        // Validate display name
        val displayNameError = when {
            state.displayName.isBlank() -> "Display name is required"
            state.displayName.trim().length < 2 -> "Display name must be at least 2 characters"
            state.displayName.trim().length > 50 -> "Display name must be 50 characters or less"
            else -> null
        }

        // Validate group ID
        val groupIdRegex = Regex("^[a-zA-Z0-9-]+$")
        val groupIdError = when {
            state.groupId.isBlank() -> "Group ID is required"
            state.groupId.trim().length < 2 -> "Group ID must be at least 2 characters"
            state.groupId.trim().length > 50 -> "Group ID must be 50 characters or less"
            !state.groupId.trim().matches(groupIdRegex) ->
                "Group ID can only contain letters, numbers, and hyphens"
            else -> null
        }

        if (displayNameError != null || groupIdError != null) {
            isValid = false
        }

        _uiState.update {
            it.copy(
                displayNameError = displayNameError,
                groupIdError = groupIdError,
                isFormValid = isValid,
            )
        }

        return isValid
    }
}

/**
 * UI state for registration screen
 */
data class RegistrationUiState(
    val displayName: String = "",
    val groupId: String = "",
    val displayNameError: String? = null,
    val groupIdError: String? = null,
    val isFormValid: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isRegistered: Boolean = false,
)

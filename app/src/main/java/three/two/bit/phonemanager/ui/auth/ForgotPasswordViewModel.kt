package three.two.bit.phonemanager.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import three.two.bit.phonemanager.network.auth.AuthApiService
import timber.log.Timber
import javax.inject.Inject

/**
 * Story E9.11: ViewModel for Forgot Password Screen
 *
 * Handles password reset request via AuthApiService.
 * Uses BuildConfig.USE_MOCK_AUTH to determine mock vs real API calls.
 */
@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(private val authApiService: AuthApiService) : ViewModel() {

    private val _uiState = MutableStateFlow(ForgotPasswordUiState())
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState.asStateFlow()

    /**
     * Request password reset email
     *
     * @param email User's email address
     */
    fun requestPasswordReset(email: String) {
        if (!isValidEmail(email)) {
            _uiState.value = _uiState.value.copy(
                emailError = "Invalid email address",
            )
            return
        }

        _uiState.value = _uiState.value.copy(
            isLoading = true,
            emailError = null,
            errorMessage = null,
        )

        viewModelScope.launch {
            try {
                // AuthApiService handles mock vs real based on BuildConfig.USE_MOCK_AUTH
                authApiService.forgotPassword(email)

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isSuccess = true,
                    submittedEmail = email,
                )
                Timber.i("Password reset requested for: $email")
            } catch (e: Exception) {
                Timber.e(e, "Password reset request failed")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Failed to send reset email",
                )
            }
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    /**
     * Clear email error
     */
    fun clearEmailError() {
        _uiState.value = _uiState.value.copy(emailError = null)
    }

    /**
     * Reset UI state for retry
     */
    fun reset() {
        _uiState.value = ForgotPasswordUiState()
    }

    private fun isValidEmail(email: String): Boolean =
        email.matches(Regex("[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"))
}

/**
 * UI State for Forgot Password Screen
 */
data class ForgotPasswordUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val submittedEmail: String = "",
    val emailError: String? = null,
    val errorMessage: String? = null,
)

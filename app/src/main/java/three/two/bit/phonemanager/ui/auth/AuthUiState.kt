package three.two.bit.phonemanager.ui.auth

import three.two.bit.phonemanager.domain.auth.User

/**
 * Story E9.11, Task 7: Authentication UI State
 *
 * Represents the current state of authentication operations.
 */
sealed class AuthUiState {
    /**
     * Initial state before any operation
     */
    data object Idle : AuthUiState()

    /**
     * Operation in progress (login, register, OAuth)
     */
    data object Loading : AuthUiState()

    /**
     * Authentication successful
     *
     * @property user The authenticated user
     */
    data class Success(val user: User) : AuthUiState()

    /**
     * Authentication failed
     *
     * @property message Error message to display
     * @property errorCode Optional error code for specific handling
     */
    data class Error(
        val message: String,
        val errorCode: String? = null
    ) : AuthUiState()
}

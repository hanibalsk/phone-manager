package three.two.bit.phonemanager.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Instant
import three.two.bit.phonemanager.domain.auth.User
import three.two.bit.phonemanager.network.auth.AuthApiService
import three.two.bit.phonemanager.network.auth.LoginRequest
import three.two.bit.phonemanager.network.auth.OAuthRequest
import three.two.bit.phonemanager.network.auth.RefreshRequest
import three.two.bit.phonemanager.network.auth.RegisterRequest
import three.two.bit.phonemanager.security.SecureStorage
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Story E9.11, Task 4: Authentication Repository
 *
 * Handles user authentication, token management, and session state.
 *
 * AC E9.11.6: Store tokens in SecureStorage on successful auth
 * AC E9.11.8: Implement token refresh with mutex to prevent concurrent requests
 *
 * Dependencies: Backend E9.1-E9.10 (auth endpoints)
 *
 * Note: Currently blocked by backend implementation.
 */
@Singleton
class AuthRepository @Inject constructor(
    private val authApiService: AuthApiService,
    private val secureStorage: SecureStorage
) {

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: Flow<User?> = _currentUser.asStateFlow()

    // Mutex to prevent concurrent token refresh requests
    private val refreshMutex = Mutex()

    init {
        // Restore user state from secure storage on initialization
        restoreUserState()
    }

    /**
     * Restore user state from SecureStorage
     *
     * Called on initialization to restore the user session
     * if tokens and user info are available.
     */
    private fun restoreUserState() {
        if (secureStorage.isAuthenticated() && secureStorage.hasUserInfo()) {
            val userId = secureStorage.getUserId()
            val email = secureStorage.getUserEmail()
            val displayName = secureStorage.getUserDisplayName()
            val createdAt = secureStorage.getUserCreatedAt()

            if (userId != null && email != null) {
                val user = User(
                    userId = userId,
                    email = email,
                    displayName = displayName ?: email.substringBefore('@'),
                    createdAt = createdAt?.let { Instant.parse(it) } ?: Instant.DISTANT_PAST
                )
                _currentUser.value = user
                Timber.d("User state restored from storage: ${user.userId}")
            }
        }
    }

    /**
     * AC E9.11.4: Register a new user account
     *
     * @param email User email address
     * @param password User password
     * @param displayName User display name
     * @return Result with User on success, exception on failure
     */
    suspend fun register(
        email: String,
        password: String,
        displayName: String
    ): Result<User> = runCatching {
        Timber.d("Registering user: $email")

        val response = authApiService.register(
            RegisterRequest(
                email = email,
                password = password,
                displayName = displayName
            )
        )

        // Store tokens securely (AC E9.11.6)
        storeTokens(
            response.tokens.accessToken,
            response.tokens.refreshToken,
            response.tokens.expiresIn
        )

        // Map to domain model and update current user
        val user = User(
            userId = response.user.id,
            email = response.user.email,
            displayName = response.user.displayName,
            createdAt = Instant.parse(response.user.createdAt)
        )
        _currentUser.value = user

        // Store user info for session restoration
        secureStorage.saveUserInfo(
            userId = response.user.id,
            email = response.user.email,
            displayName = response.user.displayName,
            createdAt = response.user.createdAt
        )

        Timber.i("User registered successfully: ${user.userId}")
        user
    }

    /**
     * AC E9.11.3: Login with email and password
     *
     * @param email User email address
     * @param password User password
     * @return Result with User on success, exception on failure
     */
    suspend fun login(
        email: String,
        password: String
    ): Result<User> = runCatching {
        Timber.d("Logging in user: $email")

        val response = authApiService.login(
            LoginRequest(
                email = email,
                password = password
            )
        )

        // Store tokens securely (AC E9.11.6)
        storeTokens(
            response.tokens.accessToken,
            response.tokens.refreshToken,
            response.tokens.expiresIn
        )

        // Map to domain model and update current user
        val user = User(
            userId = response.user.id,
            email = response.user.email,
            displayName = response.user.displayName,
            createdAt = Instant.parse(response.user.createdAt)
        )
        _currentUser.value = user

        // Store user info for session restoration
        secureStorage.saveUserInfo(
            userId = response.user.id,
            email = response.user.email,
            displayName = response.user.displayName,
            createdAt = response.user.createdAt
        )

        Timber.i("User logged in successfully: ${user.userId}")
        user
    }

    /**
     * AC E9.11.5: OAuth Sign-In (Google, Apple)
     *
     * @param provider OAuth provider (google, apple)
     * @param idToken ID token from OAuth provider
     * @return Result with User on success, exception on failure
     */
    suspend fun oauthLogin(
        provider: String,
        idToken: String
    ): Result<User> = runCatching {
        Timber.d("OAuth sign-in with provider: $provider")

        val response = authApiService.oauthSignIn(
            OAuthRequest(
                provider = provider,
                idToken = idToken
            )
        )

        // Store tokens securely (AC E9.11.6)
        storeTokens(
            response.tokens.accessToken,
            response.tokens.refreshToken,
            response.tokens.expiresIn
        )

        // Map to domain model and update current user
        val user = User(
            userId = response.user.id,
            email = response.user.email,
            displayName = response.user.displayName,
            createdAt = Instant.parse(response.user.createdAt)
        )
        _currentUser.value = user

        // Store user info for session restoration
        secureStorage.saveUserInfo(
            userId = response.user.id,
            email = response.user.email,
            displayName = response.user.displayName,
            createdAt = response.user.createdAt
        )

        Timber.i("OAuth sign-in successful: ${user.userId}")
        user
    }

    /**
     * AC E9.11.6: Logout current user
     *
     * Clears tokens and session state.
     * Always succeeds locally even if server logout fails.
     *
     * @param allDevices If true, invalidate all sessions for the user
     */
    suspend fun logout(allDevices: Boolean = false): Result<Unit> = runCatching {
        Timber.d("Logging out user")

        val refreshToken = secureStorage.getRefreshToken()

        try {
            // Attempt to logout on server (AC E9.11.6)
            if (refreshToken != null) {
                authApiService.logout(refreshToken, allDevices)
            }
        } catch (e: Exception) {
            // Ignore server errors - always clear local state
            Timber.w(e, "Server logout failed, clearing local state anyway")
        }

        // Always clear tokens, user info, and session (AC E9.11.6)
        secureStorage.clearTokens()
        secureStorage.clearUserInfo()
        _currentUser.value = null

        Timber.i("User logged out successfully")
    }

    /**
     * AC E9.11.8: Refresh access token
     *
     * Uses mutex to prevent concurrent refresh requests.
     *
     * @return Result with new access token on success, exception on failure
     */
    suspend fun refreshToken(): Result<String> = refreshMutex.withLock {
        runCatching {
            Timber.d("Refreshing access token")

            val refreshToken = secureStorage.getRefreshToken()
                ?: throw IllegalStateException("No refresh token available")

            val response = authApiService.refreshToken(
                RefreshRequest(refreshToken = refreshToken)
            )

            // Store new tokens (AC E9.11.8)
            storeTokens(
                response.tokens.accessToken,
                response.tokens.refreshToken,
                response.tokens.expiresIn
            )

            Timber.i("Access token refreshed successfully")
            response.tokens.accessToken
        }
    }

    /**
     * Request password reset
     *
     * @param email User email address
     * @return Result with success message
     */
    suspend fun forgotPassword(email: String): Result<String> = runCatching {
        Timber.d("Requesting password reset for: $email")
        val response = authApiService.forgotPassword(email)
        Timber.i("Password reset email requested")
        response.message
    }

    /**
     * Reset password with token
     *
     * @param token Password reset token from email
     * @param newPassword New password
     * @return Result with success message
     */
    suspend fun resetPassword(token: String, newPassword: String): Result<String> = runCatching {
        Timber.d("Resetting password with token")
        val response = authApiService.resetPassword(token, newPassword)
        Timber.i("Password reset successfully")
        response.message
    }

    /**
     * Verify email with token
     *
     * @param token Email verification token
     * @return Result with verification status
     */
    suspend fun verifyEmail(token: String): Result<Boolean> = runCatching {
        Timber.d("Verifying email with token")
        val response = authApiService.verifyEmail(token)
        Timber.i("Email verified: ${response.emailVerified}")
        response.emailVerified
    }

    /**
     * Request new verification email
     *
     * @return Result with success message
     */
    suspend fun requestVerification(): Result<String> = runCatching {
        Timber.d("Requesting verification email")
        val response = authApiService.requestVerification()
        Timber.i("Verification email sent")
        response.message
    }

    /**
     * Check if user is currently logged in
     *
     * @return true if user has valid tokens and session
     */
    fun isLoggedIn(): Boolean {
        return secureStorage.isAuthenticated()
    }

    /**
     * Get current authenticated user
     *
     * @return Current user or null if not authenticated
     */
    fun getCurrentUser(): User? {
        return _currentUser.value
    }

    /**
     * Story E12.6: Get current access token
     *
     * @return Access token if available, null if not authenticated
     */
    fun getAccessToken(): String? {
        return secureStorage.getAccessToken()
    }

    /**
     * Story E12.6: Get current user ID
     *
     * @return User ID if authenticated, null otherwise
     */
    fun getUserId(): String? {
        return _currentUser.value?.userId
    }

    /**
     * Store authentication tokens securely
     *
     * AC E9.11.6: Store tokens in SecureStorage on successful auth
     *
     * @param accessToken JWT access token
     * @param refreshToken JWT refresh token
     * @param expiresIn Seconds until access token expires
     */
    private fun storeTokens(accessToken: String, refreshToken: String, expiresIn: Long) {
        secureStorage.saveAccessToken(accessToken)
        secureStorage.saveRefreshToken(refreshToken)

        // Calculate expiry time (current time + expiresIn seconds)
        val expiryTimeMs = System.currentTimeMillis() + (expiresIn * 1000)
        secureStorage.saveTokenExpiryTime(expiryTimeMs)

        Timber.d("Tokens stored securely, expires in $expiresIn seconds")
    }
}

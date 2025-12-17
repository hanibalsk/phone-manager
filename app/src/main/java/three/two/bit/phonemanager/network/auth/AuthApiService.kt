package three.two.bit.phonemanager.network.auth

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import three.two.bit.phonemanager.auth.MockAuthHelper
import three.two.bit.phonemanager.network.ApiConfiguration
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Story E9.11, Task 3: Authentication API Service
 *
 * Handles all authentication-related HTTP requests.
 *
 * Dependencies: Backend E9.1-E9.10
 * - POST /api/v1/auth/register - User registration
 * - POST /api/v1/auth/login - Email/password login
 * - POST /api/v1/auth/logout - User logout
 * - POST /api/v1/auth/refresh - Refresh access token
 * - POST /api/v1/auth/oauth - OAuth sign-in (Google, Apple)
 * - POST /api/v1/auth/forgot-password - Request password reset
 * - POST /api/v1/auth/reset-password - Reset password with token
 * - POST /api/v1/auth/verify-email - Verify email with token
 * - POST /api/v1/auth/request-verification - Request new verification email
 *
 * Mock Mode: When MockAuthHelper.USE_MOCK_AUTH is true, uses mock responses
 * for development and testing without backend dependency.
 */
@Singleton
class AuthApiService @Inject constructor(private val httpClient: HttpClient, private val apiConfig: ApiConfiguration) {

    companion object {
        private const val AUTH_BASE_PATH = "/api/v1/auth"
    }

    /**
     * AC E9.11.4: Register a new user account
     *
     * @param request Registration details (email, password, displayName)
     * @return RegisterResponse with tokens and user info
     * @throws Exception on network error or validation failure
     *
     * Error Responses:
     * - 400: Invalid email format, weak password, or missing fields
     * - 409: Email already exists
     */
    suspend fun register(request: RegisterRequest): RegisterResponse {
        // Use mock if enabled for development
        if (MockAuthHelper.USE_MOCK_AUTH) {
            val mockResponse = MockAuthHelper.mockRegister(
                request.email,
                request.password,
                request.displayName,
            )
            return mockResponse.toRegisterResponse()
        }

        return httpClient.post("${apiConfig.baseUrl}$AUTH_BASE_PATH/register") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    /**
     * Legacy register method for mock compatibility
     */
    @Deprecated("Use register() with RegisterResponse", ReplaceWith("register(request)"))
    suspend fun registerLegacy(request: RegisterRequest): AuthResponse {
        if (MockAuthHelper.USE_MOCK_AUTH) {
            return MockAuthHelper.mockRegister(
                request.email,
                request.password,
                request.displayName,
            )
        }
        val response: RegisterResponse = httpClient.post("${apiConfig.baseUrl}$AUTH_BASE_PATH/register") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
        return response.toLegacyAuthResponse()
    }

    /**
     * AC E9.11.3: Login with email and password
     *
     * @param request Login credentials (email, password)
     * @return LoginResponse with tokens and user info
     * @throws Exception on network error or invalid credentials
     *
     * Error Responses:
     * - 400: Invalid email format or missing fields
     * - 401: Invalid credentials
     * - 403: Account locked or disabled
     */
    suspend fun login(request: LoginRequest): LoginResponse {
        // Use mock if enabled for development
        if (MockAuthHelper.USE_MOCK_AUTH) {
            val mockResponse = MockAuthHelper.mockLogin(request.email, request.password)
            return mockResponse.toLoginResponse()
        }

        return httpClient.post("${apiConfig.baseUrl}$AUTH_BASE_PATH/login") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    /**
     * Legacy login method for mock compatibility
     */
    @Deprecated("Use login() with LoginResponse", ReplaceWith("login(request)"))
    suspend fun loginLegacy(request: LoginRequest): AuthResponse {
        if (MockAuthHelper.USE_MOCK_AUTH) {
            return MockAuthHelper.mockLogin(request.email, request.password)
        }
        val response: LoginResponse = httpClient.post("${apiConfig.baseUrl}$AUTH_BASE_PATH/login") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
        return response.toLegacyAuthResponse()
    }

    /**
     * AC E9.11.6: Logout current user
     *
     * Invalidates the current access and refresh tokens on the server.
     * Client should clear local tokens regardless of server response.
     *
     * @param refreshToken The refresh token to invalidate
     * @param allDevices If true, invalidate all sessions for the user
     * @throws Exception on network error (ignore in client code)
     *
     * Note: Client should always clear tokens locally even if this fails
     */
    suspend fun logout(refreshToken: String, allDevices: Boolean = false) {
        // Use mock if enabled for development
        if (MockAuthHelper.USE_MOCK_AUTH) {
            MockAuthHelper.mockLogout()
            return
        }

        httpClient.post("${apiConfig.baseUrl}$AUTH_BASE_PATH/logout") {
            contentType(ContentType.Application.Json)
            setBody(LogoutRequest(refreshToken, allDevices))
        }
    }

    /**
     * AC E9.11.8: Refresh access token
     *
     * Exchange refresh token for new access token.
     *
     * @param request Refresh token request
     * @return RefreshResponse with new tokens
     * @throws Exception on network error or invalid refresh token
     *
     * Error Responses:
     * - 401: Invalid or expired refresh token (logout required)
     */
    suspend fun refreshToken(request: RefreshRequest): RefreshResponse {
        // Use mock if enabled for development
        if (MockAuthHelper.USE_MOCK_AUTH) {
            val mockResponse = MockAuthHelper.mockRefreshToken(request.refreshToken)
            return RefreshResponse(
                tokens = TokensResponse(
                    accessToken = mockResponse.accessToken,
                    refreshToken = mockResponse.refreshToken,
                    tokenType = "Bearer",
                    expiresIn = mockResponse.expiresIn,
                ),
            )
        }

        return httpClient.post("${apiConfig.baseUrl}$AUTH_BASE_PATH/refresh") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    /**
     * Legacy refreshToken method for mock compatibility
     */
    @Deprecated("Use refreshToken() with RefreshResponse", ReplaceWith("refreshToken(request)"))
    suspend fun refreshTokenLegacy(request: RefreshRequest): AuthResponse {
        if (MockAuthHelper.USE_MOCK_AUTH) {
            return MockAuthHelper.mockRefreshToken(request.refreshToken)
        }
        val response: RefreshResponse = httpClient.post("${apiConfig.baseUrl}$AUTH_BASE_PATH/refresh") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
        // RefreshResponse doesn't have user info, so we can't convert fully
        throw UnsupportedOperationException("Use refreshToken() instead")
    }

    /**
     * AC E9.11.5: OAuth Sign-In (Google, Apple)
     *
     * Authenticate with OAuth provider token.
     *
     * @param request OAuth provider and ID token
     * @return LoginResponse with tokens and user info
     * @throws Exception on network error or invalid OAuth token
     *
     * Error Responses:
     * - 400: Invalid provider or missing token
     * - 401: Invalid OAuth token
     * - 403: OAuth provider not enabled
     */
    suspend fun oauthSignIn(request: OAuthRequest): LoginResponse {
        // Use mock if enabled for development
        if (MockAuthHelper.USE_MOCK_AUTH) {
            val mockResponse = MockAuthHelper.mockOAuthSignIn(request.provider, request.idToken)
            return mockResponse.toLoginResponse()
        }

        return httpClient.post("${apiConfig.baseUrl}$AUTH_BASE_PATH/oauth") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    /**
     * Legacy oauthSignIn method for mock compatibility
     */
    @Deprecated("Use oauthSignIn() with LoginResponse", ReplaceWith("oauthSignIn(request)"))
    suspend fun oauthSignInLegacy(request: OAuthRequest): AuthResponse {
        if (MockAuthHelper.USE_MOCK_AUTH) {
            return MockAuthHelper.mockOAuthSignIn(request.provider, request.idToken)
        }
        val response: LoginResponse = httpClient.post("${apiConfig.baseUrl}$AUTH_BASE_PATH/oauth") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
        return response.toLegacyAuthResponse()
    }

    /**
     * Request password reset
     *
     * Initiates the password reset flow by sending a reset email.
     * Always returns success to prevent email enumeration attacks.
     *
     * @param email User email address
     * @return ForgotPasswordResponse with success message
     */
    suspend fun forgotPassword(email: String): ForgotPasswordResponse {
        // Use mock if enabled for development
        if (MockAuthHelper.USE_MOCK_AUTH) {
            MockAuthHelper.mockRequestPasswordReset(email)
            return ForgotPasswordResponse(
                message = "If your email is registered, you will receive a password reset link.",
            )
        }

        return httpClient.post("${apiConfig.baseUrl}$AUTH_BASE_PATH/forgot-password") {
            contentType(ContentType.Application.Json)
            setBody(ForgotPasswordRequest(email))
        }.body()
    }

    /**
     * Reset password with token
     *
     * Resets the user's password using a valid reset token.
     *
     * @param token Password reset token from email
     * @param newPassword New password
     * @return ResetPasswordResponse with success message
     * @throws Exception if token is invalid or expired
     */
    suspend fun resetPassword(token: String, newPassword: String): ResetPasswordResponse {
        // Use mock if enabled for development
        if (MockAuthHelper.USE_MOCK_AUTH) {
            // Mock always succeeds for valid token format
            return ResetPasswordResponse(
                message = "Password has been reset successfully. Please log in with your new password.",
            )
        }

        return httpClient.post("${apiConfig.baseUrl}$AUTH_BASE_PATH/reset-password") {
            contentType(ContentType.Application.Json)
            setBody(ResetPasswordRequest(token, newPassword))
        }.body()
    }

    /**
     * Verify email with token
     *
     * Verifies the user's email address using a verification token.
     *
     * @param token Email verification token
     * @return VerifyEmailResponse with verification status
     * @throws Exception if token is invalid or expired
     */
    suspend fun verifyEmail(token: String): VerifyEmailResponse {
        // Use mock if enabled for development
        if (MockAuthHelper.USE_MOCK_AUTH) {
            return VerifyEmailResponse(
                message = "Email has been verified successfully.",
                emailVerified = true,
            )
        }

        return httpClient.post("${apiConfig.baseUrl}$AUTH_BASE_PATH/verify-email") {
            contentType(ContentType.Application.Json)
            setBody(VerifyEmailRequest(token))
        }.body()
    }

    /**
     * Request new verification email
     *
     * Sends a new email verification token to the user.
     * Requires authentication.
     *
     * @return RequestVerificationResponse with success message
     * @throws Exception if email is already verified
     */
    suspend fun requestVerification(): RequestVerificationResponse {
        // Use mock if enabled for development
        if (MockAuthHelper.USE_MOCK_AUTH) {
            return RequestVerificationResponse(
                message = "Verification email has been sent.",
            )
        }

        return httpClient.post("${apiConfig.baseUrl}$AUTH_BASE_PATH/request-verification") {
            contentType(ContentType.Application.Json)
        }.body()
    }
}

/**
 * Logout Request
 *
 * @property refreshToken The refresh token to invalidate
 * @property allDevices If true, invalidate all sessions for the user
 */
@kotlinx.serialization.Serializable
private data class LogoutRequest(val refreshToken: String, val allDevices: Boolean = false)

// Extension functions to convert between response types

private fun AuthResponse.toRegisterResponse() = RegisterResponse(
    user = UserResponse(
        id = user.userId,
        email = user.email,
        displayName = user.displayName,
        createdAt = user.createdAt,
    ),
    tokens = TokensResponse(
        accessToken = accessToken,
        refreshToken = refreshToken,
        tokenType = "Bearer",
        expiresIn = expiresIn,
    ),
)

private fun AuthResponse.toLoginResponse() = LoginResponse(
    user = UserResponse(
        id = user.userId,
        email = user.email,
        displayName = user.displayName,
        createdAt = user.createdAt,
    ),
    tokens = TokensResponse(
        accessToken = accessToken,
        refreshToken = refreshToken,
        tokenType = "Bearer",
        expiresIn = expiresIn,
    ),
)

private fun RegisterResponse.toLegacyAuthResponse() = AuthResponse(
    accessToken = tokens.accessToken,
    refreshToken = tokens.refreshToken,
    expiresIn = tokens.expiresIn,
    user = UserInfo(
        userId = user.id,
        email = user.email,
        displayName = user.displayName ?: user.email.substringBefore('@'),
        createdAt = user.createdAt ?: "",
    ),
)

private fun LoginResponse.toLegacyAuthResponse() = AuthResponse(
    accessToken = tokens.accessToken,
    refreshToken = tokens.refreshToken,
    expiresIn = tokens.expiresIn,
    user = UserInfo(
        userId = user.id,
        email = user.email,
        displayName = user.displayName ?: user.email.substringBefore('@'),
        createdAt = user.createdAt ?: "",
    ),
)

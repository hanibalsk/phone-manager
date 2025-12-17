package three.two.bit.phonemanager.network.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Story E9.11, Task 3: Authentication API Data Models
 *
 * Request and response models for authentication endpoints.
 */

// Request Models

/**
 * AC E9.11.4: Register Request
 *
 * @property email User email address
 * @property password User password (minimum 8 chars, 1 uppercase, 1 number, 1 special char)
 * @property displayName User display name
 */
@Serializable
data class RegisterRequest(val email: String, val password: String, @SerialName("display_name") val displayName: String)

/**
 * AC E9.11.3: Login Request
 *
 * @property email User email address
 * @property password User password
 */
@Serializable
data class LoginRequest(val email: String, val password: String)

/**
 * AC E9.11.8: Token Refresh Request
 *
 * @property refreshToken The refresh token to exchange for new access token
 */
@Serializable
data class RefreshRequest(@SerialName("refresh_token") val refreshToken: String)

/**
 * AC E9.11.5: OAuth Sign-In Request
 *
 * @property provider OAuth provider (google, apple)
 * @property idToken ID token from OAuth provider
 */
@Serializable
data class OAuthRequest(val provider: String, @SerialName("id_token") val idToken: String)

// Response Models

/**
 * Tokens Response
 *
 * Token information from auth endpoints.
 *
 * @property accessToken JWT access token for API authentication
 * @property refreshToken JWT refresh token for obtaining new access tokens
 * @property tokenType Token type (always "Bearer")
 * @property expiresIn Seconds until access token expires
 */
@Serializable
data class TokensResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("token_type") val tokenType: String = "Bearer",
    @SerialName("expires_in") val expiresIn: Long,
)

/**
 * User Response
 *
 * User information from auth endpoints.
 *
 * @property id Unique user identifier
 * @property email User email address
 * @property displayName User display name
 * @property avatarUrl Optional user avatar URL
 * @property emailVerified Whether email has been verified
 * @property authProvider Auth provider (email, google, apple)
 * @property organizationId Optional organization ID
 * @property createdAt ISO 8601 timestamp of account creation
 */
@Serializable
data class UserResponse(
    val id: String,
    val email: String,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("email_verified") val emailVerified: Boolean = false,
    @SerialName("auth_provider") val authProvider: String = "email",
    @SerialName("organization_id") val organizationId: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
)

/**
 * AC E9.11.4: Register Response
 *
 * Returned on successful registration.
 *
 * @property user User information
 * @property tokens Token information
 * @property deviceLinked Whether device was linked during registration
 * @property requiresEmailVerification Whether email verification is required
 */
@Serializable
data class RegisterResponse(
    val user: UserResponse,
    val tokens: TokensResponse,
    @SerialName("device_linked") val deviceLinked: Boolean = false,
    @SerialName("requires_email_verification") val requiresEmailVerification: Boolean = true,
)

/**
 * AC E9.11.3: Login Response
 *
 * Returned on successful login.
 *
 * @property user User information
 * @property tokens Token information
 */
@Serializable
data class LoginResponse(val user: UserResponse, val tokens: TokensResponse)

/**
 * AC E9.11.8: Refresh Response
 *
 * Returned on successful token refresh.
 *
 * @property tokens New token information
 */
@Serializable
data class RefreshResponse(val tokens: TokensResponse)

/**
 * Legacy AuthResponse for mock mode compatibility
 *
 * @deprecated Use RegisterResponse, LoginResponse, or RefreshResponse instead
 */
@Serializable
data class AuthResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("expires_in") val expiresIn: Long,
    val user: UserInfo,
)

/**
 * Legacy UserInfo for mock mode compatibility
 *
 * @deprecated Use UserResponse instead
 */
@Serializable
data class UserInfo(
    @SerialName("user_id") val userId: String,
    val email: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("created_at") val createdAt: String,
)

/**
 * Forgot Password Request
 *
 * @property email User email address
 */
@Serializable
data class ForgotPasswordRequest(val email: String)

/**
 * Forgot Password Response
 *
 * @property message Response message
 */
@Serializable
data class ForgotPasswordResponse(val message: String)

/**
 * Reset Password Request
 *
 * @property token Password reset token from email
 * @property newPassword New password
 */
@Serializable
data class ResetPasswordRequest(val token: String, @SerialName("new_password") val newPassword: String)

/**
 * Reset Password Response
 *
 * @property message Response message
 */
@Serializable
data class ResetPasswordResponse(val message: String)

/**
 * Verify Email Request
 *
 * @property token Email verification token
 */
@Serializable
data class VerifyEmailRequest(val token: String)

/**
 * Verify Email Response
 *
 * @property message Response message
 * @property emailVerified Whether email is now verified
 */
@Serializable
data class VerifyEmailResponse(val message: String, @SerialName("email_verified") val emailVerified: Boolean)

/**
 * Request Verification Response
 *
 * @property message Response message
 */
@Serializable
data class RequestVerificationResponse(val message: String)

/**
 * Error Response
 *
 * Standardized error response from auth endpoints.
 *
 * @property error Error code (e.g., "invalid_credentials", "email_already_exists")
 * @property message Human-readable error message
 */
@Serializable
data class AuthErrorResponse(val error: String, val message: String)

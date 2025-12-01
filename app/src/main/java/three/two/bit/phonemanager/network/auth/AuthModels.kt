package three.two.bit.phonemanager.network.auth

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
data class RegisterRequest(
    val email: String,
    val password: String,
    val displayName: String
)

/**
 * AC E9.11.3: Login Request
 *
 * @property email User email address
 * @property password User password
 */
@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

/**
 * AC E9.11.8: Token Refresh Request
 *
 * @property refreshToken The refresh token to exchange for new access token
 */
@Serializable
data class RefreshRequest(
    val refreshToken: String
)

/**
 * AC E9.11.5: OAuth Sign-In Request
 *
 * @property provider OAuth provider (google, apple)
 * @property idToken ID token from OAuth provider
 */
@Serializable
data class OAuthRequest(
    val provider: String,
    val idToken: String
)

// Response Models

/**
 * AC E9.11.3-E9.11.5: Authentication Response
 *
 * Returned on successful login, registration, or OAuth sign-in.
 *
 * @property accessToken JWT access token for API authentication
 * @property refreshToken JWT refresh token for obtaining new access tokens
 * @property expiresIn Seconds until access token expires
 * @property user User information
 */
@Serializable
data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long, // seconds until access token expires
    val user: UserInfo
)

/**
 * User Information
 *
 * Basic user profile information returned with auth response.
 *
 * @property userId Unique user identifier
 * @property email User email address
 * @property displayName User display name
 * @property createdAt ISO 8601 timestamp of account creation
 */
@Serializable
data class UserInfo(
    val userId: String,
    val email: String,
    val displayName: String,
    val createdAt: String // ISO 8601 format
)

/**
 * Error Response
 *
 * Standardized error response from auth endpoints.
 *
 * @property error Error code (e.g., "invalid_credentials", "email_already_exists")
 * @property message Human-readable error message
 */
@Serializable
data class AuthErrorResponse(
    val error: String,
    val message: String
)

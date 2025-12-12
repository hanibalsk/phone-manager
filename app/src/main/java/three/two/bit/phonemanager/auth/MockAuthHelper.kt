package three.two.bit.phonemanager.auth

import kotlinx.coroutines.delay
import three.two.bit.phonemanager.BuildConfig
import three.two.bit.phonemanager.network.auth.AuthResponse
import three.two.bit.phonemanager.network.auth.UserInfo
import timber.log.Timber
import java.util.UUID

/**
 * Story E9.11: Mock Authentication Helper for Development
 *
 * Simulates backend authentication responses for development and testing.
 * Automatically disabled in release builds via BuildConfig.USE_MOCK_AUTH.
 *
 * Usage:
 * - Mock mode controlled by BuildConfig.USE_MOCK_AUTH
 * - Debug builds: true by default, override with USE_MOCK_AUTH_DEBUG in local.properties
 * - Release builds: always false
 * - Simulates network delay (500ms)
 * - Generates realistic mock responses
 * - Validates input format
 */
object MockAuthHelper {

    /**
     * Toggle for mock authentication.
     * - Debug builds: true by default, can be overridden in local.properties
     * - Release builds: always false
     */
    val USE_MOCK_AUTH: Boolean
        get() = BuildConfig.USE_MOCK_AUTH

    /**
     * Mock user registration
     *
     * Simulates backend registration with validation.
     *
     * @throws IllegalArgumentException for invalid input
     */
    suspend fun mockRegister(
        email: String,
        password: String,
        displayName: String
    ): AuthResponse {
        Timber.d("[MOCK] Registering user: $email")

        // Simulate network delay
        delay(500)

        // Basic validation
        require(email.contains("@")) { "Invalid email format" }
        require(password.length >= 8) { "Password too short" }
        require(displayName.length >= 2) { "Display name too short" }

        // Simulate duplicate email check (fail for test@test.com)
        if (email == "test@test.com") {
            throw IllegalStateException("email_already_exists")
        }

        return generateMockAuthResponse(email, displayName)
    }

    /**
     * Mock user login
     *
     * Simulates backend login with credentials check.
     *
     * @throws IllegalStateException for invalid credentials
     */
    suspend fun mockLogin(
        email: String,
        password: String
    ): AuthResponse {
        Timber.d("[MOCK] Logging in user: $email")

        // Simulate network delay
        delay(500)

        // Simulate invalid credentials (fail for wrong@email.com)
        if (email == "wrong@email.com" || password == "wrongpassword") {
            throw IllegalStateException("invalid_credentials")
        }

        // Accept any other email/password combination
        val displayName = email.substringBefore("@").replaceFirstChar { it.uppercase() }
        return generateMockAuthResponse(email, displayName)
    }

    /**
     * Mock OAuth sign-in
     *
     * Simulates OAuth authentication with Google/Apple.
     *
     * @param provider OAuth provider (google, apple)
     * @param idToken Mock ID token (can be any string)
     */
    suspend fun mockOAuthSignIn(
        provider: String,
        idToken: String
    ): AuthResponse {
        Timber.d("[MOCK] OAuth sign-in with provider: $provider")

        // Simulate network delay
        delay(500)

        // Validate provider
        require(provider in listOf("google", "apple")) { "Invalid OAuth provider" }

        // Generate mock response based on provider
        val email = when (provider) {
            "google" -> "user@gmail.com"
            "apple" -> "user@icloud.com"
            else -> "user@example.com"
        }
        val displayName = "OAuth User"

        return generateMockAuthResponse(email, displayName)
    }

    /**
     * Mock token refresh
     *
     * Simulates refreshing access token.
     *
     * @param refreshToken Current refresh token
     */
    suspend fun mockRefreshToken(refreshToken: String): AuthResponse {
        Timber.d("[MOCK] Refreshing token")

        // Simulate network delay
        delay(300)

        // Validate refresh token exists
        require(refreshToken.isNotBlank()) { "Invalid refresh token" }

        // Simulate expired refresh token (fail for "expired")
        if (refreshToken == "expired") {
            throw IllegalStateException("Invalid or expired refresh token")
        }

        // Return new tokens with same user info
        return generateMockAuthResponse(
            email = "refreshed@example.com",
            displayName = "Refreshed User"
        )
    }

    /**
     * Mock logout
     *
     * Simulates server-side logout (always succeeds).
     */
    suspend fun mockLogout() {
        Timber.d("[MOCK] Logging out user")

        // Simulate network delay
        delay(200)

        // Logout always succeeds
    }

    /**
     * Generate mock AuthResponse
     *
     * Creates realistic auth response with JWT-like tokens.
     */
    private fun generateMockAuthResponse(
        email: String,
        displayName: String
    ): AuthResponse {
        val userId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        return AuthResponse(
            accessToken = "mock_access_token_${UUID.randomUUID()}",
            refreshToken = "mock_refresh_token_${UUID.randomUUID()}",
            expiresIn = 3600, // 1 hour
            user = UserInfo(
                userId = userId,
                email = email,
                displayName = displayName,
                createdAt = java.time.Instant.ofEpochMilli(now).toString()
            )
        )
    }

    /**
     * Mock password reset request
     *
     * Simulates sending password reset email.
     */
    suspend fun mockRequestPasswordReset(email: String) {
        Timber.d("[MOCK] Requesting password reset for: $email")

        // Simulate network delay
        delay(500)

        // Basic validation
        require(email.contains("@")) { "Invalid email format" }

        // Always succeeds (email sent)
        Timber.i("[MOCK] Password reset email sent to: $email")
    }
}

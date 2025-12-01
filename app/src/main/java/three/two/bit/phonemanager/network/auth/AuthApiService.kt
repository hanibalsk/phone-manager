package three.two.bit.phonemanager.network.auth

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import three.two.bit.phonemanager.auth.MockAuthHelper
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Story E9.11, Task 3: Authentication API Service
 *
 * Handles all authentication-related HTTP requests.
 *
 * Dependencies: Backend E9.1-E9.10
 * - POST /auth/register - User registration
 * - POST /auth/login - Email/password login
 * - POST /auth/logout - User logout
 * - POST /auth/refresh - Refresh access token
 * - POST /auth/oauth - OAuth sign-in (Google, Apple)
 *
 * Mock Mode: When MockAuthHelper.USE_MOCK_AUTH is true, uses mock responses
 * for development and testing without backend dependency.
 */
@Singleton
class AuthApiService @Inject constructor(
    private val httpClient: HttpClient
) {

    /**
     * AC E9.11.4: Register a new user account
     *
     * @param request Registration details (email, password, displayName)
     * @return AuthResponse with access token, refresh token, and user info
     * @throws Exception on network error or validation failure
     *
     * Error Responses:
     * - 400: Invalid email format, weak password, or missing fields
     * - 409: Email already exists
     */
    suspend fun register(request: RegisterRequest): AuthResponse {
        // Use mock if enabled for development
        if (MockAuthHelper.USE_MOCK_AUTH) {
            return MockAuthHelper.mockRegister(
                request.email,
                request.password,
                request.displayName
            )
        }

        return httpClient.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    /**
     * AC E9.11.3: Login with email and password
     *
     * @param request Login credentials (email, password)
     * @return AuthResponse with access token, refresh token, and user info
     * @throws Exception on network error or invalid credentials
     *
     * Error Responses:
     * - 400: Invalid email format or missing fields
     * - 401: Invalid credentials
     * - 403: Account locked or disabled
     */
    suspend fun login(request: LoginRequest): AuthResponse {
        // Use mock if enabled for development
        if (MockAuthHelper.USE_MOCK_AUTH) {
            return MockAuthHelper.mockLogin(request.email, request.password)
        }

        return httpClient.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    /**
     * AC E9.11.6: Logout current user
     *
     * Invalidates the current access and refresh tokens on the server.
     * Client should clear local tokens regardless of server response.
     *
     * @throws Exception on network error (ignore in client code)
     *
     * Note: Client should always clear tokens locally even if this fails
     */
    suspend fun logout() {
        // Use mock if enabled for development
        if (MockAuthHelper.USE_MOCK_AUTH) {
            MockAuthHelper.mockLogout()
            return
        }

        httpClient.post("/auth/logout") {
            contentType(ContentType.Application.Json)
        }
    }

    /**
     * AC E9.11.8: Refresh access token
     *
     * Exchange refresh token for new access token.
     *
     * @param request Refresh token request
     * @return AuthResponse with new access token and same user info
     * @throws Exception on network error or invalid refresh token
     *
     * Error Responses:
     * - 401: Invalid or expired refresh token (logout required)
     */
    suspend fun refreshToken(request: RefreshRequest): AuthResponse {
        // Use mock if enabled for development
        if (MockAuthHelper.USE_MOCK_AUTH) {
            return MockAuthHelper.mockRefreshToken(request.refreshToken)
        }

        return httpClient.post("/auth/refresh") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    /**
     * AC E9.11.5: OAuth Sign-In (Google, Apple)
     *
     * Authenticate with OAuth provider token.
     *
     * @param request OAuth provider and ID token
     * @return AuthResponse with access token, refresh token, and user info
     * @throws Exception on network error or invalid OAuth token
     *
     * Error Responses:
     * - 400: Invalid provider or missing token
     * - 401: Invalid OAuth token
     * - 403: OAuth provider not enabled
     */
    suspend fun oauthSignIn(request: OAuthRequest): AuthResponse {
        // Use mock if enabled for development
        if (MockAuthHelper.USE_MOCK_AUTH) {
            return MockAuthHelper.mockOAuthSignIn(request.provider, request.idToken)
        }

        return httpClient.post("/auth/oauth") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }
}


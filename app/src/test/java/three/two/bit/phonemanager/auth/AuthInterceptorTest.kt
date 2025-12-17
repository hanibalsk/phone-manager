package three.two.bit.phonemanager.auth

import io.ktor.client.request.HttpRequestBuilder
import io.ktor.http.takeFrom
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import three.two.bit.phonemanager.data.repository.AuthRepository
import three.two.bit.phonemanager.security.SecureStorage
import javax.inject.Provider
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for AuthInterceptor (Story E9.11, Task 2)
 *
 * Tests cover:
 * - AC E9.11.2: Bearer token header addition
 * - AC E9.11.8: X-API-Key fallback for unauthenticated requests
 * - AC E9.11.8: 401 handling with token refresh
 * - AC E9.11.8: Logout on refresh failure
 */
class AuthInterceptorTest {

    private lateinit var secureStorage: SecureStorage
    private lateinit var authRepository: AuthRepository
    private lateinit var authRepositoryProvider: Provider<AuthRepository>
    private lateinit var authInterceptor: AuthInterceptor

    @Before
    fun setup() {
        secureStorage = mockk(relaxed = true)
        authRepository = mockk(relaxed = true)
        authRepositoryProvider = Provider { authRepository }
        authInterceptor = AuthInterceptor(secureStorage, authRepositoryProvider)
    }

    // AC E9.11.2: Bearer Token Tests

    @Test
    fun `configureRequest adds Bearer token when access token exists and not expired`() {
        // Given
        val accessToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test.token"
        every { secureStorage.getAccessToken() } returns accessToken
        every { secureStorage.isTokenExpired() } returns false

        val request = HttpRequestBuilder().apply {
            url.takeFrom("https://api.example.com/devices")
        }

        // When
        authInterceptor.configureRequest(request)

        // Then
        val authHeader = request.headers["Authorization"]
        assertTrue(authHeader == "Bearer $accessToken")
        verify { secureStorage.getAccessToken() }
        verify { secureStorage.isTokenExpired() }
    }

    @Test
    fun `configureRequest adds Bearer token even when expired to trigger refresh`() {
        // Given - token exists but is expired
        val expiredToken = "expired.token"
        every { secureStorage.getAccessToken() } returns expiredToken
        every { secureStorage.isTokenExpired() } returns true

        val request = HttpRequestBuilder().apply {
            url.takeFrom("https://api.example.com/devices")
        }

        // When
        authInterceptor.configureRequest(request)

        // Then - Bearer token should be added; server will return 401 triggering refresh
        val authHeader = request.headers["Authorization"]
        assertTrue(
            authHeader == "Bearer $expiredToken",
            "Expired token should still be sent to trigger 401 refresh flow",
        )
    }

    @Test
    fun `configureRequest does not add Bearer token when no access token`() {
        // Given
        every { secureStorage.getAccessToken() } returns null
        every { secureStorage.getApiKey() } returns null

        val request = HttpRequestBuilder().apply {
            url.takeFrom("https://api.example.com/devices")
        }

        // When
        authInterceptor.configureRequest(request)

        // Then
        val authHeader = request.headers["Authorization"]
        assertTrue(authHeader == null)
    }

    // AC E9.11.8: X-API-Key Fallback Tests (Backward Compatibility)

    @Test
    fun `configureRequest adds X-API-Key header when no access token and API key exists`() {
        // Given
        val apiKey = "test-api-key-123"
        every { secureStorage.getAccessToken() } returns null
        every { secureStorage.getApiKey() } returns apiKey

        val request = HttpRequestBuilder().apply {
            url.takeFrom("https://api.example.com/devices")
        }

        // When
        authInterceptor.configureRequest(request)

        // Then
        val apiKeyHeader = request.headers["X-API-Key"]
        assertTrue(apiKeyHeader == apiKey)
        val authHeader = request.headers["Authorization"]
        assertTrue(authHeader == null)
    }

    @Test
    fun `configureRequest prefers Bearer token over X-API-Key even when token expired`() {
        // Given - expired token exists but so does API key
        val expiredToken = "expired.token"
        val apiKey = "test-api-key-456"
        every { secureStorage.getAccessToken() } returns expiredToken
        every { secureStorage.isTokenExpired() } returns true
        every { secureStorage.getApiKey() } returns apiKey

        val request = HttpRequestBuilder().apply {
            url.takeFrom("https://api.example.com/devices")
        }

        // When
        authInterceptor.configureRequest(request)

        // Then - Bearer token should be added (even expired), NOT API key
        val authHeader = request.headers["Authorization"]
        val apiKeyHeader = request.headers["X-API-Key"]
        assertTrue(authHeader == "Bearer $expiredToken", "Expired Bearer token should be sent")
        assertTrue(apiKeyHeader == null, "X-API-Key should not be added when Bearer token exists")
    }

    @Test
    fun `configureRequest does not add X-API-Key when blank`() {
        // Given
        every { secureStorage.getAccessToken() } returns null
        every { secureStorage.getApiKey() } returns ""

        val request = HttpRequestBuilder().apply {
            url.takeFrom("https://api.example.com/devices")
        }

        // When
        authInterceptor.configureRequest(request)

        // Then
        val apiKeyHeader = request.headers["X-API-Key"]
        assertTrue(apiKeyHeader == null)
    }

    @Test
    fun `configureRequest does not add X-API-Key when null`() {
        // Given
        every { secureStorage.getAccessToken() } returns null
        every { secureStorage.getApiKey() } returns null

        val request = HttpRequestBuilder().apply {
            url.takeFrom("https://api.example.com/devices")
        }

        // When
        authInterceptor.configureRequest(request)

        // Then
        val apiKeyHeader = request.headers["X-API-Key"]
        assertTrue(apiKeyHeader == null)
    }

    @Test
    fun `configureRequest prefers Bearer token over X-API-Key when both available`() {
        // Given
        val accessToken = "valid.access.token"
        val apiKey = "api-key-should-not-be-used"
        every { secureStorage.getAccessToken() } returns accessToken
        every { secureStorage.isTokenExpired() } returns false
        every { secureStorage.getApiKey() } returns apiKey

        val request = HttpRequestBuilder().apply {
            url.takeFrom("https://api.example.com/devices")
        }

        // When
        authInterceptor.configureRequest(request)

        // Then
        val authHeader = request.headers["Authorization"]
        val apiKeyHeader = request.headers["X-API-Key"]
        assertTrue(authHeader == "Bearer $accessToken")
        assertTrue(apiKeyHeader == null, "X-API-Key should not be added when Bearer token is available")
    }

    // AC E9.11.8: Token Refresh Tests

    @Test
    fun `refreshTokenIfNeeded returns true when token refresh succeeds`() = runTest {
        // Given
        coEvery { authRepository.refreshToken() } returns Result.success("new.access.token")

        // When
        val result = authInterceptor.refreshTokenIfNeeded()

        // Then
        assertTrue(result, "Should return true to indicate token was refreshed")
        coVerify { authRepository.refreshToken() }
        coVerify(exactly = 0) { authRepository.logout() }
    }

    @Test
    fun `refreshTokenIfNeeded returns false and calls logout when token refresh fails`() = runTest {
        // Given
        coEvery { authRepository.refreshToken() } returns Result.failure(Exception("Refresh token expired"))

        // When
        val result = authInterceptor.refreshTokenIfNeeded()

        // Then
        assertFalse(result, "Should return false to indicate token refresh failed")
        coVerify { authRepository.refreshToken() }
        coVerify { authRepository.logout() }
    }

    // AC E9.11.8: Helper method tests

    @Test
    fun `getAccessToken returns token from secure storage`() {
        // Given
        val accessToken = "test.access.token"
        every { secureStorage.getAccessToken() } returns accessToken

        // When
        val result = authInterceptor.getAccessToken()

        // Then
        assertTrue(result == accessToken)
    }

    @Test
    fun `isTokenExpired returns value from secure storage`() {
        // Given
        every { secureStorage.isTokenExpired() } returns true

        // When
        val result = authInterceptor.isTokenExpired()

        // Then
        assertTrue(result)
    }

    @Test
    fun `getApiKey returns key from secure storage`() {
        // Given
        val apiKey = "test-api-key"
        every { secureStorage.getApiKey() } returns apiKey

        // When
        val result = authInterceptor.getApiKey()

        // Then
        assertTrue(result == apiKey)
    }
}

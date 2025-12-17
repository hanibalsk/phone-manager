package three.two.bit.phonemanager.data.repository

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import three.two.bit.phonemanager.network.auth.AuthApiService
import three.two.bit.phonemanager.network.auth.LoginResponse
import three.two.bit.phonemanager.network.auth.RefreshResponse
import three.two.bit.phonemanager.network.auth.RegisterResponse
import three.two.bit.phonemanager.network.auth.TokensResponse
import three.two.bit.phonemanager.network.auth.UserResponse
import three.two.bit.phonemanager.security.SecureStorage
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for AuthRepository (Story E9.11, Task 4)
 *
 * Tests cover:
 * - AC E9.11.3: Login with email/password
 * - AC E9.11.4: User registration
 * - AC E9.11.5: OAuth sign-in
 * - AC E9.11.6: Logout and session management
 * - AC E9.11.8: Token refresh
 */
class AuthRepositoryTest {

    private lateinit var authApiService: AuthApiService
    private lateinit var secureStorage: SecureStorage
    private lateinit var authRepository: AuthRepositoryImpl

    private val testUserResponse = UserResponse(
        id = "user-123",
        email = "test@example.com",
        displayName = "Test User",
        createdAt = "2025-12-01T10:00:00Z",
    )

    private val testTokensResponse = TokensResponse(
        accessToken = "access.token.123",
        refreshToken = "refresh.token.456",
        tokenType = "Bearer",
        expiresIn = 3600L, // 1 hour
    )

    private val testRegisterResponse = RegisterResponse(
        user = testUserResponse,
        tokens = testTokensResponse,
    )

    private val testLoginResponse = LoginResponse(
        user = testUserResponse,
        tokens = testTokensResponse,
    )

    private val testRefreshResponse = RefreshResponse(
        tokens = testTokensResponse,
    )

    @Before
    fun setup() {
        authApiService = mockk(relaxed = true)
        secureStorage = mockk(relaxed = true)
        authRepository = AuthRepositoryImpl(authApiService, secureStorage)
    }

    // AC E9.11.4: Registration Tests

    @Test
    fun `register calls API with correct request`() = runTest {
        // Given
        coEvery { authApiService.register(any()) } returns testRegisterResponse

        // When
        val result = authRepository.register(
            email = "new@example.com",
            password = "Password123!",
            displayName = "New User",
        )

        // Then
        assertTrue(result.isSuccess)
        coVerify {
            authApiService.register(
                match { request ->
                    request.email == "new@example.com" &&
                        request.password == "Password123!" &&
                        request.displayName == "New User"
                },
            )
        }
    }

    @Test
    fun `register stores tokens on success`() = runTest {
        // Given
        coEvery { authApiService.register(any()) } returns testRegisterResponse

        // When
        authRepository.register("test@example.com", "Password123!", "Test User")

        // Then
        verify { secureStorage.saveAccessToken(testTokensResponse.accessToken) }
        verify { secureStorage.saveRefreshToken(testTokensResponse.refreshToken) }
        verify { secureStorage.saveTokenExpiryTime(any()) }
    }

    @Test
    fun `register returns user on success`() = runTest {
        // Given
        coEvery { authApiService.register(any()) } returns testRegisterResponse

        // When
        val result = authRepository.register("test@example.com", "Password123!", "Test User")

        // Then
        assertTrue(result.isSuccess)
        val user = result.getOrNull()!!
        assertEquals("user-123", user.userId)
        assertEquals("test@example.com", user.email)
        assertEquals("Test User", user.displayName)
    }

    @Test
    fun `register updates current user flow on success`() = runTest {
        // Given
        coEvery { authApiService.register(any()) } returns testRegisterResponse

        // When
        authRepository.register("test@example.com", "Password123!", "Test User")

        // Then
        val currentUser = authRepository.currentUser.first()
        assertEquals("user-123", currentUser?.userId)
    }

    @Test
    fun `register returns failure on API error`() = runTest {
        // Given
        coEvery { authApiService.register(any()) } throws RuntimeException("email_already_exists")

        // When
        val result = authRepository.register("existing@example.com", "Password123!", "Test User")

        // Then
        assertTrue(result.isFailure)
        assertEquals("email_already_exists", result.exceptionOrNull()?.message)
        verify(exactly = 0) { secureStorage.saveAccessToken(any()) }
    }

    // AC E9.11.3: Login Tests

    @Test
    fun `login calls API with correct request`() = runTest {
        // Given
        coEvery { authApiService.login(any()) } returns testLoginResponse

        // When
        val result = authRepository.login("test@example.com", "Password123!")

        // Then
        assertTrue(result.isSuccess)
        coVerify {
            authApiService.login(
                match { request ->
                    request.email == "test@example.com" &&
                        request.password == "Password123!"
                },
            )
        }
    }

    @Test
    fun `login stores tokens on success`() = runTest {
        // Given
        coEvery { authApiService.login(any()) } returns testLoginResponse

        // When
        authRepository.login("test@example.com", "Password123!")

        // Then
        verify { secureStorage.saveAccessToken(testTokensResponse.accessToken) }
        verify { secureStorage.saveRefreshToken(testTokensResponse.refreshToken) }
        verify { secureStorage.saveTokenExpiryTime(any()) }
    }

    @Test
    fun `login returns user on success`() = runTest {
        // Given
        coEvery { authApiService.login(any()) } returns testLoginResponse

        // When
        val result = authRepository.login("test@example.com", "Password123!")

        // Then
        assertTrue(result.isSuccess)
        val user = result.getOrNull()!!
        assertEquals("user-123", user.userId)
        assertEquals("test@example.com", user.email)
    }

    @Test
    fun `login returns failure on invalid credentials`() = runTest {
        // Given
        coEvery { authApiService.login(any()) } throws RuntimeException("invalid_credentials")

        // When
        val result = authRepository.login("test@example.com", "wrong-password")

        // Then
        assertTrue(result.isFailure)
        assertEquals("invalid_credentials", result.exceptionOrNull()?.message)
    }

    // AC E9.11.5: OAuth Sign-In Tests

    @Test
    fun `oauthLogin calls API with correct request`() = runTest {
        // Given
        coEvery { authApiService.oauthSignIn(any()) } returns testLoginResponse

        // When
        val result = authRepository.oauthLogin("google", "id.token.from.google")

        // Then
        assertTrue(result.isSuccess)
        coVerify {
            authApiService.oauthSignIn(
                match { request ->
                    request.provider == "google" &&
                        request.idToken == "id.token.from.google"
                },
            )
        }
    }

    @Test
    fun `oauthLogin stores tokens on success`() = runTest {
        // Given
        coEvery { authApiService.oauthSignIn(any()) } returns testLoginResponse

        // When
        authRepository.oauthLogin("apple", "id.token.from.apple")

        // Then
        verify { secureStorage.saveAccessToken(testTokensResponse.accessToken) }
        verify { secureStorage.saveRefreshToken(testTokensResponse.refreshToken) }
    }

    @Test
    fun `oauthLogin returns failure on invalid token`() = runTest {
        // Given
        coEvery { authApiService.oauthSignIn(any()) } throws RuntimeException("oauth_failed")

        // When
        val result = authRepository.oauthLogin("google", "invalid.token")

        // Then
        assertTrue(result.isFailure)
        assertEquals("oauth_failed", result.exceptionOrNull()?.message)
    }

    // AC E9.11.6: Logout Tests

    @Test
    fun `logout clears tokens even if server logout fails`() = runTest {
        // Given
        every { secureStorage.getRefreshToken() } returns "test.refresh.token"
        coEvery { authApiService.logout(any(), any()) } throws RuntimeException("Network error")

        // When
        val result = authRepository.logout()

        // Then
        assertTrue(result.isSuccess)
        verify { secureStorage.clearTokens() }
    }

    @Test
    fun `logout clears current user`() = runTest {
        // Given - first login
        coEvery { authApiService.login(any()) } returns testLoginResponse
        authRepository.login("test@example.com", "Password123!")

        // Verify user is set
        val userBeforeLogout = authRepository.getCurrentUser()
        assertEquals("user-123", userBeforeLogout?.userId)

        // When
        authRepository.logout()

        // Then
        val userAfterLogout = authRepository.getCurrentUser()
        assertNull(userAfterLogout)
    }

    @Test
    fun `logout calls server logout endpoint`() = runTest {
        // Given
        val refreshToken = "test.refresh.token"
        every { secureStorage.getRefreshToken() } returns refreshToken
        coEvery { authApiService.logout(any(), any()) } just runs

        // When
        authRepository.logout()

        // Then
        coVerify { authApiService.logout(refreshToken, false) }
        verify { secureStorage.clearTokens() }
    }

    // AC E9.11.8: Token Refresh Tests

    @Test
    fun `refreshToken calls API with refresh token`() = runTest {
        // Given
        val refreshToken = "existing.refresh.token"
        every { secureStorage.getRefreshToken() } returns refreshToken
        coEvery { authApiService.refreshToken(any()) } returns testRefreshResponse

        // When
        val result = authRepository.refreshToken()

        // Then
        assertTrue(result.isSuccess)
        coVerify {
            authApiService.refreshToken(
                match { request ->
                    request.refreshToken == refreshToken
                },
            )
        }
    }

    @Test
    fun `refreshToken stores new tokens on success`() = runTest {
        // Given
        every { secureStorage.getRefreshToken() } returns "old.refresh.token"
        val newRefreshResponse = RefreshResponse(
            tokens = TokensResponse(
                accessToken = "new.access.token",
                refreshToken = "new.refresh.token",
                tokenType = "Bearer",
                expiresIn = 3600L,
            ),
        )
        coEvery { authApiService.refreshToken(any()) } returns newRefreshResponse

        // When
        val result = authRepository.refreshToken()

        // Then
        assertTrue(result.isSuccess)
        assertEquals("new.access.token", result.getOrNull())
        verify { secureStorage.saveAccessToken("new.access.token") }
        verify { secureStorage.saveRefreshToken("new.refresh.token") }
    }

    @Test
    fun `refreshToken returns failure when no refresh token available`() = runTest {
        // Given
        every { secureStorage.getRefreshToken() } returns null

        // When
        val result = authRepository.refreshToken()

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is SessionExpiredException)
    }

    @Test
    fun `refreshToken returns failure when API call fails`() = runTest {
        // Given
        every { secureStorage.getRefreshToken() } returns "expired.refresh.token"
        coEvery { authApiService.refreshToken(any()) } throws RuntimeException("Refresh token expired")

        // When
        val result = authRepository.refreshToken()

        // Then
        assertTrue(result.isFailure)
    }

    // Session Management Tests

    @Test
    fun `isLoggedIn returns true when authenticated`() {
        // Given
        every { secureStorage.isAuthenticated() } returns true

        // When
        val result = authRepository.isLoggedIn()

        // Then
        assertTrue(result)
    }

    @Test
    fun `isLoggedIn returns false when not authenticated`() {
        // Given
        every { secureStorage.isAuthenticated() } returns false

        // When
        val result = authRepository.isLoggedIn()

        // Then
        assertFalse(result)
    }

    @Test
    fun `getCurrentUser returns null when not logged in`() {
        // Given - no login performed

        // When
        val result = authRepository.getCurrentUser()

        // Then
        assertNull(result)
    }

    @Test
    fun `getCurrentUser returns user after login`() = runTest {
        // Given
        coEvery { authApiService.login(any()) } returns testLoginResponse
        authRepository.login("test@example.com", "Password123!")

        // When
        val result = authRepository.getCurrentUser()

        // Then
        assertEquals("user-123", result?.userId)
        assertEquals("test@example.com", result?.email)
    }

    // Token Expiry Time Calculation Tests

    @Test
    fun `storeTokens calculates expiry time correctly`() = runTest {
        // Given
        val loginResponse = LoginResponse(
            user = testUserResponse,
            tokens = TokensResponse(
                accessToken = "access.token",
                refreshToken = "refresh.token",
                tokenType = "Bearer",
                expiresIn = 3600L,
            ),
        )
        coEvery { authApiService.login(any()) } returns loginResponse

        // When
        authRepository.login("test@example.com", "Password123!")

        // Then
        verify {
            secureStorage.saveTokenExpiryTime(
                match { expiryTime ->
                    val expectedMinExpiry = System.currentTimeMillis() + (3600 * 1000) - 1000
                    val expectedMaxExpiry = System.currentTimeMillis() + (3600 * 1000) + 1000
                    expiryTime in expectedMinExpiry..expectedMaxExpiry
                },
            )
        }
    }
}

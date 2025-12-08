package three.two.bit.phonemanager.ui.auth

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Instant
import org.junit.After
import org.junit.Before
import org.junit.Test
import three.two.bit.phonemanager.data.repository.AuthRepository
import three.two.bit.phonemanager.data.repository.ConfigRepository
import three.two.bit.phonemanager.domain.auth.User
import three.two.bit.phonemanager.network.DeviceApiService
import three.two.bit.phonemanager.network.models.LinkedDeviceInfo
import three.two.bit.phonemanager.network.models.LinkedDeviceResponse
import three.two.bit.phonemanager.security.SecureStorage
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for AuthViewModel (Story E9.11, Task 7)
 *
 * Tests cover:
 * - AC E9.11.3: Login validation and API calls
 * - AC E9.11.4: Registration validation and API calls
 * - AC E9.11.5: OAuth sign-in
 * - AC E9.11.6: Logout
 * - AC E9.11.7: Input validation and error handling
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private lateinit var authRepository: AuthRepository
    private lateinit var configRepository: ConfigRepository
    private lateinit var deviceApiService: DeviceApiService
    private lateinit var secureStorage: SecureStorage
    private lateinit var viewModel: AuthViewModel
    private val testDispatcher = StandardTestDispatcher()

    private val testUser = User(
        userId = "user-123",
        email = "test@example.com",
        displayName = "Test User",
        createdAt = Instant.parse("2025-12-01T10:00:00Z")
    )

    private val testLinkedDeviceResponse = LinkedDeviceResponse(
        device = LinkedDeviceInfo(
            id = 1L,
            deviceUuid = "test-device-uuid",
            displayName = "Test Device",
            ownerUserId = "user-123",
            isPrimary = false,
            linkedAt = "2025-12-01T10:00:00Z",
        ),
        linked = true,
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        authRepository = mockk(relaxed = true)
        configRepository = mockk(relaxed = true)
        deviceApiService = mockk(relaxed = true)
        secureStorage = mockk(relaxed = true)

        // Mock secureStorage to return access token and device ID for auto-link functionality
        every { secureStorage.getAccessToken() } returns "test-access-token"
        every { secureStorage.getDeviceId() } returns "test-device-uuid"

        // Mock configRepository.config to return empty flow (config not loaded scenario)
        every { configRepository.config } returns kotlinx.coroutines.flow.flowOf(null)
        coEvery { configRepository.fetchConfig() } returns Result.failure(IllegalStateException("config unavailable"))

        // Mock deviceApiService.linkDevice to return a proper Result (fixes ClassCastException)
        coEvery {
            deviceApiService.linkDevice(any(), any(), any(), any(), any())
        } returns Result.success(testLinkedDeviceResponse)

        viewModel = AuthViewModel(authRepository, configRepository, deviceApiService, secureStorage)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // AC E9.11.7: Login Validation Tests

    @Test
    fun `login with invalid email sets email error`() = runTest {
        // When
        viewModel.login("invalid-email", "Password123!")
        advanceUntilIdle()

        // Then
        assertEquals("Please enter a valid email address", viewModel.emailError.value)
        coVerify(exactly = 0) { authRepository.login(any(), any()) }
    }

    @Test
    fun `login with empty email sets email error`() = runTest {
        // When
        viewModel.login("", "Password123!")
        advanceUntilIdle()

        // Then
        assertEquals("Please enter a valid email address", viewModel.emailError.value)
    }

    @Test
    fun `login with blank password sets password error`() = runTest {
        // When
        viewModel.login("test@example.com", "")
        advanceUntilIdle()

        // Then
        assertEquals("Password is required", viewModel.passwordError.value)
    }

    @Test
    fun `login with valid input calls repository`() = runTest {
        // Given
        coEvery { authRepository.login(any(), any()) } returns Result.success(testUser)

        // When
        viewModel.login("test@example.com", "Password123!")
        advanceUntilIdle()

        // Then
        coVerify { authRepository.login("test@example.com", "Password123!") }
    }

    @Test
    fun `login sets loading state during API call`() = runTest {
        // Given
        coEvery { authRepository.login(any(), any()) } returns Result.success(testUser)

        // When
        viewModel.login("test@example.com", "Password123!")

        // Then - should be loading before advanceUntilIdle
        assertEquals(AuthUiState.Loading, viewModel.uiState.value)

        advanceUntilIdle()
    }

    @Test
    fun `login sets success state on successful login`() = runTest {
        // Given
        coEvery { authRepository.login(any(), any()) } returns Result.success(testUser)

        // When
        viewModel.login("test@example.com", "Password123!")
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertTrue(state is AuthUiState.Success)
        assertEquals(testUser, (state as AuthUiState.Success).user)
    }

    @Test
    fun `login sets error state on invalid credentials`() = runTest {
        // Given
        coEvery { authRepository.login(any(), any()) } returns Result.failure(
            RuntimeException("invalid_credentials")
        )

        // When
        viewModel.login("test@example.com", "wrong-password")
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertTrue(state is AuthUiState.Error)
        assertEquals("Invalid email or password", (state as AuthUiState.Error).message)
    }

    // AC E9.11.7: Registration Validation Tests

    @Test
    fun `register with invalid email sets email error`() = runTest {
        // When
        viewModel.register("invalid-email", "Password123!", "Test User")
        advanceUntilIdle()

        // Then
        assertEquals("Please enter a valid email address", viewModel.emailError.value)
        coVerify(exactly = 0) { authRepository.register(any(), any(), any()) }
    }

    @Test
    fun `register with short display name sets display name error`() = runTest {
        // When
        viewModel.register("test@example.com", "Password123!", "A")
        advanceUntilIdle()

        // Then
        assertEquals("Display name must be at least 2 characters", viewModel.displayNameError.value)
    }

    @Test
    fun `register with empty display name sets display name error`() = runTest {
        // When
        viewModel.register("test@example.com", "Password123!", "")
        advanceUntilIdle()

        // Then
        assertEquals("Display name must be at least 2 characters", viewModel.displayNameError.value)
    }

    @Test
    fun `register with weak password sets password error`() = runTest {
        // When - password without uppercase
        viewModel.register("test@example.com", "password123!", "Test User")
        advanceUntilIdle()

        // Then
        assertEquals(
            "Password must be at least 8 characters with 1 uppercase, 1 number, and 1 special character",
            viewModel.passwordError.value
        )
    }

    @Test
    fun `register with password without number sets password error`() = runTest {
        // When
        viewModel.register("test@example.com", "Password!", "Test User")
        advanceUntilIdle()

        // Then
        assertEquals(
            "Password must be at least 8 characters with 1 uppercase, 1 number, and 1 special character",
            viewModel.passwordError.value
        )
    }

    @Test
    fun `register with password without special char sets password error`() = runTest {
        // When
        viewModel.register("test@example.com", "Password123", "Test User")
        advanceUntilIdle()

        // Then
        assertEquals(
            "Password must be at least 8 characters with 1 uppercase, 1 number, and 1 special character",
            viewModel.passwordError.value
        )
    }

    @Test
    fun `register with short password sets password error`() = runTest {
        // When
        viewModel.register("test@example.com", "Pa1!", "Test User")
        advanceUntilIdle()

        // Then
        assertEquals(
            "Password must be at least 8 characters with 1 uppercase, 1 number, and 1 special character",
            viewModel.passwordError.value
        )
    }

    @Test
    fun `register with valid input calls repository`() = runTest {
        // Given
        coEvery { authRepository.register(any(), any(), any()) } returns Result.success(testUser)

        // When
        viewModel.register("test@example.com", "Password123!", "Test User")
        advanceUntilIdle()

        // Then
        coVerify { authRepository.register("test@example.com", "Password123!", "Test User") }
    }

    @Test
    fun `register sets success state on successful registration`() = runTest {
        // Given
        coEvery { authRepository.register(any(), any(), any()) } returns Result.success(testUser)

        // When
        viewModel.register("test@example.com", "Password123!", "Test User")
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertTrue(state is AuthUiState.Success)
        assertEquals(testUser, (state as AuthUiState.Success).user)
    }

    @Test
    fun `register sets error state on email already exists`() = runTest {
        // Given
        coEvery { authRepository.register(any(), any(), any()) } returns Result.failure(
            RuntimeException("email_already_exists")
        )

        // When
        viewModel.register("existing@example.com", "Password123!", "Test User")
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertTrue(state is AuthUiState.Error)
        assertEquals("An account with this email already exists", (state as AuthUiState.Error).message)
    }

    // AC E9.11.5: OAuth Sign-In Tests

    @Test
    fun `oauthSignIn calls repository with provider and token`() = runTest {
        // Given
        coEvery { authRepository.oauthLogin(any(), any()) } returns Result.success(testUser)

        // When
        viewModel.oauthSignIn("google", "id.token.from.google")
        advanceUntilIdle()

        // Then
        coVerify { authRepository.oauthLogin("google", "id.token.from.google") }
    }

    @Test
    fun `oauthSignIn sets loading state during API call`() = runTest {
        // Given
        coEvery { authRepository.oauthLogin(any(), any()) } returns Result.success(testUser)

        // When
        viewModel.oauthSignIn("apple", "id.token.from.apple")

        // Then
        assertEquals(AuthUiState.Loading, viewModel.uiState.value)

        advanceUntilIdle()
    }

    @Test
    fun `oauthSignIn sets success state on success`() = runTest {
        // Given
        coEvery { authRepository.oauthLogin(any(), any()) } returns Result.success(testUser)

        // When
        viewModel.oauthSignIn("google", "id.token.from.google")
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertTrue(state is AuthUiState.Success)
    }

    @Test
    fun `oauthSignIn sets error state on failure`() = runTest {
        // Given
        coEvery { authRepository.oauthLogin(any(), any()) } returns Result.failure(
            RuntimeException("oauth_failed")
        )

        // When
        viewModel.oauthSignIn("google", "invalid.token")
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertTrue(state is AuthUiState.Error)
        assertEquals("Sign-in with this provider failed. Please try again.", (state as AuthUiState.Error).message)
    }

    // AC E9.11.6: Logout Tests

    @Test
    fun `logout calls repository logout`() = runTest {
        // When
        viewModel.logout()
        advanceUntilIdle()

        // Then
        coVerify { authRepository.logout() }
    }

    @Test
    fun `logout sets idle state`() = runTest {
        // Given - first set to success state
        coEvery { authRepository.login(any(), any()) } returns Result.success(testUser)
        viewModel.login("test@example.com", "Password123!")
        advanceUntilIdle()

        // When
        viewModel.logout()
        advanceUntilIdle()

        // Then
        assertEquals(AuthUiState.Idle, viewModel.uiState.value)
    }

    // State Reset Tests

    @Test
    fun `resetState clears UI state to idle`() = runTest {
        // Given - set error state
        coEvery { authRepository.login(any(), any()) } returns Result.failure(
            RuntimeException("error")
        )
        viewModel.login("test@example.com", "Password123!")
        advanceUntilIdle()

        // When
        viewModel.resetState()

        // Then
        assertEquals(AuthUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun `resetState clears validation errors`() = runTest {
        // Given - trigger validation error
        viewModel.login("invalid-email", "")
        advanceUntilIdle()

        // When
        viewModel.resetState()

        // Then
        assertNull(viewModel.emailError.value)
        assertNull(viewModel.passwordError.value)
        assertNull(viewModel.displayNameError.value)
    }

    // Error Message Mapping Tests

    @Test
    fun `error message for network error`() = runTest {
        // Given
        coEvery { authRepository.login(any(), any()) } returns Result.failure(
            RuntimeException("network_error")
        )

        // When
        viewModel.login("test@example.com", "Password123!")
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value as AuthUiState.Error
        assertEquals("Unable to connect to server. Please check your internet connection.", state.message)
    }

    @Test
    fun `error message for weak password`() = runTest {
        // Given
        coEvery { authRepository.register(any(), any(), any()) } returns Result.failure(
            RuntimeException("weak_password")
        )

        // When
        viewModel.register("test@example.com", "Password123!", "Test User")
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value as AuthUiState.Error
        assertEquals("Password does not meet security requirements", state.message)
    }

    @Test
    fun `error message for account locked`() = runTest {
        // Given
        coEvery { authRepository.login(any(), any()) } returns Result.failure(
            RuntimeException("account_locked")
        )

        // When
        viewModel.login("test@example.com", "Password123!")
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value as AuthUiState.Error
        assertEquals("Your account has been locked. Please contact support.", state.message)
    }

    @Test
    fun `error message for account disabled`() = runTest {
        // Given
        coEvery { authRepository.login(any(), any()) } returns Result.failure(
            RuntimeException("account_disabled")
        )

        // When
        viewModel.login("test@example.com", "Password123!")
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value as AuthUiState.Error
        assertEquals("Your account has been disabled. Please contact support.", state.message)
    }

    @Test
    fun `error message for unknown error`() = runTest {
        // Given
        coEvery { authRepository.login(any(), any()) } returns Result.failure(
            RuntimeException("Some unexpected error")
        )

        // When
        viewModel.login("test@example.com", "Password123!")
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value as AuthUiState.Error
        assertEquals("Some unexpected error", state.message)
    }

    // Email Validation Edge Cases

    @Test
    fun `valid email formats are accepted`() = runTest {
        // Given
        coEvery { authRepository.login(any(), any()) } returns Result.success(testUser)

        // Test various valid email formats
        val validEmails = listOf(
            "test@example.com",
            "user.name@domain.org",
            "user-name@domain.co.uk",
            "user_name@domain.io",
            "test123@example.com"
        )

        for (email in validEmails) {
            // When
            viewModel.login(email, "Password123!")
            advanceUntilIdle()

            // Then - no email error should be set
            assertNull(viewModel.emailError.value, "Email '$email' should be valid")
            viewModel.resetState()
        }
    }

    // Password Validation Edge Cases

    @Test
    fun `valid password with all requirements is accepted`() = runTest {
        // Given
        coEvery { authRepository.register(any(), any(), any()) } returns Result.success(testUser)

        // When
        viewModel.register("test@example.com", "Password123!", "Test User")
        advanceUntilIdle()

        // Then
        assertNull(viewModel.passwordError.value)
    }

    @Test
    fun `password with exactly 8 characters is accepted`() = runTest {
        // Given
        coEvery { authRepository.register(any(), any(), any()) } returns Result.success(testUser)

        // When - exactly 8 chars with all requirements
        viewModel.register("test@example.com", "Pass12!a", "Test User")
        advanceUntilIdle()

        // Then
        assertNull(viewModel.passwordError.value)
    }
}

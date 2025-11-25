package three.two.bit.phonemanager.ui.registration

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import three.two.bit.phonemanager.data.repository.DeviceRepository
import three.two.bit.phonemanager.network.NetworkException
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for RegistrationViewModel
 *
 * Story E1.1: Tests device registration flow and validation
 * Coverage target: > 80%
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RegistrationViewModelTest {

    private lateinit var viewModel: RegistrationViewModel
    private lateinit var deviceRepository: DeviceRepository

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        deviceRepository = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has empty fields and invalid form`() = runTest {
        viewModel = createViewModel()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("", state.displayName)
            assertEquals("", state.groupId)
            assertFalse(state.isFormValid)
            assertFalse(state.isLoading)
            assertNull(state.error)
            assertFalse(state.isRegistered)
        }
    }

    @Test
    fun `updateDisplayName updates state and validates`() = runTest {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateDisplayName("Martin's Phone")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Martin's Phone", state.displayName)
            assertNull(state.displayNameError)
        }
    }

    @Test
    fun `updateGroupId updates state and validates`() = runTest {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateGroupId("family")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("family", state.groupId)
            assertNull(state.groupIdError)
        }
    }

    @Test
    fun `validateForm fails for empty displayName`() = runTest {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateDisplayName("")
        viewModel.updateGroupId("family")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Display name is required", state.displayNameError)
            assertFalse(state.isFormValid)
        }
    }

    @Test
    fun `validateForm fails for displayName less than 2 chars`() = runTest {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateDisplayName("A")
        viewModel.updateGroupId("family")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Display name must be at least 2 characters", state.displayNameError)
            assertFalse(state.isFormValid)
        }
    }

    @Test
    fun `validateForm fails for displayName over 50 chars`() = runTest {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateDisplayName("A".repeat(51))
        viewModel.updateGroupId("family")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Display name must be 50 characters or less", state.displayNameError)
            assertFalse(state.isFormValid)
        }
    }

    @Test
    fun `validateForm fails for empty groupId`() = runTest {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateDisplayName("Martin's Phone")
        viewModel.updateGroupId("")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Group ID is required", state.groupIdError)
            assertFalse(state.isFormValid)
        }
    }

    @Test
    fun `validateForm fails for groupId with invalid characters`() = runTest {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateDisplayName("Martin's Phone")
        viewModel.updateGroupId("family@home")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Group ID can only contain letters, numbers, and hyphens", state.groupIdError)
            assertFalse(state.isFormValid)
        }
    }

    @Test
    fun `validateForm fails for groupId less than 2 chars`() = runTest {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateDisplayName("Martin's Phone")
        viewModel.updateGroupId("f")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Group ID must be at least 2 characters", state.groupIdError)
            assertFalse(state.isFormValid)
        }
    }

    @Test
    fun `validateForm succeeds for valid inputs`() = runTest {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateDisplayName("Martin's Phone")
        viewModel.updateGroupId("family")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertNull(state.displayNameError)
            assertNull(state.groupIdError)
            assertTrue(state.isFormValid)
        }
    }

    @Test
    fun `validateForm accepts groupId with hyphens`() = runTest {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateDisplayName("Martin's Phone")
        viewModel.updateGroupId("my-family-2024")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertNull(state.groupIdError)
            assertTrue(state.isFormValid)
        }
    }

    @Test
    fun `register calls repository and updates state on success`() = runTest {
        coEvery { deviceRepository.registerDevice(any(), any()) } returns Result.success(Unit)

        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateDisplayName("Martin's Phone")
        viewModel.updateGroupId("family")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.register()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { deviceRepository.registerDevice("Martin's Phone", "family") }

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.isRegistered)
            assertFalse(state.isLoading)
            assertNull(state.error)
        }
    }

    @Test
    fun `register shows loading state during request`() = runTest {
        coEvery { deviceRepository.registerDevice(any(), any()) } coAnswers {
            kotlinx.coroutines.delay(100)
            Result.success(Unit)
        }

        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateDisplayName("Martin's Phone")
        viewModel.updateGroupId("family")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.register()
        // Advance just enough for loading state
        testDispatcher.scheduler.advanceTimeBy(10)

        assertTrue(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `register shows error on network failure`() = runTest {
        coEvery {
            deviceRepository.registerDevice(any(), any())
        } returns Result.failure(NetworkException("No network connection available"))

        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateDisplayName("Martin's Phone")
        viewModel.updateGroupId("family")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.register()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isRegistered)
            assertFalse(state.isLoading)
            assertEquals("No internet connection. Please check your network.", state.error)
        }
    }

    @Test
    fun `register shows generic error on unknown failure`() = runTest {
        coEvery {
            deviceRepository.registerDevice(any(), any())
        } returns Result.failure(RuntimeException("Server error"))

        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateDisplayName("Martin's Phone")
        viewModel.updateGroupId("family")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.register()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isRegistered)
            assertEquals("Server error", state.error)
        }
    }

    @Test
    fun `register does not call repository when form is invalid`() = runTest {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateDisplayName("A") // Too short
        viewModel.updateGroupId("family")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.register()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { deviceRepository.registerDevice(any(), any()) }
    }

    @Test
    fun `clearError clears the error state`() = runTest {
        coEvery {
            deviceRepository.registerDevice(any(), any())
        } returns Result.failure(RuntimeException("Test error"))

        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateDisplayName("Martin's Phone")
        viewModel.updateGroupId("family")
        viewModel.register()
        testDispatcher.scheduler.advanceUntilIdle()

        // Error should be set
        assertEquals("Test error", viewModel.uiState.value.error)

        // Clear error
        viewModel.clearError()
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.uiState.value.error)
    }

    private fun createViewModel() = RegistrationViewModel(
        deviceRepository = deviceRepository,
    )
}

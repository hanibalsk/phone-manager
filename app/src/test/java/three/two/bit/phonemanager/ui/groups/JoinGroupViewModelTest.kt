package three.two.bit.phonemanager.ui.groups

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Before
import org.junit.Test
import three.two.bit.phonemanager.data.repository.AuthRepository
import three.two.bit.phonemanager.data.repository.GroupRepository
import three.two.bit.phonemanager.domain.model.GroupPreview
import three.two.bit.phonemanager.domain.model.GroupRole
import three.two.bit.phonemanager.domain.model.InviteValidationResult
import three.two.bit.phonemanager.domain.model.JoinGroupResult
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

/**
 * Story E11.9 Task 14: Unit tests for JoinGroupViewModel
 *
 * Tests:
 * - AC E11.9.4 (Join with invite code)
 * - AC E11.9.5 (QR code scanning)
 * - AC E11.9.8 (Deep link handling)
 */
@OptIn(ExperimentalCoroutinesApi::class)
class JoinGroupViewModelTest {

    private lateinit var groupRepository: GroupRepository
    private lateinit var authRepository: AuthRepository
    private lateinit var savedStateHandle: SavedStateHandle
    private lateinit var viewModel: JoinGroupViewModel

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        groupRepository = mockk(relaxed = true)
        authRepository = mockk(relaxed = true)
        savedStateHandle = SavedStateHandle()
    }

    // AC E11.9.4: Join with Invite Code Tests

    @Test
    fun `setInviteCode normalizes input to uppercase`() = runTest {
        // Given
        viewModel = JoinGroupViewModel(groupRepository, authRepository, savedStateHandle)

        // When
        viewModel.setInviteCode("abcd1234")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals("ABCD1234", viewModel.inviteCode.value)
    }

    @Test
    fun `setInviteCode limits to 11 characters for dash format support`() = runTest {
        // Given
        viewModel = JoinGroupViewModel(groupRepository, authRepository, savedStateHandle)

        // When - normalizeCode takes max 11 chars to support XXX-XXX-XXX format
        viewModel.setInviteCode("ABCD1234EXTRA123")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - limited to 11 characters
        assertEquals("ABCD1234EXT", viewModel.inviteCode.value)
    }

    @Test
    fun `setInviteCode filters non-alphanumeric characters but keeps dashes`() = runTest {
        // Given
        viewModel = JoinGroupViewModel(groupRepository, authRepository, savedStateHandle)

        // When - normalizeCode keeps alphanumeric AND dashes, removes other special chars
        viewModel.setInviteCode("AB-CD_12!34")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - dashes are preserved, underscore and ! are removed
        assertEquals("AB-CD1234", viewModel.inviteCode.value)
    }

    @Test
    fun `validateCode shows error for short code`() = runTest {
        // Given
        viewModel = JoinGroupViewModel(groupRepository, authRepository, savedStateHandle)
        viewModel.setInviteCode("ABC")
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.validateCode()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertTrue(state is JoinGroupUiState.Error)
        assertEquals("Invalid code format", (state as JoinGroupUiState.Error).message)
    }

    @Test
    fun `validateCode calls repository with valid code`() = runTest {
        // Given - use XXX-XXX-XXX format as required by isValidCodeFormat
        val inviteCode = "ABC-DEF-123"
        val preview = GroupPreview("group-1", "Family", 5)
        val result = InviteValidationResult(true, preview, null)
        coEvery { groupRepository.validateInviteCode(inviteCode) } returns Result.success(result)

        viewModel = JoinGroupViewModel(groupRepository, authRepository, savedStateHandle)
        viewModel.setInviteCode(inviteCode)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.validateCode()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { groupRepository.validateInviteCode(inviteCode) }
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is JoinGroupUiState.PreviewReady)
            assertEquals("Family", (state as JoinGroupUiState.PreviewReady).group.name)
        }
    }

    @Test
    fun `validateCode shows error for invalid code`() = runTest {
        // Given - use XXX-XXX-XXX format as required by isValidCodeFormat
        val inviteCode = "ABC-DEF-123"
        val result = InviteValidationResult(false, null, "Invite code expired")
        coEvery { groupRepository.validateInviteCode(inviteCode) } returns Result.success(result)

        viewModel = JoinGroupViewModel(groupRepository, authRepository, savedStateHandle)
        viewModel.setInviteCode(inviteCode)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.validateCode()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is JoinGroupUiState.Error)
            assertTrue((state as JoinGroupUiState.Error).message.contains("expired"))
        }
    }

    @Test
    fun `joinGroup requires authentication`() = runTest {
        // Given
        every { authRepository.isLoggedIn() } returns false
        viewModel = JoinGroupViewModel(groupRepository, authRepository, savedStateHandle)
        viewModel.setInviteCode("ABCD1234")

        // When
        viewModel.joinGroup()

        // Then
        viewModel.joinResult.test {
            val result = awaitItem()
            assertTrue(result is JoinResult.AuthenticationRequired)
        }
    }

    @Test
    fun `joinGroup succeeds when authenticated`() = runTest {
        // Given - use 11-character format XXX-XXX-XXX as required by ViewModel
        val inviteCode = "ABC-DEF-123"
        val joinResult = JoinGroupResult(
            groupId = "group-1",
            role = GroupRole.MEMBER,
            joinedAt = Instant.parse("2025-01-01T00:00:00Z"),
        )
        every { authRepository.isLoggedIn() } returns true
        coEvery { groupRepository.joinWithInvite(inviteCode) } returns Result.success(joinResult)

        viewModel = JoinGroupViewModel(groupRepository, authRepository, savedStateHandle)
        viewModel.setInviteCode(inviteCode)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.joinGroup()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.joinResult.test {
            val result = awaitItem()
            assertTrue(result is JoinResult.Success)
            assertEquals("group-1", (result as JoinResult.Success).groupId)
        }
    }

    @Test
    fun `joinGroup shows error on failure`() = runTest {
        // Given - use 11-character format XXX-XXX-XXX as required by ViewModel
        val inviteCode = "ABC-DEF-123"
        every { authRepository.isLoggedIn() } returns true
        coEvery { groupRepository.joinWithInvite(inviteCode) } returns Result.failure(
            Exception("already_member"),
        )

        viewModel = JoinGroupViewModel(groupRepository, authRepository, savedStateHandle)
        viewModel.setInviteCode(inviteCode)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.joinGroup()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.joinResult.test {
            val result = awaitItem()
            assertTrue(result is JoinResult.Error)
            assertTrue((result as JoinResult.Error).message.contains("already a member"))
        }
    }

    // AC E11.9.5: QR Code Scanning Tests

    @Test
    fun `handleQrCodeScan extracts code from deep link`() = runTest {
        // Given - use XXX-XXX-XXX format as required by isValidCodeFormat
        val inviteCode = "ABC-DEF-123"
        val preview = GroupPreview("group-1", "Family", 5)
        val result = InviteValidationResult(true, preview, null)
        coEvery { groupRepository.validateInviteCode(inviteCode) } returns Result.success(result)

        viewModel = JoinGroupViewModel(groupRepository, authRepository, savedStateHandle)

        // When
        viewModel.handleQrCodeScan("phonemanager://join/$inviteCode")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.inviteCode.test {
            assertEquals(inviteCode, awaitItem())
        }
        coVerify { groupRepository.validateInviteCode(inviteCode) }
    }

    @Test
    fun `handleQrCodeScan accepts plain code`() = runTest {
        // Given - use XXX-XXX-XXX format as required by isValidCodeFormat (lowercase input, uppercase result)
        val inviteCode = "ABC-DEF-123"
        val preview = GroupPreview("group-1", "Family", 5)
        val result = InviteValidationResult(true, preview, null)
        coEvery { groupRepository.validateInviteCode(inviteCode) } returns Result.success(result)

        viewModel = JoinGroupViewModel(groupRepository, authRepository, savedStateHandle)

        // When - input lowercase, should be normalized to uppercase
        viewModel.handleQrCodeScan("abc-def-123")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.inviteCode.test {
            assertEquals(inviteCode, awaitItem())
        }
    }

    @Test
    fun `handleQrCodeScan extracts code from HTTPS URL`() = runTest {
        // Given - use XXX-XXX-XXX format as required by isValidCodeFormat
        val inviteCode = "ABC-DEF-123"
        val preview = GroupPreview("group-1", "Family", 5)
        val result = InviteValidationResult(true, preview, null)
        coEvery { groupRepository.validateInviteCode(inviteCode) } returns Result.success(result)

        viewModel = JoinGroupViewModel(groupRepository, authRepository, savedStateHandle)

        // When
        viewModel.handleQrCodeScan("https://phonemanager.app/join/$inviteCode")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.inviteCode.test {
            assertEquals(inviteCode, awaitItem())
        }
    }

    @Test
    fun `handleQrCodeScan shows error for invalid QR content`() = runTest {
        // Given
        viewModel = JoinGroupViewModel(groupRepository, authRepository, savedStateHandle)

        // When
        viewModel.handleQrCodeScan("invalid content")

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is JoinGroupUiState.Error)
            assertEquals("Invalid QR code format", (state as JoinGroupUiState.Error).message)
        }
    }

    // AC E11.9.8: Deep Link Handling Tests

    @Test
    fun `handleDeepLink extracts and validates code`() = runTest {
        // Given - use XXX-XXX-XXX format as required by isValidCodeFormat
        val inviteCode = "WXY-Z56-789"
        val preview = GroupPreview("group-1", "Family", 5)
        val result = InviteValidationResult(true, preview, null)
        coEvery { groupRepository.validateInviteCode(inviteCode) } returns Result.success(result)

        viewModel = JoinGroupViewModel(groupRepository, authRepository, savedStateHandle)

        // When
        viewModel.handleDeepLink("phonemanager://join/$inviteCode")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.inviteCode.test {
            assertEquals(inviteCode, awaitItem())
        }
        coVerify { groupRepository.validateInviteCode(inviteCode) }
    }

    @Test
    fun `init validates code from SavedStateHandle`() = runTest {
        // Given - use XXX-XXX-XXX format as required by isValidCodeFormat
        val inviteCode = "INI-T12-345"
        val preview = GroupPreview("group-1", "Family", 5)
        val result = InviteValidationResult(true, preview, null)
        coEvery { groupRepository.validateInviteCode(inviteCode) } returns Result.success(result)
        savedStateHandle = SavedStateHandle(mapOf("code" to inviteCode))

        // When
        viewModel = JoinGroupViewModel(groupRepository, authRepository, savedStateHandle)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { groupRepository.validateInviteCode(inviteCode) }
        viewModel.groupPreview.test {
            val groupPreview = awaitItem()
            assertEquals("Family", groupPreview?.name)
        }
    }

    // State Management Tests

    @Test
    fun `clearState resets all state`() = runTest {
        // Given
        viewModel = JoinGroupViewModel(groupRepository, authRepository, savedStateHandle)
        viewModel.setInviteCode("ABCD1234")

        // When
        viewModel.clearState()

        // Then
        viewModel.inviteCode.test {
            assertEquals("", awaitItem())
        }
        viewModel.groupPreview.test {
            assertNull(awaitItem())
        }
        viewModel.uiState.test {
            assertTrue(awaitItem() is JoinGroupUiState.EnterCode)
        }
        viewModel.joinResult.test {
            assertTrue(awaitItem() is JoinResult.Idle)
        }
    }

    @Test
    fun `clearJoinResult resets join result to Idle`() = runTest {
        // Given
        every { authRepository.isLoggedIn() } returns false
        viewModel = JoinGroupViewModel(groupRepository, authRepository, savedStateHandle)
        viewModel.setInviteCode("ABCD1234")
        viewModel.joinGroup() // Triggers AuthenticationRequired

        // When
        viewModel.clearJoinResult()

        // Then
        viewModel.joinResult.test {
            assertTrue(awaitItem() is JoinResult.Idle)
        }
    }

    @Test
    fun `refreshAuthStatus updates isAuthenticated`() = runTest {
        // Given
        every { authRepository.isLoggedIn() } returns false
        viewModel = JoinGroupViewModel(groupRepository, authRepository, savedStateHandle)

        viewModel.isAuthenticated.test {
            assertFalse(awaitItem())
        }

        // When
        every { authRepository.isLoggedIn() } returns true
        viewModel.refreshAuthStatus()

        // Then
        viewModel.isAuthenticated.test {
            assertTrue(awaitItem())
        }
    }
}

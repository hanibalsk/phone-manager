package three.two.bit.phonemanager.ui.groups

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.junit.Before
import org.junit.Test
import three.two.bit.phonemanager.data.repository.GroupRepository
import three.two.bit.phonemanager.domain.model.Group
import three.two.bit.phonemanager.domain.model.GroupInvite
import three.two.bit.phonemanager.domain.model.GroupRole
import three.two.bit.phonemanager.domain.model.InviteStatus
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days

/**
 * Story E11.9 Task 14: Unit tests for InviteViewModel
 *
 * Tests:
 * - AC E11.9.1 (Generate invite code)
 * - AC E11.9.2 (Invite code display)
 * - AC E11.9.3 (Share invite)
 * - AC E11.9.6 (Pending invites list)
 * - AC E11.9.7 (Revoke invite)
 */
@OptIn(ExperimentalCoroutinesApi::class)
class InviteViewModelTest {

    private lateinit var groupRepository: GroupRepository
    private lateinit var savedStateHandle: SavedStateHandle
    private lateinit var viewModel: InviteViewModel

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        groupRepository = mockk(relaxed = true)
        savedStateHandle = SavedStateHandle(mapOf("groupId" to "group-1"))
    }

    // AC E11.9.1: Generate Invite Code Tests

    @Test
    fun `createInvite calls repository with correct parameters`() = runTest {
        // Given
        val testGroup = createTestGroup()
        val testInvite = createTestInvite()
        coEvery { groupRepository.getGroupDetails("group-1") } returns Result.success(testGroup)
        coEvery { groupRepository.getGroupInvites("group-1") } returns Result.success(emptyList())
        coEvery { groupRepository.createInvite("group-1", 7, 1) } returns Result.success(testInvite)

        viewModel = InviteViewModel(groupRepository, savedStateHandle)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.createInvite(expiryDays = 7, maxUses = 1)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { groupRepository.createInvite("group-1", 7, 1) }
    }

    @Test
    fun `createInvite updates currentInvite on success`() = runTest {
        // Given
        val testGroup = createTestGroup()
        val testInvite = createTestInvite()
        coEvery { groupRepository.getGroupDetails("group-1") } returns Result.success(testGroup)
        coEvery { groupRepository.getGroupInvites("group-1") } returns Result.success(emptyList())
        coEvery { groupRepository.createInvite("group-1", any(), any()) } returns Result.success(testInvite)

        viewModel = InviteViewModel(groupRepository, savedStateHandle)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.createInvite()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.currentInvite.test {
            val invite = awaitItem()
            assertNotNull(invite)
            assertEquals("ABCD1234", invite.code)
        }
    }

    @Test
    fun `createInvite shows error on failure`() = runTest {
        // Given
        val testGroup = createTestGroup()
        coEvery { groupRepository.getGroupDetails("group-1") } returns Result.success(testGroup)
        coEvery { groupRepository.getGroupInvites("group-1") } returns Result.success(emptyList())
        coEvery { groupRepository.createInvite("group-1", any(), any()) } returns Result.failure(
            Exception("Network error")
        )

        viewModel = InviteViewModel(groupRepository, savedStateHandle)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.createInvite()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.operationResult.test {
            val result = awaitItem()
            assertTrue(result is InviteOperationResult.Error)
        }
    }

    // AC E11.9.3: Share Invite Tests

    @Test
    fun `getShareContent returns correct message and deep link`() = runTest {
        // Given
        val testGroup = createTestGroup()
        val testInvite = createTestInvite()
        coEvery { groupRepository.getGroupDetails("group-1") } returns Result.success(testGroup)
        coEvery { groupRepository.getGroupInvites("group-1") } returns Result.success(listOf(testInvite))

        viewModel = InviteViewModel(groupRepository, savedStateHandle)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        val shareContent = viewModel.getShareContent(testInvite)

        // Then
        assertTrue(shareContent.message.contains("Family Group"))
        assertTrue(shareContent.message.contains("ABCD1234"))
        assertEquals("phonemanager://join/ABCD1234", shareContent.deepLink)
    }

    // AC E11.9.6: Pending Invites List Tests

    @Test
    fun `loadInvites fetches invites from repository`() = runTest {
        // Given
        val testGroup = createTestGroup()
        val invites = listOf(
            createTestInvite("invite-1", "CODE1111"),
            createTestInvite("invite-2", "CODE2222"),
        )
        coEvery { groupRepository.getGroupDetails("group-1") } returns Result.success(testGroup)
        coEvery { groupRepository.getGroupInvites("group-1") } returns Result.success(invites)

        // When
        viewModel = InviteViewModel(groupRepository, savedStateHandle)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.invites.test {
            val inviteList = awaitItem()
            assertEquals(2, inviteList.size)
            assertEquals("CODE1111", inviteList[0].code)
            assertEquals("CODE2222", inviteList[1].code)
        }
    }

    @Test
    fun `init shows loading state then success`() = runTest {
        // Given
        val testGroup = createTestGroup()
        coEvery { groupRepository.getGroupDetails("group-1") } returns Result.success(testGroup)
        coEvery { groupRepository.getGroupInvites("group-1") } returns Result.success(emptyList())

        // When
        viewModel = InviteViewModel(groupRepository, savedStateHandle)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is InviteUiState.Success)
        }
    }

    @Test
    fun `loadInvites shows error on failure`() = runTest {
        // Given
        val testGroup = createTestGroup()
        coEvery { groupRepository.getGroupDetails("group-1") } returns Result.success(testGroup)
        coEvery { groupRepository.getGroupInvites("group-1") } returns Result.failure(
            Exception("Failed to load invites")
        )

        // When
        viewModel = InviteViewModel(groupRepository, savedStateHandle)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is InviteUiState.Error)
        }
    }

    // AC E11.9.7: Revoke Invite Tests

    @Test
    fun `revokeInvite calls repository with correct parameters`() = runTest {
        // Given
        val testGroup = createTestGroup()
        val testInvite = createTestInvite()
        coEvery { groupRepository.getGroupDetails("group-1") } returns Result.success(testGroup)
        coEvery { groupRepository.getGroupInvites("group-1") } returns Result.success(listOf(testInvite))
        coEvery { groupRepository.revokeInvite("group-1", "invite-1") } returns Result.success(Unit)

        viewModel = InviteViewModel(groupRepository, savedStateHandle)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.revokeInvite("invite-1")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { groupRepository.revokeInvite("group-1", "invite-1") }
    }

    @Test
    fun `revokeInvite shows success result`() = runTest {
        // Given
        val testGroup = createTestGroup()
        val testInvite = createTestInvite()
        coEvery { groupRepository.getGroupDetails("group-1") } returns Result.success(testGroup)
        coEvery { groupRepository.getGroupInvites("group-1") } returns Result.success(listOf(testInvite))
        coEvery { groupRepository.revokeInvite("group-1", "invite-1") } returns Result.success(Unit)

        viewModel = InviteViewModel(groupRepository, savedStateHandle)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.revokeInvite("invite-1")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.operationResult.test {
            val result = awaitItem()
            assertTrue(result is InviteOperationResult.InviteRevoked)
        }
    }

    @Test
    fun `revokeInvite shows error on failure`() = runTest {
        // Given
        val testGroup = createTestGroup()
        val testInvite = createTestInvite()
        coEvery { groupRepository.getGroupDetails("group-1") } returns Result.success(testGroup)
        coEvery { groupRepository.getGroupInvites("group-1") } returns Result.success(listOf(testInvite))
        coEvery { groupRepository.revokeInvite("group-1", "invite-1") } returns Result.failure(
            Exception("Cannot revoke")
        )

        viewModel = InviteViewModel(groupRepository, savedStateHandle)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.revokeInvite("invite-1")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.operationResult.test {
            val result = awaitItem()
            assertTrue(result is InviteOperationResult.Error)
        }
    }

    // State Management Tests

    @Test
    fun `setCurrentInvite updates current invite`() = runTest {
        // Given
        val testGroup = createTestGroup()
        val invite1 = createTestInvite("invite-1", "CODE1111")
        val invite2 = createTestInvite("invite-2", "CODE2222")
        coEvery { groupRepository.getGroupDetails("group-1") } returns Result.success(testGroup)
        coEvery { groupRepository.getGroupInvites("group-1") } returns Result.success(listOf(invite1, invite2))

        viewModel = InviteViewModel(groupRepository, savedStateHandle)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.setCurrentInvite(invite2)

        // Then
        viewModel.currentInvite.test {
            val invite = awaitItem()
            assertEquals("CODE2222", invite?.code)
        }
    }

    @Test
    fun `clearOperationResult resets to Idle`() = runTest {
        // Given
        val testGroup = createTestGroup()
        val testInvite = createTestInvite()
        coEvery { groupRepository.getGroupDetails("group-1") } returns Result.success(testGroup)
        coEvery { groupRepository.getGroupInvites("group-1") } returns Result.success(listOf(testInvite))
        coEvery { groupRepository.revokeInvite("group-1", "invite-1") } returns Result.success(Unit)

        viewModel = InviteViewModel(groupRepository, savedStateHandle)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.revokeInvite("invite-1")
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.clearOperationResult()

        // Then
        viewModel.operationResult.test {
            val result = awaitItem()
            assertTrue(result is InviteOperationResult.Idle)
        }
    }

    @Test
    fun `init with blank groupId shows error`() = runTest {
        // Given
        savedStateHandle = SavedStateHandle(mapOf("groupId" to ""))

        // When
        viewModel = InviteViewModel(groupRepository, savedStateHandle)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is InviteUiState.Error)
        }
    }

    // Helper functions

    private fun createTestGroup(
        id: String = "group-1",
        name: String = "Family Group",
    ) = Group(
        id = id,
        name = name,
        description = "Test group",
        ownerId = "owner-1",
        memberCount = 5,
        userRole = GroupRole.OWNER,
        createdAt = Instant.parse("2025-01-01T00:00:00Z"),
        updatedAt = Instant.parse("2025-01-01T00:00:00Z"),
    )

    private fun createTestInvite(
        id: String = "invite-1",
        code: String = "ABCD1234",
    ) = GroupInvite(
        id = id,
        groupId = "group-1",
        groupName = "Family Group",
        code = code,
        createdBy = "user-1",
        createdAt = Clock.System.now(),
        expiresAt = Clock.System.now() + 7.days,
        maxUses = 1,
        usesRemaining = 1,
        status = InviteStatus.ACTIVE,
    )
}

package three.two.bit.phonemanager.ui.admin

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
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
import three.two.bit.phonemanager.data.repository.GroupRepository
import three.two.bit.phonemanager.domain.model.Group
import three.two.bit.phonemanager.domain.model.GroupMembership
import three.two.bit.phonemanager.domain.model.GroupRole
import three.two.bit.phonemanager.security.SecureStorage
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

/**
 * Story E9.3, E9.6: Unit tests for AdminUsersViewModel
 *
 * Tests:
 * - AC E9.3.1 (Load admin groups)
 * - AC E9.3.2 (Show managed devices/users)
 * - AC E9.6.1 (Remove action from users list)
 * - AC E9.6.2 (Confirmation dialog)
 * - AC E9.6.3 (Backend API to revoke)
 * - AC E9.6.4 (User removed immediately)
 * - AC E9.6.6 (Cannot remove self)
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AdminUsersViewModelTest {

    private lateinit var groupRepository: GroupRepository
    private lateinit var secureStorage: SecureStorage
    private lateinit var viewModel: AdminUsersViewModel

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        groupRepository = mockk(relaxed = true)
        secureStorage = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // =============================================================================
    // AC E9.3.1: Load admin groups tests
    // =============================================================================

    @Test
    fun `init loads admin groups successfully`() = runTest {
        // Given
        val groups = listOf(
            createTestGroup("group-1", "Family", GroupRole.OWNER),
            createTestGroup("group-2", "Work", GroupRole.ADMIN),
            createTestGroup("group-3", "Friends", GroupRole.MEMBER), // Should be filtered out
        )
        coEvery { groupRepository.getUserGroups() } returns Result.success(groups)

        // When
        viewModel = AdminUsersViewModel(groupRepository, secureStorage)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertEquals(2, state.adminGroups.size) // Only OWNER and ADMIN groups
            assertFalse(state.isEmpty)
            assertNull(state.error)
        }
    }

    @Test
    fun `init shows empty state when no admin groups`() = runTest {
        // Given
        val groups = listOf(
            createTestGroup("group-1", "Friends", GroupRole.MEMBER),
        )
        coEvery { groupRepository.getUserGroups() } returns Result.success(groups)

        // When
        viewModel = AdminUsersViewModel(groupRepository, secureStorage)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertTrue(state.adminGroups.isEmpty())
            assertTrue(state.isEmpty)
        }
    }

    @Test
    fun `loadAdminGroups shows error on failure`() = runTest {
        // Given
        coEvery { groupRepository.getUserGroups() } returns Result.failure(
            Exception("Network error"),
        )

        // When
        viewModel = AdminUsersViewModel(groupRepository, secureStorage)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertEquals("Network error", state.error)
        }
    }

    // =============================================================================
    // AC E9.3.2: Show managed devices/users tests
    // =============================================================================

    @Test
    fun `selectGroup loads group members`() = runTest {
        // Given
        val group = createTestGroup("group-1", "Family", GroupRole.OWNER)
        val members = listOf(
            createTestMember("user-1", "group-1", "john@example.com", "John"),
            createTestMember("user-2", "group-1", "jane@example.com", "Jane"),
        )
        coEvery { groupRepository.getUserGroups() } returns Result.success(listOf(group))
        coEvery { groupRepository.getGroupMembers("group-1") } returns Result.success(members)

        viewModel = AdminUsersViewModel(groupRepository, secureStorage)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.selectGroup(group)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(group, state.selectedGroup)
            assertEquals(2, state.groupMembers.size)
            assertFalse(state.isMembersLoading)
            assertNull(state.membersError)
        }
    }

    @Test
    fun `selectGroup shows error on failure`() = runTest {
        // Given
        val group = createTestGroup("group-1", "Family", GroupRole.OWNER)
        coEvery { groupRepository.getUserGroups() } returns Result.success(listOf(group))
        coEvery { groupRepository.getGroupMembers("group-1") } returns Result.failure(
            Exception("Failed to load members"),
        )

        viewModel = AdminUsersViewModel(groupRepository, secureStorage)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.selectGroup(group)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(group, state.selectedGroup)
            assertFalse(state.isMembersLoading)
            assertEquals("Failed to load members", state.membersError)
        }
    }

    @Test
    fun `clearSelectedGroup resets to group list view`() = runTest {
        // Given
        val group = createTestGroup("group-1", "Family", GroupRole.OWNER)
        val members = listOf(createTestMember("user-1", "group-1", "test@example.com", "Test"))
        coEvery { groupRepository.getUserGroups() } returns Result.success(listOf(group))
        coEvery { groupRepository.getGroupMembers("group-1") } returns Result.success(members)

        viewModel = AdminUsersViewModel(groupRepository, secureStorage)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.selectGroup(group)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.clearSelectedGroup()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertNull(state.selectedGroup)
            assertTrue(state.groupMembers.isEmpty())
        }
    }

    // =============================================================================
    // AC E9.6.6: Cannot remove self tests
    // =============================================================================

    @Test
    fun `isCurrentUser returns true for own user`() = runTest {
        // Given
        every { secureStorage.getUserId() } returns "current-user-id"
        coEvery { groupRepository.getUserGroups() } returns Result.success(emptyList())

        viewModel = AdminUsersViewModel(groupRepository, secureStorage)
        testDispatcher.scheduler.advanceUntilIdle()

        val ownMember = createTestMember("current-user-id", "group-1", "me@example.com", "Me")

        // When
        val result = viewModel.isCurrentUser(ownMember)

        // Then
        assertTrue(result)
    }

    @Test
    fun `isCurrentUser returns false for other user`() = runTest {
        // Given
        every { secureStorage.getUserId() } returns "current-user-id"
        coEvery { groupRepository.getUserGroups() } returns Result.success(emptyList())

        viewModel = AdminUsersViewModel(groupRepository, secureStorage)
        testDispatcher.scheduler.advanceUntilIdle()

        val otherMember = createTestMember("other-user-id", "group-1", "other@example.com", "Other")

        // When
        val result = viewModel.isCurrentUser(otherMember)

        // Then
        assertFalse(result)
    }

    // =============================================================================
    // AC E9.6.2: Confirmation dialog tests
    // =============================================================================

    @Test
    fun `showRemoveConfirmation sets memberToRemove`() = runTest {
        // Given
        coEvery { groupRepository.getUserGroups() } returns Result.success(emptyList())

        viewModel = AdminUsersViewModel(groupRepository, secureStorage)
        testDispatcher.scheduler.advanceUntilIdle()

        val member = createTestMember("user-1", "group-1", "test@example.com", "Test User")

        // When
        viewModel.showRemoveConfirmation(member)

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(member, state.memberToRemove)
        }
    }

    @Test
    fun `cancelRemoveConfirmation clears memberToRemove`() = runTest {
        // Given
        coEvery { groupRepository.getUserGroups() } returns Result.success(emptyList())

        viewModel = AdminUsersViewModel(groupRepository, secureStorage)
        testDispatcher.scheduler.advanceUntilIdle()

        val member = createTestMember("user-1", "group-1", "test@example.com", "Test User")
        viewModel.showRemoveConfirmation(member)

        // When
        viewModel.cancelRemoveConfirmation()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertNull(state.memberToRemove)
        }
    }

    // =============================================================================
    // AC E9.6.3, E9.6.4: Remove user tests
    // =============================================================================

    @Test
    fun `removeUser succeeds and removes user from list`() = runTest {
        // Given
        val group = createTestGroup("group-1", "Family", GroupRole.OWNER)
        val member1 = createTestMember("user-1", "group-1", "john@example.com", "John")
        val member2 = createTestMember("user-2", "group-1", "jane@example.com", "Jane")
        val members = listOf(member1, member2)

        coEvery { groupRepository.getUserGroups() } returns Result.success(listOf(group))
        coEvery { groupRepository.getGroupMembers("group-1") } returns Result.success(members)
        coEvery { groupRepository.removeMember("group-1", "user-1") } returns Result.success(Unit)

        viewModel = AdminUsersViewModel(groupRepository, secureStorage)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.selectGroup(group)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.showRemoveConfirmation(member1)

        // When
        viewModel.removeUser()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { groupRepository.removeMember("group-1", "user-1") }

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isRemoving)
            assertNull(state.memberToRemove)
            assertEquals(1, state.groupMembers.size)
            assertEquals("Jane", state.groupMembers[0].displayName)
            assertEquals("John", state.removeSuccess)
            assertNull(state.removeError)
        }
    }

    @Test
    fun `removeUser shows error on failure`() = runTest {
        // Given
        val group = createTestGroup("group-1", "Family", GroupRole.OWNER)
        val member = createTestMember("user-1", "group-1", "john@example.com", "John")

        coEvery { groupRepository.getUserGroups() } returns Result.success(listOf(group))
        coEvery { groupRepository.getGroupMembers("group-1") } returns Result.success(listOf(member))
        coEvery { groupRepository.removeMember("group-1", "user-1") } returns Result.failure(
            Exception("Permission denied"),
        )

        viewModel = AdminUsersViewModel(groupRepository, secureStorage)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.selectGroup(group)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.showRemoveConfirmation(member)

        // When
        viewModel.removeUser()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isRemoving)
            assertNull(state.memberToRemove)
            assertEquals("Permission denied", state.removeError)
            assertNull(state.removeSuccess)
            // Member should still be in the list
            assertEquals(1, state.groupMembers.size)
        }
    }

    @Test
    fun `removeUser does nothing without memberToRemove`() = runTest {
        // Given
        val group = createTestGroup("group-1", "Family", GroupRole.OWNER)
        coEvery { groupRepository.getUserGroups() } returns Result.success(listOf(group))
        coEvery { groupRepository.getGroupMembers("group-1") } returns Result.success(emptyList())

        viewModel = AdminUsersViewModel(groupRepository, secureStorage)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.selectGroup(group)
        testDispatcher.scheduler.advanceUntilIdle()

        // When - no member selected for removal
        viewModel.removeUser()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify(exactly = 0) { groupRepository.removeMember(any(), any()) }
    }

    @Test
    fun `removeUser does nothing without selectedGroup`() = runTest {
        // Given
        coEvery { groupRepository.getUserGroups() } returns Result.success(emptyList())

        viewModel = AdminUsersViewModel(groupRepository, secureStorage)
        testDispatcher.scheduler.advanceUntilIdle()

        val member = createTestMember("user-1", "group-1", "test@example.com", "Test")
        viewModel.showRemoveConfirmation(member)

        // When - no group selected
        viewModel.removeUser()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify(exactly = 0) { groupRepository.removeMember(any(), any()) }
    }

    // =============================================================================
    // Clear message tests
    // =============================================================================

    @Test
    fun `clearRemoveSuccess clears success message`() = runTest {
        // Given
        val group = createTestGroup("group-1", "Family", GroupRole.OWNER)
        val member = createTestMember("user-1", "group-1", "test@example.com", "Test")

        coEvery { groupRepository.getUserGroups() } returns Result.success(listOf(group))
        coEvery { groupRepository.getGroupMembers("group-1") } returns Result.success(listOf(member))
        coEvery { groupRepository.removeMember("group-1", "user-1") } returns Result.success(Unit)

        viewModel = AdminUsersViewModel(groupRepository, secureStorage)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.selectGroup(group)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.showRemoveConfirmation(member)
        viewModel.removeUser()
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.clearRemoveSuccess()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertNull(state.removeSuccess)
        }
    }

    @Test
    fun `clearRemoveError clears error message`() = runTest {
        // Given
        val group = createTestGroup("group-1", "Family", GroupRole.OWNER)
        val member = createTestMember("user-1", "group-1", "test@example.com", "Test")

        coEvery { groupRepository.getUserGroups() } returns Result.success(listOf(group))
        coEvery { groupRepository.getGroupMembers("group-1") } returns Result.success(listOf(member))
        coEvery { groupRepository.removeMember("group-1", "user-1") } returns Result.failure(
            Exception("Error"),
        )

        viewModel = AdminUsersViewModel(groupRepository, secureStorage)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.selectGroup(group)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.showRemoveConfirmation(member)
        viewModel.removeUser()
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.clearRemoveError()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertNull(state.removeError)
        }
    }

    // =============================================================================
    // Refresh tests
    // =============================================================================

    @Test
    fun `refresh reloads groups when no group selected`() = runTest {
        // Given
        val groups = listOf(createTestGroup("group-1", "Family", GroupRole.OWNER))
        coEvery { groupRepository.getUserGroups() } returns Result.success(groups)

        viewModel = AdminUsersViewModel(groupRepository, secureStorage)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.refresh()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify(exactly = 2) { groupRepository.getUserGroups() } // Once in init, once in refresh
    }

    @Test
    fun `refresh reloads members when group is selected`() = runTest {
        // Given
        val group = createTestGroup("group-1", "Family", GroupRole.OWNER)
        val members = listOf(createTestMember("user-1", "group-1", "test@example.com", "Test"))

        coEvery { groupRepository.getUserGroups() } returns Result.success(listOf(group))
        coEvery { groupRepository.getGroupMembers("group-1") } returns Result.success(members)

        viewModel = AdminUsersViewModel(groupRepository, secureStorage)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.selectGroup(group)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.refresh()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify(exactly = 2) { groupRepository.getGroupMembers("group-1") } // Once in select, once in refresh
    }

    // =============================================================================
    // Helper functions
    // =============================================================================

    private fun createTestGroup(id: String, name: String, userRole: GroupRole) = Group(
        id = id,
        name = name,
        description = null,
        ownerId = "owner-1",
        memberCount = 1,
        userRole = userRole,
        createdAt = Instant.parse("2025-01-01T00:00:00Z"),
        updatedAt = Instant.parse("2025-01-01T00:00:00Z"),
    )

    private fun createTestMember(
        userId: String,
        groupId: String,
        email: String,
        displayName: String,
        role: GroupRole = GroupRole.MEMBER,
    ) = GroupMembership(
        userId = userId,
        groupId = groupId,
        email = email,
        displayName = displayName,
        role = role,
        deviceCount = 1,
        joinedAt = Instant.parse("2025-01-01T00:00:00Z"),
        lastActiveAt = Instant.parse("2025-01-01T12:00:00Z"),
    )
}

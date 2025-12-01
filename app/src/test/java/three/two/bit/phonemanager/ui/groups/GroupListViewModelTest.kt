package three.two.bit.phonemanager.ui.groups

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
import kotlinx.datetime.Instant
import org.junit.Before
import org.junit.Test
import three.two.bit.phonemanager.data.repository.AuthRepository
import three.two.bit.phonemanager.data.repository.GroupRepository
import three.two.bit.phonemanager.domain.model.Group
import three.two.bit.phonemanager.domain.model.GroupRole
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Story E11.8 Task 13: Unit tests for GroupListViewModel
 *
 * Tests AC E11.8.1 (Group List) and AC E11.8.2 (Create Group)
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GroupListViewModelTest {

    private lateinit var groupRepository: GroupRepository
    private lateinit var authRepository: AuthRepository
    private lateinit var viewModel: GroupListViewModel

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        groupRepository = mockk(relaxed = true)
        authRepository = mockk(relaxed = true)
    }

    // AC E11.8.1: Group List Tests

    @Test
    fun `init loads groups when user is logged in`() = runTest {
        // Given
        every { authRepository.isLoggedIn() } returns true
        val groups = listOf(
            createTestGroup("group-1", "Family"),
            createTestGroup("group-2", "Friends"),
        )
        coEvery { groupRepository.getUserGroups() } returns Result.success(groups)

        // When
        viewModel = GroupListViewModel(groupRepository, authRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is GroupListUiState.Success)
            assertEquals(2, (state as GroupListUiState.Success).groups.size)
        }
    }

    @Test
    fun `init shows error when user is not logged in`() = runTest {
        // Given
        every { authRepository.isLoggedIn() } returns false

        // When
        viewModel = GroupListViewModel(groupRepository, authRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is GroupListUiState.Error)
            assertEquals("not_authenticated", (state as GroupListUiState.Error).errorCode)
        }
    }

    @Test
    fun `refreshGroups shows empty state when no groups`() = runTest {
        // Given
        every { authRepository.isLoggedIn() } returns true
        coEvery { groupRepository.getUserGroups() } returns Result.success(emptyList())

        // When
        viewModel = GroupListViewModel(groupRepository, authRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is GroupListUiState.Empty)
        }
    }

    @Test
    fun `refreshGroups shows error on failure`() = runTest {
        // Given
        every { authRepository.isLoggedIn() } returns true
        coEvery { groupRepository.getUserGroups() } returns Result.failure(
            Exception("Network error")
        )

        // When
        viewModel = GroupListViewModel(groupRepository, authRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is GroupListUiState.Error)
        }
    }

    // AC E11.8.2: Create Group Tests

    @Test
    fun `createGroup succeeds with valid name`() = runTest {
        // Given
        every { authRepository.isLoggedIn() } returns true
        coEvery { groupRepository.getUserGroups() } returns Result.success(emptyList())

        val newGroup = createTestGroup("new-group", "New Group")
        coEvery { groupRepository.createGroup(any(), any()) } returns Result.success(newGroup)

        viewModel = GroupListViewModel(groupRepository, authRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.createGroup("New Group", "Description")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.createGroupResult.test {
            val result = awaitItem()
            assertTrue(result is CreateGroupResult.Success)
            assertEquals("New Group", (result as CreateGroupResult.Success).group.name)
        }
    }

    @Test
    fun `createGroup fails with blank name`() = runTest {
        // Given
        every { authRepository.isLoggedIn() } returns true
        coEvery { groupRepository.getUserGroups() } returns Result.success(emptyList())

        viewModel = GroupListViewModel(groupRepository, authRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.createGroup("  ", null)

        // Then
        viewModel.createGroupResult.test {
            val result = awaitItem()
            assertTrue(result is CreateGroupResult.Error)
            assertEquals("Group name is required", (result as CreateGroupResult.Error).message)
        }
    }

    @Test
    fun `createGroup fails with name over 50 characters`() = runTest {
        // Given
        every { authRepository.isLoggedIn() } returns true
        coEvery { groupRepository.getUserGroups() } returns Result.success(emptyList())

        viewModel = GroupListViewModel(groupRepository, authRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.createGroup("A".repeat(51), null)

        // Then
        viewModel.createGroupResult.test {
            val result = awaitItem()
            assertTrue(result is CreateGroupResult.Error)
            assertEquals("Group name must be 50 characters or less", (result as CreateGroupResult.Error).message)
        }
    }

    @Test
    fun `createGroup refreshes list after success`() = runTest {
        // Given
        every { authRepository.isLoggedIn() } returns true
        coEvery { groupRepository.getUserGroups() } returnsMany listOf(
            Result.success(emptyList()),
            Result.success(listOf(createTestGroup("group-1", "New Group"))),
        )

        val newGroup = createTestGroup("group-1", "New Group")
        coEvery { groupRepository.createGroup(any(), any()) } returns Result.success(newGroup)

        viewModel = GroupListViewModel(groupRepository, authRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.createGroup("New Group", null)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify(exactly = 2) { groupRepository.getUserGroups() }
    }

    @Test
    fun `clearCreateGroupResult resets state to Idle`() = runTest {
        // Given
        every { authRepository.isLoggedIn() } returns true
        coEvery { groupRepository.getUserGroups() } returns Result.success(emptyList())

        viewModel = GroupListViewModel(groupRepository, authRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.createGroup("  ", null) // Trigger error

        // When
        viewModel.clearCreateGroupResult()

        // Then
        viewModel.createGroupResult.test {
            val result = awaitItem()
            assertTrue(result is CreateGroupResult.Idle)
        }
    }

    // Helper functions

    private fun createTestGroup(
        id: String,
        name: String,
        description: String? = null,
        memberCount: Int = 1,
        userRole: GroupRole = GroupRole.OWNER,
    ) = Group(
        id = id,
        name = name,
        description = description,
        ownerId = "user-1",
        memberCount = memberCount,
        userRole = userRole,
        createdAt = Instant.parse("2025-01-01T00:00:00Z"),
        updatedAt = Instant.parse("2025-01-01T00:00:00Z"),
    )
}

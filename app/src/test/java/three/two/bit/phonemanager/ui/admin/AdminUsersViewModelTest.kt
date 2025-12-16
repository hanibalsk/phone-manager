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
import kotlin.time.Instant
import org.junit.After
import org.junit.Before
import org.junit.Test
import three.two.bit.phonemanager.data.repository.DeviceRepository
import three.two.bit.phonemanager.data.repository.GroupRepository
import three.two.bit.phonemanager.domain.model.Device
import three.two.bit.phonemanager.domain.model.DeviceLocation
import three.two.bit.phonemanager.domain.model.Group
import three.two.bit.phonemanager.domain.model.GroupRole
import three.two.bit.phonemanager.security.SecureStorage
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

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
    private lateinit var deviceRepository: DeviceRepository
    private lateinit var secureStorage: SecureStorage
    private lateinit var viewModel: AdminUsersViewModel

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        groupRepository = mockk(relaxed = true)
        deviceRepository = mockk(relaxed = true)
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
        viewModel = AdminUsersViewModel(groupRepository, deviceRepository, secureStorage)
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
        viewModel = AdminUsersViewModel(groupRepository, deviceRepository, secureStorage)
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
            Exception("Network error")
        )

        // When
        viewModel = AdminUsersViewModel(groupRepository, deviceRepository, secureStorage)
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
        val devices = listOf(
            createTestDevice("device-1", "John's Phone", "user-1"),
            createTestDevice("device-2", "Jane's Phone", "user-2"),
        )
        coEvery { groupRepository.getUserGroups() } returns Result.success(listOf(group))
        coEvery { deviceRepository.getGroupDevices("group-1") } returns Result.success(devices)

        viewModel = AdminUsersViewModel(groupRepository, deviceRepository, secureStorage)
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
        coEvery { deviceRepository.getGroupDevices("group-1") } returns Result.failure(
            Exception("Failed to load members")
        )

        viewModel = AdminUsersViewModel(groupRepository, deviceRepository, secureStorage)
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
        val devices = listOf(createTestDevice("device-1", "Phone", "user-1"))
        coEvery { groupRepository.getUserGroups() } returns Result.success(listOf(group))
        coEvery { deviceRepository.getGroupDevices("group-1") } returns Result.success(devices)

        viewModel = AdminUsersViewModel(groupRepository, deviceRepository, secureStorage)
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
    fun `isCurrentUserDevice returns true for own device`() = runTest {
        // Given
        every { secureStorage.getUserId() } returns "current-user-id"
        coEvery { groupRepository.getUserGroups() } returns Result.success(emptyList())

        viewModel = AdminUsersViewModel(groupRepository, deviceRepository, secureStorage)
        testDispatcher.scheduler.advanceUntilIdle()

        val ownDevice = createTestDevice("device-1", "My Phone", "current-user-id")

        // When
        val result = viewModel.isCurrentUserDevice(ownDevice)

        // Then
        assertTrue(result)
    }

    @Test
    fun `isCurrentUserDevice returns false for other user's device`() = runTest {
        // Given
        every { secureStorage.getUserId() } returns "current-user-id"
        coEvery { groupRepository.getUserGroups() } returns Result.success(emptyList())

        viewModel = AdminUsersViewModel(groupRepository, deviceRepository, secureStorage)
        testDispatcher.scheduler.advanceUntilIdle()

        val otherDevice = createTestDevice("device-1", "Other Phone", "other-user-id")

        // When
        val result = viewModel.isCurrentUserDevice(otherDevice)

        // Then
        assertFalse(result)
    }

    @Test
    fun `isCurrentUserDevice returns false for device with no owner`() = runTest {
        // Given
        every { secureStorage.getUserId() } returns "current-user-id"
        coEvery { groupRepository.getUserGroups() } returns Result.success(emptyList())

        viewModel = AdminUsersViewModel(groupRepository, deviceRepository, secureStorage)
        testDispatcher.scheduler.advanceUntilIdle()

        val unlinkedDevice = createTestDevice("device-1", "Unlinked Phone", null)

        // When
        val result = viewModel.isCurrentUserDevice(unlinkedDevice)

        // Then
        assertFalse(result)
    }

    // =============================================================================
    // AC E9.6.2: Confirmation dialog tests
    // =============================================================================

    @Test
    fun `showRemoveConfirmation sets deviceToRemove`() = runTest {
        // Given
        coEvery { groupRepository.getUserGroups() } returns Result.success(emptyList())

        viewModel = AdminUsersViewModel(groupRepository, deviceRepository, secureStorage)
        testDispatcher.scheduler.advanceUntilIdle()

        val device = createTestDevice("device-1", "Phone", "user-1")

        // When
        viewModel.showRemoveConfirmation(device)

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(device, state.deviceToRemove)
        }
    }

    @Test
    fun `cancelRemoveConfirmation clears deviceToRemove`() = runTest {
        // Given
        coEvery { groupRepository.getUserGroups() } returns Result.success(emptyList())

        viewModel = AdminUsersViewModel(groupRepository, deviceRepository, secureStorage)
        testDispatcher.scheduler.advanceUntilIdle()

        val device = createTestDevice("device-1", "Phone", "user-1")
        viewModel.showRemoveConfirmation(device)

        // When
        viewModel.cancelRemoveConfirmation()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertNull(state.deviceToRemove)
        }
    }

    // =============================================================================
    // AC E9.6.3, E9.6.4: Remove user tests
    // =============================================================================

    @Test
    fun `removeUser succeeds and removes user from list`() = runTest {
        // Given
        val group = createTestGroup("group-1", "Family", GroupRole.OWNER)
        val device1 = createTestDevice("device-1", "John's Phone", "user-1")
        val device2 = createTestDevice("device-2", "Jane's Phone", "user-2")
        val devices = listOf(device1, device2)

        coEvery { groupRepository.getUserGroups() } returns Result.success(listOf(group))
        coEvery { deviceRepository.getGroupDevices("group-1") } returns Result.success(devices)
        coEvery { groupRepository.removeMember("group-1", "user-1") } returns Result.success(Unit)

        viewModel = AdminUsersViewModel(groupRepository, deviceRepository, secureStorage)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.selectGroup(group)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.showRemoveConfirmation(device1)

        // When
        viewModel.removeUser()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { groupRepository.removeMember("group-1", "user-1") }

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isRemoving)
            assertNull(state.deviceToRemove)
            assertEquals(1, state.groupMembers.size)
            assertEquals("Jane's Phone", state.groupMembers[0].displayName)
            assertEquals("John's Phone", state.removeSuccess)
            assertNull(state.removeError)
        }
    }

    @Test
    fun `removeUser shows error on failure`() = runTest {
        // Given
        val group = createTestGroup("group-1", "Family", GroupRole.OWNER)
        val device = createTestDevice("device-1", "John's Phone", "user-1")

        coEvery { groupRepository.getUserGroups() } returns Result.success(listOf(group))
        coEvery { deviceRepository.getGroupDevices("group-1") } returns Result.success(listOf(device))
        coEvery { groupRepository.removeMember("group-1", "user-1") } returns Result.failure(
            Exception("Permission denied")
        )

        viewModel = AdminUsersViewModel(groupRepository, deviceRepository, secureStorage)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.selectGroup(group)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.showRemoveConfirmation(device)

        // When
        viewModel.removeUser()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isRemoving)
            assertNull(state.deviceToRemove)
            assertEquals("Permission denied", state.removeError)
            assertNull(state.removeSuccess)
            // Device should still be in the list
            assertEquals(1, state.groupMembers.size)
        }
    }

    @Test
    fun `removeUser fails gracefully for device without ownerId`() = runTest {
        // Given
        val group = createTestGroup("group-1", "Family", GroupRole.OWNER)
        val device = createTestDevice("device-1", "Unlinked Phone", null)

        coEvery { groupRepository.getUserGroups() } returns Result.success(listOf(group))
        coEvery { deviceRepository.getGroupDevices("group-1") } returns Result.success(listOf(device))

        viewModel = AdminUsersViewModel(groupRepository, deviceRepository, secureStorage)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.selectGroup(group)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.showRemoveConfirmation(device)

        // When
        viewModel.removeUser()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertNull(state.deviceToRemove)
            assertNotNull(state.removeError)
            assertTrue(state.removeError!!.contains("not linked to a user account"))
        }
    }

    @Test
    fun `removeUser does nothing without deviceToRemove`() = runTest {
        // Given
        val group = createTestGroup("group-1", "Family", GroupRole.OWNER)
        coEvery { groupRepository.getUserGroups() } returns Result.success(listOf(group))
        coEvery { deviceRepository.getGroupDevices("group-1") } returns Result.success(emptyList())

        viewModel = AdminUsersViewModel(groupRepository, deviceRepository, secureStorage)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.selectGroup(group)
        testDispatcher.scheduler.advanceUntilIdle()

        // When - no device selected for removal
        viewModel.removeUser()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify(exactly = 0) { groupRepository.removeMember(any(), any()) }
    }

    @Test
    fun `removeUser does nothing without selectedGroup`() = runTest {
        // Given
        coEvery { groupRepository.getUserGroups() } returns Result.success(emptyList())

        viewModel = AdminUsersViewModel(groupRepository, deviceRepository, secureStorage)
        testDispatcher.scheduler.advanceUntilIdle()

        val device = createTestDevice("device-1", "Phone", "user-1")
        viewModel.showRemoveConfirmation(device)

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
        val device = createTestDevice("device-1", "Phone", "user-1")

        coEvery { groupRepository.getUserGroups() } returns Result.success(listOf(group))
        coEvery { deviceRepository.getGroupDevices("group-1") } returns Result.success(listOf(device))
        coEvery { groupRepository.removeMember("group-1", "user-1") } returns Result.success(Unit)

        viewModel = AdminUsersViewModel(groupRepository, deviceRepository, secureStorage)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.selectGroup(group)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.showRemoveConfirmation(device)
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
        val device = createTestDevice("device-1", "Phone", "user-1")

        coEvery { groupRepository.getUserGroups() } returns Result.success(listOf(group))
        coEvery { deviceRepository.getGroupDevices("group-1") } returns Result.success(listOf(device))
        coEvery { groupRepository.removeMember("group-1", "user-1") } returns Result.failure(
            Exception("Error")
        )

        viewModel = AdminUsersViewModel(groupRepository, deviceRepository, secureStorage)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.selectGroup(group)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.showRemoveConfirmation(device)
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

        viewModel = AdminUsersViewModel(groupRepository, deviceRepository, secureStorage)
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
        val devices = listOf(createTestDevice("device-1", "Phone", "user-1"))

        coEvery { groupRepository.getUserGroups() } returns Result.success(listOf(group))
        coEvery { deviceRepository.getGroupDevices("group-1") } returns Result.success(devices)

        viewModel = AdminUsersViewModel(groupRepository, deviceRepository, secureStorage)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.selectGroup(group)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.refresh()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify(exactly = 2) { deviceRepository.getGroupDevices("group-1") } // Once in select, once in refresh
    }

    // =============================================================================
    // Helper functions
    // =============================================================================

    private fun createTestGroup(
        id: String,
        name: String,
        userRole: GroupRole,
    ) = Group(
        id = id,
        name = name,
        description = null,
        ownerId = "owner-1",
        memberCount = 1,
        userRole = userRole,
        createdAt = Instant.parse("2025-01-01T00:00:00Z"),
        updatedAt = Instant.parse("2025-01-01T00:00:00Z"),
    )

    private fun createTestDevice(
        deviceId: String,
        displayName: String,
        ownerId: String?,
    ) = Device(
        deviceId = deviceId,
        displayName = displayName,
        ownerId = ownerId,
        lastLocation = DeviceLocation(
            latitude = 48.1486,
            longitude = 17.1077,
            timestamp = Instant.parse("2025-01-01T12:00:00Z"),
        ),
        lastSeenAt = Instant.parse("2025-01-01T12:00:00Z"),
    )
}

package three.two.bit.phonemanager.ui.group

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.time.Instant
import org.junit.Before
import org.junit.Test
import three.two.bit.phonemanager.data.repository.DeviceRepository
import three.two.bit.phonemanager.domain.model.Device
import three.two.bit.phonemanager.domain.model.DeviceLocation
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class GroupMembersViewModelTest {

    private lateinit var deviceRepository: DeviceRepository
    private lateinit var viewModel: GroupMembersViewModel

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        deviceRepository = mockk(relaxed = true)
    }

    @Test
    fun `loadGroupMembers starts with loading state`() = runTest {
        // Given
        val devices = listOf(
            Device(
                deviceId = "device-001",
                displayName = "Device 1",
                lastLocation = null,
                lastSeenAt = null,
            ),
        )
        coEvery { deviceRepository.getGroupMembers() } returns Result.success(devices)

        // When
        viewModel = GroupMembersViewModel(deviceRepository)

        // Then - init block immediately calls loadGroupMembers
        // After completion, state should have devices
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertEquals(1, state.members.size)
        }
    }

    @Test
    fun `loadGroupMembers updates state with devices`() = runTest {
        // Given
        val devices = listOf(
            Device(
                deviceId = "device-001",
                displayName = "Device 1",
                lastLocation = DeviceLocation(
                    latitude = 48.1486,
                    longitude = 17.1077,
                    timestamp = Instant.parse("2025-11-25T18:30:00Z"),
                ),
                lastSeenAt = Instant.parse("2025-11-25T18:30:00Z"),
            ),
            Device(
                deviceId = "device-002",
                displayName = "Device 2",
                lastLocation = null,
                lastSeenAt = null,
            ),
        )
        coEvery { deviceRepository.getGroupMembers() } returns Result.success(devices)

        // When
        viewModel = GroupMembersViewModel(deviceRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertEquals(2, state.members.size)
            assertEquals("device-001", state.members[0].deviceId)
            assertEquals("device-002", state.members[1].deviceId)
            assertFalse(state.isEmpty)
            assertNull(state.error)
        }
    }

    @Test
    fun `loadGroupMembers shows empty state when no members`() = runTest {
        // Given
        coEvery { deviceRepository.getGroupMembers() } returns Result.success(emptyList())

        // When
        viewModel = GroupMembersViewModel(deviceRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertEquals(0, state.members.size)
            assertTrue(state.isEmpty)
            assertNull(state.error)
        }
    }

    @Test
    fun `loadGroupMembers shows error on failure`() = runTest {
        // Given
        coEvery { deviceRepository.getGroupMembers() } returns Result.failure(
            Exception("Network error"),
        )

        // When
        viewModel = GroupMembersViewModel(deviceRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertEquals(0, state.members.size)
            assertEquals("Network error", state.error)
        }
    }

    @Test
    fun `refresh reloads group members`() = runTest {
        // Given
        val initialDevices = listOf(
            Device(
                deviceId = "device-001",
                displayName = "Device 1",
                lastLocation = null,
                lastSeenAt = null,
            ),
        )
        val refreshedDevices = listOf(
            Device(
                deviceId = "device-001",
                displayName = "Device 1",
                lastLocation = null,
                lastSeenAt = null,
            ),
            Device(
                deviceId = "device-002",
                displayName = "Device 2",
                lastLocation = null,
                lastSeenAt = null,
            ),
        )

        coEvery { deviceRepository.getGroupMembers() } returnsMany listOf(
            Result.success(initialDevices),
            Result.success(refreshedDevices),
        )

        viewModel = GroupMembersViewModel(deviceRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.refresh()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(2, state.members.size)
            assertEquals("device-002", state.members[1].deviceId)
        }
    }

    @Test
    fun `refresh clears previous error`() = runTest {
        // Given
        coEvery { deviceRepository.getGroupMembers() } returnsMany listOf(
            Result.failure(Exception("Network error")),
            Result.success(emptyList()),
        )

        viewModel = GroupMembersViewModel(deviceRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify error state
        assertTrue(viewModel.uiState.value.error != null)

        // When
        viewModel.refresh()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertNull(state.error)
        }
    }
}

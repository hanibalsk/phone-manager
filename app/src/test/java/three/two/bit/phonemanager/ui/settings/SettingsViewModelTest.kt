package three.two.bit.phonemanager.ui.settings

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Before
import org.junit.Test
import three.two.bit.phonemanager.data.repository.DeviceRepository
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private lateinit var deviceRepository: DeviceRepository
    private lateinit var viewModel: SettingsViewModel

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        deviceRepository = mockk(relaxed = true)
    }

    @Test
    fun `init loads current settings from repository`() = runTest {
        // Given
        coEvery { deviceRepository.getDisplayName() } returns "Test Device"
        coEvery { deviceRepository.getGroupId() } returns "test-group"

        // When
        viewModel = SettingsViewModel(deviceRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Test Device", state.displayName)
            assertEquals("test-group", state.groupId)
            assertFalse(state.isLoading)
            assertFalse(state.hasChanges)
        }
    }

    @Test
    fun `onDisplayNameChanged updates displayName and hasChanges`() = runTest {
        // Given
        coEvery { deviceRepository.getDisplayName() } returns "Original"
        coEvery { deviceRepository.getGroupId() } returns "group"
        viewModel = SettingsViewModel(deviceRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.onDisplayNameChanged("New Name")

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("New Name", state.displayName)
            assertTrue(state.hasChanges)
        }
    }

    @Test
    fun `onGroupIdChanged updates groupId and hasChanges`() = runTest {
        // Given
        coEvery { deviceRepository.getDisplayName() } returns "Device"
        coEvery { deviceRepository.getGroupId() } returns "original-group"
        viewModel = SettingsViewModel(deviceRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.onGroupIdChanged("new-group")

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("new-group", state.groupId)
            assertTrue(state.hasChanges)
        }
    }

    @Test
    fun `onSaveClicked validates empty displayName`() = runTest {
        // Given
        coEvery { deviceRepository.getDisplayName() } returns "Device"
        coEvery { deviceRepository.getGroupId() } returns "group"
        viewModel = SettingsViewModel(deviceRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onDisplayNameChanged("")

        // When
        viewModel.onSaveClicked()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Display name cannot be empty", state.error)
        }
    }

    @Test
    fun `onSaveClicked validates empty groupId`() = runTest {
        // Given
        coEvery { deviceRepository.getDisplayName() } returns "Device"
        coEvery { deviceRepository.getGroupId() } returns "group"
        viewModel = SettingsViewModel(deviceRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onGroupIdChanged("")

        // When
        viewModel.onSaveClicked()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Group ID cannot be empty", state.error)
        }
    }

    @Test
    fun `onSaveClicked calls registerDevice and updates state on success`() = runTest {
        // Given
        coEvery { deviceRepository.getDisplayName() } returns "Original"
        coEvery { deviceRepository.getGroupId() } returns "original-group"
        coEvery {
            deviceRepository.registerDevice(
                displayName = "Updated Device",
                groupId = "new-group",
            )
        } returns Result.success(Unit)

        viewModel = SettingsViewModel(deviceRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onDisplayNameChanged("Updated Device")
        viewModel.onGroupIdChanged("new-group")

        // When
        viewModel.onSaveClicked()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify {
            deviceRepository.registerDevice(
                displayName = "Updated Device",
                groupId = "new-group",
            )
        }

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertTrue(state.saveSuccess)
            assertFalse(state.hasChanges)
            assertNull(state.error)
        }
    }

    @Test
    fun `onSaveClicked shows error on failure`() = runTest {
        // Given
        coEvery { deviceRepository.getDisplayName() } returns "Device"
        coEvery { deviceRepository.getGroupId() } returns "group"
        coEvery {
            deviceRepository.registerDevice(any(), any())
        } returns Result.failure(Exception("Network error"))

        viewModel = SettingsViewModel(deviceRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onDisplayNameChanged("New Name")

        // When
        viewModel.onSaveClicked()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertEquals("Network error", state.error)
            assertFalse(state.saveSuccess)
        }
    }

    @Test
    fun `hasChanges is false when values match original`() = runTest {
        // Given
        coEvery { deviceRepository.getDisplayName() } returns "Device"
        coEvery { deviceRepository.getGroupId() } returns "group"
        viewModel = SettingsViewModel(deviceRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        // When - change and revert
        viewModel.onDisplayNameChanged("Different")
        viewModel.onDisplayNameChanged("Device") // back to original

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.hasChanges)
        }
    }
}

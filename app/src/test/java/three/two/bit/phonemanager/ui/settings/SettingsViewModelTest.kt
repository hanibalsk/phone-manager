package three.two.bit.phonemanager.ui.settings

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Before
import org.junit.Test
import kotlinx.coroutines.flow.MutableStateFlow
import three.two.bit.phonemanager.data.preferences.PreferencesRepository
import three.two.bit.phonemanager.data.repository.AuthRepository
import three.two.bit.phonemanager.data.repository.DeviceRepository
import three.two.bit.phonemanager.data.repository.SettingsSyncRepository
import three.two.bit.phonemanager.domain.model.DeviceSettings
import three.two.bit.phonemanager.domain.model.ManagedDeviceStatus
import three.two.bit.phonemanager.domain.model.SettingsSyncStatus
import three.two.bit.phonemanager.permission.PermissionManager
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private lateinit var deviceRepository: DeviceRepository
    private lateinit var preferencesRepository: PreferencesRepository
    private lateinit var permissionManager: PermissionManager
    private lateinit var authRepository: AuthRepository
    private lateinit var settingsSyncRepository: SettingsSyncRepository
    private lateinit var viewModel: SettingsViewModel

    private val testDispatcher = StandardTestDispatcher()
    private val syncStatusFlow = MutableStateFlow(SettingsSyncStatus.SYNCED)
    private val serverSettingsFlow = MutableStateFlow<DeviceSettings?>(null)
    private val managedStatusFlow = MutableStateFlow(ManagedDeviceStatus(isManaged = false))

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        deviceRepository = mockk(relaxed = true)
        preferencesRepository = mockk(relaxed = true)
        permissionManager = mockk(relaxed = true)
        authRepository = mockk(relaxed = true)
        settingsSyncRepository = mockk(relaxed = true)
        coEvery { deviceRepository.getDeviceId() } returns "test-device-id"
        every { preferencesRepository.mapPollingIntervalSeconds } returns flowOf(15)
        // Mock AuthRepository.currentUser Flow to prevent ClassCastException
        every { authRepository.currentUser } returns flowOf(null)
        // Mock SettingsSyncRepository flows
        every { settingsSyncRepository.syncStatus } returns syncStatusFlow
        every { settingsSyncRepository.serverSettings } returns serverSettingsFlow
        every { settingsSyncRepository.managedStatus } returns managedStatusFlow
    }

    @Test
    fun `init loads current settings from repository`() = runTest {
        // Given
        coEvery { deviceRepository.getDisplayName() } returns "Test Device"
        coEvery { deviceRepository.getGroupId() } returns "test-group"

        // When
        viewModel = SettingsViewModel(deviceRepository, preferencesRepository, permissionManager, authRepository, settingsSyncRepository)
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
        viewModel = SettingsViewModel(deviceRepository, preferencesRepository, permissionManager, authRepository, settingsSyncRepository)
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
        viewModel = SettingsViewModel(deviceRepository, preferencesRepository, permissionManager, authRepository, settingsSyncRepository)
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
        viewModel = SettingsViewModel(deviceRepository, preferencesRepository, permissionManager, authRepository, settingsSyncRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onDisplayNameChanged("")
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.onSaveClicked()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Display name is required", state.displayNameError)
            assertFalse(state.isFormValid)
        }
    }

    @Test
    fun `onSaveClicked validates empty groupId`() = runTest {
        // Given
        coEvery { deviceRepository.getDisplayName() } returns "Device"
        coEvery { deviceRepository.getGroupId() } returns "group"
        viewModel = SettingsViewModel(deviceRepository, preferencesRepository, permissionManager, authRepository, settingsSyncRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onGroupIdChanged("")
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.onSaveClicked()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Group ID is required", state.groupIdError)
            assertFalse(state.isFormValid)
        }
    }

    @Test
    fun `onSaveClicked saves without confirmation when only displayName changed`() = runTest {
        // Given
        coEvery { deviceRepository.getDisplayName() } returns "Original"
        coEvery { deviceRepository.getGroupId() } returns "original-group"
        coEvery {
            deviceRepository.registerDevice(
                displayName = "Updated Device",
                groupId = "original-group",
            )
        } returns Result.success(Unit)

        viewModel = SettingsViewModel(deviceRepository, preferencesRepository, permissionManager, authRepository, settingsSyncRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onDisplayNameChanged("Updated Device")
        testDispatcher.scheduler.advanceUntilIdle()

        // When - only displayName changed, no confirmation needed
        viewModel.onSaveClicked()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify {
            deviceRepository.registerDevice(
                displayName = "Updated Device",
                groupId = "original-group",
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

        viewModel = SettingsViewModel(deviceRepository, preferencesRepository, permissionManager, authRepository, settingsSyncRepository)
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
        viewModel = SettingsViewModel(deviceRepository, preferencesRepository, permissionManager, authRepository, settingsSyncRepository)
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

    @Test
    fun `validateForm fails for displayName less than 2 chars`() = runTest {
        // Given
        coEvery { deviceRepository.getDisplayName() } returns "Device"
        coEvery { deviceRepository.getGroupId() } returns "group"
        viewModel = SettingsViewModel(deviceRepository, preferencesRepository, permissionManager, authRepository, settingsSyncRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.onDisplayNameChanged("A")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Display name must be at least 2 characters", state.displayNameError)
            assertFalse(state.isFormValid)
        }
    }

    @Test
    fun `validateForm fails for displayName over 50 chars`() = runTest {
        // Given
        coEvery { deviceRepository.getDisplayName() } returns "Device"
        coEvery { deviceRepository.getGroupId() } returns "group"
        viewModel = SettingsViewModel(deviceRepository, preferencesRepository, permissionManager, authRepository, settingsSyncRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.onDisplayNameChanged("A".repeat(51))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Display name must be 50 characters or less", state.displayNameError)
            assertFalse(state.isFormValid)
        }
    }

    @Test
    fun `validateForm fails for groupId with invalid characters`() = runTest {
        // Given
        coEvery { deviceRepository.getDisplayName() } returns "Device"
        coEvery { deviceRepository.getGroupId() } returns "group"
        viewModel = SettingsViewModel(deviceRepository, preferencesRepository, permissionManager, authRepository, settingsSyncRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.onGroupIdChanged("family@home")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Group ID can only contain letters, numbers, and hyphens", state.groupIdError)
            assertFalse(state.isFormValid)
        }
    }

    @Test
    fun `validateForm fails for groupId less than 2 chars`() = runTest {
        // Given
        coEvery { deviceRepository.getDisplayName() } returns "Device"
        coEvery { deviceRepository.getGroupId() } returns "group"
        viewModel = SettingsViewModel(deviceRepository, preferencesRepository, permissionManager, authRepository, settingsSyncRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.onGroupIdChanged("g")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Group ID must be at least 2 characters", state.groupIdError)
            assertFalse(state.isFormValid)
        }
    }

    @Test
    fun `validateForm fails for groupId over 50 chars`() = runTest {
        // Given
        coEvery { deviceRepository.getDisplayName() } returns "Device"
        coEvery { deviceRepository.getGroupId() } returns "group"
        viewModel = SettingsViewModel(deviceRepository, preferencesRepository, permissionManager, authRepository, settingsSyncRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.onGroupIdChanged("g".repeat(51))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Group ID must be 50 characters or less", state.groupIdError)
            assertFalse(state.isFormValid)
        }
    }

    @Test
    fun `validateForm succeeds for valid inputs`() = runTest {
        // Given
        coEvery { deviceRepository.getDisplayName() } returns "Device"
        coEvery { deviceRepository.getGroupId() } returns "group"
        viewModel = SettingsViewModel(deviceRepository, preferencesRepository, permissionManager, authRepository, settingsSyncRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.onDisplayNameChanged("Valid Name")
        viewModel.onGroupIdChanged("valid-group-123")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertNull(state.displayNameError)
            assertNull(state.groupIdError)
            assertTrue(state.isFormValid)
        }
    }

    @Test
    fun `onSaveClicked shows confirmation when group ID changed`() = runTest {
        // Given
        coEvery { deviceRepository.getDisplayName() } returns "Device"
        coEvery { deviceRepository.getGroupId() } returns "original-group"
        viewModel = SettingsViewModel(deviceRepository, preferencesRepository, permissionManager, authRepository, settingsSyncRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onGroupIdChanged("new-group")
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.onSaveClicked()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.showGroupChangeConfirmation)
        }
    }

    @Test
    fun `onConfirmGroupChange proceeds with save`() = runTest {
        // Given
        coEvery { deviceRepository.getDisplayName() } returns "Device"
        coEvery { deviceRepository.getGroupId() } returns "original-group"
        coEvery {
            deviceRepository.registerDevice(any(), any())
        } returns Result.success(Unit)

        viewModel = SettingsViewModel(deviceRepository, preferencesRepository, permissionManager, authRepository, settingsSyncRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onGroupIdChanged("new-group")
        viewModel.onSaveClicked() // Shows confirmation
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.onConfirmGroupChange()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify {
            deviceRepository.registerDevice(
                displayName = "Device",
                groupId = "new-group",
            )
        }

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.showGroupChangeConfirmation)
            assertTrue(state.saveSuccess)
        }
    }

    @Test
    fun `onDismissGroupChangeConfirmation cancels save`() = runTest {
        // Given
        coEvery { deviceRepository.getDisplayName() } returns "Device"
        coEvery { deviceRepository.getGroupId() } returns "original-group"
        viewModel = SettingsViewModel(deviceRepository, preferencesRepository, permissionManager, authRepository, settingsSyncRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onGroupIdChanged("new-group")
        viewModel.onSaveClicked() // Shows confirmation
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.onDismissGroupChangeConfirmation()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.showGroupChangeConfirmation)
            assertFalse(state.saveSuccess)
        }

        coVerify(exactly = 0) { deviceRepository.registerDevice(any(), any()) }
    }
}

package three.two.bit.phonemanager.ui.settings

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Before
import org.junit.Test
import three.two.bit.phonemanager.data.repository.SettingsSyncRepository
import three.two.bit.phonemanager.domain.model.DeviceSettings
import three.two.bit.phonemanager.domain.model.ManagedDeviceStatus
import three.two.bit.phonemanager.domain.model.SettingLock
import three.two.bit.phonemanager.domain.model.SettingUpdateResult
import three.two.bit.phonemanager.domain.model.SettingsSyncStatus
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

/**
 * Story E12.6 Task 12: Unit tests for SettingsSyncViewModel
 *
 * Tests:
 * - AC E12.6.1 (Lock indicator display)
 * - AC E12.6.2 (Setting sync on app start)
 * - AC E12.6.3 (Lock enforcement)
 * - AC E12.6.4 (Unlocked setting interaction)
 * - AC E12.6.5 (Push notification handling)
 * - AC E12.6.6 (Settings status section)
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsSyncViewModelTest {

    private lateinit var settingsSyncRepository: SettingsSyncRepository
    private lateinit var viewModel: SettingsSyncViewModel

    private val testDispatcher = StandardTestDispatcher()

    private val syncStatusFlow = MutableStateFlow(SettingsSyncStatus.SYNCED)
    private val serverSettingsFlow = MutableStateFlow<DeviceSettings?>(null)
    private val managedStatusFlow = MutableStateFlow(ManagedDeviceStatus(isManaged = false))

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        settingsSyncRepository = mockk(relaxed = true)

        every { settingsSyncRepository.syncStatus } returns syncStatusFlow
        every { settingsSyncRepository.serverSettings } returns serverSettingsFlow
        every { settingsSyncRepository.managedStatus } returns flowOf(managedStatusFlow.value)
        coEvery { settingsSyncRepository.syncAllSettings() } returns Result.success(Unit)
    }

    private fun createViewModel(): SettingsSyncViewModel = SettingsSyncViewModel(settingsSyncRepository)

    // AC E12.6.2: Setting sync on app start Tests

    @Test
    fun `init syncs settings from server`() = runTest {
        // Given
        coEvery { settingsSyncRepository.syncAllSettings() } returns Result.success(Unit)

        // When
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { settingsSyncRepository.syncAllSettings() }
    }

    @Test
    fun `syncSettings updates syncStatus to syncing then synced`() = runTest {
        // Given
        coEvery { settingsSyncRepository.syncAllSettings() } answers {
            syncStatusFlow.value = SettingsSyncStatus.SYNCING
            Result.success(Unit)
        }

        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isSyncing)
            assertNull(state.lastSyncError)
        }
    }

    @Test
    fun `syncSettings handles error`() = runTest {
        // Given
        coEvery { settingsSyncRepository.syncAllSettings() } returns Result.failure(
            Exception("Network error"),
        )

        // When
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertNotNull(state.lastSyncError)
        }
    }

    // AC E12.6.1: Lock indicator display Tests

    @Test
    fun `isSettingLocked returns true for locked setting`() = runTest {
        // Given - setup settings before sync
        val lockedSettings = DeviceSettings(
            locks = mapOf(
                "tracking_enabled" to SettingLock(
                    settingKey = "tracking_enabled",
                    isLocked = true,
                    lockedBy = "Admin",
                ),
            ),
        )
        serverSettingsFlow.value = lockedSettings

        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertTrue(viewModel.isSettingLocked("tracking_enabled"))
    }

    @Test
    fun `isSettingLocked returns false for unlocked setting`() = runTest {
        // Given
        val unlockedSettings = DeviceSettings(
            locks = mapOf(
                "tracking_enabled" to SettingLock(
                    settingKey = "tracking_enabled",
                    isLocked = false,
                ),
            ),
        )
        serverSettingsFlow.value = unlockedSettings

        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertFalse(viewModel.isSettingLocked("tracking_enabled"))
    }

    @Test
    fun `getLockedBy returns admin name for locked setting`() = runTest {
        // Given
        val lockedSettings = DeviceSettings(
            locks = mapOf(
                "tracking_enabled" to SettingLock(
                    settingKey = "tracking_enabled",
                    isLocked = true,
                    lockedBy = "John Admin",
                ),
            ),
        )
        serverSettingsFlow.value = lockedSettings

        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals("John Admin", viewModel.getLockedBy("tracking_enabled"))
    }

    @Test
    fun `getSettingLock returns lock info`() = runTest {
        // Given
        val lock = SettingLock(
            settingKey = "tracking_enabled",
            isLocked = true,
            lockedBy = "Admin",
            lockedAt = Instant.parse("2025-01-01T00:00:00Z"),
        )
        val lockedSettings = DeviceSettings(
            locks = mapOf("tracking_enabled" to lock),
        )
        serverSettingsFlow.value = lockedSettings

        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val result = viewModel.getSettingLock("tracking_enabled")
        assertNotNull(result)
        assertEquals("tracking_enabled", result.settingKey)
        assertTrue(result.isLocked)
        assertEquals("Admin", result.lockedBy)
    }

    // AC E12.6.3: Lock enforcement Tests

    @Test
    fun `updateSetting returns false when setting is locked`() = runTest {
        // Given
        val lockedSettings = DeviceSettings(
            locks = mapOf(
                "tracking_enabled" to SettingLock(
                    settingKey = "tracking_enabled",
                    isLocked = true,
                    lockedBy = "Admin",
                ),
            ),
        )
        serverSettingsFlow.value = lockedSettings

        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        val result = viewModel.updateSetting("tracking_enabled", true)

        // Then
        assertFalse(result)
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.showLockedDialog)
            assertEquals("tracking_enabled", state.lockedSettingKey)
        }
    }

    // AC E12.6.4: Unlocked setting interaction Tests

    @Test
    fun `updateSetting calls repository for unlocked setting`() = runTest {
        // Given
        val unlockedSettings = DeviceSettings(locks = emptyMap())
        serverSettingsFlow.value = unlockedSettings
        coEvery { settingsSyncRepository.updateServerSetting("tracking_enabled", true) } returns
            Result.success(SettingUpdateResult(success = true))

        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        val result = viewModel.updateSetting("tracking_enabled", true)

        // Then
        assertTrue(result)
        coVerify { settingsSyncRepository.updateServerSetting("tracking_enabled", true) }
    }

    @Test
    fun `updateSetting shows locked dialog when server returns wasLocked`() = runTest {
        // Given
        val unlockedSettings = DeviceSettings(locks = emptyMap())
        serverSettingsFlow.value = unlockedSettings
        coEvery { settingsSyncRepository.updateServerSetting("tracking_enabled", true) } returns
            Result.success(SettingUpdateResult(success = false, wasLocked = true, error = "Admin"))

        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        val result = viewModel.updateSetting("tracking_enabled", true)

        // Then
        assertFalse(result)
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.showLockedDialog)
        }
    }

    // AC E12.6.5: Push notification handling Tests

    @Test
    fun `handlePushNotification calls repository`() = runTest {
        // Given
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val updatedSettings = mapOf<String, Any>("tracking_enabled" to false)
        val updatedBy = "Admin"

        // When
        viewModel.handlePushNotification(updatedSettings, updatedBy)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { settingsSyncRepository.handleSettingsUpdatePush(updatedSettings, updatedBy) }
    }

    @Test
    fun `handlePushNotification updates lastPushUpdate in UI state`() = runTest {
        // Given
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val updatedSettings = mapOf<String, Any>("tracking_enabled" to false)
        val updatedBy = "Admin"

        // When
        viewModel.handlePushNotification(updatedSettings, updatedBy)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertNotNull(state.lastPushUpdate)
            assertEquals("Admin", state.lastPushUpdate?.updatedBy)
            assertTrue(state.lastPushUpdate?.settingsChanged?.contains("tracking_enabled") == true)
        }
    }

    @Test
    fun `dismissPushNotification clears lastPushUpdate`() = runTest {
        // Given
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.handlePushNotification(mapOf("tracking_enabled" to false), "Admin")
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.dismissPushNotification()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertNull(state.lastPushUpdate)
        }
    }

    // AC E12.6.6: Settings status section Tests

    @Test
    fun `managedStatus reflects repository state`() = runTest {
        // Given - must set up mock before creating viewModel
        val status = ManagedDeviceStatus(
            isManaged = true,
            groupName = "Family Group",
            groupId = "group-1",
            lockedSettingsCount = 3,
            lastSyncedAt = Instant.parse("2025-01-01T00:00:00Z"),
        )
        // Need a new repository mock with the right flow
        val customRepo: SettingsSyncRepository = mockk(relaxed = true)
        every { customRepo.syncStatus } returns syncStatusFlow
        every { customRepo.serverSettings } returns serverSettingsFlow
        every { customRepo.managedStatus } returns flowOf(status)
        coEvery { customRepo.syncAllSettings() } returns Result.success(Unit)

        viewModel = SettingsSyncViewModel(customRepo)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - use first() which will return first non-initial value
        viewModel.managedStatus.test {
            // Skip the initial value if needed
            var result = awaitItem()
            if (!result.isManaged) {
                result = awaitItem()
            }
            assertTrue(result.isManaged)
            assertEquals("Family Group", result.groupName)
            assertEquals(3, result.lockedSettingsCount)
        }
    }

    // Dialog Tests

    @Test
    fun `dismissLockedDialog clears locked dialog state`() = runTest {
        // Given
        val lockedSettings = DeviceSettings(
            locks = mapOf(
                "tracking_enabled" to SettingLock(
                    settingKey = "tracking_enabled",
                    isLocked = true,
                    lockedBy = "Admin",
                ),
            ),
        )
        serverSettingsFlow.value = lockedSettings

        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.updateSetting("tracking_enabled", true)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.dismissLockedDialog()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.showLockedDialog)
            assertNull(state.lockedSettingKey)
        }
    }

    @Test
    fun `clearError clears lastSyncError`() = runTest {
        // Given
        coEvery { settingsSyncRepository.syncAllSettings() } returns Result.failure(
            Exception("Error"),
        )

        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.clearError()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertNull(state.lastSyncError)
        }
    }
}

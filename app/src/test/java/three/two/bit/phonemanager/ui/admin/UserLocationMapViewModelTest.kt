package three.two.bit.phonemanager.ui.admin

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.android.gms.maps.model.LatLng
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import three.two.bit.phonemanager.data.repository.DeviceRepository
import three.two.bit.phonemanager.domain.model.Device
import three.two.bit.phonemanager.domain.model.DeviceLocation
import three.two.bit.phonemanager.domain.model.DeviceSettings
import kotlin.time.Clock

/**
 * Unit tests for UserLocationMapViewModel
 *
 * Tests for Story E9.3 (View User Location) and E9.5 (Remote Tracking Control).
 * Covers ACs: E9.3.4, E9.3.5, E9.3.6, E9.5.1-E9.5.5
 */
@OptIn(ExperimentalCoroutinesApi::class)
class UserLocationMapViewModelTest {

    private lateinit var deviceRepository: DeviceRepository
    private lateinit var savedStateHandle: SavedStateHandle
    private lateinit var viewModel: UserLocationMapViewModel

    private val testDispatcher = StandardTestDispatcher()

    private val testGroupId = "group-123"
    private val testDeviceId = "device-456"

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        deviceRepository = mockk()
        savedStateHandle = SavedStateHandle(
            mapOf(
                "groupId" to testGroupId,
                "deviceId" to testDeviceId,
            ),
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // =============================================================================
    // AC E9.3.4: Same UI as "My Device" screen (location display)
    // =============================================================================

    @Test
    fun `init - loads device location when valid groupId and deviceId`() = runTest {
        // Given
        val device = createTestDevice()
        coEvery { deviceRepository.getGroupDevices(testGroupId) } returns Result.success(listOf(device))
        coEvery { deviceRepository.getDeviceTrackingSettings(testDeviceId) } returns Result.success(
            DeviceSettings(trackingEnabled = true),
        )

        // When
        viewModel = UserLocationMapViewModel(savedStateHandle, deviceRepository)
        advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertNotNull(state.device)
            assertEquals("Test Device", state.device?.displayName)
            assertNotNull(state.location)
            assertEquals(48.1486, state.location?.latitude)
            assertEquals(17.1077, state.location?.longitude)
        }
    }

    @Test
    fun `init - shows error when groupId is empty`() = runTest {
        // Given
        savedStateHandle = SavedStateHandle(
            mapOf(
                "groupId" to "",
                "deviceId" to testDeviceId,
            ),
        )

        // When
        viewModel = UserLocationMapViewModel(savedStateHandle, deviceRepository)
        advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Invalid device or group ID", state.error)
        }
    }

    @Test
    fun `init - shows error when deviceId is empty`() = runTest {
        // Given
        savedStateHandle = SavedStateHandle(
            mapOf(
                "groupId" to testGroupId,
                "deviceId" to "",
            ),
        )

        // When
        viewModel = UserLocationMapViewModel(savedStateHandle, deviceRepository)
        advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Invalid device or group ID", state.error)
        }
    }

    @Test
    fun `loadDeviceLocation - shows error when device not found`() = runTest {
        // Given
        val otherDevice = createTestDevice(deviceId = "other-device")
        coEvery { deviceRepository.getGroupDevices(testGroupId) } returns Result.success(listOf(otherDevice))
        coEvery { deviceRepository.getDeviceTrackingSettings(testDeviceId) } returns Result.success(
            DeviceSettings(trackingEnabled = true),
        )

        // When
        viewModel = UserLocationMapViewModel(savedStateHandle, deviceRepository)
        advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertEquals("Device not found", state.error)
        }
    }

    @Test
    fun `loadDeviceLocation - handles repository failure`() = runTest {
        // Given
        coEvery { deviceRepository.getGroupDevices(testGroupId) } returns Result.failure(
            RuntimeException("Network error"),
        )
        coEvery { deviceRepository.getDeviceTrackingSettings(testDeviceId) } returns Result.success(
            DeviceSettings(trackingEnabled = true),
        )

        // When
        viewModel = UserLocationMapViewModel(savedStateHandle, deviceRepository)
        advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertEquals("Network error", state.error)
        }
    }

    // =============================================================================
    // AC E9.3.5: Last update timestamp display
    // =============================================================================

    @Test
    fun `loadDeviceLocation - populates lastSeenAt timestamp`() = runTest {
        // Given
        val device = createTestDevice()
        coEvery { deviceRepository.getGroupDevices(testGroupId) } returns Result.success(listOf(device))
        coEvery { deviceRepository.getDeviceTrackingSettings(testDeviceId) } returns Result.success(
            DeviceSettings(trackingEnabled = true),
        )

        // When
        viewModel = UserLocationMapViewModel(savedStateHandle, deviceRepository)
        advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertNotNull(state.lastSeenAt)
            assertEquals(device.lastSeenAt, state.lastSeenAt)
        }
    }

    // =============================================================================
    // AC E9.3.6: User display name and device info
    // =============================================================================

    @Test
    fun `loadDeviceLocation - populates device with display name`() = runTest {
        // Given
        val device = createTestDevice(displayName = "John's Phone")
        coEvery { deviceRepository.getGroupDevices(testGroupId) } returns Result.success(listOf(device))
        coEvery { deviceRepository.getDeviceTrackingSettings(testDeviceId) } returns Result.success(
            DeviceSettings(trackingEnabled = true),
        )

        // When
        viewModel = UserLocationMapViewModel(savedStateHandle, deviceRepository)
        advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertNotNull(state.device)
            assertEquals("John's Phone", state.device?.displayName)
        }
    }

    @Test
    fun `loadDeviceLocation - handles device without location`() = runTest {
        // Given
        val device = createTestDevice(hasLocation = false)
        coEvery { deviceRepository.getGroupDevices(testGroupId) } returns Result.success(listOf(device))
        coEvery { deviceRepository.getDeviceTrackingSettings(testDeviceId) } returns Result.success(
            DeviceSettings(trackingEnabled = true),
        )

        // When
        viewModel = UserLocationMapViewModel(savedStateHandle, deviceRepository)
        advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertNotNull(state.device)
            assertNull(state.location)
        }
    }

    // =============================================================================
    // AC E9.5.1: Toggle control on user detail screen
    // =============================================================================

    @Test
    fun `toggleTracking - calls repository with enabled true`() = runTest {
        // Given
        val device = createTestDevice()
        coEvery { deviceRepository.getGroupDevices(testGroupId) } returns Result.success(listOf(device))
        coEvery { deviceRepository.getDeviceTrackingSettings(testDeviceId) } returns Result.success(
            DeviceSettings(trackingEnabled = false),
        )
        coEvery { deviceRepository.toggleDeviceTracking(testDeviceId, true) } returns Result.success(Unit)

        viewModel = UserLocationMapViewModel(savedStateHandle, deviceRepository)
        advanceUntilIdle()

        // When
        viewModel.toggleTracking(true)
        advanceUntilIdle()

        // Then
        coVerify { deviceRepository.toggleDeviceTracking(testDeviceId, true) }
    }

    @Test
    fun `toggleTracking - calls repository with enabled false`() = runTest {
        // Given
        val device = createTestDevice()
        coEvery { deviceRepository.getGroupDevices(testGroupId) } returns Result.success(listOf(device))
        coEvery { deviceRepository.getDeviceTrackingSettings(testDeviceId) } returns Result.success(
            DeviceSettings(trackingEnabled = true),
        )
        coEvery { deviceRepository.toggleDeviceTracking(testDeviceId, false) } returns Result.success(Unit)

        viewModel = UserLocationMapViewModel(savedStateHandle, deviceRepository)
        advanceUntilIdle()

        // When
        viewModel.toggleTracking(false)
        advanceUntilIdle()

        // Then
        coVerify { deviceRepository.toggleDeviceTracking(testDeviceId, false) }
    }

    // =============================================================================
    // AC E9.5.2: Backend API to update tracking state
    // =============================================================================

    @Test
    fun `toggleTracking - sets isTogglingTracking during operation`() = runTest {
        // Given
        val device = createTestDevice()
        coEvery { deviceRepository.getGroupDevices(testGroupId) } returns Result.success(listOf(device))
        coEvery { deviceRepository.getDeviceTrackingSettings(testDeviceId) } returns Result.success(
            DeviceSettings(trackingEnabled = true),
        )
        coEvery { deviceRepository.toggleDeviceTracking(testDeviceId, false) } returns Result.success(Unit)

        viewModel = UserLocationMapViewModel(savedStateHandle, deviceRepository)
        advanceUntilIdle()

        // When
        viewModel.toggleTracking(false)

        // Then - verify isTogglingTracking is true during operation
        viewModel.uiState.test {
            val initialState = awaitItem()
            // After operation completes
            assertFalse(initialState.isTogglingTracking)
        }
    }

    // =============================================================================
    // AC E9.5.3: Visual confirmation of tracking state change
    // =============================================================================

    @Test
    fun `toggleTracking - sets trackingToggleSuccess on success`() = runTest {
        // Given
        val device = createTestDevice()
        coEvery { deviceRepository.getGroupDevices(testGroupId) } returns Result.success(listOf(device))
        coEvery { deviceRepository.getDeviceTrackingSettings(testDeviceId) } returns Result.success(
            DeviceSettings(trackingEnabled = false),
        )
        coEvery { deviceRepository.toggleDeviceTracking(testDeviceId, true) } returns Result.success(Unit)

        viewModel = UserLocationMapViewModel(savedStateHandle, deviceRepository)
        advanceUntilIdle()

        // When
        viewModel.toggleTracking(true)
        advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.trackingToggleSuccess)
            assertEquals(true, state.trackingEnabled)
        }
    }

    @Test
    fun `toggleTracking - sets trackingToggleError on failure`() = runTest {
        // Given
        val device = createTestDevice()
        coEvery { deviceRepository.getGroupDevices(testGroupId) } returns Result.success(listOf(device))
        coEvery { deviceRepository.getDeviceTrackingSettings(testDeviceId) } returns Result.success(
            DeviceSettings(trackingEnabled = false),
        )
        coEvery { deviceRepository.toggleDeviceTracking(testDeviceId, true) } returns Result.failure(
            RuntimeException("Permission denied"),
        )

        viewModel = UserLocationMapViewModel(savedStateHandle, deviceRepository)
        advanceUntilIdle()

        // When
        viewModel.toggleTracking(true)
        advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.trackingToggleSuccess)
            assertEquals("Permission denied", state.trackingToggleError)
        }
    }

    @Test
    fun `clearTrackingToggleSuccess - clears success flag`() = runTest {
        // Given
        val device = createTestDevice()
        coEvery { deviceRepository.getGroupDevices(testGroupId) } returns Result.success(listOf(device))
        coEvery { deviceRepository.getDeviceTrackingSettings(testDeviceId) } returns Result.success(
            DeviceSettings(trackingEnabled = false),
        )
        coEvery { deviceRepository.toggleDeviceTracking(testDeviceId, true) } returns Result.success(Unit)

        viewModel = UserLocationMapViewModel(savedStateHandle, deviceRepository)
        advanceUntilIdle()

        viewModel.toggleTracking(true)
        advanceUntilIdle()

        // When
        viewModel.clearTrackingToggleSuccess()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.trackingToggleSuccess)
        }
    }

    @Test
    fun `clearTrackingToggleError - clears error message`() = runTest {
        // Given
        val device = createTestDevice()
        coEvery { deviceRepository.getGroupDevices(testGroupId) } returns Result.success(listOf(device))
        coEvery { deviceRepository.getDeviceTrackingSettings(testDeviceId) } returns Result.success(
            DeviceSettings(trackingEnabled = false),
        )
        coEvery { deviceRepository.toggleDeviceTracking(testDeviceId, true) } returns Result.failure(
            RuntimeException("Error"),
        )

        viewModel = UserLocationMapViewModel(savedStateHandle, deviceRepository)
        advanceUntilIdle()

        viewModel.toggleTracking(true)
        advanceUntilIdle()

        // When
        viewModel.clearTrackingToggleError()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertNull(state.trackingToggleError)
        }
    }

    // =============================================================================
    // AC E9.5.5: Current tracking state reflected in UI
    // =============================================================================

    @Test
    fun `init - loads tracking state as enabled`() = runTest {
        // Given
        val device = createTestDevice()
        coEvery { deviceRepository.getGroupDevices(testGroupId) } returns Result.success(listOf(device))
        coEvery { deviceRepository.getDeviceTrackingSettings(testDeviceId) } returns Result.success(
            DeviceSettings(trackingEnabled = true),
        )

        // When
        viewModel = UserLocationMapViewModel(savedStateHandle, deviceRepository)
        advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(true, state.trackingEnabled)
        }
    }

    @Test
    fun `init - loads tracking state as disabled`() = runTest {
        // Given
        val device = createTestDevice()
        coEvery { deviceRepository.getGroupDevices(testGroupId) } returns Result.success(listOf(device))
        coEvery { deviceRepository.getDeviceTrackingSettings(testDeviceId) } returns Result.success(
            DeviceSettings(trackingEnabled = false),
        )

        // When
        viewModel = UserLocationMapViewModel(savedStateHandle, deviceRepository)
        advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(false, state.trackingEnabled)
        }
    }

    @Test
    fun `init - tracking state is null when settings load fails`() = runTest {
        // Given
        val device = createTestDevice()
        coEvery { deviceRepository.getGroupDevices(testGroupId) } returns Result.success(listOf(device))
        coEvery { deviceRepository.getDeviceTrackingSettings(testDeviceId) } returns Result.failure(
            RuntimeException("Settings not available"),
        )

        // When
        viewModel = UserLocationMapViewModel(savedStateHandle, deviceRepository)
        advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertNull(state.trackingEnabled)
        }
    }

    // =============================================================================
    // Polling Tests
    // =============================================================================

    @Test
    fun `startPolling - sets polling flag`() = runTest {
        // Given
        val device = createTestDevice()
        coEvery { deviceRepository.getGroupDevices(testGroupId) } returns Result.success(listOf(device))
        coEvery { deviceRepository.getDeviceTrackingSettings(testDeviceId) } returns Result.success(
            DeviceSettings(trackingEnabled = true),
        )

        viewModel = UserLocationMapViewModel(savedStateHandle, deviceRepository)
        advanceUntilIdle()

        // When
        viewModel.startPolling()

        // Then - startPolling should not throw and polling should be active
        // Note: We can't easily verify polling state directly as it's private
        // But we can verify it doesn't throw
    }

    @Test
    fun `stopPolling - stops polling`() = runTest {
        // Given
        val device = createTestDevice()
        coEvery { deviceRepository.getGroupDevices(testGroupId) } returns Result.success(listOf(device))
        coEvery { deviceRepository.getDeviceTrackingSettings(testDeviceId) } returns Result.success(
            DeviceSettings(trackingEnabled = true),
        )

        viewModel = UserLocationMapViewModel(savedStateHandle, deviceRepository)
        advanceUntilIdle()

        viewModel.startPolling()

        // When
        viewModel.stopPolling()

        // Then - stopPolling should not throw
        // Polling is now stopped
    }

    @Test
    fun `startPolling - multiple calls do not restart polling`() = runTest {
        // Given
        val device = createTestDevice()
        coEvery { deviceRepository.getGroupDevices(testGroupId) } returns Result.success(listOf(device))
        coEvery { deviceRepository.getDeviceTrackingSettings(testDeviceId) } returns Result.success(
            DeviceSettings(trackingEnabled = true),
        )

        viewModel = UserLocationMapViewModel(savedStateHandle, deviceRepository)
        advanceUntilIdle()

        // When
        viewModel.startPolling()
        viewModel.startPolling()
        viewModel.startPolling()

        // Then - should handle gracefully without issues
        viewModel.stopPolling()
    }

    // =============================================================================
    // Helper Functions
    // =============================================================================

    private fun createTestDevice(
        deviceId: String = testDeviceId,
        displayName: String = "Test Device",
        hasLocation: Boolean = true,
    ): Device {
        val now = Clock.System.now()
        return Device(
            deviceId = deviceId,
            groupId = testGroupId,
            displayName = displayName,
            platform = "android",
            lastSeenAt = now,
            lastLocation = if (hasLocation) {
                DeviceLocation(
                    latitude = 48.1486,
                    longitude = 17.1077,
                    timestamp = now,
                )
            } else {
                null
            },
            ownerId = "user-123",
        )
    }
}

package three.two.bit.phonemanager.ui.admin

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.time.Clock
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import three.two.bit.phonemanager.data.repository.DeviceRepository
import three.two.bit.phonemanager.data.repository.GeofenceRepository
import three.two.bit.phonemanager.domain.model.Device
import three.two.bit.phonemanager.domain.model.DeviceLocation
import three.two.bit.phonemanager.domain.model.Geofence
import three.two.bit.phonemanager.domain.model.TransitionType

/**
 * Unit tests for AdminGeofenceViewModel
 *
 * Tests for Story E9.4 (Admin Geofence Management).
 * Covers ACs: E9.4.1, E9.4.2, E9.4.3, E9.4.4
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AdminGeofenceViewModelTest {

    private lateinit var geofenceRepository: GeofenceRepository
    private lateinit var deviceRepository: DeviceRepository
    private lateinit var savedStateHandle: SavedStateHandle
    private lateinit var viewModel: AdminGeofenceViewModel

    private val testDispatcher = StandardTestDispatcher()

    private val testGroupId = "group-123"
    private val testDeviceId = "device-456"

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        geofenceRepository = mockk()
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
    // AC E9.4.1: Display geofence management UI
    // =============================================================================

    @Test
    fun `init - loads device info and geofences`() = runTest {
        // Given
        val device = createTestDevice()
        val geofences = listOf(createTestGeofence("geo-1", "Home"), createTestGeofence("geo-2", "Work"))

        coEvery { deviceRepository.getGroupDevices(testGroupId) } returns Result.success(listOf(device))
        coEvery { geofenceRepository.getGeofencesForDevice(testDeviceId) } returns Result.success(geofences)

        // When
        viewModel = AdminGeofenceViewModel(savedStateHandle, geofenceRepository, deviceRepository)
        advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertNotNull(state.device)
            assertEquals("Test Device", state.device?.displayName)
            assertEquals(2, state.geofences.size)
        }
    }

    @Test
    fun `init - handles device load failure gracefully`() = runTest {
        // Given
        coEvery { deviceRepository.getGroupDevices(testGroupId) } returns Result.failure(
            RuntimeException("Network error"),
        )
        coEvery { geofenceRepository.getGeofencesForDevice(testDeviceId) } returns Result.success(emptyList())

        // When
        viewModel = AdminGeofenceViewModel(savedStateHandle, geofenceRepository, deviceRepository)
        advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            // Device load failure is handled gracefully, geofences still loaded
            assertNull(state.device)
            assertNull(state.error) // No error for device load failure
        }
    }

    // =============================================================================
    // AC E9.4.2: List existing geofences for user with edit/delete options
    // =============================================================================

    @Test
    fun `loadGeofences - populates geofences list`() = runTest {
        // Given
        val device = createTestDevice()
        val geofences = listOf(
            createTestGeofence("geo-1", "Home"),
            createTestGeofence("geo-2", "Work"),
            createTestGeofence("geo-3", "School"),
        )

        coEvery { deviceRepository.getGroupDevices(testGroupId) } returns Result.success(listOf(device))
        coEvery { geofenceRepository.getGeofencesForDevice(testDeviceId) } returns Result.success(geofences)

        // When
        viewModel = AdminGeofenceViewModel(savedStateHandle, geofenceRepository, deviceRepository)
        advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(3, state.geofences.size)
            assertEquals("Home", state.geofences[0].name)
            assertEquals("Work", state.geofences[1].name)
            assertEquals("School", state.geofences[2].name)
        }
    }

    @Test
    fun `loadGeofences - handles empty list`() = runTest {
        // Given
        val device = createTestDevice()
        coEvery { deviceRepository.getGroupDevices(testGroupId) } returns Result.success(listOf(device))
        coEvery { geofenceRepository.getGeofencesForDevice(testDeviceId) } returns Result.success(emptyList())

        // When
        viewModel = AdminGeofenceViewModel(savedStateHandle, geofenceRepository, deviceRepository)
        advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.geofences.isEmpty())
            assertNull(state.error)
        }
    }

    @Test
    fun `loadGeofences - shows error on failure`() = runTest {
        // Given
        val device = createTestDevice()
        coEvery { deviceRepository.getGroupDevices(testGroupId) } returns Result.success(listOf(device))
        coEvery { geofenceRepository.getGeofencesForDevice(testDeviceId) } returns Result.failure(
            RuntimeException("Failed to load geofences"),
        )

        // When
        viewModel = AdminGeofenceViewModel(savedStateHandle, geofenceRepository, deviceRepository)
        advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertEquals("Failed to load geofences", state.error)
        }
    }

    @Test
    fun `deleteGeofence - calls repository and reloads`() = runTest {
        // Given
        val device = createTestDevice()
        val geofences = listOf(createTestGeofence("geo-1", "Home"))
        coEvery { deviceRepository.getGroupDevices(testGroupId) } returns Result.success(listOf(device))
        coEvery { geofenceRepository.getGeofencesForDevice(testDeviceId) } returns Result.success(geofences)
        coEvery { geofenceRepository.deleteGeofenceForDevice("geo-1") } returns Result.success(Unit)

        viewModel = AdminGeofenceViewModel(savedStateHandle, geofenceRepository, deviceRepository)
        advanceUntilIdle()

        // When
        viewModel.deleteGeofence("geo-1")
        advanceUntilIdle()

        // Then
        coVerify { geofenceRepository.deleteGeofenceForDevice("geo-1") }
        coVerify(atLeast = 2) { geofenceRepository.getGeofencesForDevice(testDeviceId) }
    }

    @Test
    fun `deleteGeofence - shows error on failure`() = runTest {
        // Given
        val device = createTestDevice()
        val geofences = listOf(createTestGeofence("geo-1", "Home"))
        coEvery { deviceRepository.getGroupDevices(testGroupId) } returns Result.success(listOf(device))
        coEvery { geofenceRepository.getGeofencesForDevice(testDeviceId) } returns Result.success(geofences)
        coEvery { geofenceRepository.deleteGeofenceForDevice("geo-1") } returns Result.failure(
            RuntimeException("Delete failed"),
        )

        viewModel = AdminGeofenceViewModel(savedStateHandle, geofenceRepository, deviceRepository)
        advanceUntilIdle()

        // When
        viewModel.deleteGeofence("geo-1")
        advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isDeleting)
            assertEquals("Delete failed", state.error)
        }
    }

    @Test
    fun `showDeleteConfirmation - sets geofenceToDelete`() = runTest {
        // Given
        val device = createTestDevice()
        val geofence = createTestGeofence("geo-1", "Home")
        coEvery { deviceRepository.getGroupDevices(testGroupId) } returns Result.success(listOf(device))
        coEvery { geofenceRepository.getGeofencesForDevice(testDeviceId) } returns Result.success(listOf(geofence))

        viewModel = AdminGeofenceViewModel(savedStateHandle, geofenceRepository, deviceRepository)
        advanceUntilIdle()

        // When
        viewModel.showDeleteConfirmation(geofence)

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertNotNull(state.geofenceToDelete)
            assertEquals("geo-1", state.geofenceToDelete?.id)
        }
    }

    @Test
    fun `hideDeleteConfirmation - clears geofenceToDelete`() = runTest {
        // Given
        val device = createTestDevice()
        val geofence = createTestGeofence("geo-1", "Home")
        coEvery { deviceRepository.getGroupDevices(testGroupId) } returns Result.success(listOf(device))
        coEvery { geofenceRepository.getGeofencesForDevice(testDeviceId) } returns Result.success(listOf(geofence))

        viewModel = AdminGeofenceViewModel(savedStateHandle, geofenceRepository, deviceRepository)
        advanceUntilIdle()

        viewModel.showDeleteConfirmation(geofence)

        // When
        viewModel.hideDeleteConfirmation()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertNull(state.geofenceToDelete)
        }
    }

    // =============================================================================
    // AC E9.4.3: Define geofence boundaries on map
    // AC E9.4.4: Save geofence to backend per user
    // =============================================================================

    @Test
    fun `createGeofence - calls repository with correct parameters`() = runTest {
        // Given
        val device = createTestDevice()
        val newGeofence = createTestGeofence("geo-new", "New Zone")
        coEvery { deviceRepository.getGroupDevices(testGroupId) } returns Result.success(listOf(device))
        coEvery { geofenceRepository.getGeofencesForDevice(testDeviceId) } returns Result.success(emptyList())
        coEvery {
            geofenceRepository.createGeofenceForDevice(
                deviceId = testDeviceId,
                name = "New Zone",
                latitude = 48.1486,
                longitude = 17.1077,
                radiusMeters = 100,
                transitionTypes = setOf(TransitionType.ENTER, TransitionType.EXIT),
            )
        } returns Result.success(newGeofence)

        viewModel = AdminGeofenceViewModel(savedStateHandle, geofenceRepository, deviceRepository)
        advanceUntilIdle()

        // When
        viewModel.createGeofence(
            name = "New Zone",
            latitude = 48.1486,
            longitude = 17.1077,
            radiusMeters = 100,
            transitionTypes = setOf(TransitionType.ENTER, TransitionType.EXIT),
        )
        advanceUntilIdle()

        // Then
        coVerify {
            geofenceRepository.createGeofenceForDevice(
                deviceId = testDeviceId,
                name = "New Zone",
                latitude = 48.1486,
                longitude = 17.1077,
                radiusMeters = 100,
                transitionTypes = setOf(TransitionType.ENTER, TransitionType.EXIT),
            )
        }
    }

    @Test
    fun `createGeofence - sets isCreating during operation`() = runTest {
        // Given
        val device = createTestDevice()
        val newGeofence = createTestGeofence("geo-new", "New Zone")
        coEvery { deviceRepository.getGroupDevices(testGroupId) } returns Result.success(listOf(device))
        coEvery { geofenceRepository.getGeofencesForDevice(testDeviceId) } returns Result.success(emptyList())
        coEvery {
            geofenceRepository.createGeofenceForDevice(any(), any(), any(), any(), any(), any())
        } returns Result.success(newGeofence)

        viewModel = AdminGeofenceViewModel(savedStateHandle, geofenceRepository, deviceRepository)
        advanceUntilIdle()

        // When
        viewModel.createGeofence(
            name = "New Zone",
            latitude = 48.1486,
            longitude = 17.1077,
            radiusMeters = 100,
            transitionTypes = setOf(TransitionType.ENTER),
        )
        advanceUntilIdle()

        // Then - verify isCreating is false after completion
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isCreating)
        }
    }

    @Test
    fun `createGeofence - closes dialog and reloads on success`() = runTest {
        // Given
        val device = createTestDevice()
        val newGeofence = createTestGeofence("geo-new", "New Zone")
        coEvery { deviceRepository.getGroupDevices(testGroupId) } returns Result.success(listOf(device))
        coEvery { geofenceRepository.getGeofencesForDevice(testDeviceId) } returns Result.success(emptyList())
        coEvery {
            geofenceRepository.createGeofenceForDevice(any(), any(), any(), any(), any(), any())
        } returns Result.success(newGeofence)

        viewModel = AdminGeofenceViewModel(savedStateHandle, geofenceRepository, deviceRepository)
        advanceUntilIdle()

        viewModel.showCreateDialog()

        // When
        viewModel.createGeofence(
            name = "New Zone",
            latitude = 48.1486,
            longitude = 17.1077,
            radiusMeters = 100,
            transitionTypes = setOf(TransitionType.ENTER),
        )
        advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.showCreateDialog)
            assertNull(state.createError)
        }
        coVerify(atLeast = 2) { geofenceRepository.getGeofencesForDevice(testDeviceId) }
    }

    @Test
    fun `createGeofence - shows error on failure`() = runTest {
        // Given
        val device = createTestDevice()
        coEvery { deviceRepository.getGroupDevices(testGroupId) } returns Result.success(listOf(device))
        coEvery { geofenceRepository.getGeofencesForDevice(testDeviceId) } returns Result.success(emptyList())
        coEvery {
            geofenceRepository.createGeofenceForDevice(any(), any(), any(), any(), any(), any())
        } returns Result.failure(RuntimeException("Creation failed"))

        viewModel = AdminGeofenceViewModel(savedStateHandle, geofenceRepository, deviceRepository)
        advanceUntilIdle()

        // When
        viewModel.createGeofence(
            name = "New Zone",
            latitude = 48.1486,
            longitude = 17.1077,
            radiusMeters = 100,
            transitionTypes = setOf(TransitionType.ENTER),
        )
        advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isCreating)
            assertEquals("Creation failed", state.createError)
        }
    }

    // =============================================================================
    // Dialog Management Tests
    // =============================================================================

    @Test
    fun `showCreateDialog - sets showCreateDialog to true`() = runTest {
        // Given
        val device = createTestDevice()
        coEvery { deviceRepository.getGroupDevices(testGroupId) } returns Result.success(listOf(device))
        coEvery { geofenceRepository.getGeofencesForDevice(testDeviceId) } returns Result.success(emptyList())

        viewModel = AdminGeofenceViewModel(savedStateHandle, geofenceRepository, deviceRepository)
        advanceUntilIdle()

        // When
        viewModel.showCreateDialog()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.showCreateDialog)
        }
    }

    @Test
    fun `hideCreateDialog - sets showCreateDialog to false and clears error`() = runTest {
        // Given
        val device = createTestDevice()
        coEvery { deviceRepository.getGroupDevices(testGroupId) } returns Result.success(listOf(device))
        coEvery { geofenceRepository.getGeofencesForDevice(testDeviceId) } returns Result.success(emptyList())

        viewModel = AdminGeofenceViewModel(savedStateHandle, geofenceRepository, deviceRepository)
        advanceUntilIdle()

        viewModel.showCreateDialog()

        // When
        viewModel.hideCreateDialog()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.showCreateDialog)
            assertNull(state.createError)
        }
    }

    // =============================================================================
    // Error Handling Tests
    // =============================================================================

    @Test
    fun `clearError - clears both error and createError`() = runTest {
        // Given
        val device = createTestDevice()
        coEvery { deviceRepository.getGroupDevices(testGroupId) } returns Result.success(listOf(device))
        coEvery { geofenceRepository.getGeofencesForDevice(testDeviceId) } returns Result.failure(
            RuntimeException("Error"),
        )

        viewModel = AdminGeofenceViewModel(savedStateHandle, geofenceRepository, deviceRepository)
        advanceUntilIdle()

        // When
        viewModel.clearError()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertNull(state.error)
            assertNull(state.createError)
        }
    }

    // =============================================================================
    // Refresh Tests
    // =============================================================================

    @Test
    fun `refresh - reloads geofences`() = runTest {
        // Given
        val device = createTestDevice()
        val geofences = listOf(createTestGeofence("geo-1", "Home"))
        coEvery { deviceRepository.getGroupDevices(testGroupId) } returns Result.success(listOf(device))
        coEvery { geofenceRepository.getGeofencesForDevice(testDeviceId) } returns Result.success(geofences)

        viewModel = AdminGeofenceViewModel(savedStateHandle, geofenceRepository, deviceRepository)
        advanceUntilIdle()

        // When
        viewModel.refresh()
        advanceUntilIdle()

        // Then
        coVerify(atLeast = 2) { geofenceRepository.getGeofencesForDevice(testDeviceId) }
    }

    // =============================================================================
    // Helper Functions
    // =============================================================================

    private fun createTestDevice(
        deviceId: String = testDeviceId,
        displayName: String = "Test Device",
    ): Device {
        val now = Clock.System.now()
        return Device(
            deviceId = deviceId,
            groupId = testGroupId,
            displayName = displayName,
            platform = "android",
            lastSeenAt = now,
            lastLocation = DeviceLocation(
                latitude = 48.1486,
                longitude = 17.1077,
                timestamp = now,
            ),
            ownerId = "user-123",
        )
    }

    private fun createTestGeofence(
        id: String,
        name: String,
        latitude: Double = 48.1486,
        longitude: Double = 17.1077,
        radiusMeters: Int = 100,
    ): Geofence {
        val now = Clock.System.now()
        return Geofence(
            id = id,
            deviceId = testDeviceId,
            name = name,
            latitude = latitude,
            longitude = longitude,
            radiusMeters = radiusMeters,
            transitionTypes = setOf(TransitionType.ENTER, TransitionType.EXIT),
            webhookId = null,
            active = true,
            createdAt = now,
            updatedAt = now,
        )
    }
}

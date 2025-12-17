package three.two.bit.phonemanager.ui.map

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Before
import org.junit.Test
import three.two.bit.phonemanager.data.model.LocationEntity
import three.two.bit.phonemanager.data.preferences.PreferencesRepository
import three.two.bit.phonemanager.data.repository.DeviceRepository
import three.two.bit.phonemanager.domain.model.Device
import three.two.bit.phonemanager.domain.model.DeviceLocation
import three.two.bit.phonemanager.location.LocationManager
import three.two.bit.phonemanager.proximity.ProximityManager
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class MapViewModelTest {
    private lateinit var locationManager: LocationManager
    private lateinit var deviceRepository: DeviceRepository
    private lateinit var preferencesRepository: PreferencesRepository
    private lateinit var proximityManager: ProximityManager
    private lateinit var viewModel: MapViewModel

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        locationManager = mockk(relaxed = true)
        deviceRepository = mockk(relaxed = true)
        preferencesRepository = mockk(relaxed = true)
        proximityManager = mockk(relaxed = true)
        coEvery { deviceRepository.getGroupMembers() } returns Result.success(emptyList())
        coEvery { preferencesRepository.mapPollingIntervalSeconds } returns flowOf(15)
    }

    @Test
    fun `init loads current location`() = runTest {
        // Given
        val testLocation =
            LocationEntity(
                latitude = 48.1486,
                longitude = 17.1077,
                accuracy = 10.0f,
                timestamp = System.currentTimeMillis(),
                provider = "gps",
            )
        coEvery { locationManager.getCurrentLocation() } returns Result.success(testLocation)

        // When
        viewModel = MapViewModel(locationManager, deviceRepository, preferencesRepository, proximityManager)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertNotNull(state.currentLocation)
            assertEquals(48.1486, state.currentLocation!!.latitude, 0.0001)
            assertEquals(17.1077, state.currentLocation!!.longitude, 0.0001)
            assertFalse(state.isLoading)
            assertNull(state.error)
        }
    }

    @Test
    fun `loadCurrentLocation shows error on failure`() = runTest {
        // Given
        coEvery { locationManager.getCurrentLocation() } returns
            Result.failure(
                Exception("GPS not available"),
            )

        // When
        viewModel = MapViewModel(locationManager, deviceRepository, preferencesRepository, proximityManager)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertNull(state.currentLocation)
            assertFalse(state.isLoading)
            assertEquals("GPS not available", state.error)
        }
    }

    @Test
    fun `loadCurrentLocation shows error when location is null`() = runTest {
        // Given
        coEvery { locationManager.getCurrentLocation() } returns Result.success(null)

        // When
        viewModel = MapViewModel(locationManager, deviceRepository, preferencesRepository, proximityManager)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertNull(state.currentLocation)
            assertFalse(state.isLoading)
            assertEquals("Unable to get current location", state.error)
        }
    }

    @Test
    fun `refresh reloads current location`() = runTest {
        // Given
        val location1 =
            LocationEntity(
                latitude = 48.1486,
                longitude = 17.1077,
                accuracy = 10.0f,
                timestamp = System.currentTimeMillis(),
                provider = "gps",
            )
        val location2 =
            LocationEntity(
                latitude = 48.2,
                longitude = 17.2,
                accuracy = 10.0f,
                timestamp = System.currentTimeMillis() + 5 * 60 * 1000,
                provider = "gps",
            )
        coEvery { locationManager.getCurrentLocation() } returnsMany
            listOf(
                Result.success(location1),
                Result.success(location2),
            )

        viewModel = MapViewModel(locationManager, deviceRepository, preferencesRepository, proximityManager)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.refresh()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertNotNull(state.currentLocation)
            assertEquals(48.2, state.currentLocation!!.latitude, 0.0001)
            assertEquals(17.2, state.currentLocation!!.longitude, 0.0001)
        }
    }

    @Test
    fun `loadGroupMembers populates groupMembers in state`() = runTest {
        // Given
        val members =
            listOf(
                Device(
                    deviceId = "device-001",
                    displayName = "Member 1",
                    lastLocation =
                    DeviceLocation(
                        latitude = 48.15,
                        longitude = 17.15,
                        timestamp = Instant.parse("2025-11-25T12:00:00Z"),
                    ),
                    lastSeenAt = Instant.parse("2025-11-25T12:00:00Z"),
                ),
            )
        coEvery { deviceRepository.getGroupMembers() } returns Result.success(members)
        coEvery { locationManager.getCurrentLocation() } returns Result.success(null)

        // When
        viewModel = MapViewModel(locationManager, deviceRepository, preferencesRepository, proximityManager)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(1, state.groupMembers.size)
            assertEquals("Member 1", state.groupMembers[0].displayName)
        }
    }

    @Test
    fun `loadGroupMembers filters are handled by repository`() = runTest {
        // Given - repository returns filtered list (excludes current device)
        val members =
            listOf(
                Device(
                    deviceId = "device-002",
                    displayName = "Other Device",
                    lastLocation = null, // AC E3.2.5: null location handled in UI
                    lastSeenAt = null,
                ),
            )
        coEvery { deviceRepository.getGroupMembers() } returns Result.success(members)
        coEvery { locationManager.getCurrentLocation() } returns Result.success(null)

        // When
        viewModel = MapViewModel(locationManager, deviceRepository, preferencesRepository, proximityManager)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(1, state.groupMembers.size)
            assertNull(state.groupMembers[0].lastLocation) // Will be filtered in UI
        }
    }
}

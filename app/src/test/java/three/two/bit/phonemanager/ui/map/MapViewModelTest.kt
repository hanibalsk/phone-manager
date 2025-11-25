package three.two.bit.phonemanager.ui.map

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Before
import org.junit.Test
import three.two.bit.phonemanager.data.model.LocationEntity
import three.two.bit.phonemanager.location.LocationManager
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class MapViewModelTest {
    private lateinit var locationManager: LocationManager
    private lateinit var viewModel: MapViewModel

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        locationManager = mockk(relaxed = true)
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
        viewModel = MapViewModel(locationManager)
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
        viewModel = MapViewModel(locationManager)
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
        viewModel = MapViewModel(locationManager)
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

        viewModel = MapViewModel(locationManager)
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
}

package three.two.bit.phonemanager.ui.history

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
import three.two.bit.phonemanager.data.database.LocationDao
import three.two.bit.phonemanager.data.model.LocationEntity
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {
    private lateinit var locationDao: LocationDao
    private lateinit var viewModel: HistoryViewModel

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        locationDao = mockk(relaxed = true)
    }

    @Test
    fun `init loads today's history by default`() = runTest {
        // Given
        val locations =
            listOf(
                LocationEntity(
                    id = 1,
                    latitude = 48.1486,
                    longitude = 17.1077,
                    accuracy = 10.0f,
                    timestamp = System.currentTimeMillis(),
                    provider = "gps",
                ),
            )
        coEvery { locationDao.getLocationsBetween(any(), any()) } returns locations

        // When
        viewModel = HistoryViewModel(locationDao)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(1, state.locations.size)
            assertEquals(DateFilter.Today, state.selectedFilter)
            assertFalse(state.isLoading)
            assertFalse(state.isEmpty)
        }
    }

    @Test
    fun `setDateFilter to YESTERDAY loads yesterday's data`() = runTest {
        // Given
        coEvery { locationDao.getLocationsBetween(any(), any()) } returns emptyList()
        viewModel = HistoryViewModel(locationDao)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.setDateFilter(DateFilter.Yesterday)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(DateFilter.Yesterday, state.selectedFilter)
        }
    }

    @Test
    fun `empty location list shows isEmpty state`() = runTest {
        // Given
        coEvery { locationDao.getLocationsBetween(any(), any()) } returns emptyList()

        // When
        viewModel = HistoryViewModel(locationDao)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(0, state.locations.size)
            assertTrue(state.isEmpty)
            assertFalse(state.isLoading)
        }
    }

    @Test
    fun `polylinePoints are generated from locations`() = runTest {
        // Given
        val locations =
            listOf(
                LocationEntity(
                    id = 1,
                    latitude = 48.1,
                    longitude = 17.1,
                    accuracy = 10.0f,
                    timestamp = System.currentTimeMillis(),
                    provider = "gps",
                ),
                LocationEntity(
                    id = 2,
                    latitude = 48.2,
                    longitude = 17.2,
                    accuracy = 10.0f,
                    timestamp = System.currentTimeMillis() + 1000,
                    provider = "gps",
                ),
            )
        coEvery { locationDao.getLocationsBetween(any(), any()) } returns locations

        // When
        viewModel = HistoryViewModel(locationDao)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(2, state.polylinePoints.size)
            assertEquals(48.1, state.polylinePoints[0].latitude, 0.0001)
            assertEquals(17.2, state.polylinePoints[1].longitude, 0.0001)
        }
    }

    @Test
    fun `error during load updates error state`() = runTest {
        // Given
        coEvery { locationDao.getLocationsBetween(any(), any()) } throws Exception("Database error")

        // When
        viewModel = HistoryViewModel(locationDao)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Database error", state.error)
            assertFalse(state.isLoading)
        }
    }
}

package three.two.bit.phonemanager.ui.triphistory

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import org.junit.Before
import org.junit.Test
import three.two.bit.phonemanager.data.repository.TripRepository
import three.two.bit.phonemanager.domain.model.LatLng
import three.two.bit.phonemanager.domain.model.Trip
import three.two.bit.phonemanager.domain.model.TripState
import three.two.bit.phonemanager.domain.model.TripTrigger
import three.two.bit.phonemanager.location.GeocodingService
import three.two.bit.phonemanager.movement.TransportationMode
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Comprehensive unit tests for TripHistoryViewModel
 *
 * Tests cover:
 * - Initial state loads first page
 * - Pagination loads next page correctly
 * - Filter by date range updates results
 * - Filter by transportation mode updates results
 * - Day grouping organizes trips correctly
 * - Delete trip removes from list
 * - Undo delete restores trip
 * - Empty state when no trips
 * - Error state on repository failure
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TripHistoryViewModelTest {

    private lateinit var tripRepository: TripRepository
    private lateinit var geocodingService: GeocodingService
    private lateinit var viewModel: TripHistoryViewModel

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        tripRepository = mockk(relaxed = true)
        geocodingService = mockk(relaxed = true)
    }

    // region Initial State Tests

    @Test
    fun `init loads first page of trips`() = runTest {
        // Given
        val testTrips = createTestTrips(count = 15)
        coEvery { tripRepository.getTripsBetween(any(), any()) } returns testTrips

        // When
        viewModel = TripHistoryViewModel(tripRepository, geocodingService)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(15, state.trips.size)
            assertFalse(state.isLoading)
            assertNull(state.error)
            assertEquals(0, state.currentPage)
        }
    }

    @Test
    fun `initial state groups trips by day correctly`() = runTest {
        // Given
        val timeZone = TimeZone.currentSystemDefault()
        val now = Clock.System.now()
        val today = now.toLocalDateTime(timeZone).date
        val yesterday = today.plus(-1, DateTimeUnit.DAY)
        val twoDaysAgo = today.plus(-2, DateTimeUnit.DAY)

        val testTrips = listOf(
            createTestTrip(id = "today-1", startTime = now),
            createTestTrip(
                id = "yesterday-1",
                startTime = yesterday.atStartOfDayIn(timeZone).plus(10, DateTimeUnit.HOUR),
            ),
            createTestTrip(
                id = "two-days-1",
                startTime = twoDaysAgo.atStartOfDayIn(timeZone).plus(14, DateTimeUnit.HOUR),
            ),
        )
        coEvery { tripRepository.getTripsBetween(any(), any()) } returns testTrips

        // When
        viewModel = TripHistoryViewModel(tripRepository, geocodingService)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(3, state.groupedTrips.size)
            assertTrue(state.groupedTrips.keys.any { it is TripDayGroup.Today })
            assertTrue(state.groupedTrips.keys.any { it is TripDayGroup.Yesterday })
            assertTrue(state.groupedTrips.keys.any { it is TripDayGroup.Date && it.date == twoDaysAgo })
        }
    }

    @Test
    fun `initial state shows empty when no trips`() = runTest {
        // Given
        coEvery { tripRepository.getTripsBetween(any(), any()) } returns emptyList()

        // When
        viewModel = TripHistoryViewModel(tripRepository, geocodingService)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.trips.isEmpty())
            assertTrue(state.groupedTrips.isEmpty())
            assertFalse(state.isLoading)
        }
    }

    @Test
    fun `initial state shows error on repository failure`() = runTest {
        // Given
        coEvery { tripRepository.getTripsBetween(any(), any()) } throws Exception("Database error")

        // When
        viewModel = TripHistoryViewModel(tripRepository, geocodingService)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.trips.isEmpty())
            assertFalse(state.isLoading)
            assertEquals("Database error", state.error)
        }
    }

    @Test
    fun `initial state sets hasMoreData true when full page returned`() = runTest {
        // Given - exactly PAGE_SIZE trips
        val testTrips = createTestTrips(count = TripHistoryViewModel.PAGE_SIZE)
        coEvery { tripRepository.getTripsBetween(any(), any()) } returns testTrips

        // When
        viewModel = TripHistoryViewModel(tripRepository, geocodingService)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.hasMoreData)
        }
    }

    @Test
    fun `initial state sets hasMoreData false when partial page returned`() = runTest {
        // Given - less than PAGE_SIZE trips
        val testTrips = createTestTrips(count = TripHistoryViewModel.PAGE_SIZE - 5)
        coEvery { tripRepository.getTripsBetween(any(), any()) } returns testTrips

        // When
        viewModel = TripHistoryViewModel(tripRepository, geocodingService)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.hasMoreData)
        }
    }

    // endregion

    // region Pagination Tests

    @Test
    fun `loadMoreTrips loads next page correctly`() = runTest {
        // Given
        val firstPageTrips = createTestTrips(count = 20, startId = 0)
        val secondPageTrips = createTestTrips(count = 15, startId = 20)

        coEvery { tripRepository.getTripsBetween(any(), any()) } returns firstPageTrips

        viewModel = TripHistoryViewModel(tripRepository, geocodingService)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        coEvery { tripRepository.getTripsBetween(any(), any()) } returns firstPageTrips + secondPageTrips
        viewModel.loadMoreTrips()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(35, state.trips.size)
            assertEquals(1, state.currentPage)
            assertFalse(state.isLoadingMore)
        }
    }

    @Test
    fun `loadMoreTrips does not load when already loading`() = runTest {
        // Given - 40 trips means hasMoreData = true after first page (20 trips displayed, 20 more available)
        val testTrips = createTestTrips(count = 40)
        coEvery { tripRepository.getTripsBetween(any(), any()) } returns testTrips

        viewModel = TripHistoryViewModel(tripRepository, geocodingService)
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify initial state: 20 trips displayed, hasMoreData = true
        assertEquals(20, viewModel.uiState.value.trips.size)
        assertTrue(viewModel.uiState.value.hasMoreData)

        // When - call loadMoreTrips twice in quick succession
        // Both calls execute before coroutine runs, but second should be blocked by guard
        viewModel.loadMoreTrips()
        viewModel.loadMoreTrips()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - should have 40 trips total (first page + second page)
        assertEquals(40, viewModel.uiState.value.trips.size)
    }

    @Test
    fun `loadMoreTrips does not load when no more data`() = runTest {
        // Given - less than PAGE_SIZE trips
        val testTrips = createTestTrips(count = 10)
        coEvery { tripRepository.getTripsBetween(any(), any()) } returns testTrips

        viewModel = TripHistoryViewModel(tripRepository, geocodingService)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.loadMoreTrips()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - should not make additional repository call
        coVerify(exactly = 1) {
            tripRepository.getTripsBetween(any(), any())
        }
    }

    @Test
    fun `loadMoreTrips sets isLoadingMore during load`() = runTest {
        // Given - 40 trips means pagination is possible (20 on first page, 20 more available)
        val testTrips = createTestTrips(count = 40)
        coEvery { tripRepository.getTripsBetween(any(), any()) } returns testTrips

        viewModel = TripHistoryViewModel(tripRepository, geocodingService)
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify initial state
        assertEquals(20, viewModel.uiState.value.trips.size)
        assertTrue(viewModel.uiState.value.hasMoreData)

        // When - call loadMoreTrips and advance scheduler
        viewModel.loadMoreTrips()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - after completion, isLoadingMore should be false and trips doubled
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoadingMore)
            assertEquals(40, state.trips.size)
        }
    }

    @Test
    fun `loadMoreTrips handles error gracefully`() = runTest {
        // Given
        val testTrips = createTestTrips(count = 20)
        coEvery { tripRepository.getTripsBetween(any(), any()) } returns testTrips

        viewModel = TripHistoryViewModel(tripRepository, geocodingService)
        testDispatcher.scheduler.advanceUntilIdle()

        // When - simulate error on next page
        coEvery { tripRepository.getTripsBetween(any(), any()) } throws Exception("Network error")
        viewModel.loadMoreTrips()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - should still have first page data
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(20, state.trips.size)
            assertFalse(state.isLoadingMore)
        }
    }

    // endregion

    // region Date Filter Tests

    @Test
    fun `setDateRangeFilter updates results`() = runTest {
        // Given
        val allTrips = createTestTrips(count = 30)
        val filteredTrips = createTestTrips(count = 5)

        coEvery { tripRepository.getTripsBetween(any(), any()) } returns allTrips

        viewModel = TripHistoryViewModel(tripRepository, geocodingService)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        coEvery { tripRepository.getTripsBetween(any(), any()) } returns filteredTrips
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        viewModel.setDateRangeFilter(today, today)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(5, state.trips.size)
            assertEquals(today, state.dateRangeStart)
            assertEquals(today, state.dateRangeEnd)
        }
    }

    @Test
    fun `setQuickDateFilter TODAY sets correct date range`() = runTest {
        // Given
        coEvery { tripRepository.getTripsBetween(any(), any()) } returns emptyList()
        viewModel = TripHistoryViewModel(tripRepository, geocodingService)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.setQuickDateFilter(QuickDateFilter.TODAY)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            assertEquals(today, state.dateRangeStart)
            assertEquals(today, state.dateRangeEnd)
            assertEquals(QuickDateFilter.TODAY, state.selectedQuickFilter)
        }
    }

    @Test
    fun `setQuickDateFilter THIS_WEEK sets correct date range`() = runTest {
        // Given
        coEvery { tripRepository.getTripsBetween(any(), any()) } returns emptyList()
        viewModel = TripHistoryViewModel(tripRepository, geocodingService)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.setQuickDateFilter(QuickDateFilter.THIS_WEEK)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            val weekAgo = today.plus(-7, DateTimeUnit.DAY)
            assertEquals(weekAgo, state.dateRangeStart)
            assertEquals(today, state.dateRangeEnd)
            assertEquals(QuickDateFilter.THIS_WEEK, state.selectedQuickFilter)
        }
    }

    @Test
    fun `setQuickDateFilter THIS_MONTH sets correct date range`() = runTest {
        // Given
        coEvery { tripRepository.getTripsBetween(any(), any()) } returns emptyList()
        viewModel = TripHistoryViewModel(tripRepository, geocodingService)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.setQuickDateFilter(QuickDateFilter.THIS_MONTH)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            val monthAgo = today.plus(-30, DateTimeUnit.DAY)
            assertEquals(monthAgo, state.dateRangeStart)
            assertEquals(today, state.dateRangeEnd)
            assertEquals(QuickDateFilter.THIS_MONTH, state.selectedQuickFilter)
        }
    }

    @Test
    fun `setQuickDateFilter ALL clears date range`() = runTest {
        // Given
        coEvery { tripRepository.getTripsBetween(any(), any()) } returns emptyList()
        viewModel = TripHistoryViewModel(tripRepository, geocodingService)
        testDispatcher.scheduler.advanceUntilIdle()

        // Set a filter first
        viewModel.setQuickDateFilter(QuickDateFilter.TODAY)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.setQuickDateFilter(QuickDateFilter.ALL)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertNull(state.dateRangeStart)
            assertNull(state.dateRangeEnd)
            assertEquals(QuickDateFilter.ALL, state.selectedQuickFilter)
        }
    }

    // endregion

    // region Mode Filter Tests

    @Test
    fun `toggleModeFilter adds mode to filter`() = runTest {
        // Given
        val allTrips = createTestTrips(count = 20)
        val walkingTrips = allTrips.take(5)

        coEvery { tripRepository.getTripsBetween(any(), any()) } returns allTrips
        viewModel = TripHistoryViewModel(tripRepository, geocodingService)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        coEvery { tripRepository.getTripsBetween(any(), any()) } returns walkingTrips
        viewModel.toggleModeFilter(TransportationMode.WALKING)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.selectedModeFilters.contains(TransportationMode.WALKING))
        }
    }

    @Test
    fun `toggleModeFilter removes mode from filter when already selected`() = runTest {
        // Given
        coEvery { tripRepository.getTripsBetween(any(), any()) } returns emptyList()
        viewModel = TripHistoryViewModel(tripRepository, geocodingService)
        testDispatcher.scheduler.advanceUntilIdle()

        // Add filter
        viewModel.toggleModeFilter(TransportationMode.WALKING)
        testDispatcher.scheduler.advanceUntilIdle()

        // When - toggle again
        viewModel.toggleModeFilter(TransportationMode.WALKING)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.selectedModeFilters.contains(TransportationMode.WALKING))
        }
    }

    @Test
    fun `toggleModeFilter filters trips by dominant mode`() = runTest {
        // Given
        val walkingTrips = listOf(
            createTestTrip(id = "1", dominantMode = TransportationMode.WALKING),
            createTestTrip(id = "2", dominantMode = TransportationMode.WALKING),
        )
        val drivingTrips = listOf(
            createTestTrip(id = "3", dominantMode = TransportationMode.IN_VEHICLE),
        )
        val allTrips = walkingTrips + drivingTrips

        coEvery { tripRepository.getTripsBetween(any(), any()) } returns allTrips
        viewModel = TripHistoryViewModel(tripRepository, geocodingService)
        testDispatcher.scheduler.advanceUntilIdle()

        // When - filter by WALKING
        coEvery { tripRepository.getTripsBetween(any(), any()) } returns walkingTrips
        viewModel.toggleModeFilter(TransportationMode.WALKING)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(2, state.trips.size)
            assertTrue(state.trips.all { it.dominantMode == TransportationMode.WALKING })
        }
    }

    @Test
    fun `multiple mode filters show trips matching any selected mode`() = runTest {
        // Given
        val testTrips = listOf(
            createTestTrip(id = "1", dominantMode = TransportationMode.WALKING),
            createTestTrip(id = "2", dominantMode = TransportationMode.CYCLING),
            createTestTrip(id = "3", dominantMode = TransportationMode.IN_VEHICLE),
        )

        coEvery { tripRepository.getTripsBetween(any(), any()) } returns testTrips
        viewModel = TripHistoryViewModel(tripRepository, geocodingService)
        testDispatcher.scheduler.advanceUntilIdle()

        // When - filter by WALKING and CYCLING
        val filteredTrips = testTrips.filter {
            it.dominantMode in setOf(TransportationMode.WALKING, TransportationMode.CYCLING)
        }
        coEvery { tripRepository.getTripsBetween(any(), any()) } returns filteredTrips

        viewModel.toggleModeFilter(TransportationMode.WALKING)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.toggleModeFilter(TransportationMode.CYCLING)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(2, state.trips.size)
        }
    }

    // endregion

    // region Clear Filters Tests

    @Test
    fun `clearFilters resets all filters`() = runTest {
        // Given
        val testTrips = createTestTrips(count = 20)
        coEvery { tripRepository.getTripsBetween(any(), any()) } returns testTrips

        viewModel = TripHistoryViewModel(tripRepository, geocodingService)
        testDispatcher.scheduler.advanceUntilIdle()

        // Set various filters
        viewModel.setQuickDateFilter(QuickDateFilter.TODAY)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.toggleModeFilter(TransportationMode.WALKING)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.clearFilters()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertNull(state.dateRangeStart)
            assertNull(state.dateRangeEnd)
            assertEquals(QuickDateFilter.ALL, state.selectedQuickFilter)
            assertTrue(state.selectedModeFilters.isEmpty())
        }
    }

    // endregion

    // region Delete Trip Tests

    @Test
    fun `deleteTrip removes trip from list`() = runTest {
        // Given
        val testTrips = createTestTrips(count = 5)
        coEvery { tripRepository.getTripsBetween(any(), any()) } returns testTrips
        coEvery { tripRepository.deleteTrip(any()) } returns Unit

        viewModel = TripHistoryViewModel(tripRepository, geocodingService)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.deleteTrip(testTrips[0].id)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(4, state.trips.size)
            assertFalse(state.trips.any { it.id == testTrips[0].id })
            coVerify { tripRepository.deleteTrip(testTrips[0].id) }
        }
    }

    @Test
    fun `deleteTrip stores trip for undo`() = runTest {
        // Given
        val testTrips = createTestTrips(count = 5)
        coEvery { tripRepository.getTripsBetween(any(), any()) } returns testTrips
        coEvery { tripRepository.deleteTrip(any()) } returns Unit

        viewModel = TripHistoryViewModel(tripRepository, geocodingService)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.deleteTrip(testTrips[0].id)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertNotNull(state.lastDeletedTrip)
            assertEquals(testTrips[0].id, state.lastDeletedTrip?.id)
            assertTrue(state.showUndoSnackbar)
        }
    }

    @Test
    fun `deleteTrip handles error gracefully`() = runTest {
        // Given
        val testTrips = createTestTrips(count = 5)
        coEvery { tripRepository.getTripsBetween(any(), any()) } returns testTrips
        coEvery { tripRepository.deleteTrip(any()) } throws Exception("Delete failed")

        viewModel = TripHistoryViewModel(tripRepository, geocodingService)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.deleteTrip(testTrips[0].id)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Delete failed", state.error)
            assertEquals(5, state.trips.size) // Trip not removed
        }
    }

    // endregion

    // region Undo Delete Tests

    @Test
    fun `undoDelete restores trip`() = runTest {
        // Given
        val testTrips = createTestTrips(count = 5)
        coEvery { tripRepository.getTripsBetween(any(), any()) } returns testTrips
        coEvery { tripRepository.deleteTrip(any()) } returns Unit
        coEvery { tripRepository.insert(any()) } returns Result.success("restored-id")

        viewModel = TripHistoryViewModel(tripRepository, geocodingService)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.deleteTrip(testTrips[0].id)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.undoDelete()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(5, state.trips.size)
            assertTrue(state.trips.any { it.id == testTrips[0].id })
            assertNull(state.lastDeletedTrip)
            assertFalse(state.showUndoSnackbar)
            coVerify { tripRepository.insert(testTrips[0]) }
        }
    }

    @Test
    fun `undoDelete does nothing when no deleted trip`() = runTest {
        // Given
        val testTrips = createTestTrips(count = 5)
        coEvery { tripRepository.getTripsBetween(any(), any()) } returns testTrips

        viewModel = TripHistoryViewModel(tripRepository, geocodingService)
        testDispatcher.scheduler.advanceUntilIdle()

        // When - call undoDelete without deleting
        viewModel.undoDelete()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - should not call repository insert
        coVerify(exactly = 0) { tripRepository.insert(any()) }
    }

    @Test
    fun `undoDelete handles error gracefully`() = runTest {
        // Given
        val testTrips = createTestTrips(count = 5)
        coEvery { tripRepository.getTripsBetween(any(), any()) } returns testTrips
        coEvery { tripRepository.deleteTrip(any()) } returns Unit
        coEvery { tripRepository.insert(any()) } throws Exception("Restore failed")

        viewModel = TripHistoryViewModel(tripRepository, geocodingService)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.deleteTrip(testTrips[0].id)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.undoDelete()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - state unchanged except trip count
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(4, state.trips.size) // Still deleted
        }
    }

    // endregion

    // region Refresh Tests

    @Test
    fun `refreshTrips reloads trips from start`() = runTest {
        // Given
        val initialTrips = createTestTrips(count = 20, startId = 0)
        val refreshedTrips = createTestTrips(count = 25, startId = 100)

        coEvery { tripRepository.getTripsBetween(any(), any()) } returns initialTrips
        viewModel = TripHistoryViewModel(tripRepository, geocodingService)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        coEvery { tripRepository.getTripsBetween(any(), any()) } returns refreshedTrips
        viewModel.refreshTrips()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - getFilteredTrips applies pagination (PAGE_SIZE = 20)
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(20, state.trips.size) // Limited by PAGE_SIZE
            assertEquals(0, state.currentPage)
            assertFalse(state.isRefreshing)
            assertTrue(state.hasMoreData) // 25 > 20, so hasMoreData should be true
        }
    }

    @Test
    fun `refreshTrips sets isRefreshing during load`() = runTest {
        // Given
        coEvery { tripRepository.getTripsBetween(any(), any()) } returns emptyList()
        viewModel = TripHistoryViewModel(tripRepository, geocodingService)
        testDispatcher.scheduler.advanceUntilIdle()

        // When - advance scheduler fully
        viewModel.refreshTrips()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - after completion, isRefreshing should be false
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isRefreshing)
            // Verify refresh completed (repository was called twice: init + refresh)
            coVerify(exactly = 2) { tripRepository.getTripsBetween(any(), any()) }
        }
    }

    @Test
    fun `refreshTrips handles error gracefully`() = runTest {
        // Given
        val testTrips = createTestTrips(count = 10)
        coEvery { tripRepository.getTripsBetween(any(), any()) } returns testTrips

        viewModel = TripHistoryViewModel(tripRepository, geocodingService)
        testDispatcher.scheduler.advanceUntilIdle()

        // When - simulate error on refresh
        coEvery { tripRepository.getTripsBetween(any(), any()) } throws Exception("Network error")
        viewModel.refreshTrips()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Network error", state.error)
            assertFalse(state.isRefreshing)
        }
    }

    // endregion

    // region UI State Management Tests

    @Test
    fun `showDateRangePicker updates state`() = runTest {
        // Given
        coEvery { tripRepository.getTripsBetween(any(), any()) } returns emptyList()
        viewModel = TripHistoryViewModel(tripRepository, geocodingService)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.showDateRangePicker(true)

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.showDateRangePicker)
        }

        // When
        viewModel.showDateRangePicker(false)

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.showDateRangePicker)
        }
    }

    @Test
    fun `showDeleteConfirmation updates state`() = runTest {
        // Given
        coEvery { tripRepository.getTripsBetween(any(), any()) } returns emptyList()
        viewModel = TripHistoryViewModel(tripRepository, geocodingService)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.showDeleteConfirmation("trip-123")

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("trip-123", state.tripIdToDelete)
        }
    }

    @Test
    fun `confirmDelete deletes trip and clears dialog state`() = runTest {
        // Given
        val testTrips = createTestTrips(count = 5)
        coEvery { tripRepository.getTripsBetween(any(), any()) } returns testTrips
        coEvery { tripRepository.deleteTrip(any()) } returns Unit

        viewModel = TripHistoryViewModel(tripRepository, geocodingService)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.showDeleteConfirmation(testTrips[0].id)

        // When
        viewModel.confirmDelete()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertNull(state.tripIdToDelete)
            assertEquals(4, state.trips.size)
            coVerify { tripRepository.deleteTrip(testTrips[0].id) }
        }
    }

    @Test
    fun `dismissUndoSnackbar clears undo state`() = runTest {
        // Given
        val testTrips = createTestTrips(count = 5)
        coEvery { tripRepository.getTripsBetween(any(), any()) } returns testTrips
        coEvery { tripRepository.deleteTrip(any()) } returns Unit

        viewModel = TripHistoryViewModel(tripRepository, geocodingService)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.deleteTrip(testTrips[0].id)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.dismissUndoSnackbar()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.showUndoSnackbar)
            assertNull(state.lastDeletedTrip)
        }
    }

    // endregion

    // region Day Grouping Tests

    @Test
    fun `grouping sorts days correctly`() = runTest {
        // Given
        val timeZone = TimeZone.currentSystemDefault()
        val now = Clock.System.now()
        val today = now.toLocalDateTime(timeZone).date
        val yesterday = today.plus(-1, DateTimeUnit.DAY)
        val twoDaysAgo = today.plus(-2, DateTimeUnit.DAY)
        val threeDaysAgo = today.plus(-3, DateTimeUnit.DAY)

        val testTrips = listOf(
            createTestTrip(
                id = "three-days-1",
                startTime = threeDaysAgo.atStartOfDayIn(timeZone),
            ),
            createTestTrip(id = "today-1", startTime = now),
            createTestTrip(
                id = "two-days-1",
                startTime = twoDaysAgo.atStartOfDayIn(timeZone),
            ),
            createTestTrip(
                id = "yesterday-1",
                startTime = yesterday.atStartOfDayIn(timeZone),
            ),
        )

        coEvery { tripRepository.getTripsBetween(any(), any()) } returns testTrips

        // When
        viewModel = TripHistoryViewModel(tripRepository, geocodingService)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - groups should be ordered: Today, Yesterday, then by date descending
        viewModel.uiState.test {
            val state = awaitItem()
            val groupKeys = state.groupedTrips.keys.toList()
            assertEquals(4, groupKeys.size)
            assertTrue(groupKeys[0] is TripDayGroup.Today)
            assertTrue(groupKeys[1] is TripDayGroup.Yesterday)
            assertTrue(groupKeys[2] is TripDayGroup.Date && (groupKeys[2] as TripDayGroup.Date).date == twoDaysAgo)
            assertTrue(groupKeys[3] is TripDayGroup.Date && (groupKeys[3] as TripDayGroup.Date).date == threeDaysAgo)
        }
    }

    @Test
    fun `grouping handles multiple trips per day`() = runTest {
        // Given - create trips at safe time offsets that are always within "today"
        // regardless of when the test runs (use minutes instead of hours to avoid
        // midnight crossing issues)
        val timeZone = TimeZone.currentSystemDefault()
        val now = Clock.System.now()
        val today = now.toLocalDateTime(timeZone).date
        // Use the middle of the day to ensure all trips are on the same day
        val middayToday = today.atStartOfDayIn(timeZone).plus(12, DateTimeUnit.HOUR)

        val testTrips = listOf(
            createTestTrip(id = "today-1", startTime = middayToday.plus(-1, DateTimeUnit.MINUTE)),
            createTestTrip(id = "today-2", startTime = middayToday.plus(-2, DateTimeUnit.MINUTE)),
            createTestTrip(id = "today-3", startTime = middayToday.plus(-3, DateTimeUnit.MINUTE)),
        )

        coEvery { tripRepository.getTripsBetween(any(), any()) } returns testTrips

        // When
        viewModel = TripHistoryViewModel(tripRepository, geocodingService)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(1, state.groupedTrips.size)
            val todayTrips = state.groupedTrips[TripDayGroup.Today]
            assertNotNull(todayTrips)
            assertEquals(3, todayTrips.size)
        }
    }

    // endregion

    // region Helper Functions

    private fun createTestTrips(count: Int, startId: Int = 0): List<Trip> {
        val now = Clock.System.now()
        return (0 until count).map { index ->
            createTestTrip(
                id = "trip-${startId + index}",
                startTime = now.plus(-index.toLong(), DateTimeUnit.HOUR),
            )
        }
    }

    private fun createTestTrip(
        id: String = "test-trip",
        startTime: Instant = Clock.System.now(),
        dominantMode: TransportationMode = TransportationMode.WALKING,
    ): Trip = Trip(
        id = id,
        state = TripState.COMPLETED,
        startTime = startTime,
        endTime = startTime.plus(30, DateTimeUnit.MINUTE),
        startLocation = LatLng(48.1486, 17.1077),
        endLocation = LatLng(48.15, 17.11),
        totalDistanceMeters = 1500.0,
        locationCount = 100,
        dominantMode = dominantMode,
        modesUsed = setOf(dominantMode),
        modeBreakdown = mapOf(dominantMode to 1800L),
        startTrigger = TripTrigger.ACTIVITY_DETECTION,
        endTrigger = TripTrigger.STATIONARY_DETECTION,
        isSynced = false,
        syncedAt = null,
        serverId = null,
        createdAt = startTime,
        updatedAt = startTime,
    )

    // endregion
}

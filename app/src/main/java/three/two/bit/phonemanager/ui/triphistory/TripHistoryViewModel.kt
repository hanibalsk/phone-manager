package three.two.bit.phonemanager.ui.triphistory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import three.two.bit.phonemanager.data.repository.TripRepository
import three.two.bit.phonemanager.domain.model.Trip
import three.two.bit.phonemanager.movement.TransportationMode
import timber.log.Timber
import javax.inject.Inject

/**
 * Story E8.9: TripHistoryViewModel
 *
 * Manages trip history display with day grouping, filtering, and pagination.
 * ACs: E8.9.1, E8.9.2, E8.9.4, E8.9.5, E8.9.6, E8.9.7, E8.9.8
 */
@HiltViewModel
class TripHistoryViewModel @Inject constructor(private val tripRepository: TripRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(TripHistoryUiState())
    val uiState: StateFlow<TripHistoryUiState> = _uiState.asStateFlow()

    companion object {
        const val PAGE_SIZE = 20
    }

    init {
        loadTrips()
    }

    /**
     * Load initial trips (AC E8.9.6)
     */
    fun loadTrips() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val trips = getFilteredTrips(0, PAGE_SIZE)
                val groupedTrips = groupTripsByDay(trips)

                _uiState.update {
                    it.copy(
                        trips = trips,
                        groupedTrips = groupedTrips,
                        isLoading = false,
                        hasMoreData = trips.size >= PAGE_SIZE,
                        currentPage = 0,
                    )
                }

                Timber.d("Loaded ${trips.size} trips")
            } catch (e: Exception) {
                Timber.e(e, "Failed to load trips")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load trips",
                    )
                }
            }
        }
    }

    /**
     * Load more trips for pagination (AC E8.9.6)
     */
    fun loadMoreTrips() {
        if (_uiState.value.isLoadingMore || !_uiState.value.hasMoreData) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }

            try {
                val nextPage = _uiState.value.currentPage + 1
                val offset = nextPage * PAGE_SIZE
                val newTrips = getFilteredTrips(offset, PAGE_SIZE)

                val allTrips = _uiState.value.trips + newTrips
                val groupedTrips = groupTripsByDay(allTrips)

                _uiState.update {
                    it.copy(
                        trips = allTrips,
                        groupedTrips = groupedTrips,
                        isLoadingMore = false,
                        hasMoreData = newTrips.size >= PAGE_SIZE,
                        currentPage = nextPage,
                    )
                }

                Timber.d("Loaded ${newTrips.size} more trips, total: ${allTrips.size}")
            } catch (e: Exception) {
                Timber.e(e, "Failed to load more trips")
                _uiState.update { it.copy(isLoadingMore = false) }
            }
        }
    }

    /**
     * Refresh trips - pull to refresh (AC E8.9.7)
     */
    fun refreshTrips() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, error = null) }

            try {
                val trips = getFilteredTrips(0, PAGE_SIZE)
                val groupedTrips = groupTripsByDay(trips)

                _uiState.update {
                    it.copy(
                        trips = trips,
                        groupedTrips = groupedTrips,
                        isRefreshing = false,
                        hasMoreData = trips.size >= PAGE_SIZE,
                        currentPage = 0,
                    )
                }

                Timber.d("Refreshed trips, loaded ${trips.size}")
            } catch (e: Exception) {
                Timber.e(e, "Failed to refresh trips")
                _uiState.update {
                    it.copy(
                        isRefreshing = false,
                        error = e.message ?: "Failed to refresh trips",
                    )
                }
            }
        }
    }

    /**
     * Delete a trip (AC E8.9.8)
     */
    fun deleteTrip(tripId: String) {
        viewModelScope.launch {
            try {
                // Store trip for undo
                val tripToDelete = _uiState.value.trips.find { it.id == tripId }
                _uiState.update { it.copy(lastDeletedTrip = tripToDelete) }

                tripRepository.deleteTrip(tripId)

                // Remove from local state
                val updatedTrips = _uiState.value.trips.filter { it.id != tripId }
                val groupedTrips = groupTripsByDay(updatedTrips)

                _uiState.update {
                    it.copy(
                        trips = updatedTrips,
                        groupedTrips = groupedTrips,
                        showUndoSnackbar = true,
                    )
                }

                Timber.d("Deleted trip: $tripId")
            } catch (e: Exception) {
                Timber.e(e, "Failed to delete trip: $tripId")
                _uiState.update {
                    it.copy(error = e.message ?: "Failed to delete trip")
                }
            }
        }
    }

    /**
     * Undo trip deletion (AC E8.9.8)
     */
    fun undoDelete() {
        val deletedTrip = _uiState.value.lastDeletedTrip ?: return

        viewModelScope.launch {
            try {
                tripRepository.insert(deletedTrip)

                val updatedTrips = (_uiState.value.trips + deletedTrip).sortedByDescending { it.startTime }
                val groupedTrips = groupTripsByDay(updatedTrips)

                _uiState.update {
                    it.copy(
                        trips = updatedTrips,
                        groupedTrips = groupedTrips,
                        lastDeletedTrip = null,
                        showUndoSnackbar = false,
                    )
                }

                Timber.d("Restored trip: ${deletedTrip.id}")
            } catch (e: Exception) {
                Timber.e(e, "Failed to restore trip")
            }
        }
    }

    /**
     * Dismiss undo snackbar
     */
    fun dismissUndoSnackbar() {
        _uiState.update { it.copy(showUndoSnackbar = false, lastDeletedTrip = null) }
    }

    /**
     * Set date range filter (AC E8.9.4)
     */
    fun setDateRangeFilter(startDate: LocalDate?, endDate: LocalDate?) {
        _uiState.update {
            it.copy(
                dateRangeStart = startDate,
                dateRangeEnd = endDate,
            )
        }
        loadTrips()
    }

    /**
     * Set quick date filter (AC E8.9.4)
     */
    fun setQuickDateFilter(filter: QuickDateFilter) {
        val timeZone = TimeZone.currentSystemDefault()
        val now = Clock.System.now()
        val today = now.toLocalDateTime(timeZone).date

        val (startDate, endDate) = when (filter) {
            QuickDateFilter.TODAY -> today to today
            QuickDateFilter.THIS_WEEK -> today.plus(-7, DateTimeUnit.DAY) to today
            QuickDateFilter.THIS_MONTH -> today.plus(-30, DateTimeUnit.DAY) to today
            QuickDateFilter.ALL -> null to null
        }

        _uiState.update {
            it.copy(
                selectedQuickFilter = filter,
                dateRangeStart = startDate,
                dateRangeEnd = endDate,
            )
        }
        loadTrips()
    }

    /**
     * Toggle mode filter (AC E8.9.5)
     */
    fun toggleModeFilter(mode: TransportationMode) {
        val currentFilters = _uiState.value.selectedModeFilters.toMutableSet()
        if (mode in currentFilters) {
            currentFilters.remove(mode)
        } else {
            currentFilters.add(mode)
        }

        _uiState.update { it.copy(selectedModeFilters = currentFilters) }
        loadTrips()
    }

    /**
     * Clear all filters
     */
    fun clearFilters() {
        _uiState.update {
            it.copy(
                dateRangeStart = null,
                dateRangeEnd = null,
                selectedQuickFilter = QuickDateFilter.ALL,
                selectedModeFilters = emptySet(),
            )
        }
        loadTrips()
    }

    /**
     * Show/hide date range picker
     */
    fun showDateRangePicker(show: Boolean) {
        _uiState.update { it.copy(showDateRangePicker = show) }
    }

    /**
     * Show/hide delete confirmation dialog
     */
    fun showDeleteConfirmation(tripId: String?) {
        _uiState.update { it.copy(tripIdToDelete = tripId) }
    }

    /**
     * Confirm trip deletion
     */
    fun confirmDelete() {
        val tripId = _uiState.value.tripIdToDelete ?: return
        _uiState.update { it.copy(tripIdToDelete = null) }
        deleteTrip(tripId)
    }

    /**
     * Get filtered trips from repository
     */
    private suspend fun getFilteredTrips(offset: Int, limit: Int): List<Trip> {
        val state = _uiState.value
        val timeZone = TimeZone.currentSystemDefault()

        // Calculate time range
        val startTime = state.dateRangeStart?.atStartOfDayIn(timeZone)
            ?: Instant.fromEpochMilliseconds(0)
        val endTime = state.dateRangeEnd?.plus(1, DateTimeUnit.DAY)?.atStartOfDayIn(timeZone)
            ?: Clock.System.now()

        // Get trips in date range
        var trips = tripRepository.getTripsBetween(startTime, endTime)

        // Apply mode filter
        if (state.selectedModeFilters.isNotEmpty()) {
            trips = trips.filter { it.dominantMode in state.selectedModeFilters }
        }

        // Sort by start time (newest first) and apply pagination
        return trips
            .sortedByDescending { it.startTime }
            .drop(offset)
            .take(limit)
    }

    /**
     * Group trips by day for display (AC E8.9.2)
     */
    private fun groupTripsByDay(trips: List<Trip>): Map<TripDayGroup, List<Trip>> {
        val timeZone = TimeZone.currentSystemDefault()
        val now = Clock.System.now()
        val today = now.toLocalDateTime(timeZone).date
        val yesterday = today.plus(-1, DateTimeUnit.DAY)

        return trips.groupBy { trip ->
            val tripDate = trip.startTime.toLocalDateTime(timeZone).date
            when (tripDate) {
                today -> TripDayGroup.Today
                yesterday -> TripDayGroup.Yesterday
                else -> TripDayGroup.Date(tripDate)
            }
        }.toSortedMap(
            compareByDescending {
                when (it) {
                    is TripDayGroup.Today -> Long.MAX_VALUE
                    is TripDayGroup.Yesterday -> Long.MAX_VALUE - 1
                    is TripDayGroup.Date -> it.date.toEpochDays().toLong()
                }
            },
        )
    }
}

/**
 * UI State for Trip History Screen
 */
data class TripHistoryUiState(
    val trips: List<Trip> = emptyList(),
    val groupedTrips: Map<TripDayGroup, List<Trip>> = emptyMap(),
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val hasMoreData: Boolean = true,
    val currentPage: Int = 0,
    // Filters
    val dateRangeStart: LocalDate? = null,
    val dateRangeEnd: LocalDate? = null,
    val selectedQuickFilter: QuickDateFilter = QuickDateFilter.ALL,
    val selectedModeFilters: Set<TransportationMode> = emptySet(),
    val showDateRangePicker: Boolean = false,
    // Delete
    val tripIdToDelete: String? = null,
    val lastDeletedTrip: Trip? = null,
    val showUndoSnackbar: Boolean = false,
)

/**
 * Day grouping for trip display (AC E8.9.2)
 */
sealed class TripDayGroup : Comparable<TripDayGroup> {
    data object Today : TripDayGroup()
    data object Yesterday : TripDayGroup()
    data class Date(val date: LocalDate) : TripDayGroup()

    override fun compareTo(other: TripDayGroup): Int = when {
        this is Today && other !is Today -> -1
        this !is Today && other is Today -> 1
        this is Yesterday && other is Date -> -1
        this is Date && other is Yesterday -> 1
        this is Date && other is Date -> other.date.compareTo(this.date)
        else -> 0
    }
}

/**
 * Quick date filter options (AC E8.9.4)
 */
enum class QuickDateFilter {
    TODAY,
    THIS_WEEK,
    THIS_MONTH,
    ALL,
}

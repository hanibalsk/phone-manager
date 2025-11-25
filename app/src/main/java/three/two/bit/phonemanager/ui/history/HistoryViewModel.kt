package three.two.bit.phonemanager.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import three.two.bit.phonemanager.data.database.LocationDao
import three.two.bit.phonemanager.data.model.LocationEntity
import three.two.bit.phonemanager.util.PolylineUtils
import timber.log.Timber
import javax.inject.Inject

/**
 * Story E4.1/E4.2: HistoryViewModel
 *
 * Story E4.1: Manages location history display with date filtering
 * Story E4.2: Adds downsampling for performance
 * ACs: E4.1.1, E4.1.3, E4.1.4, E4.1.5, E4.1.6, E4.2.2
 */
@HiltViewModel
class HistoryViewModel
@Inject
constructor(private val locationDao: LocationDao) : ViewModel() {
    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        // AC E4.1.4: Default to "Today" preset
        setDateFilter(DateFilter.Today)
    }

    /**
     * Set date filter and load history (AC E4.1.4, E4.1.5)
     */
    fun setDateFilter(filter: DateFilter) {
        _uiState.update { it.copy(selectedFilter = filter, isLoading = true, error = null) }

        val timeZone = TimeZone.currentSystemDefault()
        val now = Clock.System.now()
        val today = now.toLocalDateTime(timeZone).date

        val (startTime, endTime) =
            when (filter) {
                is DateFilter.Today -> {
                    val start = today.atStartOfDayIn(timeZone)
                    val end = now
                    start.toEpochMilliseconds() to end.toEpochMilliseconds()
                }
                is DateFilter.Yesterday -> {
                    val yesterday = today.plus(-1, DateTimeUnit.DAY)
                    val start = yesterday.atStartOfDayIn(timeZone)
                    val end = today.atStartOfDayIn(timeZone)
                    start.toEpochMilliseconds() to end.toEpochMilliseconds()
                }
                is DateFilter.Last7Days -> {
                    val sevenDaysAgo = today.plus(-7, DateTimeUnit.DAY)
                    val start = sevenDaysAgo.atStartOfDayIn(timeZone)
                    val end = now
                    start.toEpochMilliseconds() to end.toEpochMilliseconds()
                }
                is DateFilter.Custom -> {
                    val start = filter.startDate.atStartOfDayIn(timeZone)
                    val end = filter.endDate.plus(1, DateTimeUnit.DAY).atStartOfDayIn(timeZone)
                    start.toEpochMilliseconds() to end.toEpochMilliseconds()
                }
            }

        loadHistory(startTime, endTime)
    }

    /**
     * Load location history from local database (AC E4.1.3)
     */
    private fun loadHistory(startTime: Long, endTime: Long) {
        viewModelScope.launch {
            try {
                val locations = locationDao.getLocationsBetween(startTime, endTime)

                // Story E4.2: Downsample for performance (AC E4.2.2)
                val allPoints = locations.map { loc -> LatLng(loc.latitude, loc.longitude) }
                val downsampledPoints = if (allPoints.size > 500) {
                    PolylineUtils.downsample(allPoints, targetCount = 300)
                } else {
                    allPoints
                }

                _uiState.update {
                    it.copy(
                        locations = locations,
                        polylinePoints = downsampledPoints,
                        isLoading = false,
                        isEmpty = locations.isEmpty(),
                    )
                }

                Timber.d(
                    "Loaded ${locations.size} locations, downsampled to ${downsampledPoints.size} points for history",
                )
            } catch (e: Exception) {
                Timber.e(e, "Failed to load location history")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load history",
                    )
                }
            }
        }
    }
}

/**
 * UI State for History Screen
 *
 * @property locations List of location entities for display
 * @property polylinePoints Converted LatLng points for polyline
 * @property selectedFilter Currently selected date filter
 * @property isEmpty True when no locations found for selected range
 * @property isLoading True when fetching history
 * @property error Error message if fetch failed
 */
data class HistoryUiState(
    val locations: List<LocationEntity> = emptyList(),
    val polylinePoints: List<LatLng> = emptyList(),
    val selectedFilter: DateFilter = DateFilter.Today,
    val isEmpty: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null,
)

/**
 * Date filter options (AC E4.1.4, E4.1.5)
 */
sealed class DateFilter {
    data object Today : DateFilter()

    data object Yesterday : DateFilter()

    data object Last7Days : DateFilter()

    data class Custom(val startDate: LocalDate, val endDate: LocalDate) : DateFilter()
}

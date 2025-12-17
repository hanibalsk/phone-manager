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
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import three.two.bit.phonemanager.data.database.LocationDao
import three.two.bit.phonemanager.data.model.LocationEntity
import three.two.bit.phonemanager.data.repository.DeviceRepository
import three.two.bit.phonemanager.network.DeviceApiService
import three.two.bit.phonemanager.util.PolylineUtils
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.Clock

/**
 * Story E4.1/E4.2: HistoryViewModel
 *
 * Story E4.1: Manages location history display with date filtering
 * Story E4.2: Adds downsampling for performance and device selector
 * ACs: E4.1.1, E4.1.3, E4.1.4, E4.1.5, E4.1.6, E4.2.1, E4.2.2, E4.2.3
 */
@HiltViewModel
class HistoryViewModel
@Inject
constructor(
    private val locationDao: LocationDao,
    private val deviceRepository: DeviceRepository,
    private val deviceApiService: DeviceApiService,
) : ViewModel() {
    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        // Load available devices for selector (AC E4.2.3)
        loadAvailableDevices()
        // AC E4.1.4: Default to "Today" preset
        setDateFilter(DateFilter.Today)
    }

    /**
     * Load current device and group members for device selector (AC E4.2.3)
     */
    private fun loadAvailableDevices() {
        viewModelScope.launch {
            // Create current device option
            val currentDeviceId = deviceRepository.getDeviceId()
            val currentDeviceName = deviceRepository.getDisplayName() ?: "My Device"
            val currentDevice = HistoryDevice(
                deviceId = currentDeviceId,
                displayName = currentDeviceName,
                isCurrentDevice = true,
            )

            // Set current device as selected by default
            _uiState.update {
                it.copy(
                    selectedDevice = currentDevice,
                    availableDevices = listOf(currentDevice),
                )
            }

            // Fetch group members
            deviceRepository.getGroupMembers()
                .onSuccess { groupMembers ->
                    val historyDevices = groupMembers.map { device ->
                        HistoryDevice(
                            deviceId = device.deviceId,
                            displayName = device.displayName,
                            isCurrentDevice = false,
                        )
                    }
                    _uiState.update {
                        it.copy(availableDevices = listOf(currentDevice) + historyDevices)
                    }
                    Timber.d("Loaded ${historyDevices.size} group members for history selector")
                }
                .onFailure { e ->
                    Timber.e(e, "Failed to load group members for history selector")
                    // Keep just current device available
                }
        }
    }

    /**
     * Select a device to view history (AC E4.2.3)
     */
    fun selectDevice(device: HistoryDevice) {
        _uiState.update { it.copy(selectedDevice = device) }
        // Reload history for the selected device
        setDateFilter(_uiState.value.selectedFilter)
    }

    /**
     * Show the custom date range picker flow (AC E4.1.5)
     * First shows start date picker, then end date picker
     */
    fun onCustomRangeClicked() {
        val timeZone = TimeZone.currentSystemDefault()
        val now = Clock.System.now()
        val today = now.toLocalDateTime(timeZone).date
        val sevenDaysAgo = today.plus(-7, DateTimeUnit.DAY)

        // Initialize with sensible defaults (last 7 days)
        _uiState.update {
            it.copy(
                customStartDate = sevenDaysAgo,
                customEndDate = today,
                showStartDatePicker = true,
            )
        }
    }

    /**
     * Handle start date selection (AC E4.1.5)
     */
    fun onStartDateSelected(dateMillis: Long?) {
        if (dateMillis != null) {
            val timeZone = TimeZone.currentSystemDefault()
            val instant = kotlin.time.Instant.fromEpochMilliseconds(dateMillis)
            val date = instant.toLocalDateTime(timeZone).date

            _uiState.update {
                it.copy(
                    customStartDate = date,
                    showStartDatePicker = false,
                    showEndDatePicker = true, // Move to end date selection
                )
            }
        } else {
            // User cancelled, reset to previous filter
            _uiState.update {
                it.copy(
                    showStartDatePicker = false,
                    customStartDate = null,
                    customEndDate = null,
                )
            }
        }
    }

    /**
     * Handle end date selection and apply custom filter (AC E4.1.5)
     */
    fun onEndDateSelected(dateMillis: Long?) {
        val startDate = _uiState.value.customStartDate

        if (dateMillis != null && startDate != null) {
            val timeZone = TimeZone.currentSystemDefault()
            val instant = kotlin.time.Instant.fromEpochMilliseconds(dateMillis)
            val endDate = instant.toLocalDateTime(timeZone).date

            // Ensure end date is not before start date
            val validEndDate = if (endDate < startDate) startDate else endDate

            _uiState.update {
                it.copy(
                    showEndDatePicker = false,
                    customEndDate = validEndDate,
                )
            }

            // Apply the custom filter
            setDateFilter(DateFilter.Custom(startDate, validEndDate))
        } else {
            // User cancelled, reset
            _uiState.update {
                it.copy(
                    showEndDatePicker = false,
                    customStartDate = null,
                    customEndDate = null,
                )
            }
        }
    }

    /**
     * Dismiss date pickers (AC E4.1.5)
     */
    fun dismissDatePicker() {
        _uiState.update {
            it.copy(
                showStartDatePicker = false,
                showEndDatePicker = false,
            )
        }
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
     * Load location history from local database or server (AC E4.1.3, E4.2.1)
     */
    private fun loadHistory(startTime: Long, endTime: Long) {
        viewModelScope.launch {
            try {
                val selectedDevice = _uiState.value.selectedDevice
                val polylinePoints: List<LatLng>

                if (selectedDevice?.isCurrentDevice == true) {
                    // Load from local database for current device (AC E4.1.3)
                    val locations = locationDao.getLocationsBetween(startTime, endTime)

                    // Story E4.2: Downsample for performance (AC E4.2.2)
                    val allPoints = locations.map { loc -> LatLng(loc.latitude, loc.longitude) }
                    polylinePoints = if (allPoints.size > 500) {
                        PolylineUtils.downsample(allPoints, targetCount = 300)
                    } else {
                        allPoints
                    }

                    _uiState.update {
                        it.copy(
                            locations = locations,
                            polylinePoints = polylinePoints,
                            isLoading = false,
                            isEmpty = locations.isEmpty(),
                        )
                    }

                    Timber.d(
                        "Loaded ${locations.size} local locations, downsampled to ${polylinePoints.size} points",
                    )
                } else if (selectedDevice != null) {
                    // Load from server for other devices (AC E4.2.1)
                    loadRemoteHistory(selectedDevice.deviceId, startTime, endTime)
                } else {
                    // No device selected, show empty state
                    _uiState.update {
                        it.copy(
                            locations = emptyList(),
                            polylinePoints = emptyList(),
                            isLoading = false,
                            isEmpty = true,
                        )
                    }
                }
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

    companion object {
        /** Default tolerance for server-side downsampling in meters (medium detail) */
        const val DEFAULT_TOLERANCE_METERS = 50f
    }

    /**
     * Load location history from server for a remote device (AC E4.2.1, E4.2.5)
     */
    private suspend fun loadRemoteHistory(deviceId: String, startTime: Long, endTime: Long) {
        deviceApiService.getLocationHistory(
            deviceId = deviceId,
            from = startTime,
            to = endTime,
            limit = 1000, // Fetch up to 1000 points, will downsample if needed
            order = "asc", // Chronological order for polyline
            tolerance = DEFAULT_TOLERANCE_METERS, // AC E4.2.5: Server-side downsampling (50m = medium detail)
        )
            .onSuccess { response ->
                // Convert API response to LatLng points
                val allPoints = response.locations.map { loc ->
                    LatLng(loc.latitude, loc.longitude)
                }

                // Story E4.2: Downsample for performance (AC E4.2.2)
                val polylinePoints = if (allPoints.size > 500) {
                    PolylineUtils.downsample(allPoints, targetCount = 300)
                } else {
                    allPoints
                }

                _uiState.update {
                    it.copy(
                        locations = emptyList(), // Remote history doesn't use LocationEntity
                        polylinePoints = polylinePoints,
                        isLoading = false,
                        isEmpty = allPoints.isEmpty(),
                    )
                }

                Timber.d(
                    "Loaded ${allPoints.size} remote locations for device $deviceId, " +
                        "downsampled to ${polylinePoints.size} points",
                )
            }
            .onFailure { e ->
                Timber.e(e, "Failed to load remote location history for device $deviceId")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load history from server",
                    )
                }
            }
    }
}

/**
 * UI State for History Screen
 *
 * @property locations List of location entities for display (local device only)
 * @property polylinePoints Converted LatLng points for polyline
 * @property selectedFilter Currently selected date filter
 * @property selectedDevice Currently selected device for history viewing (AC E4.2.3)
 * @property availableDevices List of available devices (current + group members) (AC E4.2.3)
 * @property isEmpty True when no locations found for selected range
 * @property isLoading True when fetching history
 * @property error Error message if fetch failed
 * @property showStartDatePicker True when start date picker should be shown (AC E4.1.5)
 * @property showEndDatePicker True when end date picker should be shown (AC E4.1.5)
 * @property customStartDate Temporary start date selection for custom range (AC E4.1.5)
 * @property customEndDate Temporary end date selection for custom range (AC E4.1.5)
 */
data class HistoryUiState(
    val locations: List<LocationEntity> = emptyList(),
    val polylinePoints: List<LatLng> = emptyList(),
    val selectedFilter: DateFilter = DateFilter.Today,
    val selectedDevice: HistoryDevice? = null,
    val availableDevices: List<HistoryDevice> = emptyList(),
    val isEmpty: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null,
    val showStartDatePicker: Boolean = false,
    val showEndDatePicker: Boolean = false,
    val customStartDate: LocalDate? = null,
    val customEndDate: LocalDate? = null,
)

/**
 * Represents a device option in the history device selector (AC E4.2.3)
 *
 * @property deviceId Unique identifier for the device
 * @property displayName Human-readable name for the device
 * @property isCurrentDevice True if this is the current user's device (local history)
 */
data class HistoryDevice(val deviceId: String, val displayName: String, val isCurrentDevice: Boolean)

/**
 * Date filter options (AC E4.1.4, E4.1.5)
 */
sealed class DateFilter {
    data object Today : DateFilter()

    data object Yesterday : DateFilter()

    data object Last7Days : DateFilter()

    data class Custom(val startDate: LocalDate, val endDate: LocalDate) : DateFilter()
}

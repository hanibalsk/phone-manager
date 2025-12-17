package three.two.bit.phonemanager.ui.movementevents

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import three.two.bit.phonemanager.data.repository.MovementEventRepository
import three.two.bit.phonemanager.domain.model.MovementEvent
import three.two.bit.phonemanager.movement.TransportationMode
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds

/**
 * Story E8.11: MovementEventsViewModel
 *
 * Manages movement events display for developer debugging.
 * ACs: E8.11.1-E8.11.9
 */
@HiltViewModel
class MovementEventsViewModel @Inject constructor(
    private val movementEventRepository: MovementEventRepository,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {

    companion object {
        private const val PAGE_SIZE = 20
        private val LIVE_MODE_INTERVAL = 5.seconds
    }

    private val _uiState = MutableStateFlow(MovementEventsUiState())
    val uiState: StateFlow<MovementEventsUiState> = _uiState.asStateFlow()

    private var liveModeJob: Job? = null
    private var loadedCount = 0

    init {
        loadEvents()
        loadStatistics()
    }

    /**
     * Load movement events with current filters (AC E8.11.3)
     */
    private fun loadEvents() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                movementEventRepository.observeRecentEvents(PAGE_SIZE)
                    .onEach { events ->
                        val filtered = filterEvents(events)
                        loadedCount = events.size
                        _uiState.update {
                            it.copy(
                                events = filtered,
                                isLoading = false,
                                hasMorePages = events.size >= PAGE_SIZE,
                            )
                        }
                    }
                    .catch { e ->
                        Timber.e(e, "Failed to load events")
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = e.message ?: "Failed to load events",
                            )
                        }
                    }
                    .launchIn(viewModelScope)
            } catch (e: Exception) {
                Timber.e(e, "Failed to load events")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load events",
                    )
                }
            }
        }
    }

    /**
     * Load more events for pagination (AC E8.11.8)
     */
    fun loadMore() {
        if (_uiState.value.isLoadingMore || !_uiState.value.hasMorePages) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }

            try {
                val newLimit = loadedCount + PAGE_SIZE
                movementEventRepository.observeRecentEvents(newLimit)
                    .onEach { events ->
                        val filtered = filterEvents(events)
                        loadedCount = events.size
                        _uiState.update {
                            it.copy(
                                events = filtered,
                                isLoadingMore = false,
                                hasMorePages = events.size >= newLimit,
                            )
                        }
                    }
                    .catch { e ->
                        Timber.e(e, "Failed to load more events")
                        _uiState.update { it.copy(isLoadingMore = false) }
                    }
                    .launchIn(viewModelScope)
            } catch (e: Exception) {
                Timber.e(e, "Failed to load more events")
                _uiState.update { it.copy(isLoadingMore = false) }
            }
        }
    }

    /**
     * Toggle live mode for auto-refresh (AC E8.11.2)
     */
    fun toggleLiveMode() {
        val newLiveMode = !_uiState.value.isLiveMode
        _uiState.update { it.copy(isLiveMode = newLiveMode) }

        if (newLiveMode) {
            startLiveMode()
        } else {
            stopLiveMode()
        }
    }

    private fun startLiveMode() {
        liveModeJob?.cancel()
        liveModeJob = viewModelScope.launch {
            while (isActive) {
                loadEvents()
                loadStatistics()
                delay(LIVE_MODE_INTERVAL)
            }
        }
    }

    private fun stopLiveMode() {
        liveModeJob?.cancel()
        liveModeJob = null
    }

    /**
     * Set mode filter (AC E8.11.7)
     */
    fun setModeFilter(mode: TransportationMode?) {
        _uiState.update { it.copy(modeFilter = mode) }
        loadEvents()
    }

    /**
     * Toggle event expansion (AC E8.11.4)
     */
    fun toggleEventExpansion(eventId: Long) {
        _uiState.update { state ->
            val expandedIds = state.expandedEventIds.toMutableSet()
            if (expandedIds.contains(eventId)) {
                expandedIds.remove(eventId)
            } else {
                expandedIds.add(eventId)
            }
            state.copy(expandedEventIds = expandedIds)
        }
    }

    /**
     * Export events to JSON (AC E8.11.5)
     */
    fun exportToJson() {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true) }

            try {
                val events = _uiState.value.events
                val json = Json { prettyPrint = true }
                val exportData = events.map { event ->
                    mapOf(
                        "id" to event.id,
                        "timestamp" to event.timestamp.toString(),
                        "previousMode" to event.previousMode.name,
                        "newMode" to event.newMode.name,
                        "detectionSource" to event.detectionSource.name,
                        "confidence" to event.confidence,
                        "detectionLatencyMs" to event.detectionLatencyMs,
                        "location" to event.location?.let {
                            mapOf(
                                "latitude" to it.latitude,
                                "longitude" to it.longitude,
                                "accuracy" to it.accuracy,
                            )
                        },
                        "deviceState" to event.deviceState?.let {
                            mapOf(
                                "batteryLevel" to it.batteryLevel,
                                "batteryCharging" to it.batteryCharging,
                                "networkType" to it.networkType?.name,
                            )
                        },
                    )
                }

                val jsonContent = json.encodeToString(exportData)
                val fileName = "movement_events_${Clock.System.now().epochSeconds}.json"
                val file = File(context.cacheDir, fileName)
                file.writeText(jsonContent)

                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    file,
                )

                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                context.startActivity(
                    Intent.createChooser(shareIntent, "Export Events").apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    },
                )

                _uiState.update { it.copy(isExporting = false) }
                Timber.d("Exported ${events.size} events to JSON")
            } catch (e: Exception) {
                Timber.e(e, "Failed to export events")
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        error = e.message ?: "Failed to export events",
                    )
                }
            }
        }
    }

    /**
     * Clear old events (>7 days) (AC E8.11.6)
     */
    fun clearOldEvents() {
        viewModelScope.launch {
            try {
                val sevenDaysAgo = Clock.System.now().minus(7.days)
                val deletedCount = movementEventRepository.deleteOldEvents(sevenDaysAgo)
                Timber.d("Deleted $deletedCount old events")
                loadEvents()
                loadStatistics()
            } catch (e: Exception) {
                Timber.e(e, "Failed to clear old events")
                _uiState.update {
                    it.copy(error = e.message ?: "Failed to clear events")
                }
            }
        }
    }

    /**
     * Show clear confirmation dialog
     */
    fun showClearConfirmation(show: Boolean) {
        _uiState.update { it.copy(showClearConfirmation = show) }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Load statistics (AC E8.11.9)
     */
    private fun loadStatistics() {
        viewModelScope.launch {
            try {
                movementEventRepository.observeUnsyncedCount()
                    .onEach { unsyncedCount ->
                        _uiState.update { it.copy(unsyncedCount = unsyncedCount) }
                    }
                    .launchIn(viewModelScope)
            } catch (e: Exception) {
                Timber.e(e, "Failed to load statistics")
            }
        }
    }

    /**
     * Filter events by mode
     */
    private fun filterEvents(events: List<MovementEvent>): List<MovementEvent> {
        val modeFilter = _uiState.value.modeFilter ?: return events
        return events.filter { event ->
            event.previousMode == modeFilter || event.newMode == modeFilter
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopLiveMode()
    }
}

/**
 * UI State for Movement Events Screen
 */
data class MovementEventsUiState(
    val events: List<MovementEvent> = emptyList(),
    val expandedEventIds: Set<Long> = emptySet(),
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val isExporting: Boolean = false,
    val hasMorePages: Boolean = false,
    val error: String? = null,
    // Live mode
    val isLiveMode: Boolean = false,
    // Filters
    val modeFilter: TransportationMode? = null,
    // Statistics
    val unsyncedCount: Int = 0,
    // Dialogs
    val showClearConfirmation: Boolean = false,
)

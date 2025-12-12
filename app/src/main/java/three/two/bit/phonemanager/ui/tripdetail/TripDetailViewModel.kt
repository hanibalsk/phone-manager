package three.two.bit.phonemanager.ui.tripdetail

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import three.two.bit.phonemanager.data.model.LocationEntity
import three.two.bit.phonemanager.data.repository.LocationRepository
import three.two.bit.phonemanager.data.repository.MovementEventRepository
import three.two.bit.phonemanager.data.repository.TripRepository
import three.two.bit.phonemanager.domain.model.MovementEvent
import three.two.bit.phonemanager.domain.model.Trip
import three.two.bit.phonemanager.movement.TransportationMode
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * Story E8.10: TripDetailViewModel
 *
 * Manages trip detail display with route map, statistics, and actions.
 * ACs: E8.10.1, E8.10.3, E8.10.7, E8.10.8, E8.10.9
 */
@HiltViewModel
class TripDetailViewModel @Inject constructor(
    private val tripRepository: TripRepository,
    private val locationRepository: LocationRepository,
    private val movementEventRepository: MovementEventRepository,
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val tripId: String = checkNotNull(savedStateHandle["tripId"])

    private val _uiState = MutableStateFlow(TripDetailUiState())
    val uiState: StateFlow<TripDetailUiState> = _uiState.asStateFlow()

    init {
        loadTripDetails()
    }

    /**
     * Load trip with locations and movement events
     */
    private fun loadTripDetails() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val trip = tripRepository.getTripById(tripId)
                if (trip == null) {
                    _uiState.update {
                        it.copy(isLoading = false, error = "Trip not found")
                    }
                    return@launch
                }

                // Load locations for the trip timeframe
                val locations = trip.endTime?.let { endTime ->
                    locationRepository.getLocationsBetween(
                        trip.startTime.toEpochMilliseconds(),
                        endTime.toEpochMilliseconds(),
                    )
                } ?: emptyList()

                // Load movement events for the trip
                val movementEvents = trip.endTime?.let { endTime ->
                    movementEventRepository.getEventsBetween(trip.startTime, endTime)
                } ?: emptyList()

                // Calculate mode breakdown
                val modeBreakdown = calculateModeBreakdown(movementEvents, trip)

                // Calculate statistics
                val statistics = calculateStatistics(trip, locations, movementEvents)

                _uiState.update {
                    it.copy(
                        trip = trip,
                        locations = locations,
                        movementEvents = movementEvents,
                        modeBreakdown = modeBreakdown,
                        statistics = statistics,
                        isLoading = false,
                    )
                }

                Timber.d("Loaded trip details: ${locations.size} locations, ${movementEvents.size} events")
            } catch (e: Exception) {
                Timber.e(e, "Failed to load trip details")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load trip details",
                    )
                }
            }
        }
    }

    /**
     * Toggle between raw and corrected path view (AC E8.10.3)
     */
    fun togglePathView() {
        _uiState.update { it.copy(showCorrectedPath = !it.showCorrectedPath) }
    }

    /**
     * Select a location point on the map (AC E8.10.10)
     */
    fun selectLocation(index: Int?) {
        _uiState.update { it.copy(selectedLocationIndex = index) }
    }

    /**
     * Show edit name dialog (AC E8.10.7)
     */
    fun showEditNameDialog(show: Boolean) {
        _uiState.update { it.copy(showEditNameDialog = show) }
    }

    /**
     * Update trip name (AC E8.10.7)
     */
    fun updateTripName(name: String) {
        if (name.isBlank()) return

        viewModelScope.launch {
            try {
                val trip = _uiState.value.trip ?: return@launch
                val updatedTrip = trip.copy(name = name)
                tripRepository.update(updatedTrip)

                _uiState.update {
                    it.copy(
                        trip = updatedTrip,
                        showEditNameDialog = false,
                    )
                }

                Timber.d("Updated trip name to: $name")
            } catch (e: Exception) {
                Timber.e(e, "Failed to update trip name")
                _uiState.update {
                    it.copy(error = e.message ?: "Failed to update name")
                }
            }
        }
    }

    /**
     * Export trip to GPX file (AC E8.10.8)
     */
    fun exportToGpx() {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true) }

            try {
                val trip = _uiState.value.trip ?: throw IllegalStateException("No trip loaded")
                val locations = _uiState.value.locations

                val gpxContent = generateGpxContent(trip, locations)
                val fileName = "trip_${trip.id}.gpx"
                val file = File(context.cacheDir, fileName)
                file.writeText(gpxContent)

                // Create share intent
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    file,
                )

                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/gpx+xml"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                context.startActivity(
                    Intent.createChooser(shareIntent, "Export Trip").apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    },
                )

                _uiState.update { it.copy(isExporting = false) }
                Timber.d("Exported trip to GPX: $fileName")
            } catch (e: Exception) {
                Timber.e(e, "Failed to export GPX")
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        error = e.message ?: "Failed to export GPX",
                    )
                }
            }
        }
    }

    /**
     * Show delete confirmation dialog (AC E8.10.9)
     */
    fun showDeleteConfirmation(show: Boolean) {
        _uiState.update { it.copy(showDeleteConfirmation = show) }
    }

    /**
     * Delete the trip (AC E8.10.9)
     */
    fun deleteTrip(onDeleted: () -> Unit) {
        viewModelScope.launch {
            try {
                tripRepository.deleteTrip(tripId)
                _uiState.update { it.copy(showDeleteConfirmation = false) }
                Timber.d("Deleted trip: $tripId")
                onDeleted()
            } catch (e: Exception) {
                Timber.e(e, "Failed to delete trip")
                _uiState.update {
                    it.copy(
                        showDeleteConfirmation = false,
                        error = e.message ?: "Failed to delete trip",
                    )
                }
            }
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Calculate mode breakdown from movement events
     */
    private fun calculateModeBreakdown(events: List<MovementEvent>, trip: Trip): List<ModeBreakdownItem> {
        if (events.isEmpty()) {
            // Return 100% for dominant mode if no events
            return listOf(
                ModeBreakdownItem(
                    mode = trip.dominantMode,
                    percentage = 100f,
                    durationSeconds = trip.durationSeconds?.toInt() ?: 0,
                ),
            )
        }

        // Group events by mode and calculate duration for each
        val modeDurations = mutableMapOf<TransportationMode, Long>()
        var previousEvent: MovementEvent? = null

        for (event in events.sortedBy { it.timestamp }) {
            previousEvent?.let { prev ->
                val duration = event.timestamp.epochSeconds - prev.timestamp.epochSeconds
                modeDurations[prev.newMode] = (modeDurations[prev.newMode] ?: 0L) + duration
            }
            previousEvent = event
        }

        // Add duration for last segment
        previousEvent?.let { last ->
            val endTime = trip.endTime ?: Instant.fromEpochMilliseconds(System.currentTimeMillis())
            val duration = endTime.epochSeconds - last.timestamp.epochSeconds
            modeDurations[last.newMode] = (modeDurations[last.newMode] ?: 0L) + duration
        }

        val totalDuration = modeDurations.values.sum().toFloat()
        if (totalDuration == 0f) {
            return listOf(
                ModeBreakdownItem(trip.dominantMode, 100f, 0),
            )
        }

        return modeDurations.map { (mode, duration) ->
            ModeBreakdownItem(
                mode = mode,
                percentage = (duration / totalDuration) * 100f,
                durationSeconds = duration.toInt(),
            )
        }.sortedByDescending { it.percentage }
    }

    /**
     * Calculate trip statistics
     */
    private suspend fun calculateStatistics(
        trip: Trip,
        locations: List<LocationEntity>,
        events: List<MovementEvent>,
    ): TripStatistics {
        val avgSpeed = if (locations.isNotEmpty()) {
            val speeds = locations.mapNotNull { it.speed }
            if (speeds.isNotEmpty()) speeds.average().toFloat() else null
        } else {
            null
        }

        // Check if path is corrected via API (only for synced trips)
        val isPathCorrected = if (trip.isSynced && trip.serverId != null) {
            tripRepository.getTripPath(trip.serverId)
                .map { it.corrected }
                .getOrDefault(false)
        } else {
            false
        }

        return TripStatistics(
            averageSpeedKmh = avgSpeed?.let { it * 3.6f }, // m/s to km/h
            locationPointCount = locations.size,
            movementEventCount = events.size,
            isPathCorrected = isPathCorrected,
        )
    }

    /**
     * Generate GPX content
     */
    private fun generateGpxContent(trip: Trip, locations: List<LocationEntity>): String {
        val timeZone = TimeZone.currentSystemDefault()
        val startTime = trip.startTime.toLocalDateTime(timeZone)
        val tripName = trip.name ?: getTripName(trip)

        val builder = StringBuilder()
        builder.appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
        builder.appendLine("""<gpx version="1.1" creator="PhoneManager" xmlns="http://www.topografix.com/GPX/1/1">""")
        builder.appendLine("  <metadata>")
        builder.appendLine("    <name>$tripName</name>")
        builder.appendLine("    <time>${trip.startTime}</time>")
        builder.appendLine("  </metadata>")
        builder.appendLine("  <trk>")
        builder.appendLine("    <name>Trip</name>")
        builder.appendLine("    <trkseg>")

        for (location in locations.sortedBy { it.timestamp }) {
            val locationTime = Instant.fromEpochMilliseconds(location.timestamp)
            builder.appendLine("""      <trkpt lat="${location.latitude}" lon="${location.longitude}">""")
            builder.appendLine("        <time>$locationTime</time>")
            location.altitude?.let { builder.appendLine("        <ele>$it</ele>") }
            location.speed?.let { builder.appendLine("        <speed>$it</speed>") }
            builder.appendLine("      </trkpt>")
        }

        builder.appendLine("    </trkseg>")
        builder.appendLine("  </trk>")
        builder.appendLine("</gpx>")

        return builder.toString()
    }

    /**
     * Get default trip name from mode
     */
    private fun getTripName(trip: Trip): String = when (trip.dominantMode) {
        TransportationMode.WALKING -> "Walk"
        TransportationMode.RUNNING -> "Run"
        TransportationMode.CYCLING -> "Bike Ride"
        TransportationMode.IN_VEHICLE -> "Drive"
        TransportationMode.STATIONARY -> "Stationary"
        TransportationMode.UNKNOWN -> "Trip"
    }
}

/**
 * UI State for Trip Detail Screen
 */
data class TripDetailUiState(
    val trip: Trip? = null,
    val locations: List<LocationEntity> = emptyList(),
    val movementEvents: List<MovementEvent> = emptyList(),
    val modeBreakdown: List<ModeBreakdownItem> = emptyList(),
    val statistics: TripStatistics? = null,
    val isLoading: Boolean = true,
    val isExporting: Boolean = false,
    val error: String? = null,
    // Map state
    val showCorrectedPath: Boolean = false,
    val selectedLocationIndex: Int? = null,
    // Dialogs
    val showEditNameDialog: Boolean = false,
    val showDeleteConfirmation: Boolean = false,
)

/**
 * Mode breakdown item for chart display
 */
data class ModeBreakdownItem(val mode: TransportationMode, val percentage: Float, val durationSeconds: Int)

/**
 * Trip statistics
 */
data class TripStatistics(
    val averageSpeedKmh: Float?,
    val locationPointCount: Int,
    val movementEventCount: Int,
    val isPathCorrected: Boolean,
)

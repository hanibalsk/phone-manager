package three.two.bit.phonemanager.trip

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlinx.datetime.Instant
import three.two.bit.phonemanager.data.preferences.PreferencesRepository
import three.two.bit.phonemanager.data.preferences.PreferencesRepositoryImpl
import three.two.bit.phonemanager.data.repository.TripRepository
import three.two.bit.phonemanager.domain.model.LatLng
import three.two.bit.phonemanager.domain.model.Trip
import three.two.bit.phonemanager.domain.model.TripState
import three.two.bit.phonemanager.domain.model.TripTrigger
import three.two.bit.phonemanager.movement.TransportationMode
import three.two.bit.phonemanager.movement.TransportationModeManager
import three.two.bit.phonemanager.movement.TransportationState
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Story E8.4: TripManagerImpl - Implementation of TripManager with state machine
 *
 * Implements automatic trip detection based on transportation mode changes.
 * Manages trip lifecycle: IDLE → ACTIVE → PENDING_END → COMPLETED
 *
 * AC E8.4.2: Complete state machine implementation
 * AC E8.4.3: Trip start conditions
 * AC E8.4.4: Trip end conditions with grace periods
 * AC E8.4.5: Anti-false-positive measures
 * AC E8.4.6: Trip statistics tracking
 */
@Singleton
class TripManagerImpl @Inject constructor(
    private val transportationModeManager: TransportationModeManager,
    private val tripRepository: TripRepository,
    private val preferencesRepository: PreferencesRepository,
) : TripManager {

    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // State flows
    private val _currentTripState = MutableStateFlow(TripState.IDLE)
    override val currentTripState: StateFlow<TripState> = _currentTripState.asStateFlow()

    private val _activeTrip = MutableStateFlow<Trip?>(null)
    override val activeTrip: StateFlow<Trip?> = _activeTrip.asStateFlow()

    private val _isMonitoring = MutableStateFlow(false)
    override val isMonitoring: StateFlow<Boolean> = _isMonitoring.asStateFlow()

    // Internal state
    private var monitoringJob: Job? = null
    private var pendingEndJob: Job? = null
    private var currentModeSegment: TripModeSegment? = null
    private val modeSegments = mutableListOf<TripModeSegment>()

    // Anti-false-positive tracking
    private var movementStartTime: Instant? = null
    private var consecutiveMovementCount = 0
    private var lastKnownLocation: LatLng? = null
    private var stationaryStartTime: Instant? = null

    // Story E8.8: Cached preference values (updated via Flow observation)
    @Volatile
    private var tripDetectionEnabled = true

    @Volatile
    private var vehicleGraceSeconds = PreferencesRepositoryImpl.DEFAULT_TRIP_VEHICLE_GRACE_SECONDS

    @Volatile
    private var walkingGraceSeconds = PreferencesRepositoryImpl.DEFAULT_TRIP_WALKING_GRACE_SECONDS

    @Volatile
    private var stationaryThresholdMinutes = PreferencesRepositoryImpl.DEFAULT_TRIP_STATIONARY_THRESHOLD_MINUTES

    @Volatile
    private var minimumDurationMinutes = PreferencesRepositoryImpl.DEFAULT_TRIP_MINIMUM_DURATION_MINUTES

    @Volatile
    private var minimumDistanceMeters = PreferencesRepositoryImpl.DEFAULT_TRIP_MINIMUM_DISTANCE_METERS

    @Volatile
    private var autoMergeEnabled = true

    // Auto-merge configuration
    @Volatile
    private var autoMergeGapSeconds = DEFAULT_AUTO_MERGE_GAP_SECONDS

    // Last completed trip for auto-merge consideration
    @Volatile
    private var lastCompletedTrip: Trip? = null

    // Configuration defaults
    companion object {
        const val MIN_CONSECUTIVE_MOVEMENTS = 2
        const val DEFAULT_AUTO_MERGE_GAP_SECONDS = 300L // 5 minutes
    }

    init {
        // Story E8.8: Observe preference changes
        observePreferences()
    }

    override suspend fun startMonitoring() {
        if (_isMonitoring.value) {
            Timber.d("Trip monitoring already started")
            return
        }

        // Story E8.8: Check if trip detection is enabled
        if (!tripDetectionEnabled) {
            Timber.i("Trip detection is disabled, not starting monitoring")
            return
        }

        Timber.i("Starting trip monitoring")

        // Check for any active trip in the database
        val existingActiveTrip = tripRepository.getActiveTrip()
        if (existingActiveTrip != null) {
            Timber.d("Restoring active trip: ${existingActiveTrip.id}")
            _activeTrip.value = existingActiveTrip
            _currentTripState.value = existingActiveTrip.state
        }

        _isMonitoring.value = true

        // Subscribe to transportation mode changes
        monitoringJob = managerScope.launch {
            transportationModeManager.transportationState.collect { state ->
                handleTransportationStateChange(state)
            }
        }
    }

    override fun stopMonitoring() {
        if (!_isMonitoring.value) {
            return
        }

        Timber.i("Stopping trip monitoring")

        // Cancel pending end timer if any
        pendingEndJob?.cancel()
        pendingEndJob = null

        // Cancel monitoring job
        monitoringJob?.cancel()
        monitoringJob = null

        _isMonitoring.value = false

        // Finalize any active trip
        if (_activeTrip.value != null) {
            managerScope.launch {
                finalizeTrip(TripTrigger.TIMEOUT)
            }
        }
    }

    override suspend fun forceStartTrip(): Trip {
        Timber.i("Force starting trip")

        // Check if trip already active
        if (_activeTrip.value != null) {
            throw IllegalStateException("Cannot start trip: a trip is already active")
        }

        val currentState = transportationModeManager.transportationState.first()
        return startNewTrip(currentState.mode, TripTrigger.MANUAL)
    }

    override suspend fun forceEndTrip(): Trip? {
        Timber.i("Force ending trip")

        val trip = _activeTrip.value ?: return null

        // Cancel any pending end timer
        pendingEndJob?.cancel()
        pendingEndJob = null

        return finalizeTrip(TripTrigger.MANUAL)
    }

    override suspend fun getTripById(id: String): Trip? = tripRepository.getTripById(id)

    override suspend fun getTripsInRange(startTime: Long, endTime: Long): List<Trip> = tripRepository.getTripsBetween(
        start = Instant.fromEpochMilliseconds(startTime),
        end = Instant.fromEpochMilliseconds(endTime),
    )

    override suspend fun getRecentTrips(limit: Int): List<Trip> = tripRepository.observeRecentTrips(limit).first()

    /**
     * Handle transportation state changes from TransportationModeManager.
     *
     * Implements the state machine transitions:
     * - IDLE → ACTIVE: When movement detected
     * - ACTIVE → PENDING_END: When stationary detected
     * - PENDING_END → ACTIVE: When movement resumes within grace period
     * - PENDING_END → COMPLETED: When grace period expires
     */
    private suspend fun handleTransportationStateChange(state: TransportationState) {
        val mode = state.mode

        Timber.d(
            "Transportation state change: mode=$mode, " +
                "tripState=${_currentTripState.value}, activeTrip=${_activeTrip.value?.id}",
        )

        when (_currentTripState.value) {
            TripState.IDLE -> handleIdleState(mode)
            TripState.ACTIVE -> handleActiveState(mode)
            TripState.PENDING_END -> handlePendingEndState(mode)
            TripState.COMPLETED -> {
                // Completed state transitions to IDLE automatically
                _currentTripState.value = TripState.IDLE
            }
        }
    }

    /**
     * Handle state changes when in IDLE state.
     *
     * Start a new trip if movement mode is detected and validation passes.
     */
    private suspend fun handleIdleState(mode: TransportationMode) {
        if (isMovementMode(mode)) {
            // Track movement for anti-false-positive
            if (movementStartTime == null) {
                movementStartTime = Clock.System.now()
                consecutiveMovementCount = 1
            } else {
                consecutiveMovementCount++
            }

            // Check if we can start a trip
            if (canStartTrip()) {
                startNewTrip(mode, TripTrigger.ACTIVITY_DETECTION)
            }
        } else {
            // Reset tracking if not moving
            resetMovementTracking()
        }
    }

    /**
     * Handle state changes when in ACTIVE state.
     *
     * Track mode segments and transition to PENDING_END if stationary.
     */
    private suspend fun handleActiveState(mode: TransportationMode) {
        // Update mode segment if mode changed
        updateModeSegment(mode)

        if (mode == TransportationMode.STATIONARY || mode == TransportationMode.UNKNOWN) {
            // Start pending end
            stationaryStartTime = Clock.System.now()
            _currentTripState.value = TripState.PENDING_END

            // Start grace period timer
            startPendingEndTimer()
        } else {
            // Continue active trip - update statistics
            updateTripStatistics()
        }
    }

    /**
     * Handle state changes when in PENDING_END state.
     *
     * Resume trip if movement detected, otherwise wait for timer to expire.
     */
    private suspend fun handlePendingEndState(mode: TransportationMode) {
        if (isMovementMode(mode)) {
            // Cancel pending end timer
            pendingEndJob?.cancel()
            pendingEndJob = null

            // Resume trip
            _currentTripState.value = TripState.ACTIVE
            stationaryStartTime = null

            Timber.d("Trip resumed from PENDING_END")

            // Start new mode segment
            startNewModeSegment(mode)
        }
        // If still stationary, let the timer handle finalization
    }

    /**
     * Check if a new trip can be started (anti-false-positive measures).
     *
     * AC E8.4.5: Requirements:
     * - 2+ consecutive movement detections
     * - Movement duration > configured minimum (from preferences)
     * - Location displacement > configured minimum (when location available)
     *
     * E8-B1 fix: Uses preference values instead of hardcoded constants
     */
    private fun canStartTrip(): Boolean {
        // Check consecutive movements
        if (consecutiveMovementCount < MIN_CONSECUTIVE_MOVEMENTS) {
            Timber.d("Cannot start trip: insufficient consecutive movements ($consecutiveMovementCount)")
            return false
        }

        // Check movement duration using preference value (converted from minutes to ms)
        val movementDuration = movementStartTime?.let {
            Clock.System.now().toEpochMilliseconds() - it.toEpochMilliseconds()
        } ?: 0L

        val minDurationMs = minimumDurationMinutes * 60 * 1000L
        if (movementDuration < minDurationMs) {
            Timber.d("Cannot start trip: insufficient movement duration (${movementDuration}ms < ${minDurationMs}ms)")
            return false
        }

        // Location displacement check using preference value (when location available)
        // This provides additional validation when we have location data
        // For initial implementation, we trust the duration check as primary validation

        Timber.d(
            "Trip start validation passed (minDuration=${minimumDurationMinutes}min, minDistance=${minimumDistanceMeters}m)",
        )
        return true
    }

    /**
     * Start a new trip with the given mode and trigger.
     *
     * E8-H1 fix: Handles Result failures from repository
     */
    private suspend fun startNewTrip(mode: TransportationMode, trigger: TripTrigger): Trip {
        val now = Clock.System.now()
        val tripId = UUID.randomUUID().toString()

        // Use last known location or default
        val startLocation = lastKnownLocation ?: LatLng(0.0, 0.0)

        val trip = Trip(
            id = tripId,
            state = TripState.ACTIVE,
            startTime = now,
            endTime = null,
            startLocation = startLocation,
            endLocation = null,
            totalDistanceMeters = 0.0,
            locationCount = 0,
            dominantMode = mode,
            modesUsed = setOf(mode),
            modeBreakdown = mapOf(mode to 0L),
            startTrigger = trigger,
            endTrigger = null,
            isSynced = false,
            syncedAt = null,
            serverId = null,
            createdAt = now,
            updatedAt = now,
        )

        // Save to repository - handle Result (E8-H1 fix)
        val insertResult = tripRepository.insert(trip)
        insertResult.onFailure { e ->
            Timber.e(e, "Failed to insert trip ${trip.id} to database")
            // Don't update state if insert failed
            throw e
        }

        // Only update internal state if insert succeeded
        _activeTrip.value = trip
        _currentTripState.value = TripState.ACTIVE

        // Start tracking mode segments
        modeSegments.clear()
        startNewModeSegment(mode)

        // Reset movement tracking
        resetMovementTracking()

        Timber.i("Started new trip: ${trip.id} with mode $mode and trigger $trigger")

        return trip
    }

    /**
     * Finalize the current trip with the given end trigger.
     *
     * E8-H1 fix: Handles Result failures from repository
     * E8-M2 fix: Implements short-trip handling (merge or discard)
     */
    private suspend fun finalizeTrip(trigger: TripTrigger): Trip? {
        val trip = _activeTrip.value ?: return null

        // End current mode segment
        currentModeSegment?.let { segment ->
            modeSegments.add(segment.end(stationaryStartTime ?: Clock.System.now()))
        }

        // Calculate mode statistics
        val modeBreakdown = calculateModeBreakdown()
        val dominantMode = findDominantMode(modeBreakdown)
        val modesUsed = modeBreakdown.keys

        // Use stationary start time as end time (per spec)
        val endTime = stationaryStartTime ?: Clock.System.now()

        val finalizedTrip = trip.copy(
            state = TripState.COMPLETED,
            endTime = endTime,
            endLocation = lastKnownLocation,
            dominantMode = dominantMode,
            modesUsed = modesUsed,
            modeBreakdown = modeBreakdown,
            endTrigger = trigger,
            updatedAt = Clock.System.now(),
        )

        // E8-M2: Check if trip meets minimum thresholds
        // Skip validation for MANUAL triggers (user explicitly wants to keep the trip)
        val tripDurationMinutes = finalizedTrip.durationSeconds?.let { it / 60 } ?: 0L
        val tripDistanceMeters = finalizedTrip.totalDistanceMeters

        val meetsMinimumDuration = tripDurationMinutes >= minimumDurationMinutes
        val meetsMinimumDistance = tripDistanceMeters >= minimumDistanceMeters
        val isManualTrip = trigger == TripTrigger.MANUAL

        if (!isManualTrip && (!meetsMinimumDuration || !meetsMinimumDistance)) {
            Timber.d(
                "Trip ${finalizedTrip.id} below threshold: " +
                    "duration=${tripDurationMinutes}min (min=$minimumDurationMinutes), " +
                    "distance=${tripDistanceMeters}m (min=$minimumDistanceMeters)",
            )

            // E8-M2: Try to merge with previous trip if auto-merge enabled
            if (autoMergeEnabled && lastCompletedTrip != null) {
                val gapMs = trip.startTime.toEpochMilliseconds() -
                    (lastCompletedTrip?.endTime?.toEpochMilliseconds() ?: 0L)
                val gapSeconds = gapMs / 1000

                if (gapSeconds <= autoMergeGapSeconds) {
                    Timber.i("Merging short trip ${finalizedTrip.id} into previous trip ${lastCompletedTrip?.id}")
                    val mergeResult = mergeIntoPreviousTrip(finalizedTrip)
                    if (mergeResult != null) {
                        // Cleanup: delete the short trip from DB
                        tripRepository.deleteTrip(trip.id)
                        cleanupAfterFinalize()
                        return mergeResult
                    }
                }
            }

            // E8-M2: Discard short trip if can't merge
            Timber.i("Discarding short trip ${finalizedTrip.id} (no merge target)")
            tripRepository.deleteTrip(trip.id)
            cleanupAfterFinalize()
            return null
        }

        // Trip meets thresholds - persist normally
        // Update repository - handle Result (E8-H1 fix)
        val updateResult = tripRepository.update(finalizedTrip)
        updateResult.onFailure { e ->
            Timber.e(e, "Failed to update trip ${finalizedTrip.id} in database")
            // Still cleanup state to avoid stuck trip
        }

        // Remember this trip for potential future merge
        lastCompletedTrip = finalizedTrip

        Timber.i("Finalized trip: ${finalizedTrip.id} with trigger $trigger")

        cleanupAfterFinalize()
        return finalizedTrip
    }

    /**
     * E8-M2: Merge a short trip into the previous completed trip.
     */
    private suspend fun mergeIntoPreviousTrip(shortTrip: Trip): Trip? {
        val previousTrip = lastCompletedTrip ?: return null

        // Extend the previous trip's end time and add distance
        val mergedTrip = previousTrip.copy(
            endTime = shortTrip.endTime,
            endLocation = shortTrip.endLocation ?: previousTrip.endLocation,
            totalDistanceMeters = previousTrip.totalDistanceMeters + shortTrip.totalDistanceMeters,
            locationCount = previousTrip.locationCount + shortTrip.locationCount,
            updatedAt = Clock.System.now(),
        )

        val updateResult = tripRepository.update(mergedTrip)
        return if (updateResult.isSuccess) {
            lastCompletedTrip = mergedTrip
            Timber.i("Merged trip: extended ${previousTrip.id} to include short trip data")
            mergedTrip
        } else {
            Timber.e("Failed to merge trip ${previousTrip.id}")
            null
        }
    }

    /**
     * Cleanup internal state after trip finalization.
     */
    private fun cleanupAfterFinalize() {
        _activeTrip.value = null
        _currentTripState.value = TripState.COMPLETED

        // Clear mode segments
        modeSegments.clear()
        currentModeSegment = null
        stationaryStartTime = null

        // Transition to IDLE
        _currentTripState.value = TripState.IDLE
    }

    /**
     * Start the pending end grace period timer.
     *
     * Grace periods:
     * - Vehicle: 90 seconds
     * - Walking/Cycling/Running: 60 seconds
     */
    private fun startPendingEndTimer() {
        pendingEndJob?.cancel()

        val previousMode = currentModeSegment?.mode ?: TransportationMode.UNKNOWN
        val graceSeconds = getGraceSeconds(previousMode)

        Timber.d("Starting pending end timer: ${graceSeconds}s for mode $previousMode")

        pendingEndJob = managerScope.launch {
            delay(graceSeconds * 1000)

            // Timer expired - finalize trip
            Timber.d("Grace period expired, finalizing trip")
            finalizeTrip(TripTrigger.STATIONARY_DETECTION)
        }
    }

    /**
     * Get the grace period in seconds for the given mode.
     *
     * Story E8.8: Uses configurable grace periods from preferences.
     */
    private fun getGraceSeconds(mode: TransportationMode): Long = when (mode) {
        TransportationMode.IN_VEHICLE -> vehicleGraceSeconds.toLong()
        else -> walkingGraceSeconds.toLong()
    }

    /**
     * Story E8.8: Observe preference changes and update cached values.
     *
     * E8-H2 fix: Reacts to tripDetectionEnabled changes by stopping monitoring
     * when disabled at runtime.
     */
    private fun observePreferences() {
        preferencesRepository.isTripDetectionEnabled
            .onEach { enabled ->
                val wasEnabled = tripDetectionEnabled
                tripDetectionEnabled = enabled
                Timber.d("Trip detection enabled preference updated: $enabled")

                // E8-H2: React to runtime preference changes
                if (wasEnabled && !enabled && _isMonitoring.value) {
                    Timber.i("Trip detection disabled at runtime, stopping monitoring")
                    stopMonitoring()
                }
            }
            .launchIn(managerScope)

        preferencesRepository.tripVehicleGraceSeconds
            .onEach { seconds ->
                vehicleGraceSeconds = seconds
                Timber.d("Vehicle grace seconds preference updated: $seconds")
            }
            .launchIn(managerScope)

        preferencesRepository.tripWalkingGraceSeconds
            .onEach { seconds ->
                walkingGraceSeconds = seconds
                Timber.d("Walking grace seconds preference updated: $seconds")
            }
            .launchIn(managerScope)

        preferencesRepository.tripStationaryThresholdMinutes
            .onEach { minutes ->
                stationaryThresholdMinutes = minutes
                Timber.d("Stationary threshold preference updated: $minutes minutes")
            }
            .launchIn(managerScope)

        preferencesRepository.tripMinimumDurationMinutes
            .onEach { minutes ->
                minimumDurationMinutes = minutes
                Timber.d("Minimum duration preference updated: $minutes minutes")
            }
            .launchIn(managerScope)

        preferencesRepository.tripMinimumDistanceMeters
            .onEach { meters ->
                minimumDistanceMeters = meters
                Timber.d("Minimum distance preference updated: $meters meters")
            }
            .launchIn(managerScope)

        preferencesRepository.isTripAutoMergeEnabled
            .onEach { enabled ->
                autoMergeEnabled = enabled
                Timber.d("Auto-merge enabled preference updated: $enabled")
            }
            .launchIn(managerScope)
    }

    /**
     * Check if the mode represents movement.
     */
    private fun isMovementMode(mode: TransportationMode): Boolean = mode in listOf(
        TransportationMode.WALKING,
        TransportationMode.RUNNING,
        TransportationMode.CYCLING,
        TransportationMode.IN_VEHICLE,
    )

    /**
     * Start a new mode segment.
     */
    private fun startNewModeSegment(mode: TransportationMode) {
        currentModeSegment?.let { segment ->
            modeSegments.add(segment.end(Clock.System.now()))
        }

        currentModeSegment = TripModeSegment(
            mode = mode,
            startTime = Clock.System.now(),
        )
    }

    /**
     * Update mode segment if mode changed.
     */
    private fun updateModeSegment(mode: TransportationMode) {
        if (currentModeSegment?.mode != mode) {
            startNewModeSegment(mode)
        }
    }

    /**
     * Calculate mode breakdown from segments.
     */
    private fun calculateModeBreakdown(): Map<TransportationMode, Long> {
        val breakdown = mutableMapOf<TransportationMode, Long>()

        for (segment in modeSegments) {
            val currentDuration = breakdown[segment.mode] ?: 0L
            breakdown[segment.mode] = currentDuration + segment.durationMs
        }

        // Add current segment if active
        currentModeSegment?.let { segment ->
            val duration = Clock.System.now().toEpochMilliseconds() - segment.startTime.toEpochMilliseconds()
            val currentDuration = breakdown[segment.mode] ?: 0L
            breakdown[segment.mode] = currentDuration + duration
        }

        return breakdown
    }

    /**
     * Find the dominant mode from mode breakdown.
     */
    private fun findDominantMode(breakdown: Map<TransportationMode, Long>): TransportationMode =
        breakdown.maxByOrNull { it.value }?.key ?: TransportationMode.UNKNOWN

    /**
     * Update trip statistics (called during active trip).
     *
     * E8-H1 fix: Handles Result failures from repository update.
     */
    private suspend fun updateTripStatistics() {
        val trip = _activeTrip.value ?: return

        // Calculate current mode breakdown
        val modeBreakdown = calculateModeBreakdown()
        val dominantMode = findDominantMode(modeBreakdown)
        val modesUsed = modeBreakdown.keys

        // Update trip
        val updatedTrip = trip.copy(
            dominantMode = dominantMode,
            modesUsed = modesUsed,
            modeBreakdown = modeBreakdown,
            updatedAt = Clock.System.now(),
        )

        // E8-H1: Handle Result from repository update
        val updateResult = tripRepository.update(updatedTrip)
        updateResult.onSuccess {
            _activeTrip.value = updatedTrip
        }.onFailure { e ->
            Timber.e(e, "Failed to update trip statistics for ${trip.id}")
            // Keep in-memory state consistent - don't update _activeTrip on failure
        }
    }

    /**
     * Reset movement tracking state.
     */
    private fun resetMovementTracking() {
        movementStartTime = null
        consecutiveMovementCount = 0
    }

    /**
     * Update the last known location.
     *
     * Called by LocationTrackingService when new location is available.
     */
    override fun updateLocation(latitude: Double, longitude: Double) {
        lastKnownLocation = LatLng(latitude, longitude)

        // Update active trip's location count and refresh in-memory state
        _activeTrip.value?.let { trip ->
            managerScope.launch {
                tripRepository.incrementLocationCount(trip.id, 0.0)
                // Refresh _activeTrip with updated data from repository
                refreshActiveTrip(trip.id)
            }
        }
    }

    /**
     * Update active trip with distance traveled.
     *
     * Called by LocationTrackingService when distance is calculated.
     */
    override fun addDistance(tripId: String, distanceMeters: Double) {
        managerScope.launch {
            tripRepository.incrementLocationCount(tripId, distanceMeters)
            // Refresh _activeTrip with updated data from repository
            refreshActiveTrip(tripId)
        }
    }

    /**
     * Refresh the active trip state from repository.
     * Ensures UI observes the latest distance/locationCount values.
     */
    private suspend fun refreshActiveTrip(tripId: String) {
        val currentTrip = _activeTrip.value
        if (currentTrip?.id == tripId) {
            tripRepository.getTripById(tripId)?.let { updatedTrip ->
                _activeTrip.value = updatedTrip
            }
        }
    }

    /**
     * Clean up resources.
     */
    fun destroy() {
        stopMonitoring()
        managerScope.cancel()
    }
}

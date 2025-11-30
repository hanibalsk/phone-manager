package three.two.bit.phonemanager.trip

import kotlinx.coroutines.flow.StateFlow
import three.two.bit.phonemanager.domain.model.Trip
import three.two.bit.phonemanager.domain.model.TripState

/**
 * Story E8.4: TripManager - Interface for managing trip detection and state
 *
 * Implements the trip state machine and provides methods for:
 * - Automatic trip detection based on transportation mode changes
 * - Manual trip start/end controls
 * - Trip query operations
 *
 * AC E8.4.1: Complete TripManager interface
 */
interface TripManager {

    /**
     * Current state of the trip state machine.
     *
     * States:
     * - IDLE: No active trip, monitoring for movement
     * - ACTIVE: Trip in progress, tracking locations
     * - PENDING_END: Movement stopped, waiting for grace period
     * - COMPLETED: Trip finalized (transitions to IDLE)
     */
    val currentTripState: StateFlow<TripState>

    /**
     * Currently active trip, or null if no trip is in progress.
     */
    val activeTrip: StateFlow<Trip?>

    /**
     * Whether trip monitoring is currently enabled.
     */
    val isMonitoring: StateFlow<Boolean>

    /**
     * Start monitoring for trips.
     *
     * Subscribes to transportation mode changes and automatically
     * detects trip start/end conditions.
     */
    suspend fun startMonitoring()

    /**
     * Stop monitoring for trips.
     *
     * If a trip is active, it will be finalized before stopping.
     */
    fun stopMonitoring()

    /**
     * Manually start a new trip.
     *
     * Creates a trip with MANUAL start trigger regardless of
     * current transportation mode.
     *
     * @return The newly created trip
     * @throws IllegalStateException if a trip is already active
     */
    suspend fun forceStartTrip(): Trip

    /**
     * Manually end the current trip.
     *
     * Finalizes the trip with MANUAL end trigger.
     *
     * @return The finalized trip, or null if no trip was active
     */
    suspend fun forceEndTrip(): Trip?

    /**
     * Get a trip by its ID.
     *
     * @param id Trip ID
     * @return Trip if found, null otherwise
     */
    suspend fun getTripById(id: String): Trip?

    /**
     * Get trips within a time range.
     *
     * @param startTime Range start timestamp (milliseconds)
     * @param endTime Range end timestamp (milliseconds)
     * @return List of trips in the time range
     */
    suspend fun getTripsInRange(startTime: Long, endTime: Long): List<Trip>

    /**
     * Get recent trips ordered by start time descending.
     *
     * @param limit Maximum number of trips to return
     * @return List of recent trips
     */
    suspend fun getRecentTrips(limit: Int = 10): List<Trip>

    /**
     * Story E8.7: Update the last known location for the active trip.
     *
     * Called by LocationTrackingService when a new location is captured.
     *
     * @param latitude Location latitude
     * @param longitude Location longitude
     */
    fun updateLocation(latitude: Double, longitude: Double)

    /**
     * Story E8.7: Add distance traveled to the active trip.
     *
     * Called by LocationTrackingService when distance is calculated.
     *
     * @param tripId Trip ID to update
     * @param distanceMeters Distance traveled in meters
     */
    fun addDistance(tripId: String, distanceMeters: Double)
}

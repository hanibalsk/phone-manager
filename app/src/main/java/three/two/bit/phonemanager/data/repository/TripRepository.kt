package three.two.bit.phonemanager.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant
import three.two.bit.phonemanager.domain.model.TodayTripStats
import three.two.bit.phonemanager.domain.model.Trip
import three.two.bit.phonemanager.movement.TransportationMode
import three.two.bit.phonemanager.network.models.PathCorrectionResponse
import three.two.bit.phonemanager.network.models.TripPathResponse
import three.two.bit.phonemanager.network.models.TripsListResponse

/**
 * Story E8.3: TripRepository - Repository interface for trip data
 *
 * Provides abstraction over trip data sources with domain model mapping.
 * AC E8.3.3: Complete TripRepository interface
 */
interface TripRepository {

    /**
     * Insert a new trip.
     *
     * @param trip Trip domain model to insert
     * @return Result containing the trip ID on success
     */
    suspend fun insert(trip: Trip): Result<String>

    /**
     * Update an existing trip.
     *
     * @param trip Trip domain model with updated values
     * @return Result indicating success or failure
     */
    suspend fun update(trip: Trip): Result<Unit>

    /**
     * Get a trip by ID.
     *
     * @param id Trip ID
     * @return Trip if found, null otherwise
     */
    suspend fun getTripById(id: String): Trip?

    /**
     * Observe a trip by ID in real-time.
     *
     * @param id Trip ID
     * @return Flow emitting Trip updates
     */
    fun observeTripById(id: String): Flow<Trip?>

    /**
     * Get the currently active trip.
     *
     * @return Active trip if one exists, null otherwise
     */
    suspend fun getActiveTrip(): Trip?

    /**
     * Observe the currently active trip in real-time.
     *
     * @return Flow emitting the active trip or null
     */
    fun observeActiveTrip(): Flow<Trip?>

    /**
     * Observe recent trips in real-time.
     *
     * @param limit Maximum number of trips to return
     * @return Flow emitting list of recent trips
     */
    fun observeRecentTrips(limit: Int = 20): Flow<List<Trip>>

    /**
     * Observe completed trips in real-time.
     *
     * @param limit Maximum number of trips to return
     * @return Flow emitting list of completed trips
     */
    fun observeCompletedTrips(limit: Int = 20): Flow<List<Trip>>

    /**
     * Get trips between two time points.
     *
     * @param start Start time (inclusive)
     * @param end End time (inclusive)
     * @return List of trips in the time range
     */
    suspend fun getTripsBetween(start: Instant, end: Instant): List<Trip>

    /**
     * Observe trips by transportation mode.
     *
     * @param mode Transportation mode to filter by
     * @return Flow emitting list of trips with the specified mode
     */
    fun observeTripsByMode(mode: TransportationMode): Flow<List<Trip>>

    /**
     * Increment the location count for a trip.
     *
     * @param tripId Trip ID
     * @param distance Distance to add to total
     */
    suspend fun incrementLocationCount(tripId: String, distance: Double)

    /**
     * Observe today's trip statistics in real-time.
     *
     * @return Flow emitting TodayTripStats
     */
    fun observeTodayStats(): Flow<TodayTripStats>

    /**
     * Observe total distance traveled since a given time.
     *
     * @param since Start time
     * @return Flow emitting total distance in meters
     */
    fun observeTotalDistanceSince(since: Instant): Flow<Double>

    /**
     * Observe trip count since a given time.
     *
     * @param since Start time
     * @return Flow emitting trip count
     */
    fun observeTripCountSince(since: Instant): Flow<Int>

    /**
     * Get unsynced trips for backend synchronization.
     *
     * @param limit Maximum number of trips to return
     * @return List of unsynced trips
     */
    suspend fun getUnsyncedTrips(limit: Int = 50): List<Trip>

    /**
     * Mark a trip as synced with the backend.
     *
     * @param tripId Trip ID
     * @param syncedAt Sync timestamp
     * @param serverId Optional server-assigned ID
     */
    suspend fun markAsSynced(tripId: String, syncedAt: Instant, serverId: String? = null)

    /**
     * Delete trips older than a given time.
     *
     * @param before Cutoff time
     * @return Number of trips deleted
     */
    suspend fun deleteOldTrips(before: Instant): Int

    /**
     * Delete a trip by ID.
     *
     * @param tripId Trip ID to delete
     */
    suspend fun deleteTrip(tripId: String)

    // API Compatibility: Remote sync methods

    /**
     * Sync a trip to the backend.
     *
     * @param trip Trip to sync
     * @return Result containing the server-assigned trip ID
     */
    suspend fun syncTrip(trip: Trip): Result<String>

    /**
     * Fetch remote trips for a device.
     *
     * @param deviceId Device identifier
     * @param status Optional status filter
     * @param from Optional start timestamp (ISO 8601)
     * @param to Optional end timestamp (ISO 8601)
     * @param limit Max results
     * @return Result containing trips list response
     */
    suspend fun fetchRemoteTrips(
        deviceId: String,
        status: String? = null,
        from: String? = null,
        to: String? = null,
        limit: Int? = null,
    ): Result<TripsListResponse>

    /**
     * Get the corrected path for a trip.
     *
     * @param tripId Trip ID (server ID)
     * @return Result containing trip path response
     */
    suspend fun getTripPath(tripId: String): Result<TripPathResponse>

    /**
     * Trigger path correction for a trip.
     *
     * Note: Rate limited to 1 per hour per trip.
     *
     * @param tripId Trip ID (server ID)
     * @return Result containing path correction response
     */
    suspend fun triggerPathCorrection(tripId: String): Result<PathCorrectionResponse>
}

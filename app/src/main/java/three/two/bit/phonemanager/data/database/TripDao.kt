package three.two.bit.phonemanager.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import three.two.bit.phonemanager.data.model.TripEntity

/**
 * Story E8.2: TripDao - Data Access Object for trips table
 *
 * Provides all CRUD operations, queries, statistics, and sync methods for trip data.
 *
 * @see TripEntity
 */
@Dao
interface TripDao {

    // region Insert/Update Methods (AC E8.2.1)

    /**
     * Insert a new trip or replace if exists.
     * @return The row ID of the inserted trip
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(trip: TripEntity): Long

    /**
     * Update an existing trip.
     */
    @Update
    suspend fun update(trip: TripEntity)

    // endregion

    // region Query Methods (AC E8.2.2)

    /**
     * Get a trip by its ID.
     * @param tripId The unique trip identifier
     * @return The trip or null if not found
     */
    @Query("SELECT * FROM trips WHERE id = :tripId")
    suspend fun getTripById(tripId: String): TripEntity?

    /**
     * Observe a trip by its ID with live updates.
     * @param tripId The unique trip identifier
     * @return Flow emitting the trip or null
     */
    @Query("SELECT * FROM trips WHERE id = :tripId")
    fun observeTripById(tripId: String): Flow<TripEntity?>

    /**
     * Get the currently active trip (state = ACTIVE).
     * @return The active trip or null if none active
     */
    @Query("SELECT * FROM trips WHERE state = 'ACTIVE' LIMIT 1")
    suspend fun getActiveTrip(): TripEntity?

    /**
     * Observe the currently active trip with live updates.
     * @return Flow emitting the active trip or null
     */
    @Query("SELECT * FROM trips WHERE state = 'ACTIVE' LIMIT 1")
    fun observeActiveTrip(): Flow<TripEntity?>

    /**
     * Observe recent trips ordered by start time descending.
     * @param limit Maximum number of trips to return
     * @return Flow emitting list of recent trips
     */
    @Query("SELECT * FROM trips ORDER BY startTime DESC LIMIT :limit")
    fun observeRecentTrips(limit: Int = 20): Flow<List<TripEntity>>

    /**
     * Get trips within a time range.
     * @param startTime Range start timestamp (inclusive)
     * @param endTime Range end timestamp (inclusive)
     * @return List of trips in the range
     */
    @Query("SELECT * FROM trips WHERE startTime BETWEEN :startTime AND :endTime ORDER BY startTime DESC")
    suspend fun getTripsBetween(startTime: Long, endTime: Long): List<TripEntity>

    /**
     * Observe trips filtered by dominant transportation mode.
     * @param mode Transportation mode to filter by (e.g., "WALKING", "DRIVING")
     * @return Flow emitting list of trips with that mode
     */
    @Query("SELECT * FROM trips WHERE dominantMode = :mode ORDER BY startTime DESC")
    fun observeTripsByMode(mode: String): Flow<List<TripEntity>>

    /**
     * Get all completed trips ordered by start time.
     * @param limit Maximum number of trips to return
     * @return Flow emitting list of completed trips
     */
    @Query("SELECT * FROM trips WHERE state = 'COMPLETED' ORDER BY startTime DESC LIMIT :limit")
    fun observeCompletedTrips(limit: Int = 20): Flow<List<TripEntity>>

    /**
     * Get trips by state.
     * @param state Trip state to filter by
     * @return List of trips in that state
     */
    @Query("SELECT * FROM trips WHERE state = :state ORDER BY startTime DESC")
    suspend fun getTripsByState(state: String): List<TripEntity>

    // endregion

    // region Statistics Methods (AC E8.2.3)

    /**
     * Increment location count and add distance for a trip.
     * Used when a new location is recorded during an active trip.
     * @param tripId The trip to update
     * @param distance Distance to add in meters
     * @param timestamp Current timestamp for updatedAt
     */
    @Query(
        """
        UPDATE trips
        SET locationCount = locationCount + 1,
            totalDistanceMeters = totalDistanceMeters + :distance,
            updatedAt = :timestamp
        WHERE id = :tripId
        """,
    )
    suspend fun incrementLocationCount(tripId: String, distance: Double, timestamp: Long)

    /**
     * Observe total distance traveled since a timestamp.
     * @param since Timestamp to count from
     * @return Flow emitting total distance in meters
     */
    @Query("SELECT SUM(totalDistanceMeters) FROM trips WHERE startTime >= :since AND state = 'COMPLETED'")
    fun observeTotalDistanceSince(since: Long): Flow<Double?>

    /**
     * Observe count of completed trips since a timestamp.
     * @param since Timestamp to count from
     * @return Flow emitting trip count
     */
    @Query("SELECT COUNT(*) FROM trips WHERE startTime >= :since AND state = 'COMPLETED'")
    fun observeTripCountSince(since: Long): Flow<Int>

    /**
     * Get average trip duration in milliseconds since a timestamp.
     * @param since Timestamp to calculate from
     * @return Average duration or null if no trips
     */
    @Query(
        """
        SELECT AVG(endTime - startTime)
        FROM trips
        WHERE startTime >= :since
          AND state = 'COMPLETED'
          AND endTime IS NOT NULL
        """,
    )
    suspend fun getAverageTripDurationSince(since: Long): Long?

    /**
     * Get total location count across all trips since a timestamp.
     * @param since Timestamp to count from
     * @return Total location count
     */
    @Query("SELECT COALESCE(SUM(locationCount), 0) FROM trips WHERE startTime >= :since")
    suspend fun getTotalLocationCountSince(since: Long): Int

    // endregion

    // region Sync Methods (AC E8.2.4)

    /**
     * Get unsynced trips for backend sync.
     * @param limit Maximum number of trips to return
     * @return List of unsynced trips
     */
    @Query("SELECT * FROM trips WHERE isSynced = 0 AND state = 'COMPLETED' ORDER BY startTime ASC LIMIT :limit")
    suspend fun getUnsyncedTrips(limit: Int = 50): List<TripEntity>

    /**
     * Mark a trip as synced with the backend.
     * @param tripId Trip to mark as synced
     * @param syncedAt Sync timestamp
     * @param serverId Optional server-assigned ID
     */
    @Query("UPDATE trips SET isSynced = 1, syncedAt = :syncedAt, serverId = :serverId WHERE id = :tripId")
    suspend fun markAsSynced(tripId: String, syncedAt: Long, serverId: String? = null)

    /**
     * Delete old trips before a given timestamp.
     * Only deletes completed and synced trips.
     * @param beforeTime Cutoff timestamp
     * @return Number of trips deleted
     */
    @Query("DELETE FROM trips WHERE startTime < :beforeTime AND state = 'COMPLETED' AND isSynced = 1")
    suspend fun deleteOldTrips(beforeTime: Long): Int

    /**
     * Observe count of unsynced completed trips.
     * @return Flow emitting count of unsynced trips
     */
    @Query("SELECT COUNT(*) FROM trips WHERE isSynced = 0 AND state = 'COMPLETED'")
    fun observeUnsyncedCount(): Flow<Int>

    /**
     * Delete a specific trip by ID.
     * @param tripId Trip to delete
     * @return Number of rows deleted
     */
    @Query("DELETE FROM trips WHERE id = :tripId")
    suspend fun deleteTrip(tripId: String): Int

    // endregion
}

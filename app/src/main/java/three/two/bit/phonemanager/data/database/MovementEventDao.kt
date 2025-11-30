package three.two.bit.phonemanager.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import three.two.bit.phonemanager.data.model.MovementEventEntity

/**
 * Story E8.2: MovementEventDao - Data Access Object for movement_events table
 *
 * Provides all CRUD operations, queries, statistics, and sync methods for movement event data.
 *
 * @see MovementEventEntity
 */
@Dao
interface MovementEventDao {

    // region Insert Methods (AC E8.2.5)

    /**
     * Insert a single movement event.
     * @param event The event to insert
     * @return The row ID of the inserted event
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: MovementEventEntity): Long

    /**
     * Insert multiple movement events in a batch.
     * @param events List of events to insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: List<MovementEventEntity>)

    // endregion

    // region Query Methods (AC E8.2.6)

    /**
     * Get a movement event by its ID.
     * @param eventId The unique event identifier
     * @return The event or null if not found
     */
    @Query("SELECT * FROM movement_events WHERE id = :eventId")
    suspend fun getEventById(eventId: Long): MovementEventEntity?

    /**
     * Observe recent movement events ordered by timestamp descending.
     * @param limit Maximum number of events to return
     * @return Flow emitting list of recent events
     */
    @Query("SELECT * FROM movement_events ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecentEvents(limit: Int = 50): Flow<List<MovementEventEntity>>

    /**
     * Observe movement events for a specific trip.
     * @param tripId Trip identifier to filter by
     * @return Flow emitting list of events for the trip
     */
    @Query("SELECT * FROM movement_events WHERE tripId = :tripId ORDER BY timestamp ASC")
    fun observeEventsByTrip(tripId: String): Flow<List<MovementEventEntity>>

    /**
     * Observe the latest (most recent) movement event.
     * @return Flow emitting the latest event or null
     */
    @Query("SELECT * FROM movement_events ORDER BY timestamp DESC LIMIT 1")
    fun observeLatestEvent(): Flow<MovementEventEntity?>

    /**
     * Get the latest movement event synchronously.
     * @return The latest event or null if none exist
     */
    @Query("SELECT * FROM movement_events ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestEvent(): MovementEventEntity?

    /**
     * Get movement events within a time range.
     * @param startTime Range start timestamp (inclusive)
     * @param endTime Range end timestamp (inclusive)
     * @return List of events in the range
     */
    @Query("SELECT * FROM movement_events WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp ASC")
    suspend fun getEventsBetween(startTime: Long, endTime: Long): List<MovementEventEntity>

    /**
     * Get events for a specific trip synchronously.
     * @param tripId Trip identifier
     * @return List of events for the trip
     */
    @Query("SELECT * FROM movement_events WHERE tripId = :tripId ORDER BY timestamp ASC")
    suspend fun getEventsByTrip(tripId: String): List<MovementEventEntity>

    /**
     * Get events filtered by new mode (transportation mode transitioned to).
     * @param newMode Transportation mode to filter by
     * @param limit Maximum number of events
     * @return List of events with that mode
     */
    @Query("SELECT * FROM movement_events WHERE newMode = :newMode ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getEventsByNewMode(newMode: String, limit: Int = 100): List<MovementEventEntity>

    /**
     * Get events filtered by detection source.
     * @param source Detection source to filter by
     * @param limit Maximum number of events
     * @return List of events from that source
     */
    @Query("SELECT * FROM movement_events WHERE detectionSource = :source ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getEventsByDetectionSource(source: String, limit: Int = 100): List<MovementEventEntity>

    // endregion

    // region Statistics Methods (AC E8.2.7)

    /**
     * Observe count of events since a timestamp.
     * @param since Timestamp to count from
     * @return Flow emitting event count
     */
    @Query("SELECT COUNT(*) FROM movement_events WHERE timestamp >= :since")
    fun observeEventCountSince(since: Long): Flow<Int>

    /**
     * Get count of events for a specific trip.
     * @param tripId Trip identifier
     * @return Number of events for the trip
     */
    @Query("SELECT COUNT(*) FROM movement_events WHERE tripId = :tripId")
    suspend fun getEventCountForTrip(tripId: String): Int

    /**
     * Get average confidence for events since a timestamp.
     * @param since Timestamp to calculate from
     * @return Average confidence (0.0-1.0) or null if no events
     */
    @Query("SELECT AVG(confidence) FROM movement_events WHERE timestamp >= :since")
    suspend fun getAverageConfidenceSince(since: Long): Float?

    /**
     * Get average detection latency in milliseconds since a timestamp.
     * @param since Timestamp to calculate from
     * @return Average latency or null if no events
     */
    @Query("SELECT AVG(detectionLatencyMs) FROM movement_events WHERE timestamp >= :since")
    suspend fun getAverageDetectionLatencySince(since: Long): Long?

    /**
     * Get count of mode transitions grouped by mode pair.
     * @param since Timestamp to count from
     * @return Map of "previousMode -> newMode" transitions with counts
     */
    @Query(
        """
        SELECT previousMode || ' -> ' || newMode AS transition, COUNT(*) AS count
        FROM movement_events
        WHERE timestamp >= :since
        GROUP BY previousMode, newMode
        ORDER BY count DESC
        """,
    )
    suspend fun getModeTransitionCounts(since: Long): List<ModeTransitionCount>

    // endregion

    // region Sync Methods (AC E8.2.7)

    /**
     * Get unsynced events for backend sync.
     * @param limit Maximum number of events to return
     * @return List of unsynced events
     */
    @Query("SELECT * FROM movement_events WHERE isSynced = 0 ORDER BY timestamp ASC LIMIT :limit")
    suspend fun getUnsyncedEvents(limit: Int = 100): List<MovementEventEntity>

    /**
     * Mark multiple events as synced.
     * @param ids Event IDs to mark as synced
     * @param syncedAt Sync timestamp
     */
    @Query("UPDATE movement_events SET isSynced = 1, syncedAt = :syncedAt WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<Long>, syncedAt: Long)

    /**
     * Observe count of unsynced events.
     * @return Flow emitting count of unsynced events
     */
    @Query("SELECT COUNT(*) FROM movement_events WHERE isSynced = 0")
    fun observeUnsyncedCount(): Flow<Int>

    /**
     * Delete old events before a given timestamp.
     * Only deletes synced events to prevent data loss.
     * @param beforeTime Cutoff timestamp
     * @return Number of events deleted
     */
    @Query("DELETE FROM movement_events WHERE timestamp < :beforeTime AND isSynced = 1")
    suspend fun deleteOldEvents(beforeTime: Long): Int

    /**
     * Delete all events for a specific trip.
     * @param tripId Trip identifier
     * @return Number of events deleted
     */
    @Query("DELETE FROM movement_events WHERE tripId = :tripId")
    suspend fun deleteEventsByTrip(tripId: String): Int

    /**
     * Delete a specific event by ID.
     * @param eventId Event to delete
     * @return Number of rows deleted
     */
    @Query("DELETE FROM movement_events WHERE id = :eventId")
    suspend fun deleteEvent(eventId: Long): Int

    // endregion
}

/**
 * Data class for mode transition count results.
 */
data class ModeTransitionCount(val transition: String, val count: Int)

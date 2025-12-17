package three.two.bit.phonemanager.data.repository

import kotlinx.coroutines.flow.Flow
import three.two.bit.phonemanager.domain.model.DetectionSource
import three.two.bit.phonemanager.domain.model.DeviceState
import three.two.bit.phonemanager.domain.model.EventLocation
import three.two.bit.phonemanager.domain.model.MovementContext
import three.two.bit.phonemanager.domain.model.MovementEvent
import three.two.bit.phonemanager.domain.model.SensorTelemetry
import three.two.bit.phonemanager.movement.TransportationMode
import three.two.bit.phonemanager.network.models.BatchMovementEventsResponse
import three.two.bit.phonemanager.network.models.MovementEventsListResponse
import kotlin.time.Instant

/**
 * Story E8.3: MovementEventRepository - Repository interface for movement event data
 *
 * Provides abstraction over movement event data sources with domain model mapping.
 * AC E8.3.5: Complete MovementEventRepository interface
 */
interface MovementEventRepository {

    /**
     * Record a movement detection event with all telemetry data.
     *
     * @param tripId Associated trip ID (null if no active trip)
     * @param previousMode Transportation mode before the change
     * @param newMode Transportation mode after the change
     * @param detectionSource Source of the detection
     * @param confidence Detection confidence (0.0-1.0)
     * @param detectionLatencyMs Time from sensor reading to detection
     * @param location Location snapshot at time of event
     * @param deviceState Device state at time of event
     * @param sensorTelemetry Sensor readings at time of event
     * @param movementContext Movement context information
     * @return Result containing the event ID on success
     */
    suspend fun recordEvent(
        tripId: String?,
        previousMode: TransportationMode,
        newMode: TransportationMode,
        detectionSource: DetectionSource,
        confidence: Float,
        detectionLatencyMs: Long,
        location: EventLocation? = null,
        deviceState: DeviceState? = null,
        sensorTelemetry: SensorTelemetry? = null,
        movementContext: MovementContext? = null,
    ): Result<Long>

    /**
     * Insert a movement event.
     *
     * @param event MovementEvent domain model
     * @return Result containing the event ID on success
     */
    suspend fun insert(event: MovementEvent): Result<Long>

    /**
     * Insert multiple movement events.
     *
     * @param events List of MovementEvent domain models
     * @return Result indicating success or failure
     */
    suspend fun insertAll(events: List<MovementEvent>): Result<Unit>

    /**
     * Get an event by ID.
     *
     * @param eventId Event ID
     * @return MovementEvent if found, null otherwise
     */
    suspend fun getEventById(eventId: Long): MovementEvent?

    /**
     * Observe recent events in real-time.
     *
     * @param limit Maximum number of events to return
     * @return Flow emitting list of recent events
     */
    fun observeRecentEvents(limit: Int = 50): Flow<List<MovementEvent>>

    /**
     * Observe events for a specific trip in real-time.
     *
     * @param tripId Trip ID
     * @return Flow emitting list of events for the trip
     */
    fun observeEventsByTrip(tripId: String): Flow<List<MovementEvent>>

    /**
     * Observe the latest event in real-time.
     *
     * @return Flow emitting the latest event or null
     */
    fun observeLatestEvent(): Flow<MovementEvent?>

    /**
     * Get the latest event.
     *
     * @return Latest MovementEvent or null
     */
    suspend fun getLatestEvent(): MovementEvent?

    /**
     * Get events between two time points.
     *
     * @param start Start time (inclusive)
     * @param end End time (inclusive)
     * @return List of events in the time range
     */
    suspend fun getEventsBetween(start: Instant, end: Instant): List<MovementEvent>

    /**
     * Get events for a specific trip.
     *
     * @param tripId Trip ID
     * @return List of events for the trip
     */
    suspend fun getEventsByTrip(tripId: String): List<MovementEvent>

    /**
     * Observe event count since a given time.
     *
     * @param since Start time
     * @return Flow emitting event count
     */
    fun observeEventCountSince(since: Instant): Flow<Int>

    /**
     * Get event count for a specific trip.
     *
     * @param tripId Trip ID
     * @return Number of events for the trip
     */
    suspend fun getEventCountForTrip(tripId: String): Int

    /**
     * Observe unsynced event count in real-time.
     *
     * @return Flow emitting unsynced count
     */
    fun observeUnsyncedCount(): Flow<Int>

    /**
     * Get unsynced events for backend synchronization.
     *
     * @param limit Maximum number of events to return
     * @return List of unsynced events
     */
    suspend fun getUnsyncedEvents(limit: Int = 100): List<MovementEvent>

    /**
     * Mark events as synced with the backend.
     *
     * @param eventIds List of event IDs to mark
     * @param syncedAt Sync timestamp
     */
    suspend fun markAsSynced(eventIds: List<Long>, syncedAt: Instant)

    /**
     * Delete events older than a given time.
     *
     * @param before Cutoff time
     * @return Number of events deleted
     */
    suspend fun deleteOldEvents(before: Instant): Int

    /**
     * Delete all events for a specific trip.
     *
     * @param tripId Trip ID
     * @return Number of events deleted
     */
    suspend fun deleteEventsByTrip(tripId: String): Int

    // API Compatibility: Remote sync methods

    /**
     * Sync events to the backend in batch.
     *
     * @param events List of events to sync
     * @param deviceId Device identifier
     * @return Result containing batch upload response
     */
    suspend fun syncEvents(events: List<MovementEvent>, deviceId: String): Result<BatchMovementEventsResponse>

    /**
     * Fetch remote events for a device.
     *
     * @param deviceId Device identifier
     * @param from Optional start timestamp (ISO 8601)
     * @param to Optional end timestamp (ISO 8601)
     * @param limit Max results
     * @param offset Pagination offset
     * @return Result containing events list response
     */
    suspend fun fetchRemoteEvents(
        deviceId: String,
        from: String? = null,
        to: String? = null,
        limit: Int? = null,
        offset: Int? = null,
    ): Result<MovementEventsListResponse>
}

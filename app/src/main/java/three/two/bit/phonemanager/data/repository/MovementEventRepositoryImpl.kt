package three.two.bit.phonemanager.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import three.two.bit.phonemanager.data.database.MovementEventDao
import three.two.bit.phonemanager.data.model.MovementEventEntity
import three.two.bit.phonemanager.data.model.toDomain
import three.two.bit.phonemanager.data.model.toEntity
import three.two.bit.phonemanager.domain.model.DetectionSource
import three.two.bit.phonemanager.domain.model.DeviceState
import three.two.bit.phonemanager.domain.model.EventLocation
import three.two.bit.phonemanager.domain.model.MovementContext
import three.two.bit.phonemanager.domain.model.MovementEvent
import three.two.bit.phonemanager.domain.model.SensorTelemetry
import three.two.bit.phonemanager.movement.TransportationMode
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Story E8.3: MovementEventRepositoryImpl - Implementation of MovementEventRepository
 *
 * Provides movement event data access with domain model mapping.
 * AC E8.3.6: Complete MovementEventRepositoryImpl with entity mapping
 */
@Singleton
class MovementEventRepositoryImpl @Inject constructor(
    private val movementEventDao: MovementEventDao,
) : MovementEventRepository {

    override suspend fun recordEvent(
        tripId: String?,
        previousMode: TransportationMode,
        newMode: TransportationMode,
        detectionSource: DetectionSource,
        confidence: Float,
        detectionLatencyMs: Long,
        location: EventLocation?,
        deviceState: DeviceState?,
        sensorTelemetry: SensorTelemetry?,
        movementContext: MovementContext?,
    ): Result<Long> = runCatching {
        Timber.d("Recording movement event: $previousMode -> $newMode (source: $detectionSource)")

        val entity = MovementEventEntity(
            timestamp = Clock.System.now().toEpochMilliseconds(),
            tripId = tripId,
            previousMode = previousMode.name,
            newMode = newMode.name,
            detectionSource = detectionSource.name,
            confidence = confidence,
            detectionLatencyMs = detectionLatencyMs,
            latitude = location?.latitude,
            longitude = location?.longitude,
            accuracy = location?.accuracy,
            speed = location?.speed,
            batteryLevel = deviceState?.batteryLevel,
            batteryCharging = deviceState?.batteryCharging,
            networkType = deviceState?.networkType?.name,
            networkStrength = deviceState?.networkStrength,
            accelerometerMagnitude = sensorTelemetry?.accelerometerMagnitude,
            accelerometerVariance = sensorTelemetry?.accelerometerVariance,
            accelerometerPeakFrequency = sensorTelemetry?.accelerometerPeakFrequency,
            gyroscopeMagnitude = sensorTelemetry?.gyroscopeMagnitude,
            stepCount = sensorTelemetry?.stepCount,
            significantMotion = sensorTelemetry?.significantMotion,
            activityType = sensorTelemetry?.activityType,
            activityConfidence = sensorTelemetry?.activityConfidence,
            distanceFromLastLocation = movementContext?.distanceFromLastLocation,
            timeSinceLastLocation = movementContext?.timeSinceLastLocation,
        )

        movementEventDao.insert(entity)
    }

    override suspend fun insert(event: MovementEvent): Result<Long> = runCatching {
        Timber.d("Inserting movement event: ${event.id}")
        movementEventDao.insert(event.toEntity())
    }

    override suspend fun insertAll(events: List<MovementEvent>): Result<Unit> = runCatching {
        Timber.d("Inserting ${events.size} movement events")
        movementEventDao.insertAll(events.map { it.toEntity() })
    }

    override suspend fun getEventById(eventId: Long): MovementEvent? {
        return movementEventDao.getEventById(eventId)?.toDomain()
    }

    override fun observeRecentEvents(limit: Int): Flow<List<MovementEvent>> {
        return movementEventDao.observeRecentEvents(limit).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun observeEventsByTrip(tripId: String): Flow<List<MovementEvent>> {
        return movementEventDao.observeEventsByTrip(tripId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun observeLatestEvent(): Flow<MovementEvent?> {
        return movementEventDao.observeLatestEvent().map { it?.toDomain() }
    }

    override suspend fun getLatestEvent(): MovementEvent? {
        return movementEventDao.getLatestEvent()?.toDomain()
    }

    override suspend fun getEventsBetween(start: Instant, end: Instant): List<MovementEvent> {
        return movementEventDao.getEventsBetween(
            startTime = start.toEpochMilliseconds(),
            endTime = end.toEpochMilliseconds(),
        ).map { it.toDomain() }
    }

    override suspend fun getEventsByTrip(tripId: String): List<MovementEvent> {
        return movementEventDao.getEventsByTrip(tripId).map { it.toDomain() }
    }

    override fun observeEventCountSince(since: Instant): Flow<Int> {
        return movementEventDao.observeEventCountSince(since.toEpochMilliseconds())
    }

    override suspend fun getEventCountForTrip(tripId: String): Int {
        return movementEventDao.getEventCountForTrip(tripId)
    }

    override fun observeUnsyncedCount(): Flow<Int> {
        return movementEventDao.observeUnsyncedCount()
    }

    override suspend fun getUnsyncedEvents(limit: Int): List<MovementEvent> {
        return movementEventDao.getUnsyncedEvents(limit).map { it.toDomain() }
    }

    override suspend fun markAsSynced(eventIds: List<Long>, syncedAt: Instant) {
        movementEventDao.markAsSynced(eventIds, syncedAt.toEpochMilliseconds())
    }

    override suspend fun deleteOldEvents(before: Instant): Int {
        return movementEventDao.deleteOldEvents(before.toEpochMilliseconds())
    }

    override suspend fun deleteEventsByTrip(tripId: String): Int {
        return movementEventDao.deleteEventsByTrip(tripId)
    }
}

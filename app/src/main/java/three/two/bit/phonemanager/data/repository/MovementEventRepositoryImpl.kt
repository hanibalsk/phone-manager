package three.two.bit.phonemanager.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.toJavaInstant
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
import three.two.bit.phonemanager.network.MovementEventApiService
import three.two.bit.phonemanager.network.models.AccelerometerTelemetryDto
import three.two.bit.phonemanager.network.models.ActivityRecognitionDto
import three.two.bit.phonemanager.network.models.BatchMovementEventsRequest
import three.two.bit.phonemanager.network.models.BatchMovementEventsResponse
import three.two.bit.phonemanager.network.models.CreateMovementEventRequest
import three.two.bit.phonemanager.network.models.DetectionSourceDetails
import three.two.bit.phonemanager.network.models.GyroscopeTelemetryDto
import three.two.bit.phonemanager.network.models.MovementEventDeviceStateDto
import three.two.bit.phonemanager.network.models.MovementEventLocationDto
import three.two.bit.phonemanager.network.models.MovementEventTelemetryDto
import three.two.bit.phonemanager.network.models.MovementEventsListResponse
import timber.log.Timber
import java.time.format.DateTimeFormatter
import java.util.UUID
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
    private val movementEventApiService: MovementEventApiService,
) : MovementEventRepository {

    private val isoFormatter: DateTimeFormatter = DateTimeFormatter.ISO_INSTANT

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

    override suspend fun getEventById(eventId: Long): MovementEvent? =
        movementEventDao.getEventById(eventId)?.toDomain()

    override fun observeRecentEvents(limit: Int): Flow<List<MovementEvent>> =
        movementEventDao.observeRecentEvents(limit).map { entities ->
            entities.map { it.toDomain() }
        }

    override fun observeEventsByTrip(tripId: String): Flow<List<MovementEvent>> =
        movementEventDao.observeEventsByTrip(tripId).map { entities ->
            entities.map { it.toDomain() }
        }

    override fun observeLatestEvent(): Flow<MovementEvent?> = movementEventDao.observeLatestEvent().map {
        it?.toDomain()
    }

    override suspend fun getLatestEvent(): MovementEvent? = movementEventDao.getLatestEvent()?.toDomain()

    override suspend fun getEventsBetween(start: Instant, end: Instant): List<MovementEvent> =
        movementEventDao.getEventsBetween(
            startTime = start.toEpochMilliseconds(),
            endTime = end.toEpochMilliseconds(),
        ).map { it.toDomain() }

    override suspend fun getEventsByTrip(tripId: String): List<MovementEvent> =
        movementEventDao.getEventsByTrip(tripId).map {
            it.toDomain()
        }

    override fun observeEventCountSince(since: Instant): Flow<Int> =
        movementEventDao.observeEventCountSince(since.toEpochMilliseconds())

    override suspend fun getEventCountForTrip(tripId: String): Int = movementEventDao.getEventCountForTrip(tripId)

    override fun observeUnsyncedCount(): Flow<Int> = movementEventDao.observeUnsyncedCount()

    override suspend fun getUnsyncedEvents(limit: Int): List<MovementEvent> =
        movementEventDao.getUnsyncedEvents(limit).map {
            it.toDomain()
        }

    override suspend fun markAsSynced(eventIds: List<Long>, syncedAt: Instant) {
        movementEventDao.markAsSynced(eventIds, syncedAt.toEpochMilliseconds())
    }

    override suspend fun deleteOldEvents(before: Instant): Int =
        movementEventDao.deleteOldEvents(before.toEpochMilliseconds())

    override suspend fun deleteEventsByTrip(tripId: String): Int = movementEventDao.deleteEventsByTrip(tripId)

    // API Compatibility: Remote sync implementations

    override suspend fun syncEvents(
        events: List<MovementEvent>,
        deviceId: String,
    ): Result<BatchMovementEventsResponse> {
        Timber.d("Syncing ${events.size} movement events to backend")

        val requests = events.map { event -> event.toApiRequest(deviceId) }
        val batchRequest = BatchMovementEventsRequest(events = requests)

        return movementEventApiService.uploadEventsBatch(batchRequest).onSuccess { response ->
            // Mark successfully synced events
            if (response.processedCount > 0) {
                val eventIds = events.take(response.processedCount).map { it.id }
                markAsSynced(eventIds, Clock.System.now())
                Timber.i("Synced ${response.processedCount} movement events")
            }
        }
    }

    override suspend fun fetchRemoteEvents(
        deviceId: String,
        from: String?,
        to: String?,
        limit: Int?,
        offset: Int?,
    ): Result<MovementEventsListResponse> = movementEventApiService.getDeviceEvents(
        deviceId = deviceId,
        from = from,
        to = to,
        limit = limit,
        offset = offset,
    )

    /**
     * Convert domain MovementEvent to API request DTO.
     */
    private fun MovementEvent.toApiRequest(deviceId: String): CreateMovementEventRequest {
        return CreateMovementEventRequest(
            eventId = UUID.randomUUID().toString(),
            deviceId = deviceId,
            timestamp = formatTimestamp(timestamp),
            previousMode = previousMode.name,
            newMode = newMode.name,
            detectionSource = DetectionSourceDetails(
                primary = detectionSource.name,
                contributing = listOf(detectionSource.name),
            ),
            confidence = confidence,
            detectionLatencyMs = detectionLatencyMs.toInt(),
            location = location?.let {
                MovementEventLocationDto(
                    latitude = it.latitude,
                    longitude = it.longitude,
                    accuracy = it.accuracy,
                    speed = it.speed,
                )
            },
            deviceState = deviceState?.let {
                MovementEventDeviceStateDto(
                    batteryLevel = it.batteryLevel,
                    batteryCharging = it.batteryCharging,
                    networkType = it.networkType?.name,
                    networkStrength = it.networkStrength,
                )
            },
            telemetry = sensorTelemetry?.let {
                MovementEventTelemetryDto(
                    accelerometer = if (it.accelerometerMagnitude != null) {
                        AccelerometerTelemetryDto(
                            magnitude = it.accelerometerMagnitude,
                            variance = it.accelerometerVariance,
                            peakFrequency = it.accelerometerPeakFrequency,
                        )
                    } else {
                        null
                    },
                    gyroscope = it.gyroscopeMagnitude?.let { mag ->
                        GyroscopeTelemetryDto(magnitude = mag)
                    },
                    stepCount = it.stepCount,
                    significantMotion = it.significantMotion,
                    activityRecognition = if (it.activityType != null && it.activityConfidence != null) {
                        ActivityRecognitionDto(
                            type = it.activityType,
                            confidence = it.activityConfidence,
                        )
                    } else {
                        null
                    },
                )
            },
            tripId = tripId,
        )
    }

    private fun formatTimestamp(instant: Instant): String =
        isoFormatter.format(instant.toJavaInstant())
}

package three.two.bit.phonemanager.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant
import three.two.bit.phonemanager.domain.model.DetectionSource
import three.two.bit.phonemanager.domain.model.DeviceState
import three.two.bit.phonemanager.domain.model.EventLocation
import three.two.bit.phonemanager.domain.model.MovementContext
import three.two.bit.phonemanager.domain.model.MovementEvent
import three.two.bit.phonemanager.domain.model.NetworkType
import three.two.bit.phonemanager.domain.model.SensorTelemetry
import three.two.bit.phonemanager.movement.TransportationMode

/**
 * Story E8.1: MovementEventEntity - Database entity for storing movement detection events
 *
 * Represents a single transportation mode change event with full sensor telemetry,
 * device state, and location snapshot captured at the moment of detection.
 *
 * @see ANDROID_APP_SPEC.md Section 2.3
 */
@Entity(
    tableName = "movement_events",
    indices = [
        Index("timestamp"),
        Index("tripId"),
        Index("isSynced"),
    ],
    foreignKeys = [
        ForeignKey(
            entity = TripEntity::class,
            parentColumns = ["id"],
            childColumns = ["tripId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
)
data class MovementEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // Event Identification
    val timestamp: Long, // Unix timestamp (ms)
    val tripId: String?, // Associated trip ID

    // Mode Transition
    val previousMode: String, // TransportationMode.name
    val newMode: String, // TransportationMode.name
    val detectionSource: String, // DetectionSource.name
    val confidence: Float, // 0.0 - 1.0
    val detectionLatencyMs: Long, // Time from sensor to detection

    // Location Snapshot
    val latitude: Double? = null,
    val longitude: Double? = null,
    val accuracy: Float? = null,
    val speed: Float? = null,

    // Device State
    val batteryLevel: Int? = null, // 0-100
    val batteryCharging: Boolean? = null,
    val networkType: String? = null, // WIFI, MOBILE, NONE
    val networkStrength: Int? = null, // dBm

    // Sensor Telemetry
    val accelerometerMagnitude: Float? = null,
    val accelerometerVariance: Float? = null,
    val accelerometerPeakFrequency: Float? = null,
    val gyroscopeMagnitude: Float? = null,
    val stepCount: Int? = null,
    val significantMotion: Boolean? = null,
    val activityType: String? = null, // Raw activity type
    val activityConfidence: Int? = null, // 0-100

    // Distance Tracking
    val distanceFromLastLocation: Float? = null,
    val timeSinceLastLocation: Long? = null,

    // Sync Status
    val isSynced: Boolean = false,
    val syncedAt: Long? = null,
)

/**
 * Story E8.3: Convert MovementEventEntity to MovementEvent domain model (AC E8.3.7)
 */
fun MovementEventEntity.toDomain(): MovementEvent = MovementEvent(
    id = id,
    timestamp = Instant.fromEpochMilliseconds(timestamp),
    tripId = tripId,
    previousMode = TransportationMode.entries.find { it.name == previousMode }
        ?: TransportationMode.UNKNOWN,
    newMode = TransportationMode.entries.find { it.name == newMode }
        ?: TransportationMode.UNKNOWN,
    detectionSource = DetectionSource.entries.find { it.name == detectionSource }
        ?: DetectionSource.UNKNOWN,
    confidence = confidence,
    detectionLatencyMs = detectionLatencyMs,
    location = if (latitude != null && longitude != null) {
        EventLocation(
            latitude = latitude,
            longitude = longitude,
            accuracy = accuracy,
            speed = speed,
        )
    } else {
        null
    },
    deviceState = if (batteryLevel != null || batteryCharging != null || networkType != null) {
        DeviceState(
            batteryLevel = batteryLevel,
            batteryCharging = batteryCharging,
            networkType = networkType?.let { nt ->
                NetworkType.entries.find { it.name == nt }
            },
            networkStrength = networkStrength,
        )
    } else {
        null
    },
    sensorTelemetry = if (accelerometerMagnitude != null ||
        gyroscopeMagnitude != null ||
        stepCount != null ||
        significantMotion != null ||
        activityType != null
    ) {
        SensorTelemetry(
            accelerometerMagnitude = accelerometerMagnitude,
            accelerometerVariance = accelerometerVariance,
            accelerometerPeakFrequency = accelerometerPeakFrequency,
            gyroscopeMagnitude = gyroscopeMagnitude,
            stepCount = stepCount,
            significantMotion = significantMotion,
            activityType = activityType,
            activityConfidence = activityConfidence,
        )
    } else {
        null
    },
    movementContext = if (distanceFromLastLocation != null || timeSinceLastLocation != null) {
        MovementContext(
            distanceFromLastLocation = distanceFromLastLocation,
            timeSinceLastLocation = timeSinceLastLocation,
        )
    } else {
        null
    },
    isSynced = isSynced,
    syncedAt = syncedAt?.let { Instant.fromEpochMilliseconds(it) },
)

/**
 * Story E8.3: Convert MovementEvent domain model to MovementEventEntity (AC E8.3.7)
 */
fun MovementEvent.toEntity(): MovementEventEntity = MovementEventEntity(
    id = id,
    timestamp = timestamp.toEpochMilliseconds(),
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
    isSynced = isSynced,
    syncedAt = syncedAt?.toEpochMilliseconds(),
)

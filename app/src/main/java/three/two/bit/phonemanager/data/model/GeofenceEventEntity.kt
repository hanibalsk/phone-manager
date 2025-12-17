package three.two.bit.phonemanager.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import three.two.bit.phonemanager.domain.model.GeofenceEvent
import three.two.bit.phonemanager.domain.model.TransitionType

/**
 * Story E6.2: GeofenceEventEntity - Room entity for geofence events
 *
 * AC E6.2.4: GeofenceEvent entity with all required fields
 * AC E6.2.5: Event logging for history/debugging
 */
@Entity(tableName = "geofence_events")
data class GeofenceEventEntity(
    @PrimaryKey val id: String,
    val deviceId: String,
    val geofenceId: String,
    val eventType: String,
    val timestamp: Long,
    val latitude: Double,
    val longitude: Double,
    val webhookDelivered: Boolean = false,
    val webhookResponseCode: Int? = null,
)

/**
 * Convert Room entity to domain model
 */
fun GeofenceEventEntity.toDomain(): GeofenceEvent = GeofenceEvent(
    id = id,
    deviceId = deviceId,
    geofenceId = geofenceId,
    eventType = TransitionType.valueOf(eventType),
    timestamp = kotlin.time.Instant.fromEpochMilliseconds(timestamp),
    latitude = latitude,
    longitude = longitude,
    webhookDelivered = webhookDelivered,
    webhookResponseCode = webhookResponseCode,
)

/**
 * Convert domain model to Room entity
 */
fun GeofenceEvent.toEntity(): GeofenceEventEntity = GeofenceEventEntity(
    id = id,
    deviceId = deviceId,
    geofenceId = geofenceId,
    eventType = eventType.name,
    timestamp = timestamp.toEpochMilliseconds(),
    latitude = latitude,
    longitude = longitude,
    webhookDelivered = webhookDelivered,
    webhookResponseCode = webhookResponseCode,
)

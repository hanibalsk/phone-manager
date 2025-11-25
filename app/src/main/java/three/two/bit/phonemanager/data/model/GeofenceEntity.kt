package three.two.bit.phonemanager.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant
import three.two.bit.phonemanager.domain.model.Geofence
import three.two.bit.phonemanager.domain.model.TransitionType

/**
 * Story E6.1: GeofenceEntity - Room database entity for geofences
 *
 * AC E6.1.1: Complete entity with all required fields
 */
@Entity(tableName = "geofences")
data class GeofenceEntity(
    @PrimaryKey
    val id: String,
    val deviceId: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Int,
    val transitionTypes: String, // Comma-separated: "ENTER,EXIT,DWELL"
    val webhookId: String? = null,
    val active: Boolean,
    val createdAt: Long, // Epoch milliseconds
    val updatedAt: Long, // Epoch milliseconds
)

/**
 * Convert GeofenceEntity to domain model
 */
fun GeofenceEntity.toDomain(): Geofence = Geofence(
    id = id,
    deviceId = deviceId,
    name = name,
    latitude = latitude,
    longitude = longitude,
    radiusMeters = radiusMeters,
    transitionTypes =
    transitionTypes.split(",")
        .filter { it.isNotBlank() }
        .map { TransitionType.valueOf(it) }
        .toSet(),
    webhookId = webhookId,
    active = active,
    createdAt = Instant.fromEpochMilliseconds(createdAt),
    updatedAt = Instant.fromEpochMilliseconds(updatedAt),
)

/**
 * Convert domain model to GeofenceEntity
 */
fun Geofence.toEntity(): GeofenceEntity = GeofenceEntity(
    id = id,
    deviceId = deviceId,
    name = name,
    latitude = latitude,
    longitude = longitude,
    radiusMeters = radiusMeters,
    transitionTypes = transitionTypes.joinToString(",") { it.name },
    webhookId = webhookId,
    active = active,
    createdAt = createdAt.toEpochMilliseconds(),
    updatedAt = updatedAt.toEpochMilliseconds(),
)

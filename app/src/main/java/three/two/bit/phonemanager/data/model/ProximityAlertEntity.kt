package three.two.bit.phonemanager.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant
import three.two.bit.phonemanager.domain.model.AlertDirection
import three.two.bit.phonemanager.domain.model.ProximityAlert
import three.two.bit.phonemanager.domain.model.ProximityState

/**
 * Story E5.1: ProximityAlertEntity - Room database entity for proximity alerts
 *
 * AC E5.1.1: Complete entity with all required fields
 */
@Entity(tableName = "proximity_alerts")
data class ProximityAlertEntity(
    @PrimaryKey
    val id: String,
    val ownerDeviceId: String,
    val targetDeviceId: String,
    val radiusMeters: Int,
    val direction: String, // Stored as String, converted from/to AlertDirection enum
    val active: Boolean,
    val lastState: String, // Stored as String, converted from/to ProximityState enum
    val createdAt: Long, // Epoch milliseconds
    val updatedAt: Long, // Epoch milliseconds
    val lastTriggeredAt: Long? = null, // Epoch milliseconds, nullable
)

/**
 * Convert ProximityAlertEntity to domain model
 */
fun ProximityAlertEntity.toDomain(targetDisplayName: String? = null): ProximityAlert = ProximityAlert(
    id = id,
    ownerDeviceId = ownerDeviceId,
    targetDeviceId = targetDeviceId,
    targetDisplayName = targetDisplayName,
    radiusMeters = radiusMeters,
    direction = AlertDirection.valueOf(direction),
    active = active,
    lastState = ProximityState.valueOf(lastState),
    createdAt = Instant.fromEpochMilliseconds(createdAt),
    updatedAt = Instant.fromEpochMilliseconds(updatedAt),
    lastTriggeredAt = lastTriggeredAt?.let { Instant.fromEpochMilliseconds(it) },
)

/**
 * Convert domain model to ProximityAlertEntity
 */
fun ProximityAlert.toEntity(): ProximityAlertEntity = ProximityAlertEntity(
    id = id,
    ownerDeviceId = ownerDeviceId,
    targetDeviceId = targetDeviceId,
    radiusMeters = radiusMeters,
    direction = direction.name,
    active = active,
    lastState = lastState.name,
    createdAt = createdAt.toEpochMilliseconds(),
    updatedAt = updatedAt.toEpochMilliseconds(),
    lastTriggeredAt = lastTriggeredAt?.toEpochMilliseconds(),
)

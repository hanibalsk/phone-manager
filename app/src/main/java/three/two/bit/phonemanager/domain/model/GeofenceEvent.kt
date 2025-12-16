package three.two.bit.phonemanager.domain.model

import kotlin.time.Instant

/**
 * Story E6.2: GeofenceEvent Domain Model
 *
 * Represents a geofence transition event
 * AC E6.2.4: Complete event entity definition
 */
data class GeofenceEvent(
    val id: String,
    val deviceId: String,
    val geofenceId: String,
    val eventType: TransitionType,
    val timestamp: Instant,
    val latitude: Double,
    val longitude: Double,
    val webhookDelivered: Boolean = false,
    val webhookResponseCode: Int? = null,
)

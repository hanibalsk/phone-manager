package three.two.bit.phonemanager.domain.model

import kotlin.time.Instant

/**
 * Story E6.1: Geofence Domain Model
 *
 * Represents a geographic boundary zone with transition monitoring
 * AC E6.1.1: Complete geofence entity definition
 */
data class Geofence(
    val id: String,
    val deviceId: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Int,
    val transitionTypes: Set<TransitionType>,
    val webhookId: String? = null,
    val active: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
)

/**
 * Geofence transition types (AC E6.1.2)
 */
enum class TransitionType {
    ENTER, // Trigger when entering zone
    EXIT, // Trigger when leaving zone
    DWELL, // Trigger after staying in zone for duration
}

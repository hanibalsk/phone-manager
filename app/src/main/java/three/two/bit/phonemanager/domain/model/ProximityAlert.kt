package three.two.bit.phonemanager.domain.model

import kotlin.time.Instant

/**
 * Story E5.1: Proximity Alert Domain Model
 *
 * Represents a proximity alert between two devices
 * AC E5.1.1: Complete alert entity definition
 * AC E5.1.2: Radius validation (50-10,000 meters)
 */
data class ProximityAlert(
    val id: String,
    val ownerDeviceId: String,
    val targetDeviceId: String,
    val targetDisplayName: String? = null,
    val radiusMeters: Int,
    val direction: AlertDirection,
    val active: Boolean,
    val lastState: ProximityState,
    val createdAt: Instant,
    val updatedAt: Instant,
    val lastTriggeredAt: Instant? = null,
) {
    init {
        require(radiusMeters in MIN_RADIUS_METERS..MAX_RADIUS_METERS) {
            "Radius must be between $MIN_RADIUS_METERS and $MAX_RADIUS_METERS meters, got: $radiusMeters"
        }
    }

    companion object {
        /** Minimum radius for proximity alerts (AC E5.1.2) */
        const val MIN_RADIUS_METERS = 50

        /** Maximum radius for proximity alerts (AC E5.1.2) */
        const val MAX_RADIUS_METERS = 10_000
    }
}

/**
 * Alert direction type (AC E5.1.3)
 */
enum class AlertDirection {
    ENTER, // Notify when target enters radius
    EXIT, // Notify when target exits radius
    BOTH, // Notify on both transitions
}

/**
 * Proximity state tracking
 */
enum class ProximityState {
    INSIDE, // Target is currently inside radius
    OUTSIDE, // Target is currently outside radius
}

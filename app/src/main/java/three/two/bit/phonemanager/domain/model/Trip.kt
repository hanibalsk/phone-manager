package three.two.bit.phonemanager.domain.model

import three.two.bit.phonemanager.movement.TransportationMode
import kotlin.time.Instant

/**
 * Story E8.3: Trip Domain Model
 *
 * Represents a user's trip/journey with transportation mode tracking.
 * AC E8.3.1: Complete Trip domain model with computed properties
 */
data class Trip(
    val id: String,
    val state: TripState,
    val startTime: Instant,
    val endTime: Instant?,
    val startLocation: LatLng,
    val endLocation: LatLng?,
    val totalDistanceMeters: Double,
    val locationCount: Int,
    val dominantMode: TransportationMode,
    val modesUsed: Set<TransportationMode>,
    val modeBreakdown: Map<TransportationMode, Long>,
    val startTrigger: TripTrigger,
    val endTrigger: TripTrigger?,
    val isSynced: Boolean,
    val syncedAt: Instant?,
    val serverId: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
    // Story E8.10: Custom trip name (AC E8.10.7)
    val name: String? = null,
) {
    /**
     * Whether the trip is currently active (in progress).
     */
    val isActive: Boolean
        get() = state == TripState.ACTIVE || state == TripState.PENDING_END

    /**
     * Trip duration in seconds, null if trip hasn't ended.
     */
    val durationSeconds: Long?
        get() = endTime?.let { it.epochSeconds - startTime.epochSeconds }

    /**
     * Average speed in km/h, null if trip hasn't ended or duration is 0.
     */
    val averageSpeedKmh: Double?
        get() = durationSeconds?.let { duration ->
            if (duration > 0) {
                (totalDistanceMeters / 1000.0) / (duration / 3600.0)
            } else {
                null
            }
        }

    /**
     * Trip duration formatted as "Xh Ym" or "Xm Ys".
     */
    val formattedDuration: String?
        get() = durationSeconds?.let { duration ->
            when {
                duration >= 3600 -> {
                    val hours = duration / 3600
                    val minutes = (duration % 3600) / 60
                    "${hours}h ${minutes}m"
                }
                duration >= 60 -> {
                    val minutes = duration / 60
                    val seconds = duration % 60
                    "${minutes}m ${seconds}s"
                }
                else -> "${duration}s"
            }
        }

    /**
     * Distance formatted in km or m.
     */
    val formattedDistance: String
        get() = when {
            totalDistanceMeters >= 1000 -> String.format("%.1f km", totalDistanceMeters / 1000.0)
            else -> String.format("%.0f m", totalDistanceMeters)
        }
}

/**
 * Trip state machine states.
 */
enum class TripState {
    /**
     * No active trip, waiting for movement detection.
     */
    IDLE,

    /**
     * Trip is in progress, actively tracking.
     */
    ACTIVE,

    /**
     * Movement stopped, waiting to confirm trip end.
     */
    PENDING_END,

    /**
     * Trip has ended and is finalized.
     */
    COMPLETED,
}

/**
 * Triggers that can start or end a trip.
 */
enum class TripTrigger {
    /**
     * User manually started/ended the trip.
     */
    MANUAL,

    /**
     * Activity recognition detected movement/stillness.
     */
    ACTIVITY_DETECTION,

    /**
     * User exited/entered a known geofence.
     */
    GEOFENCE_EXIT,

    /**
     * Significant motion sensor triggered.
     */
    SIGNIFICANT_MOTION,

    /**
     * Stationary state detected for threshold period.
     */
    STATIONARY_DETECTION,

    /**
     * Location significantly changed.
     */
    LOCATION_CHANGE,

    /**
     * Timeout triggered the transition.
     */
    TIMEOUT,
}

/**
 * Simple latitude/longitude coordinate.
 */
data class LatLng(val latitude: Double, val longitude: Double)

/**
 * Statistics for trips in a time period (e.g., today).
 */
data class TodayTripStats(
    val tripCount: Int,
    val totalDistanceMeters: Double,
    val totalDurationSeconds: Long,
    val dominantMode: TransportationMode?,
) {
    val formattedDistance: String
        get() = when {
            totalDistanceMeters >= 1000 -> String.format("%.1f km", totalDistanceMeters / 1000.0)
            else -> String.format("%.0f m", totalDistanceMeters)
        }

    val formattedDuration: String
        get() = when {
            totalDurationSeconds >= 3600 -> {
                val hours = totalDurationSeconds / 3600
                val minutes = (totalDurationSeconds % 3600) / 60
                "${hours}h ${minutes}m"
            }
            totalDurationSeconds >= 60 -> {
                val minutes = totalDurationSeconds / 60
                "${minutes}m"
            }
            else -> "${totalDurationSeconds}s"
        }

    companion object {
        /**
         * Story E8.13: Empty stats for initial state (AC E8.13.4)
         */
        val EMPTY = TodayTripStats(
            tripCount = 0,
            totalDistanceMeters = 0.0,
            totalDurationSeconds = 0L,
            dominantMode = null,
        )
    }
}

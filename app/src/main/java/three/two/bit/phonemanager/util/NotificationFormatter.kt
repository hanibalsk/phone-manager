package three.two.bit.phonemanager.util

import android.content.Context
import kotlinx.datetime.Clock
import three.two.bit.phonemanager.R
import three.two.bit.phonemanager.movement.TransportationMode

/**
 * Utility object for formatting notification content.
 * Extracted from LocationTrackingService for testability.
 */
object NotificationFormatter {

    /**
     * Get mode emoji for notification.
     */
    fun getModeEmoji(mode: TransportationMode): String = when (mode) {
        TransportationMode.WALKING -> "🚶"
        TransportationMode.RUNNING -> "🏃"
        TransportationMode.CYCLING -> "🚲"
        TransportationMode.IN_VEHICLE -> "🚗"
        TransportationMode.STATIONARY -> "📍"
        TransportationMode.UNKNOWN -> "❓"
    }

    /**
     * Format trip duration.
     * Format: "<1 min", "X min", or "Xh Ym"
     */
    fun formatTripDuration(context: Context, startEpochSeconds: Long): String {
        val now = Clock.System.now().epochSeconds
        val durationSeconds = now - startEpochSeconds

        return when {
            durationSeconds >= 3600 -> {
                val hours = durationSeconds / 3600
                val minutes = (durationSeconds % 3600) / 60
                context.getString(R.string.notification_duration_hours_minutes, hours, minutes)
            }
            durationSeconds >= 60 -> {
                val minutes = durationSeconds / 60
                context.getString(R.string.notification_duration_minutes, minutes)
            }
            else -> context.getString(R.string.notification_duration_less_than_minute)
        }
    }

    /**
     * Format trip distance.
     * Format: "X.X km" or "X m"
     */
    fun formatTripDistance(context: Context, distanceMeters: Double): String = if (distanceMeters >= 1000) {
        context.getString(R.string.notification_distance_km, distanceMeters / 1000.0)
    } else {
        context.getString(R.string.notification_distance_m, distanceMeters)
    }

    /**
     * Get "Last update: X min ago" text for notification.
     */
    fun getLastUpdateText(context: Context, lastUpdateTimestamp: Long?): String = if (lastUpdateTimestamp != null) {
        val now = Clock.System.now().toEpochMilliseconds()
        val diffMs = now - lastUpdateTimestamp
        val diffMinutes = diffMs / 60000

        when {
            diffMinutes < 1 -> context.getString(R.string.notification_last_update_just_now)
            diffMinutes < 60 -> context.getString(R.string.notification_last_update_minutes, diffMinutes.toInt())
            else -> {
                val hours = diffMinutes / 60
                context.getString(R.string.notification_last_update_hours, hours.toInt())
            }
        }
    } else {
        context.getString(R.string.notification_last_update_never)
    }
}

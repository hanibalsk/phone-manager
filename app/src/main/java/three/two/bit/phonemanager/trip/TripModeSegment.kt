package three.two.bit.phonemanager.trip

import kotlin.time.Instant
import three.two.bit.phonemanager.movement.TransportationMode

/**
 * Story E8.4: TripModeSegment - Represents a segment of a trip in a specific transportation mode
 *
 * Tracks the duration spent in each transportation mode during a trip.
 * Used to calculate dominant mode and mode breakdown statistics.
 *
 * AC E8.4.6: Track mode segments during trips
 */
data class TripModeSegment(
    /**
     * Transportation mode for this segment.
     */
    val mode: TransportationMode,

    /**
     * Start time of this mode segment.
     */
    val startTime: Instant,

    /**
     * End time of this mode segment (null if currently active).
     */
    val endTime: Instant? = null,
) {
    /**
     * Duration of this segment in milliseconds.
     *
     * Returns 0 if endTime is null (segment still active).
     */
    val durationMs: Long
        get() = endTime?.let { it.toEpochMilliseconds() - startTime.toEpochMilliseconds() } ?: 0L

    /**
     * Duration of this segment in seconds.
     */
    val durationSeconds: Long
        get() = durationMs / 1000

    /**
     * Whether this segment is still active (has no end time).
     */
    val isActive: Boolean
        get() = endTime == null

    /**
     * Create a copy with the end time set.
     */
    fun end(at: Instant): TripModeSegment = copy(endTime = at)
}

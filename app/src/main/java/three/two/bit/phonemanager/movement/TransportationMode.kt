package three.two.bit.phonemanager.movement

/**
 * Transportation mode detected by the movement detection system.
 * Used to adapt location tracking interval based on how the user is moving.
 */
enum class TransportationMode {
    /**
     * User is stationary or mode is unknown.
     * Uses default tracking interval.
     */
    STATIONARY,

    /**
     * User is walking.
     * Uses default tracking interval.
     */
    WALKING,

    /**
     * User is running.
     * Uses default tracking interval.
     */
    RUNNING,

    /**
     * User is cycling.
     * Uses default tracking interval.
     */
    CYCLING,

    /**
     * User is in a vehicle (car, bus, train, etc.).
     * Uses increased tracking frequency (45% more updates).
     */
    IN_VEHICLE,

    /**
     * Mode is currently unknown or being detected.
     * Uses default tracking interval.
     */
    UNKNOWN;

    /**
     * Returns the interval multiplier for this transportation mode.
     * Values < 1.0 mean more frequent updates (faster moving modes).
     *
     * @return multiplier to apply to base tracking interval
     */
    fun getIntervalMultiplier(): Float = when (this) {
        IN_VEHICLE -> VEHICLE_INTERVAL_MULTIPLIER
        else -> DEFAULT_INTERVAL_MULTIPLIER
    }

    /**
     * Whether this mode should trigger more frequent location updates.
     */
    fun requiresFrequentUpdates(): Boolean = this == IN_VEHICLE

    companion object {
        /**
         * Default interval multiplier (no change).
         */
        const val DEFAULT_INTERVAL_MULTIPLIER = 1.0f

        /**
         * Interval multiplier for vehicle mode.
         * 0.55 = 45% more frequent updates (55% of original interval).
         */
        const val VEHICLE_INTERVAL_MULTIPLIER = 0.55f
    }
}

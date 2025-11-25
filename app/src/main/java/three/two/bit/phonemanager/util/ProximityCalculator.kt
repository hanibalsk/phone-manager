package three.two.bit.phonemanager.util

import com.google.android.gms.maps.model.LatLng
import three.two.bit.phonemanager.domain.model.AlertDirection
import three.two.bit.phonemanager.domain.model.ProximityAlert
import three.two.bit.phonemanager.domain.model.ProximityState

/**
 * Story E5.2: Proximity calculation utilities
 *
 * AC E5.2.1: Distance calculation using Location.distanceTo()
 * AC E5.2.4: State transition detection and triggering logic
 */
object ProximityCalculator {

    /**
     * Calculate distance between two points using Haversine formula (AC E5.2.1)
     *
     * @param lat1 First point latitude
     * @param lon1 First point longitude
     * @param lat2 Second point latitude
     * @param lon2 Second point longitude
     * @return Distance in meters
     */
    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        // Haversine formula for great circle distance
        val earthRadiusMeters = 6371000.0 // Earth radius in meters

        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a =
            Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)

        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))

        return (earthRadiusMeters * c).toFloat()
    }

    /**
     * Calculate distance between two LatLng points
     */
    fun calculateDistance(point1: LatLng, point2: LatLng): Float =
        calculateDistance(point1.latitude, point1.longitude, point2.latitude, point2.longitude)

    /**
     * Check proximity and determine if alert should trigger (AC E5.2.2, E5.2.4)
     *
     * @param myLocation Current device location
     * @param targetLocation Target device location
     * @param alert Proximity alert to check
     * @return ProximityCheckResult with distance, state, and trigger decision
     */
    fun checkProximity(myLocation: LatLng, targetLocation: LatLng, alert: ProximityAlert): ProximityCheckResult {
        val distance =
            calculateDistance(
                myLocation.latitude,
                myLocation.longitude,
                targetLocation.latitude,
                targetLocation.longitude,
            )

        val isInside = distance <= alert.radiusMeters
        val newState = if (isInside) ProximityState.INSIDE else ProximityState.OUTSIDE

        // AC E5.2.4: Trigger only on state transitions, not continuous state
        val triggered = shouldTrigger(alert, newState)

        return ProximityCheckResult(
            distance = distance,
            newState = newState,
            triggered = triggered,
        )
    }

    /**
     * Determine if alert should trigger based on state transition (AC E5.2.4)
     *
     * @param alert Proximity alert with current lastState
     * @param newState New calculated state
     * @return True if alert should trigger (state transition occurred)
     */
    private fun shouldTrigger(alert: ProximityAlert, newState: ProximityState): Boolean {
        // AC E5.2.4: No trigger if state unchanged (debounce)
        if (alert.lastState == newState) return false

        // Check direction configuration
        return when (alert.direction) {
            AlertDirection.ENTER -> newState == ProximityState.INSIDE // Trigger on OUTSIDE→INSIDE
            AlertDirection.EXIT -> newState == ProximityState.OUTSIDE // Trigger on INSIDE→OUTSIDE
            AlertDirection.BOTH -> true // Trigger on any transition
        }
    }
}

/**
 * Result of proximity check
 *
 * @property distance Distance in meters between devices
 * @property newState New proximity state (INSIDE or OUTSIDE)
 * @property triggered True if alert should trigger notification
 */
data class ProximityCheckResult(val distance: Float, val newState: ProximityState, val triggered: Boolean)

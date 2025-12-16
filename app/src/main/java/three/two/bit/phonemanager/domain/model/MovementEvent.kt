package three.two.bit.phonemanager.domain.model

import kotlin.time.Instant
import three.two.bit.phonemanager.movement.TransportationMode

/**
 * Story E8.3: MovementEvent Domain Model
 *
 * Records a transportation mode change event with telemetry data.
 * AC E8.3.2: Complete MovementEvent domain model with nested data classes
 */
data class MovementEvent(
    val id: Long,
    val timestamp: Instant,
    val tripId: String?,
    val previousMode: TransportationMode,
    val newMode: TransportationMode,
    val detectionSource: DetectionSource,
    val confidence: Float,
    val detectionLatencyMs: Long,
    val location: EventLocation?,
    val deviceState: DeviceState?,
    val sensorTelemetry: SensorTelemetry?,
    val movementContext: MovementContext?,
    val isSynced: Boolean,
    val syncedAt: Instant?,
) {
    /**
     * Whether this event represents a transition to an active movement mode.
     */
    val isStartMoving: Boolean
        get() = previousMode == TransportationMode.STATIONARY && newMode != TransportationMode.STATIONARY

    /**
     * Whether this event represents a transition to stationary.
     */
    val isStopMoving: Boolean
        get() = previousMode != TransportationMode.STATIONARY && newMode == TransportationMode.STATIONARY

    /**
     * Human-readable description of the mode change.
     */
    val transitionDescription: String
        get() = "${previousMode.name} → ${newMode.name}"
}

/**
 * Source of transportation mode detection.
 */
enum class DetectionSource {
    /**
     * Google Activity Recognition API.
     */
    ACTIVITY_RECOGNITION,

    /**
     * Sensor fusion algorithm combining multiple signals.
     */
    SENSOR_FUSION,

    /**
     * Speed-based detection from GPS.
     */
    SPEED_BASED,

    /**
     * Manual user selection.
     */
    MANUAL,

    /**
     * Geofence transition triggered.
     */
    GEOFENCE,

    /**
     * Step counter/pedometer.
     */
    STEP_COUNTER,

    /**
     * Significant motion sensor.
     */
    SIGNIFICANT_MOTION,

    /**
     * Unknown or unspecified source.
     */
    UNKNOWN,
}

/**
 * Location snapshot at the time of the event.
 */
data class EventLocation(val latitude: Double, val longitude: Double, val accuracy: Float?, val speed: Float?) {
    /**
     * Whether this is a high-accuracy location (< 20m).
     */
    val isHighAccuracy: Boolean
        get() = accuracy != null && accuracy < 20f
}

/**
 * Device state at the time of the event.
 */
data class DeviceState(
    val batteryLevel: Int?,
    val batteryCharging: Boolean?,
    val networkType: NetworkType?,
    val networkStrength: Int?,
) {
    /**
     * Whether battery is low (< 20%).
     */
    val isLowBattery: Boolean
        get() = batteryLevel != null && batteryLevel < 20
}

/**
 * Network connection types.
 */
enum class NetworkType {
    WIFI,
    CELLULAR,
    NONE,
    UNKNOWN,
}

/**
 * Sensor telemetry data captured at the time of the event.
 */
data class SensorTelemetry(
    val accelerometerMagnitude: Float?,
    val accelerometerVariance: Float?,
    val accelerometerPeakFrequency: Float?,
    val gyroscopeMagnitude: Float?,
    val stepCount: Int?,
    val significantMotion: Boolean?,
    val activityType: String?,
    val activityConfidence: Int?,
) {
    /**
     * Whether significant motion was detected.
     */
    val hasSignificantMotion: Boolean
        get() = significantMotion == true

    /**
     * Whether activity confidence is high (> 80%).
     */
    val isHighConfidenceActivity: Boolean
        get() = activityConfidence != null && activityConfidence > 80
}

/**
 * Movement context information.
 */
data class MovementContext(val distanceFromLastLocation: Float?, val timeSinceLastLocation: Long?) {
    /**
     * Speed in m/s calculated from distance and time.
     */
    val calculatedSpeedMs: Float?
        get() {
            val distance = distanceFromLastLocation ?: return null
            val timeMs = timeSinceLastLocation ?: return null
            if (timeMs <= 0) return null
            return distance / (timeMs / 1000f)
        }
}

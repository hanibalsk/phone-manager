package three.two.bit.phonemanager.network.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * API Compatibility: Movement Event API models
 *
 * Models for /api/v1/movement-events endpoints
 */

/**
 * Transportation mode enum for API serialization (lowercase)
 */
@Serializable
enum class TransportationModeDto {
    @SerialName("STATIONARY")
    STATIONARY,

    @SerialName("WALKING")
    WALKING,

    @SerialName("RUNNING")
    RUNNING,

    @SerialName("CYCLING")
    CYCLING,

    @SerialName("IN_VEHICLE")
    IN_VEHICLE,

    @SerialName("UNKNOWN")
    UNKNOWN,
}

/**
 * Detection source enum for API serialization
 */
@Serializable
enum class DetectionSourceDto {
    @SerialName("ACTIVITY_RECOGNITION")
    ACTIVITY_RECOGNITION,

    @SerialName("BLUETOOTH_CAR")
    BLUETOOTH_CAR,

    @SerialName("ANDROID_AUTO")
    ANDROID_AUTO,

    @SerialName("MULTIPLE")
    MULTIPLE,

    @SerialName("NONE")
    NONE,
}

/**
 * Detection source details for movement event
 */
@Serializable
data class DetectionSourceDetails(
    val primary: String,
    val contributing: List<String> = emptyList(),
)

/**
 * Location snapshot at time of movement event
 */
@Serializable
data class MovementEventLocationDto(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float? = null,
    val speed: Float? = null,
)

/**
 * Device state at time of movement event
 */
@Serializable
data class MovementEventDeviceStateDto(
    val batteryLevel: Int? = null,
    val batteryCharging: Boolean? = null,
    val networkType: String? = null,
    val networkStrength: Int? = null,
)

/**
 * Activity recognition data
 */
@Serializable
data class ActivityRecognitionDto(
    val type: String,
    val confidence: Int,
)

/**
 * Accelerometer telemetry data
 */
@Serializable
data class AccelerometerTelemetryDto(
    val magnitude: Float? = null,
    val variance: Float? = null,
    val peakFrequency: Float? = null,
)

/**
 * Gyroscope telemetry data
 */
@Serializable
data class GyroscopeTelemetryDto(
    val magnitude: Float? = null,
)

/**
 * Sensor telemetry data for movement event
 */
@Serializable
data class MovementEventTelemetryDto(
    val accelerometer: AccelerometerTelemetryDto? = null,
    val gyroscope: GyroscopeTelemetryDto? = null,
    val stepCount: Int? = null,
    val significantMotion: Boolean? = null,
    val activityRecognition: ActivityRecognitionDto? = null,
)

/**
 * Create movement event request
 * POST /api/v1/movement-events
 */
@Serializable
data class CreateMovementEventRequest(
    val eventId: String,
    val deviceId: String,
    val timestamp: String,
    val previousMode: String,
    val newMode: String,
    val detectionSource: DetectionSourceDetails,
    val confidence: Float,
    val detectionLatencyMs: Int,
    val location: MovementEventLocationDto? = null,
    val deviceState: MovementEventDeviceStateDto? = null,
    val telemetry: MovementEventTelemetryDto? = null,
    val tripId: String? = null,
)

/**
 * Batch movement events request
 * POST /api/v1/movement-events/batch
 */
@Serializable
data class BatchMovementEventsRequest(
    val events: List<CreateMovementEventRequest>,
)

/**
 * Movement event response from server
 */
@Serializable
data class MovementEventDto(
    val eventId: String,
    val deviceId: String,
    val timestamp: String,
    val previousMode: String,
    val newMode: String,
    val detectionSource: DetectionSourceDetails,
    val confidence: Float,
    val detectionLatencyMs: Int,
    val location: MovementEventLocationDto? = null,
    val deviceState: MovementEventDeviceStateDto? = null,
    val telemetry: MovementEventTelemetryDto? = null,
    val tripId: String? = null,
    val processedAt: String? = null,
)

/**
 * Single event upload response
 * POST /api/v1/movement-events response
 */
@Serializable
data class MovementEventUploadResponse(
    val eventId: String,
    val processedAt: String,
)

/**
 * Batch event error details
 */
@Serializable
data class BatchEventError(
    val eventId: String,
    val error: String,
    val message: String,
)

/**
 * Batch movement events response
 * POST /api/v1/movement-events/batch response
 */
@Serializable
data class BatchMovementEventsResponse(
    val processedCount: Int,
    val failedCount: Int = 0,
    val errors: List<BatchEventError> = emptyList(),
)

/**
 * Pagination info for movement events list
 */
@Serializable
data class MovementEventsPaginationInfo(
    val total: Int,
    val limit: Int,
    val offset: Int,
    val hasMore: Boolean,
)

/**
 * List movement events response
 * GET /api/v1/devices/{deviceId}/movement-events response
 */
@Serializable
data class MovementEventsListResponse(
    val events: List<MovementEventDto>,
    val pagination: MovementEventsPaginationInfo,
)

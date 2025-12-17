package three.two.bit.phonemanager.network.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * API Compatibility: Trip API models
 *
 * Models for /api/v1/trips endpoints
 */

/**
 * Trip status enum for API serialization
 */
@Serializable
enum class TripStatusDto {
    @SerialName("ACTIVE")
    ACTIVE,

    @SerialName("COMPLETED")
    COMPLETED,

    @SerialName("CANCELLED")
    CANCELLED,
}

/**
 * Trip trigger enum for API serialization
 */
@Serializable
enum class TripTriggerDto {
    @SerialName("MODE_CHANGE")
    MODE_CHANGE,

    @SerialName("TIME")
    TIME,

    @SerialName("DISTANCE")
    DISTANCE,

    @SerialName("STATIONARY")
    STATIONARY,

    @SerialName("MANUAL")
    MANUAL,
}

/**
 * Location coordinate for trip start/end
 */
@Serializable
data class TripLocationDto(val latitude: Double, val longitude: Double)

/**
 * Trip statistics
 */
@Serializable
data class TripStatisticsDto(
    val distanceMeters: Double? = null,
    val durationSeconds: Int? = null,
    val locationCount: Int? = null,
    val movementEventCount: Int? = null,
)

/**
 * Transportation mode breakdown
 */
@Serializable
data class TripModesDto(val dominant: String? = null, val breakdown: Map<String, Long>? = null)

/**
 * Trip triggers (start/end)
 */
@Serializable
data class TripTriggersDto(val start: String? = null, val end: String? = null)

/**
 * Create trip request
 * POST /api/v1/trips
 */
@Serializable
data class CreateTripRequest(
    val localTripId: String,
    val deviceId: String,
    val startTime: String,
    val status: String = "ACTIVE",
    val startLocation: TripLocationDto? = null,
    val modes: TripModesDto? = null,
    val triggers: TripTriggersDto? = null,
)

/**
 * Update trip request (partial update)
 * PATCH /api/v1/trips/{tripId}
 */
@Serializable
data class UpdateTripRequest(
    val endTime: String? = null,
    val status: String? = null,
    val endLocation: TripLocationDto? = null,
    val statistics: TripStatisticsDto? = null,
    val modes: TripModesDto? = null,
    val triggers: TripTriggersDto? = null,
)

/**
 * Trip response from server
 */
@Serializable
data class TripDto(
    val tripId: String,
    val localTripId: String,
    val deviceId: String,
    val startTime: String,
    val endTime: String? = null,
    val status: String,
    val startLocation: TripLocationDto? = null,
    val endLocation: TripLocationDto? = null,
    val statistics: TripStatisticsDto? = null,
    val modes: TripModesDto? = null,
    val triggers: TripTriggersDto? = null,
    val createdAt: String,
    val updatedAt: String,
)

/**
 * Create trip response
 * POST /api/v1/trips response
 */
@Serializable
data class CreateTripResponse(val tripId: String, val localTripId: String, val createdAt: String)

/**
 * List trips response
 * GET /api/v1/devices/{deviceId}/trips response
 */
@Serializable
data class TripsListResponse(val trips: List<TripDto>, val total: Int)

/**
 * Trip location point for path/locations endpoints
 */
@Serializable
data class TripLocationPointDto(
    val timestamp: String,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float? = null,
    val speed: Float? = null,
    val transportationMode: String? = null,
)

/**
 * Trip locations response
 * GET /api/v1/trips/{tripId}/locations response
 */
@Serializable
data class TripLocationsResponse(val tripId: String, val locations: List<TripLocationPointDto>, val count: Int)

/**
 * Trip path response (corrected coordinates)
 * GET /api/v1/trips/{tripId}/path response
 */
@Serializable
data class TripPathResponse(
    val tripId: String,
    val path: List<List<Double>>,
    val corrected: Boolean = false,
    val algorithm: String? = null,
    val totalPoints: Int? = null,
    val correctedPoints: Int? = null,
)

/**
 * Path correction request
 * POST /api/v1/trips/{tripId}/correct-path
 */
@Serializable
data class PathCorrectionRequest(val algorithm: String? = null)

/**
 * Path correction response
 * POST /api/v1/trips/{tripId}/correct-path response
 */
@Serializable
data class PathCorrectionResponse(
    val tripId: String,
    val status: String,
    val message: String? = null,
    val correctedAt: String? = null,
    val totalPoints: Int? = null,
    val correctedPoints: Int? = null,
)

/**
 * Trip movement events response
 * GET /api/v1/trips/{tripId}/movement-events response
 */
@Serializable
data class TripMovementEventsResponse(val tripId: String, val events: List<MovementEventDto>, val total: Int)

package three.two.bit.phonemanager.network.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Story E6.1: Geofence API models
 *
 * Models for /api/v1/geofences endpoints
 */

/**
 * Geofence event types matching backend (serialized as lowercase)
 */
@Serializable
enum class GeofenceEventType {
    @SerialName("enter")
    ENTER,

    @SerialName("exit")
    EXIT,

    @SerialName("dwell")
    DWELL,
}

/**
 * Create geofence request
 * POST /api/v1/geofences
 */
@Serializable
data class CreateGeofenceRequest(
    @SerialName("device_id") val deviceId: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    @SerialName("radius_meters") val radiusMeters: Double,
    @SerialName("event_types") val eventTypes: List<GeofenceEventType> = listOf(GeofenceEventType.ENTER, GeofenceEventType.EXIT),
    val active: Boolean = true,
    val metadata: Map<String, String>? = null,
)

/**
 * Update geofence request (partial update)
 * PATCH /api/v1/geofences/{geofenceId}
 */
@Serializable
data class UpdateGeofenceRequest(
    val name: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    @SerialName("radius_meters") val radiusMeters: Double? = null,
    @SerialName("event_types") val eventTypes: List<GeofenceEventType>? = null,
    val active: Boolean? = null,
    val metadata: Map<String, String>? = null,
)

/**
 * Geofence response
 */
@Serializable
data class GeofenceDto(
    @SerialName("geofence_id") val geofenceId: String,
    @SerialName("device_id") val deviceId: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    @SerialName("radius_meters") val radiusMeters: Double,
    @SerialName("event_types") val eventTypes: List<GeofenceEventType>,
    val active: Boolean,
    val metadata: Map<String, String>? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
)

/**
 * List geofences response
 * GET /api/v1/geofences?deviceId={id}
 */
@Serializable
data class ListGeofencesResponse(val geofences: List<GeofenceDto>, val total: Int)

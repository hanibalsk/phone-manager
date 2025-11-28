package three.two.bit.phonemanager.network.models

import kotlinx.serialization.Serializable

/**
 * Story E6.2: Geofence Event API models
 *
 * Models for /api/v1/geofence-events endpoints
 * AC E6.2.3: Send event to backend
 */

/**
 * Create geofence event request
 * POST /api/v1/geofence-events
 *
 * Payload includes: deviceId, geofenceId, eventType, timestamp, location
 */
@Serializable
data class CreateGeofenceEventRequest(
    val deviceId: String,
    val geofenceId: String,
    val eventType: GeofenceEventType,
    val timestamp: String,
    val latitude: Double,
    val longitude: Double,
)

/**
 * Geofence event response
 */
@Serializable
data class GeofenceEventDto(
    val eventId: String,
    val deviceId: String,
    val geofenceId: String,
    val geofenceName: String? = null,
    val eventType: GeofenceEventType,
    val timestamp: String,
    val latitude: Double,
    val longitude: Double,
    val webhookDelivered: Boolean = false,
    val webhookResponseCode: Int? = null,
)

/**
 * List geofence events response
 * GET /api/v1/geofence-events?deviceId={id}
 */
@Serializable
data class ListGeofenceEventsResponse(
    val events: List<GeofenceEventDto>,
    val total: Int,
)

package three.two.bit.phonemanager.network.models

import kotlinx.serialization.Serializable

/**
 * Story E4.2: Location history API models
 *
 * Models for GET /api/v1/devices/{deviceId}/locations
 */

/**
 * Location history response with pagination
 */
@Serializable
data class LocationHistoryResponse(val locations: List<LocationHistoryItem>, val pagination: PaginationInfo)

/**
 * Single location item in history response
 */
@Serializable
data class LocationHistoryItem(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Double,
    val altitude: Double? = null,
    val bearing: Double? = null,
    val speed: Double? = null,
    val provider: String? = null,
    val batteryLevel: Int? = null,
    val networkType: String? = null,
    val capturedAt: String,
    val createdAt: String,
)

/**
 * Cursor-based pagination info
 */
@Serializable
data class PaginationInfo(val nextCursor: String? = null, val hasMore: Boolean)

package three.two.bit.phonemanager.network.models

import kotlinx.serialization.Serializable

/**
 * Story E5.1: Proximity Alert API models
 *
 * Models for /api/v1/proximity-alerts endpoints
 */

/**
 * Create proximity alert request
 * POST /api/v1/proximity-alerts
 */
@Serializable
data class CreateProximityAlertRequest(
    val sourceDeviceId: String,
    val targetDeviceId: String,
    val name: String? = null,
    val radiusMeters: Int,
    val isActive: Boolean = true,
    val metadata: Map<String, String>? = null,
)

/**
 * Update proximity alert request (partial update)
 * PATCH /api/v1/proximity-alerts/{alertId}
 */
@Serializable
data class UpdateProximityAlertRequest(
    val name: String? = null,
    val radiusMeters: Int? = null,
    val isActive: Boolean? = null,
    val metadata: Map<String, String>? = null,
)

/**
 * Proximity alert response
 */
@Serializable
data class ProximityAlertDto(
    val alertId: String,
    val sourceDeviceId: String,
    val targetDeviceId: String,
    val name: String? = null,
    val radiusMeters: Int,
    val isActive: Boolean,
    val metadata: Map<String, String>? = null,
    val createdAt: String,
    val updatedAt: String,
)

/**
 * List proximity alerts response
 * GET /api/v1/proximity-alerts?sourceDeviceId={id}
 */
@Serializable
data class ListProximityAlertsResponse(val alerts: List<ProximityAlertDto>, val total: Int)

package three.two.bit.phonemanager.domain.model

import kotlinx.datetime.Instant

/**
 * Story E6.3: Webhook Domain Model
 *
 * Represents a webhook configuration for geofence event delivery
 * AC E6.3.1: Complete webhook entity definition
 */
data class Webhook(
    val id: String,
    val ownerDeviceId: String,
    val name: String,
    val targetUrl: String,
    val secret: String,
    val enabled: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
)

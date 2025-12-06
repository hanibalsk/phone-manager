package three.two.bit.phonemanager.network.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Story E6.3: Webhook API models
 *
 * Models for /api/v1/webhooks endpoints
 * AC E6.3.1, E6.3.5: Webhook CRUD operations
 */

/**
 * Create webhook request
 * POST /api/v1/webhooks
 */
@Serializable
data class CreateWebhookRequest(
    @SerialName("owner_device_id") val ownerDeviceId: String,
    val name: String,
    @SerialName("target_url") val targetUrl: String,
    val secret: String,
    val enabled: Boolean = true,
)

/**
 * Update webhook request
 * PUT /api/v1/webhooks/{webhookId}
 */
@Serializable
data class UpdateWebhookRequest(
    val name: String? = null,
    @SerialName("target_url") val targetUrl: String? = null,
    val secret: String? = null,
    val enabled: Boolean? = null,
)

/**
 * Webhook response DTO
 */
@Serializable
data class WebhookDto(
    @SerialName("webhook_id") val webhookId: String,
    @SerialName("owner_device_id") val ownerDeviceId: String,
    val name: String,
    @SerialName("target_url") val targetUrl: String,
    val secret: String,
    val enabled: Boolean,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
)

/**
 * List webhooks response
 * GET /api/v1/webhooks?ownerDeviceId={id}
 */
@Serializable
data class ListWebhooksResponse(val webhooks: List<WebhookDto>, val total: Int)

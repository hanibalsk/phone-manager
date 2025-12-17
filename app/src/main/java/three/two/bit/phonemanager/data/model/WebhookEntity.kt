package three.two.bit.phonemanager.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import three.two.bit.phonemanager.domain.model.Webhook
import kotlin.time.Instant

/**
 * Story E6.3: WebhookEntity - Room entity for webhook configuration storage
 *
 * AC E6.3.1: Webhook entity with all required fields
 * - id (UUID), ownerDeviceId, name, targetUrl, secret, enabled, timestamps
 */
@Entity(tableName = "webhooks")
data class WebhookEntity(
    @PrimaryKey
    val id: String,
    val ownerDeviceId: String,
    val name: String,
    val targetUrl: String,
    val secret: String,
    val enabled: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)

/**
 * Convert WebhookEntity to domain model
 */
fun WebhookEntity.toDomain(): Webhook = Webhook(
    id = id,
    ownerDeviceId = ownerDeviceId,
    name = name,
    targetUrl = targetUrl,
    secret = secret,
    enabled = enabled,
    createdAt = Instant.fromEpochMilliseconds(createdAt),
    updatedAt = Instant.fromEpochMilliseconds(updatedAt),
)

/**
 * Convert domain model to WebhookEntity
 */
fun Webhook.toEntity(): WebhookEntity = WebhookEntity(
    id = id,
    ownerDeviceId = ownerDeviceId,
    name = name,
    targetUrl = targetUrl,
    secret = secret,
    enabled = enabled,
    createdAt = createdAt.toEpochMilliseconds(),
    updatedAt = updatedAt.toEpochMilliseconds(),
)

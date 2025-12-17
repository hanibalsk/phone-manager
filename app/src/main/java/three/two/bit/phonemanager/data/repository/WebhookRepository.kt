package three.two.bit.phonemanager.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import three.two.bit.phonemanager.data.database.WebhookDao
import three.two.bit.phonemanager.data.model.WebhookEntity
import three.two.bit.phonemanager.data.model.toDomain
import three.two.bit.phonemanager.data.model.toEntity
import three.two.bit.phonemanager.domain.model.Webhook
import three.two.bit.phonemanager.network.WebhookApiService
import three.two.bit.phonemanager.network.models.CreateWebhookRequest
import three.two.bit.phonemanager.network.models.UpdateWebhookRequest
import three.two.bit.phonemanager.security.SecureStorage
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Story E6.3: WebhookRepository - Manages webhook data from local storage and server
 *
 * AC E6.3.5: CRUD operations for webhooks
 */
interface WebhookRepository {
    /**
     * Observe webhooks for current device
     */
    fun observeWebhooks(): Flow<List<Webhook>>

    /**
     * Get a webhook by ID
     */
    suspend fun getWebhook(webhookId: String): Webhook?

    /**
     * Get all webhooks for current device (non-Flow)
     */
    suspend fun getAllWebhooks(): List<Webhook>

    /**
     * Get all enabled webhooks for current device
     */
    suspend fun getEnabledWebhooks(): List<Webhook>

    /**
     * Create a new webhook (AC E6.3.1)
     */
    suspend fun createWebhook(name: String, targetUrl: String, secret: String): Result<Webhook>

    /**
     * Update a webhook
     */
    suspend fun updateWebhook(
        webhookId: String,
        name: String? = null,
        targetUrl: String? = null,
        secret: String? = null,
        enabled: Boolean? = null,
    ): Result<Webhook>

    /**
     * Delete a webhook
     */
    suspend fun deleteWebhook(webhookId: String): Result<Unit>

    /**
     * Toggle webhook enabled state
     */
    suspend fun toggleWebhook(webhookId: String): Result<Webhook>

    /**
     * Sync webhooks from server
     */
    suspend fun syncFromServer(): Result<Unit>
}

@Singleton
class WebhookRepositoryImpl @Inject constructor(
    private val webhookDao: WebhookDao,
    private val webhookApiService: WebhookApiService,
    private val secureStorage: SecureStorage,
) : WebhookRepository {

    override fun observeWebhooks(): Flow<List<Webhook>> {
        val deviceId = secureStorage.getDeviceId()
        return webhookDao.observeByDevice(deviceId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getWebhook(webhookId: String): Webhook? = webhookDao.getById(webhookId)?.toDomain()

    override suspend fun getAllWebhooks(): List<Webhook> {
        val deviceId = secureStorage.getDeviceId()
        return webhookDao.getAllByDevice(deviceId).map { it.toDomain() }
    }

    override suspend fun getEnabledWebhooks(): List<Webhook> {
        val deviceId = secureStorage.getDeviceId()
        return webhookDao.getEnabledByDevice(deviceId).map { it.toDomain() }
    }

    override suspend fun createWebhook(name: String, targetUrl: String, secret: String): Result<Webhook> {
        val deviceId = secureStorage.getDeviceId()
        val now = Clock.System.now()

        // Create local entity first
        val webhook = Webhook(
            id = UUID.randomUUID().toString(),
            ownerDeviceId = deviceId,
            name = name,
            targetUrl = targetUrl,
            secret = secret,
            enabled = true,
            createdAt = now,
            updatedAt = now,
        )

        // Save locally
        webhookDao.insert(webhook.toEntity())
        Timber.d("Webhook saved locally: ${webhook.id}")

        // Sync to server
        val request = CreateWebhookRequest(
            ownerDeviceId = deviceId,
            name = name,
            targetUrl = targetUrl,
            secret = secret,
            enabled = true,
        )

        webhookApiService.createWebhook(request).fold(
            onSuccess = { response ->
                // Update local entity with server ID if different
                if (response.webhookId != webhook.id) {
                    webhookDao.deleteById(webhook.id)
                    val updatedWebhook = webhook.copy(
                        id = response.webhookId,
                        createdAt = Instant.parse(response.createdAt),
                        updatedAt = Instant.parse(response.updatedAt),
                    )
                    webhookDao.insert(updatedWebhook.toEntity())
                    Timber.i("Webhook synced with server ID: ${response.webhookId}")
                    return Result.success(updatedWebhook)
                }
                Timber.i("Webhook created on server: ${response.webhookId}")
            },
            onFailure = { error ->
                Timber.w(error, "Failed to sync webhook to server: ${webhook.id}")
                // Keep local version, will retry later
            },
        )

        return Result.success(webhook)
    }

    override suspend fun updateWebhook(
        webhookId: String,
        name: String?,
        targetUrl: String?,
        secret: String?,
        enabled: Boolean?,
    ): Result<Webhook> {
        val existing = webhookDao.getById(webhookId)
            ?: return Result.failure(IllegalArgumentException("Webhook not found: $webhookId"))

        val now = Clock.System.now()
        val updated = existing.copy(
            name = name ?: existing.name,
            targetUrl = targetUrl ?: existing.targetUrl,
            secret = secret ?: existing.secret,
            enabled = enabled ?: existing.enabled,
            updatedAt = now.toEpochMilliseconds(),
        )

        webhookDao.update(updated)
        Timber.d("Webhook updated locally: $webhookId")

        // Sync to server
        val request = UpdateWebhookRequest(
            name = name,
            targetUrl = targetUrl,
            secret = secret,
            enabled = enabled,
        )

        webhookApiService.updateWebhook(webhookId, request).fold(
            onSuccess = { response ->
                Timber.i("Webhook updated on server: ${response.webhookId}")
            },
            onFailure = { error ->
                Timber.w(error, "Failed to sync webhook update to server: $webhookId")
            },
        )

        return Result.success(updated.toDomain())
    }

    override suspend fun deleteWebhook(webhookId: String): Result<Unit> {
        // Delete from server first
        webhookApiService.deleteWebhook(webhookId).fold(
            onSuccess = {
                Timber.i("Webhook deleted from server: $webhookId")
            },
            onFailure = { error ->
                Timber.w(error, "Failed to delete webhook from server: $webhookId")
                // Continue with local delete anyway
            },
        )

        // Delete locally
        webhookDao.deleteById(webhookId)
        Timber.d("Webhook deleted locally: $webhookId")

        return Result.success(Unit)
    }

    override suspend fun toggleWebhook(webhookId: String): Result<Webhook> {
        val existing = webhookDao.getById(webhookId)
            ?: return Result.failure(IllegalArgumentException("Webhook not found: $webhookId"))

        return updateWebhook(webhookId, enabled = !existing.enabled)
    }

    override suspend fun syncFromServer(): Result<Unit> = try {
        val deviceId = secureStorage.getDeviceId()
        Timber.d("Syncing webhooks from server for device: $deviceId")

        val response = webhookApiService.listWebhooks(deviceId)

        response.fold(
            onSuccess = { listResponse ->
                val webhooks = listResponse.webhooks.map { dto ->
                    WebhookEntity(
                        id = dto.webhookId,
                        ownerDeviceId = dto.ownerDeviceId,
                        name = dto.name,
                        targetUrl = dto.targetUrl,
                        secret = dto.secret,
                        enabled = dto.enabled,
                        createdAt = Instant.parse(dto.createdAt).toEpochMilliseconds(),
                        updatedAt = Instant.parse(dto.updatedAt).toEpochMilliseconds(),
                    )
                }

                // Replace all local webhooks with server data
                webhookDao.deleteAllByDevice(deviceId)
                webhookDao.insertAll(webhooks)

                Timber.i("Synced ${webhooks.size} webhooks from server")
                Result.success(Unit)
            },
            onFailure = { error ->
                Timber.e(error, "Failed to sync webhooks from server")
                Result.failure(error)
            },
        )
    } catch (e: Exception) {
        Timber.e(e, "Error syncing webhooks from server")
        Result.failure(e)
    }
}

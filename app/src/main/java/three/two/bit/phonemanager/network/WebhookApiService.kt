package three.two.bit.phonemanager.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import three.two.bit.phonemanager.network.models.CreateWebhookRequest
import three.two.bit.phonemanager.network.models.ListWebhooksResponse
import three.two.bit.phonemanager.network.models.UpdateWebhookRequest
import three.two.bit.phonemanager.network.models.WebhookDto
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Story E6.3: WebhookApiService - HTTP client for webhook management
 *
 * Provides CRUD operations for webhooks
 * AC E6.3.5: Webhook CRUD Operations
 */
interface WebhookApiService {
    /**
     * Create a new webhook
     * POST /api/v1/webhooks
     */
    suspend fun createWebhook(request: CreateWebhookRequest): Result<WebhookDto>

    /**
     * Update an existing webhook
     * PUT /api/v1/webhooks/{webhookId}
     */
    suspend fun updateWebhook(webhookId: String, request: UpdateWebhookRequest): Result<WebhookDto>

    /**
     * Delete a webhook
     * DELETE /api/v1/webhooks/{webhookId}
     */
    suspend fun deleteWebhook(webhookId: String): Result<Unit>

    /**
     * Get a webhook by ID
     * GET /api/v1/webhooks/{webhookId}
     */
    suspend fun getWebhook(webhookId: String): Result<WebhookDto>

    /**
     * List webhooks for a device
     * GET /api/v1/webhooks?ownerDeviceId={id}
     */
    suspend fun listWebhooks(ownerDeviceId: String): Result<ListWebhooksResponse>
}

@Singleton
class WebhookApiServiceImpl @Inject constructor(
    private val httpClient: HttpClient,
    private val apiConfig: ApiConfiguration,
) : WebhookApiService {

    override suspend fun createWebhook(request: CreateWebhookRequest): Result<WebhookDto> = try {
        Timber.d("Creating webhook: ${request.name}")

        val response: WebhookDto = httpClient.post("${apiConfig.baseUrl}/api/v1/webhooks") {
            contentType(ContentType.Application.Json)
            header("X-API-Key", apiConfig.apiKey)
            setBody(request)
        }.body()

        Timber.i("Webhook created: ${response.webhookId}")
        Result.success(response)
    } catch (e: Exception) {
        Timber.e(e, "Failed to create webhook")
        Result.failure(e)
    }

    override suspend fun updateWebhook(webhookId: String, request: UpdateWebhookRequest): Result<WebhookDto> = try {
        Timber.d("Updating webhook: $webhookId")

        val response: WebhookDto = httpClient.put("${apiConfig.baseUrl}/api/v1/webhooks/$webhookId") {
            contentType(ContentType.Application.Json)
            header("X-API-Key", apiConfig.apiKey)
            setBody(request)
        }.body()

        Timber.i("Webhook updated: ${response.webhookId}")
        Result.success(response)
    } catch (e: Exception) {
        Timber.e(e, "Failed to update webhook: $webhookId")
        Result.failure(e)
    }

    override suspend fun deleteWebhook(webhookId: String): Result<Unit> = try {
        Timber.d("Deleting webhook: $webhookId")

        httpClient.delete("${apiConfig.baseUrl}/api/v1/webhooks/$webhookId") {
            header("X-API-Key", apiConfig.apiKey)
        }

        Timber.i("Webhook deleted: $webhookId")
        Result.success(Unit)
    } catch (e: Exception) {
        Timber.e(e, "Failed to delete webhook: $webhookId")
        Result.failure(e)
    }

    override suspend fun getWebhook(webhookId: String): Result<WebhookDto> = try {
        Timber.d("Getting webhook: $webhookId")

        val response: WebhookDto = httpClient.get("${apiConfig.baseUrl}/api/v1/webhooks/$webhookId") {
            header("X-API-Key", apiConfig.apiKey)
        }.body()

        Timber.i("Fetched webhook: ${response.webhookId}")
        Result.success(response)
    } catch (e: Exception) {
        Timber.e(e, "Failed to get webhook: $webhookId")
        Result.failure(e)
    }

    override suspend fun listWebhooks(ownerDeviceId: String): Result<ListWebhooksResponse> = try {
        Timber.d("Listing webhooks for device: $ownerDeviceId")

        val response: ListWebhooksResponse = httpClient.get("${apiConfig.baseUrl}/api/v1/webhooks") {
            header("X-API-Key", apiConfig.apiKey)
            parameter("ownerDeviceId", ownerDeviceId)
        }.body()

        Timber.i("Fetched ${response.total} webhooks")
        Result.success(response)
    } catch (e: Exception) {
        Timber.e(e, "Failed to list webhooks")
        Result.failure(e)
    }
}

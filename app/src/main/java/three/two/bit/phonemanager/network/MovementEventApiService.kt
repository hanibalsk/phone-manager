package three.two.bit.phonemanager.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import three.two.bit.phonemanager.network.models.BatchMovementEventsRequest
import three.two.bit.phonemanager.network.models.BatchMovementEventsResponse
import three.two.bit.phonemanager.network.models.CreateMovementEventRequest
import three.two.bit.phonemanager.network.models.MovementEventDto
import three.two.bit.phonemanager.network.models.MovementEventUploadResponse
import three.two.bit.phonemanager.network.models.MovementEventsListResponse
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * API Compatibility: MovementEventApiService - HTTP client for movement event management
 *
 * Provides methods to upload and query movement events
 * Endpoints: POST /api/v1/movement-events, POST /api/v1/movement-events/batch,
 *            GET /api/v1/devices/{deviceId}/movement-events
 */
interface MovementEventApiService {
    /**
     * Upload a single movement event
     * POST /api/v1/movement-events
     */
    suspend fun uploadEvent(request: CreateMovementEventRequest): Result<MovementEventUploadResponse>

    /**
     * Upload movement events in batch (max 100 events)
     * POST /api/v1/movement-events/batch
     */
    suspend fun uploadEventsBatch(request: BatchMovementEventsRequest): Result<BatchMovementEventsResponse>

    /**
     * Get movement events for a device
     * GET /api/v1/devices/{deviceId}/movement-events
     *
     * @param deviceId Device identifier
     * @param from Start timestamp (ISO 8601)
     * @param to End timestamp (ISO 8601)
     * @param limit Max results (1-100, default 50)
     * @param offset Pagination offset
     */
    suspend fun getDeviceEvents(
        deviceId: String,
        from: String? = null,
        to: String? = null,
        limit: Int? = null,
        offset: Int? = null,
    ): Result<MovementEventsListResponse>

    /**
     * Get a single movement event by ID
     * GET /api/v1/movement-events/{eventId}
     */
    suspend fun getEvent(eventId: String): Result<MovementEventDto>
}

@Singleton
class MovementEventApiServiceImpl @Inject constructor(
    private val httpClient: HttpClient,
    private val apiConfig: ApiConfiguration,
) : MovementEventApiService {

    override suspend fun uploadEvent(request: CreateMovementEventRequest): Result<MovementEventUploadResponse> = try {
        Timber.d("Uploading movement event: ${request.eventId}, mode change: ${request.previousMode} -> ${request.newMode}")

        val response: MovementEventUploadResponse = httpClient.post("${apiConfig.baseUrl}/api/v1/movement-events") {
            contentType(ContentType.Application.Json)
            header("X-API-Key", apiConfig.apiKey)
            setBody(request)
        }.body()

        Timber.i("Movement event uploaded: ${response.eventId}")
        Result.success(response)
    } catch (e: Exception) {
        Timber.e(e, "Failed to upload movement event: ${request.eventId}")
        Result.failure(e)
    }

    override suspend fun uploadEventsBatch(request: BatchMovementEventsRequest): Result<BatchMovementEventsResponse> =
        try {
            Timber.d("Uploading ${request.events.size} movement events in batch")

            val response: BatchMovementEventsResponse =
                httpClient.post("${apiConfig.baseUrl}/api/v1/movement-events/batch") {
                    contentType(ContentType.Application.Json)
                    header("X-API-Key", apiConfig.apiKey)
                    setBody(request)
                }.body()

            Timber.i("Batch upload complete: processed=${response.processedCount}, failed=${response.failedCount}")
            Result.success(response)
        } catch (e: Exception) {
            Timber.e(e, "Failed to upload movement events batch")
            Result.failure(e)
        }

    override suspend fun getDeviceEvents(
        deviceId: String,
        from: String?,
        to: String?,
        limit: Int?,
        offset: Int?,
    ): Result<MovementEventsListResponse> = try {
        Timber.d("Fetching movement events for device: $deviceId")

        val response: MovementEventsListResponse = httpClient.get(
            "${apiConfig.baseUrl}/api/v1/devices/$deviceId/movement-events",
        ) {
            header("X-API-Key", apiConfig.apiKey)
            from?.let { parameter("from", it) }
            to?.let { parameter("to", it) }
            limit?.let { parameter("limit", it) }
            offset?.let { parameter("offset", it) }
        }.body()

        Timber.i("Fetched ${response.events.size} movement events for device: $deviceId")
        Result.success(response)
    } catch (e: Exception) {
        Timber.e(e, "Failed to fetch movement events for device: $deviceId")
        Result.failure(e)
    }

    override suspend fun getEvent(eventId: String): Result<MovementEventDto> = try {
        Timber.d("Fetching movement event: $eventId")

        val response: MovementEventDto = httpClient.get(
            "${apiConfig.baseUrl}/api/v1/movement-events/$eventId",
        ) {
            header("X-API-Key", apiConfig.apiKey)
        }.body()

        Timber.i("Fetched movement event: ${response.eventId}")
        Result.success(response)
    } catch (e: Exception) {
        Timber.e(e, "Failed to fetch movement event: $eventId")
        Result.failure(e)
    }
}

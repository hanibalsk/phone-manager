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
import three.two.bit.phonemanager.network.models.CreateGeofenceEventRequest
import three.two.bit.phonemanager.network.models.GeofenceEventDto
import three.two.bit.phonemanager.network.models.ListGeofenceEventsResponse
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Story E6.2: GeofenceEventApiService - HTTP client for geofence events
 *
 * Provides operations for sending and fetching geofence events
 * AC E6.2.3: Send event to backend via POST /api/v1/geofence-events
 */
interface GeofenceEventApiService {
    /**
     * Send a geofence event to the server
     * POST /api/v1/geofence-events
     */
    suspend fun createEvent(request: CreateGeofenceEventRequest): Result<GeofenceEventDto>

    /**
     * List geofence events for a device
     * GET /api/v1/geofence-events?deviceId={id}
     */
    suspend fun listEvents(
        deviceId: String,
        geofenceId: String? = null,
        limit: Int = 50,
    ): Result<ListGeofenceEventsResponse>

    /**
     * Get a single event by ID
     * GET /api/v1/geofence-events/{eventId}
     */
    suspend fun getEvent(eventId: String): Result<GeofenceEventDto>
}

@Singleton
class GeofenceEventApiServiceImpl @Inject constructor(
    private val httpClient: HttpClient,
    private val apiConfig: ApiConfiguration,
) : GeofenceEventApiService {

    override suspend fun createEvent(request: CreateGeofenceEventRequest): Result<GeofenceEventDto> = try {
        Timber.d("Sending geofence event: ${request.eventType} for geofence ${request.geofenceId}")

        val response: GeofenceEventDto = httpClient.post("${apiConfig.baseUrl}/api/v1/geofence-events") {
            contentType(ContentType.Application.Json)
            header("X-API-Key", apiConfig.apiKey)
            setBody(request)
        }.body()

        Timber.i("Geofence event created: ${response.eventId}")
        Result.success(response)
    } catch (e: Exception) {
        Timber.e(e, "Failed to send geofence event")
        Result.failure(e)
    }

    override suspend fun listEvents(
        deviceId: String,
        geofenceId: String?,
        limit: Int,
    ): Result<ListGeofenceEventsResponse> = try {
        Timber.d("Listing geofence events for device: $deviceId")

        val response: ListGeofenceEventsResponse = httpClient.get("${apiConfig.baseUrl}/api/v1/geofence-events") {
            header("X-API-Key", apiConfig.apiKey)
            parameter("deviceId", deviceId)
            geofenceId?.let { parameter("geofenceId", it) }
            parameter("limit", limit)
        }.body()

        Timber.i("Fetched ${response.total} geofence events")
        Result.success(response)
    } catch (e: Exception) {
        Timber.e(e, "Failed to list geofence events")
        Result.failure(e)
    }

    override suspend fun getEvent(eventId: String): Result<GeofenceEventDto> = try {
        Timber.d("Getting geofence event: $eventId")

        val response: GeofenceEventDto = httpClient.get("${apiConfig.baseUrl}/api/v1/geofence-events/$eventId") {
            header("X-API-Key", apiConfig.apiKey)
        }.body()

        Timber.i("Fetched geofence event: ${response.eventId}")
        Result.success(response)
    } catch (e: Exception) {
        Timber.e(e, "Failed to get geofence event: $eventId")
        Result.failure(e)
    }
}

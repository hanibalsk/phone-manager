package three.two.bit.phonemanager.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import three.two.bit.phonemanager.network.models.CreateTripRequest
import three.two.bit.phonemanager.network.models.CreateTripResponse
import three.two.bit.phonemanager.network.models.PathCorrectionResponse
import three.two.bit.phonemanager.network.models.TripDto
import three.two.bit.phonemanager.network.models.TripLocationsResponse
import three.two.bit.phonemanager.network.models.TripMovementEventsResponse
import three.two.bit.phonemanager.network.models.TripPathResponse
import three.two.bit.phonemanager.network.models.TripsListResponse
import three.two.bit.phonemanager.network.models.UpdateTripRequest
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * API Compatibility: TripApiService - HTTP client for trip management
 *
 * Provides methods to create, update, and query trips
 * Endpoints: POST /api/v1/trips, PATCH /api/v1/trips/{tripId},
 *            GET /api/v1/devices/{deviceId}/trips, GET /api/v1/trips/{tripId}/path,
 *            POST /api/v1/trips/{tripId}/correct-path
 */
interface TripApiService {
    /**
     * Create a new trip
     * POST /api/v1/trips
     */
    suspend fun createTrip(request: CreateTripRequest): Result<CreateTripResponse>

    /**
     * Update an existing trip (partial update)
     * PATCH /api/v1/trips/{tripId}
     */
    suspend fun updateTrip(tripId: String, request: UpdateTripRequest): Result<TripDto>

    /**
     * Get a trip by ID
     * GET /api/v1/trips/{tripId}
     */
    suspend fun getTrip(tripId: String): Result<TripDto>

    /**
     * List trips for a device
     * GET /api/v1/devices/{deviceId}/trips
     *
     * @param deviceId Device identifier
     * @param status Filter by status (ACTIVE, COMPLETED, CANCELLED)
     * @param from Start timestamp (ISO 8601)
     * @param to End timestamp (ISO 8601)
     * @param limit Max results (1-100, default 20)
     */
    suspend fun getDeviceTrips(
        deviceId: String,
        status: String? = null,
        from: String? = null,
        to: String? = null,
        limit: Int? = null,
    ): Result<TripsListResponse>

    /**
     * Get trip locations
     * GET /api/v1/trips/{tripId}/locations
     */
    suspend fun getTripLocations(tripId: String): Result<TripLocationsResponse>

    /**
     * Get trip movement events
     * GET /api/v1/trips/{tripId}/movement-events
     */
    suspend fun getTripMovementEvents(tripId: String): Result<TripMovementEventsResponse>

    /**
     * Get trip path (corrected coordinates)
     * GET /api/v1/trips/{tripId}/path
     */
    suspend fun getTripPath(tripId: String): Result<TripPathResponse>

    /**
     * Trigger path correction for a trip
     * POST /api/v1/trips/{tripId}/correct-path
     *
     * Note: Rate limited to 1 per hour per trip
     */
    suspend fun triggerPathCorrection(tripId: String): Result<PathCorrectionResponse>

    /**
     * Delete a trip
     * DELETE /api/v1/trips/{tripId}
     */
    suspend fun deleteTrip(tripId: String): Result<Unit>
}

@Singleton
class TripApiServiceImpl @Inject constructor(
    private val httpClient: HttpClient,
    private val apiConfig: ApiConfiguration,
) : TripApiService {

    override suspend fun createTrip(request: CreateTripRequest): Result<CreateTripResponse> = try {
        Timber.d("Creating trip: localTripId=${request.localTripId}, deviceId=${request.deviceId}")

        val response: CreateTripResponse = httpClient.post("${apiConfig.baseUrl}/api/v1/trips") {
            contentType(ContentType.Application.Json)
            header("X-API-Key", apiConfig.apiKey)
            setBody(request)
        }.body()

        Timber.i("Trip created: tripId=${response.tripId}, localTripId=${response.localTripId}")
        Result.success(response)
    } catch (e: Exception) {
        Timber.e(e, "Failed to create trip: localTripId=${request.localTripId}")
        Result.failure(e)
    }

    override suspend fun updateTrip(tripId: String, request: UpdateTripRequest): Result<TripDto> = try {
        Timber.d("Updating trip: $tripId, status=${request.status}")

        val response: TripDto = httpClient.patch("${apiConfig.baseUrl}/api/v1/trips/$tripId") {
            contentType(ContentType.Application.Json)
            header("X-API-Key", apiConfig.apiKey)
            setBody(request)
        }.body()

        Timber.i("Trip updated: ${response.tripId}, status=${response.status}")
        Result.success(response)
    } catch (e: Exception) {
        Timber.e(e, "Failed to update trip: $tripId")
        Result.failure(e)
    }

    override suspend fun getTrip(tripId: String): Result<TripDto> = try {
        Timber.d("Fetching trip: $tripId")

        val response: TripDto = httpClient.get("${apiConfig.baseUrl}/api/v1/trips/$tripId") {
            header("X-API-Key", apiConfig.apiKey)
        }.body()

        Timber.i("Fetched trip: ${response.tripId}")
        Result.success(response)
    } catch (e: Exception) {
        Timber.e(e, "Failed to fetch trip: $tripId")
        Result.failure(e)
    }

    override suspend fun getDeviceTrips(
        deviceId: String,
        status: String?,
        from: String?,
        to: String?,
        limit: Int?,
    ): Result<TripsListResponse> = try {
        Timber.d("Fetching trips for device: $deviceId, status=$status")

        val response: TripsListResponse = httpClient.get(
            "${apiConfig.baseUrl}/api/v1/devices/$deviceId/trips",
        ) {
            header("X-API-Key", apiConfig.apiKey)
            status?.let { parameter("status", it) }
            from?.let { parameter("from", it) }
            to?.let { parameter("to", it) }
            limit?.let { parameter("limit", it) }
        }.body()

        Timber.i("Fetched ${response.trips.size} trips for device: $deviceId")
        Result.success(response)
    } catch (e: Exception) {
        Timber.e(e, "Failed to fetch trips for device: $deviceId")
        Result.failure(e)
    }

    override suspend fun getTripLocations(tripId: String): Result<TripLocationsResponse> = try {
        Timber.d("Fetching trip locations: $tripId")

        val response: TripLocationsResponse = httpClient.get(
            "${apiConfig.baseUrl}/api/v1/trips/$tripId/locations",
        ) {
            header("X-API-Key", apiConfig.apiKey)
        }.body()

        Timber.i("Fetched ${response.count} locations for trip: $tripId")
        Result.success(response)
    } catch (e: Exception) {
        Timber.e(e, "Failed to fetch trip locations: $tripId")
        Result.failure(e)
    }

    override suspend fun getTripMovementEvents(tripId: String): Result<TripMovementEventsResponse> = try {
        Timber.d("Fetching trip movement events: $tripId")

        val response: TripMovementEventsResponse = httpClient.get(
            "${apiConfig.baseUrl}/api/v1/trips/$tripId/movement-events",
        ) {
            header("X-API-Key", apiConfig.apiKey)
        }.body()

        Timber.i("Fetched ${response.total} movement events for trip: $tripId")
        Result.success(response)
    } catch (e: Exception) {
        Timber.e(e, "Failed to fetch trip movement events: $tripId")
        Result.failure(e)
    }

    override suspend fun getTripPath(tripId: String): Result<TripPathResponse> = try {
        Timber.d("Fetching trip path: $tripId")

        val response: TripPathResponse = httpClient.get(
            "${apiConfig.baseUrl}/api/v1/trips/$tripId/path",
        ) {
            header("X-API-Key", apiConfig.apiKey)
        }.body()

        Timber.i("Fetched trip path: $tripId, corrected=${response.corrected}")
        Result.success(response)
    } catch (e: Exception) {
        Timber.e(e, "Failed to fetch trip path: $tripId")
        Result.failure(e)
    }

    override suspend fun triggerPathCorrection(tripId: String): Result<PathCorrectionResponse> = try {
        Timber.d("Triggering path correction for trip: $tripId")

        val response: PathCorrectionResponse = httpClient.post(
            "${apiConfig.baseUrl}/api/v1/trips/$tripId/correct-path",
        ) {
            contentType(ContentType.Application.Json)
            header("X-API-Key", apiConfig.apiKey)
        }.body()

        Timber.i("Path correction triggered for trip: $tripId, status=${response.status}")
        Result.success(response)
    } catch (e: Exception) {
        Timber.e(e, "Failed to trigger path correction for trip: $tripId")
        Result.failure(e)
    }

    override suspend fun deleteTrip(tripId: String): Result<Unit> = try {
        Timber.d("Deleting trip: $tripId")

        httpClient.delete("${apiConfig.baseUrl}/api/v1/trips/$tripId") {
            header("X-API-Key", apiConfig.apiKey)
        }

        Timber.i("Trip deleted: $tripId")
        Result.success(Unit)
    } catch (e: Exception) {
        Timber.e(e, "Failed to delete trip: $tripId")
        Result.failure(e)
    }
}

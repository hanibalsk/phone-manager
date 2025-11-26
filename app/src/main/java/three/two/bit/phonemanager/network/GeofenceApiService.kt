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
import three.two.bit.phonemanager.network.models.CreateGeofenceRequest
import three.two.bit.phonemanager.network.models.GeofenceDto
import three.two.bit.phonemanager.network.models.ListGeofencesResponse
import three.two.bit.phonemanager.network.models.UpdateGeofenceRequest
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Story E6.1: GeofenceApiService - HTTP client for geofence management
 *
 * Provides CRUD operations for geofences
 * AC E6.1.4: Server sync via POST /api/v1/geofences
 */
interface GeofenceApiService {
    /**
     * Create a new geofence
     * POST /api/v1/geofences
     */
    suspend fun createGeofence(request: CreateGeofenceRequest): Result<GeofenceDto>

    /**
     * List geofences for a device
     * GET /api/v1/geofences?deviceId={id}
     */
    suspend fun listGeofences(deviceId: String, includeInactive: Boolean = false): Result<ListGeofencesResponse>

    /**
     * Get a single geofence by ID
     * GET /api/v1/geofences/{geofenceId}
     */
    suspend fun getGeofence(geofenceId: String): Result<GeofenceDto>

    /**
     * Update a geofence (partial update)
     * PATCH /api/v1/geofences/{geofenceId}
     */
    suspend fun updateGeofence(geofenceId: String, request: UpdateGeofenceRequest): Result<GeofenceDto>

    /**
     * Delete a geofence
     * DELETE /api/v1/geofences/{geofenceId}
     */
    suspend fun deleteGeofence(geofenceId: String): Result<Unit>
}

@Singleton
class GeofenceApiServiceImpl @Inject constructor(
    private val httpClient: HttpClient,
    private val apiConfig: ApiConfiguration,
) : GeofenceApiService {

    override suspend fun createGeofence(request: CreateGeofenceRequest): Result<GeofenceDto> = try {
        Timber.d("Creating geofence: ${request.name} for device ${request.deviceId}")

        val response: GeofenceDto = httpClient.post("${apiConfig.baseUrl}/api/v1/geofences") {
            contentType(ContentType.Application.Json)
            header("X-API-Key", apiConfig.apiKey)
            setBody(request)
        }.body()

        Timber.i("Geofence created: ${response.geofenceId}")
        Result.success(response)
    } catch (e: Exception) {
        Timber.e(e, "Failed to create geofence")
        Result.failure(e)
    }

    override suspend fun listGeofences(deviceId: String, includeInactive: Boolean): Result<ListGeofencesResponse> =
        try {
            Timber.d("Listing geofences for device: $deviceId")

            val response: ListGeofencesResponse = httpClient.get("${apiConfig.baseUrl}/api/v1/geofences") {
                header("X-API-Key", apiConfig.apiKey)
                parameter("deviceId", deviceId)
                if (includeInactive) {
                    parameter("includeInactive", true)
                }
            }.body()

            Timber.i("Fetched ${response.total} geofences")
            Result.success(response)
        } catch (e: Exception) {
            Timber.e(e, "Failed to list geofences")
            Result.failure(e)
        }

    override suspend fun getGeofence(geofenceId: String): Result<GeofenceDto> = try {
        Timber.d("Getting geofence: $geofenceId")

        val response: GeofenceDto = httpClient.get("${apiConfig.baseUrl}/api/v1/geofences/$geofenceId") {
            header("X-API-Key", apiConfig.apiKey)
        }.body()

        Timber.i("Fetched geofence: ${response.geofenceId}")
        Result.success(response)
    } catch (e: Exception) {
        Timber.e(e, "Failed to get geofence: $geofenceId")
        Result.failure(e)
    }

    override suspend fun updateGeofence(geofenceId: String, request: UpdateGeofenceRequest): Result<GeofenceDto> = try {
        Timber.d("Updating geofence: $geofenceId")

        val response: GeofenceDto = httpClient.patch("${apiConfig.baseUrl}/api/v1/geofences/$geofenceId") {
            contentType(ContentType.Application.Json)
            header("X-API-Key", apiConfig.apiKey)
            setBody(request)
        }.body()

        Timber.i("Geofence updated: ${response.geofenceId}")
        Result.success(response)
    } catch (e: Exception) {
        Timber.e(e, "Failed to update geofence: $geofenceId")
        Result.failure(e)
    }

    override suspend fun deleteGeofence(geofenceId: String): Result<Unit> = try {
        Timber.d("Deleting geofence: $geofenceId")

        httpClient.delete("${apiConfig.baseUrl}/api/v1/geofences/$geofenceId") {
            header("X-API-Key", apiConfig.apiKey)
        }

        Timber.i("Geofence deleted: $geofenceId")
        Result.success(Unit)
    } catch (e: Exception) {
        Timber.e(e, "Failed to delete geofence: $geofenceId")
        Result.failure(e)
    }
}

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
import three.two.bit.phonemanager.network.models.CreateProximityAlertRequest
import three.two.bit.phonemanager.network.models.ListProximityAlertsResponse
import three.two.bit.phonemanager.network.models.ProximityAlertDto
import three.two.bit.phonemanager.network.models.UpdateProximityAlertRequest
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Story E5.1: ProximityAlertApiService - HTTP client for proximity alert management
 *
 * Provides CRUD operations for proximity alerts
 * AC E5.1.4: Server sync via POST /api/v1/proximity-alerts
 * AC E5.1.6: Sync on startup
 */
interface ProximityAlertApiService {
    /**
     * Create a new proximity alert
     * POST /api/v1/proximity-alerts
     */
    suspend fun createAlert(request: CreateProximityAlertRequest): Result<ProximityAlertDto>

    /**
     * List alerts for a source device
     * GET /api/v1/proximity-alerts?sourceDeviceId={id}
     */
    suspend fun listAlerts(
        sourceDeviceId: String,
        includeInactive: Boolean = false,
    ): Result<ListProximityAlertsResponse>

    /**
     * Get a single alert by ID
     * GET /api/v1/proximity-alerts/{alertId}
     */
    suspend fun getAlert(alertId: String): Result<ProximityAlertDto>

    /**
     * Update an alert (partial update)
     * PATCH /api/v1/proximity-alerts/{alertId}
     */
    suspend fun updateAlert(alertId: String, request: UpdateProximityAlertRequest): Result<ProximityAlertDto>

    /**
     * Delete an alert
     * DELETE /api/v1/proximity-alerts/{alertId}
     */
    suspend fun deleteAlert(alertId: String): Result<Unit>
}

@Singleton
class ProximityAlertApiServiceImpl @Inject constructor(
    private val httpClient: HttpClient,
    private val apiConfig: ApiConfiguration,
) : ProximityAlertApiService {

    override suspend fun createAlert(request: CreateProximityAlertRequest): Result<ProximityAlertDto> = try {
        Timber.d("Creating proximity alert: source=${request.sourceDeviceId}, target=${request.targetDeviceId}")

        val response: ProximityAlertDto = httpClient.post("${apiConfig.baseUrl}/api/v1/proximity-alerts") {
            contentType(ContentType.Application.Json)
            header("X-API-Key", apiConfig.apiKey)
            setBody(request)
        }.body()

        Timber.i("Proximity alert created: ${response.alertId}")
        Result.success(response)
    } catch (e: Exception) {
        Timber.e(e, "Failed to create proximity alert")
        Result.failure(e)
    }

    override suspend fun listAlerts(
        sourceDeviceId: String,
        includeInactive: Boolean,
    ): Result<ListProximityAlertsResponse> = try {
        Timber.d("Listing proximity alerts for device: $sourceDeviceId")

        val response: ListProximityAlertsResponse = httpClient.get("${apiConfig.baseUrl}/api/v1/proximity-alerts") {
            header("X-API-Key", apiConfig.apiKey)
            parameter("sourceDeviceId", sourceDeviceId)
            if (includeInactive) {
                parameter("includeInactive", true)
            }
        }.body()

        Timber.i("Fetched ${response.total} proximity alerts")
        Result.success(response)
    } catch (e: Exception) {
        Timber.e(e, "Failed to list proximity alerts")
        Result.failure(e)
    }

    override suspend fun getAlert(alertId: String): Result<ProximityAlertDto> = try {
        Timber.d("Getting proximity alert: $alertId")

        val response: ProximityAlertDto = httpClient.get("${apiConfig.baseUrl}/api/v1/proximity-alerts/$alertId") {
            header("X-API-Key", apiConfig.apiKey)
        }.body()

        Timber.i("Fetched proximity alert: ${response.alertId}")
        Result.success(response)
    } catch (e: Exception) {
        Timber.e(e, "Failed to get proximity alert: $alertId")
        Result.failure(e)
    }

    override suspend fun updateAlert(
        alertId: String,
        request: UpdateProximityAlertRequest,
    ): Result<ProximityAlertDto> = try {
        Timber.d("Updating proximity alert: $alertId")

        val response: ProximityAlertDto = httpClient.patch("${apiConfig.baseUrl}/api/v1/proximity-alerts/$alertId") {
            contentType(ContentType.Application.Json)
            header("X-API-Key", apiConfig.apiKey)
            setBody(request)
        }.body()

        Timber.i("Proximity alert updated: ${response.alertId}")
        Result.success(response)
    } catch (e: Exception) {
        Timber.e(e, "Failed to update proximity alert: $alertId")
        Result.failure(e)
    }

    override suspend fun deleteAlert(alertId: String): Result<Unit> = try {
        Timber.d("Deleting proximity alert: $alertId")

        httpClient.delete("${apiConfig.baseUrl}/api/v1/proximity-alerts/$alertId") {
            header("X-API-Key", apiConfig.apiKey)
        }

        Timber.i("Proximity alert deleted: $alertId")
        Result.success(Unit)
    } catch (e: Exception) {
        Timber.e(e, "Failed to delete proximity alert: $alertId")
        Result.failure(e)
    }
}

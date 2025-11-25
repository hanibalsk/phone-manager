package three.two.bit.phonemanager.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import three.two.bit.phonemanager.network.models.DeviceInfo
import three.two.bit.phonemanager.network.models.DeviceRegistrationRequest
import three.two.bit.phonemanager.network.models.DeviceRegistrationResponse
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Story E1.1: DeviceApiService - HTTP client for device registration and management
 *
 * Provides methods to register devices and fetch group members
 */
interface DeviceApiService {
    suspend fun registerDevice(request: DeviceRegistrationRequest): Result<DeviceRegistrationResponse>
    suspend fun getGroupMembers(groupId: String): Result<List<DeviceInfo>>
}

@Singleton
class DeviceApiServiceImpl @Inject constructor(
    private val httpClient: HttpClient,
    private val apiConfig: ApiConfiguration,
) : DeviceApiService {

    /**
     * Register device with backend
     * POST /api/devices/register
     */
    override suspend fun registerDevice(request: DeviceRegistrationRequest): Result<DeviceRegistrationResponse> = try {
        Timber.d("Registering device: deviceId=${request.deviceId}, displayName=${request.displayName}")

        val response: DeviceRegistrationResponse = httpClient.post("${apiConfig.baseUrl}/api/devices/register") {
            contentType(ContentType.Application.Json)
            header("X-API-Key", apiConfig.apiKey)
            setBody(request)
        }.body()

        Timber.i("Device registered successfully: ${response.deviceId}")
        Result.success(response)
    } catch (e: Exception) {
        Timber.e(e, "Failed to register device")
        Result.failure(e)
    }

    /**
     * Get all devices in a group
     * GET /api/devices?groupId={id}
     */
    override suspend fun getGroupMembers(groupId: String): Result<List<DeviceInfo>> = try {
        Timber.d("Fetching group members for groupId=$groupId")

        val response: List<DeviceInfo> = httpClient.get("${apiConfig.baseUrl}/api/devices") {
            url {
                parameters.append("groupId", groupId)
            }
            header("X-API-Key", apiConfig.apiKey)
        }.body()

        Timber.i("Fetched ${response.size} group members")
        Result.success(response)
    } catch (e: Exception) {
        Timber.e(e, "Failed to fetch group members")
        Result.failure(e)
    }
}

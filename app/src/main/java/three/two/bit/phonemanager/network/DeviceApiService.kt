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
import three.two.bit.phonemanager.domain.model.Device
import three.two.bit.phonemanager.network.models.DeviceRegistrationRequest
import three.two.bit.phonemanager.network.models.DeviceRegistrationResponse
import three.two.bit.phonemanager.network.models.DevicesResponse
import three.two.bit.phonemanager.network.models.toDomain
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
    suspend fun getGroupMembers(groupId: String): Result<List<Device>>
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
     *
     * Story E1.2: AC E1.2.1 - Fetch group members with proper API call and headers
     */
    override suspend fun getGroupMembers(groupId: String): Result<List<Device>> = try {
        Timber.d("Fetching group members for groupId=$groupId")

        val response: DevicesResponse = httpClient.get("${apiConfig.baseUrl}/api/devices") {
            parameter("groupId", groupId)
            header("X-API-Key", apiConfig.apiKey)
        }.body()

        val devices = response.devices.map { it.toDomain() }
        Timber.i("Fetched ${devices.size} group members for group: $groupId")
        Result.success(devices)
    } catch (e: Exception) {
        Timber.e(e, "Failed to fetch group members for groupId=$groupId")
        Result.failure(e)
    }
}

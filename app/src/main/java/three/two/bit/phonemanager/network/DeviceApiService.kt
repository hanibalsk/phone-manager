package three.two.bit.phonemanager.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import three.two.bit.phonemanager.domain.model.Device
import three.two.bit.phonemanager.domain.model.UserDevice
import three.two.bit.phonemanager.network.models.DataExportResponse
import three.two.bit.phonemanager.network.models.DeviceRegistrationRequest
import three.two.bit.phonemanager.network.models.DeviceRegistrationResponse
import three.two.bit.phonemanager.network.models.DevicesResponse
import three.two.bit.phonemanager.network.models.LinkDeviceRequest
import three.two.bit.phonemanager.network.models.LinkedDeviceResponse
import three.two.bit.phonemanager.network.models.ListUserDevicesResponse
import three.two.bit.phonemanager.network.models.LocationHistoryResponse
import three.two.bit.phonemanager.network.models.TransferDeviceRequest
import three.two.bit.phonemanager.network.models.TransferDeviceResponse
import three.two.bit.phonemanager.network.models.UnlinkDeviceResponse
import three.two.bit.phonemanager.network.models.toDomain
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Story E1.1: DeviceApiService - HTTP client for device registration and management
 *
 * Provides methods to register devices, fetch group members, and location history
 */
interface DeviceApiService {
    suspend fun registerDevice(request: DeviceRegistrationRequest): Result<DeviceRegistrationResponse>
    suspend fun getGroupMembers(groupId: String): Result<List<Device>>

    /**
     * Story E4.2: Get location history for a device
     * GET /api/v1/devices/{deviceId}/locations
     *
     * AC E4.2.5: Optional tolerance parameter for server-side downsampling
     * @param tolerance Downsampling tolerance in meters (e.g., 10m fine, 50m medium, 100m coarse)
     */
    suspend fun getLocationHistory(
        deviceId: String,
        from: Long? = null,
        to: Long? = null,
        cursor: String? = null,
        limit: Int? = null,
        order: String? = null,
        tolerance: Float? = null,
    ): Result<LocationHistoryResponse>

    /**
     * Delete a device
     * DELETE /api/v1/devices/{deviceId}
     */
    suspend fun deleteDevice(deviceId: String): Result<Unit>

    // API Compatibility: GDPR Data Export/Delete

    /**
     * Export device data for GDPR compliance
     * GET /api/v1/devices/{deviceId}/data-export
     */
    suspend fun exportDeviceData(deviceId: String): Result<DataExportResponse>

    /**
     * Delete all device data for GDPR compliance
     * DELETE /api/v1/devices/{deviceId}/data
     */
    suspend fun deleteDeviceData(deviceId: String): Result<Unit>

    // ============================================================================
    // Story E10.6: Device Binding API (AC E10.6.2, E10.6.4, E10.6.5)
    // ============================================================================

    /**
     * Story E10.6 Task 2: Link a device to a user account
     * POST /api/v1/users/{userId}/devices/{deviceId}/link
     *
     * AC E10.6.2: Device link flow
     *
     * @param userId The user's UUID
     * @param deviceId The device's UUID
     * @param displayName Optional display name override
     * @param isPrimary Whether to set as primary device
     * @param accessToken JWT access token for authentication
     * @return Result with linked device info on success
     */
    suspend fun linkDevice(
        userId: String,
        deviceId: String,
        displayName: String? = null,
        isPrimary: Boolean = false,
        accessToken: String,
    ): Result<LinkedDeviceResponse>

    /**
     * Story E10.6 Task 2: Unlink a device from a user account
     * DELETE /api/v1/users/{userId}/devices/{deviceId}/unlink
     *
     * AC E10.6.4: Device unlink
     *
     * @param userId The user's UUID
     * @param deviceId The device's UUID
     * @param accessToken JWT access token for authentication
     * @return Result with unlink confirmation
     */
    suspend fun unlinkDevice(
        userId: String,
        deviceId: String,
        accessToken: String,
    ): Result<UnlinkDeviceResponse>

    /**
     * Story E10.6 Task 2: Transfer device ownership to another user
     * POST /api/v1/users/{userId}/devices/{deviceId}/transfer
     *
     * AC E10.6.5: Transfer ownership
     *
     * @param userId The current owner's UUID
     * @param deviceId The device's UUID
     * @param newOwnerId The new owner's UUID
     * @param accessToken JWT access token for authentication
     * @return Result with transfer confirmation
     */
    suspend fun transferDevice(
        userId: String,
        deviceId: String,
        newOwnerId: String,
        accessToken: String,
    ): Result<TransferDeviceResponse>

    /**
     * Story E10.6 Task 2: Get all devices owned by a user
     * GET /api/v1/users/{userId}/devices
     *
     * AC E10.6.1: Device list screen
     *
     * @param userId The user's UUID
     * @param includeInactive Whether to include inactive devices
     * @param accessToken JWT access token for authentication
     * @return Result with list of user's devices
     */
    suspend fun getUserDevices(
        userId: String,
        includeInactive: Boolean = false,
        accessToken: String,
    ): Result<List<UserDevice>>
}

@Singleton
class DeviceApiServiceImpl @Inject constructor(
    private val httpClient: HttpClient,
    private val apiConfig: ApiConfiguration,
) : DeviceApiService {

    /**
     * Register device with backend
     * POST /api/v1/devices/register
     */
    override suspend fun registerDevice(request: DeviceRegistrationRequest): Result<DeviceRegistrationResponse> = try {
        Timber.d("Registering device: deviceId=${request.deviceId}, displayName=${request.displayName}")

        val response: DeviceRegistrationResponse = httpClient.post("${apiConfig.baseUrl}/api/v1/devices/register") {
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
     * GET /api/v1/devices?groupId={id}
     *
     * Story E1.2: AC E1.2.1 - Fetch group members with proper API call and headers
     */
    override suspend fun getGroupMembers(groupId: String): Result<List<Device>> = try {
        Timber.d("Fetching group members for groupId=$groupId")

        val response: DevicesResponse = httpClient.get("${apiConfig.baseUrl}/api/v1/devices") {
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

    /**
     * Story E4.2: Get location history for a device
     * GET /api/v1/devices/{deviceId}/locations
     *
     * AC E4.2.1: Fetch history from server with cursor-based pagination
     * AC E4.2.5: Optional tolerance parameter for server-side downsampling (meters)
     */
    override suspend fun getLocationHistory(
        deviceId: String,
        from: Long?,
        to: Long?,
        cursor: String?,
        limit: Int?,
        order: String?,
        tolerance: Float?,
    ): Result<LocationHistoryResponse> = try {
        Timber.d("Fetching location history for deviceId=$deviceId, tolerance=$tolerance")

        val response: LocationHistoryResponse = httpClient.get(
            "${apiConfig.baseUrl}/api/v1/devices/$deviceId/locations",
        ) {
            header("X-API-Key", apiConfig.apiKey)
            from?.let { parameter("from", it) }
            to?.let { parameter("to", it) }
            cursor?.let { parameter("cursor", it) }
            limit?.let { parameter("limit", it) }
            order?.let { parameter("order", it) }
            tolerance?.let { parameter("tolerance", it) }
        }.body()

        Timber.i(
            "Fetched ${response.locations.size} locations for device: $deviceId, hasMore=${response.pagination.hasMore}",
        )
        Result.success(response)
    } catch (e: Exception) {
        Timber.e(e, "Failed to fetch location history for deviceId=$deviceId")
        Result.failure(e)
    }

    /**
     * Delete a device
     * DELETE /api/v1/devices/{deviceId}
     */
    override suspend fun deleteDevice(deviceId: String): Result<Unit> = try {
        Timber.d("Deleting device: $deviceId")

        httpClient.delete("${apiConfig.baseUrl}/api/v1/devices/$deviceId") {
            header("X-API-Key", apiConfig.apiKey)
        }

        Timber.i("Device deleted: $deviceId")
        Result.success(Unit)
    } catch (e: Exception) {
        Timber.e(e, "Failed to delete device: $deviceId")
        Result.failure(e)
    }

    // API Compatibility: GDPR Data Export/Delete

    /**
     * Export device data for GDPR compliance
     * GET /api/v1/devices/{deviceId}/data-export
     */
    override suspend fun exportDeviceData(deviceId: String): Result<DataExportResponse> = try {
        Timber.d("Requesting data export for device: $deviceId")

        val response: DataExportResponse = httpClient.get(
            "${apiConfig.baseUrl}/api/v1/devices/$deviceId/data-export",
        ) {
            header("X-API-Key", apiConfig.apiKey)
        }.body()

        Timber.i("Data export initiated for device: $deviceId, url=${response.exportUrl}")
        Result.success(response)
    } catch (e: Exception) {
        Timber.e(e, "Failed to request data export for device: $deviceId")
        Result.failure(e)
    }

    /**
     * Delete all device data for GDPR compliance
     * DELETE /api/v1/devices/{deviceId}/data
     */
    override suspend fun deleteDeviceData(deviceId: String): Result<Unit> = try {
        Timber.d("Deleting all data for device: $deviceId")

        httpClient.delete("${apiConfig.baseUrl}/api/v1/devices/$deviceId/data") {
            header("X-API-Key", apiConfig.apiKey)
        }

        Timber.i("All data deleted for device: $deviceId")
        Result.success(Unit)
    } catch (e: Exception) {
        Timber.e(e, "Failed to delete data for device: $deviceId")
        Result.failure(e)
    }

    // ============================================================================
    // Story E10.6: Device Binding API Implementation
    // ============================================================================

    /**
     * Story E10.6 Task 2: Link device to user account
     * POST /api/v1/users/{userId}/devices/{deviceId}/link
     */
    override suspend fun linkDevice(
        userId: String,
        deviceId: String,
        displayName: String?,
        isPrimary: Boolean,
        accessToken: String,
    ): Result<LinkedDeviceResponse> = try {
        Timber.d("Linking device $deviceId to user $userId")

        val response: LinkedDeviceResponse = httpClient.post(
            "${apiConfig.baseUrl}/api/v1/users/$userId/devices/$deviceId/link",
        ) {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $accessToken")
            setBody(LinkDeviceRequest(displayName = displayName, isPrimary = isPrimary))
        }.body()

        Timber.i("Device linked successfully: $deviceId to user $userId")
        Result.success(response)
    } catch (e: Exception) {
        Timber.e(e, "Failed to link device $deviceId to user $userId")
        Result.failure(e)
    }

    /**
     * Story E10.6 Task 2: Unlink device from user account
     * DELETE /api/v1/users/{userId}/devices/{deviceId}/unlink
     */
    override suspend fun unlinkDevice(
        userId: String,
        deviceId: String,
        accessToken: String,
    ): Result<UnlinkDeviceResponse> = try {
        Timber.d("Unlinking device $deviceId from user $userId")

        val response: UnlinkDeviceResponse = httpClient.delete(
            "${apiConfig.baseUrl}/api/v1/users/$userId/devices/$deviceId/unlink",
        ) {
            header("Authorization", "Bearer $accessToken")
        }.body()

        Timber.i("Device unlinked successfully: $deviceId from user $userId")
        Result.success(response)
    } catch (e: Exception) {
        Timber.e(e, "Failed to unlink device $deviceId from user $userId")
        Result.failure(e)
    }

    /**
     * Story E10.6 Task 2: Transfer device ownership
     * POST /api/v1/users/{userId}/devices/{deviceId}/transfer
     */
    override suspend fun transferDevice(
        userId: String,
        deviceId: String,
        newOwnerId: String,
        accessToken: String,
    ): Result<TransferDeviceResponse> = try {
        Timber.d("Transferring device $deviceId from user $userId to $newOwnerId")

        val response: TransferDeviceResponse = httpClient.post(
            "${apiConfig.baseUrl}/api/v1/users/$userId/devices/$deviceId/transfer",
        ) {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $accessToken")
            setBody(TransferDeviceRequest(newOwnerId = newOwnerId))
        }.body()

        Timber.i("Device transferred successfully: $deviceId to new owner $newOwnerId")
        Result.success(response)
    } catch (e: Exception) {
        Timber.e(e, "Failed to transfer device $deviceId to $newOwnerId")
        Result.failure(e)
    }

    /**
     * Story E10.6 Task 2: Get user's devices
     * GET /api/v1/users/{userId}/devices
     */
    override suspend fun getUserDevices(
        userId: String,
        includeInactive: Boolean,
        accessToken: String,
    ): Result<List<UserDevice>> = try {
        Timber.d("Fetching devices for user $userId, includeInactive=$includeInactive")

        val response: ListUserDevicesResponse = httpClient.get(
            "${apiConfig.baseUrl}/api/v1/users/$userId/devices",
        ) {
            header("Authorization", "Bearer $accessToken")
            parameter("includeInactive", includeInactive)
        }.body()

        val devices = response.devices.map { it.toDomain() }
        Timber.i("Fetched ${devices.size} devices for user $userId")
        Result.success(devices)
    } catch (e: Exception) {
        Timber.e(e, "Failed to fetch devices for user $userId")
        Result.failure(e)
    }
}

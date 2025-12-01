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
import three.two.bit.phonemanager.domain.model.Device
import three.two.bit.phonemanager.domain.model.UserDevice
import three.two.bit.phonemanager.domain.model.DeviceSettings
import three.two.bit.phonemanager.network.models.DataExportResponse
import three.two.bit.phonemanager.network.models.DeviceRegistrationRequest
import three.two.bit.phonemanager.network.models.DeviceRegistrationResponse
import three.two.bit.phonemanager.network.models.DeviceSettingsResponse
import three.two.bit.phonemanager.network.models.DevicesResponse
import three.two.bit.phonemanager.network.models.LinkDeviceRequest
import three.two.bit.phonemanager.network.models.LinkedDeviceResponse
import three.two.bit.phonemanager.network.models.ListUserDevicesResponse
import three.two.bit.phonemanager.network.models.LocationHistoryResponse
import three.two.bit.phonemanager.network.models.TransferDeviceRequest
import three.two.bit.phonemanager.network.models.TransferDeviceResponse
import three.two.bit.phonemanager.network.models.UnlinkDeviceResponse
import three.two.bit.phonemanager.network.models.AdminDeviceSettingsResponse
import three.two.bit.phonemanager.network.models.BulkUpdateResponse
import three.two.bit.phonemanager.network.models.BulkUpdateSettingsRequest
import three.two.bit.phonemanager.network.models.LockSettingsRequest
import three.two.bit.phonemanager.network.models.LockSettingsResponse
import three.two.bit.phonemanager.network.models.MemberDevicesResponse
import three.two.bit.phonemanager.network.models.SaveTemplateRequest
import three.two.bit.phonemanager.network.models.SaveTemplateResponse
import three.two.bit.phonemanager.network.models.SettingsHistoryResponse
import three.two.bit.phonemanager.network.models.TemplatesResponse
import three.two.bit.phonemanager.network.models.UpdateDeviceSettingsRequest
import three.two.bit.phonemanager.network.models.UpdateSettingRequest
import three.two.bit.phonemanager.network.models.UpdateSettingResponse
import three.two.bit.phonemanager.network.models.UpdateSettingsResponse
import three.two.bit.phonemanager.network.models.CreateUnlockRequestBody
import three.two.bit.phonemanager.network.models.CreateUnlockRequestResponse
import three.two.bit.phonemanager.network.models.UnlockRequestListResponse
import three.two.bit.phonemanager.network.models.WithdrawUnlockRequestResponse
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

    // ============================================================================
    // Story E12.6: Device Settings Sync API (AC E12.6.2, E12.6.4)
    // ============================================================================

    /**
     * Story E12.6 Task 2: Get device settings and lock states
     * GET /api/v1/devices/{deviceId}/settings
     *
     * AC E12.6.2: Fetch settings and lock states from server
     *
     * @param deviceId The device's UUID
     * @param accessToken JWT access token for authentication
     * @return Result with device settings and locks
     */
    suspend fun getDeviceSettings(
        deviceId: String,
        accessToken: String,
    ): Result<DeviceSettings>

    /**
     * Story E12.6 Task 2: Update a device setting
     * PUT /api/v1/devices/{deviceId}/settings
     *
     * AC E12.6.4: Update setting on server (respects locks)
     *
     * @param deviceId The device's UUID
     * @param key The setting key to update
     * @param value The new value (as string)
     * @param accessToken JWT access token for authentication
     * @return Result with update response (may include 403 if locked)
     */
    suspend fun updateDeviceSetting(
        deviceId: String,
        key: String,
        value: String,
        accessToken: String,
    ): Result<UpdateSettingResponse>

    // ============================================================================
    // Story E12.7: Admin Settings Management API (AC E12.7.1-E12.7.8)
    // ============================================================================

    /**
     * Story E12.7 Task 2: Get member devices for a group (admin only)
     * GET /api/v1/groups/{groupId}/devices
     *
     * AC E12.7.1: Device Settings List Screen
     *
     * @param groupId The group's UUID
     * @param accessToken JWT access token for authentication
     * @return Result with list of member devices
     */
    suspend fun getGroupMemberDevices(
        groupId: String,
        accessToken: String,
    ): Result<MemberDevicesResponse>

    /**
     * Story E12.7 Task 2: Get device settings for admin
     * GET /api/v1/admin/devices/{deviceId}/settings
     *
     * AC E12.7.3: View Remote Settings
     *
     * @param deviceId The device's UUID
     * @param accessToken JWT access token for authentication
     * @return Result with admin-level device settings
     */
    suspend fun getAdminDeviceSettings(
        deviceId: String,
        accessToken: String,
    ): Result<AdminDeviceSettingsResponse>

    /**
     * Story E12.7 Task 2: Update device settings (admin)
     * PUT /api/v1/admin/devices/{deviceId}/settings
     *
     * AC E12.7.4: Modify Remote Settings
     *
     * @param deviceId The device's UUID
     * @param changes Map of setting keys to new values
     * @param notifyUser Whether to send push notification to user
     * @param accessToken JWT access token for authentication
     * @return Result with update response
     */
    suspend fun updateAdminDeviceSettings(
        deviceId: String,
        changes: Map<String, Any>,
        notifyUser: Boolean = true,
        accessToken: String,
    ): Result<UpdateSettingsResponse>

    /**
     * Story E12.7 Task 2: Lock/unlock settings (admin)
     * PUT /api/v1/admin/devices/{deviceId}/settings/locks
     *
     * AC E12.7.5: Lock/Unlock Settings
     *
     * @param deviceId The device's UUID
     * @param settingKeys List of setting keys to lock/unlock
     * @param lock True to lock, false to unlock
     * @param accessToken JWT access token for authentication
     * @return Result with lock operation response
     */
    suspend fun lockDeviceSettings(
        deviceId: String,
        settingKeys: List<String>,
        lock: Boolean,
        accessToken: String,
    ): Result<LockSettingsResponse>

    /**
     * Story E12.7 Task 2: Get settings history (audit trail)
     * GET /api/v1/admin/devices/{deviceId}/settings/history
     *
     * AC E12.7.8: Audit Trail
     *
     * @param deviceId The device's UUID
     * @param limit Number of entries to return
     * @param offset Starting offset for pagination
     * @param accessToken JWT access token for authentication
     * @return Result with settings change history
     */
    suspend fun getSettingsHistory(
        deviceId: String,
        limit: Int = 50,
        offset: Int = 0,
        accessToken: String,
    ): Result<SettingsHistoryResponse>

    /**
     * Story E12.7 Task 2: Bulk update settings across devices
     * POST /api/v1/admin/devices/bulk-update
     *
     * AC E12.7.6: Bulk Settings Application
     *
     * @param deviceIds List of device UUIDs to update
     * @param settings Map of setting keys to new values
     * @param locks List of setting keys to lock (optional)
     * @param notifyUsers Whether to send push notifications
     * @param accessToken JWT access token for authentication
     * @return Result with bulk update results
     */
    suspend fun bulkUpdateSettings(
        deviceIds: List<String>,
        settings: Map<String, Any>,
        locks: List<String>? = null,
        notifyUsers: Boolean = true,
        accessToken: String,
    ): Result<BulkUpdateResponse>

    /**
     * Story E12.7 Task 2: Get settings templates
     * GET /api/v1/settings/templates
     *
     * AC E12.7.7: Settings Templates
     *
     * @param accessToken JWT access token for authentication
     * @return Result with list of templates
     */
    suspend fun getSettingsTemplates(
        accessToken: String,
    ): Result<TemplatesResponse>

    /**
     * Story E12.7 Task 2: Save settings template
     * POST /api/v1/settings/templates
     *
     * AC E12.7.7: Settings Templates
     *
     * @param name Template name
     * @param description Template description
     * @param settings Map of setting keys to values
     * @param lockedSettings List of settings to lock when applying
     * @param isShared Whether other admins can use this template
     * @param accessToken JWT access token for authentication
     * @return Result with saved template
     */
    suspend fun saveSettingsTemplate(
        name: String,
        description: String?,
        settings: Map<String, Any>,
        lockedSettings: List<String>,
        isShared: Boolean = false,
        accessToken: String,
    ): Result<SaveTemplateResponse>

    /**
     * Story E12.7 Task 2: Delete settings template
     * DELETE /api/v1/settings/templates/{templateId}
     *
     * AC E12.7.7: Settings Templates
     *
     * @param templateId The template's UUID
     * @param accessToken JWT access token for authentication
     * @return Result with success/failure
     */
    suspend fun deleteSettingsTemplate(
        templateId: String,
        accessToken: String,
    ): Result<Unit>

    // ============================================================================
    // Story E12.8: Unlock Request API (AC E12.8.1-E12.8.8)
    // ============================================================================

    /**
     * Story E12.8 Task 2: Create unlock request
     * POST /api/v1/devices/{deviceId}/settings/unlock-requests
     *
     * AC E12.8.2: Submit Unlock Request
     *
     * @param deviceId The device's UUID
     * @param settingKey The setting key to request unlock for
     * @param reason User-provided reason for the unlock request
     * @param accessToken JWT access token for authentication
     * @return Result with created unlock request
     */
    suspend fun createUnlockRequest(
        deviceId: String,
        settingKey: String,
        reason: String,
        accessToken: String,
    ): Result<CreateUnlockRequestResponse>

    /**
     * Story E12.8 Task 2: Get user's unlock requests
     * GET /api/v1/devices/{deviceId}/settings/unlock-requests
     *
     * AC E12.8.3: View My Unlock Requests
     *
     * @param deviceId The device's UUID
     * @param status Optional filter by status (pending, approved, denied, withdrawn)
     * @param accessToken JWT access token for authentication
     * @return Result with list of unlock requests
     */
    suspend fun getUnlockRequests(
        deviceId: String,
        status: String? = null,
        accessToken: String,
    ): Result<UnlockRequestListResponse>

    /**
     * Story E12.8 Task 2: Withdraw unlock request
     * DELETE /api/v1/unlock-requests/{requestId}
     *
     * AC E12.8.4: Withdraw Unlock Request
     *
     * @param requestId The unlock request's UUID
     * @param accessToken JWT access token for authentication
     * @return Result with withdrawal confirmation
     */
    suspend fun withdrawUnlockRequest(
        requestId: String,
        accessToken: String,
    ): Result<WithdrawUnlockRequestResponse>
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

    // ============================================================================
    // Story E12.6: Device Settings Sync API Implementation
    // ============================================================================

    /**
     * Story E12.6 Task 2: Get device settings and lock states
     * GET /api/v1/devices/{deviceId}/settings
     */
    override suspend fun getDeviceSettings(
        deviceId: String,
        accessToken: String,
    ): Result<DeviceSettings> = try {
        Timber.d("Fetching settings for device $deviceId")

        val response: DeviceSettingsResponse = httpClient.get(
            "${apiConfig.baseUrl}/api/v1/devices/$deviceId/settings",
        ) {
            header("Authorization", "Bearer $accessToken")
        }.body()

        val settings = response.toDomain()
        Timber.i("Fetched settings for device $deviceId, ${settings.lockedCount()} locked")
        Result.success(settings)
    } catch (e: Exception) {
        Timber.e(e, "Failed to fetch settings for device $deviceId")
        Result.failure(e)
    }

    /**
     * Story E12.6 Task 2: Update a device setting
     * PUT /api/v1/devices/{deviceId}/settings
     */
    override suspend fun updateDeviceSetting(
        deviceId: String,
        key: String,
        value: String,
        accessToken: String,
    ): Result<UpdateSettingResponse> = try {
        Timber.d("Updating setting $key for device $deviceId")

        val response: UpdateSettingResponse = httpClient.put(
            "${apiConfig.baseUrl}/api/v1/devices/$deviceId/settings",
        ) {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $accessToken")
            setBody(UpdateSettingRequest(key = key, value = value))
        }.body()

        if (response.success) {
            Timber.i("Successfully updated setting $key for device $deviceId")
        } else {
            Timber.w("Failed to update setting $key: ${response.error}, locked=${response.isLocked}")
        }
        Result.success(response)
    } catch (e: Exception) {
        Timber.e(e, "Failed to update setting $key for device $deviceId")
        Result.failure(e)
    }

    // ============================================================================
    // Story E12.7: Admin Settings Management API Implementation
    // ============================================================================

    /**
     * Story E12.7 Task 2: Get member devices for a group
     * GET /api/v1/groups/{groupId}/devices
     */
    override suspend fun getGroupMemberDevices(
        groupId: String,
        accessToken: String,
    ): Result<MemberDevicesResponse> = try {
        Timber.d("Fetching member devices for group $groupId")

        val response: MemberDevicesResponse = httpClient.get(
            "${apiConfig.baseUrl}/api/v1/groups/$groupId/devices",
        ) {
            header("Authorization", "Bearer $accessToken")
        }.body()

        Timber.i("Fetched ${response.totalCount} member devices for group $groupId")
        Result.success(response)
    } catch (e: Exception) {
        Timber.e(e, "Failed to fetch member devices for group $groupId")
        Result.failure(e)
    }

    /**
     * Story E12.7 Task 2: Get device settings for admin
     * GET /api/v1/admin/devices/{deviceId}/settings
     */
    override suspend fun getAdminDeviceSettings(
        deviceId: String,
        accessToken: String,
    ): Result<AdminDeviceSettingsResponse> = try {
        Timber.d("Fetching admin settings for device $deviceId")

        val response: AdminDeviceSettingsResponse = httpClient.get(
            "${apiConfig.baseUrl}/api/v1/admin/devices/$deviceId/settings",
        ) {
            header("Authorization", "Bearer $accessToken")
        }.body()

        Timber.i("Fetched admin settings for device $deviceId")
        Result.success(response)
    } catch (e: Exception) {
        Timber.e(e, "Failed to fetch admin settings for device $deviceId")
        Result.failure(e)
    }

    /**
     * Story E12.7 Task 2: Update device settings (admin)
     * PUT /api/v1/admin/devices/{deviceId}/settings
     */
    override suspend fun updateAdminDeviceSettings(
        deviceId: String,
        changes: Map<String, Any>,
        notifyUser: Boolean,
        accessToken: String,
    ): Result<UpdateSettingsResponse> = try {
        Timber.d("Updating admin settings for device $deviceId: ${changes.keys}")

        val response: UpdateSettingsResponse = httpClient.put(
            "${apiConfig.baseUrl}/api/v1/admin/devices/$deviceId/settings",
        ) {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $accessToken")
            setBody(UpdateDeviceSettingsRequest(changes = changes, notifyUser = notifyUser))
        }.body()

        if (response.success) {
            Timber.i("Successfully updated admin settings for device $deviceId")
        } else {
            Timber.w("Failed to update admin settings: ${response.error}")
        }
        Result.success(response)
    } catch (e: Exception) {
        Timber.e(e, "Failed to update admin settings for device $deviceId")
        Result.failure(e)
    }

    /**
     * Story E12.7 Task 2: Lock/unlock settings
     * PUT /api/v1/admin/devices/{deviceId}/settings/locks
     */
    override suspend fun lockDeviceSettings(
        deviceId: String,
        settingKeys: List<String>,
        lock: Boolean,
        accessToken: String,
    ): Result<LockSettingsResponse> = try {
        val action = if (lock) "lock" else "unlock"
        Timber.d("${action}ing settings $settingKeys for device $deviceId")

        val response: LockSettingsResponse = httpClient.put(
            "${apiConfig.baseUrl}/api/v1/admin/devices/$deviceId/settings/locks",
        ) {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $accessToken")
            setBody(LockSettingsRequest(settingKeys = settingKeys, lock = lock))
        }.body()

        if (response.success) {
            Timber.i("Successfully ${action}ed ${settingKeys.size} settings for device $deviceId")
        } else {
            Timber.w("Failed to $action settings: ${response.error}")
        }
        Result.success(response)
    } catch (e: Exception) {
        Timber.e(e, "Failed to lock/unlock settings for device $deviceId")
        Result.failure(e)
    }

    /**
     * Story E12.7 Task 2: Get settings history
     * GET /api/v1/admin/devices/{deviceId}/settings/history
     */
    override suspend fun getSettingsHistory(
        deviceId: String,
        limit: Int,
        offset: Int,
        accessToken: String,
    ): Result<SettingsHistoryResponse> = try {
        Timber.d("Fetching settings history for device $deviceId")

        val response: SettingsHistoryResponse = httpClient.get(
            "${apiConfig.baseUrl}/api/v1/admin/devices/$deviceId/settings/history",
        ) {
            header("Authorization", "Bearer $accessToken")
            parameter("limit", limit)
            parameter("offset", offset)
        }.body()

        Timber.i("Fetched ${response.changes.size} history entries for device $deviceId")
        Result.success(response)
    } catch (e: Exception) {
        Timber.e(e, "Failed to fetch settings history for device $deviceId")
        Result.failure(e)
    }

    /**
     * Story E12.7 Task 2: Bulk update settings
     * POST /api/v1/admin/devices/bulk-update
     */
    override suspend fun bulkUpdateSettings(
        deviceIds: List<String>,
        settings: Map<String, Any>,
        locks: List<String>?,
        notifyUsers: Boolean,
        accessToken: String,
    ): Result<BulkUpdateResponse> = try {
        Timber.d("Bulk updating settings for ${deviceIds.size} devices")

        val response: BulkUpdateResponse = httpClient.post(
            "${apiConfig.baseUrl}/api/v1/admin/devices/bulk-update",
        ) {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $accessToken")
            setBody(
                BulkUpdateSettingsRequest(
                    deviceIds = deviceIds,
                    settings = settings,
                    locks = locks,
                    notifyUsers = notifyUsers,
                ),
            )
        }.body()

        Timber.i(
            "Bulk update complete: ${response.successful.size} successful, ${response.failed.size} failed",
        )
        Result.success(response)
    } catch (e: Exception) {
        Timber.e(e, "Failed to bulk update settings")
        Result.failure(e)
    }

    /**
     * Story E12.7 Task 2: Get settings templates
     * GET /api/v1/settings/templates
     */
    override suspend fun getSettingsTemplates(
        accessToken: String,
    ): Result<TemplatesResponse> = try {
        Timber.d("Fetching settings templates")

        val response: TemplatesResponse = httpClient.get(
            "${apiConfig.baseUrl}/api/v1/settings/templates",
        ) {
            header("Authorization", "Bearer $accessToken")
        }.body()

        Timber.i("Fetched ${response.templates.size} settings templates")
        Result.success(response)
    } catch (e: Exception) {
        Timber.e(e, "Failed to fetch settings templates")
        Result.failure(e)
    }

    /**
     * Story E12.7 Task 2: Save settings template
     * POST /api/v1/settings/templates
     */
    override suspend fun saveSettingsTemplate(
        name: String,
        description: String?,
        settings: Map<String, Any>,
        lockedSettings: List<String>,
        isShared: Boolean,
        accessToken: String,
    ): Result<SaveTemplateResponse> = try {
        Timber.d("Saving settings template: $name")

        val response: SaveTemplateResponse = httpClient.post(
            "${apiConfig.baseUrl}/api/v1/settings/templates",
        ) {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $accessToken")
            setBody(
                SaveTemplateRequest(
                    name = name,
                    description = description,
                    settings = settings,
                    lockedSettings = lockedSettings,
                    isShared = isShared,
                ),
            )
        }.body()

        if (response.success) {
            Timber.i("Successfully saved template: $name")
        } else {
            Timber.w("Failed to save template: ${response.error}")
        }
        Result.success(response)
    } catch (e: Exception) {
        Timber.e(e, "Failed to save settings template: $name")
        Result.failure(e)
    }

    /**
     * Story E12.7 Task 2: Delete settings template
     * DELETE /api/v1/settings/templates/{templateId}
     */
    override suspend fun deleteSettingsTemplate(
        templateId: String,
        accessToken: String,
    ): Result<Unit> = try {
        Timber.d("Deleting settings template: $templateId")

        httpClient.delete("${apiConfig.baseUrl}/api/v1/settings/templates/$templateId") {
            header("Authorization", "Bearer $accessToken")
        }

        Timber.i("Successfully deleted template: $templateId")
        Result.success(Unit)
    } catch (e: Exception) {
        Timber.e(e, "Failed to delete settings template: $templateId")
        Result.failure(e)
    }

    // ============================================================================
    // Story E12.8: Unlock Request API Implementation
    // ============================================================================

    /**
     * Story E12.8 Task 2: Create unlock request
     * POST /api/v1/devices/{deviceId}/settings/unlock-requests
     */
    override suspend fun createUnlockRequest(
        deviceId: String,
        settingKey: String,
        reason: String,
        accessToken: String,
    ): Result<CreateUnlockRequestResponse> = try {
        Timber.d("Creating unlock request for device $deviceId, setting $settingKey")

        val response: CreateUnlockRequestResponse = httpClient.post(
            "${apiConfig.baseUrl}/api/v1/devices/$deviceId/settings/unlock-requests",
        ) {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $accessToken")
            setBody(CreateUnlockRequestBody(settingKey = settingKey, reason = reason))
        }.body()

        Timber.i("Successfully created unlock request: ${response.id}")
        Result.success(response)
    } catch (e: Exception) {
        Timber.e(e, "Failed to create unlock request for device $deviceId")
        Result.failure(e)
    }

    /**
     * Story E12.8 Task 2: Get user's unlock requests
     * GET /api/v1/devices/{deviceId}/settings/unlock-requests
     */
    override suspend fun getUnlockRequests(
        deviceId: String,
        status: String?,
        accessToken: String,
    ): Result<UnlockRequestListResponse> = try {
        Timber.d("Fetching unlock requests for device $deviceId, status=$status")

        val response: UnlockRequestListResponse = httpClient.get(
            "${apiConfig.baseUrl}/api/v1/devices/$deviceId/settings/unlock-requests",
        ) {
            header("Authorization", "Bearer $accessToken")
            status?.let { parameter("status", it) }
        }.body()

        Timber.i("Fetched ${response.requests.size} unlock requests for device $deviceId")
        Result.success(response)
    } catch (e: Exception) {
        Timber.e(e, "Failed to fetch unlock requests for device $deviceId")
        Result.failure(e)
    }

    /**
     * Story E12.8 Task 2: Withdraw unlock request
     * DELETE /api/v1/unlock-requests/{requestId}
     */
    override suspend fun withdrawUnlockRequest(
        requestId: String,
        accessToken: String,
    ): Result<WithdrawUnlockRequestResponse> = try {
        Timber.d("Withdrawing unlock request: $requestId")

        val response: WithdrawUnlockRequestResponse = httpClient.delete(
            "${apiConfig.baseUrl}/api/v1/unlock-requests/$requestId",
        ) {
            header("Authorization", "Bearer $accessToken")
        }.body()

        if (response.success) {
            Timber.i("Successfully withdrew unlock request: $requestId")
        } else {
            Timber.w("Failed to withdraw unlock request: ${response.error}")
        }
        Result.success(response)
    } catch (e: Exception) {
        Timber.e(e, "Failed to withdraw unlock request: $requestId")
        Result.failure(e)
    }
}

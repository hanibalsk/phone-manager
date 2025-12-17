package three.two.bit.phonemanager.network.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import three.two.bit.phonemanager.domain.model.Device
import three.two.bit.phonemanager.domain.model.DeviceLocation
import kotlin.time.Instant

/**
 * Story E1.1: Device registration request payload
 *
 * Sent to POST /api/devices/register
 */
@Serializable
data class DeviceRegistrationRequest(
    @SerialName("device_id") val deviceId: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("group_id") val groupId: String,
    val platform: String = "android",
    @SerialName("fcm_token") val fcmToken: String? = null,
)

/**
 * Story E1.1: Device registration response
 *
 * Received from POST /api/devices/register
 */
@Serializable
data class DeviceRegistrationResponse(
    @SerialName("device_id") val deviceId: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("group_id") val groupId: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
)

/**
 * Story E1.2: Device info for group member display
 */
@Serializable
data class DeviceInfo(
    @SerialName("device_id") val deviceId: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("group_id") val groupId: String,
    @SerialName("last_location") val lastLocation: DeviceLastLocation? = null,
)

/**
 * Story E1.2: Last known location of a device
 */
@Serializable
data class DeviceLastLocation(
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val accuracy: Float? = null,
)

/**
 * Story E1.2: Response for GET /api/devices?groupId={id}
 *
 * Container for list of devices in a group
 */
@Serializable
data class DevicesResponse(val devices: List<DeviceDto>)

/**
 * Story E1.2: Device data transfer object
 *
 * Represents a device in the API response
 */
@Serializable
data class DeviceDto(
    @SerialName("device_id") val deviceId: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("last_location") val lastLocation: LocationDto? = null,
    @SerialName("last_seen_at") val lastSeenAt: String? = null,
)

/**
 * Story E1.2: Location data transfer object
 *
 * Represents a geographic location in the API response
 */
@Serializable
data class LocationDto(val latitude: Double, val longitude: Double, val timestamp: String)

/**
 * Maps DeviceDto from API response to Device domain model
 */
fun DeviceDto.toDomain(): Device = Device(
    deviceId = deviceId,
    displayName = displayName,
    lastLocation =
    lastLocation?.let {
        DeviceLocation(
            latitude = it.latitude,
            longitude = it.longitude,
            timestamp = Instant.parse(it.timestamp),
        )
    },
    lastSeenAt = lastSeenAt?.let { Instant.parse(it) },
)

// API Compatibility: GDPR Data Export/Delete

/**
 * GDPR data export response
 * GET /api/v1/devices/{deviceId}/data-export
 */
@Serializable
data class DataExportResponse(
    @SerialName("device_id") val deviceId: String,
    @SerialName("export_url") val exportUrl: String,
    @SerialName("expires_at") val expiresAt: String,
    val format: String = "json",
)

// ============================================================================
// Story E10.6: Device Binding API Models
// ============================================================================

/**
 * Story E10.6 Task 2: Request body for linking a device to a user
 * POST /api/v1/users/{userId}/devices/{deviceId}/link
 */
@Serializable
data class LinkDeviceRequest(
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("is_primary") val isPrimary: Boolean = false,
)

/**
 * Story E10.6 Task 2: Response for device link operation
 * Note: Fields are nullable to handle cases where API returns partial response
 */
@Serializable
data class LinkedDeviceResponse(
    val device: LinkedDeviceInfo? = null,
    val linked: Boolean = false,
    // Alternative response fields the API might return
    val success: Boolean? = null,
    val message: String? = null,
)

/**
 * Story E10.6 Task 2: Device info in link response
 */
@Serializable
data class LinkedDeviceInfo(
    val id: Long,
    @SerialName("device_uuid") val deviceUuid: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("owner_user_id") val ownerUserId: String,
    @SerialName("is_primary") val isPrimary: Boolean,
    @SerialName("linked_at") val linkedAt: String,
)

/**
 * Story E10.6 Task 2: Response for list user devices
 * GET /api/v1/users/{userId}/devices
 */
@Serializable
data class ListUserDevicesResponse(val devices: List<UserDeviceDto>, val count: Int)

/**
 * Story E10.6 Task 2: Device DTO in user's device list
 */
@Serializable
data class UserDeviceDto(
    val id: Long,
    @SerialName("device_uuid") val deviceUuid: String,
    @SerialName("display_name") val displayName: String,
    val platform: String,
    @SerialName("is_primary") val isPrimary: Boolean,
    val active: Boolean,
    @SerialName("linked_at") val linkedAt: String?,
    @SerialName("last_seen_at") val lastSeenAt: String?,
)

/**
 * Story E10.6 Task 2: Response for unlink device operation
 * DELETE /api/v1/users/{userId}/devices/{deviceId}/unlink
 */
@Serializable
data class UnlinkDeviceResponse(@SerialName("device_uuid") val deviceUuid: String, val unlinked: Boolean)

/**
 * Story E10.6 Task 2: Request body for transferring device ownership
 * POST /api/v1/users/{userId}/devices/{deviceId}/transfer
 */
@Serializable
data class TransferDeviceRequest(@SerialName("new_owner_id") val newOwnerId: String)

/**
 * Story E10.6 Task 2: Response for transfer device operation
 */
@Serializable
data class TransferDeviceResponse(
    val device: LinkedDeviceInfo,
    @SerialName("previous_owner_id") val previousOwnerId: String,
    @SerialName("new_owner_id") val newOwnerId: String,
    val transferred: Boolean,
)

/**
 * Story E10.6 Task 2: Maps UserDeviceDto from API response to UserDevice domain model
 */
fun UserDeviceDto.toDomain(): three.two.bit.phonemanager.domain.model.UserDevice =
    three.two.bit.phonemanager.domain.model.UserDevice(
        id = id,
        deviceUuid = deviceUuid,
        displayName = displayName,
        platform = platform,
        isPrimary = isPrimary,
        active = active,
        linkedAt = linkedAt?.let { Instant.parse(it) },
        lastSeenAt = lastSeenAt?.let { Instant.parse(it) },
    )

package three.two.bit.phonemanager.network.models

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import three.two.bit.phonemanager.domain.model.Device
import three.two.bit.phonemanager.domain.model.DeviceLocation

/**
 * Story E1.1: Device registration request payload
 *
 * Sent to POST /api/devices/register
 */
@Serializable
data class DeviceRegistrationRequest(
    val deviceId: String,
    val displayName: String,
    val groupId: String,
    val platform: String = "android",
    val fcmToken: String? = null,
)

/**
 * Story E1.1: Device registration response
 *
 * Received from POST /api/devices/register
 */
@Serializable
data class DeviceRegistrationResponse(
    val deviceId: String,
    val displayName: String,
    val groupId: String,
    val createdAt: String,
    val updatedAt: String,
)

/**
 * Story E1.2: Device info for group member display
 */
@Serializable
data class DeviceInfo(
    val deviceId: String,
    val displayName: String,
    val groupId: String,
    val lastLocation: DeviceLastLocation? = null,
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
    val deviceId: String,
    val displayName: String,
    val lastLocation: LocationDto? = null,
    val lastSeenAt: String? = null,
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
    val deviceId: String,
    val exportUrl: String,
    val expiresAt: String,
    val format: String = "json",
)

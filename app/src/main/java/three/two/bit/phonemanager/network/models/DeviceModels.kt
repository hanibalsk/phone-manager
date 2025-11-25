package three.two.bit.phonemanager.network.models

import kotlinx.serialization.Serializable

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

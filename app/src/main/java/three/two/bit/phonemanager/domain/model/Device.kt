package three.two.bit.phonemanager.domain.model

import kotlinx.datetime.Instant

/**
 * Represents a device in a group for location sharing.
 *
 * @property deviceId Unique identifier for the device
 * @property displayName Human-readable name for the device (e.g., "Martin's Phone")
 * @property lastLocation Last known location of the device, null if no location data available
 * @property lastSeenAt Timestamp of when the device was last seen online, null if never seen
 * @property ownerId User ID of the device owner, null if not linked to any account (Story E10.6)
 * @property ownerEmail Email of the device owner, null if not linked (Story E10.6)
 * @property isLinkedToAccount Whether the device is linked to a user account (Story E10.6)
 * @property isPrimary Whether this is the user's primary device (Story E10.6)
 * @property linkedAt When the device was linked to the account (Story E10.6)
 * @property platform Device platform (e.g., "android", "ios") (Story E10.6)
 * @property groupId Group the device belongs to (Story E10.6)
 */
data class Device(
    val deviceId: String,
    val displayName: String,
    val lastLocation: DeviceLocation? = null,
    val lastSeenAt: Instant? = null,
    val ownerId: String? = null,
    val ownerEmail: String? = null,
    val isLinkedToAccount: Boolean = false,
    val isPrimary: Boolean = false,
    val linkedAt: Instant? = null,
    val platform: String = "android",
    val groupId: String? = null,
) {
    /**
     * Story E10.6 AC E10.6.1: Helper to check if this is the current device
     *
     * @param currentDeviceId The device ID of the current device running the app
     * @return true if this device is the current device
     */
    fun isCurrentDevice(currentDeviceId: String): Boolean = deviceId == currentDeviceId
}

/**
 * Represents a geographic location of a device.
 *
 * @property latitude Latitude coordinate in degrees (-90 to 90)
 * @property longitude Longitude coordinate in degrees (-180 to 180)
 * @property timestamp When this location was recorded
 */
data class DeviceLocation(val latitude: Double, val longitude: Double, val timestamp: Instant)

/**
 * Story E10.6: Represents a device owned by a user from the list_user_devices endpoint.
 *
 * This is used for the "My Devices" screen where users can manage their linked devices.
 *
 * @property id Internal database ID
 * @property deviceUuid External device UUID
 * @property displayName Human-readable name for the device
 * @property platform Device platform (e.g., "android", "ios")
 * @property isPrimary Whether this is the user's primary device
 * @property active Whether the device is currently active
 * @property linkedAt When the device was linked to the account
 * @property lastSeenAt When the device was last active
 */
data class UserDevice(
    val id: Long,
    val deviceUuid: String,
    val displayName: String,
    val platform: String,
    val isPrimary: Boolean,
    val active: Boolean,
    val linkedAt: Instant?,
    val lastSeenAt: Instant?,
) {
    /**
     * Helper to check if this is the current device
     *
     * @param currentDeviceId The device ID of the current device running the app
     * @return true if this device is the current device
     */
    fun isCurrentDevice(currentDeviceId: String): Boolean = deviceUuid == currentDeviceId
}

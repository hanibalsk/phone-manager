package three.two.bit.phonemanager.domain.model

import kotlinx.datetime.Instant

/**
 * Represents a device in a group for location sharing.
 *
 * @property deviceId Unique identifier for the device
 * @property displayName Human-readable name for the device (e.g., "Martin's Phone")
 * @property lastLocation Last known location of the device, null if no location data available
 * @property lastSeenAt Timestamp of when the device was last seen online, null if never seen
 */
data class Device(
    val deviceId: String,
    val displayName: String,
    val lastLocation: DeviceLocation? = null,
    val lastSeenAt: Instant? = null,
)

/**
 * Represents a geographic location of a device.
 *
 * @property latitude Latitude coordinate in degrees (-90 to 90)
 * @property longitude Longitude coordinate in degrees (-180 to 180)
 * @property timestamp When this location was recorded
 */
data class DeviceLocation(val latitude: Double, val longitude: Double, val timestamp: Instant)

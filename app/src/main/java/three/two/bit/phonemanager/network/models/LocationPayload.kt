package three.two.bit.phonemanager.network.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import three.two.bit.phonemanager.data.model.LocationEntity

/**
 * Story 0.2.2: LocationPayload - JSON payload for location transmission
 *
 * Serializable data class for sending location data to backend API
 */
@Serializable
data class LocationPayload(
    @SerialName("device_id") val deviceId: String,
    val timestamp: Long,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val altitude: Double? = null,
    val bearing: Float? = null,
    val speed: Float? = null,
    val provider: String? = null,
    @SerialName("battery_level") val batteryLevel: Int? = null,
    @SerialName("network_type") val networkType: String? = null,
    // API Compatibility: New fields for movement tracking
    @SerialName("transportation_mode") val transportationMode: String? = null,
    @SerialName("detection_source") val detectionSource: String? = null,
    @SerialName("trip_id") val tripId: String? = null,
)

/**
 * Story 0.2.2: Batch location transmission
 */
@Serializable
data class LocationBatchPayload(
    @SerialName("device_id") val deviceId: String,
    val locations: List<LocationPayload>
)

/**
 * Story 0.2.2: API response models
 */
@Serializable
data class LocationUploadResponse(
    val success: Boolean,
    val message: String? = null,
    @SerialName("processed_count") val processedCount: Int = 0
)

/**
 * Extension function to convert LocationEntity to LocationPayload
 *
 * @param deviceId Device identifier
 * @param transportationMode Current transportation mode (optional)
 * @param detectionSource How mode was detected (optional)
 * @param tripId Associated trip ID (optional)
 */
fun LocationEntity.toPayload(
    deviceId: String,
    transportationMode: String? = null,
    detectionSource: String? = null,
    tripId: String? = null,
): LocationPayload = LocationPayload(
    deviceId = deviceId,
    timestamp = timestamp,
    latitude = latitude,
    longitude = longitude,
    accuracy = accuracy,
    altitude = altitude,
    bearing = bearing,
    speed = speed,
    provider = provider,
    batteryLevel = null, // Will be populated by NetworkManager
    networkType = null, // Will be populated by NetworkManager
    transportationMode = transportationMode,
    detectionSource = detectionSource,
    tripId = tripId,
)

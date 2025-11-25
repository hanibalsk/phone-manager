package com.phonemanager.network.models

import com.phonemanager.data.model.LocationEntity
import kotlinx.serialization.Serializable

/**
 * Story 0.2.2: LocationPayload - JSON payload for location transmission
 *
 * Serializable data class for sending location data to backend API
 */
@Serializable
data class LocationPayload(
    val deviceId: String,
    val timestamp: Long,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val altitude: Double? = null,
    val bearing: Float? = null,
    val speed: Float? = null,
    val provider: String? = null,
    val batteryLevel: Int? = null,
    val networkType: String? = null,
)

/**
 * Story 0.2.2: Batch location transmission
 */
@Serializable
data class LocationBatchPayload(val deviceId: String, val locations: List<LocationPayload>)

/**
 * Story 0.2.2: API response models
 */
@Serializable
data class LocationUploadResponse(val success: Boolean, val message: String? = null, val processedCount: Int = 0)

/**
 * Extension function to convert LocationEntity to LocationPayload
 */
fun LocationEntity.toPayload(deviceId: String): LocationPayload = LocationPayload(
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
)

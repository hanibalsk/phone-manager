package three.two.bit.phonemanager.network.models

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import three.two.bit.phonemanager.domain.model.DeviceSettings
import three.two.bit.phonemanager.domain.model.SettingLock

/**
 * Story E12.6: Network models for device settings sync.
 *
 * API contracts for:
 * - GET /devices/{deviceId}/settings
 * - PUT /devices/{deviceId}/settings
 */

// =============================================================================
// GET /devices/{deviceId}/settings Response
// =============================================================================

/**
 * Story E12.6 (AC E12.6.2): Response from GET /devices/{deviceId}/settings
 */
@Serializable
data class DeviceSettingsResponse(
    val settings: SettingsDto,
    val locks: List<SettingLockDto> = emptyList(),
    @SerialName("last_synced_at")
    val lastSyncedAt: String? = null,
)

/**
 * Story E12.6: Settings values DTO.
 */
@Serializable
data class SettingsDto(
    @SerialName("tracking_enabled")
    val trackingEnabled: Boolean = false,
    @SerialName("tracking_interval_seconds")
    val trackingIntervalSeconds: Int = 300,
    @SerialName("secret_mode_enabled")
    val secretModeEnabled: Boolean = false,
    @SerialName("show_weather_in_notification")
    val showWeatherInNotification: Boolean = true,
    @SerialName("trip_detection_enabled")
    val tripDetectionEnabled: Boolean = true,
    @SerialName("trip_minimum_duration_minutes")
    val tripMinimumDurationMinutes: Int = 2,
    @SerialName("trip_minimum_distance_meters")
    val tripMinimumDistanceMeters: Int = 100,
)

/**
 * Story E12.6 (AC E12.6.1): Lock information DTO.
 */
@Serializable
data class SettingLockDto(
    @SerialName("setting_key")
    val settingKey: String,
    @SerialName("is_locked")
    val isLocked: Boolean,
    @SerialName("locked_by")
    val lockedBy: String? = null,
    @SerialName("locked_at")
    val lockedAt: String? = null,
)

// =============================================================================
// PUT /devices/{deviceId}/settings Request/Response
// =============================================================================

/**
 * Story E12.6 (AC E12.6.4): Request to update a single setting.
 */
@Serializable
data class UpdateSettingRequest(
    val key: String,
    val value: String,
)

/**
 * Story E12.6 (AC E12.6.4): Response from setting update.
 */
@Serializable
data class UpdateSettingResponse(
    val success: Boolean,
    val error: String? = null,
    @SerialName("is_locked")
    val isLocked: Boolean = false,
)

// =============================================================================
// Domain Mappers
// =============================================================================

/**
 * Convert DeviceSettingsResponse to domain model.
 */
fun DeviceSettingsResponse.toDomain(): DeviceSettings {
    val lockMap = locks.associate { lock ->
        lock.settingKey to lock.toDomain()
    }

    val syncedAt = lastSyncedAt?.let {
        try {
            Instant.parse(it)
        } catch (e: Exception) {
            null
        }
    }

    return DeviceSettings(
        trackingEnabled = settings.trackingEnabled,
        trackingIntervalSeconds = settings.trackingIntervalSeconds,
        secretModeEnabled = settings.secretModeEnabled,
        showWeatherInNotification = settings.showWeatherInNotification,
        tripDetectionEnabled = settings.tripDetectionEnabled,
        tripMinimumDurationMinutes = settings.tripMinimumDurationMinutes,
        tripMinimumDistanceMeters = settings.tripMinimumDistanceMeters,
        locks = lockMap,
        lastSyncedAt = syncedAt,
    )
}

/**
 * Convert SettingLockDto to domain model.
 */
fun SettingLockDto.toDomain(): SettingLock {
    val lockedAtInstant = lockedAt?.let {
        try {
            Instant.parse(it)
        } catch (e: Exception) {
            null
        }
    }

    return SettingLock(
        settingKey = settingKey,
        isLocked = isLocked,
        lockedBy = lockedBy,
        lockedAt = lockedAtInstant,
    )
}

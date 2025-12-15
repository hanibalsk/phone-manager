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
 *
 * Backend returns each setting as an object with `value` and `is_locked` properties:
 * {"secret_mode_enabled": {"value": false, "is_locked": false}, ...}
 */
@Serializable
data class SettingsDto(
    @SerialName("tracking_enabled")
    val trackingEnabled: SettingValueDto<Boolean>? = null,
    @SerialName("tracking_interval_seconds")
    val trackingIntervalSeconds: SettingValueDto<Int>? = null,
    @SerialName("secret_mode_enabled")
    val secretModeEnabled: SettingValueDto<Boolean>? = null,
    @SerialName("show_weather_in_notification")
    val showWeatherInNotification: SettingValueDto<Boolean>? = null,
    @SerialName("trip_detection_enabled")
    val tripDetectionEnabled: SettingValueDto<Boolean>? = null,
    @SerialName("trip_minimum_duration_minutes")
    val tripMinimumDurationMinutes: SettingValueDto<Int>? = null,
    @SerialName("trip_minimum_distance_meters")
    val tripMinimumDistanceMeters: SettingValueDto<Int>? = null,
)

/**
 * Story E12.6: Individual setting value wrapper from backend.
 *
 * Each setting is returned as {"value": <T>, "is_locked": bool}
 */
@Serializable
data class SettingValueDto<T>(
    val value: T,
    @SerialName("is_locked")
    val isLocked: Boolean = false,
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
    // Build lock map from both:
    // 1. The `locks` array (legacy/explicit locks)
    // 2. The `is_locked` flag embedded in each setting value
    val lockMap = mutableMapOf<String, SettingLock>()

    // Add locks from the locks array
    locks.forEach { lock ->
        lockMap[lock.settingKey] = lock.toDomain()
    }

    // Add/update locks from embedded is_locked flags in settings
    settings.trackingEnabled?.let {
        if (it.isLocked) lockMap["tracking_enabled"] = SettingLock("tracking_enabled", true, null, null)
    }
    settings.trackingIntervalSeconds?.let {
        if (it.isLocked) lockMap["tracking_interval_seconds"] = SettingLock("tracking_interval_seconds", true, null, null)
    }
    settings.secretModeEnabled?.let {
        if (it.isLocked) lockMap["secret_mode_enabled"] = SettingLock("secret_mode_enabled", true, null, null)
    }
    settings.showWeatherInNotification?.let {
        if (it.isLocked) lockMap["show_weather_in_notification"] = SettingLock("show_weather_in_notification", true, null, null)
    }
    settings.tripDetectionEnabled?.let {
        if (it.isLocked) lockMap["trip_detection_enabled"] = SettingLock("trip_detection_enabled", true, null, null)
    }
    settings.tripMinimumDurationMinutes?.let {
        if (it.isLocked) lockMap["trip_minimum_duration_minutes"] = SettingLock("trip_minimum_duration_minutes", true, null, null)
    }
    settings.tripMinimumDistanceMeters?.let {
        if (it.isLocked) lockMap["trip_minimum_distance_meters"] = SettingLock("trip_minimum_distance_meters", true, null, null)
    }

    val syncedAt = lastSyncedAt?.let {
        try {
            Instant.parse(it)
        } catch (e: Exception) {
            null
        }
    }

    return DeviceSettings(
        trackingEnabled = settings.trackingEnabled?.value ?: false,
        trackingIntervalSeconds = settings.trackingIntervalSeconds?.value ?: 300,
        secretModeEnabled = settings.secretModeEnabled?.value ?: false,
        showWeatherInNotification = settings.showWeatherInNotification?.value ?: true,
        tripDetectionEnabled = settings.tripDetectionEnabled?.value ?: true,
        tripMinimumDurationMinutes = settings.tripMinimumDurationMinutes?.value ?: 2,
        tripMinimumDistanceMeters = settings.tripMinimumDistanceMeters?.value ?: 100,
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

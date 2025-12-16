package three.two.bit.phonemanager.domain.model

import kotlin.time.Instant

/**
 * Story E12.6: Device settings with lock support.
 *
 * Contains all configurable device settings and their lock states.
 * When a setting is locked, only admins can modify it.
 *
 * AC E12.6.1: Lock indicator display support
 * AC E12.6.2: Setting sync from server
 *
 * @property trackingEnabled Whether location tracking is enabled
 * @property trackingIntervalSeconds Interval between location updates in seconds
 * @property secretModeEnabled Whether secret mode is active
 * @property showWeatherInNotification Whether to show weather in notification
 * @property tripDetectionEnabled Whether trip detection is enabled
 * @property tripMinimumDurationMinutes Minimum duration for a valid trip
 * @property tripMinimumDistanceMeters Minimum distance for a valid trip
 * @property locks Map of setting keys to their lock information
 * @property lastSyncedAt When settings were last synced from server
 */
data class DeviceSettings(
    val trackingEnabled: Boolean = false,
    val trackingIntervalSeconds: Int = 300,
    val secretModeEnabled: Boolean = false,
    val showWeatherInNotification: Boolean = true,
    val tripDetectionEnabled: Boolean = true,
    val tripMinimumDurationMinutes: Int = 2,
    val tripMinimumDistanceMeters: Int = 100,
    val locks: Map<String, SettingLock> = emptyMap(),
    val lastSyncedAt: Instant? = null,
) {
    /**
     * Check if a specific setting is locked.
     *
     * @param key The setting key to check
     * @return true if the setting is locked by an admin
     */
    fun isLocked(key: String): Boolean = locks[key]?.isLocked == true

    /**
     * Check if a specific setting can be modified by the user.
     *
     * @param key The setting key to check
     * @return true if the setting is not locked and can be modified
     */
    fun canModify(key: String): Boolean = !isLocked(key)

    /**
     * Get the lock information for a specific setting.
     *
     * @param key The setting key
     * @return SettingLock if the setting has lock info, null otherwise
     */
    fun getLock(key: String): SettingLock? = locks[key]

    /**
     * Get who locked a specific setting.
     *
     * @param key The setting key
     * @return The name/email of who locked it, or null if not locked
     */
    fun getLockedBy(key: String): String? = locks[key]?.lockedBy

    /**
     * Get the number of currently locked settings.
     *
     * @return Count of locked settings
     */
    fun lockedCount(): Int = locks.values.count { it.isLocked }

    companion object {
        // Setting keys - must match backend exactly
        const val KEY_TRACKING_ENABLED = "tracking_enabled"
        const val KEY_TRACKING_INTERVAL_MINUTES = "tracking_interval_minutes"
        const val KEY_SECRET_MODE_ENABLED = "secret_mode_enabled"
        const val KEY_GEOFENCE_NOTIFICATIONS_ENABLED = "geofence_notifications_enabled"
        const val KEY_BATTERY_OPTIMIZATION_ENABLED = "battery_optimization_enabled"
        const val KEY_NOTIFICATION_SOUNDS_ENABLED = "notification_sounds_enabled"
        const val KEY_SOS_ENABLED = "sos_enabled"
        const val KEY_MOVEMENT_DETECTION_ENABLED = "movement_detection_enabled"

        // Legacy keys for backward compatibility
        @Deprecated("Use KEY_TRACKING_INTERVAL_MINUTES", ReplaceWith("KEY_TRACKING_INTERVAL_MINUTES"))
        const val KEY_TRACKING_INTERVAL_SECONDS = "tracking_interval_seconds"
        const val KEY_SHOW_WEATHER_IN_NOTIFICATION = "show_weather_in_notification"
        const val KEY_TRIP_DETECTION_ENABLED = "trip_detection_enabled"
        const val KEY_TRIP_MINIMUM_DURATION_MINUTES = "trip_minimum_duration_minutes"
        const val KEY_TRIP_MINIMUM_DISTANCE_METERS = "trip_minimum_distance_meters"

        /**
         * All lockable setting keys.
         */
        val LOCKABLE_KEYS = listOf(
            KEY_TRACKING_ENABLED,
            KEY_TRACKING_INTERVAL_MINUTES,
            KEY_SECRET_MODE_ENABLED,
            KEY_GEOFENCE_NOTIFICATIONS_ENABLED,
            KEY_BATTERY_OPTIMIZATION_ENABLED,
            KEY_NOTIFICATION_SOUNDS_ENABLED,
            KEY_SOS_ENABLED,
            KEY_MOVEMENT_DETECTION_ENABLED,
        )
    }
}

/**
 * Story E12.6: Lock information for a device setting.
 *
 * Represents the lock state of a single setting, including
 * who locked it and when.
 *
 * @property settingKey The setting this lock applies to
 * @property isLocked Whether the setting is currently locked
 * @property lockedBy Who locked the setting (admin email/name)
 * @property lockedAt When the setting was locked
 */
data class SettingLock(
    val settingKey: String,
    val isLocked: Boolean,
    val lockedBy: String? = null,
    val lockedAt: Instant? = null,
)

/**
 * Story E12.6: Sync status for settings.
 *
 * Tracks the synchronization state between local and server settings.
 */
enum class SettingsSyncStatus {
    /**
     * Settings are in sync with server.
     */
    SYNCED,

    /**
     * Settings sync is in progress.
     */
    SYNCING,

    /**
     * Local changes are pending sync to server.
     */
    PENDING,

    /**
     * Settings sync failed (will retry).
     */
    ERROR,

    /**
     * Device is offline, showing cached settings.
     */
    OFFLINE,

    /**
     * User is not authenticated, sign-in required to sync settings.
     */
    NOT_AUTHENTICATED,
}

/**
 * Story E12.6: Result of a setting update attempt.
 *
 * @property success Whether the update succeeded
 * @property error Error message if update failed
 * @property wasLocked True if the update failed because setting is locked
 */
data class SettingUpdateResult(
    val success: Boolean,
    val error: String? = null,
    val wasLocked: Boolean = false,
)

/**
 * Story E12.6: Managed device status.
 *
 * Information about the device's management status in a group.
 *
 * @property isManaged Whether device is in a managed group
 * @property groupName Name of the managing group
 * @property groupId ID of the managing group
 * @property lockedSettingsCount Number of locked settings
 * @property lastSyncedAt When settings were last synced
 */
data class ManagedDeviceStatus(
    val isManaged: Boolean,
    val groupName: String? = null,
    val groupId: String? = null,
    val lockedSettingsCount: Int = 0,
    val lastSyncedAt: Instant? = null,
)

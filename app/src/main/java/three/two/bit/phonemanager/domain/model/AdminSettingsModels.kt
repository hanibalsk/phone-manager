package three.two.bit.phonemanager.domain.model

import kotlinx.datetime.Instant
import kotlin.time.Clock
/**
 * Story E12.7: Admin Settings Management Domain Models
 *
 * Models for admin-side settings management, including
 * device settings, change history, and templates.
 */

/**
 * Member device with settings information for admin view.
 * AC E12.7.1: Device Settings List Screen
 */
data class MemberDeviceSettings(
    val deviceId: String,
    val deviceName: String,
    val ownerUserId: String,
    val ownerName: String,
    val ownerEmail: String,
    val isOnline: Boolean,
    val lastSeen: Instant?,
    val settings: Map<String, Any>,
    val locks: Map<String, SettingLock>,
    val lastSyncedAt: Instant?,
    val lastModifiedBy: String?,
) {
    /**
     * Check if a specific setting is locked.
     */
    fun isLocked(key: String): Boolean = locks[key]?.isLocked == true

    /**
     * Get the lock for a specific setting.
     */
    fun getLock(key: String): SettingLock? = locks[key]

    /**
     * Get the value for a specific setting.
     */
    fun <T> getValue(key: String, default: T): T {
        @Suppress("UNCHECKED_CAST")
        return (settings[key] as? T) ?: default
    }

    /**
     * Count of locked settings.
     */
    fun lockedCount(): Int = locks.count { it.value.isLocked }

    companion object {
        /** Empty device settings for initial state */
        val EMPTY = MemberDeviceSettings(
            deviceId = "",
            deviceName = "",
            ownerUserId = "",
            ownerName = "",
            ownerEmail = "",
            isOnline = false,
            lastSeen = null,
            settings = emptyMap(),
            locks = emptyMap(),
            lastSyncedAt = null,
            lastModifiedBy = null,
        )
    }
}

/**
 * Represents a single setting change for audit trail.
 * AC E12.7.8: Audit Trail
 */
data class SettingChange(
    val id: String,
    val settingKey: String,
    val oldValue: Any?,
    val newValue: Any?,
    val changedBy: String,
    val changedByName: String,
    val changedAt: Instant,
    val changeType: SettingChangeType,
    val deviceId: String? = null,
) {
    /**
     * Human-readable description of the change.
     */
    fun getDescription(): String = when (changeType) {
        SettingChangeType.VALUE_CHANGED -> "Changed from $oldValue to $newValue"
        SettingChangeType.LOCKED -> "Locked by $changedByName"
        SettingChangeType.UNLOCKED -> "Unlocked by $changedByName"
        SettingChangeType.RESET -> "Reset to default"
    }
}

/**
 * Type of setting change.
 */
enum class SettingChangeType {
    VALUE_CHANGED,
    LOCKED,
    UNLOCKED,
    RESET,
}

/**
 * Settings template for applying to multiple devices.
 * AC E12.7.7: Settings Templates
 */
data class SettingsTemplate(
    val id: String,
    val name: String,
    val description: String? = null,
    val settings: Map<String, Any>,
    val lockedSettings: Set<String>,
    val createdBy: String,
    val createdByName: String,
    val createdAt: Instant,
    val updatedAt: Instant? = null,
    val isShared: Boolean = false,
) {
    /**
     * Check if a setting is included in this template.
     */
    fun includesSetting(key: String): Boolean = settings.containsKey(key)

    /**
     * Check if a setting should be locked when applying this template.
     */
    fun shouldLock(key: String): Boolean = key in lockedSettings

    companion object {
        /** Default template with tracking enabled settings */
        fun createDefault(
            id: String,
            name: String,
            createdBy: String,
            createdByName: String,
        ) = SettingsTemplate(
            id = id,
            name = name,
            settings = mapOf(
                DeviceSettings.KEY_TRACKING_ENABLED to true,
                DeviceSettings.KEY_TRACKING_INTERVAL_SECONDS to 60,
                DeviceSettings.KEY_SECRET_MODE_ENABLED to false,
            ),
            lockedSettings = setOf(DeviceSettings.KEY_TRACKING_ENABLED),
            createdBy = createdBy,
            createdByName = createdByName,
            createdAt = Clock.System.now(),
        )
    }
}

/**
 * Result of applying settings to devices.
 * AC E12.7.6: Bulk Settings Application
 */
data class BulkSettingsResult(
    val successful: List<DeviceSettingsResult>,
    val failed: List<DeviceSettingsResult>,
) {
    val successCount: Int get() = successful.size
    val failureCount: Int get() = failed.size
    val totalCount: Int get() = successCount + failureCount
    val isAllSuccessful: Boolean get() = failed.isEmpty()
}

/**
 * Result of applying settings to a single device.
 */
data class DeviceSettingsResult(
    val deviceId: String,
    val deviceName: String,
    val success: Boolean,
    val error: String? = null,
    val appliedSettings: Map<String, Any>? = null,
)

/**
 * Online status filter for device list.
 * AC E12.7.1: Filter by online/offline status
 */
enum class DeviceStatusFilter {
    ALL,
    ONLINE,
    OFFLINE,
}

/**
 * Category for grouping settings in UI.
 * AC E12.7.2: Settings grouped by category
 */
enum class SettingCategory(val displayName: String) {
    TRACKING("Location Tracking"),
    TRIP_DETECTION("Trip Detection"),
    DISPLAY("Display Settings"),
    NOTIFICATIONS("Notifications"),
}

/**
 * Setting definition with metadata for admin UI.
 */
data class SettingDefinition(
    val key: String,
    val displayName: String,
    val description: String,
    val category: SettingCategory,
    val type: SettingType,
    val defaultValue: Any,
    val validation: SettingValidation? = null,
) {
    companion object {
        /** All available settings with their definitions */
        val ALL_SETTINGS = listOf(
            SettingDefinition(
                key = DeviceSettings.KEY_TRACKING_ENABLED,
                displayName = "Location Tracking",
                description = "Enable or disable location tracking",
                category = SettingCategory.TRACKING,
                type = SettingType.BOOLEAN,
                defaultValue = true,
            ),
            SettingDefinition(
                key = DeviceSettings.KEY_TRACKING_INTERVAL_SECONDS,
                displayName = "Tracking Interval",
                description = "How often to record location (in seconds)",
                category = SettingCategory.TRACKING,
                type = SettingType.INTEGER,
                defaultValue = 60,
                validation = SettingValidation.IntRange(10, 3600),
            ),
            SettingDefinition(
                key = DeviceSettings.KEY_SECRET_MODE_ENABLED,
                displayName = "Secret Mode",
                description = "Hide tracking notification",
                category = SettingCategory.TRACKING,
                type = SettingType.BOOLEAN,
                defaultValue = false,
            ),
            SettingDefinition(
                key = DeviceSettings.KEY_SHOW_WEATHER_IN_NOTIFICATION,
                displayName = "Show Weather",
                description = "Display weather in notification",
                category = SettingCategory.NOTIFICATIONS,
                type = SettingType.BOOLEAN,
                defaultValue = true,
            ),
            SettingDefinition(
                key = DeviceSettings.KEY_TRIP_DETECTION_ENABLED,
                displayName = "Trip Detection",
                description = "Automatically detect and record trips",
                category = SettingCategory.TRIP_DETECTION,
                type = SettingType.BOOLEAN,
                defaultValue = true,
            ),
            SettingDefinition(
                key = DeviceSettings.KEY_TRIP_MINIMUM_DURATION_MINUTES,
                displayName = "Minimum Trip Duration",
                description = "Minimum duration for a trip (in minutes)",
                category = SettingCategory.TRIP_DETECTION,
                type = SettingType.INTEGER,
                defaultValue = 5,
                validation = SettingValidation.IntRange(1, 60),
            ),
            SettingDefinition(
                key = DeviceSettings.KEY_TRIP_MINIMUM_DISTANCE_METERS,
                displayName = "Minimum Trip Distance",
                description = "Minimum distance for a trip (in meters)",
                category = SettingCategory.TRIP_DETECTION,
                type = SettingType.INTEGER,
                defaultValue = 100,
                validation = SettingValidation.IntRange(10, 10000),
            ),
        )

        /** Get definition by key */
        fun forKey(key: String): SettingDefinition? =
            ALL_SETTINGS.find { it.key == key }

        /** Get settings grouped by category */
        fun byCategory(): Map<SettingCategory, List<SettingDefinition>> =
            ALL_SETTINGS.groupBy { it.category }
    }
}

/**
 * Type of setting value.
 */
enum class SettingType {
    BOOLEAN,
    INTEGER,
    STRING,
    FLOAT,
}

/**
 * Validation rules for settings.
 */
sealed class SettingValidation {
    data class IntRange(val min: Int, val max: Int) : SettingValidation() {
        fun isValid(value: Int): Boolean = value in min..max
        fun errorMessage(value: Int): String = "Value must be between $min and $max"
    }

    data class FloatRange(val min: Float, val max: Float) : SettingValidation() {
        fun isValid(value: Float): Boolean = value in min..max
        fun errorMessage(value: Float): String = "Value must be between $min and $max"
    }

    data class StringLength(val min: Int, val max: Int) : SettingValidation() {
        fun isValid(value: String): Boolean = value.length in min..max
        fun errorMessage(value: String): String = "Length must be between $min and $max characters"
    }

    data class Pattern(val regex: Regex, val description: String) : SettingValidation() {
        fun isValid(value: String): Boolean = regex.matches(value)
        fun errorMessage(value: String): String = description
    }
}

/**
 * Validate a setting value against its definition.
 * @return Error message if invalid, null if valid
 */
fun validateSettingValue(key: String, value: Any): String? {
    val definition = SettingDefinition.forKey(key) ?: return "Unknown setting: $key"

    // Type check
    val typeValid = when (definition.type) {
        SettingType.BOOLEAN -> value is Boolean
        SettingType.INTEGER -> value is Int || value is Long || (value is Number && value.toDouble() % 1 == 0.0)
        SettingType.FLOAT -> value is Float || value is Double || value is Number
        SettingType.STRING -> value is String
    }

    if (!typeValid) {
        return "Invalid type for ${definition.displayName}"
    }

    // Validation check
    return when (val validation = definition.validation) {
        is SettingValidation.IntRange -> {
            val intValue = (value as Number).toInt()
            if (!validation.isValid(intValue)) validation.errorMessage(intValue) else null
        }
        is SettingValidation.FloatRange -> {
            val floatValue = (value as Number).toFloat()
            if (!validation.isValid(floatValue)) validation.errorMessage(floatValue) else null
        }
        is SettingValidation.StringLength -> {
            val stringValue = value as String
            if (!validation.isValid(stringValue)) validation.errorMessage(stringValue) else null
        }
        is SettingValidation.Pattern -> {
            val stringValue = value as String
            if (!validation.isValid(stringValue)) validation.errorMessage(stringValue) else null
        }
        null -> null
    }
}

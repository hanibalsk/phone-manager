package three.two.bit.phonemanager.domain.policy

import three.two.bit.phonemanager.data.preferences.PreferencesRepository
import three.two.bit.phonemanager.domain.model.DevicePolicy
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Story E13.10: Android Enrollment Flow - Policy Application Logic
 *
 * AC E13.10.5: Apply device policies from enrollment.
 * Maps server policy values to local app preferences.
 *
 * Supported policy settings:
 * - tracking_enabled: Boolean - Enable/disable location tracking
 * - tracking_interval: Int - Tracking interval in minutes
 * - secret_mode_enabled: Boolean - Enable/disable secret mode
 * - movement_detection_enabled: Boolean - Enable/disable movement detection
 * - trip_detection_enabled: Boolean - Enable/disable trip detection
 * - show_weather_in_notification: Boolean - Show weather in notification
 * - map_polling_interval: Int - Map polling interval in seconds
 */
@Singleton
class PolicyApplicator @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
) {
    companion object {
        // Policy setting keys
        const val KEY_TRACKING_ENABLED = "tracking_enabled"
        const val KEY_TRACKING_INTERVAL = "tracking_interval"
        const val KEY_SECRET_MODE_ENABLED = "secret_mode_enabled"
        const val KEY_MOVEMENT_DETECTION_ENABLED = "movement_detection_enabled"
        const val KEY_TRIP_DETECTION_ENABLED = "trip_detection_enabled"
        const val KEY_SHOW_WEATHER_IN_NOTIFICATION = "show_weather_in_notification"
        const val KEY_MAP_POLLING_INTERVAL = "map_polling_interval"
        const val KEY_ACTIVITY_RECOGNITION_ENABLED = "activity_recognition_enabled"
        const val KEY_BLUETOOTH_CAR_DETECTION_ENABLED = "bluetooth_car_detection_enabled"
        const val KEY_ANDROID_AUTO_DETECTION_ENABLED = "android_auto_detection_enabled"
        const val KEY_VEHICLE_INTERVAL_MULTIPLIER = "vehicle_interval_multiplier"
        const val KEY_DEFAULT_INTERVAL_MULTIPLIER = "default_interval_multiplier"
        const val KEY_TRIP_STATIONARY_THRESHOLD = "trip_stationary_threshold"
        const val KEY_TRIP_MINIMUM_DURATION = "trip_minimum_duration"
        const val KEY_TRIP_MINIMUM_DISTANCE = "trip_minimum_distance"
        const val KEY_TRIP_AUTO_MERGE_ENABLED = "trip_auto_merge_enabled"

        // All known policy keys for validation
        val ALL_POLICY_KEYS = setOf(
            KEY_TRACKING_ENABLED,
            KEY_TRACKING_INTERVAL,
            KEY_SECRET_MODE_ENABLED,
            KEY_MOVEMENT_DETECTION_ENABLED,
            KEY_TRIP_DETECTION_ENABLED,
            KEY_SHOW_WEATHER_IN_NOTIFICATION,
            KEY_MAP_POLLING_INTERVAL,
            KEY_ACTIVITY_RECOGNITION_ENABLED,
            KEY_BLUETOOTH_CAR_DETECTION_ENABLED,
            KEY_ANDROID_AUTO_DETECTION_ENABLED,
            KEY_VEHICLE_INTERVAL_MULTIPLIER,
            KEY_DEFAULT_INTERVAL_MULTIPLIER,
            KEY_TRIP_STATIONARY_THRESHOLD,
            KEY_TRIP_MINIMUM_DURATION,
            KEY_TRIP_MINIMUM_DISTANCE,
            KEY_TRIP_AUTO_MERGE_ENABLED,
        )
    }

    /**
     * Apply device policies from enrollment.
     * AC E13.10.5: Applies all policy settings to local preferences.
     *
     * @param policy The device policy from enrollment
     * @return PolicyApplicationResult with success status and details
     */
    suspend fun applyPolicies(policy: DevicePolicy): PolicyApplicationResult {
        Timber.i("Applying device policies: ${policy.settings.size} settings, ${policy.locks.size} locks")

        val appliedSettings = mutableListOf<String>()
        val failedSettings = mutableListOf<PolicyFailure>()
        val skippedSettings = mutableListOf<String>()

        for ((key, value) in policy.settings) {
            try {
                val applied = applySetting(key, value)
                if (applied) {
                    appliedSettings.add(key)
                    Timber.d("Applied policy setting: $key = $value")
                } else {
                    skippedSettings.add(key)
                    Timber.d("Skipped unknown policy setting: $key")
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to apply policy setting: $key")
                failedSettings.add(PolicyFailure(key, e.message ?: "Unknown error"))
            }
        }

        val success = failedSettings.isEmpty()
        Timber.i(
            "Policy application complete: applied=${appliedSettings.size}, " +
                "failed=${failedSettings.size}, skipped=${skippedSettings.size}",
        )

        return PolicyApplicationResult(
            success = success,
            appliedSettings = appliedSettings,
            failedSettings = failedSettings,
            skippedSettings = skippedSettings,
        )
    }

    /**
     * Apply a single policy setting.
     *
     * @param key The setting key
     * @param value The value to apply
     * @return true if applied, false if unknown key
     */
    private suspend fun applySetting(key: String, value: Any): Boolean {
        return when (key) {
            KEY_TRACKING_ENABLED -> {
                preferencesRepository.setTrackingEnabled(value.toBoolean())
                true
            }

            KEY_TRACKING_INTERVAL -> {
                val minutes = value.toInt()
                if (minutes in 1..60) {
                    preferencesRepository.setTrackingInterval(minutes)
                }
                true
            }

            KEY_SECRET_MODE_ENABLED -> {
                preferencesRepository.setSecretModeEnabled(value.toBoolean())
                true
            }

            KEY_MOVEMENT_DETECTION_ENABLED -> {
                preferencesRepository.setMovementDetectionEnabled(value.toBoolean())
                true
            }

            KEY_ACTIVITY_RECOGNITION_ENABLED -> {
                preferencesRepository.setActivityRecognitionEnabled(value.toBoolean())
                true
            }

            KEY_BLUETOOTH_CAR_DETECTION_ENABLED -> {
                preferencesRepository.setBluetoothCarDetectionEnabled(value.toBoolean())
                true
            }

            KEY_ANDROID_AUTO_DETECTION_ENABLED -> {
                preferencesRepository.setAndroidAutoDetectionEnabled(value.toBoolean())
                true
            }

            KEY_VEHICLE_INTERVAL_MULTIPLIER -> {
                val multiplier = value.toFloat()
                if (multiplier in 0.1f..1.0f) {
                    preferencesRepository.setVehicleIntervalMultiplier(multiplier)
                }
                true
            }

            KEY_DEFAULT_INTERVAL_MULTIPLIER -> {
                val multiplier = value.toFloat()
                if (multiplier in 0.1f..2.0f) {
                    preferencesRepository.setDefaultIntervalMultiplier(multiplier)
                }
                true
            }

            KEY_TRIP_DETECTION_ENABLED -> {
                preferencesRepository.setTripDetectionEnabled(value.toBoolean())
                true
            }

            KEY_TRIP_STATIONARY_THRESHOLD -> {
                val minutes = value.toInt()
                preferencesRepository.setTripStationaryThresholdMinutes(minutes)
                true
            }

            KEY_TRIP_MINIMUM_DURATION -> {
                val minutes = value.toInt()
                preferencesRepository.setTripMinimumDurationMinutes(minutes)
                true
            }

            KEY_TRIP_MINIMUM_DISTANCE -> {
                val meters = value.toInt()
                preferencesRepository.setTripMinimumDistanceMeters(meters)
                true
            }

            KEY_TRIP_AUTO_MERGE_ENABLED -> {
                preferencesRepository.setTripAutoMergeEnabled(value.toBoolean())
                true
            }

            KEY_SHOW_WEATHER_IN_NOTIFICATION -> {
                preferencesRepository.setShowWeatherInNotification(value.toBoolean())
                true
            }

            KEY_MAP_POLLING_INTERVAL -> {
                val seconds = value.toInt()
                if (seconds in 10..30) {
                    preferencesRepository.setMapPollingIntervalSeconds(seconds)
                }
                true
            }

            else -> false // Unknown setting
        }
    }

    /**
     * Check if a setting is locked by policy.
     *
     * @param policy The device policy
     * @param settingKey The setting key to check
     * @return true if the setting is locked
     */
    fun isSettingLocked(policy: DevicePolicy?, settingKey: String): Boolean {
        return policy?.isLocked(settingKey) ?: false
    }

    /**
     * Get the policy value for a setting.
     *
     * @param policy The device policy
     * @param settingKey The setting key
     * @return The policy value, or null if not set
     */
    fun getPolicyValue(policy: DevicePolicy?, settingKey: String): Any? {
        return policy?.getPolicyValue(settingKey)
    }

    /**
     * Get a human-readable name for a setting key.
     */
    fun getSettingDisplayName(settingKey: String): String {
        return when (settingKey) {
            KEY_TRACKING_ENABLED -> "Location Tracking"
            KEY_TRACKING_INTERVAL -> "Tracking Interval"
            KEY_SECRET_MODE_ENABLED -> "Secret Mode"
            KEY_MOVEMENT_DETECTION_ENABLED -> "Movement Detection"
            KEY_TRIP_DETECTION_ENABLED -> "Trip Detection"
            KEY_SHOW_WEATHER_IN_NOTIFICATION -> "Weather in Notification"
            KEY_MAP_POLLING_INTERVAL -> "Map Refresh Rate"
            KEY_ACTIVITY_RECOGNITION_ENABLED -> "Activity Recognition"
            KEY_BLUETOOTH_CAR_DETECTION_ENABLED -> "Bluetooth Car Detection"
            KEY_ANDROID_AUTO_DETECTION_ENABLED -> "Android Auto Detection"
            KEY_VEHICLE_INTERVAL_MULTIPLIER -> "Vehicle Tracking Rate"
            KEY_DEFAULT_INTERVAL_MULTIPLIER -> "Default Tracking Rate"
            KEY_TRIP_STATIONARY_THRESHOLD -> "Trip End Threshold"
            KEY_TRIP_MINIMUM_DURATION -> "Minimum Trip Duration"
            KEY_TRIP_MINIMUM_DISTANCE -> "Minimum Trip Distance"
            KEY_TRIP_AUTO_MERGE_ENABLED -> "Auto-merge Trips"
            else -> settingKey.replace("_", " ")
                .split(" ")
                .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
        }
    }
}

/**
 * Helper extension to convert Any to Boolean.
 */
private fun Any.toBoolean(): Boolean {
    return when (this) {
        is Boolean -> this
        is String -> this.lowercase() in listOf("true", "1", "yes", "on")
        is Number -> this.toInt() != 0
        else -> false
    }
}

/**
 * Helper extension to convert Any to Int.
 */
private fun Any.toInt(): Int {
    return when (this) {
        is Int -> this
        is Long -> this.toInt()
        is Double -> this.toInt()
        is Float -> this.toInt()
        is String -> this.toIntOrNull() ?: 0
        is Number -> this.toInt()
        else -> 0
    }
}

/**
 * Helper extension to convert Any to Float.
 */
private fun Any.toFloat(): Float {
    return when (this) {
        is Float -> this
        is Double -> this.toFloat()
        is Int -> this.toFloat()
        is Long -> this.toFloat()
        is String -> this.toFloatOrNull() ?: 0f
        is Number -> this.toFloat()
        else -> 0f
    }
}

/**
 * Result of policy application.
 */
data class PolicyApplicationResult(
    val success: Boolean,
    val appliedSettings: List<String>,
    val failedSettings: List<PolicyFailure>,
    val skippedSettings: List<String>,
)

/**
 * Details of a policy application failure.
 */
data class PolicyFailure(
    val settingKey: String,
    val error: String,
)

package com.phonemanager.util

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.getSystemService
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Story 0.2.4: PowerUtil - Battery optimization and Doze mode handling
 *
 * Provides utilities for:
 * - Checking battery optimization status
 * - Requesting battery optimization exemption
 * - Checking Doze mode status
 * - Checking if exact alarms can be scheduled (Android 12+)
 */
@Singleton
class PowerUtil @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val powerManager: PowerManager? by lazy {
        context.getSystemService()
    }

    private val alarmManager: AlarmManager? by lazy {
        context.getSystemService()
    }

    /**
     * Check if the app is ignoring battery optimizations
     *
     * Battery optimization can prevent background services from running.
     * For reliable location tracking, the app should request exemption.
     */
    fun isIgnoringBatteryOptimizations(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false
        } else {
            // Battery optimization doesn't exist before Android M
            true
        }
    }

    /**
     * Check if device is currently in Doze mode
     *
     * Doze mode restricts app activity to save battery.
     */
    fun isDeviceIdleMode(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            powerManager?.isDeviceIdleMode == true
        } else {
            false
        }
    }

    /**
     * Check if the app can schedule exact alarms (Android 12+)
     *
     * Exact alarms can be used to wake up the device for critical tasks.
     */
    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager?.canScheduleExactAlarms() ?: false
        } else {
            // Exact alarms don't require permission before Android 12
            true
        }
    }

    /**
     * Create an intent to request battery optimization exemption
     *
     * This opens the system settings page where the user can disable
     * battery optimization for this app.
     */
    fun createBatteryOptimizationIntent(): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        } else {
            // Return settings intent as fallback
            Intent(Settings.ACTION_SETTINGS)
        }
    }

    /**
     * Create an intent to request exact alarm permission (Android 12+)
     *
     * This opens the system settings page where the user can grant
     * exact alarm permission.
     */
    fun createExactAlarmPermissionIntent(): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        } else {
            // Return settings intent as fallback
            Intent(Settings.ACTION_SETTINGS)
        }
    }

    /**
     * Check if the device is in power save mode
     *
     * Power save mode can affect background processing.
     */
    fun isPowerSaveMode(): Boolean {
        return powerManager?.isPowerSaveMode == true
    }

    /**
     * Get comprehensive power status information
     */
    fun getPowerStatus(): PowerStatus {
        return PowerStatus(
            isIgnoringBatteryOptimizations = isIgnoringBatteryOptimizations(),
            isDeviceIdleMode = isDeviceIdleMode(),
            canScheduleExactAlarms = canScheduleExactAlarms(),
            isPowerSaveMode = isPowerSaveMode()
        )
    }

    /**
     * Log power status for debugging
     */
    fun logPowerStatus() {
        val status = getPowerStatus()
        Timber.d(
            """
            Power Status:
            - Ignoring battery optimizations: ${status.isIgnoringBatteryOptimizations}
            - Device in Doze mode: ${status.isDeviceIdleMode}
            - Can schedule exact alarms: ${status.canScheduleExactAlarms}
            - Power save mode: ${status.isPowerSaveMode}
            """.trimIndent()
        )
    }
}

/**
 * Data class holding power-related status information
 */
data class PowerStatus(
    val isIgnoringBatteryOptimizations: Boolean,
    val isDeviceIdleMode: Boolean,
    val canScheduleExactAlarms: Boolean,
    val isPowerSaveMode: Boolean
)

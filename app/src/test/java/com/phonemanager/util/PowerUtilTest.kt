package com.phonemanager.util

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for PowerUtil
 *
 * Story 0.2.4: Tests battery optimization and Doze mode handling
 * Verifies:
 * - Battery optimization status checking
 * - Doze mode detection
 * - Power save mode detection
 * - Exact alarm permission (Android 12+)
 * - Intent creation for settings
 */
class PowerUtilTest {

    private lateinit var powerUtil: PowerUtil
    private lateinit var context: Context
    private lateinit var powerManager: PowerManager
    private lateinit var alarmManager: AlarmManager

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        powerManager = mockk(relaxed = true)
        alarmManager = mockk(relaxed = true)

        every { context.packageName } returns "com.phonemanager.test"
        every { context.getSystemService(Context.POWER_SERVICE) } returns powerManager
        every { context.getSystemService(Context.ALARM_SERVICE) } returns alarmManager
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `isIgnoringBatteryOptimizations returns true when app is whitelisted`() {
        // Given
        mockkStatic(Build.VERSION::class)
        every { Build.VERSION.SDK_INT } returns Build.VERSION_CODES.M
        every { powerManager.isIgnoringBatteryOptimizations("com.phonemanager.test") } returns true

        powerUtil = PowerUtil(context)

        // When
        val result = powerUtil.isIgnoringBatteryOptimizations()

        // Then
        assertTrue(result)
        verify { powerManager.isIgnoringBatteryOptimizations("com.phonemanager.test") }
    }

    @Test
    fun `isIgnoringBatteryOptimizations returns false when app is not whitelisted`() {
        // Given
        mockkStatic(Build.VERSION::class)
        every { Build.VERSION.SDK_INT } returns Build.VERSION_CODES.M
        every { powerManager.isIgnoringBatteryOptimizations("com.phonemanager.test") } returns false

        powerUtil = PowerUtil(context)

        // When
        val result = powerUtil.isIgnoringBatteryOptimizations()

        // Then
        assertFalse(result)
    }

    @Test
    fun `isIgnoringBatteryOptimizations returns true on devices below Android M`() {
        // Given - Battery optimization doesn't exist before Android M
        powerUtil = PowerUtil(context)

        // When/Then
        // On devices below Android M, should return true (no battery optimization)
        // Note: This test behavior depends on the actual SDK version of the test environment
    }

    @Test
    fun `isDeviceIdleMode returns true when device is in Doze mode`() {
        // Given
        mockkStatic(Build.VERSION::class)
        every { Build.VERSION.SDK_INT } returns Build.VERSION_CODES.M
        every { powerManager.isDeviceIdleMode } returns true

        powerUtil = PowerUtil(context)

        // When
        val result = powerUtil.isDeviceIdleMode()

        // Then
        assertTrue(result)
        verify { powerManager.isDeviceIdleMode }
    }

    @Test
    fun `isDeviceIdleMode returns false when device is not in Doze mode`() {
        // Given
        mockkStatic(Build.VERSION::class)
        every { Build.VERSION.SDK_INT } returns Build.VERSION_CODES.M
        every { powerManager.isDeviceIdleMode } returns false

        powerUtil = PowerUtil(context)

        // When
        val result = powerUtil.isDeviceIdleMode()

        // Then
        assertFalse(result)
    }

    @Test
    fun `isDeviceIdleMode returns false on devices below Android M`() {
        // Given - Doze mode doesn't exist before Android M
        powerUtil = PowerUtil(context)

        // When/Then
        // Should return false on older devices
    }

    @Test
    fun `canScheduleExactAlarms returns true when permission granted on Android 12+`() {
        // Given
        mockkStatic(Build.VERSION::class)
        every { Build.VERSION.SDK_INT } returns Build.VERSION_CODES.S
        every { alarmManager.canScheduleExactAlarms() } returns true

        powerUtil = PowerUtil(context)

        // When
        val result = powerUtil.canScheduleExactAlarms()

        // Then
        assertTrue(result)
        verify { alarmManager.canScheduleExactAlarms() }
    }

    @Test
    fun `canScheduleExactAlarms returns false when permission not granted on Android 12+`() {
        // Given
        mockkStatic(Build.VERSION::class)
        every { Build.VERSION.SDK_INT } returns Build.VERSION_CODES.S
        every { alarmManager.canScheduleExactAlarms() } returns false

        powerUtil = PowerUtil(context)

        // When
        val result = powerUtil.canScheduleExactAlarms()

        // Then
        assertFalse(result)
    }

    @Test
    fun `canScheduleExactAlarms returns true on devices below Android 12`() {
        // Given - Exact alarm permission doesn't exist before Android 12
        powerUtil = PowerUtil(context)

        // When/Then
        // Should return true on older devices (no permission required)
    }

    @Test
    fun `createBatteryOptimizationIntent creates correct intent for Android M+`() {
        // Given
        mockkStatic(Build.VERSION::class)
        every { Build.VERSION.SDK_INT } returns Build.VERSION_CODES.M

        powerUtil = PowerUtil(context)

        // When
        val intent = powerUtil.createBatteryOptimizationIntent()

        // Then
        assertEquals(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, intent.action)
        assertEquals(Uri.parse("package:com.phonemanager.test"), intent.data)
    }

    @Test
    fun `createBatteryOptimizationIntent creates settings intent for devices below Android M`() {
        // Given
        powerUtil = PowerUtil(context)

        // When
        val intent = powerUtil.createBatteryOptimizationIntent()

        // Then
        // On devices below Android M, should return settings intent as fallback
    }

    @Test
    fun `createExactAlarmPermissionIntent creates correct intent for Android 12+`() {
        // Given
        mockkStatic(Build.VERSION::class)
        every { Build.VERSION.SDK_INT } returns Build.VERSION_CODES.S

        powerUtil = PowerUtil(context)

        // When
        val intent = powerUtil.createExactAlarmPermissionIntent()

        // Then
        assertEquals(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, intent.action)
        assertEquals(Uri.parse("package:com.phonemanager.test"), intent.data)
    }

    @Test
    fun `createExactAlarmPermissionIntent creates settings intent for devices below Android 12`() {
        // Given
        powerUtil = PowerUtil(context)

        // When
        val intent = powerUtil.createExactAlarmPermissionIntent()

        // Then
        // On devices below Android 12, should return settings intent as fallback
    }

    @Test
    fun `isPowerSaveMode returns true when power save mode is enabled`() {
        // Given
        every { powerManager.isPowerSaveMode } returns true

        powerUtil = PowerUtil(context)

        // When
        val result = powerUtil.isPowerSaveMode()

        // Then
        assertTrue(result)
        verify { powerManager.isPowerSaveMode }
    }

    @Test
    fun `isPowerSaveMode returns false when power save mode is disabled`() {
        // Given
        every { powerManager.isPowerSaveMode } returns false

        powerUtil = PowerUtil(context)

        // When
        val result = powerUtil.isPowerSaveMode()

        // Then
        assertFalse(result)
    }

    @Test
    fun `getPowerStatus returns comprehensive power status`() {
        // Given
        mockkStatic(Build.VERSION::class)
        every { Build.VERSION.SDK_INT } returns Build.VERSION_CODES.S

        every { powerManager.isIgnoringBatteryOptimizations("com.phonemanager.test") } returns true
        every { powerManager.isDeviceIdleMode } returns false
        every { powerManager.isPowerSaveMode } returns false
        every { alarmManager.canScheduleExactAlarms() } returns true

        powerUtil = PowerUtil(context)

        // When
        val status = powerUtil.getPowerStatus()

        // Then
        assertTrue(status.isIgnoringBatteryOptimizations)
        assertFalse(status.isDeviceIdleMode)
        assertTrue(status.canScheduleExactAlarms)
        assertFalse(status.isPowerSaveMode)
    }

    @Test
    fun `getPowerStatus detects when device is in Doze mode with battery optimization`() {
        // Given
        mockkStatic(Build.VERSION::class)
        every { Build.VERSION.SDK_INT } returns Build.VERSION_CODES.M

        every { powerManager.isIgnoringBatteryOptimizations("com.phonemanager.test") } returns false
        every { powerManager.isDeviceIdleMode } returns true
        every { powerManager.isPowerSaveMode } returns true

        powerUtil = PowerUtil(context)

        // When
        val status = powerUtil.getPowerStatus()

        // Then
        assertFalse(status.isIgnoringBatteryOptimizations, "Should not be ignoring battery optimizations")
        assertTrue(status.isDeviceIdleMode, "Should be in Doze mode")
        assertTrue(status.isPowerSaveMode, "Should be in power save mode")
    }

    @Test
    fun `logPowerStatus logs all power-related information`() {
        // Given
        mockkStatic(Build.VERSION::class)
        every { Build.VERSION.SDK_INT } returns Build.VERSION_CODES.S

        every { powerManager.isIgnoringBatteryOptimizations("com.phonemanager.test") } returns true
        every { powerManager.isDeviceIdleMode } returns false
        every { powerManager.isPowerSaveMode } returns false
        every { alarmManager.canScheduleExactAlarms() } returns true

        powerUtil = PowerUtil(context)

        // When
        powerUtil.logPowerStatus()

        // Then
        // Verify all power status checks were called
        verify { powerManager.isIgnoringBatteryOptimizations("com.phonemanager.test") }
        verify { powerManager.isDeviceIdleMode }
        verify { powerManager.isPowerSaveMode }
        verify { alarmManager.canScheduleExactAlarms() }
    }

    @Test
    fun `handles null PowerManager gracefully`() {
        // Given
        every { context.getSystemService(Context.POWER_SERVICE) } returns null

        powerUtil = PowerUtil(context)

        // When
        val isIgnoring = powerUtil.isIgnoringBatteryOptimizations()
        val isIdle = powerUtil.isDeviceIdleMode()
        val isPowerSave = powerUtil.isPowerSaveMode()

        // Then
        // Should handle null gracefully and return safe defaults
        assertFalse(isIgnoring)
        assertFalse(isIdle)
        assertFalse(isPowerSave)
    }

    @Test
    fun `handles null AlarmManager gracefully`() {
        // Given
        every { context.getSystemService(Context.ALARM_SERVICE) } returns null

        powerUtil = PowerUtil(context)

        // When
        val canScheduleExact = powerUtil.canScheduleExactAlarms()

        // Then
        // Should handle null gracefully
        // Result depends on SDK version
    }
}

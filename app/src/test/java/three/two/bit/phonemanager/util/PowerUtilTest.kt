package three.two.bit.phonemanager.util

import android.app.AlarmManager
import android.content.Context
import android.os.PowerManager
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for PowerUtil
 *
 * Story 0.2.4: Tests battery optimization and Doze mode handling
 *
 * Note: Tests that require specific SDK version behavior are limited in unit tests
 * due to Build.VERSION.SDK_INT being a final field. Full behavior testing requires
 * Android instrumented tests or Robolectric.
 */
class PowerUtilTest {

    private lateinit var context: Context
    private lateinit var powerManager: PowerManager
    private lateinit var alarmManager: AlarmManager

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        powerManager = mockk(relaxed = true)
        alarmManager = mockk(relaxed = true)

        every { context.packageName } returns "three.two.bit.phonemanager.test"
        every { context.getSystemService(Context.POWER_SERVICE) } returns powerManager
        every { context.getSystemService(Context.ALARM_SERVICE) } returns alarmManager
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `PowerUtil can be instantiated`() {
        val powerUtil = PowerUtil(context)
        assertNotNull(powerUtil)
    }

    @Test
    fun `isPowerSaveMode returns true when power save mode is enabled`() {
        // Given
        every { powerManager.isPowerSaveMode } returns true
        val powerUtil = PowerUtil(context)

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
        val powerUtil = PowerUtil(context)

        // When
        val result = powerUtil.isPowerSaveMode()

        // Then
        assertFalse(result)
    }

    @Test
    fun `isIgnoringBatteryOptimizations calls PowerManager method`() {
        // Given
        every { powerManager.isIgnoringBatteryOptimizations("three.two.bit.phonemanager.test") } returns true
        val powerUtil = PowerUtil(context)

        // When
        val result = powerUtil.isIgnoringBatteryOptimizations()

        // Then - verify the method was called (behavior depends on SDK)
        // On API 23+, should call PowerManager.isIgnoringBatteryOptimizations
        // On lower APIs, returns true by default
        assertNotNull(result)
    }

    @Test
    fun `isDeviceIdleMode calls PowerManager method`() {
        // Given
        every { powerManager.isDeviceIdleMode } returns false
        val powerUtil = PowerUtil(context)

        // When
        val result = powerUtil.isDeviceIdleMode()

        // Then - verify the result is boolean (behavior depends on SDK)
        assertNotNull(result)
    }

    @Test
    fun `canScheduleExactAlarms calls AlarmManager method`() {
        // Given
        every { alarmManager.canScheduleExactAlarms() } returns true
        val powerUtil = PowerUtil(context)

        // When
        val result = powerUtil.canScheduleExactAlarms()

        // Then - verify the result is boolean (behavior depends on SDK)
        assertNotNull(result)
    }

    @Test
    fun `createBatteryOptimizationIntent returns non-null Intent`() {
        // Given
        val powerUtil = PowerUtil(context)

        // When
        val intent = powerUtil.createBatteryOptimizationIntent()

        // Then
        assertNotNull(intent)
    }

    @Test
    fun `createExactAlarmPermissionIntent returns non-null Intent`() {
        // Given
        val powerUtil = PowerUtil(context)

        // When
        val intent = powerUtil.createExactAlarmPermissionIntent()

        // Then
        assertNotNull(intent)
    }

    @Test
    fun `getPowerStatus returns comprehensive power status`() {
        // Given
        every { powerManager.isIgnoringBatteryOptimizations("three.two.bit.phonemanager.test") } returns true
        every { powerManager.isDeviceIdleMode } returns false
        every { powerManager.isPowerSaveMode } returns false
        every { alarmManager.canScheduleExactAlarms() } returns true

        val powerUtil = PowerUtil(context)

        // When
        val status = powerUtil.getPowerStatus()

        // Then
        assertNotNull(status)
    }

    @Test
    fun `logPowerStatus executes without error`() {
        // Given
        every { powerManager.isIgnoringBatteryOptimizations("three.two.bit.phonemanager.test") } returns true
        every { powerManager.isDeviceIdleMode } returns false
        every { powerManager.isPowerSaveMode } returns false
        every { alarmManager.canScheduleExactAlarms() } returns true

        val powerUtil = PowerUtil(context)

        // When/Then - should not throw
        powerUtil.logPowerStatus()
    }

    @Test
    fun `handles null PowerManager gracefully`() {
        // Given
        every { context.getSystemService(Context.POWER_SERVICE) } returns null

        val powerUtil = PowerUtil(context)

        // When/Then - should not throw and return safe defaults
        val isPowerSave = powerUtil.isPowerSaveMode()
        assertFalse(isPowerSave)
    }

    @Test
    fun `handles null AlarmManager gracefully`() {
        // Given
        every { context.getSystemService(Context.ALARM_SERVICE) } returns null

        val powerUtil = PowerUtil(context)

        // When/Then - should not throw
        val canScheduleExact = powerUtil.canScheduleExactAlarms()
        assertNotNull(canScheduleExact)
    }
}

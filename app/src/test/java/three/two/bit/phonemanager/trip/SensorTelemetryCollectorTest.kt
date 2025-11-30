package three.two.bit.phonemanager.trip

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Story E8.5: SensorTelemetryCollector Unit Tests
 *
 * Tests for AccelerometerBuffer and TelemetrySnapshot data class.
 * Note: Full SensorTelemetryCollector requires Android context and cannot be unit tested,
 * but AccelerometerBuffer is pure Kotlin and can be tested.
 */
class SensorTelemetryCollectorTest {

    // region AccelerometerBuffer Tests

    @Test
    fun `AccelerometerBuffer returns null when empty`() {
        val buffer = AccelerometerBuffer()

        assertNull(buffer.getMagnitude())
        assertNull(buffer.getVariance())
        assertNull(buffer.getPeakFrequency())
    }

    @Test
    fun `AccelerometerBuffer calculates magnitude correctly`() {
        val buffer = AccelerometerBuffer()

        // Add a sample with x=3, y=4, z=0 -> magnitude = 5
        buffer.add(3f, 4f, 0f)

        val magnitude = buffer.getMagnitude()
        assertNotNull(magnitude)
        assertEquals(5f, magnitude, 0.01f)
    }

    @Test
    fun `AccelerometerBuffer calculates average magnitude`() {
        val buffer = AccelerometerBuffer()

        // Add samples: magnitude 5, then magnitude 10
        buffer.add(3f, 4f, 0f) // magnitude = 5
        buffer.add(6f, 8f, 0f) // magnitude = 10

        val avgMagnitude = buffer.getMagnitude()
        assertNotNull(avgMagnitude)
        assertEquals(7.5f, avgMagnitude, 0.01f)
    }

    @Test
    fun `AccelerometerBuffer returns null variance with single sample`() {
        val buffer = AccelerometerBuffer()

        buffer.add(3f, 4f, 0f)

        assertNull(buffer.getVariance())
    }

    @Test
    fun `AccelerometerBuffer calculates variance correctly`() {
        val buffer = AccelerometerBuffer()

        // Add samples with magnitudes 5 and 10
        // Mean = 7.5, variance = ((5-7.5)^2 + (10-7.5)^2) / 2 = (6.25 + 6.25) / 2 = 6.25
        buffer.add(3f, 4f, 0f) // magnitude = 5
        buffer.add(6f, 8f, 0f) // magnitude = 10

        val variance = buffer.getVariance()
        assertNotNull(variance)
        assertEquals(6.25f, variance, 0.01f)
    }

    @Test
    fun `AccelerometerBuffer returns null peak frequency with insufficient samples`() {
        val buffer = AccelerometerBuffer()

        buffer.add(3f, 4f, 0f)
        buffer.add(3f, 4f, 0f)

        assertNull(buffer.getPeakFrequency())
    }

    @Test
    fun `AccelerometerBuffer size returns correct count`() {
        val buffer = AccelerometerBuffer()

        assertEquals(0, buffer.size())

        buffer.add(1f, 0f, 0f)
        assertEquals(1, buffer.size())

        buffer.add(2f, 0f, 0f)
        assertEquals(2, buffer.size())
    }

    @Test
    fun `AccelerometerBuffer clear removes all samples`() {
        val buffer = AccelerometerBuffer()

        buffer.add(1f, 0f, 0f)
        buffer.add(2f, 0f, 0f)
        buffer.add(3f, 0f, 0f)

        assertEquals(3, buffer.size())

        buffer.clear()

        assertEquals(0, buffer.size())
        assertNull(buffer.getMagnitude())
    }

    // endregion

    // region TelemetrySnapshot Tests

    @Test
    fun `TelemetrySnapshot can be created with all null values`() {
        val snapshot = SensorTelemetryCollector.TelemetrySnapshot(
            accelerometerMagnitude = null,
            accelerometerVariance = null,
            accelerometerPeakFrequency = null,
            gyroscopeMagnitude = null,
            stepCount = null,
            significantMotion = null,
            batteryLevel = null,
            batteryCharging = null,
            networkType = null,
            networkStrength = null,
        )

        assertNull(snapshot.accelerometerMagnitude)
        assertNull(snapshot.gyroscopeMagnitude)
        assertNull(snapshot.stepCount)
        assertNull(snapshot.batteryLevel)
    }

    @Test
    fun `TelemetrySnapshot can be created with all values`() {
        val snapshot = SensorTelemetryCollector.TelemetrySnapshot(
            accelerometerMagnitude = 9.8f,
            accelerometerVariance = 0.5f,
            accelerometerPeakFrequency = 2.5f,
            gyroscopeMagnitude = 0.1f,
            stepCount = 1000,
            significantMotion = true,
            batteryLevel = 85,
            batteryCharging = false,
            networkType = "WIFI",
            networkStrength = -50,
        )

        assertEquals(9.8f, snapshot.accelerometerMagnitude)
        assertEquals(0.5f, snapshot.accelerometerVariance)
        assertEquals(2.5f, snapshot.accelerometerPeakFrequency)
        assertEquals(0.1f, snapshot.gyroscopeMagnitude)
        assertEquals(1000, snapshot.stepCount)
        assertTrue(snapshot.significantMotion!!)
        assertEquals(85, snapshot.batteryLevel)
        assertFalse(snapshot.batteryCharging!!)
        assertEquals("WIFI", snapshot.networkType)
        assertEquals(-50, snapshot.networkStrength)
    }

    @Test
    fun `TelemetrySnapshot supports data class copy`() {
        val original = SensorTelemetryCollector.TelemetrySnapshot(
            accelerometerMagnitude = 9.8f,
            accelerometerVariance = null,
            accelerometerPeakFrequency = null,
            gyroscopeMagnitude = null,
            stepCount = null,
            significantMotion = null,
            batteryLevel = 50,
            batteryCharging = true,
            networkType = "MOBILE",
            networkStrength = null,
        )

        val updated = original.copy(batteryLevel = 75)

        assertEquals(9.8f, updated.accelerometerMagnitude)
        assertEquals(75, updated.batteryLevel)
        assertTrue(updated.batteryCharging!!)
    }

    @Test
    fun `TelemetrySnapshot network types are valid`() {
        val wifiSnapshot = SensorTelemetryCollector.TelemetrySnapshot(
            accelerometerMagnitude = null,
            accelerometerVariance = null,
            accelerometerPeakFrequency = null,
            gyroscopeMagnitude = null,
            stepCount = null,
            significantMotion = null,
            batteryLevel = null,
            batteryCharging = null,
            networkType = "WIFI",
            networkStrength = null,
        )

        val mobileSnapshot = wifiSnapshot.copy(networkType = "MOBILE")
        val noneSnapshot = wifiSnapshot.copy(networkType = "NONE")
        val unknownSnapshot = wifiSnapshot.copy(networkType = "UNKNOWN")

        assertEquals("WIFI", wifiSnapshot.networkType)
        assertEquals("MOBILE", mobileSnapshot.networkType)
        assertEquals("NONE", noneSnapshot.networkType)
        assertEquals("UNKNOWN", unknownSnapshot.networkType)
    }

    // endregion
}

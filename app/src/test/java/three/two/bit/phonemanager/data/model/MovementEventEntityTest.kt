package three.two.bit.phonemanager.data.model

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Story E8.1: MovementEventEntity Unit Tests
 *
 * Tests for movement event entity structure, defaults, and field values
 * Coverage target: > 80%
 */
class MovementEventEntityTest {

    // region Entity Creation Tests

    @Test
    fun `MovementEventEntity default values are set correctly`() {
        val event = createMinimalMovementEvent()

        assertEquals(0L, event.id)
        assertNull(event.tripId)
        assertNull(event.latitude)
        assertNull(event.longitude)
        assertNull(event.accuracy)
        assertNull(event.speed)
        assertNull(event.batteryLevel)
        assertNull(event.batteryCharging)
        assertNull(event.networkType)
        assertNull(event.networkStrength)
        assertNull(event.accelerometerMagnitude)
        assertNull(event.accelerometerVariance)
        assertNull(event.accelerometerPeakFrequency)
        assertNull(event.gyroscopeMagnitude)
        assertNull(event.stepCount)
        assertNull(event.significantMotion)
        assertNull(event.activityType)
        assertNull(event.activityConfidence)
        assertNull(event.distanceFromLastLocation)
        assertNull(event.timeSinceLastLocation)
        assertFalse(event.isSynced)
        assertNull(event.syncedAt)
    }

    @Test
    fun `MovementEventEntity accepts all required fields`() {
        val event = MovementEventEntity(
            timestamp = 1700000000000L,
            tripId = "trip-123",
            previousMode = "STATIONARY",
            newMode = "WALKING",
            detectionSource = "ACTIVITY_RECOGNITION",
            confidence = 0.85f,
            detectionLatencyMs = 150L,
        )

        assertEquals(1700000000000L, event.timestamp)
        assertEquals("trip-123", event.tripId)
        assertEquals("STATIONARY", event.previousMode)
        assertEquals("WALKING", event.newMode)
        assertEquals("ACTIVITY_RECOGNITION", event.detectionSource)
        assertEquals(0.85f, event.confidence, 0.001f)
        assertEquals(150L, event.detectionLatencyMs)
    }

    @Test
    fun `MovementEventEntity accepts all optional location fields`() {
        val event = createMinimalMovementEvent().copy(
            latitude = 48.8566,
            longitude = 2.3522,
            accuracy = 5.5f,
            speed = 1.2f,
        )

        assertEquals(48.8566, event.latitude!!, 0.0001)
        assertEquals(2.3522, event.longitude!!, 0.0001)
        assertEquals(5.5f, event.accuracy!!, 0.01f)
        assertEquals(1.2f, event.speed!!, 0.01f)
    }

    @Test
    fun `MovementEventEntity accepts all device state fields`() {
        val event = createMinimalMovementEvent().copy(
            batteryLevel = 75,
            batteryCharging = true,
            networkType = "WIFI",
            networkStrength = 4,
        )

        assertEquals(75, event.batteryLevel)
        assertTrue(event.batteryCharging!!)
        assertEquals("WIFI", event.networkType)
        assertEquals(4, event.networkStrength)
    }

    @Test
    fun `MovementEventEntity accepts all sensor telemetry fields`() {
        val event = createMinimalMovementEvent().copy(
            accelerometerMagnitude = 9.81f,
            accelerometerVariance = 0.15f,
            accelerometerPeakFrequency = 2.5f,
            gyroscopeMagnitude = 0.02f,
            stepCount = 1234,
            significantMotion = true,
        )

        assertEquals(9.81f, event.accelerometerMagnitude!!, 0.01f)
        assertEquals(0.15f, event.accelerometerVariance!!, 0.001f)
        assertEquals(2.5f, event.accelerometerPeakFrequency!!, 0.01f)
        assertEquals(0.02f, event.gyroscopeMagnitude!!, 0.001f)
        assertEquals(1234, event.stepCount)
        assertTrue(event.significantMotion!!)
    }

    @Test
    fun `MovementEventEntity accepts activity recognition fields`() {
        val event = createMinimalMovementEvent().copy(
            activityType = "WALKING",
            activityConfidence = 92,
        )

        assertEquals("WALKING", event.activityType)
        assertEquals(92, event.activityConfidence)
    }

    @Test
    fun `MovementEventEntity accepts movement context fields`() {
        val event = createMinimalMovementEvent().copy(
            distanceFromLastLocation = 125.5f,
            timeSinceLastLocation = 30000L,
        )

        assertEquals(125.5f, event.distanceFromLastLocation!!, 0.01f)
        assertEquals(30000L, event.timeSinceLastLocation)
    }

    // endregion

    // region Transportation Mode Tests

    @Test
    fun `MovementEventEntity accepts STATIONARY mode`() {
        val event = createMinimalMovementEvent(previousMode = "STATIONARY", newMode = "WALKING")
        assertEquals("STATIONARY", event.previousMode)
        assertEquals("WALKING", event.newMode)
    }

    @Test
    fun `MovementEventEntity accepts WALKING mode`() {
        val event = createMinimalMovementEvent(previousMode = "WALKING", newMode = "RUNNING")
        assertEquals("WALKING", event.previousMode)
        assertEquals("RUNNING", event.newMode)
    }

    @Test
    fun `MovementEventEntity accepts RUNNING mode`() {
        val event = createMinimalMovementEvent(previousMode = "RUNNING", newMode = "WALKING")
        assertEquals("RUNNING", event.previousMode)
    }

    @Test
    fun `MovementEventEntity accepts CYCLING mode`() {
        val event = createMinimalMovementEvent(previousMode = "CYCLING", newMode = "STATIONARY")
        assertEquals("CYCLING", event.previousMode)
    }

    @Test
    fun `MovementEventEntity accepts DRIVING mode`() {
        val event = createMinimalMovementEvent(previousMode = "DRIVING", newMode = "STATIONARY")
        assertEquals("DRIVING", event.previousMode)
    }

    @Test
    fun `MovementEventEntity accepts TRANSIT mode`() {
        val event = createMinimalMovementEvent(previousMode = "TRANSIT", newMode = "WALKING")
        assertEquals("TRANSIT", event.previousMode)
    }

    @Test
    fun `MovementEventEntity accepts UNKNOWN mode`() {
        val event = createMinimalMovementEvent(previousMode = "UNKNOWN", newMode = "WALKING")
        assertEquals("UNKNOWN", event.previousMode)
    }

    // endregion

    // region Detection Source Tests

    @Test
    fun `MovementEventEntity accepts ACTIVITY_RECOGNITION source`() {
        val event = createMinimalMovementEvent(detectionSource = "ACTIVITY_RECOGNITION")
        assertEquals("ACTIVITY_RECOGNITION", event.detectionSource)
    }

    @Test
    fun `MovementEventEntity accepts SENSOR_FUSION source`() {
        val event = createMinimalMovementEvent(detectionSource = "SENSOR_FUSION")
        assertEquals("SENSOR_FUSION", event.detectionSource)
    }

    @Test
    fun `MovementEventEntity accepts SPEED_BASED source`() {
        val event = createMinimalMovementEvent(detectionSource = "SPEED_BASED")
        assertEquals("SPEED_BASED", event.detectionSource)
    }

    @Test
    fun `MovementEventEntity accepts MANUAL source`() {
        val event = createMinimalMovementEvent(detectionSource = "MANUAL")
        assertEquals("MANUAL", event.detectionSource)
    }

    @Test
    fun `MovementEventEntity accepts GEOFENCE source`() {
        val event = createMinimalMovementEvent(detectionSource = "GEOFENCE")
        assertEquals("GEOFENCE", event.detectionSource)
    }

    // endregion

    // region Confidence Tests

    @Test
    fun `MovementEventEntity confidence range 0 to 1`() {
        val lowConfidence = createMinimalMovementEvent(confidence = 0.0f)
        assertEquals(0.0f, lowConfidence.confidence, 0.001f)

        val highConfidence = createMinimalMovementEvent(confidence = 1.0f)
        assertEquals(1.0f, highConfidence.confidence, 0.001f)

        val midConfidence = createMinimalMovementEvent(confidence = 0.75f)
        assertEquals(0.75f, midConfidence.confidence, 0.001f)
    }

    // endregion

    // region Sync Tracking Tests

    @Test
    fun `MovementEventEntity isSynced defaults to false`() {
        val event = createMinimalMovementEvent()
        assertFalse(event.isSynced)
    }

    @Test
    fun `MovementEventEntity can be marked as synced`() {
        val syncedAt = System.currentTimeMillis()
        val event = createMinimalMovementEvent().copy(
            isSynced = true,
            syncedAt = syncedAt,
        )

        assertTrue(event.isSynced)
        assertEquals(syncedAt, event.syncedAt)
    }

    // endregion

    // region Network Type Tests

    @Test
    fun `MovementEventEntity accepts WIFI networkType`() {
        val event = createMinimalMovementEvent().copy(networkType = "WIFI")
        assertEquals("WIFI", event.networkType)
    }

    @Test
    fun `MovementEventEntity accepts CELLULAR networkType`() {
        val event = createMinimalMovementEvent().copy(networkType = "CELLULAR")
        assertEquals("CELLULAR", event.networkType)
    }

    @Test
    fun `MovementEventEntity accepts NONE networkType`() {
        val event = createMinimalMovementEvent().copy(networkType = "NONE")
        assertEquals("NONE", event.networkType)
    }

    // endregion

    // region Full Event Test

    @Test
    fun `MovementEventEntity can be created with all fields populated`() {
        val event = MovementEventEntity(
            id = 1L,
            timestamp = 1700000000000L,
            tripId = "trip-456",
            previousMode = "STATIONARY",
            newMode = "WALKING",
            detectionSource = "ACTIVITY_RECOGNITION",
            confidence = 0.92f,
            detectionLatencyMs = 120L,
            latitude = 48.8566,
            longitude = 2.3522,
            accuracy = 4.5f,
            speed = 1.3f,
            batteryLevel = 82,
            batteryCharging = false,
            networkType = "CELLULAR",
            networkStrength = 3,
            accelerometerMagnitude = 9.78f,
            accelerometerVariance = 0.12f,
            accelerometerPeakFrequency = 2.1f,
            gyroscopeMagnitude = 0.015f,
            stepCount = 5678,
            significantMotion = true,
            activityType = "WALKING",
            activityConfidence = 90,
            distanceFromLastLocation = 15.2f,
            timeSinceLastLocation = 12000L,
            isSynced = false,
            syncedAt = null,
        )

        assertEquals(1L, event.id)
        assertEquals("trip-456", event.tripId)
        assertEquals("STATIONARY", event.previousMode)
        assertEquals("WALKING", event.newMode)
        assertEquals("ACTIVITY_RECOGNITION", event.detectionSource)
        assertEquals(0.92f, event.confidence, 0.001f)
        assertEquals(120L, event.detectionLatencyMs)
        assertEquals(48.8566, event.latitude!!, 0.0001)
        assertEquals(2.3522, event.longitude!!, 0.0001)
        assertEquals(4.5f, event.accuracy!!, 0.01f)
        assertEquals(1.3f, event.speed!!, 0.01f)
        assertEquals(82, event.batteryLevel)
        assertFalse(event.batteryCharging!!)
        assertEquals("CELLULAR", event.networkType)
        assertEquals(3, event.networkStrength)
        assertEquals(9.78f, event.accelerometerMagnitude!!, 0.01f)
        assertEquals(0.12f, event.accelerometerVariance!!, 0.001f)
        assertEquals(2.1f, event.accelerometerPeakFrequency!!, 0.01f)
        assertEquals(0.015f, event.gyroscopeMagnitude!!, 0.0001f)
        assertEquals(5678, event.stepCount)
        assertTrue(event.significantMotion!!)
        assertEquals("WALKING", event.activityType)
        assertEquals(90, event.activityConfidence)
        assertEquals(15.2f, event.distanceFromLastLocation!!, 0.01f)
        assertEquals(12000L, event.timeSinceLastLocation)
    }

    // endregion

    // Helper functions

    private fun createMinimalMovementEvent(
        previousMode: String = "STATIONARY",
        newMode: String = "WALKING",
        detectionSource: String = "ACTIVITY_RECOGNITION",
        confidence: Float = 0.8f,
    ) = MovementEventEntity(
        timestamp = System.currentTimeMillis(),
        tripId = null,
        previousMode = previousMode,
        newMode = newMode,
        detectionSource = detectionSource,
        confidence = confidence,
        detectionLatencyMs = 100L,
    )
}

package three.two.bit.phonemanager.data.model

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Story E8.1: TripEntity Unit Tests
 *
 * Tests for trip entity structure, defaults, and state values
 * Coverage target: > 80%
 */
class TripEntityTest {

    // region Entity Creation Tests

    @Test
    fun `TripEntity default values are set correctly`() {
        val trip = createMinimalTrip()

        assertEquals(0.0, trip.totalDistanceMeters)
        assertEquals(0, trip.locationCount)
        assertFalse(trip.isSynced)
        assertNull(trip.syncedAt)
        assertNull(trip.serverId)
        assertTrue(trip.createdAt > 0)
        assertTrue(trip.updatedAt > 0)
    }

    @Test
    fun `TripEntity accepts all required fields`() {
        val trip = TripEntity(
            id = "trip-123",
            state = "ACTIVE",
            startTime = 1700000000000L,
            endTime = null,
            startLatitude = 48.8566,
            startLongitude = 2.3522,
            endLatitude = null,
            endLongitude = null,
            dominantMode = "WALKING",
            modesUsedJson = "[\"WALKING\"]",
            modeBreakdownJson = "{\"WALKING\": 100.0}",
            startTrigger = "MANUAL",
            endTrigger = null,
        )

        assertEquals("trip-123", trip.id)
        assertEquals("ACTIVE", trip.state)
        assertEquals(1700000000000L, trip.startTime)
        assertNull(trip.endTime)
        assertEquals(48.8566, trip.startLatitude, 0.0001)
        assertEquals(2.3522, trip.startLongitude, 0.0001)
        assertNull(trip.endLatitude)
        assertNull(trip.endLongitude)
        assertEquals("WALKING", trip.dominantMode)
        assertEquals("[\"WALKING\"]", trip.modesUsedJson)
        assertEquals("{\"WALKING\": 100.0}", trip.modeBreakdownJson)
        assertEquals("MANUAL", trip.startTrigger)
        assertNull(trip.endTrigger)
    }

    @Test
    fun `TripEntity accepts end state values`() {
        val trip = TripEntity(
            id = "trip-completed",
            state = "COMPLETED",
            startTime = 1700000000000L,
            endTime = 1700003600000L,
            startLatitude = 48.8566,
            startLongitude = 2.3522,
            endLatitude = 48.8584,
            endLongitude = 2.2945,
            totalDistanceMeters = 5234.5,
            locationCount = 150,
            dominantMode = "DRIVING",
            modesUsedJson = "[\"WALKING\", \"DRIVING\"]",
            modeBreakdownJson = "{\"WALKING\": 10.0, \"DRIVING\": 90.0}",
            startTrigger = "ACTIVITY_DETECTION",
            endTrigger = "STATIONARY_DETECTION",
        )

        assertEquals("COMPLETED", trip.state)
        assertEquals(1700003600000L, trip.endTime)
        assertEquals(48.8584, trip.endLatitude!!, 0.0001)
        assertEquals(2.2945, trip.endLongitude!!, 0.0001)
        assertEquals(5234.5, trip.totalDistanceMeters, 0.01)
        assertEquals(150, trip.locationCount)
        assertEquals("STATIONARY_DETECTION", trip.endTrigger)
    }

    // endregion

    // region State Value Tests

    @Test
    fun `TripEntity state IDLE is valid`() {
        val trip = createMinimalTrip(state = "IDLE")
        assertEquals("IDLE", trip.state)
    }

    @Test
    fun `TripEntity state ACTIVE is valid`() {
        val trip = createMinimalTrip(state = "ACTIVE")
        assertEquals("ACTIVE", trip.state)
    }

    @Test
    fun `TripEntity state PENDING_END is valid`() {
        val trip = createMinimalTrip(state = "PENDING_END")
        assertEquals("PENDING_END", trip.state)
    }

    @Test
    fun `TripEntity state COMPLETED is valid`() {
        val trip = createMinimalTrip(state = "COMPLETED")
        assertEquals("COMPLETED", trip.state)
    }

    // endregion

    // region Trigger Value Tests

    @Test
    fun `TripEntity accepts MANUAL startTrigger`() {
        val trip = createMinimalTrip(startTrigger = "MANUAL")
        assertEquals("MANUAL", trip.startTrigger)
    }

    @Test
    fun `TripEntity accepts ACTIVITY_DETECTION startTrigger`() {
        val trip = createMinimalTrip(startTrigger = "ACTIVITY_DETECTION")
        assertEquals("ACTIVITY_DETECTION", trip.startTrigger)
    }

    @Test
    fun `TripEntity accepts GEOFENCE_EXIT startTrigger`() {
        val trip = createMinimalTrip(startTrigger = "GEOFENCE_EXIT")
        assertEquals("GEOFENCE_EXIT", trip.startTrigger)
    }

    @Test
    fun `TripEntity accepts SIGNIFICANT_MOTION startTrigger`() {
        val trip = createMinimalTrip(startTrigger = "SIGNIFICANT_MOTION")
        assertEquals("SIGNIFICANT_MOTION", trip.startTrigger)
    }

    // endregion

    // region Transportation Mode Tests

    @Test
    fun `TripEntity accepts WALKING dominantMode`() {
        val trip = createMinimalTrip(dominantMode = "WALKING")
        assertEquals("WALKING", trip.dominantMode)
    }

    @Test
    fun `TripEntity accepts RUNNING dominantMode`() {
        val trip = createMinimalTrip(dominantMode = "RUNNING")
        assertEquals("RUNNING", trip.dominantMode)
    }

    @Test
    fun `TripEntity accepts CYCLING dominantMode`() {
        val trip = createMinimalTrip(dominantMode = "CYCLING")
        assertEquals("CYCLING", trip.dominantMode)
    }

    @Test
    fun `TripEntity accepts DRIVING dominantMode`() {
        val trip = createMinimalTrip(dominantMode = "DRIVING")
        assertEquals("DRIVING", trip.dominantMode)
    }

    @Test
    fun `TripEntity accepts TRANSIT dominantMode`() {
        val trip = createMinimalTrip(dominantMode = "TRANSIT")
        assertEquals("TRANSIT", trip.dominantMode)
    }

    @Test
    fun `TripEntity accepts STATIONARY dominantMode`() {
        val trip = createMinimalTrip(dominantMode = "STATIONARY")
        assertEquals("STATIONARY", trip.dominantMode)
    }

    @Test
    fun `TripEntity accepts UNKNOWN dominantMode`() {
        val trip = createMinimalTrip(dominantMode = "UNKNOWN")
        assertEquals("UNKNOWN", trip.dominantMode)
    }

    // endregion

    // region Sync Tracking Tests

    @Test
    fun `TripEntity isSynced defaults to false`() {
        val trip = createMinimalTrip()
        assertFalse(trip.isSynced)
    }

    @Test
    fun `TripEntity can be marked as synced`() {
        val syncedAt = System.currentTimeMillis()
        val trip = createMinimalTrip().copy(
            isSynced = true,
            syncedAt = syncedAt,
            serverId = "server-trip-123",
        )

        assertTrue(trip.isSynced)
        assertEquals(syncedAt, trip.syncedAt)
        assertEquals("server-trip-123", trip.serverId)
    }

    // endregion

    // region JSON Fields Tests

    @Test
    fun `TripEntity modesUsedJson stores array format`() {
        val modesJson = "[\"WALKING\", \"DRIVING\", \"TRANSIT\"]"
        val trip = createMinimalTrip(modesUsedJson = modesJson)
        assertEquals(modesJson, trip.modesUsedJson)
    }

    @Test
    fun `TripEntity modeBreakdownJson stores object format`() {
        val breakdownJson = "{\"WALKING\": 25.5, \"DRIVING\": 60.0, \"TRANSIT\": 14.5}"
        val trip = createMinimalTrip(modeBreakdownJson = breakdownJson)
        assertEquals(breakdownJson, trip.modeBreakdownJson)
    }

    // endregion

    // Helper functions

    private fun createMinimalTrip(
        id: String = "test-trip",
        state: String = "IDLE",
        startTrigger: String = "MANUAL",
        dominantMode: String = "UNKNOWN",
        modesUsedJson: String = "[\"UNKNOWN\"]",
        modeBreakdownJson: String = "{\"UNKNOWN\": 100.0}",
    ) = TripEntity(
        id = id,
        state = state,
        startTime = System.currentTimeMillis(),
        endTime = null,
        startLatitude = 48.8566,
        startLongitude = 2.3522,
        endLatitude = null,
        endLongitude = null,
        dominantMode = dominantMode,
        modesUsedJson = modesUsedJson,
        modeBreakdownJson = modeBreakdownJson,
        startTrigger = startTrigger,
        endTrigger = null,
    )
}

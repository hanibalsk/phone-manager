package three.two.bit.phonemanager.util

import com.google.android.gms.maps.model.LatLng
import org.junit.Test
import three.two.bit.phonemanager.domain.model.AlertDirection
import three.two.bit.phonemanager.domain.model.ProximityAlert
import three.two.bit.phonemanager.domain.model.ProximityState
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Clock

class ProximityCalculatorTest {

    private val now = Clock.System.now()

    @Test
    fun `calculateDistance returns correct distance for same point`() {
        // Given
        val lat = 48.1486
        val lon = 17.1077

        // When
        val distance = ProximityCalculator.calculateDistance(lat, lon, lat, lon)

        // Then
        assertEquals(0f, distance, 0.1f)
    }

    @Test
    fun `calculateDistance calculates correct distance between points`() {
        // Given - approximately 1km apart
        val point1 = LatLng(48.1486, 17.1077) // Bratislava
        val point2 = LatLng(48.1486, 17.1200) // ~1km east

        // When
        val distance = ProximityCalculator.calculateDistance(point1, point2)

        // Then - roughly 900-1000 meters
        assertTrue(distance in 800f..1100f, "Distance $distance should be around 900-1000m")
    }

    @Test
    fun `checkProximity detects INSIDE state when within radius`() {
        // Given
        val myLocation = LatLng(48.1486, 17.1077)
        val targetLocation = LatLng(48.1487, 17.1078) // ~100m away
        val alert =
            createAlert(
                radiusMeters = 200,
                direction = AlertDirection.ENTER,
                lastState = ProximityState.OUTSIDE,
            )

        // When
        val result = ProximityCalculator.checkProximity(myLocation, targetLocation, alert)

        // Then
        assertEquals(ProximityState.INSIDE, result.newState)
        assertTrue(result.distance < 200f)
    }

    @Test
    fun `checkProximity detects OUTSIDE state when beyond radius`() {
        // Given
        val myLocation = LatLng(48.1486, 17.1077)
        val targetLocation = LatLng(48.1600, 17.1200) // ~1.5km away
        val alert =
            createAlert(
                radiusMeters = 500,
                direction = AlertDirection.EXIT,
                lastState = ProximityState.INSIDE,
            )

        // When
        val result = ProximityCalculator.checkProximity(myLocation, targetLocation, alert)

        // Then
        assertEquals(ProximityState.OUTSIDE, result.newState)
        assertTrue(result.distance > 500f)
    }

    @Test
    fun `checkProximity triggers on ENTER when transitioning OUTSIDE to INSIDE`() {
        // Given
        val myLocation = LatLng(48.1486, 17.1077)
        val targetLocation = LatLng(48.1487, 17.1078) // ~100m away
        val alert =
            createAlert(
                radiusMeters = 200,
                direction = AlertDirection.ENTER,
                lastState = ProximityState.OUTSIDE,
            )

        // When
        val result = ProximityCalculator.checkProximity(myLocation, targetLocation, alert)

        // Then
        assertTrue(result.triggered, "Should trigger on OUTSIDE→INSIDE for ENTER direction")
    }

    @Test
    fun `checkProximity does not trigger on ENTER when already INSIDE`() {
        // Given
        val myLocation = LatLng(48.1486, 17.1077)
        val targetLocation = LatLng(48.1487, 17.1078) // ~100m away
        val alert =
            createAlert(
                radiusMeters = 200,
                direction = AlertDirection.ENTER,
                lastState = ProximityState.INSIDE, // Already inside
            )

        // When
        val result = ProximityCalculator.checkProximity(myLocation, targetLocation, alert)

        // Then
        assertFalse(result.triggered, "Should NOT trigger when already INSIDE (no state change)")
    }

    @Test
    fun `checkProximity triggers on EXIT when transitioning INSIDE to OUTSIDE`() {
        // Given
        val myLocation = LatLng(48.1486, 17.1077)
        val targetLocation = LatLng(48.1600, 17.1200) // ~1.5km away
        val alert =
            createAlert(
                radiusMeters = 500,
                direction = AlertDirection.EXIT,
                lastState = ProximityState.INSIDE,
            )

        // When
        val result = ProximityCalculator.checkProximity(myLocation, targetLocation, alert)

        // Then
        assertTrue(result.triggered, "Should trigger on INSIDE→OUTSIDE for EXIT direction")
    }

    @Test
    fun `checkProximity does not trigger on EXIT when already OUTSIDE`() {
        // Given
        val myLocation = LatLng(48.1486, 17.1077)
        val targetLocation = LatLng(48.1600, 17.1200) // ~1.5km away
        val alert =
            createAlert(
                radiusMeters = 500,
                direction = AlertDirection.EXIT,
                lastState = ProximityState.OUTSIDE, // Already outside
            )

        // When
        val result = ProximityCalculator.checkProximity(myLocation, targetLocation, alert)

        // Then
        assertFalse(result.triggered, "Should NOT trigger when already OUTSIDE (no state change)")
    }

    @Test
    fun `checkProximity triggers on BOTH for any state transition`() {
        // Given
        val myLocation = LatLng(48.1486, 17.1077)
        val targetInside = LatLng(48.1487, 17.1078) // ~100m away
        val targetOutside = LatLng(48.1600, 17.1200) // ~1.5km away

        // When - OUTSIDE→INSIDE transition
        val alertOutside =
            createAlert(
                radiusMeters = 200,
                direction = AlertDirection.BOTH,
                lastState = ProximityState.OUTSIDE,
            )
        val resultEnter = ProximityCalculator.checkProximity(myLocation, targetInside, alertOutside)

        // Then
        assertTrue(resultEnter.triggered, "BOTH should trigger on OUTSIDE→INSIDE")

        // When - INSIDE→OUTSIDE transition
        val alertInside =
            createAlert(
                radiusMeters = 200,
                direction = AlertDirection.BOTH,
                lastState = ProximityState.INSIDE,
            )
        val resultExit = ProximityCalculator.checkProximity(myLocation, targetOutside, alertInside)

        // Then
        assertTrue(resultExit.triggered, "BOTH should trigger on INSIDE→OUTSIDE")
    }

    @Test
    fun `checkProximity does not trigger on BOTH when state unchanged`() {
        // Given
        val myLocation = LatLng(48.1486, 17.1077)
        val targetLocation = LatLng(48.1487, 17.1078) // ~100m away
        val alert =
            createAlert(
                radiusMeters = 200,
                direction = AlertDirection.BOTH,
                lastState = ProximityState.INSIDE, // Already inside
            )

        // When
        val result = ProximityCalculator.checkProximity(myLocation, targetLocation, alert)

        // Then
        assertFalse(result.triggered, "Should NOT trigger when state unchanged, even with BOTH")
    }

    private fun createAlert(radiusMeters: Int, direction: AlertDirection, lastState: ProximityState): ProximityAlert =
        ProximityAlert(
            id = "test-alert",
            ownerDeviceId = "owner-123",
            targetDeviceId = "target-456",
            targetDisplayName = "Test User",
            radiusMeters = radiusMeters,
            direction = direction,
            active = true,
            lastState = lastState,
            createdAt = now,
            updatedAt = now,
            lastTriggeredAt = null,
        )
}

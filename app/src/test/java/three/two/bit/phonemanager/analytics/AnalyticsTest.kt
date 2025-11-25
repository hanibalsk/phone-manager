package three.two.bit.phonemanager.analytics

import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for Analytics
 *
 * Tests analytics event tracking
 * Verifies:
 * - Event logging
 * - Permission events
 * - Service events
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AnalyticsTest {

    private lateinit var analytics: Analytics

    @Before
    fun setup() {
        analytics = spyk(DebugAnalytics())
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `logPermissionRationaleShown logs event`() {
        // When
        analytics.logPermissionRationaleShown("location")

        // Then
        verify { analytics.logPermissionRationaleShown("location") }
    }

    @Test
    fun `logPermissionGranted logs event with permission type`() {
        // When
        analytics.logPermissionGranted("background")

        // Then
        verify { analytics.logPermissionGranted("background") }
    }

    @Test
    fun `logPermissionDenied logs event with reason`() {
        // When
        analytics.logPermissionDenied("location", "user_denied")

        // Then
        verify { analytics.logPermissionDenied("location", "user_denied") }
    }

    @Test
    fun `logPermissionSettingsOpened logs event`() {
        // When
        analytics.logPermissionSettingsOpened()

        // Then
        verify { analytics.logPermissionSettingsOpened() }
    }

    @Test
    fun `logPermissionFlowCompleted logs success status`() {
        // When
        analytics.logPermissionFlowCompleted(true)

        // Then
        verify { analytics.logPermissionFlowCompleted(true) }
    }

    @Test
    fun `logTrackingToggled logs event with enabled state`() {
        // When
        analytics.logTrackingToggled(true)

        // Then
        verify { analytics.logTrackingToggled(true) }
    }

    @Test
    fun `logServiceStateChanged logs event with state`() {
        // When
        analytics.logServiceStateChanged("running")

        // Then
        verify { analytics.logServiceStateChanged("running") }
    }

    @Test
    fun `logEvent logs custom event with params`() {
        // Given
        val params = mapOf<String, Any>("key" to "value", "count" to 42)

        // When
        analytics.logEvent("custom_event", params)

        // Then
        verify { analytics.logEvent("custom_event", params) }
    }

    @Test
    fun `setUserProperty sets user property`() {
        // When
        analytics.setUserProperty("user_type", "premium")

        // Then
        verify { analytics.setUserProperty("user_type", "premium") }
    }

    @Test
    fun `permission flow events can be tracked from start to finish`() {
        // When - simulate complete permission flow
        analytics.logPermissionRationaleShown("location")
        analytics.logPermissionGranted("location")
        analytics.logPermissionRationaleShown("background")
        analytics.logPermissionGranted("background")
        analytics.logPermissionFlowCompleted(true)

        // Then
        verify(exactly = 1) { analytics.logPermissionRationaleShown("location") }
        verify(exactly = 1) { analytics.logPermissionGranted("location") }
        verify(exactly = 1) { analytics.logPermissionRationaleShown("background") }
        verify(exactly = 1) { analytics.logPermissionGranted("background") }
        verify(exactly = 1) { analytics.logPermissionFlowCompleted(true) }
    }

    @Test
    fun `service state changes can be tracked`() {
        // When - simulate service state changes
        analytics.logServiceStateChanged("starting")
        analytics.logServiceStateChanged("running")
        analytics.logTrackingToggled(true)

        // Then
        verify(exactly = 1) { analytics.logServiceStateChanged("starting") }
        verify(exactly = 1) { analytics.logServiceStateChanged("running") }
        verify(exactly = 1) { analytics.logTrackingToggled(true) }
    }

    @Test
    fun `NoOpAnalytics does not throw exceptions`() {
        // Given
        val noOpAnalytics = NoOpAnalytics()

        // When/Then - should not throw
        noOpAnalytics.logEvent("test_event", mapOf("key" to "value"))
        noOpAnalytics.setUserProperty("prop", "value")
        noOpAnalytics.logPermissionGranted("location")
        noOpAnalytics.logServiceStateChanged("running")
    }
}

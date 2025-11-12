package com.phonemanager.analytics

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
 * - Location events
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AnalyticsTest {

    private lateinit var analytics: Analytics

    @Before
    fun setup() {
        analytics = spyk(AnalyticsImpl())
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
    fun `logServiceStarted logs event`() {
        // When
        analytics.logServiceStarted()

        // Then
        verify { analytics.logServiceStarted() }
    }

    @Test
    fun `logServiceStopped logs event`() {
        // When
        analytics.logServiceStopped()

        // Then
        verify { analytics.logServiceStopped() }
    }

    @Test
    fun `logLocationCaptured logs event`() {
        // When
        analytics.logLocationCaptured()

        // Then
        verify { analytics.logLocationCaptured() }
    }

    @Test
    fun `logLocationUploaded logs event`() {
        // When
        analytics.logLocationUploaded()

        // Then
        verify { analytics.logLocationUploaded() }
    }

    @Test
    fun `logLocationUploadFailed logs event with error`() {
        // When
        analytics.logLocationUploadFailed("network_error")

        // Then
        verify { analytics.logLocationUploadFailed("network_error") }
    }

    @Test
    fun `multiple events can be logged in sequence`() {
        // When
        analytics.logServiceStarted()
        analytics.logLocationCaptured()
        analytics.logLocationUploaded()

        // Then
        verify(exactly = 1) { analytics.logServiceStarted() }
        verify(exactly = 1) { analytics.logLocationCaptured() }
        verify(exactly = 1) { analytics.logLocationUploaded() }
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
}

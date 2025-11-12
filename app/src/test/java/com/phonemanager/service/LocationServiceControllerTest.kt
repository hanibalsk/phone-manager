package com.phonemanager.service

import android.content.Context
import android.content.Intent
import com.phonemanager.permission.PermissionManager
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertTrue

/**
 * Unit tests for LocationServiceController
 *
 * Tests service control operations
 * Verifies:
 * - Service start
 * - Service stop
 * - Permission checking
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LocationServiceControllerTest {

    private lateinit var serviceController: LocationServiceController
    private lateinit var context: Context
    private lateinit var permissionManager: PermissionManager

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        permissionManager = mockk(relaxed = true)

        every { context.startForegroundService(any()) } returns mockk(relaxed = true)
        every { context.stopService(any()) } returns true

        serviceController = LocationServiceController(context, permissionManager)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `startTracking succeeds when permissions granted`() = runTest {
        // Given
        every { permissionManager.hasLocationPermission() } returns true
        every { permissionManager.hasBackgroundLocationPermission() } returns true

        // When
        val result = serviceController.startTracking()

        // Then
        assertTrue(result.isSuccess)
        verify { context.startForegroundService(any()) }
    }

    @Test
    fun `startTracking fails when location permission not granted`() = runTest {
        // Given
        every { permissionManager.hasLocationPermission() } returns false

        // When
        val result = serviceController.startTracking()

        // Then
        assertTrue(result.isFailure)
        verify(exactly = 0) { context.startForegroundService(any()) }
    }

    @Test
    fun `startTracking fails when background permission not granted`() = runTest {
        // Given
        every { permissionManager.hasLocationPermission() } returns true
        every { permissionManager.hasBackgroundLocationPermission() } returns false

        // When
        val result = serviceController.startTracking()

        // Then
        assertTrue(result.isFailure)
        verify(exactly = 0) { context.startForegroundService(any()) }
    }

    @Test
    fun `stopTracking stops the service`() = runTest {
        // When
        val result = serviceController.stopTracking()

        // Then
        assertTrue(result.isSuccess)
        verify { context.stopService(any()) }
    }

    @Test
    fun `stopTracking returns success even if service not running`() = runTest {
        // Given
        every { context.stopService(any()) } returns false

        // When
        val result = serviceController.stopTracking()

        // Then
        assertTrue(result.isSuccess)
    }

    @Test
    fun `updateInterval sends intent with new interval`() = runTest {
        // Given
        val newInterval = 10

        // When
        val result = serviceController.updateInterval(newInterval)

        // Then
        assertTrue(result.isSuccess)
        verify {
            context.startForegroundService(match {
                it.getIntExtra(LocationTrackingService.EXTRA_INTERVAL_MINUTES, -1) == newInterval
            })
        }
    }

    @Test
    fun `startTracking sends correct action intent`() = runTest {
        // Given
        every { permissionManager.hasLocationPermission() } returns true
        every { permissionManager.hasBackgroundLocationPermission() } returns true

        // When
        serviceController.startTracking()

        // Then
        verify {
            context.startForegroundService(match {
                it.action == LocationTrackingService.ACTION_START_TRACKING
            })
        }
    }

    @Test
    fun `stopTracking sends correct action intent`() = runTest {
        // When
        serviceController.stopTracking()

        // Then
        verify {
            context.stopService(match {
                it.action == LocationTrackingService.ACTION_STOP_TRACKING
            })
        }
    }
}

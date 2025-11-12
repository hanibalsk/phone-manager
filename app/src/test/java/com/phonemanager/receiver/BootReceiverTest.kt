package com.phonemanager.receiver

import android.content.Context
import android.content.Intent
import com.phonemanager.data.model.HealthStatus
import com.phonemanager.data.model.ServiceHealth
import com.phonemanager.data.repository.LocationRepository
import com.phonemanager.service.ServiceController
import com.phonemanager.watchdog.WatchdogManager
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for BootReceiver
 *
 * Story 0.2.4: Tests service restart after boot
 * Verifies:
 * - Service restarts when it was running before reboot
 * - Service doesn't start when it wasn't running before reboot
 * - Handles BOOT_COMPLETED intent
 * - Handles QUICKBOOT_POWERON intent
 * - Watchdog is started after successful restore
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BootReceiverTest {

    private lateinit var bootReceiver: BootReceiver
    private lateinit var context: Context
    private lateinit var locationRepository: LocationRepository
    private lateinit var serviceController: ServiceController
    private lateinit var watchdogManager: WatchdogManager

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        locationRepository = mockk(relaxed = true)
        serviceController = mockk(relaxed = true)
        watchdogManager = mockk(relaxed = true)

        bootReceiver = BootReceiver()

        // Inject dependencies using reflection since @Inject can't be used in unit tests
        bootReceiver.apply {
            this::class.java.getDeclaredField("locationRepository").apply {
                isAccessible = true
                set(bootReceiver, locationRepository)
            }
            this::class.java.getDeclaredField("serviceController").apply {
                isAccessible = true
                set(bootReceiver, serviceController)
            }
            this::class.java.getDeclaredField("watchdogManager").apply {
                isAccessible = true
                set(bootReceiver, watchdogManager)
            }
        }
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `onReceive with BOOT_COMPLETED restarts service when it was running`() = runTest {
        // Given
        val intent = Intent(Intent.ACTION_BOOT_COMPLETED)
        val serviceHealth = ServiceHealth(
            isRunning = true,
            healthStatus = HealthStatus.HEALTHY,
            locationCount = 10
        )

        coEvery { locationRepository.getServiceHealth() } returns flowOf(serviceHealth)
        coEvery { serviceController.startTracking() } returns Result.success(Unit)
        coEvery { watchdogManager.startWatchdog() } just Runs

        // Mock goAsync
        val pendingResult = mockk<android.content.BroadcastReceiver.PendingResult>(relaxed = true)
        every { bootReceiver.goAsync() } returns pendingResult

        // When
        bootReceiver.onReceive(context, intent)

        // Wait for coroutine to complete
        kotlinx.coroutines.delay(100)

        // Then
        coVerify { locationRepository.getServiceHealth() }
        coVerify { serviceController.startTracking() }
        coVerify { watchdogManager.startWatchdog() }
        verify { pendingResult.finish() }
    }

    @Test
    fun `onReceive with BOOT_COMPLETED does not start service when it was not running`() = runTest {
        // Given
        val intent = Intent(Intent.ACTION_BOOT_COMPLETED)
        val serviceHealth = ServiceHealth(
            isRunning = false,
            healthStatus = HealthStatus.HEALTHY
        )

        coEvery { locationRepository.getServiceHealth() } returns flowOf(serviceHealth)

        // Mock goAsync
        val pendingResult = mockk<android.content.BroadcastReceiver.PendingResult>(relaxed = true)
        every { bootReceiver.goAsync() } returns pendingResult

        // When
        bootReceiver.onReceive(context, intent)

        // Wait for coroutine to complete
        kotlinx.coroutines.delay(100)

        // Then
        coVerify { locationRepository.getServiceHealth() }
        coVerify(exactly = 0) { serviceController.startTracking() }
        coVerify(exactly = 0) { watchdogManager.startWatchdog() }
        verify { pendingResult.finish() }
    }

    @Test
    fun `onReceive with QUICKBOOT_POWERON restarts service when it was running`() = runTest {
        // Given
        val intent = Intent("android.intent.action.QUICKBOOT_POWERON")
        val serviceHealth = ServiceHealth(
            isRunning = true,
            healthStatus = HealthStatus.HEALTHY,
            locationCount = 5
        )

        coEvery { locationRepository.getServiceHealth() } returns flowOf(serviceHealth)
        coEvery { serviceController.startTracking() } returns Result.success(Unit)
        coEvery { watchdogManager.startWatchdog() } just Runs

        // Mock goAsync
        val pendingResult = mockk<android.content.BroadcastReceiver.PendingResult>(relaxed = true)
        every { bootReceiver.goAsync() } returns pendingResult

        // When
        bootReceiver.onReceive(context, intent)

        // Wait for coroutine to complete
        kotlinx.coroutines.delay(100)

        // Then
        coVerify { locationRepository.getServiceHealth() }
        coVerify { serviceController.startTracking() }
        coVerify { watchdogManager.startWatchdog() }
    }

    @Test
    fun `onReceive handles service start failure gracefully`() = runTest {
        // Given
        val intent = Intent(Intent.ACTION_BOOT_COMPLETED)
        val serviceHealth = ServiceHealth(
            isRunning = true,
            healthStatus = HealthStatus.HEALTHY
        )
        val exception = Exception("Service start failed")

        coEvery { locationRepository.getServiceHealth() } returns flowOf(serviceHealth)
        coEvery { serviceController.startTracking() } returns Result.failure(exception)

        // Mock goAsync
        val pendingResult = mockk<android.content.BroadcastReceiver.PendingResult>(relaxed = true)
        every { bootReceiver.goAsync() } returns pendingResult

        // When
        bootReceiver.onReceive(context, intent)

        // Wait for coroutine to complete
        kotlinx.coroutines.delay(100)

        // Then
        coVerify { locationRepository.getServiceHealth() }
        coVerify { serviceController.startTracking() }
        coVerify(exactly = 0) { watchdogManager.startWatchdog() } // Should not start watchdog on failure
        verify { pendingResult.finish() }
    }

    @Test
    fun `onReceive handles exception in getServiceHealth gracefully`() = runTest {
        // Given
        val intent = Intent(Intent.ACTION_BOOT_COMPLETED)

        coEvery { locationRepository.getServiceHealth() } throws Exception("Database error")

        // Mock goAsync
        val pendingResult = mockk<android.content.BroadcastReceiver.PendingResult>(relaxed = true)
        every { bootReceiver.goAsync() } returns pendingResult

        // When
        bootReceiver.onReceive(context, intent)

        // Wait for coroutine to complete
        kotlinx.coroutines.delay(100)

        // Then
        coVerify { locationRepository.getServiceHealth() }
        coVerify(exactly = 0) { serviceController.startTracking() }
        verify { pendingResult.finish() } // Should still finish pending result
    }

    @Test
    fun `onReceive ignores unrelated intents`() = runTest {
        // Given
        val intent = Intent("android.intent.action.SOME_OTHER_ACTION")

        // Mock goAsync - should not be called for unrelated intents
        val pendingResult = mockk<android.content.BroadcastReceiver.PendingResult>(relaxed = true)
        every { bootReceiver.goAsync() } returns pendingResult

        // When
        bootReceiver.onReceive(context, intent)

        // Then
        coVerify(exactly = 0) { locationRepository.getServiceHealth() }
        coVerify(exactly = 0) { serviceController.startTracking() }
    }

    @Test
    fun `onReceive uses goAsync to allow coroutine work`() = runTest {
        // Given
        val intent = Intent(Intent.ACTION_BOOT_COMPLETED)
        val serviceHealth = ServiceHealth(
            isRunning = true,
            healthStatus = HealthStatus.HEALTHY
        )

        coEvery { locationRepository.getServiceHealth() } returns flowOf(serviceHealth)
        coEvery { serviceController.startTracking() } returns Result.success(Unit)
        coEvery { watchdogManager.startWatchdog() } just Runs

        // Mock goAsync
        val pendingResult = mockk<android.content.BroadcastReceiver.PendingResult>(relaxed = true)
        every { bootReceiver.goAsync() } returns pendingResult

        // When
        bootReceiver.onReceive(context, intent)

        // Wait for coroutine to complete
        kotlinx.coroutines.delay(100)

        // Then
        verify { bootReceiver.goAsync() } // Verify goAsync was called
        verify { pendingResult.finish() } // Verify finish was called
    }
}

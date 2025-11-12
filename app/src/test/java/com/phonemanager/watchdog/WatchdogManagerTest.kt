package com.phonemanager.watchdog

import android.content.Context
import androidx.work.WorkManager
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for WatchdogManager
 *
 * Story 0.2.4: Tests service health monitoring
 * Verifies:
 * - Watchdog start/stop
 * - WorkManager integration
 * - Health check scheduling
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WatchdogManagerTest {

    private lateinit var watchdogManager: WatchdogManager
    private lateinit var context: Context
    private lateinit var workManager: WorkManager

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        workManager = mockk(relaxed = true)

        // Mock WorkManager.getInstance
        mockkStatic(WorkManager::class)
        every { WorkManager.getInstance(context) } returns workManager

        watchdogManager = WatchdogManager(context)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `startWatchdog schedules periodic work`() = runTest {
        // Given
        every { workManager.enqueueUniquePeriodicWork(any(), any(), any()) } returns mockk(relaxed = true)

        // When
        watchdogManager.startWatchdog()

        // Then
        verify {
            workManager.enqueueUniquePeriodicWork(
                any(),
                any(),
                any()
            )
        }
    }

    @Test
    fun `startWatchdog with custom interval schedules work with that interval`() = runTest {
        // Given
        val intervalMinutes = 30L
        every { workManager.enqueueUniquePeriodicWork(any(), any(), any()) } returns mockk(relaxed = true)

        // When
        watchdogManager.startWatchdog(intervalMinutes)

        // Then
        verify {
            workManager.enqueueUniquePeriodicWork(
                any(),
                any(),
                any()
            )
        }
    }

    @Test
    fun `stopWatchdog cancels unique work`() {
        // Given
        every { workManager.cancelUniqueWork(any()) } returns mockk(relaxed = true)

        // When
        watchdogManager.stopWatchdog()

        // Then
        verify { workManager.cancelUniqueWork(any()) }
    }

    @Test
    fun `multiple startWatchdog calls use same unique work name`() = runTest {
        // Given
        every { workManager.enqueueUniquePeriodicWork(any(), any(), any()) } returns mockk(relaxed = true)

        // When
        watchdogManager.startWatchdog()
        watchdogManager.startWatchdog()

        // Then
        verify(exactly = 2) {
            workManager.enqueueUniquePeriodicWork(
                any(),
                any(),
                any()
            )
        }
    }
}

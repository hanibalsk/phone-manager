package com.phonemanager.ui.main

import app.cash.turbine.test
import com.phonemanager.analytics.Analytics
import com.phonemanager.data.preferences.PreferencesRepository
import com.phonemanager.data.repository.LocationRepository
import com.phonemanager.permission.PermissionManager
import com.phonemanager.permission.PermissionState
import com.phonemanager.service.LocationServiceController
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Unit tests for LocationTrackingViewModel
 *
 * Story 1.1: Tests toggle functionality and state management
 * Coverage target: > 80%
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LocationTrackingViewModelTest {

    private lateinit var viewModel: LocationTrackingViewModel
    private lateinit var preferencesRepository: PreferencesRepository
    private lateinit var serviceController: LocationServiceController
    private lateinit var permissionManager: PermissionManager
    private lateinit var locationRepository: LocationRepository
    private lateinit var analytics: Analytics

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        // Create mocks
        preferencesRepository = mockk(relaxed = true)
        serviceController = mockk(relaxed = true)
        permissionManager = mockk(relaxed = true)
        locationRepository = mockk(relaxed = true)
        analytics = mockk(relaxed = true)

        // Setup default mock responses
        every { preferencesRepository.isTrackingEnabled } returns flowOf(false)
        every { preferencesRepository.trackingInterval } returns flowOf(5)
        every { permissionManager.observePermissionState() } returns flowOf(PermissionState.AllGranted)
        every { serviceController.isServiceRunning() } returns false
        every { serviceController.observeEnhancedServiceState() } returns flowOf(mockk(relaxed = true))
        every { locationRepository.observeLocationCount() } returns flowOf(0)
        every { locationRepository.observeTodayLocationCount() } returns flowOf(0)
        every { locationRepository.observeLastLocation() } returns flowOf(null)
        every { locationRepository.observeAverageAccuracy() } returns flowOf(null)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is Stopped`() = runTest {
        viewModel = createViewModel()

        viewModel.trackingState.test {
            val state = awaitItem()
            assertIs<TrackingState.Stopped>(state)
        }
    }

    @Test
    fun `startTracking succeeds when permissions granted`() = runTest {
        every { permissionManager.hasAllRequiredPermissions() } returns true
        coEvery { serviceController.startTracking() } returns Result.success(Unit)

        viewModel = createViewModel()
        viewModel.toggleTracking()
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify service was started
        coVerify { serviceController.startTracking() }
        coVerify { preferencesRepository.setTrackingEnabled(true) }

        // Verify analytics tracking
        verify { analytics.logServiceStateChanged("starting") }
        verify { analytics.logTrackingToggled(true) }
        verify { analytics.logServiceStateChanged("running") }
    }

    @Test
    fun `startTracking fails when permissions not granted`() = runTest {
        every { permissionManager.hasAllRequiredPermissions() } returns false

        viewModel = createViewModel()
        viewModel.toggleTracking()
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify service was NOT started
        coVerify(exactly = 0) { serviceController.startTracking() }
        coVerify(exactly = 0) { analytics.logTrackingToggled(any()) }
    }

    @Test
    fun `stopTracking succeeds and updates state`() = runTest {
        every { permissionManager.hasAllRequiredPermissions() } returns true
        every { preferencesRepository.isTrackingEnabled } returns flowOf(true)
        every { serviceController.isServiceRunning() } returns true
        coEvery { serviceController.stopTracking() } returns Result.success(Unit)

        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.toggleTracking()
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify service was stopped
        coVerify { serviceController.stopTracking() }
        coVerify { preferencesRepository.setTrackingEnabled(false) }

        // Verify analytics tracking
        verify { analytics.logServiceStateChanged("stopping") }
        verify { analytics.logTrackingToggled(false) }
        verify { analytics.logServiceStateChanged("stopped") }
    }

    @Test
    fun `startTracking handles error gracefully`() = runTest {
        val errorMessage = "Service start failed"
        every { permissionManager.hasAllRequiredPermissions() } returns true
        coEvery { serviceController.startTracking() } returns Result.failure(Exception(errorMessage))

        viewModel = createViewModel()
        viewModel.toggleTracking()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.trackingState.test {
            val state = awaitItem()
            assertIs<TrackingState.Error>(state)
            assertEquals(errorMessage, state.message)
        }

        // Verify error analytics
        verify { analytics.logServiceStateChanged("error") }
    }

    @Test
    fun `ignore rapid toggle taps during transition`() = runTest {
        every { permissionManager.hasAllRequiredPermissions() } returns true
        coEvery { serviceController.startTracking() } returns Result.success(Unit)
        coEvery { serviceController.stopTracking() } returns Result.success(Unit)

        viewModel = createViewModel()

        // First toggle starts tracking - state goes from Stopped -> Starting
        viewModel.toggleTracking()

        // Second toggle while in Starting state should be ignored
        // (before advanceUntilIdle completes the first operation)
        viewModel.toggleTracking()

        // Now advance to let the coroutines complete
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify service start was called only once (second toggle was ignored during Starting state)
        coVerify(exactly = 1) { serviceController.startTracking() }
    }

    @Test
    fun `state reconciliation detects desync and attempts restart when permissions granted`() = runTest {
        // Story 1.4: DataStore says tracking enabled, but service not running
        // With permissions granted, it should attempt to restart the service
        every { preferencesRepository.isTrackingEnabled } returns flowOf(true)
        every { serviceController.isServiceRunning() } returns false
        every { permissionManager.hasAllRequiredPermissions() } returns true
        coEvery { serviceController.startTracking() } returns Result.success(Unit)

        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify service restart was attempted
        coVerify { serviceController.startTracking() }
        verify { analytics.logServiceStateChanged("restored_after_desync") }
    }

    @Test
    fun `state reconciliation corrects persisted state when permissions not granted`() = runTest {
        // Story 1.4: DataStore says tracking enabled, but service not running
        // Without permissions, it should correct the persisted state
        every { preferencesRepository.isTrackingEnabled } returns flowOf(true)
        every { serviceController.isServiceRunning() } returns false
        every { permissionManager.hasAllRequiredPermissions() } returns false

        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify persisted state was corrected
        coVerify { preferencesRepository.setTrackingEnabled(false) }
    }

    @Test
    fun `permission state updated from PermissionManager`() = runTest {
        val permissionFlow = flowOf(
            PermissionState.Checking,
            PermissionState.AllGranted
        )
        every { permissionManager.observePermissionState() } returns permissionFlow

        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.permissionState.test {
            val state = awaitItem()
            assertIs<PermissionState>(state)
        }
    }

    private fun createViewModel() = LocationTrackingViewModel(
        preferencesRepository = preferencesRepository,
        serviceController = serviceController,
        permissionManager = permissionManager,
        locationRepository = locationRepository,
        analytics = analytics
    )
}

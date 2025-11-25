package three.two.bit.phonemanager.service

import android.app.ActivityManager
import android.content.Context
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import three.two.bit.phonemanager.data.preferences.PreferencesRepository
import three.two.bit.phonemanager.data.repository.LocationRepository
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for LocationServiceController
 *
 * Tests service control operations
 * Verifies:
 * - Service state management
 * - Service running check (Story 1.4)
 *
 * Note: Full integration tests for service start/stop require
 * Android instrumented tests due to Intent creation limitations
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LocationServiceControllerTest {

    private lateinit var context: Context
    private lateinit var locationRepository: LocationRepository
    private lateinit var preferencesRepository: PreferencesRepository

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        locationRepository = mockk(relaxed = true)
        preferencesRepository = mockk(relaxed = true)

        // Setup default mocks
        every { preferencesRepository.trackingInterval } returns flowOf(5)
        every { context.packageName } returns "three.two.bit.phonemanager"
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // Story 1.4: Tests for isServiceRunning()

    @Test
    fun `isServiceRunning returns false when service list is empty`() = runTest {
        // Given
        val activityManager = mockk<ActivityManager>()
        every { context.getSystemService(Context.ACTIVITY_SERVICE) } returns activityManager

        @Suppress("DEPRECATION")
        every { activityManager.getRunningServices(any()) } returns emptyList()

        val serviceController = LocationServiceControllerImpl(context, locationRepository, preferencesRepository)

        // When
        val result = serviceController.isServiceRunning()

        // Then
        assertFalse(result)
    }

    @Test
    fun `isServiceRunning returns false when ActivityManager is null`() = runTest {
        // Given
        every { context.getSystemService(Context.ACTIVITY_SERVICE) } returns null

        val serviceController = LocationServiceControllerImpl(context, locationRepository, preferencesRepository)

        // When
        val result = serviceController.isServiceRunning()

        // Then - falls back to in-memory state which is false initially
        assertFalse(result)
    }

    @Test
    fun `isServiceRunning handles exception gracefully`() = runTest {
        // Given
        val activityManager = mockk<ActivityManager>()
        every { context.getSystemService(Context.ACTIVITY_SERVICE) } returns activityManager

        @Suppress("DEPRECATION")
        every { activityManager.getRunningServices(any()) } throws SecurityException("Not allowed")

        val serviceController = LocationServiceControllerImpl(context, locationRepository, preferencesRepository)

        // When
        val result = serviceController.isServiceRunning()

        // Then - falls back to in-memory state which is false initially
        assertFalse(result)
    }

    @Test
    fun `observeServiceState returns non-null flow`() = runTest {
        // Given
        val serviceController = LocationServiceControllerImpl(context, locationRepository, preferencesRepository)

        // When
        val flow = serviceController.observeServiceState()

        // Then
        assertNotNull(flow)
    }

    @Test
    fun `observeEnhancedServiceState returns non-null flow`() = runTest {
        // Given - setup locationRepository mock for observeServiceHealth
        every { locationRepository.observeServiceHealth() } returns flowOf(mockk(relaxed = true))
        every { locationRepository.observeLocationCount() } returns flowOf(0)

        val serviceController = LocationServiceControllerImpl(context, locationRepository, preferencesRepository)

        // When
        val flow = serviceController.observeEnhancedServiceState()

        // Then
        assertNotNull(flow)
    }

    @Test
    fun `ServiceState data class has expected fields`() {
        // Given
        val state = ServiceState(
            isRunning = true,
            lastUpdate = java.time.Instant.now(),
            locationCount = 5,
        )

        // Then
        assertTrue(state.isRunning)
        assertNotNull(state.lastUpdate)
        assertTrue(state.locationCount == 5)
    }

    @Test
    fun `initial ServiceState is not running`() {
        // Given
        val serviceController = LocationServiceControllerImpl(context, locationRepository, preferencesRepository)

        // When - observeServiceState returns StateFlow, get value directly
        val flow = serviceController.observeServiceState()

        // Then - StateFlow.value is always available without suspension
        // Cast to StateFlow to access value property directly
        val stateFlow = flow as kotlinx.coroutines.flow.StateFlow<ServiceState>
        assertFalse(stateFlow.value.isRunning)
    }

    @Test
    fun `LocationServiceController interface defines required methods`() {
        // Given
        val controller: LocationServiceController = mockk(relaxed = true)

        // Then - verify interface contract
        coEvery { controller.startTracking() } returns Result.success(Unit)
        coEvery { controller.stopTracking() } returns Result.success(Unit)
        every { controller.observeServiceState() } returns flowOf(mockk())
        every { controller.observeEnhancedServiceState() } returns flowOf(mockk())
        every { controller.isServiceRunning() } returns false

        // These should compile without error, verifying the interface
        assertNotNull(controller)
    }
}

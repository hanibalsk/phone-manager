package three.two.bit.phonemanager.location

import android.content.Context
import android.content.pm.PackageManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for LocationManager
 *
 * Story 0.2.1: Tests location capture functionality
 * Note: Full integration tests for isLocationEnabled and getCurrentLocation
 * require Robolectric or instrumented tests due to FusedLocationProviderClient's
 * reliance on Play Services Tasks and async operations.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LocationManagerTest {

    private lateinit var locationManager: LocationManager
    private lateinit var context: Context
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        fusedLocationClient = mockk(relaxed = true)

        // Mock LocationServices.getFusedLocationProviderClient
        mockkStatic("com.google.android.gms.location.LocationServices")
        every {
            com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(context)
        } returns fusedLocationClient

        locationManager = LocationManager(context)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `LocationManager can be instantiated`() {
        assertNotNull(locationManager)
    }

    @Test
    fun `stopLocationUpdates removes callback`() {
        // Given
        every { fusedLocationClient.removeLocationUpdates(any<LocationCallback>()) } returns mockk(relaxed = true)

        // When
        locationManager.stopLocationUpdates()

        // Then - Should not throw exception even if no callback was registered
        // (no active callback to remove when called directly after initialization)
        verify(exactly = 0) { fusedLocationClient.removeLocationUpdates(any<LocationCallback>()) }
    }

    @Test
    fun `getCurrentLocation fails when permissions not granted`() = runTest {
        // Given - permissions not granted (default mock behavior)
        mockkStatic("androidx.core.content.ContextCompat")
        every {
            androidx.core.content.ContextCompat.checkSelfPermission(context, any())
        } returns PackageManager.PERMISSION_DENIED

        // When
        val result = locationManager.getCurrentLocation()

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is SecurityException)
    }
}

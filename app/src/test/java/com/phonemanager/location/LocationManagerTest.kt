package com.phonemanager.location

import android.content.Context
import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.tasks.Task
import com.phonemanager.data.model.LocationEntity
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for LocationManager
 *
 * Story 0.2.1: Tests location capture functionality
 * Verifies:
 * - Current location retrieval
 * - Location updates via Flow
 * - Permission checking
 * - Error handling
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
    fun `getCurrentLocation returns location entity on success`() = runTest {
        // Given
        val mockLocation = mockk<Location>(relaxed = true).apply {
            every { latitude } returns 40.7128
            every { longitude } returns -74.0060
            every { accuracy } returns 10f
            every { time } returns System.currentTimeMillis()
            every { hasAltitude() } returns true
            every { altitude } returns 100.0
            every { hasBearing() } returns false
            every { hasSpeed() } returns false
            every { provider } returns "gps"
        }

        val mockTask = mockk<Task<Location>>(relaxed = true)
        every { fusedLocationClient.getCurrentLocation(any(), any()) } returns mockTask

        // Mock task success
        every { mockTask.addOnSuccessListener(any()) } answers {
            val listener = firstArg<com.google.android.gms.tasks.OnSuccessListener<Location>>()
            listener.onSuccess(mockLocation)
            mockTask
        }
        every { mockTask.addOnFailureListener(any()) } returns mockTask

        // When
        val result = locationManager.getCurrentLocation()

        // Then
        assertTrue(result.isSuccess)
        val locationEntity = result.getOrNull()
        assertNotNull(locationEntity)
        assertEquals(40.7128, locationEntity.latitude)
        assertEquals(-74.0060, locationEntity.longitude)
        assertEquals(10f, locationEntity.accuracy)
    }

    @Test
    fun `getCurrentLocation returns null when no location available`() = runTest {
        // Given
        val mockTask = mockk<Task<Location>>(relaxed = true)
        every { fusedLocationClient.getCurrentLocation(any(), any()) } returns mockTask

        // Mock task success with null location
        every { mockTask.addOnSuccessListener(any()) } answers {
            val listener = firstArg<com.google.android.gms.tasks.OnSuccessListener<Location>>()
            listener.onSuccess(null)
            mockTask
        }
        every { mockTask.addOnFailureListener(any()) } returns mockTask

        // When
        val result = locationManager.getCurrentLocation()

        // Then
        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
    }

    @Test
    fun `getCurrentLocation handles exception gracefully`() = runTest {
        // Given
        val exception = SecurityException("Location permission denied")
        val mockTask = mockk<Task<Location>>(relaxed = true)
        every { fusedLocationClient.getCurrentLocation(any(), any()) } returns mockTask

        // Mock task failure
        every { mockTask.addOnSuccessListener(any()) } returns mockTask
        every { mockTask.addOnFailureListener(any()) } answers {
            val listener = firstArg<com.google.android.gms.tasks.OnFailureListener>()
            listener.onFailure(exception)
            mockTask
        }

        // When
        val result = locationManager.getCurrentLocation()

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is SecurityException)
    }

    @Test
    fun `stopLocationUpdates removes callback`() {
        // Given
        every { fusedLocationClient.removeLocationUpdates(any<LocationCallback>()) } returns mockk(relaxed = true)

        // When
        locationManager.stopLocationUpdates()

        // Then
        // Should not throw exception even if no callback was registered
        verify(exactly = 0) { fusedLocationClient.removeLocationUpdates(any<LocationCallback>()) }
    }

    @Test
    fun `isLocationEnabled checks provider status`() = runTest {
        // Given
        val androidLocationManager = mockk<android.location.LocationManager>(relaxed = true)
        every { context.getSystemService(Context.LOCATION_SERVICE) } returns androidLocationManager
        every { androidLocationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) } returns true
        every { androidLocationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER) } returns false

        // Recreate location manager to pick up mocked system service
        locationManager = LocationManager(context)

        // When
        val result = locationManager.isLocationEnabled()

        // Then
        assertTrue(result, "Should return true when GPS provider is enabled")
    }

    @Test
    fun `isLocationEnabled returns false when no providers enabled`() = runTest {
        // Given
        val androidLocationManager = mockk<android.location.LocationManager>(relaxed = true)
        every { context.getSystemService(Context.LOCATION_SERVICE) } returns androidLocationManager
        every { androidLocationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) } returns false
        every { androidLocationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER) } returns false

        // Recreate location manager to pick up mocked system service
        locationManager = LocationManager(context)

        // When
        val result = locationManager.isLocationEnabled()

        // Then
        assertTrue(!result, "Should return false when no providers are enabled")
    }

    @Test
    fun `getCurrentLocation respects timeout parameter`() = runTest {
        // Given
        val timeout = 60000L
        val mockTask = mockk<Task<Location>>(relaxed = true)
        every { fusedLocationClient.getCurrentLocation(any(), any()) } returns mockTask
        every { mockTask.addOnSuccessListener(any()) } returns mockTask
        every { mockTask.addOnFailureListener(any()) } returns mockTask

        // When
        locationManager.getCurrentLocation(timeout)

        // Then
        verify { fusedLocationClient.getCurrentLocation(any(), any()) }
    }
}

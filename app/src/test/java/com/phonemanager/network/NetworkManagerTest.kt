package com.phonemanager.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.phonemanager.data.model.LocationEntity
import com.phonemanager.network.models.LocationUploadResponse
import com.phonemanager.security.SecureStorage
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for NetworkManager
 *
 * Story 0.2.2: Tests network operations
 * Verifies:
 * - Network availability checking
 * - Location upload
 * - Batch upload
 * - Device info gathering
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NetworkManagerTest {

    private lateinit var networkManager: NetworkManager
    private lateinit var context: Context
    private lateinit var locationApiService: LocationApiService
    private lateinit var secureStorage: SecureStorage
    private lateinit var connectivityManager: ConnectivityManager

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        locationApiService = mockk(relaxed = true)
        secureStorage = mockk(relaxed = true)
        connectivityManager = mockk(relaxed = true)

        every { context.getSystemService(Context.CONNECTIVITY_SERVICE) } returns connectivityManager
        every { secureStorage.getDeviceId() } returns "test-device-123"

        networkManager = NetworkManager(context, locationApiService, secureStorage)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `isNetworkAvailable returns true when network is connected`() {
        // Given
        val network = mockk<Network>()
        val networkCapabilities = mockk<NetworkCapabilities>()

        every { connectivityManager.activeNetwork } returns network
        every { connectivityManager.getNetworkCapabilities(network) } returns networkCapabilities
        every { networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns true
        every { networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) } returns true

        // When
        val result = networkManager.isNetworkAvailable()

        // Then
        assertTrue(result)
    }

    @Test
    fun `isNetworkAvailable returns false when no active network`() {
        // Given
        every { connectivityManager.activeNetwork } returns null

        // When
        val result = networkManager.isNetworkAvailable()

        // Then
        assertFalse(result)
    }

    @Test
    fun `isNetworkAvailable returns false when network not validated`() {
        // Given
        val network = mockk<Network>()
        val networkCapabilities = mockk<NetworkCapabilities>()

        every { connectivityManager.activeNetwork } returns network
        every { connectivityManager.getNetworkCapabilities(network) } returns networkCapabilities
        every { networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns true
        every { networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) } returns false

        // When
        val result = networkManager.isNetworkAvailable()

        // Then
        assertFalse(result)
    }

    @Test
    fun `getNetworkType returns WiFi when connected to WiFi`() {
        // Given
        val network = mockk<Network>()
        val networkCapabilities = mockk<NetworkCapabilities>()

        every { connectivityManager.activeNetwork } returns network
        every { connectivityManager.getNetworkCapabilities(network) } returns networkCapabilities
        every { networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) } returns true
        every { networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) } returns false
        every { networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) } returns false

        // When
        val result = networkManager.getNetworkType()

        // Then
        assertEquals("WiFi", result)
    }

    @Test
    fun `getNetworkType returns Cellular when connected to cellular`() {
        // Given
        val network = mockk<Network>()
        val networkCapabilities = mockk<NetworkCapabilities>()

        every { connectivityManager.activeNetwork } returns network
        every { connectivityManager.getNetworkCapabilities(network) } returns networkCapabilities
        every { networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) } returns false
        every { networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) } returns true
        every { networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) } returns false

        // When
        val result = networkManager.getNetworkType()

        // Then
        assertEquals("Cellular", result)
    }

    @Test
    fun `getNetworkType returns None when no network`() {
        // Given
        every { connectivityManager.activeNetwork } returns null

        // When
        val result = networkManager.getNetworkType()

        // Then
        assertEquals("None", result)
    }

    @Test
    fun `getBatteryLevel returns valid percentage`() {
        // Given - battery intent is registered
        // Battery level calculation is done via system broadcast

        // When
        val result = networkManager.getBatteryLevel()

        // Then
        assertTrue(result >= -1, "Battery level should be -1 or positive")
        assertTrue(result <= 100, "Battery level should not exceed 100")
    }

    @Test
    fun `uploadLocation fails when network unavailable`() = runTest {
        // Given
        every { connectivityManager.activeNetwork } returns null

        val locationEntity = LocationEntity(
            id = 1,
            latitude = 40.7128,
            longitude = -74.0060,
            accuracy = 10f,
            timestamp = System.currentTimeMillis(),
        )

        // When
        val result = networkManager.uploadLocation(locationEntity)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is NetworkException)
    }

    @Test
    fun `uploadLocation succeeds when network available`() = runTest {
        // Given
        val network = mockk<Network>()
        val networkCapabilities = mockk<NetworkCapabilities>()

        every { connectivityManager.activeNetwork } returns network
        every { connectivityManager.getNetworkCapabilities(network) } returns networkCapabilities
        every { networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns true
        every { networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) } returns true
        every { networkCapabilities.hasTransport(any()) } returns false

        val locationEntity = LocationEntity(
            id = 1,
            latitude = 40.7128,
            longitude = -74.0060,
            accuracy = 10f,
            timestamp = System.currentTimeMillis(),
        )

        val response = LocationUploadResponse(
            success = true,
            message = "Location uploaded",
            processedCount = 1,
        )

        coEvery { locationApiService.uploadLocation(any()) } returns Result.success(response)

        // When
        val result = networkManager.uploadLocation(locationEntity)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(true, result.getOrNull()?.success)
        coVerify { locationApiService.uploadLocation(any()) }
    }

    @Test
    fun `uploadLocationBatch fails with empty list`() = runTest {
        // Given
        val network = mockk<Network>()
        val networkCapabilities = mockk<NetworkCapabilities>()

        every { connectivityManager.activeNetwork } returns network
        every { connectivityManager.getNetworkCapabilities(network) } returns networkCapabilities
        every { networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns true
        every { networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) } returns true

        // When
        val result = networkManager.uploadLocationBatch(emptyList())

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `uploadLocationBatch succeeds with valid locations`() = runTest {
        // Given
        val network = mockk<Network>()
        val networkCapabilities = mockk<NetworkCapabilities>()

        every { connectivityManager.activeNetwork } returns network
        every { connectivityManager.getNetworkCapabilities(network) } returns networkCapabilities
        every { networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns true
        every { networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) } returns true
        every { networkCapabilities.hasTransport(any()) } returns false

        val locations = listOf(
            LocationEntity(
                id = 1,
                latitude = 40.7128,
                longitude = -74.0060,
                accuracy = 10f,
                timestamp = System.currentTimeMillis(),
            ),
            LocationEntity(
                id = 2,
                latitude = 34.0522,
                longitude = -118.2437,
                accuracy = 15f,
                timestamp = System.currentTimeMillis(),
            ),
        )

        val response = LocationUploadResponse(
            success = true,
            message = "Batch uploaded",
            processedCount = 2,
        )

        coEvery { locationApiService.uploadLocations(any()) } returns Result.success(response)

        // When
        val result = networkManager.uploadLocationBatch(locations)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull()?.processedCount)
        coVerify { locationApiService.uploadLocations(any()) }
    }

    @Test
    fun `testConnection returns true when network available`() = runTest {
        // Given
        val network = mockk<Network>()
        val networkCapabilities = mockk<NetworkCapabilities>()

        every { connectivityManager.activeNetwork } returns network
        every { connectivityManager.getNetworkCapabilities(network) } returns networkCapabilities
        every { networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns true
        every { networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) } returns true
        every { networkCapabilities.hasTransport(any()) } returns false
        every { networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) } returns true

        // When
        val result = networkManager.testConnection()

        // Then
        assertTrue(result)
    }

    @Test
    fun `testConnection returns false when network unavailable`() = runTest {
        // Given
        every { connectivityManager.activeNetwork } returns null

        // When
        val result = networkManager.testConnection()

        // Then
        assertFalse(result)
    }
}

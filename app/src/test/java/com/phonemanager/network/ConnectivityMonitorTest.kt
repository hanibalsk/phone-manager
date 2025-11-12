package com.phonemanager.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import app.cash.turbine.test
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for ConnectivityMonitor
 *
 * Story 0.2.3: Tests connectivity monitoring
 * Verifies:
 * - Network state observation
 * - Network callbacks
 * - Connectivity changes
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ConnectivityMonitorTest {

    private lateinit var connectivityMonitor: ConnectivityMonitor
    private lateinit var context: Context
    private lateinit var connectivityManager: ConnectivityManager

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        connectivityManager = mockk(relaxed = true)

        every { context.getSystemService(Context.CONNECTIVITY_SERVICE) } returns connectivityManager
        every { connectivityManager.registerNetworkCallback(any<NetworkRequest>(), any()) } just Runs
        every { connectivityManager.unregisterNetworkCallback(any<ConnectivityManager.NetworkCallback>()) } just Runs

        connectivityMonitor = ConnectivityMonitor(context)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `isNetworkAvailable returns true when network is available`() {
        // Given
        val network = mockk<Network>()
        val networkCapabilities = mockk<NetworkCapabilities>()

        every { connectivityManager.activeNetwork } returns network
        every { connectivityManager.getNetworkCapabilities(network) } returns networkCapabilities
        every { networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns true
        every { networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) } returns true

        // When
        val result = connectivityMonitor.isNetworkAvailable()

        // Then
        assertTrue(result)
    }

    @Test
    fun `isNetworkAvailable returns false when no active network`() {
        // Given
        every { connectivityManager.activeNetwork } returns null

        // When
        val result = connectivityMonitor.isNetworkAvailable()

        // Then
        assertFalse(result)
    }

    @Test
    fun `isNetworkAvailable returns false when capabilities are null`() {
        // Given
        val network = mockk<Network>()
        every { connectivityManager.activeNetwork } returns network
        every { connectivityManager.getNetworkCapabilities(network) } returns null

        // When
        val result = connectivityMonitor.isNetworkAvailable()

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
        val result = connectivityMonitor.isNetworkAvailable()

        // Then
        assertFalse(result)
    }

    @Test
    fun `observeConnectivity registers network callback`() = runTest {
        // Given
        val network = mockk<Network>()
        val networkCapabilities = mockk<NetworkCapabilities>()

        every { connectivityManager.activeNetwork } returns network
        every { connectivityManager.getNetworkCapabilities(network) } returns networkCapabilities
        every { networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns true
        every { networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) } returns true

        // When
        connectivityMonitor.observeConnectivity().test {
            // Then
            val initialState = awaitItem()
            assertTrue(initialState, "Initial state should match current network availability")

            verify { connectivityManager.registerNetworkCallback(any<NetworkRequest>(), any()) }

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observeConnectivity emits false when no network initially`() = runTest {
        // Given
        every { connectivityManager.activeNetwork } returns null

        // When
        connectivityMonitor.observeConnectivity().test {
            // Then
            val initialState = awaitItem()
            assertFalse(initialState, "Initial state should be false when no network")

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observeConnectivity unregisters callback on close`() = runTest {
        // Given
        every { connectivityManager.activeNetwork } returns null

        // When
        connectivityMonitor.observeConnectivity().test {
            awaitItem()
            cancelAndConsumeRemainingEvents()
        }

        // Then
        verify { connectivityManager.unregisterNetworkCallback(any<ConnectivityManager.NetworkCallback>()) }
    }
}

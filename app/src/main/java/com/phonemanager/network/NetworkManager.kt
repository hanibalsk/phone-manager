package com.phonemanager.network

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import com.phonemanager.data.model.LocationEntity
import com.phonemanager.network.models.LocationBatchPayload
import com.phonemanager.network.models.LocationPayload
import com.phonemanager.network.models.LocationUploadResponse
import com.phonemanager.network.models.toPayload
import com.phonemanager.security.SecureStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Story 0.2.2: NetworkManager - Manages network connectivity and location transmission
 *
 * Responsibilities:
 * - Check network connectivity
 * - Upload locations to backend
 * - Get device information (battery, network type)
 * - Handle transmission errors
 */
@Singleton
class NetworkManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val locationApiService: LocationApiService,
    private val secureStorage: SecureStorage
) {

    private val connectivityManager: ConnectivityManager by lazy {
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    /**
     * Check if device has active network connection
     */
    fun isNetworkAvailable(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    /**
     * Get current network type as string
     */
    fun getNetworkType(): String {
        val network = connectivityManager.activeNetwork ?: return "None"
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return "Unknown"

        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WiFi"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            else -> "Unknown"
        }
    }

    /**
     * Get current battery level percentage
     */
    fun getBatteryLevel(): Int {
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1

        return if (level >= 0 && scale > 0) {
            (level * 100 / scale)
        } else {
            -1
        }
    }

    /**
     * Upload single location to backend
     */
    suspend fun uploadLocation(location: LocationEntity): Result<LocationUploadResponse> {
        if (!isNetworkAvailable()) {
            return Result.failure(NetworkException("No network connection available"))
        }

        val deviceId = secureStorage.getDeviceId()
        val payload = location.toPayload(deviceId).copy(
            batteryLevel = getBatteryLevel(),
            networkType = getNetworkType()
        )

        return locationApiService.uploadLocation(payload)
    }

    /**
     * Upload batch of locations to backend
     */
    suspend fun uploadLocationBatch(locations: List<LocationEntity>): Result<LocationUploadResponse> {
        if (!isNetworkAvailable()) {
            return Result.failure(NetworkException("No network connection available"))
        }

        if (locations.isEmpty()) {
            return Result.failure(IllegalArgumentException("Cannot upload empty location batch"))
        }

        val deviceId = secureStorage.getDeviceId()
        val batteryLevel = getBatteryLevel()
        val networkType = getNetworkType()

        val payloads = locations.map { location ->
            location.toPayload(deviceId).copy(
                batteryLevel = batteryLevel,
                networkType = networkType
            )
        }

        val batch = LocationBatchPayload(
            deviceId = deviceId,
            locations = payloads
        )

        Timber.d("Uploading batch of ${locations.size} locations")
        return locationApiService.uploadLocations(batch)
    }

    /**
     * Test network connectivity to API endpoint
     */
    suspend fun testConnection(): Boolean {
        if (!isNetworkAvailable()) {
            Timber.w("Network not available")
            return false
        }

        // In a real implementation, this would ping the API health endpoint
        // For now, just check network availability
        Timber.d("Network connection test: ${getNetworkType()}")
        return true
    }
}

/**
 * Custom exception for network-related errors
 */
class NetworkException(message: String) : Exception(message)

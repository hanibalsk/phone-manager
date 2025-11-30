package three.two.bit.phonemanager.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import three.two.bit.phonemanager.data.model.LocationEntity
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Story 0.2.1: LocationManager - Wrapper for FusedLocationProviderClient
 *
 * Provides a clean interface for location operations:
 * - Single location capture (getCurrentLocation)
 * - Continuous location updates (startLocationUpdates)
 * - Permission checking
 */
@Singleton
class LocationManager @Inject constructor(@ApplicationContext private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    // Track active callback for cleanup - use synchronized access
    @Volatile
    private var activeLocationCallback: LocationCallback? = null
    private val callbackLock = Any()

    /**
     * Story 0.2.1: Get current location (single capture)
     *
     * @return LocationEntity or null if location unavailable
     */
    suspend fun getCurrentLocation(): Result<LocationEntity?> {
        if (!hasLocationPermission()) {
            return Result.failure(SecurityException("Location permission not granted"))
        }

        return try {
            val location = fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                null,
            ).await()

            if (location != null) {
                Timber.d(
                    "Current location obtained: ${location.latitude}, ${location.longitude}, accuracy=${location.accuracy}m",
                )
                Result.success(location.toLocationEntity())
            } else {
                Timber.w("Current location is null")
                Result.success(null)
            }
        } catch (e: SecurityException) {
            Timber.e(e, "Security exception getting current location")
            Result.failure(e)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get current location")
            Result.failure(e)
        }
    }

    /**
     * Story 0.2.2: Start continuous location updates
     *
     * @param intervalMillis Update interval in milliseconds
     * @return Flow<LocationEntity> emitting location updates
     */
    fun startLocationUpdates(intervalMillis: Long): Flow<LocationEntity> = callbackFlow {
        if (!hasLocationPermission()) {
            close(SecurityException("Location permission not granted"))
            return@callbackFlow
        }

        // Stop any existing location updates to prevent memory leak
        synchronized(callbackLock) {
            activeLocationCallback?.let { existingCallback ->
                Timber.w("Stopping existing location updates before starting new ones")
                fusedLocationClient.removeLocationUpdates(existingCallback)
            }
        }

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            intervalMillis,
        )
            .setMinUpdateIntervalMillis(intervalMillis / 2)
            .setMaxUpdateDelayMillis(intervalMillis * 2)
            .build()

        // Create callback local to this flow
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    Timber.d(
                        "Location update: ${location.latitude}, ${location.longitude}, accuracy=${location.accuracy}m",
                    )
                    trySend(location.toLocationEntity())
                }
            }
        }

        // Store reference for external cleanup
        synchronized(callbackLock) {
            activeLocationCallback = callback
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                callback,
                Looper.getMainLooper(),
            ).await()

            Timber.i("Location updates started with interval ${intervalMillis}ms")
        } catch (e: SecurityException) {
            Timber.e(e, "Security exception starting location updates")
            synchronized(callbackLock) {
                activeLocationCallback = null
            }
            close(e)
        } catch (e: Exception) {
            Timber.e(e, "Failed to start location updates")
            synchronized(callbackLock) {
                activeLocationCallback = null
            }
            close(e)
        }

        awaitClose {
            Timber.d("Stopping location updates")
            synchronized(callbackLock) {
                fusedLocationClient.removeLocationUpdates(callback)
                if (activeLocationCallback === callback) {
                    activeLocationCallback = null
                }
            }
        }
    }

    /**
     * Stop location updates
     */
    fun stopLocationUpdates() {
        synchronized(callbackLock) {
            activeLocationCallback?.let {
                fusedLocationClient.removeLocationUpdates(it)
                Timber.d("Location updates stopped")
            }
            activeLocationCallback = null
        }
    }

    /**
     * Check if app has location permission
     */
    private fun hasLocationPermission(): Boolean = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED

    /**
     * Check if location services are enabled on device
     */
    suspend fun isLocationEnabled(): Boolean {
        if (!hasLocationPermission()) {
            Timber.w("Location permission not granted")
            return false
        }
        return try {
            @Suppress("MissingPermission")
            val location = fusedLocationClient.getLastLocation().await()
            // If we can get last location, services are enabled
            true
        } catch (e: Exception) {
            Timber.w(e, "Location services may be disabled")
            false
        }
    }
}

/**
 * Extension function to convert Android Location to LocationEntity
 */
private fun Location.toLocationEntity(): LocationEntity = LocationEntity(
    latitude = latitude,
    longitude = longitude,
    accuracy = accuracy,
    timestamp = time,
    altitude = if (hasAltitude()) altitude else null,
    bearing = if (hasBearing()) bearing else null,
    speed = if (hasSpeed()) speed else null,
    provider = provider,
)

package three.two.bit.phonemanager.location

import android.content.Context
import android.location.Geocoder
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.IOException
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Trip Geocoding Enhancement: GeocodingService
 *
 * Provides reverse geocoding functionality with in-memory caching
 * to convert lat/lng coordinates to human-readable addresses.
 */
@Singleton
class GeocodingService @Inject constructor(private val context: Context) {

    private val geocoder: Geocoder? = if (Geocoder.isPresent()) {
        Geocoder(context, Locale.getDefault())
    } else {
        Timber.w("Geocoder not available on this device")
        null
    }

    // In-memory cache with lat/lng key (rounded to 4 decimal places ~11m precision)
    private val cache = ConcurrentHashMap<String, String>()

    companion object {
        private const val MAX_CACHE_SIZE = 500
        private const val COORDINATE_PRECISION = 10000.0 // 4 decimal places
    }

    /**
     * Get a short location name for the given coordinates.
     * Returns null if geocoding fails or is unavailable.
     *
     * @param latitude The latitude
     * @param longitude The longitude
     * @return A short location name (e.g., "123 Main St" or "Downtown") or null
     */
    suspend fun getLocationName(latitude: Double, longitude: Double): String? = withContext(Dispatchers.IO) {
        if (geocoder == null) return@withContext null

        // Round coordinates for cache key
        val cacheKey = getCacheKey(latitude, longitude)

        // Check cache first
        cache[cacheKey]?.let { return@withContext it }

        try {
            val address = getAddressFromCoordinates(latitude, longitude)
            address?.let { addr ->
                val locationName = formatLocationName(addr)
                // Cache the result
                if (cache.size < MAX_CACHE_SIZE) {
                    cache[cacheKey] = locationName
                }
                return@withContext locationName
            }
        } catch (e: IOException) {
            Timber.w(e, "Geocoding failed for ($latitude, $longitude)")
        } catch (e: Exception) {
            Timber.e(e, "Unexpected error during geocoding for ($latitude, $longitude)")
        }

        null
    }

    /**
     * Get a trip title showing start and end locations.
     * Returns null if geocoding fails.
     *
     * @param startLat Start latitude
     * @param startLng Start longitude
     * @param endLat End latitude (optional)
     * @param endLng End longitude (optional)
     * @return A trip title (e.g., "123 Main St → 456 Oak Ave") or null
     */
    suspend fun getTripTitle(startLat: Double, startLng: Double, endLat: Double?, endLng: Double?): String? {
        val startName = getLocationName(startLat, startLng) ?: return null

        return if (endLat != null && endLng != null) {
            val endName = getLocationName(endLat, endLng) ?: return startName
            "$startName → $endName"
        } else {
            startName
        }
    }

    @Suppress("DEPRECATION")
    private suspend fun getAddressFromCoordinates(latitude: Double, longitude: Double): android.location.Address? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Use the new async API for Android 13+
            suspendCancellableCoroutine { continuation ->
                geocoder?.getFromLocation(
                    latitude,
                    longitude,
                    1,
                    object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<android.location.Address>) {
                            continuation.resume(addresses.firstOrNull())
                        }

                        override fun onError(errorMessage: String?) {
                            Timber.w("Geocode error: $errorMessage")
                            continuation.resume(null)
                        }
                    },
                ) ?: continuation.resume(null)
            }
        } else {
            // Use the deprecated sync API for older Android versions
            geocoder?.getFromLocation(latitude, longitude, 1)?.firstOrNull()
        }

    private fun formatLocationName(address: android.location.Address): String {
        // Priority order for location name:
        // 1. Street address (e.g., "123 Main St")
        // 2. Feature name (e.g., "Central Park")
        // 3. Thoroughfare (street name without number)
        // 4. Sub-locality (neighborhood)
        // 5. Locality (city)
        // 6. Sub-admin area (county)

        // Try to get a short street address
        val streetNumber = address.subThoroughfare
        val streetName = address.thoroughfare

        if (streetNumber != null && streetName != null) {
            return "$streetNumber $streetName"
        }

        if (streetName != null) {
            return streetName
        }

        // Try feature name (for landmarks, parks, etc.)
        address.featureName?.let { feature ->
            // Avoid using coordinates as feature name
            if (!feature.contains(",") && !feature.matches(Regex("-?\\d+\\.\\d+"))) {
                return feature
            }
        }

        // Fall back to neighborhood or locality
        address.subLocality?.let { return it }
        address.locality?.let { return it }
        address.subAdminArea?.let { return it }
        address.adminArea?.let { return it }

        return "Unknown location"
    }

    private fun getCacheKey(latitude: Double, longitude: Double): String {
        val roundedLat = (latitude * COORDINATE_PRECISION).toLong() / COORDINATE_PRECISION
        val roundedLng = (longitude * COORDINATE_PRECISION).toLong() / COORDINATE_PRECISION
        return "$roundedLat,$roundedLng"
    }

    /**
     * Clear the geocoding cache.
     */
    fun clearCache() {
        cache.clear()
        Timber.d("Geocoding cache cleared")
    }
}

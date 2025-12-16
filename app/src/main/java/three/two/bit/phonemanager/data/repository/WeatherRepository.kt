package three.two.bit.phonemanager.data.repository

import three.two.bit.phonemanager.data.cache.WeatherCache
import three.two.bit.phonemanager.domain.model.Weather
import three.two.bit.phonemanager.network.NetworkManager
import three.two.bit.phonemanager.network.WeatherApiService
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Story E7.1: WeatherRepository - Manages weather data with caching
 *
 * AC E7.1.3: Implements cache-first strategy with graceful degradation
 * AC E7.1.6: Error handling with fallback to cache
 */
interface WeatherRepository {
    /**
     * Get weather data for a location
     *
     * AC E7.1.3: Cache-first strategy:
     * 1. Check cache first (30-minute TTL)
     * 2. Fetch from API if cache expired
     * 3. Return cached data if offline
     * 4. Not block on failures (graceful degradation)
     *
     * AC E7.1.6: Return cached data if available on error, null otherwise
     *
     * @param latitude Location latitude
     * @param longitude Location longitude
     * @return Weather data or null if unavailable
     */
    suspend fun getWeather(latitude: Double, longitude: Double): Weather?
}

@Singleton
class WeatherRepositoryImpl @Inject constructor(
    private val weatherApiService: WeatherApiService,
    private val weatherCache: WeatherCache,
    private val networkManager: NetworkManager,
) : WeatherRepository {

    /**
     * AC E7.1.3: Cache-first strategy with graceful degradation
     * AC E7.1.6: Graceful error handling, return cache on failure
     */
    override suspend fun getWeather(latitude: Double, longitude: Double): Weather? {
        // AC E7.1.3: Check cache first (only valid, non-expired data)
        val validCachedWeather = weatherCache.getValidWeather()

        if (validCachedWeather != null) {
            Timber.d("Returning valid cached weather: temp=${validCachedWeather.current.temperature}°C")
            return validCachedWeather
        }

        // AC E7.1.3: Cache expired or missing, try to fetch fresh data
        if (!networkManager.isNetworkAvailable()) {
            Timber.w("No network available, returning stale cache if available")
            // AC E7.1.6: Return any cached data if offline, even if expired
            val staleCachedWeather = weatherCache.getWeather()
            return staleCachedWeather
        }

        // Fetch from API
        return try {
            weatherApiService.getWeather(latitude, longitude).fold(
                onSuccess = { weather ->
                    // Save to cache for future use
                    weatherCache.saveWeather(weather)
                    Timber.i("Fresh weather fetched and cached: temp=${weather.current.temperature}°C")
                    weather
                },
                onFailure = { error ->
                    // AC E7.1.6: On failure, return any cached data if available (even expired)
                    Timber.e(error, "Failed to fetch weather, falling back to cache")
                    weatherCache.getWeather() // Returns even expired cache
                },
            )
        } catch (e: kotlinx.coroutines.CancellationException) {
            // Coroutine cancelled - rethrow to propagate cancellation properly
            Timber.d("Weather repository operation cancelled")
            throw e
        }
    }
}

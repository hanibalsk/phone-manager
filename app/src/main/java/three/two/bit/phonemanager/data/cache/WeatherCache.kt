package three.two.bit.phonemanager.data.cache

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import three.two.bit.phonemanager.domain.model.Weather
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Story E7.1: WeatherCache - Persistent storage for weather data
 *
 * AC E7.1.4: Cache weather data using DataStore with TTL (30 minutes)
 */

private val Context.weatherDataStore: DataStore<Preferences> by preferencesDataStore(name = "weather_cache")

interface WeatherCache {
    /**
     * Save weather data to cache
     * AC E7.1.4: Store serialized Weather object with timestamp
     */
    suspend fun saveWeather(weather: Weather)

    /**
     * Get cached weather regardless of expiration
     * AC E7.1.4: Returns cached data even if expired (for offline fallback)
     *
     * @return Weather if cache exists, null if no cached data
     */
    suspend fun getWeather(): Weather?

    /**
     * Get cached weather only if not expired
     * AC E7.1.4: Check TTL (30 minutes) before returning cached data
     *
     * @return Weather if cache is valid, null if expired or missing
     */
    suspend fun getValidWeather(): Weather?

    /**
     * Clear weather cache
     */
    suspend fun clearWeather()

    /**
     * Check if cached weather is valid (not expired)
     * AC E7.1.4: 30-minute cache TTL
     */
    suspend fun isCacheValid(): Boolean
}

@OptIn(ExperimentalTime::class)
@Singleton
class WeatherCacheImpl @Inject constructor(@ApplicationContext private val context: Context) : WeatherCache {

    companion object {
        /**
         * AC E7.1.4: 30-minute cache TTL
         */
        private val CACHE_TTL = 30.minutes

        private val WEATHER_DATA_KEY = stringPreferencesKey("weather_data")
        private val WEATHER_TIMESTAMP_KEY = longPreferencesKey("weather_timestamp")
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * AC E7.1.4: Store serialized Weather object with timestamp
     */
    override suspend fun saveWeather(weather: Weather) {
        try {
            val weatherJson = json.encodeToString(weather)
            val timestamp = Clock.System.now().toEpochMilliseconds()

            context.weatherDataStore.edit { preferences ->
                preferences[WEATHER_DATA_KEY] = weatherJson
                preferences[WEATHER_TIMESTAMP_KEY] = timestamp
            }

            Timber.d("Weather cached: temp=${weather.current.temperature}°C, timestamp=$timestamp")
        } catch (e: Exception) {
            Timber.e(e, "Failed to save weather to cache")
        }
    }

    /**
     * Get cached weather regardless of expiration (for offline fallback)
     */
    override suspend fun getWeather(): Weather? = try {
        val preferences = context.weatherDataStore.data.first()

        val weatherJson = preferences[WEATHER_DATA_KEY]

        if (weatherJson == null) {
            Timber.d("No cached weather found")
            null
        } else {
            val weather = json.decodeFromString<Weather>(weatherJson)
            Timber.d("Cached weather retrieved: temp=${weather.current.temperature}°C")
            weather
        }
    } catch (e: kotlinx.coroutines.CancellationException) {
        // Coroutine cancelled (e.g., user navigated away) - rethrow to propagate cancellation properly
        Timber.d("Weather cache read cancelled")
        throw e
    } catch (e: Exception) {
        Timber.e(e, "Failed to read weather from cache")
        null
    }

    /**
     * AC E7.1.4: Check TTL before returning cached data
     */
    override suspend fun getValidWeather(): Weather? = try {
        val preferences = context.weatherDataStore.data.first()

        val weatherJson = preferences[WEATHER_DATA_KEY]
        val timestamp = preferences[WEATHER_TIMESTAMP_KEY]

        if (weatherJson == null || timestamp == null) {
            Timber.d("No cached weather found")
            null
        } else {
            val cachedTime = Instant.fromEpochMilliseconds(timestamp)
            val now = Clock.System.now()
            val age = now - cachedTime

            if (age > CACHE_TTL) {
                Timber.d("Cached weather expired: age=$age (TTL=$CACHE_TTL)")
                null
            } else {
                val weather = json.decodeFromString<Weather>(weatherJson)
                Timber.d("Valid cached weather retrieved: temp=${weather.current.temperature}°C, age=$age")
                weather
            }
        }
    } catch (e: kotlinx.coroutines.CancellationException) {
        // Coroutine cancelled (e.g., user navigated away) - rethrow to propagate cancellation properly
        Timber.d("Weather valid cache read cancelled")
        throw e
    } catch (e: Exception) {
        Timber.e(e, "Failed to read weather from cache")
        null
    }

    override suspend fun clearWeather() {
        try {
            context.weatherDataStore.edit { preferences ->
                preferences.remove(WEATHER_DATA_KEY)
                preferences.remove(WEATHER_TIMESTAMP_KEY)
            }
            Timber.d("Weather cache cleared")
        } catch (e: Exception) {
            Timber.e(e, "Failed to clear weather cache")
        }
    }

    /**
     * AC E7.1.4: Check if cache is valid (not expired)
     */
    override suspend fun isCacheValid(): Boolean = try {
        val preferences = context.weatherDataStore.data.first()
        val timestamp = preferences[WEATHER_TIMESTAMP_KEY]

        if (timestamp == null) {
            false
        } else {
            val cachedTime = Instant.fromEpochMilliseconds(timestamp)
            val now = Clock.System.now()
            val age = now - cachedTime
            age <= CACHE_TTL
        }
    } catch (e: kotlinx.coroutines.CancellationException) {
        // Coroutine cancelled (e.g., user navigated away) - rethrow to propagate cancellation properly
        Timber.d("Cache validity check cancelled")
        throw e
    } catch (e: Exception) {
        Timber.e(e, "Failed to check cache validity")
        false
    }
}

package three.two.bit.phonemanager.data.repository

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import three.two.bit.phonemanager.data.cache.WeatherCache
import three.two.bit.phonemanager.domain.model.CurrentConditions
import three.two.bit.phonemanager.domain.model.DailyForecast
import three.two.bit.phonemanager.domain.model.LocationCoordinates
import three.two.bit.phonemanager.domain.model.Weather
import three.two.bit.phonemanager.domain.model.WeatherCode
import three.two.bit.phonemanager.network.NetworkManager
import three.two.bit.phonemanager.network.WeatherApiService

/**
 * Story E7.1: WeatherRepository Unit Tests
 *
 * AC E7.1.3: Tests cache-first strategy
 * AC E7.1.4: Tests TTL expiration
 * AC E7.1.6: Tests error handling and graceful degradation
 */
class WeatherRepositoryTest {

    private lateinit var weatherApiService: WeatherApiService
    private lateinit var weatherCache: WeatherCache
    private lateinit var networkManager: NetworkManager
    private lateinit var repository: WeatherRepository

    private val testLatitude = 37.7749
    private val testLongitude = -122.4194
    private val testWeather = Weather(
        current = CurrentConditions(
            temperature = 24.5,
            feelsLike = 26.1,
            humidity = 45,
            windSpeed = 12.3,
            weatherCode = WeatherCode.MAINLY_CLEAR,
        ),
        daily = listOf(
            DailyForecast(
                date = LocalDate.parse("2025-11-28"),
                tempMin = 18.0,
                tempMax = 26.0,
                weatherCode = WeatherCode.MAINLY_CLEAR,
            ),
        ),
        lastUpdated = Clock.System.now(),
        locationCoordinates = LocationCoordinates(testLatitude, testLongitude),
    )

    @Before
    fun setup() {
        weatherApiService = mockk()
        weatherCache = mockk(relaxed = true)
        networkManager = mockk()
        repository = WeatherRepositoryImpl(weatherApiService, weatherCache, networkManager)
    }

    /**
     * AC E7.1.3: Test cache-first strategy - return valid cached data
     */
    @Test
    fun `getWeather returns valid cached data without API call`() = runBlocking {
        // Given: Valid cache available
        coEvery { weatherCache.getValidWeather() } returns testWeather

        // When: Get weather
        val result = repository.getWeather(testLatitude, testLongitude)

        // Then: Returns cached weather without API call
        assertNotNull(result)
        assertEquals(testWeather, result)
        coVerify(exactly = 0) { weatherApiService.getWeather(any(), any()) }
        coVerify { weatherCache.getValidWeather() }
    }

    /**
     * AC E7.1.4: Test TTL expiration - fetch from API when cache expired
     */
    @Test
    fun `getWeather fetches from API when cache expired and network available`() = runBlocking {
        // Given: Cache expired, network available
        coEvery { weatherCache.getValidWeather() } returns null
        every { networkManager.isNetworkAvailable() } returns true
        coEvery { weatherApiService.getWeather(testLatitude, testLongitude) } returns Result.success(testWeather)

        // When: Get weather
        val result = repository.getWeather(testLatitude, testLongitude)

        // Then: Fetches from API and caches result
        assertNotNull(result)
        assertEquals(testWeather, result)
        coVerify { weatherApiService.getWeather(testLatitude, testLongitude) }
        coVerify { weatherCache.saveWeather(testWeather) }
    }

    /**
     * AC E7.1.6: Test error handling - return cached data when API fails
     */
    @Test
    fun `getWeather returns cached data when API fails`() = runBlocking {
        // Given: Valid cache expired, API call fails
        coEvery { weatherCache.getValidWeather() } returns null
        coEvery { weatherCache.getWeather() } returns testWeather
        every { networkManager.isNetworkAvailable() } returns true
        coEvery { weatherApiService.getWeather(testLatitude, testLongitude) } returns
            Result.failure(Exception("Network error"))

        // When: Get weather
        val result = repository.getWeather(testLatitude, testLongitude)

        // Then: Returns cached data despite being expired
        assertNotNull(result)
        assertEquals(testWeather, result)
    }

    /**
     * AC E7.1.6: Test error handling - return null when no cache and API fails
     */
    @Test
    fun `getWeather returns null when no cache and API fails`() = runBlocking {
        // Given: No cache, API call fails
        coEvery { weatherCache.getValidWeather() } returns null
        coEvery { weatherCache.getWeather() } returns null
        every { networkManager.isNetworkAvailable() } returns true
        coEvery { weatherApiService.getWeather(testLatitude, testLongitude) } returns
            Result.failure(Exception("Network error"))

        // When: Get weather
        val result = repository.getWeather(testLatitude, testLongitude)

        // Then: Returns null
        assertNull(result)
    }

    /**
     * AC E7.1.3: Test offline behavior - return cached data when offline
     */
    @Test
    fun `getWeather returns cached data when offline even if expired`() = runBlocking {
        // Given: Valid cache expired but any cache available, no network
        coEvery { weatherCache.getValidWeather() } returns null
        coEvery { weatherCache.getWeather() } returns testWeather
        every { networkManager.isNetworkAvailable() } returns false

        // When: Get weather
        val result = repository.getWeather(testLatitude, testLongitude)

        // Then: Returns cached data without API call
        assertNotNull(result)
        assertEquals(testWeather, result)
        coVerify(exactly = 0) { weatherApiService.getWeather(any(), any()) }
    }

    /**
     * AC E7.1.6: Test graceful degradation - return null when offline and no cache
     */
    @Test
    fun `getWeather returns null when offline and no cache`() = runBlocking {
        // Given: No cache, no network
        coEvery { weatherCache.getValidWeather() } returns null
        coEvery { weatherCache.getWeather() } returns null
        every { networkManager.isNetworkAvailable() } returns false

        // When: Get weather
        val result = repository.getWeather(testLatitude, testLongitude)

        // Then: Returns null without crashing
        assertNull(result)
        coVerify(exactly = 0) { weatherApiService.getWeather(any(), any()) }
    }
}

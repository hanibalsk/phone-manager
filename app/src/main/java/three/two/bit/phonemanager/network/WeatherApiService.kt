package three.two.bit.phonemanager.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import three.two.bit.phonemanager.domain.model.Weather
import three.two.bit.phonemanager.network.models.OpenMeteoResponse
import three.two.bit.phonemanager.network.models.toDomain
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Story E7.1: WeatherApiService - HTTP client for Open-Meteo weather API
 *
 * AC E7.1.1: Fetches weather data from Open-Meteo API using Ktor HttpClient
 * No API key required (free tier)
 */
interface WeatherApiService {
    /**
     * Fetch weather data for a location
     *
     * AC E7.1.1: Call Open-Meteo API with lat/lon parameters
     *
     * @param latitude Location latitude
     * @param longitude Location longitude
     * @return Result containing Weather domain model or error
     */
    suspend fun getWeather(latitude: Double, longitude: Double): Result<Weather>
}

@Singleton
class WeatherApiServiceImpl @Inject constructor(private val httpClient: HttpClient) : WeatherApiService {

    companion object {
        private const val BASE_URL = "https://api.open-meteo.com/v1/forecast"
    }

    /**
     * Fetch weather data from Open-Meteo API
     * AC E7.1.1: Use Ktor HttpClient to call Open-Meteo API
     *
     * Request parameters:
     * - latitude, longitude: Location coordinates
     * - current: Current weather fields
     * - daily: Daily forecast fields
     * - timezone: Auto-detect from location
     * - forecast_days: Number of forecast days (5)
     */
    override suspend fun getWeather(latitude: Double, longitude: Double): Result<Weather> = try {
        // Validate coordinates
        require(latitude in -90.0..90.0) { "Latitude must be between -90 and 90, got $latitude" }
        require(longitude in -180.0..180.0) { "Longitude must be between -180 and 180, got $longitude" }

        Timber.d("Fetching weather for lat=$latitude, lon=$longitude")

        val response: OpenMeteoResponse = httpClient.get(BASE_URL) {
            parameter("latitude", latitude)
            parameter("longitude", longitude)
            parameter("current", "temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m,apparent_temperature")
            parameter("daily", "temperature_2m_max,temperature_2m_min,weather_code")
            parameter("timezone", "auto")
            parameter("forecast_days", 5)
        }.body()

        // AC E7.1.1: Map API response to domain model
        val weather = response.toDomain(latitude, longitude)
        Timber.i(
            "Weather fetched successfully: temp=${weather.current.temperature}°C, condition=${weather.current.weatherCode}",
        )
        Result.success(weather)
    } catch (e: Exception) {
        Timber.e(e, "Failed to fetch weather for lat=$latitude, lon=$longitude")
        Result.failure(e)
    }
}

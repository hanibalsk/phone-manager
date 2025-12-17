package three.two.bit.phonemanager.network.models

import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import three.two.bit.phonemanager.domain.model.CurrentConditions
import three.two.bit.phonemanager.domain.model.DailyForecast
import three.two.bit.phonemanager.domain.model.LocationCoordinates
import three.two.bit.phonemanager.domain.model.Weather
import three.two.bit.phonemanager.domain.model.WeatherCode
import kotlin.time.Clock

/**
 * Story E7.1: Open-Meteo API response models
 *
 * Network DTOs matching the Open-Meteo API response structure
 * API Endpoint: https://api.open-meteo.com/v1/forecast
 */

/**
 * Top-level response from Open-Meteo API
 * AC E7.1.1: Main response container
 */
@Serializable
data class OpenMeteoResponse(val current: CurrentWeatherDto, val daily: DailyForecastDto)

/**
 * Current weather conditions from API
 * AC E7.1.1: Current weather fields
 */
@Serializable
data class CurrentWeatherDto(
    @SerialName("temperature_2m")
    val temperature: Double,
    @SerialName("apparent_temperature")
    val feelsLike: Double,
    @SerialName("relative_humidity_2m")
    val humidity: Int,
    @SerialName("wind_speed_10m")
    val windSpeed: Double,
    @SerialName("weather_code")
    val weatherCode: Int,
)

/**
 * Daily forecast data from API
 * AC E7.1.1: Daily forecast fields
 */
@Serializable
data class DailyForecastDto(
    val time: List<String>,
    @SerialName("temperature_2m_max")
    val temperatureMax: List<Double>,
    @SerialName("temperature_2m_min")
    val temperatureMin: List<Double>,
    @SerialName("weather_code")
    val weatherCode: List<Int>,
)

/**
 * Maps OpenMeteoResponse from API to Weather domain model
 * AC E7.1.1: Map API response to domain model
 */
fun OpenMeteoResponse.toDomain(latitude: Double, longitude: Double): Weather {
    val dailyForecasts = daily.time.indices.map { index ->
        DailyForecast(
            date = LocalDate.parse(daily.time[index]),
            tempMin = daily.temperatureMin[index],
            tempMax = daily.temperatureMax[index],
            weatherCode = WeatherCode.fromCode(daily.weatherCode[index]),
        )
    }

    return Weather(
        current = CurrentConditions(
            temperature = current.temperature,
            feelsLike = current.feelsLike,
            humidity = current.humidity,
            windSpeed = current.windSpeed,
            weatherCode = WeatherCode.fromCode(current.weatherCode),
        ),
        daily = dailyForecasts,
        lastUpdated = Clock.System.now(),
        locationCoordinates = LocationCoordinates(latitude, longitude),
    )
}

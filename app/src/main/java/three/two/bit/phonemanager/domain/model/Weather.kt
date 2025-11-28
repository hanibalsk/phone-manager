package three.two.bit.phonemanager.domain.model

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

/**
 * Story E7.1: Weather domain models
 *
 * AC E7.1.2: Domain representation of weather data
 */

/**
 * Represents weather information for a location
 *
 * @property current Current weather conditions
 * @property daily List of daily forecasts (typically 5 days)
 * @property lastUpdated When this data was fetched
 * @property locationCoordinates Location for which this weather applies
 */
@Serializable
data class Weather(
    val current: CurrentConditions,
    val daily: List<DailyForecast>,
    val lastUpdated: Instant,
    val locationCoordinates: LocationCoordinates,
)

/**
 * Current weather conditions
 * AC E7.1.2: Current conditions fields
 *
 * @property temperature Temperature in Celsius
 * @property feelsLike Apparent temperature (feels like) in Celsius
 * @property humidity Relative humidity percentage (0-100)
 * @property windSpeed Wind speed in km/h
 * @property weatherCode WMO weather code indicating conditions
 */
@Serializable
data class CurrentConditions(
    val temperature: Double,
    val feelsLike: Double,
    val humidity: Int,
    val windSpeed: Double,
    val weatherCode: WeatherCode,
)

/**
 * Daily weather forecast
 * AC E7.1.2: Daily forecast fields
 *
 * @property date The date for this forecast
 * @property tempMin Minimum temperature in Celsius
 * @property tempMax Maximum temperature in Celsius
 * @property weatherCode WMO weather code for the day
 */
@Serializable
data class DailyForecast(val date: LocalDate, val tempMin: Double, val tempMax: Double, val weatherCode: WeatherCode)

/**
 * Geographic coordinates for weather location
 *
 * @property latitude Latitude coordinate in degrees
 * @property longitude Longitude coordinate in degrees
 */
@Serializable
data class LocationCoordinates(val latitude: Double, val longitude: Double)

/**
 * WMO (World Meteorological Organization) Weather Codes
 * AC E7.1.2: Weather code enum mapping
 *
 * Maps standardized weather condition codes to descriptive conditions
 */
enum class WeatherCode(val code: Int, val description: String, val emoji: String) {
    CLEAR_SKY(0, "Clear sky", "☀️"),
    MAINLY_CLEAR(1, "Mainly clear", "🌤️"),
    PARTLY_CLOUDY(2, "Partly cloudy", "⛅"),
    OVERCAST(3, "Overcast", "☁️"),
    FOG(45, "Fog", "🌫️"),
    DEPOSITING_RIME_FOG(48, "Depositing rime fog", "🌫️"),
    DRIZZLE_LIGHT(51, "Light drizzle", "🌦️"),
    DRIZZLE_MODERATE(53, "Moderate drizzle", "🌦️"),
    DRIZZLE_DENSE(55, "Dense drizzle", "🌧️"),
    FREEZING_DRIZZLE_LIGHT(56, "Light freezing drizzle", "🌧️"),
    FREEZING_DRIZZLE_DENSE(57, "Dense freezing drizzle", "🌧️"),
    RAIN_SLIGHT(61, "Slight rain", "🌧️"),
    RAIN_MODERATE(63, "Moderate rain", "🌧️"),
    RAIN_HEAVY(65, "Heavy rain", "🌧️"),
    FREEZING_RAIN_LIGHT(66, "Light freezing rain", "🌧️"),
    FREEZING_RAIN_HEAVY(68, "Heavy freezing rain", "🌧️"),
    SNOW_SLIGHT(71, "Slight snow", "🌨️"),
    SNOW_MODERATE(73, "Moderate snow", "🌨️"),
    SNOW_HEAVY(75, "Heavy snow", "🌨️"),
    SNOW_GRAINS(77, "Snow grains", "🌨️"),
    RAIN_SHOWERS_SLIGHT(80, "Slight rain showers", "🌦️"),
    RAIN_SHOWERS_MODERATE(81, "Moderate rain showers", "🌦️"),
    RAIN_SHOWERS_VIOLENT(82, "Violent rain showers", "🌧️"),
    SNOW_SHOWERS_SLIGHT(85, "Slight snow showers", "🌨️"),
    SNOW_SHOWERS_HEAVY(86, "Heavy snow showers", "🌨️"),
    THUNDERSTORM(95, "Thunderstorm", "⛈️"),
    THUNDERSTORM_SLIGHT_HAIL(96, "Thunderstorm with slight hail", "⛈️"),
    THUNDERSTORM_HEAVY_HAIL(99, "Thunderstorm with heavy hail", "⛈️"),
    UNKNOWN(-1, "Unknown", "❓"),
    ;

    companion object {
        /**
         * Maps a numeric WMO code to the corresponding WeatherCode enum
         * Returns UNKNOWN if the code is not recognized
         */
        fun fromCode(code: Int): WeatherCode = entries.find { it.code == code } ?: UNKNOWN
    }
}

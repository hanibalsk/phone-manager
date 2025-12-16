package three.two.bit.phonemanager.domain.model

import androidx.annotation.StringRes
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import three.two.bit.phonemanager.R

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
enum class WeatherCode(val code: Int, @StringRes val descriptionResId: Int, val emoji: String) {
    CLEAR_SKY(0, R.string.weather_condition_clear_sky, "☀️"),
    MAINLY_CLEAR(1, R.string.weather_condition_mainly_clear, "🌤️"),
    PARTLY_CLOUDY(2, R.string.weather_condition_partly_cloudy, "⛅"),
    OVERCAST(3, R.string.weather_condition_overcast, "☁️"),
    FOG(45, R.string.weather_condition_fog, "🌫️"),
    DEPOSITING_RIME_FOG(48, R.string.weather_condition_depositing_rime_fog, "🌫️"),
    DRIZZLE_LIGHT(51, R.string.weather_condition_drizzle_light, "🌦️"),
    DRIZZLE_MODERATE(53, R.string.weather_condition_drizzle_moderate, "🌦️"),
    DRIZZLE_DENSE(55, R.string.weather_condition_drizzle_dense, "🌧️"),
    FREEZING_DRIZZLE_LIGHT(56, R.string.weather_condition_freezing_drizzle_light, "🌧️"),
    FREEZING_DRIZZLE_DENSE(57, R.string.weather_condition_freezing_drizzle_dense, "🌧️"),
    RAIN_SLIGHT(61, R.string.weather_condition_rain_slight, "🌧️"),
    RAIN_MODERATE(63, R.string.weather_condition_rain_moderate, "🌧️"),
    RAIN_HEAVY(65, R.string.weather_condition_rain_heavy, "🌧️"),
    FREEZING_RAIN_LIGHT(66, R.string.weather_condition_freezing_rain_light, "🌧️"),
    FREEZING_RAIN_HEAVY(68, R.string.weather_condition_freezing_rain_heavy, "🌧️"),
    SNOW_SLIGHT(71, R.string.weather_condition_snow_slight, "🌨️"),
    SNOW_MODERATE(73, R.string.weather_condition_snow_moderate, "🌨️"),
    SNOW_HEAVY(75, R.string.weather_condition_snow_heavy, "🌨️"),
    SNOW_GRAINS(77, R.string.weather_condition_snow_grains, "🌨️"),
    RAIN_SHOWERS_SLIGHT(80, R.string.weather_condition_rain_showers_slight, "🌦️"),
    RAIN_SHOWERS_MODERATE(81, R.string.weather_condition_rain_showers_moderate, "🌦️"),
    RAIN_SHOWERS_VIOLENT(82, R.string.weather_condition_rain_showers_violent, "🌧️"),
    SNOW_SHOWERS_SLIGHT(85, R.string.weather_condition_snow_showers_slight, "🌨️"),
    SNOW_SHOWERS_HEAVY(86, R.string.weather_condition_snow_showers_heavy, "🌨️"),
    THUNDERSTORM(95, R.string.weather_condition_thunderstorm, "⛈️"),
    THUNDERSTORM_SLIGHT_HAIL(96, R.string.weather_condition_thunderstorm_slight_hail, "⛈️"),
    THUNDERSTORM_HEAVY_HAIL(99, R.string.weather_condition_thunderstorm_heavy_hail, "⛈️"),
    UNKNOWN(-1, R.string.weather_condition_unknown, "❓"),
    ;

    companion object {
        /**
         * Maps a numeric WMO code to the corresponding WeatherCode enum
         * Returns UNKNOWN if the code is not recognized
         */
        fun fromCode(code: Int): WeatherCode = entries.find { it.code == code } ?: UNKNOWN
    }
}

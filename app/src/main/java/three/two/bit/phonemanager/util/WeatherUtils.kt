package three.two.bit.phonemanager.util

import android.content.Context
import three.two.bit.phonemanager.domain.model.Weather
import three.two.bit.phonemanager.domain.model.WeatherCode
import kotlin.math.roundToInt

/**
 * Story E7.2: Weather utility functions for notification display
 *
 * AC E7.2.1: Format weather for notification title
 */

/**
 * Format weather for notification title
 * AC E7.2.1: "{icon} {temp}°C"
 *
 * @return Formatted string like "☀️ 24°C"
 */
fun Weather.toNotificationTitle(): String {
    val temp = current.temperature.roundToInt()
    val emoji = current.weatherCode.emoji
    return "$emoji $temp°C"
}

/**
 * Format weather condition for notification text
 * AC E7.2.2: Weather condition description
 *
 * @param context Context to resolve the localized weather condition string
 * @return Condition description like "Partly Cloudy"
 */
fun Weather.toNotificationText(context: Context): String = context.getString(current.weatherCode.descriptionResId)

/**
 * Maps WeatherCode to emoji for notification display
 * AC E7.2.1: Weather icon emoji
 *
 * This is already provided by WeatherCode.emoji property,
 * but we keep this for backward compatibility
 */
fun weatherCodeToEmoji(code: WeatherCode): String = code.emoji

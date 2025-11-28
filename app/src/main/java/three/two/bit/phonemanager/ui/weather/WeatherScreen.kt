package three.two.bit.phonemanager.ui.weather

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import three.two.bit.phonemanager.domain.model.DailyForecast
import three.two.bit.phonemanager.domain.model.Weather
import kotlin.math.roundToInt

/**
 * Story E7.3: Weather Screen - Display current conditions and 5-day forecast
 *
 * AC E7.3.1-E7.3.7: Full weather UI with loading, error, and pull-to-refresh
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WeatherViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Weather Forecast") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
            )
        },
        modifier = modifier,
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            when (val state = uiState) {
                is WeatherUiState.Loading -> {
                    // AC E7.3.5: Loading state
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Loading weather...")
                    }
                }

                is WeatherUiState.Error -> {
                    // AC E7.3.6: Error state
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.refreshWeather() }) {
                            Text("Retry")
                        }
                    }
                }

                is WeatherUiState.Success -> {
                    // AC E7.3.1, E7.3.2, E7.3.3: Success state with weather data
                    WeatherContent(
                        weather = state.weather,
                        lastUpdatedText = state.lastUpdatedText,
                        isOffline = state.isOffline,
                        onRefresh = { viewModel.refreshWeather() },
                    )
                }
            }
        }
    }
}

/**
 * Weather content with current conditions and forecast
 * AC E7.3.1, E7.3.2, E7.3.3
 */
@Composable
private fun WeatherContent(
    weather: Weather,
    lastUpdatedText: String,
    isOffline: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            // AC E7.3.1: Current conditions card
            CurrentConditionsCard(weather = weather)
        }

        item {
            HorizontalDivider()
        }

        item {
            // AC E7.3.2: 5-day forecast title
            Text(
                text = "5-Day Forecast",
                style = MaterialTheme.typography.titleMedium,
            )
        }

        // AC E7.3.2: 5-day forecast list
        items(weather.daily) { forecast ->
            ForecastListItem(forecast = forecast)
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            // AC E7.3.3: Last updated indicator
            Text(
                text = if (isOffline) {
                    "Offline - showing cached data"
                } else {
                    "Updated $lastUpdatedText"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * AC E7.3.1: Current conditions card
 */
@Composable
private fun CurrentConditionsCard(weather: Weather, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Weather icon
            Text(
                text = weather.current.weatherCode.emoji,
                fontSize = 72.sp,
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Temperature
            Text(
                text = "${weather.current.temperature.roundToInt()}°C",
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
            )

            // Condition
            Text(
                text = weather.current.weatherCode.description,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Feels like
            Text(
                text = "Feels like ${weather.current.feelsLike.roundToInt()}°C",
                style = MaterialTheme.typography.bodyMedium,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Humidity and Wind
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "💧", fontSize = 24.sp)
                    Text(text = "${weather.current.humidity}%", style = MaterialTheme.typography.bodyMedium)
                    Text(text = "Humidity", style = MaterialTheme.typography.bodySmall)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "💨", fontSize = 24.sp)
                    Text(
                        text = "${weather.current.windSpeed.roundToInt()} km/h",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(text = "Wind", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

/**
 * AC E7.3.2: Forecast list item for daily forecast
 */
@Composable
private fun ForecastListItem(forecast: DailyForecast, modifier: Modifier = Modifier) {
    val dayName = formatDayName(forecast.date)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Day name
        Text(
            text = dayName,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )

        // Weather icon
        Text(
            text = forecast.weatherCode.emoji,
            fontSize = 24.sp,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        // Temperatures
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${forecast.tempMin.roundToInt()}°",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(text = "/", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = "${forecast.tempMax.roundToInt()}°",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

/**
 * AC E7.3.2: Format day name (Today, Tomorrow, day of week)
 */
private fun formatDayName(date: LocalDate): String {
    val tz = TimeZone.currentSystemDefault()
    val now = kotlinx.datetime.Clock.System.now()
    val today = now.toLocalDateTime(tz).date
    val tomorrow = today.plus(1, DateTimeUnit.DAY)

    return when (date) {
        today -> "Today"
        tomorrow -> "Tomorrow"
        else -> when (date.dayOfWeek) {
            DayOfWeek.MONDAY -> "Monday"
            DayOfWeek.TUESDAY -> "Tuesday"
            DayOfWeek.WEDNESDAY -> "Wednesday"
            DayOfWeek.THURSDAY -> "Thursday"
            DayOfWeek.FRIDAY -> "Friday"
            DayOfWeek.SATURDAY -> "Saturday"
            DayOfWeek.SUNDAY -> "Sunday"
            else -> date.toString()
        }
    }
}

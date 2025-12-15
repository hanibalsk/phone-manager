package three.two.bit.phonemanager.ui.weather

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.time.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import three.two.bit.phonemanager.R
import three.two.bit.phonemanager.domain.model.DailyForecast
import three.two.bit.phonemanager.domain.model.Weather
import three.two.bit.phonemanager.domain.model.WeatherCode
import kotlin.math.roundToInt

/**
 * Story E7.3: Weather Screen - Display current conditions and 5-day forecast
 * Enhanced with Lottie animations and modern design
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WeatherViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    BackHandler {
        (context as? Activity)?.finish()
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Background based on weather
        val weatherCode = when (val state = uiState) {
            is WeatherUiState.Success -> state.weather.current.weatherCode
            else -> WeatherCode.CLEAR_SKY
        }

        WeatherBackground(weatherCode = weatherCode)

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            stringResource(R.string.weather_title),
                            color = Color.White,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { (context as? Activity)?.finish() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back),
                                tint = Color.White,
                            )
                        }
                    },
                    actions = {
                        if (uiState is WeatherUiState.Success) {
                            IconButton(onClick = { viewModel.refreshWeather() }) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = stringResource(R.string.refresh),
                                    tint = Color.White,
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                    ),
                )
            },
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            ) {
                when (val state = uiState) {
                    is WeatherUiState.Loading -> LoadingContent()
                    is WeatherUiState.Error -> ErrorContent(
                        message = state.message,
                        onRetry = { viewModel.refreshWeather() },
                    )
                    is WeatherUiState.Success -> WeatherContent(
                        weather = state.weather,
                        lastUpdatedText = state.lastUpdatedText,
                        isOffline = state.isOffline,
                    )
                }
            }
        }
    }
}

/**
 * Weather background with gradient
 */
@Composable
private fun WeatherBackground(weatherCode: WeatherCode) {
    val gradientColors = getWeatherGradient(weatherCode)

    Box(modifier = Modifier.fillMaxSize()) {
        // Gradient background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(gradientColors),
                ),
        )

        // Subtle overlay for better text readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.1f),
                            Color.Transparent,
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.2f),
                        ),
                    ),
                ),
        )
    }
}

/**
 * Gets gradient colors based on weather condition
 */
private fun getWeatherGradient(weatherCode: WeatherCode): List<Color> = when (weatherCode) {
    WeatherCode.CLEAR_SKY, WeatherCode.MAINLY_CLEAR -> listOf(
        Color(0xFF4FC3F7),
        Color(0xFF2196F3),
        Color(0xFF1976D2),
    )
    WeatherCode.PARTLY_CLOUDY -> listOf(
        Color(0xFF81D4FA),
        Color(0xFF4FC3F7),
        Color(0xFF29B6F6),
    )
    WeatherCode.OVERCAST -> listOf(
        Color(0xFF90A4AE),
        Color(0xFF78909C),
        Color(0xFF607D8B),
    )
    WeatherCode.FOG, WeatherCode.DEPOSITING_RIME_FOG -> listOf(
        Color(0xFFB0BEC5),
        Color(0xFF90A4AE),
        Color(0xFF78909C),
    )
    WeatherCode.DRIZZLE_LIGHT, WeatherCode.DRIZZLE_MODERATE, WeatherCode.DRIZZLE_DENSE,
    WeatherCode.RAIN_SLIGHT, WeatherCode.RAIN_SHOWERS_SLIGHT, WeatherCode.RAIN_SHOWERS_MODERATE,
    -> listOf(
        Color(0xFF607D8B),
        Color(0xFF546E7A),
        Color(0xFF455A64),
    )
    WeatherCode.RAIN_MODERATE, WeatherCode.RAIN_HEAVY, WeatherCode.RAIN_SHOWERS_VIOLENT,
    WeatherCode.FREEZING_DRIZZLE_LIGHT, WeatherCode.FREEZING_DRIZZLE_DENSE,
    WeatherCode.FREEZING_RAIN_LIGHT, WeatherCode.FREEZING_RAIN_HEAVY,
    -> listOf(
        Color(0xFF455A64),
        Color(0xFF37474F),
        Color(0xFF263238),
    )
    WeatherCode.SNOW_SLIGHT, WeatherCode.SNOW_MODERATE, WeatherCode.SNOW_HEAVY,
    WeatherCode.SNOW_GRAINS, WeatherCode.SNOW_SHOWERS_SLIGHT, WeatherCode.SNOW_SHOWERS_HEAVY,
    -> listOf(
        Color(0xFFE1F5FE),
        Color(0xFFB3E5FC),
        Color(0xFF81D4FA),
    )
    WeatherCode.THUNDERSTORM, WeatherCode.THUNDERSTORM_SLIGHT_HAIL,
    WeatherCode.THUNDERSTORM_HEAVY_HAIL,
    -> listOf(
        Color(0xFF37474F),
        Color(0xFF263238),
        Color(0xFF1A1A2E),
    )
    WeatherCode.UNKNOWN -> listOf(
        Color(0xFF4FC3F7),
        Color(0xFF2196F3),
        Color(0xFF1976D2),
    )
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Color.White)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                stringResource(R.string.weather_loading),
                color = Color.White,
            )
        }
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        GlassCard(modifier = Modifier.padding(24.dp)) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = message,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(16.dp))
                IconButton(onClick = onRetry) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = stringResource(R.string.weather_retry),
                        tint = Color.White,
                        modifier = Modifier.size(32.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun WeatherContent(weather: Weather, lastUpdatedText: String, isOffline: Boolean) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }

        // Main temperature display
        item {
            CurrentWeatherHero(weather = weather)
        }

        // Weather details row
        item {
            WeatherDetailsRow(weather = weather)
        }

        // 5-day forecast
        item {
            Text(
                text = stringResource(R.string.weather_forecast_title),
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }

        item {
            ForecastCard(forecasts = weather.daily)
        }

        // Last updated
        item {
            Text(
                text = if (isOffline) {
                    stringResource(R.string.weather_offline_indicator)
                } else {
                    stringResource(R.string.weather_last_updated, lastUpdatedText)
                },
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun CurrentWeatherHero(weather: Weather) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Small weather icon using emoji (fast, reliable)
        Text(
            text = weather.current.weatherCode.emoji,
            fontSize = 48.sp,
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Temperature
        Text(
            text = "${weather.current.temperature.roundToInt()}°",
            color = Color.White,
            fontSize = 96.sp,
            fontWeight = FontWeight.Thin,
        )

        // Condition
        Text(
            text = stringResource(weather.current.weatherCode.descriptionResId),
            color = Color.White.copy(alpha = 0.9f),
            style = MaterialTheme.typography.headlineSmall,
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Feels like
        Text(
            text = stringResource(R.string.weather_feels_like, weather.current.feelsLike.roundToInt()),
            color = Color.White.copy(alpha = 0.7f),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun WeatherDetailsRow(weather: Weather) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        WeatherDetailItem(
            icon = Icons.Default.WaterDrop,
            value = "${weather.current.humidity}%",
            label = stringResource(R.string.weather_humidity_label),
        )
        WeatherDetailItem(
            icon = Icons.Default.Air,
            value = stringResource(R.string.weather_wind, weather.current.windSpeed),
            label = stringResource(R.string.weather_wind_label),
        )
    }
}

@Composable
private fun WeatherDetailItem(icon: ImageVector, value: String, label: String) {
    GlassCard(
        modifier = Modifier.width(140.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(28.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = label,
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun ForecastCard(forecasts: List<DailyForecast>) {
    GlassCard {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(forecasts) { forecast ->
                ForecastDayItem(forecast = forecast)
            }
        }
    }
}

@Composable
private fun ForecastDayItem(forecast: DailyForecast) {
    val dayName = formatDayName(
        date = forecast.date,
        todayStr = stringResource(R.string.day_today),
        tomorrowStr = stringResource(R.string.day_tomorrow),
        mondayStr = stringResource(R.string.day_monday),
        tuesdayStr = stringResource(R.string.day_tuesday),
        wednesdayStr = stringResource(R.string.day_wednesday),
        thursdayStr = stringResource(R.string.day_thursday),
        fridayStr = stringResource(R.string.day_friday),
        saturdayStr = stringResource(R.string.day_saturday),
        sundayStr = stringResource(R.string.day_sunday),
    )

    Column(
        modifier = Modifier.width(72.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = dayName,
            color = Color.White.copy(alpha = 0.7f),
            style = MaterialTheme.typography.bodySmall,
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Weather emoji icon (standard, fast loading)
        Text(
            text = forecast.weatherCode.emoji,
            fontSize = 32.sp,
        )

        Spacer(modifier = Modifier.height(8.dp))

        // High temp
        Text(
            text = "${forecast.tempMax.roundToInt()}°",
            color = Color.White,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
        )

        // Low temp
        Text(
            text = "${forecast.tempMin.roundToInt()}°",
            color = Color.White.copy(alpha = 0.6f),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

/**
 * Glass morphism style card
 */
@Composable
private fun GlassCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.15f),
        ),
    ) {
        content()
    }
}

private fun formatDayName(
    date: LocalDate,
    todayStr: String,
    tomorrowStr: String,
    mondayStr: String,
    tuesdayStr: String,
    wednesdayStr: String,
    thursdayStr: String,
    fridayStr: String,
    saturdayStr: String,
    sundayStr: String,
): String {
    val tz = TimeZone.currentSystemDefault()
    val now = Clock.System.now()
    val today = now.toLocalDateTime(tz).date
    val tomorrow = today.plus(1, DateTimeUnit.DAY)

    return when (date) {
        today -> todayStr
        tomorrow -> tomorrowStr
        else -> when (date.dayOfWeek) {
            DayOfWeek.MONDAY -> mondayStr
            DayOfWeek.TUESDAY -> tuesdayStr
            DayOfWeek.WEDNESDAY -> wednesdayStr
            DayOfWeek.THURSDAY -> thursdayStr
            DayOfWeek.FRIDAY -> fridayStr
            DayOfWeek.SATURDAY -> saturdayStr
            DayOfWeek.SUNDAY -> sundayStr
        }
    }
}

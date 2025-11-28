package three.two.bit.phonemanager.ui.weather

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import three.two.bit.phonemanager.data.repository.LocationRepository
import three.two.bit.phonemanager.data.repository.WeatherRepository
import three.two.bit.phonemanager.domain.model.Weather
import three.two.bit.phonemanager.network.NetworkManager
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes

/**
 * Story E7.3: WeatherViewModel
 *
 * AC E7.3.4, E7.3.5, E7.3.6: Manages weather UI state and refresh
 */

/**
 * UI state for WeatherScreen
 * AC E7.3.5, E7.3.6: Loading, Success, and Error states
 */
sealed class WeatherUiState {
    data object Loading : WeatherUiState()
    data class Success(val weather: Weather, val lastUpdatedText: String, val isOffline: Boolean = false) :
        WeatherUiState()

    data class Error(val message: String) : WeatherUiState()
}

@HiltViewModel
class WeatherViewModel @Inject constructor(
    private val weatherRepository: WeatherRepository,
    private val locationRepository: LocationRepository,
    private val networkManager: NetworkManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow<WeatherUiState>(WeatherUiState.Loading)
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    init {
        loadWeather()
    }

    /**
     * AC E7.3.4: Pull-to-refresh functionality
     */
    fun refreshWeather() {
        viewModelScope.launch {
            _uiState.value = WeatherUiState.Loading
            loadWeather()
        }
    }

    /**
     * Load weather for current location
     * AC E7.3.3: Show last updated time
     * AC E7.3.6: Show error if no data available
     */
    private fun loadWeather() {
        viewModelScope.launch {
            try {
                // Get latest location
                val location = locationRepository.getLatestLocation().first()

                if (location == null) {
                    _uiState.value = WeatherUiState.Error("No location data available")
                    return@launch
                }

                // Fetch weather (uses cache if valid)
                val weather = weatherRepository.getWeather(location.latitude, location.longitude)

                if (weather != null) {
                    val isOffline = !networkManager.isNetworkAvailable()
                    _uiState.value = WeatherUiState.Success(
                        weather = weather,
                        lastUpdatedText = formatLastUpdated(weather.lastUpdated),
                        isOffline = isOffline,
                    )
                } else {
                    _uiState.value = WeatherUiState.Error("Unable to load weather")
                }
            } catch (e: Exception) {
                _uiState.value = WeatherUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    /**
     * AC E7.3.3: Format last updated time as relative string
     */
    private fun formatLastUpdated(lastUpdated: Instant): String {
        val now = Clock.System.now()
        val duration = now - lastUpdated
        val minutes = duration.inWholeMinutes

        return when {
            minutes < 1 -> "Just now"
            minutes < 60 -> "$minutes ${if (minutes == 1L) "minute" else "minutes"} ago"
            else -> {
                val hours = duration.inWholeHours
                "$hours ${if (hours == 1L) "hour" else "hours"} ago"
            }
        }
    }
}

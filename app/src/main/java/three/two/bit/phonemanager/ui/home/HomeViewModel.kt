package three.two.bit.phonemanager.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import three.two.bit.phonemanager.data.preferences.PreferencesRepository
import three.two.bit.phonemanager.data.repository.TripRepository
import three.two.bit.phonemanager.domain.model.TodayTripStats
import three.two.bit.phonemanager.domain.model.Trip
import three.two.bit.phonemanager.trip.TripManager
import javax.inject.Inject

/**
 * Story E2.1: HomeViewModel
 * Story E8.13: Active trip and daily summary
 *
 * Manages secret mode state and provides toggle functionality
 * Provides active trip state and daily statistics for home screen cards
 * ACs: E2.1.1, E2.1.2, E2.1.3, E8.13.4, E8.13.5
 */
@HiltViewModel
class HomeViewModel
@Inject
constructor(
    private val preferencesRepository: PreferencesRepository,
    private val tripManager: TripManager,
    private val tripRepository: TripRepository,
) : ViewModel() {

    /**
     * Secret mode state (AC E2.1.1)
     */
    val isSecretModeEnabled: StateFlow<Boolean> =
        preferencesRepository.isSecretModeEnabled
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = false,
            )

    /**
     * Story E8.13: Active trip state (AC E8.13.4)
     */
    val activeTrip: StateFlow<Trip?> = tripManager.activeTrip
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null,
        )

    /**
     * Story E8.13: Today's trip statistics (AC E8.13.4)
     */
    val todayStats: StateFlow<TodayTripStats> = tripRepository.observeTodayStats()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = TodayTripStats.EMPTY,
        )

    /**
     * Trip ending state for snackbar feedback
     */
    private val _tripEndedEvent = MutableStateFlow<Boolean>(false)
    val tripEndedEvent: StateFlow<Boolean> = _tripEndedEvent.asStateFlow()

    /**
     * Toggle secret mode (AC E2.1.2, E2.1.3)
     * Called by hidden gestures - no visible feedback
     */
    fun toggleSecretMode() {
        viewModelScope.launch {
            val currentState = isSecretModeEnabled.value
            preferencesRepository.setSecretModeEnabled(!currentState)
        }
    }

    /**
     * Story E8.13: End the active trip manually (AC E8.13.5)
     */
    fun endActiveTrip() {
        viewModelScope.launch {
            tripManager.forceEndTrip()
            _tripEndedEvent.value = true
        }
    }

    /**
     * Clear trip ended event after showing snackbar
     */
    fun clearTripEndedEvent() {
        _tripEndedEvent.value = false
    }
}

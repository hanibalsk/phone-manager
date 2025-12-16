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
import three.two.bit.phonemanager.data.repository.GroupRepository
import three.two.bit.phonemanager.data.repository.TripRepository
import three.two.bit.phonemanager.domain.model.Group
import three.two.bit.phonemanager.domain.model.TodayTripStats
import three.two.bit.phonemanager.domain.model.Trip
import three.two.bit.phonemanager.trip.TripManager
import timber.log.Timber
import javax.inject.Inject

/**
 * Story E2.1: HomeViewModel
 * Story E8.13: Active trip and daily summary
 * Story E9.1: Admin role detection
 *
 * Manages secret mode state and provides toggle functionality
 * Provides active trip state and daily statistics for home screen cards
 * Provides admin access detection for admin toggle on homescreen
 * ACs: E2.1.1, E2.1.2, E2.1.3, E8.13.4, E8.13.5, E9.1.1, E9.1.2
 */
@HiltViewModel
class HomeViewModel
@Inject
constructor(
    private val preferencesRepository: PreferencesRepository,
    private val tripManager: TripManager,
    private val tripRepository: TripRepository,
    private val groupRepository: GroupRepository,
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
     * Story E9.1: Admin access detection (AC E9.1.1, E9.1.2)
     *
     * True if user is OWNER or ADMIN in at least one group.
     * Used to show/hide admin toggle on homescreen.
     */
    val hasAdminAccess: StateFlow<Boolean> = groupRepository.hasAdminAccess
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false,
        )

    /**
     * Story E9.1: Groups where user has admin access (AC E9.1.3)
     *
     * List of groups where user is OWNER or ADMIN.
     * Used to populate admin users list.
     */
    val adminGroups: StateFlow<List<Group>> = groupRepository.adminGroups
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    /**
     * Story E9.2: Admin view mode toggle state
     *
     * When true, homescreen shows "Users" view instead of "My Device" view.
     */
    private val _isAdminViewMode = MutableStateFlow(false)
    val isAdminViewMode: StateFlow<Boolean> = _isAdminViewMode.asStateFlow()

    init {
        // Load groups on init to populate admin access cache
        refreshAdminAccess()
    }

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

    /**
     * Story E9.1: Refresh admin access by loading user groups (AC E9.1.4)
     *
     * Called on init and can be called to refresh admin status.
     */
    fun refreshAdminAccess() {
        viewModelScope.launch {
            groupRepository.getUserGroups()
                .onSuccess { groups ->
                    Timber.d("Loaded ${groups.size} groups for admin access check")
                }
                .onFailure { error ->
                    Timber.w(error, "Failed to load groups for admin access check")
                }
        }
    }

    /**
     * Story E9.2: Toggle admin view mode (AC E9.2.1)
     *
     * Switches between "My Device" and "Users" view on homescreen.
     */
    fun toggleAdminViewMode() {
        _isAdminViewMode.value = !_isAdminViewMode.value
    }

    /**
     * Story E9.2: Set admin view mode explicitly
     */
    fun setAdminViewMode(enabled: Boolean) {
        _isAdminViewMode.value = enabled
    }
}

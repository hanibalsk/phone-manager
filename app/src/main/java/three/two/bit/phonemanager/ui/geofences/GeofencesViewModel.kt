package three.two.bit.phonemanager.ui.geofences

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import three.two.bit.phonemanager.data.repository.GeofenceRepository
import three.two.bit.phonemanager.data.repository.WebhookRepository
import three.two.bit.phonemanager.domain.model.Geofence
import three.two.bit.phonemanager.domain.model.TransitionType
import three.two.bit.phonemanager.domain.model.Webhook
import three.two.bit.phonemanager.location.LocationManager
import timber.log.Timber
import javax.inject.Inject

/**
 * Story E6.1: GeofencesViewModel
 *
 * Manages geofence state and user actions
 * AC E6.1.5: Geofence management (list, create, edit, delete, toggle)
 */
@HiltViewModel
class GeofencesViewModel @Inject constructor(
    private val geofenceRepository: GeofenceRepository,
    private val webhookRepository: WebhookRepository,
    private val locationManager: LocationManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(GeofencesUiState())
    val uiState: StateFlow<GeofencesUiState> = _uiState.asStateFlow()

    private val _currentLocationState = MutableStateFlow(CurrentLocationState())
    val currentLocationState: StateFlow<CurrentLocationState> = _currentLocationState.asStateFlow()

    /**
     * Observable geofences from repository
     */
    val geofences: StateFlow<List<Geofence>> = geofenceRepository.observeGeofences()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    /**
     * Story E6.3: Observable webhooks for geofence linking (AC E6.3.2)
     */
    val webhooks: StateFlow<List<Webhook>> = webhookRepository.observeWebhooks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    init {
        // Sync on startup
        syncFromServer()
    }

    /**
     * Sync geofences from server on startup
     */
    private fun syncFromServer() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true) }
            geofenceRepository.syncFromServer().fold(
                onSuccess = { count ->
                    Timber.i("Synced $count geofences from server")
                    _uiState.update { it.copy(isSyncing = false, syncError = null) }
                },
                onFailure = { error ->
                    Timber.w(error, "Failed to sync geofences from server")
                    _uiState.update { it.copy(isSyncing = false, syncError = error.message) }
                },
            )
        }
    }

    /**
     * Refresh data
     */
    fun refresh() {
        syncFromServer()
    }

    /**
     * Create a new geofence (AC E6.1.5, AC E6.3.2)
     *
     * @param webhookId Optional webhook to link for event delivery
     */
    fun createGeofence(
        name: String,
        latitude: Double,
        longitude: Double,
        radiusMeters: Int,
        transitionTypes: Set<TransitionType>,
        webhookId: String? = null,
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCreating = true) }
            geofenceRepository.createGeofence(
                name = name,
                latitude = latitude,
                longitude = longitude,
                radiusMeters = radiusMeters,
                transitionTypes = transitionTypes,
                webhookId = webhookId,
            ).fold(
                onSuccess = { geofence ->
                    Timber.i("Geofence created: ${geofence.id}")
                    _uiState.update { it.copy(isCreating = false, createError = null) }
                },
                onFailure = { error ->
                    Timber.e(error, "Failed to create geofence")
                    _uiState.update { it.copy(isCreating = false, createError = error.message) }
                },
            )
        }
    }

    /**
     * Toggle geofence active state (AC E6.1.5)
     */
    fun toggleGeofenceActive(geofenceId: String, active: Boolean) {
        viewModelScope.launch {
            geofenceRepository.toggleGeofenceActive(geofenceId, active).fold(
                onSuccess = {
                    Timber.d("Geofence ${if (active) "activated" else "deactivated"}: $geofenceId")
                },
                onFailure = { error ->
                    Timber.e(error, "Failed to toggle geofence")
                },
            )
        }
    }

    /**
     * Delete a geofence (AC E6.1.5)
     */
    fun deleteGeofence(geofenceId: String) {
        viewModelScope.launch {
            geofenceRepository.deleteGeofence(geofenceId).fold(
                onSuccess = {
                    Timber.i("Geofence deleted: $geofenceId")
                },
                onFailure = { error ->
                    Timber.e(error, "Failed to delete geofence")
                },
            )
        }
    }

    /**
     * Clear create error
     */
    fun clearCreateError() {
        _uiState.update { it.copy(createError = null) }
    }

    /**
     * Fetch current device location for geofence creation
     */
    fun fetchCurrentLocation() {
        viewModelScope.launch {
            _currentLocationState.update { it.copy(isLoading = true, error = null) }
            locationManager.getCurrentLocation().fold(
                onSuccess = { locationEntity ->
                    if (locationEntity != null) {
                        Timber.i("Current location fetched: ${locationEntity.latitude}, ${locationEntity.longitude}")
                        _currentLocationState.update {
                            it.copy(
                                isLoading = false,
                                latitude = locationEntity.latitude,
                                longitude = locationEntity.longitude,
                                error = null,
                            )
                        }
                    } else {
                        Timber.w("Current location is null")
                        _currentLocationState.update {
                            it.copy(isLoading = false, error = "Location unavailable")
                        }
                    }
                },
                onFailure = { error ->
                    Timber.e(error, "Failed to get current location")
                    _currentLocationState.update {
                        it.copy(isLoading = false, error = error.message ?: "Failed to get location")
                    }
                },
            )
        }
    }

    /**
     * Clear current location state
     */
    fun clearLocationState() {
        _currentLocationState.update { CurrentLocationState() }
    }
}

/**
 * UI State for Geofences Screen
 */
data class GeofencesUiState(
    val isSyncing: Boolean = false,
    val syncError: String? = null,
    val isCreating: Boolean = false,
    val createError: String? = null,
)

/**
 * State for current location fetch
 */
data class CurrentLocationState(
    val isLoading: Boolean = false,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val error: String? = null,
)

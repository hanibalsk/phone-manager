package three.two.bit.phonemanager.ui.admin

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import three.two.bit.phonemanager.data.repository.DeviceRepository
import three.two.bit.phonemanager.data.repository.GeofenceRepository
import three.two.bit.phonemanager.domain.model.Device
import three.two.bit.phonemanager.domain.model.Geofence
import three.two.bit.phonemanager.domain.model.TransitionType
import timber.log.Timber
import javax.inject.Inject

/**
 * Story E9.4: Admin Geofence ViewModel
 *
 * Manages geofence state for admin management of user geofences.
 * Admins can view, create, and delete geofences for managed users.
 *
 * ACs: E9.4.1, E9.4.2, E9.4.3, E9.4.4
 */
@HiltViewModel
class AdminGeofenceViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val geofenceRepository: GeofenceRepository,
    private val deviceRepository: DeviceRepository,
) : ViewModel() {

    private val groupId: String = savedStateHandle.get<String>("groupId") ?: ""
    private val deviceId: String = savedStateHandle.get<String>("deviceId") ?: ""

    private val _uiState = MutableStateFlow(AdminGeofenceUiState())
    val uiState: StateFlow<AdminGeofenceUiState> = _uiState.asStateFlow()

    init {
        loadDeviceAndGeofences()
    }

    /**
     * Load device info and its geofences
     *
     * AC E9.4.1: Display geofence management UI
     */
    private fun loadDeviceAndGeofences() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            // Load device info for display
            deviceRepository.getGroupDevices(groupId)
                .onSuccess { devices ->
                    val device = devices.find { it.deviceId == deviceId }
                    _uiState.update { it.copy(device = device) }
                }
                .onFailure { error ->
                    Timber.w(error, "Failed to load device info")
                }

            // Load geofences for this device
            loadGeofences()
        }
    }

    /**
     * Load geofences for the managed device
     *
     * AC E9.4.2: List existing geofences for user
     */
    fun loadGeofences() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            geofenceRepository.getGeofencesForDevice(deviceId)
                .onSuccess { geofences ->
                    Timber.d("Loaded ${geofences.size} geofences for device $deviceId")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            geofences = geofences,
                            error = null,
                        )
                    }
                }
                .onFailure { error ->
                    Timber.w(error, "Failed to load geofences for device $deviceId")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Failed to load geofences",
                        )
                    }
                }
        }
    }

    /**
     * Create a new geofence for the managed device
     *
     * AC E9.4.3: Define geofence boundaries on map
     * AC E9.4.4: Save geofence to backend per user
     */
    fun createGeofence(
        name: String,
        latitude: Double,
        longitude: Double,
        radiusMeters: Int,
        transitionTypes: Set<TransitionType>,
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCreating = true, createError = null) }

            geofenceRepository.createGeofenceForDevice(
                deviceId = deviceId,
                name = name,
                latitude = latitude,
                longitude = longitude,
                radiusMeters = radiusMeters,
                transitionTypes = transitionTypes,
            ).onSuccess { geofence ->
                Timber.i("Created geofence for device $deviceId: ${geofence.id}")
                _uiState.update {
                    it.copy(
                        isCreating = false,
                        createError = null,
                        showCreateDialog = false,
                    )
                }
                // Reload geofences to show the new one
                loadGeofences()
            }.onFailure { error ->
                Timber.e(error, "Failed to create geofence for device $deviceId")
                _uiState.update {
                    it.copy(
                        isCreating = false,
                        createError = error.message ?: "Failed to create geofence",
                    )
                }
            }
        }
    }

    /**
     * Delete a geofence
     *
     * AC E9.4.2: Edit/delete options for existing geofences
     */
    fun deleteGeofence(geofenceId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true) }

            geofenceRepository.deleteGeofenceForDevice(geofenceId)
                .onSuccess {
                    Timber.i("Deleted geofence: $geofenceId")
                    _uiState.update { it.copy(isDeleting = false) }
                    // Reload geofences
                    loadGeofences()
                }
                .onFailure { error ->
                    Timber.e(error, "Failed to delete geofence: $geofenceId")
                    _uiState.update {
                        it.copy(
                            isDeleting = false,
                            error = error.message ?: "Failed to delete geofence",
                        )
                    }
                }
        }
    }

    /**
     * Show the create geofence dialog
     */
    fun showCreateDialog() {
        _uiState.update { it.copy(showCreateDialog = true) }
    }

    /**
     * Hide the create geofence dialog
     */
    fun hideCreateDialog() {
        _uiState.update { it.copy(showCreateDialog = false, createError = null) }
    }

    /**
     * Show delete confirmation for a geofence
     */
    fun showDeleteConfirmation(geofence: Geofence) {
        _uiState.update { it.copy(geofenceToDelete = geofence) }
    }

    /**
     * Hide delete confirmation dialog
     */
    fun hideDeleteConfirmation() {
        _uiState.update { it.copy(geofenceToDelete = null) }
    }

    /**
     * Clear error state
     */
    fun clearError() {
        _uiState.update { it.copy(error = null, createError = null) }
    }

    /**
     * Refresh geofences
     */
    fun refresh() {
        loadGeofences()
    }
}

/**
 * UI State for Admin Geofence Screen
 *
 * @property device The managed device info
 * @property geofences List of geofences for the device
 * @property isLoading True when loading geofences
 * @property isCreating True when creating a new geofence
 * @property isDeleting True when deleting a geofence
 * @property error General error message
 * @property createError Error during geofence creation
 * @property showCreateDialog True when showing create dialog
 * @property geofenceToDelete Geofence pending delete confirmation
 */
data class AdminGeofenceUiState(
    val device: Device? = null,
    val geofences: List<Geofence> = emptyList(),
    val isLoading: Boolean = false,
    val isCreating: Boolean = false,
    val isDeleting: Boolean = false,
    val error: String? = null,
    val createError: String? = null,
    val showCreateDialog: Boolean = false,
    val geofenceToDelete: Geofence? = null,
)

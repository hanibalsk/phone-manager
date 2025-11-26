package three.two.bit.phonemanager.ui.alerts

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
import three.two.bit.phonemanager.data.repository.AlertRepository
import three.two.bit.phonemanager.data.repository.DeviceRepository
import three.two.bit.phonemanager.domain.model.AlertDirection
import three.two.bit.phonemanager.domain.model.Device
import three.two.bit.phonemanager.domain.model.ProximityAlert
import timber.log.Timber
import javax.inject.Inject

/**
 * Story E5.1: AlertsViewModel
 *
 * Manages proximity alert state and user actions
 * AC E5.1.5: Alert management (list, create, edit, delete, toggle)
 * AC E5.1.6: Sync on startup
 */
@HiltViewModel
class AlertsViewModel @Inject constructor(
    private val alertRepository: AlertRepository,
    private val deviceRepository: DeviceRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AlertsUiState())
    val uiState: StateFlow<AlertsUiState> = _uiState.asStateFlow()

    /**
     * Observable alerts from repository
     */
    val alerts: StateFlow<List<ProximityAlert>> = alertRepository.observeAlerts()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    init {
        // AC E5.1.6: Sync on startup
        syncFromServer()
        loadGroupMembers()
    }

    /**
     * AC E5.1.6: Sync alerts from server on startup
     */
    private fun syncFromServer() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true) }
            alertRepository.syncFromServer().fold(
                onSuccess = { count ->
                    Timber.i("Synced $count alerts from server")
                    _uiState.update { it.copy(isSyncing = false, syncError = null) }
                },
                onFailure = { error ->
                    Timber.w(error, "Failed to sync alerts from server")
                    _uiState.update { it.copy(isSyncing = false, syncError = error.message) }
                },
            )
        }
    }

    /**
     * Load group members for target device selection
     */
    private fun loadGroupMembers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMembers = true) }
            deviceRepository.getGroupMembers().fold(
                onSuccess = { members ->
                    _uiState.update { it.copy(groupMembers = members, isLoadingMembers = false) }
                },
                onFailure = { error ->
                    Timber.w(error, "Failed to load group members")
                    _uiState.update { it.copy(isLoadingMembers = false) }
                },
            )
        }
    }

    /**
     * Refresh data
     */
    fun refresh() {
        syncFromServer()
        loadGroupMembers()
    }

    /**
     * Create a new proximity alert (AC E5.1.5)
     */
    fun createAlert(targetDeviceId: String, radiusMeters: Int, direction: AlertDirection) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCreating = true) }
            alertRepository.createAlert(targetDeviceId, radiusMeters, direction).fold(
                onSuccess = { alert ->
                    Timber.i("Alert created: ${alert.id}")
                    _uiState.update { it.copy(isCreating = false, createError = null) }
                },
                onFailure = { error ->
                    Timber.e(error, "Failed to create alert")
                    _uiState.update { it.copy(isCreating = false, createError = error.message) }
                },
            )
        }
    }

    /**
     * Toggle alert active state (AC E5.1.5)
     */
    fun toggleAlertActive(alertId: String, active: Boolean) {
        viewModelScope.launch {
            alertRepository.toggleAlertActive(alertId, active).fold(
                onSuccess = {
                    Timber.d("Alert ${if (active) "activated" else "deactivated"}: $alertId")
                },
                onFailure = { error ->
                    Timber.e(error, "Failed to toggle alert")
                },
            )
        }
    }

    /**
     * Delete an alert (AC E5.1.5)
     */
    fun deleteAlert(alertId: String) {
        viewModelScope.launch {
            alertRepository.deleteAlert(alertId).fold(
                onSuccess = {
                    Timber.i("Alert deleted: $alertId")
                },
                onFailure = { error ->
                    Timber.e(error, "Failed to delete alert")
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
}

/**
 * UI State for Alerts Screen
 */
data class AlertsUiState(
    val groupMembers: List<Device> = emptyList(),
    val isLoadingMembers: Boolean = false,
    val isSyncing: Boolean = false,
    val syncError: String? = null,
    val isCreating: Boolean = false,
    val createError: String? = null,
)

package three.two.bit.phonemanager.ui.debug

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import three.two.bit.phonemanager.movement.DebugDetectionState
import three.two.bit.phonemanager.movement.TransportationModeManager
import three.two.bit.phonemanager.movement.TransportationState
import timber.log.Timber
import javax.inject.Inject

/**
 * Movement Debug Screen ViewModel
 *
 * Provides real-time debug information about transportation detection
 * for diagnosing issues with car detection, movement detection, and Android Auto.
 */
@HiltViewModel
class MovementDebugViewModel @Inject constructor(
    private val transportationModeManager: TransportationModeManager,
) : ViewModel() {

    /**
     * Current debug detection state with all source information.
     */
    val debugState: StateFlow<DebugDetectionState> = transportationModeManager.debugState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = transportationModeManager.debugState.value,
        )

    /**
     * Current transportation state.
     */
    val transportationState: StateFlow<TransportationState> = transportationModeManager.transportationState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = TransportationState(),
        )

    /**
     * Whether monitoring is currently active.
     */
    val isMonitoring: StateFlow<Boolean> = transportationModeManager.isMonitoring
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false,
        )

    private val _eventHistory = MutableStateFlow<List<DebugEvent>>(emptyList())
    val eventHistory: StateFlow<List<DebugEvent>> = _eventHistory.asStateFlow()

    private val _isLiveUpdating = MutableStateFlow(true)
    val isLiveUpdating: StateFlow<Boolean> = _isLiveUpdating.asStateFlow()

    init {
        // Subscribe to state changes and log them to history
        viewModelScope.launch {
            debugState.collect { state ->
                if (_isLiveUpdating.value) {
                    addEvent(
                        DebugEvent(
                            timestamp = System.currentTimeMillis(),
                            type = "state_update",
                            message = "Mode: ${state.activityMode} (${state.activityConfidence}%), " +
                                "Car: ${state.isBluetoothCarConnected}, Auto: ${state.isAndroidAutoActive}",
                        ),
                    )
                }
            }
        }
    }

    /**
     * Restart all monitoring services.
     */
    fun restartMonitoring() {
        viewModelScope.launch {
            Timber.i("Restarting transportation monitoring from debug screen")
            addEvent(DebugEvent(System.currentTimeMillis(), "action", "Restarting monitoring..."))

            transportationModeManager.stopMonitoring()
            transportationModeManager.startMonitoring()

            addEvent(DebugEvent(System.currentTimeMillis(), "action", "Monitoring restarted"))
        }
    }

    /**
     * Toggle live updates.
     */
    fun toggleLiveUpdates() {
        _isLiveUpdating.value = !_isLiveUpdating.value
        val status = if (_isLiveUpdating.value) "enabled" else "paused"
        addEvent(DebugEvent(System.currentTimeMillis(), "action", "Live updates $status"))
    }

    /**
     * Clear event history.
     */
    fun clearHistory() {
        _eventHistory.value = emptyList()
    }

    private fun addEvent(event: DebugEvent) {
        // Keep only the last 100 events
        _eventHistory.value = (listOf(event) + _eventHistory.value).take(100)
    }
}

/**
 * Debug event for history tracking.
 */
data class DebugEvent(
    val timestamp: Long,
    val type: String,
    val message: String,
)

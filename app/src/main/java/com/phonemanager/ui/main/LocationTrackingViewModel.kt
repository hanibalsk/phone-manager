package com.phonemanager.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phonemanager.data.preferences.PreferencesRepository
import com.phonemanager.data.repository.LocationRepository
import com.phonemanager.domain.model.EnhancedServiceState
import com.phonemanager.domain.model.LocationStats
import com.phonemanager.domain.model.ServiceStatus
import com.phonemanager.permission.PermissionManager
import com.phonemanager.permission.PermissionState
import com.phonemanager.service.LocationServiceController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/**
 * Story 1.1/1.3: LocationTrackingViewModel - Manages location tracking toggle state and service monitoring
 */
@HiltViewModel
class LocationTrackingViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val serviceController: LocationServiceController,
    private val permissionManager: PermissionManager,
    private val locationRepository: LocationRepository
) : ViewModel() {

    private val _trackingState = MutableStateFlow<TrackingState>(TrackingState.Stopped)
    val trackingState: StateFlow<TrackingState> = _trackingState.asStateFlow()

    private val _permissionState = MutableStateFlow<PermissionState>(PermissionState.Checking)
    val permissionState: StateFlow<PermissionState> = _permissionState.asStateFlow()

    /**
     * Story 1.3: Enhanced service state for ServiceStatusCard
     */
    val serviceState: StateFlow<EnhancedServiceState> = serviceController.observeEnhancedServiceState()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = EnhancedServiceState(
                isRunning = false,
                status = ServiceStatus.STOPPED,
                lastUpdate = null,
                locationCount = 0,
                currentInterval = java.time.Duration.ofMinutes(5),
                healthStatus = com.phonemanager.data.model.HealthStatus.HEALTHY,
                errorMessage = null
            )
        )

    /**
     * Story 1.3: Location statistics for LocationStatsCard
     */
    val locationStats: StateFlow<LocationStats> = combine(
        locationRepository.observeLocationCount(),
        locationRepository.observeTodayLocationCount(),
        locationRepository.observeLastLocation(),
        locationRepository.observeAverageAccuracy(),
        preferencesRepository.trackingInterval
    ) { totalCount, todayCount, lastLocation, avgAccuracy, intervalMinutes ->
        LocationStats(
            totalCount = totalCount,
            todayCount = todayCount,
            lastLocation = lastLocation,
            averageAccuracy = avgAccuracy,
            trackingInterval = java.time.Duration.ofMinutes(intervalMinutes.toLong())
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LocationStats(
            totalCount = 0,
            todayCount = 0,
            lastLocation = null,
            averageAccuracy = null,
            trackingInterval = java.time.Duration.ofMinutes(5)
        )
    )

    init {
        viewModelScope.launch {
            // Load persisted toggle state
            preferencesRepository.isTrackingEnabled.collect { enabled ->
                // Reconcile with actual service state
                val isServiceActuallyRunning = serviceController.isServiceRunning()

                _trackingState.value = when {
                    enabled && isServiceActuallyRunning -> TrackingState.Active()
                    enabled && !isServiceActuallyRunning -> {
                        // State desync - service should be running but isn't
                        Timber.w("State desync detected: toggle ON but service not running")
                        TrackingState.Stopped
                    }
                    else -> TrackingState.Stopped
                }
            }
        }

        viewModelScope.launch {
            // Monitor permission state
            permissionManager.observePermissionState().collect { state ->
                _permissionState.value = state
            }
        }
    }

    fun toggleTracking() {
        viewModelScope.launch {
            when (_trackingState.value) {
                is TrackingState.Stopped -> startTracking()
                is TrackingState.Active -> stopTracking()
                is TrackingState.Starting, is TrackingState.Stopping -> {
                    // Ignore rapid taps during transition
                    Timber.d("Ignoring toggle during transition state")
                }
            }
        }
    }

    private suspend fun startTracking() {
        if (!permissionManager.hasAllRequiredPermissions()) {
            Timber.w("Cannot start tracking: permissions not granted")
            return
        }

        _trackingState.value = TrackingState.Starting

        serviceController.startTracking()
            .onSuccess {
                preferencesRepository.setTrackingEnabled(true)
                _trackingState.value = TrackingState.Active()
                Timber.i("Tracking started successfully")
            }
            .onFailure { error ->
                _trackingState.value = TrackingState.Error(error.message ?: "Failed to start")
                Timber.e(error, "Failed to start tracking")
            }
    }

    private suspend fun stopTracking() {
        _trackingState.value = TrackingState.Stopping

        serviceController.stopTracking()
            .onSuccess {
                preferencesRepository.setTrackingEnabled(false)
                _trackingState.value = TrackingState.Stopped
                Timber.i("Tracking stopped successfully")
            }
            .onFailure { error ->
                _trackingState.value = TrackingState.Error(error.message ?: "Failed to stop")
                Timber.e(error, "Failed to stop tracking")
            }
    }
}

/**
 * Story 1.1: TrackingState - Sealed class representing tracking states
 */
sealed class TrackingState {
    object Stopped : TrackingState()
    object Starting : TrackingState()
    data class Active(val lastUpdate: Instant? = null) : TrackingState()
    object Stopping : TrackingState()
    data class Error(val message: String) : TrackingState()
}

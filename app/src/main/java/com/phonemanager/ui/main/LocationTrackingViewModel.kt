package com.phonemanager.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phonemanager.analytics.Analytics
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
    private val locationRepository: LocationRepository,
    private val analytics: Analytics
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
            // Story 1.4: Load persisted toggle state and reconcile with actual service state
            preferencesRepository.isTrackingEnabled.collect { persistedEnabled ->
                reconcileServiceState(persistedEnabled)
            }
        }

        viewModelScope.launch {
            // Monitor permission state
            permissionManager.observePermissionState().collect { state ->
                _permissionState.value = state
            }
        }
    }

    /**
     * Story 1.4: Reconcile persisted state with actual service state.
     *
     * Handles state desynchronization by:
     * - Restarting service if persisted=ON but actual=OFF
     * - Stopping service if persisted=OFF but actual=ON (logs and corrects)
     * - Setting correct UI state when states match
     */
    private suspend fun reconcileServiceState(persistedEnabled: Boolean) {
        val isServiceActuallyRunning = serviceController.isServiceRunning()

        when {
            persistedEnabled && !isServiceActuallyRunning -> {
                // State desync: should be running but isn't - restart service
                Timber.w("State desync detected: persisted=ON, actual=OFF - restarting service")
                if (permissionManager.hasAllRequiredPermissions()) {
                    _trackingState.value = TrackingState.Starting
                    serviceController.startTracking()
                        .onSuccess {
                            _trackingState.value = TrackingState.Active()
                            analytics.logServiceStateChanged("restored_after_desync")
                            Timber.i("Service restored successfully after state desync")
                        }
                        .onFailure { error ->
                            // If restart fails, sync persisted state to match reality
                            preferencesRepository.setTrackingEnabled(false)
                            _trackingState.value = TrackingState.Stopped
                            Timber.e(error, "Failed to restore service after desync, corrected persisted state")
                        }
                } else {
                    // Can't restart without permissions, correct persisted state
                    preferencesRepository.setTrackingEnabled(false)
                    _trackingState.value = TrackingState.Stopped
                    Timber.w("Cannot restore service: permissions not granted, corrected persisted state")
                }
            }
            !persistedEnabled && isServiceActuallyRunning -> {
                // State desync: shouldn't be running but is - stop and correct
                Timber.w("State desync detected: persisted=OFF, actual=ON - stopping service")
                _trackingState.value = TrackingState.Stopping
                serviceController.stopTracking()
                    .onSuccess {
                        _trackingState.value = TrackingState.Stopped
                        analytics.logServiceStateChanged("stopped_after_desync")
                        Timber.i("Service stopped after state desync correction")
                    }
                    .onFailure { error ->
                        _trackingState.value = TrackingState.Error(error.message ?: "Failed to stop")
                        Timber.e(error, "Failed to stop service after desync")
                    }
            }
            persistedEnabled && isServiceActuallyRunning -> {
                _trackingState.value = TrackingState.Active()
            }
            else -> {
                _trackingState.value = TrackingState.Stopped
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
                is TrackingState.Error -> startTracking() // Allow retry from error state
            }
        }
    }

    private suspend fun startTracking() {
        if (!permissionManager.hasAllRequiredPermissions()) {
            Timber.w("Cannot start tracking: permissions not granted")
            return
        }

        _trackingState.value = TrackingState.Starting
        analytics.logServiceStateChanged("starting")

        serviceController.startTracking()
            .onSuccess {
                preferencesRepository.setTrackingEnabled(true)
                _trackingState.value = TrackingState.Active()
                analytics.logTrackingToggled(true)
                analytics.logServiceStateChanged("running")
                Timber.i("Tracking started successfully")
            }
            .onFailure { error ->
                _trackingState.value = TrackingState.Error(error.message ?: "Failed to start")
                analytics.logServiceStateChanged("error")
                Timber.e(error, "Failed to start tracking")
            }
    }

    private suspend fun stopTracking() {
        _trackingState.value = TrackingState.Stopping
        analytics.logServiceStateChanged("stopping")

        serviceController.stopTracking()
            .onSuccess {
                preferencesRepository.setTrackingEnabled(false)
                _trackingState.value = TrackingState.Stopped
                analytics.logTrackingToggled(false)
                analytics.logServiceStateChanged("stopped")
                Timber.i("Tracking stopped successfully")
            }
            .onFailure { error ->
                _trackingState.value = TrackingState.Error(error.message ?: "Failed to stop")
                analytics.logServiceStateChanged("error")
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

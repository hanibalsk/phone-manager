package com.phonemanager.service

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Build
import com.phonemanager.data.model.HealthStatus
import com.phonemanager.data.repository.LocationRepository
import com.phonemanager.data.preferences.PreferencesRepository
import com.phonemanager.domain.model.EnhancedServiceState
import com.phonemanager.domain.model.ServiceStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import timber.log.Timber
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Story 1.1/1.3: LocationServiceController - Abstracts service lifecycle and communication
 */
interface LocationServiceController {
    suspend fun startTracking(): Result<Unit>
    suspend fun stopTracking(): Result<Unit>
    fun observeServiceState(): Flow<ServiceState>
    fun observeEnhancedServiceState(): Flow<EnhancedServiceState>
    fun isServiceRunning(): Boolean
}

@Singleton
class LocationServiceControllerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val locationRepository: LocationRepository,
    private val preferencesRepository: PreferencesRepository
) : LocationServiceController {

    private val _serviceState = MutableStateFlow(
        ServiceState(
            isRunning = false,
            lastUpdate = null,
            locationCount = 0
        )
    )

    override suspend fun startTracking(): Result<Unit> {
        return try {
            val intent = Intent(context, LocationTrackingService::class.java).apply {
                action = LocationTrackingService.ACTION_START_TRACKING
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }

            _serviceState.value = _serviceState.value.copy(isRunning = true)

            Timber.i("Location tracking service start requested")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to start tracking service")
            Result.failure(e)
        }
    }

    override suspend fun stopTracking(): Result<Unit> {
        return try {
            val intent = Intent(context, LocationTrackingService::class.java).apply {
                action = LocationTrackingService.ACTION_STOP_TRACKING
            }
            context.startService(intent)

            _serviceState.value = _serviceState.value.copy(isRunning = false)

            Timber.i("Location tracking service stop requested")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to stop tracking service")
            Result.failure(e)
        }
    }

    override fun observeServiceState(): Flow<ServiceState> = _serviceState.asStateFlow()

    /**
     * Story 1.3: Observe enhanced service state combining multiple data sources
     */
    override fun observeEnhancedServiceState(): Flow<EnhancedServiceState> {
        return combine(
            _serviceState,
            locationRepository.observeServiceHealth(),
            locationRepository.observeLocationCount(),
            preferencesRepository.trackingInterval
        ) { serviceState, serviceHealth, locationCount, intervalMinutes ->
            EnhancedServiceState(
                isRunning = serviceState.isRunning,
                status = when {
                    !serviceState.isRunning -> ServiceStatus.STOPPED
                    serviceHealth.healthStatus == HealthStatus.GPS_ACQUIRING -> ServiceStatus.GPS_ACQUIRING
                    serviceHealth.healthStatus == HealthStatus.ERROR -> ServiceStatus.ERROR
                    else -> ServiceStatus.RUNNING
                },
                lastUpdate = serviceHealth.lastLocationUpdate?.let { Instant.ofEpochMilli(it) },
                locationCount = locationCount,
                currentInterval = java.time.Duration.ofMinutes(intervalMinutes.toLong()),
                healthStatus = serviceHealth.healthStatus,
                errorMessage = serviceHealth.errorMessage
            )
        }
    }

    /**
     * Story 1.4: Check if the location service is actually running at the OS level.
     *
     * Uses ActivityManager.getRunningServices() to check the actual service state,
     * not just in-memory state. This ensures accurate state reconciliation after
     * process death or service kill.
     *
     * Note: getRunningServices is deprecated since API 26 but still works.
     * Alternative approaches (binding, WorkManager query) are more complex.
     */
    override fun isServiceRunning(): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            ?: return _serviceState.value.isRunning // Fallback to in-memory state

        @Suppress("DEPRECATION") // Still works, and alternatives are more complex
        val runningServices = try {
            activityManager.getRunningServices(Int.MAX_VALUE)
        } catch (e: Exception) {
            Timber.w(e, "Failed to get running services, falling back to in-memory state")
            return _serviceState.value.isRunning
        }

        val isRunning = runningServices?.any { serviceInfo ->
            serviceInfo.service.className == LocationTrackingService::class.java.name
        } ?: false

        Timber.d("isServiceRunning check: actual=$isRunning, in-memory=${_serviceState.value.isRunning}")

        return isRunning
    }
}

/**
 * Story 1.1: ServiceState - Represents service state
 * Enhanced in Story 1.3 with more details
 */
data class ServiceState(
    val isRunning: Boolean,
    val lastUpdate: java.time.Instant?,
    val locationCount: Int
)

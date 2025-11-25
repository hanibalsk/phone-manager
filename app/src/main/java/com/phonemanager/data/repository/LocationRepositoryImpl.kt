package com.phonemanager.data.repository

import com.phonemanager.data.database.LocationDao
import com.phonemanager.data.model.HealthStatus
import com.phonemanager.data.model.LocationEntity
import com.phonemanager.data.model.ServiceHealth
import com.phonemanager.data.preferences.PreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Epic 0.2.3/Story 1.4: LocationRepositoryImpl - Location data repository implementation
 *
 * Story 1.4: Enhanced to persist service health state via PreferencesRepository
 * for reliable boot restoration.
 */
@Singleton
class LocationRepositoryImpl @Inject constructor(
    private val locationDao: LocationDao,
    private val preferencesRepository: PreferencesRepository
) : LocationRepository {

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Service health state - initialized from persisted storage
    private val _serviceHealth = MutableStateFlow(
        ServiceHealth(
            isRunning = false,
            healthStatus = HealthStatus.HEALTHY
        )
    )

    init {
        // Story 1.4: Restore persisted state on initialization
        repositoryScope.launch {
            combine(
                preferencesRepository.serviceRunningState,
                preferencesRepository.lastLocationUpdateTime
            ) { isRunning, lastUpdate ->
                Timber.d("Restoring service health from persistence: isRunning=$isRunning, lastUpdate=$lastUpdate")
                _serviceHealth.value = _serviceHealth.value.copy(
                    isRunning = isRunning,
                    lastLocationUpdate = lastUpdate
                )
            }.collect { /* Keep collecting to stay in sync */ }
        }
    }

    override fun observeLocationCount(): Flow<Int> {
        return locationDao.observeLocationCount()
    }

    override fun observeTodayLocationCount(): Flow<Int> {
        val startOfDay = LocalDate.now()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        return locationDao.observeTodayLocationCount(startOfDay)
    }

    override fun observeLastLocation(): Flow<LocationEntity?> {
        return locationDao.observeLastLocation()
    }

    override fun observeAverageAccuracy(): Flow<Float?> {
        return locationDao.observeAverageAccuracy()
    }

    override fun observeServiceHealth(): Flow<ServiceHealth> {
        return _serviceHealth.asStateFlow()
    }

    override fun observeAllLocations(): Flow<List<LocationEntity>> {
        return locationDao.observeAllLocations()
    }

    override suspend fun insertLocation(location: LocationEntity): Long {
        Timber.d("Inserting location: lat=${location.latitude}, lon=${location.longitude}")
        return locationDao.insert(location)
    }

    override suspend fun deleteLocationsBefore(beforeMillis: Long): Int {
        return locationDao.deleteLocationsBefore(beforeMillis)
    }

    override suspend fun deleteAllLocations() {
        locationDao.deleteAll()
    }

    /**
     * Story 1.4: Update service health - called by LocationTrackingService
     *
     * Persists the isRunning and lastLocationUpdate to DataStore for boot restoration.
     * In-memory state is also updated for UI reactivity.
     */
    fun updateServiceHealth(health: ServiceHealth) {
        _serviceHealth.value = health
        Timber.d("Service health updated: $health")

        // Story 1.4: Persist state for boot restoration
        repositoryScope.launch {
            try {
                preferencesRepository.setServiceRunningState(health.isRunning)
                if (health.isRunning && health.lastLocationUpdate != null) {
                    preferencesRepository.setLastLocationUpdateTime(health.lastLocationUpdate)
                } else if (!health.isRunning) {
                    // Clear stale timestamp when service stops
                    preferencesRepository.clearLastLocationUpdateTime()
                }
                Timber.d("Service health persisted successfully")
            } catch (e: Exception) {
                Timber.e(e, "Failed to persist service health")
            }
        }
    }

    /**
     * Get service health as a flow (one-shot)
     * Used by ServiceHealthCheckWorker and BootReceiver
     */
    override fun getServiceHealth(): Flow<ServiceHealth> {
        return _serviceHealth.asStateFlow()
    }

    /**
     * Get the latest location as a flow (one-shot)
     * Used by ServiceHealthCheckWorker to check for stale data
     */
    override fun getLatestLocation(): Flow<LocationEntity?> {
        return locationDao.observeLastLocation()
    }
}

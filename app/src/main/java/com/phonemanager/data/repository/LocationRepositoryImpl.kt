package com.phonemanager.data.repository

import com.phonemanager.data.database.LocationDao
import com.phonemanager.data.model.HealthStatus
import com.phonemanager.data.model.LocationEntity
import com.phonemanager.data.model.ServiceHealth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Epic 0.2.3: LocationRepositoryImpl - Stub implementation for Epic 1 development
 * Full implementation will be completed in Epic 0.2
 */
@Singleton
class LocationRepositoryImpl @Inject constructor(
    private val locationDao: LocationDao
) : LocationRepository {

    // Stub service health state
    private val _serviceHealth = MutableStateFlow(
        ServiceHealth(
            isRunning = false,
            healthStatus = HealthStatus.HEALTHY
        )
    )

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
     * Update service health - called by LocationTrackingService
     * Stub implementation for Epic 1
     */
    fun updateServiceHealth(health: ServiceHealth) {
        _serviceHealth.value = health
        Timber.d("Service health updated: $health")
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

package com.phonemanager.domain.model

import com.phonemanager.data.model.LocationEntity
import java.time.Duration
import java.time.Instant

/**
 * Story 1.3: Enhanced ServiceState - Complete service state with health status
 */
data class EnhancedServiceState(
    val isRunning: Boolean,
    val status: ServiceStatus,
    val lastUpdate: Instant?,
    val locationCount: Int,
    val currentInterval: Duration,
    val healthStatus: com.phonemanager.data.model.HealthStatus,
    val errorMessage: String? = null,
)

enum class ServiceStatus {
    STOPPED,
    STARTING,
    RUNNING,
    GPS_ACQUIRING,
    STOPPING,
    ERROR,
}

/**
 * Story 1.3: LocationStats - Statistics about collected locations
 */
data class LocationStats(
    val totalCount: Int,
    val todayCount: Int,
    val lastLocation: LocationEntity?,
    val averageAccuracy: Float?,
    val trackingInterval: Duration,
)

package com.phonemanager.data.repository

import com.phonemanager.data.model.LocationEntity
import com.phonemanager.data.model.ServiceHealth
import kotlinx.coroutines.flow.Flow

/**
 * Epic 0.2.3: LocationRepository - Repository interface for location data
 * Provides abstraction over data sources (database, service)
 */
interface LocationRepository {

    /**
     * Observe location count in real-time
     */
    fun observeLocationCount(): Flow<Int>

    /**
     * Observe today's location count
     */
    fun observeTodayLocationCount(): Flow<Int>

    /**
     * Observe the last captured location
     */
    fun observeLastLocation(): Flow<LocationEntity?>

    /**
     * Observe average accuracy of captured locations
     */
    fun observeAverageAccuracy(): Flow<Float?>

    /**
     * Observe service health status
     */
    fun observeServiceHealth(): Flow<ServiceHealth>

    /**
     * Observe all locations
     */
    fun observeAllLocations(): Flow<List<LocationEntity>>

    /**
     * Insert a new location
     */
    suspend fun insertLocation(location: LocationEntity): Long

    /**
     * Delete locations before timestamp
     */
    suspend fun deleteLocationsBefore(beforeMillis: Long): Int

    /**
     * Delete all locations
     */
    suspend fun deleteAllLocations()
}

package three.two.bit.phonemanager.data.repository

import kotlinx.coroutines.flow.Flow
import three.two.bit.phonemanager.data.model.LocationEntity
import three.two.bit.phonemanager.data.model.ServiceHealth

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

    /**
     * Get service health as a flow (one-shot)
     * Used by ServiceHealthCheckWorker and BootReceiver
     */
    fun getServiceHealth(): Flow<ServiceHealth>

    /**
     * Get the latest location as a flow (one-shot)
     * Used by ServiceHealthCheckWorker to check for stale data
     */
    fun getLatestLocation(): Flow<LocationEntity?>

    /**
     * Story E8.10: Get locations within time range for trip detail display
     * @param startTimeMillis Start timestamp in milliseconds
     * @param endTimeMillis End timestamp in milliseconds
     * @return List of locations ordered chronologically
     */
    suspend fun getLocationsBetween(startTimeMillis: Long, endTimeMillis: Long): List<LocationEntity>
}

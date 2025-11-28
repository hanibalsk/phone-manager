package three.two.bit.phonemanager.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import three.two.bit.phonemanager.data.model.LocationEntity

/**
 * Story 0.2.3/E4.1: LocationDao - Database access object for locations
 *
 * Story E4.1: Add date range query for history viewing
 */
@Dao
interface LocationDao {

    @Insert
    suspend fun insert(location: LocationEntity): Long

    @Query("SELECT * FROM locations WHERE id = :id")
    suspend fun getById(id: Long): LocationEntity?

    @Query("SELECT * FROM locations ORDER BY timestamp DESC LIMIT 1")
    fun observeLastLocation(): Flow<LocationEntity?>

    @Query("SELECT COUNT(*) FROM locations")
    fun observeLocationCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM locations WHERE timestamp >= :startOfDayMillis")
    fun observeTodayLocationCount(startOfDayMillis: Long): Flow<Int>

    @Query("SELECT AVG(accuracy) FROM locations")
    fun observeAverageAccuracy(): Flow<Float?>

    @Query("SELECT * FROM locations ORDER BY timestamp DESC")
    fun observeAllLocations(): Flow<List<LocationEntity>>

    @Query("DELETE FROM locations WHERE timestamp < :beforeMillis")
    suspend fun deleteLocationsBefore(beforeMillis: Long): Int

    @Query("DELETE FROM locations")
    suspend fun deleteAll()

    /**
     * Story E4.1: Get locations within date range for history display (AC E4.1.3, E4.1.5)
     * @param startTime Start timestamp in milliseconds
     * @param endTime End timestamp in milliseconds
     * @return List of locations ordered chronologically
     */
    @Query(
        """
        SELECT * FROM locations
        WHERE timestamp >= :startTime AND timestamp <= :endTime
        ORDER BY timestamp ASC
    """,
    )
    suspend fun getLocationsBetween(startTime: Long, endTime: Long): List<LocationEntity>

    /**
     * Story E4.2: Mark location as synced after successful upload (AC E4.2.4)
     * @param id Location ID
     * @param syncedAt Timestamp when synced
     */
    @Query("UPDATE locations SET isSynced = 1, syncedAt = :syncedAt WHERE id = :id")
    suspend fun markAsSynced(id: Long, syncedAt: Long)

    /**
     * Story E4.2: Get count of unsynced locations (AC E4.2.4)
     */
    @Query("SELECT COUNT(*) FROM locations WHERE isSynced = 0")
    fun observeUnsyncedCount(): Flow<Int>
}

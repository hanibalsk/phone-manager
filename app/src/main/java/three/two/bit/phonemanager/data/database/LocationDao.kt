package three.two.bit.phonemanager.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import three.two.bit.phonemanager.data.model.LocationEntity

/**
 * Story 0.2.3: LocationDao - Database access object for locations
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
}

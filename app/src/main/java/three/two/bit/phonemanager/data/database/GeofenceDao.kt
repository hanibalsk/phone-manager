package three.two.bit.phonemanager.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import three.two.bit.phonemanager.data.model.GeofenceEntity

/**
 * Story E6.1: GeofenceDao - Database access for geofences
 *
 * AC E6.1.5: CRUD operations for geofence management
 */
@Dao
interface GeofenceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(geofence: GeofenceEntity)

    @Update
    suspend fun update(geofence: GeofenceEntity)

    @Delete
    suspend fun delete(geofence: GeofenceEntity)

    @Query("SELECT * FROM geofences WHERE id = :id")
    suspend fun getById(id: String): GeofenceEntity?

    @Query("SELECT * FROM geofences WHERE deviceId = :deviceId ORDER BY createdAt DESC")
    fun observeGeofencesByDevice(deviceId: String): Flow<List<GeofenceEntity>>

    @Query("SELECT * FROM geofences WHERE deviceId = :deviceId AND active = 1")
    fun observeActiveGeofences(deviceId: String): Flow<List<GeofenceEntity>>

    @Query("SELECT * FROM geofences WHERE deviceId = :deviceId")
    suspend fun getAllByDevice(deviceId: String): List<GeofenceEntity>

    @Query("DELETE FROM geofences WHERE deviceId = :deviceId")
    suspend fun deleteAllByDevice(deviceId: String)

    @Query("DELETE FROM geofences")
    suspend fun deleteAll()
}

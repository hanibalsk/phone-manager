package three.two.bit.phonemanager.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import three.two.bit.phonemanager.data.model.GeofenceEventEntity

/**
 * Story E6.2: GeofenceEventDao - Data access object for geofence events
 *
 * AC E6.2.5: Event logging for history/debugging
 */
@Dao
interface GeofenceEventDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: GeofenceEventEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: List<GeofenceEventEntity>)

    @Update
    suspend fun update(event: GeofenceEventEntity)

    @Delete
    suspend fun delete(event: GeofenceEventEntity)

    @Query("SELECT * FROM geofence_events WHERE id = :eventId")
    suspend fun getById(eventId: String): GeofenceEventEntity?

    @Query("SELECT * FROM geofence_events WHERE deviceId = :deviceId ORDER BY timestamp DESC")
    fun observeEventsByDevice(deviceId: String): Flow<List<GeofenceEventEntity>>

    @Query("SELECT * FROM geofence_events WHERE geofenceId = :geofenceId ORDER BY timestamp DESC")
    fun observeEventsByGeofence(geofenceId: String): Flow<List<GeofenceEventEntity>>

    @Query("SELECT * FROM geofence_events WHERE deviceId = :deviceId ORDER BY timestamp DESC")
    suspend fun getAllByDevice(deviceId: String): List<GeofenceEventEntity>

    @Query("SELECT * FROM geofence_events WHERE geofenceId = :geofenceId ORDER BY timestamp DESC")
    suspend fun getAllByGeofence(geofenceId: String): List<GeofenceEventEntity>

    @Query("SELECT * FROM geofence_events WHERE deviceId = :deviceId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentByDevice(deviceId: String, limit: Int): List<GeofenceEventEntity>

    @Query("SELECT * FROM geofence_events WHERE webhookDelivered = 0 ORDER BY timestamp ASC")
    suspend fun getPendingWebhookDelivery(): List<GeofenceEventEntity>

    @Query("DELETE FROM geofence_events WHERE deviceId = :deviceId")
    suspend fun deleteAllByDevice(deviceId: String)

    @Query("DELETE FROM geofence_events WHERE geofenceId = :geofenceId")
    suspend fun deleteAllByGeofence(geofenceId: String)

    @Query("DELETE FROM geofence_events")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM geofence_events WHERE deviceId = :deviceId")
    suspend fun countByDevice(deviceId: String): Int
}

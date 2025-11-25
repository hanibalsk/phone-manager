package three.two.bit.phonemanager.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import three.two.bit.phonemanager.data.model.ProximityAlertEntity

/**
 * Story E5.1: ProximityAlertDao - Database access for proximity alerts
 *
 * AC E5.1.5: CRUD operations for alert management
 */
@Dao
interface ProximityAlertDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(alert: ProximityAlertEntity)

    @Update
    suspend fun update(alert: ProximityAlertEntity)

    @Delete
    suspend fun delete(alert: ProximityAlertEntity)

    @Query("SELECT * FROM proximity_alerts WHERE id = :id")
    suspend fun getById(id: String): ProximityAlertEntity?

    @Query("SELECT * FROM proximity_alerts WHERE ownerDeviceId = :ownerDeviceId ORDER BY createdAt DESC")
    fun observeAlertsByOwner(ownerDeviceId: String): Flow<List<ProximityAlertEntity>>

    @Query("SELECT * FROM proximity_alerts WHERE ownerDeviceId = :ownerDeviceId AND active = 1")
    fun observeActiveAlerts(ownerDeviceId: String): Flow<List<ProximityAlertEntity>>

    @Query("SELECT * FROM proximity_alerts WHERE ownerDeviceId = :ownerDeviceId")
    suspend fun getAllByOwner(ownerDeviceId: String): List<ProximityAlertEntity>

    @Query("DELETE FROM proximity_alerts WHERE ownerDeviceId = :ownerDeviceId")
    suspend fun deleteAllByOwner(ownerDeviceId: String)

    @Query("DELETE FROM proximity_alerts")
    suspend fun deleteAll()
}

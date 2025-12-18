package three.two.bit.phonemanager.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import three.two.bit.phonemanager.data.model.PendingDeviceLinkEntity

/**
 * Story UGM-1.4: DAO for pending device link operations
 *
 * Manages the offline queue for device link retries.
 */
@Dao
interface PendingDeviceLinkDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PendingDeviceLinkEntity)

    @Query("SELECT * FROM pending_device_links LIMIT 1")
    suspend fun getNext(): PendingDeviceLinkEntity?

    @Query("SELECT * FROM pending_device_links")
    suspend fun getAll(): List<PendingDeviceLinkEntity>

    @Query("DELETE FROM pending_device_links WHERE deviceId = :deviceId")
    suspend fun delete(deviceId: String)

    @Query("DELETE FROM pending_device_links")
    suspend fun deleteAll()

    @Query("UPDATE pending_device_links SET retryCount = retryCount + 1 WHERE deviceId = :deviceId")
    suspend fun incrementRetryCount(deviceId: String)

    @Query("SELECT COUNT(*) FROM pending_device_links")
    suspend fun count(): Int
}

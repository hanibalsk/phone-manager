package three.two.bit.phonemanager.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import three.two.bit.phonemanager.data.model.WebhookEntity

/**
 * Story E6.3: WebhookDao - Data Access Object for webhooks table
 *
 * AC E6.3.5: CRUD operations for webhook management
 */
@Dao
interface WebhookDao {

    /**
     * Insert a new webhook (or replace existing)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(webhook: WebhookEntity)

    /**
     * Insert multiple webhooks (for sync)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(webhooks: List<WebhookEntity>)

    /**
     * Update an existing webhook
     */
    @Update
    suspend fun update(webhook: WebhookEntity)

    /**
     * Delete a webhook
     */
    @Delete
    suspend fun delete(webhook: WebhookEntity)

    /**
     * Delete webhook by ID
     */
    @Query("DELETE FROM webhooks WHERE id = :webhookId")
    suspend fun deleteById(webhookId: String)

    /**
     * Get webhook by ID
     */
    @Query("SELECT * FROM webhooks WHERE id = :webhookId")
    suspend fun getById(webhookId: String): WebhookEntity?

    /**
     * Observe all webhooks for a device
     */
    @Query("SELECT * FROM webhooks WHERE ownerDeviceId = :deviceId ORDER BY name ASC")
    fun observeByDevice(deviceId: String): Flow<List<WebhookEntity>>

    /**
     * Get all webhooks for a device (non-Flow)
     */
    @Query("SELECT * FROM webhooks WHERE ownerDeviceId = :deviceId ORDER BY name ASC")
    suspend fun getAllByDevice(deviceId: String): List<WebhookEntity>

    /**
     * Get all enabled webhooks for a device
     */
    @Query("SELECT * FROM webhooks WHERE ownerDeviceId = :deviceId AND enabled = 1 ORDER BY name ASC")
    suspend fun getEnabledByDevice(deviceId: String): List<WebhookEntity>

    /**
     * Get all webhooks (for sync purposes)
     */
    @Query("SELECT * FROM webhooks")
    suspend fun getAll(): List<WebhookEntity>

    /**
     * Delete all webhooks for a device
     */
    @Query("DELETE FROM webhooks WHERE ownerDeviceId = :deviceId")
    suspend fun deleteAllByDevice(deviceId: String)

    /**
     * Delete all webhooks
     */
    @Query("DELETE FROM webhooks")
    suspend fun deleteAll()
}

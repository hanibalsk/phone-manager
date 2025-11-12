package com.phonemanager.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.phonemanager.data.model.LocationQueueEntity
import com.phonemanager.data.model.QueueStatus
import kotlinx.coroutines.flow.Flow

/**
 * Story 0.2.3: LocationQueueDao - Data access for location upload queue
 */
@Dao
interface LocationQueueDao {

    /**
     * Insert queue item (replace if exists)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(queueItem: LocationQueueEntity): Long

    /**
     * Update queue item
     */
    @Update
    suspend fun update(queueItem: LocationQueueEntity)

    /**
     * Get all pending items (including retry pending)
     */
    @Query("""
        SELECT * FROM location_queue
        WHERE status IN ('PENDING', 'RETRY_PENDING')
        AND (nextRetryTime IS NULL OR nextRetryTime <= :currentTime)
        ORDER BY queuedAt ASC
        LIMIT :limit
    """)
    suspend fun getPendingItems(currentTime: Long, limit: Int = 50): List<LocationQueueEntity>

    /**
     * Get item by location ID
     */
    @Query("SELECT * FROM location_queue WHERE locationId = :locationId")
    suspend fun getByLocationId(locationId: Long): LocationQueueEntity?

    /**
     * Observe pending count
     */
    @Query("SELECT COUNT(*) FROM location_queue WHERE status IN ('PENDING', 'RETRY_PENDING')")
    fun observePendingCount(): Flow<Int>

    /**
     * Observe failed count
     */
    @Query("SELECT COUNT(*) FROM location_queue WHERE status = 'FAILED'")
    fun observeFailedCount(): Flow<Int>

    /**
     * Delete uploaded items older than timestamp
     */
    @Query("DELETE FROM location_queue WHERE status = 'UPLOADED' AND queuedAt < :beforeTimestamp")
    suspend fun deleteUploadedBefore(beforeTimestamp: Long): Int

    /**
     * Delete all items with status
     */
    @Query("DELETE FROM location_queue WHERE status = :status")
    suspend fun deleteByStatus(status: QueueStatus): Int

    /**
     * Get failed items for manual retry
     */
    @Query("SELECT * FROM location_queue WHERE status = 'FAILED' ORDER BY queuedAt DESC LIMIT :limit")
    suspend fun getFailedItems(limit: Int = 100): List<LocationQueueEntity>

    /**
     * Reset failed items to retry
     */
    @Query("UPDATE location_queue SET status = 'RETRY_PENDING', retryCount = 0, nextRetryTime = :retryTime WHERE status = 'FAILED'")
    suspend fun resetFailedItems(retryTime: Long): Int

    /**
     * Get queue statistics
     */
    @Transaction
    @Query("""
        SELECT
            COUNT(CASE WHEN status = 'PENDING' THEN 1 END) as pending,
            COUNT(CASE WHEN status = 'UPLOADING' THEN 1 END) as uploading,
            COUNT(CASE WHEN status = 'UPLOADED' THEN 1 END) as uploaded,
            COUNT(CASE WHEN status = 'FAILED' THEN 1 END) as failed,
            COUNT(CASE WHEN status = 'RETRY_PENDING' THEN 1 END) as retryPending
        FROM location_queue
    """)
    suspend fun getQueueStats(): QueueStats
}

/**
 * Queue statistics data class
 */
data class QueueStats(
    val pending: Int,
    val uploading: Int,
    val uploaded: Int,
    val failed: Int,
    val retryPending: Int
) {
    val total: Int get() = pending + uploading + uploaded + failed + retryPending
    val needsUpload: Int get() = pending + retryPending
}

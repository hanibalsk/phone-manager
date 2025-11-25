package com.phonemanager.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Story 0.2.3: LocationQueueEntity - Queue item for location upload tracking
 *
 * Separate from LocationEntity to track upload status and retry attempts
 */
@Entity(tableName = "location_queue")
data class LocationQueueEntity(
    @PrimaryKey
    val locationId: Long, // FK to LocationEntity.id
    val status: QueueStatus,
    val retryCount: Int = 0,
    val lastAttemptTime: Long? = null,
    val nextRetryTime: Long? = null,
    val errorMessage: String? = null,
    val queuedAt: Long = System.currentTimeMillis(),
)

/**
 * Queue item status
 */
enum class QueueStatus {
    PENDING, // Waiting to be uploaded
    UPLOADING, // Currently being uploaded
    UPLOADED, // Successfully uploaded
    FAILED, // Failed after max retries
    RETRY_PENDING, // Failed but will retry
}

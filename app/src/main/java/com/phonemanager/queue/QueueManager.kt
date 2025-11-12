package com.phonemanager.queue

import com.phonemanager.data.database.LocationDao
import com.phonemanager.data.database.LocationQueueDao
import com.phonemanager.data.model.LocationQueueEntity
import com.phonemanager.data.model.QueueStatus
import com.phonemanager.network.NetworkManager
import kotlinx.coroutines.flow.Flow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min
import kotlin.math.pow

/**
 * Story 0.2.3: QueueManager - Manages location upload queue and retry logic
 *
 * Responsibilities:
 * - Add locations to upload queue
 * - Process queue with retry logic
 * - Exponential backoff for failed uploads
 * - Track upload status
 */
@Singleton
class QueueManager @Inject constructor(
    private val locationDao: LocationDao,
    private val locationQueueDao: LocationQueueDao,
    private val networkManager: NetworkManager
) {

    companion object {
        private const val MAX_RETRIES = 5
        private const val INITIAL_BACKOFF_MS = 1000L // 1 second
        private const val MAX_BACKOFF_MS = 300_000L // 5 minutes
        private const val BATCH_SIZE = 50
    }

    /**
     * Add location to upload queue
     */
    suspend fun enqueueLocation(locationId: Long) {
        val queueItem = LocationQueueEntity(
            locationId = locationId,
            status = QueueStatus.PENDING
        )

        locationQueueDao.insert(queueItem)
        Timber.d("Location $locationId enqueued for upload")
    }

    /**
     * Process pending items in the queue
     *
     * @return Number of items successfully uploaded
     */
    suspend fun processQueue(): Int {
        if (!networkManager.isNetworkAvailable()) {
            Timber.d("Network unavailable, skipping queue processing")
            return 0
        }

        val currentTime = System.currentTimeMillis()
        val pendingItems = locationQueueDao.getPendingItems(currentTime, BATCH_SIZE)

        if (pendingItems.isEmpty()) {
            Timber.d("No pending items to upload")
            return 0
        }

        Timber.i("Processing ${pendingItems.size} queued items")
        var successCount = 0

        for (queueItem in pendingItems) {
            val success = processQueueItem(queueItem)
            if (success) {
                successCount++
            }
        }

        Timber.i("Queue processing complete: $successCount/$${pendingItems.size} succeeded")
        return successCount
    }

    /**
     * Process single queue item
     */
    private suspend fun processQueueItem(queueItem: LocationQueueEntity): Boolean {
        try {
            // Mark as uploading
            locationQueueDao.update(
                queueItem.copy(
                    status = QueueStatus.UPLOADING,
                    lastAttemptTime = System.currentTimeMillis()
                )
            )

            // Get location entity
            val location = locationDao.getById(queueItem.locationId)
            if (location == null) {
                Timber.w("Location ${queueItem.locationId} not found, marking queue item as failed")
                locationQueueDao.update(
                    queueItem.copy(
                        status = QueueStatus.FAILED,
                        errorMessage = "Location not found in database"
                    )
                )
                return false
            }

            // Upload to backend
            val result = networkManager.uploadLocation(location)

            result.onSuccess { response ->
                Timber.i("Location ${queueItem.locationId} uploaded successfully")
                locationQueueDao.update(
                    queueItem.copy(
                        status = QueueStatus.UPLOADED,
                        lastAttemptTime = System.currentTimeMillis()
                    )
                )
                return true
            }.onFailure { error ->
                Timber.e(error, "Failed to upload location ${queueItem.locationId}")
                handleUploadFailure(queueItem, error.message)
                return false
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception processing queue item ${queueItem.locationId}")
            handleUploadFailure(queueItem, e.message)
        }

        return false
    }

    /**
     * Handle upload failure with exponential backoff retry logic
     */
    private suspend fun handleUploadFailure(queueItem: LocationQueueEntity, errorMessage: String?) {
        val newRetryCount = queueItem.retryCount + 1

        if (newRetryCount >= MAX_RETRIES) {
            // Max retries exceeded, mark as failed
            Timber.w("Location ${queueItem.locationId} failed after $MAX_RETRIES attempts")
            locationQueueDao.update(
                queueItem.copy(
                    status = QueueStatus.FAILED,
                    retryCount = newRetryCount,
                    lastAttemptTime = System.currentTimeMillis(),
                    errorMessage = errorMessage ?: "Upload failed after max retries"
                )
            )
        } else {
            // Calculate next retry time with exponential backoff + jitter
            val backoffMs = calculateBackoff(newRetryCount)
            val nextRetryTime = System.currentTimeMillis() + backoffMs

            Timber.d("Scheduling retry #$newRetryCount for location ${queueItem.locationId} in ${backoffMs}ms")

            locationQueueDao.update(
                queueItem.copy(
                    status = QueueStatus.RETRY_PENDING,
                    retryCount = newRetryCount,
                    lastAttemptTime = System.currentTimeMillis(),
                    nextRetryTime = nextRetryTime,
                    errorMessage = errorMessage
                )
            )
        }
    }

    /**
     * Calculate exponential backoff with jitter
     *
     * Formula: min(INITIAL_BACKOFF * 2^retryCount + jitter, MAX_BACKOFF)
     */
    private fun calculateBackoff(retryCount: Int): Long {
        val exponentialBackoff = INITIAL_BACKOFF_MS * (2.0.pow(retryCount.toDouble())).toLong()
        val jitter = (Math.random() * INITIAL_BACKOFF_MS).toLong()
        val backoff = exponentialBackoff + jitter

        return min(backoff, MAX_BACKOFF_MS)
    }

    /**
     * Retry all failed items
     */
    suspend fun retryFailedItems(): Int {
        val retryTime = System.currentTimeMillis()
        return locationQueueDao.resetFailedItems(retryTime)
    }

    /**
     * Clean up old uploaded items (keep last 7 days)
     */
    suspend fun cleanupOldItems(): Int {
        val weekAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
        return locationQueueDao.deleteUploadedBefore(weekAgo)
    }

    /**
     * Observe pending count
     */
    fun observePendingCount(): Flow<Int> {
        return locationQueueDao.observePendingCount()
    }

    /**
     * Observe failed count
     */
    fun observeFailedCount(): Flow<Int> {
        return locationQueueDao.observeFailedCount()
    }

    /**
     * Get queue statistics
     */
    suspend fun getQueueStats() = locationQueueDao.getQueueStats()
}

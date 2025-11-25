package three.two.bit.phonemanager.queue

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import three.two.bit.phonemanager.data.database.LocationDao
import three.two.bit.phonemanager.data.database.LocationQueueDao
import three.two.bit.phonemanager.data.model.LocationQueueEntity
import three.two.bit.phonemanager.data.model.QueueStatus
import three.two.bit.phonemanager.network.NetworkManager
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min
import kotlin.math.pow
import kotlin.random.Random

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
    private val networkManager: NetworkManager,
) {

    companion object {
        private const val MAX_RETRIES = 5
        private const val INITIAL_BACKOFF_MS = 1000L // 1 second
        private const val MAX_BACKOFF_MS = 300_000L // 5 minutes
        private const val BATCH_SIZE = 50
    }

    // Mutex to prevent concurrent queue processing
    private val processingMutex = Mutex()
    private val isProcessing = AtomicBoolean(false)

    /**
     * Add location to upload queue
     */
    suspend fun enqueueLocation(locationId: Long) {
        val queueItem = LocationQueueEntity(
            locationId = locationId,
            status = QueueStatus.PENDING,
        )

        locationQueueDao.insert(queueItem)
        Timber.d("Location $locationId enqueued for upload")
    }

    /**
     * Process pending items in the queue
     * Uses mutex to prevent concurrent processing which could cause race conditions
     *
     * @return Number of items successfully uploaded
     */
    suspend fun processQueue(): Int {
        // Quick check to avoid mutex contention if already processing
        if (isProcessing.get()) {
            Timber.d("Queue processing already in progress, skipping")
            return 0
        }

        return processingMutex.withLock {
            // Double-check after acquiring lock
            if (isProcessing.getAndSet(true)) {
                Timber.d("Queue processing already in progress (after lock), skipping")
                return@withLock 0
            }

            try {
                processQueueInternal()
            } finally {
                isProcessing.set(false)
            }
        }
    }

    /**
     * Internal queue processing logic - called within mutex lock
     */
    private suspend fun processQueueInternal(): Int {
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

        Timber.i("Queue processing complete: $successCount/${pendingItems.size} succeeded")
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
                    lastAttemptTime = System.currentTimeMillis(),
                ),
            )

            // Get location entity
            val location = locationDao.getById(queueItem.locationId)
            if (location == null) {
                Timber.w("Location ${queueItem.locationId} not found, marking queue item as failed")
                locationQueueDao.update(
                    queueItem.copy(
                        status = QueueStatus.FAILED,
                        errorMessage = "Location not found in database",
                    ),
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
                        lastAttemptTime = System.currentTimeMillis(),
                    ),
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
                    errorMessage = errorMessage ?: "Upload failed after max retries",
                ),
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
                    errorMessage = errorMessage,
                ),
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
        val jitter = (Random.nextDouble() * INITIAL_BACKOFF_MS).toLong()
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
    fun observePendingCount(): Flow<Int> = locationQueueDao.observePendingCount()

    /**
     * Observe failed count
     */
    fun observeFailedCount(): Flow<Int> = locationQueueDao.observeFailedCount()

    /**
     * Get queue statistics
     */
    suspend fun getQueueStats() = locationQueueDao.getQueueStats()
}

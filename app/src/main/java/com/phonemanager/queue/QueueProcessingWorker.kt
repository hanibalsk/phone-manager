package com.phonemanager.queue

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

/**
 * Story 0.2.3: QueueProcessingWorker - Background worker for processing upload queue
 *
 * Triggered periodically by WorkManager to process pending location uploads
 */
@HiltWorker
class QueueProcessingWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val queueManager: QueueManager
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Timber.d("QueueProcessingWorker started")

        return try {
            // Process the queue
            val successCount = queueManager.processQueue()

            // Clean up old uploaded items
            val cleanedCount = queueManager.cleanupOldItems()

            Timber.i("QueueProcessingWorker completed: uploaded=$successCount, cleaned=$cleanedCount")

            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "QueueProcessingWorker failed")

            // Retry on failure
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    companion object {
        const val WORK_NAME = "queue_processing_work"
    }
}

package com.phonemanager.queue

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Story 0.2.3: WorkManagerScheduler - Schedules background queue processing
 *
 * Schedules periodic work to process the upload queue
 */
@Singleton
class WorkManagerScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val workManager = WorkManager.getInstance(context)

    /**
     * Schedule periodic queue processing
     *
     * @param intervalMinutes How often to process the queue (default: 15 minutes)
     */
    fun scheduleQueueProcessing(intervalMinutes: Long = 15) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val queueWork = PeriodicWorkRequestBuilder<QueueProcessingWorker>(
            intervalMinutes, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniquePeriodicWork(
            QueueProcessingWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            queueWork
        )

        Timber.i("Queue processing scheduled every $intervalMinutes minutes")
    }

    /**
     * Cancel queue processing
     */
    fun cancelQueueProcessing() {
        workManager.cancelUniqueWork(QueueProcessingWorker.WORK_NAME)
        Timber.d("Queue processing cancelled")
    }

    /**
     * Check if queue processing is scheduled
     */
    fun isQueueProcessingScheduled(): Boolean {
        val workInfo = workManager.getWorkInfosForUniqueWork(QueueProcessingWorker.WORK_NAME)
        return try {
            val info = workInfo.get()
            info.isNotEmpty() && !info[0].state.isFinished
        } catch (e: Exception) {
            false
        }
    }
}

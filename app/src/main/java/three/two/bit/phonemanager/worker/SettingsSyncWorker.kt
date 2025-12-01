package three.two.bit.phonemanager.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import three.two.bit.phonemanager.data.repository.SettingsSyncRepository
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Story E12.6: Worker for periodic settings synchronization.
 *
 * AC E12.6.2: Setting sync on app start and periodically
 * - Sync settings from server every 15 minutes (when network available)
 * - Update lock states and managed device status
 * - Handle offline gracefully with cached states
 */
@HiltWorker
class SettingsSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val settingsSyncRepository: SettingsSyncRepository,
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val WORK_NAME = "settings_sync_work"
        private const val SYNC_INTERVAL_MINUTES = 15L

        /**
         * Schedule periodic settings sync.
         * Call this on app start and after login.
         */
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<SettingsSyncWorker>(
                SYNC_INTERVAL_MINUTES,
                TimeUnit.MINUTES,
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    1,
                    TimeUnit.MINUTES,
                )
                .addTag(WORK_NAME)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    workRequest,
                )

            Timber.i("Scheduled periodic settings sync every $SYNC_INTERVAL_MINUTES minutes")
        }

        /**
         * Cancel periodic settings sync.
         * Call this on logout.
         */
        fun cancel(context: Context) {
            WorkManager.getInstance(context)
                .cancelUniqueWork(WORK_NAME)
            Timber.i("Cancelled periodic settings sync")
        }
    }

    override suspend fun doWork(): Result {
        Timber.d("SettingsSyncWorker starting")

        return try {
            val result = settingsSyncRepository.syncAllSettings()

            result.fold(
                onSuccess = {
                    Timber.i("SettingsSyncWorker completed successfully")
                    Result.success()
                },
                onFailure = { error ->
                    Timber.w(error, "SettingsSyncWorker failed, will retry")
                    Result.retry()
                },
            )
        } catch (e: Exception) {
            Timber.e(e, "SettingsSyncWorker exception")
            Result.retry()
        }
    }
}

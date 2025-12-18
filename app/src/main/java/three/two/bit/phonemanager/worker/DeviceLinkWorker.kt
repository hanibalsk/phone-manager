package three.two.bit.phonemanager.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import three.two.bit.phonemanager.receiver.DeviceLinkRetryReceiver
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import three.two.bit.phonemanager.R
import three.two.bit.phonemanager.data.database.PendingDeviceLinkDao
import three.two.bit.phonemanager.data.model.PendingDeviceLinkEntity
import three.two.bit.phonemanager.network.DeviceApiService
import three.two.bit.phonemanager.security.SecureStorage
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Story UGM-1.4: Worker for retrying device link operations when online.
 *
 * AC 2: Auto-retry when online
 * AC 3: Update state on success
 * AC 4: Handle retry exhaustion (NFR-R2)
 *
 * - Retry with exponential backoff (30s, 60s, 120s)
 * - After 3 failed retries, show notification
 * - Clear queue on success
 */
@HiltWorker
class DeviceLinkWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val pendingDeviceLinkDao: PendingDeviceLinkDao,
    private val deviceApiService: DeviceApiService,
    private val secureStorage: SecureStorage,
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val WORK_NAME = "device_link_work"
        private const val MAX_RETRIES = 3
        private const val NOTIFICATION_CHANNEL_ID = "device_link_channel"
        private const val NOTIFICATION_ID = 1002

        /**
         * Schedule one-time device link retry.
         * Called when a pending link operation is queued.
         */
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<DeviceLinkWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    30,
                    TimeUnit.SECONDS,
                )
                .addTag(WORK_NAME)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    WORK_NAME,
                    ExistingWorkPolicy.KEEP,
                    workRequest,
                )

            Timber.i("Scheduled device link retry work")
        }

        /**
         * Cancel pending device link work.
         */
        fun cancel(context: Context) {
            WorkManager.getInstance(context)
                .cancelUniqueWork(WORK_NAME)
            Timber.i("Cancelled device link work")
        }
    }

    override suspend fun doWork(): Result {
        Timber.d("DeviceLinkWorker starting")

        val pendingLink = pendingDeviceLinkDao.getNext() ?: run {
            Timber.d("No pending device links in queue")
            return Result.success()
        }

        // Check if we've exceeded max retries (AC 4: NFR-R2)
        if (pendingLink.retryCount >= MAX_RETRIES) {
            Timber.w("Device link exceeded max retries: ${pendingLink.deviceId}")
            showRetryExhaustedNotification()
            pendingDeviceLinkDao.delete(pendingLink.deviceId)
            return Result.success() // Don't retry further
        }

        val accessToken = secureStorage.getAccessToken()
        if (accessToken == null) {
            Timber.w("No access token available for device link retry")
            return Result.retry()
        }

        return try {
            val result = deviceApiService.linkDevice(
                userId = pendingLink.userId,
                deviceId = pendingLink.deviceId,
                displayName = null,
                isPrimary = false,
                accessToken = accessToken,
            )

            result.fold(
                onSuccess = {
                    Timber.i("Device linked successfully via worker: ${pendingLink.deviceId}")
                    // AC 3: Update state on success
                    secureStorage.saveDeviceLinkTimestamp()
                    pendingDeviceLinkDao.delete(pendingLink.deviceId)
                    Result.success()
                },
                onFailure = { exception ->
                    val message = exception.message ?: ""
                    when {
                        message.contains("409") || message.contains("conflict", ignoreCase = true) -> {
                            // Device already linked - treat as success and remove from queue
                            Timber.i("Device already linked: ${pendingLink.deviceId}")
                            pendingDeviceLinkDao.delete(pendingLink.deviceId)
                            Result.success()
                        }
                        else -> {
                            Timber.w(exception, "Device link failed, will retry")
                            pendingDeviceLinkDao.incrementRetryCount(pendingLink.deviceId)
                            Result.retry()
                        }
                    }
                },
            )
        } catch (e: Exception) {
            Timber.e(e, "DeviceLinkWorker exception")
            pendingDeviceLinkDao.incrementRetryCount(pendingLink.deviceId)
            Result.retry()
        }
    }

    /**
     * AC 4: Show notification when retry exhaustion occurs
     * - Notification provides manual retry action
     */
    private fun showRetryExhaustedNotification() {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel for Android O+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                context.getString(R.string.device_link_notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = context.getString(R.string.device_link_notification_channel_description)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Create retry action PendingIntent
        val retryIntent = Intent(context, DeviceLinkRetryReceiver::class.java).apply {
            action = DeviceLinkRetryReceiver.ACTION_RETRY_DEVICE_LINK
        }
        val retryPendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            retryIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_foreground_service)
            .setContentTitle(context.getString(R.string.device_link_retry_failed_title))
            .setContentText(context.getString(R.string.device_link_retry_failed_message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .addAction(
                R.drawable.ic_foreground_service,
                context.getString(R.string.device_link_retry_button),
                retryPendingIntent,
            )
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}

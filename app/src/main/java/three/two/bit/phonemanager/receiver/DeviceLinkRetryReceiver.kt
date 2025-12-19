package three.two.bit.phonemanager.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import three.two.bit.phonemanager.data.database.PendingDeviceLinkDao
import three.two.bit.phonemanager.data.model.PendingDeviceLinkEntity
import three.two.bit.phonemanager.security.SecureStorage
import three.two.bit.phonemanager.worker.DeviceLinkWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Story UGM-1.4: BroadcastReceiver for manual device link retry from notification.
 *
 * AC 4: Handle retry exhaustion (NFR-R2)
 * - Notification provides manual retry action
 *
 * This receiver is triggered when the user taps the "Retry" action
 * on the retry-exhausted notification. It re-queues the device link
 * operation and schedules the worker.
 */
@AndroidEntryPoint
class DeviceLinkRetryReceiver : BroadcastReceiver() {

    @Inject
    lateinit var pendingDeviceLinkDao: PendingDeviceLinkDao

    @Inject
    lateinit var secureStorage: SecureStorage

    companion object {
        const val ACTION_RETRY_DEVICE_LINK = "three.two.bit.phonemanager.action.RETRY_DEVICE_LINK"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_RETRY_DEVICE_LINK) {
            return
        }

        Timber.i("DeviceLinkRetryReceiver: Manual retry requested")

        val pendingResult = goAsync()

        // Create a scoped coroutine that will be cancelled when work is done
        val scope = CoroutineScope(Dispatchers.IO)

        scope.launch {
            try {
                // Get device ID and user ID from secure storage
                val deviceId = secureStorage.getDeviceId()
                val userId = secureStorage.getUserId()

                if (userId != null) {
                    // Re-queue the device link with reset retry count
                    val pendingLink = PendingDeviceLinkEntity(
                        deviceId = deviceId,
                        userId = userId,
                        timestamp = System.currentTimeMillis(),
                        retryCount = 0,
                    )
                    pendingDeviceLinkDao.insert(pendingLink)
                    Timber.i("Re-queued device link for retry: $deviceId")

                    // Schedule the worker
                    DeviceLinkWorker.schedule(context)
                } else {
                    Timber.w("Cannot retry device link: missing userId")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error in DeviceLinkRetryReceiver")
            } finally {
                pendingResult.finish()
                scope.cancel() // Clean up the scope to prevent memory leaks
            }
        }
    }
}

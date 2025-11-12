package com.phonemanager.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.phonemanager.data.repository.LocationRepository
import com.phonemanager.service.ServiceController
import com.phonemanager.watchdog.WatchdogManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Story 0.2.4: BootReceiver - Auto-starts tracking service on device boot
 *
 * Listens for BOOT_COMPLETED and QUICKBOOT_POWERON intents to restore
 * the location tracking service if it was running before reboot.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var locationRepository: LocationRepository

    @Inject
    lateinit var serviceController: ServiceController

    @Inject
    lateinit var watchdogManager: WatchdogManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            Timber.i("Boot completed, checking if tracking should be restored")

            // Use goAsync to allow coroutine work
            val pendingResult = goAsync()

            scope.launch {
                try {
                    // Check if service was running before reboot
                    val serviceHealth = locationRepository.getServiceHealth().first()

                    if (serviceHealth.isRunning) {
                        Timber.i("Restoring location tracking service after boot")

                        // Start tracking service
                        val result = serviceController.startTracking()

                        if (result.isSuccess) {
                            Timber.i("Location tracking service restored successfully")

                            // Schedule health check watchdog
                            watchdogManager.startWatchdog()
                        } else {
                            Timber.e("Failed to restore location tracking service: ${result.exceptionOrNull()}")
                        }
                    } else {
                        Timber.d("Location tracking was not running before reboot, not starting")
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Error in BootReceiver")
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}

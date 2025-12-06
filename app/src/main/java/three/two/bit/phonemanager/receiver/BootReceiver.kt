package three.two.bit.phonemanager.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import three.two.bit.phonemanager.data.repository.LocationRepository
import three.two.bit.phonemanager.service.LocationServiceController
import three.two.bit.phonemanager.watchdog.WatchdogManager
import timber.log.Timber
import javax.inject.Inject

/**
 * Story 0.2.4/1.4: BootReceiver - Auto-starts tracking service on device boot
 *
 * Listens for BOOT_COMPLETED and QUICKBOOT_POWERON intents to restore
 * the location tracking service if it was running before reboot.
 *
 * Story 1.4: Now reads from persisted state via LocationRepository which
 * initializes from DataStore, ensuring accurate restoration after reboot.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var locationRepository: LocationRepository

    @Inject
    lateinit var serviceController: LocationServiceController

    @Inject
    lateinit var watchdogManager: WatchdogManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onReceive(context: Context, intent: Intent) {
        // Skip processing during instrumented tests to avoid Hilt initialization issues
        // HiltTestApplication doesn't initialize the component until tests start
        if (isRunningInTestEnvironment(context)) {
            Timber.d("BootReceiver: Skipping in test environment")
            return
        }

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

    /**
     * Detects if the app is running in an instrumented test environment.
     * This checks if the application class is a test application (HiltTestApplication)
     * which indicates we're running under androidTest.
     */
    private fun isRunningInTestEnvironment(context: Context): Boolean {
        return try {
            // Check if the application class name contains "Test" (HiltTestApplication)
            val appClassName = context.applicationContext.javaClass.name
            val isTestApp = appClassName.contains("Test", ignoreCase = true) ||
                    appClassName.contains("Hilt_", ignoreCase = true)

            // Also check for test runner in the call stack
            val isTestRunner = Thread.currentThread().stackTrace.any { element ->
                element.className.contains("androidx.test") ||
                element.className.contains("junit") ||
                element.className.contains("InstrumentationRegistry")
            }

            isTestApp || isTestRunner
        } catch (e: Exception) {
            Timber.w(e, "Failed to detect test environment, assuming production")
            false
        }
    }
}

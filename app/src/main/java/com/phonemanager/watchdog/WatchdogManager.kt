package com.phonemanager.watchdog

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Story 0.2.4: WatchdogManager - Schedules service health checks
 *
 * Monitors the location tracking service health and restarts it if needed.
 * Uses WorkManager to schedule periodic health checks.
 */
@Singleton
class WatchdogManager @Inject constructor(@ApplicationContext private val context: Context) {

    private val workManager = WorkManager.getInstance(context)

    companion object {
        private const val WATCHDOG_WORK_NAME = "service_health_watchdog"
        private const val DEFAULT_CHECK_INTERVAL_MINUTES = 15L
    }

    /**
     * Start the service health watchdog
     *
     * @param intervalMinutes How often to check service health (default: 15 minutes)
     */
    fun startWatchdog(intervalMinutes: Long = DEFAULT_CHECK_INTERVAL_MINUTES) {
        val constraints = Constraints.Builder()
            .build()

        val watchdogWork = PeriodicWorkRequestBuilder<ServiceHealthCheckWorker>(
            intervalMinutes,
            TimeUnit.MINUTES,
        )
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniquePeriodicWork(
            WATCHDOG_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            watchdogWork,
        )

        Timber.i("Service health watchdog started (check interval: $intervalMinutes minutes)")
    }

    /**
     * Stop the service health watchdog
     */
    fun stopWatchdog() {
        workManager.cancelUniqueWork(WATCHDOG_WORK_NAME)
        Timber.d("Service health watchdog stopped")
    }

    /**
     * Check if watchdog is running
     */
    fun isWatchdogRunning(): Boolean {
        val workInfo = workManager.getWorkInfosForUniqueWork(WATCHDOG_WORK_NAME)
        return try {
            val info = workInfo.get()
            info.isNotEmpty() && !info[0].state.isFinished
        } catch (e: Exception) {
            false
        }
    }
}

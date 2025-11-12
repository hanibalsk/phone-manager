package com.phonemanager.watchdog

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.phonemanager.data.repository.LocationRepository
import com.phonemanager.service.ServiceController
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Story 0.2.4: ServiceHealthCheckWorker - Monitors service health
 *
 * Periodically checks if the location tracking service is healthy.
 * If the service should be running but isn't, or hasn't captured
 * a location recently, attempts to restart it.
 */
@HiltWorker
class ServiceHealthCheckWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val locationRepository: LocationRepository,
    private val serviceController: ServiceController
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "service_health_check"
        private const val STALE_THRESHOLD_MINUTES = 30L
    }

    override suspend fun doWork(): Result {
        return try {
            Timber.d("Performing service health check")

            val serviceHealth = locationRepository.getServiceHealth().first()

            // Check if service should be running but isn't
            if (serviceHealth.isRunning) {
                // Check if last location capture was too long ago
                val lastLocation = locationRepository.getLatestLocation().first()
                val now = System.currentTimeMillis()

                val isStale = if (lastLocation != null) {
                    val minutesSinceLastCapture = TimeUnit.MILLISECONDS.toMinutes(
                        now - lastLocation.timestamp
                    )
                    minutesSinceLastCapture > STALE_THRESHOLD_MINUTES
                } else {
                    // No locations captured yet - might be a problem
                    true
                }

                if (isStale) {
                    Timber.w("Service appears stale (no location for $STALE_THRESHOLD_MINUTES+ minutes), attempting restart")

                    // Attempt to restart the service
                    val stopResult = serviceController.stopTracking()
                    if (stopResult.isSuccess) {
                        val startResult = serviceController.startTracking()

                        if (startResult.isSuccess) {
                            Timber.i("Service successfully restarted by watchdog")
                        } else {
                            Timber.e("Failed to restart service: ${startResult.exceptionOrNull()}")
                        }
                    } else {
                        Timber.e("Failed to stop service: ${stopResult.exceptionOrNull()}")
                    }
                } else {
                    Timber.d("Service health check passed - service is healthy")
                }
            } else {
                Timber.d("Service is not supposed to be running, skipping health check")
            }

            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "Error during service health check")
            Result.failure()
        }
    }
}

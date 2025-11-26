package three.two.bit.phonemanager.geofence

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import dagger.hilt.android.qualifiers.ApplicationContext
import three.two.bit.phonemanager.domain.model.TransitionType
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import three.two.bit.phonemanager.domain.model.Geofence as DomainGeofence

/**
 * Story E6.1: GeofenceManager - Wraps Android GeofencingClient
 *
 * AC E6.1.3: Android Geofencing API Registration
 * Uses GeofencingClient.addGeofences() for geofence registration
 */
interface GeofenceManager {
    /**
     * Register a geofence with the Android Geofencing API
     */
    suspend fun addGeofence(geofence: DomainGeofence): Result<Unit>

    /**
     * Register multiple geofences at once
     */
    suspend fun addGeofences(geofences: List<DomainGeofence>): Result<Unit>

    /**
     * Remove a geofence by ID
     */
    suspend fun removeGeofence(geofenceId: String): Result<Unit>

    /**
     * Remove multiple geofences by ID
     */
    suspend fun removeGeofences(geofenceIds: List<String>): Result<Unit>

    /**
     * Remove all registered geofences
     */
    suspend fun removeAllGeofences(): Result<Unit>

    /**
     * Check if background location permission is granted
     */
    fun hasRequiredPermissions(): Boolean
}

@Singleton
class GeofenceManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : GeofenceManager {

    private val geofencingClient: GeofencingClient = LocationServices.getGeofencingClient(context)

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        PendingIntent.getBroadcast(context, 0, intent, flags)
    }

    override fun hasRequiredPermissions(): Boolean {
        val hasFineLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED

        val hasBackgroundLocation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        return hasFineLocation && hasBackgroundLocation
    }

    override suspend fun addGeofence(geofence: DomainGeofence): Result<Unit> =
        addGeofences(listOf(geofence))

    override suspend fun addGeofences(geofences: List<DomainGeofence>): Result<Unit> {
        if (!hasRequiredPermissions()) {
            return Result.failure(SecurityException("Missing required location permissions for geofencing"))
        }

        if (geofences.isEmpty()) {
            return Result.success(Unit)
        }

        val androidGeofences = geofences.map { it.toAndroidGeofence() }

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER or GeofencingRequest.INITIAL_TRIGGER_DWELL)
            .addGeofences(androidGeofences)
            .build()

        return suspendCoroutine { continuation ->
            try {
                geofencingClient.addGeofences(request, geofencePendingIntent)
                    .addOnSuccessListener {
                        Timber.i("Successfully added ${geofences.size} geofence(s)")
                        continuation.resume(Result.success(Unit))
                    }
                    .addOnFailureListener { e ->
                        Timber.e(e, "Failed to add geofences")
                        continuation.resume(Result.failure(e))
                    }
            } catch (e: SecurityException) {
                Timber.e(e, "SecurityException when adding geofences")
                continuation.resume(Result.failure(e))
            }
        }
    }

    override suspend fun removeGeofence(geofenceId: String): Result<Unit> =
        removeGeofences(listOf(geofenceId))

    override suspend fun removeGeofences(geofenceIds: List<String>): Result<Unit> {
        if (geofenceIds.isEmpty()) {
            return Result.success(Unit)
        }

        return suspendCoroutine { continuation ->
            geofencingClient.removeGeofences(geofenceIds)
                .addOnSuccessListener {
                    Timber.i("Successfully removed ${geofenceIds.size} geofence(s)")
                    continuation.resume(Result.success(Unit))
                }
                .addOnFailureListener { e ->
                    Timber.e(e, "Failed to remove geofences")
                    continuation.resume(Result.failure(e))
                }
        }
    }

    override suspend fun removeAllGeofences(): Result<Unit> = suspendCoroutine { continuation ->
        geofencingClient.removeGeofences(geofencePendingIntent)
            .addOnSuccessListener {
                Timber.i("Successfully removed all geofences")
                continuation.resume(Result.success(Unit))
            }
            .addOnFailureListener { e ->
                Timber.e(e, "Failed to remove all geofences")
                continuation.resume(Result.failure(e))
            }
    }

    /**
     * Convert domain Geofence to Android Geofence
     */
    private fun DomainGeofence.toAndroidGeofence(): Geofence {
        val transitionTypes = mapTransitionTypes(this.transitionTypes)

        return Geofence.Builder()
            .setRequestId(this.id)
            .setCircularRegion(
                this.latitude,
                this.longitude,
                this.radiusMeters.toFloat(),
            )
            .setTransitionTypes(transitionTypes)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setLoiteringDelay(DWELL_LOITERING_DELAY_MS) // For DWELL transitions
            .build()
    }

    /**
     * Map domain TransitionTypes to Android Geofence transition flags
     */
    private fun mapTransitionTypes(types: Set<TransitionType>): Int {
        var flags = 0
        if (TransitionType.ENTER in types) {
            flags = flags or Geofence.GEOFENCE_TRANSITION_ENTER
        }
        if (TransitionType.EXIT in types) {
            flags = flags or Geofence.GEOFENCE_TRANSITION_EXIT
        }
        if (TransitionType.DWELL in types) {
            flags = flags or Geofence.GEOFENCE_TRANSITION_DWELL
        }
        return flags
    }

    companion object {
        // Loitering delay for dwell transitions (5 minutes)
        private const val DWELL_LOITERING_DELAY_MS = 5 * 60 * 1000
    }
}

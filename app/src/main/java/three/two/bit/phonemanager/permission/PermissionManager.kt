package three.two.bit.phonemanager.permission

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Story 1.2: PermissionManager - Handles all permission checking and state management
 *
 * Movement detection additions:
 * - Activity Recognition permission (Android Q+)
 * - Bluetooth Connect permission (Android S+)
 */
interface PermissionManager {
    fun hasLocationPermission(): Boolean
    fun hasBackgroundLocationPermission(): Boolean
    fun hasNotificationPermission(): Boolean
    fun hasAllRequiredPermissions(): Boolean
    fun shouldShowLocationRationale(activity: Activity): Boolean
    fun shouldShowBackgroundRationale(activity: Activity): Boolean
    fun observePermissionState(): Flow<PermissionState>
    fun updatePermissionState()

    // Movement detection permissions (optional - only needed if feature is enabled)
    fun hasActivityRecognitionPermission(): Boolean
    fun hasBluetoothConnectPermission(): Boolean
    fun hasAllMovementDetectionPermissions(): Boolean
    fun shouldShowActivityRecognitionRationale(activity: Activity): Boolean
    fun shouldShowBluetoothRationale(activity: Activity): Boolean
}

@Singleton
class PermissionManagerImpl @Inject constructor(@param:ApplicationContext private val context: Context) : PermissionManager {

    private val permissionStateFlow = MutableStateFlow<PermissionState>(PermissionState.Checking)

    override fun hasLocationPermission(): Boolean = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED

    override fun hasBackgroundLocationPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return true // Not required on Android 9 and below
        }
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun hasNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true // Not required on Android 12 and below
        }
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun hasAllRequiredPermissions(): Boolean = hasLocationPermission() &&
        hasBackgroundLocationPermission() &&
        hasNotificationPermission()

    override fun shouldShowLocationRationale(activity: Activity): Boolean =
        ActivityCompat.shouldShowRequestPermissionRationale(
            activity,
            Manifest.permission.ACCESS_FINE_LOCATION,
        )

    override fun shouldShowBackgroundRationale(activity: Activity): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return false
        }
        return ActivityCompat.shouldShowRequestPermissionRationale(
            activity,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION,
        )
    }

    override fun observePermissionState(): Flow<PermissionState> = permissionStateFlow.asStateFlow()

    override fun updatePermissionState() {
        val newState = when {
            !hasLocationPermission() -> PermissionState.LocationDenied
            !hasBackgroundLocationPermission() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                PermissionState.BackgroundDenied(foregroundGranted = true)
            }

            !hasNotificationPermission() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                PermissionState.NotificationDenied
            }

            else -> PermissionState.AllGranted
        }

        permissionStateFlow.value = newState
        Timber.d("Permission state updated: $newState")
    }

    // Movement detection permissions implementation

    override fun hasActivityRecognitionPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return true // Not required on Android 9 and below
        }
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACTIVITY_RECOGNITION,
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun hasBluetoothConnectPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return true // Not required on Android 11 and below
        }
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_CONNECT,
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun hasAllMovementDetectionPermissions(): Boolean =
        hasActivityRecognitionPermission() && hasBluetoothConnectPermission()

    override fun shouldShowActivityRecognitionRationale(activity: Activity): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return false
        }
        return ActivityCompat.shouldShowRequestPermissionRationale(
            activity,
            Manifest.permission.ACTIVITY_RECOGNITION,
        )
    }

    override fun shouldShowBluetoothRationale(activity: Activity): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return false
        }
        return ActivityCompat.shouldShowRequestPermissionRationale(
            activity,
            Manifest.permission.BLUETOOTH_CONNECT,
        )
    }
}

/**
 * Story 1.2: PermissionState - Sealed class representing permission states
 */
sealed class PermissionState {
    object Checking : PermissionState()
    object AllGranted : PermissionState()
    object LocationDenied : PermissionState()
    data class BackgroundDenied(val foregroundGranted: Boolean) : PermissionState()
    object NotificationDenied : PermissionState()
    data class PermanentlyDenied(val permission: String) : PermissionState()
}

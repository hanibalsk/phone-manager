package com.phonemanager.ui.permissions

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phonemanager.analytics.Analytics
import com.phonemanager.permission.PermissionManager
import com.phonemanager.permission.PermissionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Story 1.2: PermissionViewModel - Manages permission flow and state
 * Story 1.2, AC 1.2.12: Includes analytics tracking for permission events
 */
@HiltViewModel
class PermissionViewModel @Inject constructor(
    private val permissionManager: PermissionManager,
    private val analytics: Analytics,
) : ViewModel() {

    private val _permissionState = MutableStateFlow<PermissionState>(PermissionState.Checking)
    val permissionState: StateFlow<PermissionState> = _permissionState.asStateFlow()

    private val _showLocationRationale = MutableStateFlow(false)
    val showLocationRationale: StateFlow<Boolean> = _showLocationRationale.asStateFlow()

    private val _showBackgroundRationale = MutableStateFlow(false)
    val showBackgroundRationale: StateFlow<Boolean> = _showBackgroundRationale.asStateFlow()

    private val _showNotificationRationale = MutableStateFlow(false)
    val showNotificationRationale: StateFlow<Boolean> = _showNotificationRationale.asStateFlow()

    private val _showSettingsDialog = MutableStateFlow(false)
    val showSettingsDialog: StateFlow<Boolean> = _showSettingsDialog.asStateFlow()

    init {
        viewModelScope.launch {
            permissionManager.observePermissionState().collect { state ->
                _permissionState.value = state
            }
        }
        checkPermissions()
    }

    fun checkPermissions() {
        permissionManager.updatePermissionState()
        Timber.d("Permissions checked")
    }

    fun requestLocationPermission(activity: Activity) {
        Timber.d("Requesting location permission")
        // Always show rationale for first-time users
        _showLocationRationale.value = true
        analytics.logPermissionRationaleShown("location")
    }

    fun onLocationRationaleAccepted() {
        Timber.d("Location rationale accepted")
        _showLocationRationale.value = false
        // Trigger system permission dialog (handled by Activity)
    }

    fun onLocationRationaleDismissed() {
        Timber.d("Location rationale dismissed")
        _showLocationRationale.value = false
        analytics.logPermissionDenied("location", "rationale_dismissed")
    }

    fun onLocationPermissionResult(granted: Boolean, shouldShowRationale: Boolean) {
        Timber.d("Location permission result: granted=$granted, shouldShowRationale=$shouldShowRationale")

        if (granted) {
            analytics.logPermissionGranted("location")
            permissionManager.updatePermissionState()
            // Check if background permission needed
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                _showBackgroundRationale.value = true
            }
        } else {
            if (!shouldShowRationale) {
                // Permanently denied
                analytics.logPermissionDenied("location", "permanently_denied")
                _permissionState.value = PermissionState.PermanentlyDenied(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                )
                _showSettingsDialog.value = true
            } else {
                analytics.logPermissionDenied("location", "user_denied")
                _permissionState.value = PermissionState.LocationDenied
            }
        }
    }

    fun onBackgroundRationaleAccepted() {
        Timber.d("Background rationale accepted")
        _showBackgroundRationale.value = false
        analytics.logPermissionRationaleShown("background")
        // Trigger system permission dialog (handled by Activity)
    }

    fun onBackgroundRationaleDismissed() {
        Timber.d("Background rationale dismissed")
        _showBackgroundRationale.value = false
        analytics.logPermissionDenied("background", "rationale_dismissed")
        // User can still use foreground-only tracking
        permissionManager.updatePermissionState()
    }

    fun onBackgroundPermissionResult(granted: Boolean, shouldShowRationale: Boolean) {
        Timber.d("Background permission result: granted=$granted, shouldShowRationale=$shouldShowRationale")

        permissionManager.updatePermissionState()

        if (granted) {
            analytics.logPermissionGranted("background")
            // Check if all permissions granted for flow completion
            checkPermissionFlowCompletion()
        } else {
            if (!shouldShowRationale) {
                analytics.logPermissionDenied("background", "permanently_denied")
                // Permanently denied - but foreground still granted
                _permissionState.value = PermissionState.BackgroundDenied(foregroundGranted = true)
            } else {
                analytics.logPermissionDenied("background", "user_denied")
            }
            checkPermissionFlowCompletion()
        }
    }

    fun requestNotificationPermission() {
        Timber.d("Requesting notification permission")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            _showNotificationRationale.value = true
            analytics.logPermissionRationaleShown("notification")
        }
    }

    fun onNotificationRationaleAccepted() {
        Timber.d("Notification rationale accepted")
        _showNotificationRationale.value = false
        // Trigger system permission dialog (handled by Activity)
    }

    fun onNotificationRationaleDismissed() {
        Timber.d("Notification rationale dismissed")
        _showNotificationRationale.value = false
        analytics.logPermissionDenied("notification", "rationale_dismissed")
    }

    fun onNotificationPermissionResult(granted: Boolean) {
        Timber.d("Notification permission result: granted=$granted")
        permissionManager.updatePermissionState()

        if (granted) {
            analytics.logPermissionGranted("notification")
        } else {
            analytics.logPermissionDenied("notification", "user_denied")
        }
        checkPermissionFlowCompletion()
    }

    fun dismissSettingsDialog() {
        _showSettingsDialog.value = false
    }

    fun openAppSettings(context: Context) {
        Timber.d("Opening app settings")
        analytics.logPermissionSettingsOpened()
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
        _showSettingsDialog.value = false
    }

    /**
     * Story 1.2, AC 1.2.12: Track permission flow completion
     */
    private fun checkPermissionFlowCompletion() {
        val allGranted = permissionManager.hasAllRequiredPermissions()
        analytics.logPermissionFlowCompleted(allGranted)
    }
}

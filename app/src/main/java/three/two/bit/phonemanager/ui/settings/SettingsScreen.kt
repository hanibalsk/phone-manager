@file:Suppress("DEPRECATION") // hiltViewModel() deprecation - using stable API

package three.two.bit.phonemanager.ui.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import three.two.bit.phonemanager.R
import three.two.bit.phonemanager.domain.model.DeviceSettings
import three.two.bit.phonemanager.domain.model.EnrollmentStatus
import three.two.bit.phonemanager.domain.model.SettingDefinition
import three.two.bit.phonemanager.domain.model.SettingsSyncStatus
import three.two.bit.phonemanager.ui.settings.components.DeviceLinkInfoCard
import three.two.bit.phonemanager.ui.unlock.RequestUnlockDialog
import kotlin.math.roundToInt

/**
 * Story E1.3: Settings Screen
 * Story E8.12: Trip Detection Settings
 * Story E12.6: Settings Lock & Sync
 *
 * Allows users to update display name, group ID, and trip detection settings
 * Shows lock indicators for admin-managed settings
 * ACs: E1.3.1, E1.3.2, E1.3.3, E1.3.4, E8.12.1-E8.12.8, E12.6.1-E12.6.8
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToTripHistory: () -> Unit = {},
    onNavigateToMovementEvents: () -> Unit = {},
    onNavigateToMovementDebug: () -> Unit = {},
    onNavigateToLogin: () -> Unit = {},
    onNavigateToGroups: () -> Unit = {},
    onNavigateToMyDevices: () -> Unit = {},
    // Story E12.8: Navigate to unlock requests screen
    onNavigateToUnlockRequests: (deviceId: String) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val showWeatherInNotification by viewModel.showWeatherInNotification.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Story E9.11: Authentication state (AC E9.11.6, E9.11.8)
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    var showLogoutDialog by remember { mutableStateOf(false) }

    // Story UGM-1.3: Device link info for ownership display
    val deviceLinkInfo by viewModel.deviceLinkInfo.collectAsStateWithLifecycle()

    // Movement detection states
    val isMovementDetectionEnabled by viewModel.isMovementDetectionEnabled.collectAsStateWithLifecycle()
    val isActivityRecognitionEnabled by viewModel.isActivityRecognitionEnabled.collectAsStateWithLifecycle()
    val isBluetoothCarDetectionEnabled by viewModel.isBluetoothCarDetectionEnabled.collectAsStateWithLifecycle()
    val isAndroidAutoDetectionEnabled by viewModel.isAndroidAutoDetectionEnabled.collectAsStateWithLifecycle()
    val vehicleIntervalMultiplier by viewModel.vehicleIntervalMultiplier.collectAsStateWithLifecycle()
    val defaultIntervalMultiplier by viewModel.defaultIntervalMultiplier.collectAsStateWithLifecycle()
    val movementPermissionState by viewModel.movementPermissionState.collectAsStateWithLifecycle()

    // Story E8.12: Trip detection states
    val isTripDetectionEnabled by viewModel.isTripDetectionEnabled.collectAsStateWithLifecycle()
    val tripStationaryThreshold by viewModel.tripStationaryThreshold.collectAsStateWithLifecycle()
    val tripMinimumDuration by viewModel.tripMinimumDuration.collectAsStateWithLifecycle()
    val tripMinimumDistance by viewModel.tripMinimumDistance.collectAsStateWithLifecycle()
    val isTripAutoMergeEnabled by viewModel.isTripAutoMergeEnabled.collectAsStateWithLifecycle()

    // Permission dialog state
    var showPermissionDialog by remember { mutableStateOf(false) }

    // Story E12.6: Settings sync and lock state (AC E12.6.1-E12.6.8)
    val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()
    val serverSettings by viewModel.serverSettings.collectAsStateWithLifecycle()
    val managedStatus by viewModel.managedStatus.collectAsStateWithLifecycle()
    val lockDialogState by viewModel.lockDialogState.collectAsStateWithLifecycle()

    // Story E13.10: Enterprise enrollment state (AC E13.10.8)
    val enrollmentStatus by viewModel.enrollmentStatus.collectAsStateWithLifecycle()
    val organizationInfo by viewModel.organizationInfo.collectAsStateWithLifecycle()
    val enrollmentDevicePolicy by viewModel.enrollmentDevicePolicy.collectAsStateWithLifecycle()
    val isEnrollmentLoading by viewModel.isEnrollmentLoading.collectAsStateWithLifecycle()
    val showUnenrollDialog by viewModel.showUnenrollDialog.collectAsStateWithLifecycle()

    // Story E12.8: Unlock request dialog state (AC E12.8.1, E12.8.2)
    var showUnlockRequestDialog by remember { mutableStateOf(false) }
    var unlockRequestSettingKey by remember { mutableStateOf<String?>(null) }
    var unlockRequestSettingName by remember { mutableStateOf<String?>(null) }
    var unlockRequestReason by remember { mutableStateOf("") }
    var isSubmittingUnlockRequest by remember { mutableStateOf(false) }

    // Build list of permissions to request
    val permissionsToRequest = remember {
        buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(Manifest.permission.ACTIVITY_RECOGNITION)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(Manifest.permission.BLUETOOTH_CONNECT)
            }
        }.toTypedArray()
    }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        viewModel.updateMovementPermissionState()
        // If all permissions granted, enable movement detection
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            viewModel.enableMovementDetectionAfterPermission()
        }
    }

    // Show success message with delay before navigation (AC E1.3.2, E1.3.3)
    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            snackbarHostState.showSnackbar("Settings saved successfully")
            kotlinx.coroutines.delay(500) // Allow user to see success message
            onNavigateBack()
        }
    }

    // Show error message (AC E1.3.2, E1.3.3)
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier.testTag("settings_screen"),
    ) { paddingValues ->
        Column(
            modifier =
            Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Loading state
            if (uiState.isLoading) {
                CircularProgressIndicator()
            }

            // Story E12.6: Offline indicator (AC E12.6.8)
            when (syncStatus) {
                SettingsSyncStatus.OFFLINE -> OfflineBanner()
                SettingsSyncStatus.NOT_AUTHENTICATED -> NotAuthenticatedBanner()
                else -> { /* No banner needed */ }
            }

            // Story E12.6: Managed device status card (AC E12.6.6)
            if (managedStatus.isManaged) {
                ManagedStatusCard(
                    groupName = managedStatus.groupName,
                    lockedCount = managedStatus.lockedSettingsCount,
                    lastSyncTime = managedStatus.lastSyncedAt?.toString()?.take(10),
                    isSyncing = syncStatus == SettingsSyncStatus.SYNCING,
                    onSyncClick = viewModel::syncSettings,
                )
            }

            // Story E13.10: Enterprise enrollment status card (AC E13.10.8)
            if (enrollmentStatus == EnrollmentStatus.ENROLLED && organizationInfo != null) {
                EnrollmentStatusCard(
                    organizationInfo = organizationInfo!!,
                    lockedSettingsCount = enrollmentDevicePolicy?.lockedCount() ?: 0,
                    isUnenrolling = isEnrollmentLoading,
                    onUnenrollClick = viewModel::showUnenrollConfirmation,
                )
            }

            // Story E9.11: Authentication Section (AC E9.11.6, E9.11.8)
            currentUser?.let { user ->
                // Logged In State
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.settings_account),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    OutlinedTextField(
                        value = user.email,
                        onValueChange = {},
                        label = { Text(stringResource(R.string.auth_email)) },
                        enabled = false,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        supportingText = { Text(stringResource(R.string.auth_signed_in_as, user.displayName)) },
                    )

                    // Story UGM-1.3: Device ownership info card (AC 1, 2, 3, 4)
                    DeviceLinkInfoCard(
                        deviceLinkInfo = deviceLinkInfo,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        // My Devices navigation button
                        androidx.compose.material3.OutlinedCard(
                            onClick = onNavigateToMyDevices,
                            modifier = Modifier.weight(1f),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = stringResource(R.string.settings_my_devices),
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = stringResource(R.string.settings_navigate_to_devices),
                                )
                            }
                        }

                        // Groups navigation button
                        androidx.compose.material3.OutlinedCard(
                            onClick = onNavigateToGroups,
                            modifier = Modifier.weight(1f),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = stringResource(R.string.settings_groups),
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = stringResource(R.string.settings_navigate_to_groups),
                                )
                            }
                        }
                    }

                    Button(
                        onClick = { showLogoutDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("logout_button"),
                    ) {
                        Text(stringResource(R.string.auth_sign_out))
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            } ?: run {
                // Not Logged In State
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(R.string.settings_sign_in_prompt),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Button(
                        onClick = onNavigateToLogin,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.auth_sign_in))
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            }

            // Device ID Display (read-only for transparency)
            OutlinedTextField(
                value = viewModel.deviceId,
                onValueChange = {},
                label = { Text(stringResource(R.string.label_device_id)) },
                enabled = false,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                supportingText = { Text(stringResource(R.string.hint_device_id)) },
            )

            // Display Name TextField (AC E1.3.2)
            OutlinedTextField(
                value = uiState.displayName,
                onValueChange = viewModel::onDisplayNameChanged,
                label = { Text(stringResource(R.string.label_display_name)) },
                enabled = !uiState.isLoading,
                isError = uiState.displayNameError != null,
                supportingText = {
                    if (uiState.displayNameError != null) {
                        Text(uiState.displayNameError!!)
                    } else {
                        Text(stringResource(R.string.hint_display_name))
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            // Group ID TextField (AC E1.3.3)
            OutlinedTextField(
                value = uiState.groupId,
                onValueChange = viewModel::onGroupIdChanged,
                label = { Text(stringResource(R.string.label_group_id)) },
                enabled = !uiState.isLoading,
                isError = uiState.groupIdError != null,
                supportingText = {
                    if (uiState.groupIdError != null) {
                        Text(uiState.groupIdError!!)
                    } else {
                        Text(stringResource(R.string.hint_group_id_visibility))
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            // Helper text
            Text(
                text = stringResource(R.string.settings_changes_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // Divider between device settings and app settings
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Story E7.4: Weather Notification Toggle (AC E7.4.1)
            // Story E12.6: With lock support (AC E12.6.1)
            Text(
                text = stringResource(R.string.settings_notification_settings),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth(),
            )

            SettingToggleItem(
                title = stringResource(R.string.settings_weather_notification),
                summary = stringResource(R.string.settings_weather_notification_summary),
                checked = showWeatherInNotification,
                onCheckedChange = { viewModel.setShowWeatherInNotification(it) },
                isLocked = viewModel.isSettingLocked(DeviceSettings.KEY_SHOW_WEATHER_IN_NOTIFICATION),
                lockedBy = viewModel.getLockedBy(DeviceSettings.KEY_SHOW_WEATHER_IN_NOTIFICATION),
                onLockedClick = { viewModel.showLockedDialog(DeviceSettings.KEY_SHOW_WEATHER_IN_NOTIFICATION) },
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Story E3.3: Map Polling Interval Selector (AC E3.3.5)
            Text(
                text = stringResource(R.string.settings_map_settings),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth(),
            )

            PollingIntervalSelector(
                selectedInterval = uiState.mapPollingIntervalSeconds,
                onIntervalSelected = viewModel::onPollingIntervalChanged,
                enabled = !uiState.isLoading,
                modifier = Modifier.fillMaxWidth(),
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Movement Detection Settings Section
            Text(
                text = stringResource(R.string.settings_movement_detection),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth(),
            )

            // Master toggle for movement detection
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_movement_detection_enable),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = stringResource(R.string.settings_movement_detection_summary),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = isMovementDetectionEnabled,
                    onCheckedChange = { enabled ->
                        val needsPermissions = viewModel.setMovementDetectionEnabled(enabled)
                        if (needsPermissions) {
                            showPermissionDialog = true
                        }
                    },
                )
            }

            // Sub-settings (only visible when movement detection is enabled)
            if (isMovementDetectionEnabled) {
                // Activity Recognition toggle
                SettingsToggleRow(
                    title = stringResource(R.string.settings_movement_activity_recognition),
                    summary = stringResource(R.string.settings_movement_activity_recognition_summary),
                    checked = isActivityRecognitionEnabled,
                    onCheckedChange = viewModel::setActivityRecognitionEnabled,
                )

                // Bluetooth car detection toggle
                SettingsToggleRow(
                    title = stringResource(R.string.settings_movement_bluetooth_car),
                    summary = stringResource(R.string.settings_movement_bluetooth_car_summary),
                    checked = isBluetoothCarDetectionEnabled,
                    onCheckedChange = viewModel::setBluetoothCarDetectionEnabled,
                )

                // Android Auto detection toggle
                SettingsToggleRow(
                    title = stringResource(R.string.settings_movement_android_auto),
                    summary = stringResource(R.string.settings_movement_android_auto_summary),
                    checked = isAndroidAutoDetectionEnabled,
                    onCheckedChange = viewModel::setAndroidAutoDetectionEnabled,
                )

                // Vehicle interval multiplier slider
                IntervalMultiplierSlider(
                    title = stringResource(R.string.settings_movement_vehicle_multiplier),
                    summaryFormat = R.string.settings_movement_vehicle_multiplier_summary,
                    value = vehicleIntervalMultiplier,
                    onValueChange = viewModel::setVehicleIntervalMultiplier,
                    valueRange = 0.1f..1.0f,
                )

                // Default interval multiplier slider
                IntervalMultiplierSlider(
                    title = stringResource(R.string.settings_movement_default_multiplier),
                    summaryFormat = R.string.settings_movement_default_multiplier_summary,
                    value = defaultIntervalMultiplier,
                    onValueChange = viewModel::setDefaultIntervalMultiplier,
                    valueRange = 0.1f..2.0f,
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Story E8.12: Trip Detection Settings Section (AC E8.12.1)
            // Story E12.6: With lock support (AC E12.6.1)
            Text(
                text = stringResource(R.string.settings_trip_detection),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth(),
            )

            // Enable Trip Detection toggle (AC E8.12.2)
            SettingToggleItem(
                title = stringResource(R.string.settings_trip_detection_enable),
                summary = stringResource(R.string.settings_trip_detection_summary),
                checked = isTripDetectionEnabled,
                onCheckedChange = viewModel::setTripDetectionEnabled,
                isLocked = viewModel.isSettingLocked(DeviceSettings.KEY_TRIP_DETECTION_ENABLED),
                lockedBy = viewModel.getLockedBy(DeviceSettings.KEY_TRIP_DETECTION_ENABLED),
                onLockedClick = { viewModel.showLockedDialog(DeviceSettings.KEY_TRIP_DETECTION_ENABLED) },
            )

            // Trip detection sub-settings (only visible when enabled)
            if (isTripDetectionEnabled) {
                // Stationary threshold (AC E8.12.3)
                StationaryThresholdSelector(
                    selectedMinutes = tripStationaryThreshold,
                    onMinutesSelected = viewModel::setTripStationaryThreshold,
                )

                // Minimum trip duration (AC E8.12.4)
                SettingsStepperRow(
                    title = stringResource(R.string.settings_trip_minimum_duration),
                    value = tripMinimumDuration,
                    unit = stringResource(R.string.settings_trip_minutes),
                    minValue = 1,
                    maxValue = 10,
                    onValueChange = viewModel::setTripMinimumDuration,
                )

                // Minimum trip distance (AC E8.12.5)
                SettingsStepperRow(
                    title = stringResource(R.string.settings_trip_minimum_distance),
                    value = tripMinimumDistance,
                    unit = stringResource(R.string.settings_trip_meters),
                    minValue = 50,
                    maxValue = 500,
                    step = 50,
                    onValueChange = viewModel::setTripMinimumDistance,
                )

                // Auto-merge toggle (AC E8.12.6)
                SettingsToggleRow(
                    title = stringResource(R.string.settings_trip_auto_merge),
                    summary = stringResource(R.string.settings_trip_auto_merge_summary),
                    checked = isTripAutoMergeEnabled,
                    onCheckedChange = viewModel::setTripAutoMergeEnabled,
                )
            }

            // Navigation links (AC E8.12.7)
            SettingsNavigationRow(
                title = stringResource(R.string.settings_view_trip_history),
                onClick = onNavigateToTripHistory,
            )

            SettingsNavigationRow(
                title = stringResource(R.string.settings_view_movement_events),
                onClick = onNavigateToMovementEvents,
            )

            // Movement Debug screen for diagnostics
            SettingsNavigationRow(
                title = stringResource(R.string.settings_movement_debug),
                onClick = onNavigateToMovementDebug,
            )

            // Story E12.8: Navigation to unlock requests (AC E12.8.3)
            if (managedStatus.isManaged) {
                SettingsNavigationRow(
                    title = stringResource(R.string.settings_view_unlock_requests),
                    onClick = { onNavigateToUnlockRequests(viewModel.deviceId) },
                )
            }

            // Save Button (AC E1.3.2, E1.3.3)
            Button(
                onClick = viewModel::onSaveClicked,
                enabled = !uiState.isLoading && uiState.hasChanges && uiState.isFormValid,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.button_save_changes))
            }
        }
    }

    // Group Change Confirmation Dialog
    if (uiState.showGroupChangeConfirmation) {
        AlertDialog(
            onDismissRequest = viewModel::onDismissGroupChangeConfirmation,
            title = { Text(stringResource(R.string.dialog_change_group_title)) },
            text = {
                Text(
                    "Changing your Group ID will move you to a different group. " +
                        "You will no longer see devices from your current group, " +
                        "and will only see devices in the new group.",
                )
            },
            confirmButton = {
                Button(onClick = viewModel::onConfirmGroupChange) {
                    Text(stringResource(R.string.button_change_group))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onDismissGroupChangeConfirmation) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    // Movement Detection Permission Dialog
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text(stringResource(R.string.permission_movement_title)) },
            text = { Text(stringResource(R.string.permission_movement_rationale)) },
            confirmButton = {
                Button(
                    onClick = {
                        showPermissionDialog = false
                        if (permissionsToRequest.isNotEmpty()) {
                            permissionLauncher.launch(permissionsToRequest)
                        } else {
                            // No permissions needed (old Android version), enable directly
                            viewModel.enableMovementDetectionAfterPermission()
                        }
                    },
                ) {
                    Text(stringResource(R.string.permission_continue))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text(stringResource(R.string.permission_not_now))
                }
            },
        )
    }

    // Story E9.11: Logout Confirmation Dialog (AC E9.11.6)
    if (showLogoutDialog) {
        AlertDialog(
            modifier = Modifier.testTag("confirm_dialog"),
            onDismissRequest = { showLogoutDialog = false },
            title = { Text(stringResource(R.string.auth_sign_out)) },
            text = { Text(stringResource(R.string.auth_sign_out_confirm_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        viewModel.logout()
                    },
                    modifier = Modifier.testTag("confirm_button"),
                ) {
                    Text(stringResource(R.string.auth_sign_out))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showLogoutDialog = false },
                    modifier = Modifier.testTag("cancel_button"),
                ) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    // Story E12.6: Setting Locked Dialog (AC E12.6.3)
    lockDialogState?.let { state ->
        SettingLockedDialog(
            settingKey = state.settingKey,
            lockedBy = state.lockedBy,
            onDismiss = viewModel::dismissLockedDialog,
            onRequestUnlock = {
                // Story E12.8: Show unlock request dialog (AC E12.8.1)
                unlockRequestSettingKey = state.settingKey
                unlockRequestSettingName = SettingDefinition.forKey(state.settingKey)?.displayName
                    ?: state.settingKey.replace("_", " ").replaceFirstChar { it.uppercase() }
                unlockRequestReason = ""
                showUnlockRequestDialog = true
                viewModel.dismissLockedDialog()
            },
        )
    }

    // Story E12.8: Request Unlock Dialog (AC E12.8.1, E12.8.2)
    if (showUnlockRequestDialog && unlockRequestSettingKey != null) {
        RequestUnlockDialog(
            settingName = unlockRequestSettingName ?: "",
            reason = unlockRequestReason,
            onReasonChange = { newReason ->
                unlockRequestReason = newReason.take(200)
            },
            onSubmit = {
                isSubmittingUnlockRequest = true
                viewModel.submitUnlockRequest(
                    settingKey = unlockRequestSettingKey!!,
                    reason = unlockRequestReason,
                    onSuccess = {
                        isSubmittingUnlockRequest = false
                        showUnlockRequestDialog = false
                        unlockRequestSettingKey = null
                        unlockRequestSettingName = null
                        unlockRequestReason = ""
                    },
                    onError = {
                        isSubmittingUnlockRequest = false
                    },
                )
            },
            onDismiss = {
                if (!isSubmittingUnlockRequest) {
                    showUnlockRequestDialog = false
                    unlockRequestSettingKey = null
                    unlockRequestSettingName = null
                    unlockRequestReason = ""
                }
            },
            isSubmitting = isSubmittingUnlockRequest,
            isValid = unlockRequestReason.length in 5..200,
            errorMessage = when {
                unlockRequestReason.isEmpty() -> null
                unlockRequestReason.length < 5 -> "Reason must be at least 5 characters"
                unlockRequestReason.length > 200 -> "Reason cannot exceed 200 characters"
                else -> null
            },
            remainingCharacters = 200 - unlockRequestReason.length,
        )
    }

    // Story E13.10: Unenroll confirmation dialog (AC E13.10.9)
    if (showUnenrollDialog && organizationInfo != null) {
        UnenrollConfirmationDialog(
            organizationName = organizationInfo!!.name,
            onConfirm = {
                viewModel.unenrollDevice(
                    onSuccess = {
                        viewModel.dismissUnenrollDialog()
                    },
                    onError = { /* Error shown via snackbar */ },
                )
            },
            onDismiss = viewModel::dismissUnenrollDialog,
        )
    }
}

/**
 * Story E3.3: Polling Interval Selector (AC E3.3.5)
 *
 * Allows users to choose between 10, 15, 20, or 30 second polling intervals
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PollingIntervalSelector(
    selectedInterval: Int,
    onIntervalSelected: (Int) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    // Available polling interval options (AC E3.3.5)
    val intervalOptions = listOf(10, 15, 20, 30)

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = stringResource(R.string.hint_polling_interval, selectedInterval),
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text(stringResource(R.string.label_map_polling_interval)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            supportingText = { Text(stringResource(R.string.hint_map_polling)) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            intervalOptions.forEach { interval ->
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.hint_polling_interval, interval)) },
                    onClick = {
                        onIntervalSelected(interval)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}

/**
 * Reusable settings toggle row component
 */
@Composable
private fun SettingsToggleRow(
    title: String,
    summary: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

/**
 * Slider for interval multiplier settings
 */
@Composable
private fun IntervalMultiplierSlider(
    title: String,
    summaryFormat: Int,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 16.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = stringResource(summaryFormat, (value * 100).roundToInt()),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = ((valueRange.endInclusive - valueRange.start) * 10).roundToInt() - 1,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * Story E8.12: Stationary threshold selector with filter chips (AC E8.12.3)
 */
@Composable
private fun StationaryThresholdSelector(
    selectedMinutes: Int,
    onMinutesSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options = listOf(1, 5, 10, 30)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_trip_stationary_threshold),
            style = MaterialTheme.typography.bodyMedium,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.forEach { minutes ->
                FilterChip(
                    selected = selectedMinutes == minutes,
                    onClick = { onMinutesSelected(minutes) },
                    label = {
                        Text(
                            stringResource(R.string.settings_trip_threshold_minutes, minutes),
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                )
            }
        }
    }
}

/**
 * Story E8.12: Stepper row for numeric settings (AC E8.12.4, E8.12.5)
 */
@Composable
private fun SettingsStepperRow(
    title: String,
    value: Int,
    unit: String,
    minValue: Int,
    maxValue: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    step: Int = 1,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            IconButton(
                onClick = { onValueChange((value - step).coerceAtLeast(minValue)) },
                enabled = value > minValue,
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = stringResource(R.string.settings_trip_decrease),
                )
            }

            Text(
                text = "$value $unit",
                style = MaterialTheme.typography.bodyLarge,
            )

            IconButton(
                onClick = { onValueChange((value + step).coerceAtMost(maxValue)) },
                enabled = value < maxValue,
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.settings_trip_increase),
                )
            }
        }
    }
}

/**
 * Story E8.12: Navigation row for links to other screens (AC E8.12.7)
 */
@Composable
private fun SettingsNavigationRow(title: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}

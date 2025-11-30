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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import three.two.bit.phonemanager.R
import kotlin.math.roundToInt

/**
 * Story E1.3: Settings Screen
 * Story E8.12: Trip Detection Settings
 *
 * Allows users to update display name, group ID, and trip detection settings
 * ACs: E1.3.1, E1.3.2, E1.3.3, E1.3.4, E8.12.1-E8.12.8
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToTripHistory: () -> Unit = {},
    onNavigateToMovementEvents: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val showWeatherInNotification by viewModel.showWeatherInNotification.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

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
            Text(
                text = stringResource(R.string.settings_notification_settings),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_weather_notification),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = stringResource(R.string.settings_weather_notification_summary),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = showWeatherInNotification,
                    onCheckedChange = { viewModel.setShowWeatherInNotification(it) },
                )
            }

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
            Text(
                text = stringResource(R.string.settings_trip_detection),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth(),
            )

            // Enable Trip Detection toggle (AC E8.12.2)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_trip_detection_enable),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = stringResource(R.string.settings_trip_detection_summary),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = isTripDetectionEnabled,
                    onCheckedChange = viewModel::setTripDetectionEnabled,
                )
            }

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

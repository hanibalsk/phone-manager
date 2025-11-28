package three.two.bit.phonemanager.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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

/**
 * Story E1.3: Settings Screen
 *
 * Allows users to update display name and group ID
 * ACs: E1.3.1, E1.3.2, E1.3.3, E1.3.4
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel(), onNavigateBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val showWeatherInNotification by viewModel.showWeatherInNotification.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

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
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
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
                .padding(16.dp),
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
                label = { Text("Device ID") },
                enabled = false,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                supportingText = { Text("Unique identifier for this device") },
            )

            // Display Name TextField (AC E1.3.2)
            OutlinedTextField(
                value = uiState.displayName,
                onValueChange = viewModel::onDisplayNameChanged,
                label = { Text("Display Name") },
                enabled = !uiState.isLoading,
                isError = uiState.displayNameError != null,
                supportingText = {
                    if (uiState.displayNameError != null) {
                        Text(uiState.displayNameError!!)
                    } else {
                        Text("How your device appears to others")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            // Group ID TextField (AC E1.3.3)
            OutlinedTextField(
                value = uiState.groupId,
                onValueChange = viewModel::onGroupIdChanged,
                label = { Text("Group ID") },
                enabled = !uiState.isLoading,
                isError = uiState.groupIdError != null,
                supportingText = {
                    if (uiState.groupIdError != null) {
                        Text(uiState.groupIdError!!)
                    } else {
                        Text("Devices with the same Group ID can see each other")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            // Helper text
            Text(
                text = "Changes will take effect after saving",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // Divider between device settings and app settings
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Story E7.4: Weather Notification Toggle (AC E7.4.1)
            Text(
                text = "Notification Settings",
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
                text = "Map Settings",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth(),
            )

            PollingIntervalSelector(
                selectedInterval = uiState.mapPollingIntervalSeconds,
                onIntervalSelected = viewModel::onPollingIntervalChanged,
                enabled = !uiState.isLoading,
                modifier = Modifier.fillMaxWidth(),
            )

            // Save Button (AC E1.3.2, E1.3.3)
            Button(
                onClick = viewModel::onSaveClicked,
                enabled = !uiState.isLoading && uiState.hasChanges && uiState.isFormValid,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Save Changes")
            }
        }
    }

    // Group Change Confirmation Dialog
    if (uiState.showGroupChangeConfirmation) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = viewModel::onDismissGroupChangeConfirmation,
            title = { Text("Change Group?") },
            text = {
                Text(
                    "Changing your Group ID will move you to a different group. " +
                        "You will no longer see devices from your current group, " +
                        "and will only see devices in the new group.",
                )
            },
            confirmButton = {
                Button(onClick = viewModel::onConfirmGroupChange) {
                    Text("Change Group")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = viewModel::onDismissGroupChangeConfirmation) {
                    Text("Cancel")
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
            value = "$selectedInterval seconds",
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text("Map Polling Interval") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            supportingText = { Text("How often to refresh group member locations on the map") },
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
                    text = { Text("$interval seconds") },
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

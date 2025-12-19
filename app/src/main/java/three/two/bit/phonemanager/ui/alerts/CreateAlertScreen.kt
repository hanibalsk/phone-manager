@file:Suppress("DEPRECATION") // hiltViewModel() deprecation - using stable API

package three.two.bit.phonemanager.ui.alerts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import three.two.bit.phonemanager.R
import three.two.bit.phonemanager.domain.model.AlertDirection
import three.two.bit.phonemanager.domain.model.Device
import three.two.bit.phonemanager.domain.model.ProximityAlert
import kotlin.math.roundToInt

/**
 * Story E5.1: Create Alert Screen
 *
 * Form for creating new proximity alerts
 * AC E5.1.2: Radius configuration (50-10,000m)
 * AC E5.1.3: Direction selection (ENTER, EXIT, BOTH)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateAlertScreen(viewModel: AlertsViewModel = hiltViewModel(), onNavigateBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Form state
    var selectedDevice by rememberSaveable { mutableStateOf<Device?>(null) }
    var radiusSliderValue by rememberSaveable { mutableFloatStateOf(0.5f) } // 0-1 range for slider
    var selectedDirection by rememberSaveable { mutableStateOf(AlertDirection.BOTH) }

    // Convert slider value to meters (50-10,000 logarithmic scale)
    val radiusMeters = sliderToRadius(radiusSliderValue)

    // Show error in snackbar
    LaunchedEffect(uiState.createError) {
        uiState.createError?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearCreateError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.create_alert_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                actions = {
                    // Save button
                    IconButton(
                        onClick = {
                            selectedDevice?.let { device ->
                                viewModel.createAlert(
                                    targetDeviceId = device.deviceId,
                                    radiusMeters = radiusMeters,
                                    direction = selectedDirection,
                                )
                                onNavigateBack()
                            }
                        },
                        enabled = selectedDevice != null && !uiState.isCreating,
                    ) {
                        if (uiState.isCreating) {
                            CircularProgressIndicator(
                                modifier = Modifier.padding(4.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(Icons.Default.Check, "Save")
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            // Device selector section
            DeviceSelectorSection(
                devices = uiState.groupMembers,
                selectedDevice = selectedDevice,
                isLoading = uiState.isLoadingMembers,
                onDeviceSelected = { selectedDevice = it },
            )

            // Radius section (AC E5.1.2)
            RadiusSection(
                sliderValue = radiusSliderValue,
                radiusMeters = radiusMeters,
                onSliderChange = { radiusSliderValue = it },
            )

            // Direction section (AC E5.1.3)
            DirectionSection(
                selectedDirection = selectedDirection,
                onDirectionSelected = { selectedDirection = it },
            )
        }
    }
}

/**
 * Device selector dropdown
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeviceSelectorSection(
    devices: List<Device>,
    selectedDevice: Device?,
    isLoading: Boolean,
    onDeviceSelected: (Device) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Text(
            text = stringResource(R.string.create_alert_target_device),
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.create_alert_target_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(12.dp))

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else if (devices.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                ),
            ) {
                Text(
                    text = stringResource(R.string.create_alert_no_members),
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        } else {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it },
            ) {
                OutlinedTextField(
                    value = selectedDevice?.displayName ?: "",
                    onValueChange = {},
                    readOnly = true,
                    placeholder = { Text(stringResource(R.string.placeholder_select_device)) },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    devices.forEach { device ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(device.displayName)
                                    device.lastSeenAt?.let {
                                        Text(
                                            text = stringResource(R.string.last_seen, it),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            },
                            onClick = {
                                onDeviceSelected(device)
                                expanded = false
                            },
                        )
                    }
                }
            }
        }
    }
}

/**
 * Radius configuration slider (AC E5.1.2: 50-10,000m)
 */
@Composable
private fun RadiusSection(sliderValue: Float, radiusMeters: Int, onSliderChange: (Float) -> Unit) {
    Column {
        Text(
            text = stringResource(R.string.create_alert_radius_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.create_alert_radius_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Radius display
        Text(
            text = formatRadius(radiusMeters),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Slider
        Slider(
            value = sliderValue,
            onValueChange = onSliderChange,
            modifier = Modifier.fillMaxWidth(),
        )

        // Range labels (AC E5.1.2)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = formatRadius(ProximityAlert.MIN_RADIUS_METERS),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = formatRadius(ProximityAlert.MAX_RADIUS_METERS),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Direction selection (AC E5.1.3)
 */
@Composable
private fun DirectionSection(selectedDirection: AlertDirection, onDirectionSelected: (AlertDirection) -> Unit) {
    Column {
        Text(
            text = stringResource(R.string.create_alert_direction_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.create_alert_direction_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(12.dp))

        Column(modifier = Modifier.selectableGroup()) {
            DirectionOption(
                direction = AlertDirection.ENTER,
                title = stringResource(R.string.alert_direction_enter),
                description = stringResource(R.string.alert_direction_enter_desc),
                selected = selectedDirection == AlertDirection.ENTER,
                onClick = { onDirectionSelected(AlertDirection.ENTER) },
            )
            DirectionOption(
                direction = AlertDirection.EXIT,
                title = stringResource(R.string.alert_direction_exit),
                description = stringResource(R.string.alert_direction_exit_desc),
                selected = selectedDirection == AlertDirection.EXIT,
                onClick = { onDirectionSelected(AlertDirection.EXIT) },
            )
            DirectionOption(
                direction = AlertDirection.BOTH,
                title = stringResource(R.string.alert_direction_both),
                description = stringResource(R.string.alert_direction_both_desc),
                selected = selectedDirection == AlertDirection.BOTH,
                onClick = { onDirectionSelected(AlertDirection.BOTH) },
            )
        }
    }
}

@Composable
private fun DirectionOption(
    direction: AlertDirection,
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton,
            )
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Convert slider value (0-1) to radius meters using logarithmic scale
 * Uses ProximityAlert.MIN_RADIUS_METERS and MAX_RADIUS_METERS constants (AC E5.1.2)
 */
private fun sliderToRadius(sliderValue: Float): Int {
    // Logarithmic scale for natural feel (small values more precise)
    val minLog = kotlin.math.ln(ProximityAlert.MIN_RADIUS_METERS.toFloat())
    val maxLog = kotlin.math.ln(ProximityAlert.MAX_RADIUS_METERS.toFloat())
    val logValue = minLog + (maxLog - minLog) * sliderValue
    return kotlin.math.exp(logValue).roundToInt()
        .coerceIn(ProximityAlert.MIN_RADIUS_METERS, ProximityAlert.MAX_RADIUS_METERS)
}

/**
 * Format radius for display
 */
private fun formatRadius(meters: Int): String = when {
    meters >= 1000 -> "${meters / 1000.0}km".replace(".0km", "km")
    else -> "${meters}m"
}

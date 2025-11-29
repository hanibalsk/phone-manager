package three.two.bit.phonemanager.ui.geofences

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import three.two.bit.phonemanager.R
import three.two.bit.phonemanager.domain.model.TransitionType
import three.two.bit.phonemanager.domain.model.Webhook
import kotlin.math.roundToInt

/**
 * Story E6.1: Create Geofence Screen
 *
 * Form for creating new geofences
 * AC E6.1.2: Transition type selection (ENTER, EXIT, DWELL)
 * AC E6.1.6: Location selection (coordinates)
 * AC E6.3.2: Webhook linking
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGeofenceScreen(viewModel: GeofencesViewModel = hiltViewModel(), onNavigateBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val webhooks by viewModel.webhooks.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Form state
    var name by rememberSaveable { mutableStateOf("") }
    var latitudeText by rememberSaveable { mutableStateOf("") }
    var longitudeText by rememberSaveable { mutableStateOf("") }
    var radiusSliderValue by rememberSaveable { mutableFloatStateOf(0.5f) }
    var enterSelected by rememberSaveable { mutableStateOf(true) }
    var exitSelected by rememberSaveable { mutableStateOf(true) }
    var dwellSelected by rememberSaveable { mutableStateOf(false) }
    var selectedWebhookId by rememberSaveable { mutableStateOf<String?>(null) }

    // Convert slider value to meters (50-10,000 logarithmic scale)
    val radiusMeters = sliderToRadius(radiusSliderValue)

    // Validate form
    val latitude = latitudeText.toDoubleOrNull()
    val longitude = longitudeText.toDoubleOrNull()
    val isValidLocation = latitude != null &&
        longitude != null &&
        latitude in -90.0..90.0 &&
        longitude in -180.0..180.0
    val hasTransitionType = enterSelected || exitSelected || dwellSelected
    val isFormValid = name.isNotBlank() && isValidLocation && hasTransitionType

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
                title = { Text("Create Geofence") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    // Save button
                    IconButton(
                        onClick = {
                            if (isFormValid && latitude != null && longitude != null) {
                                val transitionTypes = buildSet {
                                    if (enterSelected) add(TransitionType.ENTER)
                                    if (exitSelected) add(TransitionType.EXIT)
                                    if (dwellSelected) add(TransitionType.DWELL)
                                }
                                viewModel.createGeofence(
                                    name = name,
                                    latitude = latitude,
                                    longitude = longitude,
                                    radiusMeters = radiusMeters,
                                    transitionTypes = transitionTypes,
                                    webhookId = selectedWebhookId,
                                )
                                onNavigateBack()
                            }
                        },
                        enabled = isFormValid && !uiState.isCreating,
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
            // Name section
            NameSection(
                name = name,
                onNameChange = { name = it },
            )

            // Location section (AC E6.1.6)
            LocationSection(
                latitudeText = latitudeText,
                longitudeText = longitudeText,
                onLatitudeChange = { latitudeText = it },
                onLongitudeChange = { longitudeText = it },
                isValidLocation = isValidLocation || (latitudeText.isBlank() && longitudeText.isBlank()),
            )

            // Radius section
            RadiusSection(
                sliderValue = radiusSliderValue,
                radiusMeters = radiusMeters,
                onSliderChange = { radiusSliderValue = it },
            )

            // Transition types section (AC E6.1.2)
            TransitionTypesSection(
                enterSelected = enterSelected,
                exitSelected = exitSelected,
                dwellSelected = dwellSelected,
                onEnterChange = { enterSelected = it },
                onExitChange = { exitSelected = it },
                onDwellChange = { dwellSelected = it },
            )

            // Webhook section (AC E6.3.2)
            if (webhooks.isNotEmpty()) {
                WebhookSection(
                    webhooks = webhooks,
                    selectedWebhookId = selectedWebhookId,
                    onWebhookSelected = { selectedWebhookId = it },
                )
            }
        }
    }
}

/**
 * Name input section
 */
@Composable
private fun NameSection(name: String, onNameChange: (String) -> Unit) {
    Column {
        Text(
            text = stringResource(R.string.create_geofence_name_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.create_geofence_name_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Name") },
            placeholder = { Text("e.g., Home, Office, School") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * Location input section (AC E6.1.6)
 */
@Composable
private fun LocationSection(
    latitudeText: String,
    longitudeText: String,
    onLatitudeChange: (String) -> Unit,
    onLongitudeChange: (String) -> Unit,
    isValidLocation: Boolean,
) {
    Column {
        Text(
            text = stringResource(R.string.create_geofence_location_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.create_geofence_location_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = latitudeText,
                onValueChange = onLatitudeChange,
                label = { Text("Latitude") },
                placeholder = { Text("e.g., 37.7749") },
                singleLine = true,
                isError = !isValidLocation,
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = longitudeText,
                onValueChange = onLongitudeChange,
                label = { Text("Longitude") },
                placeholder = { Text("e.g., -122.4194") },
                singleLine = true,
                isError = !isValidLocation,
                modifier = Modifier.weight(1f),
            )
        }

        if (!isValidLocation) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.create_geofence_invalid_coords),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Use current location button (future enhancement)
        OutlinedButton(
            onClick = { /* TODO: Implement current location */ },
            modifier = Modifier.fillMaxWidth(),
            enabled = false, // Disabled for now
        ) {
            Icon(Icons.Default.MyLocation, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Use Current Location (Coming Soon)")
        }
    }
}

/**
 * Radius configuration slider
 */
@Composable
private fun RadiusSection(sliderValue: Float, radiusMeters: Int, onSliderChange: (Float) -> Unit) {
    Column {
        Text(
            text = stringResource(R.string.create_geofence_radius_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.create_geofence_radius_hint),
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

        // Range labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "50m",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "10km",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Transition types selection (AC E6.1.2)
 */
@Composable
private fun TransitionTypesSection(
    enterSelected: Boolean,
    exitSelected: Boolean,
    dwellSelected: Boolean,
    onEnterChange: (Boolean) -> Unit,
    onExitChange: (Boolean) -> Unit,
    onDwellChange: (Boolean) -> Unit,
) {
    Column {
        Text(
            text = stringResource(R.string.create_geofence_trigger_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.create_geofence_trigger_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(12.dp))

        TransitionTypeCheckbox(
            label = stringResource(R.string.create_geofence_trigger_enter),
            description = "Trigger when entering the area",
            checked = enterSelected,
            onCheckedChange = onEnterChange,
        )
        TransitionTypeCheckbox(
            label = stringResource(R.string.create_geofence_trigger_exit),
            description = "Trigger when leaving the area",
            checked = exitSelected,
            onCheckedChange = onExitChange,
        )
        TransitionTypeCheckbox(
            label = stringResource(R.string.create_geofence_trigger_dwell),
            description = "Trigger after staying in the area",
            checked = dwellSelected,
            onCheckedChange = onDwellChange,
        )
    }
}

@Composable
private fun TransitionTypeCheckbox(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = label,
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
 * Convert slider value (0-1) to radius meters (50-10,000) using logarithmic scale
 */
private fun sliderToRadius(sliderValue: Float): Int {
    // Logarithmic scale: 50m to 10,000m
    val minLog = kotlin.math.ln(50f)
    val maxLog = kotlin.math.ln(10000f)
    val logValue = minLog + (maxLog - minLog) * sliderValue
    return kotlin.math.exp(logValue).roundToInt().coerceIn(50, 10000)
}

/**
 * Format radius for display
 */
private fun formatRadius(meters: Int): String = when {
    meters >= 1000 -> "${meters / 1000.0}km".replace(".0km", "km")
    else -> "${meters}m"
}

/**
 * Story E6.3: Webhook selector section (AC E6.3.2)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WebhookSection(webhooks: List<Webhook>, selectedWebhookId: String?, onWebhookSelected: (String?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val selectedWebhook = webhooks.find { it.id == selectedWebhookId }

    Column {
        Text(
            text = stringResource(R.string.create_geofence_webhook_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.create_geofence_webhook_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(12.dp))

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
        ) {
            OutlinedTextField(
                value = selectedWebhook?.name ?: "None",
                onValueChange = {},
                readOnly = true,
                label = { Text("Webhook") },
                leadingIcon = {
                    Icon(Icons.Default.Link, contentDescription = null)
                },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                // None option
                DropdownMenuItem(
                    text = { Text("None") },
                    onClick = {
                        onWebhookSelected(null)
                        expanded = false
                    },
                )

                // Available webhooks
                webhooks.forEach { webhook ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(webhook.name)
                                Text(
                                    text = webhook.targetUrl,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        },
                        onClick = {
                            onWebhookSelected(webhook.id)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

@file:Suppress("DEPRECATION") // hiltViewModel() deprecation - using stable API

package three.two.bit.phonemanager.ui.admin

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import three.two.bit.phonemanager.R
import three.two.bit.phonemanager.domain.model.Geofence
import three.two.bit.phonemanager.domain.model.TransitionType
import kotlin.math.roundToInt

/**
 * Story E9.4: Admin Geofence Screen
 *
 * Allows admins to manage geofences for users in their groups.
 * Supports viewing, creating, and deleting geofences.
 *
 * ACs: E9.4.1, E9.4.2, E9.4.3, E9.4.4
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminGeofenceScreen(viewModel: AdminGeofenceViewModel = hiltViewModel(), onNavigateBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Show errors in snackbar
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    LaunchedEffect(uiState.createError) {
        uiState.createError?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.device?.displayName?.let {
                            stringResource(R.string.admin_geofences_title, it)
                        } ?: stringResource(R.string.admin_manage_geofences),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showCreateDialog() },
                containerColor = MaterialTheme.colorScheme.primary,
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.admin_add_geofence))
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        val isRefreshing = uiState.isLoading && uiState.geofences.isNotEmpty()

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            when {
                // Initial loading
                uiState.isLoading && uiState.geofences.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                }
                // Empty state
                uiState.geofences.isEmpty() -> {
                    EmptyGeofencesContent(
                        deviceName = uiState.device?.displayName ?: "",
                        modifier = Modifier.fillMaxSize(),
                        onCreateClick = { viewModel.showCreateDialog() },
                    )
                }
                // Geofence list
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(16.dp),
                    ) {
                        items(
                            items = uiState.geofences,
                            key = { it.id },
                        ) { geofence ->
                            SwipeableGeofenceItem(
                                geofence = geofence,
                                onDelete = { viewModel.showDeleteConfirmation(geofence) },
                            )
                        }
                    }
                }
            }
        }

        // Create geofence bottom sheet
        if (uiState.showCreateDialog) {
            ModalBottomSheet(
                onDismissRequest = { viewModel.hideCreateDialog() },
                sheetState = bottomSheetState,
            ) {
                CreateGeofenceContent(
                    isCreating = uiState.isCreating,
                    onCreate = { name, lat, lng, radius, transitions ->
                        viewModel.createGeofence(name, lat, lng, radius, transitions)
                    },
                    onCancel = { viewModel.hideCreateDialog() },
                )
            }
        }

        // Delete confirmation dialog
        uiState.geofenceToDelete?.let { geofence ->
            AlertDialog(
                onDismissRequest = { viewModel.hideDeleteConfirmation() },
                title = { Text(stringResource(R.string.admin_delete_geofence_title)) },
                text = {
                    Text(stringResource(R.string.admin_delete_geofence_message, geofence.name))
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteGeofence(geofence.id)
                            viewModel.hideDeleteConfirmation()
                        },
                        enabled = !uiState.isDeleting,
                    ) {
                        if (uiState.isDeleting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text(stringResource(R.string.delete))
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.hideDeleteConfirmation() }) {
                        Text(stringResource(R.string.cancel))
                    }
                },
            )
        }
    }
}

/**
 * Swipeable geofence item with delete action
 */
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("DEPRECATION") // confirmValueChange deprecation - TODO: migrate to new API
@Composable
private fun SwipeableGeofenceItem(geofence: Geofence, onDelete: () -> Unit) {
    val currentOnDelete by rememberUpdatedState(onDelete)
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                currentOnDelete()
                true
            } else {
                false
            }
        },
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val color by animateColorAsState(
                when (dismissState.targetValue) {
                    SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.error
                    else -> Color.Transparent
                },
                label = "swipe_color",
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.delete),
                    tint = Color.White,
                )
            }
        },
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
    ) {
        GeofenceCard(geofence = geofence)
    }
}

/**
 * Geofence card displaying geofence information
 */
@Composable
private fun GeofenceCard(geofence: Geofence) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (geofence.active) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (geofence.active) {
                    Icons.Default.LocationOn
                } else {
                    Icons.Default.LocationOff
                },
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = if (geofence.active) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outline
                },
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = geofence.name,
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatGeofenceDescription(geofence),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * Format geofence description string
 */
private fun formatGeofenceDescription(geofence: Geofence): String {
    val radiusText = when {
        geofence.radiusMeters >= 1000 -> "${geofence.radiusMeters / 1000}km"
        else -> "${geofence.radiusMeters}m"
    }
    val transitionText = when {
        geofence.transitionTypes.containsAll(setOf(TransitionType.ENTER, TransitionType.EXIT)) -> "enter/exit"
        geofence.transitionTypes.contains(TransitionType.ENTER) -> "enter"
        geofence.transitionTypes.contains(TransitionType.EXIT) -> "exit"
        geofence.transitionTypes.contains(TransitionType.DWELL) -> "dwell"
        else -> "any"
    }
    return "$radiusText radius, trigger on $transitionText"
}

/**
 * Empty state when no geofences exist
 */
@Composable
private fun EmptyGeofencesContent(deviceName: String, modifier: Modifier = Modifier, onCreateClick: () -> Unit) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Default.LocationOff,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.admin_no_geofences_title),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (deviceName.isNotEmpty()) {
                stringResource(R.string.admin_no_geofences_message, deviceName)
            } else {
                stringResource(R.string.admin_no_geofences_message_generic)
            },
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onCreateClick) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.admin_add_geofence))
        }
    }
}

/**
 * Create geofence form content for bottom sheet
 *
 * AC E9.4.3: Define geofence boundaries
 */
@Composable
private fun CreateGeofenceContent(
    isCreating: Boolean,
    onCreate: (String, Double, Double, Int, Set<TransitionType>) -> Unit,
    onCancel: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var latitudeText by rememberSaveable { mutableStateOf("") }
    var longitudeText by rememberSaveable { mutableStateOf("") }
    var radiusSliderValue by rememberSaveable { mutableFloatStateOf(0.5f) }
    var enterSelected by rememberSaveable { mutableStateOf(true) }
    var exitSelected by rememberSaveable { mutableStateOf(true) }
    var dwellSelected by rememberSaveable { mutableStateOf(false) }

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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.admin_create_geofence_title),
            style = MaterialTheme.typography.headlineSmall,
        )

        // Name field
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(stringResource(R.string.label_name)) },
            placeholder = { Text(stringResource(R.string.placeholder_geofence_name)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        // Location fields
        Text(
            text = stringResource(R.string.admin_geofence_location),
            style = MaterialTheme.typography.titleSmall,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = latitudeText,
                onValueChange = { latitudeText = it },
                label = { Text(stringResource(R.string.label_latitude)) },
                singleLine = true,
                isError = latitudeText.isNotBlank() && latitude == null,
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = longitudeText,
                onValueChange = { longitudeText = it },
                label = { Text(stringResource(R.string.label_longitude)) },
                singleLine = true,
                isError = longitudeText.isNotBlank() && longitude == null,
                modifier = Modifier.weight(1f),
            )
        }

        // Radius slider
        Column {
            Text(
                text = stringResource(R.string.admin_geofence_radius, formatRadius(radiusMeters)),
                style = MaterialTheme.typography.titleSmall,
            )
            Slider(
                value = radiusSliderValue,
                onValueChange = { radiusSliderValue = it },
                modifier = Modifier.fillMaxWidth(),
            )
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

        // Transition types
        Text(
            text = stringResource(R.string.admin_geofence_triggers),
            style = MaterialTheme.typography.titleSmall,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = enterSelected, onCheckedChange = { enterSelected = it })
            Text(stringResource(R.string.create_geofence_trigger_enter))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = exitSelected, onCheckedChange = { exitSelected = it })
            Text(stringResource(R.string.create_geofence_trigger_exit))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = dwellSelected, onCheckedChange = { dwellSelected = it })
            Text(stringResource(R.string.create_geofence_trigger_dwell))
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.cancel))
            }
            Button(
                onClick = {
                    // isFormValid guarantees latitude/longitude are non-null via isValidLocation check
                    if (isFormValid) {
                        val transitionTypes = buildSet {
                            if (enterSelected) add(TransitionType.ENTER)
                            if (exitSelected) add(TransitionType.EXIT)
                            if (dwellSelected) add(TransitionType.DWELL)
                        }
                        onCreate(name, latitude!!, longitude!!, radiusMeters, transitionTypes)
                    }
                },
                enabled = isFormValid && !isCreating,
                modifier = Modifier.weight(1f),
            ) {
                if (isCreating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(stringResource(R.string.button_create))
                }
            }
        }
    }
}

/**
 * Convert slider value (0-1) to radius meters (50-10,000) using logarithmic scale
 */
private fun sliderToRadius(sliderValue: Float): Int {
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

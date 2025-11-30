package three.two.bit.phonemanager.ui.history

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import three.two.bit.phonemanager.R

/**
 * Story E4.1/E4.2: History Screen
 *
 * Displays location history as polyline on map with date filters and device selector
 * ACs: E4.1.1, E4.1.2, E4.1.3, E4.1.4, E4.1.5, E4.1.6, E4.2.1, E4.2.3
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: HistoryViewModel = hiltViewModel(), onNavigateBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Camera position - center on first point or default
    val cameraPositionState =
        rememberCameraPositionState {
            val firstPoint = uiState.polylinePoints.firstOrNull() ?: LatLng(0.0, 0.0)
            position = CameraPosition.fromLatLngZoom(firstPoint, 18f)
        }

    // Center camera when polyline changes and zoom to show all points
    LaunchedEffect(uiState.polylinePoints) {
        if (uiState.polylinePoints.isNotEmpty()) {
            if (uiState.polylinePoints.size == 1) {
                // Single point - zoom in close
                cameraPositionState.position = CameraPosition.fromLatLngZoom(uiState.polylinePoints.first(), 18f)
            } else {
                // Multiple points - create bounds to fit all points
                val boundsBuilder = LatLngBounds.builder()
                uiState.polylinePoints.forEach { boundsBuilder.include(it) }
                val bounds = boundsBuilder.build()

                // Animate camera to show all points with padding
                cameraPositionState.animate(
                    com.google.android.gms.maps.CameraUpdateFactory.newLatLngBounds(bounds, 100),
                )
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.history_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier =
            Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            // AC E4.2.3: Device selector
            if (uiState.availableDevices.isNotEmpty()) {
                DeviceSelector(
                    selectedDevice = uiState.selectedDevice,
                    availableDevices = uiState.availableDevices,
                    onDeviceSelected = viewModel::selectDevice,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }

            // AC E4.1.4, E4.1.5: Date filter chips
            DateFilterRow(
                selectedFilter = uiState.selectedFilter,
                onFilterSelected = viewModel::setDateFilter,
                onCustomRangeClicked = viewModel::onCustomRangeClicked,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )

            // Map or loading/error/empty states
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
            ) {
                when {
                    // Loading state
                    uiState.isLoading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }
                    // Error state
                    uiState.error != null -> {
                        Text(
                            text = uiState.error!!,
                            modifier = Modifier.align(Alignment.Center),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    // AC E4.1.6: Empty state
                    uiState.isEmpty -> {
                        EmptyHistoryContent(
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }
                    // AC E4.1.1, E4.1.2: Map with polyline
                    else -> {
                        GoogleMap(
                            modifier = Modifier.fillMaxSize(),
                            cameraPositionState = cameraPositionState,
                            properties = MapProperties(),
                            uiSettings =
                            MapUiSettings(
                                zoomControlsEnabled = true,
                                scrollGesturesEnabled = true,
                                zoomGesturesEnabled = true,
                            ),
                        ) {
                            // AC E4.1.2: Polyline connecting locations chronologically
                            if (uiState.polylinePoints.isNotEmpty()) {
                                Polyline(
                                    points = uiState.polylinePoints,
                                    color = Color(0xFF2196F3), // Blue color
                                    width = 10f,
                                )

                                // Add start marker (green)
                                Marker(
                                    state = MarkerState(position = uiState.polylinePoints.first()),
                                    title = stringResource(R.string.history_start),
                                    snippet = stringResource(R.string.history_path_start),
                                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN),
                                )

                                // Add end marker (red) if different from start
                                if (uiState.polylinePoints.size > 1) {
                                    Marker(
                                        state = MarkerState(position = uiState.polylinePoints.last()),
                                        title = stringResource(R.string.history_end),
                                        snippet = stringResource(R.string.history_path_end),
                                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // AC E4.1.5: Start Date Picker Dialog
    if (uiState.showStartDatePicker) {
        val initialDateMillis = uiState.customStartDate?.let {
            it.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
        }
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialDateMillis,
            selectableDates = PastSelectableDates,
        )

        DatePickerDialog(
            onDismissRequest = viewModel::dismissDatePicker,
            confirmButton = {
                TextButton(onClick = { viewModel.onStartDateSelected(datePickerState.selectedDateMillis) }) {
                    Text(stringResource(R.string.history_next))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDatePicker) {
                    Text(stringResource(R.string.cancel))
                }
            },
        ) {
            DatePicker(
                state = datePickerState,
                title = {
                    Text(stringResource(R.string.history_select_start_date), modifier = Modifier.padding(16.dp))
                },
            )
        }
    }

    // AC E4.1.5: End Date Picker Dialog
    if (uiState.showEndDatePicker) {
        val initialDateMillis = uiState.customEndDate?.let {
            it.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
        }
        val startDateMillis = uiState.customStartDate?.let {
            it.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
        } ?: 0L

        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialDateMillis,
            selectableDates = EndDateSelectableDates(startDateMillis),
        )

        DatePickerDialog(
            onDismissRequest = viewModel::dismissDatePicker,
            confirmButton = {
                TextButton(onClick = { viewModel.onEndDateSelected(datePickerState.selectedDateMillis) }) {
                    Text(stringResource(R.string.history_apply))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDatePicker) {
                    Text(stringResource(R.string.cancel))
                }
            },
        ) {
            DatePicker(
                state = datePickerState,
                title = { Text(stringResource(R.string.history_select_end_date), modifier = Modifier.padding(16.dp)) },
            )
        }
    }
}

/**
 * SelectableDates that only allows past dates (AC E4.1.5)
 */
@OptIn(ExperimentalMaterial3Api::class)
private object PastSelectableDates : SelectableDates {
    override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis <= System.currentTimeMillis()
}

/**
 * SelectableDates for end date - must be on or after start date and not in future (AC E4.1.5)
 */
@OptIn(ExperimentalMaterial3Api::class)
private class EndDateSelectableDates(private val startDateMillis: Long) : SelectableDates {
    override fun isSelectableDate(utcTimeMillis: Long): Boolean =
        utcTimeMillis >= startDateMillis && utcTimeMillis <= System.currentTimeMillis()
}

/**
 * Device selector dropdown (AC E4.2.3)
 *
 * Allows switching between viewing own history and group members' history
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeviceSelector(
    selectedDevice: HistoryDevice?,
    availableDevices: List<HistoryDevice>,
    onDeviceSelected: (HistoryDevice) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val myDeviceLabel = stringResource(R.string.history_my_device)

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.width(8.dp))
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier.weight(1f),
        ) {
            OutlinedTextField(
                value = selectedDevice?.let {
                    if (it.isCurrentDevice) myDeviceLabel else it.displayName
                } ?: myDeviceLabel,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                modifier = Modifier
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth(),
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                availableDevices.forEach { device ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                if (device.isCurrentDevice) {
                                    myDeviceLabel
                                } else {
                                    device.displayName
                                },
                            )
                        },
                        onClick = {
                            onDeviceSelected(device)
                            expanded = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                    )
                }
            }
        }
    }
}

/**
 * Date filter row with preset and custom chips (AC E4.1.4, E4.1.5)
 */
@Composable
private fun DateFilterRow(
    selectedFilter: DateFilter,
    onFilterSelected: (DateFilter) -> Unit,
    onCustomRangeClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = selectedFilter is DateFilter.Today,
            onClick = { onFilterSelected(DateFilter.Today) },
            label = { Text(stringResource(R.string.day_today)) },
        )
        FilterChip(
            selected = selectedFilter is DateFilter.Yesterday,
            onClick = { onFilterSelected(DateFilter.Yesterday) },
            label = { Text(stringResource(R.string.history_yesterday)) },
        )
        FilterChip(
            selected = selectedFilter is DateFilter.Last7Days,
            onClick = { onFilterSelected(DateFilter.Last7Days) },
            label = { Text(stringResource(R.string.history_last_7_days)) },
        )
        // AC E4.1.5: Custom date range chip
        FilterChip(
            selected = selectedFilter is DateFilter.Custom,
            onClick = onCustomRangeClicked,
            label = {
                if (selectedFilter is DateFilter.Custom) {
                    Text("${selectedFilter.startDate} - ${selectedFilter.endDate}")
                } else {
                    Text(stringResource(R.string.history_custom))
                }
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            },
        )
    }
}

/**
 * Empty state when no history exists (AC E4.1.6)
 */
@Composable
private fun EmptyHistoryContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Default.History,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.history_empty_title),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.history_empty_hint),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

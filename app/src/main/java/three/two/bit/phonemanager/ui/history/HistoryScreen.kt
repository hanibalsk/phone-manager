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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn

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
            position = CameraPosition.fromLatLngZoom(firstPoint, 13f)
        }

    // Center camera when polyline changes
    LaunchedEffect(uiState.polylinePoints) {
        if (uiState.polylinePoints.isNotEmpty()) {
            val firstPoint = uiState.polylinePoints.first()
            cameraPositionState.position = CameraPosition.fromLatLngZoom(firstPoint, 13f)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Location History") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
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
                                    width = 8f,
                                )
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
                    Text("Next")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDatePicker) {
                    Text("Cancel")
                }
            },
        ) {
            DatePicker(
                state = datePickerState,
                title = { Text("Select Start Date", modifier = Modifier.padding(16.dp)) },
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
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDatePicker) {
                    Text("Cancel")
                }
            },
        ) {
            DatePicker(
                state = datePickerState,
                title = { Text("Select End Date", modifier = Modifier.padding(16.dp)) },
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
                    if (it.isCurrentDevice) "${it.displayName} (Me)" else it.displayName
                } ?: "Select device",
                onValueChange = {},
                readOnly = true,
                label = { Text("Device") },
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
                                    "${device.displayName} (Me)"
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
            label = { Text("Today") },
        )
        FilterChip(
            selected = selectedFilter is DateFilter.Yesterday,
            onClick = { onFilterSelected(DateFilter.Yesterday) },
            label = { Text("Yesterday") },
        )
        FilterChip(
            selected = selectedFilter is DateFilter.Last7Days,
            onClick = { onFilterSelected(DateFilter.Last7Days) },
            label = { Text("Last 7 Days") },
        )
        // AC E4.1.5: Custom date range chip
        FilterChip(
            selected = selectedFilter is DateFilter.Custom,
            onClick = onCustomRangeClicked,
            label = {
                if (selectedFilter is DateFilter.Custom) {
                    Text("${selectedFilter.startDate} - ${selectedFilter.endDate}")
                } else {
                    Text("Custom")
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
            text = "No location history for this period",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Try selecting a different date range",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

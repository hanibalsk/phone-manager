package three.two.bit.phonemanager.ui.tripdetail

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import three.two.bit.phonemanager.R
import three.two.bit.phonemanager.domain.model.Trip
import three.two.bit.phonemanager.movement.TransportationMode
import three.two.bit.phonemanager.ui.tripdetail.components.ModeBreakdownChart
import three.two.bit.phonemanager.ui.tripdetail.components.TripMap
import three.two.bit.phonemanager.ui.tripdetail.components.TripStatisticsCard

/**
 * Story E8.10: Trip Detail Screen
 *
 * Displays trip details with route map, statistics, and actions.
 * ACs: E8.10.1-E8.10.10
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripDetailScreen(onNavigateBack: () -> Unit, viewModel: TripDetailViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.trip_detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                actions = {
                    // Edit name button (AC E8.10.7)
                    IconButton(onClick = { viewModel.showEditNameDialog(true) }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(R.string.trip_edit_name),
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            when {
                uiState.isLoading -> {
                    LoadingState()
                }

                uiState.error != null -> {
                    ErrorState(
                        message = uiState.error!!,
                        onRetry = { viewModel.clearError() },
                    )
                }

                uiState.trip != null -> {
                    TripDetailContent(
                        uiState = uiState,
                        onTogglePathView = viewModel::togglePathView,
                        onLocationSelected = viewModel::selectLocation,
                        onExportGpx = viewModel::exportToGpx,
                        onDelete = { viewModel.showDeleteConfirmation(true) },
                    )
                }
            }
        }
    }

    // Edit name dialog (AC E8.10.7)
    if (uiState.showEditNameDialog) {
        EditTripNameDialog(
            currentName = uiState.trip?.name ?: "",
            onConfirm = { newName ->
                viewModel.updateTripName(newName)
            },
            onDismiss = { viewModel.showEditNameDialog(false) },
        )
    }

    // Delete confirmation dialog (AC E8.10.9)
    if (uiState.showDeleteConfirmation) {
        DeleteConfirmationDialog(
            onConfirm = { viewModel.deleteTrip(onNavigateBack) },
            onDismiss = { viewModel.showDeleteConfirmation(false) },
        )
    }
}

@Composable
private fun TripDetailContent(
    uiState: TripDetailUiState,
    onTogglePathView: () -> Unit,
    onLocationSelected: (Int?) -> Unit,
    onExportGpx: () -> Unit,
    onDelete: () -> Unit,
) {
    val trip = uiState.trip ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        // Map section (AC E8.10.2, E8.10.3)
        TripMap(
            locations = uiState.locations,
            showCorrectedPath = uiState.showCorrectedPath,
            correctedPath = uiState.correctedPath,
            hasCorrectedPath = uiState.hasCorrectedPath,
            selectedLocationIndex = uiState.selectedLocationIndex,
            onTogglePathView = onTogglePathView,
            onLocationSelected = onLocationSelected,
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp),
        )

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Trip info card (AC E8.10.4)
            TripInfoCard(trip = trip)

            // Mode breakdown chart (AC E8.10.5)
            if (uiState.modeBreakdown.isNotEmpty()) {
                ModeBreakdownChart(items = uiState.modeBreakdown)
            }

            // Statistics (AC E8.10.6)
            uiState.statistics?.let { stats ->
                TripStatisticsCard(statistics = stats)
            }

            // Action buttons (AC E8.10.8, E8.10.9)
            ActionButtons(
                isExporting = uiState.isExporting,
                onExportGpx = onExportGpx,
                onDelete = onDelete,
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun TripInfoCard(trip: Trip, modifier: Modifier = Modifier) {
    val timeZone = TimeZone.currentSystemDefault()
    val startDateTime = trip.startTime.toLocalDateTime(timeZone)
    val endDateTime = trip.endTime?.toLocalDateTime(timeZone)

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            // Trip name and date
            Text(
                text = trip.name ?: getTripName(trip),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = formatDate(startDateTime),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Duration and distance row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                StatColumn(
                    label = stringResource(R.string.trip_detail_duration),
                    value = trip.formattedDuration ?: "--",
                )
                StatColumn(
                    label = stringResource(R.string.trip_detail_distance),
                    value = trip.formattedDistance,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Start and end times
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TimeColumn(
                    label = stringResource(R.string.trip_detail_start),
                    time = formatTime(startDateTime),
                )
                TimeColumn(
                    label = stringResource(R.string.trip_detail_end),
                    time = endDateTime?.let { formatTime(it) } ?: stringResource(R.string.in_progress),
                )
            }
        }
    }
}

@Composable
private fun StatColumn(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun TimeColumn(label: String, time: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = time,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun ActionButtons(
    isExporting: Boolean,
    onExportGpx: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Export GPX button (AC E8.10.8)
        OutlinedButton(
            onClick = onExportGpx,
            modifier = Modifier.weight(1f),
            enabled = !isExporting,
        ) {
            if (isExporting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                )
            } else {
                Icon(
                    imageVector = Icons.Default.IosShare,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(modifier = Modifier.padding(horizontal = 4.dp))
            Text(stringResource(R.string.trip_export_gpx))
        }

        // Delete button (AC E8.10.9)
        Button(
            onClick = onDelete,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
            ),
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.padding(horizontal = 4.dp))
            Text(stringResource(R.string.delete))
        }
    }
}

@Composable
private fun EditTripNameDialog(currentName: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.trip_edit_name)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.label_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name) },
                enabled = name.isNotBlank(),
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
private fun DeleteConfirmationDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.trip_delete_title)) },
        text = { Text(stringResource(R.string.trip_delete_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    stringResource(R.string.delete),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.error),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.error,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onRetry) {
            Text(stringResource(R.string.button_retry))
        }
    }
}

private fun getTripName(trip: Trip): String = when (trip.dominantMode) {
    TransportationMode.WALKING -> "Walk"
    TransportationMode.RUNNING -> "Run"
    TransportationMode.CYCLING -> "Bike Ride"
    TransportationMode.IN_VEHICLE -> "Drive"
    TransportationMode.STATIONARY -> "Stationary"
    TransportationMode.UNKNOWN -> "Trip"
}

private fun formatDate(dateTime: kotlinx.datetime.LocalDateTime): String {
    val month = when (dateTime.monthNumber) {
        1 -> "January"
        2 -> "February"
        3 -> "March"
        4 -> "April"
        5 -> "May"
        6 -> "June"
        7 -> "July"
        8 -> "August"
        9 -> "September"
        10 -> "October"
        11 -> "November"
        12 -> "December"
        else -> ""
    }
    return "$month ${dateTime.dayOfMonth}, ${dateTime.year}"
}

private fun formatTime(dateTime: kotlinx.datetime.LocalDateTime): String {
    val hour = if (dateTime.hour > 12) {
        dateTime.hour - 12
    } else if (dateTime.hour == 0) {
        12
    } else {
        dateTime.hour
    }
    val amPm = if (dateTime.hour >= 12) "PM" else "AM"
    return String.format("%d:%02d %s", hour, dateTime.minute, amPm)
}

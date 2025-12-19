@file:Suppress("DEPRECATION") // hiltViewModel() deprecation - using stable API

package three.two.bit.phonemanager.ui.movementevents

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Battery4Bar
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SyncDisabled
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import three.two.bit.phonemanager.R
import three.two.bit.phonemanager.domain.model.MovementEvent
import three.two.bit.phonemanager.movement.TransportationMode

/**
 * Story E8.11: Movement Events Screen
 *
 * Displays movement events for developer debugging.
 * ACs: E8.11.1-E8.11.9
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovementEventsScreen(onNavigateBack: () -> Unit, viewModel: MovementEventsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.movement_events_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                actions = {
                    // Export button (AC E8.11.5)
                    IconButton(
                        onClick = { viewModel.exportToJson() },
                        enabled = !uiState.isExporting && uiState.events.isNotEmpty(),
                    ) {
                        if (uiState.isExporting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.IosShare,
                                contentDescription = stringResource(R.string.movement_events_export),
                            )
                        }
                    }
                    // Clear old events button (AC E8.11.6)
                    IconButton(
                        onClick = { viewModel.showClearConfirmation(true) },
                        enabled = uiState.events.isNotEmpty(),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.movement_events_clear),
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            // Live mode toggle and statistics (AC E8.11.2, E8.11.9)
            LiveModeHeader(
                isLiveMode = uiState.isLiveMode,
                eventCount = uiState.events.size,
                unsyncedCount = uiState.unsyncedCount,
                onToggleLiveMode = viewModel::toggleLiveMode,
            )

            // Mode filter chips (AC E8.11.7)
            ModeFilterRow(
                selectedMode = uiState.modeFilter,
                onSelectMode = viewModel::setModeFilter,
            )

            // Events list
            when {
                uiState.isLoading && uiState.events.isEmpty() -> {
                    LoadingState()
                }

                uiState.error != null && uiState.events.isEmpty() -> {
                    ErrorState(
                        message = uiState.error!!,
                        onRetry = { viewModel.clearError() },
                    )
                }

                uiState.events.isEmpty() -> {
                    EmptyState()
                }

                else -> {
                    EventsList(
                        events = uiState.events,
                        expandedEventIds = uiState.expandedEventIds,
                        hasMorePages = uiState.hasMorePages,
                        isLoadingMore = uiState.isLoadingMore,
                        onToggleExpand = viewModel::toggleEventExpansion,
                        onLoadMore = viewModel::loadMore,
                    )
                }
            }
        }
    }

    // Clear confirmation dialog
    if (uiState.showClearConfirmation) {
        ClearConfirmationDialog(
            onConfirm = {
                viewModel.clearOldEvents()
                viewModel.showClearConfirmation(false)
            },
            onDismiss = { viewModel.showClearConfirmation(false) },
        )
    }
}

@Composable
private fun LiveModeHeader(
    isLiveMode: Boolean,
    eventCount: Int,
    unsyncedCount: Int,
    onToggleLiveMode: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isLiveMode) {
                MaterialTheme.colorScheme.primaryContainer
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
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.movement_events_live_mode),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (isLiveMode) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF4CAF50)),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = stringResource(R.string.movement_events_count, eventCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = if (unsyncedCount > 0) {
                                Icons.Default.SyncDisabled
                            } else {
                                Icons.Default.Sync
                            },
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = if (unsyncedCount > 0) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.movement_events_unsynced, unsyncedCount),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (unsyncedCount > 0) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                }
            }
            Switch(
                checked = isLiveMode,
                onCheckedChange = { onToggleLiveMode() },
            )
        }
    }
}

@Composable
private fun ModeFilterRow(
    selectedMode: TransportationMode?,
    onSelectMode: (TransportationMode?) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            FilterChip(
                selected = selectedMode == null,
                onClick = { onSelectMode(null) },
                label = { Text(stringResource(R.string.trip_filter_all)) },
            )
        }
        items(TransportationMode.entries.filter { it != TransportationMode.UNKNOWN }) { mode ->
            FilterChip(
                selected = selectedMode == mode,
                onClick = { onSelectMode(mode) },
                label = { Text(getModeLabel(mode)) },
            )
        }
    }
}

@Composable
private fun EventsList(
    events: List<MovementEvent>,
    expandedEventIds: Set<Long>,
    hasMorePages: Boolean,
    isLoadingMore: Boolean,
    onToggleExpand: (Long) -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(events, key = { it.id }) { event ->
            EventCard(
                event = event,
                isExpanded = expandedEventIds.contains(event.id),
                onToggleExpand = { onToggleExpand(event.id) },
            )
        }

        // Load more button (AC E8.11.8)
        if (hasMorePages) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isLoadingMore) {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                    } else {
                        TextButton(onClick = onLoadMore) {
                            Text(stringResource(R.string.load_more))
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun EventCard(
    event: MovementEvent,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val timeZone = TimeZone.currentSystemDefault()
    val dateTime = event.timestamp.toLocalDateTime(timeZone)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable { onToggleExpand() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            // Header row - timestamp and mode transition (AC E8.11.3)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = formatTime(dateTime),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Icon(
                    imageVector = if (isExpanded) {
                        Icons.Default.ExpandLess
                    } else {
                        Icons.Default.ExpandMore
                    },
                    contentDescription = if (isExpanded) {
                        stringResource(R.string.collapse)
                    } else {
                        stringResource(R.string.expand)
                    },
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Mode transition
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ModeChip(mode = event.previousMode)
                Text(
                    text = " → ",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                ModeChip(mode = event.newMode)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Source and confidence
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = event.detectionSource.name.replace("_", " "),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "(${String.format("%.0f", event.confidence * 100)}%)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = getConfidenceColor(event.confidence),
                )
            }

            // Expanded details (AC E8.11.4)
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                Column(
                    modifier = Modifier.padding(top = 12.dp),
                ) {
                    // Location
                    event.location?.let { location ->
                        DetailRow(
                            icon = Icons.Default.LocationOn,
                            label = stringResource(R.string.movement_events_location),
                            value = "${String.format("%.4f", location.latitude)}, " +
                                "${String.format("%.4f", location.longitude)} " +
                                "(±${String.format("%.0f", location.accuracy)}m)",
                        )
                    }

                    // Device state
                    event.deviceState?.let { deviceState ->
                        DetailRow(
                            icon = if (deviceState.batteryCharging == true) {
                                Icons.Default.BatteryChargingFull
                            } else {
                                Icons.Default.Battery4Bar
                            },
                            label = stringResource(R.string.movement_events_battery),
                            value = "${deviceState.batteryLevel}%" +
                                if (deviceState.batteryCharging == true) " (charging)" else "",
                        )

                        DetailRow(
                            icon = Icons.Default.Wifi,
                            label = stringResource(R.string.movement_events_network),
                            value = deviceState.networkType?.name ?: "Unknown",
                        )
                    }

                    // Sensor telemetry
                    event.sensorTelemetry?.let { telemetry ->
                        DetailRow(
                            icon = Icons.Default.Speed,
                            label = stringResource(R.string.movement_events_accelerometer),
                            value = "magnitude: ${String.format("%.2f", telemetry.accelerometerMagnitude)} m/s² " +
                                "(var: ${String.format("%.3f", telemetry.accelerometerVariance)})",
                        )

                        telemetry.stepCount?.let { steps ->
                            DetailRow(
                                icon = Icons.Default.Speed,
                                label = stringResource(R.string.movement_events_steps),
                                value = steps.toString(),
                            )
                        }
                    }

                    // Detection latency
                    DetailRow(
                        icon = Icons.Default.Timer,
                        label = stringResource(R.string.movement_events_latency),
                        value = "${event.detectionLatencyMs}ms",
                    )
                }
            }
        }
    }
}

@Composable
private fun ModeChip(mode: TransportationMode, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(getModeColor(mode).copy(alpha = 0.2f))
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        Text(
            text = getModeLabel(mode),
            style = MaterialTheme.typography.labelMedium,
            color = getModeColor(mode),
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
        )
    }
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

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.movement_events_empty_title),
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.movement_events_empty_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ClearConfirmationDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.movement_events_clear_title)) },
        text = { Text(stringResource(R.string.movement_events_clear_message)) },
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
private fun getModeLabel(mode: TransportationMode): String = when (mode) {
    TransportationMode.WALKING -> stringResource(R.string.trip_mode_walking)
    TransportationMode.RUNNING -> stringResource(R.string.trip_mode_running)
    TransportationMode.CYCLING -> stringResource(R.string.trip_mode_cycling)
    TransportationMode.IN_VEHICLE -> stringResource(R.string.trip_mode_driving)
    TransportationMode.STATIONARY -> stringResource(R.string.trip_mode_stationary)
    TransportationMode.UNKNOWN -> stringResource(R.string.trip_mode_unknown)
}

private fun getModeColor(mode: TransportationMode): Color = when (mode) {
    TransportationMode.WALKING -> Color(0xFF4CAF50)
    TransportationMode.RUNNING -> Color(0xFFF44336)
    TransportationMode.CYCLING -> Color(0xFF2196F3)
    TransportationMode.IN_VEHICLE -> Color(0xFF9C27B0)
    TransportationMode.STATIONARY -> Color(0xFF607D8B)
    TransportationMode.UNKNOWN -> Color(0xFF9E9E9E)
}

private fun getConfidenceColor(confidence: Float): Color = when {
    confidence >= 0.9f -> Color(0xFF4CAF50)
    confidence >= 0.7f -> Color(0xFFFFC107)
    else -> Color(0xFFF44336)
}

private fun formatTime(dateTime: kotlinx.datetime.LocalDateTime): String =
    String.format("%02d:%02d:%02d", dateTime.hour, dateTime.minute, dateTime.second)

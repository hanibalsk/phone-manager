@file:Suppress("DEPRECATION") // hiltViewModel() deprecation - using stable API

package three.two.bit.phonemanager.ui.debug

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import three.two.bit.phonemanager.R
import three.two.bit.phonemanager.movement.BluetoothDeviceInfo
import three.two.bit.phonemanager.movement.DebugDetectionState
import three.two.bit.phonemanager.movement.TransportationMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Movement Debug Screen
 *
 * Displays real-time debug information about transportation detection
 * for diagnosing issues with car detection, movement detection, and Android Auto.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovementDebugScreen(
    onNavigateBack: () -> Unit,
    viewModel: MovementDebugViewModel = hiltViewModel(),
) {
    val debugState by viewModel.debugState.collectAsState()
    val isMonitoring by viewModel.isMonitoring.collectAsState()
    val isLiveUpdating by viewModel.isLiveUpdating.collectAsState()
    val eventHistory by viewModel.eventHistory.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.movement_debug_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                actions = {
                    // Live/Pause toggle
                    IconButton(onClick = { viewModel.toggleLiveUpdates() }) {
                        Icon(
                            imageVector = if (isLiveUpdating) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isLiveUpdating) {
                                stringResource(R.string.movement_debug_pause)
                            } else {
                                stringResource(R.string.movement_debug_resume)
                            },
                        )
                    }
                    // Restart monitoring
                    IconButton(onClick = { viewModel.restartMonitoring() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.movement_debug_restart),
                        )
                    }
                    // Clear history
                    IconButton(onClick = { viewModel.clearHistory() }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.movement_debug_clear),
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Monitoring status
            item {
                MonitoringStatusCard(isMonitoring = isMonitoring, isLive = isLiveUpdating)
            }

            // Current transportation state
            item {
                CurrentStateCard(debugState = debugState)
            }

            // Activity Recognition details
            item {
                ActivityRecognitionCard(debugState = debugState)
            }

            // Bluetooth Car Detection details
            item {
                BluetoothCarCard(debugState = debugState)
            }

            // Android Auto details
            item {
                AndroidAutoCard(debugState = debugState)
            }

            // Event history section header
            item {
                Text(
                    text = stringResource(R.string.movement_debug_event_history),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            // Event history list
            items(eventHistory.take(50)) { event ->
                EventHistoryItem(event = event)
            }

            // Bottom spacing
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun MonitoringStatusCard(isMonitoring: Boolean, isLive: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isMonitoring) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.errorContainer
            },
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(
                        if (isMonitoring && isLive) Color.Green else if (isMonitoring) Color.Yellow else Color.Red,
                    ),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isMonitoring) {
                        stringResource(R.string.movement_debug_monitoring_active)
                    } else {
                        stringResource(R.string.movement_debug_monitoring_inactive)
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = if (isLive) {
                        stringResource(R.string.movement_debug_live_updates)
                    } else {
                        stringResource(R.string.movement_debug_updates_paused)
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun CurrentStateCard(debugState: DebugDetectionState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text(
                text = stringResource(R.string.movement_debug_current_state),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = getModeIcon(debugState.transportationState.mode),
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = debugState.transportationState.mode.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Source: ${debugState.transportationState.source.name}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "In Vehicle: ${debugState.transportationState.isInVehicle}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ActivityRecognitionCard(debugState: DebugDetectionState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.movement_debug_activity_recognition),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                StatusIndicator(isActive = debugState.isActivityRecognitionMonitoring)
            }

            Spacer(modifier = Modifier.height(8.dp))

            DebugRow(label = "Mode", value = debugState.activityMode.name)
            DebugRow(label = "Confidence", value = "${debugState.activityConfidence}%")

            debugState.activityDebugInfo?.let { info ->
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    text = "Raw Activities:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                info.allActivities.forEach { (name, confidence) ->
                    Text(
                        text = "  $name: $confidence%",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }
        }
    }
}

@Composable
private fun BluetoothCarCard(debugState: DebugDetectionState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Bluetooth,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.movement_debug_bluetooth_car),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
                StatusIndicator(isActive = debugState.isBluetoothMonitoring)
            }

            Spacer(modifier = Modifier.height(8.dp))

            DebugRow(label = "Connected to Car", value = debugState.isBluetoothCarConnected.toString())
            debugState.connectedCarDeviceName?.let { name ->
                DebugRow(label = "Car Device", value = name)
            }

            if (debugState.connectedAudioDevices.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    text = "Connected Audio Devices:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                debugState.connectedAudioDevices.forEach { device ->
                    BluetoothDeviceRow(device = device)
                }
            }

            if (debugState.potentialCarDevices.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    text = "Potential Car Devices (not recognized):",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.error,
                )
                debugState.potentialCarDevices.forEach { device ->
                    BluetoothDeviceRow(device = device)
                }
            }
        }
    }
}

@Composable
private fun BluetoothDeviceRow(device: BluetoothDeviceInfo) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, top = 4.dp),
    ) {
        Text(
            text = device.name ?: "(unknown name)",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = "Profile: ${device.profile}, Class: ${device.deviceClass}, Major: ${device.majorClass}",
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp,
        )
    }
}

@Composable
private fun AndroidAutoCard(debugState: DebugDetectionState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.movement_debug_android_auto),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
                StatusIndicator(isActive = debugState.isAndroidAutoMonitoring)
            }

            Spacer(modifier = Modifier.height(8.dp))

            DebugRow(label = "In Car Mode", value = debugState.isAndroidAutoActive.toString())
            DebugRow(label = "Detection Source", value = debugState.androidAutoSource)
            if (debugState.androidAutoLastCheck > 0) {
                DebugRow(
                    label = "Last Check",
                    value = formatTimestamp(debugState.androidAutoLastCheck),
                )
            }
        }
    }
}

@Composable
private fun StatusIndicator(isActive: Boolean) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(if (isActive) Color(0xFF4CAF50) else Color(0xFFE53935))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(
            text = if (isActive) "ON" else "OFF",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun DebugRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun EventHistoryItem(event: DebugEvent) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = formatTime(event.timestamp),
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(60.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = event.message,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
        )
    }
}

private fun getModeIcon(mode: TransportationMode): ImageVector = when (mode) {
    TransportationMode.WALKING -> Icons.AutoMirrored.Filled.DirectionsWalk
    TransportationMode.RUNNING -> Icons.AutoMirrored.Filled.DirectionsRun
    TransportationMode.CYCLING -> Icons.AutoMirrored.Filled.DirectionsBike
    TransportationMode.IN_VEHICLE -> Icons.Default.DirectionsCar
    TransportationMode.STATIONARY -> Icons.Default.LocationOn
    TransportationMode.UNKNOWN -> Icons.Default.QuestionMark
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

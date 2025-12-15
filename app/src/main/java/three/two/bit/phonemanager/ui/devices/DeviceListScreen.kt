package three.two.bit.phonemanager.ui.devices

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import three.two.bit.phonemanager.R
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.time.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import three.two.bit.phonemanager.domain.model.UserDevice

/**
 * Story E10.6 Task 4: Device List Screen
 *
 * AC E10.6.1: Device List Screen
 * - List of all devices linked to user's account
 * - Device display name, partial ID, last seen timestamp
 * - Current device indicator (highlighted)
 * - "Link New Device" FAB
 * - Pull-to-refresh functionality
 *
 * @param viewModel The DeviceManagementViewModel
 * @param onNavigateBack Callback to navigate back
 * @param onNavigateToDeviceDetail Callback to navigate to device detail
 * @param onLinkDevice Callback when user wants to link current device
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceListScreen(
    viewModel: DeviceManagementViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToDeviceDetail: (UserDevice) -> Unit,
    onLinkDevice: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val operationResult by viewModel.operationResult.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var isRefreshing by remember { mutableStateOf(false) }
    var showLinkDialog by remember { mutableStateOf(false) }

    // Handle operation results
    LaunchedEffect(operationResult) {
        when (val result = operationResult) {
            is DeviceOperationResult.Success -> {
                snackbarHostState.showSnackbar(result.message)
                viewModel.clearOperationResult()
            }
            is DeviceOperationResult.Error -> {
                snackbarHostState.showSnackbar(result.message)
                viewModel.clearOperationResult()
            }
            else -> {}
        }
    }

    // Handle refresh completion
    LaunchedEffect(uiState) {
        if (uiState !is DeviceUiState.Loading) {
            isRefreshing = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.devices_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
            )
        },
        floatingActionButton = {
            // Only show FAB if current device is not linked
            if (!viewModel.isCurrentDeviceLinked()) {
                FloatingActionButton(
                    onClick = { showLinkDialog = true },
                ) {
                    Icon(Icons.Default.Add, stringResource(R.string.link_device_title))
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                viewModel.refreshDevices()
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            when (val state = uiState) {
                is DeviceUiState.Loading -> {
                    LoadingState()
                }
                is DeviceUiState.Success -> {
                    DeviceList(
                        devices = state.devices,
                        currentDeviceId = state.currentDeviceId,
                        onDeviceClick = { device ->
                            viewModel.selectDevice(device)
                            onNavigateToDeviceDetail(device)
                        },
                    )
                }
                is DeviceUiState.Empty -> {
                    EmptyState(onLinkDevice = { showLinkDialog = true })
                }
                is DeviceUiState.Error -> {
                    ErrorState(
                        message = state.message,
                        onRetry = { viewModel.refreshDevices() },
                    )
                }
            }
        }
    }

    // Link Device Dialog
    if (showLinkDialog) {
        LinkDeviceDialog(
            onDismiss = { showLinkDialog = false },
            onConfirm = { displayName, isPrimary ->
                showLinkDialog = false
                viewModel.linkCurrentDevice(displayName, isPrimary)
            },
        )
    }
}

/**
 * Device list with cards
 */
@Composable
private fun DeviceList(
    devices: List<UserDevice>,
    currentDeviceId: String,
    onDeviceClick: (UserDevice) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }

        items(devices, key = { it.id }) { device ->
            DeviceCard(
                device = device,
                isCurrentDevice = device.isCurrentDevice(currentDeviceId),
                onClick = { onDeviceClick(device) },
            )
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }
    }
}

/**
 * AC E10.6.1: Device card showing name, partial ID, last seen, and current device indicator
 */
@Composable
private fun DeviceCard(
    device: UserDevice,
    isCurrentDevice: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = if (isCurrentDevice) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            )
        } else {
            CardDefaults.cardColors()
        },
        border = if (isCurrentDevice) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            null
        },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Device icon
            Icon(
                imageVector = Icons.Default.PhoneAndroid,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = if (isCurrentDevice) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Device info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = device.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )

                    // Primary device indicator
                    if (device.isPrimary) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = stringResource(R.string.devices_primary_device),
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }

                    // Current device indicator
                    if (isCurrentDevice) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.devices_this_device),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                // Partial device ID
                Text(
                    text = formatPartialDeviceId(device.deviceUuid),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                // Last seen timestamp
                device.lastSeenAt?.let { lastSeen ->
                    Text(
                        text = stringResource(R.string.last_seen, formatTimestamp(lastSeen)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/**
 * Loading state with spinner
 */
@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

/**
 * Empty state when no devices are linked
 */
@Composable
private fun EmptyState(onLinkDevice: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                imageVector = Icons.Default.PhoneAndroid,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text(
                text = stringResource(R.string.devices_empty_title),
                style = MaterialTheme.typography.titleLarge,
            )

            Text(
                text = stringResource(R.string.devices_empty_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            androidx.compose.material3.Button(onClick = onLinkDevice) {
                Icon(Icons.Default.Add, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.link_device_title))
            }
        }
    }
}

/**
 * Error state with retry option
 */
@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
            )

            androidx.compose.material3.Button(onClick = onRetry) {
                Text(stringResource(R.string.button_retry))
            }
        }
    }
}

/**
 * Format device UUID to show partial ID (e.g., "abc123...xyz789")
 */
private fun formatPartialDeviceId(deviceId: String): String {
    return if (deviceId.length > 12) {
        "${deviceId.take(6)}...${deviceId.takeLast(6)}"
    } else {
        deviceId
    }
}

/**
 * Format timestamp for display
 */
private fun formatTimestamp(instant: Instant): String {
    val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    val now = Clock.System.now()
        .toLocalDateTime(TimeZone.currentSystemDefault())

    return when {
        localDateTime.date == now.date -> {
            // Today: show time only
            String.format(
                "%02d:%02d",
                localDateTime.hour,
                localDateTime.minute
            )
        }
        localDateTime.date.dayOfYear == now.date.dayOfYear - 1 &&
            localDateTime.date.year == now.date.year -> {
            // Yesterday
            "Yesterday"
        }
        else -> {
            // Other dates: show date
            String.format(
                "%d/%d/%d",
                localDateTime.monthNumber,
                localDateTime.dayOfMonth,
                localDateTime.year
            )
        }
    }
}

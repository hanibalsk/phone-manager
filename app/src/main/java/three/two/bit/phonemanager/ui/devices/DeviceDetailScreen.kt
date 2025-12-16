package three.two.bit.phonemanager.ui.devices

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapHoriz
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import three.two.bit.phonemanager.R
import three.two.bit.phonemanager.domain.model.UserDevice

/**
 * Story E10.6 Task 5: Device Detail Screen
 *
 * AC E10.6.3: Device Detail View
 * - Full device ID
 * - Display name (editable)
 * - Last activity timestamp
 * - "Unlink Device" button
 * - "Transfer Ownership" button (if user is owner)
 *
 * AC E10.6.4: Device Unlink with confirmation dialog
 * AC E10.6.5: Transfer Ownership with email input dialog
 *
 * @param viewModel The DeviceManagementViewModel
 * @param onNavigateBack Callback to navigate back
 * @param onDeviceUnlinked Callback when current device is unlinked (need to clear auth)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceDetailScreen(
    viewModel: DeviceManagementViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onDeviceUnlinked: () -> Unit = {},
) {
    val detailUiState by viewModel.detailUiState.collectAsStateWithLifecycle()
    val operationResult by viewModel.operationResult.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var showUnlinkDialog by remember { mutableStateOf(false) }
    var showTransferDialog by remember { mutableStateOf(false) }
    var showEditNameDialog by remember { mutableStateOf(false) }

    // Handle operation results
    LaunchedEffect(operationResult) {
        when (val result = operationResult) {
            is DeviceOperationResult.Success -> {
                snackbarHostState.showSnackbar(result.message)
                viewModel.clearOperationResult()

                // Check if we unlinked the current device
                if (result.message.contains("signed out", ignoreCase = true)) {
                    onDeviceUnlinked()
                }
            }
            is DeviceOperationResult.Error -> {
                snackbarHostState.showSnackbar(result.message)
                viewModel.clearOperationResult()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.device_details)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        when (val state = detailUiState) {
            is DeviceDetailUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            is DeviceDetailUiState.Success -> {
                DeviceDetailContent(
                    device = state.device,
                    isCurrentDevice = state.isCurrentDevice,
                    isOwner = state.isOwner,
                    onUnlinkClick = { showUnlinkDialog = true },
                    onTransferClick = { showTransferDialog = true },
                    onEditNameClick = { showEditNameDialog = true },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                )
            }
            is DeviceDetailUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onNavigateBack) {
                            Text(stringResource(R.string.group_go_back))
                        }
                    }
                }
            }
        }
    }

    // Unlink confirmation dialog
    if (showUnlinkDialog && detailUiState is DeviceDetailUiState.Success) {
        val device = (detailUiState as DeviceDetailUiState.Success).device
        val isCurrentDevice = (detailUiState as DeviceDetailUiState.Success).isCurrentDevice

        UnlinkDeviceDialog(
            deviceName = device.displayName,
            isCurrentDevice = isCurrentDevice,
            onDismiss = { showUnlinkDialog = false },
            onConfirm = {
                showUnlinkDialog = false
                val shouldNavigateAway = viewModel.unlinkDevice(device.deviceUuid)
                if (!shouldNavigateAway) {
                    // If not unlinking current device, go back to list
                    onNavigateBack()
                }
            },
        )
    }

    // Transfer ownership dialog
    if (showTransferDialog && detailUiState is DeviceDetailUiState.Success) {
        val device = (detailUiState as DeviceDetailUiState.Success).device

        TransferOwnershipDialog(
            deviceName = device.displayName,
            onDismiss = { showTransferDialog = false },
            onConfirm = { newOwnerId ->
                showTransferDialog = false
                viewModel.transferDevice(device.deviceUuid, newOwnerId)
            },
        )
    }

    // Edit name dialog
    if (showEditNameDialog && detailUiState is DeviceDetailUiState.Success) {
        val device = (detailUiState as DeviceDetailUiState.Success).device

        EditDeviceNameDialog(
            currentName = device.displayName,
            onDismiss = { showEditNameDialog = false },
            onConfirm = { newName ->
                showEditNameDialog = false
                viewModel.updateDeviceName(device.deviceUuid, newName)
            },
        )
    }
}

/**
 * Device detail content
 */
@Composable
private fun DeviceDetailContent(
    device: UserDevice,
    isCurrentDevice: Boolean,
    isOwner: Boolean,
    onUnlinkClick: () -> Unit,
    onTransferClick: () -> Unit,
    onEditNameClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Device header
        DeviceHeader(
            device = device,
            isCurrentDevice = isCurrentDevice,
            onEditNameClick = onEditNameClick,
        )

        // Device information card
        DeviceInfoCard(device = device)

        // Actions
        Spacer(modifier = Modifier.height(8.dp))

        // Unlink button
        OutlinedButton(
            onClick = onUnlinkClick,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error,
            ),
        ) {
            Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (isCurrentDevice) stringResource(R.string.device_unlink_this) else stringResource(R.string.device_unlink))
        }

        // Transfer button (only for owners)
        if (isOwner) {
            OutlinedButton(
                onClick = onTransferClick,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.SwapHoriz, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.device_transfer_title))
            }
        }

        // Warning for current device
        if (isCurrentDevice) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                ),
            ) {
                Text(
                    text = stringResource(R.string.device_unlink_current_device_warning),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(12.dp),
                )
            }
        }
    }
}

/**
 * Device header with icon and name
 */
@Composable
private fun DeviceHeader(
    device: UserDevice,
    isCurrentDevice: Boolean,
    onEditNameClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.PhoneAndroid,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = if (isCurrentDevice) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = device.displayName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )

                if (device.isPrimary) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = stringResource(R.string.device_primary_content_desc),
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            if (isCurrentDevice) {
                Text(
                    text = stringResource(R.string.device_this_device_label),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        // Edit name button
        IconButton(onClick = onEditNameClick) {
            Icon(Icons.Default.Edit, stringResource(R.string.device_edit_name_content_desc))
        }
    }
}

/**
 * Device information card with full details
 */
@Composable
private fun DeviceInfoCard(device: UserDevice) {
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.device_info_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            InfoRow(label = stringResource(R.string.device_info_device_id_label), value = device.deviceUuid)
            InfoRow(
                label = stringResource(R.string.device_info_platform_label),
                value = device.platform.replaceFirstChar { it.uppercase() },
            )
            InfoRow(
                label = stringResource(R.string.device_info_status_label),
                value = if (device.active) stringResource(R.string.device_status_active) else stringResource(R.string.device_status_inactive),
            )

            device.linkedAt?.let { linkedAt ->
                InfoRow(label = stringResource(R.string.device_info_linked_label), value = formatFullTimestamp(linkedAt))
            }

            device.lastSeenAt?.let { lastSeen ->
                InfoRow(label = stringResource(R.string.device_info_last_seen_label), value = formatFullTimestamp(lastSeen))
            }
        }
    }
}

/**
 * Information row with label and value
 */
@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

/**
 * AC E10.6.4: Unlink device confirmation dialog
 */
@Composable
private fun UnlinkDeviceDialog(
    deviceName: String,
    isCurrentDevice: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.device_unlink_title))
        },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.device_unlink_message, deviceName),
                )

                if (isCurrentDevice) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.device_unlink_warning),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text(stringResource(R.string.device_unlink))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

/**
 * AC E10.6.5: Transfer ownership dialog with user ID input
 */
@Composable
private fun TransferOwnershipDialog(
    deviceName: String,
    onDismiss: () -> Unit,
    onConfirm: (newOwnerId: String) -> Unit,
) {
    var newOwnerId by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.device_transfer_title))
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.device_transfer_description, deviceName),
                )

                OutlinedTextField(
                    value = newOwnerId,
                    onValueChange = {
                        newOwnerId = it
                        showError = false
                    },
                    label = { Text(stringResource(R.string.device_new_owner_label)) },
                    placeholder = { Text(stringResource(R.string.device_new_owner_hint)) },
                    singleLine = true,
                    isError = showError,
                    supportingText = if (showError) {
                        { Text(stringResource(R.string.device_invalid_user_id)) }
                    } else {
                        null
                    },
                    modifier = Modifier.fillMaxWidth(),
                )

                Text(
                    text = stringResource(R.string.device_transfer_help),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (newOwnerId.isBlank()) {
                        showError = true
                    } else {
                        onConfirm(newOwnerId.trim())
                    }
                },
            ) {
                Text(stringResource(R.string.device_transfer_title))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

/**
 * Edit device name dialog
 */
@Composable
private fun EditDeviceNameDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (newName: String) -> Unit,
) {
    var newName by remember { mutableStateOf(currentName) }
    var showError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.device_edit_name_title))
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.device_edit_name_description),
                )

                OutlinedTextField(
                    value = newName,
                    onValueChange = {
                        newName = it
                        showError = false
                    },
                    label = { Text(stringResource(R.string.device_display_name_label)) },
                    singleLine = true,
                    isError = showError,
                    supportingText = if (showError) {
                        { Text(stringResource(R.string.device_name_required)) }
                    } else {
                        null
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (newName.isBlank()) {
                        showError = true
                    } else {
                        onConfirm(newName.trim())
                    }
                },
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

/**
 * Format timestamp for full display
 */
private fun formatFullTimestamp(instant: Instant): String {
    val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    return String.format(
        "%d/%d/%d %02d:%02d",
        localDateTime.monthNumber,
        localDateTime.day,
        localDateTime.year,
        localDateTime.hour,
        localDateTime.minute,
    )
}

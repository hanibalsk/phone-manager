@file:Suppress("DEPRECATION") // hiltViewModel() deprecation - using stable API

package three.two.bit.phonemanager.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import three.two.bit.phonemanager.R
import three.two.bit.phonemanager.domain.model.DeviceStatusFilter
import three.two.bit.phonemanager.domain.model.MemberDeviceSettings

/**
 * Story E12.7: Member Devices Screen
 *
 * Lists all member devices in a group for admin settings management.
 *
 * AC E12.7.1: Device Settings List Screen
 * - List devices with owner info
 * - Search and filter functionality
 * - Online/offline status indicators
 * - Navigate to device settings
 * - Multi-select for bulk operations
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberDevicesScreen(
    onNavigateBack: () -> Unit,
    onDeviceClick: (deviceId: String) -> Unit,
    onBulkEditClick: (deviceIds: List<String>) -> Unit,
    viewModel: MemberDevicesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (uiState.isInBulkEditMode) {
                        Text(stringResource(R.string.admin_selected_count, uiState.selectedCount))
                    } else {
                        Text(stringResource(R.string.admin_member_devices))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (uiState.isInBulkEditMode) {
                            viewModel.exitBulkEditMode()
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(
                            imageVector = if (uiState.isInBulkEditMode) {
                                Icons.Default.Clear
                            } else {
                                Icons.AutoMirrored.Filled.ArrowBack
                            },
                            contentDescription = if (uiState.isInBulkEditMode) {
                                stringResource(R.string.admin_cancel_selection)
                            } else {
                                stringResource(R.string.back)
                            },
                        )
                    }
                },
                actions = {
                    if (uiState.isInBulkEditMode) {
                        TextButton(
                            onClick = { viewModel.selectAllDevices() },
                            enabled = uiState.selectedCount < uiState.filteredDevices.size,
                        ) {
                            Text(stringResource(R.string.admin_select_all))
                        }
                    } else {
                        IconButton(onClick = { viewModel.refresh() }) {
                            Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.refresh))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (uiState.isInBulkEditMode) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                ),
            )
        },
        floatingActionButton = {
            if (uiState.isInBulkEditMode && uiState.hasSelection) {
                FloatingActionButton(
                    onClick = {
                        onBulkEditClick(uiState.selectedDevices.toList())
                    },
                ) {
                    Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.admin_bulk_edit))
                }
            } else if (!uiState.isInBulkEditMode && uiState.devices.isNotEmpty()) {
                FloatingActionButton(
                    onClick = { viewModel.enterBulkEditMode() },
                ) {
                    Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.admin_bulk_edit_mode))
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            // Search bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text(stringResource(R.string.admin_search_placeholder)) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                            Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.admin_clear_search))
                        }
                    }
                },
                singleLine = true,
            )

            // Filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = uiState.statusFilter == DeviceStatusFilter.ALL,
                    onClick = { viewModel.onStatusFilterChanged(DeviceStatusFilter.ALL) },
                    label = { Text(stringResource(R.string.admin_filter_all, uiState.devices.size)) },
                )
                FilterChip(
                    selected = uiState.statusFilter == DeviceStatusFilter.ONLINE,
                    onClick = { viewModel.onStatusFilterChanged(DeviceStatusFilter.ONLINE) },
                    label = { Text(stringResource(R.string.admin_filter_online, uiState.onlineCount)) },
                )
                FilterChip(
                    selected = uiState.statusFilter == DeviceStatusFilter.OFFLINE,
                    onClick = { viewModel.onStatusFilterChanged(DeviceStatusFilter.OFFLINE) },
                    label = { Text(stringResource(R.string.admin_filter_offline, uiState.offlineCount)) },
                )
            }

            // Device list
            PullToRefreshBox(
                isRefreshing = uiState.isLoading,
                onRefresh = { viewModel.refresh() },
                modifier = Modifier.fillMaxSize(),
            ) {
                if (uiState.filteredDevices.isEmpty() && !uiState.isLoading) {
                    EmptyDeviceList(
                        message = if (uiState.searchQuery.isNotEmpty()) {
                            stringResource(R.string.admin_no_devices_match_search)
                        } else {
                            stringResource(R.string.admin_no_devices_in_group)
                        },
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(
                            items = uiState.filteredDevices,
                            key = { it.deviceId },
                        ) { device ->
                            MemberDeviceCard(
                                device = device,
                                isSelected = uiState.selectedDevices.contains(device.deviceId),
                                isInBulkEditMode = uiState.isInBulkEditMode,
                                onClick = {
                                    if (uiState.isInBulkEditMode) {
                                        viewModel.toggleDeviceSelection(device.deviceId)
                                    } else {
                                        onDeviceClick(device.deviceId)
                                    }
                                },
                                onLongClick = {
                                    if (!uiState.isInBulkEditMode) {
                                        viewModel.enterBulkEditMode()
                                        viewModel.toggleDeviceSelection(device.deviceId)
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MemberDeviceCard(
    device: MemberDeviceSettings,
    isSelected: Boolean,
    isInBulkEditMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 1.dp,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Selection checkbox (in bulk edit mode)
            if (isInBulkEditMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() },
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            // Online indicator
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(
                        if (device.isOnline) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline
                        },
                    ),
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Device info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.deviceName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = device.ownerName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = device.ownerEmail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Lock indicator
            if (device.lockedCount() > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = stringResource(R.string.admin_locked_settings),
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${device.lockedCount()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyDeviceList(message: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.outline,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

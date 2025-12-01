package three.two.bit.phonemanager.ui.unlock

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pending
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import three.two.bit.phonemanager.domain.model.UnlockRequest
import three.two.bit.phonemanager.domain.model.UnlockRequestFilter
import three.two.bit.phonemanager.domain.model.UnlockRequestStatus
import three.two.bit.phonemanager.domain.model.UnlockRequestSummary

/**
 * Story E12.8: Unlock Requests Screen
 *
 * Displays list of unlock requests for a device.
 *
 * AC E12.8.3: View My Unlock Requests
 * AC E12.8.4: Withdraw Unlock Request
 * AC E12.8.6: Admin Response Display
 * AC E12.8.7: Filter by status
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnlockRequestsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: UnlockRequestViewModel = hiltViewModel(),
) {
    val requests by viewModel.filteredRequests.collectAsState()
    val summary by viewModel.requestSummary.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val currentFilter by viewModel.currentFilter.collectAsState()
    val selectedRequest by viewModel.selectedRequest.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    // Show error messages
    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    // Show success messages
    LaunchedEffect(successMessage) {
        successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSuccessMessage()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Unlock Requests") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate back",
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = isLoading,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
            ) {
                // Summary badges
                RequestSummaryRow(
                    summary = summary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )

                // Filter chips
                FilterChipsRow(
                    currentFilter = currentFilter,
                    onFilterChange = viewModel::setFilter,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Request list
                if (requests.isEmpty() && !isLoading) {
                    EmptyRequestsState(
                        filter = currentFilter,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(requests, key = { it.id }) { request ->
                            RequestCard(
                                request = request,
                                onClick = { viewModel.selectRequest(request) },
                                onWithdraw = { viewModel.withdrawRequest(request.id) },
                            )
                        }
                    }
                }
            }
        }
    }

    // Request detail dialog
    selectedRequest?.let { request ->
        RequestDetailDialog(
            request = request,
            onDismiss = { viewModel.clearSelectedRequest() },
            onWithdraw = {
                viewModel.withdrawRequest(request.id)
            },
        )
    }
}

/**
 * Summary row showing counts by status.
 * AC E12.8.10: Show badge with pending request count
 */
@Composable
private fun RequestSummaryRow(
    summary: UnlockRequestSummary,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SummaryBadge(
            count = summary.pendingCount,
            label = "Pending",
            color = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.weight(1f),
        )
        SummaryBadge(
            count = summary.approvedCount,
            label = "Approved",
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
        )
        SummaryBadge(
            count = summary.deniedCount,
            label = "Denied",
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SummaryBadge(
    count: Int,
    label: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleLarge,
                color = color,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = color,
            )
        }
    }
}

/**
 * Filter chips for request status.
 * AC E12.8.7: Filter by status
 */
@Composable
private fun FilterChipsRow(
    currentFilter: UnlockRequestFilter,
    onFilterChange: (UnlockRequestFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        UnlockRequestFilter.entries.forEach { filter ->
            FilterChip(
                selected = currentFilter == filter,
                onClick = { onFilterChange(filter) },
                label = {
                    Text(
                        text = when (filter) {
                            UnlockRequestFilter.ALL -> "All"
                            UnlockRequestFilter.PENDING -> "Pending"
                            UnlockRequestFilter.APPROVED -> "Approved"
                            UnlockRequestFilter.DENIED -> "Denied"
                            UnlockRequestFilter.WITHDRAWN -> "Withdrawn"
                        },
                    )
                },
                leadingIcon = if (currentFilter == filter) {
                    {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(FilterChipDefaults.IconSize),
                        )
                    }
                } else {
                    null
                },
            )
        }
    }
}

/**
 * Card displaying a single unlock request.
 * AC E12.8.3: Shows status badge, timestamp, setting name
 */
@Composable
private fun RequestCard(
    request: UnlockRequest,
    onClick: () -> Unit,
    onWithdraw: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
    ) {
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = request.getSettingDisplayName(),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                StatusChip(status = request.status)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = request.reason,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = formatInstant(request.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                if (request.canWithdraw()) {
                    TextButton(onClick = onWithdraw) {
                        Icon(
                            imageVector = Icons.Default.Undo,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Withdraw")
                    }
                }
            }

            // Show admin response if decided
            if (request.isDecided()) {
                Spacer(modifier = Modifier.height(8.dp))
                AdminResponseSection(request = request)
            }
        }
    }
}

/**
 * Status chip with icon and color.
 * AC E12.8.3: Status badge
 */
@Composable
private fun StatusChip(
    status: UnlockRequestStatus,
    modifier: Modifier = Modifier,
) {
    val (icon, color, text) = when (status) {
        UnlockRequestStatus.PENDING -> Triple(
            Icons.Default.Pending,
            MaterialTheme.colorScheme.tertiary,
            "Pending",
        )
        UnlockRequestStatus.APPROVED -> Triple(
            Icons.Default.Check,
            MaterialTheme.colorScheme.primary,
            "Approved",
        )
        UnlockRequestStatus.DENIED -> Triple(
            Icons.Default.Clear,
            MaterialTheme.colorScheme.error,
            "Denied",
        )
        UnlockRequestStatus.WITHDRAWN -> Triple(
            Icons.Default.Undo,
            MaterialTheme.colorScheme.outline,
            "Withdrawn",
        )
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f),
        ),
    ) {
        Row(
            modifier = modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(14.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = color,
            )
        }
    }
}

/**
 * Admin response section.
 * AC E12.8.6: Admin Response Display
 */
@Composable
private fun AdminResponseSection(
    request: UnlockRequest,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (request.isApproved()) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            },
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Response from ",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = request.respondedByName ?: "Admin",
                    style = MaterialTheme.typography.labelMedium,
                )
            }

            if (!request.response.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = request.response,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            request.respondedAt?.let { respondedAt ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatInstant(respondedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * Empty state when no requests match filter.
 */
@Composable
private fun EmptyRequestsState(
    filter: UnlockRequestFilter,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.outline,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = when (filter) {
                    UnlockRequestFilter.ALL -> "No unlock requests"
                    UnlockRequestFilter.PENDING -> "No pending requests"
                    UnlockRequestFilter.APPROVED -> "No approved requests"
                    UnlockRequestFilter.DENIED -> "No denied requests"
                    UnlockRequestFilter.WITHDRAWN -> "No withdrawn requests"
                },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Unlock requests you create will appear here",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

/**
 * Format Instant to readable string.
 */
private fun formatInstant(instant: Instant): String {
    val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    return "${localDateTime.monthNumber}/${localDateTime.dayOfMonth}/${localDateTime.year} " +
        "${localDateTime.hour.toString().padStart(2, '0')}:${localDateTime.minute.toString().padStart(2, '0')}"
}

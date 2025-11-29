package three.two.bit.phonemanager.ui.alerts

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import three.two.bit.phonemanager.R
import three.two.bit.phonemanager.domain.model.AlertDirection
import three.two.bit.phonemanager.domain.model.ProximityAlert

/**
 * Story E5.1: Alerts Screen
 *
 * Displays list of proximity alerts with management options
 * AC E5.1.5: View list, create, edit, delete, enable/disable alerts
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsScreen(
    viewModel: AlertsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToCreate: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val alerts by viewModel.alerts.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Proximity Alerts") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCreate,
                containerColor = MaterialTheme.colorScheme.primary,
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.alerts_create))
            }
        },
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = uiState.isSyncing,
            onRefresh = viewModel::refresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            when {
                // Initial loading
                uiState.isSyncing && alerts.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                }
                // Empty state
                alerts.isEmpty() -> {
                    EmptyAlertsContent(
                        modifier = Modifier.fillMaxSize(),
                        onCreateClick = onNavigateToCreate,
                    )
                }
                // Alert list
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    ) {
                        items(
                            items = alerts,
                            key = { it.id },
                        ) { alert ->
                            SwipeableAlertItem(
                                alert = alert,
                                onToggleActive = { viewModel.toggleAlertActive(alert.id, it) },
                                onDelete = { viewModel.deleteAlert(alert.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Swipeable alert item with delete action
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableAlertItem(alert: ProximityAlert, onToggleActive: (Boolean) -> Unit, onDelete: () -> Unit) {
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
        AlertCard(
            alert = alert,
            onToggleActive = onToggleActive,
        )
    }
}

/**
 * Alert card displaying alert information
 */
@Composable
private fun AlertCard(alert: ProximityAlert, onToggleActive: (Boolean) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (alert.active) {
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
            // Icon
            Icon(
                imageVector = if (alert.active) {
                    Icons.Default.NotificationsActive
                } else {
                    Icons.Default.NotificationsOff
                },
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = if (alert.active) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outline
                },
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Alert info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = alert.targetDisplayName ?: "Device ${alert.targetDeviceId.take(8)}...",
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatAlertDescription(alert),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Toggle switch
            Switch(
                checked = alert.active,
                onCheckedChange = onToggleActive,
            )
        }
    }
}

/**
 * Format alert description string
 */
private fun formatAlertDescription(alert: ProximityAlert): String {
    val radiusText = when {
        alert.radiusMeters >= 1000 -> "${alert.radiusMeters / 1000}km"
        else -> "${alert.radiusMeters}m"
    }
    val directionText = when (alert.direction) {
        AlertDirection.ENTER -> "enters"
        AlertDirection.EXIT -> "exits"
        AlertDirection.BOTH -> "enters/exits"
    }
    return "Notify when $directionText within $radiusText"
}

/**
 * Empty state when no alerts exist
 */
@Composable
private fun EmptyAlertsContent(modifier: Modifier = Modifier, onCreateClick: () -> Unit) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Default.NotificationsOff,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.alerts_empty_title),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.alerts_empty_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(24.dp))
        androidx.compose.material3.Button(onClick = onCreateClick) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Create Alert")
        }
    }
}

@file:Suppress("DEPRECATION") // hiltViewModel() deprecation - using stable API

package three.two.bit.phonemanager.ui.group

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import three.two.bit.phonemanager.R
import three.two.bit.phonemanager.domain.model.Device
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/**
 * Story E1.2: Group Members Screen
 *
 * Displays list of devices in the same group
 * ACs: E1.2.3, E1.2.4, E1.2.5, E1.2.6
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupMembersScreen(viewModel: GroupMembersViewModel = hiltViewModel(), onNavigateBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.group_members_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
            )
        },
    ) { paddingValues ->
        // Only show pull-to-refresh indicator when refreshing existing data (not initial load)
        val isRefreshing = uiState.isLoading && uiState.members.isNotEmpty()

        // Pull-to-refresh wrapper (AC E1.2.6)
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            when {
                // Show loading state for initial load (AC E1.2.3)
                uiState.isLoading && uiState.members.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
                // Show error state with retry (AC E1.2.5)
                uiState.error != null -> {
                    ErrorContent(
                        message = uiState.error!!,
                        onRetry = viewModel::refresh,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                // Show empty state (AC E1.2.4)
                uiState.isEmpty -> {
                    EmptyGroupContent(
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                // Show list of devices (AC E1.2.3)
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(uiState.members, key = { it.deviceId }) { device ->
                            DeviceCard(device = device)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Device card displaying device info (AC E1.2.3)
 */
@Composable
private fun DeviceCard(device: Device) {
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            // Display name (AC E1.2.3)
            Text(
                text = device.displayName,
                style = MaterialTheme.typography.titleMedium,
            )
            // Last seen time in human-readable format (AC E1.2.3)
            device.lastSeenAt?.let { lastSeen ->
                Text(
                    text = stringResource(R.string.last_seen, formatRelativeTime(lastSeen)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * Empty state when no other devices in group (AC E1.2.4)
 */
@Composable
private fun EmptyGroupContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Default.People,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(16.dp))
            // Empty state message (AC E1.2.4)
            Text(
                text = stringResource(R.string.group_no_members),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            // Instructions on how to add devices (AC E1.2.4)
            Text(
                text = stringResource(R.string.group_share_hint),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Error state with retry button (AC E1.2.5)
 */
@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.error,
            )
            Spacer(modifier = Modifier.height(16.dp))
            // Error message (AC E1.2.5)
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(16.dp))
            // Retry button (AC E1.2.5)
            Button(onClick = onRetry) {
                Text(stringResource(R.string.button_retry))
            }
        }
    }
}

/**
 * Format timestamp as relative time (AC E1.2.3)
 * Examples: "Just now", "2 min ago", "3h ago", "2d ago"
 */
@Composable
private fun formatRelativeTime(instant: Instant): String {
    val now = Clock.System.now()
    val duration = now - instant
    return when {
        duration < 1.minutes -> stringResource(R.string.time_just_now)
        duration < 1.hours -> stringResource(R.string.time_minutes_ago, duration.inWholeMinutes.toInt())
        duration < 1.days -> stringResource(R.string.time_hours_ago, duration.inWholeHours.toInt())
        duration < 7.days -> stringResource(R.string.time_days_ago, duration.inWholeDays.toInt())
        else -> {
            // For older dates, show the actual date
            instant.toLocalDateTime(TimeZone.currentSystemDefault())
                .date.toString()
        }
    }
}

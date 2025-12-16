package three.two.bit.phonemanager.ui.groups

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
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import three.two.bit.phonemanager.R
import three.two.bit.phonemanager.domain.model.ExpiryUrgency
import three.two.bit.phonemanager.domain.model.GroupInvite

/**
 * Story E11.9 Task 7: PendingInvitesScreen
 *
 * AC E11.9.6: Pending Invites List
 * AC E11.9.7: Revoke Invite
 *
 * Shows list of all pending invites for a group with management options.
 *
 * @param viewModel The InviteViewModel (shared with InviteMembersScreen)
 * @param onNavigateBack Callback to navigate back
 * @param onInviteClick Callback when an invite is clicked (to view details)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PendingInvitesScreen(
    viewModel: InviteViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onInviteClick: (GroupInvite) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val invites by viewModel.invites.collectAsStateWithLifecycle()
    val operationResult by viewModel.operationResult.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var inviteToRevoke by remember { mutableStateOf<GroupInvite?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }

    // Handle operation results
    LaunchedEffect(operationResult) {
        when (val result = operationResult) {
            is InviteOperationResult.InviteCreated -> {
                snackbarHostState.showSnackbar("New invite created!")
                viewModel.clearOperationResult()
            }
            is InviteOperationResult.InviteRevoked -> {
                snackbarHostState.showSnackbar("Invite revoked")
                viewModel.clearOperationResult()
            }
            is InviteOperationResult.Error -> {
                snackbarHostState.showSnackbar(result.message)
                viewModel.clearOperationResult()
            }
            else -> {}
        }
    }

    // Update refresh state
    LaunchedEffect(uiState) {
        if (uiState !is InviteUiState.Loading) {
            isRefreshing = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.group_pending_invites)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.createInvite() },
            ) {
                Icon(Icons.Default.Add, stringResource(R.string.group_create_new_invite))
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        when (uiState) {
            is InviteUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            is InviteUiState.Success -> {
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = {
                        isRefreshing = true
                        viewModel.loadInvites()
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                ) {
                    if (invites.isEmpty()) {
                        EmptyInvitesState()
                    } else {
                        InvitesList(
                            invites = invites,
                            onInviteClick = onInviteClick,
                            onRevokeClick = { inviteToRevoke = it },
                        )
                    }
                }
            }
            is InviteUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = (uiState as InviteUiState.Error).message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadInvites() }) {
                            Text(stringResource(R.string.group_retry))
                        }
                    }
                }
            }
        }
    }

    // Revoke confirmation dialog
    inviteToRevoke?.let { invite ->
        RevokeInviteDialog(
            invite = invite,
            onDismiss = { inviteToRevoke = null },
            onConfirm = {
                viewModel.revokeInvite(invite.id)
                inviteToRevoke = null
            },
        )
    }
}

/**
 * Empty state when no invites exist
 */
@Composable
private fun EmptyInvitesState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Link,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            )
            Text(
                text = stringResource(R.string.group_no_pending_invites),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.group_create_invite_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
        }
    }
}

/**
 * List of invites
 */
@Composable
private fun InvitesList(
    invites: List<GroupInvite>,
    onInviteClick: (GroupInvite) -> Unit,
    onRevokeClick: (GroupInvite) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(invites, key = { it.id }) { invite ->
            InviteCard(
                invite = invite,
                onClick = { onInviteClick(invite) },
                onRevokeClick = { onRevokeClick(invite) },
            )
        }
    }
}

/**
 * Individual invite card
 */
@Composable
private fun InviteCard(
    invite: GroupInvite,
    onClick: () -> Unit,
    onRevokeClick: () -> Unit,
) {
    val urgency = invite.getExpiryUrgency()

    val cardColors = when (urgency) {
        ExpiryUrgency.CRITICAL -> CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        )
        ExpiryUrgency.WARNING -> CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        )
        else -> CardDefaults.cardColors()
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = cardColors,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // Code in monospace font
                Text(
                    text = invite.code,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                    ),
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Created date
                Text(
                    text = stringResource(R.string.group_created_on, formatDate(invite.createdAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                // Expiry info
                Text(
                    text = formatExpiryStatus(invite),
                    style = MaterialTheme.typography.bodySmall,
                    color = when (urgency) {
                        ExpiryUrgency.CRITICAL -> MaterialTheme.colorScheme.error
                        ExpiryUrgency.WARNING -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )

                // Uses info
                if (invite.isMultiUse()) {
                    Text(
                        text = if (invite.maxUses == -1) {
                            "Unlimited uses"
                        } else {
                            "${invite.usesRemaining}/${invite.maxUses} uses remaining"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Revoke button
            IconButton(onClick = onRevokeClick) {
                Icon(
                    Icons.Default.Delete,
                    "Revoke invite",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

/**
 * Revoke invite confirmation dialog
 */
@Composable
private fun RevokeInviteDialog(
    invite: GroupInvite,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.group_revoke_invite_title)) },
        text = {
            Column {
                Text(stringResource(R.string.group_revoke_invite_message))
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.group_invite_code_prefix, invite.code),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace,
                    ),
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.group_invite_revoke_warning),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(stringResource(R.string.group_revoke))
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
 * Format date for display
 */
private fun formatDate(instant: Instant): String {
    val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    return String.format(
        "%d/%d/%d",
        localDateTime.monthNumber,
        localDateTime.day,
        localDateTime.year,
    )
}

/**
 * Format expiry status for display
 */
private fun formatExpiryStatus(invite: GroupInvite): String {
    val remainingMs = invite.getTimeRemainingMillis()

    if (remainingMs <= 0) return "Expired"

    val seconds = remainingMs / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        days > 0 -> "Expires in ${days} ${if (days == 1L) "day" else "days"}"
        hours > 0 -> "Expires in ${hours} ${if (hours == 1L) "hour" else "hours"}"
        minutes > 0 -> "Expires in ${minutes}m"
        else -> "Expires soon"
    }
}

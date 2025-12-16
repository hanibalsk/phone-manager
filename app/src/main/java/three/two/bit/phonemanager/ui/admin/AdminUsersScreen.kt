package three.two.bit.phonemanager.ui.admin

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import three.two.bit.phonemanager.R
import three.two.bit.phonemanager.domain.model.Device
import three.two.bit.phonemanager.domain.model.Group
import three.two.bit.phonemanager.domain.model.GroupRole

/**
 * Story E9.3: Admin Users Screen
 * Story E9.6: Remove User from Managed List
 *
 * Shows admin groups and their members for admin/owner users.
 * Allows viewing user locations from managed groups and removing users.
 *
 * ACs: E9.3.1, E9.3.2, E9.3.3, E9.3.4, E9.3.5, E9.3.6, E9.6.1-E9.6.6
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUsersScreen(
    viewModel: AdminUsersViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToUserLocation: (groupId: String, deviceId: String) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Get localized strings for snackbar messages
    val userRemovedMessage = stringResource(R.string.admin_user_removed)

    // Handle back press - if in member view, go back to group list
    BackHandler(enabled = uiState.selectedGroup != null) {
        viewModel.clearSelectedGroup()
    }

    // Story E9.6: Show success/error messages
    LaunchedEffect(uiState.removeSuccess) {
        uiState.removeSuccess?.let { displayName ->
            snackbarHostState.showSnackbar(userRemovedMessage.replace("%1\$s", displayName))
            viewModel.clearRemoveSuccess()
        }
    }

    LaunchedEffect(uiState.removeError) {
        uiState.removeError?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearRemoveError()
        }
    }

    // Story E9.6: Confirmation dialog
    if (uiState.deviceToRemove != null) {
        RemoveUserConfirmationDialog(
            deviceName = uiState.deviceToRemove!!.displayName,
            isRemoving = uiState.isRemoving,
            onConfirm = { viewModel.removeUser() },
            onDismiss = { viewModel.cancelRemoveConfirmation() },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState.selectedGroup != null) {
                            uiState.selectedGroup!!.name
                        } else {
                            stringResource(R.string.admin_managed_users)
                        },
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (uiState.selectedGroup != null) {
                                viewModel.clearSelectedGroup()
                            } else {
                                onNavigateBack()
                            }
                        },
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = uiState.isLoading || uiState.isMembersLoading,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            when {
                // Loading state
                uiState.isLoading && uiState.adminGroups.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
                // Error state
                uiState.error != null -> {
                    ErrorState(
                        message = uiState.error!!,
                        onRetry = { viewModel.loadAdminGroups() },
                    )
                }
                // Empty state - no admin groups
                uiState.isEmpty -> {
                    EmptyAdminGroupsState()
                }
                // Show members of selected group
                uiState.selectedGroup != null -> {
                    GroupMembersContent(
                        group = uiState.selectedGroup!!,
                        members = uiState.groupMembers,
                        isLoading = uiState.isMembersLoading,
                        error = uiState.membersError,
                        onMemberClick = { device ->
                            onNavigateToUserLocation(uiState.selectedGroup!!.id, device.deviceId)
                        },
                        onRemoveMember = { device ->
                            viewModel.showRemoveConfirmation(device)
                        },
                        isCurrentUserDevice = { device ->
                            viewModel.isCurrentUserDevice(device)
                        },
                    )
                }
                // Show list of admin groups
                else -> {
                    AdminGroupsContent(
                        groups = uiState.adminGroups,
                        onGroupClick = { group -> viewModel.selectGroup(group) },
                    )
                }
            }
        }
    }
}

/**
 * Content showing list of admin groups
 */
@Composable
private fun AdminGroupsContent(
    groups: List<Group>,
    onGroupClick: (Group) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }

        item {
            Text(
                text = stringResource(R.string.admin_select_group),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        items(groups, key = { it.id }) { group ->
            AdminGroupCard(
                group = group,
                onClick = { onGroupClick(group) },
            )
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }
    }
}

/**
 * Admin group card
 */
@Composable
private fun AdminGroupCard(
    group: Group,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Group,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary,
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = group.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Role badge
                    RoleBadge(role = group.userRole)
                }

                Text(
                    text = "${group.memberCount} ${if (group.memberCount == 1) "member" else "members"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * Content showing group members
 *
 * Story E9.6: Supports removal of members via swipe-to-delete
 * AC E9.6.1: Remove action accessible from users list (swipe gesture)
 * AC E9.6.6: Cannot remove self from list (validation via isCurrentUserDevice)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GroupMembersContent(
    group: Group,
    members: List<Device>,
    isLoading: Boolean,
    error: String?,
    onMemberClick: (Device) -> Unit,
    onRemoveMember: (Device) -> Unit,
    isCurrentUserDevice: (Device) -> Boolean,
) {
    when {
        isLoading && members.isEmpty() -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }
        error != null && members.isEmpty() -> {
            ErrorState(
                message = error,
                onRetry = null,
            )
        }
        members.isEmpty() -> {
            EmptyMembersState()
        }
        else -> {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }

                items(members, key = { it.deviceId }) { device ->
                    val isOwnDevice = isCurrentUserDevice(device)

                    // Story E9.6: Swipe-to-delete for non-self devices
                    // AC E9.6.6: Cannot remove self from list
                    if (!isOwnDevice) {
                        SwipeToDeleteDeviceCard(
                            device = device,
                            onClick = { onMemberClick(device) },
                            onDelete = { onRemoveMember(device) },
                        )
                    } else {
                        // Own device - no swipe-to-delete
                        DeviceMemberCard(
                            device = device,
                            onClick = { onMemberClick(device) },
                            isOwnDevice = true,
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(8.dp)) }
            }
        }
    }
}

/**
 * Story E9.6: Swipe-to-delete wrapper for device cards
 *
 * AC E9.6.1: Remove action accessible from users list (swipe gesture)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDeleteDeviceCard(
    device: Device,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                false // Don't actually dismiss - let the dialog handle it
            } else {
                false
            }
        },
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.error)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.admin_remove_user),
                    tint = MaterialTheme.colorScheme.onError,
                )
            }
        },
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
    ) {
        DeviceMemberCard(
            device = device,
            onClick = onClick,
            isOwnDevice = false,
        )
    }
}

/**
 * Device member card showing device info and location status
 *
 * AC E9.3.3: Select user from list navigates to device detail screen
 * AC E9.3.5: Displays last update timestamp
 * AC E9.6.6: Shows indicator if this is the current user's device
 */
@Composable
private fun DeviceMemberCard(
    device: Device,
    onClick: () -> Unit,
    isOwnDevice: Boolean = false,
) {
    val hasLocation = device.lastLocation != null

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Device icon with location status indicator
            Box {
                Icon(
                    imageVector = Icons.Default.Smartphone,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = if (isOwnDevice) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                if (hasLocation) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = stringResource(R.string.admin_view_location),
                        modifier = Modifier
                            .size(18.dp)
                            .align(Alignment.BottomEnd),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = device.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )

                    // Show "You" badge for own device
                    if (isOwnDevice) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.primaryContainer,
                        ) {
                            Text(
                                text = stringResource(R.string.admin_you),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            )
                        }
                    }
                }

                // Last seen timestamp
                device.lastSeenAt?.let { lastSeen ->
                    Text(
                        text = stringResource(R.string.last_seen, formatRelativeTime(lastSeen)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } ?: run {
                    if (!hasLocation) {
                        Text(
                            text = stringResource(R.string.admin_no_location),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // View location icon
            if (hasLocation) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = stringResource(R.string.admin_view_location),
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

/**
 * Role badge
 */
@Composable
private fun RoleBadge(role: GroupRole) {
    val (text, containerColor) = when (role) {
        GroupRole.OWNER -> stringResource(R.string.admin_role_owner) to MaterialTheme.colorScheme.primary
        GroupRole.ADMIN -> stringResource(R.string.admin_role_admin) to MaterialTheme.colorScheme.secondary
        GroupRole.MEMBER -> stringResource(R.string.admin_role_member) to MaterialTheme.colorScheme.surfaceVariant
    }

    val contentColor = when (role) {
        GroupRole.OWNER -> MaterialTheme.colorScheme.onPrimary
        GroupRole.ADMIN -> MaterialTheme.colorScheme.onSecondary
        GroupRole.MEMBER -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        shape = MaterialTheme.shapes.small,
        color = containerColor,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

/**
 * Empty state when no admin groups
 */
@Composable
private fun EmptyAdminGroupsState() {
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
                imageVector = Icons.Default.Group,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text(
                text = stringResource(R.string.admin_no_groups),
                style = MaterialTheme.typography.titleLarge,
            )
        }
    }
}

/**
 * Empty state when no members in group
 */
@Composable
private fun EmptyMembersState() {
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
                imageVector = Icons.Default.PersonOff,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text(
                text = stringResource(R.string.admin_no_users),
                style = MaterialTheme.typography.titleLarge,
            )
        }
    }
}

/**
 * Error state with retry button
 */
@Composable
private fun ErrorState(
    message: String,
    onRetry: (() -> Unit)?,
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

            if (onRetry != null) {
                Button(onClick = onRetry) {
                    Text(stringResource(R.string.retry))
                }
            }
        }
    }
}

/**
 * Format timestamp as relative time
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
            instant.toLocalDateTime(TimeZone.currentSystemDefault())
                .date.toString()
        }
    }
}

/**
 * Story E9.6: Confirmation dialog for removing a user
 *
 * AC E9.6.2: Confirmation dialog before removal
 */
@Composable
private fun RemoveUserConfirmationDialog(
    deviceName: String,
    isRemoving: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!isRemoving) onDismiss() },
        title = {
            Text(stringResource(R.string.admin_remove_user_title))
        },
        text = {
            Text(stringResource(R.string.admin_remove_user_message, deviceName))
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = !isRemoving,
            ) {
                if (isRemoving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(
                        text = stringResource(R.string.admin_remove),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isRemoving,
            ) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

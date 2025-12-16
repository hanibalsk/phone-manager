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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PhonelinkRing
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import three.two.bit.phonemanager.R
import three.two.bit.phonemanager.domain.model.Group
import three.two.bit.phonemanager.domain.model.GroupMembership
import three.two.bit.phonemanager.domain.model.GroupRole

/**
 * Story E11.8 Task 8: Group Detail Screen
 *
 * AC E11.8.3: View Group Details
 * - Group name, description, member count
 * - User's role in the group
 * - Created date
 *
 * AC E11.8.4: View Members (preview)
 * - Show member count with navigation to full list
 *
 * AC E11.8.6: Leave Group
 * - Leave button (non-owners only)
 *
 * AC E11.8.7: Delete Group
 * - Delete button (owners only)
 *
 * AC E11.8.8: Group Settings
 * - Edit name/description (admins and owners)
 *
 * Story E11.9: Added invite members navigation
 * Story E12.7: Added member devices settings navigation
 *
 * @param viewModel The GroupDetailViewModel
 * @param onNavigateBack Callback to navigate back
 * @param onNavigateToMembers Callback to navigate to manage members screen
 * @param onNavigateToInvite Callback to navigate to invite members screen
 * @param onNavigateToMemberDevices Callback to navigate to member devices settings screen (E12.7)
 * @param onGroupDeleted Callback when group is deleted
 * @param onLeftGroup Callback when user leaves group
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    viewModel: GroupDetailViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToMembers: (String) -> Unit,
    onNavigateToInvite: (String) -> Unit = {},
    onNavigateToMemberDevices: (String) -> Unit = {},
    onGroupDeleted: () -> Unit = {},
    onLeftGroup: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val members by viewModel.members.collectAsStateWithLifecycle()
    val operationResult by viewModel.operationResult.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showLeaveDialog by remember { mutableStateOf(false) }
    var showEditNameDialog by remember { mutableStateOf(false) }
    var showEditDescriptionDialog by remember { mutableStateOf(false) }

    // Handle operation results
    LaunchedEffect(operationResult) {
        when (val result = operationResult) {
            is GroupOperationResult.Success -> {
                snackbarHostState.showSnackbar(result.message)
                viewModel.clearOperationResult()
            }
            is GroupOperationResult.Error -> {
                snackbarHostState.showSnackbar(result.message)
                viewModel.clearOperationResult()
            }
            is GroupOperationResult.GroupDeleted -> {
                snackbarHostState.showSnackbar("Group deleted")
                viewModel.clearOperationResult()
                onGroupDeleted()
            }
            is GroupOperationResult.LeftGroup -> {
                snackbarHostState.showSnackbar("You left the group")
                viewModel.clearOperationResult()
                onLeftGroup()
            }
            is GroupOperationResult.DeviceAdded -> {
                snackbarHostState.showSnackbar("Device added to group")
                viewModel.clearOperationResult()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.group_detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                actions = {
                    // Settings button (for admins/owners)
                    if (uiState is GroupDetailUiState.Success &&
                        (uiState as GroupDetailUiState.Success).canManageMembers
                    ) {
                        IconButton(onClick = {
                            val group = (uiState as GroupDetailUiState.Success).group
                            onNavigateToMembers(group.id)
                        }) {
                            Icon(Icons.Default.Settings, stringResource(R.string.group_detail_manage_members))
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        when (val state = uiState) {
            is GroupDetailUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            is GroupDetailUiState.Success -> {
                GroupDetailContent(
                    group = state.group,
                    members = members.take(5), // Preview first 5 members
                    canManageMembers = state.canManageMembers,
                    canDelete = state.canDelete,
                    canLeave = state.canLeave,
                    isLoading = operationResult is GroupOperationResult.Loading,
                    onEditName = { showEditNameDialog = true },
                    onEditDescription = { showEditDescriptionDialog = true },
                    onViewMembers = { onNavigateToMembers(state.group.id) },
                    onInviteMembers = { onNavigateToInvite(state.group.id) },
                    onMemberDevices = { onNavigateToMemberDevices(state.group.id) },
                    onAddDeviceToGroup = { viewModel.addCurrentDeviceToGroup() },
                    onLeaveGroup = { showLeaveDialog = true },
                    onDeleteGroup = { showDeleteDialog = true },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                )
            }
            is GroupDetailUiState.Error -> {
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

    // Delete confirmation dialog
    if (showDeleteDialog && uiState is GroupDetailUiState.Success) {
        val group = (uiState as GroupDetailUiState.Success).group

        DeleteGroupDialog(
            groupName = group.name,
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                showDeleteDialog = false
                viewModel.deleteGroup()
            },
        )
    }

    // Leave confirmation dialog
    if (showLeaveDialog && uiState is GroupDetailUiState.Success) {
        val group = (uiState as GroupDetailUiState.Success).group

        LeaveGroupDialog(
            groupName = group.name,
            onDismiss = { showLeaveDialog = false },
            onConfirm = {
                showLeaveDialog = false
                viewModel.leaveGroup()
            },
        )
    }

    // Edit name dialog
    if (showEditNameDialog && uiState is GroupDetailUiState.Success) {
        val group = (uiState as GroupDetailUiState.Success).group

        EditGroupNameDialog(
            currentName = group.name,
            onDismiss = { showEditNameDialog = false },
            onConfirm = { newName ->
                showEditNameDialog = false
                viewModel.updateGroupName(newName)
            },
        )
    }

    // Edit description dialog
    if (showEditDescriptionDialog && uiState is GroupDetailUiState.Success) {
        val group = (uiState as GroupDetailUiState.Success).group

        EditGroupDescriptionDialog(
            currentDescription = group.description,
            onDismiss = { showEditDescriptionDialog = false },
            onConfirm = { newDescription ->
                showEditDescriptionDialog = false
                viewModel.updateGroupDescription(newDescription)
            },
        )
    }
}

/**
 * Group detail content
 */
@Composable
private fun GroupDetailContent(
    group: Group,
    members: List<GroupMembership>,
    canManageMembers: Boolean,
    canDelete: Boolean,
    canLeave: Boolean,
    isLoading: Boolean,
    onEditName: () -> Unit,
    onEditDescription: () -> Unit,
    onViewMembers: () -> Unit,
    onInviteMembers: () -> Unit,
    onMemberDevices: () -> Unit,
    onAddDeviceToGroup: () -> Unit,
    onLeaveGroup: () -> Unit,
    onDeleteGroup: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Group header
        GroupHeader(
            group = group,
            canEdit = canManageMembers,
            onEditName = onEditName,
        )

        // Group information card
        GroupInfoCard(
            group = group,
            canEdit = canManageMembers,
            onEditDescription = onEditDescription,
        )

        // Members preview card
        MembersPreviewCard(
            members = members,
            totalCount = group.memberCount,
            onViewAll = onViewMembers,
        )

        // Story E11.9: Invite members button (for admins/owners)
        if (canManageMembers) {
            Button(
                onClick = onInviteMembers,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.PersonAdd, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.group_detail_invite_members))
            }

            // Quick add this device to group button (for admins/owners)
            Button(
                onClick = onAddDeviceToGroup,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(Icons.Default.PhonelinkRing, null, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.group_detail_add_this_device))
            }

            // Story E12.7: Manage member devices button (for admins/owners)
            OutlinedButton(
                onClick = onMemberDevices,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.PhoneAndroid, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.group_detail_manage_devices))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Leave button (non-owners)
        if (canLeave) {
            OutlinedButton(
                onClick = onLeaveGroup,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.group_detail_leave))
            }
        }

        // Delete button (owners only)
        if (canDelete) {
            OutlinedButton(
                onClick = onDeleteGroup,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.group_detail_delete))
            }

            // Warning for owners
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                ),
            ) {
                Text(
                    text = stringResource(R.string.group_detail_delete_warning),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(12.dp),
                )
            }
        }
    }
}

/**
 * Group header with icon and name
 */
@Composable
private fun GroupHeader(
    group: Group,
    canEdit: Boolean,
    onEditName: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.Group,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary,
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = group.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Role badge
                RoleBadge(role = group.userRole)
            }

            // Member count
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${group.memberCount} ${if (group.memberCount == 1) "member" else "members"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Edit name button
        if (canEdit) {
            IconButton(onClick = onEditName) {
                Icon(Icons.Default.Edit, "Edit name")
            }
        }
    }
}

/**
 * Group information card with details
 */
@Composable
private fun GroupInfoCard(
    group: Group,
    canEdit: Boolean,
    onEditDescription: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.group_detail_info_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )

                if (canEdit) {
                    IconButton(onClick = onEditDescription, modifier = Modifier.size(24.dp)) {
                        Icon(
                            Icons.Default.Edit,
                            stringResource(R.string.group_detail_edit_description),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }

            // Description
            if (group.description != null) {
                Text(
                    text = group.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else if (canEdit) {
                Text(
                    text = stringResource(R.string.group_detail_add_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                )
            }

            // Created date
            group.createdAt?.let { createdAt ->
                InfoRow(label = stringResource(R.string.group_detail_created), value = formatFullTimestamp(createdAt))
            }

            InfoRow(label = stringResource(R.string.group_detail_your_role), value = group.userRole.name.lowercase().replaceFirstChar { it.uppercase() })
        }
    }
}

/**
 * Members preview card
 */
@Composable
private fun MembersPreviewCard(
    members: List<GroupMembership>,
    totalCount: Int,
    onViewAll: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.group_detail_members_title, totalCount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )

                Button(onClick = onViewAll) {
                    Text(stringResource(R.string.group_detail_view_all))
                }
            }

            // Member preview list
            members.forEach { member ->
                MemberPreviewItem(member = member)
            }

            if (members.isEmpty()) {
                Text(
                    text = stringResource(R.string.group_detail_no_members),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * Member preview item
 */
@Composable
private fun MemberPreviewItem(member: GroupMembership) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = member.displayName,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = member.email,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // Role badge
        MemberRoleBadge(role = member.role)
    }
}

/**
 * Role badge for group detail header
 */
@Composable
private fun RoleBadge(role: GroupRole) {
    val (text, containerColor) = when (role) {
        GroupRole.OWNER -> "Owner" to MaterialTheme.colorScheme.primary
        GroupRole.ADMIN -> "Admin" to MaterialTheme.colorScheme.secondary
        GroupRole.MEMBER -> "Member" to MaterialTheme.colorScheme.surfaceVariant
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
 * Smaller role badge for member list
 */
@Composable
private fun MemberRoleBadge(role: GroupRole) {
    val (text, containerColor) = when (role) {
        GroupRole.OWNER -> "Owner" to MaterialTheme.colorScheme.primaryContainer
        GroupRole.ADMIN -> "Admin" to MaterialTheme.colorScheme.secondaryContainer
        GroupRole.MEMBER -> return // Don't show badge for regular members
    }

    val contentColor = when (role) {
        GroupRole.OWNER -> MaterialTheme.colorScheme.onPrimaryContainer
        GroupRole.ADMIN -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> return
    }

    Surface(
        shape = MaterialTheme.shapes.extraSmall,
        color = containerColor,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
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
 * Format timestamp for full display
 */
private fun formatFullTimestamp(instant: Instant): String {
    val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    return String.format(
        "%d/%d/%d",
        localDateTime.monthNumber,
        localDateTime.dayOfMonth,
        localDateTime.year,
    )
}

package three.two.bit.phonemanager.ui.groups

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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import three.two.bit.phonemanager.R
import three.two.bit.phonemanager.domain.model.GroupMembership
import three.two.bit.phonemanager.domain.model.GroupRole

/**
 * Story E11.8 Task 9: Manage Members Screen
 * Story UGM-2.2: Navigate to Member Device Details
 *
 * AC E11.8.4: View Members
 * - List of all group members
 * - Display name, email, role badge
 * - Device count per member
 *
 * AC E11.8.5: Member Management (for admins/owners)
 * - Role change buttons (promote/demote)
 * - Remove member option
 * - Transfer ownership option (owners only)
 *
 * AC UGM-2.2: Navigate to member details
 * - Tap member card to view their device details
 *
 * @param viewModel The GroupDetailViewModel (shared with GroupDetailScreen)
 * @param onNavigateBack Callback to navigate back
 * @param onInviteMember Callback to navigate to invite screen (Story E11.9)
 * @param onNavigateToMemberDetails Callback to navigate to member's device details (Story UGM-2.2)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageMembersScreen(
    viewModel: GroupDetailViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onInviteMember: () -> Unit = {},
    onNavigateToMemberDetails: (groupId: String, userId: String) -> Unit = { _, _ -> },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val members by viewModel.members.collectAsStateWithLifecycle()
    val isMembersLoading by viewModel.isMembersLoading.collectAsStateWithLifecycle()
    val operationResult by viewModel.operationResult.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var isRefreshing by remember { mutableStateOf(false) }

    var selectedMember by remember { mutableStateOf<GroupMembership?>(null) }
    var showRoleChangeDialog by remember { mutableStateOf(false) }
    var showRemoveMemberDialog by remember { mutableStateOf(false) }
    var showTransferOwnershipDialog by remember { mutableStateOf(false) }

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
            else -> {}
        }
    }

    // Handle refresh completion
    LaunchedEffect(members) {
        isRefreshing = false
    }

    val canManageMembers = (uiState as? GroupDetailUiState.Success)?.canManageMembers == true
    val isOwner = (uiState as? GroupDetailUiState.Success)?.group?.isOwner() == true

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.group_manage_members)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
            )
        },
        floatingActionButton = {
            // Invite member FAB (for admins/owners) - links to E11.9
            if (canManageMembers) {
                FloatingActionButton(onClick = onInviteMember) {
                    Icon(Icons.Default.PersonAdd, stringResource(R.string.group_invite_members))
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                viewModel.loadMembers()
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            when (val state = uiState) {
                is GroupDetailUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is GroupDetailUiState.Success -> {
                    if (isMembersLoading && members.isEmpty()) {
                        // Show loading while members are being loaded initially
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator()
                        }
                    } else if (members.isEmpty()) {
                        EmptyMembersState()
                    } else {
                        MembersList(
                            members = members,
                            canManage = canManageMembers,
                            isOwner = isOwner,
                            canManageMember = { viewModel.canManageMember(it) },
                            onMemberClick = { member ->
                                // Story UGM-2.2: Navigate to member's device details
                                onNavigateToMemberDetails(state.group.id, member.userId)
                            },
                            onChangeRole = { member ->
                                selectedMember = member
                                showRoleChangeDialog = true
                            },
                            onRemoveMember = { member ->
                                selectedMember = member
                                showRemoveMemberDialog = true
                            },
                            onTransferOwnership = { member ->
                                selectedMember = member
                                showTransferOwnershipDialog = true
                            },
                        )
                    }
                }
                is GroupDetailUiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
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
    }

    // Role change dialog
    if (showRoleChangeDialog && selectedMember != null) {
        ChangeRoleDialog(
            member = selectedMember!!,
            onDismiss = {
                showRoleChangeDialog = false
                selectedMember = null
            },
            onChangeRole = { newRole ->
                showRoleChangeDialog = false
                viewModel.updateMemberRole(selectedMember!!.userId, newRole)
                selectedMember = null
            },
        )
    }

    // Remove member dialog
    if (showRemoveMemberDialog && selectedMember != null) {
        RemoveMemberDialog(
            memberName = selectedMember!!.displayName,
            onDismiss = {
                showRemoveMemberDialog = false
                selectedMember = null
            },
            onConfirm = {
                showRemoveMemberDialog = false
                viewModel.removeMember(selectedMember!!.userId)
                selectedMember = null
            },
        )
    }

    // Transfer ownership dialog
    if (showTransferOwnershipDialog && selectedMember != null) {
        TransferGroupOwnershipDialog(
            memberName = selectedMember!!.displayName,
            onDismiss = {
                showTransferOwnershipDialog = false
                selectedMember = null
            },
            onConfirm = {
                showTransferOwnershipDialog = false
                viewModel.transferOwnership(selectedMember!!.userId)
                selectedMember = null
            },
        )
    }
}

/**
 * Members list
 */
@Composable
private fun MembersList(
    members: List<GroupMembership>,
    canManage: Boolean,
    isOwner: Boolean,
    canManageMember: (GroupMembership) -> Boolean,
    onMemberClick: (GroupMembership) -> Unit,
    onChangeRole: (GroupMembership) -> Unit,
    onRemoveMember: (GroupMembership) -> Unit,
    onTransferOwnership: (GroupMembership) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }

        items(members, key = { it.userId }) { member ->
            MemberCard(
                member = member,
                canManage = canManage && canManageMember(member),
                isOwner = isOwner,
                onClick = { onMemberClick(member) },
                onChangeRole = { onChangeRole(member) },
                onRemove = { onRemoveMember(member) },
                onTransferOwnership = { onTransferOwnership(member) },
            )
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }
    }
}

/**
 * Member card with management options
 */
@Composable
private fun MemberCard(
    member: GroupMembership,
    canManage: Boolean,
    isOwner: Boolean,
    onClick: () -> Unit,
    onChangeRole: () -> Unit,
    onRemove: () -> Unit,
    onTransferOwnership: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Member icon
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Member info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = member.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Role badge
                    MemberRoleBadge(role = member.role)
                }

                Text(
                    text = member.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                // Device count
                Text(
                    text = pluralStringResource(
                        R.plurals.group_member_device_count,
                        member.deviceCount,
                        member.deviceCount,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Management menu
            if (canManage) {
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, stringResource(R.string.group_member_options_content_desc))
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                    ) {
                        // Change role option
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (member.role == GroupRole.ADMIN) {
                                        stringResource(R.string.group_demote_to_member)
                                    } else {
                                        stringResource(R.string.group_promote_to_admin)
                                    },
                                )
                            },
                            onClick = {
                                showMenu = false
                                onChangeRole()
                            },
                        )

                        // Transfer ownership (owners only, for non-owners)
                        if (isOwner && member.role != GroupRole.OWNER) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.group_transfer_ownership)) },
                                onClick = {
                                    showMenu = false
                                    onTransferOwnership()
                                },
                            )
                        }

                        // Remove member
                        DropdownMenuItem(
                            text = {
                                Text(
                                    stringResource(R.string.group_remove_from_group),
                                    color = MaterialTheme.colorScheme.error,
                                )
                            },
                            onClick = {
                                showMenu = false
                                onRemove()
                            },
                        )
                    }
                }
            }
        }
    }
}

/**
 * Role badge for members
 */
@Composable
private fun MemberRoleBadge(role: GroupRole) {
    val (text, containerColor) = when (role) {
        GroupRole.OWNER -> stringResource(R.string.group_role_owner) to MaterialTheme.colorScheme.primary
        GroupRole.ADMIN -> stringResource(R.string.group_role_admin) to MaterialTheme.colorScheme.secondary
        GroupRole.MEMBER -> stringResource(R.string.group_role_member) to MaterialTheme.colorScheme.surfaceVariant
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
 * Empty state when no members
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
                imageVector = Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text(
                text = stringResource(R.string.group_no_members_title),
                style = MaterialTheme.typography.titleLarge,
            )

            Text(
                text = stringResource(R.string.group_invite_people_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

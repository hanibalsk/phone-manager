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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Smartphone
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import three.two.bit.phonemanager.R
import three.two.bit.phonemanager.domain.model.Group
import three.two.bit.phonemanager.domain.model.GroupRole

/**
 * Story E11.8 Task 6: Group List Screen
 *
 * AC E11.8.1: Group List Screen
 * - LazyColumn of group cards
 * - Group name, member count, user role badge
 * - "Create Group" FAB
 * - Pull-to-refresh functionality
 * - Empty state with "Create Your First Group"
 * - Loading indicator
 * - Navigation to group detail
 *
 * Story E11.9: Added "Join Group" action in top bar
 *
 * @param viewModel The GroupListViewModel
 * @param onNavigateBack Callback to navigate back
 * @param onNavigateToGroupDetail Callback to navigate to group detail
 * @param onNavigateToJoinGroup Callback to navigate to join group screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupListScreen(
    viewModel: GroupListViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToGroupDetail: (Group) -> Unit,
    onNavigateToJoinGroup: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val createGroupResult by viewModel.createGroupResult.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var isRefreshing by remember { mutableStateOf(false) }
    var showCreateGroupDialog by remember { mutableStateOf(false) }

    // Handle create group results
    LaunchedEffect(createGroupResult) {
        when (val result = createGroupResult) {
            is CreateGroupResult.Success -> {
                snackbarHostState.showSnackbar("Group \"${result.group.name}\" created")
                viewModel.clearCreateGroupResult()
            }
            is CreateGroupResult.Error -> {
                snackbarHostState.showSnackbar(result.message)
                viewModel.clearCreateGroupResult()
            }
            else -> {}
        }
    }

    // Handle refresh completion
    LaunchedEffect(uiState) {
        if (uiState !is GroupListUiState.Loading) {
            isRefreshing = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.groups_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                // Story E11.9: Add "Join Group" action
                actions = {
                    IconButton(onClick = onNavigateToJoinGroup) {
                        Icon(Icons.Default.PersonAdd, stringResource(R.string.groups_join))
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateGroupDialog = true },
            ) {
                Icon(Icons.Default.Add, stringResource(R.string.groups_create))
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                viewModel.refreshGroups()
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            when (val state = uiState) {
                is GroupListUiState.Loading -> {
                    LoadingState()
                }
                is GroupListUiState.Success -> {
                    GroupList(
                        groups = state.groups,
                        onGroupClick = onNavigateToGroupDetail,
                    )
                }
                is GroupListUiState.Empty -> {
                    EmptyState(onCreateGroup = { showCreateGroupDialog = true })
                }
                is GroupListUiState.Error -> {
                    ErrorState(
                        message = state.message,
                        onRetry = { viewModel.refreshGroups() },
                    )
                }
            }
        }
    }

    // Create Group Dialog
    if (showCreateGroupDialog) {
        CreateGroupDialog(
            onDismiss = { showCreateGroupDialog = false },
            onConfirm = { name, description ->
                showCreateGroupDialog = false
                viewModel.createGroup(name, description)
            },
        )
    }
}

/**
 * AC E11.8.1: Group list with cards
 */
@Composable
private fun GroupList(groups: List<Group>, onGroupClick: (Group) -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }

        items(groups, key = { it.id }) { group ->
            GroupCard(
                group = group,
                onClick = { onGroupClick(group) },
            )
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }
    }
}

/**
 * AC E11.8.1: Group card showing name, member count, and user role badge
 */
@Composable
private fun GroupCard(group: Group, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Group icon
            Icon(
                imageVector = Icons.Default.Group,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary,
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Group info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = group.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Role badge
                    RoleBadge(role = group.userRole)
                }

                // Member count and device indicator row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (group.memberCount == 1) {
                            stringResource(R.string.groups_member_count_one, group.memberCount)
                        } else {
                            stringResource(R.string.groups_member_count, group.memberCount)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    // Story UGM-3.7 AC 3: Device assignment indicator
                    if (group.hasCurrentDevice) {
                        Spacer(modifier = Modifier.width(12.dp))
                        Icon(
                            imageVector = Icons.Default.Smartphone,
                            contentDescription = stringResource(R.string.groups_device_in_group),
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.groups_my_device),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                // Description (if present)
                group.description?.let { description ->
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/**
 * AC E11.8.1: Role badge showing user's role in the group
 */
@Composable
private fun RoleBadge(role: GroupRole) {
    val (text, containerColor) = when (role) {
        GroupRole.OWNER -> stringResource(R.string.groups_role_owner) to MaterialTheme.colorScheme.primary
        GroupRole.ADMIN -> stringResource(R.string.groups_role_admin) to MaterialTheme.colorScheme.secondary
        GroupRole.MEMBER -> stringResource(R.string.groups_role_member) to MaterialTheme.colorScheme.surfaceVariant
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
 * AC E11.8.1: Empty state with "Create Your First Group" CTA
 */
@Composable
private fun EmptyState(onCreateGroup: () -> Unit) {
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
                text = stringResource(R.string.groups_empty_title),
                style = MaterialTheme.typography.titleLarge,
            )

            Text(
                text = stringResource(R.string.groups_empty_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Button(onClick = onCreateGroup) {
                Icon(Icons.Default.Add, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.groups_create_first))
            }
        }
    }
}

/**
 * Error state with retry option
 */
@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
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

            Button(onClick = onRetry) {
                Text(stringResource(R.string.button_retry))
            }
        }
    }
}

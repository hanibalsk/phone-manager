package three.two.bit.phonemanager.ui.groups

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.GroupWork
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import three.two.bit.phonemanager.R

/**
 * Story UGM-4.3: Group Migration Screen
 * Story UGM-4.4: Handle Migration Errors and Offline
 *
 * Allows users to migrate their registration group to an authenticated group.
 *
 * UGM-4.3 ACs:
 * AC 1: Pre-fill group name with registration group info
 * AC 2: Validate group name (3-50 chars)
 * AC 3: Call migration API on submit
 * AC 4: Show success when user becomes OWNER
 * AC 5: Registration group deleted automatically
 * AC 6: Show progress indicator during migration
 *
 * UGM-4.4 ACs:
 * AC 1: Network error handling
 * AC 2: Retry option
 * AC 3: Offline detection
 * AC 4: No offline queue
 * AC 5: Server error handling
 * AC 6: Retry functionality
 *
 * @param onNavigateBack Callback to navigate back
 * @param onMigrationSuccess Callback when migration succeeds with new group ID
 * @param viewModel The migration view model
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupMigrationScreen(
    onNavigateBack: () -> Unit,
    onMigrationSuccess: (groupId: String) -> Unit,
    viewModel: GroupMigrationViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val groupName by viewModel.groupName.collectAsState()
    val nameError by viewModel.nameError.collectAsState()
    val deviceCount by viewModel.deviceCount.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()

    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Handle migration success - navigate to new group
    LaunchedEffect(uiState) {
        if (uiState is MigrationUiState.Success) {
            val newGroup = (uiState as MigrationUiState.Success).newGroup
            onMigrationSuccess(newGroup.id)
        }
    }

    // Story UGM-4.4 AC 3: Show offline message via snackbar when in offline state
    LaunchedEffect(uiState) {
        if (uiState is MigrationUiState.Offline) {
            snackbarHostState.showSnackbar("Migration requires an internet connection.")
            viewModel.clearError()
        }
    }

    Scaffold(
        modifier = Modifier.testTag("migration_screen"),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.migration_screen_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Story UGM-4.4 AC 3: Offline banner
            if (!isOnline) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                    ),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.WifiOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = stringResource(R.string.migration_error_offline),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Story UGM-4.4 AC 1, 2, 5: Error banner with retry
            val errorState = uiState as? MigrationUiState.Error
            if (errorState != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                    ),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                    ) {
                        Text(
                            text = errorState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                        if (errorState.isRetryable && isOnline) {
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(
                                onClick = { viewModel.retry() },
                                modifier = Modifier.align(Alignment.End),
                            ) {
                                Text(stringResource(R.string.migration_retry_button))
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Header icon
            Icon(
                imageVector = Icons.Default.GroupWork,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Title
            Text(
                text = stringResource(R.string.migration_screen_header),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Description
            Text(
                text = stringResource(R.string.migration_screen_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Current group info card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Devices,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.size(12.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.migration_current_devices),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = stringResource(R.string.migration_device_count, deviceCount),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Group name input (AC 1, 2)
            OutlinedTextField(
                value = groupName,
                onValueChange = { viewModel.updateGroupName(it) },
                label = { Text(stringResource(R.string.migration_group_name_label)) },
                placeholder = { Text(stringResource(R.string.migration_group_name_placeholder)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("group_name_input"),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        if (viewModel.canMigrate()) {
                            viewModel.migrate()
                        }
                    },
                ),
                singleLine = true,
                isError = nameError != null,
                supportingText = {
                    if (nameError != null) {
                        Text(nameError!!)
                    } else {
                        Text(stringResource(R.string.migration_group_name_hint))
                    }
                },
                enabled = uiState !is MigrationUiState.Loading,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Benefits list
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    Text(
                        text = stringResource(R.string.migration_benefits_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    BenefitItem(stringResource(R.string.migration_benefit_roles))
                    BenefitItem(stringResource(R.string.migration_benefit_invites))
                    BenefitItem(stringResource(R.string.migration_benefit_management))
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Migrate button (AC 3, 6)
            Button(
                onClick = { viewModel.migrate() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("migrate_button"),
                enabled = viewModel.canMigrate(),
            ) {
                if (uiState is MigrationUiState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text(stringResource(R.string.migration_button))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Cancel button
            TextButton(
                onClick = onNavigateBack,
                modifier = Modifier.testTag("cancel_button"),
                enabled = uiState !is MigrationUiState.Loading,
            ) {
                Text(stringResource(R.string.migration_cancel_button))
            }
        }
    }
}

@Composable
private fun BenefitItem(text: String) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

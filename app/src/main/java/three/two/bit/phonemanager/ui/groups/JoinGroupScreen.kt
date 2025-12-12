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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import three.two.bit.phonemanager.R
import three.two.bit.phonemanager.domain.model.GroupPreview

/**
 * Story E11.9 Task 8: JoinGroupScreen
 *
 * AC E11.9.4: Join with Invite Code
 * AC E11.9.5: QR Code Scanning
 *
 * Screen for joining a group using an invite code.
 *
 * @param viewModel The JoinGroupViewModel
 * @param onNavigateBack Callback to navigate back
 * @param onNavigateToQrScanner Callback to open QR scanner
 * @param onNavigateToLogin Callback to navigate to login (if not authenticated)
 * @param onJoinSuccess Callback when successfully joined (passes groupId)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinGroupScreen(
    viewModel: JoinGroupViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToQrScanner: () -> Unit = {},
    onNavigateToLogin: () -> Unit = {},
    onJoinSuccess: (String) -> Unit = {},
) {
    val inviteCode by viewModel.inviteCode.collectAsStateWithLifecycle()
    val groupPreview by viewModel.groupPreview.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isAuthenticated by viewModel.isAuthenticated.collectAsStateWithLifecycle()
    val joinResult by viewModel.joinResult.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var showJoinConfirmation by remember { mutableStateOf(false) }

    // Pre-load strings for use in LaunchedEffect
    val joinSuccessMessage = stringResource(R.string.join_group_success)
    val signInRequiredMessage = stringResource(R.string.join_group_sign_in_required)

    // Handle join result
    LaunchedEffect(joinResult) {
        when (val result = joinResult) {
            is JoinResult.Success -> {
                snackbarHostState.showSnackbar(joinSuccessMessage)
                viewModel.clearJoinResult()
                onJoinSuccess(result.groupId)
            }
            is JoinResult.Error -> {
                snackbarHostState.showSnackbar(result.message)
                viewModel.clearJoinResult()
            }
            is JoinResult.AuthenticationRequired -> {
                snackbarHostState.showSnackbar(signInRequiredMessage)
                viewModel.clearJoinResult()
                onNavigateToLogin()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.join_group_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        JoinGroupContent(
            inviteCode = inviteCode,
            onCodeChange = { viewModel.setInviteCode(it) },
            groupPreview = groupPreview,
            uiState = uiState,
            isAuthenticated = isAuthenticated,
            isJoining = joinResult is JoinResult.Joining,
            onValidate = { viewModel.validateCode() },
            onJoin = { showJoinConfirmation = true },
            onScanQr = onNavigateToQrScanner,
            onSignIn = onNavigateToLogin,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        )
    }

    // Join confirmation dialog
    if (showJoinConfirmation && groupPreview != null) {
        JoinConfirmationDialog(
            group = groupPreview!!,
            onDismiss = { showJoinConfirmation = false },
            onConfirm = {
                showJoinConfirmation = false
                viewModel.joinGroup()
            },
        )
    }
}

/**
 * Main content for join group flow
 */
@Composable
private fun JoinGroupContent(
    inviteCode: String,
    onCodeChange: (String) -> Unit,
    groupPreview: GroupPreview?,
    uiState: JoinGroupUiState,
    isAuthenticated: Boolean,
    isJoining: Boolean,
    onValidate: () -> Unit,
    onJoin: () -> Unit,
    onScanQr: () -> Unit,
    onSignIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        // Header
        Icon(
            imageVector = Icons.Default.Group,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary,
        )

        Text(
            text = stringResource(R.string.join_group_enter_code_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )

        Text(
            text = stringResource(R.string.join_group_enter_code_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        // Code input field
        OutlinedTextField(
            value = inviteCode,
            onValueChange = onCodeChange,
            label = { Text(stringResource(R.string.join_group_code_label)) },
            placeholder = { Text(stringResource(R.string.join_group_code_placeholder)) },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            textStyle = MaterialTheme.typography.headlineSmall.copy(
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center,
                letterSpacing = 4.sp,
            ),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(
                onDone = { if (inviteCode.length == 8) onValidate() }
            ),
            isError = uiState is JoinGroupUiState.Error,
            supportingText = {
                when (uiState) {
                    is JoinGroupUiState.Error -> Text(uiState.message)
                    else -> Text(stringResource(R.string.join_group_code_counter, inviteCode.length))
                }
            },
        )

        // Validate button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilledTonalButton(
                onClick = onScanQr,
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Default.QrCodeScanner, null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.join_group_scan_qr))
            }

            Button(
                onClick = onValidate,
                enabled = inviteCode.length == 8 && uiState !is JoinGroupUiState.Validating,
                modifier = Modifier.weight(1f),
            ) {
                if (uiState is JoinGroupUiState.Validating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text(stringResource(R.string.join_group_validate))
                }
            }
        }

        // Group preview (shown when code is validated)
        if (groupPreview != null && uiState is JoinGroupUiState.PreviewReady) {
            GroupPreviewCard(
                group = groupPreview,
                isAuthenticated = isAuthenticated,
                isJoining = isJoining,
                onJoin = onJoin,
                onSignIn = onSignIn,
            )
        }
    }
}

/**
 * Group preview card shown after successful validation
 */
@Composable
private fun GroupPreviewCard(
    group: GroupPreview,
    isAuthenticated: Boolean,
    isJoining: Boolean,
    onJoin: () -> Unit,
    onSignIn: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Group,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )

            Text(
                text = group.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center,
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${group.memberCount} ${if (group.memberCount == 1) "member" else "members"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (isAuthenticated) {
                Button(
                    onClick = onJoin,
                    enabled = !isJoining,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (isJoining) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.join_group_joining))
                    } else {
                        Text(stringResource(R.string.join_group_button))
                    }
                }
            } else {
                Text(
                    text = stringResource(R.string.join_group_sign_in_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                )

                Button(
                    onClick = onSignIn,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.join_group_sign_in))
                }
            }
        }
    }
}

/**
 * Join confirmation dialog
 */
@Composable
private fun JoinConfirmationDialog(
    group: GroupPreview,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Default.Group, null, modifier = Modifier.size(32.dp))
        },
        title = { Text(stringResource(R.string.join_group_confirm_title, group.name)) },
        text = {
            Column {
                Text(stringResource(R.string.join_group_confirm_message, group.memberCount))
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.join_group_confirm_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) { Text(stringResource(R.string.join_group_confirm_button)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

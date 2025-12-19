package three.two.bit.phonemanager.ui.auth

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import three.two.bit.phonemanager.R
import three.two.bit.phonemanager.ui.auth.components.DeviceLinkConflictDialog
import three.two.bit.phonemanager.ui.groups.MigrationPromptDialog

/**
 * Story E9.11, Task 5: Login Screen UI
 *
 * AC E9.11.3: Email/password login with validation
 * AC E9.11.5: OAuth sign-in buttons (Google, Apple)
 * AC E9.11.7: Error handling and loading states
 *
 * Features:
 * - Email TextField with validation
 * - Password TextField with visibility toggle
 * - Sign In button
 * - Google Sign-In button
 * - Apple Sign-In button
 * - Create Account link
 * - Forgot Password link
 * - Loading indicator
 * - Error messages via Snackbar
 * - Story UGM-1.2: Device link conflict dialog
 */
@Composable
fun LoginScreen(
    onNavigateToRegister: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    onLoginSuccess: () -> Unit,
    onGoogleSignIn: () -> Unit = {},
    onAppleSignIn: () -> Unit = {},
    onNavigateToMigration: (groupId: String) -> Unit = {},
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val emailError by viewModel.emailError.collectAsState()
    val passwordError by viewModel.passwordError.collectAsState()

    // Story UGM-1.2: Device link state for conflict detection
    val deviceLinkState by viewModel.deviceLinkState.collectAsState()
    var showConflictDialog by rememberSaveable { mutableStateOf(false) }

    // Story UGM-4.2: Migration state for registration group migration
    val migrationState by viewModel.migrationState.collectAsState()

    // Feature flag states
    val isGoogleSignInEnabled by viewModel.isGoogleSignInEnabled.collectAsState()
    val isAppleSignInEnabled by viewModel.isAppleSignInEnabled.collectAsState()
    val isRegistrationEnabled by viewModel.isRegistrationEnabled.collectAsState()
    val isOAuthOnly by viewModel.isOAuthOnly.collectAsState()
    val isConfigLoaded by viewModel.isConfigLoaded.collectAsState()
    val isConfigLoading by viewModel.isConfigLoading.collectAsState()

    // Show retry banner when config failed to load (not loading and not loaded)
    val showConfigRetryBanner = !isConfigLoading && !isConfigLoaded

    // Show OAuth buttons only if at least one OAuth provider is enabled
    val showOAuthSection = isGoogleSignInEnabled || isAppleSignInEnabled

    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Story UGM-1.2: Show conflict dialog when device is already linked (AC 2)
    // Story UGM-1.4: Show snackbar when device link is queued for retry
    LaunchedEffect(deviceLinkState) {
        when (deviceLinkState) {
            is DeviceLinkState.AlreadyLinked -> {
                showConflictDialog = true
            }
            is DeviceLinkState.Queued -> {
                snackbarHostState.showSnackbar(
                    context.getString(R.string.device_link_queued_message),
                )
            }
            else -> { /* No action for other states */ }
        }
    }

    // Handle successful login - only navigate if no migration prompt pending
    LaunchedEffect(uiState, migrationState) {
        if (uiState is AuthUiState.Success) {
            // Wait for migration check to complete
            when (migrationState) {
                is MigrationState.NoMigration, is MigrationState.Dismissed -> {
                    // No migration needed or dismissed, proceed with login
                    onLoginSuccess()
                }
                is MigrationState.HasRegistrationGroup -> {
                    // Migration prompt will be shown via dialog
                    // Don't navigate yet
                }
                is MigrationState.Checking -> {
                    // Still checking, wait
                }
            }
        }
    }

    // Show error messages
    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Error) {
            snackbarHostState.showSnackbar((uiState as AuthUiState.Error).message)
        }
    }

    Scaffold(
        modifier = Modifier.testTag("login_screen"),
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Title
            Text(
                text = stringResource(R.string.auth_welcome_back),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.auth_sign_in_to_continue),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // Config retry banner - shown when config failed to load
            if (showConfigRetryBanner) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.auth_unable_to_load_server_settings),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(
                        onClick = { viewModel.refreshConfig() },
                    ) {
                        Text(stringResource(R.string.retry))
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Email/Password login section - hidden in OAuth-only mode
            if (!isOAuthOnly) {
                // Email TextField (AC E9.11.3, E9.11.7)
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text(stringResource(R.string.auth_email)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("email_input"),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next,
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) },
                    ),
                    singleLine = true,
                    isError = emailError != null,
                    supportingText = emailError?.let { { Text(it) } },
                    enabled = uiState !is AuthUiState.Loading,
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Password TextField (AC E9.11.3, E9.11.7)
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.auth_password)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("password_input"),
                    visualTransformation = if (passwordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            viewModel.login(email, password)
                        },
                    ),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) {
                                    Icons.Filled.Visibility
                                } else {
                                    Icons.Filled.VisibilityOff
                                },
                                contentDescription = if (passwordVisible) {
                                    stringResource(R.string.auth_hide_password)
                                } else {
                                    stringResource(R.string.auth_show_password)
                                },
                            )
                        }
                    },
                    singleLine = true,
                    isError = passwordError != null,
                    supportingText = passwordError?.let { { Text(it) } },
                    enabled = uiState !is AuthUiState.Loading,
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Forgot Password Link
                TextButton(
                    onClick = onNavigateToForgotPassword,
                    modifier = Modifier.align(Alignment.End),
                    enabled = uiState !is AuthUiState.Loading,
                ) {
                    Text(stringResource(R.string.auth_forgot_password))
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Sign In Button (AC E9.11.3)
                Button(
                    onClick = { viewModel.login(email, password) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("sign_in_button"),
                    enabled = uiState !is AuthUiState.Loading,
                ) {
                    if (uiState is AuthUiState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Text(stringResource(R.string.auth_sign_in))
                    }
                }
            }

            // Divider with "OR" - only shown when both email/password and OAuth are available
            if (!isOAuthOnly && showOAuthSection) {
                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f))
                    Text(
                        text = stringResource(R.string.common_or),
                        modifier = Modifier.padding(horizontal = 16.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // OAuth buttons section - shown when at least one provider is enabled
            if (showOAuthSection) {
                // Google Sign-In Button (AC E9.11.5)
                if (isGoogleSignInEnabled) {
                    OutlinedButton(
                        onClick = onGoogleSignIn,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        enabled = uiState !is AuthUiState.Loading,
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_google),
                            contentDescription = stringResource(R.string.auth_google),
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.auth_continue_google))
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Apple Sign-In Button (AC E9.11.5)
                if (isAppleSignInEnabled) {
                    OutlinedButton(
                        onClick = onAppleSignIn,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        enabled = uiState !is AuthUiState.Loading,
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_apple),
                            contentDescription = stringResource(R.string.auth_apple),
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.auth_continue_apple))
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Create Account Link - only shown when registration is enabled
            if (isRegistrationEnabled && !isOAuthOnly) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.auth_no_account_question),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    TextButton(
                        onClick = onNavigateToRegister,
                        modifier = Modifier.testTag("create_account_link"),
                        enabled = uiState !is AuthUiState.Loading,
                    ) {
                        Text(stringResource(R.string.auth_create_account))
                    }
                }
            }
        }
    }

    // Story UGM-1.2: Device link conflict dialog (AC 2, 3, 4)
    if (showConflictDialog) {
        DeviceLinkConflictDialog(
            onContinue = {
                // AC 3: User can proceed without device linking
                showConflictDialog = false
                viewModel.clearDeviceLinkState()
                onLoginSuccess()
            },
            onContactSupport = {
                // AC 4: Open support email
                val supportEmail = context.getString(R.string.support_email)
                val emailSubject = context.getString(R.string.support_email_subject_device_conflict)
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:$supportEmail")
                    putExtra(Intent.EXTRA_SUBJECT, emailSubject)
                }
                context.startActivity(intent)
            },
            onLogout = {
                // AC 4: Log out option
                showConflictDialog = false
                viewModel.logout()
            },
        )
    }

    // Story UGM-4.2: Migration prompt dialog (AC 1, 2, 3, 4, 5)
    val migrationGroupInfo = (migrationState as? MigrationState.HasRegistrationGroup)?.groupInfo
    if (migrationGroupInfo != null && uiState is AuthUiState.Success) {
        MigrationPromptDialog(
            groupInfo = migrationGroupInfo,
            onMigrate = {
                // AC 5: Navigate to migration screen
                viewModel.clearMigrationState()
                onNavigateToMigration(migrationGroupInfo.groupId)
            },
            onDismiss = {
                // AC 4: Dismiss and proceed without migration
                viewModel.dismissMigrationPrompt()
                onLoginSuccess()
            },
        )
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    MaterialTheme {
        LoginScreen(
            onNavigateToRegister = {},
            onNavigateToForgotPassword = {},
            onLoginSuccess = {},
        )
    }
}

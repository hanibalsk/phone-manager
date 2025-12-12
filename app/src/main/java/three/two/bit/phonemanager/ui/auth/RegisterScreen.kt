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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import three.two.bit.phonemanager.R

/**
 * Story E9.11, Task 6: Register Screen UI
 *
 * AC E9.11.4: User registration with validation
 * AC E9.11.7: Error handling and loading states
 *
 * Features:
 * - Display name TextField
 * - Email TextField with validation
 * - Password TextField with strength indicator
 * - Confirm password TextField
 * - Terms of service Checkbox
 * - Create Account button
 * - Sign In link
 * - Loading indicator
 * - Error messages via Snackbar
 */
@Composable
fun RegisterScreen(
    onNavigateToLogin: () -> Unit,
    onRegisterSuccess: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val emailError by viewModel.emailError.collectAsState()
    val passwordError by viewModel.passwordError.collectAsState()
    val displayNameError by viewModel.displayNameError.collectAsState()

    // Feature flag state - registration must be enabled
    val isRegistrationEnabled by viewModel.isRegistrationEnabled.collectAsState()

    // Redirect to login if registration is disabled
    LaunchedEffect(isRegistrationEnabled) {
        if (!isRegistrationEnabled) {
            onNavigateToLogin()
        }
    }

    var displayName by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var confirmPasswordVisible by rememberSaveable { mutableStateOf(false) }
    var acceptedTerms by rememberSaveable { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Password strength level (non-composable, can be used in derivedStateOf)
    val passwordStrengthLevel by remember {
        derivedStateOf {
            calculatePasswordStrengthLevel(password)
        }
    }

    // Password strength with colors (composable)
    val passwordStrength = passwordStrengthLevel.toPasswordStrength()

    // Password match check
    val passwordsMatch by remember {
        derivedStateOf {
            confirmPassword.isNotEmpty() && password == confirmPassword
        }
    }

    // Handle successful registration
    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Success) {
            onRegisterSuccess()
        }
    }

    // Show error messages
    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Error) {
            snackbarHostState.showSnackbar((uiState as AuthUiState.Error).message)
        }
    }

    Scaffold(
        modifier = Modifier.testTag("register_screen"),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Title
            Text(
                text = stringResource(R.string.auth_create_account),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.auth_join_today),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Display Name TextField (AC E9.11.4)
            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text(stringResource(R.string.auth_display_name)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("display_name_input"),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                singleLine = true,
                isError = displayNameError != null,
                supportingText = displayNameError?.let { { Text(it) } },
                enabled = uiState !is AuthUiState.Loading
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Email TextField (AC E9.11.4, E9.11.7)
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text(stringResource(R.string.auth_email)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("register_email_input"),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                singleLine = true,
                isError = emailError != null,
                supportingText = emailError?.let { { Text(it) } },
                enabled = uiState !is AuthUiState.Loading
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Password TextField with Strength Indicator (AC E9.11.4, E9.11.7)
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(stringResource(R.string.auth_password)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("register_password_input"),
                visualTransformation = if (passwordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
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
                                "Hide password"
                            } else {
                                "Show password"
                            }
                        )
                    }
                },
                singleLine = true,
                isError = passwordError != null,
                supportingText = {
                    Column {
                        passwordError?.let { Text(it) }
                        if (password.isNotEmpty() && passwordError == null) {
                            Text(
                                text = stringResource(R.string.auth_password_strength, passwordStrength.label),
                                color = passwordStrength.color,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                },
                enabled = uiState !is AuthUiState.Loading
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Confirm Password TextField
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text(stringResource(R.string.auth_confirm_password)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("confirm_password_input"),
                visualTransformation = if (confirmPasswordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { focusManager.clearFocus() }
                ),
                trailingIcon = {
                    Row {
                        if (confirmPassword.isNotEmpty()) {
                            Icon(
                                imageVector = if (passwordsMatch) {
                                    Icons.Filled.CheckCircle
                                } else {
                                    Icons.Filled.Error
                                },
                                contentDescription = null,
                                tint = if (passwordsMatch) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.error
                                }
                            )
                        }
                        IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                            Icon(
                                imageVector = if (confirmPasswordVisible) {
                                    Icons.Filled.Visibility
                                } else {
                                    Icons.Filled.VisibilityOff
                                },
                                contentDescription = if (confirmPasswordVisible) {
                                    "Hide password"
                                } else {
                                    "Show password"
                                }
                            )
                        }
                    }
                },
                singleLine = true,
                isError = confirmPassword.isNotEmpty() && !passwordsMatch,
                supportingText = {
                    if (confirmPassword.isNotEmpty() && !passwordsMatch) {
                        Text(stringResource(R.string.auth_passwords_mismatch))
                    }
                },
                enabled = uiState !is AuthUiState.Loading
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Terms of Service Checkbox (AC E9.11.4)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = acceptedTerms,
                    onCheckedChange = { acceptedTerms = it },
                    enabled = uiState !is AuthUiState.Loading
                )
                Text(
                    text = stringResource(R.string.auth_agree_terms),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Create Account Button (AC E9.11.4)
            Button(
                onClick = {
                    if (passwordsMatch && acceptedTerms) {
                        viewModel.register(email, password, displayName)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("create_account_button"),
                enabled = uiState !is AuthUiState.Loading &&
                    passwordsMatch &&
                    acceptedTerms &&
                    displayName.isNotBlank() &&
                    email.isNotBlank() &&
                    password.isNotBlank()
            ) {
                if (uiState is AuthUiState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(stringResource(R.string.auth_create_account))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Sign In Link
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.auth_already_have_account_question),
                    style = MaterialTheme.typography.bodyMedium
                )
                TextButton(
                    onClick = onNavigateToLogin,
                    enabled = uiState !is AuthUiState.Loading
                ) {
                    Text(stringResource(R.string.auth_sign_in))
                }
            }
        }
    }
}

/**
 * Password strength indicator
 */
data class PasswordStrength(
    val label: String,
    val color: androidx.compose.ui.graphics.Color
)

/**
 * Password strength levels (non-composable)
 */
enum class PasswordStrengthLevel {
    WEAK, MEDIUM, STRONG
}

/**
 * Calculate password strength level based on security requirements
 */
fun calculatePasswordStrengthLevel(password: String): PasswordStrengthLevel {
    return when {
        password.length < 8 -> PasswordStrengthLevel.WEAK
        password.length < 12 && password.any { it.isUpperCase() } && password.any { it.isDigit() } -> PasswordStrengthLevel.MEDIUM
        password.length >= 12 && password.any { it.isUpperCase() } && password.any { it.isDigit() } && password.any { !it.isLetterOrDigit() } -> PasswordStrengthLevel.STRONG
        else -> PasswordStrengthLevel.WEAK
    }
}

/**
 * Convert strength level to display model with colors (composable)
 */
@Composable
fun PasswordStrengthLevel.toPasswordStrength(): PasswordStrength {
    return when (this) {
        PasswordStrengthLevel.WEAK -> PasswordStrength("Weak", MaterialTheme.colorScheme.error)
        PasswordStrengthLevel.MEDIUM -> PasswordStrength("Medium", MaterialTheme.colorScheme.tertiary)
        PasswordStrengthLevel.STRONG -> PasswordStrength("Strong", MaterialTheme.colorScheme.primary)
    }
}

@Preview(showBackground = true)
@Composable
fun RegisterScreenPreview() {
    MaterialTheme {
        RegisterScreen(
            onNavigateToLogin = {},
            onRegisterSuccess = {}
        )
    }
}

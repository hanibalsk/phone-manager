@file:Suppress("DEPRECATION") // hiltViewModel() deprecation - using stable API

package three.two.bit.phonemanager.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import three.two.bit.phonemanager.R

/**
 * Story E9.11, Task 11: Forgot Password Screen
 *
 * Allows users to request a password reset link via email.
 *
 * Features:
 * - Email input with validation
 * - Send reset link button
 * - Success confirmation
 * - Error handling
 *
 * Uses ForgotPasswordViewModel for API calls (mock/real based on BuildConfig.USE_MOCK_AUTH)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(viewModel: ForgotPasswordViewModel = hiltViewModel(), onNavigateBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    var email by rememberSaveable { mutableStateOf("") }

    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Show error message in snackbar
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.auth_reset_password)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.back),
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
            verticalArrangement = Arrangement.Center,
        ) {
            if (uiState.isSuccess) {
                // Success State
                SuccessContent(
                    email = uiState.submittedEmail,
                    onNavigateBack = onNavigateBack,
                )
            } else {
                // Input State
                InputContent(
                    email = email,
                    onEmailChange = {
                        email = it
                        viewModel.clearEmailError()
                    },
                    emailError = uiState.emailError,
                    isLoading = uiState.isLoading,
                    onSubmit = {
                        focusManager.clearFocus()
                        viewModel.requestPasswordReset(email)
                    },
                    onNavigateBack = onNavigateBack,
                )
            }
        }
    }
}

@Composable
private fun SuccessContent(email: String, onNavigateBack: () -> Unit) {
    Icon(
        imageVector = Icons.Default.CheckCircle,
        contentDescription = stringResource(R.string.content_desc_success),
        modifier = Modifier.size(80.dp),
        tint = MaterialTheme.colorScheme.primary,
    )

    Spacer(modifier = Modifier.height(24.dp))

    Text(
        text = stringResource(R.string.auth_reset_link_sent_title),
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.primary,
    )

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = stringResource(R.string.auth_reset_link_sent_to),
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = email,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.primary,
        textAlign = TextAlign.Center,
    )

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = stringResource(R.string.auth_reset_link_sent_instructions),
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Spacer(modifier = Modifier.height(32.dp))

    Button(
        onClick = onNavigateBack,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(stringResource(R.string.auth_back_to_login))
    }
}

@Composable
private fun InputContent(
    email: String,
    onEmailChange: (String) -> Unit,
    emailError: String?,
    isLoading: Boolean,
    onSubmit: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    Text(
        text = stringResource(R.string.auth_forgot_password_title),
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.primary,
    )

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = stringResource(R.string.auth_forgot_password_instructions),
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Spacer(modifier = Modifier.height(32.dp))

    // Email TextField
    OutlinedTextField(
        value = email,
        onValueChange = onEmailChange,
        label = { Text(stringResource(R.string.auth_email)) },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(
            onDone = { onSubmit() },
        ),
        singleLine = true,
        isError = emailError != null,
        supportingText = emailError?.let { { Text(it) } },
        enabled = !isLoading,
    )

    Spacer(modifier = Modifier.height(24.dp))

    // Send Reset Link Button
    Button(
        onClick = onSubmit,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        enabled = !isLoading && email.isNotBlank(),
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MaterialTheme.colorScheme.onPrimary,
            )
        } else {
            Text(stringResource(R.string.auth_send_reset_link))
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = stringResource(R.string.auth_remember_password_question),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    TextButton(
        onClick = onNavigateBack,
        enabled = !isLoading,
    ) {
        Text(stringResource(R.string.auth_back_to_login))
    }
}

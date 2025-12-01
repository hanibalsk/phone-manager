package three.two.bit.phonemanager.ui.enrollment

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Story E13.10: Android Enrollment Flow - Enrollment Screen
 *
 * Screen for entering enrollment code and enrolling device.
 *
 * AC E13.10.2: Enrollment code input field, Scan QR button, Enroll button
 * AC E13.10.7: Error handling and display
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnrollmentScreen(
    onNavigateBack: () -> Unit,
    onEnrollmentSuccess: () -> Unit,
    onNavigateToQRScanner: () -> Unit,
    viewModel: EnrollmentViewModel = hiltViewModel(),
    initialToken: String? = null,
) {
    val enrollmentCode by viewModel.enrollmentCode.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val codeError by viewModel.codeError.collectAsState()
    val error by viewModel.error.collectAsState()

    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Handle initial token from deep link (AC E13.10.3)
    LaunchedEffect(initialToken) {
        initialToken?.let { token ->
            viewModel.updateEnrollmentCode(token)
        }
    }

    // Handle success navigation
    LaunchedEffect(uiState) {
        if (uiState is EnrollmentUiState.Success) {
            onEnrollmentSuccess()
        }
    }

    // Show error in snackbar
    LaunchedEffect(error) {
        error?.let { errorMessage ->
            snackbarHostState.showSnackbar(errorMessage)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Company Enrollment") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Header icon
            Icon(
                imageVector = Icons.Default.Business,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary,
            )

            // Title
            Text(
                text = "Enroll Your Device",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )

            // Help text (AC E13.10.2)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Text(
                    text = "Enter the enrollment code provided by your IT administrator, " +
                        "or scan the QR code from your enrollment email.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp),
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Enrollment code input (AC E13.10.2)
            OutlinedTextField(
                value = enrollmentCode,
                onValueChange = { viewModel.updateEnrollmentCode(it) },
                label = { Text("Enrollment Code") },
                placeholder = { Text("Enter 16-20 character code") },
                isError = codeError != null,
                supportingText = codeError?.let { { Text(it) } },
                singleLine = true,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        if (viewModel.isCodeValid()) {
                            viewModel.enrollDevice()
                        }
                    },
                ),
            )

            // Scan QR Code button (AC E13.10.2, E13.10.3)
            OutlinedButton(
                onClick = onNavigateToQRScanner,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp),
                )
                Text("Scan QR Code")
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Enroll button (AC E13.10.2)
            Button(
                onClick = { viewModel.enrollDevice() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading && viewModel.isCodeValid(),
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text("Enroll Device")
                }
            }

            // Error message (AC E13.10.7)
            if (uiState is EnrollmentUiState.Error) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = (uiState as EnrollmentUiState.Error).message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { viewModel.clearError() },
                        ) {
                            Text("Try Again")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Footer help text
            Text(
                text = "Contact your IT administrator if you don't have an enrollment code.",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

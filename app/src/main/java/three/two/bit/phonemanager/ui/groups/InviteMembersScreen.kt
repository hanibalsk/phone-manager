package three.two.bit.phonemanager.ui.groups

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import three.two.bit.phonemanager.domain.model.ExpiryUrgency
import three.two.bit.phonemanager.domain.model.GroupInvite
import three.two.bit.phonemanager.util.QRCodeGenerator

/**
 * Story E11.9 Task 6: InviteMembersScreen
 *
 * AC E11.9.1: Generate Invite Code
 * AC E11.9.2: Invite Code Display
 * AC E11.9.3: Share Invite
 *
 * Main screen for creating and sharing group invites.
 *
 * @param viewModel The InviteViewModel
 * @param onNavigateBack Callback to navigate back
 * @param onNavigateToPendingInvites Callback to view pending invites
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InviteMembersScreen(
    viewModel: InviteViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToPendingInvites: () -> Unit = {},
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentInvite by viewModel.currentInvite.collectAsStateWithLifecycle()
    val invites by viewModel.invites.collectAsStateWithLifecycle()
    val group by viewModel.group.collectAsStateWithLifecycle()
    val operationResult by viewModel.operationResult.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Handle operation results
    LaunchedEffect(operationResult) {
        when (val result = operationResult) {
            is InviteOperationResult.InviteCreated -> {
                snackbarHostState.showSnackbar("Invite code created!")
                viewModel.clearOperationResult()
            }
            is InviteOperationResult.InviteRevoked -> {
                snackbarHostState.showSnackbar("Invite revoked")
                viewModel.clearOperationResult()
            }
            is InviteOperationResult.Error -> {
                snackbarHostState.showSnackbar(result.message)
                viewModel.clearOperationResult()
            }
            else -> {}
        }
    }

    // Create an invite on first load if none exists
    LaunchedEffect(uiState, currentInvite) {
        if (uiState is InviteUiState.Success && currentInvite == null && invites.isEmpty()) {
            viewModel.createInvite()
        } else if (uiState is InviteUiState.Success && currentInvite == null && invites.isNotEmpty()) {
            // Use the first existing invite
            viewModel.setCurrentInvite(invites.first())
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Invite Members") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (invites.isNotEmpty()) {
                        TextButton(onClick = onNavigateToPendingInvites) {
                            Text("View All (${invites.size})")
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        when (uiState) {
            is InviteUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            is InviteUiState.Success -> {
                InviteContent(
                    invite = currentInvite,
                    groupName = group?.name ?: "Group",
                    isCreating = operationResult is InviteOperationResult.Creating,
                    onCopyCode = { code ->
                        copyToClipboard(context, code)
                        viewModel.clearOperationResult()
                    },
                    onShare = { invite ->
                        val shareContent = viewModel.getShareContent(invite)
                        shareInvite(context, shareContent)
                    },
                    onGenerateNew = { viewModel.createInvite() },
                    onShowSnackbar = { message ->
                        // Launch in a coroutine scope
                    },
                    snackbarHostState = snackbarHostState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                )
            }
            is InviteUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = (uiState as InviteUiState.Error).message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onNavigateBack) {
                            Text("Go Back")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Invite content showing code and sharing options
 */
@Composable
private fun InviteContent(
    invite: GroupInvite?,
    groupName: String,
    isCreating: Boolean,
    onCopyCode: (String) -> Unit,
    onShare: (GroupInvite) -> Unit,
    onGenerateNew: () -> Unit,
    onShowSnackbar: (String) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        if (invite != null && !isCreating) {
            // Invite code display
            InviteCodeCard(
                invite = invite,
                onCopyCode = {
                    onCopyCode(invite.code)
                    onShowSnackbar("Code copied to clipboard")
                },
            )

            // QR Code
            QRCodeCard(invite = invite)

            // Expiry indicator
            ExpiryIndicator(invite = invite)

            // Share button
            Button(
                onClick = { onShare(invite) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.Share, null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Share Invite")
            }

            // Copy code button
            FilledTonalButton(
                onClick = {
                    onCopyCode(invite.code)
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Copy Code")
            }

            // Generate new code button
            TextButton(
                onClick = onGenerateNew,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Generate New Code")
            }
        } else {
            // Creating state
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    CircularProgressIndicator()
                    Text(
                        text = "Creating invite code...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/**
 * Large invite code display card
 */
@Composable
private fun InviteCodeCard(
    invite: GroupInvite,
    onCopyCode: () -> Unit,
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Invite Code",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Large code display
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = invite.code,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 4.sp,
                    ),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(onClick = onCopyCode) {
                    Icon(
                        Icons.Default.ContentCopy,
                        "Copy code",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }

            // Uses info
            if (invite.isMultiUse()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (invite.maxUses == -1) {
                        "Unlimited uses"
                    } else {
                        "${invite.usesRemaining} of ${invite.maxUses} uses remaining"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                )
            }
        }
    }
}

/**
 * QR Code display card
 */
@Composable
private fun QRCodeCard(invite: GroupInvite) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                Icons.Default.QrCode2,
                null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Scan to Join",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Generate QR code bitmap
            val qrBitmap = remember(invite.code) {
                QRCodeGenerator.generateQRCode(invite.getDeepLink(), 300)
            }

            if (qrBitmap != null) {
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .background(Color.White)
                        .padding(8.dp),
                ) {
                    Image(
                        bitmap = qrBitmap.asImageBitmap(),
                        contentDescription = "QR Code for invite",
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "QR Code\nunavailable",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/**
 * Expiry countdown indicator
 */
@Composable
private fun ExpiryIndicator(invite: GroupInvite) {
    val urgency = invite.getExpiryUrgency()
    val remainingMs = invite.getTimeRemainingMillis()

    val (backgroundColor, textColor) = when (urgency) {
        ExpiryUrgency.NORMAL -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        ExpiryUrgency.WARNING -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        ExpiryUrgency.CRITICAL -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        ExpiryUrgency.EXPIRED -> MaterialTheme.colorScheme.error to MaterialTheme.colorScheme.onError
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = formatRemainingTime(remainingMs),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = textColor,
            )
        }
    }
}

/**
 * Format remaining time in human-readable form
 */
private fun formatRemainingTime(milliseconds: Long): String {
    if (milliseconds <= 0) return "Expired"

    val seconds = milliseconds / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        days > 0 -> "Expires in ${days} ${if (days == 1L) "day" else "days"}"
        hours > 0 -> "Expires in ${hours} ${if (hours == 1L) "hour" else "hours"}"
        minutes > 0 -> "Expires in ${minutes} ${if (minutes == 1L) "minute" else "minutes"}"
        else -> "Expires in less than a minute"
    }
}

/**
 * Copy text to clipboard
 */
private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Invite Code", text)
    clipboard.setPrimaryClip(clip)
}

/**
 * Share invite via Android share sheet
 */
private fun shareInvite(context: Context, content: ShareContent) {
    val shareText = "${content.message}\n\nOr use this link: ${content.deepLink}"

    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, shareText)
        putExtra(Intent.EXTRA_SUBJECT, "Join my Phone Manager group")
    }

    context.startActivity(Intent.createChooser(shareIntent, "Share invite via"))
}

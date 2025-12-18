package three.two.bit.phonemanager.ui.auth.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import three.two.bit.phonemanager.R

/**
 * Story UGM-1.2: Handle Device Already Linked to Different User
 *
 * Dialog shown when a user logs in on a device that is already linked to another account.
 * Provides options to continue without linking, contact support, or log out.
 *
 * AC 2: Display conflict message - "This device is linked to another account"
 * AC 3: Allow login continuation - Continue button lets user proceed without device linking
 * AC 4: Provide support options - Contact Support and Log out buttons
 */
@Composable
fun DeviceLinkConflictDialog(
    onContinue: () -> Unit,
    onContactSupport: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        modifier = modifier.testTag("device_link_conflict_dialog"),
        onDismissRequest = onContinue, // Allow dismissing by tapping outside
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
            )
        },
        title = {
            Text(
                text = stringResource(R.string.device_link_conflict_title),
                textAlign = TextAlign.Center,
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.device_link_conflict_message),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = stringResource(R.string.device_link_conflict_explanation),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = onContinue,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("device_link_conflict_continue"),
                ) {
                    Text(stringResource(R.string.device_link_conflict_continue))
                }
                OutlinedButton(
                    onClick = onContactSupport,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("device_link_conflict_support"),
                ) {
                    Text(stringResource(R.string.device_link_conflict_contact_support))
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onLogout,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .testTag("device_link_conflict_logout"),
            ) {
                Text(
                    text = stringResource(R.string.device_link_conflict_logout),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun DeviceLinkConflictDialogPreview() {
    MaterialTheme {
        DeviceLinkConflictDialog(
            onContinue = {},
            onContactSupport = {},
            onLogout = {},
        )
    }
}

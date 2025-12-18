package three.two.bit.phonemanager.ui.groups

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GroupWork
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import three.two.bit.phonemanager.R
import three.two.bit.phonemanager.domain.model.RegistrationGroupInfo

/**
 * Story UGM-4.2: Migration Prompt Dialog
 *
 * Displayed after login when a registration group is detected.
 * Prompts the user to migrate their registration group to an authenticated group.
 *
 * AC 1: Display group name and device count
 * AC 2: Show "Migrate Group" and "Not Now" buttons
 * AC 3: Include explanation of benefits
 *
 * @param groupInfo Information about the registration group
 * @param onMigrate Callback when user taps "Upgrade Group"
 * @param onDismiss Callback when user taps "Not Now"
 */
@Composable
fun MigrationPromptDialog(
    groupInfo: RegistrationGroupInfo,
    onMigrate: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.GroupWork,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        title = {
            Text(
                text = stringResource(R.string.migration_prompt_title),
                textAlign = TextAlign.Center,
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(
                        R.string.migration_prompt_message,
                        groupInfo.groupName,
                        groupInfo.deviceCount,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.migration_prompt_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        },
        confirmButton = {
            Button(onClick = onMigrate) {
                Text(stringResource(R.string.migration_prompt_migrate_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.migration_prompt_skip_button))
            }
        },
    )
}

package three.two.bit.phonemanager.ui.groups

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhonelinkRing
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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

/**
 * Story UGM-3.5: Device Assignment Dialog
 *
 * Prompts user to add their device to a group after joining.
 *
 * AC 1: Show prompt after join completes successfully
 * AC 2: "Add My Device" calls API to add device to group
 * AC 3: "Not Now" dismisses prompt and proceeds
 *
 * @param groupName The name of the group just joined
 * @param isAdding Whether the add device operation is in progress
 * @param onAddDevice Callback when user taps "Add My Device"
 * @param onSkip Callback when user taps "Not Now"
 */
@Composable
fun DeviceAssignmentDialog(
    groupName: String,
    isAdding: Boolean = false,
    onAddDevice: () -> Unit,
    onSkip: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!isAdding) onSkip() },
        icon = {
            Icon(
                imageVector = Icons.Default.PhonelinkRing,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        title = {
            Text(
                text = stringResource(R.string.device_assignment_title),
                textAlign = TextAlign.Center,
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.device_assignment_message, groupName),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.device_assignment_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onAddDevice,
                enabled = !isAdding,
            ) {
                if (isAdding) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text(stringResource(R.string.device_assignment_add_button))
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onSkip,
                enabled = !isAdding,
            ) {
                Text(stringResource(R.string.device_assignment_skip_button))
            }
        },
    )
}

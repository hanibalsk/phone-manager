package three.two.bit.phonemanager.ui.devices

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Story E10.6 Task 6: Device Link Dialog
 *
 * AC E10.6.2: Device link flow confirmation dialog
 * - Shows device info being linked
 * - Explains what linking means (privacy, management)
 * - Add "Link Device" and "Cancel" buttons
 * - Calls viewModel.linkCurrentDevice() on confirm
 *
 * @param onDismiss Callback when dialog is dismissed
 * @param onConfirm Callback with display name and isPrimary when user confirms
 */
@Composable
fun LinkDeviceDialog(
    onDismiss: () -> Unit,
    onConfirm: (displayName: String?, isPrimary: Boolean) -> Unit,
) {
    var displayName by remember { mutableStateOf("") }
    var isPrimary by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Link This Device",
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Explanation
                Text(
                    text = "Link this device to your account to:",
                    style = MaterialTheme.typography.bodyMedium,
                )

                Column(
                    modifier = Modifier.padding(start = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    BulletPoint("Manage it from other devices")
                    BulletPoint("View it in your device list")
                    BulletPoint("Transfer ownership to others")
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Display name input
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Device Name (optional)") },
                    placeholder = { Text("e.g., My Phone") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                // Primary device checkbox
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = isPrimary,
                        onCheckedChange = { isPrimary = it },
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Set as primary device",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = "Primary device is used for notifications",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // Privacy notice
                Text(
                    text = "This device's location will be visible to you from your other devices.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        displayName.ifBlank { null },
                        isPrimary,
                    )
                },
            ) {
                Text("Link Device")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

/**
 * Simple bullet point text
 */
@Composable
private fun BulletPoint(text: String) {
    Row {
        Text(
            text = "•",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(end = 8.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

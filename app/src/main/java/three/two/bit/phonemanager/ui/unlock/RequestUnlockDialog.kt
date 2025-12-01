package three.two.bit.phonemanager.ui.unlock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Story E12.8: Request Unlock Dialog
 *
 * Dialog for requesting to unlock a locked setting.
 *
 * AC E12.8.1: Request Unlock from Locked Setting
 * AC E12.8.2: Submit Unlock Request
 */
@Composable
fun RequestUnlockDialog(
    settingName: String,
    reason: String,
    onReasonChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
    isSubmitting: Boolean,
    isValid: Boolean,
    errorMessage: String?,
    remainingCharacters: Int,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        modifier = modifier,
        icon = {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        title = {
            Text(
                text = "Request Unlock",
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        text = {
            Column {
                Text(
                    text = "Request to unlock \"$settingName\"",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Please provide a reason for this request. The admin will review your request and respond.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = reason,
                    onValueChange = onReasonChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Reason") },
                    placeholder = { Text("Why do you need to change this setting?") },
                    supportingText = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            if (errorMessage != null) {
                                Text(
                                    text = errorMessage,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            } else {
                                Spacer(modifier = Modifier.width(1.dp))
                            }
                            Text(
                                text = "$remainingCharacters characters remaining",
                                textAlign = TextAlign.End,
                            )
                        }
                    },
                    isError = errorMessage != null,
                    minLines = 3,
                    maxLines = 5,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                    ),
                    enabled = !isSubmitting,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onSubmit,
                enabled = isValid && !isSubmitting,
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .height(16.dp)
                            .width(16.dp),
                        strokeWidth = 2.dp,
                    )
                }
                Text(if (isSubmitting) "Submitting..." else "Submit Request")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSubmitting,
            ) {
                Text("Cancel")
            }
        },
    )
}


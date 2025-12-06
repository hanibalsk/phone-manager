package three.two.bit.phonemanager.ui.groups

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import three.two.bit.phonemanager.R

/**
 * Story E11.8 Task 7: Create Group Dialog
 *
 * AC E11.8.2: Create Group
 * - Dialog with name (required) and description (optional) fields
 * - Name field with 50 character max
 * - Create button calls createGroup API
 * - Success returns to group list with new group shown
 *
 * @param onDismiss Callback when dialog is dismissed
 * @param onConfirm Callback with group name and description when user confirms
 */
@Composable
fun CreateGroupDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, description: String?) -> Unit,
) {
    var groupName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf<String?>(null) }

    val errorRequired = stringResource(R.string.create_group_name_required)
    val errorTooLong = stringResource(R.string.create_group_name_too_long)

    // Validate name
    fun validateName(): Boolean {
        return when {
            groupName.isBlank() -> {
                nameError = errorRequired
                false
            }
            groupName.length > 50 -> {
                nameError = errorTooLong
                false
            }
            else -> {
                nameError = null
                true
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.create_group_title),
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
                    text = stringResource(R.string.create_group_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                // Group name input (required)
                OutlinedTextField(
                    value = groupName,
                    onValueChange = {
                        if (it.length <= 50) {
                            groupName = it
                            nameError = null
                        }
                    },
                    label = { Text(stringResource(R.string.create_group_name_label)) },
                    placeholder = { Text(stringResource(R.string.create_group_name_placeholder)) },
                    singleLine = true,
                    isError = nameError != null,
                    supportingText = {
                        if (nameError != null) {
                            Text(
                                text = nameError!!,
                                color = MaterialTheme.colorScheme.error,
                            )
                        } else {
                            Text(
                                text = "${groupName.length}/50",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )

                // Description input (optional)
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.create_group_description_label)) },
                    placeholder = { Text(stringResource(R.string.create_group_description_placeholder)) },
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (validateName()) {
                        onConfirm(
                            groupName.trim(),
                            description.trim().ifBlank { null },
                        )
                    }
                },
                enabled = groupName.isNotBlank(),
            ) {
                Text(stringResource(R.string.create_group_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

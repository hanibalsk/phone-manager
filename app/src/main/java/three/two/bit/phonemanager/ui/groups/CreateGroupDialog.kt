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
import androidx.compose.ui.unit.dp

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

    // Validate name
    fun validateName(): Boolean {
        return when {
            groupName.isBlank() -> {
                nameError = "Group name is required"
                false
            }
            groupName.length > 50 -> {
                nameError = "Group name must be 50 characters or less"
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
                text = "Create Group",
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
                    text = "Create a group to share device locations with family and friends.",
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
                    label = { Text("Group Name *") },
                    placeholder = { Text("e.g., Family") },
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
                    label = { Text("Description (optional)") },
                    placeholder = { Text("What's this group for?") },
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
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

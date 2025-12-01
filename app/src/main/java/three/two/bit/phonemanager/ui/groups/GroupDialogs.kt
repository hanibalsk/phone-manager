package three.two.bit.phonemanager.ui.groups

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import three.two.bit.phonemanager.domain.model.GroupMembership
import three.two.bit.phonemanager.domain.model.GroupRole

/**
 * Story E11.8 Task 10: Group Dialogs
 *
 * Various confirmation and editing dialogs for group management:
 * - DeleteGroupDialog (AC E11.8.7)
 * - LeaveGroupDialog (AC E11.8.6)
 * - EditGroupNameDialog (AC E11.8.8)
 * - EditGroupDescriptionDialog (AC E11.8.8)
 * - ChangeRoleDialog (AC E11.8.5)
 * - RemoveMemberDialog (AC E11.8.5)
 * - TransferGroupOwnershipDialog (AC E11.8.5)
 */

/**
 * AC E11.8.7: Delete group confirmation dialog
 */
@Composable
fun DeleteGroupDialog(
    groupName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Delete Group?")
        },
        text = {
            Column {
                Text(
                    text = "Are you sure you want to delete \"$groupName\"?",
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "This action cannot be undone. All members will be removed from the group.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text("Delete")
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
 * AC E11.8.6: Leave group confirmation dialog
 */
@Composable
fun LeaveGroupDialog(
    groupName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Leave Group?")
        },
        text = {
            Column {
                Text(
                    text = "Are you sure you want to leave \"$groupName\"?",
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "You will lose access to shared device locations and will need to be re-invited to rejoin.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text("Leave")
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
 * AC E11.8.8: Edit group name dialog
 */
@Composable
fun EditGroupNameDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var newName by remember { mutableStateOf(currentName) }
    var nameError by remember { mutableStateOf<String?>(null) }

    fun validateName(): Boolean {
        return when {
            newName.isBlank() -> {
                nameError = "Group name is required"
                false
            }
            newName.length > 50 -> {
                nameError = "Group name must be 50 characters or less"
                false
            }
            newName.trim() == currentName -> {
                nameError = "Name hasn't changed"
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
            Text("Edit Group Name")
        },
        text = {
            OutlinedTextField(
                value = newName,
                onValueChange = {
                    if (it.length <= 50) {
                        newName = it
                        nameError = null
                    }
                },
                label = { Text("Group Name") },
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
                            text = "${newName.length}/50",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (validateName()) {
                        onConfirm(newName.trim())
                    }
                },
                enabled = newName.isNotBlank() && newName.trim() != currentName,
            ) {
                Text("Save")
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
 * AC E11.8.8: Edit group description dialog
 */
@Composable
fun EditGroupDescriptionDialog(
    currentDescription: String?,
    onDismiss: () -> Unit,
    onConfirm: (String?) -> Unit,
) {
    var newDescription by remember { mutableStateOf(currentDescription ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Edit Description")
        },
        text = {
            OutlinedTextField(
                value = newDescription,
                onValueChange = { newDescription = it },
                label = { Text("Description (optional)") },
                placeholder = { Text("What's this group for?") },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val trimmed = newDescription.trim()
                    onConfirm(trimmed.ifBlank { null })
                },
            ) {
                Text("Save")
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
 * AC E11.8.5: Change member role dialog
 */
@Composable
fun ChangeRoleDialog(
    member: GroupMembership,
    onDismiss: () -> Unit,
    onChangeRole: (GroupRole) -> Unit,
) {
    val currentRole = member.role
    var selectedRole by remember {
        mutableStateOf(
            if (currentRole == GroupRole.ADMIN) GroupRole.MEMBER else GroupRole.ADMIN
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Change Role")
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Select a new role for ${member.displayName}:",
                    style = MaterialTheme.typography.bodyMedium,
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Admin option
                RoleOption(
                    title = "Admin",
                    description = "Can manage members and group settings",
                    selected = selectedRole == GroupRole.ADMIN,
                    enabled = currentRole != GroupRole.ADMIN,
                    onClick = { selectedRole = GroupRole.ADMIN },
                )

                // Member option
                RoleOption(
                    title = "Member",
                    description = "Can view shared device locations",
                    selected = selectedRole == GroupRole.MEMBER,
                    enabled = currentRole != GroupRole.MEMBER,
                    onClick = { selectedRole = GroupRole.MEMBER },
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onChangeRole(selectedRole) },
                enabled = selectedRole != currentRole,
            ) {
                Text("Change Role")
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
 * Role selection option
 */
@Composable
private fun RoleOption(
    title: String,
    description: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            enabled = enabled,
        )

        Column(
            modifier = Modifier.padding(start = 12.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                },
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                },
            )
        }
    }
}

/**
 * AC E11.8.5: Remove member confirmation dialog
 */
@Composable
fun RemoveMemberDialog(
    memberName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Remove Member?")
        },
        text = {
            Column {
                Text(
                    text = "Are you sure you want to remove \"$memberName\" from the group?",
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "They will lose access to shared device locations and will need to be re-invited to rejoin.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text("Remove")
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
 * AC E11.8.5: Transfer group ownership dialog
 */
@Composable
fun TransferGroupOwnershipDialog(
    memberName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Transfer Ownership?")
        },
        text = {
            Column {
                Text(
                    text = "Are you sure you want to transfer ownership of this group to \"$memberName\"?",
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "You will become an admin and they will become the owner. Only they will be able to delete the group.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text("Transfer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import three.two.bit.phonemanager.R
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
fun DeleteGroupDialog(groupName: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.group_dialog_delete_title))
        },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.group_dialog_delete_confirm, groupName),
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.group_dialog_delete_message),
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
                Text(stringResource(R.string.delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

/**
 * AC E11.8.6: Leave group confirmation dialog
 */
@Composable
fun LeaveGroupDialog(groupName: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.group_dialog_leave_title))
        },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.group_dialog_leave_confirm, groupName),
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.group_dialog_leave_message),
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
                Text(stringResource(R.string.group_dialog_leave))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

/**
 * AC E11.8.8: Edit group name dialog
 */
@Composable
fun EditGroupNameDialog(currentName: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var newName by remember { mutableStateOf(currentName) }
    var nameError by remember { mutableStateOf<String?>(null) }

    // Pre-load error strings at composable level
    val errorNameRequired = stringResource(R.string.group_dialog_name_required)
    val errorNameTooLong = stringResource(R.string.group_dialog_name_too_long)
    val errorNameUnchanged = stringResource(R.string.group_dialog_name_unchanged)

    fun validateName(): Boolean = when {
        newName.isBlank() -> {
            nameError = errorNameRequired
            false
        }
        newName.length > 50 -> {
            nameError = errorNameTooLong
            false
        }
        newName.trim() == currentName -> {
            nameError = errorNameUnchanged
            false
        }
        else -> {
            nameError = null
            true
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.group_dialog_edit_name_title))
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
                label = { Text(stringResource(R.string.group_dialog_group_name)) },
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
                            text = stringResource(R.string.group_dialog_name_counter, newName.length),
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
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

/**
 * AC E11.8.8: Edit group description dialog
 */
@Composable
fun EditGroupDescriptionDialog(currentDescription: String?, onDismiss: () -> Unit, onConfirm: (String?) -> Unit) {
    var newDescription by remember { mutableStateOf(currentDescription ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.group_dialog_edit_description_title))
        },
        text = {
            OutlinedTextField(
                value = newDescription,
                onValueChange = { newDescription = it },
                label = { Text(stringResource(R.string.group_dialog_description_optional)) },
                placeholder = { Text(stringResource(R.string.group_dialog_description_hint)) },
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
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

/**
 * AC E11.8.5: Change member role dialog
 */
@Composable
fun ChangeRoleDialog(member: GroupMembership, onDismiss: () -> Unit, onChangeRole: (GroupRole) -> Unit) {
    val currentRole = member.role
    var selectedRole by remember {
        mutableStateOf(
            if (currentRole == GroupRole.ADMIN) GroupRole.MEMBER else GroupRole.ADMIN,
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.group_dialog_change_role_title))
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(R.string.group_dialog_select_new_role_for, member.displayName),
                    style = MaterialTheme.typography.bodyMedium,
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Admin option
                RoleOption(
                    title = stringResource(R.string.group_role_admin),
                    description = stringResource(R.string.group_role_admin_desc),
                    selected = selectedRole == GroupRole.ADMIN,
                    enabled = currentRole != GroupRole.ADMIN,
                    onClick = { selectedRole = GroupRole.ADMIN },
                )

                // Member option
                RoleOption(
                    title = stringResource(R.string.group_role_member),
                    description = stringResource(R.string.group_role_member_desc),
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
                Text(stringResource(R.string.group_dialog_change_role))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

/**
 * Role selection option
 */
@Composable
private fun RoleOption(title: String, description: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
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
fun RemoveMemberDialog(memberName: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.group_dialog_remove_member_title))
        },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.group_dialog_remove_member_confirm, memberName),
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.group_dialog_remove_member_warning),
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
                Text(stringResource(R.string.group_dialog_remove))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

/**
 * AC E11.8.5: Transfer group ownership dialog
 */
@Composable
fun TransferGroupOwnershipDialog(memberName: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.group_dialog_transfer_title))
        },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.group_dialog_transfer_confirm, memberName),
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.group_dialog_transfer_warning),
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
                Text(stringResource(R.string.group_dialog_transfer))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

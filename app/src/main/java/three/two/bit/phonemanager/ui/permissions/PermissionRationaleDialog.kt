package three.two.bit.phonemanager.ui.permissions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import three.two.bit.phonemanager.R
import three.two.bit.phonemanager.ui.theme.PhoneManagerTheme

/**
 * Story 1.2: PermissionRationaleDialog - Material 3 dialog explaining permission needs
 */
@Composable
fun PermissionRationaleDialog(
    type: PermissionType,
    onAccept: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = when (type) {
                    PermissionType.LOCATION -> Icons.Default.LocationOn
                    PermissionType.BACKGROUND -> Icons.Default.MyLocation
                    PermissionType.NOTIFICATION -> Icons.Default.Notifications
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        title = {
            Text(
                text = when (type) {
                    PermissionType.LOCATION -> stringResource(R.string.permission_location_title)
                    PermissionType.BACKGROUND -> stringResource(R.string.permission_background_title)
                    PermissionType.NOTIFICATION -> stringResource(R.string.permission_notification_title)
                },
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = when (type) {
                        PermissionType.LOCATION ->
                            stringResource(R.string.permission_location_rationale)

                        PermissionType.BACKGROUND ->
                            stringResource(R.string.permission_background_rationale)

                        PermissionType.NOTIFICATION ->
                            stringResource(R.string.permission_notification_rationale)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = stringResource(R.string.permission_privacy_notice),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontStyle = FontStyle.Italic,
                )
            }
        },
        confirmButton = {
            Button(onClick = onAccept) {
                Text(stringResource(R.string.permission_continue))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.permission_not_now))
            }
        },
        modifier = modifier,
    )
}

enum class PermissionType {
    LOCATION,
    BACKGROUND,
    NOTIFICATION,
}

@Preview(showBackground = true)
@Composable
fun PermissionRationaleDialogPreview() {
    PhoneManagerTheme {
        PermissionRationaleDialog(
            type = PermissionType.LOCATION,
            onAccept = {},
            onDismiss = {},
        )
    }
}

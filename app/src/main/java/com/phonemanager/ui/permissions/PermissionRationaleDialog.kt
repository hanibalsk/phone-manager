package com.phonemanager.ui.permissions

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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.phonemanager.ui.theme.PhoneManagerTheme

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
                    PermissionType.LOCATION -> "Location Permission Required"
                    PermissionType.BACKGROUND -> "Background Location Access"
                    PermissionType.NOTIFICATION -> "Notification Permission"
                },
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = when (type) {
                        PermissionType.LOCATION ->
                            "Phone Manager needs access to your location to track your device's position."
                        PermissionType.BACKGROUND ->
                            "To continue tracking your location when the app is closed or in the background, " +
                                "please allow location access \"All the time\" on the next screen."
                        PermissionType.NOTIFICATION ->
                            "A persistent notification is required while location tracking is active. " +
                                "This helps you know when tracking is running and provides quick access to stop it."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Your privacy: Location data is stored on your device and only sent to " +
                        "endpoints you configure.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontStyle = FontStyle.Italic,
                )
            }
        },
        confirmButton = {
            Button(onClick = onAccept) {
                Text("Continue")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Not Now")
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

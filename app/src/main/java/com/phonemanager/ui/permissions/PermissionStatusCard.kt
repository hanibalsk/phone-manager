package com.phonemanager.ui.permissions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.phonemanager.permission.PermissionState
import com.phonemanager.ui.theme.PhoneManagerTheme

/**
 * Story 1.2: PermissionStatusCard - Displays current permission status with action buttons
 */
@Composable
fun PermissionStatusCard(
    permissionState: PermissionState,
    onGrantPermissions: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (permissionState) {
                is PermissionState.AllGranted -> MaterialTheme.colorScheme.primaryContainer
                is PermissionState.LocationDenied -> MaterialTheme.colorScheme.errorContainer
                is PermissionState.BackgroundDenied -> MaterialTheme.colorScheme.tertiaryContainer
                is PermissionState.NotificationDenied -> MaterialTheme.colorScheme.tertiaryContainer
                is PermissionState.PermanentlyDenied -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = when (permissionState) {
                            is PermissionState.AllGranted -> Icons.Default.CheckCircle
                            is PermissionState.LocationDenied -> Icons.Default.Cancel
                            is PermissionState.BackgroundDenied -> Icons.Default.Warning
                            is PermissionState.NotificationDenied -> Icons.Default.Warning
                            is PermissionState.PermanentlyDenied -> Icons.Default.Block
                            else -> Icons.Default.Info
                        },
                        contentDescription = null,
                        tint = when (permissionState) {
                            is PermissionState.AllGranted -> MaterialTheme.colorScheme.primary
                            is PermissionState.LocationDenied -> MaterialTheme.colorScheme.error
                            is PermissionState.BackgroundDenied -> MaterialTheme.colorScheme.tertiary
                            is PermissionState.NotificationDenied -> MaterialTheme.colorScheme.tertiary
                            is PermissionState.PermanentlyDenied -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )

                    Text(
                        text = when (permissionState) {
                            is PermissionState.AllGranted -> "All Permissions Granted"
                            is PermissionState.LocationDenied -> "Location Permission Denied"
                            is PermissionState.BackgroundDenied -> "Background Location Restricted"
                            is PermissionState.NotificationDenied -> "Notification Permission Denied"
                            is PermissionState.PermanentlyDenied -> "Permission Blocked"
                            else -> "Checking Permissions..."
                        },
                        style = MaterialTheme.typography.titleSmall
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = when (permissionState) {
                        is PermissionState.AllGranted ->
                            "Location tracking is available"
                        is PermissionState.LocationDenied ->
                            "Grant location permission to enable tracking"
                        is PermissionState.BackgroundDenied ->
                            "Tracking only works when app is open"
                        is PermissionState.NotificationDenied ->
                            "Notification required for tracking service"
                        is PermissionState.PermanentlyDenied ->
                            "Enable permission in Settings"
                        else -> "Loading..."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (permissionState !is PermissionState.AllGranted &&
                permissionState !is PermissionState.Checking) {
                Button(
                    onClick = if (permissionState is PermissionState.PermanentlyDenied) {
                        onOpenSettings
                    } else {
                        onGrantPermissions
                    }
                ) {
                    Text(
                        text = if (permissionState is PermissionState.PermanentlyDenied) {
                            "Open Settings"
                        } else {
                            "Grant"
                        }
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PermissionStatusCardPreview() {
    PhoneManagerTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PermissionStatusCard(
                permissionState = PermissionState.AllGranted,
                onGrantPermissions = {},
                onOpenSettings = {}
            )

            PermissionStatusCard(
                permissionState = PermissionState.LocationDenied,
                onGrantPermissions = {},
                onOpenSettings = {}
            )

            PermissionStatusCard(
                permissionState = PermissionState.BackgroundDenied(foregroundGranted = true),
                onGrantPermissions = {},
                onOpenSettings = {}
            )
        }
    }
}

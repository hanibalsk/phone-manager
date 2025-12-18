package three.two.bit.phonemanager.ui.settings.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import three.two.bit.phonemanager.R
import three.two.bit.phonemanager.ui.settings.DeviceLinkInfo
import java.text.DateFormat
import java.util.Date

/**
 * Story UGM-1.3: Device Link Info Card
 *
 * Displays device ownership status in the Settings screen.
 * Shows linked email for authenticated users or "Not linked" status.
 * Tapping shows detailed device info in a bottom sheet.
 *
 * AC 1: Display linked account for authenticated users
 * AC 2: Display unlinked status
 * AC 3: Show device details
 * AC 4: Detailed device info on tap
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceLinkInfoCard(
    deviceLinkInfo: DeviceLinkInfo,
    modifier: Modifier = Modifier,
) {
    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("device_link_info_card")
            .clickable { showBottomSheet = true },
        colors = CardDefaults.cardColors(
            containerColor = if (deviceLinkInfo.isLinked) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (deviceLinkInfo.isLinked) {
                    Icons.Default.Link
                } else {
                    Icons.Default.LinkOff
                },
                contentDescription = null,
                modifier = Modifier
                    .size(24.dp)
                    .padding(end = 4.dp),
                tint = if (deviceLinkInfo.isLinked) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
            ) {
                Text(
                    text = if (deviceLinkInfo.isLinked) {
                        stringResource(R.string.device_link_status_linked)
                    } else {
                        stringResource(R.string.device_link_status_not_linked)
                    },
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                if (deviceLinkInfo.isLinked && deviceLinkInfo.linkedEmail != null) {
                    Text(
                        text = deviceLinkInfo.linkedEmail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Text(
                    text = stringResource(R.string.device_link_tap_for_details),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }

    // AC 4: Bottom sheet with detailed device info
    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState,
        ) {
            DeviceLinkInfoBottomSheet(
                deviceLinkInfo = deviceLinkInfo,
                modifier = Modifier.padding(bottom = 32.dp),
            )
        }
    }
}

/**
 * Story UGM-1.3: Device Link Info Bottom Sheet
 *
 * Shows detailed device information including:
 * - Device ID (truncated)
 * - Linked account email
 * - Link timestamp
 * - Device name
 *
 * AC 4: Detailed device info on tap
 */
@Composable
private fun DeviceLinkInfoBottomSheet(
    deviceLinkInfo: DeviceLinkInfo,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
    ) {
        Text(
            text = stringResource(R.string.device_link_info_title),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp),
        )

        // Device Name
        InfoRow(
            label = stringResource(R.string.device_link_info_device_name),
            value = deviceLinkInfo.deviceName ?: stringResource(R.string.device_link_info_unnamed),
        )

        // Device ID (truncated for display)
        val truncatedDeviceId = if (deviceLinkInfo.deviceId.length > 16) {
            "${deviceLinkInfo.deviceId.take(8)}...${deviceLinkInfo.deviceId.takeLast(4)}"
        } else {
            deviceLinkInfo.deviceId
        }
        InfoRow(
            label = stringResource(R.string.device_link_info_device_id),
            value = truncatedDeviceId,
        )

        // Link Status
        InfoRow(
            label = stringResource(R.string.device_link_info_status),
            value = if (deviceLinkInfo.isLinked) {
                stringResource(R.string.device_link_status_linked)
            } else {
                stringResource(R.string.device_link_status_not_linked)
            },
        )

        // Linked Account
        if (deviceLinkInfo.isLinked && deviceLinkInfo.linkedEmail != null) {
            InfoRow(
                label = stringResource(R.string.device_link_info_linked_to),
                value = deviceLinkInfo.linkedEmail,
            )
        }

        // Link Timestamp
        if (deviceLinkInfo.linkedAt != null) {
            val dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
            val formattedDate = dateFormat.format(Date(deviceLinkInfo.linkedAt))
            InfoRow(
                label = stringResource(R.string.device_link_info_linked_at),
                value = formattedDate,
            )
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.4f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(0.6f),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DeviceLinkInfoCardLinkedPreview() {
    MaterialTheme {
        DeviceLinkInfoCard(
            deviceLinkInfo = DeviceLinkInfo(
                isLinked = true,
                linkedEmail = "user@example.com",
                linkedAt = System.currentTimeMillis(),
                deviceName = "My Phone",
                deviceId = "abc123-def456-ghi789-jkl012",
            ),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DeviceLinkInfoCardNotLinkedPreview() {
    MaterialTheme {
        DeviceLinkInfoCard(
            deviceLinkInfo = DeviceLinkInfo(
                isLinked = false,
                linkedEmail = null,
                linkedAt = null,
                deviceName = "Unknown Device",
                deviceId = "abc123-def456-ghi789-jkl012",
            ),
        )
    }
}

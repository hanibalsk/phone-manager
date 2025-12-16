package three.two.bit.phonemanager.ui.unlock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pending
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import three.two.bit.phonemanager.R
import three.two.bit.phonemanager.domain.model.UnlockRequest
import three.two.bit.phonemanager.domain.model.UnlockRequestStatus

/**
 * Story E12.8: Request Detail Dialog
 *
 * Shows full details of an unlock request.
 *
 * AC E12.8.3: View My Unlock Requests
 * AC E12.8.4: Withdraw Unlock Request
 * AC E12.8.6: Admin Response Display
 */
@Composable
fun RequestDetailDialog(
    request: UnlockRequest,
    onDismiss: () -> Unit,
    onWithdraw: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
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
                text = request.getSettingDisplayName(),
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        text = {
            Column {
                // Status
                DetailRow(
                    label = stringResource(R.string.unlock_status_label),
                    content = {
                        StatusDisplay(status = request.status)
                    },
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Reason
                DetailRow(
                    label = stringResource(R.string.unlock_reason_label),
                    value = request.reason,
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Requested at
                DetailRow(
                    label = stringResource(R.string.unlock_requested_label),
                    value = formatInstant(request.createdAt),
                )

                // Admin response section for decided requests
                if (request.isDecided()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))

                    AdminResponseDetails(request = request)
                }
            }
        },
        confirmButton = {
            if (request.canWithdraw()) {
                TextButton(onClick = onWithdraw) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.unlock_withdraw))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.unlock_close))
            }
        },
    )
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun DetailRow(
    label: String,
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(4.dp))
        content()
    }
}

@Composable
private fun StatusDisplay(
    status: UnlockRequestStatus,
    modifier: Modifier = Modifier,
) {
    val (icon, color, text) = when (status) {
        UnlockRequestStatus.PENDING -> Triple(
            Icons.Default.Pending,
            MaterialTheme.colorScheme.tertiary,
            stringResource(R.string.unlock_status_pending_detail),
        )
        UnlockRequestStatus.APPROVED -> Triple(
            Icons.Default.Check,
            MaterialTheme.colorScheme.primary,
            stringResource(R.string.unlock_status_approved_detail),
        )
        UnlockRequestStatus.DENIED -> Triple(
            Icons.Default.Clear,
            MaterialTheme.colorScheme.error,
            stringResource(R.string.unlock_status_denied_detail),
        )
        UnlockRequestStatus.WITHDRAWN -> Triple(
            Icons.AutoMirrored.Filled.Undo,
            MaterialTheme.colorScheme.outline,
            stringResource(R.string.unlock_status_withdrawn_detail),
        )
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = color,
        )
    }
}

/**
 * AC E12.8.6: Admin Response Display
 */
@Composable
private fun AdminResponseDetails(
    request: UnlockRequest,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (request.isApproved()) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
            },
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text(
                text = stringResource(
                    if (request.isApproved()) R.string.unlock_status_approved else R.string.unlock_status_denied,
                ),
                style = MaterialTheme.typography.titleMedium,
                color = if (request.isApproved()) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                },
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Responded by
            Text(
                text = stringResource(
                    R.string.unlock_response_by,
                    request.respondedByName ?: stringResource(R.string.unlock_admin_fallback),
                ),
                style = MaterialTheme.typography.bodyMedium,
            )

            // Response message
            if (!request.response.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.unlock_response_message_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = request.response,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            // Responded at
            request.respondedAt?.let { respondedAt ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(
                        R.string.unlock_response_responded,
                        formatInstant(respondedAt),
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun formatInstant(instant: Instant): String {
    val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    return "${localDateTime.monthNumber}/${localDateTime.day}/${localDateTime.year} " +
        "${localDateTime.hour.toString().padStart(2, '0')}:${localDateTime.minute.toString().padStart(2, '0')}"
}

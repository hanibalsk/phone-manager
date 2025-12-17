package three.two.bit.phonemanager.ui.settings

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import three.two.bit.phonemanager.R

/**
 * Story E12.6: Setting item with lock support.
 *
 * Displays a setting with optional lock indicator, disabled state,
 * and tooltip on long-press.
 *
 * AC E12.6.1: Lock indicator display
 * - Lock icon (🔒) next to setting name
 * - "Managed by [admin name]" subtitle
 * - Disabled interaction (grayed out)
 * - Tooltip explaining lock on long-press
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SettingToggleItem(
    title: String,
    summary: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    isLocked: Boolean = false,
    lockedBy: String? = null,
    onLockedClick: (() -> Unit)? = null,
) {
    var showTooltip by remember { mutableStateOf(false) }

    val effectiveAlpha = if (isLocked) 0.5f else 1f

    Row(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                enabled = true,
                onClick = {
                    if (isLocked) {
                        onLockedClick?.invoke()
                    }
                },
                onLongClick = {
                    if (isLocked) {
                        showTooltip = true
                    }
                },
            )
            .padding(vertical = 8.dp, horizontal = 16.dp)
            .alpha(effectiveAlpha),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                )
                if (isLocked) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = stringResource(R.string.setting_locked),
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (isLocked && lockedBy != null) {
                Text(
                    text = stringResource(R.string.managed_by, lockedBy),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            } else {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = { newValue ->
                if (isLocked) {
                    onLockedClick?.invoke()
                } else {
                    onCheckedChange(newValue)
                }
            },
            enabled = !isLocked,
        )
    }

    // Tooltip dialog for long-press
    if (showTooltip && isLocked) {
        SettingLockTooltip(
            settingName = title,
            lockedBy = lockedBy,
            onDismiss = { showTooltip = false },
        )
    }
}

/**
 * Story E12.6: Text-based setting item with lock support.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SettingTextItem(
    title: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLocked: Boolean = false,
    lockedBy: String? = null,
    onLockedClick: (() -> Unit)? = null,
) {
    var showTooltip by remember { mutableStateOf(false) }

    val effectiveAlpha = if (isLocked) 0.5f else 1f

    Row(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                enabled = true,
                onClick = {
                    if (isLocked) {
                        onLockedClick?.invoke()
                    } else {
                        onClick()
                    }
                },
                onLongClick = {
                    if (isLocked) {
                        showTooltip = true
                    }
                },
            )
            .padding(vertical = 12.dp, horizontal = 16.dp)
            .alpha(effectiveAlpha),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                )
                if (isLocked) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = stringResource(R.string.setting_locked),
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (isLocked && lockedBy != null) {
                Text(
                    text = stringResource(R.string.managed_by, lockedBy),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    // Tooltip dialog for long-press
    if (showTooltip && isLocked) {
        SettingLockTooltip(
            settingName = title,
            lockedBy = lockedBy,
            onDismiss = { showTooltip = false },
        )
    }
}

/**
 * Story E12.6: Tooltip showing lock information.
 *
 * Displayed on long-press of a locked setting.
 */
@Composable
private fun SettingLockTooltip(settingName: String, lockedBy: String?, onDismiss: () -> Unit) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.setting_locked)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.setting_locked_explanation, settingName),
                )
                if (lockedBy != null) {
                    Text(
                        text = stringResource(R.string.managed_by, lockedBy),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.ok))
            }
        },
    )
}

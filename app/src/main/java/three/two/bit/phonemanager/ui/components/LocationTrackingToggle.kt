package three.two.bit.phonemanager.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import three.two.bit.phonemanager.R
import three.two.bit.phonemanager.permission.PermissionState
import three.two.bit.phonemanager.ui.main.LocationTrackingViewModel
import three.two.bit.phonemanager.ui.main.TrackingState
import three.two.bit.phonemanager.ui.theme.PhoneManagerTheme

/**
 * Story 1.1: LocationTrackingToggle - Material 3 toggle for starting/stopping tracking
 */
@Composable
fun LocationTrackingToggle(modifier: Modifier = Modifier, viewModel: LocationTrackingViewModel = hiltViewModel()) {
    val trackingState by viewModel.trackingState.collectAsState()
    val permissionState by viewModel.permissionState.collectAsState()

    val isChecked = trackingState is TrackingState.Active
    val isEnabled = permissionState is PermissionState.AllGranted &&
        trackingState !is TrackingState.Starting &&
        trackingState !is TrackingState.Stopping

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("tracking_toggle"),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.location_tracking),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = when (trackingState) {
                        is TrackingState.Stopped -> if (permissionState is PermissionState.AllGranted) {
                            stringResource(R.string.tracking_inactive)
                        } else {
                            stringResource(R.string.tracking_permissions_required)
                        }

                        is TrackingState.Starting -> stringResource(R.string.tracking_starting)
                        is TrackingState.Active -> stringResource(R.string.tracking_active)
                        is TrackingState.Stopping -> stringResource(R.string.tracking_stopping)
                        is TrackingState.Error -> (trackingState as TrackingState.Error).message
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = when (trackingState) {
                        is TrackingState.Active -> MaterialTheme.colorScheme.primary
                        is TrackingState.Error -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }

            if (trackingState is TrackingState.Starting || trackingState is TrackingState.Stopping) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                )
            } else {
                val stopHint = stringResource(R.string.tracking_toggle_stop_hint)
                val startHint = stringResource(R.string.tracking_toggle_start_hint)
                Switch(
                    checked = isChecked,
                    onCheckedChange = { viewModel.toggleTracking() },
                    enabled = isEnabled,
                    modifier = Modifier.semantics {
                        contentDescription = if (isChecked) {
                            stopHint
                        } else {
                            startHint
                        }
                    },
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LocationTrackingTogglePreview() {
    PhoneManagerTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Note: Preview won't work with hiltViewModel()
            // This is just for layout preview
            Text(stringResource(R.string.location_tracking_preview))
        }
    }
}

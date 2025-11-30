package three.two.bit.phonemanager.ui.tripdetail.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import three.two.bit.phonemanager.R
import three.two.bit.phonemanager.ui.tripdetail.TripStatistics as TripStats

/**
 * Story E8.10: TripStatistics Component
 *
 * Displays trip statistics in a card format.
 * AC E8.10.6: Trip statistics display
 */
@Composable
fun TripStatisticsCard(
    statistics: TripStats,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Text(
                text = stringResource(R.string.trip_detail_statistics),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Average speed
            StatisticRow(
                icon = Icons.Default.Speed,
                label = stringResource(R.string.trip_detail_avg_speed),
                value = statistics.averageSpeedKmh?.let {
                    String.format("%.1f km/h", it)
                } ?: "--",
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Location points
            StatisticRow(
                icon = Icons.Default.LocationOn,
                label = stringResource(R.string.trip_detail_location_points),
                value = statistics.locationPointCount.toString(),
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Movement events
            StatisticRow(
                icon = Icons.Default.Timeline,
                label = stringResource(R.string.trip_detail_events),
                value = statistics.movementEventCount.toString(),
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Path corrected
            StatisticRow(
                icon = if (statistics.isPathCorrected) {
                    Icons.Default.CheckCircle
                } else {
                    Icons.Default.Route
                },
                label = stringResource(R.string.trip_detail_path_corrected),
                value = if (statistics.isPathCorrected) {
                    stringResource(R.string.yes)
                } else {
                    stringResource(R.string.no)
                },
                valueColor = if (statistics.isPathCorrected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}

@Composable
private fun StatisticRow(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.padding(horizontal = 8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = valueColor,
        )
    }
}

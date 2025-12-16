package three.two.bit.phonemanager.ui.tripdetail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import three.two.bit.phonemanager.R
import three.two.bit.phonemanager.movement.TransportationMode
import three.two.bit.phonemanager.ui.tripdetail.ModeBreakdownItem

/**
 * Story E8.10: ModeBreakdownChart Component
 *
 * Displays transportation mode breakdown as a horizontal bar chart.
 * AC E8.10.5: Mode breakdown with percentages
 */
@Composable
fun ModeBreakdownChart(items: List<ModeBreakdownItem>, modifier: Modifier = Modifier) {
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
                text = stringResource(R.string.trip_detail_mode_breakdown),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Stacked horizontal bar
            if (items.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .clip(RoundedCornerShape(12.dp)),
                ) {
                    items.forEach { item ->
                        Box(
                            modifier = Modifier
                                .weight(item.percentage.coerceAtLeast(0.1f))
                                .height(24.dp)
                                .background(getModeColor(item.mode)),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Legend
                items.forEach { item ->
                    ModeBreakdownRow(item = item)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun ModeBreakdownRow(item: ModeBreakdownItem, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(getModeColor(item.mode).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = getModeIcon(item.mode),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = getModeColor(item.mode),
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = getModeLabel(item.mode),
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Text(
            text = "${String.format("%.0f", item.percentage)}%",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = getModeColor(item.mode),
        )
    }
}

@Composable
private fun getModeLabel(mode: TransportationMode): String = when (mode) {
    TransportationMode.WALKING -> stringResource(R.string.trip_mode_walking)
    TransportationMode.RUNNING -> stringResource(R.string.trip_mode_running)
    TransportationMode.CYCLING -> stringResource(R.string.trip_mode_cycling)
    TransportationMode.IN_VEHICLE -> stringResource(R.string.trip_mode_driving)
    TransportationMode.STATIONARY -> stringResource(R.string.trip_mode_stationary)
    TransportationMode.UNKNOWN -> stringResource(R.string.trip_mode_unknown)
}

private fun getModeIcon(mode: TransportationMode): ImageVector = when (mode) {
    TransportationMode.WALKING -> Icons.AutoMirrored.Filled.DirectionsWalk
    TransportationMode.RUNNING -> Icons.AutoMirrored.Filled.DirectionsRun
    TransportationMode.CYCLING -> Icons.AutoMirrored.Filled.DirectionsBike
    TransportationMode.IN_VEHICLE -> Icons.Default.DirectionsCar
    TransportationMode.STATIONARY -> Icons.Default.HourglassEmpty
    TransportationMode.UNKNOWN -> Icons.AutoMirrored.Filled.Help
}

private fun getModeColor(mode: TransportationMode): Color = when (mode) {
    TransportationMode.WALKING -> Color(0xFF4CAF50)
    TransportationMode.RUNNING -> Color(0xFFF44336)
    TransportationMode.CYCLING -> Color(0xFF2196F3)
    TransportationMode.IN_VEHICLE -> Color(0xFF9C27B0)
    TransportationMode.STATIONARY -> Color(0xFF607D8B)
    TransportationMode.UNKNOWN -> Color(0xFF9E9E9E)
}

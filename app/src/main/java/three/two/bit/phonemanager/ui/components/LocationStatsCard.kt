package three.two.bit.phonemanager.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import three.two.bit.phonemanager.R
import three.two.bit.phonemanager.domain.model.LocationStats
import three.two.bit.phonemanager.ui.theme.PhoneManagerTheme
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Story 1.3: LocationStatsCard - Displays location statistics
 */
@Composable
fun LocationStatsCard(locationStats: LocationStats, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Location Statistics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )

            // Location count
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.stats_today),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = locationStats.todayCount.toString(),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                Column {
                    Text(
                        text = stringResource(R.string.stats_all_time),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = locationStats.totalCount.toString(),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Column {
                    Text(
                        text = stringResource(R.string.stats_interval),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "${locationStats.trackingInterval.toMinutes()} min",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Divider()

            // Last update
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.stats_last_update),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = locationStats.lastLocation?.let {
                            formatTimestamp(Instant.ofEpochMilli(it.timestamp))
                        } ?: "No locations yet",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                locationStats.averageAccuracy?.let { accuracy ->
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = stringResource(R.string.stats_avg_accuracy),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "±${accuracy.toInt()}m",
                            style = MaterialTheme.typography.bodyMedium,
                            color = when {
                                accuracy <= 10f -> Color.Green
                                accuracy <= 50f -> Color(0xFFFF9800)
                                else -> Color.Red
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun formatTimestamp(instant: Instant): String {
    val now = Instant.now()
    val duration = Duration.between(instant, now)

    return when {
        duration.toMinutes() < 1 -> "Just now"
        duration.toMinutes() < 60 -> "${duration.toMinutes()} min ago"
        duration.toHours() < 24 -> "${duration.toHours()} hours ago"
        else -> {
            // Format as "Jan 11, 2:30 PM"
            val formatter = DateTimeFormatter.ofPattern("MMM dd, h:mm a")
            instant.atZone(ZoneId.systemDefault()).format(formatter)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LocationStatsCardPreview() {
    PhoneManagerTheme {
        LocationStatsCard(
            locationStats = LocationStats(
                totalCount = 1234,
                todayCount = 42,
                lastLocation = null,
                averageAccuracy = 15.3f,
                trackingInterval = Duration.ofMinutes(5),
            ),
            modifier = Modifier.padding(16.dp),
        )
    }
}

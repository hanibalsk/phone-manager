package three.two.bit.phonemanager.ui.components

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.rounded.DirectionsBike
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.QuestionMark
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.time.Clock
import three.two.bit.phonemanager.R
import three.two.bit.phonemanager.domain.model.Trip
import three.two.bit.phonemanager.movement.TransportationMode

/**
 * Story E8.13: Active Trip Card Component (AC E8.13.1)
 *
 * Displays active trip information with:
 * - Mode icon
 * - "Active Trip" title
 * - Start time
 * - Updating duration
 * - Distance
 * - Location count
 * - Progress indicator
 * - "End Trip" button
 */
@Composable
fun ActiveTripCard(trip: Trip, onEndTrip: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    // Update duration every second
    var currentTime by remember { mutableLongStateOf(Clock.System.now().epochSeconds) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            currentTime = Clock.System.now().epochSeconds
        }
    }

    val durationSeconds = currentTime - trip.startTime.epochSeconds
    val formattedDuration = formatDuration(durationSeconds)

    // Use Android's DateUtils for localized relative time
    val startTimeMs = trip.startTime.toEpochMilliseconds()
    val relativeStartTime = DateUtils.getRelativeTimeSpanString(
        startTimeMs,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS,
        DateUtils.FORMAT_ABBREV_RELATIVE,
    ).toString()

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            // Header row: Mode icon + Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = getModeIcon(trip.dominantMode),
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.active_trip_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = stringResource(R.string.active_trip_started, relativeStartTime),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Stats row: Duration, Distance, Locations
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                StatItem(
                    label = stringResource(R.string.active_trip_duration),
                    value = formattedDuration,
                )
                StatItem(
                    label = stringResource(R.string.active_trip_distance),
                    value = trip.formattedDistance,
                )
                StatItem(
                    label = stringResource(R.string.active_trip_locations),
                    value = trip.locationCount.toString(),
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress indicator
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f),
            )

            Spacer(modifier = Modifier.height(16.dp))

            // End Trip button
            Button(
                onClick = onEndTrip,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Stop,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(R.string.active_trip_end))
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
        )
    }
}

private fun getModeIcon(mode: TransportationMode): ImageVector = when (mode) {
    TransportationMode.WALKING -> Icons.Rounded.DirectionsWalk
    TransportationMode.RUNNING -> Icons.Rounded.DirectionsRun
    TransportationMode.CYCLING -> Icons.Rounded.DirectionsBike
    TransportationMode.IN_VEHICLE -> Icons.Rounded.DirectionsCar
    TransportationMode.STATIONARY -> Icons.Rounded.LocationOn
    TransportationMode.UNKNOWN -> Icons.Rounded.QuestionMark
}

@Composable
private fun formatDuration(seconds: Long): String = when {
    seconds >= 3600 -> {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        stringResource(R.string.duration_hours_minutes, hours, minutes)
    }
    seconds >= 60 -> {
        val minutes = seconds / 60
        val secs = seconds % 60
        stringResource(R.string.duration_minutes_seconds, minutes, secs)
    }
    else -> stringResource(R.string.duration_seconds, seconds)
}

// formatStartTime removed - now using Android's DateUtils.getRelativeTimeSpanString()
// which provides automatic localization to device language

package three.two.bit.phonemanager.ui.triphistory.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import three.two.bit.phonemanager.R
import java.text.DateFormat
import java.util.Date
import three.two.bit.phonemanager.domain.model.Trip
import three.two.bit.phonemanager.movement.TransportationMode

/**
 * Story E8.9: TripCard Component
 *
 * Displays a single trip with mode icon, duration, distance, and times.
 * AC E8.9.3: Trip card information
 * AC E8.9.8: Swipe to delete
 * Trip Geocoding Enhancement: Display geocoded location names
 *
 * @param trip The trip to display
 * @param onClick Callback when the card is clicked
 * @param onSwipeToDelete Callback when the card is swiped to delete
 * @param displayName Optional geocoded/custom name to display instead of mode name
 * @param modifier Modifier for styling
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripCard(
    trip: Trip,
    onClick: () -> Unit,
    onSwipeToDelete: () -> Unit,
    displayName: String? = null,
    modifier: Modifier = Modifier,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onSwipeToDelete()
                true
            } else {
                false
            }
        },
    )

    // Reset dismiss state after delete
    LaunchedEffect(trip.id) {
        dismissState.reset()
    }

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.error)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.delete),
                    tint = MaterialTheme.colorScheme.onError,
                )
            }
        },
        content = {
            TripCardContent(
                trip = trip,
                onClick = onClick,
                displayName = displayName,
                modifier = modifier,
            )
        },
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
    )
}

@Composable
private fun TripCardContent(
    trip: Trip,
    onClick: () -> Unit,
    displayName: String? = null,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Mode icon (AC E8.9.3)
            ModeIcon(
                mode = trip.dominantMode,
                modifier = Modifier.size(48.dp),
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Trip details
            Column(
                modifier = Modifier.weight(1f),
            ) {
                // Trip name (geocoded name if available, otherwise mode name)
                Text(
                    text = displayName ?: getTripName(trip),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Start and end times
                Text(
                    text = formatTripTimes(trip),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Duration and distance
            Column(
                horizontalAlignment = Alignment.End,
            ) {
                // Duration
                Text(
                    text = trip.formattedDuration ?: "--",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Distance
                Text(
                    text = trip.formattedDistance,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * Mode icon component (AC E8.9.3)
 */
@Composable
fun ModeIcon(mode: TransportationMode, modifier: Modifier = Modifier) {
    val (icon, tint) = getModeIconAndColor(mode)

    Box(
        modifier = modifier
            .background(
                color = tint.copy(alpha = 0.1f),
                shape = MaterialTheme.shapes.medium,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = mode.name,
            modifier = Modifier.size(28.dp),
            tint = tint,
        )
    }
}

/**
 * Get icon and color for transportation mode
 */
@Composable
fun getModeIconAndColor(mode: TransportationMode): Pair<ImageVector, Color> = when (mode) {
    TransportationMode.WALKING -> Icons.AutoMirrored.Filled.DirectionsWalk to MaterialTheme.colorScheme.tertiary
    TransportationMode.RUNNING -> Icons.AutoMirrored.Filled.DirectionsRun to MaterialTheme.colorScheme.error
    TransportationMode.CYCLING -> Icons.AutoMirrored.Filled.DirectionsBike to MaterialTheme.colorScheme.secondary
    TransportationMode.IN_VEHICLE -> Icons.Default.DirectionsCar to MaterialTheme.colorScheme.primary
    TransportationMode.STATIONARY -> Icons.Default.HourglassEmpty to MaterialTheme.colorScheme.outline
    TransportationMode.UNKNOWN -> Icons.AutoMirrored.Filled.Help to MaterialTheme.colorScheme.outline
}

/**
 * Get fallback trip name based on transportation mode.
 * Used when no custom name or geocoded name is available.
 * Trip Geocoding Enhancement: This is the fallback when geocoding fails or is unavailable.
 */
@Composable
private fun getTripName(trip: Trip): String = when (trip.dominantMode) {
    TransportationMode.WALKING -> stringResource(R.string.trip_mode_walking)
    TransportationMode.RUNNING -> stringResource(R.string.trip_mode_running)
    TransportationMode.CYCLING -> stringResource(R.string.trip_mode_cycling)
    TransportationMode.IN_VEHICLE -> stringResource(R.string.trip_mode_driving)
    TransportationMode.STATIONARY -> stringResource(R.string.trip_mode_stationary)
    TransportationMode.UNKNOWN -> stringResource(R.string.trip_mode_unknown)
}

/**
 * Format trip start and end times using locale-aware formatting
 */
@Composable
private fun formatTripTimes(trip: Trip): String {
    val timeFormat = DateFormat.getTimeInstance(DateFormat.SHORT)
    val startFormatted = timeFormat.format(Date(trip.startTime.toEpochMilliseconds()))
    val inProgress = stringResource(R.string.in_progress)

    return trip.endTime?.let { endInstant ->
        val endFormatted = timeFormat.format(Date(endInstant.toEpochMilliseconds()))
        "$startFormatted - $endFormatted"
    } ?: "$startFormatted - $inProgress"
}

/**
 * Simple TripCard without swipe (for previews/simple displays)
 * Trip Geocoding Enhancement: Supports displayName parameter
 */
@Composable
fun SimpleTripCard(
    trip: Trip,
    onClick: () -> Unit,
    displayName: String? = null,
    modifier: Modifier = Modifier,
) {
    TripCardContent(
        trip = trip,
        onClick = onClick,
        displayName = displayName,
        modifier = modifier,
    )
}

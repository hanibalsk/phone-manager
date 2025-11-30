package three.two.bit.phonemanager.ui.triphistory.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import three.two.bit.phonemanager.R
import three.two.bit.phonemanager.movement.TransportationMode
import three.two.bit.phonemanager.ui.triphistory.QuickDateFilter

/**
 * Story E8.9: TripFilterBar Component
 *
 * Filter bar with quick date filters and transportation mode filters.
 * AC E8.9.4: Quick filters (Today, This Week, This Month)
 * AC E8.9.5: Mode filters (Walking, Running, Cycling, Driving)
 */
@Composable
fun TripFilterBar(
    selectedQuickFilter: QuickDateFilter,
    selectedModes: Set<TransportationMode>,
    hasActiveFilters: Boolean,
    onQuickFilterSelected: (QuickDateFilter) -> Unit,
    onModeToggled: (TransportationMode) -> Unit,
    onDateRangeClick: () -> Unit,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            // Quick date filters row (AC E8.9.4)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                QuickDateFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = selectedQuickFilter == filter,
                        onClick = { onQuickFilterSelected(filter) },
                        label = { Text(getQuickFilterLabel(filter)) },
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Custom date range button
                FilterChip(
                    selected = false,
                    onClick = onDateRangeClick,
                    label = { Text(stringResource(R.string.trip_filter_custom_range)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 4.dp),
                        )
                    },
                )

                // Clear filters button (only show when filters active)
                if (hasActiveFilters) {
                    IconButton(onClick = onClearFilters) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = stringResource(R.string.trip_filter_clear),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            // Transportation mode filters row (AC E8.9.5)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ModeFilterChip(
                    mode = TransportationMode.WALKING,
                    icon = Icons.Default.DirectionsWalk,
                    isSelected = TransportationMode.WALKING in selectedModes,
                    onToggle = { onModeToggled(TransportationMode.WALKING) },
                )
                ModeFilterChip(
                    mode = TransportationMode.RUNNING,
                    icon = Icons.Default.DirectionsRun,
                    isSelected = TransportationMode.RUNNING in selectedModes,
                    onToggle = { onModeToggled(TransportationMode.RUNNING) },
                )
                ModeFilterChip(
                    mode = TransportationMode.CYCLING,
                    icon = Icons.Default.DirectionsBike,
                    isSelected = TransportationMode.CYCLING in selectedModes,
                    onToggle = { onModeToggled(TransportationMode.CYCLING) },
                )
                ModeFilterChip(
                    mode = TransportationMode.IN_VEHICLE,
                    icon = Icons.Default.DirectionsCar,
                    isSelected = TransportationMode.IN_VEHICLE in selectedModes,
                    onToggle = { onModeToggled(TransportationMode.IN_VEHICLE) },
                )
            }
        }
    }
}

@Composable
private fun ModeFilterChip(
    mode: TransportationMode,
    icon: ImageVector,
    isSelected: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterChip(
        selected = isSelected,
        onClick = onToggle,
        label = { Text(getModeLabel(mode)) },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        modifier = modifier,
    )
}

@Composable
private fun getQuickFilterLabel(filter: QuickDateFilter): String = when (filter) {
    QuickDateFilter.TODAY -> stringResource(R.string.trip_filter_today)
    QuickDateFilter.THIS_WEEK -> stringResource(R.string.trip_filter_this_week)
    QuickDateFilter.THIS_MONTH -> stringResource(R.string.trip_filter_this_month)
    QuickDateFilter.ALL -> stringResource(R.string.trip_filter_all)
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

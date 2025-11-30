package three.two.bit.phonemanager.ui.geofences

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import three.two.bit.phonemanager.R
import three.two.bit.phonemanager.domain.model.Geofence
import three.two.bit.phonemanager.domain.model.TransitionType

/**
 * Story E6.1: Geofences Screen
 *
 * Displays list of geofences with management options
 * AC E6.1.5: View list, create, edit, delete, enable/disable geofences
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeofencesScreen(
    viewModel: GeofencesViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToCreate: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val geofences by viewModel.geofences.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.geofences_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCreate,
                containerColor = MaterialTheme.colorScheme.primary,
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.geofences_create))
            }
        },
    ) { paddingValues ->
        // Only show pull-to-refresh indicator when refreshing existing data (not initial load)
        val isRefreshing = uiState.isSyncing && geofences.isNotEmpty()

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            when {
                // Initial loading (show centered indicator, not pull-to-refresh)
                uiState.isSyncing && geofences.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                }
                // Empty state
                geofences.isEmpty() -> {
                    EmptyGeofencesContent(
                        modifier = Modifier.fillMaxSize(),
                        onCreateClick = onNavigateToCreate,
                    )
                }
                // Geofence list
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    ) {
                        items(
                            items = geofences,
                            key = { it.id },
                        ) { geofence ->
                            SwipeableGeofenceItem(
                                geofence = geofence,
                                onToggleActive = { viewModel.toggleGeofenceActive(geofence.id, it) },
                                onDelete = { viewModel.deleteGeofence(geofence.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Swipeable geofence item with delete action
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableGeofenceItem(geofence: Geofence, onToggleActive: (Boolean) -> Unit, onDelete: () -> Unit) {
    val currentOnDelete by rememberUpdatedState(onDelete)
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                currentOnDelete()
                true
            } else {
                false
            }
        },
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val color by animateColorAsState(
                when (dismissState.targetValue) {
                    SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.error
                    else -> Color.Transparent
                },
                label = "swipe_color",
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.delete),
                    tint = Color.White,
                )
            }
        },
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
    ) {
        GeofenceCard(
            geofence = geofence,
            onToggleActive = onToggleActive,
        )
    }
}

/**
 * Geofence card displaying geofence information
 */
@Composable
private fun GeofenceCard(geofence: Geofence, onToggleActive: (Boolean) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (geofence.active) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Icon
            Icon(
                imageVector = if (geofence.active) {
                    Icons.Default.LocationOn
                } else {
                    Icons.Default.LocationOff
                },
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = if (geofence.active) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outline
                },
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Geofence info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = geofence.name,
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatGeofenceDescription(geofence),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Toggle switch
            Switch(
                checked = geofence.active,
                onCheckedChange = onToggleActive,
            )
        }
    }
}

/**
 * Format geofence description string
 */
private fun formatGeofenceDescription(geofence: Geofence): String {
    val radiusText = when {
        geofence.radiusMeters >= 1000 -> "${geofence.radiusMeters / 1000}km"
        else -> "${geofence.radiusMeters}m"
    }
    val transitionText = when {
        geofence.transitionTypes.containsAll(setOf(TransitionType.ENTER, TransitionType.EXIT)) -> "enter/exit"
        geofence.transitionTypes.contains(TransitionType.ENTER) -> "enter"
        geofence.transitionTypes.contains(TransitionType.EXIT) -> "exit"
        geofence.transitionTypes.contains(TransitionType.DWELL) -> "dwell"
        else -> "any"
    }
    return "$radiusText radius, trigger on $transitionText"
}

/**
 * Empty state when no geofences exist
 */
@Composable
private fun EmptyGeofencesContent(modifier: Modifier = Modifier, onCreateClick: () -> Unit) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Default.LocationOff,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.geofences_empty_title),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.geofences_empty_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(24.dp))
        androidx.compose.material3.Button(onClick = onCreateClick) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.button_create_geofence))
        }
    }
}

package three.two.bit.phonemanager.ui.triphistory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import three.two.bit.phonemanager.R
import three.two.bit.phonemanager.ui.triphistory.components.TripCard
import three.two.bit.phonemanager.ui.triphistory.components.TripFilterBar

/**
 * Story E8.9: Trip History Screen
 *
 * Displays trip history with day grouping, filtering, and pagination.
 * ACs: E8.9.1-E8.9.10
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripHistoryScreen(
    onNavigateBack: () -> Unit,
    onTripClick: (String) -> Unit,
    viewModel: TripHistoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Undo snackbar message
    val undoMessage = stringResource(R.string.trip_deleted_undo)

    // Show undo snackbar when trip is deleted (AC E8.9.8)
    LaunchedEffect(uiState.showUndoSnackbar) {
        if (uiState.showUndoSnackbar) {
            scope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = undoMessage,
                    actionLabel = "Undo",
                )
                if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                    viewModel.undoDelete()
                } else {
                    viewModel.dismissUndoSnackbar()
                }
            }
        }
    }

    // Load more when reaching end of list (AC E8.9.6)
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItems = listState.layoutInfo.totalItemsCount
            lastVisibleItem >= totalItems - 5 && !uiState.isLoadingMore && uiState.hasMoreData
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            viewModel.loadMoreTrips()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.trip_history_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                    actionColor = MaterialTheme.colorScheme.inversePrimary,
                )
            }
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            // Filter bar (AC E8.9.4, E8.9.5)
            TripFilterBar(
                selectedQuickFilter = uiState.selectedQuickFilter,
                selectedModes = uiState.selectedModeFilters,
                hasActiveFilters = uiState.dateRangeStart != null ||
                    uiState.dateRangeEnd != null ||
                    uiState.selectedModeFilters.isNotEmpty(),
                onQuickFilterSelected = viewModel::setQuickDateFilter,
                onModeToggled = viewModel::toggleModeFilter,
                onDateRangeClick = { viewModel.showDateRangePicker(true) },
                onClearFilters = viewModel::clearFilters,
            )

            // Main content with pull-to-refresh (AC E8.9.7)
            PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = viewModel::refreshTrips,
                modifier = Modifier.fillMaxSize(),
            ) {
                when {
                    uiState.isLoading -> {
                        LoadingState()
                    }

                    uiState.error != null -> {
                        ErrorState(
                            message = uiState.error!!,
                            onRetry = viewModel::loadTrips,
                        )
                    }

                    uiState.groupedTrips.isEmpty() -> {
                        EmptyState()
                    }

                    else -> {
                        // Trip list with day grouping (AC E8.9.2)
                        TripList(
                            groupedTrips = uiState.groupedTrips,
                            isLoadingMore = uiState.isLoadingMore,
                            listState = listState,
                            onTripClick = onTripClick,
                            onSwipeToDelete = { tripId ->
                                viewModel.showDeleteConfirmation(tripId)
                            },
                        )
                    }
                }
            }
        }
    }

    // Delete confirmation dialog (AC E8.9.8)
    if (uiState.tripIdToDelete != null) {
        DeleteConfirmationDialog(
            onConfirm = viewModel::confirmDelete,
            onDismiss = { viewModel.showDeleteConfirmation(null) },
        )
    }

    // Date range picker dialog (AC E8.9.4)
    if (uiState.showDateRangePicker) {
        DateRangePickerDialog(
            initialStartDate = uiState.dateRangeStart,
            initialEndDate = uiState.dateRangeEnd,
            onConfirm = { start, end ->
                viewModel.setDateRangeFilter(start, end)
                viewModel.showDateRangePicker(false)
            },
            onDismiss = { viewModel.showDateRangePicker(false) },
        )
    }
}

@Composable
private fun TripList(
    groupedTrips: Map<TripDayGroup, List<three.two.bit.phonemanager.domain.model.Trip>>,
    isLoadingMore: Boolean,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onTripClick: (String) -> Unit,
    onSwipeToDelete: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        groupedTrips.forEach { (dayGroup, trips) ->
            // Day header (AC E8.9.2)
            item(key = "header_$dayGroup") {
                DayHeader(dayGroup = dayGroup)
            }

            // Trip cards for this day (AC E8.9.3)
            items(
                items = trips,
                key = { trip -> trip.id },
            ) { trip ->
                TripCard(
                    trip = trip,
                    onClick = { onTripClick(trip.id) },
                    onSwipeToDelete = { onSwipeToDelete(trip.id) },
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }

        // Loading more indicator (AC E8.9.6)
        if (isLoadingMore) {
            item(key = "loading_more") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

@Composable
private fun DayHeader(dayGroup: TripDayGroup, modifier: Modifier = Modifier) {
    val headerText = when (dayGroup) {
        is TripDayGroup.Today -> stringResource(R.string.trip_day_today)
        is TripDayGroup.Yesterday -> stringResource(R.string.trip_day_yesterday)
        is TripDayGroup.Date -> formatDate(dayGroup.date)
    }

    Text(
        text = headerText,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun formatDate(date: LocalDate): String {
    // Format as "Mon, Jan 15" or similar
    val dayOfWeek = when (date.dayOfWeek) {
        java.time.DayOfWeek.MONDAY -> "Mon"
        java.time.DayOfWeek.TUESDAY -> "Tue"
        java.time.DayOfWeek.WEDNESDAY -> "Wed"
        java.time.DayOfWeek.THURSDAY -> "Thu"
        java.time.DayOfWeek.FRIDAY -> "Fri"
        java.time.DayOfWeek.SATURDAY -> "Sat"
        java.time.DayOfWeek.SUNDAY -> "Sun"
    }
    val month = when (date.monthNumber) {
        1 -> "Jan"
        2 -> "Feb"
        3 -> "Mar"
        4 -> "Apr"
        5 -> "May"
        6 -> "Jun"
        7 -> "Jul"
        8 -> "Aug"
        9 -> "Sep"
        10 -> "Oct"
        11 -> "Nov"
        12 -> "Dec"
        else -> ""
    }
    return "$dayOfWeek, $month ${date.dayOfMonth}"
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.error),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.error,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onRetry) {
            Text(stringResource(R.string.button_retry))
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Default.DirectionsWalk,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.trip_history_empty_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.trip_history_empty_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        )
    }
}

@Composable
private fun DeleteConfirmationDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.trip_delete_title)) },
        text = { Text(stringResource(R.string.trip_delete_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    stringResource(R.string.delete),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateRangePickerDialog(
    initialStartDate: LocalDate?,
    initialEndDate: LocalDate?,
    onConfirm: (LocalDate?, LocalDate?) -> Unit,
    onDismiss: () -> Unit,
) {
    // Convert LocalDate to milliseconds for DateRangePicker
    val initialStartMillis = initialStartDate?.atStartOfDayIn(TimeZone.currentSystemDefault())
        ?.toEpochMilliseconds()
    val initialEndMillis = initialEndDate?.atStartOfDayIn(TimeZone.currentSystemDefault())
        ?.toEpochMilliseconds()

    val dateRangePickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = initialStartMillis,
        initialSelectedEndDateMillis = initialEndMillis,
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
        ) {
            Column {
                // Header with title
                Text(
                    text = stringResource(R.string.trip_filter_custom_range),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp),
                )

                // DateRangePicker
                DateRangePicker(
                    state = dateRangePickerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    title = null,
                    showModeToggle = true,
                )

                // Action buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel))
                    }
                    TextButton(
                        onClick = {
                            // Convert milliseconds back to LocalDate
                            val startDate = dateRangePickerState.selectedStartDateMillis?.let { millis ->
                                Instant.fromEpochMilliseconds(millis)
                                    .toLocalDateTime(TimeZone.currentSystemDefault())
                                    .date
                            }
                            val endDate = dateRangePickerState.selectedEndDateMillis?.let { millis ->
                                Instant.fromEpochMilliseconds(millis)
                                    .toLocalDateTime(TimeZone.currentSystemDefault())
                                    .date
                            }
                            onConfirm(startDate, endDate)
                        },
                    ) {
                        Text(stringResource(R.string.ok))
                    }
                }
            }
        }
    }
}

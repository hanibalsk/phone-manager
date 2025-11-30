package three.two.bit.phonemanager.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Instant
import three.two.bit.phonemanager.data.database.TripDao
import three.two.bit.phonemanager.data.model.TripEntity
import three.two.bit.phonemanager.data.model.toDomain
import three.two.bit.phonemanager.data.model.toEntity
import three.two.bit.phonemanager.domain.model.TodayTripStats
import three.two.bit.phonemanager.domain.model.Trip
import three.two.bit.phonemanager.movement.TransportationMode
import timber.log.Timber
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Story E8.3: TripRepositoryImpl - Implementation of TripRepository
 *
 * Provides trip data access with domain model mapping.
 * AC E8.3.4: Complete TripRepositoryImpl with entity mapping
 */
@Singleton
class TripRepositoryImpl @Inject constructor(private val tripDao: TripDao) : TripRepository {

    override suspend fun insert(trip: Trip): Result<String> = runCatching {
        Timber.d("Inserting trip: ${trip.id}")
        tripDao.insert(trip.toEntity())
        trip.id
    }

    override suspend fun update(trip: Trip): Result<Unit> = runCatching {
        Timber.d("Updating trip: ${trip.id}")
        tripDao.update(trip.toEntity())
    }

    override suspend fun getTripById(id: String): Trip? = tripDao.getTripById(id)?.toDomain()

    override fun observeTripById(id: String): Flow<Trip?> = tripDao.observeTripById(id).map { it?.toDomain() }

    override suspend fun getActiveTrip(): Trip? = tripDao.getActiveTrip()?.toDomain()

    override fun observeActiveTrip(): Flow<Trip?> = tripDao.observeActiveTrip().map { it?.toDomain() }

    override fun observeRecentTrips(limit: Int): Flow<List<Trip>> = tripDao.observeRecentTrips(limit).map { entities ->
        entities.map { it.toDomain() }
    }

    override fun observeCompletedTrips(limit: Int): Flow<List<Trip>> =
        tripDao.observeCompletedTrips(limit).map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun getTripsBetween(start: Instant, end: Instant): List<Trip> = tripDao.getTripsBetween(
        startTime = start.toEpochMilliseconds(),
        endTime = end.toEpochMilliseconds(),
    ).map { it.toDomain() }

    override fun observeTripsByMode(mode: TransportationMode): Flow<List<Trip>> =
        tripDao.observeTripsByMode(mode.name).map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun incrementLocationCount(tripId: String, distance: Double) {
        tripDao.incrementLocationCount(
            tripId = tripId,
            distance = distance,
            timestamp = System.currentTimeMillis(),
        )
    }

    override fun observeTodayStats(): Flow<TodayTripStats> {
        val startOfDay = getStartOfDayMillis()

        return combine(
            tripDao.observeTripCountSince(startOfDay),
            tripDao.observeTotalDistanceSince(startOfDay),
            tripDao.observeCompletedTrips(100), // Get completed trips to calculate duration and mode
        ) { count, distance, allCompletedTrips ->
            // Filter to today's completed trips
            val todayTrips = allCompletedTrips.filter { it.startTime >= startOfDay }

            val dominantMode = findDominantMode(todayTrips)

            // Calculate total duration from actual trip data
            val totalDuration = todayTrips.sumOf { trip ->
                val end = trip.endTime ?: System.currentTimeMillis()
                (end - trip.startTime) / 1000 // Convert ms to seconds
            }

            TodayTripStats(
                tripCount = count,
                totalDistanceMeters = distance ?: 0.0,
                totalDurationSeconds = totalDuration,
                dominantMode = dominantMode,
            )
        }
    }

    override fun observeTotalDistanceSince(since: Instant): Flow<Double> =
        tripDao.observeTotalDistanceSince(since.toEpochMilliseconds())
            .map { it ?: 0.0 }

    override fun observeTripCountSince(since: Instant): Flow<Int> =
        tripDao.observeTripCountSince(since.toEpochMilliseconds())

    override suspend fun getUnsyncedTrips(limit: Int): List<Trip> = tripDao.getUnsyncedTrips(limit).map {
        it.toDomain()
    }

    override suspend fun markAsSynced(tripId: String, syncedAt: Instant, serverId: String?) {
        tripDao.markAsSynced(
            tripId = tripId,
            syncedAt = syncedAt.toEpochMilliseconds(),
            serverId = serverId,
        )
    }

    override suspend fun deleteOldTrips(before: Instant): Int = tripDao.deleteOldTrips(before.toEpochMilliseconds())

    override suspend fun deleteTrip(tripId: String) {
        tripDao.deleteTrip(tripId)
    }

    /**
     * Get start of today in milliseconds.
     */
    private fun getStartOfDayMillis(): Long = LocalDate.now()
        .atStartOfDay(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()

    /**
     * Find dominant transportation mode from a list of trips.
     */
    private fun findDominantMode(trips: List<TripEntity>): TransportationMode? {
        if (trips.isEmpty()) return null

        // Count duration by mode across all trips
        val modeDurations = mutableMapOf<TransportationMode, Long>()

        for (trip in trips) {
            val mode = TransportationMode.entries.find { it.name == trip.dominantMode }
                ?: TransportationMode.UNKNOWN
            val duration = (trip.endTime ?: System.currentTimeMillis()) - trip.startTime
            modeDurations[mode] = (modeDurations[mode] ?: 0L) + duration
        }

        return modeDurations.maxByOrNull { it.value }?.key
    }
}

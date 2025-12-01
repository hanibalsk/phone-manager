package three.two.bit.phonemanager.data.repository

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.junit.Before
import org.junit.Test
import three.two.bit.phonemanager.data.database.TripDao
import three.two.bit.phonemanager.data.model.TripEntity
import three.two.bit.phonemanager.domain.model.LatLng
import three.two.bit.phonemanager.domain.model.Trip
import three.two.bit.phonemanager.domain.model.TripState
import three.two.bit.phonemanager.domain.model.TripTrigger
import three.two.bit.phonemanager.movement.TransportationMode
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Story E8.3: TripRepository Unit Tests
 *
 * Tests for TripRepository with mocked TripDao.
 */
class TripRepositoryTest {

    private lateinit var tripDao: TripDao
    private lateinit var tripApiService: three.two.bit.phonemanager.network.TripApiService
    private lateinit var repository: TripRepositoryImpl

    @Before
    fun setup() {
        tripDao = mockk(relaxed = true)
        tripApiService = mockk(relaxed = true)
        repository = TripRepositoryImpl(tripDao, tripApiService)
    }

    // region Test Data

    private fun createTestTripEntity(
        id: String = "test-trip-1",
        state: String = "ACTIVE",
        startTime: Long = System.currentTimeMillis() - 3600000,
        endTime: Long? = null,
    ) = TripEntity(
        id = id,
        state = state,
        startTime = startTime,
        endTime = endTime,
        startLatitude = 37.7749,
        startLongitude = -122.4194,
        endLatitude = null,
        endLongitude = null,
        totalDistanceMeters = 1500.0,
        locationCount = 25,
        dominantMode = "WALKING",
        modesUsedJson = """["WALKING"]""",
        modeBreakdownJson = """{"WALKING": 3600000}""",
        startTrigger = "ACTIVITY_DETECTION",
        endTrigger = null,
        isSynced = false,
        syncedAt = null,
        serverId = null,
    )

    private fun createTestTrip(id: String = "test-trip-1", state: TripState = TripState.ACTIVE) = Trip(
        id = id,
        state = state,
        startTime = Clock.System.now(),
        endTime = null,
        startLocation = LatLng(37.7749, -122.4194),
        endLocation = null,
        totalDistanceMeters = 1500.0,
        locationCount = 25,
        dominantMode = TransportationMode.WALKING,
        modesUsed = setOf(TransportationMode.WALKING),
        modeBreakdown = mapOf(TransportationMode.WALKING to 3600000L),
        startTrigger = TripTrigger.ACTIVITY_DETECTION,
        endTrigger = null,
        isSynced = false,
        syncedAt = null,
        serverId = null,
        createdAt = Clock.System.now(),
        updatedAt = Clock.System.now(),
    )

    // endregion

    // region Insert/Update Tests

    @Test
    fun `insert trip should call dao insert and return trip id`() = runTest {
        val trip = createTestTrip()
        coEvery { tripDao.insert(any()) } returns 1L

        val result = repository.insert(trip)

        assertTrue(result.isSuccess)
        assertEquals(trip.id, result.getOrNull())
        coVerify { tripDao.insert(any()) }
    }

    @Test
    fun `update trip should call dao update`() = runTest {
        val trip = createTestTrip()

        val result = repository.update(trip)

        assertTrue(result.isSuccess)
        coVerify { tripDao.update(any()) }
    }

    // endregion

    // region Query Tests

    @Test
    fun `getTripById returns mapped domain model`() = runTest {
        val entity = createTestTripEntity()
        coEvery { tripDao.getTripById("test-trip-1") } returns entity

        val result = repository.getTripById("test-trip-1")

        assertNotNull(result)
        assertEquals("test-trip-1", result.id)
        assertEquals(TripState.ACTIVE, result.state)
        assertEquals(TransportationMode.WALKING, result.dominantMode)
    }

    @Test
    fun `getTripById returns null when not found`() = runTest {
        coEvery { tripDao.getTripById("nonexistent") } returns null

        val result = repository.getTripById("nonexistent")

        assertNull(result)
    }

    @Test
    fun `observeTripById emits mapped domain model`() = runTest {
        val entity = createTestTripEntity()
        every { tripDao.observeTripById("test-trip-1") } returns flowOf(entity)

        val result = repository.observeTripById("test-trip-1").first()

        assertNotNull(result)
        assertEquals("test-trip-1", result.id)
    }

    @Test
    fun `getActiveTrip returns mapped active trip`() = runTest {
        val entity = createTestTripEntity(state = "ACTIVE")
        coEvery { tripDao.getActiveTrip() } returns entity

        val result = repository.getActiveTrip()

        assertNotNull(result)
        assertEquals(TripState.ACTIVE, result.state)
        assertTrue(result.isActive)
    }

    @Test
    fun `observeActiveTrip emits active trip`() = runTest {
        val entity = createTestTripEntity(state = "ACTIVE")
        every { tripDao.observeActiveTrip() } returns flowOf(entity)

        val result = repository.observeActiveTrip().first()

        assertNotNull(result)
        assertTrue(result.isActive)
    }

    @Test
    fun `observeRecentTrips emits list of mapped trips`() = runTest {
        val entities = listOf(
            createTestTripEntity(id = "trip-1"),
            createTestTripEntity(id = "trip-2"),
        )
        every { tripDao.observeRecentTrips(10) } returns flowOf(entities)

        val result = repository.observeRecentTrips(10).first()

        assertEquals(2, result.size)
        assertEquals("trip-1", result[0].id)
        assertEquals("trip-2", result[1].id)
    }

    @Test
    fun `observeTripsByMode filters by transportation mode`() = runTest {
        val entities = listOf(createTestTripEntity())
        every { tripDao.observeTripsByMode("WALKING") } returns flowOf(entities)

        val result = repository.observeTripsByMode(TransportationMode.WALKING).first()

        assertEquals(1, result.size)
        assertEquals(TransportationMode.WALKING, result[0].dominantMode)
    }

    @Test
    fun `getTripsBetween returns trips in time range`() = runTest {
        val now = System.currentTimeMillis()
        val entities = listOf(createTestTripEntity())
        coEvery { tripDao.getTripsBetween(any(), any()) } returns entities

        val start = Instant.fromEpochMilliseconds(now - 86400000)
        val end = Instant.fromEpochMilliseconds(now)
        val result = repository.getTripsBetween(start, end)

        assertEquals(1, result.size)
    }

    // endregion

    // region Statistics Tests

    @Test
    fun `incrementLocationCount calls dao with correct params`() = runTest {
        repository.incrementLocationCount("trip-1", 150.0)

        coVerify { tripDao.incrementLocationCount("trip-1", 150.0, any()) }
    }

    @Test
    fun `observeTotalDistanceSince returns distance flow`() = runTest {
        every { tripDao.observeTotalDistanceSince(any()) } returns flowOf(5000.0)

        val since = Clock.System.now()
        val result = repository.observeTotalDistanceSince(since).first()

        assertEquals(5000.0, result)
    }

    @Test
    fun `observeTripCountSince returns count flow`() = runTest {
        every { tripDao.observeTripCountSince(any()) } returns flowOf(5)

        val since = Clock.System.now()
        val result = repository.observeTripCountSince(since).first()

        assertEquals(5, result)
    }

    // endregion

    // region Sync Tests

    @Test
    fun `getUnsyncedTrips returns unsynced trips`() = runTest {
        val entities = listOf(
            createTestTripEntity(id = "unsynced-1", state = "COMPLETED"),
        )
        coEvery { tripDao.getUnsyncedTrips(50) } returns entities

        val result = repository.getUnsyncedTrips()

        assertEquals(1, result.size)
        assertEquals("unsynced-1", result[0].id)
    }

    @Test
    fun `markAsSynced updates trip sync status`() = runTest {
        val syncedAt = Clock.System.now()

        repository.markAsSynced("trip-1", syncedAt, "server-id-123")

        coVerify {
            tripDao.markAsSynced(
                tripId = "trip-1",
                syncedAt = syncedAt.toEpochMilliseconds(),
                serverId = "server-id-123",
            )
        }
    }

    @Test
    fun `deleteOldTrips removes trips before cutoff`() = runTest {
        coEvery { tripDao.deleteOldTrips(any()) } returns 5
        val before = Clock.System.now()

        val result = repository.deleteOldTrips(before)

        assertEquals(5, result)
    }

    @Test
    fun `deleteTrip removes specific trip`() = runTest {
        repository.deleteTrip("trip-1")

        coVerify { tripDao.deleteTrip("trip-1") }
    }

    // endregion
}

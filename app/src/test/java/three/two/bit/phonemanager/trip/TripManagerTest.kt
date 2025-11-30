package three.two.bit.phonemanager.trip

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.Before
import org.junit.Test
import three.two.bit.phonemanager.data.preferences.PreferencesRepository
import three.two.bit.phonemanager.data.repository.TripRepository
import three.two.bit.phonemanager.domain.model.LatLng
import three.two.bit.phonemanager.domain.model.Trip
import three.two.bit.phonemanager.domain.model.TripState
import three.two.bit.phonemanager.domain.model.TripTrigger
import three.two.bit.phonemanager.movement.DetectionSource
import three.two.bit.phonemanager.movement.TransportationMode
import three.two.bit.phonemanager.movement.TransportationModeManager
import three.two.bit.phonemanager.movement.TransportationState
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Story E8.4: TripManager Unit Tests
 *
 * Tests for TripManager state machine and trip management logic.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TripManagerTest {

    private lateinit var transportationModeManager: TransportationModeManager
    private lateinit var tripRepository: TripRepository
    private lateinit var preferencesRepository: PreferencesRepository
    private lateinit var tripManager: TripManagerImpl

    private val transportationStateFlow = MutableStateFlow(
        TransportationState(
            mode = TransportationMode.STATIONARY,
            isInVehicle = false,
            source = DetectionSource.NONE,
        ),
    )

    @Before
    fun setup() {
        transportationModeManager = mockk(relaxed = true)
        tripRepository = mockk(relaxed = true)
        preferencesRepository = mockk(relaxed = true)

        every { transportationModeManager.transportationState } returns transportationStateFlow

        coEvery { tripRepository.getActiveTrip() } returns null
        coEvery { tripRepository.insert(any()) } returns Result.success("trip-id")
        coEvery { tripRepository.update(any()) } returns Result.success(Unit)

        // E8-M1 fix: Stub all preference Flows to avoid NPE during TripManagerImpl.init()
        // TripManagerImpl.observePreferences() is called in init block and requires these Flows
        every { preferencesRepository.isTripDetectionEnabled } returns flowOf(true)
        every { preferencesRepository.tripVehicleGraceSeconds } returns flowOf(90)
        every { preferencesRepository.tripWalkingGraceSeconds } returns flowOf(60)
        every { preferencesRepository.tripStationaryThresholdMinutes } returns flowOf(5)
        every { preferencesRepository.tripMinimumDurationMinutes } returns flowOf(2)
        every { preferencesRepository.tripMinimumDistanceMeters } returns flowOf(100)
        every { preferencesRepository.isTripAutoMergeEnabled } returns flowOf(true)

        tripManager = TripManagerImpl(
            transportationModeManager = transportationModeManager,
            tripRepository = tripRepository,
            preferencesRepository = preferencesRepository,
        )
    }

    // region Initial State Tests

    @Test
    fun `initial state should be IDLE`() = runTest {
        assertEquals(TripState.IDLE, tripManager.currentTripState.value)
    }

    @Test
    fun `initial active trip should be null`() = runTest {
        assertNull(tripManager.activeTrip.value)
    }

    @Test
    fun `initial monitoring should be false`() = runTest {
        assertFalse(tripManager.isMonitoring.value)
    }

    // endregion

    // region Monitoring Lifecycle Tests

    @Test
    fun `startMonitoring sets isMonitoring to true`() = runTest {
        tripManager.startMonitoring()
        assertTrue(tripManager.isMonitoring.value)
    }

    @Test
    fun `stopMonitoring sets isMonitoring to false`() = runTest {
        tripManager.startMonitoring()
        tripManager.stopMonitoring()
        assertFalse(tripManager.isMonitoring.value)
    }

    @Test
    fun `startMonitoring restores active trip from repository`() = runTest {
        val activeTrip = createTestTrip(state = TripState.ACTIVE)
        coEvery { tripRepository.getActiveTrip() } returns activeTrip

        tripManager.startMonitoring()

        assertEquals(activeTrip, tripManager.activeTrip.value)
        assertEquals(TripState.ACTIVE, tripManager.currentTripState.value)
    }

    // endregion

    // region Force Start/End Tests

    @Test
    fun `forceStartTrip creates new trip with MANUAL trigger`() = runTest {
        val trip = tripManager.forceStartTrip()

        assertNotNull(trip)
        assertEquals(TripState.ACTIVE, trip.state)
        assertEquals(TripTrigger.MANUAL, trip.startTrigger)
        coVerify { tripRepository.insert(any()) }
    }

    @Test
    fun `forceStartTrip updates state to ACTIVE`() = runTest {
        tripManager.forceStartTrip()

        assertEquals(TripState.ACTIVE, tripManager.currentTripState.value)
        assertNotNull(tripManager.activeTrip.value)
    }

    @Test
    fun `forceEndTrip finalizes trip with MANUAL trigger`() = runTest {
        tripManager.forceStartTrip()
        val finalizedTrip = tripManager.forceEndTrip()

        assertNotNull(finalizedTrip)
        assertEquals(TripState.COMPLETED, finalizedTrip.state)
        assertEquals(TripTrigger.MANUAL, finalizedTrip.endTrigger)
    }

    @Test
    fun `forceEndTrip returns null when no active trip`() = runTest {
        val result = tripManager.forceEndTrip()
        assertNull(result)
    }

    @Test
    fun `forceEndTrip transitions to IDLE after completion`() = runTest {
        tripManager.forceStartTrip()
        tripManager.forceEndTrip()

        assertEquals(TripState.IDLE, tripManager.currentTripState.value)
        assertNull(tripManager.activeTrip.value)
    }

    // endregion

    // region Query Tests

    @Test
    fun `getTripById delegates to repository`() = runTest {
        val trip = createTestTrip()
        coEvery { tripRepository.getTripById("test-id") } returns trip

        val result = tripManager.getTripById("test-id")

        assertEquals(trip, result)
    }

    @Test
    fun `getTripsInRange delegates to repository`() = runTest {
        val trips = listOf(createTestTrip())
        coEvery { tripRepository.getTripsBetween(any(), any()) } returns trips

        val result = tripManager.getTripsInRange(0, 1000)

        assertEquals(trips, result)
    }

    @Test
    fun `getRecentTrips delegates to repository`() = runTest {
        val trips = listOf(createTestTrip())
        coEvery { tripRepository.observeRecentTrips(any()) } returns flowOf(trips)

        val result = tripManager.getRecentTrips(10)

        assertEquals(trips, result)
    }

    // endregion

    // region TripModeSegment Tests

    @Test
    fun `TripModeSegment calculates duration correctly`() {
        val now = Clock.System.now()
        val later = kotlinx.datetime.Instant.fromEpochMilliseconds(
            now.toEpochMilliseconds() + 60000,
        )

        val segment = TripModeSegment(
            mode = TransportationMode.WALKING,
            startTime = now,
            endTime = later,
        )

        assertEquals(60000L, segment.durationMs)
        assertEquals(60L, segment.durationSeconds)
    }

    @Test
    fun `TripModeSegment returns 0 duration when active`() {
        val segment = TripModeSegment(
            mode = TransportationMode.WALKING,
            startTime = Clock.System.now(),
            endTime = null,
        )

        assertEquals(0L, segment.durationMs)
        assertTrue(segment.isActive)
    }

    @Test
    fun `TripModeSegment end creates copy with endTime`() {
        val now = Clock.System.now()
        val segment = TripModeSegment(
            mode = TransportationMode.WALKING,
            startTime = now,
        )

        val later = kotlinx.datetime.Instant.fromEpochMilliseconds(
            now.toEpochMilliseconds() + 30000,
        )
        val ended = segment.end(later)

        assertNull(segment.endTime)
        assertEquals(later, ended.endTime)
        assertFalse(ended.isActive)
    }

    // endregion

    // region Location Update Tests

    @Test
    fun `updateLocation stores last known location`() = runTest {
        tripManager.updateLocation(37.7749, -122.4194)

        // Force start a trip to verify location is used
        val trip = tripManager.forceStartTrip()

        assertEquals(37.7749, trip.startLocation.latitude)
        assertEquals(-122.4194, trip.startLocation.longitude)
    }

    @Test
    fun `addDistance delegates to repository`() = runTest {
        tripManager.addDistance("trip-id", 150.0)

        coVerify { tripRepository.incrementLocationCount("trip-id", 150.0) }
    }

    // endregion

    // region Helper Functions

    private fun createTestTrip(
        id: String = "test-trip",
        state: TripState = TripState.ACTIVE,
    ) = Trip(
        id = id,
        state = state,
        startTime = Clock.System.now(),
        endTime = null,
        startLocation = LatLng(37.7749, -122.4194),
        endLocation = null,
        totalDistanceMeters = 0.0,
        locationCount = 0,
        dominantMode = TransportationMode.WALKING,
        modesUsed = setOf(TransportationMode.WALKING),
        modeBreakdown = mapOf(TransportationMode.WALKING to 0L),
        startTrigger = TripTrigger.MANUAL,
        endTrigger = null,
        isSynced = false,
        syncedAt = null,
        serverId = null,
        createdAt = Clock.System.now(),
        updatedAt = Clock.System.now(),
    )

    // endregion
}

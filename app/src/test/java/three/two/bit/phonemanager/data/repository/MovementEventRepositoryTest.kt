package three.two.bit.phonemanager.data.repository

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.time.Clock
import org.junit.Before
import org.junit.Test
import three.two.bit.phonemanager.data.database.MovementEventDao
import three.two.bit.phonemanager.data.model.MovementEventEntity
import three.two.bit.phonemanager.domain.model.DetectionSource
import three.two.bit.phonemanager.domain.model.DeviceState
import three.two.bit.phonemanager.domain.model.EventLocation
import three.two.bit.phonemanager.domain.model.MovementContext
import three.two.bit.phonemanager.domain.model.MovementEvent
import three.two.bit.phonemanager.domain.model.NetworkType
import three.two.bit.phonemanager.domain.model.SensorTelemetry
import three.two.bit.phonemanager.movement.TransportationMode
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Story E8.3: MovementEventRepository Unit Tests
 *
 * Tests for MovementEventRepository with mocked MovementEventDao.
 */
class MovementEventRepositoryTest {

    private lateinit var movementEventDao: MovementEventDao
    private lateinit var movementEventApiService: three.two.bit.phonemanager.network.MovementEventApiService
    private lateinit var repository: MovementEventRepositoryImpl

    @Before
    fun setup() {
        movementEventDao = mockk(relaxed = true)
        movementEventApiService = mockk(relaxed = true)
        repository = MovementEventRepositoryImpl(movementEventDao, movementEventApiService)
    }

    // region Test Data

    private fun createTestEventEntity(
        id: Long = 1L,
        tripId: String? = "trip-1",
        previousMode: String = "STATIONARY",
        newMode: String = "WALKING",
    ) = MovementEventEntity(
        id = id,
        timestamp = System.currentTimeMillis(),
        tripId = tripId,
        previousMode = previousMode,
        newMode = newMode,
        detectionSource = "ACTIVITY_RECOGNITION",
        confidence = 0.85f,
        detectionLatencyMs = 150L,
        latitude = 37.7749,
        longitude = -122.4194,
        accuracy = 10.0f,
        speed = 1.5f,
        batteryLevel = 85,
        batteryCharging = false,
        networkType = "WIFI",
        networkStrength = -55,
        accelerometerMagnitude = 9.8f,
        accelerometerVariance = 0.5f,
        accelerometerPeakFrequency = 2.5f,
        gyroscopeMagnitude = 0.1f,
        stepCount = 100,
        significantMotion = true,
        activityType = "walking",
        activityConfidence = 90,
        distanceFromLastLocation = 50.0f,
        timeSinceLastLocation = 30000L,
        isSynced = false,
        syncedAt = null,
    )

    private fun createTestEvent(id: Long = 1L, tripId: String? = "trip-1") = MovementEvent(
        id = id,
        timestamp = Clock.System.now(),
        tripId = tripId,
        previousMode = TransportationMode.STATIONARY,
        newMode = TransportationMode.WALKING,
        detectionSource = DetectionSource.ACTIVITY_RECOGNITION,
        confidence = 0.85f,
        detectionLatencyMs = 150L,
        location = EventLocation(
            latitude = 37.7749,
            longitude = -122.4194,
            accuracy = 10.0f,
            speed = 1.5f,
        ),
        deviceState = DeviceState(
            batteryLevel = 85,
            batteryCharging = false,
            networkType = NetworkType.WIFI,
            networkStrength = -55,
        ),
        sensorTelemetry = SensorTelemetry(
            accelerometerMagnitude = 9.8f,
            accelerometerVariance = 0.5f,
            accelerometerPeakFrequency = 2.5f,
            gyroscopeMagnitude = 0.1f,
            stepCount = 100,
            significantMotion = true,
            activityType = "walking",
            activityConfidence = 90,
        ),
        movementContext = MovementContext(
            distanceFromLastLocation = 50.0f,
            timeSinceLastLocation = 30000L,
        ),
        isSynced = false,
        syncedAt = null,
    )

    // endregion

    // region RecordEvent Tests

    @Test
    fun `recordEvent creates entity and returns event id`() = runTest {
        coEvery { movementEventDao.insert(any()) } returns 123L

        val result = repository.recordEvent(
            tripId = "trip-1",
            previousMode = TransportationMode.STATIONARY,
            newMode = TransportationMode.WALKING,
            detectionSource = DetectionSource.ACTIVITY_RECOGNITION,
            confidence = 0.85f,
            detectionLatencyMs = 150L,
            location = EventLocation(37.7749, -122.4194, 10.0f, 1.5f),
            deviceState = DeviceState(85, false, NetworkType.WIFI, -55),
            sensorTelemetry = SensorTelemetry(9.8f, 0.5f, 2.5f, 0.1f, 100, true, "walking", 90),
            movementContext = MovementContext(50.0f, 30000L),
        )

        assertTrue(result.isSuccess)
        assertEquals(123L, result.getOrNull())
        coVerify { movementEventDao.insert(any()) }
    }

    @Test
    fun `recordEvent works with null optional fields`() = runTest {
        coEvery { movementEventDao.insert(any()) } returns 456L

        val result = repository.recordEvent(
            tripId = null,
            previousMode = TransportationMode.WALKING,
            newMode = TransportationMode.IN_VEHICLE,
            detectionSource = DetectionSource.SPEED_BASED,
            confidence = 0.75f,
            detectionLatencyMs = 200L,
        )

        assertTrue(result.isSuccess)
        assertEquals(456L, result.getOrNull())
    }

    // endregion

    // region Insert Tests

    @Test
    fun `insert event should call dao insert`() = runTest {
        val event = createTestEvent()
        coEvery { movementEventDao.insert(any()) } returns 1L

        val result = repository.insert(event)

        assertTrue(result.isSuccess)
        coVerify { movementEventDao.insert(any()) }
    }

    @Test
    fun `insertAll events should call dao insertAll`() = runTest {
        val events = listOf(
            createTestEvent(id = 1L),
            createTestEvent(id = 2L),
        )

        val result = repository.insertAll(events)

        assertTrue(result.isSuccess)
        coVerify { movementEventDao.insertAll(any()) }
    }

    // endregion

    // region Query Tests

    @Test
    fun `getEventById returns mapped domain model`() = runTest {
        val entity = createTestEventEntity()
        coEvery { movementEventDao.getEventById(1L) } returns entity

        val result = repository.getEventById(1L)

        assertNotNull(result)
        assertEquals(1L, result.id)
        assertEquals(TransportationMode.STATIONARY, result.previousMode)
        assertEquals(TransportationMode.WALKING, result.newMode)
        assertEquals(DetectionSource.ACTIVITY_RECOGNITION, result.detectionSource)
    }

    @Test
    fun `getEventById returns null when not found`() = runTest {
        coEvery { movementEventDao.getEventById(999L) } returns null

        val result = repository.getEventById(999L)

        assertNull(result)
    }

    @Test
    fun `observeRecentEvents emits list of mapped events`() = runTest {
        val entities = listOf(
            createTestEventEntity(id = 1L),
            createTestEventEntity(id = 2L),
        )
        every { movementEventDao.observeRecentEvents(10) } returns flowOf(entities)

        val result = repository.observeRecentEvents(10).first()

        assertEquals(2, result.size)
        assertEquals(1L, result[0].id)
        assertEquals(2L, result[1].id)
    }

    @Test
    fun `observeEventsByTrip filters by trip id`() = runTest {
        val entities = listOf(createTestEventEntity(tripId = "trip-1"))
        every { movementEventDao.observeEventsByTrip("trip-1") } returns flowOf(entities)

        val result = repository.observeEventsByTrip("trip-1").first()

        assertEquals(1, result.size)
        assertEquals("trip-1", result[0].tripId)
    }

    @Test
    fun `observeLatestEvent emits latest event`() = runTest {
        val entity = createTestEventEntity()
        every { movementEventDao.observeLatestEvent() } returns flowOf(entity)

        val result = repository.observeLatestEvent().first()

        assertNotNull(result)
    }

    @Test
    fun `getLatestEvent returns latest event`() = runTest {
        val entity = createTestEventEntity()
        coEvery { movementEventDao.getLatestEvent() } returns entity

        val result = repository.getLatestEvent()

        assertNotNull(result)
    }

    @Test
    fun `getEventsBetween returns events in time range`() = runTest {
        val entities = listOf(createTestEventEntity())
        coEvery { movementEventDao.getEventsBetween(any(), any()) } returns entities

        val start = Clock.System.now()
        val end = Clock.System.now()
        val result = repository.getEventsBetween(start, end)

        assertEquals(1, result.size)
    }

    @Test
    fun `getEventsByTrip returns events for trip`() = runTest {
        val entities = listOf(createTestEventEntity(tripId = "trip-1"))
        coEvery { movementEventDao.getEventsByTrip("trip-1") } returns entities

        val result = repository.getEventsByTrip("trip-1")

        assertEquals(1, result.size)
    }

    // endregion

    // region Statistics Tests

    @Test
    fun `observeEventCountSince returns count flow`() = runTest {
        every { movementEventDao.observeEventCountSince(any()) } returns flowOf(10)

        val since = Clock.System.now()
        val result = repository.observeEventCountSince(since).first()

        assertEquals(10, result)
    }

    @Test
    fun `getEventCountForTrip returns event count`() = runTest {
        coEvery { movementEventDao.getEventCountForTrip("trip-1") } returns 5

        val result = repository.getEventCountForTrip("trip-1")

        assertEquals(5, result)
    }

    @Test
    fun `observeUnsyncedCount returns count flow`() = runTest {
        every { movementEventDao.observeUnsyncedCount() } returns flowOf(15)

        val result = repository.observeUnsyncedCount().first()

        assertEquals(15, result)
    }

    // endregion

    // region Sync Tests

    @Test
    fun `getUnsyncedEvents returns unsynced events`() = runTest {
        val entities = listOf(createTestEventEntity())
        coEvery { movementEventDao.getUnsyncedEvents(100) } returns entities

        val result = repository.getUnsyncedEvents()

        assertEquals(1, result.size)
    }

    @Test
    fun `markAsSynced updates sync status`() = runTest {
        val syncedAt = Clock.System.now()
        val eventIds = listOf(1L, 2L, 3L)

        repository.markAsSynced(eventIds, syncedAt)

        coVerify {
            movementEventDao.markAsSynced(eventIds, syncedAt.toEpochMilliseconds())
        }
    }

    @Test
    fun `deleteOldEvents removes events before cutoff`() = runTest {
        coEvery { movementEventDao.deleteOldEvents(any()) } returns 10
        val before = Clock.System.now()

        val result = repository.deleteOldEvents(before)

        assertEquals(10, result)
    }

    @Test
    fun `deleteEventsByTrip removes events for trip`() = runTest {
        coEvery { movementEventDao.deleteEventsByTrip("trip-1") } returns 5

        val result = repository.deleteEventsByTrip("trip-1")

        assertEquals(5, result)
    }

    // endregion

    // region Domain Model Mapping Tests

    @Test
    fun `entity maps nested location correctly`() = runTest {
        val entity = createTestEventEntity()
        coEvery { movementEventDao.getEventById(1L) } returns entity

        val result = repository.getEventById(1L)

        assertNotNull(result?.location)
        assertEquals(37.7749, result?.location?.latitude)
        assertEquals(-122.4194, result?.location?.longitude)
        assertEquals(10.0f, result?.location?.accuracy)
        assertEquals(1.5f, result?.location?.speed)
    }

    @Test
    fun `entity maps nested device state correctly`() = runTest {
        val entity = createTestEventEntity()
        coEvery { movementEventDao.getEventById(1L) } returns entity

        val result = repository.getEventById(1L)

        assertNotNull(result?.deviceState)
        assertEquals(85, result?.deviceState?.batteryLevel)
        assertEquals(false, result?.deviceState?.batteryCharging)
        assertEquals(NetworkType.WIFI, result?.deviceState?.networkType)
    }

    @Test
    fun `entity maps nested sensor telemetry correctly`() = runTest {
        val entity = createTestEventEntity()
        coEvery { movementEventDao.getEventById(1L) } returns entity

        val result = repository.getEventById(1L)

        assertNotNull(result?.sensorTelemetry)
        assertEquals(9.8f, result?.sensorTelemetry?.accelerometerMagnitude)
        assertEquals(100, result?.sensorTelemetry?.stepCount)
        assertEquals(true, result?.sensorTelemetry?.significantMotion)
    }

    @Test
    fun `entity with null optional fields maps correctly`() = runTest {
        val entity = MovementEventEntity(
            id = 1L,
            timestamp = System.currentTimeMillis(),
            tripId = null,
            previousMode = "STATIONARY",
            newMode = "WALKING",
            detectionSource = "MANUAL",
            confidence = 1.0f,
            detectionLatencyMs = 0L,
            // All optional fields null
        )
        coEvery { movementEventDao.getEventById(1L) } returns entity

        val result = repository.getEventById(1L)

        assertNotNull(result)
        assertNull(result.tripId)
        assertNull(result.location)
        assertNull(result.deviceState)
        assertNull(result.sensorTelemetry)
        assertNull(result.movementContext)
    }

    // endregion
}

package three.two.bit.phonemanager.data.repository

import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import three.two.bit.phonemanager.data.database.ProximityAlertDao
import three.two.bit.phonemanager.data.model.ProximityAlertEntity
import three.two.bit.phonemanager.domain.model.AlertDirection
import three.two.bit.phonemanager.domain.model.ProximityState
import three.two.bit.phonemanager.network.NetworkManager
import three.two.bit.phonemanager.network.ProximityAlertApiService
import three.two.bit.phonemanager.network.models.ProximityAlertDto
import three.two.bit.phonemanager.security.SecureStorage
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Story E5.1: AlertRepository Unit Tests
 *
 * Tests for proximity alert CRUD operations and server sync
 * Coverage target: > 80%
 */
class AlertRepositoryTest {

    private lateinit var repository: AlertRepositoryImpl
    private lateinit var proximityAlertDao: ProximityAlertDao
    private lateinit var alertApiService: ProximityAlertApiService
    private lateinit var networkManager: NetworkManager
    private lateinit var secureStorage: SecureStorage

    private val testDeviceId = "test-device-id"
    private val testTargetDeviceId = "target-device-id"

    @Before
    fun setup() {
        proximityAlertDao = mockk(relaxed = true)
        alertApiService = mockk(relaxed = true)
        networkManager = mockk(relaxed = true)
        secureStorage = mockk(relaxed = true)

        every { secureStorage.getDeviceId() } returns testDeviceId
        every { networkManager.isNetworkAvailable() } returns true

        repository = AlertRepositoryImpl(
            proximityAlertDao = proximityAlertDao,
            alertApiService = alertApiService,
            networkManager = networkManager,
            secureStorage = secureStorage,
        )
    }

    // region createAlert tests

    @Test
    fun `createAlert saves alert locally`() = runTest {
        coEvery { alertApiService.createAlert(any()) } returns Result.failure(Exception("Network error"))

        val result = repository.createAlert(
            targetDeviceId = testTargetDeviceId,
            radiusMeters = 100,
            direction = AlertDirection.ENTER,
        )

        assertTrue(result.isSuccess)
        coVerify { proximityAlertDao.insert(any()) }
    }

    @Test
    fun `createAlert sets correct default values`() = runTest {
        coEvery { alertApiService.createAlert(any()) } returns Result.failure(Exception("Network error"))

        val result = repository.createAlert(
            targetDeviceId = testTargetDeviceId,
            radiusMeters = 100,
            direction = AlertDirection.ENTER,
        )

        assertTrue(result.isSuccess)
        val alert = result.getOrNull()!!
        assertEquals(testDeviceId, alert.ownerDeviceId)
        assertEquals(testTargetDeviceId, alert.targetDeviceId)
        assertEquals(100, alert.radiusMeters)
        assertEquals(AlertDirection.ENTER, alert.direction)
        assertTrue(alert.active)
        assertEquals(ProximityState.OUTSIDE, alert.lastState)
        assertNull(alert.lastTriggeredAt)
    }

    @Test
    fun `createAlert syncs to server when network available`() = runTest {
        val serverResponse = ProximityAlertDto(
            alertId = "server-alert-id",
            sourceDeviceId = testDeviceId,
            targetDeviceId = testTargetDeviceId,
            radiusMeters = 100,
            isActive = true,
            createdAt = "2025-11-28T12:00:00Z",
            updatedAt = "2025-11-28T12:00:00Z",
        )
        coEvery { alertApiService.createAlert(any()) } returns Result.success(serverResponse)

        val result = repository.createAlert(
            targetDeviceId = testTargetDeviceId,
            radiusMeters = 100,
            direction = AlertDirection.ENTER,
        )

        assertTrue(result.isSuccess)
        coVerify { alertApiService.createAlert(any()) }
    }

    @Test
    fun `createAlert does not call server when network unavailable`() = runTest {
        every { networkManager.isNetworkAvailable() } returns false

        repository.createAlert(
            targetDeviceId = testTargetDeviceId,
            radiusMeters = 100,
            direction = AlertDirection.ENTER,
        )

        coVerify(exactly = 0) { alertApiService.createAlert(any()) }
    }

    // endregion

    // region getAlert tests

    @Test
    fun `getAlert returns alert when found`() = runTest {
        val entity = createTestAlertEntity("alert-1")
        coEvery { proximityAlertDao.getById("alert-1") } returns entity

        val result = repository.getAlert("alert-1")

        assertNotNull(result)
        assertEquals("alert-1", result.id)
    }

    @Test
    fun `getAlert returns null when not found`() = runTest {
        coEvery { proximityAlertDao.getById("non-existent") } returns null

        val result = repository.getAlert("non-existent")

        assertNull(result)
    }

    // endregion

    // region toggleAlertActive tests

    @Test
    fun `toggleAlertActive updates local database`() = runTest {
        val entity = createTestAlertEntity("alert-1", active = true)
        coEvery { proximityAlertDao.getById("alert-1") } returns entity

        val result = repository.toggleAlertActive("alert-1", false)

        assertTrue(result.isSuccess)
        coVerify {
            proximityAlertDao.update(match { it.active == false })
        }
    }

    @Test
    fun `toggleAlertActive returns failure for non-existent alert`() = runTest {
        coEvery { proximityAlertDao.getById("non-existent") } returns null

        val result = repository.toggleAlertActive("non-existent", false)

        assertTrue(result.isFailure)
    }

    @Test
    fun `toggleAlertActive syncs to server when network available`() = runTest {
        val entity = createTestAlertEntity("alert-1")
        coEvery { proximityAlertDao.getById("alert-1") } returns entity

        repository.toggleAlertActive("alert-1", false)

        coVerify { alertApiService.updateAlert("alert-1", any()) }
    }

    // endregion

    // region deleteAlert tests

    @Test
    fun `deleteAlert removes from local database`() = runTest {
        val entity = createTestAlertEntity("alert-1")
        coEvery { proximityAlertDao.getById("alert-1") } returns entity
        coJustRun { proximityAlertDao.delete(entity) }
        coEvery { alertApiService.deleteAlert("alert-1") } returns Result.success(Unit)

        val result = repository.deleteAlert("alert-1")

        assertTrue(result.isSuccess)
        coVerify { proximityAlertDao.delete(entity) }
    }

    @Test
    fun `deleteAlert returns failure for non-existent alert`() = runTest {
        coEvery { proximityAlertDao.getById("non-existent") } returns null

        val result = repository.deleteAlert("non-existent")

        assertTrue(result.isFailure)
    }

    @Test
    fun `deleteAlert syncs to server when network available`() = runTest {
        val entity = createTestAlertEntity("alert-1")
        coEvery { proximityAlertDao.getById("alert-1") } returns entity
        coJustRun { proximityAlertDao.delete(entity) }
        coEvery { alertApiService.deleteAlert("alert-1") } returns Result.success(Unit)

        repository.deleteAlert("alert-1")

        coVerify { alertApiService.deleteAlert("alert-1") }
    }

    // endregion

    // region syncFromServer tests

    @Test
    fun `syncFromServer returns failure when network unavailable`() = runTest {
        every { networkManager.isNetworkAvailable() } returns false

        val result = repository.syncFromServer()

        assertTrue(result.isFailure)
    }

    // endregion

    // region updateLastTriggered tests

    @Test
    fun `updateLastTriggered updates database`() = runTest {
        val entity = createTestAlertEntity("alert-1")
        coEvery { proximityAlertDao.getById("alert-1") } returns entity

        val triggeredAt = kotlinx.datetime.Clock.System.now()
        repository.updateLastTriggered("alert-1", triggeredAt)

        coVerify {
            proximityAlertDao.update(match {
                it.lastTriggeredAt == triggeredAt.toEpochMilliseconds()
            })
        }
    }

    @Test
    fun `updateLastTriggered does nothing for non-existent alert`() = runTest {
        coEvery { proximityAlertDao.getById("non-existent") } returns null

        repository.updateLastTriggered("non-existent", kotlinx.datetime.Clock.System.now())

        coVerify(exactly = 0) { proximityAlertDao.update(any()) }
    }

    // endregion

    // region updateProximityState tests

    @Test
    fun `updateProximityState updates database`() = runTest {
        val entity = createTestAlertEntity("alert-1")
        coEvery { proximityAlertDao.getById("alert-1") } returns entity

        repository.updateProximityState("alert-1", ProximityState.INSIDE)

        coVerify {
            proximityAlertDao.update(match {
                it.lastState == ProximityState.INSIDE.name
            })
        }
    }

    @Test
    fun `updateProximityState does nothing for non-existent alert`() = runTest {
        coEvery { proximityAlertDao.getById("non-existent") } returns null

        repository.updateProximityState("non-existent", ProximityState.INSIDE)

        coVerify(exactly = 0) { proximityAlertDao.update(any()) }
    }

    // endregion

    // region observeAlerts tests

    @Test
    fun `observeAlerts returns flow from DAO`() = runTest {
        val entities = listOf(
            createTestAlertEntity("alert-1"),
            createTestAlertEntity("alert-2"),
        )
        every { proximityAlertDao.observeAlertsByOwner(testDeviceId) } returns flowOf(entities)

        val flow = repository.observeAlerts()

        flow.collect { alerts ->
            assertEquals(2, alerts.size)
        }
    }

    // endregion

    // region observeActiveAlerts tests

    @Test
    fun `observeActiveAlerts returns only active alerts`() = runTest {
        val entities = listOf(createTestAlertEntity("alert-1", active = true))
        every { proximityAlertDao.observeActiveAlerts(testDeviceId) } returns flowOf(entities)

        val flow = repository.observeActiveAlerts()

        flow.collect { alerts ->
            assertEquals(1, alerts.size)
            assertTrue(alerts[0].active)
        }
    }

    // endregion

    // Helper functions

    private fun createTestAlertEntity(
        id: String,
        active: Boolean = true,
    ) = ProximityAlertEntity(
        id = id,
        ownerDeviceId = testDeviceId,
        targetDeviceId = testTargetDeviceId,
        radiusMeters = 100,
        direction = AlertDirection.ENTER.name,
        active = active,
        lastState = ProximityState.OUTSIDE.name,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis(),
        lastTriggeredAt = null,
    )
}

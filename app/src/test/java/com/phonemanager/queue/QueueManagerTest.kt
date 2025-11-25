package com.phonemanager.queue

import com.phonemanager.data.database.LocationDao
import com.phonemanager.data.database.LocationQueueDao
import com.phonemanager.data.model.LocationEntity
import com.phonemanager.data.model.LocationQueueEntity
import com.phonemanager.data.model.QueueStatus
import com.phonemanager.network.NetworkManager
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for QueueManager
 *
 * Story 0.2.3: Tests queue management and retry logic
 * Verifies:
 * - Location enqueueing
 * - Queue processing with network check
 * - Retry logic with exponential backoff
 * - Failed item handling
 * - Queue statistics
 */
@OptIn(ExperimentalCoroutinesApi::class)
class QueueManagerTest {

    private lateinit var queueManager: QueueManager
    private lateinit var locationDao: LocationDao
    private lateinit var locationQueueDao: LocationQueueDao
    private lateinit var networkManager: NetworkManager

    @Before
    fun setup() {
        locationDao = mockk(relaxed = true)
        locationQueueDao = mockk(relaxed = true)
        networkManager = mockk(relaxed = true)

        queueManager = QueueManager(
            locationDao = locationDao,
            locationQueueDao = locationQueueDao,
            networkManager = networkManager,
        )
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `enqueueLocation adds location to queue with PENDING status`() = runTest {
        // Given
        val locationId = 123L

        coEvery { locationQueueDao.insert(any()) } returns locationId

        // When
        queueManager.enqueueLocation(locationId)

        // Then
        coVerify {
            locationQueueDao.insert(
                match {
                    it.locationId == locationId && it.status == QueueStatus.PENDING
                },
            )
        }
    }

    @Test
    fun `processQueue returns 0 when network unavailable`() = runTest {
        // Given
        every { networkManager.isNetworkAvailable() } returns false

        // When
        val result = queueManager.processQueue()

        // Then
        assertEquals(0, result)
        coVerify(exactly = 0) { locationQueueDao.getPendingItems(any(), any()) }
    }

    @Test
    fun `processQueue returns 0 when no pending items`() = runTest {
        // Given
        every { networkManager.isNetworkAvailable() } returns true
        coEvery { locationQueueDao.getPendingItems(any(), any()) } returns emptyList()

        // When
        val result = queueManager.processQueue()

        // Then
        assertEquals(0, result)
    }

    @Test
    fun `processQueue successfully uploads pending items`() = runTest {
        // Given
        val locationId = 1L
        val queueItem = LocationQueueEntity(
            locationId = locationId,
            status = QueueStatus.PENDING,
            retryCount = 0,
            queuedAt = System.currentTimeMillis(),
        )
        val locationEntity = LocationEntity(
            id = locationId,
            latitude = 40.7128,
            longitude = -74.0060,
            accuracy = 10f,
            timestamp = System.currentTimeMillis(),
        )
        val uploadResponse = mockk<com.phonemanager.network.models.LocationUploadResponse>(relaxed = true)

        every { networkManager.isNetworkAvailable() } returns true
        coEvery { locationQueueDao.getPendingItems(any(), any()) } returns listOf(queueItem)
        coEvery { locationDao.getById(locationId) } returns locationEntity
        coEvery { networkManager.uploadLocation(locationEntity) } returns Result.success(uploadResponse)
        coEvery { locationQueueDao.update(any()) } just Runs

        // When
        val result = queueManager.processQueue()

        // Then
        assertEquals(1, result)
        coVerify {
            // First update to UPLOADING
            locationQueueDao.update(
                match {
                    it.locationId == queueItem.locationId && it.status == QueueStatus.UPLOADING
                },
            )

            // Then update to UPLOADED
            locationQueueDao.update(
                match {
                    it.locationId == queueItem.locationId && it.status == QueueStatus.UPLOADED
                },
            )
        }
    }

    @Test
    fun `processQueue handles upload failure with retry logic`() = runTest {
        // Given
        val locationId = 1L
        val queueItem = LocationQueueEntity(
            locationId = locationId,
            status = QueueStatus.PENDING,
            retryCount = 0,
            queuedAt = System.currentTimeMillis(),
        )
        val locationEntity = LocationEntity(
            id = locationId,
            latitude = 40.7128,
            longitude = -74.0060,
            accuracy = 10f,
            timestamp = System.currentTimeMillis(),
        )

        every { networkManager.isNetworkAvailable() } returns true
        coEvery { locationQueueDao.getPendingItems(any(), any()) } returns listOf(queueItem)
        coEvery { locationDao.getById(locationId) } returns locationEntity
        coEvery { networkManager.uploadLocation(locationEntity) } returns Result.failure(Exception("Network error"))
        coEvery { locationQueueDao.update(any()) } just Runs

        // When
        val result = queueManager.processQueue()

        // Then
        assertEquals(0, result, "Should return 0 success count on failure")
        coVerify {
            // Update to RETRY_PENDING with incremented retry count
            locationQueueDao.update(
                match {
                    it.locationId == queueItem.locationId &&
                        it.status == QueueStatus.RETRY_PENDING &&
                        it.retryCount == 1 &&
                        (it.nextRetryTime ?: 0) > System.currentTimeMillis()
                },
            )
        }
    }

    @Test
    fun `processQueue marks item as FAILED after max retries`() = runTest {
        // Given
        val locationId = 1L
        val queueItem = LocationQueueEntity(
            locationId = locationId,
            status = QueueStatus.PENDING,
            retryCount = 4, // Already tried 4 times, next will be 5 (max)
            queuedAt = System.currentTimeMillis(),
        )
        val locationEntity = LocationEntity(
            id = locationId,
            latitude = 40.7128,
            longitude = -74.0060,
            accuracy = 10f,
            timestamp = System.currentTimeMillis(),
        )

        every { networkManager.isNetworkAvailable() } returns true
        coEvery { locationQueueDao.getPendingItems(any(), any()) } returns listOf(queueItem)
        coEvery { locationDao.getById(locationId) } returns locationEntity
        coEvery { networkManager.uploadLocation(locationEntity) } returns Result.failure(Exception("Network error"))
        coEvery { locationQueueDao.update(any()) } just Runs

        // When
        val result = queueManager.processQueue()

        // Then
        assertEquals(0, result)
        coVerify {
            // Should mark as FAILED after max retries
            locationQueueDao.update(
                match {
                    it.locationId == queueItem.locationId &&
                        it.status == QueueStatus.FAILED &&
                        it.retryCount == 5
                },
            )
        }
    }

    @Test
    fun `processQueue handles missing location entity`() = runTest {
        // Given
        val locationId = 1L
        val queueItem = LocationQueueEntity(
            locationId = locationId,
            status = QueueStatus.PENDING,
            retryCount = 0,
            queuedAt = System.currentTimeMillis(),
        )

        every { networkManager.isNetworkAvailable() } returns true
        coEvery { locationQueueDao.getPendingItems(any(), any()) } returns listOf(queueItem)
        coEvery { locationDao.getById(locationId) } returns null
        coEvery { locationQueueDao.update(any()) } just Runs

        // When
        val result = queueManager.processQueue()

        // Then
        assertEquals(0, result)
        coVerify {
            locationQueueDao.update(
                match {
                    it.locationId == queueItem.locationId &&
                        it.status == QueueStatus.FAILED &&
                        it.errorMessage == "Location not found in database"
                },
            )
        }
    }

    @Test
    fun `processQueue processes multiple items`() = runTest {
        // Given
        val queueItem1 = LocationQueueEntity(
            locationId = 1L,
            status = QueueStatus.PENDING,
            retryCount = 0,
            queuedAt = System.currentTimeMillis(),
        )
        val queueItem2 = LocationQueueEntity(
            locationId = 2L,
            status = QueueStatus.PENDING,
            retryCount = 0,
            queuedAt = System.currentTimeMillis(),
        )
        val location1 = LocationEntity(
            id = 1L,
            latitude = 40.7128,
            longitude = -74.0060,
            accuracy = 10f,
            timestamp = System.currentTimeMillis(),
        )
        val location2 = LocationEntity(
            id = 2L,
            latitude = 34.0522,
            longitude = -118.2437,
            accuracy = 15f,
            timestamp = System.currentTimeMillis(),
        )
        val uploadResponse = mockk<com.phonemanager.network.models.LocationUploadResponse>(relaxed = true)

        every { networkManager.isNetworkAvailable() } returns true
        coEvery { locationQueueDao.getPendingItems(any(), any()) } returns listOf(queueItem1, queueItem2)
        coEvery { locationDao.getById(1L) } returns location1
        coEvery { locationDao.getById(2L) } returns location2
        coEvery { networkManager.uploadLocation(location1) } returns Result.success(uploadResponse)
        coEvery { networkManager.uploadLocation(location2) } returns Result.success(uploadResponse)
        coEvery { locationQueueDao.update(any()) } just Runs

        // When
        val result = queueManager.processQueue()

        // Then
        assertEquals(2, result)
        coVerify(exactly = 2) {
            locationQueueDao.update(match { it.status == QueueStatus.UPLOADED })
        }
    }

    @Test
    fun `retryFailedItems resets failed items for retry`() = runTest {
        // Given
        coEvery { locationQueueDao.resetFailedItems(any()) } returns 3

        // When
        val result = queueManager.retryFailedItems()

        // Then
        assertEquals(3, result)
        coVerify { locationQueueDao.resetFailedItems(any()) }
    }

    @Test
    fun `cleanupOldItems removes uploaded items older than 7 days`() = runTest {
        // Given
        val expectedCount = 10
        coEvery { locationQueueDao.deleteUploadedBefore(any()) } returns expectedCount

        // When
        val result = queueManager.cleanupOldItems()

        // Then
        assertEquals(expectedCount, result)
        coVerify {
            locationQueueDao.deleteUploadedBefore(
                match {
                    // Verify it's approximately 7 days ago (within 1 second tolerance)
                    val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
                    Math.abs(it - sevenDaysAgo) < 1000
                },
            )
        }
    }

    @Test
    fun `observePendingCount returns flow from dao`() = runTest {
        // Given
        val expectedFlow = flowOf(5)
        every { locationQueueDao.observePendingCount() } returns expectedFlow

        // When
        val result = queueManager.observePendingCount()

        // Then
        assertEquals(expectedFlow, result)
    }

    @Test
    fun `observeFailedCount returns flow from dao`() = runTest {
        // Given
        val expectedFlow = flowOf(2)
        every { locationQueueDao.observeFailedCount() } returns expectedFlow

        // When
        val result = queueManager.observeFailedCount()

        // Then
        assertEquals(expectedFlow, result)
    }

    @Test
    fun `exponential backoff increases with retry count`() = runTest {
        // This test verifies that the backoff time increases exponentially
        // We'll trigger multiple failures and verify nextRetryTime increases

        val locationId = 1L
        val locationEntity = LocationEntity(
            id = locationId,
            latitude = 40.7128,
            longitude = -74.0060,
            accuracy = 10f,
            timestamp = System.currentTimeMillis(),
        )

        val capturedUpdates = mutableListOf<LocationQueueEntity>()

        every { networkManager.isNetworkAvailable() } returns true
        coEvery { locationDao.getById(locationId) } returns locationEntity
        coEvery { networkManager.uploadLocation(locationEntity) } returns Result.failure(Exception("Network error"))
        coEvery { locationQueueDao.update(capture(capturedUpdates)) } just Runs

        // Simulate 3 retries
        for (retryCount in 0..2) {
            val queueItem = LocationQueueEntity(
                locationId = locationId,
                status = QueueStatus.PENDING,
                retryCount = retryCount,
                queuedAt = System.currentTimeMillis(),
            )

            coEvery { locationQueueDao.getPendingItems(any(), any()) } returns listOf(queueItem)
            queueManager.processQueue()
        }

        // Verify backoff times increase
        val retryPendingUpdates = capturedUpdates.filter { it.status == QueueStatus.RETRY_PENDING }
        assertTrue(retryPendingUpdates.size >= 3, "Should have at least 3 retry updates")

        // Verify each subsequent retry has a longer wait time
        for (i in 1 until retryPendingUpdates.size) {
            val prevWaitTime =
                (retryPendingUpdates[i - 1].nextRetryTime ?: 0) - (retryPendingUpdates[i - 1].lastAttemptTime ?: 0)
            val currWaitTime =
                (retryPendingUpdates[i].nextRetryTime ?: 0) - (retryPendingUpdates[i].lastAttemptTime ?: 0)
            // Current wait time should be roughly double the previous (accounting for jitter)
            assertTrue(
                currWaitTime > prevWaitTime,
                "Backoff should increase: previous=$prevWaitTime, current=$currWaitTime",
            )
        }
    }
}

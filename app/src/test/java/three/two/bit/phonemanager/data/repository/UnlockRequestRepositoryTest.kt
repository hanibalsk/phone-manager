package three.two.bit.phonemanager.data.repository

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.Before
import org.junit.Test
import three.two.bit.phonemanager.domain.model.UnlockRequestFilter
import three.two.bit.phonemanager.domain.model.UnlockRequestStatus
import three.two.bit.phonemanager.network.DeviceApiService
import three.two.bit.phonemanager.network.models.CreateUnlockRequestResponse
import three.two.bit.phonemanager.network.models.UnlockRequestListResponse
import three.two.bit.phonemanager.network.models.UnlockRequestResponse
import three.two.bit.phonemanager.network.models.WithdrawUnlockRequestResponse
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for UnlockRequestRepository
 *
 * Story E12.8: Unlock Request UI
 * Coverage target: > 80%
 */
class UnlockRequestRepositoryTest {

    private lateinit var repository: UnlockRequestRepositoryImpl
    private lateinit var deviceApiService: DeviceApiService
    private lateinit var authRepository: AuthRepository

    private val testAccessToken = "test-access-token"
    private val testDeviceId = "test-device-id"
    private val testRequestId = "test-request-id"

    @Before
    fun setup() {
        deviceApiService = mockk(relaxed = true)
        authRepository = mockk(relaxed = true)

        coEvery { authRepository.getAccessToken() } returns testAccessToken

        repository = UnlockRequestRepositoryImpl(
            deviceApiService = deviceApiService,
            authRepository = authRepository,
        )
    }

    // AC E12.8.2: Submit Unlock Request Tests

    @Test
    fun `createUnlockRequest returns failure when not authenticated`() = runTest {
        coEvery { authRepository.getAccessToken() } returns null

        val result = repository.createUnlockRequest(
            deviceId = testDeviceId,
            settingKey = "tracking_enabled",
            reason = "I need to disable tracking temporarily",
        )

        assertTrue(result.isFailure)
        assertEquals("Not authenticated", result.exceptionOrNull()?.message)
    }

    @Test
    fun `createUnlockRequest returns failure for empty reason`() = runTest {
        val result = repository.createUnlockRequest(
            deviceId = testDeviceId,
            settingKey = "tracking_enabled",
            reason = "",
        )

        assertTrue(result.isFailure)
        assertEquals("Reason cannot be empty", result.exceptionOrNull()?.message)
    }

    @Test
    fun `createUnlockRequest returns failure for blank reason`() = runTest {
        val result = repository.createUnlockRequest(
            deviceId = testDeviceId,
            settingKey = "tracking_enabled",
            reason = "   ",
        )

        assertTrue(result.isFailure)
        assertEquals("Reason cannot be empty", result.exceptionOrNull()?.message)
    }

    @Test
    fun `createUnlockRequest returns failure for reason less than 5 characters`() = runTest {
        val result = repository.createUnlockRequest(
            deviceId = testDeviceId,
            settingKey = "tracking_enabled",
            reason = "test",
        )

        assertTrue(result.isFailure)
        assertEquals("Reason must be at least 5 characters", result.exceptionOrNull()?.message)
    }

    @Test
    fun `createUnlockRequest returns failure for reason over 200 characters`() = runTest {
        val result = repository.createUnlockRequest(
            deviceId = testDeviceId,
            settingKey = "tracking_enabled",
            reason = "a".repeat(201),
        )

        assertTrue(result.isFailure)
        assertEquals("Reason cannot exceed 200 characters", result.exceptionOrNull()?.message)
    }

    @Test
    fun `createUnlockRequest returns created request on success`() = runTest {
        val response = CreateUnlockRequestResponse(
            id = testRequestId,
            deviceId = testDeviceId,
            settingKey = "tracking_enabled",
            reason = "I need to disable tracking for privacy",
            status = "pending",
            requestedBy = "user@test.com",
            requestedByName = "Test User",
            createdAt = "2025-01-15T10:00:00Z",
        )
        coEvery {
            deviceApiService.createUnlockRequest(
                deviceId = testDeviceId,
                settingKey = "tracking_enabled",
                reason = "I need to disable tracking for privacy",
                accessToken = testAccessToken,
            )
        } returns Result.success(response)

        val result = repository.createUnlockRequest(
            deviceId = testDeviceId,
            settingKey = "tracking_enabled",
            reason = "I need to disable tracking for privacy",
        )

        assertTrue(result.isSuccess)
        val request = result.getOrNull()
        assertNotNull(request)
        assertEquals(testRequestId, request.id)
        assertEquals("tracking_enabled", request.settingKey)
        assertEquals(UnlockRequestStatus.PENDING, request.status)
    }

    @Test
    fun `createUnlockRequest adds request to local list`() = runTest {
        val response = CreateUnlockRequestResponse(
            id = testRequestId,
            deviceId = testDeviceId,
            settingKey = "tracking_enabled",
            reason = "Valid reason here",
            status = "pending",
            requestedBy = "user@test.com",
            requestedByName = "Test User",
            createdAt = "2025-01-15T10:00:00Z",
        )
        coEvery {
            deviceApiService.createUnlockRequest(any(), any(), any(), any())
        } returns Result.success(response)

        assertTrue(repository.requests.value.isEmpty())

        repository.createUnlockRequest(
            deviceId = testDeviceId,
            settingKey = "tracking_enabled",
            reason = "Valid reason here",
        )

        assertEquals(1, repository.requests.value.size)
        assertEquals(testRequestId, repository.requests.value[0].id)
    }

    @Test
    fun `createUnlockRequest updates request summary`() = runTest {
        val response = CreateUnlockRequestResponse(
            id = testRequestId,
            deviceId = testDeviceId,
            settingKey = "tracking_enabled",
            reason = "Valid reason here",
            status = "pending",
            requestedBy = "user@test.com",
            requestedByName = "Test User",
            createdAt = "2025-01-15T10:00:00Z",
        )
        coEvery {
            deviceApiService.createUnlockRequest(any(), any(), any(), any())
        } returns Result.success(response)

        assertEquals(0, repository.requestSummary.value.pendingCount)

        repository.createUnlockRequest(
            deviceId = testDeviceId,
            settingKey = "tracking_enabled",
            reason = "Valid reason here",
        )

        assertEquals(1, repository.requestSummary.value.pendingCount)
    }

    // AC E12.8.3: View My Unlock Requests Tests

    @Test
    fun `getUnlockRequests returns failure when not authenticated`() = runTest {
        coEvery { authRepository.getAccessToken() } returns null

        val result = repository.getUnlockRequests(testDeviceId)

        assertTrue(result.isFailure)
    }

    @Test
    fun `getUnlockRequests returns list of requests on success`() = runTest {
        val response = UnlockRequestListResponse(
            requests = listOf(
                UnlockRequestResponse(
                    id = "request-1",
                    deviceId = testDeviceId,
                    settingKey = "tracking_enabled",
                    reason = "Need to disable",
                    status = "pending",
                    requestedBy = "user@test.com",
                    requestedByName = "User",
                    createdAt = "2025-01-15T10:00:00Z",
                ),
                UnlockRequestResponse(
                    id = "request-2",
                    deviceId = testDeviceId,
                    settingKey = "secret_mode_enabled",
                    reason = "Privacy needed",
                    status = "approved",
                    requestedBy = "user@test.com",
                    requestedByName = "User",
                    createdAt = "2025-01-14T08:00:00Z",
                    respondedBy = "admin@test.com",
                    respondedByName = "Admin",
                    response = "Approved for 24 hours",
                    respondedAt = "2025-01-14T09:00:00Z",
                ),
            ),
            total = 2,
        )
        coEvery {
            deviceApiService.getUnlockRequests(
                deviceId = testDeviceId,
                status = null,
                accessToken = testAccessToken,
            )
        } returns Result.success(response)

        val result = repository.getUnlockRequests(testDeviceId)

        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull()?.size)
    }

    @Test
    fun `getUnlockRequests with filter passes correct status parameter`() = runTest {
        val response = UnlockRequestListResponse(requests = emptyList())
        coEvery {
            deviceApiService.getUnlockRequests(
                deviceId = testDeviceId,
                status = "pending",
                accessToken = testAccessToken,
            )
        } returns Result.success(response)

        repository.getUnlockRequests(testDeviceId, UnlockRequestFilter.PENDING)

        coVerify {
            deviceApiService.getUnlockRequests(
                deviceId = testDeviceId,
                status = "pending",
                accessToken = testAccessToken,
            )
        }
    }

    @Test
    fun `getUnlockRequests updates requests state flow`() = runTest {
        val response = UnlockRequestListResponse(
            requests = listOf(
                UnlockRequestResponse(
                    id = "request-1",
                    deviceId = testDeviceId,
                    settingKey = "tracking_enabled",
                    reason = "Test reason",
                    status = "pending",
                    requestedBy = "user@test.com",
                    requestedByName = "User",
                    createdAt = "2025-01-15T10:00:00Z",
                ),
            ),
        )
        coEvery {
            deviceApiService.getUnlockRequests(any(), any(), any())
        } returns Result.success(response)

        repository.getUnlockRequests(testDeviceId)

        assertEquals(1, repository.requests.value.size)
    }

    @Test
    fun `getUnlockRequests sorts requests by createdAt descending`() = runTest {
        val response = UnlockRequestListResponse(
            requests = listOf(
                UnlockRequestResponse(
                    id = "old-request",
                    deviceId = testDeviceId,
                    settingKey = "tracking_enabled",
                    reason = "Old request",
                    status = "approved",
                    requestedBy = "user@test.com",
                    requestedByName = "User",
                    createdAt = "2025-01-14T10:00:00Z",
                ),
                UnlockRequestResponse(
                    id = "new-request",
                    deviceId = testDeviceId,
                    settingKey = "secret_mode_enabled",
                    reason = "New request",
                    status = "pending",
                    requestedBy = "user@test.com",
                    requestedByName = "User",
                    createdAt = "2025-01-15T10:00:00Z",
                ),
            ),
        )
        coEvery {
            deviceApiService.getUnlockRequests(any(), any(), any())
        } returns Result.success(response)

        val result = repository.getUnlockRequests(testDeviceId)

        assertTrue(result.isSuccess)
        // Newest first
        assertEquals("new-request", result.getOrNull()?.get(0)?.id)
        assertEquals("old-request", result.getOrNull()?.get(1)?.id)
    }

    // AC E12.8.4: Withdraw Unlock Request Tests

    @Test
    fun `withdrawRequest returns failure when not authenticated`() = runTest {
        coEvery { authRepository.getAccessToken() } returns null

        val result = repository.withdrawRequest(testRequestId)

        assertTrue(result.isFailure)
    }

    @Test
    fun `withdrawRequest returns failure for non-pending request`() = runTest {
        // Setup - add an approved request to local state
        val response = UnlockRequestListResponse(
            requests = listOf(
                UnlockRequestResponse(
                    id = testRequestId,
                    deviceId = testDeviceId,
                    settingKey = "tracking_enabled",
                    reason = "Test",
                    status = "approved",
                    requestedBy = "user@test.com",
                    createdAt = "2025-01-15T10:00:00Z",
                ),
            ),
        )
        coEvery {
            deviceApiService.getUnlockRequests(any(), any(), any())
        } returns Result.success(response)
        repository.getUnlockRequests(testDeviceId)

        val result = repository.withdrawRequest(testRequestId)

        assertTrue(result.isFailure)
        assertEquals("Request cannot be withdrawn", result.exceptionOrNull()?.message)
    }

    @Test
    fun `withdrawRequest returns success on successful withdrawal`() = runTest {
        // Setup - add a pending request to local state
        val listResponse = UnlockRequestListResponse(
            requests = listOf(
                UnlockRequestResponse(
                    id = testRequestId,
                    deviceId = testDeviceId,
                    settingKey = "tracking_enabled",
                    reason = "Test reason here",
                    status = "pending",
                    requestedBy = "user@test.com",
                    createdAt = "2025-01-15T10:00:00Z",
                ),
            ),
        )
        coEvery {
            deviceApiService.getUnlockRequests(any(), any(), any())
        } returns Result.success(listResponse)
        repository.getUnlockRequests(testDeviceId)

        val withdrawResponse = WithdrawUnlockRequestResponse(
            success = true,
            message = "Request withdrawn",
        )
        coEvery {
            deviceApiService.withdrawUnlockRequest(testRequestId, testAccessToken)
        } returns Result.success(withdrawResponse)

        val result = repository.withdrawRequest(testRequestId)

        assertTrue(result.isSuccess)
    }

    @Test
    fun `withdrawRequest removes request from local list on success`() = runTest {
        // Setup - add a pending request
        val listResponse = UnlockRequestListResponse(
            requests = listOf(
                UnlockRequestResponse(
                    id = testRequestId,
                    deviceId = testDeviceId,
                    settingKey = "tracking_enabled",
                    reason = "Test reason here",
                    status = "pending",
                    requestedBy = "user@test.com",
                    createdAt = "2025-01-15T10:00:00Z",
                ),
            ),
        )
        coEvery {
            deviceApiService.getUnlockRequests(any(), any(), any())
        } returns Result.success(listResponse)
        repository.getUnlockRequests(testDeviceId)
        assertEquals(1, repository.requests.value.size)

        val withdrawResponse = WithdrawUnlockRequestResponse(success = true)
        coEvery {
            deviceApiService.withdrawUnlockRequest(testRequestId, testAccessToken)
        } returns Result.success(withdrawResponse)

        repository.withdrawRequest(testRequestId)

        assertEquals(0, repository.requests.value.size)
    }

    @Test
    fun `withdrawRequest returns failure on API error`() = runTest {
        // Setup - add a pending request
        val listResponse = UnlockRequestListResponse(
            requests = listOf(
                UnlockRequestResponse(
                    id = testRequestId,
                    deviceId = testDeviceId,
                    settingKey = "tracking_enabled",
                    reason = "Test reason here",
                    status = "pending",
                    requestedBy = "user@test.com",
                    createdAt = "2025-01-15T10:00:00Z",
                ),
            ),
        )
        coEvery {
            deviceApiService.getUnlockRequests(any(), any(), any())
        } returns Result.success(listResponse)
        repository.getUnlockRequests(testDeviceId)

        val withdrawResponse = WithdrawUnlockRequestResponse(
            success = false,
            error = "Request already processed",
        )
        coEvery {
            deviceApiService.withdrawUnlockRequest(testRequestId, testAccessToken)
        } returns Result.success(withdrawResponse)

        val result = repository.withdrawRequest(testRequestId)

        assertTrue(result.isFailure)
        assertEquals("Request already processed", result.exceptionOrNull()?.message)
    }

    // AC E12.8.5: Request Status Notifications Tests

    @Test
    fun `updateRequestStatus updates request in local list`() = runTest {
        // Setup - add a pending request
        val listResponse = UnlockRequestListResponse(
            requests = listOf(
                UnlockRequestResponse(
                    id = testRequestId,
                    deviceId = testDeviceId,
                    settingKey = "tracking_enabled",
                    reason = "Test reason",
                    status = "pending",
                    requestedBy = "user@test.com",
                    createdAt = "2025-01-15T10:00:00Z",
                ),
            ),
        )
        coEvery {
            deviceApiService.getUnlockRequests(any(), any(), any())
        } returns Result.success(listResponse)
        repository.getUnlockRequests(testDeviceId)

        val respondedAt = Clock.System.now()
        repository.updateRequestStatus(
            requestId = testRequestId,
            status = UnlockRequestStatus.APPROVED,
            adminName = "Admin User",
            responseMessage = "Approved for testing",
            respondedAt = respondedAt,
        )

        val updatedRequest = repository.requests.value.find { it.id == testRequestId }
        assertNotNull(updatedRequest)
        assertEquals(UnlockRequestStatus.APPROVED, updatedRequest.status)
        assertEquals("Admin User", updatedRequest.respondedByName)
        assertEquals("Approved for testing", updatedRequest.response)
        assertEquals(respondedAt, updatedRequest.respondedAt)
    }

    @Test
    fun `updateRequestStatus updates summary counts`() = runTest {
        // Setup - add a pending request
        val listResponse = UnlockRequestListResponse(
            requests = listOf(
                UnlockRequestResponse(
                    id = testRequestId,
                    deviceId = testDeviceId,
                    settingKey = "tracking_enabled",
                    reason = "Test reason",
                    status = "pending",
                    requestedBy = "user@test.com",
                    createdAt = "2025-01-15T10:00:00Z",
                ),
            ),
        )
        coEvery {
            deviceApiService.getUnlockRequests(any(), any(), any())
        } returns Result.success(listResponse)
        repository.getUnlockRequests(testDeviceId)

        assertEquals(1, repository.requestSummary.value.pendingCount)
        assertEquals(0, repository.requestSummary.value.approvedCount)

        repository.updateRequestStatus(
            requestId = testRequestId,
            status = UnlockRequestStatus.APPROVED,
            adminName = "Admin",
            responseMessage = null,
            respondedAt = null,
        )

        assertEquals(0, repository.requestSummary.value.pendingCount)
        assertEquals(1, repository.requestSummary.value.approvedCount)
    }

    // Helper Methods Tests

    @Test
    fun `getRequestById returns request if exists`() = runTest {
        val listResponse = UnlockRequestListResponse(
            requests = listOf(
                UnlockRequestResponse(
                    id = testRequestId,
                    deviceId = testDeviceId,
                    settingKey = "tracking_enabled",
                    reason = "Test reason",
                    status = "pending",
                    requestedBy = "user@test.com",
                    createdAt = "2025-01-15T10:00:00Z",
                ),
            ),
        )
        coEvery {
            deviceApiService.getUnlockRequests(any(), any(), any())
        } returns Result.success(listResponse)
        repository.getUnlockRequests(testDeviceId)

        val request = repository.getRequestById(testRequestId)

        assertNotNull(request)
        assertEquals(testRequestId, request.id)
    }

    @Test
    fun `getRequestById returns null if request not found`() = runTest {
        val request = repository.getRequestById("non-existent")

        assertNull(request)
    }

    @Test
    fun `clearError clears error state`() = runTest {
        // Trigger an error
        coEvery {
            deviceApiService.getUnlockRequests(any(), any(), any())
        } returns Result.failure(Exception("Test error"))

        repository.getUnlockRequests(testDeviceId)
        assertEquals("Test error", repository.error.value)

        repository.clearError()

        assertNull(repository.error.value)
    }

    @Test
    fun `refresh calls getUnlockRequests`() = runTest {
        val response = UnlockRequestListResponse(requests = emptyList())
        coEvery {
            deviceApiService.getUnlockRequests(any(), any(), any())
        } returns Result.success(response)

        repository.refresh(testDeviceId)

        coVerify {
            deviceApiService.getUnlockRequests(
                deviceId = testDeviceId,
                status = null,
                accessToken = testAccessToken,
            )
        }
    }

    @Test
    fun `isLoading state is managed correctly during operations`() = runTest {
        assertFalse(repository.isLoading.value)

        val response = UnlockRequestListResponse(requests = emptyList())
        coEvery {
            deviceApiService.getUnlockRequests(any(), any(), any())
        } returns Result.success(response)

        repository.getUnlockRequests(testDeviceId)

        // After completion, loading should be false
        assertFalse(repository.isLoading.value)
    }

    // Request Summary Tests

    @Test
    fun `requestSummary correctly counts all statuses`() = runTest {
        val response = UnlockRequestListResponse(
            requests = listOf(
                UnlockRequestResponse(
                    id = "request-1",
                    deviceId = testDeviceId,
                    settingKey = "setting1",
                    reason = "Reason 1",
                    status = "pending",
                    requestedBy = "user@test.com",
                    createdAt = "2025-01-15T10:00:00Z",
                ),
                UnlockRequestResponse(
                    id = "request-2",
                    deviceId = testDeviceId,
                    settingKey = "setting2",
                    reason = "Reason 2",
                    status = "pending",
                    requestedBy = "user@test.com",
                    createdAt = "2025-01-15T10:00:00Z",
                ),
                UnlockRequestResponse(
                    id = "request-3",
                    deviceId = testDeviceId,
                    settingKey = "setting3",
                    reason = "Reason 3",
                    status = "approved",
                    requestedBy = "user@test.com",
                    createdAt = "2025-01-14T10:00:00Z",
                ),
                UnlockRequestResponse(
                    id = "request-4",
                    deviceId = testDeviceId,
                    settingKey = "setting4",
                    reason = "Reason 4",
                    status = "denied",
                    requestedBy = "user@test.com",
                    createdAt = "2025-01-13T10:00:00Z",
                ),
            ),
        )
        coEvery {
            deviceApiService.getUnlockRequests(any(), any(), any())
        } returns Result.success(response)

        repository.getUnlockRequests(testDeviceId)

        assertEquals(2, repository.requestSummary.value.pendingCount)
        assertEquals(1, repository.requestSummary.value.approvedCount)
        assertEquals(1, repository.requestSummary.value.deniedCount)
        assertEquals(0, repository.requestSummary.value.withdrawnCount)
        assertEquals(4, repository.requestSummary.value.totalCount)
    }
}

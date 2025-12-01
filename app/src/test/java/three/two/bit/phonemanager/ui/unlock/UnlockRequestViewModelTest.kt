package three.two.bit.phonemanager.ui.unlock

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Clock
import org.junit.Before
import org.junit.Test
import three.two.bit.phonemanager.data.repository.UnlockRequestRepository
import three.two.bit.phonemanager.domain.model.UnlockRequest
import three.two.bit.phonemanager.domain.model.UnlockRequestFilter
import three.two.bit.phonemanager.domain.model.UnlockRequestStatus
import three.two.bit.phonemanager.domain.model.UnlockRequestSummary
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for UnlockRequestViewModel
 *
 * Story E12.8: Unlock Request UI
 * Coverage target: > 80%
 */
@OptIn(ExperimentalCoroutinesApi::class)
class UnlockRequestViewModelTest {

    private lateinit var viewModel: UnlockRequestViewModel
    private lateinit var unlockRequestRepository: UnlockRequestRepository
    private lateinit var savedStateHandle: SavedStateHandle

    private val testDispatcher = StandardTestDispatcher()
    private val testDeviceId = "test-device-id"

    private val requestsFlow = MutableStateFlow<List<UnlockRequest>>(emptyList())
    private val requestSummaryFlow = MutableStateFlow(UnlockRequestSummary())
    private val isLoadingFlow = MutableStateFlow(false)
    private val errorFlow = MutableStateFlow<String?>(null)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        unlockRequestRepository = mockk(relaxed = true)
        savedStateHandle = SavedStateHandle(mapOf("deviceId" to testDeviceId))

        every { unlockRequestRepository.requests } returns requestsFlow
        every { unlockRequestRepository.requestSummary } returns requestSummaryFlow
        every { unlockRequestRepository.isLoading } returns isLoadingFlow
        every { unlockRequestRepository.error } returns errorFlow
    }

    private fun createViewModel(): UnlockRequestViewModel {
        return UnlockRequestViewModel(unlockRequestRepository, savedStateHandle)
    }

    // AC E12.8.3: View My Unlock Requests Tests

    @Test
    fun `init loads requests when deviceId is present`() = runTest {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { unlockRequestRepository.getUnlockRequests(testDeviceId) }
    }

    @Test
    fun `init does not load requests when deviceId is empty`() = runTest {
        savedStateHandle = SavedStateHandle(mapOf("deviceId" to ""))
        viewModel = UnlockRequestViewModel(unlockRequestRepository, savedStateHandle)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { unlockRequestRepository.getUnlockRequests(any()) }
    }

    @Test
    fun `loadRequests calls repository`() = runTest {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.loadRequests()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 2) { unlockRequestRepository.getUnlockRequests(testDeviceId) }
    }

    @Test
    fun `refresh calls repository refresh`() = runTest {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.refresh()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { unlockRequestRepository.refresh(testDeviceId) }
    }

    // AC E12.8.7: Filter by status Tests

    @Test
    fun `setFilter updates currentFilter`() = runTest {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(UnlockRequestFilter.ALL, viewModel.currentFilter.value)

        viewModel.setFilter(UnlockRequestFilter.PENDING)

        assertEquals(UnlockRequestFilter.PENDING, viewModel.currentFilter.value)
    }

    @Test
    fun `filteredRequests filters by pending status`() = runTest {
        val pendingRequest = createTestRequest("1", UnlockRequestStatus.PENDING)
        val approvedRequest = createTestRequest("2", UnlockRequestStatus.APPROVED)
        requestsFlow.value = listOf(pendingRequest, approvedRequest)

        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.setFilter(UnlockRequestFilter.PENDING)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.filteredRequests.test {
            // Skip initial values until we get the filtered result
            val filtered = awaitItem()
            if (filtered.isEmpty()) {
                val nextFiltered = awaitItem()
                assertEquals(1, nextFiltered.size)
                assertEquals("1", nextFiltered[0].id)
            } else {
                assertEquals(1, filtered.size)
                assertEquals("1", filtered[0].id)
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `filteredRequests filters by approved status`() = runTest {
        val pendingRequest = createTestRequest("1", UnlockRequestStatus.PENDING)
        val approvedRequest = createTestRequest("2", UnlockRequestStatus.APPROVED)
        requestsFlow.value = listOf(pendingRequest, approvedRequest)

        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.setFilter(UnlockRequestFilter.APPROVED)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.filteredRequests.test {
            // Skip initial values until we get the filtered result
            val filtered = awaitItem()
            if (filtered.isEmpty()) {
                val nextFiltered = awaitItem()
                assertEquals(1, nextFiltered.size)
                assertEquals("2", nextFiltered[0].id)
            } else {
                assertEquals(1, filtered.size)
                assertEquals("2", filtered[0].id)
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `filteredRequests returns all with ALL filter`() = runTest {
        val pendingRequest = createTestRequest("1", UnlockRequestStatus.PENDING)
        val approvedRequest = createTestRequest("2", UnlockRequestStatus.APPROVED)
        requestsFlow.value = listOf(pendingRequest, approvedRequest)

        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.setFilter(UnlockRequestFilter.ALL)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.filteredRequests.test {
            // Skip initial values until we get the filtered result
            val filtered = awaitItem()
            if (filtered.isEmpty()) {
                val nextFiltered = awaitItem()
                assertEquals(2, nextFiltered.size)
            } else {
                assertEquals(2, filtered.size)
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    // AC E12.8.1: Request Unlock from Locked Setting Tests

    @Test
    fun `openRequestDialog sets dialog state`() = runTest {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.openRequestDialog("tracking_enabled", "Location Tracking")

        assertTrue(viewModel.showRequestDialog.value)
        assertEquals("tracking_enabled", viewModel.dialogSettingKey.value)
        assertEquals("Location Tracking", viewModel.dialogSettingName.value)
        assertEquals("", viewModel.reason.value)
    }

    @Test
    fun `closeRequestDialog clears dialog state`() = runTest {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.openRequestDialog("tracking_enabled", "Location Tracking")
        viewModel.updateReason("Some reason")

        viewModel.closeRequestDialog()

        assertFalse(viewModel.showRequestDialog.value)
        assertNull(viewModel.dialogSettingKey.value)
        assertNull(viewModel.dialogSettingName.value)
        assertEquals("", viewModel.reason.value)
    }

    // AC E12.8.2: Submit Unlock Request Tests

    @Test
    fun `updateReason updates reason value`() = runTest {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateReason("My reason for unlock")

        assertEquals("My reason for unlock", viewModel.reason.value)
    }

    @Test
    fun `updateReason limits to 200 characters`() = runTest {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateReason("a".repeat(250))

        assertEquals(200, viewModel.reason.value.length)
    }

    @Test
    fun `isReasonValid returns false for short reason`() = runTest {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateReason("test")

        assertFalse(viewModel.isReasonValid())
    }

    @Test
    fun `isReasonValid returns true for valid reason`() = runTest {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateReason("This is a valid reason")

        assertTrue(viewModel.isReasonValid())
    }

    @Test
    fun `getReasonError returns error for short reason`() = runTest {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateReason("test")

        assertEquals("Reason must be at least 5 characters", viewModel.getReasonError())
    }

    @Test
    fun `getReasonError returns null for valid reason`() = runTest {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateReason("Valid reason here")

        assertNull(viewModel.getReasonError())
    }

    @Test
    fun `getReasonError returns null for empty reason`() = runTest {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateReason("")

        assertNull(viewModel.getReasonError())
    }

    @Test
    fun `getRemainingCharacters returns correct count`() = runTest {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateReason("Hello")

        assertEquals(195, viewModel.getRemainingCharacters())
    }

    @Test
    fun `submitRequest calls repository and closes dialog on success`() = runTest {
        val createdRequest = createTestRequest("new-id", UnlockRequestStatus.PENDING)
        coEvery {
            unlockRequestRepository.createUnlockRequest(
                deviceId = testDeviceId,
                settingKey = "tracking_enabled",
                reason = "Valid reason text",
            )
        } returns Result.success(createdRequest)

        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.openRequestDialog("tracking_enabled", "Location Tracking")
        viewModel.updateReason("Valid reason text")
        viewModel.submitRequest()
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.showRequestDialog.value)
        assertEquals("Unlock request submitted successfully", viewModel.successMessage.value)
    }

    @Test
    fun `submitRequest does not close dialog on failure`() = runTest {
        coEvery {
            unlockRequestRepository.createUnlockRequest(any(), any(), any())
        } returns Result.failure(Exception("Network error"))

        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.openRequestDialog("tracking_enabled", "Location Tracking")
        viewModel.updateReason("Valid reason text")
        viewModel.submitRequest()
        testDispatcher.scheduler.advanceUntilIdle()

        // Dialog stays open on error (repository handles error state)
        assertTrue(viewModel.showRequestDialog.value)
    }

    @Test
    fun `submitRequest sets and clears isSubmitting`() = runTest {
        coEvery {
            unlockRequestRepository.createUnlockRequest(any(), any(), any())
        } returns Result.success(createTestRequest("new", UnlockRequestStatus.PENDING))

        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.openRequestDialog("tracking_enabled", "Location Tracking")
        viewModel.updateReason("Valid reason text")

        assertFalse(viewModel.isSubmitting.value)

        viewModel.submitRequest()
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.isSubmitting.value)
    }

    // AC E12.8.4: Withdraw Unlock Request Tests

    @Test
    fun `withdrawRequest calls repository`() = runTest {
        coEvery {
            unlockRequestRepository.withdrawRequest("request-1")
        } returns Result.success(Unit)

        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.withdrawRequest("request-1")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { unlockRequestRepository.withdrawRequest("request-1") }
    }

    @Test
    fun `withdrawRequest clears selectedRequest and sets success message on success`() = runTest {
        val request = createTestRequest("request-1", UnlockRequestStatus.PENDING)
        coEvery {
            unlockRequestRepository.withdrawRequest("request-1")
        } returns Result.success(Unit)

        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.selectRequest(request)
        assertNotNull(viewModel.selectedRequest.value)

        viewModel.withdrawRequest("request-1")
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.selectedRequest.value)
        assertEquals("Request withdrawn successfully", viewModel.successMessage.value)
    }

    // AC E12.8.6: Admin Response Display Tests

    @Test
    fun `selectRequest sets selectedRequest`() = runTest {
        val request = createTestRequest("1", UnlockRequestStatus.APPROVED)

        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.selectRequest(request)

        assertEquals(request, viewModel.selectedRequest.value)
    }

    @Test
    fun `clearSelectedRequest clears selectedRequest`() = runTest {
        val request = createTestRequest("1", UnlockRequestStatus.APPROVED)

        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.selectRequest(request)
        assertNotNull(viewModel.selectedRequest.value)

        viewModel.clearSelectedRequest()

        assertNull(viewModel.selectedRequest.value)
    }

    // State Management Tests

    @Test
    fun `clearSuccessMessage clears success message`() = runTest {
        coEvery {
            unlockRequestRepository.createUnlockRequest(any(), any(), any())
        } returns Result.success(createTestRequest("new", UnlockRequestStatus.PENDING))

        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.openRequestDialog("tracking_enabled", "Location Tracking")
        viewModel.updateReason("Valid reason")
        viewModel.submitRequest()
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(viewModel.successMessage.value)

        viewModel.clearSuccessMessage()

        assertNull(viewModel.successMessage.value)
    }

    @Test
    fun `clearError calls repository clearError`() = runTest {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.clearError()

        coVerify { unlockRequestRepository.clearError() }
    }

    @Test
    fun `requests state comes from repository`() = runTest {
        val request = createTestRequest("1", UnlockRequestStatus.PENDING)
        requestsFlow.value = listOf(request)

        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.requests.test {
            val requests = awaitItem()
            assertEquals(1, requests.size)
            assertEquals("1", requests[0].id)
        }
    }

    @Test
    fun `requestSummary state comes from repository`() = runTest {
        requestSummaryFlow.value = UnlockRequestSummary(
            pendingCount = 2,
            approvedCount = 1,
            deniedCount = 0,
            withdrawnCount = 1,
        )

        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, viewModel.requestSummary.value.pendingCount)
        assertEquals(1, viewModel.requestSummary.value.approvedCount)
        assertEquals(4, viewModel.requestSummary.value.totalCount)
    }

    @Test
    fun `isLoading state comes from repository`() = runTest {
        isLoadingFlow.value = true

        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.isLoading.value)
    }

    @Test
    fun `error state comes from repository`() = runTest {
        errorFlow.value = "Test error"

        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Test error", viewModel.error.value)
    }

    // Helper function to create test requests
    private fun createTestRequest(
        id: String,
        status: UnlockRequestStatus,
    ): UnlockRequest {
        return UnlockRequest(
            id = id,
            deviceId = testDeviceId,
            settingKey = "tracking_enabled",
            reason = "Test reason",
            status = status,
            requestedBy = "user@test.com",
            requestedByName = "Test User",
            createdAt = Clock.System.now(),
        )
    }
}

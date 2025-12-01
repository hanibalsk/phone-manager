package three.two.bit.phonemanager.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Instant
import three.two.bit.phonemanager.domain.model.UnlockRequest
import three.two.bit.phonemanager.domain.model.UnlockRequestFilter
import three.two.bit.phonemanager.domain.model.UnlockRequestStatus
import three.two.bit.phonemanager.domain.model.UnlockRequestSummary
import three.two.bit.phonemanager.network.DeviceApiService
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Story E12.8: Unlock Request Repository
 *
 * Repository for managing unlock requests.
 * Handles creation, listing, and withdrawal of unlock requests.
 *
 * AC E12.8.2: Submit Unlock Request
 * AC E12.8.3: View My Unlock Requests
 * AC E12.8.4: Withdraw Unlock Request
 * AC E12.8.8: Setting Auto-Unlock on Approval
 */
interface UnlockRequestRepository {
    /** All unlock requests for the current device */
    val requests: StateFlow<List<UnlockRequest>>

    /** Summary of request counts by status */
    val requestSummary: StateFlow<UnlockRequestSummary>

    /** Loading state */
    val isLoading: StateFlow<Boolean>

    /** Error state */
    val error: StateFlow<String?>

    /**
     * Create a new unlock request.
     * AC E12.8.2: Submit Unlock Request
     *
     * @param deviceId The device's UUID
     * @param settingKey The setting key to request unlock for
     * @param reason User-provided reason (max 200 characters)
     * @return Result with created request on success
     */
    suspend fun createUnlockRequest(
        deviceId: String,
        settingKey: String,
        reason: String,
    ): Result<UnlockRequest>

    /**
     * Get all unlock requests for a device.
     * AC E12.8.3: View My Unlock Requests
     *
     * @param deviceId The device's UUID
     * @param filter Optional filter by status
     * @return Result with list of requests on success
     */
    suspend fun getUnlockRequests(
        deviceId: String,
        filter: UnlockRequestFilter = UnlockRequestFilter.ALL,
    ): Result<List<UnlockRequest>>

    /**
     * Withdraw a pending unlock request.
     * AC E12.8.4: Withdraw Unlock Request
     *
     * @param requestId The request's UUID
     * @return Result with success/failure
     */
    suspend fun withdrawRequest(requestId: String): Result<Unit>

    /**
     * Update local request state when FCM notification received.
     * AC E12.8.5: Request Status Notifications
     * AC E12.8.8: Setting Auto-Unlock on Approval
     *
     * @param requestId The request ID
     * @param status New status
     * @param adminName Admin who responded
     * @param responseMessage Admin's response message
     * @param respondedAt When the decision was made
     */
    fun updateRequestStatus(
        requestId: String,
        status: UnlockRequestStatus,
        adminName: String?,
        responseMessage: String?,
        respondedAt: Instant?,
    )

    /**
     * Get a single request by ID.
     */
    fun getRequestById(requestId: String): UnlockRequest?

    /**
     * Clear error state.
     */
    fun clearError()

    /**
     * Refresh requests from server.
     */
    suspend fun refresh(deviceId: String)
}

@Singleton
class UnlockRequestRepositoryImpl @Inject constructor(
    private val deviceApiService: DeviceApiService,
    private val authRepository: AuthRepository,
) : UnlockRequestRepository {

    private val _requests = MutableStateFlow<List<UnlockRequest>>(emptyList())
    override val requests: StateFlow<List<UnlockRequest>> = _requests.asStateFlow()

    private val _requestSummary = MutableStateFlow(UnlockRequestSummary())
    override val requestSummary: StateFlow<UnlockRequestSummary> = _requestSummary.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    override val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    override val error: StateFlow<String?> = _error.asStateFlow()

    override suspend fun createUnlockRequest(
        deviceId: String,
        settingKey: String,
        reason: String,
    ): Result<UnlockRequest> {
        // Validate reason length
        if (reason.isBlank()) {
            return Result.failure(IllegalArgumentException("Reason cannot be empty"))
        }
        if (reason.length < 5) {
            return Result.failure(IllegalArgumentException("Reason must be at least 5 characters"))
        }
        if (reason.length > 200) {
            return Result.failure(IllegalArgumentException("Reason cannot exceed 200 characters"))
        }

        val accessToken = authRepository.getAccessToken()
            ?: return Result.failure(IllegalStateException("Not authenticated"))

        _isLoading.value = true
        _error.value = null

        return try {
            val result = deviceApiService.createUnlockRequest(
                deviceId = deviceId,
                settingKey = settingKey,
                reason = reason,
                accessToken = accessToken,
            )

            result.fold(
                onSuccess = { response ->
                    val request = UnlockRequest(
                        id = response.id,
                        deviceId = response.deviceId,
                        settingKey = response.settingKey,
                        reason = response.reason,
                        status = UnlockRequestStatus.fromString(response.status),
                        requestedBy = response.requestedBy,
                        requestedByName = response.requestedByName,
                        createdAt = parseInstant(response.createdAt),
                    )

                    // Add to local list
                    _requests.value = listOf(request) + _requests.value
                    updateSummary()

                    Timber.i("Created unlock request ${request.id} for setting $settingKey")
                    _isLoading.value = false
                    Result.success(request)
                },
                onFailure = { error ->
                    _error.value = error.message
                    Timber.e(error, "Failed to create unlock request")
                    _isLoading.value = false
                    Result.failure(error)
                },
            )
        } catch (e: Exception) {
            _error.value = e.message
            Timber.e(e, "Failed to create unlock request")
            _isLoading.value = false
            Result.failure(e)
        }
    }

    override suspend fun getUnlockRequests(
        deviceId: String,
        filter: UnlockRequestFilter,
    ): Result<List<UnlockRequest>> {
        val accessToken = authRepository.getAccessToken()
            ?: return Result.failure(IllegalStateException("Not authenticated"))

        _isLoading.value = true
        _error.value = null

        return try {
            val statusFilter = when (filter) {
                UnlockRequestFilter.ALL -> null
                UnlockRequestFilter.PENDING -> "pending"
                UnlockRequestFilter.APPROVED -> "approved"
                UnlockRequestFilter.DENIED -> "denied"
                UnlockRequestFilter.WITHDRAWN -> "withdrawn"
            }

            val result = deviceApiService.getUnlockRequests(
                deviceId = deviceId,
                status = statusFilter,
                accessToken = accessToken,
            )

            result.fold(
                onSuccess = { response ->
                    val requests = response.requests.map { apiRequest ->
                        UnlockRequest(
                            id = apiRequest.id,
                            deviceId = apiRequest.deviceId,
                            settingKey = apiRequest.settingKey,
                            reason = apiRequest.reason,
                            status = UnlockRequestStatus.fromString(apiRequest.status),
                            requestedBy = apiRequest.requestedBy,
                            requestedByName = apiRequest.requestedByName,
                            createdAt = parseInstant(apiRequest.createdAt),
                            respondedBy = apiRequest.respondedBy,
                            respondedByName = apiRequest.respondedByName,
                            response = apiRequest.response,
                            respondedAt = apiRequest.respondedAt?.let { parseInstant(it) },
                        )
                    }.sortedByDescending { it.createdAt }

                    _requests.value = requests
                    updateSummary()

                    Timber.i("Fetched ${requests.size} unlock requests for device $deviceId")
                    _isLoading.value = false
                    Result.success(requests)
                },
                onFailure = { error ->
                    _error.value = error.message
                    Timber.e(error, "Failed to fetch unlock requests")
                    _isLoading.value = false
                    Result.failure(error)
                },
            )
        } catch (e: Exception) {
            _error.value = e.message
            Timber.e(e, "Failed to fetch unlock requests")
            _isLoading.value = false
            Result.failure(e)
        }
    }

    override suspend fun withdrawRequest(requestId: String): Result<Unit> {
        val accessToken = authRepository.getAccessToken()
            ?: return Result.failure(IllegalStateException("Not authenticated"))

        // Check if request is pending
        val request = _requests.value.find { it.id == requestId }
        if (request != null && !request.canWithdraw()) {
            return Result.failure(IllegalStateException("Request cannot be withdrawn"))
        }

        _isLoading.value = true
        _error.value = null

        return try {
            val result = deviceApiService.withdrawUnlockRequest(
                requestId = requestId,
                accessToken = accessToken,
            )

            result.fold(
                onSuccess = { response ->
                    if (response.success) {
                        // Remove from local list
                        _requests.value = _requests.value.filter { it.id != requestId }
                        updateSummary()

                        Timber.i("Withdrew unlock request $requestId")
                        _isLoading.value = false
                        Result.success(Unit)
                    } else {
                        _error.value = response.error ?: "Failed to withdraw request"
                        _isLoading.value = false
                        Result.failure(Exception(response.error ?: "Failed to withdraw request"))
                    }
                },
                onFailure = { error ->
                    _error.value = error.message
                    Timber.e(error, "Failed to withdraw unlock request")
                    _isLoading.value = false
                    Result.failure(error)
                },
            )
        } catch (e: Exception) {
            _error.value = e.message
            Timber.e(e, "Failed to withdraw unlock request")
            _isLoading.value = false
            Result.failure(e)
        }
    }

    override fun updateRequestStatus(
        requestId: String,
        status: UnlockRequestStatus,
        adminName: String?,
        responseMessage: String?,
        respondedAt: Instant?,
    ) {
        _requests.value = _requests.value.map { request ->
            if (request.id == requestId) {
                request.copy(
                    status = status,
                    respondedByName = adminName,
                    response = responseMessage,
                    respondedAt = respondedAt,
                )
            } else {
                request
            }
        }
        updateSummary()
        Timber.i("Updated unlock request $requestId status to $status")
    }

    override fun getRequestById(requestId: String): UnlockRequest? {
        return _requests.value.find { it.id == requestId }
    }

    override fun clearError() {
        _error.value = null
    }

    override suspend fun refresh(deviceId: String) {
        getUnlockRequests(deviceId)
    }

    private fun updateSummary() {
        val allRequests = _requests.value
        _requestSummary.value = UnlockRequestSummary(
            pendingCount = allRequests.count { it.status == UnlockRequestStatus.PENDING },
            approvedCount = allRequests.count { it.status == UnlockRequestStatus.APPROVED },
            deniedCount = allRequests.count { it.status == UnlockRequestStatus.DENIED },
            withdrawnCount = allRequests.count { it.status == UnlockRequestStatus.WITHDRAWN },
        )
    }

    private fun parseInstant(isoString: String): Instant {
        return try {
            Instant.parse(isoString)
        } catch (e: Exception) {
            Timber.w(e, "Failed to parse instant: $isoString")
            kotlinx.datetime.Clock.System.now()
        }
    }
}

package three.two.bit.phonemanager.ui.unlock

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import three.two.bit.phonemanager.data.repository.UnlockRequestRepository
import three.two.bit.phonemanager.domain.model.UnlockRequest
import three.two.bit.phonemanager.domain.model.UnlockRequestFilter
import three.two.bit.phonemanager.domain.model.UnlockRequestSummary
import timber.log.Timber
import javax.inject.Inject

/**
 * Story E12.8: Unlock Request ViewModel
 *
 * Manages UI state for unlock request screens.
 *
 * AC E12.8.3: View My Unlock Requests
 * AC E12.8.4: Withdraw Unlock Request
 * AC E12.8.7: Filter by status
 */
@HiltViewModel
class UnlockRequestViewModel @Inject constructor(
    private val unlockRequestRepository: UnlockRequestRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    /** Device ID from navigation arguments */
    private val deviceId: String = savedStateHandle.get<String>("deviceId") ?: ""

    /** Current filter selection */
    private val _currentFilter = MutableStateFlow(UnlockRequestFilter.ALL)
    val currentFilter: StateFlow<UnlockRequestFilter> = _currentFilter.asStateFlow()

    /** Dialog state for request unlock */
    private val _showRequestDialog = MutableStateFlow(false)
    val showRequestDialog: StateFlow<Boolean> = _showRequestDialog.asStateFlow()

    /** Setting key for current unlock request dialog */
    private val _dialogSettingKey = MutableStateFlow<String?>(null)
    val dialogSettingKey: StateFlow<String?> = _dialogSettingKey.asStateFlow()

    /** Setting display name for current unlock request dialog */
    private val _dialogSettingName = MutableStateFlow<String?>(null)
    val dialogSettingName: StateFlow<String?> = _dialogSettingName.asStateFlow()

    /** Reason input for unlock request */
    private val _reason = MutableStateFlow("")
    val reason: StateFlow<String> = _reason.asStateFlow()

    /** Request creation in progress */
    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    /** Request to show details for */
    private val _selectedRequest = MutableStateFlow<UnlockRequest?>(null)
    val selectedRequest: StateFlow<UnlockRequest?> = _selectedRequest.asStateFlow()

    /** Success message to show */
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    /** All requests from repository */
    val requests: StateFlow<List<UnlockRequest>> = unlockRequestRepository.requests

    /** Request summary from repository */
    val requestSummary: StateFlow<UnlockRequestSummary> = unlockRequestRepository.requestSummary

    /** Loading state from repository */
    val isLoading: StateFlow<Boolean> = unlockRequestRepository.isLoading

    /** Error state from repository */
    val error: StateFlow<String?> = unlockRequestRepository.error

    /** Filtered requests based on current filter */
    val filteredRequests: StateFlow<List<UnlockRequest>> = combine(
        requests,
        currentFilter,
    ) { allRequests, filter ->
        when (filter) {
            UnlockRequestFilter.ALL -> allRequests
            UnlockRequestFilter.PENDING -> allRequests.filter { it.isPending() }
            UnlockRequestFilter.APPROVED -> allRequests.filter { it.isApproved() }
            UnlockRequestFilter.DENIED -> allRequests.filter { it.isDenied() }
            UnlockRequestFilter.WITHDRAWN -> allRequests.filter { it.status == three.two.bit.phonemanager.domain.model.UnlockRequestStatus.WITHDRAWN }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList(),
    )

    init {
        if (deviceId.isNotEmpty()) {
            loadRequests()
        }
    }

    /**
     * Load unlock requests for the device.
     * AC E12.8.3: View My Unlock Requests
     */
    fun loadRequests() {
        if (deviceId.isEmpty()) {
            Timber.w("Cannot load requests: deviceId is empty")
            return
        }

        viewModelScope.launch {
            unlockRequestRepository.getUnlockRequests(deviceId)
        }
    }

    /**
     * Refresh requests from server.
     */
    fun refresh() {
        if (deviceId.isEmpty()) return

        viewModelScope.launch {
            unlockRequestRepository.refresh(deviceId)
        }
    }

    /**
     * Set the current filter.
     * AC E12.8.7: Filter by status
     */
    fun setFilter(filter: UnlockRequestFilter) {
        _currentFilter.value = filter
    }

    /**
     * Open the request unlock dialog for a setting.
     * AC E12.8.1: Request Unlock from Locked Setting
     */
    fun openRequestDialog(settingKey: String, settingName: String) {
        _dialogSettingKey.value = settingKey
        _dialogSettingName.value = settingName
        _reason.value = ""
        _showRequestDialog.value = true
    }

    /**
     * Close the request unlock dialog.
     */
    fun closeRequestDialog() {
        _showRequestDialog.value = false
        _dialogSettingKey.value = null
        _dialogSettingName.value = null
        _reason.value = ""
    }

    /**
     * Update the reason text.
     */
    fun updateReason(newReason: String) {
        // Limit to 200 characters as per AC E12.8.2
        _reason.value = newReason.take(200)
    }

    /**
     * Submit the unlock request.
     * AC E12.8.2: Submit Unlock Request
     */
    fun submitRequest() {
        val settingKey = _dialogSettingKey.value ?: return
        val reasonText = _reason.value

        if (deviceId.isEmpty()) {
            Timber.w("Cannot submit request: deviceId is empty")
            return
        }

        viewModelScope.launch {
            _isSubmitting.value = true

            val result = unlockRequestRepository.createUnlockRequest(
                deviceId = deviceId,
                settingKey = settingKey,
                reason = reasonText,
            )

            _isSubmitting.value = false

            result.fold(
                onSuccess = { request ->
                    Timber.i("Created unlock request ${request.id}")
                    _successMessage.value = "Unlock request submitted successfully"
                    closeRequestDialog()
                },
                onFailure = { error ->
                    Timber.e(error, "Failed to create unlock request")
                    // Error is handled by repository
                },
            )
        }
    }

    /**
     * Withdraw a pending unlock request.
     * AC E12.8.4: Withdraw Unlock Request
     */
    fun withdrawRequest(requestId: String) {
        viewModelScope.launch {
            val result = unlockRequestRepository.withdrawRequest(requestId)

            result.fold(
                onSuccess = {
                    Timber.i("Withdrew unlock request $requestId")
                    _successMessage.value = "Request withdrawn successfully"
                    _selectedRequest.value = null
                },
                onFailure = { error ->
                    Timber.e(error, "Failed to withdraw unlock request")
                },
            )
        }
    }

    /**
     * Select a request to view details.
     * AC E12.8.6: Admin Response Display
     */
    fun selectRequest(request: UnlockRequest) {
        _selectedRequest.value = request
    }

    /**
     * Clear selected request.
     */
    fun clearSelectedRequest() {
        _selectedRequest.value = null
    }

    /**
     * Clear success message.
     */
    fun clearSuccessMessage() {
        _successMessage.value = null
    }

    /**
     * Clear error state.
     */
    fun clearError() {
        unlockRequestRepository.clearError()
    }

    /**
     * Check if reason is valid for submission.
     * AC E12.8.2: Reason 5-200 characters
     */
    fun isReasonValid(): Boolean {
        val reasonText = _reason.value
        return reasonText.length in 5..200
    }

    /**
     * Get reason validation error message.
     */
    fun getReasonError(): String? {
        val reasonText = _reason.value
        return when {
            reasonText.isEmpty() -> null // Don't show error for empty
            reasonText.length < 5 -> "Reason must be at least 5 characters"
            reasonText.length > 200 -> "Reason cannot exceed 200 characters"
            else -> null
        }
    }

    /**
     * Get remaining characters for reason.
     */
    fun getRemainingCharacters(): Int {
        return 200 - _reason.value.length
    }
}

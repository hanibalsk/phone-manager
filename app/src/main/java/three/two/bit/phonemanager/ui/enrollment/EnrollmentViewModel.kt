package three.two.bit.phonemanager.ui.enrollment

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import three.two.bit.phonemanager.data.repository.EnrollmentRepository
import three.two.bit.phonemanager.domain.model.DevicePolicy
import three.two.bit.phonemanager.domain.model.EnrollmentResult
import three.two.bit.phonemanager.domain.model.EnrollmentStatus
import three.two.bit.phonemanager.domain.model.EnrollmentToken
import three.two.bit.phonemanager.domain.model.OrganizationInfo
import timber.log.Timber
import javax.inject.Inject

/**
 * Story E13.10: Android Enrollment Flow - ViewModel
 *
 * Manages enrollment UI state and user interactions.
 *
 * AC E13.10.2: Enrollment code input
 * AC E13.10.3: QR code scanning
 * AC E13.10.4: Enroll device
 * AC E13.10.5: Apply device policies
 * AC E13.10.6: Enrollment success screen
 * AC E13.10.7: Error handling
 */
@HiltViewModel
class EnrollmentViewModel @Inject constructor(
    private val enrollmentRepository: EnrollmentRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    // Enrollment code input
    private val _enrollmentCode = MutableStateFlow(savedStateHandle.get<String>("enrollmentCode") ?: "")
    val enrollmentCode: StateFlow<String> = _enrollmentCode.asStateFlow()

    // UI state
    private val _uiState = MutableStateFlow<EnrollmentUiState>(EnrollmentUiState.Idle)
    val uiState: StateFlow<EnrollmentUiState> = _uiState.asStateFlow()

    // Enrollment status from repository
    val enrollmentStatus: StateFlow<EnrollmentStatus> = enrollmentRepository.enrollmentStatus

    // Loading state from repository
    val isLoading: StateFlow<Boolean> = enrollmentRepository.isLoading

    // Error state from repository
    val error: StateFlow<String?> = enrollmentRepository.error

    // Organization info from repository (for success screen)
    val organizationInfo: StateFlow<OrganizationInfo?> = enrollmentRepository.organizationInfo

    // Device policy from repository (for success screen summary)
    val devicePolicy: StateFlow<DevicePolicy?> = enrollmentRepository.devicePolicy

    // Code validation error
    private val _codeError = MutableStateFlow<String?>(null)
    val codeError: StateFlow<String?> = _codeError.asStateFlow()

    // QR scanner visibility
    private val _showQrScanner = MutableStateFlow(false)
    val showQrScanner: StateFlow<Boolean> = _showQrScanner.asStateFlow()

    init {
        // Check if we have a pre-filled token from deep link
        savedStateHandle.get<String>("token")?.let { token ->
            if (token.isNotBlank()) {
                _enrollmentCode.value = token
                // Auto-enroll if token was provided via deep link
                if (savedStateHandle.get<Boolean>("autoEnroll") == true) {
                    enrollDevice()
                }
            }
        }
    }

    /**
     * Update the enrollment code.
     * AC E13.10.2: Enrollment code input field.
     */
    fun updateEnrollmentCode(code: String) {
        // Remove any whitespace and limit to max length
        val cleanedCode = code.replace("\\s".toRegex(), "")
            .take(EnrollmentToken.MAX_LENGTH)

        _enrollmentCode.value = cleanedCode
        savedStateHandle["enrollmentCode"] = cleanedCode
        _codeError.value = null
    }

    /**
     * Validate the enrollment code format.
     * AC E13.10.2: Alphanumeric, 16-20 chars.
     */
    fun validateCode(code: String = _enrollmentCode.value): Boolean {
        val token = EnrollmentToken(code)
        if (!token.isValid) {
            _codeError.value = when {
                code.isBlank() -> null // Don't show error for empty input
                code.length < EnrollmentToken.MIN_LENGTH ->
                    "Code must be at least ${EnrollmentToken.MIN_LENGTH} characters"
                code.length > EnrollmentToken.MAX_LENGTH ->
                    "Code must be at most ${EnrollmentToken.MAX_LENGTH} characters"
                !code.all { it.isLetterOrDigit() } ->
                    "Code can only contain letters and numbers"
                else -> "Invalid code format"
            }
            return false
        }
        _codeError.value = null
        return true
    }

    /**
     * Check if the current code is valid for submission.
     */
    fun isCodeValid(): Boolean = EnrollmentToken(_enrollmentCode.value).isValid

    /**
     * Parse enrollment token from QR code data.
     * AC E13.10.3: Detect and parse enrollment QR code.
     *
     * @param data Raw QR code data
     * @return true if valid enrollment QR code
     */
    fun parseQRCode(data: String): Boolean {
        val token = EnrollmentToken.fromQRCode(data)
        return if (token != null && token.isValid) {
            _enrollmentCode.value = token.token
            savedStateHandle["enrollmentCode"] = token.token
            _codeError.value = null
            _showQrScanner.value = false
            Timber.i("Parsed enrollment token from QR code")
            true
        } else {
            Timber.w("Invalid QR code data: $data")
            false
        }
    }

    /**
     * Enroll the device with the current code.
     * AC E13.10.4: Call POST /enroll endpoint.
     */
    fun enrollDevice() {
        val code = _enrollmentCode.value

        if (!validateCode(code)) {
            return
        }

        _uiState.value = EnrollmentUiState.Loading

        viewModelScope.launch {
            val token = EnrollmentToken(code)
            val result = enrollmentRepository.enrollDevice(token)

            result.fold(
                onSuccess = { enrollmentResult ->
                    Timber.i("Enrollment successful: ${enrollmentResult.organization.name}")
                    _uiState.value = EnrollmentUiState.Success(enrollmentResult)
                },
                onFailure = { e ->
                    Timber.e(e, "Enrollment failed")
                    _uiState.value = EnrollmentUiState.Error(
                        enrollmentRepository.error.value ?: "Enrollment failed",
                    )
                },
            )
        }
    }

    /**
     * Show the QR scanner.
     * AC E13.10.3: Open camera viewfinder.
     */
    fun showQrScanner() {
        _showQrScanner.value = true
    }

    /**
     * Hide the QR scanner.
     */
    fun hideQrScanner() {
        _showQrScanner.value = false
    }

    /**
     * Clear error state.
     */
    fun clearError() {
        _codeError.value = null
        enrollmentRepository.clearError()
        if (_uiState.value is EnrollmentUiState.Error) {
            _uiState.value = EnrollmentUiState.Idle
        }
    }

    /**
     * Reset to idle state (e.g., after navigating away from success).
     */
    fun resetState() {
        _uiState.value = EnrollmentUiState.Idle
        _enrollmentCode.value = ""
        savedStateHandle["enrollmentCode"] = ""
    }

    /**
     * Get the number of locked settings for display.
     * AC E13.10.6: Show locked settings count.
     */
    fun getLockedSettingsCount(): Int = devicePolicy.value?.lockedCount() ?: 0
}

/**
 * UI state for enrollment screen.
 */
sealed class EnrollmentUiState {
    /** Initial idle state */
    data object Idle : EnrollmentUiState()

    /** Enrollment in progress */
    data object Loading : EnrollmentUiState()

    /** Enrollment succeeded */
    data class Success(val result: EnrollmentResult) : EnrollmentUiState()

    /** Enrollment failed */
    data class Error(val message: String) : EnrollmentUiState()
}

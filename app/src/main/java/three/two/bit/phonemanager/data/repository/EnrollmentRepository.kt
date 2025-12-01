package three.two.bit.phonemanager.data.repository

import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import three.two.bit.phonemanager.BuildConfig
import three.two.bit.phonemanager.domain.model.DeviceEnrollmentInfo
import three.two.bit.phonemanager.domain.model.DevicePolicy
import three.two.bit.phonemanager.domain.model.EnrollmentResult
import three.two.bit.phonemanager.domain.model.EnrollmentStatus
import three.two.bit.phonemanager.domain.model.EnrollmentToken
import three.two.bit.phonemanager.domain.model.OrganizationInfo
import three.two.bit.phonemanager.domain.policy.PolicyApplicator
import three.two.bit.phonemanager.network.EnrollmentApiService
import three.two.bit.phonemanager.security.SecureStorage
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Story E13.10: Android Enrollment Flow - Repository
 *
 * Handles device enrollment, unenrollment, and policy management.
 *
 * AC E13.10.4: Enroll device with token
 * AC E13.10.5: Apply device policies
 * AC E13.10.8: Check enrollment status
 * AC E13.10.9: Unenroll device
 */
@Singleton
class EnrollmentRepository @Inject constructor(
    private val enrollmentApiService: EnrollmentApiService,
    private val secureStorage: SecureStorage,
    private val policyApplicator: PolicyApplicator,
) {

    private val _enrollmentStatus = MutableStateFlow(EnrollmentStatus.NOT_ENROLLED)
    val enrollmentStatus: StateFlow<EnrollmentStatus> = _enrollmentStatus.asStateFlow()

    private val _organizationInfo = MutableStateFlow<OrganizationInfo?>(null)
    val organizationInfo: StateFlow<OrganizationInfo?> = _organizationInfo.asStateFlow()

    private val _devicePolicy = MutableStateFlow<DevicePolicy?>(null)
    val devicePolicy: StateFlow<DevicePolicy?> = _devicePolicy.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        // Load enrollment status from secure storage on initialization
        loadEnrollmentState()
    }

    /**
     * Load enrollment state from secure storage.
     */
    private fun loadEnrollmentState() {
        val status = secureStorage.getEnrollmentStatus()
        _enrollmentStatus.value = status

        if (status == EnrollmentStatus.ENROLLED) {
            _organizationInfo.value = secureStorage.getOrganizationInfo()
            _devicePolicy.value = secureStorage.getDevicePolicy()
        }
    }

    /**
     * Enroll device with an enrollment token.
     * AC E13.10.4: Call POST /enroll endpoint with code and device info.
     *
     * @param token The enrollment token/code
     * @return Result with EnrollmentResult on success
     */
    suspend fun enrollDevice(token: EnrollmentToken): Result<EnrollmentResult> {
        if (!token.isValid) {
            return Result.failure(IllegalArgumentException("Invalid enrollment token format"))
        }

        _isLoading.value = true
        _error.value = null
        _enrollmentStatus.value = EnrollmentStatus.ENROLLING

        return try {
            val deviceInfo = createDeviceInfo()
            val result = enrollmentApiService.enrollDevice(token.token, deviceInfo)

            result.fold(
                onSuccess = { enrollmentResult ->
                    Timber.i("Enrollment successful for org: ${enrollmentResult.organization.name}")

                    // Save enrollment data to secure storage
                    saveOrganizationInfo(enrollmentResult.organization)
                    saveTokens(enrollmentResult)

                    // Apply policies to device settings (AC E13.10.5)
                    applyPolicies(enrollmentResult.policy)

                    _enrollmentStatus.value = EnrollmentStatus.ENROLLED
                    _organizationInfo.value = enrollmentResult.organization
                    _devicePolicy.value = enrollmentResult.policy
                    _isLoading.value = false

                    Result.success(enrollmentResult)
                },
                onFailure = { e ->
                    Timber.e(e, "Enrollment failed")
                    _enrollmentStatus.value = EnrollmentStatus.NOT_ENROLLED
                    _error.value = getEnrollmentErrorMessage(e)
                    _isLoading.value = false
                    Result.failure(e)
                },
            )
        } catch (e: Exception) {
            Timber.e(e, "Enrollment failed with exception")
            _enrollmentStatus.value = EnrollmentStatus.NOT_ENROLLED
            _error.value = getEnrollmentErrorMessage(e)
            _isLoading.value = false
            Result.failure(e)
        }
    }

    /**
     * Unenroll device from organization.
     * AC E13.10.9: Call POST /devices/{id}/unenroll endpoint.
     *
     * @return Result with Unit on success
     */
    suspend fun unenrollDevice(): Result<Unit> {
        val accessToken = secureStorage.getAccessToken()
            ?: return Result.failure(IllegalStateException("Not authenticated"))
        val deviceId = secureStorage.getDeviceId()

        _isLoading.value = true
        _error.value = null
        _enrollmentStatus.value = EnrollmentStatus.UNENROLLING

        return try {
            val result = enrollmentApiService.unenrollDevice(deviceId, accessToken)

            result.fold(
                onSuccess = {
                    Timber.i("Unenrollment successful")

                    // Clear enrollment data
                    clearEnrollmentData()

                    _enrollmentStatus.value = EnrollmentStatus.NOT_ENROLLED
                    _organizationInfo.value = null
                    _devicePolicy.value = null
                    _isLoading.value = false

                    Result.success(Unit)
                },
                onFailure = { e ->
                    Timber.e(e, "Unenrollment failed")
                    _enrollmentStatus.value = EnrollmentStatus.ENROLLED
                    _error.value = getUnenrollmentErrorMessage(e)
                    _isLoading.value = false
                    Result.failure(e)
                },
            )
        } catch (e: Exception) {
            Timber.e(e, "Unenrollment failed with exception")
            _enrollmentStatus.value = EnrollmentStatus.ENROLLED
            _error.value = getUnenrollmentErrorMessage(e)
            _isLoading.value = false
            Result.failure(e)
        }
    }

    /**
     * Save policies to secure storage.
     * AC E13.10.5: Save policies to local storage.
     */
    fun savePolicies(policy: DevicePolicy) {
        secureStorage.saveDevicePolicy(policy)
        _devicePolicy.value = policy
        Timber.d("Policies saved: ${policy.settings.size} settings, ${policy.locks.size} locks")
    }

    /**
     * Apply policies to device settings.
     * AC E13.10.5: Apply all policy settings immediately.
     *
     * Uses PolicyApplicator to map policy values to local preferences.
     */
    suspend fun applyPolicies(policy: DevicePolicy): Result<Unit> {
        return try {
            // Save policy to storage first
            savePolicies(policy)

            // Apply policy settings to preferences
            val applicationResult = policyApplicator.applyPolicies(policy)

            if (applicationResult.success) {
                Timber.i("Policies applied successfully: ${applicationResult.appliedSettings.size} settings")
                Result.success(Unit)
            } else {
                val failedKeys = applicationResult.failedSettings.map { it.settingKey }
                Timber.w("Some policies failed to apply: $failedKeys")
                Result.success(Unit) // Still success if policy saved
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to apply policies")
            Result.failure(e)
        }
    }

    /**
     * Check if device is enrolled.
     * AC E13.10.8: Managed device indicator.
     */
    fun isEnrolled(): Boolean = _enrollmentStatus.value == EnrollmentStatus.ENROLLED

    /**
     * Get organization info for enrolled device.
     * AC E13.10.8: Show "Managed by {org}" banner.
     */
    fun getOrganizationInfo(): OrganizationInfo? = _organizationInfo.value

    /**
     * Get current device policy.
     */
    fun getDevicePolicy(): DevicePolicy? = _devicePolicy.value

    /**
     * Check if a setting is locked by policy.
     */
    fun isSettingLocked(key: String): Boolean {
        return _devicePolicy.value?.isLocked(key) == true
    }

    /**
     * Get a policy value.
     */
    fun <T> getPolicyValue(key: String, default: T): T {
        return _devicePolicy.value?.getPolicyValue(key, default) ?: default
    }

    /**
     * Clear error state.
     */
    fun clearError() {
        _error.value = null
    }

    /**
     * Create device info for enrollment.
     */
    private fun createDeviceInfo(): DeviceEnrollmentInfo {
        return DeviceEnrollmentInfo(
            deviceId = secureStorage.getDeviceId(),
            manufacturer = Build.MANUFACTURER,
            model = Build.MODEL,
            osVersion = "Android ${Build.VERSION.RELEASE}",
            appVersion = BuildConfig.VERSION_NAME,
        )
    }

    /**
     * Save organization info to secure storage.
     */
    private fun saveOrganizationInfo(org: OrganizationInfo) {
        secureStorage.saveOrganizationInfo(org)
        Timber.d("Organization info saved: ${org.name}")
    }

    /**
     * Save authentication tokens from enrollment.
     */
    private fun saveTokens(result: EnrollmentResult) {
        secureStorage.saveAccessToken(result.accessToken)
        secureStorage.saveRefreshToken(result.refreshToken)
        // Set token expiry to 1 hour from now (typical JWT expiry)
        secureStorage.saveTokenExpiryTime(System.currentTimeMillis() + 3600 * 1000)
        Timber.d("Tokens saved from enrollment")
    }

    /**
     * Clear all enrollment data.
     */
    private fun clearEnrollmentData() {
        secureStorage.clearEnrollmentData()
        secureStorage.clearTokens()
        Timber.d("Enrollment data cleared")
    }

    /**
     * Get user-friendly error message for enrollment failures.
     * AC E13.10.7: Enrollment error handling.
     */
    private fun getEnrollmentErrorMessage(e: Throwable): String {
        val message = e.message ?: ""
        return when {
            message.contains("invalid", ignoreCase = true) ||
                message.contains("not found", ignoreCase = true) ||
                message.contains("expired", ignoreCase = true) ->
                "Enrollment code not found or expired"

            message.contains("already", ignoreCase = true) ||
                message.contains("409") ||
                message.contains("conflict", ignoreCase = true) ->
                "Device already enrolled"

            message.contains("network", ignoreCase = true) ||
                message.contains("connection", ignoreCase = true) ||
                message.contains("timeout", ignoreCase = true) ->
                "Cannot connect to enrollment server"

            message.contains("policy", ignoreCase = true) ->
                "Failed to apply policies, contact IT"

            else -> "Enrollment failed. Please try again or contact IT."
        }
    }

    /**
     * Get user-friendly error message for unenrollment failures.
     */
    private fun getUnenrollmentErrorMessage(e: Throwable): String {
        val message = e.message ?: ""
        return when {
            message.contains("403") ||
                message.contains("forbidden", ignoreCase = true) ||
                message.contains("not allowed", ignoreCase = true) ->
                "Unenrollment not allowed by company policy"

            message.contains("network", ignoreCase = true) ||
                message.contains("connection", ignoreCase = true) ->
                "Cannot connect to server"

            else -> "Unenrollment failed. Please contact IT."
        }
    }
}

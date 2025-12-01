package three.two.bit.phonemanager.ui.devices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import three.two.bit.phonemanager.data.repository.AuthRepository
import three.two.bit.phonemanager.domain.model.UserDevice
import three.two.bit.phonemanager.network.DeviceApiService
import three.two.bit.phonemanager.security.SecureStorage
import timber.log.Timber
import javax.inject.Inject

/**
 * Story E10.6 Task 3: Device Management ViewModel
 *
 * Handles all device management operations including:
 * - Listing user's devices (AC E10.6.1)
 * - Linking current device (AC E10.6.2)
 * - Unlinking devices (AC E10.6.4)
 * - Transferring ownership (AC E10.6.5)
 *
 * Dependencies:
 * - AuthRepository for authentication state and tokens
 * - DeviceApiService for API calls
 * - SecureStorage for device ID and token access
 */
@HiltViewModel
class DeviceManagementViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val deviceApiService: DeviceApiService,
    private val secureStorage: SecureStorage,
) : ViewModel() {

    private val _uiState = MutableStateFlow<DeviceUiState>(DeviceUiState.Loading)
    val uiState: StateFlow<DeviceUiState> = _uiState.asStateFlow()

    private val _operationResult = MutableStateFlow<DeviceOperationResult>(DeviceOperationResult.Idle)
    val operationResult: StateFlow<DeviceOperationResult> = _operationResult.asStateFlow()

    private val _selectedDevice = MutableStateFlow<UserDevice?>(null)
    val selectedDevice: StateFlow<UserDevice?> = _selectedDevice.asStateFlow()

    private val _detailUiState = MutableStateFlow<DeviceDetailUiState>(DeviceDetailUiState.Loading)
    val detailUiState: StateFlow<DeviceDetailUiState> = _detailUiState.asStateFlow()

    // Current device ID for highlighting in the list
    val currentDeviceId: String
        get() = secureStorage.getDeviceId()

    init {
        // Auto-load devices when ViewModel is created
        refreshDevices()
    }

    /**
     * AC E10.6.1: Refresh the device list
     *
     * Fetches all devices linked to the current user's account.
     */
    fun refreshDevices() {
        val user = authRepository.getCurrentUser()
        if (user == null) {
            _uiState.value = DeviceUiState.Error(
                message = "You must be signed in to view your devices",
                errorCode = "not_authenticated"
            )
            return
        }

        val accessToken = secureStorage.getAccessToken()
        if (accessToken == null) {
            _uiState.value = DeviceUiState.Error(
                message = "Authentication required",
                errorCode = "no_token"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = DeviceUiState.Loading

            val result = deviceApiService.getUserDevices(
                userId = user.userId,
                includeInactive = false,
                accessToken = accessToken
            )

            result.fold(
                onSuccess = { devices ->
                    Timber.i("Loaded ${devices.size} devices for user ${user.userId}")
                    _uiState.value = if (devices.isEmpty()) {
                        DeviceUiState.Empty
                    } else {
                        DeviceUiState.Success(
                            devices = devices,
                            currentDeviceId = currentDeviceId
                        )
                    }
                },
                onFailure = { error ->
                    Timber.e(error, "Failed to load devices")
                    _uiState.value = DeviceUiState.Error(
                        message = getErrorMessage(error),
                        errorCode = getErrorCode(error)
                    )
                }
            )
        }
    }

    /**
     * AC E10.6.2: Link the current device to the user's account
     *
     * @param displayName Optional display name for the device
     * @param isPrimary Whether to set as primary device
     */
    fun linkCurrentDevice(displayName: String? = null, isPrimary: Boolean = false) {
        val user = authRepository.getCurrentUser()
        if (user == null) {
            _operationResult.value = DeviceOperationResult.Error(
                message = "You must be signed in to link a device",
                errorCode = "not_authenticated"
            )
            return
        }

        val accessToken = secureStorage.getAccessToken()
        if (accessToken == null) {
            _operationResult.value = DeviceOperationResult.Error(
                message = "Authentication required",
                errorCode = "no_token"
            )
            return
        }

        viewModelScope.launch {
            _operationResult.value = DeviceOperationResult.Loading

            val result = deviceApiService.linkDevice(
                userId = user.userId,
                deviceId = currentDeviceId,
                displayName = displayName,
                isPrimary = isPrimary,
                accessToken = accessToken
            )

            result.fold(
                onSuccess = {
                    Timber.i("Device linked successfully: $currentDeviceId")
                    _operationResult.value = DeviceOperationResult.Success(
                        message = "Device linked to your account"
                    )
                    // Refresh the device list
                    refreshDevices()
                },
                onFailure = { error ->
                    Timber.e(error, "Failed to link device")
                    _operationResult.value = DeviceOperationResult.Error(
                        message = getErrorMessage(error),
                        errorCode = getErrorCode(error)
                    )
                }
            )
        }
    }

    /**
     * AC E10.6.4: Unlink a device from the user's account
     *
     * @param deviceUuid UUID of the device to unlink
     * @return true if unlinking current device (app should clear auth)
     */
    fun unlinkDevice(deviceUuid: String): Boolean {
        val user = authRepository.getCurrentUser()
        if (user == null) {
            _operationResult.value = DeviceOperationResult.Error(
                message = "You must be signed in to unlink a device",
                errorCode = "not_authenticated"
            )
            return false
        }

        val accessToken = secureStorage.getAccessToken()
        if (accessToken == null) {
            _operationResult.value = DeviceOperationResult.Error(
                message = "Authentication required",
                errorCode = "no_token"
            )
            return false
        }

        val isUnlinkingCurrentDevice = deviceUuid == currentDeviceId

        viewModelScope.launch {
            _operationResult.value = DeviceOperationResult.Loading

            val result = deviceApiService.unlinkDevice(
                userId = user.userId,
                deviceId = deviceUuid,
                accessToken = accessToken
            )

            result.fold(
                onSuccess = {
                    Timber.i("Device unlinked successfully: $deviceUuid")

                    if (isUnlinkingCurrentDevice) {
                        // Clear authentication state when unlinking current device
                        authRepository.logout()
                        _operationResult.value = DeviceOperationResult.Success(
                            message = "Device unlinked. You have been signed out."
                        )
                    } else {
                        _operationResult.value = DeviceOperationResult.Success(
                            message = "Device unlinked from your account"
                        )
                        // Refresh the device list
                        refreshDevices()
                    }
                },
                onFailure = { error ->
                    Timber.e(error, "Failed to unlink device")
                    _operationResult.value = DeviceOperationResult.Error(
                        message = getErrorMessage(error),
                        errorCode = getErrorCode(error)
                    )
                }
            )
        }

        return isUnlinkingCurrentDevice
    }

    /**
     * AC E10.6.5: Transfer device ownership to another user
     *
     * @param deviceUuid UUID of the device to transfer
     * @param newOwnerId UUID of the new owner
     */
    fun transferDevice(deviceUuid: String, newOwnerId: String) {
        val user = authRepository.getCurrentUser()
        if (user == null) {
            _operationResult.value = DeviceOperationResult.Error(
                message = "You must be signed in to transfer a device",
                errorCode = "not_authenticated"
            )
            return
        }

        val accessToken = secureStorage.getAccessToken()
        if (accessToken == null) {
            _operationResult.value = DeviceOperationResult.Error(
                message = "Authentication required",
                errorCode = "no_token"
            )
            return
        }

        viewModelScope.launch {
            _operationResult.value = DeviceOperationResult.Loading

            val result = deviceApiService.transferDevice(
                userId = user.userId,
                deviceId = deviceUuid,
                newOwnerId = newOwnerId,
                accessToken = accessToken
            )

            result.fold(
                onSuccess = { response ->
                    Timber.i("Device transferred successfully: $deviceUuid to ${response.newOwnerId}")
                    _operationResult.value = DeviceOperationResult.Success(
                        message = "Device ownership transferred"
                    )
                    // Refresh the device list
                    refreshDevices()
                },
                onFailure = { error ->
                    Timber.e(error, "Failed to transfer device")
                    _operationResult.value = DeviceOperationResult.Error(
                        message = getErrorMessage(error),
                        errorCode = getErrorCode(error)
                    )
                }
            )
        }
    }

    /**
     * Select a device to view details
     *
     * @param device The device to view
     */
    fun selectDevice(device: UserDevice) {
        _selectedDevice.value = device
        _detailUiState.value = DeviceDetailUiState.Success(
            device = device,
            isCurrentDevice = device.deviceUuid == currentDeviceId,
            isOwner = true // Current user always owns devices in their list
        )
    }

    /**
     * Clear selected device
     */
    fun clearSelectedDevice() {
        _selectedDevice.value = null
        _detailUiState.value = DeviceDetailUiState.Loading
    }

    /**
     * Reset operation result state
     */
    fun clearOperationResult() {
        _operationResult.value = DeviceOperationResult.Idle
    }

    /**
     * Check if the current device is linked to the user's account
     *
     * @return true if the current device is in the user's device list
     */
    fun isCurrentDeviceLinked(): Boolean {
        val state = _uiState.value
        return if (state is DeviceUiState.Success) {
            state.devices.any { it.deviceUuid == currentDeviceId }
        } else {
            false
        }
    }

    /**
     * Convert exception to user-friendly error message
     */
    private fun getErrorMessage(exception: Throwable): String {
        val message = exception.message ?: ""
        return when {
            message.contains("401") || message.contains("unauthorized", ignoreCase = true) ->
                "Session expired. Please sign in again."
            message.contains("403") || message.contains("forbidden", ignoreCase = true) ->
                "You don't have permission for this action."
            message.contains("404") || message.contains("not found", ignoreCase = true) ->
                "Device not found."
            message.contains("409") || message.contains("conflict", ignoreCase = true) ->
                "Device is already linked to another user."
            message.contains("network", ignoreCase = true) ||
                message.contains("connection", ignoreCase = true) ->
                "Network error. Please check your connection."
            else -> "Something went wrong. Please try again."
        }
    }

    /**
     * Extract error code from exception
     */
    private fun getErrorCode(exception: Throwable): String? {
        val message = exception.message ?: ""
        return when {
            message.contains("401") -> "unauthorized"
            message.contains("403") -> "forbidden"
            message.contains("404") -> "not_found"
            message.contains("409") -> "conflict"
            message.contains("network", ignoreCase = true) -> "network_error"
            else -> null
        }
    }
}

package three.two.bit.phonemanager.ui.devices

import three.two.bit.phonemanager.domain.model.UserDevice

/**
 * Story E10.6 Task 3: UI State for Device Management
 *
 * Represents the different states of the device management screens.
 */
sealed interface DeviceUiState {
    /**
     * Initial loading state when fetching devices
     */
    data object Loading : DeviceUiState

    /**
     * Successfully loaded devices
     *
     * @property devices List of user's devices
     * @property currentDeviceId The device ID of the current device running the app
     */
    data class Success(
        val devices: List<UserDevice>,
        val currentDeviceId: String,
    ) : DeviceUiState

    /**
     * Error state when something went wrong
     *
     * @property message User-friendly error message
     * @property errorCode Optional error code for debugging
     */
    data class Error(
        val message: String,
        val errorCode: String? = null,
    ) : DeviceUiState

    /**
     * Empty state when user has no linked devices
     */
    data object Empty : DeviceUiState
}

/**
 * Story E10.6 Task 3: UI State for device detail screen
 */
sealed interface DeviceDetailUiState {
    /**
     * Loading device details
     */
    data object Loading : DeviceDetailUiState

    /**
     * Successfully loaded device details
     *
     * @property device The device being viewed
     * @property isCurrentDevice Whether this is the current device
     * @property isOwner Whether the current user is the owner
     */
    data class Success(
        val device: UserDevice,
        val isCurrentDevice: Boolean,
        val isOwner: Boolean = true,
    ) : DeviceDetailUiState

    /**
     * Error loading device details
     */
    data class Error(
        val message: String,
        val errorCode: String? = null,
    ) : DeviceDetailUiState
}

/**
 * Story E10.6 Task 3: Result state for device operations (link, unlink, transfer)
 */
sealed interface DeviceOperationResult {
    /**
     * Operation completed successfully
     *
     * @property message Success message to display
     */
    data class Success(val message: String) : DeviceOperationResult

    /**
     * Operation failed
     *
     * @property message Error message to display
     * @property errorCode Optional error code
     */
    data class Error(
        val message: String,
        val errorCode: String? = null,
    ) : DeviceOperationResult

    /**
     * Operation is in progress
     */
    data object Loading : DeviceOperationResult

    /**
     * No operation in progress (idle)
     */
    data object Idle : DeviceOperationResult
}

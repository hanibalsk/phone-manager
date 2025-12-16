package three.two.bit.phonemanager.data.repository

import three.two.bit.phonemanager.domain.model.Device
import three.two.bit.phonemanager.domain.model.DeviceSettings
import three.two.bit.phonemanager.network.DeviceApiService
import three.two.bit.phonemanager.network.NetworkException
import three.two.bit.phonemanager.network.NetworkManager
import three.two.bit.phonemanager.network.models.DeviceRegistrationRequest
import three.two.bit.phonemanager.security.SecureStorage
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Story E1.1: DeviceRepository - Device registration and management
 *
 * Handles device registration, storage, and group member retrieval
 */
interface DeviceRepository {
    suspend fun registerDevice(displayName: String, groupId: String): Result<Unit>
    suspend fun getGroupMembers(): Result<List<Device>>

    /**
     * Story E9.3: Get devices for a specific group (for admin use).
     *
     * Unlike getGroupMembers(), this allows fetching devices from any group
     * where the user has admin/owner access.
     *
     * @param groupId The ID of the group to fetch devices from
     * @return Result with list of devices or error
     */
    suspend fun getGroupDevices(groupId: String): Result<List<Device>>

    /**
     * Story E9.5: Get device tracking settings (for admin use).
     *
     * AC E9.5.5: Current tracking state reflected in UI
     *
     * @param deviceId The ID of the device to get settings for
     * @return Result with device settings or error
     */
    suspend fun getDeviceTrackingSettings(deviceId: String): Result<DeviceSettings>

    /**
     * Story E9.5: Toggle device tracking (for admin use).
     *
     * AC E9.5.1: Toggle control on user detail screen
     * AC E9.5.2: Backend API to update tracking state
     * AC E9.5.3: Visual confirmation of tracking state change
     *
     * @param deviceId The ID of the device to update
     * @param enabled Whether tracking should be enabled
     * @return Result with success or error
     */
    suspend fun toggleDeviceTracking(deviceId: String, enabled: Boolean): Result<Unit>

    fun isRegistered(): Boolean
    fun getDeviceId(): String
    fun getDisplayName(): String?
    fun getGroupId(): String?
}

@Singleton
class DeviceRepositoryImpl @Inject constructor(
    private val secureStorage: SecureStorage,
    private val deviceApiService: DeviceApiService,
    private val networkManager: NetworkManager,
    private val authRepository: AuthRepository,
) : DeviceRepository {

    /**
     * Register device with backend server
     *
     * @param displayName User-visible name for the device
     * @param groupId Group identifier for location sharing
     * @return Result.success on success, Result.failure with error on failure
     */
    override suspend fun registerDevice(displayName: String, groupId: String): Result<Unit> {
        if (!networkManager.isNetworkAvailable()) {
            Timber.w("Network not available for device registration")
            return Result.failure(NetworkException("No network connection available"))
        }

        val request = DeviceRegistrationRequest(
            deviceId = secureStorage.getDeviceId(),
            displayName = displayName,
            groupId = groupId,
            platform = "android",
        )

        return deviceApiService.registerDevice(request).map { response ->
            // Store locally on success
            secureStorage.setDisplayName(displayName)
            secureStorage.setGroupId(groupId)
            Timber.i("Device registration complete: ${response.deviceId}")
        }
    }

    /**
     * Get all devices in the current group (excluding current device)
     *
     * Story E1.2: AC E1.2.1, E1.2.2 - Fetch and filter group members
     *
     * @return Result with list of devices (excluding self) or error
     */
    override suspend fun getGroupMembers(): Result<List<Device>> {
        val groupId = secureStorage.getGroupId()
            ?: return Result.failure(IllegalStateException("Device not registered to a group"))

        if (!networkManager.isNetworkAvailable()) {
            return Result.failure(NetworkException("No network connection available"))
        }

        val currentDeviceId = secureStorage.getDeviceId()

        return deviceApiService.getGroupMembers(groupId).map { devices ->
            // Filter out the current device from the list
            val filteredDevices = devices.filter { it.deviceId != currentDeviceId }
            Timber.d("Filtered group members: ${filteredDevices.size} of ${devices.size} devices (excluding self)")
            filteredDevices
        }
    }

    /**
     * Check if device is registered
     */
    override fun isRegistered(): Boolean = secureStorage.isRegistered()

    /**
     * Get unique device ID
     */
    override fun getDeviceId(): String = secureStorage.getDeviceId()

    /**
     * Get stored display name
     */
    override fun getDisplayName(): String? = secureStorage.getDisplayName()

    /**
     * Get stored group ID
     */
    override fun getGroupId(): String? = secureStorage.getGroupId()

    /**
     * Story E9.3: Get devices for a specific group (for admin use).
     *
     * AC E9.3.2: Show list of managed devices/users
     *
     * @param groupId The ID of the group to fetch devices from
     * @return Result with list of devices or error
     */
    override suspend fun getGroupDevices(groupId: String): Result<List<Device>> {
        if (!networkManager.isNetworkAvailable()) {
            return Result.failure(NetworkException("No network connection available"))
        }

        return deviceApiService.getGroupMembers(groupId).also { result ->
            result.onSuccess { devices ->
                Timber.d("Fetched ${devices.size} devices for group $groupId")
            }
            result.onFailure { error ->
                Timber.w(error, "Failed to fetch devices for group $groupId")
            }
        }
    }

    /**
     * Story E9.5: Get device tracking settings (for admin use).
     *
     * AC E9.5.5: Current tracking state reflected in UI
     */
    override suspend fun getDeviceTrackingSettings(deviceId: String): Result<DeviceSettings> {
        if (!networkManager.isNetworkAvailable()) {
            return Result.failure(NetworkException("No network connection available"))
        }

        val accessToken = authRepository.getAccessToken()
            ?: return Result.failure(IllegalStateException("Not authenticated"))

        return deviceApiService.getDeviceSettings(deviceId, accessToken).also { result ->
            result.onSuccess { settings ->
                Timber.d("Fetched tracking settings for device $deviceId: enabled=${settings.trackingEnabled}")
            }
            result.onFailure { error ->
                Timber.w(error, "Failed to fetch tracking settings for device $deviceId")
            }
        }
    }

    /**
     * Story E9.5: Toggle device tracking (for admin use).
     *
     * AC E9.5.1: Toggle control on user detail screen
     * AC E9.5.2: Backend API to update tracking state
     */
    override suspend fun toggleDeviceTracking(deviceId: String, enabled: Boolean): Result<Unit> {
        if (!networkManager.isNetworkAvailable()) {
            return Result.failure(NetworkException("No network connection available"))
        }

        val accessToken = authRepository.getAccessToken()
            ?: return Result.failure(IllegalStateException("Not authenticated"))

        val settings = mapOf(DeviceSettings.KEY_TRACKING_ENABLED to enabled)

        return deviceApiService.updateAdminDeviceSettings(
            deviceId = deviceId,
            changes = settings,
            notifyUser = true,
            accessToken = accessToken,
        ).map { response ->
            if (response.success) {
                Timber.i("Successfully toggled tracking for device $deviceId: enabled=$enabled")
            } else {
                Timber.w("Failed to toggle tracking: ${response.error}")
                throw IllegalStateException(response.error ?: "Failed to update tracking")
            }
        }
    }
}

package three.two.bit.phonemanager.data.repository

import three.two.bit.phonemanager.domain.model.Device
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
}

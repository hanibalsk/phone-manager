package three.two.bit.phonemanager.data.repository

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.Before
import org.junit.Test
import three.two.bit.phonemanager.domain.model.Device
import three.two.bit.phonemanager.domain.model.DeviceLocation
import three.two.bit.phonemanager.network.DeviceApiService
import three.two.bit.phonemanager.network.NetworkException
import three.two.bit.phonemanager.network.NetworkManager
import three.two.bit.phonemanager.network.models.DeviceRegistrationResponse
import three.two.bit.phonemanager.security.SecureStorage
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for DeviceRepository
 *
 * Story E1.1: Tests device registration logic
 * Coverage target: > 80%
 */
class DeviceRepositoryTest {

    private lateinit var repository: DeviceRepositoryImpl
    private lateinit var secureStorage: SecureStorage
    private lateinit var deviceApiService: DeviceApiService
    private lateinit var networkManager: NetworkManager

    @Before
    fun setup() {
        secureStorage = mockk(relaxed = true)
        deviceApiService = mockk(relaxed = true)
        networkManager = mockk(relaxed = true)

        every { secureStorage.getDeviceId() } returns "test-device-id"
        every { networkManager.isNetworkAvailable() } returns true

        repository = DeviceRepositoryImpl(
            secureStorage = secureStorage,
            deviceApiService = deviceApiService,
            networkManager = networkManager,
        )
    }

    @Test
    fun `registerDevice returns failure when network unavailable`() = runTest {
        every { networkManager.isNetworkAvailable() } returns false

        val result = repository.registerDevice("Martin's Phone", "family")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is NetworkException)
        assertEquals("No network connection available", result.exceptionOrNull()?.message)

        coVerify(exactly = 0) { deviceApiService.registerDevice(any()) }
    }

    @Test
    fun `registerDevice calls API with correct request payload`() = runTest {
        val response = DeviceRegistrationResponse(
            deviceId = "test-device-id",
            displayName = "Martin's Phone",
            groupId = "family",
            createdAt = "2025-11-25T12:00:00Z",
            updatedAt = "2025-11-25T12:00:00Z",
        )
        coEvery { deviceApiService.registerDevice(any()) } returns Result.success(response)

        repository.registerDevice("Martin's Phone", "family")

        coVerify {
            deviceApiService.registerDevice(
                match { request ->
                    request.deviceId == "test-device-id" &&
                        request.displayName == "Martin's Phone" &&
                        request.groupId == "family" &&
                        request.platform == "android"
                },
            )
        }
    }

    @Test
    fun `registerDevice stores credentials on success`() = runTest {
        val response = DeviceRegistrationResponse(
            deviceId = "test-device-id",
            displayName = "Martin's Phone",
            groupId = "family",
            createdAt = "2025-11-25T12:00:00Z",
            updatedAt = "2025-11-25T12:00:00Z",
        )
        coEvery { deviceApiService.registerDevice(any()) } returns Result.success(response)

        val result = repository.registerDevice("Martin's Phone", "family")

        assertTrue(result.isSuccess)
        verify { secureStorage.setDisplayName("Martin's Phone") }
        verify { secureStorage.setGroupId("family") }
    }

    @Test
    fun `registerDevice returns failure on API error`() = runTest {
        coEvery {
            deviceApiService.registerDevice(any())
        } returns Result.failure(RuntimeException("Server error"))

        val result = repository.registerDevice("Martin's Phone", "family")

        assertTrue(result.isFailure)
        assertEquals("Server error", result.exceptionOrNull()?.message)

        // Should not store credentials on failure
        verify(exactly = 0) { secureStorage.setDisplayName(any()) }
        verify(exactly = 0) { secureStorage.setGroupId(any()) }
    }

    @Test
    fun `isRegistered returns true when displayName and groupId exist`() {
        every { secureStorage.isRegistered() } returns true

        assertTrue(repository.isRegistered())
    }

    @Test
    fun `isRegistered returns false when not registered`() {
        every { secureStorage.isRegistered() } returns false

        assertFalse(repository.isRegistered())
    }

    @Test
    fun `getDeviceId returns stored device ID`() {
        every { secureStorage.getDeviceId() } returns "unique-device-id"

        assertEquals("unique-device-id", repository.getDeviceId())
    }

    @Test
    fun `getDisplayName returns stored display name`() {
        every { secureStorage.getDisplayName() } returns "Martin's Phone"

        assertEquals("Martin's Phone", repository.getDisplayName())
    }

    @Test
    fun `getDisplayName returns null when not set`() {
        every { secureStorage.getDisplayName() } returns null

        assertNull(repository.getDisplayName())
    }

    @Test
    fun `getGroupId returns stored group ID`() {
        every { secureStorage.getGroupId() } returns "family"

        assertEquals("family", repository.getGroupId())
    }

    @Test
    fun `getGroupId returns null when not set`() {
        every { secureStorage.getGroupId() } returns null

        assertNull(repository.getGroupId())
    }

    @Test
    fun `getGroupMembers returns failure when not registered`() = runTest {
        every { secureStorage.getGroupId() } returns null

        val result = repository.getGroupMembers()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }

    @Test
    fun `getGroupMembers returns failure when network unavailable`() = runTest {
        every { secureStorage.getGroupId() } returns "family"
        every { networkManager.isNetworkAvailable() } returns false

        val result = repository.getGroupMembers()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is NetworkException)
    }

    @Test
    fun `getGroupMembers calls API with correct groupId`() = runTest {
        every { secureStorage.getGroupId() } returns "family"
        coEvery { deviceApiService.getGroupMembers("family") } returns Result.success(emptyList())

        repository.getGroupMembers()

        coVerify { deviceApiService.getGroupMembers("family") }
    }

    @Test
    fun `getGroupMembers filters out current device`() = runTest {
        // Given
        val currentDeviceId = "device-current"
        val devices = listOf(
            Device(
                deviceId = currentDeviceId,
                displayName = "My Device",
                lastLocation = null,
                lastSeenAt = null,
            ),
            Device(
                deviceId = "device-001",
                displayName = "Other Device 1",
                lastLocation = DeviceLocation(
                    latitude = 48.1486,
                    longitude = 17.1077,
                    timestamp = Instant.parse("2025-11-25T18:30:00Z"),
                ),
                lastSeenAt = Instant.parse("2025-11-25T18:30:00Z"),
            ),
            Device(
                deviceId = "device-002",
                displayName = "Other Device 2",
                lastLocation = null,
                lastSeenAt = null,
            ),
        )

        every { secureStorage.getGroupId() } returns "family"
        every { secureStorage.getDeviceId() } returns currentDeviceId
        every { networkManager.isNetworkAvailable() } returns true
        coEvery { deviceApiService.getGroupMembers("family") } returns Result.success(devices)

        // When
        val result = repository.getGroupMembers()

        // Then
        assertTrue(result.isSuccess)
        val filteredDevices = result.getOrNull()!!
        assertEquals(2, filteredDevices.size)
        assertEquals("device-001", filteredDevices[0].deviceId)
        assertEquals("device-002", filteredDevices[1].deviceId)
    }

    @Test
    fun `getGroupMembers returns empty list when only current device in group`() = runTest {
        // Given
        val currentDeviceId = "device-current"
        val devices = listOf(
            Device(
                deviceId = currentDeviceId,
                displayName = "My Device",
                lastLocation = null,
                lastSeenAt = null,
            ),
        )

        every { secureStorage.getGroupId() } returns "family"
        every { secureStorage.getDeviceId() } returns currentDeviceId
        every { networkManager.isNetworkAvailable() } returns true
        coEvery { deviceApiService.getGroupMembers("family") } returns Result.success(devices)

        // When
        val result = repository.getGroupMembers()

        // Then
        assertTrue(result.isSuccess)
        val filteredDevices = result.getOrNull()!!
        assertEquals(0, filteredDevices.size)
    }

    @Test
    fun `getGroupMembers returns empty list when API returns empty list`() = runTest {
        // Given
        every { secureStorage.getGroupId() } returns "family"
        every { secureStorage.getDeviceId() } returns "device-current"
        every { networkManager.isNetworkAvailable() } returns true
        coEvery { deviceApiService.getGroupMembers("family") } returns Result.success(emptyList())

        // When
        val result = repository.getGroupMembers()

        // Then
        assertTrue(result.isSuccess)
        val filteredDevices = result.getOrNull()!!
        assertEquals(0, filteredDevices.size)
    }

    @Test
    fun `getGroupMembers propagates API error`() = runTest {
        // Given
        every { secureStorage.getGroupId() } returns "family"
        every { secureStorage.getDeviceId() } returns "device-current"
        every { networkManager.isNetworkAvailable() } returns true
        coEvery { deviceApiService.getGroupMembers("family") } returns Result.failure(Exception("API error"))

        // When
        val result = repository.getGroupMembers()

        // Then
        assertTrue(result.isFailure)
    }
}

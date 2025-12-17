package three.two.bit.phonemanager.data.repository

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import three.two.bit.phonemanager.network.DeviceApiService
import three.two.bit.phonemanager.network.models.AdminDeviceSettingsResponse
import three.two.bit.phonemanager.network.models.BulkUpdateDeviceResult
import three.two.bit.phonemanager.network.models.BulkUpdateResponse
import three.two.bit.phonemanager.network.models.LockSettingsResponse
import three.two.bit.phonemanager.network.models.MemberDeviceResponse
import three.two.bit.phonemanager.network.models.MemberDevicesResponse
import three.two.bit.phonemanager.network.models.SaveTemplateResponse
import three.two.bit.phonemanager.network.models.SettingChangeResponse
import three.two.bit.phonemanager.network.models.SettingLockResponse
import three.two.bit.phonemanager.network.models.SettingValueResponse
import three.two.bit.phonemanager.network.models.SettingsHistoryResponse
import three.two.bit.phonemanager.network.models.SettingsTemplateResponse
import three.two.bit.phonemanager.network.models.TemplatesResponse
import three.two.bit.phonemanager.network.models.UpdateSettingsResponse
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock

/**
 * Unit tests for AdminSettingsRepository
 *
 * Story E12.7: Admin Settings Management
 * Coverage target: > 80%
 */
class AdminSettingsRepositoryTest {

    private lateinit var repository: AdminSettingsRepositoryImpl
    private lateinit var deviceApiService: DeviceApiService
    private lateinit var authRepository: AuthRepository

    private val testAccessToken = "test-access-token"
    private val testGroupId = "test-group-id"
    private val testDeviceId = "test-device-id"

    @Before
    fun setup() {
        deviceApiService = mockk(relaxed = true)
        authRepository = mockk(relaxed = true)

        coEvery { authRepository.getAccessToken() } returns testAccessToken

        repository = AdminSettingsRepositoryImpl(
            deviceApiService = deviceApiService,
            authRepository = authRepository,
        )
    }

    // AC E12.7.1: Device Settings List Screen Tests

    @Test
    fun `getMemberDevices returns failure when not authenticated`() = runTest {
        coEvery { authRepository.getAccessToken() } returns null

        val result = repository.getMemberDevices(testGroupId)

        assertTrue(result.isFailure)
        assertEquals("Not authenticated", result.exceptionOrNull()?.message)
    }

    @Test
    fun `getMemberDevices returns list of devices on success`() = runTest {
        val response = MemberDevicesResponse(
            devices = listOf(
                MemberDeviceResponse(
                    deviceId = "device-1",
                    displayName = "Phone 1",
                    lastSeenAt = "2025-01-15T10:00:00Z",
                ),
                MemberDeviceResponse(
                    deviceId = "device-2",
                    displayName = "Phone 2",
                    lastSeenAt = "2025-01-14T08:00:00Z",
                ),
            ),
        )
        coEvery {
            deviceApiService.getGroupMemberDevices(testGroupId, testAccessToken)
        } returns Result.success(response)

        val result = repository.getMemberDevices(testGroupId)

        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull()?.size)
        assertEquals("device-1", result.getOrNull()?.get(0)?.deviceId)
        assertEquals("Phone 1", result.getOrNull()?.get(0)?.deviceName)
    }

    @Test
    fun `getMemberDevices updates memberDevices state flow`() = runTest {
        val response = MemberDevicesResponse(
            devices = listOf(
                MemberDeviceResponse(
                    deviceId = "device-1",
                    displayName = "Phone 1",
                    lastSeenAt = null,
                ),
            ),
        )
        coEvery {
            deviceApiService.getGroupMemberDevices(testGroupId, testAccessToken)
        } returns Result.success(response)

        repository.getMemberDevices(testGroupId)

        assertEquals(1, repository.memberDevices.value.size)
        assertEquals("device-1", repository.memberDevices.value[0].deviceId)
    }

    // AC E12.7.3: View Remote Settings Tests

    @Test
    fun `getDeviceSettings returns failure when not authenticated`() = runTest {
        coEvery { authRepository.getAccessToken() } returns null

        val result = repository.getDeviceSettings(testDeviceId)

        assertTrue(result.isFailure)
        assertEquals("Not authenticated", result.exceptionOrNull()?.message)
    }

    @Test
    fun `getDeviceSettings returns device settings on success`() = runTest {
        val response = AdminDeviceSettingsResponse(
            deviceId = testDeviceId,
            deviceName = "Test Phone",
            ownerUserId = "user-1",
            ownerName = "Test User",
            ownerEmail = "test@test.com",
            isOnline = true,
            lastSeen = "2025-01-15T10:00:00Z",
            settings = mapOf(
                "tracking_enabled" to SettingValueResponse(value = true, isLocked = true, lockedBy = "admin@test.com", lockedAt = "2025-01-15T09:00:00Z"),
                "tracking_interval_seconds" to SettingValueResponse(value = 60),
            ),
            locks = mapOf(
                "tracking_enabled" to SettingLockResponse(
                    isLocked = true,
                    lockedBy = "admin@test.com",
                    lockedAt = "2025-01-15T09:00:00Z",
                ),
            ),
            lastSyncedAt = "2025-01-15T10:00:00Z",
            lastModifiedBy = "admin@test.com",
        )
        coEvery {
            deviceApiService.getAdminDeviceSettings(testDeviceId, testAccessToken)
        } returns Result.success(response)

        val result = repository.getDeviceSettings(testDeviceId)

        assertTrue(result.isSuccess)
        val settings = result.getOrNull()
        assertNotNull(settings)
        assertEquals(testDeviceId, settings.deviceId)
        assertEquals("Test Phone", settings.deviceName)
        assertEquals(true, settings.settings["tracking_enabled"])
        assertTrue(settings.isLocked("tracking_enabled"))
    }

    @Test
    fun `getDeviceSettings updates currentDeviceSettings state flow`() = runTest {
        val response = AdminDeviceSettingsResponse(
            deviceId = testDeviceId,
            deviceName = "Test Phone",
            ownerUserId = "user-1",
            ownerName = "Test User",
            ownerEmail = "test@test.com",
            isOnline = true,
            lastSeen = null,
            settings = emptyMap(),
            locks = emptyMap(),
            lastSyncedAt = null,
            lastModifiedBy = null,
        )
        coEvery {
            deviceApiService.getAdminDeviceSettings(testDeviceId, testAccessToken)
        } returns Result.success(response)

        repository.getDeviceSettings(testDeviceId)

        assertNotNull(repository.currentDeviceSettings.value)
        assertEquals(testDeviceId, repository.currentDeviceSettings.value?.deviceId)
    }

    // AC E12.7.4: Modify Remote Settings Tests

    @Test
    fun `updateDeviceSettings returns failure when not authenticated`() = runTest {
        coEvery { authRepository.getAccessToken() } returns null

        val result = repository.updateDeviceSettings(
            deviceId = testDeviceId,
            changes = mapOf("tracking_enabled" to false),
        )

        assertTrue(result.isFailure)
    }

    @Test
    fun `updateDeviceSettings returns applied settings on success`() = runTest {
        val changes = mapOf<String, Any>("tracking_enabled" to false)
        val response = UpdateSettingsResponse(
            updated = listOf("tracking_enabled"),
            locked = emptyList(),
            invalid = emptyList(),
            settings = mapOf("tracking_enabled" to SettingValueResponse(value = false)),
        )
        coEvery {
            deviceApiService.updateAdminDeviceSettings(
                deviceId = testDeviceId,
                changes = changes,
                notifyUser = true,
                accessToken = testAccessToken,
            )
        } returns Result.success(response)

        val result = repository.updateDeviceSettings(testDeviceId, changes)

        assertTrue(result.isSuccess)
        assertEquals(false, result.getOrNull()?.get("tracking_enabled"))
    }

    @Test
    fun `updateDeviceSettings returns failure on error response`() = runTest {
        val response = UpdateSettingsResponse(
            updated = emptyList(),
            locked = listOf("tracking_enabled"),
            invalid = emptyList(),
            settings = emptyMap(),
        )
        coEvery {
            deviceApiService.updateAdminDeviceSettings(
                deviceId = testDeviceId,
                changes = any(),
                notifyUser = any(),
                accessToken = testAccessToken,
            )
        } returns Result.success(response)

        val result = repository.updateDeviceSettings(testDeviceId, mapOf("tracking_enabled" to false))

        assertTrue(result.isFailure)
        assertEquals("Settings locked: tracking_enabled", result.exceptionOrNull()?.message)
    }

    // AC E12.7.5: Lock/Unlock Settings Tests

    @Test
    fun `lockSettings returns failure when not authenticated`() = runTest {
        coEvery { authRepository.getAccessToken() } returns null

        val result = repository.lockSettings(testDeviceId, listOf("tracking_enabled"))

        assertTrue(result.isFailure)
    }

    @Test
    fun `lockSettings returns locked count on success`() = runTest {
        val response = LockSettingsResponse(
            success = true,
            lockedCount = 2,
            unlockedCount = 0,
        )
        coEvery {
            deviceApiService.lockDeviceSettings(
                deviceId = testDeviceId,
                settingKeys = listOf("tracking_enabled", "secret_mode_enabled"),
                lock = true,
                accessToken = testAccessToken,
            )
        } returns Result.success(response)

        val result = repository.lockSettings(
            testDeviceId,
            listOf("tracking_enabled", "secret_mode_enabled"),
        )

        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull())
    }

    @Test
    fun `unlockSettings returns unlocked count on success`() = runTest {
        val response = LockSettingsResponse(
            success = true,
            lockedCount = 0,
            unlockedCount = 1,
        )
        coEvery {
            deviceApiService.lockDeviceSettings(
                deviceId = testDeviceId,
                settingKeys = listOf("tracking_enabled"),
                lock = false,
                accessToken = testAccessToken,
            )
        } returns Result.success(response)

        val result = repository.unlockSettings(testDeviceId, listOf("tracking_enabled"))

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull())
    }

    // AC E12.7.6: Bulk Settings Application Tests

    @Test
    fun `bulkUpdateDevices returns failure when not authenticated`() = runTest {
        coEvery { authRepository.getAccessToken() } returns null

        val result = repository.bulkUpdateDevices(
            deviceIds = listOf("device-1", "device-2"),
            settings = mapOf("tracking_enabled" to true),
        )

        assertTrue(result.isFailure)
    }

    @Test
    fun `bulkUpdateDevices returns results for all devices`() = runTest {
        val response = BulkUpdateResponse(
            successful = listOf(
                BulkUpdateDeviceResult(
                    deviceId = "device-1",
                    deviceName = "Phone 1",
                    appliedSettings = mapOf("tracking_enabled" to true),
                ),
            ),
            failed = listOf(
                BulkUpdateDeviceResult(
                    deviceId = "device-2",
                    deviceName = "Phone 2",
                    error = "Device offline",
                ),
            ),
        )
        coEvery {
            deviceApiService.bulkUpdateSettings(
                deviceIds = listOf("device-1", "device-2"),
                settings = mapOf("tracking_enabled" to true),
                locks = null,
                notifyUsers = true,
                accessToken = testAccessToken,
            )
        } returns Result.success(response)

        val result = repository.bulkUpdateDevices(
            deviceIds = listOf("device-1", "device-2"),
            settings = mapOf("tracking_enabled" to true),
        )

        assertTrue(result.isSuccess)
        val bulkResult = result.getOrNull()
        assertNotNull(bulkResult)
        assertEquals(1, bulkResult.successCount)
        assertEquals(1, bulkResult.failureCount)
        assertEquals(2, bulkResult.totalCount)
        assertFalse(bulkResult.isAllSuccessful)
    }

    // AC E12.7.7: Settings Templates Tests

    @Test
    fun `getTemplates returns failure when not authenticated`() = runTest {
        coEvery { authRepository.getAccessToken() } returns null

        val result = repository.getTemplates()

        assertTrue(result.isFailure)
    }

    @Test
    fun `getTemplates returns list of templates on success`() = runTest {
        val response = TemplatesResponse(
            templates = listOf(
                SettingsTemplateResponse(
                    id = "template-1",
                    name = "Default Template",
                    description = "Standard tracking settings",
                    settings = mapOf("tracking_enabled" to true),
                    lockedSettings = listOf("tracking_enabled"),
                    createdBy = "admin@test.com",
                    createdByName = "Admin",
                    createdAt = "2025-01-15T10:00:00Z",
                    updatedAt = null,
                    isShared = true,
                ),
            ),
        )
        coEvery {
            deviceApiService.getSettingsTemplates(testAccessToken)
        } returns Result.success(response)

        val result = repository.getTemplates()

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.size)
        assertEquals("template-1", result.getOrNull()?.get(0)?.id)
        assertEquals("Default Template", result.getOrNull()?.get(0)?.name)
    }

    @Test
    fun `saveTemplate returns saved template on success`() = runTest {
        val templateResponse = SettingsTemplateResponse(
            id = "template-new",
            name = "New Template",
            description = "Test description",
            settings = mapOf("tracking_enabled" to true),
            lockedSettings = emptyList(),
            createdBy = "admin@test.com",
            createdByName = "Admin",
            createdAt = "2025-01-15T10:00:00Z",
            updatedAt = null,
            isShared = false,
        )
        val response = SaveTemplateResponse(
            success = true,
            template = templateResponse,
        )
        coEvery {
            deviceApiService.saveSettingsTemplate(
                name = "New Template",
                description = "Test description",
                settings = mapOf("tracking_enabled" to true),
                lockedSettings = emptyList(),
                isShared = false,
                accessToken = testAccessToken,
            )
        } returns Result.success(response)

        val template = three.two.bit.phonemanager.domain.model.SettingsTemplate(
            id = "",
            name = "New Template",
            description = "Test description",
            settings = mapOf("tracking_enabled" to true),
            lockedSettings = emptySet(),
            createdBy = "admin@test.com",
            createdByName = "Admin",
            createdAt = Clock.System.now(),
            isShared = false,
        )

        val result = repository.saveTemplate(template)

        assertTrue(result.isSuccess)
        assertEquals("template-new", result.getOrNull()?.id)
    }

    @Test
    fun `deleteTemplate returns success on successful deletion`() = runTest {
        coEvery {
            deviceApiService.deleteSettingsTemplate("template-1", testAccessToken)
        } returns Result.success(Unit)

        // First load templates
        val templatesResponse = TemplatesResponse(
            templates = listOf(
                SettingsTemplateResponse(
                    id = "template-1",
                    name = "Template 1",
                    description = null,
                    settings = emptyMap(),
                    lockedSettings = emptyList(),
                    createdBy = "admin",
                    createdByName = "Admin",
                    createdAt = "2025-01-15T10:00:00Z",
                    updatedAt = null,
                    isShared = false,
                ),
            ),
        )
        coEvery {
            deviceApiService.getSettingsTemplates(testAccessToken)
        } returns Result.success(templatesResponse)

        repository.getTemplates()
        assertEquals(1, repository.templates.value.size)

        val result = repository.deleteTemplate("template-1")

        assertTrue(result.isSuccess)
        assertEquals(0, repository.templates.value.size)
    }

    // AC E12.7.8: Audit Trail Tests

    @Test
    fun `getSettingsHistory returns failure when not authenticated`() = runTest {
        coEvery { authRepository.getAccessToken() } returns null

        val result = repository.getSettingsHistory(testDeviceId)

        assertTrue(result.isFailure)
    }

    @Test
    fun `getSettingsHistory returns list of changes on success`() = runTest {
        val response = SettingsHistoryResponse(
            changes = listOf(
                SettingChangeResponse(
                    id = "change-1",
                    settingKey = "tracking_enabled",
                    oldValue = false,
                    newValue = true,
                    changedBy = "admin@test.com",
                    changedByName = "Admin",
                    changedAt = "2025-01-15T10:00:00Z",
                    changeType = "VALUE_CHANGED",
                ),
                SettingChangeResponse(
                    id = "change-2",
                    settingKey = "tracking_enabled",
                    oldValue = null,
                    newValue = null,
                    changedBy = "admin@test.com",
                    changedByName = "Admin",
                    changedAt = "2025-01-15T11:00:00Z",
                    changeType = "LOCKED",
                ),
            ),
            totalCount = 2,
        )
        coEvery {
            deviceApiService.getSettingsHistory(
                deviceId = testDeviceId,
                limit = 50,
                offset = 0,
                accessToken = testAccessToken,
            )
        } returns Result.success(response)

        val result = repository.getSettingsHistory(testDeviceId)

        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull()?.size)
        assertEquals("tracking_enabled", result.getOrNull()?.get(0)?.settingKey)
        assertEquals(
            three.two.bit.phonemanager.domain.model.SettingChangeType.VALUE_CHANGED,
            result.getOrNull()?.get(0)?.changeType,
        )
        assertEquals(
            three.two.bit.phonemanager.domain.model.SettingChangeType.LOCKED,
            result.getOrNull()?.get(1)?.changeType,
        )
    }

    // State Management Tests

    @Test
    fun `clearCurrentDevice clears current device settings`() = runTest {
        // Setup - load device settings first
        val response = AdminDeviceSettingsResponse(
            deviceId = testDeviceId,
            deviceName = "Test Phone",
            ownerUserId = "user-1",
            ownerName = "Test User",
            ownerEmail = "test@test.com",
            isOnline = true,
            lastSeen = null,
            settings = emptyMap(),
            locks = emptyMap(),
            lastSyncedAt = null,
            lastModifiedBy = null,
        )
        coEvery {
            deviceApiService.getAdminDeviceSettings(testDeviceId, testAccessToken)
        } returns Result.success(response)

        repository.getDeviceSettings(testDeviceId)
        assertNotNull(repository.currentDeviceSettings.value)

        // Clear
        repository.clearCurrentDevice()

        assertNull(repository.currentDeviceSettings.value)
    }

    @Test
    fun `clearError clears error state`() = runTest {
        // Trigger an error
        coEvery {
            deviceApiService.getAdminDeviceSettings(testDeviceId, testAccessToken)
        } returns Result.failure(Exception("Test error"))

        repository.getDeviceSettings(testDeviceId)

        // Verify error is set
        assertEquals("Test error", repository.error.value)

        // Clear error
        repository.clearError()

        assertNull(repository.error.value)
    }

    @Test
    fun `isLoading state is managed correctly during operations`() = runTest {
        assertFalse(repository.isLoading.value)

        val response = MemberDevicesResponse(devices = emptyList())
        coEvery {
            deviceApiService.getGroupMemberDevices(testGroupId, testAccessToken)
        } returns Result.success(response)

        repository.getMemberDevices(testGroupId)

        // After completion, loading should be false
        assertFalse(repository.isLoading.value)
    }
}

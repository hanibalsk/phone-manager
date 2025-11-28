package three.two.bit.phonemanager.data.repository

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import three.two.bit.phonemanager.data.database.WebhookDao
import three.two.bit.phonemanager.data.model.WebhookEntity
import three.two.bit.phonemanager.network.WebhookApiService
import three.two.bit.phonemanager.network.models.ListWebhooksResponse
import three.two.bit.phonemanager.network.models.WebhookDto
import three.two.bit.phonemanager.security.SecureStorage
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Story E6.3: WebhookRepository Unit Tests
 *
 * Tests for webhook CRUD operations and server sync
 * Coverage target: > 80%
 */
class WebhookRepositoryTest {

    private lateinit var repository: WebhookRepositoryImpl
    private lateinit var webhookDao: WebhookDao
    private lateinit var webhookApiService: WebhookApiService
    private lateinit var secureStorage: SecureStorage

    private val testDeviceId = "test-device-id"

    @Before
    fun setup() {
        webhookDao = mockk(relaxed = true)
        webhookApiService = mockk(relaxed = true)
        secureStorage = mockk(relaxed = true)

        every { secureStorage.getDeviceId() } returns testDeviceId

        repository = WebhookRepositoryImpl(
            webhookDao = webhookDao,
            webhookApiService = webhookApiService,
            secureStorage = secureStorage,
        )
    }

    // region createWebhook tests

    @Test
    fun `createWebhook saves webhook locally`() = runTest {
        coEvery { webhookApiService.createWebhook(any()) } returns Result.failure(Exception("Network error"))

        val result = repository.createWebhook(
            name = "Test Webhook",
            targetUrl = "https://example.com/webhook",
            secret = "test-secret",
        )

        assertTrue(result.isSuccess)
        coVerify { webhookDao.insert(any()) }
    }

    @Test
    fun `createWebhook sets correct default values`() = runTest {
        coEvery { webhookApiService.createWebhook(any()) } returns Result.failure(Exception("Network error"))

        val result = repository.createWebhook(
            name = "Test Webhook",
            targetUrl = "https://example.com/webhook",
            secret = "test-secret",
        )

        assertTrue(result.isSuccess)
        val webhook = result.getOrNull()!!
        assertEquals(testDeviceId, webhook.ownerDeviceId)
        assertEquals("Test Webhook", webhook.name)
        assertEquals("https://example.com/webhook", webhook.targetUrl)
        assertEquals("test-secret", webhook.secret)
        assertTrue(webhook.enabled)
    }

    @Test
    fun `createWebhook syncs to server`() = runTest {
        val serverResponse = WebhookDto(
            webhookId = "server-webhook-id",
            ownerDeviceId = testDeviceId,
            name = "Test Webhook",
            targetUrl = "https://example.com/webhook",
            secret = "test-secret",
            enabled = true,
            createdAt = "2025-11-28T12:00:00Z",
            updatedAt = "2025-11-28T12:00:00Z",
        )
        coEvery { webhookApiService.createWebhook(any()) } returns Result.success(serverResponse)

        val result = repository.createWebhook(
            name = "Test Webhook",
            targetUrl = "https://example.com/webhook",
            secret = "test-secret",
        )

        assertTrue(result.isSuccess)
        coVerify { webhookApiService.createWebhook(any()) }
    }

    // endregion

    // region getWebhook tests

    @Test
    fun `getWebhook returns webhook when found`() = runTest {
        val entity = createTestWebhookEntity("webhook-1")
        coEvery { webhookDao.getById("webhook-1") } returns entity

        val result = repository.getWebhook("webhook-1")

        assertNotNull(result)
        assertEquals("webhook-1", result.id)
    }

    @Test
    fun `getWebhook returns null when not found`() = runTest {
        coEvery { webhookDao.getById("non-existent") } returns null

        val result = repository.getWebhook("non-existent")

        assertNull(result)
    }

    // endregion

    // region getAllWebhooks tests

    @Test
    fun `getAllWebhooks returns all webhooks for device`() = runTest {
        val entities = listOf(
            createTestWebhookEntity("webhook-1"),
            createTestWebhookEntity("webhook-2"),
        )
        coEvery { webhookDao.getAllByDevice(testDeviceId) } returns entities

        val result = repository.getAllWebhooks()

        assertEquals(2, result.size)
    }

    // endregion

    // region getEnabledWebhooks tests

    @Test
    fun `getEnabledWebhooks returns only enabled webhooks`() = runTest {
        val entities = listOf(
            createTestWebhookEntity("webhook-1", enabled = true),
        )
        coEvery { webhookDao.getEnabledByDevice(testDeviceId) } returns entities

        val result = repository.getEnabledWebhooks()

        assertEquals(1, result.size)
        assertTrue(result[0].enabled)
    }

    // endregion

    // region updateWebhook tests

    @Test
    fun `updateWebhook updates local database`() = runTest {
        val entity = createTestWebhookEntity("webhook-1")
        coEvery { webhookDao.getById("webhook-1") } returns entity
        val updateResponse = WebhookDto(
            webhookId = "webhook-1",
            ownerDeviceId = testDeviceId,
            name = "Updated Name",
            targetUrl = "https://example.com/webhook",
            secret = "test-secret",
            enabled = true,
            createdAt = "2025-11-28T12:00:00Z",
            updatedAt = "2025-11-28T12:00:00Z",
        )
        coEvery { webhookApiService.updateWebhook(any(), any()) } returns Result.success(updateResponse)

        val result = repository.updateWebhook("webhook-1", name = "Updated Name")

        assertTrue(result.isSuccess)
        coVerify { webhookDao.update(any()) }
    }

    @Test
    fun `updateWebhook returns failure for non-existent webhook`() = runTest {
        coEvery { webhookDao.getById("non-existent") } returns null

        val result = repository.updateWebhook("non-existent", name = "Updated Name")

        assertTrue(result.isFailure)
    }

    @Test
    fun `updateWebhook syncs to server`() = runTest {
        val entity = createTestWebhookEntity("webhook-1")
        coEvery { webhookDao.getById("webhook-1") } returns entity
        val updateResponse = WebhookDto(
            webhookId = "webhook-1",
            ownerDeviceId = testDeviceId,
            name = "Updated Name",
            targetUrl = "https://example.com/webhook",
            secret = "test-secret",
            enabled = true,
            createdAt = "2025-11-28T12:00:00Z",
            updatedAt = "2025-11-28T12:00:00Z",
        )
        coEvery { webhookApiService.updateWebhook(any(), any()) } returns Result.success(updateResponse)

        repository.updateWebhook("webhook-1", name = "Updated Name")

        coVerify { webhookApiService.updateWebhook("webhook-1", any()) }
    }

    // endregion

    // region deleteWebhook tests

    @Test
    fun `deleteWebhook removes from local database`() = runTest {
        coEvery { webhookApiService.deleteWebhook("webhook-1") } returns Result.success(Unit)

        val result = repository.deleteWebhook("webhook-1")

        assertTrue(result.isSuccess)
        coVerify { webhookDao.deleteById("webhook-1") }
    }

    @Test
    fun `deleteWebhook syncs to server first`() = runTest {
        coEvery { webhookApiService.deleteWebhook("webhook-1") } returns Result.success(Unit)

        repository.deleteWebhook("webhook-1")

        coVerify { webhookApiService.deleteWebhook("webhook-1") }
    }

    @Test
    fun `deleteWebhook still deletes locally when server fails`() = runTest {
        coEvery { webhookApiService.deleteWebhook("webhook-1") } returns Result.failure(Exception("Network error"))

        val result = repository.deleteWebhook("webhook-1")

        assertTrue(result.isSuccess)
        coVerify { webhookDao.deleteById("webhook-1") }
    }

    // endregion

    // region toggleWebhook tests

    @Test
    fun `toggleWebhook toggles enabled state`() = runTest {
        val entity = createTestWebhookEntity("webhook-1", enabled = true)
        coEvery { webhookDao.getById("webhook-1") } returns entity
        val updateResponse = WebhookDto(
            webhookId = "webhook-1",
            ownerDeviceId = testDeviceId,
            name = "Test Webhook",
            targetUrl = "https://example.com/webhook",
            secret = "test-secret",
            enabled = false,
            createdAt = "2025-11-28T12:00:00Z",
            updatedAt = "2025-11-28T12:00:00Z",
        )
        coEvery { webhookApiService.updateWebhook(any(), any()) } returns Result.success(updateResponse)

        val result = repository.toggleWebhook("webhook-1")

        assertTrue(result.isSuccess)
        coVerify {
            webhookDao.update(match { it.enabled == false })
        }
    }

    @Test
    fun `toggleWebhook returns failure for non-existent webhook`() = runTest {
        coEvery { webhookDao.getById("non-existent") } returns null

        val result = repository.toggleWebhook("non-existent")

        assertTrue(result.isFailure)
    }

    // endregion

    // region syncFromServer tests

    @Test
    fun `syncFromServer replaces local data with server data`() = runTest {
        val serverResponse = ListWebhooksResponse(
            webhooks = emptyList(),
            total = 0,
        )
        coEvery { webhookApiService.listWebhooks(testDeviceId) } returns Result.success(serverResponse)

        val result = repository.syncFromServer()

        assertTrue(result.isSuccess)
        coVerify { webhookDao.deleteAllByDevice(testDeviceId) }
    }

    @Test
    fun `syncFromServer returns failure when API fails`() = runTest {
        coEvery { webhookApiService.listWebhooks(testDeviceId) } returns Result.failure(Exception("API error"))

        val result = repository.syncFromServer()

        assertTrue(result.isFailure)
    }

    // endregion

    // region observeWebhooks tests

    @Test
    fun `observeWebhooks returns flow from DAO`() = runTest {
        val entities = listOf(
            createTestWebhookEntity("webhook-1"),
            createTestWebhookEntity("webhook-2"),
        )
        every { webhookDao.observeByDevice(testDeviceId) } returns flowOf(entities)

        val flow = repository.observeWebhooks()

        flow.collect { webhooks ->
            assertEquals(2, webhooks.size)
        }
    }

    // endregion

    // Helper functions

    private fun createTestWebhookEntity(id: String, enabled: Boolean = true) = WebhookEntity(
        id = id,
        ownerDeviceId = testDeviceId,
        name = "Test Webhook",
        targetUrl = "https://example.com/webhook",
        secret = "test-secret",
        enabled = enabled,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis(),
    )
}

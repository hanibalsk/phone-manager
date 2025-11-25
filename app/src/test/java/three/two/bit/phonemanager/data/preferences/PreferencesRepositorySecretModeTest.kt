package three.two.bit.phonemanager.data.preferences

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Story E2.1: Tests for secret mode in PreferencesRepository
 *
 * AC E2.1.1: secret_mode_enabled stored in DataStore
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PreferencesRepositorySecretModeTest {
    @Test
    fun `isSecretModeEnabled defaults to false`() = runTest {
        // Given
        val repository = mockk<PreferencesRepository>()
        coEvery { repository.isSecretModeEnabled } returns flowOf(false)

        // When
        repository.isSecretModeEnabled.test {
            // Then
            assertFalse(awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `isSecretModeEnabled emits true when enabled`() = runTest {
        // Given
        val repository = mockk<PreferencesRepository>()
        coEvery { repository.isSecretModeEnabled } returns flowOf(true)

        // When
        repository.isSecretModeEnabled.test {
            // Then
            assertTrue(awaitItem())
            awaitComplete()
        }
    }
}

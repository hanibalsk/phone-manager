package three.two.bit.phonemanager.ui.home

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Before
import org.junit.Test
import three.two.bit.phonemanager.data.preferences.PreferencesRepository
import three.two.bit.phonemanager.data.repository.GroupRepository
import three.two.bit.phonemanager.data.repository.TripRepository
import three.two.bit.phonemanager.trip.TripManager
import kotlin.test.assertFalse

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private lateinit var preferencesRepository: PreferencesRepository
    private lateinit var tripManager: TripManager
    private lateinit var tripRepository: TripRepository
    private lateinit var groupRepository: GroupRepository
    private lateinit var viewModel: HomeViewModel
    private lateinit var secretModeFlow: MutableStateFlow<Boolean>

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        preferencesRepository = mockk(relaxed = true)
        tripManager = mockk(relaxed = true)
        tripRepository = mockk(relaxed = true)
        groupRepository = mockk(relaxed = true)
        secretModeFlow = MutableStateFlow(false)
        coEvery { preferencesRepository.isSecretModeEnabled } returns secretModeFlow
        every { groupRepository.hasAdminAccess } returns kotlinx.coroutines.flow.flowOf(false)
        every { groupRepository.adminGroups } returns kotlinx.coroutines.flow.flowOf(emptyList())
    }

    @Test
    fun `isSecretModeEnabled emits false by default`() = runTest {
        // When
        viewModel = HomeViewModel(preferencesRepository, tripManager, tripRepository, groupRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertFalse(viewModel.isSecretModeEnabled.value)
    }

    @Test
    fun `toggleSecretMode calls setSecretModeEnabled with opposite value when false`() = runTest {
        // Given
        secretModeFlow.value = false
        viewModel = HomeViewModel(preferencesRepository, tripManager, tripRepository, groupRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.toggleSecretMode()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { preferencesRepository.setSecretModeEnabled(true) }
    }

    @Test
    fun `toggleSecretMode does not throw when called`() = runTest {
        // Given
        secretModeFlow.value = false
        viewModel = HomeViewModel(preferencesRepository, tripManager, tripRepository, groupRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        // When - should not throw
        viewModel.toggleSecretMode()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - verify it was called (already tested above)
        coVerify(exactly = 1) { preferencesRepository.setSecretModeEnabled(any()) }
    }
}

package three.two.bit.phonemanager.ui.group

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import three.two.bit.phonemanager.data.repository.DeviceRepository
import three.two.bit.phonemanager.domain.model.Device
import javax.inject.Inject

/**
 * Story E1.2: ViewModel for Group Members screen
 *
 * Manages UI state for displaying devices in the same group
 * ACs: E1.2.3, E1.2.4, E1.2.5
 */
@HiltViewModel
class GroupMembersViewModel @Inject constructor(private val deviceRepository: DeviceRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(GroupMembersUiState())
    val uiState: StateFlow<GroupMembersUiState> = _uiState.asStateFlow()

    init {
        loadGroupMembers()
    }

    /**
     * Load group members from repository
     *
     * AC E1.2.3: Fetch and display group members
     * AC E1.2.4: Handle empty group state
     * AC E1.2.5: Handle errors with retry capability
     */
    fun loadGroupMembers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            deviceRepository.getGroupMembers().fold(
                onSuccess = { devices ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            members = devices,
                            isEmpty = devices.isEmpty(),
                            error = null,
                        )
                    }
                },
                onFailure = { exception ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = exception.message ?: "Failed to load group members",
                        )
                    }
                },
            )
        }
    }

    /**
     * Refresh group members (pull-to-refresh)
     *
     * AC E1.2.6: Pull-to-refresh functionality
     */
    fun refresh() {
        loadGroupMembers()
    }
}

/**
 * UI State for Group Members screen
 *
 * @property members List of devices in the group (excluding current device)
 * @property isLoading True when fetching data
 * @property error Error message if fetch failed, null otherwise
 * @property isEmpty True when no other devices in group
 */
data class GroupMembersUiState(
    val members: List<Device> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isEmpty: Boolean = false,
)

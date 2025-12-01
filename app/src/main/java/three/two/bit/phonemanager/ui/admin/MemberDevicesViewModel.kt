package three.two.bit.phonemanager.ui.admin

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import three.two.bit.phonemanager.data.repository.AdminSettingsRepository
import three.two.bit.phonemanager.domain.model.DeviceStatusFilter
import three.two.bit.phonemanager.domain.model.MemberDeviceSettings
import timber.log.Timber
import javax.inject.Inject

/**
 * Story E12.7: ViewModel for Member Devices Screen
 *
 * Manages the list of member devices in a group for admin settings management.
 *
 * AC E12.7.1: Device Settings List Screen
 * - List all member devices
 * - Search and filter devices
 * - Show online/offline status
 * - Navigate to device settings
 */
@HiltViewModel
class MemberDevicesViewModel @Inject constructor(
    private val adminSettingsRepository: AdminSettingsRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val groupId: String = savedStateHandle.get<String>("groupId")
        ?: throw IllegalArgumentException("groupId is required")

    private val _uiState = MutableStateFlow(MemberDevicesUiState())
    val uiState: StateFlow<MemberDevicesUiState> = _uiState.asStateFlow()

    init {
        loadMemberDevices()
    }

    /**
     * Load member devices for the group.
     * AC E12.7.1: Fetch device list
     */
    fun loadMemberDevices() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            adminSettingsRepository.getMemberDevices(groupId).fold(
                onSuccess = { devices ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            devices = devices,
                            filteredDevices = applyFilters(devices, it.searchQuery, it.statusFilter),
                        )
                    }
                    Timber.i("Loaded ${devices.size} member devices for group $groupId")
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Failed to load devices",
                        )
                    }
                    Timber.e(error, "Failed to load member devices")
                },
            )
        }
    }

    /**
     * Update search query and filter devices.
     * AC E12.7.1: Search functionality
     */
    fun onSearchQueryChanged(query: String) {
        _uiState.update { state ->
            state.copy(
                searchQuery = query,
                filteredDevices = applyFilters(state.devices, query, state.statusFilter),
            )
        }
    }

    /**
     * Update status filter.
     * AC E12.7.1: Filter by online/offline
     */
    fun onStatusFilterChanged(filter: DeviceStatusFilter) {
        _uiState.update { state ->
            state.copy(
                statusFilter = filter,
                filteredDevices = applyFilters(state.devices, state.searchQuery, filter),
            )
        }
    }

    /**
     * Toggle device selection for bulk operations.
     * AC E12.7.6: Bulk Settings Application
     */
    fun toggleDeviceSelection(deviceId: String) {
        _uiState.update { state ->
            val selectedDevices = state.selectedDevices.toMutableSet()
            if (selectedDevices.contains(deviceId)) {
                selectedDevices.remove(deviceId)
            } else {
                selectedDevices.add(deviceId)
            }
            state.copy(selectedDevices = selectedDevices)
        }
    }

    /**
     * Select all visible devices.
     */
    fun selectAllDevices() {
        _uiState.update { state ->
            state.copy(
                selectedDevices = state.filteredDevices.map { it.deviceId }.toSet(),
            )
        }
    }

    /**
     * Clear all device selection.
     */
    fun clearSelection() {
        _uiState.update { it.copy(selectedDevices = emptySet()) }
    }

    /**
     * Enter bulk edit mode.
     */
    fun enterBulkEditMode() {
        _uiState.update { it.copy(isInBulkEditMode = true) }
    }

    /**
     * Exit bulk edit mode.
     */
    fun exitBulkEditMode() {
        _uiState.update {
            it.copy(
                isInBulkEditMode = false,
                selectedDevices = emptySet(),
            )
        }
    }

    /**
     * Clear error message.
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Refresh device list.
     */
    fun refresh() {
        loadMemberDevices()
    }

    /**
     * Apply search and status filters to device list.
     */
    private fun applyFilters(
        devices: List<MemberDeviceSettings>,
        searchQuery: String,
        statusFilter: DeviceStatusFilter,
    ): List<MemberDeviceSettings> {
        return devices.filter { device ->
            // Apply search filter
            val matchesSearch = searchQuery.isBlank() ||
                device.deviceName.contains(searchQuery, ignoreCase = true) ||
                device.ownerName.contains(searchQuery, ignoreCase = true) ||
                device.ownerEmail.contains(searchQuery, ignoreCase = true)

            // Apply status filter
            val matchesStatus = when (statusFilter) {
                DeviceStatusFilter.ALL -> true
                DeviceStatusFilter.ONLINE -> device.isOnline
                DeviceStatusFilter.OFFLINE -> !device.isOnline
            }

            matchesSearch && matchesStatus
        }
    }
}

/**
 * UI state for Member Devices screen.
 */
data class MemberDevicesUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val devices: List<MemberDeviceSettings> = emptyList(),
    val filteredDevices: List<MemberDeviceSettings> = emptyList(),
    val searchQuery: String = "",
    val statusFilter: DeviceStatusFilter = DeviceStatusFilter.ALL,
    val selectedDevices: Set<String> = emptySet(),
    val isInBulkEditMode: Boolean = false,
) {
    val selectedCount: Int get() = selectedDevices.size
    val hasSelection: Boolean get() = selectedDevices.isNotEmpty()
    val onlineCount: Int get() = devices.count { it.isOnline }
    val offlineCount: Int get() = devices.count { !it.isOnline }
}

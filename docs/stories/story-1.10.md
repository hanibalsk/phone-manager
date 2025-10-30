# Story 1.10: Service Control UI

**Status:** Ready for Implementation
**Epic:** 1 - Background Location Tracking Service
**Priority:** Must Have (MVP)
**Complexity:** Medium
**Estimated Effort:** 3 Story Points (1-2 days)

## Story

As a device user,
I want to control the location tracking service from the app settings,
so that I can enable/disable tracking and view service status.

## Acceptance Criteria

### AC1: Service Toggle
**Given** the settings screen is opened
**When** viewing service controls
**Then** a toggle switch must be displayed to enable/disable location tracking
**And** the toggle state must reflect the current service status (running/stopped)

### AC2: Service Status Display
**Given** the settings screen is opened
**When** viewing service status
**Then** current status must show one of:
- "Running" (green indicator) - Service is actively tracking
- "Stopped" (gray indicator) - Service is not running
- "Error" (red indicator) - Service failed to start with error description

### AC3: Last Transmission Time
**Given** location data has been successfully transmitted to the server
**When** viewing settings
**Then** the timestamp of last successful transmission must be displayed
**And** the format must be user-friendly (e.g., "5 minutes ago", "Today at 2:30 PM")
**And** if no transmission occurred yet, display "No data transmitted yet"

### AC4: Permission Status Indicator
**Given** location permissions are required for the service
**When** viewing settings
**Then** permission status must be clearly indicated:
- "All Permissions Granted" (green check) - Fine + Background location granted
- "Foreground Only" (yellow warning) - Only fine location granted
- "Denied" (red X) - Permissions not granted
**And** a "Grant Permissions" button must be shown if permissions are missing

### AC5: Service Control Actions
**Given** the service toggle is switched
**When** enabling the service:
- **Then** app must check permissions first
- **And** if permissions missing, show permission request
- **And** if permissions granted, start the service
- **And** show success/error message
**When** disabling the service:
- **Then** service must stop gracefully
- **And** notification must be removed
- **And** show confirmation message "Location tracking stopped"

### AC6: Settings Navigation
**Given** user wants to access service controls
**When** navigating the app
**Then** settings screen must be accessible from navigation menu or home screen
**And** settings icon/button must be clearly visible

## Tasks / Subtasks

### Task 1: Create Service State Domain Models
- [ ] Create service state models in `domain/model/`:
  ```kotlin
  data class ServiceState(
      val isRunning: Boolean,
      val lastTransmissionTime: Long?,
      val permissionStatus: PermissionStatus,
      val errorMessage: String? = null
  )

  enum class PermissionStatus {
      GRANTED_ALL,          // Fine + Background
      GRANTED_FOREGROUND,   // Fine only
      DENIED                // No permissions
  }
  ```

### Task 2: Create Location Service Manager
- [ ] Create `LocationServiceManager` in `data/service/`:
  ```kotlin
  interface LocationServiceManager {
      fun startService()
      fun stopService()
      fun isServiceRunning(): Boolean
      fun observeServiceState(): Flow<ServiceState>
  }
  ```
- [ ] Implement service control logic
- [ ] Handle service intents (START_SERVICE, STOP_SERVICE)
- [ ] Implement state observation

### Task 3: Create Service Control Use Cases
- [ ] Create `StartLocationServiceUseCase`:
  ```kotlin
  class StartLocationServiceUseCase @Inject constructor(
      private val serviceManager: LocationServiceManager,
      private val permissionRepository: PermissionRepository
  ) {
      suspend operator fun invoke(): Result<Unit> {
          return when (permissionRepository.checkPermissions()) {
              PermissionStatus.GRANTED_ALL -> {
                  serviceManager.startService()
                  Result.Success(Unit)
              }
              else -> Result.Error("Permissions not granted")
          }
      }
  }
  ```
- [ ] Create `StopLocationServiceUseCase`
- [ ] Create `GetServiceStateUseCase`
- [ ] Create `GetLastTransmissionTimeUseCase`

### Task 4: Create Settings ViewModel
- [ ] Create `SettingsViewModel` in `presentation/settings/`:
  ```kotlin
  @HiltViewModel
  class SettingsViewModel @Inject constructor(
      private val startServiceUseCase: StartLocationServiceUseCase,
      private val stopServiceUseCase: StopLocationServiceUseCase,
      private val getServiceStateUseCase: GetServiceStateUseCase,
      private val permissionRepository: PermissionRepository
  ) : ViewModel() {

      private val _serviceState = MutableStateFlow(ServiceState())
      val serviceState: StateFlow<ServiceState> = _serviceState.asStateFlow()

      private val _uiState = MutableStateFlow(SettingsUiState())
      val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

      init {
          observeServiceState()
      }

      fun startService() {
          viewModelScope.launch {
              when (val result = startServiceUseCase()) {
                  is Result.Success -> {
                      _uiState.update {
                          it.copy(message = "Location tracking started")
                      }
                  }
                  is Result.Error -> {
                      _uiState.update {
                          it.copy(error = result.message)
                      }
                  }
              }
          }
      }

      fun stopService() {
          viewModelScope.launch {
              stopServiceUseCase()
              _uiState.update {
                  it.copy(message = "Location tracking stopped")
              }
          }
      }

      fun requestPermissions() {
          // Trigger permission request
      }

      private fun observeServiceState() {
          viewModelScope.launch {
              getServiceStateUseCase()
                  .collect { state ->
                      _serviceState.value = state
                  }
          }
      }
  }
  ```

### Task 5: Create Settings UI Screen
- [ ] Create `SettingsScreen` composable in `presentation/settings/`:
  - Service toggle switch
  - Service status card
  - Permission status section
  - Last transmission time display
  - Grant permissions button (if needed)
  - Error/success message snackbar

### Task 6: Create UI Components
- [ ] Create `ServiceStatusCard` composable:
  ```kotlin
  @Composable
  fun ServiceStatusCard(
      isRunning: Boolean,
      errorMessage: String?,
      modifier: Modifier = Modifier
  ) {
      Card(modifier = modifier.fillMaxWidth()) {
          Row(
              modifier = Modifier.padding(16.dp),
              verticalAlignment = Alignment.CenterVertically
          ) {
              StatusIndicator(
                  color = when {
                      errorMessage != null -> Color.Red
                      isRunning -> Color.Green
                      else -> Color.Gray
                  }
              )
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                  Text(
                      text = when {
                          errorMessage != null -> "Error"
                          isRunning -> "Running"
                          else -> "Stopped"
                      },
                      style = MaterialTheme.typography.titleMedium
                  )
                  if (errorMessage != null) {
                      Text(
                          text = errorMessage,
                          style = MaterialTheme.typography.bodySmall,
                          color = Color.Red
                      )
                  }
              }
          }
      }
  }
  ```

- [ ] Create `PermissionStatusSection` composable
- [ ] Create `LastTransmissionDisplay` composable
- [ ] Create `ServiceToggleSwitch` composable

### Task 7: Implement Permission Request Flow
- [ ] Use Accompanist Permissions library
- [ ] Create permission request launcher
- [ ] Handle permission results
- [ ] Show rationale dialogs
- [ ] Navigate to app settings if permanently denied

### Task 8: Add Navigation
- [ ] Add settings route to navigation graph
- [ ] Create settings navigation icon/button
- [ ] Implement navigation to settings screen
- [ ] Add back navigation

### Task 9: Implement Last Transmission Time Formatting
- [ ] Create utility function for relative time formatting:
  ```kotlin
  fun formatRelativeTime(timestamp: Long?): String {
      if (timestamp == null) return "No data transmitted yet"

      val now = System.currentTimeMillis()
      val diff = now - timestamp

      return when {
          diff < 60_000 -> "Just now"
          diff < 3600_000 -> "${diff / 60_000} minutes ago"
          diff < 86400_000 -> {
              val hours = diff / 3600_000
              if (hours == 1L) "1 hour ago" else "$hours hours ago"
          }
          diff < 172800_000 -> "Yesterday"
          else -> {
              val formatter = SimpleDateFormat("MMM dd 'at' h:mm a", Locale.getDefault())
              formatter.format(Date(timestamp))
          }
      }
  }
  ```

### Task 10: Add Dependency Injection
- [ ] Create `SettingsModule` in `di/`:
  ```kotlin
  @Module
  @InstallIn(SingletonComponent::class)
  abstract class SettingsModule {

      @Binds
      @Singleton
      abstract fun bindLocationServiceManager(
          impl: LocationServiceManagerImpl
      ): LocationServiceManager
  }
  ```

### Task 11: Testing
- [ ] Write unit tests:
  - `SettingsViewModelTest`: Test service start/stop, state updates
  - `StartLocationServiceUseCaseTest`: Test permission checks, service starting
  - `StopLocationServiceUseCaseTest`: Test service stopping
  - `LocationServiceManagerTest`: Test service control logic
- [ ] Write UI tests:
  - `SettingsScreenTest`: Test toggle interactions, button clicks
  - `ServiceStatusCardTest`: Test different status displays
  - `PermissionStatusSectionTest`: Test permission indicators
- [ ] Manual testing:
  - Toggle service on/off
  - Verify service starts and notification appears
  - Verify service stops and notification disappears
  - Test permission request flow
  - Test error scenarios (no permissions, service failure)
  - Verify last transmission time updates correctly

### Task 12: Documentation
- [ ] Add KDoc to all public APIs
- [ ] Document service control flow
- [ ] Create UI component documentation
- [ ] Update architecture documentation

## Technical Details

### Settings Screen Layout

```kotlin
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val serviceState by viewModel.serviceState.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Settings") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Service Toggle Section
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Location Tracking",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Switch(
                        checked = serviceState.isRunning,
                        onCheckedChange = { enabled ->
                            if (enabled) viewModel.startService()
                            else viewModel.stopService()
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Service Status Card
            ServiceStatusCard(
                isRunning = serviceState.isRunning,
                errorMessage = serviceState.errorMessage
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Permission Status Section
            PermissionStatusSection(
                permissionStatus = serviceState.permissionStatus,
                onRequestPermissions = { viewModel.requestPermissions() }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Last Transmission Time
            LastTransmissionDisplay(
                timestamp = serviceState.lastTransmissionTime
            )

            // Snackbar for messages
            if (uiState.message != null) {
                LaunchedEffect(uiState.message) {
                    // Show snackbar
                }
            }
        }
    }
}
```

### Service Manager Implementation

```kotlin
class LocationServiceManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val serviceStateRepository: ServiceStateRepository
) : LocationServiceManager {

    override fun startService() {
        val intent = Intent(context, LocationTrackingService::class.java).apply {
            action = ACTION_START_SERVICE
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    override fun stopService() {
        val intent = Intent(context, LocationTrackingService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        context.startService(intent)
    }

    override fun isServiceRunning(): Boolean {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        @Suppress("DEPRECATION")
        return manager.getRunningServices(Int.MAX_VALUE)
            .any { it.service.className == LocationTrackingService::class.java.name }
    }

    override fun observeServiceState(): Flow<ServiceState> {
        return serviceStateRepository.observeServiceState()
    }

    companion object {
        const val ACTION_START_SERVICE = "ACTION_START_SERVICE"
        const val ACTION_STOP_SERVICE = "ACTION_STOP_SERVICE"
    }
}
```

## Definition of Done

- [ ] All acceptance criteria met
- [ ] Settings screen created with all required components
- [ ] Service toggle functional (start/stop)
- [ ] Service status displayed accurately
- [ ] Permission status indicators working
- [ ] Last transmission time displayed correctly
- [ ] Permission request flow implemented
- [ ] Navigation to settings working
- [ ] Unit tests written and passing (>80% coverage)
- [ ] UI tests passing
- [ ] Manual testing completed
- [ ] Code reviewed and approved
- [ ] Documentation updated
- [ ] No lint warnings
- [ ] Accessibility features implemented (content descriptions, semantics)

## Dependencies

**Blocks:**
- None (completes MVP)

**Blocked By:**
- Story 1.1: Permission Management (needs permission repository) ✅
- Story 1.4: Server Configuration (needs configuration access) ✅
- Story 1.5: Network Communication (needs last transmission time) ✅

## Risks & Mitigations

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| Service state sync issues | Medium | Medium | Use StateFlow for reactive updates, test thoroughly |
| Permission request complexity | High | Medium | Use Accompanist Permissions, implement clear flow |
| UI state management errors | Low | Low | Use sealed classes for UI state, comprehensive testing |
| Navigation integration issues | Low | Low | Follow Compose Navigation best practices |

## Testing Strategy

### Unit Tests
- `SettingsViewModelTest`:
  - Test service start with permissions granted
  - Test service start with permissions denied
  - Test service stop
  - Test state observation
- `StartLocationServiceUseCaseTest`:
  - Test permission check logic
  - Test service start success/failure
- `LocationServiceManagerTest`:
  - Test service control methods
  - Test service state observation

### UI Tests
- `SettingsScreenTest`:
  - Test toggle switch interaction
  - Test permission request button
  - Test status displays
  - Test snackbar messages
- `ServiceStatusCardTest`:
  - Test running state display
  - Test stopped state display
  - Test error state display

### Integration Tests
- Test full service control flow (start → running → stop)
- Test permission request flow
- Test state persistence across screen navigation

### Manual Testing
- [ ] Toggle service on - verify service starts and notification shows
- [ ] Toggle service off - verify service stops and notification disappears
- [ ] Test with all permissions granted
- [ ] Test with only foreground permission
- [ ] Test with no permissions
- [ ] Verify last transmission time updates after successful transmission
- [ ] Test error scenarios (service failure, permission denial)
- [ ] Test on Android 10, 12, 14

## Notes

### Service State Observation
- Use `Flow<ServiceState>` for reactive state updates
- Service state should update immediately when service starts/stops
- Last transmission time should update from network layer (Story 1.5)

### Permission Request Best Practices
- Always explain why permissions are needed before requesting
- Handle "Don't ask again" scenario with settings navigation
- Show clear permission status indicators
- Provide actionable buttons to request permissions

### UI/UX Considerations
- Use Material Design 3 components
- Provide clear visual feedback for actions (success/error messages)
- Use appropriate colors for status indicators (green/yellow/red)
- Ensure accessibility (content descriptions, large tap targets)
- Support dark theme

### Future Enhancements
- Add battery usage estimate display
- Add data usage statistics
- Add location update interval configuration shortcut
- Add quick toggle widget for home screen

## References

- [Compose Material3](https://developer.android.com/jetpack/androidx/releases/compose-material3)
- [Accompanist Permissions](https://google.github.io/accompanist/permissions/)
- [Android Services](https://developer.android.com/guide/components/services)
- [Hilt ViewModel](https://developer.android.com/training/dependency-injection/hilt-jetpack)

---

**Story Created:** 2025-10-30
**Created By:** BMAD Requirements Analyst
**Epic:** [Epic 1: Background Location Tracking Service](../epics/epic-1-location-tracking.md)
**Depends On:** [Story 1.1](./story-1.1.md), [Story 1.4](./story-1.4.md), [Story 1.5](./story-1.5.md)

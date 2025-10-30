# Story 1.1: Permission Management & Architecture Foundation

**Status:** Ready for Implementation
**Epic:** 1 - Background Location Tracking Service
**Priority:** MVP - Critical Path (Blocks all other stories)
**Complexity:** Medium
**Estimated Effort:** 3-5 days

## Story

As a developer,
I want to establish runtime permission handling and Clean Architecture foundation,
so that the app can properly request location permissions and has a solid architectural structure for future features.

## Acceptance Criteria

1. **Permission Flow:**
   - [ ] App requests ACCESS_FINE_LOCATION and ACCESS_COARSE_LOCATION on first launch
   - [ ] Background location permission (ACCESS_BACKGROUND_LOCATION) requested after foreground granted (Android 10+)
   - [ ] User can grant/deny permissions with appropriate feedback
   - [ ] Permission rationale displayed before requests (as required by Android guidelines)
   - [ ] Permission state persists across app restarts

2. **Architecture Layers:**
   - [ ] Clean Architecture layers properly structured (Domain, Data, Presentation)
   - [ ] Hilt dependency injection modules configured for all layers
   - [ ] Base repository interface pattern established
   - [ ] Base use case pattern established
   - [ ] Package structure follows documented conventions

3. **Permission Management:**
   - [ ] PermissionManager utility class handles all permission logic
   - [ ] Permission states tracked using StateFlow
   - [ ] Educational dialogs explain why permissions are needed
   - [ ] Graceful handling of "Don't ask again" scenario
   - [ ] Deep link to app settings for manual permission grant

4. **Testing:**
   - [ ] Unit tests for permission state management logic
   - [ ] Instrumentation tests for permission dialog flows
   - [ ] Tested on Android 10, 12, and 14

## Tasks / Subtasks

### Task 1: Configure Hilt Dependency Injection
- [ ] Add Hilt dependencies to build.gradle.kts
  - `com.google.dagger:hilt-android:2.51`
  - `com.google.dagger:hilt-android-compiler:2.51` (kapt)
  - `androidx.hilt:hilt-navigation-compose:1.2.0`
- [ ] Apply Hilt plugins to app module
- [ ] Annotate PhoneManagerApp with `@HiltAndroidApp`
- [ ] Create base Hilt modules structure in `three.two.bit.phone.manager.di` package

### Task 2: Establish Clean Architecture Package Structure
- [ ] Create domain layer packages:
  - `three.two.bit.phone.manager.feature.location.domain.model`
  - `three.two.bit.phone.manager.feature.location.domain.repository`
  - `three.two.bit.phone.manager.feature.location.domain.usecase`
- [ ] Create data layer packages:
  - `three.two.bit.phone.manager.feature.location.data.repository`
  - `three.two.bit.phone.manager.feature.location.data.source`
  - `three.two.bit.phone.manager.feature.location.data.model`
- [ ] Create presentation layer packages:
  - `three.two.bit.phone.manager.feature.location.presentation.ui`
  - `three.two.bit.phone.manager.feature.location.presentation.viewmodel`
  - `three.two.bit.phone.manager.feature.location.presentation.model`

### Task 3: Implement Permission Management
- [ ] Add permission declarations to AndroidManifest.xml:
  ```xml
  <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
  <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
  <uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />
  ```
- [ ] Create `PermissionManager` class in `three.two.bit.phone.manager.core.permission`
- [ ] Implement permission state data classes:
  - `PermissionState` sealed interface
  - `LocationPermissionStatus` data class
- [ ] Implement permission request logic with proper Android version handling
- [ ] Add Accompanist Permissions dependency: `com.google.accompanist:accompanist-permissions:0.34.0`

### Task 4: Create Permission UI Flow
- [ ] Create `PermissionRationaleDialog` composable
- [ ] Create `PermissionDeniedDialog` composable with settings navigation
- [ ] Implement permission request in `LocationScreen` composable
- [ ] Add permission status indicators
- [ ] Implement deep link to app settings

### Task 5: Create Base Architecture Patterns
- [ ] Create base `Repository` interface pattern
- [ ] Create base `UseCase` abstract class/interface
- [ ] Create `Result` sealed interface for error handling:
  ```kotlin
  sealed interface Result<out T> {
      data class Success<T>(val data: T) : Result<T>
      data class Error(val message: String, val exception: Throwable? = null) : Result<Nothing>
  }
  ```
- [ ] Create extension functions for Result type

### Task 6: Implement Dependency Injection Modules
- [ ] Create `AppModule` for application-level dependencies
- [ ] Create `PermissionModule` for permission manager
- [ ] Configure Hilt for ViewModel injection
- [ ] Document DI module conventions

### Task 7: Testing
- [ ] Write unit tests for `PermissionManager`:
  - Test permission state transitions
  - Test Android version-specific logic
  - Test "Don't ask again" scenario handling
- [ ] Write instrumentation tests:
  - Test permission dialog appears
  - Test permission grant flow
  - Test permission denial flow
  - Test background permission flow (Android 10+)
- [ ] Manual testing on Android 10, 12, 14 devices

### Task 8: Documentation
- [ ] Document permission flow in code comments
- [ ] Add KDoc to public APIs
- [ ] Update ARCHITECTURE.md with actual package structure
- [ ] Create permission flow diagram

## Technical Details

### Dependencies to Add

```kotlin
// build.gradle.kts (app)
dependencies {
    // Hilt
    implementation("com.google.dagger:hilt-android:2.51")
    kapt("com.google.dagger:hilt-android-compiler:2.51")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Permissions
    implementation("com.google.accompanist:accompanist-permissions:0.34.0")

    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("io.mockk:mockk:1.13.11")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("com.google.truth:truth:1.4.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
```

### Permission Flow Logic

```kotlin
// Permission request sequence
1. Check if foreground location permission granted
   ├─ YES: Check background permission (Android 10+)
   │   ├─ YES: Enable tracking
   │   └─ NO: Request background permission
   └─ NO: Show rationale → Request foreground permission

2. Handle foreground permission result
   ├─ GRANTED: Proceed to background permission check
   ├─ DENIED: Show denial message
   └─ PERMANENTLY DENIED: Show settings dialog

3. Handle background permission result (Android 10+)
   ├─ GRANTED: Enable tracking
   ├─ DENIED: Explain limitation, offer foreground-only mode
   └─ PERMANENTLY DENIED: Show settings dialog
```

### Architecture Pattern Example

```kotlin
// Domain Layer - Use Case
class GetLocationPermissionStatusUseCase @Inject constructor(
    private val permissionRepository: PermissionRepository
) {
    operator fun invoke(): Flow<LocationPermissionStatus> {
        return permissionRepository.getPermissionStatus()
    }
}

// Data Layer - Repository Interface (Domain)
interface PermissionRepository {
    fun getPermissionStatus(): Flow<LocationPermissionStatus>
    suspend fun requestPermissions(): Result<Unit>
}

// Data Layer - Repository Implementation (Data)
class PermissionRepositoryImpl @Inject constructor(
    private val permissionManager: PermissionManager
) : PermissionRepository {
    override fun getPermissionStatus(): Flow<LocationPermissionStatus> {
        return permissionManager.permissionStateFlow
    }

    override suspend fun requestPermissions(): Result<Unit> {
        return try {
            permissionManager.requestLocationPermissions()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error("Failed to request permissions", e)
        }
    }
}

// Presentation Layer - ViewModel
@HiltViewModel
class LocationPermissionViewModel @Inject constructor(
    private val getLocationPermissionStatusUseCase: GetLocationPermissionStatusUseCase
) : ViewModel() {
    val permissionState = getLocationPermissionStatusUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = LocationPermissionStatus.Unknown
        )
}
```

## Definition of Done

- [ ] All acceptance criteria met
- [ ] Code review completed
- [ ] Unit tests written and passing (>80% coverage)
- [ ] Instrumentation tests passing
- [ ] Manual testing completed on Android 10, 12, 14
- [ ] Documentation updated
- [ ] No lint warnings
- [ ] Hilt configuration verified
- [ ] Package structure follows conventions
- [ ] Permission flows tested on multiple devices

## Dependencies

**Blocks:**
- Story 1.2 (requires permission framework)
- All other Epic 1 stories

**Blocked By:**
- Story 0.1 (Complete ✅)

## Risks & Mitigations

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| Android version differences | High | Medium | Version-specific handling, comprehensive testing |
| OEM-specific permission behavior | Medium | Medium | Test on Samsung, Xiaomi, Pixel devices |
| User denies permissions | High | High | Clear rationale, graceful degradation |
| Hilt configuration complexity | Low | Medium | Follow official documentation, use Android Studio wizard |

## Testing Strategy

### Unit Tests
- `PermissionManagerTest`: Test state transitions, version handling
- `GetLocationPermissionStatusUseCaseTest`: Test use case logic
- `PermissionRepositoryImplTest`: Test repository with mocked PermissionManager

### Integration Tests
- `PermissionFlowTest`: Test full permission request flow
- `PermissionDialogTest`: Test dialog appearance and interactions
- `SettingsNavigationTest`: Test deep link to settings

### Manual Testing Checklist
- [ ] Test on Android 10 (background permission separate)
- [ ] Test on Android 12 (approximate location option)
- [ ] Test on Android 14 (latest restrictions)
- [ ] Test "Don't ask again" scenario
- [ ] Test settings navigation
- [ ] Test app restart with granted permissions
- [ ] Test app restart with denied permissions
- [ ] Test on multiple OEMs (Samsung, Xiaomi, Pixel)

## Notes

### Android Version-Specific Behavior

**Android 10 (API 29)+:**
- Background location must be requested separately from foreground
- User sees "Allow all the time" option
- System may show additional prompts for background usage

**Android 12 (API 31)+:**
- User can choose "Approximate" vs "Precise" location
- System periodically prompts users to review background permissions

**Android 14 (API 34)+:**
- Stricter foreground service requirements
- Enhanced permission rationale requirements

### Best Practices
1. Always request foreground permissions before background
2. Explain why each permission is needed before requesting
3. Handle denial gracefully with alternative workflows
4. Provide clear path to settings for manual grant
5. Respect user's decision if they permanently deny
6. Test on real devices, not just emulators
7. Consider battery impact in permission explanations

## References

- [Android Permissions Best Practices](https://developer.android.com/training/permissions/requesting)
- [Request Location Permissions](https://developer.android.com/training/location/permissions)
- [Accompanist Permissions](https://google.github.io/accompanist/permissions/)
- [Hilt Documentation](https://dagger.dev/hilt/)
- BMAD Technical Evaluation Report
- `/home/user/phone-manager/ARCHITECTURE.md`

---

**Story Created:** 2025-10-30
**Created By:** BMAD Epic Optimizer + Requirements Analyst
**Epic:** [Epic 1: Background Location Tracking Service](../epics/epic-1-location-tracking.md)

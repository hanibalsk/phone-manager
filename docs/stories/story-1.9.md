# Story 1.9: Device Identification and Initialization

**Status:** Ready for Implementation
**Epic:** 1 - Background Location Tracking Service
**Priority:** Must Have (MVP) - BLOCKS Story 1.5
**Complexity:** Low
**Estimated Effort:** 3 Story Points (1-2 days)

## Story

As a system administrator,
I want each device to have a unique identifier,
so that I can distinguish location data from different devices on the server.

## Acceptance Criteria

### AC1: Unique Device ID Generation
**Given** the app is launched for the first time
**When** device identification is initialized
**Then** a unique UUID must be generated and persisted
**And** the same UUID must be used for all subsequent app launches

### AC2: Device ID Persistence
**Given** a device ID has been generated
**When** the app is restarted or the device reboots
**Then** the same device ID must be retrieved and used
**And** the device ID must remain consistent across app updates

### AC3: Device Metadata Collection
**Given** device identification is initialized
**When** collecting device information
**Then** metadata must include:
- Device ID (UUID)
- Device model (e.g., "Pixel 7 Pro")
- Android version (e.g., "14")
- App version (e.g., "1.0.0")
- Creation timestamp

### AC4: Device ID in Network Requests
**Given** location data is being transmitted (Story 1.5)
**When** creating the JSON payload
**Then** the device ID must be included in every request
**And** the device ID field must be named "deviceId"

### AC5: Secure Storage
**Given** device information is stored locally
**When** persisting the data
**Then** EncryptedSharedPreferences or DataStore must be used
**And** device ID must be non-exportable (no backup)

## Tasks / Subtasks

### Task 1: Create Device Info Domain Model
- [ ] Create `DeviceInfo` data class in `domain/model/`:
  ```kotlin
  data class DeviceInfo(
      val deviceId: String,           // UUID
      val deviceModel: String,        // Build.MODEL
      val androidVersion: String,     // Build.VERSION.RELEASE
      val appVersion: String,         // BuildConfig.VERSION_NAME
      val createdAt: Long            // Timestamp in milliseconds
  )
  ```

### Task 2: Create Device Repository Interface
- [ ] Create `DeviceRepository` interface in `domain/repository/`:
  ```kotlin
  interface DeviceRepository {
      suspend fun getDeviceInfo(): Result<DeviceInfo>
      suspend fun getDeviceId(): Result<String>
      suspend fun initializeDevice(): Result<DeviceInfo>
      fun isDeviceInitialized(): Boolean
  }
  ```

### Task 3: Implement Device Repository
- [ ] Create `DeviceRepositoryImpl` in `data/repository/`:
  - Inject DataStore or EncryptedSharedPreferences
  - Implement UUID generation: `UUID.randomUUID().toString()`
  - Collect device metadata from Android Build class
  - Implement initialization check
  - Handle first launch vs. subsequent launches

### Task 4: Create Initialize Device Use Case
- [ ] Create `InitializeDeviceUseCase` in `domain/usecase/`:
  ```kotlin
  class InitializeDeviceUseCase @Inject constructor(
      private val deviceRepository: DeviceRepository
  ) {
      suspend operator fun invoke(): Result<DeviceInfo> {
          return if (deviceRepository.isDeviceInitialized()) {
              deviceRepository.getDeviceInfo()
          } else {
              deviceRepository.initializeDevice()
          }
      }
  }
  ```

### Task 5: Create Get Device ID Use Case
- [ ] Create `GetDeviceIdUseCase` in `domain/usecase/`:
  ```kotlin
  class GetDeviceIdUseCase @Inject constructor(
      private val deviceRepository: DeviceRepository
  ) {
      suspend operator fun invoke(): Result<String> {
          return deviceRepository.getDeviceId()
      }
  }
  ```

### Task 6: Implement Data Storage
- [ ] Add DataStore dependency if not present:
  ```kotlin
  implementation("androidx.datastore:datastore-preferences:1.0.0")
  ```
- [ ] Or use EncryptedSharedPreferences:
  ```kotlin
  implementation("androidx.security:security-crypto:1.1.0-alpha06")
  ```
- [ ] Create data storage wrapper class
- [ ] Implement save and retrieve operations
- [ ] Add exclusion from backups in AndroidManifest

### Task 7: Initialize on App Launch
- [ ] Update `PhoneManagerApp.onCreate()` to initialize device:
  ```kotlin
  @HiltAndroidApp
  class PhoneManagerApp : Application() {
      @Inject
      lateinit var initializeDeviceUseCase: InitializeDeviceUseCase

      override fun onCreate() {
          super.onCreate()

          // Initialize device ID on first launch
          lifecycleScope.launch {
              initializeDeviceUseCase()
          }
      }
  }
  ```

### Task 8: Create Dependency Injection Module
- [ ] Create `DeviceModule` in `di/`:
  ```kotlin
  @Module
  @InstallIn(SingletonComponent::class)
  abstract class DeviceModule {

      @Binds
      @Singleton
      abstract fun bindDeviceRepository(
          impl: DeviceRepositoryImpl
      ): DeviceRepository
  }
  ```

### Task 9: Testing
- [ ] Write unit tests:
  - `DeviceRepositoryImplTest`: Test initialization, retrieval, persistence
  - `InitializeDeviceUseCaseTest`: Test first launch vs. subsequent launches
  - `GetDeviceIdUseCaseTest`: Test device ID retrieval
- [ ] Write integration tests:
  - Test device ID remains consistent across app restarts
  - Test device ID is excluded from backups
  - Test metadata collection accuracy
- [ ] Manual testing:
  - Install app and verify device ID generated
  - Restart app and verify same device ID
  - Reinstall app and verify new device ID generated
  - Check device ID in logs

### Task 10: Documentation
- [ ] Add KDoc to all public APIs
- [ ] Document device initialization flow
- [ ] Add comments explaining UUID generation
- [ ] Update architecture documentation

## Technical Details

### Device ID Generation

```kotlin
class DeviceRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    @ApplicationContext private val context: Context
) : DeviceRepository {

    private val DEVICE_ID_KEY = stringPreferencesKey("device_id")
    private val DEVICE_MODEL_KEY = stringPreferencesKey("device_model")
    private val ANDROID_VERSION_KEY = stringPreferencesKey("android_version")
    private val APP_VERSION_KEY = stringPreferencesKey("app_version")
    private val CREATED_AT_KEY = longPreferencesKey("created_at")

    override suspend fun initializeDevice(): Result<DeviceInfo> {
        return try {
            val deviceId = UUID.randomUUID().toString()
            val deviceInfo = DeviceInfo(
                deviceId = deviceId,
                deviceModel = Build.MODEL,
                androidVersion = Build.VERSION.RELEASE,
                appVersion = getAppVersion(),
                createdAt = System.currentTimeMillis()
            )

            dataStore.edit { preferences ->
                preferences[DEVICE_ID_KEY] = deviceInfo.deviceId
                preferences[DEVICE_MODEL_KEY] = deviceInfo.deviceModel
                preferences[ANDROID_VERSION_KEY] = deviceInfo.androidVersion
                preferences[APP_VERSION_KEY] = deviceInfo.appVersion
                preferences[CREATED_AT_KEY] = deviceInfo.createdAt
            }

            Result.Success(deviceInfo)
        } catch (e: Exception) {
            Result.Error("Failed to initialize device", e)
        }
    }

    override suspend fun getDeviceInfo(): Result<DeviceInfo> {
        return try {
            val preferences = dataStore.data.first()
            val deviceInfo = DeviceInfo(
                deviceId = preferences[DEVICE_ID_KEY] ?: throw IllegalStateException("Device not initialized"),
                deviceModel = preferences[DEVICE_MODEL_KEY] ?: Build.MODEL,
                androidVersion = preferences[ANDROID_VERSION_KEY] ?: Build.VERSION.RELEASE,
                appVersion = preferences[APP_VERSION_KEY] ?: getAppVersion(),
                createdAt = preferences[CREATED_AT_KEY] ?: 0L
            )
            Result.Success(deviceInfo)
        } catch (e: Exception) {
            Result.Error("Failed to get device info", e)
        }
    }

    override suspend fun getDeviceId(): Result<String> {
        return try {
            val preferences = dataStore.data.first()
            val deviceId = preferences[DEVICE_ID_KEY]
                ?: throw IllegalStateException("Device not initialized")
            Result.Success(deviceId)
        } catch (e: Exception) {
            Result.Error("Failed to get device ID", e)
        }
    }

    override fun isDeviceInitialized(): Boolean {
        return runBlocking {
            val preferences = dataStore.data.first()
            preferences[DEVICE_ID_KEY] != null
        }
    }

    private fun getAppVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }
}
```

### Backup Exclusion (AndroidManifest.xml)

```xml
<application
    android:allowBackup="false"
    android:dataExtractionRules="@xml/data_extraction_rules"
    android:fullBackupContent="@xml/backup_rules"
    ...>
</application>
```

Add to `res/xml/backup_rules.xml`:
```xml
<exclude domain="sharedpref" path="datastore/device_prefs.preferences_pb" />
```

### JSON Payload Integration (Story 1.5)

```json
{
    "deviceId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "timestamp": 1698765432000,
    "latitude": 37.7749,
    "longitude": -122.4194,
    "accuracy": 10.5,
    "provider": "fused"
}
```

## Definition of Done

- [ ] All acceptance criteria met
- [ ] Device ID generated on first launch
- [ ] Device ID persists across app restarts
- [ ] Device metadata collected accurately
- [ ] Device ID included in network requests (verified with Story 1.5)
- [ ] Secure storage implemented
- [ ] Unit tests written and passing (>80% coverage)
- [ ] Integration tests passing
- [ ] Manual testing completed
- [ ] Code reviewed and approved
- [ ] Documentation updated
- [ ] No lint warnings

## Dependencies

**Blocks:**
- Story 1.5: Network Communication (needs deviceId in JSON payload)

**Blocked By:**
- Story 0.2: Architecture Foundation (needs DI and repository pattern) ✅ Must Complete First

## Risks & Mitigations

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| Device ID changes on reinstall | Expected | Low | Document this behavior, acceptable for fresh installs |
| UUID collision | Very Low | High | UUID v4 has 1 in 2^122 collision probability - negligible |
| Backup/restore issues | Low | Medium | Exclude from backups, generate new ID on restore |
| Device metadata access failure | Low | Low | Use fallback values ("unknown") |

## Testing Strategy

### Unit Tests
- `DeviceRepositoryImplTest`:
  - Test UUID generation format
  - Test data persistence
  - Test retrieval logic
  - Test initialization check
- `InitializeDeviceUseCaseTest`:
  - Test first launch scenario
  - Test subsequent launch scenario
- `GetDeviceIdUseCaseTest`:
  - Test successful retrieval
  - Test uninitialized state

### Integration Tests
- `DeviceInitializationTest`:
  - Test full initialization flow
  - Test consistency across app restarts
  - Test device info accuracy

### Manual Testing
- [ ] Install app fresh - verify device ID generated and logged
- [ ] Restart app multiple times - verify same device ID
- [ ] Clear app data - verify new device ID generated
- [ ] Check device model, Android version, app version are correct
- [ ] Verify device ID format is valid UUID

## Notes

### UUID Format
- Using UUID v4 (random)
- Format: `xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx`
- 36 characters including hyphens
- Globally unique with probability 1 - (n^2 / 2^122)

### Why Not Android ID?
- Android ID can change on factory reset
- Android ID may not be unique on some devices
- UUID provides better control and consistency

### Privacy Considerations
- Device ID is not personally identifiable information (PII)
- Device ID cannot be traced back to a user without additional data
- Device ID is stored locally and only transmitted to configured server
- No Google Analytics or third-party tracking

## References

- [UUID (Java)](https://docs.oracle.com/javase/8/docs/api/java/util/UUID.html)
- [DataStore](https://developer.android.com/topic/libraries/architecture/datastore)
- [Android Build](https://developer.android.com/reference/android/os/Build)
- [Backup and Restore](https://developer.android.com/guide/topics/data/backup)

---

**Story Created:** 2025-10-30
**Created By:** BMAD Requirements Analyst
**Epic:** [Epic 1: Background Location Tracking Service](../epics/epic-1-location-tracking.md)
**Depends On:** [Story 0.2: Architecture Foundation](./story-0.2.md)
**Blocks:** [Story 1.5: Network Communication](./story-1.5.md)

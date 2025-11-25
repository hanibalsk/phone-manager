# Story E1.1: Device Registration Flow

**Story ID**: E1.1
**Epic**: 1 - Device Registration & Groups
**Priority**: Must-Have (Critical Path)
**Estimate**: 3 story points (2-3 days)
**Status**: Ready for Review
**Created**: 2025-11-25
**PRD Reference**: Feature 6, FR-6.1.x (Section 5)

---

## User Story

```
AS A user
I WANT to register my device with a display name and group ID
SO THAT other devices in my group can identify me and see my location
```

---

## Business Value

- Enables multi-device location sharing - the foundation for all group features
- Establishes device identity for proximity alerts, geofencing, and history viewing
- Creates the basis for trusted group membership
- Required before any group-based feature can function

---

## Background / Problem Statement

The current implementation has `SecureStorage` with deviceId generation but lacks:
1. **No registration flow** - Device doesn't register with the backend server
2. **No displayName/groupId** - Can't identify devices or form groups
3. **No settings screen** - User can't configure device identity
4. **No API integration** - Missing `/api/devices/register` endpoint call

**Current State** (SecureStorage.kt):
- ✅ deviceId auto-generated (UUID)
- ✅ API key storage
- ❌ No displayName storage
- ❌ No groupId storage
- ❌ No server registration

---

## Acceptance Criteria

### AC E1.1.1: First Launch Registration Screen
**Given** I am launching the app for the first time
**When** the app detects no registration exists (no displayName/groupId stored)
**Then** the app should display a Registration screen
**And** the screen should show:
  - Display Name input field (required, 2-50 characters)
  - Group ID input field (required, 2-50 characters, alphanumeric + hyphen)
  - "Register" button (disabled until both fields valid)
  - Brief explanation of what group ID means

**Verification**: Fresh install → Registration screen appears first

---

### AC E1.1.2: Device Registration API Call
**Given** I have entered a valid display name and group ID
**When** I tap "Register"
**Then** the app should call `POST /api/devices/register` with payload:
```json
{
  "deviceId": "<generated-uuid>",
  "displayName": "<user-input>",
  "groupId": "<user-input>",
  "platform": "android"
}
```
**And** the X-API-Key header should be included
**And** a loading indicator should be shown during the request

**Verification**: Use network inspector to verify request payload and headers

---

### AC E1.1.3: Successful Registration Storage
**Given** the registration API returns HTTP 200/201
**When** the response is received
**Then** displayName should be stored in SecureStorage
**And** groupId should be stored in SecureStorage
**And** the user should navigate to the Home screen
**And** a success toast should appear: "Device registered successfully"

**Verification**: Check SecureStorage values after registration → all fields populated

---

### AC E1.1.4: Registration Error Handling
**Given** the registration API fails (network error, server error)
**When** the error response is received
**Then** an error message should be displayed to the user
**And** the Register button should be re-enabled
**And** the user should remain on the Registration screen
**And** the error should be logged via Timber

**Error Messages**:
- Network unavailable: "No internet connection. Please check your network."
- Server error (5xx): "Server error. Please try again later."
- Client error (4xx): "Registration failed: {error_message}"

**Verification**: Disable network → attempt registration → error shown

---

### AC E1.1.5: Skip Registration When Already Registered
**Given** displayName and groupId already exist in SecureStorage
**When** the app launches
**Then** the Registration screen should NOT appear
**And** the app should navigate directly to Home screen
**And** existing registration data should be used for API calls

**Verification**: Register → force close → reopen → goes to Home directly

---

### AC E1.1.6: Input Validation
**Given** I am on the Registration screen
**When** I enter invalid input
**Then** validation errors should be shown:
  - Empty display name: "Display name is required"
  - Display name < 2 chars: "Display name must be at least 2 characters"
  - Display name > 50 chars: "Display name must be 50 characters or less"
  - Empty group ID: "Group ID is required"
  - Invalid group ID chars: "Group ID can only contain letters, numbers, and hyphens"
  - Group ID < 2 chars: "Group ID must be at least 2 characters"

**Verification**: Enter invalid values → validation messages appear → button disabled

---

## Technical Details

### Architecture

**Pattern**: Extend existing MVVM + Repository pattern

**Data Flow**:
```
┌────────────────────────────────────────────────────────────────┐
│                    REGISTRATION FLOW                            │
│                                                                 │
│  RegistrationScreen (Compose)                                   │
│         │                                                       │
│         ▼                                                       │
│  RegistrationViewModel                                          │
│         │                                                       │
│         ├──► validate input                                     │
│         │                                                       │
│         ▼                                                       │
│  DeviceRepository                                               │
│         │                                                       │
│         ├──► SecureStorage (store displayName, groupId)        │
│         │                                                       │
│         └──► DeviceApiService (POST /api/devices/register)     │
│                    │                                            │
│                    ▼                                            │
│              Backend Server                                     │
└────────────────────────────────────────────────────────────────┘
```

### Implementation Files

#### 1. SecureStorage Extensions
**File**: `app/src/main/java/three/two/bit/phonemanager/security/SecureStorage.kt`

```kotlin
// Add to companion object
private const val KEY_DISPLAY_NAME = "display_name"
private const val KEY_GROUP_ID = "group_id"

// Add methods
fun getDisplayName(): String? = encryptedPrefs.getString(KEY_DISPLAY_NAME, null)

fun setDisplayName(displayName: String) {
    encryptedPrefs.edit()
        .putString(KEY_DISPLAY_NAME, displayName)
        .apply()
    Timber.d("Display name stored")
}

fun getGroupId(): String? = encryptedPrefs.getString(KEY_GROUP_ID, null)

fun setGroupId(groupId: String) {
    encryptedPrefs.edit()
        .putString(KEY_GROUP_ID, groupId)
        .apply()
    Timber.d("Group ID stored")
}

fun isRegistered(): Boolean = getDisplayName() != null && getGroupId() != null
```

---

#### 2. Device API Models
**File**: `app/src/main/java/three/two/bit/phonemanager/network/models/DeviceModels.kt` (NEW)

```kotlin
@Serializable
data class DeviceRegistrationRequest(
    val deviceId: String,
    val displayName: String,
    val groupId: String,
    val platform: String = "android",
    val fcmToken: String? = null
)

@Serializable
data class DeviceRegistrationResponse(
    val deviceId: String,
    val displayName: String,
    val groupId: String,
    val createdAt: String,
    val updatedAt: String
)
```

---

#### 3. Device API Service
**File**: `app/src/main/java/three/two/bit/phonemanager/network/DeviceApiService.kt` (NEW)

```kotlin
interface DeviceApiService {
    suspend fun registerDevice(request: DeviceRegistrationRequest): Result<DeviceRegistrationResponse>
}

@Singleton
class DeviceApiServiceImpl @Inject constructor(
    private val httpClient: HttpClient,
    private val apiConfig: ApiConfiguration,
) : DeviceApiService {

    override suspend fun registerDevice(
        request: DeviceRegistrationRequest
    ): Result<DeviceRegistrationResponse> = try {
        val response: DeviceRegistrationResponse = httpClient.post("${apiConfig.baseUrl}/api/devices/register") {
            contentType(ContentType.Application.Json)
            header("X-API-Key", apiConfig.apiKey)
            setBody(request)
        }.body()

        Timber.i("Device registered: ${response.deviceId}")
        Result.success(response)
    } catch (e: Exception) {
        Timber.e(e, "Failed to register device")
        Result.failure(e)
    }
}
```

---

#### 4. Device Repository
**File**: `app/src/main/java/three/two/bit/phonemanager/data/repository/DeviceRepository.kt` (NEW)

```kotlin
interface DeviceRepository {
    suspend fun registerDevice(displayName: String, groupId: String): Result<Unit>
    fun isRegistered(): Boolean
    fun getDeviceId(): String
    fun getDisplayName(): String?
    fun getGroupId(): String?
}

@Singleton
class DeviceRepositoryImpl @Inject constructor(
    private val secureStorage: SecureStorage,
    private val deviceApiService: DeviceApiService,
    private val networkManager: NetworkManager,
) : DeviceRepository {

    override suspend fun registerDevice(displayName: String, groupId: String): Result<Unit> {
        if (!networkManager.isNetworkAvailable()) {
            return Result.failure(NetworkException("No network connection available"))
        }

        val request = DeviceRegistrationRequest(
            deviceId = secureStorage.getDeviceId(),
            displayName = displayName,
            groupId = groupId,
            platform = "android"
        )

        return deviceApiService.registerDevice(request).map { response ->
            // Store locally on success
            secureStorage.setDisplayName(displayName)
            secureStorage.setGroupId(groupId)
            Timber.i("Device registration complete: ${response.deviceId}")
        }
    }

    override fun isRegistered(): Boolean = secureStorage.isRegistered()
    override fun getDeviceId(): String = secureStorage.getDeviceId()
    override fun getDisplayName(): String? = secureStorage.getDisplayName()
    override fun getGroupId(): String? = secureStorage.getGroupId()
}
```

---

#### 5. Registration ViewModel
**File**: `app/src/main/java/three/two/bit/phonemanager/ui/registration/RegistrationViewModel.kt` (NEW)

```kotlin
@HiltViewModel
class RegistrationViewModel @Inject constructor(
    private val deviceRepository: DeviceRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegistrationUiState())
    val uiState: StateFlow<RegistrationUiState> = _uiState.asStateFlow()

    fun updateDisplayName(name: String) {
        _uiState.update { it.copy(displayName = name, displayNameError = null) }
        validateForm()
    }

    fun updateGroupId(groupId: String) {
        _uiState.update { it.copy(groupId = groupId, groupIdError = null) }
        validateForm()
    }

    fun register() {
        if (!validateForm()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val result = deviceRepository.registerDevice(
                displayName = _uiState.value.displayName,
                groupId = _uiState.value.groupId
            )

            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false, isRegistered = true) }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(isLoading = false, error = e.message ?: "Registration failed")
                    }
                }
            )
        }
    }

    private fun validateForm(): Boolean {
        val state = _uiState.value
        var isValid = true

        // Validate display name
        val displayNameError = when {
            state.displayName.isBlank() -> "Display name is required"
            state.displayName.length < 2 -> "Display name must be at least 2 characters"
            state.displayName.length > 50 -> "Display name must be 50 characters or less"
            else -> null
        }

        // Validate group ID
        val groupIdRegex = Regex("^[a-zA-Z0-9-]+$")
        val groupIdError = when {
            state.groupId.isBlank() -> "Group ID is required"
            state.groupId.length < 2 -> "Group ID must be at least 2 characters"
            !state.groupId.matches(groupIdRegex) -> "Group ID can only contain letters, numbers, and hyphens"
            else -> null
        }

        if (displayNameError != null || groupIdError != null) {
            isValid = false
        }

        _uiState.update {
            it.copy(
                displayNameError = displayNameError,
                groupIdError = groupIdError,
                isFormValid = isValid
            )
        }

        return isValid
    }
}

data class RegistrationUiState(
    val displayName: String = "",
    val groupId: String = "",
    val displayNameError: String? = null,
    val groupIdError: String? = null,
    val isFormValid: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isRegistered: Boolean = false
)
```

---

#### 6. Registration Screen (Compose)
**File**: `app/src/main/java/three/two/bit/phonemanager/ui/registration/RegistrationScreen.kt` (NEW)

```kotlin
@Composable
fun RegistrationScreen(
    viewModel: RegistrationViewModel = hiltViewModel(),
    onRegistrationComplete: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isRegistered) {
        if (uiState.isRegistered) {
            onRegistrationComplete()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Device Registration",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Register your device to share location with your group",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Display Name Field
        OutlinedTextField(
            value = uiState.displayName,
            onValueChange = viewModel::updateDisplayName,
            label = { Text("Display Name") },
            placeholder = { Text("e.g., Martin's Phone") },
            isError = uiState.displayNameError != null,
            supportingText = uiState.displayNameError?.let { { Text(it) } },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Group ID Field
        OutlinedTextField(
            value = uiState.groupId,
            onValueChange = viewModel::updateGroupId,
            label = { Text("Group ID") },
            placeholder = { Text("e.g., family") },
            isError = uiState.groupIdError != null,
            supportingText = uiState.groupIdError?.let { { Text(it) } }
                ?: { Text("Share this ID with others to form a group") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Error message
        uiState.error?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Register Button
        Button(
            onClick = viewModel::register,
            enabled = uiState.isFormValid && !uiState.isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text("Register")
            }
        }
    }
}
```

---

#### 7. Navigation Updates
**File**: `app/src/main/java/three/two.bit/phonemanager/MainActivity.kt` (MODIFY)

```kotlin
// Add to navigation graph
composable("registration") {
    RegistrationScreen(
        onRegistrationComplete = {
            navController.navigate("home") {
                popUpTo("registration") { inclusive = true }
            }
        }
    )
}

// Update start destination logic
val startDestination = if (deviceRepository.isRegistered()) "home" else "registration"
```

---

### Dependencies

**Internal Dependencies**:
- `SecureStorage` (extend with displayName, groupId)
- `NetworkManager.isNetworkAvailable()`
- `ApiConfiguration` (for base URL)
- Hilt DI modules

**External Dependencies**: None new (uses existing Ktor, Hilt)

---

## Tasks / Subtasks

- [ ] **Task 1: Extend SecureStorage** (AC: E1.1.3, E1.1.5)
  - [ ] Add KEY_DISPLAY_NAME and KEY_GROUP_ID constants
  - [ ] Add getDisplayName() and setDisplayName() methods
  - [ ] Add getGroupId() and setGroupId() methods
  - [ ] Add isRegistered() method
  - [ ] Add unit tests for new methods

- [ ] **Task 2: Create Device API Models** (AC: E1.1.2)
  - [ ] Create DeviceModels.kt with request/response classes
  - [ ] Add @Serializable annotations for Ktor

- [ ] **Task 3: Create DeviceApiService** (AC: E1.1.2, E1.1.4)
  - [ ] Create DeviceApiService interface
  - [ ] Implement DeviceApiServiceImpl with Ktor
  - [ ] Handle errors and return Result type
  - [ ] Add to Hilt DI module

- [ ] **Task 4: Create DeviceRepository** (AC: E1.1.2, E1.1.3, E1.1.4)
  - [ ] Create DeviceRepository interface
  - [ ] Implement DeviceRepositoryImpl
  - [ ] Add network availability check
  - [ ] Store credentials on success
  - [ ] Add to Hilt DI module
  - [ ] Add unit tests

- [ ] **Task 5: Create RegistrationViewModel** (AC: E1.1.1, E1.1.6)
  - [ ] Create RegistrationUiState data class
  - [ ] Implement input validation logic
  - [ ] Implement register() with error handling
  - [ ] Add unit tests for validation

- [ ] **Task 6: Create RegistrationScreen** (AC: E1.1.1, E1.1.6)
  - [ ] Create Compose UI with Material 3
  - [ ] Display name input with validation
  - [ ] Group ID input with validation
  - [ ] Register button with loading state
  - [ ] Error message display

- [ ] **Task 7: Update Navigation** (AC: E1.1.1, E1.1.5)
  - [ ] Add registration route to NavHost
  - [ ] Update start destination based on isRegistered()
  - [ ] Handle navigation after successful registration

- [ ] **Task 8: Testing** (All ACs)
  - [ ] Unit tests for SecureStorage extensions
  - [ ] Unit tests for DeviceRepository
  - [ ] Unit tests for RegistrationViewModel
  - [ ] Integration test for registration flow

---

## Testing Strategy

### Unit Tests

```kotlin
// SecureStorageTest.kt
@Test
fun `setDisplayName stores value`()

@Test
fun `getDisplayName returns stored value`()

@Test
fun `isRegistered returns true when displayName and groupId set`()

@Test
fun `isRegistered returns false when displayName missing`()

// DeviceRepositoryTest.kt
@Test
fun `registerDevice calls API with correct payload`()

@Test
fun `registerDevice stores values on success`()

@Test
fun `registerDevice returns error when network unavailable`()

// RegistrationViewModelTest.kt
@Test
fun `validateForm fails for empty displayName`()

@Test
fun `validateForm fails for invalid groupId characters`()

@Test
fun `register calls repository and updates state`()
```

### Manual Testing Checklist

- [ ] Fresh install → Registration screen appears
- [ ] Enter valid name and group → Registration succeeds
- [ ] Force close and reopen → Goes to Home directly
- [ ] Disable network → Registration shows error
- [ ] Invalid input → Validation messages appear
- [ ] API returns error → Error message displayed

---

## Definition of Done

- [ ] SecureStorage extended with displayName/groupId methods
- [ ] DeviceApiService created with registration endpoint
- [ ] DeviceRepository implemented with registration logic
- [ ] RegistrationViewModel with validation
- [ ] RegistrationScreen Compose UI complete
- [ ] Navigation updated for registration flow
- [ ] All unit tests passing
- [ ] Manual testing completed
- [ ] Code review approved
- [ ] No lint errors

---

## Risks & Mitigations

**RISK**: Backend API not ready
- **Impact**: Can't test registration
- **Mitigation**: Create mock API service for development; test with mock responses
- **Contingency**: Store locally only until backend available

**RISK**: Network errors during registration
- **Impact**: User can't complete registration
- **Mitigation**: Clear error messages; retry button; offline-first option
- **Contingency**: Allow "offline registration" that syncs later

---

## Dev Notes

### Source References

- [Source: PRD Section 5, Feature 6 - FR-6.1.x]
- [Source: PRD Section 6.3 - POST /api/devices/register API spec]
- [Source: SecureStorage.kt - Existing encrypted storage pattern]
- [Source: LocationApiService.kt - Existing Ktor HTTP client pattern]

### Project Structure Notes

- New files in `ui/registration/` package
- New files in `network/models/` package
- Extends existing `security/SecureStorage.kt`
- Uses existing Hilt DI patterns from `di/` modules

---

## Change Log

| Date | Author | Changes |
|------|--------|---------|
| 2025-11-25 | Claude | Initial story creation from epics.md and PRD |

---

**Last Updated**: 2025-11-25
**Status**: Ready for Review
**Dependencies**: Epic 0 (Foundation) - DONE

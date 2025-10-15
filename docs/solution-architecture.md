# Phone Manager Solution Architecture

**Project:** Phone Manager
**Date:** 2025-10-15
**Author:** Martin
**Version:** 1.0
**Architecture Type:** Native Android (Kotlin) with Jetpack Compose

---

## Executive Summary

Phone Manager is a lightweight, battery-optimized Android application that securely collects device location data in the background and transmits encrypted payloads to a user-configured n8n webhook endpoint. The architecture follows Android best practices with a clean separation of concerns using MVVM pattern, Jetpack Compose for UI, WorkManager for background tasks, and Koin for dependency injection.

**Key Architectural Highlights:**
- **Minimal Dependencies:** Lean stack focusing on essential Android Jetpack libraries
- **Security-First:** AES-256-CBC encryption with hardware-backed KeyStore
- **Battery-Optimized:** WorkManager with Doze mode compatibility, fused location provider
- **Offline-Capable:** Local queue with automatic retry and sync when connectivity restored
- **Simple UX:** Single Activity with Compose Navigation, two screens (Home + Settings)

**Target Android Versions:** API 26 (Android 8.0) minimum, targeting API 34 (Android 14)

---

## 1. Technology Stack and Decisions

### 1.1 Technology and Library Decision Table

| Category | Technology | Version | Rationale |
|----------|------------|---------|-----------|
| **Language** | Kotlin | 1.9.22 | Modern, concise, Android-first language with coroutines support |
| **Build System** | Gradle (Kotlin DSL) | 8.2.0 | Standard Android build system with type-safe configuration |
| **Minimum SDK** | Android 8.0 (Oreo) | API 26 | Balance of feature support and market coverage (~95% devices) |
| **Target SDK** | Android 14 | API 34 | Latest stable Android version for modern features |
| **Compile SDK** | Android 14 | API 34 | Latest APIs and build tools |
| **UI Framework** | Jetpack Compose | 1.6.0 | Modern declarative UI, less boilerplate than XML views |
| **Compose BOM** | androidx.compose | 2024.02.00 | Bill of Materials for consistent Compose versions |
| **Material Design** | Material 3 (Compose) | 1.2.0 | Modern Material Design with dynamic theming support |
| **Navigation** | Compose Navigation | 2.7.6 | Type-safe navigation for Compose screens |
| **Architecture Pattern** | MVVM + Clean Architecture | - | Separation of concerns, testability, Android standard |
| **Dependency Injection** | Koin | 3.5.3 | Lightweight DI, simpler than Hilt, minimal boilerplate |
| **Coroutines** | kotlinx.coroutines | 1.7.3 | Asynchronous programming, thread management |
| **ViewModel** | androidx.lifecycle:viewmodel-compose | 2.7.0 | State management, lifecycle-aware, survives config changes |
| **Background Work** | WorkManager | 2.9.0 | Deferrable background tasks, Doze-compatible, guaranteed execution |
| **Location Services** | Google Play Services Location | 21.1.0 | Fused Location Provider for battery-efficient location access |
| **HTTP Client** | Retrofit | 2.9.0 | Type-safe REST client for n8n webhook calls |
| **HTTP Logging** | OkHttp Logging Interceptor | 4.12.0 | Network request/response debugging |
| **JSON Serialization** | Kotlinx Serialization | 1.6.2 | Kotlin-native JSON, faster and safer than Gson |
| **Encryption** | Android Security Crypto | 1.1.0-alpha06 | EncryptedSharedPreferences for secure config storage |
| **Crypto (Manual)** | javax.crypto (built-in) | Android Platform | AES-256-CBC encryption for location payloads |
| **KeyStore** | Android KeyStore (built-in) | Android Platform | Hardware-backed secure key storage |
| **Local Database** | Room | 2.6.1 | SQLite abstraction for transmission queue, type-safe queries |
| **Logging** | Timber | 5.0.1 | Structured logging with debug/release tree switching |
| **Testing - Unit** | JUnit 5 | 5.10.1 | Modern unit testing framework with better assertions |
| **Testing - Android** | AndroidX Test | 1.5.0 | Android instrumentation tests, UI testing |
| **Testing - Compose** | Compose UI Test | 1.6.0 | Compose-specific UI testing utilities |
| **Testing - Coroutines** | kotlinx-coroutines-test | 1.7.3 | Test coroutines and suspend functions |
| **Testing - Mockk** | MockK | 1.13.8 | Kotlin-friendly mocking library |
| **Code Quality** | Detekt | 1.23.4 | Kotlin static analysis and linting |

### 1.2 Key Architectural Decisions

**ADR-001: Native Android with Kotlin**
- **Decision:** Use native Android development with Kotlin instead of cross-platform (Flutter/React Native)
- **Rationale:**
  - Android-only requirement (no iOS)
  - Need platform-specific APIs (WorkManager, FusedLocationProvider, KeyStore)
  - Better battery optimization with native code
  - Direct access to Android background service lifecycle
- **Trade-offs:** No code sharing with iOS (not needed), steeper learning curve than cross-platform

**ADR-002: Jetpack Compose over XML Views**
- **Decision:** Use Jetpack Compose for all UI
- **Rationale:**
  - Modern declarative UI paradigm (less boilerplate)
  - Easier state management
  - Better tooling and preview support
  - Future-proof (Android's recommended UI toolkit)
- **Trade-offs:** Smaller community resources than XML Views, slightly larger APK size

**ADR-003: Koin over Hilt/Dagger**
- **Decision:** Use Koin for dependency injection
- **Rationale:**
  - Simpler setup (no annotation processing, faster builds)
  - More Kotlin-idiomatic DSL
  - Sufficient for app size and complexity
  - Easier to learn and maintain
- **Trade-offs:** Runtime DI (slightly slower than Hilt's compile-time), less compile-time safety

**ADR-004: WorkManager for Background Tasks**
- **Decision:** Use WorkManager for periodic location collection and retry logic
- **Rationale:**
  - Guaranteed execution (survives app kill and reboot)
  - Doze mode and App Standby compatible
  - Battery-efficient with constraints (network, charging)
  - Supported by Google, future-proof
- **Trade-offs:** Not suitable for precise timing (acceptable for 5-15 min intervals)

**ADR-005: Room Database for Transmission Queue**
- **Decision:** Use Room for local SQLite database to queue failed transmissions
- **Rationale:**
  - Type-safe SQL queries with compile-time verification
  - Integration with Kotlin Coroutines and Flow
  - Efficient for queueing and batch operations
  - Standard Android persistence solution
- **Trade-offs:** Slightly heavier than raw SharedPreferences (acceptable for queue use case)

**ADR-006: AES-256-CBC with Android KeyStore**
- **Decision:** Implement manual AES-256-CBC encryption with KeyStore-managed keys
- **Rationale:**
  - Strong encryption standard compatible with n8n decryption
  - Hardware-backed key storage on modern devices
  - Full control over encryption parameters (IV, padding)
  - No third-party crypto dependencies
- **Trade-offs:** More complex than using EncryptedFile API, requires careful IV management

**ADR-007: No Analytics or Crash Reporting**
- **Decision:** Minimal approach with no third-party analytics or crash reporting
- **Rationale:**
  - Simplicity and minimal APK size
  - Privacy-focused (no data sent to third parties)
  - Google Play Console provides basic crash reports
  - Can add later if needed
- **Trade-offs:** Less visibility into usage patterns and crashes in production

---

## 2. Architecture Overview

### 2.1 High-Level Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                        Presentation Layer                        │
│  ┌──────────────┐                          ┌─────────────────┐  │
│  │  HomeScreen  │                          │ SettingsScreen  │  │
│  │  (Compose)   │                          │   (Compose)     │  │
│  └──────┬───────┘                          └────────┬────────┘  │
│         │                                           │            │
│         ▼                                           ▼            │
│  ┌──────────────┐                          ┌─────────────────┐  │
│  │ HomeViewModel│                          │SettingsViewModel│  │
│  └──────┬───────┘                          └────────┬────────┘  │
└─────────┼──────────────────────────────────────────┼───────────┘
          │                                           │
┌─────────┼───────────────────────────────────────────┼───────────┐
│         │           Domain Layer (Minimal)          │           │
│         ▼                                           ▼            │
│  ┌────────────────────────────┐         ┌──────────────────┐   │
│  │   LocationTrackingUseCase  │         │   ConfigUseCase  │   │
│  └──────────┬─────────────────┘         └────────┬─────────┘   │
└─────────────┼──────────────────────────────────────┼───────────┘
              │                                      │
┌─────────────┼──────────────────────────────────────┼───────────┐
│             │             Data Layer               │           │
│             ▼                                      ▼            │
│  ┌──────────────────┐                   ┌──────────────────┐  │
│  │ LocationRepository│                  │ ConfigRepository │  │
│  └────┬──────┬──────┘                   └─────────┬────────┘  │
│       │      │                                     │           │
│       │      └──────────────┐                     │           │
│       ▼                     ▼                     ▼            │
│  ┌─────────────┐   ┌────────────────┐  ┌──────────────────┐  │
│  │  Location   │   │    Network     │  │  Preferences     │  │
│  │ DataSource  │   │  Repository    │  │   DataSource     │  │
│  │(FusedLoc)   │   └────┬───────────┘  │ (Encrypted)      │  │
│  └─────────────┘        │              └──────────────────┘  │
│                         ▼                                     │
│               ┌───────────────────┐                           │
│               │ Transmission Queue│                           │
│               │  (Room Database)  │                           │
│               └───────────────────┘                           │
└───────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                    Background Services                           │
│  ┌──────────────────┐         ┌────────────────────────────┐   │
│  │ LocationWorker   │         │      RetryWorker           │   │
│  │  (WorkManager)   │         │     (WorkManager)          │   │
│  │  - Periodic      │         │  - Exponential Backoff     │   │
│  │  - 5/10/15 min   │         │  - Queue Processing        │   │
│  └──────────────────┘         └────────────────────────────┘   │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │            LocationService (Foreground)                  │  │
│  │            - Active tracking notification                │  │
│  │            - Prevents process kill                       │  │
│  └──────────────────────────────────────────────────────────┘  │
└───────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                    Cross-Cutting Concerns                        │
│  ┌───────────────┐  ┌──────────────────┐  ┌─────────────────┐  │
│  │ CryptoManager │  │ PermissionManager│  │  Koin Modules   │  │
│  │  - AES-256    │  │  - Runtime perms │  │  - DI Container │  │
│  │  - KeyStore   │  │  - Background loc│  │                 │  │
│  └───────────────┘  └──────────────────┘  └─────────────────┘  │
└───────────────────────────────────────────────────────────────┘

          External Integration: n8n Webhook (HTTPS POST)
```

### 2.2 Architecture Pattern: MVVM + Clean Architecture (Simplified)

**Layer Responsibilities:**

**UI Layer (Compose + ViewModel):**
- User interface rendering with Jetpack Compose
- User input handling
- State management via ViewModel
- Navigation between screens

**Domain Layer (Minimal Use Cases):**
- Business logic coordination
- Cross-repository orchestration
- Use case: Location tracking workflow (collect → encrypt → transmit → retry)

**Data Layer (Repositories + Data Sources):**
- Data access abstraction
- Multiple data sources (Location API, Network, Database, Preferences)
- Repository pattern isolates data sources from ViewModels

**Background Services:**
- WorkManager workers for deferred tasks
- Foreground service for active tracking

**Why This Architecture:**
- **MVVM:** Android standard, works well with Compose and lifecycle
- **Clean Architecture:** Separation of concerns without over-engineering
- **Minimal Domain Layer:** Use cases only where needed (avoid premature abstraction)
- **Repository Pattern:** Testability and data source isolation

---

## 3. Data Architecture

### 3.1 Data Models

**Location Data Model**
```kotlin
data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val altitude: Double?,
    val bearing: Float?,
    val speed: Float?,
    val provider: String,
    val timestamp: Long = System.currentTimeMillis()
)
```

**Encrypted Payload Model**
```kotlin
data class EncryptedPayload(
    val deviceId: String,
    val timestamp: String, // ISO 8601 format
    val encryptedData: String, // Base64-encoded encrypted JSON
    val iv: String, // Base64-encoded initialization vector
    val encryptionAlgorithm: String = "AES-256-CBC"
)
```

**Transmission Queue Entity (Room)**
```kotlin
@Entity(tableName = "transmission_queue")
data class QueuedTransmission(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val payload: String, // JSON serialized EncryptedPayload
    val attemptCount: Int = 0,
    val createdAt: Long,
    val lastAttemptAt: Long? = null,
    val status: TransmissionStatus = TransmissionStatus.PENDING
)

enum class TransmissionStatus {
    PENDING, IN_PROGRESS, FAILED, COMPLETED
}
```

**Configuration Model**
```kotlin
data class AppConfig(
    val webhookUrl: String,
    val trackingInterval: TrackingInterval,
    val encryptionKey: String, // Stored reference (actual key in KeyStore)
    val isTrackingEnabled: Boolean = false
)

enum class TrackingInterval(val minutes: Long) {
    FIVE_MINUTES(5),
    TEN_MINUTES(10),
    FIFTEEN_MINUTES(15)
}
```

### 3.2 Data Flow

**Location Collection Flow:**
```
1. User enables tracking (HomeScreen)
2. HomeViewModel → LocationTrackingUseCase
3. LocationWorker scheduled (WorkManager periodic)
4. LocationWorker executes:
   a. Request location from FusedLocationProvider
   b. Encrypt location with CryptoManager
   c. Attempt transmission via NetworkRepository
   d. If fails: Add to Room queue
   e. If succeeds: Log success
5. RetryWorker processes queue periodically
```

**Encryption Flow:**
```
1. LocationData (plaintext) → CryptoManager
2. Serialize to JSON string
3. Generate random IV (Initialization Vector)
4. Retrieve AES key from KeyStore
5. Encrypt JSON with AES-256-CBC
6. Base64-encode encrypted bytes and IV
7. Create EncryptedPayload with metadata
8. Return to caller
```

**Network Transmission Flow:**
```
1. EncryptedPayload → NetworkRepository
2. Serialize payload to JSON
3. POST to webhook URL via Retrofit
4. On success (200-299): Return success
5. On network error: Throw exception → queue in Room
6. On server error (4xx/5xx): Retry based on status code
```

### 3.3 Data Storage

**EncryptedSharedPreferences (Configuration):**
- Webhook URL (encrypted at rest)
- Tracking interval preference
- Encryption key alias (reference to KeyStore)
- Tracking enabled state
- Device ID (UUID generated on first run)

**Room Database (Transmission Queue):**
- Table: `transmission_queue`
- Purpose: Store failed transmissions for retry
- Max size: 100 entries (FIFO, oldest discarded)
- Indexed by `status` and `createdAt` for efficient queries

**Android KeyStore (Encryption Keys):**
- AES key (256-bit) generated with KeyGenerator
- Hardware-backed when available (TEE/Secure Element)
- Key alias: "phone_manager_aes_key"
- StrongBox backed on supported devices (Pixel 3+)

**No Cloud Storage:**
- All data stored locally on device
- No user accounts or cloud sync
- Privacy-first approach

---

## 4. Component and Integration Overview

### 4.1 Core Components

#### 4.1.1 Presentation Layer

**MainActivity (Single Activity)**
```kotlin
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PhoneManagerTheme {
                PhoneManagerNavHost()
            }
        }
    }
}
```

**HomeScreen (Composable)**
- Displays tracking status (Active/Inactive)
- Toggle switch to enable/disable tracking
- Permission status indicator
- Button to request location permissions
- Navigation to Settings

**SettingsScreen (Composable)**
- Webhook URL configuration (TextField with validation)
- Tracking interval selector (5/10/15 minutes)
- Encryption key display (read-only, with copy button)
- Test connection button (send test payload)

**Navigation (Compose Navigation)**
```kotlin
sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Settings : Screen("settings")
}

@Composable
fun PhoneManagerNavHost() {
    val navController = rememberNavController()
    NavHost(navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) { HomeScreen(navController) }
        composable(Screen.Settings.route) { SettingsScreen(navController) }
    }
}
```

#### 4.1.2 ViewModel Layer

**HomeViewModel**
```kotlin
class HomeViewModel(
    private val locationUseCase: LocationTrackingUseCase,
    private val configRepository: ConfigRepository,
    private val permissionManager: PermissionManager
) : ViewModel() {

    val trackingState: StateFlow<TrackingState>
    val permissionStatus: StateFlow<PermissionStatus>

    fun toggleTracking()
    fun requestPermissions()
}
```

**SettingsViewModel**
```kotlin
class SettingsViewModel(
    private val configRepository: ConfigRepository,
    private val networkRepository: NetworkRepository
) : ViewModel() {

    val config: StateFlow<AppConfig>

    fun updateWebhookUrl(url: String)
    fun updateInterval(interval: TrackingInterval)
    fun testConnection()
}
```

#### 4.1.3 Domain Layer

**LocationTrackingUseCase**
```kotlin
class LocationTrackingUseCase(
    private val locationRepository: LocationRepository,
    private val networkRepository: NetworkRepository,
    private val cryptoManager: CryptoManager
) {
    suspend fun collectAndTransmit(): Result<Unit>
}
```

#### 4.1.4 Data Layer

**LocationRepository**
```kotlin
class LocationRepository(
    private val locationDataSource: LocationDataSource
) {
    suspend fun getCurrentLocation(): Result<LocationData>
}
```

**NetworkRepository**
```kotlin
class NetworkRepository(
    private val webhookService: WebhookService,
    private val queueDao: TransmissionQueueDao
) {
    suspend fun transmit(payload: EncryptedPayload): Result<Unit>
    suspend fun retryQueuedTransmissions()
}
```

**ConfigRepository**
```kotlin
class ConfigRepository(
    private val prefsDataSource: PreferencesDataSource
) {
    fun getConfig(): Flow<AppConfig>
    suspend fun updateConfig(config: AppConfig)
}
```

#### 4.1.5 Background Services

**LocationWorker (WorkManager)**
```kotlin
class LocationWorker(
    context: Context,
    params: WorkerParameters,
    private val locationUseCase: LocationTrackingUseCase
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return when (locationUseCase.collectAndTransmit()) {
            is Result.Success -> Result.success()
            is Result.Error -> Result.retry()
        }
    }
}
```

**Scheduling:**
```kotlin
val constraints = Constraints.Builder()
    .setRequiredNetworkType(NetworkType.CONNECTED)
    .build()

val workRequest = PeriodicWorkRequestBuilder<LocationWorker>(
    repeatInterval = intervalMinutes,
    repeatIntervalTimeUnit = TimeUnit.MINUTES
)
    .setConstraints(constraints)
    .build()

WorkManager.getInstance(context).enqueueUniquePeriodicWork(
    "location_collection",
    ExistingPeriodicWorkPolicy.REPLACE,
    workRequest
)
```

**RetryWorker (WorkManager)**
```kotlin
class RetryWorker(
    context: Context,
    params: WorkerParameters,
    private val networkRepository: NetworkRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        networkRepository.retryQueuedTransmissions()
        return Result.success()
    }
}
```

**LocationService (Foreground Service)**
```kotlin
class LocationService : Service() {
    private lateinit var notificationChannel: NotificationChannel

    override fun onCreate() {
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    private fun buildNotification(): Notification {
        // Persistent notification: "Phone Manager is tracking location"
    }
}
```

#### 4.1.6 Cross-Cutting Components

**CryptoManager**
```kotlin
class CryptoManager(private val context: Context) {

    fun encrypt(locationData: LocationData): EncryptedPayload {
        // 1. Serialize LocationData to JSON
        // 2. Generate random IV (16 bytes)
        // 3. Get AES key from KeyStore
        // 4. Encrypt with AES/CBC/PKCS7Padding
        // 5. Base64-encode encrypted bytes and IV
        // 6. Return EncryptedPayload
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)

        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                "AndroidKeyStore"
            )
            keyGenerator.init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                    .setKeySize(256)
                    .setUserAuthenticationRequired(false)
                    .build()
            )
            keyGenerator.generateKey()
        }

        return (keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
    }

    companion object {
        private const val KEY_ALIAS = "phone_manager_aes_key"
    }
}
```

**PermissionManager**
```kotlin
class PermissionManager(private val context: Context) {

    fun checkLocationPermissions(): PermissionStatus {
        val fineLocation = context.checkSelfPermission(ACCESS_FINE_LOCATION)
        val backgroundLocation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.checkSelfPermission(ACCESS_BACKGROUND_LOCATION)
        } else {
            PackageManager.PERMISSION_GRANTED
        }

        return when {
            fineLocation != GRANTED -> PermissionStatus.DENIED
            backgroundLocation != GRANTED -> PermissionStatus.FOREGROUND_ONLY
            else -> PermissionStatus.GRANTED
        }
    }

    fun shouldShowRationale(activity: Activity): Boolean
}
```

### 4.2 External Integrations

**n8n Webhook Integration**

**Retrofit Service Interface:**
```kotlin
interface WebhookService {
    @POST
    suspend fun sendLocation(
        @Url url: String,
        @Body payload: EncryptedPayload
    ): Response<Unit>
}
```

**Payload Example (JSON):**
```json
{
  "device_id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "timestamp": "2025-10-15T14:23:45.123Z",
  "encrypted_data": "U2FsdGVkX1+vupppZksvRf5pq5g5XjFRIipRkwB0K1Y=...",
  "iv": "MTIzNDU2Nzg5MGFiY2RlZg==",
  "encryption_algorithm": "AES-256-CBC"
}
```

**n8n Decryption Node Configuration:**
```javascript
// n8n Function Node Example
const crypto = require('crypto');

const encryptedData = Buffer.from(items[0].json.encrypted_data, 'base64');
const iv = Buffer.from(items[0].json.iv, 'base64');
const key = Buffer.from('YOUR_32_BYTE_KEY_HERE', 'utf8'); // 32 bytes for AES-256

const decipher = crypto.createDecipheriv('aes-256-cbc', key, iv);
let decrypted = decipher.update(encryptedData, null, 'utf8');
decrypted += decipher.final('utf8');

const locationData = JSON.parse(decrypted);
return [{ json: locationData }];
```

---

## 5. Architecture Decision Records

**See Section 1.2 for detailed ADRs:**
- ADR-001: Native Android with Kotlin
- ADR-002: Jetpack Compose over XML Views
- ADR-003: Koin over Hilt/Dagger
- ADR-004: WorkManager for Background Tasks
- ADR-005: Room Database for Transmission Queue
- ADR-006: AES-256-CBC with Android KeyStore
- ADR-007: No Analytics or Crash Reporting

**Additional ADRs:**

**ADR-008: Fused Location Provider over GPS**
- **Decision:** Use Google Play Services Fused Location Provider instead of direct GPS
- **Rationale:**
  - Battery-efficient (combines GPS, WiFi, cell tower)
  - Automatic best source selection
  - Simpler API than LocationManager
  - Respects power mode settings
- **Trade-offs:** Requires Google Play Services (not available on all devices, but acceptable for target market)

**ADR-009: Exponential Backoff for Retries**
- **Decision:** Retry failed transmissions with exponential backoff (1s, 2s, 4s, 8s, 16s, max 5 attempts)
- **Rationale:**
  - Prevents overwhelming n8n webhook during outages
  - Gives transient errors time to resolve
  - Balances responsiveness with resource usage
- **Trade-offs:** May delay transmission up to 31 seconds (acceptable for non-real-time use case)

**ADR-010: Single Activity Architecture**
- **Decision:** Use Single Activity with Compose Navigation instead of multi-activity
- **Rationale:**
  - Compose Navigation is designed for single-activity
  - Simpler state management and navigation
  - Better animation transitions
  - Android best practice for modern apps
- **Trade-offs:** All screens share same lifecycle (acceptable for simple two-screen app)

---

## 6. Implementation Guidance

### 6.1 Build Configuration

**Project-level build.gradle.kts:**
```kotlin
plugins {
    id("com.android.application") version "8.2.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
    id("com.google.devtools.ksp") version "1.9.22-1.0.17" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.22" apply false
}
```

**App-level build.gradle.kts:**
```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.phonemanager"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.phonemanager"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
}

dependencies {
    // Compose BOM
    val composeBom = platform("androidx.compose:compose-bom:2024.02.00")
    implementation(composeBom)
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")

    // Koin
    implementation("io.insert-koin:koin-android:3.5.3")
    implementation("io.insert-koin:koin-androidx-compose:3.5.3")

    // Additional dependencies (see Technology Table)
}
```

### 6.2 Permission Configuration

**AndroidManifest.xml:**
```xml
<manifest>
    <!-- Location permissions -->
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />

    <!-- Network -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

    <!-- Foreground service -->
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_LOCATION" />

    <!-- Post notifications (Android 13+) -->
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

    <!-- Boot receiver (restart service after reboot) -->
    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />

    <application>
        <activity android:name=".MainActivity" />

        <service
            android:name=".service.LocationService"
            android:foregroundServiceType="location"
            android:exported="false" />

        <receiver
            android:name=".receiver.BootReceiver"
            android:enabled="true"
            android:exported="false">
            <intent-filter>
                <action android:name="android.intent.action.BOOT_COMPLETED" />
            </intent-filter>
        </receiver>
    </application>
</manifest>
```

### 6.3 Koin Module Setup

**di/AppModule.kt:**
```kotlin
val appModule = module {
    single { CryptoManager(androidContext()) }
    single { PermissionManager(androidContext()) }
}
```

**di/DataModule.kt:**
```kotlin
val dataModule = module {
    // Room Database
    single {
        Room.databaseBuilder(
            androidContext(),
            PhoneManagerDatabase::class.java,
            "phone_manager_db"
        ).build()
    }
    single { get<PhoneManagerDatabase>().transmissionQueueDao() }

    // Retrofit
    single {
        Retrofit.Builder()
            .baseUrl("https://dummy.com/") // Base URL not used (dynamic URL per request)
            .addConverterFactory(Json.asConverterFactory("application/json".toMediaType()))
            .client(get())
            .build()
    }
    single { get<Retrofit>().create(WebhookService::class.java) }

    // OkHttp
    single {
        OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
                        else HttpLoggingInterceptor.Level.NONE
            })
            .build()
    }

    // Data Sources
    single { LocationDataSource(androidContext()) }
    single { PreferencesDataSource(androidContext()) }

    // Repositories
    single { LocationRepository(get()) }
    single { NetworkRepository(get(), get()) }
    single { ConfigRepository(get()) }
}
```

**di/DomainModule.kt:**
```kotlin
val domainModule = module {
    factory { LocationTrackingUseCase(get(), get(), get()) }
    factory { ConfigUseCase(get()) }
}
```

**di/ViewModelModule.kt:**
```kotlin
val viewModelModule = module {
    viewModel { HomeViewModel(get(), get(), get()) }
    viewModel { SettingsViewModel(get(), get()) }
}
```

### 6.4 WorkManager Initialization

**PhoneManagerApp.kt:**
```kotlin
class PhoneManagerApp : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger()
            androidContext(this@PhoneManagerApp)
            modules(appModule, dataModule, domainModule, viewModelModule)
        }

        Timber.plant(if (BuildConfig.DEBUG) Timber.DebugTree() else ReleaseTree())
    }
}
```

**Scheduling Location Worker:**
```kotlin
fun scheduleLocationTracking(context: Context, interval: TrackingInterval) {
    val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .setRequiresBatteryNotLow(true)
        .build()

    val workRequest = PeriodicWorkRequestBuilder<LocationWorker>(
        repeatInterval = interval.minutes,
        repeatIntervalTimeUnit = TimeUnit.MINUTES
    )
        .setConstraints(constraints)
        .setBackoffCriteria(
            BackoffPolicy.EXPONENTIAL,
            1, TimeUnit.SECONDS
        )
        .build()

    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "location_collection",
        ExistingPeriodicWorkPolicy.REPLACE,
        workRequest
    )
}
```

### 6.5 Battery Optimization Best Practices

**1. Use WorkManager Constraints:**
```kotlin
Constraints.Builder()
    .setRequiresBatteryNotLow(true) // Don't run when battery critical
    .setRequiredNetworkType(NetworkType.CONNECTED) // Only when online
    .build()
```

**2. Fused Location Provider Configuration:**
```kotlin
val locationRequest = LocationRequest.Builder(
    Priority.PRIORITY_BALANCED_POWER_ACCURACY, // Not HIGH_ACCURACY
    intervalMinutes * 60 * 1000L
).apply {
    setMinUpdateIntervalMillis(intervalMinutes * 60 * 1000L)
    setMaxUpdateDelayMillis(intervalMinutes * 60 * 1000L)
}.build()
```

**3. Batch Transmissions:**
- Queue failed transmissions locally (Room)
- Retry in batches instead of individual requests
- Use WorkManager constraints to wait for optimal conditions

**4. Doze Mode Guidance:**
- WorkManager automatically respects Doze mode
- Consider requesting Doze whitelist for critical use cases (user must approve)
- Test with `adb shell dumpsys battery unplug` and `adb shell dumpsys deviceidle force-idle`

---

## 7. Proposed Source Tree

```
phone-manager/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml
│   │   │   ├── java/com/phonemanager/
│   │   │   │   ├── PhoneManagerApp.kt
│   │   │   │   ├── MainActivity.kt
│   │   │   │   │
│   │   │   │   ├── ui/
│   │   │   │   │   ├── theme/
│   │   │   │   │   │   ├── Color.kt
│   │   │   │   │   │   ├── Theme.kt
│   │   │   │   │   │   └── Type.kt
│   │   │   │   │   ├── navigation/
│   │   │   │   │   │   └── PhoneManagerNavHost.kt
│   │   │   │   │   ├── home/
│   │   │   │   │   │   ├── HomeScreen.kt
│   │   │   │   │   │   ├── HomeViewModel.kt
│   │   │   │   │   │   └── components/
│   │   │   │   │   │       ├── TrackingToggle.kt
│   │   │   │   │   │       ├── StatusIndicator.kt
│   │   │   │   │   │       └── PermissionPrompt.kt
│   │   │   │   │   └── settings/
│   │   │   │   │       ├── SettingsScreen.kt
│   │   │   │   │       ├── SettingsViewModel.kt
│   │   │   │   │       └── components/
│   │   │   │   │           ├── WebhookConfig.kt
│   │   │   │   │           ├── IntervalSelector.kt
│   │   │   │   │           └── EncryptionKeyDisplay.kt
│   │   │   │   │
│   │   │   │   ├── domain/
│   │   │   │   │   ├── model/
│   │   │   │   │   │   ├── LocationData.kt
│   │   │   │   │   │   ├── EncryptedPayload.kt
│   │   │   │   │   │   ├── AppConfig.kt
│   │   │   │   │   │   └── TrackingInterval.kt
│   │   │   │   │   └── usecase/
│   │   │   │   │       ├── LocationTrackingUseCase.kt
│   │   │   │   │       └── ConfigUseCase.kt
│   │   │   │   │
│   │   │   │   ├── data/
│   │   │   │   │   ├── repository/
│   │   │   │   │   │   ├── LocationRepository.kt
│   │   │   │   │   │   ├── NetworkRepository.kt
│   │   │   │   │   │   └── ConfigRepository.kt
│   │   │   │   │   ├── source/
│   │   │   │   │   │   ├── local/
│   │   │   │   │   │   │   ├── database/
│   │   │   │   │   │   │   │   ├── PhoneManagerDatabase.kt
│   │   │   │   │   │   │   │   ├── dao/
│   │   │   │   │   │   │   │   │   └── TransmissionQueueDao.kt
│   │   │   │   │   │   │   │   └── entity/
│   │   │   │   │   │   │   │       └── QueuedTransmission.kt
│   │   │   │   │   │   │   └── preferences/
│   │   │   │   │   │   │       └── PreferencesDataSource.kt
│   │   │   │   │   │   └── remote/
│   │   │   │   │   │       ├── NetworkDataSource.kt
│   │   │   │   │   │       └── api/
│   │   │   │   │   │           └── WebhookService.kt
│   │   │   │   │   ├── location/
│   │   │   │   │   │   └── LocationDataSource.kt
│   │   │   │   │   └── worker/
│   │   │   │   │       ├── LocationWorker.kt
│   │   │   │   │       ├── RetryWorker.kt
│   │   │   │   │       └── WorkerFactory.kt
│   │   │   │   │
│   │   │   │   ├── service/
│   │   │   │   │   └── LocationService.kt
│   │   │   │   │
│   │   │   │   ├── receiver/
│   │   │   │   │   └── BootReceiver.kt
│   │   │   │   │
│   │   │   │   ├── util/
│   │   │   │   │   ├── CryptoManager.kt
│   │   │   │   │   ├── PermissionManager.kt
│   │   │   │   │   └── extensions/
│   │   │   │   │       ├── ContextExtensions.kt
│   │   │   │   │       └── FlowExtensions.kt
│   │   │   │   │
│   │   │   │   └── di/
│   │   │   │       ├── AppModule.kt
│   │   │   │       ├── DataModule.kt
│   │   │   │       ├── DomainModule.kt
│   │   │   │       └── ViewModelModule.kt
│   │   │   │
│   │   │   └── res/
│   │   │       ├── values/
│   │   │       │   ├── strings.xml
│   │   │       │   ├── colors.xml
│   │   │       │   └── themes.xml
│   │   │       ├── drawable/
│   │   │       │   └── ic_launcher.xml
│   │   │       └── mipmap-*/
│   │   │           └── ic_launcher.png
│   │   │
│   │   ├── test/
│   │   │   └── java/com/phonemanager/
│   │   │       ├── domain/
│   │   │       │   └── usecase/
│   │   │       │       └── LocationTrackingUseCaseTest.kt
│   │   │       ├── data/
│   │   │       │   └── repository/
│   │   │       │       ├── LocationRepositoryTest.kt
│   │   │       │       └── NetworkRepositoryTest.kt
│   │   │       └── util/
│   │   │           └── CryptoManagerTest.kt
│   │   │
│   │   └── androidTest/
│   │       └── java/com/phonemanager/
│   │           ├── ui/
│   │           │   ├── HomeScreenTest.kt
│   │           │   └── SettingsScreenTest.kt
│   │           └── data/
│   │               └── database/
│   │                   └── TransmissionQueueDaoTest.kt
│   │
│   ├── build.gradle.kts
│   └── proguard-rules.pro
│
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradlew
├── gradlew.bat
├── .gitignore
└── README.md
```

**Total Estimated Files:** ~50 Kotlin files, ~10 XML files

**Package Structure Philosophy:**
- **ui/**: All Compose screens and components (feature-based organization)
- **domain/**: Business logic, models, use cases (minimal)
- **data/**: Repositories, data sources, workers, Room entities
- **service/**: Foreground services
- **receiver/**: Broadcast receivers
- **util/**: Cross-cutting concerns (crypto, permissions)
- **di/**: Koin dependency injection modules

---

## 8. Testing Strategy

### 8.1 Unit Tests (test/)

**Target Coverage:** 80% of business logic

**Test Framework:** JUnit 5 + MockK + Coroutines Test

**Key Test Targets:**
1. **LocationTrackingUseCase** - Core business logic
   - Test successful location collection and transmission
   - Test failure scenarios (no location, network error)
   - Test encryption integration

2. **CryptoManager** - Encryption logic
   - Test AES-256-CBC encryption/decryption roundtrip
   - Test IV randomness
   - Test KeyStore integration (mocked)

3. **Repositories** - Data layer logic
   - Test LocationRepository location retrieval
   - Test NetworkRepository transmission and queuing
   - Test ConfigRepository preference management

4. **ViewModels** - UI state management
   - Test state transitions (tracking on/off)
   - Test user interactions (toggle, save settings)
   - Test error handling and recovery

**Example Test:**
```kotlin
@Test
fun `collectAndTransmit should encrypt and transmit location successfully`() = runTest {
    // Given
    val mockLocation = LocationData(37.7749, -122.4194, 10.5f, null, null, null, "fused")
    val mockPayload = EncryptedPayload("device123", "2025-10-15T12:00:00Z", "encrypted", "iv")

    coEvery { locationRepository.getCurrentLocation() } returns Result.success(mockLocation)
    every { cryptoManager.encrypt(mockLocation) } returns mockPayload
    coEvery { networkRepository.transmit(mockPayload) } returns Result.success(Unit)

    // When
    val result = locationTrackingUseCase.collectAndTransmit()

    // Then
    assertThat(result.isSuccess).isTrue()
    coVerify { locationRepository.getCurrentLocation() }
    verify { cryptoManager.encrypt(mockLocation) }
    coVerify { networkRepository.transmit(mockPayload) }
}
```

### 8.2 Instrumentation Tests (androidTest/)

**Target Coverage:** Critical user flows and Android-specific components

**Test Framework:** AndroidX Test + Compose UI Test + Room Testing

**Key Test Targets:**
1. **Compose UI Tests**
   - HomeScreen: Toggle tracking, navigate to settings
   - SettingsScreen: Update webhook URL, change interval

2. **Room Database Tests**
   - TransmissionQueueDao CRUD operations
   - Query performance with 100+ entries

3. **WorkManager Tests**
   - LocationWorker execution
   - RetryWorker queue processing

**Example UI Test:**
```kotlin
@Test
fun homeScreen_toggleTracking_updatesState() {
    composeTestRule.setContent {
        PhoneManagerTheme {
            HomeScreen(navController = rememberNavController())
        }
    }

    // Initially tracking is off
    composeTestRule.onNodeWithText("Tracking: Inactive").assertIsDisplayed()

    // Click toggle
    composeTestRule.onNodeWithTag("tracking_toggle").performClick()

    // Verify state updated
    composeTestRule.onNodeWithText("Tracking: Active").assertIsDisplayed()
}
```

### 8.3 Manual Testing

**Battery Testing:**
- Use Battery Historian to analyze power consumption
- Test with device in Doze mode (`adb shell dumpsys deviceidle force-idle`)
- Monitor battery drain over 8 hours with tracking enabled

**Location Testing:**
- Test with mock location provider (developer options)
- Test with GPS off, WiFi only, cell tower only
- Test in airplane mode (should queue transmissions)

**Permission Testing:**
- Test permission denied scenarios
- Test Android 13+ granular location permissions
- Test background location permission flow

**Network Testing:**
- Test with network disabled (airplane mode)
- Test with slow network (Chrome DevTools throttling via proxy)
- Test with n8n webhook returning 4xx/5xx errors

### 8.4 Testing Specialist Section

**Status:** Simple testing approach (handled inline)

**Rationale:**
- App has straightforward testing needs
- Standard Android testing tools sufficient
- No complex E2E scenarios or critical UI flows

**Testing Approach:**
- Unit tests for business logic (80% coverage goal)
- Instrumentation tests for UI and database
- Manual testing for battery and permissions
- Google Play Console pre-launch reports for device compatibility

---

## 9. Deployment and Operations

### 9.1 Build Variants

**Debug Build:**
- Minification disabled
- Logging enabled (Timber debug tree)
- Network logging enabled (OkHttp interceptor)
- Debuggable APK

**Release Build:**
- ProGuard/R8 minification enabled
- Logging disabled (Timber release tree)
- Code obfuscation enabled
- Signed APK/AAB

**ProGuard Rules (proguard-rules.pro):**
```proguard
# Keep Koin classes
-keep class org.koin.** { *; }

# Keep Retrofit models
-keep class com.phonemanager.domain.model.** { *; }

# Keep Room entities
-keepclassmembers class * extends androidx.room.RoomDatabase {
    public static ** DATABASE_NAME;
}

# Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
```

### 9.2 Signing Configuration

**Key Generation:**
```bash
keytool -genkey -v -keystore phone-manager-release.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias phone-manager
```

**build.gradle.kts:**
```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file("../phone-manager-release.jks")
            storePassword = System.getenv("KEYSTORE_PASSWORD")
            keyAlias = "phone-manager"
            keyPassword = System.getenv("KEY_PASSWORD")
        }
    }

    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
        }
    }
}
```

### 9.3 Google Play Console Deployment

**Internal Testing Track:**
1. Build release AAB: `./gradlew bundleRelease`
2. Upload to Google Play Console → Internal Testing
3. Add test users (email addresses)
4. Distribute to testers
5. Collect feedback and fix critical bugs

**Production Release:**
1. Promote from Internal Testing → Production
2. Create store listing (title, description, screenshots)
3. Complete Privacy Policy (required for location permissions)
4. Submit for review (typically 1-3 days)

**Store Listing Requirements:**
- App name: "Phone Manager"
- Short description (80 chars): "Secure background location tracking with encrypted transmission to n8n workflows"
- Full description: Detail features, security, battery optimization
- Screenshots: Home screen, Settings screen, notification
- Privacy policy: Explain location data usage, encryption, no third-party sharing

### 9.4 Version Management

**Semantic Versioning:**
- `versionName = "1.0.0"` (user-visible)
- `versionCode = 1` (integer, increments with each release)

**Version Bump Strategy:**
- Major (1.x.x): Breaking changes, major features
- Minor (x.1.x): New features, non-breaking changes
- Patch (x.x.1): Bug fixes, minor improvements

### 9.5 Release Checklist

- [ ] All tests passing (unit + instrumentation)
- [ ] ProGuard rules verified (test release build)
- [ ] Signing configuration correct
- [ ] Version code and name incremented
- [ ] Changelog updated
- [ ] Privacy policy updated (if needed)
- [ ] Store listing reviewed (screenshots, description)
- [ ] Battery testing completed (no excessive drain)
- [ ] Permission flows tested (Android 13+)
- [ ] n8n webhook integration verified
- [ ] Internal testing completed (3+ testers)

### 9.6 DevOps Specialist Section

**Status:** No CI/CD (handled inline)

**Rationale:**
- User requested "no CI"
- Manual builds acceptable for single developer
- Google Play Console provides basic deployment pipeline

**Manual Build and Deploy Process:**
1. Run tests: `./gradlew test`
2. Build release: `./gradlew bundleRelease`
3. Upload AAB to Google Play Console manually
4. Deploy to Internal Testing or Production track

**Future CI/CD Recommendations (if needed later):**
- GitHub Actions workflow for automated testing
- Fastlane for automated Google Play deployment
- Gradle Play Publisher plugin

---

## 10. Security

### 10.1 Security Architecture

**Encryption Layer:**
- **Algorithm:** AES-256-CBC (industry standard symmetric encryption)
- **Key Storage:** Android KeyStore (hardware-backed when available)
- **IV Generation:** SecureRandom for each encryption operation (prevents pattern detection)
- **Padding:** PKCS7 (standard padding for block ciphers)

**Data Protection:**
- **Location Data:** Never stored in plaintext (encrypted immediately after collection)
- **Configuration:** EncryptedSharedPreferences for webhook URL and settings
- **Encryption Key:** Stored in KeyStore, never exposed to app code or logs
- **Network:** HTTPS-only communication (no HTTP fallback)

**Permission Model:**
- **Runtime Permissions:** Request fine location + background location dynamically
- **Minimal Permissions:** Only request necessary permissions (no camera, contacts, etc.)
- **Permission Rationale:** Explain why each permission is needed before requesting

### 10.2 Threat Model

**Threats Considered:**

1. **Network Interception (Man-in-the-Middle)**
   - **Mitigation:** HTTPS-only, optional certificate pinning
   - **Risk:** Low (TLS encryption standard)

2. **Device Compromise (Root/Malware)**
   - **Mitigation:** KeyStore hardware backing, StrongBox on supported devices
   - **Risk:** Medium (if device rooted, KeyStore may be bypassed)
   - **Note:** Can't protect against compromised device (root access defeats all app security)

3. **Data Leakage via Logs**
   - **Mitigation:** No plaintext location in logs, Timber release tree disabled in production
   - **Risk:** Low (strict logging discipline)

4. **Unauthorized Access to n8n Webhook**
   - **Mitigation:** User-controlled webhook URL, encryption key shared separately
   - **Risk:** Low (user responsibility to secure n8n endpoint)

5. **Local Data Extraction (Backup/File Access)**
   - **Mitigation:** EncryptedSharedPreferences, Room database not human-readable
   - **Risk:** Low (encryption at rest)

**Out of Scope:**
- Protection against device firmware exploits
- Protection against physical device access (unlocked device)
- Protection if user shares encryption key insecurely

### 10.3 Security Best Practices

**Code Security:**
```kotlin
// ✅ DO: Use KeyStore for key storage
val keyStore = KeyStore.getInstance("AndroidKeyStore")

// ❌ DON'T: Hardcode encryption keys
const val SECRET_KEY = "my-secret-key" // NEVER DO THIS

// ✅ DO: Generate random IVs
val iv = ByteArray(16)
SecureRandom().nextBytes(iv)

// ❌ DON'T: Reuse IVs
val iv = byteArrayOf(0, 0, 0, ...) // NEVER DO THIS

// ✅ DO: Use HTTPS
@POST("https://n8n.example.com/webhook")

// ❌ DON'T: Allow HTTP fallback
if (url.startsWith("http://")) { ... } // NEVER ALLOW
```

**Network Security Config (res/xml/network_security_config.xml):**
```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <base-config cleartextTrafficPermitted="false" />
    <!-- Optional: Certificate Pinning -->
    <domain-config>
        <domain includeSubdomains="true">n8n.example.com</domain>
        <pin-set>
            <pin digest="SHA-256">base64-encoded-pin</pin>
        </pin-set>
    </domain-config>
</network-security-config>
```

**AndroidManifest.xml Security:**
```xml
<application
    android:allowBackup="false"
    android:usesCleartextTraffic="false"
    android:networkSecurityConfig="@xml/network_security_config">
```

### 10.4 Privacy Considerations

**Data Minimization:**
- Only collect necessary location data (lat/lon, accuracy, timestamp)
- No user account or personal information collected
- No location history stored permanently (only queue for failed transmissions)

**User Control:**
- User can disable tracking at any time (toggle on Home screen)
- User controls webhook destination (n8n URL)
- User can delete app to remove all local data

**Privacy Policy Requirements:**
- Clearly state location data collection and purpose
- Explain encryption and transmission to user-specified webhook
- Specify no third-party data sharing (only user's n8n instance)
- Provide contact information for privacy inquiries

**Google Play Policy Compliance:**
- Background location permission requires prominent disclosure (Home screen explanation)
- Privacy Policy URL required in Google Play listing
- Data Safety form must accurately describe data collection and sharing

### 10.5 Security Specialist Section

**Status:** Core security handled inline (no specialist needed)

**Rationale:**
- Security requirements are straightforward (AES encryption, KeyStore, HTTPS)
- Standard Android security practices sufficient
- No compliance requirements (HIPAA/PCI/SOC2)
- User-controlled webhook (no server-side security to manage)

**Security Validation:**
- Unit tests for CryptoManager (encryption/decryption roundtrip)
- Manual testing of network traffic (verify HTTPS, inspect payload encryption)
- Code review for hardcoded secrets or logging sensitive data
- ProGuard rules to prevent reverse engineering

---

## Specialist Sections Summary

**Testing:** Simple approach (inline) - Standard Android testing tools sufficient
**DevOps:** No CI/CD (inline) - Manual builds acceptable for single developer
**Security:** Core security (inline) - Standard encryption and KeyStore practices sufficient

**Rationale for Inline Approach:**
All three specialist areas have straightforward requirements that align with Android best practices. No need for separate specialist agent review at this stage. Can revisit if requirements become more complex (e.g., HIPAA compliance, multi-region deployment, complex E2E testing).

---

_Generated using BMad Method Solution Architecture workflow on 2025-10-15_

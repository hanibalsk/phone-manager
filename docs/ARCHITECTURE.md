# Phone Manager Architecture

## Overview

Phone Manager follows **Clean Architecture** principles with **MVVM** (Model-View-ViewModel) pattern for the presentation layer. The application is organized into three distinct layers with clear boundaries and dependencies flowing inward.

**Backend Server**: [phone-manager-backend](https://github.com/hanibalsk/phone-manager-backend)

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────┐
│                        PRESENTATION LAYER                           │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐                  │
│  │   Screens   │  │ ViewModels  │  │ Navigation  │                  │
│  │  (Compose)  │  │ (StateFlow) │  │  (NavHost)  │                  │
│  └─────────────┘  └─────────────┘  └─────────────┘                  │
├─────────────────────────────────────────────────────────────────────┤
│                          DOMAIN LAYER                               │
│  ┌─────────────┐  ┌─────────────────────┐  ┌─────────────┐          │
│  │   Models    │  │ Repository Interfaces│  │  Use Cases  │          │
│  └─────────────┘  └─────────────────────┘  └─────────────┘          │
├─────────────────────────────────────────────────────────────────────┤
│                           DATA LAYER                                │
│  ┌──────────┐  ┌───────────┐  ┌───────────┐  ┌─────────────┐        │
│  │ Room DB  │  │ API Svc   │  │ DataStore │  │SecureStorage│        │
│  │ (DAOs)   │  │ (Ktor)    │  │(Prefs)    │  │(Encrypted)  │        │
│  └──────────┘  └───────────┘  └───────────┘  └─────────────┘        │
│  ┌────────────────────────────────────────────────────────────┐     │
│  │                 Repository Implementations                  │     │
│  └────────────────────────────────────────────────────────────┘     │
└─────────────────────────────────────────────────────────────────────┘
```

## Layer Responsibilities

### Presentation Layer (`ui/`)

The presentation layer handles all user interface concerns using Jetpack Compose.

**Components:**
- **Screens**: Composable functions representing full-screen UI
- **ViewModels**: State management with `StateFlow` and business logic coordination
- **Navigation**: Compose Navigation with `NavHost` and route definitions
- **Components**: Reusable UI building blocks

**Key Characteristics:**
- Single Activity architecture (`MainActivity`)
- Unidirectional data flow (events up, state down)
- ViewModels scoped to navigation destinations
- UI state represented as immutable data classes

```kotlin
// Example ViewModel pattern
@HiltViewModel
class GeofencesViewModel @Inject constructor(
    private val geofenceRepository: GeofenceRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(GeofencesUiState())
    val uiState: StateFlow<GeofencesUiState> = _uiState.asStateFlow()

    val geofences: StateFlow<List<Geofence>> = geofenceRepository.observeGeofences()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
```

### Domain Layer (`domain/`)

The domain layer contains business logic and entity definitions.

**Components:**
- **Models**: Core business entities (`Geofence`, `Webhook`, `ProximityAlert`)
- **Repository Interfaces**: Contracts for data operations
- **Use Cases**: Business logic encapsulation (when needed)

**Key Characteristics:**
- Pure Kotlin (no Android dependencies)
- Defines contracts that data layer implements
- Contains validation and business rules

```kotlin
// Domain model example
data class Geofence(
    val id: String,
    val deviceId: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Int,
    val transitionTypes: Set<TransitionType>,
    val webhookId: String?,
    val active: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
)

enum class TransitionType { ENTER, EXIT, DWELL }
```

### Data Layer (`data/`)

The data layer manages all data sources and persistence.

**Components:**
- **Database**: Room entities, DAOs, migrations
- **Network**: Ktor HTTP client, API services, DTOs
- **Preferences**: DataStore for user preferences
- **Security**: SecureStorage for encrypted data
- **Repositories**: Implementation of domain interfaces

**Key Characteristics:**
- Local-first architecture (data available offline)
- Server sync when network available
- Automatic conflict resolution
- Encrypted storage for sensitive data

```kotlin
// Repository implementation pattern
@Singleton
class GeofenceRepositoryImpl @Inject constructor(
    private val geofenceDao: GeofenceDao,
    private val geofenceApiService: GeofenceApiService,
    private val networkManager: NetworkManager,
) : GeofenceRepository {

    override fun observeGeofences(): Flow<List<Geofence>> =
        geofenceDao.observeGeofencesByDevice(deviceId)
            .map { entities -> entities.map { it.toDomain() } }

    override suspend fun createGeofence(...): Result<Geofence> {
        // 1. Save locally first
        geofenceDao.insert(geofence.toEntity())

        // 2. Sync to server if network available
        if (networkManager.isNetworkAvailable()) {
            geofenceApiService.createGeofence(request)
        }

        return Result.success(geofence)
    }
}
```

## Dependency Injection

Phone Manager uses **Hilt** for dependency injection with the following module structure:

```
di/
├── DatabaseModule.kt      # Room database, DAOs, migrations
├── NetworkModule.kt       # HttpClient, API services
├── RepositoryModule.kt    # Repository bindings
├── LocationModule.kt      # Location services
├── PreferencesModule.kt   # DataStore
├── PermissionModule.kt    # Permission manager
├── GeofenceModule.kt      # Geofencing client
└── WorkManagerModule.kt   # Worker factories
```

**Module Example:**
```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindGeofenceRepository(
        impl: GeofenceRepositoryImpl
    ): GeofenceRepository

    @Binds
    @Singleton
    abstract fun bindWebhookRepository(
        impl: WebhookRepositoryImpl
    ): WebhookRepository
}
```

## Navigation Architecture

Single Activity with Compose Navigation:

```kotlin
sealed class Screen(val route: String) {
    object Registration : Screen("registration")
    object Home : Screen("home")
    object GroupMembers : Screen("group_members")
    object Settings : Screen("settings")
    object Map : Screen("map")
    object History : Screen("history")
    object Alerts : Screen("alerts")
    object CreateAlert : Screen("create_alert")
    object Geofences : Screen("geofences")
    object CreateGeofence : Screen("create_geofence")
    object Webhooks : Screen("webhooks")
    object CreateWebhook : Screen("create_webhook")
}
```

**Navigation Flow:**
```
Registration → Home ─┬→ Group Members
                     ├→ Settings
                     ├→ Map
                     ├→ History
                     ├→ Alerts → Create Alert
                     ├→ Geofences → Create Geofence
                     └→ Webhooks → Create Webhook
```

## Data Flow

### Location Tracking Flow

```
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│   Location   │ →  │  Location    │ →  │   Location   │
│   Service    │    │  Manager     │    │  Repository  │
└──────────────┘    └──────────────┘    └──────────────┘
       │                                       │
       ↓                                       ↓
┌──────────────┐                       ┌──────────────┐
│ Foreground   │                       │   Room DB    │
│ Notification │                       │  (Local)     │
└──────────────┘                       └──────────────┘
                                               │
                                               ↓
                                       ┌──────────────┐
                                       │ Upload Queue │
                                       └──────────────┘
                                               │
                                               ↓
                                       ┌──────────────┐
                                       │ API Service  │
                                       │  (Server)    │
                                       └──────────────┘
```

### Geofence Event Flow

```
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│   Geofence   │ →  │  Broadcast   │ →  │  Geofence    │
│   Client     │    │  Receiver    │    │  Repository  │
└──────────────┘    └──────────────┘    └──────────────┘
                                               │
                           ┌───────────────────┼───────────────────┐
                           ↓                   ↓                   ↓
                    ┌──────────────┐    ┌──────────────┐    ┌──────────────┐
                    │  Room DB     │    │  Webhook     │    │Notification  │
                    │ (Event Log)  │    │  Dispatch    │    │  Manager     │
                    └──────────────┘    └──────────────┘    └──────────────┘
```

## Background Processing

### Services

**LocationTrackingService** (Foreground Service)
- Persistent notification for ongoing location tracking
- Periodic GPS capture using FusedLocationProvider
- Service health monitoring with automatic recovery
- Secret mode notification variants

### WorkManager

**Workers:**
- `QueueProcessingWorker`: Process upload queue, retry failed uploads
- `ServiceHealthCheckWorker`: Monitor and restart service if needed

**Constraints:**
- Network availability for uploads
- Battery optimization compliance

### Receivers

- `BootReceiver`: Auto-start service on device boot
- `GeofenceBroadcastReceiver`: Handle geofence transition events

## Database Architecture

### Room Database (Version 7)

**Entities:**
1. `LocationEntity` - GPS location records
2. `LocationQueueEntity` - Upload queue
3. `ProximityAlertEntity` - Alert configurations
4. `GeofenceEntity` - Geofence definitions
5. `GeofenceEventEntity` - Geofence trigger events
6. `WebhookEntity` - Webhook configurations

**Migration Strategy:**
- Sequential migrations (v2→v3→v4→v5→v6→v7)
- Schema validation in tests
- Non-destructive migrations preserve data

```kotlin
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS webhooks (
                id TEXT PRIMARY KEY NOT NULL,
                ownerDeviceId TEXT NOT NULL,
                name TEXT NOT NULL,
                targetUrl TEXT NOT NULL,
                secret TEXT NOT NULL,
                enabled INTEGER NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL
            )
        """)
    }
}
```

## Network Architecture

### HTTP Client Configuration

```kotlin
val httpClient = HttpClient(Android) {
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
    install(HttpTimeout) {
        requestTimeoutMillis = 30_000
    }
    defaultRequest {
        header("X-API-Key", apiKey)
        contentType(ContentType.Application.Json)
    }
}
```

### API Service Pattern

```kotlin
interface GeofenceApiService {
    suspend fun createGeofence(request: CreateGeofenceRequest): Result<GeofenceDto>
    suspend fun listGeofences(deviceId: String): Result<ListGeofencesResponse>
    suspend fun updateGeofence(id: String, request: UpdateGeofenceRequest): Result<GeofenceDto>
    suspend fun deleteGeofence(id: String): Result<Unit>
}
```

## Security Architecture

### Secure Storage

```kotlin
class SecureStorage @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedPrefs = EncryptedSharedPreferences.create(
        context, "secure_prefs", masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
}
```

### Security Measures

1. **Device Identity**: UUID stored in encrypted preferences
2. **API Authentication**: X-API-Key header with secure storage
3. **Webhook Security**: HMAC-SHA256 signatures
4. **Transport Security**: HTTPS-only communication
5. **Permission Model**: Runtime permissions with rationale dialogs

## Testing Architecture

### Test Structure

```
app/src/test/                    # Unit tests
├── data/repository/             # Repository tests
├── ui/*/                        # ViewModel tests
└── util/                        # Utility tests

app/src/androidTest/             # Instrumented tests
├── data/database/               # DAO tests
└── ui/                          # UI tests
```

### Testing Patterns

- **ViewModels**: StateFlow assertions with Turbine
- **Repositories**: MockK for dependencies
- **DAOs**: In-memory Room database
- **Flows**: Coroutines test dispatchers

## Design Patterns

| Pattern | Usage |
|---------|-------|
| Repository | Data source abstraction |
| MVVM | UI state management |
| Singleton | Services, repositories |
| Factory | Hilt providers |
| Observer | Flow/StateFlow reactive updates |
| Strategy | Notification channels, location providers |
| Builder | Request/Response DTOs |

## Performance Considerations

1. **Database**
   - Index on frequently queried columns
   - Paging for large datasets
   - Background thread operations

2. **Network**
   - Batch uploads when possible
   - Retry with exponential backoff
   - Offline-first design

3. **Location**
   - Fused location provider for battery efficiency
   - Configurable tracking intervals
   - Smart location updates based on movement

4. **Memory**
   - LazyColumn for lists
   - Image caching
   - Lifecycle-aware coroutines

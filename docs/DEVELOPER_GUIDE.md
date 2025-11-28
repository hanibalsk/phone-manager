# Phone Manager Developer Guide

## Prerequisites

Before you begin, ensure you have the following installed:

- **Android Studio**: Hedgehog (2023.1.1) or newer
- **JDK**: Version 17 or newer
- **Android SDK**: API level 26 minimum
- **Git**: For version control
- **Google Maps API Key**: For map features

## Getting Started

### 1. Clone the Repository

```bash
git clone https://github.com/your-org/phone-manager.git
cd phone-manager
```

### 2. Configure API Keys

Create or edit `local.properties` in the project root:

```properties
# Google Maps API Key (required for map features)
MAPS_API_KEY=your_google_maps_api_key

# Backend API Configuration
API_BASE_URL=https://your-api-server.com
API_KEY=your_api_key

# Optional: Debug settings
DEBUG_LOG_ENABLED=true
```

### 3. Sync and Build

Open the project in Android Studio, then:

```bash
# Sync Gradle
./gradlew --refresh-dependencies

# Build debug APK
./gradlew assembleDebug

# Run tests
./gradlew test
```

### 4. Run the App

1. Connect an Android device or start an emulator
2. Click "Run" in Android Studio or:
   ```bash
   ./gradlew installDebug
   ```

## Project Structure

```
phone-manager/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/three/two/bit/phonemanager/
│   │   │   │   ├── analytics/       # Event tracking
│   │   │   │   ├── data/            # Data layer
│   │   │   │   │   ├── database/    # Room DAOs
│   │   │   │   │   ├── model/       # Entities
│   │   │   │   │   ├── preferences/ # DataStore
│   │   │   │   │   └── repository/  # Repositories
│   │   │   │   ├── di/              # Hilt modules
│   │   │   │   ├── domain/          # Domain models
│   │   │   │   ├── geofence/        # Geofencing
│   │   │   │   ├── location/        # Location capture
│   │   │   │   ├── network/         # API services
│   │   │   │   ├── permission/      # Permissions
│   │   │   │   ├── queue/           # Work queue
│   │   │   │   ├── receiver/        # Broadcast receivers
│   │   │   │   ├── security/        # Secure storage
│   │   │   │   ├── service/         # Foreground service
│   │   │   │   ├── ui/              # Compose screens
│   │   │   │   ├── util/            # Utilities
│   │   │   │   └── watchdog/        # Service monitoring
│   │   │   └── res/                 # Resources
│   │   └── test/                    # Unit tests
│   └── build.gradle.kts
├── docs/                            # Documentation
├── gradle/                          # Gradle wrapper
└── build.gradle.kts                 # Root build file
```

## Architecture Overview

### Layer Responsibilities

**Presentation Layer (`ui/`)**
- Jetpack Compose screens
- ViewModels with StateFlow
- Navigation with Compose NavHost

**Domain Layer (`domain/`)**
- Business entities (data classes)
- Repository interfaces
- Validation rules

**Data Layer (`data/`)**
- Room database (entities, DAOs)
- API services (Ktor)
- Repository implementations
- DataStore preferences

### Dependency Flow

```
UI → Domain → Data
     ↑         ↓
     └─────────┘ (via Repository interfaces)
```

## Development Workflow

### Creating a New Feature

1. **Define Domain Model** (`domain/model/`)
   ```kotlin
   data class Feature(
       val id: String,
       val name: String,
       val enabled: Boolean,
   )
   ```

2. **Create Room Entity** (`data/model/`)
   ```kotlin
   @Entity(tableName = "features")
   data class FeatureEntity(
       @PrimaryKey val id: String,
       val name: String,
       val enabled: Boolean,
   )
   ```

3. **Create DAO** (`data/database/`)
   ```kotlin
   @Dao
   interface FeatureDao {
       @Query("SELECT * FROM features")
       fun observeAll(): Flow<List<FeatureEntity>>

       @Insert(onConflict = OnConflictStrategy.REPLACE)
       suspend fun insert(feature: FeatureEntity)
   }
   ```

4. **Update Database** (`data/database/AppDatabase.kt`)
   - Add entity to `@Database` annotation
   - Create migration if needed
   - Add DAO abstract method

5. **Create Repository Interface** (`domain/` or `data/repository/`)
   ```kotlin
   interface FeatureRepository {
       fun observeFeatures(): Flow<List<Feature>>
       suspend fun createFeature(name: String): Result<Feature>
   }
   ```

6. **Implement Repository** (`data/repository/`)
   ```kotlin
   @Singleton
   class FeatureRepositoryImpl @Inject constructor(
       private val dao: FeatureDao,
   ) : FeatureRepository { ... }
   ```

7. **Register in DI Module** (`di/`)
   ```kotlin
   @Binds
   abstract fun bindFeatureRepository(
       impl: FeatureRepositoryImpl
   ): FeatureRepository
   ```

8. **Create ViewModel** (`ui/feature/`)
   ```kotlin
   @HiltViewModel
   class FeatureViewModel @Inject constructor(
       private val repository: FeatureRepository,
   ) : ViewModel() { ... }
   ```

9. **Create Screen** (`ui/feature/`)
   ```kotlin
   @Composable
   fun FeatureScreen(
       viewModel: FeatureViewModel = hiltViewModel(),
   ) { ... }
   ```

10. **Add Navigation Route** (`ui/navigation/`)

### Database Migrations

When adding new tables or columns:

1. **Increment database version** in `AppDatabase.kt`
2. **Create migration** object:
   ```kotlin
   val MIGRATION_7_8 = object : Migration(7, 8) {
       override fun migrate(db: SupportSQLiteDatabase) {
           db.execSQL("""
               CREATE TABLE IF NOT EXISTS new_table (
                   id TEXT PRIMARY KEY NOT NULL,
                   name TEXT NOT NULL
               )
           """)
       }
   }
   ```
3. **Register migration** in `DatabaseModule.kt`

### Adding API Endpoints

1. **Create DTO models** (`network/models/`)
   ```kotlin
   @Serializable
   data class FeatureDto(
       val id: String,
       val name: String,
   )
   ```

2. **Create API service interface** (`network/`)
   ```kotlin
   interface FeatureApiService {
       suspend fun getFeatures(): Result<List<FeatureDto>>
       suspend fun createFeature(request: CreateFeatureRequest): Result<FeatureDto>
   }
   ```

3. **Implement service** with Ktor
4. **Register in NetworkModule**

## Code Style

### Kotlin Guidelines

- Follow [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use Spotless for formatting: `./gradlew spotlessApply`
- Prefer `val` over `var`
- Use data classes for models
- Leverage Kotlin coroutines for async work

### Compose Guidelines

- Extract reusable components
- Use `remember` for expensive computations
- Prefer `LazyColumn` for lists
- Handle loading, error, and empty states
- Use `collectAsStateWithLifecycle()` for Flows

### Testing Guidelines

- Write unit tests for ViewModels
- Mock dependencies with MockK
- Test Flows with Turbine
- Use `runTest` for coroutine tests

## Testing

### Running Tests

```bash
# All unit tests
./gradlew test

# Specific test class
./gradlew test --tests "*.RegistrationViewModelTest"

# Instrumented tests (requires device/emulator)
./gradlew connectedAndroidTest

# Test coverage report
./gradlew jacocoTestReport
```

### Writing Unit Tests

```kotlin
class FeatureViewModelTest {
    @MockK
    private lateinit var repository: FeatureRepository

    private lateinit var viewModel: FeatureViewModel

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        viewModel = FeatureViewModel(repository)
    }

    @Test
    fun `loadFeatures updates state`() = runTest {
        // Given
        coEvery { repository.observeFeatures() } returns flowOf(listOf(testFeature))

        // When
        viewModel.features.test {
            // Then
            assertEquals(listOf(testFeature), awaitItem())
        }
    }
}
```

### Testing Flows

```kotlin
@Test
fun `state updates on action`() = runTest {
    viewModel.uiState.test {
        // Initial state
        assertEquals(UiState(), awaitItem())

        // Trigger action
        viewModel.doSomething()

        // Updated state
        assertEquals(UiState(loading = true), awaitItem())
        assertEquals(UiState(loading = false, data = result), awaitItem())
    }
}
```

## Debugging

### Logging

Use Timber for logging:

```kotlin
Timber.d("Debug message")
Timber.i("Info message")
Timber.w(exception, "Warning message")
Timber.e(exception, "Error message")
```

### Network Inspection

Enable logging in debug builds:

```kotlin
install(Logging) {
    level = LogLevel.ALL
    logger = object : Logger {
        override fun log(message: String) {
            Timber.tag("HTTP").d(message)
        }
    }
}
```

### Database Inspection

Use Android Studio's Database Inspector:
1. Run app on debug build
2. View → Tool Windows → App Inspection
3. Select Database Inspector tab

## Common Tasks

### Updating Dependencies

1. Edit version in `libs.versions.toml`
2. Sync Gradle
3. Test thoroughly

### Adding New Screens

1. Create Composable in `ui/{feature}/`
2. Create ViewModel if needed
3. Add route to `Screen` sealed class
4. Add `composable` call in `PhoneManagerNavHost`

### Handling Permissions

```kotlin
@Composable
fun FeatureWithPermission() {
    val permissionState = rememberPermissionState(
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    when {
        permissionState.status.isGranted -> {
            // Show feature
        }
        permissionState.status.shouldShowRationale -> {
            // Show rationale
        }
        else -> {
            // Request permission
            LaunchedEffect(Unit) {
                permissionState.launchPermissionRequest()
            }
        }
    }
}
```

## Build Variants

| Variant | Use Case |
|---------|----------|
| debug | Development and testing |
| release | Production builds |

### Release Build

```bash
# Build release APK
./gradlew assembleRelease

# Build release bundle
./gradlew bundleRelease
```

## Troubleshooting

### Build Issues

**Gradle sync fails:**
```bash
./gradlew --stop
./gradlew clean
./gradlew --refresh-dependencies
```

**KSP issues:**
- Clean build: `./gradlew clean`
- Invalidate caches in Android Studio

### Runtime Issues

**Service not starting:**
- Check foreground service permissions
- Verify notification channel exists
- Check battery optimization settings

**Location not updating:**
- Verify location permissions
- Check FusedLocationProviderClient initialization
- Ensure GPS is enabled

**Database issues:**
- Check migration path
- Verify entity annotations
- Use Database Inspector for debugging

## Contributing

### Pull Request Process

1. Create feature branch from `main`
2. Write tests for new functionality
3. Ensure all tests pass
4. Run Spotless: `./gradlew spotlessApply`
5. Update documentation if needed
6. Create PR with clear description

### Commit Messages

Follow conventional commits:
```
feat: Add webhook configuration screen
fix: Resolve geofence trigger issue
docs: Update API reference
test: Add ViewModel unit tests
refactor: Extract location capture logic
```

## Resources

- [Android Developer Guides](https://developer.android.com/guide)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-guide.html)
- [Hilt Dependency Injection](https://dagger.dev/hilt/)
- [Room Database](https://developer.android.com/training/data-storage/room)

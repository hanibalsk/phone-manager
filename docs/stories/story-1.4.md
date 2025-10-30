# Story 1.4: Network Layer & Server Communication

**Status:** Ready for Implementation
**Epic:** 1 - Background Location Tracking Service
**Priority:** MVP - Critical Path
**Complexity:** Medium
**Estimated Effort:** 4-6 days

## Story

As a developer,
I want to implement a robust network layer for transmitting location data to a remote server,
so that location data can be reliably sent to the configured server endpoint with proper error handling.

## Acceptance Criteria

1. **Retrofit Setup:**
   - [ ] Retrofit client configured with base URL from DataStore
   - [ ] OkHttp client with proper timeouts and interceptors
   - [ ] HTTPS enforced (reject plain HTTP)
   - [ ] TLS certificate validation enabled
   - [ ] Proper Content-Type and Accept headers

2. **API Endpoints:**
   - [ ] POST /api/locations endpoint for batch upload
   - [ ] Request body includes: deviceId, locations[], timestamp
   - [ ] Response handling: 200/201 (success), 400 (bad request), 401 (unauthorized), 500 (server error)
   - [ ] Proper error messages for all failure scenarios

3. **Configuration Management:**
   - [ ] Server URL stored in DataStore
   - [ ] API authentication token/key configurable
   - [ ] Configuration survives app restart
   - [ ] Default configuration provided
   - [ ] Configuration validation before use

4. **Network State Monitoring:**
   - [ ] Check network connectivity before requests
   - [ ] Handle network type changes (WiFi/Cellular)
   - [ ] Provide network state to repository layer

5. **Data Models:**
   - [ ] Request DTOs (Data Transfer Objects)
   - [ ] Response DTOs
   - [ ] Proper JSON serialization with Moshi
   - [ ] Field naming strategy (camelCase/snake_case)

6. **Error Handling:**
   - [ ] Network timeouts handled gracefully
   - [ ] HTTP error codes mapped to meaningful errors
   - [ ] JSON parsing errors handled
   - [ ] Connection errors handled

7. **Testing:**
   - [ ] Unit tests for API service
   - [ ] Integration tests with MockWebServer
   - [ ] Test various network conditions
   - [ ] Test server error scenarios

8. **URL Security Validation:**
   - [ ] Server URL must use HTTPS protocol (reject HTTP)
   - [ ] URL format validation before saving configuration
   - [ ] Certificate pinning optional but recommended for production
   - [ ] Invalid SSL certificates rejected (no insecure bypass)
   - [ ] Clear error message when HTTP URL provided: "HTTPS required for security"

## Tasks / Subtasks

### Task 1: Add Dependencies
```kotlin
dependencies {
    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.9.0")
    
    // OkHttp
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    
    // Moshi
    implementation("com.squareup.moshi:moshi-kotlin:1.15.0")
    ksp("com.squareup.moshi:moshi-kotlin-codegen:1.15.0")
    
    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    
    // Testing
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
}
```

### Task 2: Create Data Models

**Request DTOs:**
```kotlin
@JsonClass(generateAdapter = true)
data class LocationUploadRequest(
    @Json(name = "device_id")
    val deviceId: String,
    
    @Json(name = "locations")
    val locations: List<LocationDto>,
    
    @Json(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
data class LocationDto(
    @Json(name = "latitude")
    val latitude: Double,
    
    @Json(name = "longitude")
    val longitude: Double,
    
    @Json(name = "accuracy")
    val accuracy: Float,
    
    @Json(name = "timestamp")
    val timestamp: Long,
    
    @Json(name = "altitude")
    val altitude: Double?,
    
    @Json(name = "bearing")
    val bearing: Float?,
    
    @Json(name = "speed")
    val speed: Float?,
    
    @Json(name = "provider")
    val provider: String,
    
    @Json(name = "battery_level")
    val batteryLevel: Int?
)

@JsonClass(generateAdapter = true)
data class LocationUploadResponse(
    @Json(name = "success")
    val success: Boolean,
    
    @Json(name = "message")
    val message: String?,
    
    @Json(name = "received_count")
    val receivedCount: Int
)
```

**Mappers:**
```kotlin
fun Location.toDto(batteryLevel: Int? = null): LocationDto {
    return LocationDto(
        latitude = latitude,
        longitude = longitude,
        accuracy = accuracy,
        timestamp = timestamp,
        altitude = altitude,
        bearing = bearing,
        speed = speed,
        provider = provider,
        batteryLevel = batteryLevel
    )
}
```

### Task 3: Create API Service
```kotlin
interface LocationApiService {
    
    @POST("api/locations")
    suspend fun uploadLocations(
        @Body request: LocationUploadRequest
    ): Response<LocationUploadResponse>
    
    @GET("api/health")
    suspend fun healthCheck(): Response<Unit>
}
```

### Task 4: Create Network Configuration
```kotlin
@JsonClass(generateAdapter = true)
data class NetworkConfig(
    @Json(name = "server_url")
    val serverUrl: String = "https://example.com",
    
    @Json(name = "api_token")
    val apiToken: String? = null,
    
    @Json(name = "timeout_seconds")
    val timeoutSeconds: Int = 30,
    
    @Json(name = "max_retries")
    val maxRetries: Int = 3
)
```

### Task 5: Implement DataStore Configuration
```kotlin
object ConfigKeys {
    val SERVER_URL = stringPreferencesKey("server_url")
    val API_TOKEN = stringPreferencesKey("api_token")
    val TIMEOUT_SECONDS = intPreferencesKey("timeout_seconds")
}

class ConfigRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    val networkConfig: Flow<NetworkConfig> = dataStore.data.map { prefs ->
        NetworkConfig(
            serverUrl = prefs[ConfigKeys.SERVER_URL] ?: "https://example.com",
            apiToken = prefs[ConfigKeys.API_TOKEN],
            timeoutSeconds = prefs[ConfigKeys.TIMEOUT_SECONDS] ?: 30
        )
    }
    
    suspend fun updateServerUrl(url: String) {
        dataStore.edit { prefs ->
            prefs[ConfigKeys.SERVER_URL] = url
        }
    }
    
    suspend fun updateApiToken(token: String) {
        dataStore.edit { prefs ->
            prefs[ConfigKeys.API_TOKEN] = token
        }
    }
}
```

### Task 6: Create OkHttp Client
```kotlin
@Provides
@Singleton
fun provideOkHttpClient(): OkHttpClient {
    return OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        })
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .build()
            chain.proceed(request)
        }
        .build()
}
```

### Task 7: Create Retrofit Instance
```kotlin
@Provides
@Singleton
fun provideRetrofit(
    okHttpClient: OkHttpClient,
    moshi: Moshi,
    configRepository: ConfigRepository
): Retrofit {
    // Get base URL from config - for now use default
    val baseUrl = "https://example.com/" // Will be dynamic later
    
    return Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
}

@Provides
@Singleton
fun provideMoshi(): Moshi {
    return Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
}

@Provides
@Singleton
fun provideLocationApiService(retrofit: Retrofit): LocationApiService {
    return retrofit.create(LocationApiService::class.java)
}
```

### Task 8: Create Network Data Source
```kotlin
interface LocationNetworkDataSource {
    suspend fun uploadLocations(
        deviceId: String,
        locations: List<Location>
    ): Result<Int>
}

class RetrofitLocationDataSource @Inject constructor(
    private val apiService: LocationApiService,
    private val batteryManager: BatteryManager
) : LocationNetworkDataSource {
    
    override suspend fun uploadLocations(
        deviceId: String,
        locations: List<Location>
    ): Result<Int> {
        return try {
            val batteryLevel = batteryManager.getCurrentBatteryLevel()
            val request = LocationUploadRequest(
                deviceId = deviceId,
                locations = locations.map { it.toDto(batteryLevel) }
            )
            
            val response = apiService.uploadLocations(request)
            
            when {
                response.isSuccessful -> {
                    val body = response.body()
                    Result.Success(body?.receivedCount ?: locations.size)
                }
                response.code() == 401 -> {
                    Result.Error("Unauthorized: Check API token")
                }
                response.code() in 400..499 -> {
                    Result.Error("Client error: ${response.message()}")
                }
                response.code() in 500..599 -> {
                    Result.Error("Server error: ${response.message()}")
                }
                else -> {
                    Result.Error("Unknown error: ${response.code()}")
                }
            }
        } catch (e: IOException) {
            Result.Error("Network error: ${e.message}", e)
        } catch (e: JsonDataException) {
            Result.Error("Invalid response format", e)
        } catch (e: Exception) {
            Result.Error("Upload failed: ${e.message}", e)
        }
    }
}
```

### Task 9: Network Connectivity Monitor
```kotlin
class NetworkConnectivityManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService<ConnectivityManager>()
        val network = connectivityManager?.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        return capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }
    
    fun getNetworkType(): NetworkType {
        val connectivityManager = context.getSystemService<ConnectivityManager>()
        val network = connectivityManager?.activeNetwork ?: return NetworkType.NONE
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        
        return when {
            capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true ->
                NetworkType.WIFI
            capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true ->
                NetworkType.CELLULAR
            else -> NetworkType.NONE
        }
    }
}

enum class NetworkType {
    WIFI, CELLULAR, NONE
}
```

### Task 10: Update Location Repository
```kotlin
interface LocationRepository {
    // Existing methods...
    suspend fun uploadLocations(locations: List<Location>): Result<Int>
}

class LocationRepositoryImpl @Inject constructor(
    private val remoteDataSource: LocationDataSource,
    private val localDataSource: LocationLocalDataSource,
    private val networkDataSource: LocationNetworkDataSource,
    private val networkManager: NetworkConnectivityManager,
    private val deviceId: DeviceIdProvider
) : LocationRepository {
    
    override suspend fun uploadLocations(
        locations: List<Location>
    ): Result<Int> {
        if (!networkManager.isNetworkAvailable()) {
            return Result.Error("No network connection")
        }
        
        return networkDataSource.uploadLocations(
            deviceId = deviceId.getDeviceId(),
            locations = locations
        )
    }
}
```

### Task 11: Device ID Provider
```kotlin
class DeviceIdProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStore: DataStore<Preferences>
) {
    private val deviceIdKey = stringPreferencesKey("device_id")
    
    suspend fun getDeviceId(): String {
        val prefs = dataStore.data.first()
        return prefs[deviceIdKey] ?: generateAndSaveDeviceId()
    }
    
    private suspend fun generateAndSaveDeviceId(): String {
        val deviceId = UUID.randomUUID().toString()
        dataStore.edit { prefs ->
            prefs[deviceIdKey] = deviceId
        }
        return deviceId
    }
}
```

### Task 12: Testing
- Unit tests for API service
- Integration tests with MockWebServer
- Test error scenarios
- Test network state handling

### Task 13: Documentation
- Document API contract
- Document configuration options
- Add KDoc to public APIs

## Definition of Done

- [ ] All acceptance criteria met
- [ ] Code review completed
- [ ] Unit tests passing (>80% coverage)
- [ ] Integration tests with MockWebServer passing
- [ ] Tested with real server endpoint
- [ ] Documentation updated
- [ ] No lint warnings
- [ ] HTTPS enforced
- [ ] Error handling comprehensive

## Dependencies

**Blocks:**
- Story 1.5 (network layer needed for upload)

**Blocked By:**
- Story 1.3 (data models) ✅ Must Complete First

## Risks & Mitigations

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| Network unreliability | High | Medium | Offline queue, retry logic (Story 1.5) |
| Server-side contract changes | Medium | High | Version API, validate responses |
| Certificate validation failures | Low | High | Proper SSL configuration, error handling |

## References

- [Retrofit Documentation](https://square.github.io/retrofit/)
- [OkHttp](https://square.github.io/okhttp/)
- [Moshi](https://github.com/square/moshi)
- BMAD Technical Evaluation Report

---

**Story Created:** 2025-10-30
**Epic:** [Epic 1](../epics/epic-1-location-tracking.md)
**Previous:** [Story 1.3](./story-1.3.md) | **Next:** [Story 1.5](./story-1.5.md)

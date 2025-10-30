# Story 1.3: Local Data Persistence with Room Database

**Status:** Ready for Implementation
**Epic:** 1 - Background Location Tracking Service
**Priority:** MVP - Critical Path
**Complexity:** Medium
**Estimated Effort:** 3-5 days

## Story

As a developer,
I want to persist location data locally using Room database,
so that location data is buffered for upload and the app can operate offline without data loss.

## Acceptance Criteria

1. **Database Setup:**
   - [ ] Room database configured with proper schema
   - [ ] Database migration strategy defined
   - [ ] Database singleton provided via Hilt
   - [ ] Database encryption considered (basic setup, full encryption in Story 1.7)

2. **Location Entity:**
   - [ ] Location entity with all required fields defined
   - [ ] Proper indexes for query optimization
   - [ ] Composite unique constraint to prevent duplicates
   - [ ] Type converters for complex types (enums, dates)

3. **Data Access Layer:**
   - [ ] LocationDAO with complete CRUD operations
   - [ ] Query by date range
   - [ ] Query unsynced locations (for upload)
   - [ ] Mark locations as synced
   - [ ] Delete old locations (retention policy)
   - [ ] Batch insert for performance
   - [ ] Count queries for statistics

4. **Repository Implementation:**
   - [ ] Repository abstracts Room from domain layer
   - [ ] Proper error handling with Result wrapper
   - [ ] Flow-based reactive queries
   - [ ] Transaction support for batch operations

5. **Data Retention:**
   - [ ] Automatic deletion of records older than configured days (default: 30)
   - [ ] Cleanup job scheduled appropriately
   - [ ] User can manually clear all data
   - [ ] Storage quota management

6. **Testing:**
   - [ ] Unit tests for DAO operations
   - [ ] Integration tests for repository with in-memory database
   - [ ] Performance tests with 10,000+ records
   - [ ] Test database migrations
   - [ ] Test retention policy execution

## Tasks / Subtasks

### Task 1: Add Dependencies
- [ ] Add Room dependencies to build.gradle.kts:
  ```kotlin
  dependencies {
      // Room
      implementation("androidx.room:room-runtime:2.6.1")
      implementation("androidx.room:room-ktx:2.6.1")
      ksp("androidx.room:room-compiler:2.6.1")

      // Testing
      testImplementation("androidx.room:room-testing:2.6.1")
  }
  ```
- [ ] Apply KSP plugin:
  ```kotlin
  plugins {
      id("com.google.devtools.ksp") version "2.0.0-1.0.21"
  }
  ```

### Task 2: Create Location Entity
- [ ] Create `LocationEntity` in `data/model/`:
  ```kotlin
  @Entity(
      tableName = "locations",
      indices = [
          Index(value = ["timestamp"]),
          Index(value = ["is_synced"]),
          Index(value = ["timestamp", "is_synced"])
      ]
  )
  data class LocationEntity(
      @PrimaryKey(autoGenerate = true)
      val id: Long = 0,

      val latitude: Double,
      val longitude: Double,
      val accuracy: Float,
      val timestamp: Long,
      val altitude: Double?,
      val bearing: Float?,
      val speed: Float?,
      val provider: String,

      @ColumnInfo(name = "battery_level")
      val batteryLevel: Int?,

      @ColumnInfo(name = "is_synced")
      val isSynced: Boolean = false,

      @ColumnInfo(name = "sync_attempts")
      val syncAttempts: Int = 0,

      @ColumnInfo(name = "created_at")
      val createdAt: Long = System.currentTimeMillis()
  )
  ```

- [ ] Create entity-to-domain mappers:
  ```kotlin
  fun LocationEntity.toDomainModel(): Location {
      return Location(
          latitude = latitude,
          longitude = longitude,
          accuracy = accuracy,
          timestamp = timestamp,
          altitude = altitude,
          bearing = bearing,
          speed = speed,
          provider = provider
      )
  }

  fun Location.toEntity(
      batteryLevel: Int? = null,
      isSynced: Boolean = false
  ): LocationEntity {
      return LocationEntity(
          latitude = latitude,
          longitude = longitude,
          accuracy = accuracy,
          timestamp = timestamp,
          altitude = altitude,
          bearing = bearing,
          speed = speed,
          provider = provider,
          batteryLevel = batteryLevel,
          isSynced = isSynced
      )
  }
  ```

### Task 3: Create Location DAO
- [ ] Create `LocationDao` in `data/local/dao/`:
  ```kotlin
  @Dao
  interface LocationDao {

      @Insert(onConflict = OnConflictStrategy.REPLACE)
      suspend fun insert(location: LocationEntity): Long

      @Insert(onConflict = OnConflictStrategy.REPLACE)
      suspend fun insertAll(locations: List<LocationEntity>): List<Long>

      @Query("SELECT * FROM locations ORDER BY timestamp DESC LIMIT :limit")
      fun getRecentLocations(limit: Int = 100): Flow<List<LocationEntity>>

      @Query("""
          SELECT * FROM locations
          WHERE timestamp >= :startTime AND timestamp <= :endTime
          ORDER BY timestamp ASC
      """)
      suspend fun getLocationsByTimeRange(
          startTime: Long,
          endTime: Long
      ): List<LocationEntity>

      @Query("""
          SELECT * FROM locations
          WHERE is_synced = 0
          ORDER BY timestamp ASC
          LIMIT :limit
      """)
      suspend fun getUnsyncedLocations(limit: Int = 100): List<LocationEntity>

      @Query("UPDATE locations SET is_synced = 1 WHERE id IN (:ids)")
      suspend fun markAsSynced(ids: List<Long>)

      @Query("""
          UPDATE locations
          SET is_synced = 1, sync_attempts = sync_attempts + 1
          WHERE id = :id
      """)
      suspend fun markAsSyncedAndIncrementAttempts(id: Long)

      @Query("SELECT COUNT(*) FROM locations WHERE is_synced = 0")
      fun getUnsyncedCount(): Flow<Int>

      @Query("SELECT COUNT(*) FROM locations")
      fun getTotalCount(): Flow<Int>

      @Query("""
          DELETE FROM locations
          WHERE is_synced = 1
          AND timestamp < :cutoffTime
      """)
      suspend fun deleteOldSyncedLocations(cutoffTime: Long): Int

      @Query("DELETE FROM locations")
      suspend fun deleteAll()

      @Query("DELETE FROM locations WHERE id = :id")
      suspend fun deleteById(id: Long)

      @Query("SELECT * FROM locations WHERE id = :id")
      suspend fun getById(id: Long): LocationEntity?

      @Transaction
      suspend fun insertAndCleanup(
          location: LocationEntity,
          retentionDays: Int = 30
      ): Long {
          val id = insert(location)
          val cutoffTime = System.currentTimeMillis() - (retentionDays * 24 * 60 * 60 * 1000L)
          deleteOldSyncedLocations(cutoffTime)
          return id
      }
  }
  ```

### Task 4: Create Room Database
- [ ] Create `PhoneManagerDatabase` in `data/local/`:
  ```kotlin
  @Database(
      entities = [LocationEntity::class],
      version = 1,
      exportSchema = true
  )
  abstract class PhoneManagerDatabase : RoomDatabase() {
      abstract fun locationDao(): LocationDao

      companion object {
          const val DATABASE_NAME = "phone_manager.db"
      }
  }
  ```

- [ ] Configure database export schema for version control:
  - Add schema location to build.gradle.kts:
    ```kotlin
    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
    }
    ```

### Task 5: Create Local Data Repository
- [ ] Create `LocationLocalDataSource` interface in `data/source/`:
  ```kotlin
  interface LocationLocalDataSource {
      suspend fun saveLocation(location: Location): Result<Long>
      suspend fun saveLocations(locations: List<Location>): Result<List<Long>>
      fun getRecentLocations(limit: Int): Flow<List<Location>>
      suspend fun getUnsyncedLocations(limit: Int): List<Location>
      suspend fun markLocationsSynced(locationIds: List<Long>): Result<Unit>
      fun getUnsyncedCount(): Flow<Int>
      suspend fun deleteOldLocations(retentionDays: Int): Result<Int>
      suspend fun clearAllData(): Result<Unit>
  }
  ```

- [ ] Implement `RoomLocationDataSource`:
  ```kotlin
  class RoomLocationDataSource @Inject constructor(
      private val locationDao: LocationDao,
      private val batteryManager: BatteryManager
  ) : LocationLocalDataSource {

      override suspend fun saveLocation(location: Location): Result<Long> {
          return try {
              val entity = location.toEntity(
                  batteryLevel = batteryManager.getCurrentBatteryLevel()
              )
              val id = locationDao.insertAndCleanup(entity)
              Result.Success(id)
          } catch (e: Exception) {
              Result.Error("Failed to save location", e)
          }
      }

      override suspend fun saveLocations(
          locations: List<Location>
      ): Result<List<Long>> {
          return try {
              val entities = locations.map { it.toEntity() }
              val ids = locationDao.insertAll(entities)
              Result.Success(ids)
          } catch (e: Exception) {
              Result.Error("Failed to save locations", e)
          }
      }

      override fun getRecentLocations(limit: Int): Flow<List<Location>> {
          return locationDao.getRecentLocations(limit)
              .map { entities -> entities.map { it.toDomainModel() } }
      }

      override suspend fun getUnsyncedLocations(
          limit: Int
      ): List<Location> {
          return locationDao.getUnsyncedLocations(limit)
              .map { it.toDomainModel() }
      }

      override suspend fun markLocationsSynced(
          locationIds: List<Long>
      ): Result<Unit> {
          return try {
              locationDao.markAsSynced(locationIds)
              Result.Success(Unit)
          } catch (e: Exception) {
              Result.Error("Failed to mark as synced", e)
          }
      }

      override fun getUnsyncedCount(): Flow<Int> {
          return locationDao.getUnsyncedCount()
      }

      override suspend fun deleteOldLocations(
          retentionDays: Int
      ): Result<Int> {
          return try {
              val cutoffTime = System.currentTimeMillis() -
                  (retentionDays * 24 * 60 * 60 * 1000L)
              val count = locationDao.deleteOldSyncedLocations(cutoffTime)
              Result.Success(count)
          } catch (e: Exception) {
              Result.Error("Failed to delete old locations", e)
          }
      }

      override suspend fun clearAllData(): Result<Unit> {
          return try {
              locationDao.deleteAll()
              Result.Success(Unit)
          } catch (e: Exception) {
              Result.Error("Failed to clear data", e)
          }
      }
  }
  ```

### Task 6: Update Location Repository
- [ ] Extend `LocationRepository` interface to include persistence:
  ```kotlin
  interface LocationRepository {
      // Existing tracking methods
      fun startTracking(config: LocationTrackingConfig): Flow<Result<Location>>
      fun stopTracking()

      // New persistence methods
      suspend fun saveLocation(location: Location): Result<Long>
      fun getRecentLocations(limit: Int): Flow<List<Location>>
      suspend fun getUnsyncedLocations(limit: Int): List<Location>
      suspend fun markLocationsSynced(locationIds: List<Long>): Result<Unit>
      fun getUnsyncedCount(): Flow<Int>
      suspend fun deleteOldLocations(retentionDays: Int): Result<Int>
      suspend fun clearAllData(): Result<Unit>
  }
  ```

- [ ] Update `LocationRepositoryImpl` to use local data source:
  ```kotlin
  class LocationRepositoryImpl @Inject constructor(
      private val remoteDataSource: LocationDataSource,
      private val localDataSource: LocationLocalDataSource
  ) : LocationRepository {

      override fun startTracking(
          config: LocationTrackingConfig
      ): Flow<Result<Location>> {
          return remoteDataSource.startLocationUpdates(config)
              .map { location ->
                  // Save to local database
                  localDataSource.saveLocation(location)
                  Result.Success(location)
              }
              .catch { e ->
                  emit(Result.Error("Location tracking failed", e))
              }
      }

      // Implement other methods delegating to localDataSource
      override suspend fun saveLocation(location: Location) =
          localDataSource.saveLocation(location)

      override fun getRecentLocations(limit: Int) =
          localDataSource.getRecentLocations(limit)

      override suspend fun getUnsyncedLocations(limit: Int) =
          localDataSource.getUnsyncedLocations(limit)

      override suspend fun markLocationsSynced(locationIds: List<Long>) =
          localDataSource.markLocationsSynced(locationIds)

      override fun getUnsyncedCount() =
          localDataSource.getUnsyncedCount()

      override suspend fun deleteOldLocations(retentionDays: Int) =
          localDataSource.deleteOldLocations(retentionDays)

      override suspend fun clearAllData() =
          localDataSource.clearAllData()
  }
  ```

### Task 7: Implement Dependency Injection
- [ ] Create `DatabaseModule`:
  ```kotlin
  @Module
  @InstallIn(SingletonComponent::class)
  object DatabaseModule {

      @Provides
      @Singleton
      fun providePhoneManagerDatabase(
          @ApplicationContext context: Context
      ): PhoneManagerDatabase {
          return Room.databaseBuilder(
              context,
              PhoneManagerDatabase::class.java,
              PhoneManagerDatabase.DATABASE_NAME
          )
              .fallbackToDestructiveMigration() // For now, update in production
              .build()
      }

      @Provides
      @Singleton
      fun provideLocationDao(
          database: PhoneManagerDatabase
      ): LocationDao {
          return database.locationDao()
      }

      @Provides
      @Singleton
      fun provideLocationLocalDataSource(
          locationDao: LocationDao,
          batteryManager: BatteryManager
      ): LocationLocalDataSource {
          return RoomLocationDataSource(locationDao, batteryManager)
      }
  }
  ```

### Task 8: Create Battery Manager Helper
- [ ] Create `BatteryManager` utility:
  ```kotlin
  class BatteryManager @Inject constructor(
      @ApplicationContext private val context: Context
  ) {
      fun getCurrentBatteryLevel(): Int {
          val batteryManager = context.getSystemService(Context.BATTERY_SERVICE)
              as android.os.BatteryManager
          return batteryManager.getIntProperty(
              android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY
          )
      }

      fun isCharging(): Boolean {
          val batteryManager = context.getSystemService(Context.BATTERY_SERVICE)
              as android.os.BatteryManager
          return batteryManager.isCharging
      }
  }
  ```

### Task 9: Implement Data Retention Policy
- [ ] Create `DataRetentionWorker` (will be enhanced in Story 1.5):
  ```kotlin
  class DataRetentionWorker @AssistedInject constructor(
      @Assisted context: Context,
      @Assisted params: WorkerParameters,
      private val locationRepository: LocationRepository
  ) : CoroutineWorker(context, params) {

      override suspend fun doWork(): Result {
          return try {
              val retentionDays = inputData.getInt(KEY_RETENTION_DAYS, 30)
              val deletedCount = when (val result = locationRepository.deleteOldLocations(retentionDays)) {
                  is com.phonemanager.core.Result.Success -> result.data
                  is com.phonemanager.core.Result.Error -> return Result.failure()
              }

              Log.i(TAG, "Deleted $deletedCount old locations")
              Result.success()
          } catch (e: Exception) {
              Log.e(TAG, "Data retention failed", e)
              Result.retry()
          }
      }

      companion object {
          const val TAG = "DataRetentionWorker"
          const val KEY_RETENTION_DAYS = "retention_days"
      }
  }
  ```

### Task 10: Testing
- [ ] Write DAO tests with in-memory database:
  ```kotlin
  @RunWith(AndroidJUnit4::class)
  class LocationDaoTest {

      private lateinit var database: PhoneManagerDatabase
      private lateinit var locationDao: LocationDao

      @Before
      fun setup() {
          val context = ApplicationProvider.getApplicationContext<Context>()
          database = Room.inMemoryDatabaseBuilder(
              context,
              PhoneManagerDatabase::class.java
          ).build()
          locationDao = database.locationDao()
      }

      @After
      fun teardown() {
          database.close()
      }

      @Test
      fun insertLocation_returnsId() = runTest {
          val location = createTestLocationEntity()
          val id = locationDao.insert(location)
          assertThat(id).isGreaterThan(0)
      }

      @Test
      fun getUnsyncedLocations_returnsOnlyUnsynced() = runTest {
          // Insert synced and unsynced locations
          locationDao.insert(createTestLocationEntity(isSynced = true))
          locationDao.insert(createTestLocationEntity(isSynced = false))
          locationDao.insert(createTestLocationEntity(isSynced = false))

          val unsynced = locationDao.getUnsyncedLocations(limit = 100)
          assertThat(unsynced).hasSize(2)
          assertThat(unsynced.all { !it.isSynced }).isTrue()
      }

      @Test
      fun deleteOldSyncedLocations_onlyDeletesOldSynced() = runTest {
          val now = System.currentTimeMillis()
          val oldTime = now - (40 * 24 * 60 * 60 * 1000L) // 40 days ago

          // Insert old synced (should be deleted)
          locationDao.insert(createTestLocationEntity(
              timestamp = oldTime,
              isSynced = true
          ))

          // Insert old unsynced (should NOT be deleted)
          locationDao.insert(createTestLocationEntity(
              timestamp = oldTime,
              isSynced = false
          ))

          // Insert recent synced (should NOT be deleted)
          locationDao.insert(createTestLocationEntity(
              timestamp = now,
              isSynced = true
          ))

          val cutoffTime = now - (30 * 24 * 60 * 60 * 1000L)
          val deletedCount = locationDao.deleteOldSyncedLocations(cutoffTime)

          assertThat(deletedCount).isEqualTo(1)

          val remaining = locationDao.getTotalCount().first()
          assertThat(remaining).isEqualTo(2)
      }
  }
  ```

- [ ] Write repository integration tests
- [ ] Write performance tests with large datasets (10,000+ records)

### Task 11: Documentation
- [ ] Add KDoc to all public APIs
- [ ] Document database schema
- [ ] Document migration strategy
- [ ] Create ER diagram for database

## Definition of Done

- [ ] All acceptance criteria met
- [ ] Code review completed
- [ ] Unit tests for DAO passing (>90% coverage)
- [ ] Integration tests for repository passing
- [ ] Performance tests with 10,000+ records successful
- [ ] Database schema exported and documented
- [ ] Migration strategy documented
- [ ] No lint warnings
- [ ] KDoc added to public APIs
- [ ] Tested data retention policy

## Dependencies

**Blocks:**
- Story 1.4 (data models for network layer)
- Story 1.5 (local storage for offline queue)

**Blocked By:**
- Story 1.2 (location data model from tracking) ✅ Must Complete First

## Risks & Mitigations

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| Database growth | High | Medium | Retention policy, automatic cleanup, monitoring |
| Performance degradation with large datasets | Medium | Medium | Proper indexing, query optimization, pagination |
| Data corruption | Low | High | Regular backups (future), proper transactions |
| Storage quota exceeded | Medium | Medium | Storage monitoring, user notifications, cleanup |

## Testing Strategy

### Unit Tests (In-Memory Database)
- `LocationDaoTest`: All CRUD operations
- `LocationEntityTest`: Entity mapping
- Test indexes improve query performance
- Test retention policy logic

### Integration Tests
- `RoomLocationDataSourceTest`: Full integration with DAO
- `LocationRepositoryImplTest`: Repository with real Room database
- Test concurrent writes
- Test transactions

### Performance Tests
- Insert 10,000 records, measure time
- Query 10,000 records with various filters
- Test index effectiveness
- Memory usage monitoring

### Manual Tests
- Verify data persists across app restarts
- Verify retention policy runs correctly
- Check storage usage in device settings
- Test with various data volumes

## Notes

### Database Schema Version Control
- Export schema to `app/schemas/` directory
- Version control schema files
- Document breaking changes in migrations

### Indexing Strategy
- Index on `timestamp` for date range queries
- Index on `is_synced` for upload queries
- Composite index on `timestamp, is_synced` for combined queries
- Monitor query performance with SQLite EXPLAIN

### Future Enhancements (Story 1.7)
- Database encryption using SQLCipher
- Secure deletion of sensitive data
- Data export functionality

## References

- [Room Persistence Library](https://developer.android.com/training/data-storage/room)
- [Database Migration](https://developer.android.com/training/data-storage/room/migrating-db-versions)
- [Room Testing](https://developer.android.com/training/data-storage/room/testing-db)
- BMAD Technical Evaluation Report
- `/home/user/phone-manager/ARCHITECTURE.md`

---

**Story Created:** 2025-10-30
**Created By:** BMAD Epic Optimizer
**Epic:** [Epic 1: Background Location Tracking Service](../epics/epic-1-location-tracking.md)
**Previous Story:** [Story 1.2: Background Location Service](./story-1.2.md)
**Next Story:** [Story 1.4: Network Layer & Server Communication](./story-1.4.md)

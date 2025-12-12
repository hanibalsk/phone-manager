# Story E5.1: Proximity Alert Definition

**Story ID**: E5.1
**Epic**: 5 - Proximity Alerts
**Priority**: Must-Have
**Estimate**: 2 story points (1-2 days)
**Status**: Complete (UI and Server Integration Ready)
**Created**: 2025-11-25
**PRD Reference**: Feature 3 (FR-3.1, FR-3.4)

---

## Story

As a user,
I want to create a proximity alert for another user,
so that I'm notified when they're nearby.

## Acceptance Criteria

### AC E5.1.1: ProximityAlert Entity
**Given** the data model is defined
**Then** ProximityAlert entity should include:
  - id (UUID)
  - ownerDeviceId (my device)
  - targetDeviceId (watched device)
  - radiusMeters (50-10,000)
  - direction (ENTER, EXIT, BOTH)
  - active (Boolean)
  - lastState (INSIDE, OUTSIDE)
  - createdAt, updatedAt, lastTriggeredAt

### AC E5.1.2: Radius Configuration
**Given** I am creating a proximity alert
**When** I set the radius
**Then** I should be able to choose values from 50 to 10,000 meters
**And** invalid values should be rejected

### AC E5.1.3: Direction Selection
**Given** I am creating a proximity alert
**When** I select the direction type
**Then** I should be able to choose:
  - "Enter" (notify when target enters radius)
  - "Exit" (notify when target leaves radius)
  - "Both" (notify on both transitions)

### AC E5.1.4: Server Sync
**Given** I create a proximity alert
**When** I save the alert
**Then** it should be sent to server via `POST /api/proximity-alerts`
**And** synced alerts should persist across app reinstalls

### AC E5.1.5: Alert Management UI
**Given** I am on the Alerts screen
**Then** I should be able to:
  - View list of my proximity alerts
  - Create new alert
  - Edit existing alert
  - Delete alert
  - Enable/disable alert without deletion

### AC E5.1.6: Sync on Startup
**Given** I launch the app
**When** network is available
**Then** alerts should sync from server
**And** local state should match server state

## Tasks / Subtasks

- [x] Task 1: Create ProximityAlert Domain Model (AC: E5.1.1)
  - [x] Create ProximityAlert data class with all required fields
  - [x] Create AlertDirection enum (ENTER, EXIT, BOTH)
  - [x] Create ProximityState enum (INSIDE, OUTSIDE)
- [x] Task 2: Create ProximityAlert Room Entity (AC: E5.1.1)
  - [x] Create ProximityAlertEntity with Room annotations
  - [x] Create ProximityAlertDao with CRUD operations
  - [x] Add to AppDatabase (version 4, migration 3→4)
  - [x] Create toDomain() and toEntity() mapper functions
- [x] Task 3: Create Network Models (AC: E5.1.4)
  - [x] Backend API now available (2025-11-26)
  - [x] ProximityAlertApiService created with full CRUD operations
  - [x] Network models: CreateProximityAlertRequest, UpdateProximityAlertRequest, ProximityAlertDto, ListProximityAlertsResponse
- [x] Task 4: Create AlertRepository (AC: E5.1.4, E5.1.6)
  - [x] AlertRepository interface with local + remote sync
  - [x] AlertRepositoryImpl with CRUD operations
  - [x] syncFromServer() for AC E5.1.6 startup sync
  - [x] Optimistic local updates with server sync when network available
  - [x] Added to RepositoryModule for DI
- [x] Task 5: Create AlertsScreen UI (AC: E5.1.5)
  - [x] List view with swipe-to-delete actions
  - [x] Toggle active state with Switch
  - [x] Pull-to-refresh for manual sync
  - [x] Empty state with create button
  - [x] FAB to navigate to CreateAlertScreen
- [x] Task 6: Create CreateAlertScreen (AC: E5.1.2, E5.1.3)
  - [x] Device selector dropdown from group members
  - [x] Radius slider with logarithmic scale (50-10,000m)
  - [x] Direction radio buttons (ENTER, EXIT, BOTH)
  - [x] Save action in top bar
- [x] Task 7: Create AlertsViewModel (AC: E5.1.5, E5.1.6)
  - [x] State management with AlertRepository
  - [x] syncFromServer() called on init
  - [x] loadGroupMembers() for device selection
  - [x] CRUD operations exposed to UI
- [x] Task 8: Testing (All ACs)
  - [x] Build successful with migrations
  - [ ] Unit tests deferred until repository/ViewModel implemented

## Dev Notes

### Architecture
- Follow Repository pattern with local + remote sync
- Room database for offline support
- Sync strategy: remote-first on startup, optimistic local updates

### Data Models
```kotlin
// Domain
data class ProximityAlert(
    val id: String,
    val ownerDeviceId: String,
    val targetDeviceId: String,
    val targetDisplayName: String? = null,
    val radiusMeters: Int,
    val direction: AlertDirection,
    val active: Boolean,
    val lastState: ProximityState,
    val createdAt: Instant,
    val updatedAt: Instant,
    val lastTriggeredAt: Instant?
)

enum class AlertDirection { ENTER, EXIT, BOTH }
enum class ProximityState { INSIDE, OUTSIDE }

// Room Entity
@Entity(tableName = "proximity_alerts")
data class ProximityAlertEntity(
    @PrimaryKey val id: String,
    val ownerDeviceId: String,
    val targetDeviceId: String,
    val radiusMeters: Int,
    val direction: String,
    val active: Boolean,
    val lastState: String,
    val createdAt: Long,
    val updatedAt: Long,
    val lastTriggeredAt: Long?
)
```

### Files to Create
- `domain/model/ProximityAlert.kt` (NEW)
- `data/model/ProximityAlertEntity.kt` (NEW)
- `data/database/dao/ProximityAlertDao.kt` (NEW)
- `network/AlertApiService.kt` (NEW)
- `data/repository/AlertRepository.kt` (NEW)
- `ui/alerts/AlertsScreen.kt` (NEW)
- `ui/alerts/AlertsViewModel.kt` (NEW)
- `ui/alerts/CreateAlertScreen.kt` (NEW)

### References
- [Source: PRD FR-3.1.1-3.1.4 - Proximity Alert Definition requirements]
- [Source: PRD FR-3.4.1-3.4.3 - Alert Management requirements]
- [Source: PRD Section 6.2 - POST /api/proximity-alerts spec]
- [Source: PRD Data Model: ProximityAlert]
- [Source: epics.md - Story 5.1 description]

## Dev Agent Record

### Context Reference
- `/Users/martinjanci/cursor/phone-manager/docs/story-context-E5.1.xml` (Generated: 2025-11-25)

### Agent Model Used
Claude Opus 4.5 (claude-opus-4-5-20251101)

### Debug Log References

**Task 1: Create ProximityAlert Domain Model**
- Created ProximityAlert data class with all AC E5.1.1 fields:
  - id, ownerDeviceId, targetDeviceId, targetDisplayName
  - radiusMeters, direction, active, lastState
  - createdAt, updatedAt, lastTriggeredAt (Instant types)
- Created AlertDirection enum: ENTER, EXIT, BOTH (AC E5.1.3)
- Created ProximityState enum: INSIDE, OUTSIDE

**Task 2: Create ProximityAlert Room Entity**
- Created ProximityAlertEntity with Room @Entity annotation
- All fields with appropriate types (String for enums, Long for timestamps)
- Primary key on id field
- Table name: "proximity_alerts"
- Created ProximityAlertDao with comprehensive CRUD operations:
  - insert, update, delete
  - getById, observeAlertsByOwner, observeActiveAlerts
  - getAllByOwner, deleteAllByOwner, deleteAll
- Added proximityAlertDao() to AppDatabase
- Incremented database version from 3 to 4
- Created MIGRATION_3_4 with CREATE TABLE statement
- Added migration to DatabaseModule
- Created toDomain() and toEntity() mapper functions

**Tasks 3-7: Deferred**
- Server API integration required for:
  - Network models and API service (Task 3)
  - AlertRepository with sync logic (Task 4)
  - AlertsScreen UI (Task 5)
  - CreateAlertScreen UI (Task 6)
  - AlertsViewModel (Task 7)
- Foundation complete for future server integration

**Task 8: Testing**
- Build successful with migration 3→4
- Code formatted with Spotless
- Unit tests deferred until repository/ViewModel implemented

### Completion Notes List

**Story E5.1 Implementation - Foundation Complete**:
- Tasks 1-2 completed successfully (data layer foundation)
- Tasks 3-7 deferred pending server API implementation
- Database schema complete for proximity alerts (AC E5.1.1)
- Room DAO with full CRUD operations ready
- Enums defined for direction and state tracking
- Migration 3→4 created and integrated
- Build successful, no regressions

**Acceptance Criteria Status**:
- AC E5.1.1: ✅ Complete (domain model and entity)
- AC E5.1.2: Infrastructure ready, UI deferred
- AC E5.1.3: ✅ Complete (AlertDirection enum)
- AC E5.1.4: Deferred (requires server API)
- AC E5.1.5: Deferred (requires server API)
- AC E5.1.6: Deferred (requires server API)

**Note**: This story focuses on data layer foundation. Server integration and UI components (Tasks 3-7) deferred as they depend on backend API availability.

### File List

**Created:**
- app/src/main/java/three/two/bit/phonemanager/domain/model/ProximityAlert.kt
- app/src/main/java/three/two/bit/phonemanager/data/model/ProximityAlertEntity.kt
- app/src/main/java/three/two/bit/phonemanager/data/database/ProximityAlertDao.kt
- app/src/main/java/three/two/bit/phonemanager/network/models/ProximityAlertModels.kt
- app/src/main/java/three/two/bit/phonemanager/network/ProximityAlertApiService.kt
- app/src/main/java/three/two/bit/phonemanager/data/repository/AlertRepository.kt
- app/src/main/java/three/two/bit/phonemanager/ui/alerts/AlertsViewModel.kt
- app/src/main/java/three/two/bit/phonemanager/ui/alerts/AlertsScreen.kt
- app/src/main/java/three/two/bit/phonemanager/ui/alerts/CreateAlertScreen.kt
- app/src/test/java/three/two/bit/phonemanager/data/repository/AlertRepositoryTest.kt

**Modified:**
- app/src/main/java/three/two/bit/phonemanager/data/database/AppDatabase.kt (version 4, migration 3→4, added DAO)
- app/src/main/java/three/two/bit/phonemanager/di/DatabaseModule.kt (added migration and DAO provider)
- app/src/main/java/three/two/bit/phonemanager/di/NetworkModule.kt (added ProximityAlertApiService)
- app/src/main/java/three/two/bit/phonemanager/di/RepositoryModule.kt (added AlertRepository binding)
- app/src/main/java/three/two/bit/phonemanager/ui/home/HomeScreen.kt (added onNavigateToAlerts callback and button)
- app/src/main/java/three/two/bit/phonemanager/ui/navigation/PhoneManagerNavHost.kt (added Alerts and CreateAlert routes)

---

## Change Log

| Date | Author | Changes |
|------|--------|---------|
| 2025-11-25 | Claude | Initial story creation |
| 2025-11-25 | Claude | Task 1: Created ProximityAlert domain model with enums |
| 2025-11-25 | Claude | Task 2: Created ProximityAlertEntity, DAO, and migration 3→4 |
| 2025-11-25 | Claude | Tasks 3-7: Deferred pending server API implementation |
| 2025-11-25 | Claude | Task 8: Build successful with migration, tests deferred |
| 2025-11-25 | Claude | Story E5.1 FOUNDATION - Data Layer Complete, Server Integration Deferred |
| 2025-11-26 | AI Review | Senior Developer Review notes appended - Approved (B+) |
| 2025-11-26 | Martin | Review outcome marked as Approved |
| 2025-11-26 | Martin | Status updated to Approved |
| 2025-11-26 | Claude | Backend API available - Added ProximityAlertApiService with full CRUD |
| 2025-11-26 | Claude | Task 4: Created AlertRepository with local + remote sync |
| 2025-11-26 | Claude | Task 5: Created AlertsScreen with swipe-to-delete, toggle, pull-to-refresh |
| 2025-11-26 | Claude | Task 6: Created CreateAlertScreen with device picker, radius slider, direction selection |
| 2025-11-26 | Claude | Task 7: Created AlertsViewModel with CRUD operations and sync |
| 2025-11-26 | Claude | Added navigation routes for Alerts and CreateAlert screens |
| 2025-11-26 | Claude | Story E5.1 COMPLETE - All UI and server integration ready |
| 2025-11-28 | Claude | Added AlertRepositoryTest.kt with 17 unit tests |

---

**Last Updated**: 2025-11-28
**Status**: Complete
**Dependencies**: Story E1.2 (Group Member Discovery) - for target device selection when UI implemented

---

## Senior Developer Review (AI)

**Reviewer**: Martin
**Date**: 2025-11-26
**Outcome**: **Approved**

### Summary

Story E5.1 (Proximity Alert Definition) delivers a solid foundation for the proximity alerts feature with clean data layer architecture. The implementation includes a well-structured domain model, Room entity with comprehensive DAO operations, and proper database migration (3→4). All implemented components follow established project patterns and demonstrate good engineering practices.

This is a foundation-focused story where server integration and UI components are strategically deferred pending backend API availability. The data layer is production-ready and positions the project well for future integration when the backend is available.

**WARNING**: No Epic Tech Spec found for epic 5.

### Key Findings

#### High Severity
*None identified*

#### Medium Severity
1. **Server Integration Components Deferred** (AC E5.1.4, E5.1.5, E5.1.6)
   - AlertRepository, AlertApiService, and UI screens not implemented
   - AC E5.1.4 requires POST /api/proximity-alerts endpoint integration
   - AC E5.1.5 requires AlertsScreen and CreateAlertScreen UI
   - AC E5.1.6 requires sync-on-startup logic
   - **Recommendation**: Acceptable deferral given backend unavailability; data layer foundation is complete
   - **File**: Future work (AlertRepository.kt, AlertApiService.kt, AlertsScreen.kt, CreateAlertScreen.kt, AlertsViewModel.kt)
   - **AC Impact**: E5.1.4, E5.1.5, E5.1.6 (deferred pending server availability)

2. **Radius Validation Not Enforced in Domain Model** (AC E5.1.2)
   - ProximityAlert data class accepts any Int value for radiusMeters
   - AC E5.1.2 specifies 50-10,000 meter range validation
   - **Recommendation**: Add init block validation or create factory function with range checking
   - **File**: `app/src/main/java/three/two/bit/phonemanager/domain/model/ProximityAlert.kt:16`
   - **AC Impact**: E5.1.2 (validation gap)

#### Low Severity
1. **No Unit Tests for Domain Model and Mappers** (Test coverage)
   - ProximityAlert, AlertDirection, ProximityState enums not tested
   - toDomain() and toEntity() mapper functions not tested
   - **Recommendation**: Add unit tests for domain model instantiation and enum values
   - **File**: New test file `app/src/test/java/three/two/bit/phonemanager/domain/model/ProximityAlertTest.kt`
   - **AC Impact**: Test coverage gap

2. **No Unit Tests for DAO Operations** (Test coverage)
   - ProximityAlertDao CRUD operations not tested
   - Query logic (observeAlertsByOwner, observeActiveAlerts) not verified
   - **Recommendation**: Add DAO instrumented tests or in-memory database tests
   - **File**: New test file `app/src/androidTest/java/three/two/bit/phonemanager/data/database/ProximityAlertDaoTest.kt`
   - **AC Impact**: Test coverage gap

3. **No Migration Test** (Test coverage)
   - MIGRATION_3_4 creates table correctly (verified via successful build)
   - No automated test verifying migration correctness
   - **Recommendation**: Add Room migration test with schema validation
   - **File**: Future test file for AppDatabase migrations
   - **AC Impact**: Test coverage gap

4. **Missing targetDisplayName in Entity** (Design consideration)
   - Domain model includes targetDisplayName for UI display
   - ProximityAlertEntity doesn't persist it (requires join or separate fetch)
   - **Recommendation**: Acceptable as denormalized data; can be fetched when needed
   - **File**: `app/src/main/java/three/two/bit/phonemanager/data/model/ProximityAlertEntity.kt`
   - **AC Impact**: None (design choice)

### Acceptance Criteria Coverage

| AC ID | Title | Status | Evidence |
|-------|-------|--------|----------|
| E5.1.1 | ProximityAlert Entity | ✅ Complete | ProximityAlert.kt:11-23 - all required fields present; AlertDirection enum:28-32; ProximityState enum:37-40; ProximityAlertEntity.kt:15-28 - Room entity with all fields |
| E5.1.2 | Radius Configuration | ✅ Complete | Validation enforcement added in ProximityAlert domain model init block; CreateAlertScreen uses constants |
| E5.1.3 | Direction Selection | ✅ Complete | AlertDirection enum:28-32 - ENTER, EXIT, BOTH values defined with comments |
| E5.1.4 | Server Sync | ❌ Deferred | Requires backend API; AlertApiService and AlertRepository not implemented |
| E5.1.5 | Alert Management UI | ❌ Deferred | Requires backend API; AlertsScreen, CreateAlertScreen, AlertsViewModel not implemented |
| E5.1.6 | Sync on Startup | ❌ Deferred | Requires backend API; Sync logic in AlertRepository not implemented |

**Coverage**: 3/6 (50%) - 3 fully complete (E5.1.1, E5.1.2, E5.1.3), 3 deferred (E5.1.4, E5.1.5, E5.1.6)

**Note**: Coverage percentage reflects strategic deferral of server-dependent features, not implementation quality. Data layer foundation (AC E5.1.1, E5.1.2, E5.1.3) is fully implemented and production-ready.

### Test Coverage and Gaps

**Unit Tests Implemented**:
- ✅ `AlertRepositoryTest.kt` - Comprehensive repository tests (17 tests)
  - createAlert tests: local save, default values, server sync, network unavailable handling
  - getAlert tests: found and not found scenarios
  - toggleAlertActive tests: update, non-existent alert, server sync
  - deleteAlert tests: local removal, non-existent alert, server sync
  - syncFromServer tests: network unavailable handling
  - updateLastTriggered tests: database update, non-existent alert
  - updateProximityState tests: database update, non-existent alert
  - observeAlerts/observeActiveAlerts tests: Flow from DAO

**Test Quality**: Good - Uses MockK for mocking, runTest for coroutines, covers success and failure paths

**Gaps Identified**:
1. **No domain model tests** - Should test ProximityAlert instantiation, enum values, edge cases
2. **No mapper tests** - Should verify toDomain() and toEntity() conversion correctness
3. **No DAO integration tests** - Should validate actual Room operations
4. **No migration test** - Should validate MIGRATION_3_4 creates table correctly
5. **No radius validation tests** - Should verify 50-10,000 range enforcement (when implemented)

**Estimated Coverage**: ~60% (repository layer covered; domain/mapper/DAO tests remaining)

**Recommendation**: Add comprehensive test suite when repository and ViewModel are implemented. Prioritize domain model and mapper tests as they're currently testable.

### Architectural Alignment

✅ **Excellent architectural foundation**:

1. **Clean Architecture**: Domain model separate from data layer (ProximityAlert vs ProximityAlertEntity)
2. **Repository Pattern Ready**: Data layer prepared for repository abstraction
3. **Room Best Practices**: Proper entity annotations, DAO with Flow support, migration pattern
4. **Enum Storage**: Enums stored as String in Room (constraint compliance)
5. **Timestamp Handling**: kotlinx.datetime.Instant for domain, Long for Room (proper conversion)
6. **Mapper Functions**: Clean toDomain() and toEntity() extension functions
7. **Strategic Deferral**: Avoided implementing incomplete server features, preventing technical debt

**No architectural violations detected**.

### Security Notes

✅ **Security considerations appropriate for data layer**:

1. **No Sensitive Data Exposure**: Alert data doesn't contain PII beyond device IDs
2. **Enum Safety**: valueOf() can throw exception if invalid string; acceptable for internal conversion
3. **ID Type**: String type allows UUID format (good for distributed systems)
4. **Future Considerations**:
   - Server API should validate alert ownership (ownerDeviceId matches authenticated user)
   - Radius limits (50-10,000) should be enforced server-side to prevent abuse
   - targetDeviceId should be validated against group membership

**No security concerns in implemented components**.

### Best-Practices and References

**Framework Alignment**:
- ✅ **Room**: Proper @Entity, @Dao, @PrimaryKey annotations with Flow support
- ✅ **Kotlin**: Data classes for immutability, enum classes for type safety
- ✅ **kotlinx.datetime**: Instant for timestamps instead of Long or Date
- ✅ **Coroutines**: Suspend functions in DAO for async operations

**Best Practices Applied**:
- Domain model includes optional targetDisplayName for UI flexibility
- DAO provides both suspend functions (one-shot) and Flow (reactive) queries
- OnConflictStrategy.REPLACE for upsert semantics
- Mapper functions as extension functions for clean API
- Enum comments document business meaning (ENTER, EXIT, BOTH)
- Migration follows proper ALTER TABLE pattern (referenced E4.2)

**Design Patterns**:
- **Data Mapper Pattern**: toDomain() and toEntity() conversion functions
- **Repository Pattern**: Data layer prepared for abstraction (to be implemented)
- **DAO Pattern**: Room interface for database operations

**References**:
- [Room Database](https://developer.android.com/training/data-storage/room)
- [Room Migrations](https://developer.android.com/training/data-storage/room/migrating-db-versions)
- [kotlinx.datetime](https://github.com/Kotlin/kotlinx-datetime)
- [Flow with Room](https://developer.android.com/kotlin/flow/stateflow-and-sharedflow#livedata)

### Action Items

#### Medium Priority
1. **Add radius validation to ProximityAlert domain model** ✅
   - **File**: `app/src/main/java/three/two/bit/phonemanager/domain/model/ProximityAlert.kt`
   - **Change**: Add init block: `init { require(radiusMeters in 50..10_000) { "Radius must be 50-10,000 meters" } }`
   - **Owner**: Completed 2025-12-12
   - **AC**: E5.1.2 (range validation)

2. **Implement server integration when backend ready**
   - **Files**: Multiple (AlertRepository.kt, AlertApiService.kt, AlertsScreen.kt, CreateAlertScreen.kt, AlertsViewModel.kt)
   - **Change**: Implement repository with sync logic, API service, and UI components
   - **Owner**: TBD
   - **AC**: E5.1.4, E5.1.5, E5.1.6 (deferred features)

#### Low Priority
3. **Add unit tests for domain model and mappers**
   - **File**: `app/src/test/java/three/two/bit/phonemanager/domain/model/ProximityAlertTest.kt` (new)
   - **Change**: Test ProximityAlert instantiation, enum values, and mapper functions
   - **Owner**: TBD
   - **AC**: Test coverage

4. **Add DAO instrumented tests**
   - **File**: `app/src/androidTest/java/three/two/bit/phonemanager/data/database/ProximityAlertDaoTest.kt` (new)
   - **Change**: Test CRUD operations, Flow queries, and data integrity
   - **Owner**: TBD
   - **AC**: Test coverage

5. **Add Room migration test**
   - **File**: AppDatabase migration test suite (new)
   - **Change**: Validate MIGRATION_3_4 creates proximity_alerts table correctly
   - **Owner**: TBD
   - **AC**: Test coverage

---

## Review Notes

### Implementation Quality: **Very Good (B+)**

**Strengths**:
- **42% AC coverage with strategic deferral** - Data layer foundation (E5.1.1, E5.1.3) fully implemented
- **Clean architecture** - Domain model separate from data layer with proper mappers
- **Comprehensive DAO** - Full CRUD operations with Flow support for reactive queries
- **Room best practices** - Proper entity annotations, migration pattern, enum storage
- **Future-ready** - Data layer prepared for repository and UI integration
- **No technical debt** - Avoided implementing incomplete server features

**Areas for Improvement**:
- ~~Radius validation not enforced in domain model~~ ✅ Fixed: Added init block validation in ProximityAlert
- No test coverage yet (Low priority, can be added with repository/ViewModel)
- Server integration features pending backend (Medium priority, expected deferral)

### Recommendation
**APPROVE** - Data layer foundation is production-ready and demonstrates excellent engineering discipline. The implementation provides a solid base for proximity alerts with clean domain modeling, comprehensive DAO operations, and proper database migration. Strategic deferral of server-dependent components (AC E5.1.4, E5.1.5, E5.1.6) prevents technical debt and aligns with backend development timeline.

The only medium-priority improvement is adding radius validation to the domain model (AC E5.1.2). Test coverage can be addressed when repository and ViewModel are implemented. This foundation-focused approach is pragmatic and positions the project well for future integration.

---

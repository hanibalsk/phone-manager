# Epic 8: Movement Tracking - Testing Plan

**Created**: 2025-11-30
**Status**: Active
**Epic**: E8 - Movement Tracking & Intelligent Path Detection

---

## Executive Summary

This testing plan covers the comprehensive testing requirements for Epic 8, which implements movement tracking, trip detection, and intelligent path analysis. The epic consists of 14 stories (E8.1-E8.14) with approximately 29 incomplete testing tasks.

## Current Test Coverage

### Existing Tests (Completed)

| Component | Test File | Coverage |
|-----------|-----------|----------|
| TripEntity | `TripEntityTest.kt` | Entity creation, validation |
| MovementEventEntity | `MovementEventEntityTest.kt` | Entity creation, validation |
| TripDao | `TripDaoTest.kt` | CRUD operations |
| MovementEventDao | `MovementEventDaoTest.kt` | CRUD operations |
| TripRepository | `TripRepositoryTest.kt` | Repository layer |
| MovementEventRepository | `MovementEventRepositoryTest.kt` | Repository layer |
| SensorTelemetryCollector | `SensorTelemetryCollectorTest.kt` | Telemetry snapshots, buffer calculations |
| TripManager | `TripManagerTest.kt` | State machine, lifecycle |

### Pending Tests by Story

#### Story E8.9: Trip History Screen
- [ ] Unit test ViewModel pagination
- [ ] Unit test filter logic
- [ ] Unit test day grouping
- [ ] UI test trip card display
- [ ] UI test filter interactions

#### Story E8.10: Trip Detail Screen
- [ ] Unit test ViewModel state management
- [ ] Unit test GPX export generation
- [ ] UI test map rendering
- [ ] UI test edit name flow
- [ ] UI test delete flow

#### Story E8.11: Movement Events Screen
- [ ] Unit test ViewModel live mode
- [ ] Unit test export generation
- [ ] Unit test pagination
- [ ] UI test event card expansion

#### Story E8.12: Trip Detection Settings
- [ ] Unit test ViewModel state management
- [ ] UI test toggle interactions
- [ ] UI test stepper interactions
- [ ] UI test navigation links

#### Story E8.13: Home Screen Cards
- [ ] Unit test ViewModel trip observation
- [ ] Unit test duration formatting
- [ ] UI test active trip card display
- [ ] UI test daily summary card display
- [ ] UI test end trip action

#### Story E8.14: Trip Notification
- [ ] Unit test notification content building
- [ ] Unit test mode emoji mapping
- [ ] Unit test duration formatting
- [ ] Unit test distance formatting
- [ ] Manual test notification appearance

---

## Test Categories

### 1. Unit Tests (Priority: High)

#### 1.1 ViewModel Tests

**TripHistoryViewModel** (`TripHistoryViewModelTest.kt`)
```kotlin
// Test cases:
- Initial state loads first page
- Pagination loads next page correctly
- Filter by date range updates results
- Filter by transportation mode updates results
- Day grouping organizes trips correctly
- Delete trip removes from list
- Undo delete restores trip
- Empty state when no trips
- Error state on repository failure
```

**TripDetailViewModel** (`TripDetailViewModelTest.kt`)
```kotlin
// Test cases:
- Load trip by ID populates state
- Edit name updates trip
- Delete trip navigates back
- GPX export generates correct format
- Toggle raw/corrected path updates display
- Mode breakdown calculates correctly
- Statistics compute accurately
```

**MovementEventsViewModel** (`MovementEventsViewModelTest.kt`)
```kotlin
// Test cases:
- Initial state loads events
- Live mode enables real-time updates
- Live mode disables stops updates
- Pagination loads more events
- Export JSON generates correct format
- Export CSV generates correct format
- Filter by mode transition works
- Statistics calculate correctly
```

**TripDetectionSettingsViewModel** (`TripDetectionSettingsViewModelTest.kt`)
```kotlin
// Test cases:
- Initial state loads from preferences
- Toggle trip detection updates preference
- Stepper values update preferences
- Validation prevents invalid values
- Reset to defaults works correctly
```

**HomeViewModel Trip Extensions** (`HomeViewModelTripTest.kt`)
```kotlin
// Test cases:
- Active trip card shows when trip active
- Active trip card hidden when no trip
- Daily summary calculates correctly
- End trip action ends current trip
- Navigation to history works
```

#### 1.2 Formatting/Utility Tests

**NotificationFormattingTest** (`NotificationFormattingTest.kt`)
```kotlin
// Test cases:
- buildTripNotificationContent formats correctly
- getModeEmoji returns correct emoji for each mode
- formatTripDuration handles seconds < 60
- formatTripDuration handles minutes < 60
- formatTripDuration handles hours
- formatTripDistance handles meters < 1000
- formatTripDistance handles kilometers
- getLastUpdateText handles just now
- getLastUpdateText handles minutes ago
- getLastUpdateText handles hours ago
- getLastUpdateText handles never
```

**TripFormattingTest** (`TripFormattingTest.kt`)
```kotlin
// Test cases:
- formatDuration handles all ranges
- formatDistance handles metric
- formatStartTime localizes correctly
- Day grouping keys generate correctly
```

### 2. Integration Tests (Priority: Medium)

#### 2.1 Repository Integration

**TripRepositoryIntegrationTest**
```kotlin
// Test cases:
- Create trip persists to database
- Update trip updates database
- Delete trip removes from database
- Query by date range returns correct results
- Query by mode returns correct results
- Pagination returns correct pages
- Increment location count updates trip
```

#### 2.2 Service Integration

**LocationTrackingServiceTripIntegrationTest**
```kotlin
// Test cases:
- Service starts trip monitoring on start
- Service stops trip monitoring on stop
- Location capture enriches with trip context
- Trip statistics update on location capture
- Notification updates with trip info
```

### 3. UI Tests (Priority: Low - Manual First)

#### 3.1 Screen Tests

**TripHistoryScreenTest**
```kotlin
// Test cases:
- Trip cards display correct information
- Swipe to delete shows snackbar
- Filter chips toggle correctly
- Date range picker opens and selects
- Empty state shows when no trips
- Pull to refresh reloads data
- Infinite scroll loads more
- Navigation to detail works
```

**TripDetailScreenTest**
```kotlin
// Test cases:
- Map renders with polyline
- Statistics display correctly
- Mode breakdown chart renders
- Edit name dialog works
- Delete confirmation dialog works
- GPX export triggers share
- Raw/corrected toggle works
```

**MovementEventsScreenTest**
```kotlin
// Test cases:
- Event cards display correctly
- Expand/collapse works
- Live mode indicator shows
- Export menu opens
- Statistics section displays
```

**TripDetectionSettingsScreenTest**
```kotlin
// Test cases:
- Toggle switches work
- Stepper increments/decrements
- Navigation to sub-screens works
- Values persist on screen rotation
```

---

## Test Execution Strategy

### Phase 1: Unit Tests (Immediate)
1. Create ViewModel test files for E8.9-E8.14
2. Implement notification formatting tests
3. Run with `./gradlew testDebugUnitTest`
4. Target: 80% coverage on ViewModels

### Phase 2: Integration Tests (Short-term)
1. Add repository integration tests
2. Add service integration tests
3. Run with `./gradlew connectedDebugAndroidTest`
4. Target: Critical paths covered

### Phase 3: UI Tests (Medium-term)
1. Add Compose UI tests for critical flows
2. Use `createComposeRule()` for screen tests
3. Target: Happy path coverage

### Phase 4: Manual Testing (Ongoing)
1. Real device testing for notifications
2. GPS simulation for trip detection
3. Battery/performance monitoring

---

## Test Infrastructure

### Dependencies Required
```kotlin
// build.gradle.kts additions if needed
testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
testImplementation("io.mockk:mockk:1.13.8")
testImplementation("app.cash.turbine:turbine:1.0.0") // For Flow testing
testImplementation("androidx.arch.core:core-testing:2.2.0")
```

### Test Utilities
```kotlin
// TestDispatchers for coroutine testing
// FakeTripRepository for ViewModel tests
// FakePreferencesRepository for settings tests
```

---

## Coverage Targets

| Category | Target | Current |
|----------|--------|---------|
| Data Layer (E8.1-E8.3) | 90% | ~85% |
| Core Logic (E8.4-E8.6) | 85% | ~80% |
| ViewModels (E8.9-E8.14) | 80% | ~20% |
| UI Composables | 50% | 0% |
| Overall Epic 8 | 75% | ~50% |

---

## Manual Test Scenarios

### Trip Detection Flow
1. Enable trip detection in settings
2. Start moving (walking/driving)
3. Verify trip starts automatically
4. Check notification shows trip status
5. Stop moving for threshold duration
6. Verify trip ends automatically
7. Check trip appears in history

### Notification Updates
1. Start active trip
2. Verify notification shows mode emoji
3. Change transportation mode
4. Verify emoji updates in real-time
5. Verify duration updates every minute
6. Verify distance updates on location capture
7. Verify "Last update" line is accurate

### Trip History Management
1. View trip history screen
2. Apply date filter
3. Apply mode filter
4. Swipe to delete trip
5. Tap undo to restore
6. Navigate to trip detail
7. Export trip as GPX

---

## Risk Assessment

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| Flaky location tests | High | Medium | Use mock locations |
| Notification timing | Medium | Low | Use test dispatchers |
| Database migrations | Low | High | Test migrations explicitly |
| Memory leaks in flows | Medium | Medium | Use turbine for flow tests |

---

## Next Steps

1. **Immediate**: Create `TripHistoryViewModelTest.kt` and `NotificationFormattingTest.kt`
2. **This Week**: Complete all ViewModel unit tests
3. **Next Week**: Add integration tests for critical paths
4. **Ongoing**: Manual testing with each release

---

**Last Updated**: 2025-11-30

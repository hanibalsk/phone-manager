# Epic 8: Movement Tracking & Intelligent Path Detection

**Author:** Martin
**Date:** 2025-11-30
**Status:** Planned (Not Started)
**PRD Reference:** PRD-movement-tracking.md
**Dependencies:** Epic 0 ✅ (Foundation), existing TransportationModeManager, LocationTrackingService

---

## Epic Overview

| Sub-Epic | Name | Stories | Est. Days | Description |
|----------|------|---------|-----------|-------------|
| E8.A | Trip Detection & Data Layer | 8 | 3-4 | Database schema, repositories, trip state machine, movement event logging, service integration |
| E8.B | Trip UI & User Experience | 6 | 2-3 | Trip history screen, trip detail screen, movement events screen, settings, home screen updates |

**Total:** 14 stories, 5-7 days estimated

---

## Purpose

Add intelligent movement tracking, automatic trip detection, and trip history features to the existing Phone Manager Android app. Includes sensor telemetry logging, transportation mode awareness, and backend sync preparation for path corrections.

---

## Sub-Epic E8.A: Trip Detection & Data Layer

**Goal:** Establish the complete data foundation and core business logic for automatic trip detection, movement event logging, and service integration.

**Dependencies:** Existing TransportationModeManager, LocationTrackingService, Room database v7

**Acceptance Criteria:**
- Database migrates cleanly from v7 to v8
- Trips auto-start on mode change from stationary
- Trips auto-end after configurable stationary threshold
- Movement events logged with full telemetry on every mode change
- Locations enriched with trip ID and transportation mode

---

### Story E8.1: Create TripEntity and MovementEventEntity with Room database migration

**Priority:** Must Have
**Status:** Planned

**Description:**
As a developer, I need the database schema for trips and movement events so that trip data can be persisted locally.

**Acceptance Criteria:**
- [ ] TripEntity created with all fields per spec (id, state, times, locations, statistics, modes, triggers, sync status)
- [ ] MovementEventEntity created with all fields per spec (mode transition, telemetry, location snapshot, device state)
- [ ] LocationEntity extended with transportationMode, detectionSource, modeConfidence, tripId, correction fields
- [ ] MIGRATION_7_8 implemented and tested
- [ ] All indexes created for query performance
- [ ] Foreign key from movement_events to trips with SET NULL on delete

**Technical Notes:**
- See ANDROID_APP_SPEC.md sections 2.2-2.5 for entity definitions
- Use JSON columns for modesUsedJson, modeBreakdownJson

---

### Story E8.2: Implement TripDao and MovementEventDao with all queries

**Priority:** Must Have
**Status:** Planned

**Description:**
As a developer, I need DAO interfaces with all required queries so that trip and event data can be accessed efficiently.

**Acceptance Criteria:**
- [ ] TripDao: insert, update, getById, observeById, getActiveTrip, observeActiveTrip, observeRecentTrips, getTripsBetween, observeTripsByMode, incrementLocationCount, observeTotalDistanceSince, observeTripCountSince, getUnsyncedTrips, markAsSynced, deleteOldTrips
- [ ] MovementEventDao: insert, insertAll, getEventById, observeRecentEvents, observeEventsByTrip, observeLatestEvent, getEventsBetween, observeEventCountSince, getEventCountForTrip, getUnsyncedEvents, markAsSynced, observeUnsyncedCount, deleteOldEvents
- [ ] All Flow-returning methods work correctly with Room
- [ ] Pagination support where specified

**Technical Notes:**
- See ANDROID_APP_SPEC.md sections 2.6-2.7 for DAO definitions

---

### Story E8.3: Create TripRepository and MovementEventRepository with domain models

**Priority:** Must Have
**Status:** Planned

**Description:**
As a developer, I need repository interfaces and implementations so that data access is abstracted from the rest of the app.

**Acceptance Criteria:**
- [ ] Trip domain model created with computed properties (isActive, averageSpeedKmh, durationSeconds)
- [ ] MovementEvent domain model created with nested EventLocation, DeviceState, SensorTelemetry
- [ ] TripRepository interface and TripRepositoryImpl created
- [ ] MovementEventRepository interface and MovementEventRepositoryImpl created
- [ ] Entity ↔ Domain model mapping functions implemented
- [ ] Hilt bindings added to RepositoryModule

**Technical Notes:**
- See ANDROID_APP_SPEC.md Appendix A for domain model definitions

---

### Story E8.4: Implement TripManager with state machine

**Priority:** Must Have
**Status:** Planned

**Description:**
As a system, I need a TripManager that implements the trip state machine so that trips are automatically detected based on transportation mode changes.

**Acceptance Criteria:**
- [ ] TripManager interface created with currentTripState, activeTrip, startMonitoring, stopMonitoring, forceStartTrip, forceEndTrip, query methods
- [ ] TripManagerImpl implements state machine: IDLE → ACTIVE → PENDING_END → COMPLETED
- [ ] Trip starts when mode changes from STATIONARY/UNKNOWN to movement mode
- [ ] Trip enters PENDING_END when mode becomes STATIONARY
- [ ] Trip completes when stationary duration exceeds threshold
- [ ] Grace periods respected (vehicle: 90s, walking: 60s configurable)
- [ ] End time set to when user became stationary, not current time
- [ ] Anti-false-positive: require 2+ consecutive movement locations, ignore <30s transitions
- [ ] Hilt binding added to TripModule

**Technical Notes:**
- See ANDROID_APP_SPEC.md section 3 for state machine diagram and logic

---

### Story E8.5: Create SensorTelemetryCollector for movement event enrichment

**Priority:** Must Have
**Status:** Planned

**Description:**
As a system, I need to collect sensor telemetry at the moment of mode change so that movement events contain rich diagnostic data.

**Acceptance Criteria:**
- [ ] SensorTelemetryCollector singleton created
- [ ] TelemetrySnapshot data class with accelerometer (magnitude, variance, peakFrequency), gyroscope, stepCount, significantMotion, device state
- [ ] collect() suspend function gathers current sensor readings
- [ ] Accelerometer data from 5-second rolling window with FFT for peak frequency
- [ ] Battery level, charging status, network type, network strength captured
- [ ] Graceful handling when sensors unavailable

**Technical Notes:**
- See ANDROID_APP_SPEC.md section 4.2 for data sources

---

### Story E8.6: Integrate movement event recording into TransportationModeManager

**Priority:** Must Have
**Status:** Planned

**Description:**
As a system, I need to record movement events whenever the transportation state changes so that all mode transitions are logged with telemetry.

**Acceptance Criteria:**
- [ ] TransportationModeManager subscribes to its own transportationState flow
- [ ] On mode change, calls SensorTelemetryCollector.collect()
- [ ] Records MovementEventEntity via MovementEventRepository
- [ ] Includes: previousMode, newMode, source, confidence, detectionLatencyMs, location, telemetry, tripId
- [ ] Detection latency calculated from previous state timestamp

**Technical Notes:**
- See ANDROID_APP_SPEC.md section 4.3 for integration code

---

### Story E8.7: Integrate TripManager into LocationTrackingService with location enrichment

**Priority:** Must Have
**Status:** Planned

**Description:**
As a system, I need the LocationTrackingService to enrich locations with trip context and update trip statistics so that all data is properly associated.

**Acceptance Criteria:**
- [ ] LocationTrackingService injects TripManager
- [ ] Location captures include: transportationMode, detectionSource, modeConfidence, tripId
- [ ] When active trip exists, call tripRepository.incrementLocationCount with distance
- [ ] TripManager.startMonitoring() called when service starts
- [ ] TripManager.stopMonitoring() called when service stops
- [ ] Notification updated with active trip info (duration, distance)

**Technical Notes:**
- See ANDROID_APP_SPEC.md section 5.1 for integration code

---

### Story E8.8: Add trip detection preferences to PreferencesRepository

**Priority:** Should Have
**Status:** Planned

**Description:**
As a user, I need configurable trip detection parameters so that I can adjust sensitivity to my travel patterns.

**Acceptance Criteria:**
- [ ] isTripDetectionEnabled (default: true)
- [ ] tripStationaryThresholdMinutes (default: 5, range: 1-30)
- [ ] tripMinimumDurationMinutes (default: 2, range: 1-10)
- [ ] tripMinimumDistanceMeters (default: 100, range: 50-500)
- [ ] isTripAutoMergeEnabled (default: true)
- [ ] tripVehicleGraceSeconds (default: 90, range: 30-180)
- [ ] tripWalkingGraceSeconds (default: 60, range: 30-120)
- [ ] All preferences exposed as Flow for reactive updates
- [ ] TripManager observes preferences and adjusts behavior

**Technical Notes:**
- See ANDROID_APP_SPEC.md section 3.5 for preference definitions

---

## Sub-Epic E8.B: Trip UI & User Experience

**Goal:** Provide users with comprehensive trip history visualization, trip detail views, and settings management, plus developer debugging tools.

**Dependencies:** Sub-Epic E8.A (Trip Detection & Data Layer)

**Acceptance Criteria:**
- Users can view trip history grouped by day
- Users can view trip details with route map
- Users can toggle raw/corrected path on map
- Users can configure trip detection settings
- Active trip status visible on home screen

---

### Story E8.9: Create Trip History Screen with day grouping, filtering, and pagination

**Priority:** Must Have
**Status:** Planned

**Description:**
As a user, I want to view my trip history so that I can review my past travels.

**Acceptance Criteria:**
- [ ] TripHistoryScreen with TripHistoryViewModel created
- [ ] Trips displayed in cards grouped by day (Today, Yesterday, dates)
- [ ] Trip card shows: mode icon, trip name, duration, distance, times, mini route preview
- [ ] Filter by date range (calendar picker)
- [ ] Filter by transportation mode
- [ ] Pagination with "Load More" (20 per page)
- [ ] Pull to refresh
- [ ] Swipe to delete with confirmation
- [ ] Tap navigates to Trip Detail Screen
- [ ] Empty state when no trips

**Technical Notes:**
- See ANDROID_APP_SPEC.md section 6.1 for wireframe
- Navigation route: Screen.TripHistory

---

### Story E8.10: Create Trip Detail Screen with route map and path toggle

**Priority:** Must Have
**Status:** Planned

**Description:**
As a user, I want to view detailed information about a trip so that I can see the route, statistics, and mode breakdown.

**Acceptance Criteria:**
- [ ] TripDetailScreen with TripDetailViewModel created
- [ ] Map view showing route polyline with start (green) and end (red) markers
- [ ] Toggle between raw GPS path (dotted) and corrected path (solid)
- [ ] Trip info: name, date, duration, distance, start/end times and locations
- [ ] Mode breakdown chart (horizontal bar showing percentages)
- [ ] Statistics: average speed, location points, movement events, path corrected status
- [ ] Edit trip name action
- [ ] Export to GPX file action
- [ ] Delete trip action with confirmation
- [ ] Map fits bounds to route

**Technical Notes:**
- See ANDROID_APP_SPEC.md section 6.2 for wireframe
- Navigation route: Screen.TripDetail with tripId parameter

---

### Story E8.11: Create Movement Events Screen for developer debugging

**Priority:** Should Have
**Status:** Planned

**Description:**
As a developer, I want to view the movement events log so that I can verify detection accuracy and debug issues.

**Acceptance Criteria:**
- [ ] MovementEventsScreen with MovementEventsViewModel created
- [ ] Live mode toggle for auto-refresh on new events
- [ ] Event list showing: timestamp, mode transition, source, confidence
- [ ] Expandable event details: location, device state, full telemetry
- [ ] Export to JSON/CSV
- [ ] Clear old events (>7 days) action
- [ ] Filter by mode transition
- [ ] Pagination with "Load More"
- [ ] Statistics: total event count, unsynced count

**Technical Notes:**
- See ANDROID_APP_SPEC.md section 6.3 for wireframe
- Navigation route: Screen.MovementEvents
- Access from Settings screen (developer section)

---

### Story E8.12: Add Trip Detection settings section to Settings Screen

**Priority:** Must Have
**Status:** Planned

**Description:**
As a user, I want to configure trip detection parameters so that the system matches my travel patterns.

**Acceptance Criteria:**
- [ ] New "Trip Detection" section in SettingsScreen
- [ ] Enable Trip Detection toggle
- [ ] End Trip After Stationary: segmented control (1/5/10/30 min)
- [ ] Minimum Trip Duration: stepper (1-10 min)
- [ ] Minimum Trip Distance: stepper (50-500 m)
- [ ] Auto-Merge Brief Stops toggle
- [ ] "View Trip History" navigation link
- [ ] "View Movement Events" navigation link (developer)
- [ ] SettingsViewModel updated with trip detection state and actions

**Technical Notes:**
- See ANDROID_APP_SPEC.md section 6.4 for wireframe

---

### Story E8.13: Add active trip card and daily summary to Home Screen

**Priority:** Should Have
**Status:** Planned

**Description:**
As a user, I want to see my current trip status and daily summary on the home screen so that I have quick access to trip information.

**Acceptance Criteria:**
- [ ] When trip active: Card showing mode icon, "Active Trip", start time, duration, distance, location count, progress indicator, "End Trip" button
- [ ] When no trip: Card showing "Today's Activity", trip count, total moving time, total distance, "View History" link
- [ ] HomeViewModel updated to observe activeTrip and daily statistics
- [ ] Card tap navigates to Trip History

**Technical Notes:**
- See ANDROID_APP_SPEC.md section 6.5 for wireframe

---

### Story E8.14: Update tracking notification with active trip status

**Priority:** Should Have
**Status:** Planned

**Description:**
As a user, I want the tracking notification to show my current trip status so that I know the app is tracking my journey.

**Acceptance Criteria:**
- [ ] When trip active, notification shows: "🚗 Trip in progress • 23 min • 8.2 km"
- [ ] Mode icon updates based on current transportation mode
- [ ] Duration and distance update periodically
- [ ] Standard tracking info when no active trip

**Technical Notes:**
- See ANDROID_APP_SPEC.md section 6.6 for notification format

---

## Implementation Sequence

Based on dependencies, implement in this order:

**Phase 1 (Day 1-2): Data Foundation**
1. E8.1: Database entities and migration
2. E8.2: DAOs
3. E8.3: Repositories and domain models

**Phase 2 (Day 2-3): Core Logic**
4. E8.8: Trip detection preferences
5. E8.5: Sensor telemetry collector
6. E8.4: TripManager state machine

**Phase 3 (Day 3-4): Service Integration**
7. E8.6: Movement event recording in TransportationModeManager
8. E8.7: TripManager integration in LocationTrackingService

**Phase 4 (Day 4-5): UI Screens**
9. E8.12: Settings screen updates
10. E8.9: Trip History Screen
11. E8.10: Trip Detail Screen

**Phase 5 (Day 5-6): Polish**
12. E8.13: Home screen updates
13. E8.14: Notification updates
14. E8.11: Movement Events Screen (developer)

**Phase 6 (Day 6-7): Testing & Refinement**
- Integration testing
- Manual testing scenarios per ANDROID_APP_SPEC.md section 10.4
- Bug fixes and polish

---

## Related Documents

- **Android App Spec:** `docs/ANDROID_APP_SPEC.md`
- **Backend API Spec:** `docs/BACKEND_API_SPEC.md`
- **PRD:** `docs/PRD-movement-tracking.md`

---

## Change Log

| Date | Author | Changes |
|------|--------|---------|
| 2025-11-30 | Martin | Initial epic creation from Android App Spec |

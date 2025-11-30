# Movement Tracking & Intelligent Path Detection - Product Requirements Document (PRD)

**Author:** Martin
**Date:** 2025-11-30
**Project Level:** Level 2
**Project Type:** Mobile Application (Android) - Feature Addition
**Target Scale:** 14 stories across 2 sub-epics (Epic 8)

---

## Description, Context and Goals

### Description

Movement Tracking & Intelligent Path Detection is a feature enhancement for the Phone Manager Android app that adds automatic trip detection, movement event logging with full sensor telemetry, and comprehensive trip history management. The system intelligently detects when users begin and end trips based on transportation mode changes, logs detailed movement events with sensor data, and provides users with a complete trip history including route visualization with optional backend-corrected paths.

### Deployment Intent

**Production app** - Extending existing Phone Manager application deployed via Play Store.

### Context

Phone Manager already tracks user location and detects transportation modes via the existing `TransportationModeManager`. However, this data is transient and not organized into meaningful trip segments. Users have no visibility into their travel patterns, and the system lacks the infrastructure to support intelligent path detection and backend-powered route corrections. This feature set transforms raw location and mode data into structured trips, enabling trip history review, route visualization, and laying the foundation for future intelligent path detection capabilities including frequent location clustering, route pattern detection, and ETA prediction.

### Goals

1. **Automatic Trip Detection** - Enable seamless trip lifecycle management that automatically starts and ends trips based on transportation mode changes without user intervention

2. **Comprehensive Movement Logging** - Capture detailed movement events with full sensor telemetry for analytics, debugging, and future intelligent path detection

3. **Trip History Visualization** - Provide users with accessible trip history and detailed trip views including route maps with raw and corrected path display

---

## Requirements

### Functional Requirements

| ID | Requirement |
|----|-------------|
| FR001 | System shall automatically detect trip start when transportation mode changes from STATIONARY/UNKNOWN to a movement mode (WALKING, RUNNING, CYCLING, IN_VEHICLE) |
| FR002 | System shall automatically detect trip end when user remains stationary beyond configurable threshold (default: 5 minutes) |
| FR003 | System shall log movement events with full sensor telemetry (accelerometer, gyroscope, step count) on every transportation mode change |
| FR004 | System shall associate location captures with current transportation mode, detection source, and active trip ID |
| FR005 | System shall track trip statistics including distance, duration, location count, and transportation mode breakdown |
| FR006 | System shall store backend-provided path corrections and display corrected routes alongside raw GPS paths |
| FR007 | User shall be able to view trip history grouped by day with filtering by date range and transportation mode |
| FR008 | User shall be able to view trip details including route map, mode breakdown chart, and trip statistics |
| FR009 | User shall be able to toggle between raw GPS path and corrected path on trip detail map |
| FR010 | User shall be able to manually force-start or force-end a trip |
| FR011 | User shall be able to configure trip detection parameters (stationary threshold, minimum duration, minimum distance) |
| FR012 | System shall display active trip status on home screen with duration, distance, and location count |
| FR013 | System shall support brief stop tolerance (grace periods) to prevent false trip endings at traffic lights |
| FR014 | Developer shall be able to view movement events log with full telemetry for debugging purposes |

### Non-Functional Requirements

| ID | Category | Requirement |
|----|----------|-------------|
| NFR001 | **Performance** | Trip detection latency shall be <500ms from mode change to trip state update |
| NFR002 | **Battery** | Movement event logging and trip tracking shall add no more than 5% additional battery consumption over existing location tracking |
| NFR003 | **Storage** | Database migration from v7 to v8 shall preserve all existing location data without data loss |
| NFR004 | **Reliability** | Trip state shall persist across app restarts and device reboots; active trips shall resume automatically |
| NFR005 | **Offline** | All trip detection, logging, and storage shall function fully offline; sync to backend shall queue when connectivity unavailable |

---

## User Journeys

### Primary Journey: Daily Commute Tracking

```
┌─────────────────────────────────────────────────────────────────────────┐
│ PERSONA: Regular commuter who wants to track travel patterns           │
└─────────────────────────────────────────────────────────────────────────┘

1. USER LEAVES HOME
   ├─ Phone detects mode change: STATIONARY → WALKING
   ├─ System auto-starts new trip (trigger: MODE_CHANGE)
   ├─ Movement event logged with location + telemetry
   └─ Home screen shows: "🚶 Active Trip • Started 8:15 AM"

2. USER ENTERS CAR
   ├─ Phone detects mode change: WALKING → IN_VEHICLE (via Bluetooth)
   ├─ Movement event logged, trip continues
   ├─ Mode segment tracked: WALKING duration recorded
   └─ Home screen updates: "🚗 Active Trip • 2 min • 0.3 km"

3. USER STOPS AT TRAFFIC LIGHT (90 seconds)
   ├─ Mode briefly shows STATIONARY
   ├─ Grace period active (vehicle: 90s threshold)
   └─ Trip continues without interruption

4. USER ARRIVES AT WORK
   ├─ Mode changes: IN_VEHICLE → WALKING → STATIONARY
   ├─ Stationary threshold timer starts (5 min default)
   ├─ After 5 min stationary: Trip auto-ends
   ├─ End time set to when user became stationary
   └─ Notification: "Trip completed • 45 min • 12.5 km"

5. USER REVIEWS TRIP LATER
   ├─ Opens Trip History from home screen card
   ├─ Sees today's trips grouped, taps morning commute
   ├─ Views route on map (toggles raw ↔ corrected path)
   ├─ Reviews mode breakdown: 92% driving, 8% walking
   └─ Checks statistics: avg speed, location points, etc.
```

---

## UX Design Principles

1. **Invisible Intelligence** - Trip detection operates automatically in the background; users should rarely need to manually start/end trips. The system "just works."

2. **Glanceable Status** - Active trip information on home screen provides instant awareness (duration, distance, mode) without requiring navigation into detail screens.

3. **Progressive Disclosure** - Trip history shows essential info (mode icon, times, distance) in cards; detailed statistics, maps, and telemetry available on drill-down.

4. **Trust Through Transparency** - Show both raw GPS path and corrected path; let users toggle to understand how their data is processed and improved.

5. **Developer-Friendly Debugging** - Movement Events screen exposes full telemetry for developers/power users to verify detection accuracy without cluttering main UI.

---

## Epics

### Epic 8: Movement Tracking & Intelligent Path Detection

| Sub-Epic | Name | Stories | Est. Days |
|----------|------|---------|-----------|
| E8.A | Trip Detection & Data Layer | 8 | 3-4 |
| E8.B | Trip UI & User Experience | 6 | 2-3 |

**Total:** 14 stories, 5-7 days

#### Sub-Epic E8.A Stories (Data Layer)
- E8.1: TripEntity, MovementEventEntity, database migration v7→v8
- E8.2: TripDao and MovementEventDao with all queries
- E8.3: TripRepository, MovementEventRepository, domain models
- E8.4: TripManager with state machine (IDLE→ACTIVE→PENDING_END→COMPLETED)
- E8.5: SensorTelemetryCollector for movement event enrichment
- E8.6: Movement event recording in TransportationModeManager
- E8.7: TripManager integration in LocationTrackingService
- E8.8: Trip detection preferences in PreferencesRepository

#### Sub-Epic E8.B Stories (UI)
- E8.9: Trip History Screen with filtering and pagination
- E8.10: Trip Detail Screen with route map and path toggle
- E8.11: Movement Events Screen (developer debugging)
- E8.12: Trip Detection settings section
- E8.13: Active trip card on Home Screen
- E8.14: Tracking notification with trip status

> **See:** `docs/product/Epic-E8-Movement-Tracking.md` for detailed story breakdown

---

## Out of Scope

Features preserved for future phases:

1. **Intelligent Path Detection** - Frequent location clustering, route pattern detection, deviation alerts, ETA prediction
2. **Backend Path Correction Processing** - Actual map-snapping via OSRM/Valhalla (app is correction-ready)
3. **Trip Sharing** - Share trips with other users or social platforms
4. **Trip Naming AI** - Automatic naming based on start/end locations
5. **Carbon Footprint Calculation** - Emissions estimates by mode
6. **Multi-device Trip Correlation** - Family member trip correlation

---

## Assumptions & Dependencies

### Assumptions
- Existing `TransportationModeManager` provides reliable mode detection
- Room database migration system handles v7→v8 cleanly
- Device sensors (accelerometer, gyroscope, step counter) available on target devices (API 26+)

### Dependencies
- **Internal:** Epic 0 Foundation ✅, LocationTrackingService, TransportationModeManager
- **External:** Google Maps SDK (trip detail map), Android Sensor APIs

---

## Next Steps

1. **Review PRD** with stakeholders
2. **Begin implementation** following Epic-E8 story sequence
3. **Tech spec available** in `docs/ANDROID_APP_SPEC.md` (detailed implementation guide)
4. **Backend API ready** in `docs/BACKEND_API_SPEC.md` for future sync integration

---

## Document Status

- [x] Goals and context defined
- [x] All functional requirements documented
- [x] User journey covers primary persona
- [x] Epic structure defined with story breakdown
- [x] Technical spec available (ANDROID_APP_SPEC.md)
- [ ] Ready for implementation

---

## Related Documents

| Document | Path | Description |
|----------|------|-------------|
| Epic Breakdown | `docs/product/Epic-E8-Movement-Tracking.md` | Detailed stories with acceptance criteria |
| Android App Spec | `docs/ANDROID_APP_SPEC.md` | Technical implementation specification |
| Backend API Spec | `docs/BACKEND_API_SPEC.md` | API contracts for backend integration |
| Main Epics | `docs/epics.md` | Master epic list for Phone Manager |

---

_This PRD follows BMAD Level 2 structure - focused requirements for a feature addition to an existing application._

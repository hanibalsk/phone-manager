# Phone Manager - Epic Breakdown

**Author:** Martin
**Date:** 2025-11-28
**Project Level:** 2
**Target Scale:** 24 stories across 8 epics
**Source:** PRD Phone Manager v1.1 (2025-11-25)

---

## Epic Overview

This project consists of 8 epics aligned with the PRD MVP features:

- **Epic 0**: Foundation Infrastructure (DONE) - 5 stories ✅
- **Epic 1**: Device Registration & Groups (DONE) - 3 stories ✅
- **Epic 2**: Secret Mode (PLANNED) - 2 stories
- **Epic 3**: Real-Time Map & Group Display (PARTIAL) - 3 stories 🔄
- **Epic 4**: Location History (DONE) - 2 stories ✅
- **Epic 5**: Proximity Alerts (DONE) - 2 stories ✅
- **Epic 6**: Geofencing with Webhooks (DONE) - 3 stories ✅
- **Epic 7**: Weather Forecast (DONE) - 4 stories ✅

**Total Stories:** 24 (20 done + 4 remaining)

---

## Epic 0: Foundation Infrastructure (DONE) ✅

**Priority:** Critical (Prerequisite)
**Status:** Done
**Type:** Technical Enabler

### Purpose
Core location tracking infrastructure - already implemented and verified.

### Completed Components

| Component | Status | Description |
|-----------|--------|-------------|
| Foreground Service | ✅ Done | LocationTrackingService with notifications, error recovery |
| Location Manager | ✅ Done | GPS capture using FusedLocationProviderClient |
| Room Database | ✅ Done | SQLite with LocationDao, LocationQueueDao |
| Ktor HTTP Client | ✅ Done | API service with batch upload |
| DataStore Preferences | ✅ Done | Tracking state persistence |
| Secure Storage | ✅ Done | Encrypted API keys, device ID |
| WorkManager | ✅ Done | Background queue processing |
| Service Watchdog | ✅ Done | Health monitoring |
| Boot Receiver | ✅ Done | Auto-start on device boot |
| Permission Management | ✅ Done | Runtime permission handling |
| Hilt DI | ✅ Done | Full dependency injection |

### Existing Stories (Reference Only)

- Story 0.1: Project structure ✅
- Story 0.2.1-0.2.4: Core services ✅
- Story 1.1-1.4: Location tracking core ✅

---

## Epic 1: Device Registration & Groups ✅

**Priority:** Critical (Foundation for other features)
**Estimated Effort:** Medium (3-4 days)
**Status:** Done
**PRD Reference:** Feature 6 (Section 5, FR-6.x)

### Purpose
Enable devices to register with the server and join groups for location sharing. This is the foundation for all multi-device features.

### Stories

#### Story E1.1: Device Registration Flow
**Status:** Complete
**Estimated Effort:** Medium
**PRD Reference:** FR-6.1

**User Story:**
As a user, I want to register my device with a display name so that others can identify me

**Acceptance Criteria:**
- Generate unique deviceId (UUID) on first launch
- Settings screen with displayName input field
- Settings screen with groupId input field
- Register device via `POST /api/devices/register`
- Store deviceId, displayName, groupId in SecureStorage
- Registration payload: deviceId, displayName, groupId, platform

#### Story E1.2: Group Member Discovery
**Status:** Complete
**Estimated Effort:** Small
**PRD Reference:** FR-6.2, FR-6.3

**User Story:**
As a user, I want to see devices in my group so that I can share location with specific people

**Acceptance Criteria:**
- Fetch group members via `GET /api/devices?groupId={id}`
- Create Device entity and DeviceRepository
- Display group member list in UI
- Response includes: deviceId, displayName, lastLocation (optional)

#### Story E1.3: Device Settings Screen
**Status:** Complete
**Estimated Effort:** Small

**User Story:**
As a user, I want to configure my device settings so that I can update my display name or change groups

**Acceptance Criteria:**
- Settings screen with device configuration
- Ability to update displayName
- Ability to change groupId
- Re-register on changes via PUT/POST

---

## Epic 2: Secret Mode

**Priority:** High
**Estimated Effort:** Low (2 days)
**Status:** Planned (Not Started)
**PRD Reference:** Feature 1 (Section 5, FR-1.x)

### Purpose
Application remains fully functional but minimally visible to casual observers.

### Stories

#### Story E2.1: Secret Mode Activation
**Status:** Approved (Ready for Development)
**Estimated Effort:** Small
**PRD Reference:** FR-1.1, FR-1.3

**User Story:**
As a user, I want to enable secret mode so that the app is minimally visible on my device

**Acceptance Criteria:**
- Store `secret_mode_enabled: Boolean` in DataStore
- Hidden activation: long-press on logo (3 seconds) OR 5x tap on version
- No visible UI indication when secret mode is enabled
- No "Location tracking is ON" toasts in secret mode
- Disable verbose Logcat logging in secret mode

#### Story E2.2: Discreet Notification
**Status:** Approved (Ready for Development)
**Estimated Effort:** Small
**PRD Reference:** FR-1.2

**User Story:**
As a user, I want the foreground notification to be discreet so that others don't notice tracking

**Acceptance Criteria:**
- Generic notification title: "Service running"
- Neutral icon (no GPS/location symbol)
- IMPORTANCE_MIN or IMPORTANCE_LOW
- No sound and no vibration
- Switch between normal/secret notification based on mode

---

## Epic 3: Real-Time Map & Group Display 🔄

**Priority:** Critical
**Estimated Effort:** High (5-6 days)
**Status:** Partial (Foundation Complete)
**PRD Reference:** Feature 2 (Section 5, FR-2.x)
**Dependencies:** Epic 1 (Device Registration) ✅

### Purpose
Display current device location and all group members on an interactive Google Map.

### Stories

#### Story E3.1: Google Maps Integration
**Status:** Approved (Foundation Implemented)
**Estimated Effort:** Medium
**PRD Reference:** FR-2.1

**User Story:**
As a user, I want to see my current location on a map so that I can orient myself

**Acceptance Criteria:**
- Integrate Google Maps SDK with Jetpack Compose
- Display current device location with distinctive marker
- Center map on current device location on initial load
- Support standard map interactions (pan, zoom, rotate)
- Create MapScreen and MapViewModel

#### Story E3.2: Group Members on Map
**Status:** Approved (Pending Implementation)
**Estimated Effort:** Medium
**PRD Reference:** FR-2.2

**User Story:**
As a user, I want to see all group members' locations on the map so that I know where everyone is

**Acceptance Criteria:**
- Display markers for all devices in same groupId
- Each marker displays device's displayName
- Markers visually distinct from current device marker
- Tapping marker shows additional info (name, last update time)

#### Story E3.3: Real-Time Location Polling
**Status:** Approved (Pending Implementation)
**Estimated Effort:** Medium
**PRD Reference:** FR-2.3

**User Story:**
As a user, I want location markers to update periodically so that I see near real-time positions

**Acceptance Criteria:**
- Poll for group members' locations every 10-30 seconds (configurable)
- Update markers on map without manual refresh
- Indicate last update time for each member
- Handle network failures gracefully with retry logic

---

## Epic 4: Location History ✅

**Priority:** High
**Estimated Effort:** Medium (3-4 days)
**Status:** Complete (Core Features Implemented)
**PRD Reference:** Feature 4 (Section 5, FR-4.x)

### Purpose
Store, sync, and visualize historical location data as polylines on the map.

### Stories

#### Story E4.1: Location History UI
**Status:** Approved (Foundation Implemented)
**Estimated Effort:** Medium
**PRD Reference:** FR-4.1, FR-4.3

**User Story:**
As a user, I want to view my location history on the map so that I can see where I've been

**Acceptance Criteria:**
- Create HistoryScreen with map view
- Display history as polyline on map
- Support viewing own device history from local Room database
- Date/time filter (today, yesterday, last 7 days, custom range)

#### Story E4.2: History Performance & Server Sync
**Status:** Complete (Core Features Implemented)
**Estimated Effort:** Medium
**PRD Reference:** FR-4.2, FR-4.4

**User Story:**
As a user, I want to view other group members' history so that I can see where they've been

**Acceptance Criteria:**
- Fetch history from server via `GET /api/devices/{deviceId}/locations`
- Downsample points for display (target: 200-500 points visible)
- Support viewing other group members' history
- Mark synced records to avoid duplicate uploads (add isSynced, syncedAt to LocationEntity)

---

## Epic 5: Proximity Alerts ✅

**Priority:** Medium
**Estimated Effort:** Medium (3-4 days)
**Status:** Complete
**PRD Reference:** Feature 3 (Section 5, FR-3.x)
**Dependencies:** Epic 1 ✅, Epic 3 🔄

### Purpose
Alert user when approaching or leaving proximity of another group member.

### Stories

#### Story E5.1: Proximity Alert Definition
**Status:** Complete
**Estimated Effort:** Medium
**PRD Reference:** FR-3.1, FR-3.4

**User Story:**
As a user, I want to create a proximity alert for another user so that I'm notified when they're nearby

**Acceptance Criteria:**
- Create ProximityAlert entity: ownerDeviceId, targetDeviceId, radiusMeters, direction, active
- Support radius values from 50 to 10,000 meters
- Support direction types: "enter", "exit", "both"
- Store alerts on server for sync via `POST /api/proximity-alerts`
- UI to create, view, edit, and delete proximity alerts

#### Story E5.2: Proximity Alert Triggering
**Status:** Foundation Complete (Calculation Logic Ready)
**Estimated Effort:** Medium
**PRD Reference:** FR-3.2, FR-3.3

**User Story:**
As a user, I want to receive notifications when proximity alerts trigger so that I know when someone is nearby

**Acceptance Criteria:**
- Client-side distance calculation using Haversine or Location.distanceTo()
- Calculation occurs during each location poll cycle
- Track lastState ("inside" or "outside") for each alert
- Alert triggers only on state transition (debounce)
- Display local notification when alert triggers

---

## Epic 6: Geofencing with Webhooks ✅

**Priority:** Medium
**Estimated Effort:** High (5-6 days)
**Status:** Complete
**PRD Reference:** Feature 5 (Section 5, FR-5.x)

### Purpose
Define place-based alerts that trigger local notifications and webhook automations for Home Assistant/n8n integration.

### Stories

#### Story E6.1: Geofence Definition
**Status:** Complete
**Estimated Effort:** Medium
**PRD Reference:** FR-5.1

**User Story:**
As a user, I want to define geofence zones so that I get alerts for specific places

**Acceptance Criteria:**
- Create Geofence entity: name, latitude, longitude, radiusMeters, transitionTypes
- Support transition types: ENTER, EXIT, DWELL
- Register geofences with Android Geofencing API
- Store geofences on server via `POST /api/geofences`
- UI to create, view, edit, and delete geofences

#### Story E6.2: Geofence Events & Notifications
**Status:** Complete
**Estimated Effort:** Medium
**PRD Reference:** FR-5.2

**User Story:**
As a user, I want local notifications on geofence events so that I know when I enter/exit zones

**Acceptance Criteria:**
- Receive geofence events via GeofenceBroadcastReceiver
- Display local notification on geofence event
- Send event to backend: `POST /api/geofence-events`
- Create GeofenceEvent entity for logging

#### Story E6.3: Webhook Integration
**Status:** Complete
**Estimated Effort:** Medium
**PRD Reference:** FR-5.3, FR-5.4

**User Story:**
As a user, I want geofence events to trigger webhooks so that I can automate actions in Home Assistant or n8n

**Acceptance Criteria:**
- Create Webhook entity: targetUrl, secret (for HMAC)
- Link geofences to webhooks
- Backend sends HTTP POST to webhook URL on geofence event
- Webhook payload includes: deviceId, geofenceId, eventType, timestamp, location
- Webhook request includes HMAC signature in X-Signature header
- CRUD operations for webhooks via `/api/webhooks`

---

## Epic 7: Weather Forecast Integration ✅

**Priority:** Medium
**Estimated Effort:** Medium (3-4 days)
**Status:** ✅ **COMPLETE - Production Ready**
**PRD Reference:** Enhancement (Post-MVP)
**Dependencies:** Epic 0 ✅ (uses existing Ktor, DataStore, LocationManager)
**Completed:** 2025-11-28

### Purpose
Transform the foreground service notification from a generic "Service running" message into useful weather information. Provides dual value: less intrusive notification + useful weather forecast feature.

### Implementation Summary

**Files Created:** 11 new files
**Files Modified:** 8 existing files
**Test Coverage:** 273 unit tests, all passing
**Code Review:** All stories approved
**Device Testing:** Verified on SM-A366B (Android API 36)

### Stories

#### Story E7.1: Weather API Integration & Caching ✅
**Status:** Complete - Approved
**Estimated Effort:** Medium (1 day)
**Actual Effort:** 1 day

**User Story:**
As a developer, I want to integrate the Open-Meteo weather API so that I can retrieve weather data using the device's location

**Implementation:**
- ✅ WeatherApiService with Ktor HttpClient
- ✅ Open-Meteo API integration (https://api.open-meteo.com/v1/forecast)
- ✅ Weather domain model with CurrentConditions, DailyForecast, WeatherCode enum (27 conditions)
- ✅ WeatherRepository with cache-first strategy
- ✅ WeatherCache using DataStore with 30-minute TTL
- ✅ WeatherModule for Hilt DI
- ✅ 6 comprehensive unit tests
- ✅ Coordinate validation (lat: -90 to 90, lon: -180 to 180)

#### Story E7.2: Notification Weather Display ✅
**Status:** Complete - Approved
**Estimated Effort:** Medium (1 day)
**Actual Effort:** 1 day

**User Story:**
As a user, I want to see current weather in my tracking notification so that the notification provides useful information

**Implementation:**
- ✅ WeatherRepository injected into LocationTrackingService
- ✅ Three-way notification logic: secret mode > weather > original
- ✅ Weather formatting utilities (toNotificationTitle, toNotificationText)
- ✅ Notification channel: IMPORTANCE_MIN, VISIBILITY_SECRET, silent
- ✅ Automatic weather refresh on location updates
- ✅ @Volatile cached weather (no runBlocking, prevents ANR)

**Device Test Results:**
- Weather cached: -0.7°C (Bratislava, Slovakia)
- Service running with weather integration
- Notification updating automatically

#### Story E7.3: Weather Screen UI ✅
**Status:** Complete - Approved
**Estimated Effort:** Medium (1 day)
**Actual Effort:** 1 day

**User Story:**
As a user, I want to view detailed weather forecast so that I can plan my day

**Implementation:**
- ✅ WeatherScreen with Material3 design
- ✅ WeatherViewModel with Loading/Success/Error states
- ✅ Current conditions card (large temp, emoji icon, feels like, humidity, wind)
- ✅ 5-day forecast list with day formatting (Today/Tomorrow/DayOfWeek)
- ✅ Last updated indicator with relative time
- ✅ Error state with retry button
- ✅ Navigation integration (Screen.Weather route)
- ✅ All strings externalized with stringResource()
- ✅ SecurityException handling for permission denial

#### Story E7.4: Weather Settings Toggle ✅
**Status:** Complete - Approved
**Estimated Effort:** Small (0.5 day)
**Actual Effort:** 0.5 day

**User Story:**
As a user, I want to toggle weather display in notifications so that I can choose between weather info and standard notification

**Implementation:**
- ✅ showWeatherInNotification preference in PreferencesRepository
- ✅ Settings toggle UI with Row/Switch layout
- ✅ SettingsViewModel with StateFlow integration
- ✅ Default: true (weather enabled)
- ✅ Immediate notification update via observer
- ✅ DataStore persistence across restarts

### Review Outcomes

**All Stories Approved** with minor findings addressed:
- Fixed cache offline fallback logic
- Added coordinate validation
- Updated notification channel configuration
- Removed blocking I/O (runBlocking)
- Replaced hardcoded strings with stringResource()
- Added permission checks

### Technical Notes
- **API**: Open-Meteo (free, unlimited, no API key)
- **Cache**: 30-minute TTL in DataStore with offline fallback
- **Refresh**: Automatic on location updates (no separate timer)
- **Priority**: Secret mode > Weather > Original notification
- **Full spec**: See `/docs/product/Epic-E7-Weather-Forecast.md`
- **Test Report**: See `/docs/testing/EPIC-7-COMPLETION-SUMMARY.md`

---

## Dependencies

```
Epic 0 (Foundation) ✅ DONE
    │
    ├──► Epic 7 (Weather Forecast) 🆕  [Independent - uses existing location]
    │
    ▼
Epic 1 (Device Registration) ─────┐
    │                              │
    ├──► Epic 2 (Secret Mode)     │
    │                              │
    ▼                              │
Epic 3 (Map Display) ◄────────────┘
    │
    ├──► Epic 4 (Location History)
    │
    └──► Epic 5 (Proximity Alerts)
              │
              ▼
         Epic 6 (Geofencing + Webhooks)
```

**Critical Path:** Epic 0 → Epic 1 → Epic 3 → Epic 5/6
**Independent:** Epic 7 (Weather) can be implemented anytime after Epic 0

---

## Implementation Order (Recommended)

| Order | Epic | Rationale |
|-------|------|-----------|
| 1 | Epic 1: Device Registration | Foundation for all multi-device features |
| 2 | Epic 2: Secret Mode | Quick win, client-only, extends existing notification |
| 3 | Epic 3: Map Display | Core visual feature, depends on Epic 1 |
| 4 | Epic 4: Location History | Extends map with historical data |
| 5 | Epic 5: Proximity Alerts | Requires group member locations |
| 6 | Epic 6: Geofencing + Webhooks | Most complex, requires backend webhook dispatcher |
| 7 | Epic 7: Weather Forecast | Independent, can be done anytime; enhances notification UX |

---

## Notes

- All stories follow the naming convention: `story-{epic}.{story}.md`
- Story status progression: Planned → Draft → In Progress → Ready for Review → Done
- Each story must have acceptance criteria and tasks/subtasks
- Dev notes should reference PRD sections (e.g., [Source: PRD FR-6.1])
- Existing Epic 0 stories (0.1, 0.2.x, 1.x) remain for reference but are DONE

---

## Change Log

| Date | Author | Changes |
|------|--------|---------|
| 2025-10-28 | Martin | Original epic breakdown |
| 2025-11-25 | Martin | Complete rewrite to align with PRD v1.1 MVP features |
| 2025-11-28 | John (PM) | Added Epic 7: Weather Forecast Integration (4 stories) |
| 2025-11-28 | Dev Agent | Completed Epic 7: All 4 stories implemented, tested, reviewed, and approved |
| 2025-11-28 | Martin | Epic 7 marked complete - production ready with device testing verified |

# Product Requirements Document (PRD)
# Phone Manager

**Version:** 1.1
**Date:** 2025-11-25
**Author:** Martin
**Status:** Draft
**Source:** Product Brief - Phone Manager (2025-11-25)

---

## Table of Contents

1. [Overview](#1-overview)
2. [Current Implementation Status](#2-current-implementation-status)
3. [Goals and Success Metrics](#3-goals-and-success-metrics)
4. [User Stories](#4-user-stories)
5. [Functional Requirements](#5-functional-requirements)
   - [Feature 1: Secret Mode](#feature-1-secret-mode)
   - [Feature 2: Real-Time Map & Group Display](#feature-2-real-time-map--group-display)
   - [Feature 3: Proximity Alerts](#feature-3-proximity-alerts)
   - [Feature 4: Location History](#feature-4-location-history)
   - [Feature 5: Geofencing with Webhooks](#feature-5-geofencing-with-webhooks)
   - [Feature 6: Device Registration & Groups](#feature-6-device-registration--groups)
6. [API Specifications](#6-api-specifications)
7. [Data Models](#7-data-models)
8. [Non-Functional Requirements](#8-non-functional-requirements)
9. [Technical Architecture](#9-technical-architecture)
10. [Security Requirements](#10-security-requirements)
11. [Out of Scope](#11-out-of-scope)
12. [Dependencies and Risks](#12-dependencies-and-risks)
13. [Release Criteria](#13-release-criteria)

---

## 1. Overview

### 1.1 Product Summary

Phone Manager is a private, self-hosted location tracking application for Android devices designed for trusted groups such as families or small teams. The application enables real-time location monitoring, intelligent proximity and geofence alerts, movement history visualization, and automation integration via webhooks.

### 1.2 Target Users

| Segment | Description |
|---------|-------------|
| **Primary** | Privacy-conscious parents and family coordinators (age 30-55, technical comfort moderate-high) |
| **Secondary** | Small business owners tracking field employees or assets (2-10 workers) |

### 1.3 Technology Stack

| Component | Technology |
|-----------|------------|
| **Android Client** | Kotlin, Jetpack Compose, Hilt, Room, DataStore, Ktor Client |
| **Backend** | Rust (Actix-web or Axum) |
| **Database** | PostgreSQL (server), SQLite/Room (client) |
| **Maps** | Google Maps SDK for Compose |
| **Location** | Google Play Services, Geofencing API |

### 1.4 Project Package

```
three.two.bit.phonemanager
```

---

## 2. Current Implementation Status

### 2.1 Implementation Overview

The Phone Manager Android application has an existing codebase with core location tracking infrastructure already implemented. This PRD builds upon the existing foundation.

### 2.2 Already Implemented (✅)

| Component | Location | Description |
|-----------|----------|-------------|
| **Foreground Service** | `service/LocationTrackingService.kt` | Full location tracking service with notifications, error recovery, exponential backoff |
| **Location Manager** | `location/LocationManager.kt` | GPS location capture using FusedLocationProviderClient |
| **Room Database** | `data/database/AppDatabase.kt` | SQLite database with LocationDao, LocationQueueDao |
| **Location Entity** | `data/model/LocationEntity.kt` | Entity with lat, lng, accuracy, timestamp, altitude, bearing, speed, provider |
| **Location Queue** | `data/model/LocationQueueEntity.kt` | Queue entity for pending uploads |
| **Ktor HTTP Client** | `network/LocationApiService.kt` | API service with single/batch upload using Ktor Client |
| **Network Manager** | `network/NetworkManager.kt` | Connectivity monitoring, battery level, network type |
| **API Configuration** | `network/LocationApiService.kt` | ApiConfiguration with baseUrl, apiKey, endpoints |
| **DataStore Preferences** | `data/preferences/PreferencesRepository.kt` | Tracking enabled, interval, service state persistence |
| **Secure Storage** | `security/SecureStorage.kt` | Encrypted storage for API keys, device ID |
| **WorkManager** | `queue/WorkManagerScheduler.kt` | Background queue processing |
| **Queue Manager** | `queue/QueueManager.kt` | Location upload queue management |
| **Service Watchdog** | `watchdog/WatchdogManager.kt` | Service health monitoring |
| **Boot Receiver** | `receiver/BootReceiver.kt` | Auto-start service on device boot |
| **Permission Management** | `permission/PermissionManager.kt` | Runtime permission handling |
| **Home Screen UI** | `ui/home/HomeScreen.kt` | Main UI with permission cards, tracking toggle |
| **Hilt DI Modules** | `di/*.kt` | Full dependency injection setup |

### 2.3 Existing Data Models

**LocationEntity** (already matches PRD requirements):
```kotlin
@Entity(tableName = "locations")
data class LocationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val timestamp: Long,
    val altitude: Double? = null,
    val bearing: Float? = null,
    val speed: Float? = null,
    val provider: String? = null,
)
```

### 2.4 Existing API Endpoints (Implemented)

| Method | Endpoint | Status |
|--------|----------|--------|
| POST | `/api/locations` | ✅ Implemented |
| POST | `/api/locations/batch` | ✅ Implemented |

### 2.5 Needs Implementation (❌)

| Feature | Components Needed |
|---------|-------------------|
| **Secret Mode** | DataStore flag, notification variants, hidden activation gesture |
| **Map Display** | Google Maps SDK, MapScreen, MapViewModel |
| **Group Members** | Device entity, DeviceRepository, group fetching API |
| **Proximity Alerts** | ProximityAlert entity, AlertRepository, distance calculation |
| **Location History UI** | HistoryScreen, polyline rendering, date filter |
| **Geofencing** | Android Geofencing API, GeofenceBroadcastReceiver |
| **Webhooks** | Webhook entity, backend webhook dispatcher |
| **Device Registration** | Registration flow, displayName/groupId settings |

### 2.6 Needs Extension (⚠️)

| Component | Current State | Extension Needed |
|-----------|---------------|------------------|
| **LocationEntity** | Basic fields | Add `isSynced`, `syncedAt` for sync tracking |
| **PreferencesRepository** | Basic prefs | Add `secret_mode_enabled`, `displayName`, `groupId` |
| **Notification** | Standard format | Add secret mode variant (discreet) |
| **SecureStorage** | deviceId, apiKey | Add displayName, groupId storage |

---

## 3. Goals and Success Metrics

### 3.1 Business Goals

| Goal | Description |
|------|-------------|
| **G1** | Deliver all 6 MVP features in working condition |
| **G2** | Achieve 99% uptime for location tracking during active use |
| **G3** | Replace need for any commercial tracking application |

### 3.2 Key Performance Indicators

| KPI | Target | Measurement Method |
|-----|--------|-------------------|
| Location capture rate | >95% | Successful captures / expected captures |
| Alert delivery rate | >99% | Alerts delivered / alerts triggered |
| App crash rate | <1% | Crash-free sessions |
| Background service uptime | >99% | Time running / time expected |
| API response time | <500ms | Average backend response time |
| Battery efficiency | <10% daily drain | Additional drain from tracking |

---

## 4. User Stories

### 4.1 Epic: Secret Mode

| ID | User Story | Priority |
|----|------------|----------|
| US-1.1 | As a user, I want to enable secret mode so that the app is minimally visible on my device | Must Have |
| US-1.2 | As a user, I want the foreground notification to be discreet so that others don't notice the tracking | Must Have |
| US-1.3 | As a user, I want secret mode activation to be hidden so that only I know how to enable it | Should Have |

### 4.2 Epic: Map & Group Display

| ID | User Story | Priority |
|----|------------|----------|
| US-2.1 | As a user, I want to see my current location on a map so that I can orient myself | Must Have |
| US-2.2 | As a user, I want to see all group members' locations on the map so that I know where everyone is | Must Have |
| US-2.3 | As a user, I want location markers to update periodically so that I see near real-time positions | Must Have |
| US-2.4 | As a user, I want to see member names on their markers so that I can identify who is where | Should Have |

### 4.3 Epic: Proximity Alerts

| ID | User Story | Priority |
|----|------------|----------|
| US-3.1 | As a user, I want to create a proximity alert for another user so that I'm notified when they're nearby | Must Have |
| US-3.2 | As a user, I want to set the alert radius so that I control the trigger distance | Must Have |
| US-3.3 | As a user, I want to choose alert direction (enter/exit/both) so that I get relevant notifications | Should Have |
| US-3.4 | As a user, I want alerts to debounce so that I don't get repeated notifications | Must Have |

### 4.4 Epic: Location History

| ID | User Story | Priority |
|----|------------|----------|
| US-4.1 | As a user, I want to view my location history on the map so that I can see where I've been | Must Have |
| US-4.2 | As a user, I want to filter history by date range so that I can focus on specific time periods | Must Have |
| US-4.3 | As a user, I want history displayed as a polyline so that I can see my travel path | Must Have |
| US-4.4 | As a user, I want to view other group members' history so that I can see where they've been | Should Have |

### 4.5 Epic: Geofencing with Webhooks

| ID | User Story | Priority |
|----|------------|----------|
| US-5.1 | As a user, I want to define geofence zones so that I get alerts for specific places | Must Have |
| US-5.2 | As a user, I want geofence events to trigger webhooks so that I can automate actions | Must Have |
| US-5.3 | As a user, I want to configure webhook URLs securely so that my automations are protected | Must Have |
| US-5.4 | As a user, I want local notifications on geofence events so that I know when I enter/exit zones | Must Have |

### 4.6 Epic: Device Registration & Groups

| ID | User Story | Priority |
|----|------------|----------|
| US-6.1 | As a user, I want to register my device with a display name so that others can identify me | Must Have |
| US-6.2 | As a user, I want to join a group so that I can share my location with specific people | Must Have |
| US-6.3 | As a user, I want to see only group members' locations so that privacy is maintained | Must Have |

---

## 5. Functional Requirements

### Feature 1: Secret Mode

**Objective:** Application remains fully functional but minimally visible to casual observers.

#### FR-1.1 Secret Mode Setting

| ID | Requirement | Priority |
|----|-------------|----------|
| FR-1.1.1 | System SHALL store `secret_mode_enabled: Boolean` in DataStore | Must Have |
| FR-1.1.2 | System SHALL provide a hidden activation mechanism (long-press on logo OR 5x tap on version) | Must Have |
| FR-1.1.3 | System SHALL NOT show any UI indication when secret mode is enabled | Must Have |

#### FR-1.2 Notification Behavior (Secret Mode)

| ID | Requirement | Priority |
|----|-------------|----------|
| FR-1.2.1 | Foreground notification SHALL use generic title (e.g., "Service running") | Must Have |
| FR-1.2.2 | Foreground notification SHALL use neutral icon (no GPS/location symbol) | Must Have |
| FR-1.2.3 | Foreground notification SHALL use `IMPORTANCE_MIN` or `IMPORTANCE_LOW` | Must Have |
| FR-1.2.4 | Foreground notification SHALL have no sound and no vibration | Must Have |

#### FR-1.3 UI Behavior (Secret Mode)

| ID | Requirement | Priority |
|----|-------------|----------|
| FR-1.3.1 | System SHALL NOT display "Location tracking is ON" toasts or messages | Must Have |
| FR-1.3.2 | System SHALL log errors only to internal debug UI, not as visible alerts | Must Have |
| FR-1.3.3 | System SHALL disable verbose logging in Logcat when secret mode is enabled | Must Have |
| FR-1.3.4 | System SHALL NOT log sensitive information (coordinates, device names) to Logcat | Must Have |

#### FR-1.4 Backend Impact

| ID | Requirement | Priority |
|----|-------------|----------|
| FR-1.4.1 | Secret mode SHALL be purely client-side; no special API required | Must Have |

#### Acceptance Criteria - Secret Mode

```gherkin
Feature: Secret Mode

Scenario: Enable secret mode via hidden gesture
  Given the app is open on the main screen
  When I long-press on the app logo for 3 seconds
  Then secret mode should be enabled
  And no visible confirmation should appear

Scenario: Discreet notification in secret mode
  Given secret mode is enabled
  And background tracking is active
  Then the foreground notification should display "Service running"
  And the notification icon should be a generic system icon
  And the notification should have no sound or vibration

Scenario: No location indicators in secret mode
  Given secret mode is enabled
  When the app tracks location
  Then no toast messages should appear
  And no status bar indicators should show GPS activity
```

---

### Feature 2: Real-Time Map & Group Display

**Objective:** Display current device location and all group members on an interactive map.

#### FR-2.1 Map Display

| ID | Requirement | Priority |
|----|-------------|----------|
| FR-2.1.1 | System SHALL integrate Google Maps SDK with Jetpack Compose | Must Have |
| FR-2.1.2 | System SHALL display the current device location with a distinctive marker | Must Have |
| FR-2.1.3 | System SHALL center map on current device location on initial load | Must Have |
| FR-2.1.4 | System SHALL support standard map interactions (pan, zoom, rotate) | Must Have |

#### FR-2.2 Group Member Display

| ID | Requirement | Priority |
|----|-------------|----------|
| FR-2.2.1 | System SHALL display markers for all devices in the same `groupId` | Must Have |
| FR-2.2.2 | Each marker SHALL display the device's `displayName` | Must Have |
| FR-2.2.3 | Markers SHALL be visually distinct from the current device marker | Should Have |
| FR-2.2.4 | Tapping a marker SHALL show additional info (name, last update time) | Should Have |

#### FR-2.3 Real-Time Updates

| ID | Requirement | Priority |
|----|-------------|----------|
| FR-2.3.1 | System SHALL poll for group members' locations every 10-30 seconds (configurable) | Must Have |
| FR-2.3.2 | System SHALL update markers on the map without requiring manual refresh | Must Have |
| FR-2.3.3 | System SHALL indicate last update time for each member | Should Have |
| FR-2.3.4 | System SHALL handle network failures gracefully with retry logic | Must Have |

#### Acceptance Criteria - Map & Group Display

```gherkin
Feature: Real-Time Map Display

Scenario: View group members on map
  Given I am logged in with deviceId "device-001" in groupId "family"
  And there are 2 other devices in groupId "family"
  When I open the map screen
  Then I should see my location marker
  And I should see 2 other markers for group members
  And each marker should display the member's display name

Scenario: Real-time location updates
  Given the map screen is open
  And polling interval is set to 15 seconds
  When 15 seconds elapse
  Then the system should fetch updated locations from the server
  And markers should update to reflect new positions
```

---

### Feature 3: Proximity Alerts

**Objective:** Alert user when approaching or leaving proximity of another group member.

#### FR-3.1 Proximity Alert Definition

| ID | Requirement | Priority |
|----|-------------|----------|
| FR-3.1.1 | System SHALL allow creating alerts with: ownerDeviceId, targetDeviceId, radiusMeters | Must Have |
| FR-3.1.2 | System SHALL support radius values from 50 to 10,000 meters | Must Have |
| FR-3.1.3 | System SHALL support direction types: "enter", "exit", "both" | Should Have |
| FR-3.1.4 | System SHALL store alerts on the server for sync across devices | Must Have |

#### FR-3.2 Proximity Calculation

| ID | Requirement | Priority |
|----|-------------|----------|
| FR-3.2.1 | Distance calculation SHALL be performed on the client | Must Have |
| FR-3.2.2 | System SHALL use Haversine formula or `Location.distanceTo()` | Must Have |
| FR-3.2.3 | Calculation SHALL occur during each location poll cycle | Must Have |

#### FR-3.3 Alert Triggering

| ID | Requirement | Priority |
|----|-------------|----------|
| FR-3.3.1 | System SHALL track `lastState` ("inside" or "outside") for each alert | Must Have |
| FR-3.3.2 | Alert SHALL trigger only on state transition (outside→inside or inside→outside) | Must Have |
| FR-3.3.3 | System SHALL display local notification when alert triggers | Must Have |
| FR-3.3.4 | System MAY send event to backend for audit logging | Should Have |

#### FR-3.4 Alert Management

| ID | Requirement | Priority |
|----|-------------|----------|
| FR-3.4.1 | System SHALL provide UI to create, view, edit, and delete proximity alerts | Must Have |
| FR-3.4.2 | System SHALL allow enabling/disabling alerts without deletion | Should Have |
| FR-3.4.3 | System SHALL sync alerts from server on app startup | Must Have |

#### Data Model: ProximityAlert

```kotlin
data class ProximityAlert(
    val id: String,
    val ownerDeviceId: String,
    val targetDeviceId: String,
    val radiusMeters: Int,
    val active: Boolean,
    val direction: AlertDirection, // ENTER, EXIT, BOTH
    val lastState: ProximityState, // INSIDE, OUTSIDE
    val createdAt: Instant,
    val updatedAt: Instant,
    val lastTriggeredAt: Instant?
)

enum class AlertDirection { ENTER, EXIT, BOTH }
enum class ProximityState { INSIDE, OUTSIDE }
```

#### Acceptance Criteria - Proximity Alerts

```gherkin
Feature: Proximity Alerts

Scenario: Create proximity alert
  Given I am on the Alerts screen
  When I tap "Create Alert"
  And I select target device "Dad's Phone"
  And I set radius to 500 meters
  And I select direction "enter"
  And I tap "Save"
  Then a proximity alert should be created
  And it should sync to the server

Scenario: Trigger proximity alert on approach
  Given I have an active proximity alert for "Dad's Phone" with radius 200m, direction "enter"
  And Dad's Phone is currently 500m away (state: OUTSIDE)
  When Dad's Phone moves to 150m away
  Then a notification should appear: "You are near Dad's Phone"
  And lastState should update to INSIDE
  And lastTriggeredAt should update

Scenario: No duplicate alert on repeated proximity
  Given I triggered a proximity alert and am still within radius
  When the next poll cycle occurs
  And I am still within radius
  Then no new notification should appear
```

---

### Feature 4: Location History

**Objective:** Store, sync, and visualize historical location data.

#### FR-4.1 Local Storage

| ID | Requirement | Priority |
|----|-------------|----------|
| FR-4.1.1 | System SHALL store all location points in Room database | Must Have |
| FR-4.1.2 | Each location record SHALL include: latitude, longitude, timestamp, accuracy, speed | Must Have |
| FR-4.1.3 | System SHALL handle offline storage and sync when connectivity returns | Must Have |

#### FR-4.2 Server Synchronization

| ID | Requirement | Priority |
|----|-------------|----------|
| FR-4.2.1 | System SHALL sync locations to server via `/api/locations/batch` | Must Have |
| FR-4.2.2 | System SHALL mark synced records to avoid duplicate uploads | Must Have |
| FR-4.2.3 | Batch size SHALL be configurable (default: 100 records) | Should Have |

#### FR-4.3 History Viewing

| ID | Requirement | Priority |
|----|-------------|----------|
| FR-4.3.1 | System SHALL provide date/time filter (today, yesterday, last 7 days, custom range) | Must Have |
| FR-4.3.2 | System SHALL display history as polyline on map | Must Have |
| FR-4.3.3 | System SHALL support viewing own device history | Must Have |
| FR-4.3.4 | System SHALL support viewing other group members' history (from server) | Should Have |

#### FR-4.4 Performance Optimization

| ID | Requirement | Priority |
|----|-------------|----------|
| FR-4.4.1 | System SHALL downsample points for display (target: 200-500 points visible) | Must Have |
| FR-4.4.2 | Downsampling algorithm SHALL preserve path accuracy at current zoom level | Must Have |
| FR-4.4.3 | System SHALL support server-side downsampling via `simplify=true` parameter | Should Have |

#### FR-4.5 Data Retention

| ID | Requirement | Priority |
|----|-------------|----------|
| FR-4.5.1 | Local retention period SHALL be configurable (default: 30 days) | Should Have |
| FR-4.5.2 | System SHALL automatically purge local data older than retention period | Should Have |
| FR-4.5.3 | Server retention SHALL be configurable independently | Should Have |

#### Data Model: LocationRecord

```kotlin
data class LocationRecord(
    val id: String,
    val deviceId: String,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val speed: Float?,
    val bearing: Float?,
    val altitude: Double?,
    val timestamp: Instant,
    val syncedAt: Instant?,
    val isSynced: Boolean
)
```

#### Acceptance Criteria - Location History

```gherkin
Feature: Location History

Scenario: View today's location history
  Given I have recorded 1000 location points today
  When I open History and select "Today"
  Then a polyline should appear on the map showing my path
  And the polyline should have approximately 200-500 visible points (downsampled)

Scenario: View group member's history
  Given "Mom's Phone" is in my group
  And Mom's Phone has location history on the server
  When I select "Mom's Phone" and filter to "Yesterday"
  Then a polyline should appear showing Mom's path for yesterday

Scenario: Offline location recording
  Given the device has no network connectivity
  When location is recorded
  Then it should be stored in local Room database
  And isSynced should be false
  When network connectivity returns
  Then locations should sync to server via batch API
  And isSynced should update to true
```

---

### Feature 5: Geofencing with Webhooks

**Objective:** Define place-based alerts that trigger local notifications and webhook automations.

#### FR-5.1 Geofence Definition

| ID | Requirement | Priority |
|----|-------------|----------|
| FR-5.1.1 | System SHALL allow creating geofences with: name, latitude, longitude, radiusMeters | Must Have |
| FR-5.1.2 | System SHALL support transition types: ENTER, EXIT, DWELL | Must Have |
| FR-5.1.3 | System SHALL register geofences with Android Geofencing API | Must Have |
| FR-5.1.4 | System SHALL store geofences on server for sync and management | Must Have |

#### FR-5.2 Geofence Events

| ID | Requirement | Priority |
|----|-------------|----------|
| FR-5.2.1 | System SHALL receive geofence events via BroadcastReceiver | Must Have |
| FR-5.2.2 | System SHALL display local notification on geofence event | Must Have |
| FR-5.2.3 | System SHALL send event to backend: `POST /api/geofence-events` | Must Have |

#### FR-5.3 Webhook Integration

| ID | Requirement | Priority |
|----|-------------|----------|
| FR-5.3.1 | System SHALL support linking geofences to webhooks | Must Have |
| FR-5.3.2 | Webhooks SHALL be defined with: targetUrl, secret (for HMAC) | Must Have |
| FR-5.3.3 | Backend SHALL send HTTP POST to webhook URL on geofence event | Must Have |
| FR-5.3.4 | Webhook payload SHALL include: deviceId, geofenceId, eventType, timestamp, location | Must Have |
| FR-5.3.5 | Webhook request SHALL include HMAC signature in `X-Signature` header | Must Have |

#### FR-5.4 Webhook Management

| ID | Requirement | Priority |
|----|-------------|----------|
| FR-5.4.1 | System SHALL provide CRUD operations for webhooks | Must Have |
| FR-5.4.2 | System SHALL validate webhook URL format | Should Have |
| FR-5.4.3 | System SHALL support enabling/disabling webhooks | Should Have |

#### Data Model: Geofence (PlaceAlert)

```kotlin
data class Geofence(
    val id: String,
    val deviceId: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Int,
    val transitionTypes: Set<TransitionType>, // ENTER, EXIT, DWELL
    val webhookId: String?,
    val active: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant
)

enum class TransitionType { ENTER, EXIT, DWELL }
```

#### Data Model: Webhook

```kotlin
data class Webhook(
    val id: String,
    val ownerDeviceId: String,
    val name: String,
    val targetUrl: String,
    val secret: String, // For HMAC signing
    val enabled: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant
)
```

#### Data Model: GeofenceEvent

```kotlin
data class GeofenceEvent(
    val id: String,
    val deviceId: String,
    val geofenceId: String,
    val eventType: TransitionType,
    val timestamp: Instant,
    val latitude: Double,
    val longitude: Double,
    val webhookDelivered: Boolean,
    val webhookResponseCode: Int?
)
```

#### Webhook Payload Example

```json
{
  "event": "geofence_trigger",
  "deviceId": "device-001",
  "deviceName": "Martin's Phone",
  "geofenceId": "geo-home-001",
  "geofenceName": "Home",
  "eventType": "ENTER",
  "timestamp": "2025-11-25T18:30:00Z",
  "location": {
    "latitude": 48.1486,
    "longitude": 17.1077
  }
}
```

#### Acceptance Criteria - Geofencing with Webhooks

```gherkin
Feature: Geofencing with Webhooks

Scenario: Create geofence with webhook
  Given I am on the Geofences screen
  When I tap "Create Geofence"
  And I set name to "Home"
  And I set location to my home coordinates
  And I set radius to 100 meters
  And I select transition types "ENTER" and "EXIT"
  And I link webhook "Home Assistant"
  And I tap "Save"
  Then the geofence should be registered with Android Geofencing API
  And the geofence should sync to the server

Scenario: Geofence triggers webhook
  Given I have a geofence "Office" with webhook "n8n Workflow"
  And the webhook URL is "https://n8n.example.com/webhook/office"
  When I enter the "Office" geofence
  Then a local notification should appear: "Entered Office"
  And the app should send POST to /api/geofence-events
  And the backend should POST to n8n webhook URL
  And the request should include X-Signature header with HMAC

Scenario: Webhook HMAC verification
  Given webhook secret is "my-secret-key"
  When backend sends webhook
  Then X-Signature header should contain HMAC-SHA256 of payload using secret
```

---

### Feature 6: Device Registration & Groups

**Objective:** Enable devices to register and form groups for location sharing.

#### FR-6.1 Device Registration

| ID | Requirement | Priority |
|----|-------------|----------|
| FR-6.1.1 | System SHALL generate unique deviceId (UUID) on first launch | Must Have |
| FR-6.1.2 | System SHALL allow setting displayName for the device | Must Have |
| FR-6.1.3 | System SHALL register device with server via `POST /api/devices/register` | Must Have |
| FR-6.1.4 | Registration SHALL include: deviceId, displayName, groupId, platform | Must Have |

#### FR-6.2 Group Management

| ID | Requirement | Priority |
|----|-------------|----------|
| FR-6.2.1 | System SHALL allow setting groupId (e.g., "family", "team-alpha") | Must Have |
| FR-6.2.2 | Devices with same groupId SHALL see each other's locations | Must Have |
| FR-6.2.3 | System SHALL support changing groupId | Should Have |

#### FR-6.3 Device Discovery

| ID | Requirement | Priority |
|----|-------------|----------|
| FR-6.3.1 | System SHALL fetch group members via `GET /api/devices?groupId={id}` | Must Have |
| FR-6.3.2 | Response SHALL include: deviceId, displayName, lastLocation (optional) | Must Have |

#### Data Model: Device

```kotlin
data class Device(
    val deviceId: String,
    val displayName: String,
    val groupId: String,
    val platform: String, // "android"
    val fcmToken: String?,
    val lastLocation: Location?,
    val lastSeenAt: Instant?,
    val createdAt: Instant,
    val updatedAt: Instant
)
```

#### Acceptance Criteria - Device Registration

```gherkin
Feature: Device Registration

Scenario: First-time device registration
  Given I am launching the app for the first time
  When I enter display name "Martin's Phone"
  And I enter group ID "family"
  And I tap "Register"
  Then a unique deviceId should be generated
  And the device should be registered with the server
  And I should see the map screen

Scenario: View group members
  Given I am registered in group "family"
  And there are 3 devices in group "family"
  When I fetch group members
  Then I should receive a list of 3 devices
  And each device should have deviceId and displayName
```

---

## 6. API Specifications

### 6.1 Base Configuration

| Property | Value |
|----------|-------|
| Base URL | `https://{user-server}/api` |
| Protocol | HTTPS (required) |
| Authentication | API Key per device (header: `X-API-Key`) |
| Content-Type | `application/json` |

### 6.2 Endpoints Overview

| Category | Method | Endpoint | Description |
|----------|--------|----------|-------------|
| **Locations** | POST | `/locations` | Submit single location |
| | POST | `/locations/batch` | Submit batch of locations |
| **Devices** | POST | `/devices/register` | Register/update device |
| | GET | `/devices` | List devices by group |
| | GET | `/devices/{deviceId}/location` | Get device's last location |
| | GET | `/devices/{deviceId}/locations` | Get device's location history |
| **Proximity Alerts** | POST | `/proximity-alerts` | Create alert |
| | GET | `/proximity-alerts` | List alerts by owner |
| | PUT | `/proximity-alerts/{id}` | Update alert |
| | DELETE | `/proximity-alerts/{id}` | Delete alert |
| | POST | `/proximity-alerts/{id}/events` | Log alert trigger (optional) |
| **Geofences** | POST | `/geofences` | Create geofence |
| | GET | `/geofences` | List geofences by device |
| | PUT | `/geofences/{id}` | Update geofence |
| | DELETE | `/geofences/{id}` | Delete geofence |
| **Webhooks** | POST | `/webhooks` | Create webhook |
| | GET | `/webhooks` | List webhooks by device |
| | PUT | `/webhooks/{id}` | Update webhook |
| | DELETE | `/webhooks/{id}` | Delete webhook |
| **Geofence Events** | POST | `/geofence-events` | Report geofence trigger |

### 6.3 Detailed API Specifications

#### POST /api/devices/register

**Request:**
```json
{
  "deviceId": "uuid-string",
  "displayName": "Martin's Phone",
  "groupId": "family",
  "platform": "android",
  "fcmToken": "optional-fcm-token"
}
```

**Response (200):**
```json
{
  "deviceId": "uuid-string",
  "displayName": "Martin's Phone",
  "groupId": "family",
  "createdAt": "2025-11-25T10:00:00Z",
  "updatedAt": "2025-11-25T10:00:00Z"
}
```

#### GET /api/devices?groupId={groupId}

**Response (200):**
```json
{
  "devices": [
    {
      "deviceId": "device-001",
      "displayName": "Martin's Phone",
      "lastLocation": {
        "latitude": 48.1486,
        "longitude": 17.1077,
        "timestamp": "2025-11-25T18:30:00Z"
      },
      "lastSeenAt": "2025-11-25T18:30:00Z"
    },
    {
      "deviceId": "device-002",
      "displayName": "Partner's Phone",
      "lastLocation": null,
      "lastSeenAt": "2025-11-25T18:25:00Z"
    }
  ]
}
```

#### POST /api/locations/batch

**Request:**
```json
{
  "deviceId": "device-001",
  "locations": [
    {
      "latitude": 48.1486,
      "longitude": 17.1077,
      "accuracy": 10.5,
      "speed": 0.0,
      "bearing": 180.0,
      "altitude": 150.0,
      "timestamp": "2025-11-25T18:30:00Z"
    }
  ]
}
```

**Response (201):**
```json
{
  "accepted": 50,
  "rejected": 0
}
```

#### GET /api/devices/{deviceId}/locations

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| from | ISO8601 | Yes | Start timestamp |
| to | ISO8601 | Yes | End timestamp |
| page | int | No | Page number (default: 1) |
| pageSize | int | No | Results per page (default: 100, max: 1000) |
| simplify | bool | No | Enable server-side downsampling |

**Response (200):**
```json
{
  "deviceId": "device-001",
  "locations": [...],
  "pagination": {
    "page": 1,
    "pageSize": 100,
    "totalPages": 5,
    "totalRecords": 450
  }
}
```

#### POST /api/proximity-alerts

**Request:**
```json
{
  "ownerDeviceId": "device-001",
  "targetDeviceId": "device-002",
  "radiusMeters": 200,
  "direction": "enter",
  "active": true
}
```

**Response (201):**
```json
{
  "id": "alert-uuid",
  "ownerDeviceId": "device-001",
  "targetDeviceId": "device-002",
  "radiusMeters": 200,
  "direction": "enter",
  "active": true,
  "lastState": "outside",
  "createdAt": "2025-11-25T10:00:00Z"
}
```

#### POST /api/geofences

**Request:**
```json
{
  "deviceId": "device-001",
  "name": "Home",
  "latitude": 48.1486,
  "longitude": 17.1077,
  "radiusMeters": 100,
  "transitionTypes": ["ENTER", "EXIT"],
  "webhookId": "webhook-001",
  "active": true
}
```

#### POST /api/geofence-events

**Request:**
```json
{
  "deviceId": "device-001",
  "geofenceId": "geo-001",
  "eventType": "ENTER",
  "timestamp": "2025-11-25T18:30:00Z",
  "location": {
    "latitude": 48.1486,
    "longitude": 17.1077
  }
}
```

**Response (201):**
```json
{
  "id": "event-uuid",
  "webhookTriggered": true,
  "webhookResponseCode": 200
}
```

---

## 7. Data Models

### 7.1 Entity Relationship Diagram

```
┌─────────────┐       ┌─────────────────┐
│   Device    │───────│  LocationRecord │
│             │ 1   * │                 │
└─────────────┘       └─────────────────┘
      │
      │ 1
      │
      ├───────────────┐
      │               │
      ▼ *             ▼ *
┌─────────────┐ ┌─────────────┐
│ ProximityAlert│ │  Geofence   │
│             │ │             │
└─────────────┘ └─────────────┘
                      │
                      │ *
                      ▼ 1
                ┌─────────────┐
                │   Webhook   │
                │             │
                └─────────────┘
                      │
                      │ 1
                      ▼ *
                ┌─────────────┐
                │GeofenceEvent│
                │             │
                └─────────────┘
```

### 7.2 Database Schema (Server - PostgreSQL)

```sql
-- Devices
CREATE TABLE devices (
    device_id VARCHAR(36) PRIMARY KEY,
    display_name VARCHAR(100) NOT NULL,
    group_id VARCHAR(50) NOT NULL,
    platform VARCHAR(20) NOT NULL,
    fcm_token VARCHAR(255),
    last_latitude DOUBLE PRECISION,
    last_longitude DOUBLE PRECISION,
    last_seen_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_devices_group ON devices(group_id);

-- Location Records
CREATE TABLE location_records (
    id VARCHAR(36) PRIMARY KEY,
    device_id VARCHAR(36) REFERENCES devices(device_id),
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    accuracy REAL,
    speed REAL,
    bearing REAL,
    altitude DOUBLE PRECISION,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_locations_device_time ON location_records(device_id, timestamp);

-- Proximity Alerts
CREATE TABLE proximity_alerts (
    id VARCHAR(36) PRIMARY KEY,
    owner_device_id VARCHAR(36) REFERENCES devices(device_id),
    target_device_id VARCHAR(36) REFERENCES devices(device_id),
    radius_meters INTEGER NOT NULL,
    direction VARCHAR(10) NOT NULL, -- 'enter', 'exit', 'both'
    active BOOLEAN DEFAULT TRUE,
    last_state VARCHAR(10) DEFAULT 'outside',
    last_triggered_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Geofences
CREATE TABLE geofences (
    id VARCHAR(36) PRIMARY KEY,
    device_id VARCHAR(36) REFERENCES devices(device_id),
    name VARCHAR(100) NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    radius_meters INTEGER NOT NULL,
    transition_types VARCHAR(50) NOT NULL, -- comma-separated: 'ENTER,EXIT,DWELL'
    webhook_id VARCHAR(36),
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Webhooks
CREATE TABLE webhooks (
    id VARCHAR(36) PRIMARY KEY,
    owner_device_id VARCHAR(36) REFERENCES devices(device_id),
    name VARCHAR(100) NOT NULL,
    target_url VARCHAR(500) NOT NULL,
    secret VARCHAR(100) NOT NULL,
    enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Geofence Events
CREATE TABLE geofence_events (
    id VARCHAR(36) PRIMARY KEY,
    device_id VARCHAR(36) REFERENCES devices(device_id),
    geofence_id VARCHAR(36) REFERENCES geofences(id),
    event_type VARCHAR(10) NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    webhook_delivered BOOLEAN DEFAULT FALSE,
    webhook_response_code INTEGER,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);
```

---

## 8. Non-Functional Requirements

### 8.1 Performance Requirements

| ID | Requirement | Target |
|----|-------------|--------|
| NFR-P1 | API response time | <500ms (95th percentile) |
| NFR-P2 | Map rendering with 500 markers | <2 seconds |
| NFR-P3 | Polyline rendering (500 points) | <1 second |
| NFR-P4 | Background location capture | Every configured interval ±10% |
| NFR-P5 | Batch sync throughput | 100 locations per request |

### 8.2 Reliability Requirements

| ID | Requirement | Target |
|----|-------------|--------|
| NFR-R1 | Background service uptime | >99% |
| NFR-R2 | Data loss on crash | Zero (persist before crash) |
| NFR-R3 | Offline operation | Full local functionality |
| NFR-R4 | Sync recovery | Automatic on connectivity restore |

### 8.3 Battery Efficiency

| ID | Requirement | Target |
|----|-------------|--------|
| NFR-B1 | Additional battery drain (standard mode) | <10% per day |
| NFR-B2 | Additional battery drain (frequent mode) | <15% per day |
| NFR-B3 | Idle battery impact | <1% per hour |

### 8.4 Scalability

| ID | Requirement | Target |
|----|-------------|--------|
| NFR-S1 | Devices per group | Up to 20 |
| NFR-S2 | Total devices per server | Up to 50 |
| NFR-S3 | Location records per device/day | Up to 10,000 |
| NFR-S4 | Historical data retention | Configurable (30-365 days) |

---

## 9. Technical Architecture

### 9.1 Client Architecture (Android)

```
┌─────────────────────────────────────────────────────────────┐
│                        UI Layer                              │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐           │
│  │  MapScreen  │ │HistoryScreen│ │AlertsScreen │  ...      │
│  └─────────────┘ └─────────────┘ └─────────────┘           │
├─────────────────────────────────────────────────────────────┤
│                     ViewModel Layer                          │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐           │
│  │MapViewModel │ │HistoryVM   │ │ AlertsVM    │  ...      │
│  └─────────────┘ └─────────────┘ └─────────────┘           │
├─────────────────────────────────────────────────────────────┤
│                     Use Case Layer                           │
│  ┌───────────────────┐ ┌───────────────────┐               │
│  │ GetGroupLocations │ │ CreateProximityAlert│  ...        │
│  └───────────────────┘ └───────────────────┘               │
├─────────────────────────────────────────────────────────────┤
│                    Repository Layer                          │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐           │
│  │LocationRepo │ │ DeviceRepo  │ │  AlertRepo  │  ...      │
│  └─────────────┘ └─────────────┘ └─────────────┘           │
├─────────────────────────────────────────────────────────────┤
│                     Data Sources                             │
│  ┌─────────────────┐       ┌─────────────────┐             │
│  │   Local (Room)   │       │  Remote (API)   │             │
│  │   DataStore      │       │  Ktor Client    │             │
│  └─────────────────┘       └─────────────────┘             │
├─────────────────────────────────────────────────────────────┤
│                  Background Services                         │
│  ┌─────────────────┐ ┌─────────────────┐                   │
│  │LocationService  │ │GeofenceBroadcast│                   │
│  │(Foreground)     │ │Receiver         │                   │
│  └─────────────────┘ └─────────────────┘                   │
└─────────────────────────────────────────────────────────────┘
```

### 9.2 Server Architecture (Rust)

```
┌─────────────────────────────────────────────────────────────┐
│                      API Layer                               │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐           │
│  │ /devices    │ │ /locations  │ │ /geofences  │  ...      │
│  └─────────────┘ └─────────────┘ └─────────────┘           │
├─────────────────────────────────────────────────────────────┤
│                    Service Layer                             │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐           │
│  │DeviceService│ │LocationSvc  │ │WebhookSvc   │           │
│  └─────────────┘ └─────────────┘ └─────────────┘           │
├─────────────────────────────────────────────────────────────┤
│                   Repository Layer                           │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐           │
│  │DeviceRepo   │ │LocationRepo │ │GeofenceRepo │           │
│  └─────────────┘ └─────────────┘ └─────────────┘           │
├─────────────────────────────────────────────────────────────┤
│                      Database                                │
│                   PostgreSQL / SQLite                        │
└─────────────────────────────────────────────────────────────┘
        │
        │ Webhook Dispatcher
        ▼
┌─────────────────────────────────────────────────────────────┐
│              External Systems                                │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐           │
│  │Home Assistant│ │    n8n     │ │Custom Server│           │
│  └─────────────┘ └─────────────┘ └─────────────┘           │
└─────────────────────────────────────────────────────────────┘
```

---

## 10. Security Requirements

### 10.1 Authentication & Authorization

| ID | Requirement |
|----|-------------|
| SEC-1 | Each device SHALL have unique API key |
| SEC-2 | API key SHALL be transmitted via `X-API-Key` header |
| SEC-3 | All API requests SHALL be authenticated |
| SEC-4 | Devices SHALL only access data within their group |

### 10.2 Data Protection

| ID | Requirement |
|----|-------------|
| SEC-5 | All API communication SHALL use HTTPS |
| SEC-6 | Webhook payloads SHALL be signed with HMAC-SHA256 |
| SEC-7 | Webhook secrets SHALL be stored securely (not plaintext in logs) |
| SEC-8 | Location data SHALL not be logged with identifiable information |

### 10.3 Secret Mode Security

| ID | Requirement |
|----|-------------|
| SEC-9 | Secret mode activation mechanism SHALL not be documented in visible UI |
| SEC-10 | No sensitive data SHALL appear in Android Logcat in secret mode |

---

## 11. Out of Scope

The following features are explicitly **NOT** included in MVP:

| Feature | Planned Phase |
|---------|---------------|
| iOS application | Phase 2+ |
| Web dashboard | Phase 2+ |
| Panic/SOS button | Phase 2 |
| Temporary live sharing links | Phase 2 |
| Speed and movement alerts | Phase 2 |
| Battery monitoring and alerts | Phase 2 |
| Named zones with automatic tagging | Phase 2 |
| Role-based access control | Phase 3 |
| Data export (CSV, GPX, KML) | Phase 2 |
| Multi-profile tracking schedules | Phase 3 |
| Incident/note tagging | Phase 3 |
| Advanced analytics (heatmaps) | Phase 3 |

---

## 12. Dependencies and Risks

### 12.1 External Dependencies

| Dependency | Risk Level | Mitigation |
|------------|------------|------------|
| Google Maps SDK | Medium | Monitor API costs; consider OSM/Mapbox fallback |
| Google Play Services (Geofencing) | Medium | Implement manual fallback for geofencing |
| Network connectivity | Low | Robust offline storage and sync |

### 12.2 Technical Risks

| Risk | Impact | Likelihood | Mitigation |
|------|--------|------------|------------|
| Android background restrictions | High | Medium | Proper foreground service, WorkManager, battery optimization exemption |
| Geofencing unreliable | Medium | Medium | Combine with periodic location checks |
| Battery drain concerns | Medium | Medium | Configurable intervals, power-efficient location providers |

---

## 13. Release Criteria

### 13.1 MVP Release Checklist

| ID | Criterion | Status |
|----|-----------|--------|
| RC-1 | All 6 core features functional | ⬜ |
| RC-2 | Stable background tracking for 24+ hours | ⬜ |
| RC-3 | All acceptance criteria passing | ⬜ |
| RC-4 | API response times <500ms | ⬜ |
| RC-5 | Battery drain <10% per day | ⬜ |
| RC-6 | 3+ devices operating simultaneously | ⬜ |
| RC-7 | Webhook integration tested with Home Assistant | ⬜ |
| RC-8 | Secret mode fully functional | ⬜ |
| RC-9 | No critical or high severity bugs | ⬜ |
| RC-10 | Basic documentation complete | ⬜ |

---

## Document History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2025-11-25 | Martin | Initial PRD based on Product Brief |
| 1.1 | 2025-11-25 | Martin | Added Section 2: Current Implementation Status; renumbered all sections; updated to reflect existing codebase analysis |

---

_This PRD is derived from the Product Brief: Phone Manager (2025-11-25) and detailed technical specifications provided by the stakeholder._

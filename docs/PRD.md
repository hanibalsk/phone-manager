# Product Requirements Document (PRD)

**Project Name:** Phone Manager
**Version:** 1.0
**Date:** 2025-10-15
**Author:** Martin
**Platform:** Android
**Project Type:** Mobile App (Native Android)

---

## Executive Summary

Phone Manager is a lightweight Android application that collects device location data in the background and securely transmits it to an n8n webhook endpoint. The app features AES-encrypted JSON payloads, battery-optimized location tracking, and a simple permission management UI.

---

## Product Vision

Create a reliable, battery-efficient location tracking solution that runs seamlessly in the background while providing users with transparent permission controls and secure data transmission to n8n automation workflows.

---

## Target Users

- Users who need automated location tracking
- Individuals using n8n for workflow automation
- Users requiring secure, encrypted location data transmission

---

## Functional Requirements (FRs)

### FR-1: Location Collection
- **FR-1.1**: Collect device GPS location data periodically (every 5-15 minutes, user-configurable)
- **FR-1.2**: Support background location collection when app is not in foreground
- **FR-1.3**: Continue location tracking across device restarts (persistent service)
- **FR-1.4**: Handle location permission states (granted, denied, always, while-in-use)

### FR-2: Data Encryption and Transmission
- **FR-2.1**: Encrypt location data using AES-256 encryption before transmission
- **FR-2.2**: Format encrypted data as JSON payload
- **FR-2.3**: Send encrypted JSON to configured n8n webhook endpoint via HTTPS POST
- **FR-2.4**: Implement retry logic for failed transmissions (exponential backoff)
- **FR-2.5**: Queue location data when network unavailable, sync when connection restored

### FR-3: Permission Management UI
- **FR-3.1**: Simple home screen showing tracking status (Active/Inactive)
- **FR-3.2**: Toggle to enable/disable location tracking
- **FR-3.3**: Button to request location permissions (Android 13+ background location flow)
- **FR-3.4**: Display current permission status (Not Granted, Granted, Background Access)
- **FR-3.5**: Settings screen to configure tracking interval (5, 10, 15 minutes)
- **FR-3.6**: Display n8n webhook endpoint configuration field

### FR-4: n8n Integration
- **FR-4.1**: Configurable n8n webhook URL in app settings
- **FR-4.2**: Include encryption key configuration for n8n decryption
- **FR-4.3**: Send location payload structure compatible with n8n JSON parser
- **FR-4.4**: Include device identifier in payload for n8n workflow routing

### FR-5: Battery Optimization
- **FR-5.1**: Use Android WorkManager for periodic location collection (not continuous GPS)
- **FR-5.2**: Implement Doze mode compatibility (wake device only when necessary)
- **FR-5.3**: Use fused location provider for battery-efficient location access
- **FR-5.4**: Batch location updates when network unavailable to reduce wake locks

---

## Non-Functional Requirements (NFRs)

### NFR-1: Performance
- **NFR-1.1**: Location data transmission latency <5 seconds under normal network conditions
- **NFR-1.2**: Background service memory footprint <50MB
- **NFR-1.3**: App startup time <2 seconds

### NFR-2: Battery Efficiency
- **NFR-2.1**: Battery drain <2% per hour during active tracking
- **NFR-2.2**: Zero battery drain when tracking disabled
- **NFR-2.3**: Optimize location accuracy vs. battery trade-off (balanced power mode)

### NFR-3: Security
- **NFR-3.1**: AES-256-CBC encryption for all location data
- **NFR-3.2**: Encryption keys stored in Android KeyStore (hardware-backed when available)
- **NFR-3.3**: HTTPS-only communication with n8n webhook (no HTTP fallback)
- **NFR-3.4**: No plaintext location data stored on device
- **NFR-3.5**: Certificate pinning for n8n webhook endpoint (optional, recommended)

### NFR-4: Reliability
- **NFR-4.1**: 99% successful location transmission rate under normal conditions
- **NFR-4.2**: Graceful degradation when network unavailable (queue and retry)
- **NFR-4.3**: Service restart after crash or device reboot

### NFR-5: Compatibility
- **NFR-5.1**: Support Android 8.0 (API 26) and above
- **NFR-5.2**: Handle Android 13+ granular location permission model
- **NFR-5.3**: Doze mode and App Standby compatibility

### NFR-6: Usability
- **NFR-6.1**: Simple, single-screen UI (minimal complexity)
- **NFR-6.2**: Clear permission request explanations (Google Play policy compliance)
- **NFR-6.3**: Persistent notification when tracking active (Android O+ requirement)

---

## Epics and User Stories

### Epic 1: Location Tracking Core

**Priority:** Critical
**Estimated Effort:** High

#### Stories:
1. **Story 1.1**: As a user, I want to enable location tracking so the app collects my position periodically
   - **Acceptance Criteria:**
     - User can toggle tracking on/off from main screen
     - Toggle state persists across app restarts
     - Background service starts when tracking enabled

2. **Story 1.2**: As a user, I want the app to request necessary permissions so I can grant location access
   - **Acceptance Criteria:**
     - App requests fine location permission on first launch
     - App requests background location permission (Android 10+)
     - Clear explanations shown before permission requests
     - Permission status displayed on main screen

3. **Story 1.3**: As a user, I want location collected periodically in the background so I don't need to keep the app open
   - **Acceptance Criteria:**
     - Location collected every 5-15 minutes (configurable)
     - Works when app is closed or device screen off
     - Service survives device reboot
     - Persistent notification shown when tracking active

### Epic 2: Secure Data Transmission

**Priority:** Critical
**Estimated Effort:** Medium

#### Stories:
1. **Story 2.1**: As a user, I want my location data encrypted so it's secure during transmission
   - **Acceptance Criteria:**
     - Location data encrypted with AES-256 before sending
     - Encryption key stored securely in Android KeyStore
     - No plaintext location data in logs or storage

2. **Story 2.2**: As a user, I want location data sent to my n8n webhook so I can process it in my workflows
   - **Acceptance Criteria:**
     - HTTPS POST to configured webhook URL
     - JSON payload structure: `{"encrypted_data": "...", "iv": "...", "device_id": "..."}`
     - Successful transmission confirmed (HTTP 200-299)

3. **Story 2.3**: As a user, I want failed transmissions retried automatically so no data is lost
   - **Acceptance Criteria:**
     - Exponential backoff retry logic (1s, 2s, 4s, 8s, 16s)
     - Queue location data when network unavailable
     - Sync queued data when network restored
     - Max queue size: 100 locations (discard oldest)

### Epic 3: Configuration and Settings

**Priority:** Medium
**Estimated Effort:** Low

#### Stories:
1. **Story 3.1**: As a user, I want to configure my n8n webhook URL so the app knows where to send data
   - **Acceptance Criteria:**
     - Settings screen with webhook URL input field
     - URL validation (must be HTTPS)
     - Test connection button (send test payload)

2. **Story 3.2**: As a user, I want to configure tracking interval so I can balance accuracy and battery life
   - **Acceptance Criteria:**
     - Settings screen with interval selector (5, 10, 15 minutes)
     - Interval change applied immediately
     - Current interval displayed on main screen

3. **Story 3.3**: As a user, I want to configure encryption key so n8n can decrypt my data
   - **Acceptance Criteria:**
     - Settings screen with encryption key input field
     - Key stored securely in Android KeyStore
     - Option to generate random key
     - Copy key to clipboard for n8n configuration

### Epic 4: Battery Optimization

**Priority:** High
**Estimated Effort:** Medium

#### Stories:
1. **Story 4.1**: As a user, I want minimal battery drain so the app doesn't impact my device usage
   - **Acceptance Criteria:**
     - Use WorkManager for periodic tasks (not continuous GPS)
     - Use fused location provider (battery-efficient)
     - Batch updates when network unavailable
     - <2% battery drain per hour

2. **Story 4.2**: As a user, I want the app to work with Doze mode so tracking continues in power-saving mode
   - **Acceptance Criteria:**
     - Doze mode whitelisting guidance
     - WorkManager respects Doze constraints
     - Wake device only when necessary

---

## Technical Constraints

1. **Platform**: Android 8.0 (API 26) minimum, target Android 14 (API 34)
2. **Language**: Kotlin (preferred for Android development)
3. **Architecture**: MVVM with Repository pattern
4. **Dependency Injection**: Hilt (Dagger)
5. **Background Work**: WorkManager API
6. **Location**: Google Play Services Location API (Fused Location Provider)
7. **Encryption**: Android Crypto API (AES/CBC/PKCS7Padding)
8. **Networking**: Retrofit 2 + OkHttp 3
9. **Storage**: EncryptedSharedPreferences, Room Database (for queue)

---

## External Integrations

### n8n Webhook Integration

**Endpoint**: User-configured HTTPS URL
**Method**: POST
**Content-Type**: application/json

**Payload Structure**:
```json
{
  "device_id": "unique-device-identifier",
  "timestamp": "2025-10-15T12:34:56.789Z",
  "encrypted_data": "base64-encoded-encrypted-json",
  "iv": "base64-encoded-initialization-vector",
  "encryption_algorithm": "AES-256-CBC"
}
```

**Encrypted Data Structure (before encryption)**:
```json
{
  "latitude": 37.7749,
  "longitude": -122.4194,
  "accuracy": 10.5,
  "altitude": 15.2,
  "bearing": 45.0,
  "speed": 5.5,
  "provider": "fused"
}
```

**n8n Decryption Requirements**:
- AES-256-CBC decryption node
- Base64 decode IV and encrypted data
- Shared encryption key (configured in both app and n8n)
- JSON parse decrypted payload

---

## Out of Scope (Phase 1)

- Location history visualization in app
- Multi-user support
- Cloud backup of location data
- Geofencing alerts
- Location sharing with other users
- iOS version
- Web dashboard

---

## Success Metrics

1. **Reliability**: 99%+ location transmission success rate
2. **Battery Efficiency**: <2% battery drain per hour during active tracking
3. **User Adoption**: Successful permission grant rate >80%
4. **Performance**: <5s transmission latency, <2s app startup
5. **Security**: Zero plaintext location data leaks (audit logs, storage, network)

---

## Assumptions and Dependencies

### Assumptions:
- User has stable internet connection (WiFi or cellular data)
- User has n8n instance accessible via HTTPS
- User understands basic encryption key management
- User grants necessary location permissions

### Dependencies:
- Google Play Services (Location API)
- n8n webhook endpoint availability
- Android KeyStore API availability (API 23+)

---

## Risks and Mitigations

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| User denies location permissions | High | Critical | Clear permission rationale, in-app guidance |
| Battery drain complaints | Medium | High | Aggressive WorkManager optimization, user-configurable intervals |
| Network unavailable for extended period | Medium | Medium | Queue up to 100 locations, sync when network restored |
| n8n webhook endpoint down | Medium | Medium | Retry logic, queue locations, user notification |
| Android version fragmentation | Low | Medium | Support API 26+, test on multiple Android versions |
| Encryption key loss | Low | High | Key backup guidance, key regeneration flow |

---

## Glossary

- **AES**: Advanced Encryption Standard (symmetric encryption)
- **WorkManager**: Android Jetpack library for deferrable background work
- **Fused Location Provider**: Google Play Services API combining GPS, WiFi, and cell tower data
- **Doze Mode**: Android power-saving feature restricting background activity
- **n8n**: Open-source workflow automation tool
- **KeyStore**: Android secure storage for cryptographic keys

---

## Approval

**Status**: Draft
**PRD Version**: 1.0
**Next Phase**: Solution Architecture

---

_This PRD was created as part of the BMAD solution architecture workflow._

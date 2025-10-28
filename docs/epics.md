# Phone Manager - Epic Breakdown

**Author:** Martin
**Date:** 2025-10-28
**Project Level:** 2
**Target Scale:** 17 stories across 5 epics

---

## Epic Overview

This project consists of 5 epics:
- **Epic 0**: Project Setup and Infrastructure (Enabler) - 6 stories
- **Epic 1**: Location Tracking Core - 3 stories
- **Epic 2**: Secure Data Transmission - 3 stories
- **Epic 3**: Configuration and Settings - 3 stories
- **Epic 4**: Battery Optimization - 2 stories

**Total Stories:** 17

---

## Epic 0: Project Setup and Infrastructure (Enabler)

**Priority:** Critical (Prerequisite)
**Estimated Effort:** Medium (1 week)
**Type:** Technical Enabler
**Status:** Planned

### Purpose
Establish the foundational architecture, build configuration, and infrastructure required for all subsequent feature development.

### Stories

#### Story 0.1: Create Android project structure with Kotlin and Jetpack Compose
**Status:** Planned
**Estimated Effort:** Medium

**Acceptance Criteria:**
- Android project created with Kotlin 1.9.22
- Jetpack Compose 1.6.0 configured with Material 3
- Gradle build scripts set up with Kotlin DSL
- Target SDK 34, minimum SDK 26
- Single Activity architecture with Compose Navigation
- App compiles and runs with empty home screen

#### Story 0.2: Configure Koin dependency injection
**Status:** Planned
**Estimated Effort:** Small

**Acceptance Criteria:**
- Koin 3.5.3 integrated
- DI modules created (AppModule, DataModule, DomainModule, ViewModelModule)
- Application class configured with Koin initialization
- Sample ViewModel injection working

#### Story 0.3: Set up Room database infrastructure
**Status:** Planned
**Estimated Effort:** Medium

**Acceptance Criteria:**
- Room 2.6.1 configured with KSP
- PhoneManagerDatabase created
- TransmissionQueueDao and entity defined
- Database migration strategy established
- Basic CRUD operations tested

#### Story 0.4: Configure WorkManager for background tasks
**Status:** Planned
**Estimated Effort:** Small

**Acceptance Criteria:**
- WorkManager 2.9.0 integrated
- WorkerFactory configured with Koin
- Sample periodic worker created and tested
- Doze mode constraints configured

#### Story 0.5: Implement base architecture patterns
**Status:** Planned
**Estimated Effort:** Medium

**Acceptance Criteria:**
- Repository pattern base classes/interfaces
- ViewModel base setup with StateFlow
- Navigation graph structure
- Data models package structure
- Use case pattern established

#### Story 0.6: Configure build variants and ProGuard
**Status:** Planned
**Estimated Effort:** Small

**Acceptance Criteria:**
- Debug and Release build types configured
- ProGuard rules defined for Koin, Retrofit, Room
- Signing configuration prepared (without committing keys)
- Build successfully generates minified release APK
- Timber logging configured (debug vs release trees)

---

## Epic 1: Location Tracking Core

**Priority:** Critical
**Estimated Effort:** High
**Status:** Planned

### Stories

#### Story 1.1: Enable location tracking toggle
**Status:** Planned
**Estimated Effort:** Medium

**User Story:**
As a user, I want to enable location tracking so the app collects my position periodically

**Acceptance Criteria:**
- User can toggle tracking on/off from main screen
- Toggle state persists across app restarts
- Background service starts when tracking enabled

#### Story 1.2: Request location permissions
**Status:** Planned
**Estimated Effort:** Medium

**User Story:**
As a user, I want the app to request necessary permissions so I can grant location access

**Acceptance Criteria:**
- App requests fine location permission on first launch
- App requests background location permission (Android 10+)
- Clear explanations shown before permission requests
- Permission status displayed on main screen

#### Story 1.3: Implement background location collection
**Status:** Planned
**Estimated Effort:** Large

**User Story:**
As a user, I want location collected periodically in the background so I don't need to keep the app open

**Acceptance Criteria:**
- Location collected every 5-15 minutes (configurable)
- Works when app is closed or device screen off
- Service survives device reboot
- Persistent notification shown when tracking active

---

## Epic 2: Secure Data Transmission

**Priority:** Critical
**Estimated Effort:** Medium
**Status:** Planned

### Stories

#### Story 2.1: Encrypt location data
**Status:** Planned
**Estimated Effort:** Medium

**User Story:**
As a user, I want my location data encrypted so it's secure during transmission

**Acceptance Criteria:**
- Location data encrypted with AES-256 before sending
- Encryption key stored securely in Android KeyStore
- No plaintext location data in logs or storage

#### Story 2.2: Send data to n8n webhook
**Status:** Planned
**Estimated Effort:** Medium

**User Story:**
As a user, I want location data sent to my n8n webhook so I can process it in my workflows

**Acceptance Criteria:**
- HTTPS POST to configured webhook URL
- JSON payload structure: `{"encrypted_data": "...", "iv": "...", "device_id": "..."}`
- Successful transmission confirmed (HTTP 200-299)

#### Story 2.3: Implement retry logic for failed transmissions
**Status:** Planned
**Estimated Effort:** Medium

**User Story:**
As a user, I want failed transmissions retried automatically so no data is lost

**Acceptance Criteria:**
- Exponential backoff retry logic (1s, 2s, 4s, 8s, 16s)
- Queue location data when network unavailable
- Sync queued data when network restored
- Max queue size: 100 locations (discard oldest)

---

## Epic 3: Configuration and Settings

**Priority:** Medium
**Estimated Effort:** Low
**Status:** Planned

### Stories

#### Story 3.1: Configure n8n webhook URL
**Status:** Planned
**Estimated Effort:** Small

**User Story:**
As a user, I want to configure my n8n webhook URL so the app knows where to send data

**Acceptance Criteria:**
- Settings screen with webhook URL input field
- URL validation (must be HTTPS)
- Test connection button (send test payload)

#### Story 3.2: Configure tracking interval
**Status:** Planned
**Estimated Effort:** Small

**User Story:**
As a user, I want to configure tracking interval so I can balance accuracy and battery life

**Acceptance Criteria:**
- Settings screen with interval selector (5, 10, 15 minutes)
- Interval change applied immediately
- Current interval displayed on main screen

#### Story 3.3: Configure encryption key
**Status:** Planned
**Estimated Effort:** Small

**User Story:**
As a user, I want to configure encryption key so n8n can decrypt my data

**Acceptance Criteria:**
- Settings screen with encryption key input field
- Key stored securely in Android KeyStore
- Option to generate random key
- Copy key to clipboard for n8n configuration

---

## Epic 4: Battery Optimization

**Priority:** High
**Estimated Effort:** Medium
**Status:** Planned

### Stories

#### Story 4.1: Minimize battery drain
**Status:** Planned
**Estimated Effort:** Medium

**User Story:**
As a user, I want minimal battery drain so the app doesn't impact my device usage

**Acceptance Criteria:**
- Use WorkManager for periodic tasks (not continuous GPS)
- Use fused location provider (battery-efficient)
- Batch updates when network unavailable
- <2% battery drain per hour

#### Story 4.2: Implement Doze mode compatibility
**Status:** Planned
**Estimated Effort:** Medium

**User Story:**
As a user, I want the app to work with Doze mode so tracking continues in power-saving mode

**Acceptance Criteria:**
- Doze mode whitelisting guidance
- WorkManager respects Doze constraints
- Wake device only when necessary

---

## Dependencies

- Epic 0 blocks all other epics (foundation requirement)
- Epic 1 must complete before Epic 2 (need location data before transmission)
- Epic 3 can be done in parallel with Epic 1-2
- Epic 4 should be done after Epic 1 (optimize after base implementation)

---

## Notes

- All stories follow the naming convention: `story-{epic}.{story}.md`
- Story status progression: Planned → Draft → In Progress → Ready for Review → Done
- Each story must have acceptance criteria and tasks/subtasks
- Dev notes should reference source documents (PRD, tech specs, architecture docs)

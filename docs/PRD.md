---
stepsCompleted: [1, 2, 3, 4, 7, 8, 9, 10, 11]
inputDocuments:
  - docs/product-brief-phone-manager-2025-11-25.md
  - docs/epics.md
  - /Users/martinjanci/.claude/plans/async-wondering-turtle.md
  - /Users/martinjanci/projects/github.com/hanibalsk/phone-manager-backend/docs/prd-unified-group-management.md
documentCounts:
  briefs: 1
  research: 0
  brainstorming: 0
  projectDocs: 35
workflowType: 'prd'
lastStep: 11
status: 'complete'
completedAt: '2025-12-18'
project_name: 'Phone Manager - Unified Group Management'
user_name: 'Partner'
date: '2025-12-18'
---

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

### Epic 0: Project Setup and Infrastructure (Enabler)

**Priority:** Critical (Prerequisite)
**Estimated Effort:** Medium (1 week)
**Type:** Technical Enabler

**Purpose:** Establish the foundational architecture, build configuration, and infrastructure required for all subsequent feature development. This epic has no direct user-facing value but is essential for implementing all other epics.

#### Stories:
1. **Story 0.1**: As a developer, I want to create the Android project structure with Kotlin and Jetpack Compose so I have a modern foundation
   - **Acceptance Criteria:**
     - Android project created with Kotlin 1.9.22
     - Jetpack Compose 1.6.0 configured with Material 3
     - Gradle build scripts set up with Kotlin DSL
     - Target SDK 34, minimum SDK 26
     - Single Activity architecture with Compose Navigation
     - App compiles and runs with empty home screen

2. **Story 0.2**: As a developer, I want to configure Koin dependency injection so I have clean separation of concerns
   - **Acceptance Criteria:**
     - Koin 3.5.3 integrated
     - DI modules created (AppModule, DataModule, DomainModule, ViewModelModule)
     - Application class configured with Koin initialization
     - Sample ViewModel injection working

3. **Story 0.3**: As a developer, I want to set up Room database infrastructure so I can queue failed transmissions
   - **Acceptance Criteria:**
     - Room 2.6.1 configured with KSP
     - PhoneManagerDatabase created
     - TransmissionQueueDao and entity defined
     - Database migration strategy established
     - Basic CRUD operations tested

4. **Story 0.4**: As a developer, I want to configure WorkManager so I can schedule background tasks reliably
   - **Acceptance Criteria:**
     - WorkManager 2.9.0 integrated
     - WorkerFactory configured with Koin
     - Sample periodic worker created and tested
     - Doze mode constraints configured

5. **Story 0.5**: As a developer, I want to implement base architecture patterns so I have consistent structure
   - **Acceptance Criteria:**
     - Repository pattern base classes/interfaces
     - ViewModel base setup with StateFlow
     - Navigation graph structure
     - Data models package structure
     - Use case pattern established

6. **Story 0.6**: As a developer, I want to configure build variants and ProGuard so I have optimized releases
   - **Acceptance Criteria:**
     - Debug and Release build types configured
     - ProGuard rules defined for Koin, Retrofit, Room
     - Signing configuration prepared (without committing keys)
     - Build successfully generates minified release APK
     - Timber logging configured (debug vs release trees)

**Dependencies:** None (first epic to implement)

**Blocks:** Epic 1, Epic 2, Epic 3, Epic 4 (all feature epics require this foundation)

---

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

### Epic 5: Admin/Owner Management

**Priority:** High
**Estimated Effort:** Medium-High

**Overview:** Admins/owners can manage multiple users' devices from the app. Navigation via homescreen toggle between "My Device" and "Users" view. Reuses existing device screens for consistency.

#### Stories:

1. **Story 5.1**: As an admin, I want my role detected from backend so I can access admin features
   - **Acceptance Criteria:**
     - Backend API returns user role (admin/owner/user) on authentication
     - Admin toggle/menu visible only if role is admin or owner
     - Role cached locally, refreshed on app launch
     - Graceful handling if role endpoint unavailable

2. **Story 5.2**: As an admin, I want to toggle between "My Device" and "Users" view on homescreen
   - **Acceptance Criteria:**
     - Homescreen displays toggle/switch for admins only
     - "My Device" shows current device status (existing functionality)
     - "Users" view shows list of managed devices/users
     - Toggle state persists during session
     - Smooth transition animation between views

3. **Story 5.3**: As an admin, I want to view a user's current location using the existing device screen
   - **Acceptance Criteria:**
     - Select user from list navigates to device detail screen
     - Same UI as "My Device" screen (reused components)
     - Shows user's current location on map
     - Displays last update timestamp
     - Back navigation returns to users list

4. **Story 5.4**: As an admin, I want to create/update geofence boundaries for a user
   - **Acceptance Criteria:**
     - Geofence configuration accessible from user detail screen
     - Map-based geofence drawing (circle with adjustable radius)
     - Coordinate input option (latitude, longitude, radius)
     - Save geofence to backend per user
     - Visual indicator when user is inside/outside geofence
     - Support multiple geofences per user (optional)

5. **Story 5.5**: As an admin, I want to enable/disable tracking for a user
   - **Acceptance Criteria:**
     - Toggle control on user detail screen
     - Backend API to update tracking state for managed device
     - Push notification or next-sync update to target device
     - Visual confirmation of tracking state change
     - Audit log of tracking state changes (optional)

6. **Story 5.6**: As an admin, I want to remove a user from my managed list
   - **Acceptance Criteria:**
     - Remove action with confirmation dialog
     - Backend API to revoke management relationship
     - User removed from list immediately
     - Removed user's device continues to function independently
     - Cannot remove self from list

---

## Technical Constraints

1. **Platform**: Android 8.0 (API 26) minimum, target Android 14 (API 34)
2. **Language**: Kotlin (preferred for Android development)
3. **UI Framework**: Jetpack Compose with Material 3
4. **Architecture**: MVVM with Clean Architecture (simplified), Repository pattern
5. **Dependency Injection**: Koin (lightweight, minimal boilerplate)
6. **Background Work**: WorkManager API
7. **Location**: Google Play Services Location API (Fused Location Provider)
8. **Encryption**: Android Crypto API (AES/CBC/PKCS7Padding)
9. **Networking**: Retrofit 2 + OkHttp 3
10. **Storage**: EncryptedSharedPreferences, Room Database (for queue)
11. **Build System**: Gradle with Kotlin DSL

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
- Cloud backup of location data
- Location sharing with other users (peer-to-peer)
- iOS version
- Web dashboard
- Push notifications for geofence alerts (Phase 2)

**Moved to In-Scope (v1.2):**
- ~~Multi-user support~~ → Epic 5: Admin/Owner Management
- ~~Geofencing alerts~~ → Epic 5, Story 5.4: Geofence boundaries

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

**Status**: Updated
**PRD Version**: 1.2
**Last Updated**: 2025-12-16
**Next Phase**: Tech Spec Generation

**Changelog (v1.2):**
- Added Epic 5: Admin/Owner Management (6 new stories)
- Moved multi-user support and geofencing from Out of Scope to In-Scope
- Admin features: role-based access, user list, location viewing, geofence management, tracking control, user removal
- UI approach: Homescreen toggle, reuses existing device screens
- Total epics: 6 (1 enabler + 5 feature epics)
- Total stories: 23 (6 technical + 17 user-facing)

**Changelog (v1.1):**
- Added Epic 0: Project Setup and Infrastructure (enabler epic)
- Updated Technical Constraints (Koin instead of Hilt, added Jetpack Compose)
- Added 6 technical stories for foundational setup

---

_This PRD was created and updated as part of the BMAD solution architecture workflow._

---

# ADDENDUM: Unified Group Management (Frontend)

**Version:** 1.3
**Date:** 2025-12-18
**Author:** Partner
**Backend PRD:** `phone-manager-backend/docs/prd-unified-group-management.md`

---

## Executive Summary - Unified Group Management (Frontend)

Phone Manager currently has two conflicting group systems that create friction for users upgrading from anonymous to authenticated usage:

1. **Registration Groups** - Simple string-based groups for anonymous device discovery (no authentication required)
2. **Authenticated Groups** - User-managed groups with roles (OWNER/ADMIN/MEMBER) via invite codes

This PRD defines the **Android frontend changes** required to unify these systems, enabling seamless transition from anonymous to authenticated usage without data loss.

### What Makes This Special

**Seamless anonymous-to-authenticated upgrade path:**

- **Zero-friction onboarding** - Users can share location with family immediately without login
- **No data loss on upgrade** - Registration group migrates cleanly to authenticated group with all devices preserved
- **Device follows user** - Auto-linking ensures device ownership is clear after login
- **Flexible multi-group sharing** - One device can participate in multiple authenticated groups
- **Admin visibility** - Group owners/admins can see WHO owns each device and manage their settings

### Core User Flows

1. **Auto-link device on login** - Device automatically linked to user account upon authentication
2. **Group Migration Wizard** - Prompt user to convert registration group to authenticated group
3. **Manual device-to-group assignment** - Explicit choice to add device when joining new groups
4. **Unified Admin View** - Admin sees all members with their linked devices in one place

## Project Classification

| Attribute | Value |
|-----------|-------|
| **Technical Type** | Mobile App (Android Native) |
| **Domain** | General (Family/Team Location Sharing) |
| **Complexity** | Medium |
| **Project Context** | Brownfield - extending existing system |
| **Backend PRD** | `phone-manager-backend/docs/prd-unified-group-management.md` |

**Tech Stack:** Kotlin, Jetpack Compose, MVVM, Hilt DI, Room, Ktor Client, Google Maps SDK

**Architecture:** Repository pattern, ViewModel with StateFlow, Navigation with SavedStateHandle

---

## Success Criteria - Unified Group Management

### User Success

| Criteria | Target | Measurement |
|----------|--------|-------------|
| **Migration Completion** | <2 minutes | Time from login to completed group migration |
| **Zero Data Loss** | 100% | All devices from registration group appear in authenticated group |
| **Device Linking** | Automatic | Device linked to user account immediately upon login |
| **Admin Clarity** | Immediate | Admin sees device ownership within 1 tap from member list |
| **Multi-Group Support** | Seamless | User can add device to additional groups without confusion |

**"Aha!" Moments:**
- Admin taps on member → sees their linked devices with current location
- User logs in → device automatically shows as "owned by you"
- Migration wizard → all family devices preserved in new authenticated group

### Business Success

| Criteria | Target | Measurement |
|----------|--------|-------------|
| **Anonymous-to-Auth Conversion** | >60% | Users who register anonymously and later create accounts |
| **Group Migration Rate** | >80% | Users prompted who complete migration |
| **Admin Feature Adoption** | >70% | Admins/Owners who use member management features |
| **Reduced Support Queries** | -50% | Fewer "where are my devices?" questions |

### Technical Success

| Criteria | Target | Measurement |
|----------|--------|-------------|
| **Auto-Link Success Rate** | >99% | Devices successfully linked on login |
| **Migration API Success** | >99% | Successful calls to `/api/v1/groups/migrate` |
| **UI Response Time** | <500ms | GroupMigrationScreen load time |
| **Error Recovery** | 100% | All migration failures show clear retry path |
| **Backend Dependency** | Graceful | App functions if migration endpoint unavailable |

### Measurable Outcomes

**At 1 Month:**
- All new logins trigger auto-link successfully
- Migration wizard tested with 10+ real users
- Admin view shows linked devices for all members

**At 3 Months:**
- 80%+ of registration groups migrated to authenticated groups
- Zero orphaned devices (devices without owner after user logged in)
- Admin features used by majority of group owners

---

## Product Scope - Unified Group Management

### MVP - Minimum Viable Product

**Must have for launch:**

1. **Auto-Link Device on Login** (Story UGM-1)
   - Device `owner_user_id` set automatically after authentication
   - Works with email/password and OAuth login
   - Existing `autoLinkCurrentDevice()` enhanced

2. **Group Migration Wizard** (Story UGM-2)
   - Detect if device is in registration group after login
   - Prompt: "Migrate your group 'X' to a managed group?"
   - Create authenticated group with user as OWNER
   - Migrate all devices from registration group
   - Delete registration group after successful migration

3. **Device-to-Group Assignment** (Story UGM-3)
   - After joining group via invite code, prompt: "Add your device to this group?"
   - Device explicitly added to authenticated group
   - One device can be in multiple groups

4. **Enhanced Admin View** (Story UGM-4)
   - Member list shows linked device count per member
   - Tap member → UserHomeScreen shows their devices
   - Handle "no devices" and "unlinked device" edge cases

### Growth Features (Post-MVP)

- **Bulk Device Migration** - Migrate multiple devices at once
- **Group Merge** - Combine two authenticated groups
- **Device Transfer** - Move device ownership between users
- **Migration History** - Audit log of group migrations
- **Push Notifications** - Notify other devices when migration occurs

### Vision (Future)

- **Cross-Platform Groups** - iOS devices in same groups
- **Organization Hierarchy** - Nested groups for enterprises
- **Device Policies** - Apply settings to all devices in group
- **Automated Group Assignment** - Rules-based device placement

---

## User Journeys - Unified Group Management

### Journey 1: Martin - From Anonymous Tracker to Family Group Owner

Martin is a privacy-conscious father of two teenagers who discovered Phone Manager while searching for a self-hosted alternative to Life360. He's tired of commercial apps harvesting his family's location data. One evening, he installs Phone Manager on his phone, names his device "Dad's Phone", and creates a simple group called "family123" - no account needed, just instant location sharing.

Over the next week, Martin installs the app on his wife's phone and both kids' devices, all joining "family123". The family can now see each other on the map. It works perfectly, but Martin realizes he wants more control - the ability to see who's who, manage settings remotely, and invite his elderly mother to the group securely.

Martin decides to create an account. After logging in with his email, his device is automatically linked to his new account - he sees "Dad's Phone (owned by you)" in the settings. Then the magic happens: a friendly prompt appears asking "Would you like to upgrade 'family123' to a managed group?" He taps "Yes", names it "The Martins", and within seconds, all 4 family devices appear in his new authenticated group. He's now the Owner with full control.

The breakthrough comes when Martin generates an invite code (ABC-DEF-GHI) and texts it to his mother. She joins with one tap, and Martin can now see exactly which family member is where - no more anonymous "Device 3" labels. Six months later, Martin has created a second group for his cycling club, and Phone Manager has become his trusted solution for all location sharing needs.

**Journey reveals requirements for:**
- Auto-link device on login (UGM-1)
- Group Migration Wizard with clear prompt (UGM-2)
- Preserve all devices during migration
- Owner role assignment after migration
- Invite code generation for new members

---

### Journey 2: Sarah - Admin Managing Family Devices

Sarah is Martin's wife and a co-admin of "The Martins" group. Their teenage son Alex has been coming home later than agreed, and Sarah wants to check his location history and set up a geofence alert for when he arrives at school.

One morning, Sarah opens Phone Manager and taps on the "Users" tab at the bottom of the home screen. She sees the family member list: Martin (Owner), herself (Admin), Alex (Member), and Emma (Member). Each name shows a small badge indicating how many devices they have linked.

Sarah taps on "Alex" and is taken to his UserHomeScreen - the same familiar interface she sees for her own device, but now showing Alex's data. She can see his current location on the map, his last update timestamp, and his tracking status. She notices his device shows "Location Available" with a green indicator.

She scrolls down and taps "Manage Geofences" to set up a school arrival alert. After saving the geofence, she also toggles "Tracking" to ensure Alex's location updates more frequently during school hours. A confirmation appears: "Tracking settings updated for Alex's Phone."

That evening, Sarah receives a notification: "Alex arrived at School" - exactly what she needed for peace of mind.

**Journey reveals requirements for:**
- Admin toggle between "My Device" and "Users" view
- Member list with device count badges
- UserHomeScreen showing another user's data (UGM-4)
- Geofence management for other users
- Tracking toggle for managed devices
- Clear visual indicators for device status

---

### Journey 3: Emma - Joining Family Group via Invite Code

Emma is Martin's 14-year-old daughter who just got a new phone for her birthday. Her old device was already in the family group, but this new phone needs to be set up fresh.

Emma downloads Phone Manager and registers her device as "Emma's iPhone" with a temporary group "emma-temp" just to get started. The app works immediately - she can see her location being tracked. But she wants to be in the family group with everyone else.

Her dad texts her an invite code: ABC-DEF-GHI. Emma opens the app, goes to Settings → Groups → Join Group, and enters the code. A preview appears: "The Martins - 4 members, Owner: Dad". She taps "Join" but gets a message: "Please log in to join this group."

Emma quickly creates an account with her email. Upon logging in, her device is automatically linked to her account. She re-enters the invite code, sees the group preview again, and this time taps "Join" successfully. A prompt appears: "Add 'Emma's iPhone' to The Martins for location sharing?" She confirms, and within seconds, she appears in the family group.

Her dad sees a notification: "Emma joined The Martins" - and can now see her new device on the family map.

**Journey reveals requirements for:**
- Invite code validation with group preview
- Authentication required for joining authenticated groups
- Auto-link device on login (UGM-1)
- Device-to-group assignment prompt (UGM-3)
- Explicit device addition to group
- Notification to group owner/admins

---

### Journey 4: Alex - Anonymous User Upgrading After Login

Alex is Martin's 16-year-old son who has been using Phone Manager for months without an account. His device "Alex's Phone" is in the "family123" registration group (before the family migrated). One day, his dad migrates the family group, and Alex's device automatically moves to the new authenticated group.

But Alex wants his own account now - he's planning a road trip with friends and wants to create his own group for the trip. He opens the app and creates an account with his email.

Upon logging in, Alex sees his device automatically linked: "Alex's Phone (owned by you)". Since his device is already in "The Martins" authenticated group (migrated by his dad), there's no migration prompt - he's already set up.

Alex goes to Settings → Groups → Create Group and makes a new group called "Road Trip Crew". He generates an invite code and shares it with his three friends. When they join, Alex can see all of them on the map. His phone is now in TWO groups: "The Martins" (as a member) and "Road Trip Crew" (as the owner).

After the road trip, Alex deletes the "Road Trip Crew" group - his device remains in the family group unaffected.

**Journey reveals requirements for:**
- Auto-link device on login (UGM-1)
- Device can be in multiple groups simultaneously
- No migration prompt if already in authenticated group
- Group creation as Owner
- Group deletion without affecting other memberships
- Multi-group device management

---

### Journey Requirements Summary

| Journey | Key Capabilities Revealed |
|---------|--------------------------|
| **Martin (Migration)** | Auto-link, Migration Wizard, Device preservation, Owner role, Invite generation |
| **Sarah (Admin)** | Users tab, Member list, UserHomeScreen, Geofence management, Tracking control |
| **Emma (Join via Invite)** | Invite validation, Auth requirement, Auto-link, Device-to-group prompt |
| **Alex (Multi-group)** | Auto-link, Multi-group support, Group creation/deletion, No duplicate migration |

**Frontend Screens Required:**
1. **GroupMigrationScreen** - Wizard for upgrading registration group
2. **DeviceAssignmentDialog** - Prompt after joining group
3. **UserHomeScreen** - Already implemented, needs device indicators
4. **AdminUsersScreen** - Member list with device badges
5. **Multi-group indicator** - Show which groups device belongs to

---

## Mobile App Specific Requirements - Unified Group Management

### Platform Requirements

| Requirement | Value | Notes |
|-------------|-------|-------|
| **Platform** | Android Native | Kotlin with Jetpack Compose |
| **Minimum SDK** | API 26 (Android 8.0) | Already established in base PRD |
| **Target SDK** | API 34 (Android 14) | Latest stable target |
| **Architecture** | MVVM + Clean Architecture | Repository pattern, StateFlow |
| **DI Framework** | Hilt | Migration from Koin in progress |
| **Network** | Ktor Client | REST API communication |

### Device Permissions

The Unified Group Management feature utilizes existing permissions:

| Permission | Required For | When Requested |
|------------|--------------|----------------|
| `INTERNET` | API calls (migration, linking) | Always (manifest) |
| `ACCESS_FINE_LOCATION` | Location sharing in groups | On tracking enable |
| `ACCESS_BACKGROUND_LOCATION` | Background location updates | After foreground granted |
| `POST_NOTIFICATIONS` | Migration/join notifications | On first notification trigger |

**No new permissions required** - UGM features use existing permission grants.

### Offline Mode Considerations

| Scenario | Behavior | Implementation |
|----------|----------|----------------|
| **Auto-link on login** | Retry on network restoration | Queue in Room DB, WorkManager retry |
| **Group migration** | Block until online | Show "requires connection" message |
| **Device-to-group assignment** | Retry on network restoration | Queue pending, sync when online |
| **Member list viewing** | Cached data shown | Room cache with stale indicator |

**Offline Strategy:**
- Migration operations require connectivity (transactional)
- Device linking queued for retry if offline
- Member/device lists cached locally
- Stale data indicators shown when cached

### Push Notification Strategy

| Event | Notification | Target |
|-------|--------------|--------|
| **Migration complete** | "Your group has been upgraded" | Migrating user |
| **New member joined** | "Emma joined The Martins" | Group owners/admins |
| **Device added to group** | "Emma's iPhone added to group" | Group owners/admins |
| **Member removed** | "Alex left the group" | Group owners/admins |

**Implementation:**
- Firebase Cloud Messaging (FCM) for push delivery
- Backend triggers notifications on group events
- Deep links to relevant screens (group detail, member list)

### Store Compliance

| Requirement | Status | Notes |
|-------------|--------|-------|
| **Play Store Guidelines** | Compliant | No policy changes for UGM |
| **Location Permission Rationale** | Already implemented | Base PRD covers this |
| **Background Location Justification** | Already approved | Required for tracking feature |
| **Data Safety Section** | Update needed | Add group membership data collection |

**Data Safety Update Required:**
- Group membership data collected
- Device-to-group associations stored
- User profile data (display name, email) shared within groups

### Navigation Architecture

```
GroupsScreen (List)
    ├── GroupDetailScreen
    │   ├── ManageMembersScreen
    │   │   └── UserHomeScreen (tap on member)
    │   ├── InviteMemberScreen
    │   └── GroupSettingsScreen
    ├── CreateGroupScreen
    ├── JoinGroupScreen (invite code)
    └── GroupMigrationScreen (NEW - post-login wizard)
```

### State Management

| Screen | ViewModel | State Class |
|--------|-----------|-------------|
| GroupMigrationScreen | GroupMigrationViewModel (NEW) | MigrationUiState |
| JoinGroupScreen | JoinGroupViewModel | JoinGroupUiState (extended) |
| ManageMembersScreen | GroupDetailViewModel | GroupDetailUiState (existing) |
| UserHomeScreen | UserHomeViewModel | UserHomeUiState (existing) |

**New State Classes:**
```kotlin
sealed interface MigrationUiState {
    data object Checking : MigrationUiState
    data class Found(val registrationGroupId: String, val deviceCount: Int) : MigrationUiState
    data object Migrating : MigrationUiState
    data object Success : MigrationUiState
    data class Error(val message: String) : MigrationUiState
    data object NotNeeded : MigrationUiState
}
```

### Implementation Considerations

**Backward Compatibility:**
- Existing registration group flows continue to work
- No forced migration - users prompted but can skip
- Anonymous users unaffected until they log in

**Error Handling:**
- Migration failures show clear retry option
- Network errors queued for retry
- Partial failure states handled gracefully

**Testing Strategy:**
- Unit tests for ViewModels with mock repositories
- Integration tests for API calls
- UI tests for migration wizard flow
- Manual testing for edge cases (offline, partial migration)

---

## Project Scoping & Phased Development - Unified Group Management

### MVP Strategy & Philosophy

**MVP Approach:** Problem-Solving MVP
- Solve the core friction: devices stuck between registration and authenticated group systems
- Minimal viable experience: login → auto-link → migrate → done
- Learn: Does automatic migration reduce "lost device" support tickets?

**Resource Requirements:**
- 1 Android developer (2-3 weeks)
- Backend API ready (companion PRD covers backend)
- QA: Integration testing focus

**Why Problem-Solving MVP:**
The core problem is clear (users lose devices when upgrading from anonymous to authenticated). The solution is straightforward (auto-link + migration wizard). We don't need to build platform features or complex UX - just make the upgrade path work seamlessly.

### MVP Feature Set (Phase 1)

**Core User Journeys Supported:**
| Journey | MVP Support | Notes |
|---------|-------------|-------|
| Martin (Migration) | Full | Primary journey - migration wizard |
| Sarah (Admin) | Partial | Member list with devices, no advanced features |
| Emma (Join) | Full | Invite code + device assignment |
| Alex (Multi-group) | Full | Multi-group support built into architecture |

**Must-Have Capabilities (4 Stories):**

| Story | Description | Without This... |
|-------|-------------|-----------------|
| **UGM-1: Auto-Link** | Device linked to user on login | Device remains orphaned, admin can't see owner |
| **UGM-2: Migration Wizard** | Convert registration group to authenticated | Users lose existing group members |
| **UGM-3: Device Assignment** | Prompt to add device when joining group | Device not in group, location not shared |
| **UGM-4: Enhanced Admin View** | See member devices in admin screen | Admin can't manage member devices |

**Explicitly NOT in MVP:**
- Migration rollback (can be done manually via support)
- Partial migration (all devices or none - keeps it simple)
- Push notifications for migration events (users are in the app)
- Bulk device management (one device at a time is fine for families)

### Post-MVP Features

**Phase 2 (Growth) - Post-Launch:**
- **Push notifications** for group events (member joined, device added)
- **Bulk device migration** (select which devices to migrate)
- **Migration history** (audit log of past migrations)
- **Device transfer** between users within group

**Phase 3 (Expansion) - Future:**
- **Group merge** capability (combine two authenticated groups)
- **Cross-platform** support (iOS devices in same groups)
- **Organization hierarchy** (nested groups for enterprises)
- **Device policies** (apply tracking settings to all devices in group)

### Risk Mitigation Strategy

**Technical Risks:**

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| Migration API fails mid-transaction | Low | High | Backend handles atomically - all or nothing |
| Device already linked to different user | Medium | Medium | Show error, suggest support contact |
| Network failure during migration | Medium | Medium | Show retry option, queue for later |

**Market Risks:**

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| Users don't want to create accounts | Medium | High | Preserve anonymous mode, make migration optional |
| Migration too confusing | Low | Medium | Simple wizard with clear progress |
| Users want to stay in both systems | Low | Low | Not supported - explain upgrade benefits |

**Resource Risks:**

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| Backend API delayed | Medium | High | Can mock API for UI development, test with stubs |
| QA time insufficient | Medium | Medium | Focus on happy path, edge cases in v1.1 |
| Developer unavailable | Low | High | Well-documented stories enable handoff |

### Development Sequence

**Recommended Implementation Order:**

1. **UGM-1: Auto-Link Device** (foundation - other features depend on this)
2. **UGM-4: Enhanced Admin View** (quick win - mostly UI, existing data)
3. **UGM-3: Device Assignment** (extends JoinGroupScreen)
4. **UGM-2: Migration Wizard** (most complex - new screen, new API)

**Rationale:** Auto-link is required for all other features. Admin view can be shipped independently. Device assignment is a small addition. Migration wizard is the most complex and can come last since anonymous users continue to work.

---

## Functional Requirements - Unified Group Management

### Device-User Linking

- **FR1:** Authenticated user's device can be automatically linked to their account upon successful login
- **FR2:** System can detect if a device is already linked to a different user account
- **FR3:** User can view which account their device is linked to
- **FR4:** System can update device ownership in backend when linking occurs

### Group Migration

- **FR5:** System can detect if authenticated user's device is in a registration group (not authenticated group)
- **FR6:** User can view migration prompt showing registration group name and device count
- **FR7:** User can choose to migrate their registration group to an authenticated group
- **FR8:** User can specify a name for the new authenticated group during migration
- **FR9:** System can migrate all devices from registration group to new authenticated group atomically
- **FR10:** System can assign OWNER role to migrating user in new authenticated group
- **FR11:** System can delete the registration group after successful migration
- **FR12:** User can dismiss migration prompt and continue without migrating
- **FR13:** User can retry migration if it fails due to network error

### Device-to-Group Assignment

- **FR14:** User can view device assignment prompt after joining an authenticated group
- **FR15:** User can choose to add their device to a newly joined group
- **FR16:** User can decline to add their device to a newly joined group
- **FR17:** System can add device to authenticated group for location sharing
- **FR18:** Device can belong to multiple authenticated groups simultaneously
- **FR19:** User can view which groups their device belongs to

### Group Membership Management

- **FR20:** Owner/Admin can view list of all members in their authenticated group
- **FR21:** Owner/Admin can view device count badge for each group member
- **FR22:** Owner/Admin can tap on a member to view their device details
- **FR23:** Owner/Admin can view member's linked devices with current location
- **FR24:** Owner/Admin can view member's device tracking status
- **FR25:** Owner/Admin can view member's device last update timestamp

### Group Creation & Joining

- **FR26:** Authenticated user can create a new authenticated group
- **FR27:** User can specify group name when creating a group
- **FR28:** Creator becomes OWNER of newly created group
- **FR29:** User can generate invite code for their group (Owner/Admin)
- **FR30:** User can enter invite code to preview group before joining
- **FR31:** User can join authenticated group using valid invite code
- **FR32:** System can validate invite code before allowing join
- **FR33:** System requires authentication before joining authenticated group

### Multi-Group Support

- **FR34:** User can be a member of multiple authenticated groups
- **FR35:** User can view all groups they belong to
- **FR36:** User can switch between groups in the app
- **FR37:** User can leave a group they are a member of (non-owners)
- **FR38:** Owner can delete a group they own

### Error Handling & Recovery

- **FR39:** System can display clear error message when migration fails
- **FR40:** System can display clear error message when device linking fails
- **FR41:** User can retry failed operations with one tap
- **FR42:** System can queue device linking for retry when offline
- **FR43:** System can show "requires connection" message for migration when offline

---

## Non-Functional Requirements - Unified Group Management

### Performance

| Requirement | Target | Measurement |
|-------------|--------|-------------|
| **NFR-P1:** Auto-link device on login | < 500ms | Time from login success to device linked |
| **NFR-P2:** Migration wizard load time | < 500ms | Time to display migration prompt after login |
| **NFR-P3:** Group migration completion | < 2s perceived | User waits max 2 seconds with progress indicator |
| **NFR-P4:** Member list load time | < 1s | Time to display member list with device counts |
| **NFR-P5:** Device assignment operation | < 1s | Time to add device to group |

**Rationale:** Users expect instant feedback. Migration is a one-time operation that must feel seamless to encourage adoption.

### Security

| Requirement | Description |
|-------------|-------------|
| **NFR-S1:** Device linking requires valid JWT | Only authenticated users can link devices |
| **NFR-S2:** Group migration requires ownership proof | Only device owner can initiate migration |
| **NFR-S3:** Member data visibility respects roles | Non-admins cannot see member device details |
| **NFR-S4:** Invite codes expire | Codes valid for limited time (24-48 hours) |
| **NFR-S5:** Secure storage of group membership | Group data encrypted at rest on device |

**Rationale:** Group membership and location data are sensitive. Role-based access control prevents unauthorized viewing.

### Reliability

| Requirement | Target | Description |
|-------------|--------|-------------|
| **NFR-R1:** Migration atomicity | 100% | All devices migrate or none do - no partial state |
| **NFR-R2:** Auto-link retry on failure | 3 attempts | Queue and retry device linking if network fails |
| **NFR-R3:** Data preservation during migration | 100% | No device or location history lost |
| **NFR-R4:** Graceful degradation offline | Full | Show cached data with stale indicator when offline |
| **NFR-R5:** Error recovery | User-initiated | Clear retry option for all failed operations |

**Rationale:** Data loss during migration would destroy user trust. Atomic operations prevent inconsistent state.

### Integration

| Requirement | Description |
|-------------|-------------|
| **NFR-I1:** Backend API version compatibility | Support API v1 endpoints defined in companion PRD |
| **NFR-I2:** Offline queue persistence | Pending operations survive app restart |
| **NFR-I3:** Network state monitoring | Detect connectivity changes and auto-retry queued operations |
| **NFR-I4:** Backend timeout handling | 30s timeout with user feedback and retry option |

**Rationale:** Backend API is the source of truth. Frontend must handle network unreliability gracefully.

### Accessibility

| Requirement | Description |
|-------------|-------------|
| **NFR-A1:** Screen reader support | All UI elements have content descriptions |
| **NFR-A2:** Touch target size | Minimum 48dp for interactive elements |
| **NFR-A3:** Color contrast | WCAG AA compliance (4.5:1 for text) |
| **NFR-A4:** Focus navigation | Logical tab order in migration wizard |

**Rationale:** Mobile accessibility best practices ensure usability for all users.

### Skipped Categories

**Scalability** - Not included because:
- Family groups typically have <10 members
- Base Phone Manager app already handles location data at scale
- No multi-tenancy or enterprise considerations for this feature

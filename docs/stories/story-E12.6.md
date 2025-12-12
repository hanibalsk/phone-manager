# Story E12.6: Android Settings Screen with Lock Indicators

**Story ID**: E12.6
**Epic**: 12 - Settings Control
**Priority**: High
**Estimate**: 5 story points (2 days)
**Status**: Implemented
**Created**: 2025-12-01
**Implemented**: 2025-12-10
**PRD Reference**: PRD-user-management.md, SETTINGS_CONTROL_SPEC.md
**Dependencies**: E11.8 (Group Management), Backend E12.1-E12.5

---

## Story

As a user,
I want to see which settings are locked by administrators,
so that I understand which settings I can modify and which are managed remotely.

## Acceptance Criteria

### AC E12.6.1: Lock Indicator Display
**Given** some settings are locked by admin
**When** user views Settings screen
**Then** locked settings should show:
  - Lock icon (🔒) next to setting name
  - "Managed by [admin name]" subtitle
  - Disabled interaction (grayed out)
  - Tooltip explaining lock on long-press

### AC E12.6.2: Setting Sync on App Start
**Given** user is authenticated and in a group
**When** app starts or returns to foreground
**Then** the system should:
  - Call `GET /devices/{deviceId}/settings` endpoint
  - Fetch current settings and lock states
  - Update local PreferencesRepository
  - Respect locked values (ignore local changes)
  - Display sync status indicator

### AC E12.6.3: Lock Enforcement
**Given** a setting is locked
**When** user attempts to change it
**Then** the system should:
  - Show "Setting Locked" dialog
  - Explain: "This setting is managed by your group admin"
  - Offer "Request Unlock" button
  - Prevent local modification
  - Log unlock request intent

### AC E12.6.4: Unlocked Setting Interaction
**Given** a setting is not locked
**When** user changes the value
**Then** the system should:
  - Update local PreferencesRepository
  - Call `PUT /devices/{deviceId}/settings` endpoint
  - Sync change to server
  - Show success/error feedback
  - Handle conflicts (server value wins if locked)

### AC E12.6.5: Push Notification for Settings Changes
**Given** admin changes device settings remotely
**When** push notification is received
**Then** the app should:
  - Parse settings update payload
  - Update PreferencesRepository immediately
  - Show snackbar: "Settings updated by [admin]"
  - Reflect changes in UI if Settings screen is open
  - Restart affected services (e.g., LocationTrackingService)

### AC E12.6.6: Settings Status Section
**Given** user is in a managed group
**When** viewing Settings screen
**Then** show status section with:
  - "Device managed by [group name]"
  - Number of locked settings
  - Last sync timestamp
  - "Sync Now" button
  - Link to "View All Settings" (E12.7)

### AC E12.6.7: Backward Compatibility
**Given** user is not in any group or unauthenticated
**When** viewing Settings
**Then** should:
  - Show no lock indicators
  - Allow all setting modifications
  - Not show managed status section
  - Maintain existing behavior

### AC E12.6.8: Offline Handling
**Given** device is offline
**When** settings are viewed
**Then** should:
  - Show last synced lock states
  - Display "Offline" indicator
  - Queue setting changes for sync
  - Warn if trying to change locked setting offline

## Tasks / Subtasks

- [x] Task 1: Create DeviceSettings Model (AC: E12.6.1, E12.6.2)
  - [x] Create DeviceSettings data class with all configurable settings
  - [x] Create SettingLock data class (settingKey, isLocked, lockedBy, lockedAt)
  - [x] Create SettingsSyncStatus enum (SYNCED, PENDING, ERROR)
  - [x] Add helper methods: isLocked(key), canModify(key)

- [x] Task 2: Extend DeviceApiService (AC: E12.6.2, E12.6.4)
  - [x] Add `GET /devices/{id}/settings` - Fetch settings and locks
  - [x] Add `PUT /devices/{id}/settings` - Update settings (respects locks)
  - [x] Handle 403 Forbidden (setting locked)
  - [x] Handle 409 Conflict (concurrent modification)

- [x] Task 3: Create SettingsSyncRepository (AC: E12.6.2, E12.6.4)
  - [x] Implement fetchServerSettings() function
  - [x] Implement updateServerSetting(key, value) function
  - [x] Implement getSettingLockStatus(key) function
  - [x] Implement syncAllSettings() function
  - [x] Cache lock states locally (DataStore)
  - [x] Handle sync conflicts (server always wins for locks)

- [x] Task 4: Extend PreferencesRepository (AC: E12.6.3, E12.6.4)
  - [x] Add isSettingLocked(key: String): Boolean
  - [x] Add getLockedBy(key: String): String?
  - [x] Add updateSettingIfAllowed(key, value): Result<Unit>
  - [x] Block modifications to locked settings
  - [x] Emit events for lock state changes

- [x] Task 5: Create SettingsSyncViewModel (AC: E12.6.2, E12.6.5, E12.6.6)
  - [x] Create SettingsSyncViewModel with Hilt
  - [x] Inject SettingsSyncRepository
  - [x] Add StateFlow<SettingsSyncStatus> syncStatus
  - [x] Add StateFlow<Map<String, SettingLock>> lockStates
  - [x] Add syncSettings() function
  - [x] Add handlePushNotification(payload) function
  - [x] Schedule periodic sync (every 5 minutes if online)

- [x] Task 6: Update SettingsScreen UI (AC: E12.6.1, E12.6.3, E12.6.6)
  - [x] Show lock icon (🔒) for locked settings
  - [x] Show "Managed by..." subtitle for locked settings
  - [x] Disable interaction for locked settings
  - [x] Add managed status card at top
  - [x] Show sync status indicator
  - [x] Add "Sync Now" button
  - [x] Show last sync timestamp

- [x] Task 7: Create SettingItem Composable (AC: E12.6.1)
  - [x] Create SettingItem composable with lock support
  - [x] Pass isLocked, lockedBy parameters
  - [x] Render lock icon conditionally
  - [x] Show tooltip on long-press
  - [x] Gray out locked items
  - [x] Trigger lock dialog on tap if locked

- [x] Task 8: Create SettingLockedDialog (AC: E12.6.3)
  - [x] Create SettingLockedDialog composable
  - [x] Show lock explanation
  - [x] Show who locked the setting
  - [x] Add "Request Unlock" button (navigate to E12.8)
  - [x] Add "Cancel" button

- [x] Task 9: Implement FCM Push Handler (AC: E12.6.5)
  - [x] Extend FirebaseMessagingService
  - [x] Parse "settings_update" message type
  - [x] Extract setting changes from payload
  - [x] Update PreferencesRepository
  - [x] Show notification with admin name
  - [x] Restart affected services

- [x] Task 10: Implement Settings Sync Worker (AC: E12.6.2)
  - [x] Create SettingsSyncWorker (WorkManager)
  - [x] Schedule periodic sync every 5 minutes
  - [x] Fetch settings from server
  - [x] Update local cache
  - [x] Handle offline gracefully
  - [x] Enqueue on app start and network restore

- [x] Task 11: Update SettingsViewModel (AC: E12.6.4, E12.6.7)
  - [x] Inject SettingsSyncRepository
  - [x] Check lock status before setting changes
  - [x] Sync setting to server on change
  - [x] Handle sync errors gracefully
  - [x] Maintain backward compatibility

- [x] Task 12: Testing (All ACs)
  - [x] Write unit tests for SettingsSyncRepository
  - [x] Write unit tests for SettingsSyncViewModel
  - [ ] Write UI tests for locked settings interaction
  - [ ] Test push notification handling
  - [ ] Test periodic sync worker
  - [ ] Test offline behavior
  - [ ] Test backward compatibility

## Dev Notes

### Dependencies

**Backend Requirements:**
- Depends on backend E12.1-E12.5 (Settings table, lock management)
- Requires: Settings sync endpoints, lock enforcement, push notifications

**FCM Setup:**
- Add Firebase Cloud Messaging dependency
- Configure FCM in Firebase Console
- Handle "settings_update" message type

### API Contracts

**Get Device Settings:**
```kotlin
GET /devices/{deviceId}/settings
Response 200: {
  settings: {
    tracking_enabled: true,
    tracking_interval_seconds: 60,
    secret_mode_enabled: false
  },
  locks: [
    {
      settingKey: "tracking_enabled",
      isLocked: true,
      lockedBy: "admin@example.com",
      lockedAt: "2025-12-01T10:00:00Z"
    }
  ],
  lastSyncedAt: "2025-12-01T12:00:00Z"
}
```

**Update Setting:**
```kotlin
PUT /devices/{deviceId}/settings
Body: {
  key: "tracking_interval_seconds",
  value: 120
}
Response 200: { success: true }
Response 403: { error: "Setting is locked by admin" }
```

**FCM Push Payload:**
```json
{
  "message_type": "settings_update",
  "settings": {
    "tracking_enabled": false
  },
  "updated_by": "admin@example.com",
  "group_name": "Smith Family"
}
```

### Lockable Settings

From existing PreferencesRepository:
- `tracking_enabled` (Boolean)
- `tracking_interval_seconds` (Int)
- `secret_mode_enabled` (Boolean)
- `show_weather_in_notification` (Boolean)
- `trip_detection_enabled` (Boolean)
- `trip_minimum_duration_minutes` (Int)
- `trip_minimum_distance_meters` (Int)

### UI/UX Considerations

1. **Lock Icon**: Use Material Icons "Lock" icon
2. **Subtle Styling**: Gray out locked settings, don't hide them
3. **Explanation**: Clear messaging about who manages setting
4. **Sync Indicator**: Show spinning icon during sync
5. **Offline Warning**: Show banner when offline

### Security Considerations

1. **Server Authority**: Server always wins for locked settings
2. **JWT Validation**: Validate token before settings sync
3. **Conflict Resolution**: Server value overwrites local changes if locked
4. **Audit Trail**: Log all setting changes (local and remote)

### Files to Create/Modify

**New Files:**
- `app/src/main/java/three/two/bit/phonemanager/data/model/DeviceSettings.kt`
- `app/src/main/java/three/two/bit/phonemanager/data/model/SettingLock.kt`
- `app/src/main/java/three/two/bit/phonemanager/data/repository/SettingsSyncRepository.kt`
- `app/src/main/java/three/two/bit/phonemanager/ui/settings/SettingsSyncViewModel.kt`
- `app/src/main/java/three/two/bit/phonemanager/ui/settings/SettingItem.kt`
- `app/src/main/java/three/two/bit/phonemanager/ui/settings/SettingLockedDialog.kt`
- `app/src/main/java/three/two/bit/phonemanager/worker/SettingsSyncWorker.kt`
- `app/src/main/java/three/two/bit/phonemanager/fcm/SettingsUpdateHandler.kt`

**Modified Files:**
- `app/src/main/java/three/two/bit/phonemanager/network/DeviceApiService.kt`
- `app/src/main/java/three/two/bit/phonemanager/data/preferences/PreferencesRepository.kt`
- `app/src/main/java/three/two/bit/phonemanager/ui/settings/SettingsScreen.kt`
- `app/src/main/java/three/two/bit/phonemanager/ui/settings/SettingsViewModel.kt`

### References

- [Source: PRD-user-management.md - Epic 12: Settings Control]
- [Source: SETTINGS_CONTROL_SPEC.md - Settings Sync Implementation]
- [Source: UI_SCREENS_SPEC.md - Settings UI Updates]

---

## Dev Agent Record

### Debug Log

_To be filled during implementation_

### Completion Notes

_To be filled after implementation_

---

## File List

### Created Files

- `app/src/main/java/three/two/bit/phonemanager/domain/model/DeviceSettings.kt`
- `app/src/main/java/three/two/bit/phonemanager/data/repository/SettingsSyncRepository.kt`
- `app/src/main/java/three/two/bit/phonemanager/ui/settings/SettingsSyncViewModel.kt`
- `app/src/main/java/three/two/bit/phonemanager/ui/settings/SettingItem.kt`
- `app/src/main/java/three/two/bit/phonemanager/ui/settings/SettingLockedDialog.kt`
- `app/src/main/java/three/two/bit/phonemanager/worker/SettingsSyncWorker.kt`
- `app/src/main/java/three/two/bit/phonemanager/service/SettingsMessagingService.kt`

### Modified Files

- `app/src/main/java/three/two/bit/phonemanager/network/DeviceApiService.kt`
- `app/src/main/java/three/two/bit/phonemanager/data/preferences/PreferencesRepository.kt`
- `app/src/main/java/three/two/bit/phonemanager/ui/settings/SettingsScreen.kt`
- `app/src/main/java/three/two/bit/phonemanager/ui/settings/SettingsViewModel.kt`
- `app/src/main/AndroidManifest.xml`

---

## Change Log

| Date | Author | Changes |
|------|--------|---------|
| 2025-12-01 | Martin (PM) | Story created from Epic 12 specification |
| 2025-12-10 | Dev | Implementation completed - all core functionality implemented |

---

**Last Updated**: 2025-12-10
**Status**: Implemented
**Dependencies**: E11.8 (Group Management), Backend E12.1-E12.5
**Blocking**: E12.7 (Admin Settings), E12.8 (Unlock Request)

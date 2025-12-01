# Story E12.6: Android Settings Screen with Lock Indicators

**Story ID**: E12.6
**Epic**: 12 - Settings Control
**Priority**: High
**Estimate**: 5 story points (2 days)
**Status**: Planned
**Created**: 2025-12-01
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

- [ ] Task 1: Create DeviceSettings Model (AC: E12.6.1, E12.6.2)
  - [ ] Create DeviceSettings data class with all configurable settings
  - [ ] Create SettingLock data class (settingKey, isLocked, lockedBy, lockedAt)
  - [ ] Create SettingsSyncStatus enum (SYNCED, PENDING, ERROR)
  - [ ] Add helper methods: isLocked(key), canModify(key)

- [ ] Task 2: Extend DeviceApiService (AC: E12.6.2, E12.6.4)
  - [ ] Add `GET /devices/{id}/settings` - Fetch settings and locks
  - [ ] Add `PUT /devices/{id}/settings` - Update settings (respects locks)
  - [ ] Handle 403 Forbidden (setting locked)
  - [ ] Handle 409 Conflict (concurrent modification)

- [ ] Task 3: Create SettingsSyncRepository (AC: E12.6.2, E12.6.4)
  - [ ] Implement fetchServerSettings() function
  - [ ] Implement updateServerSetting(key, value) function
  - [ ] Implement getSettingLockStatus(key) function
  - [ ] Implement syncAllSettings() function
  - [ ] Cache lock states locally (DataStore)
  - [ ] Handle sync conflicts (server always wins for locks)

- [ ] Task 4: Extend PreferencesRepository (AC: E12.6.3, E12.6.4)
  - [ ] Add isSettingLocked(key: String): Boolean
  - [ ] Add getLockedBy(key: String): String?
  - [ ] Add updateSettingIfAllowed(key, value): Result<Unit>
  - [ ] Block modifications to locked settings
  - [ ] Emit events for lock state changes

- [ ] Task 5: Create SettingsSyncViewModel (AC: E12.6.2, E12.6.5, E12.6.6)
  - [ ] Create SettingsSyncViewModel with Hilt
  - [ ] Inject SettingsSyncRepository
  - [ ] Add StateFlow<SettingsSyncStatus> syncStatus
  - [ ] Add StateFlow<Map<String, SettingLock>> lockStates
  - [ ] Add syncSettings() function
  - [ ] Add handlePushNotification(payload) function
  - [ ] Schedule periodic sync (every 5 minutes if online)

- [ ] Task 6: Update SettingsScreen UI (AC: E12.6.1, E12.6.3, E12.6.6)
  - [ ] Show lock icon (🔒) for locked settings
  - [ ] Show "Managed by..." subtitle for locked settings
  - [ ] Disable interaction for locked settings
  - [ ] Add managed status card at top
  - [ ] Show sync status indicator
  - [ ] Add "Sync Now" button
  - [ ] Show last sync timestamp

- [ ] Task 7: Create SettingItem Composable (AC: E12.6.1)
  - [ ] Create SettingItem composable with lock support
  - [ ] Pass isLocked, lockedBy parameters
  - [ ] Render lock icon conditionally
  - [ ] Show tooltip on long-press
  - [ ] Gray out locked items
  - [ ] Trigger lock dialog on tap if locked

- [ ] Task 8: Create SettingLockedDialog (AC: E12.6.3)
  - [ ] Create SettingLockedDialog composable
  - [ ] Show lock explanation
  - [ ] Show who locked the setting
  - [ ] Add "Request Unlock" button (navigate to E12.8)
  - [ ] Add "Cancel" button

- [ ] Task 9: Implement FCM Push Handler (AC: E12.6.5)
  - [ ] Extend FirebaseMessagingService
  - [ ] Parse "settings_update" message type
  - [ ] Extract setting changes from payload
  - [ ] Update PreferencesRepository
  - [ ] Show notification with admin name
  - [ ] Restart affected services

- [ ] Task 10: Implement Settings Sync Worker (AC: E12.6.2)
  - [ ] Create SettingsSyncWorker (WorkManager)
  - [ ] Schedule periodic sync every 5 minutes
  - [ ] Fetch settings from server
  - [ ] Update local cache
  - [ ] Handle offline gracefully
  - [ ] Enqueue on app start and network restore

- [ ] Task 11: Update SettingsViewModel (AC: E12.6.4, E12.6.7)
  - [ ] Inject SettingsSyncRepository
  - [ ] Check lock status before setting changes
  - [ ] Sync setting to server on change
  - [ ] Handle sync errors gracefully
  - [ ] Maintain backward compatibility

- [ ] Task 12: Testing (All ACs)
  - [ ] Write unit tests for SettingsSyncRepository
  - [ ] Write unit tests for SettingsSyncViewModel
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

_To be filled during implementation_

### Modified Files

_To be filled during implementation_

---

## Change Log

| Date | Author | Changes |
|------|--------|---------|
| 2025-12-01 | Martin (PM) | Story created from Epic 12 specification |

---

**Last Updated**: 2025-12-01
**Status**: Planned
**Dependencies**: E11.8 (Group Management), Backend E12.1-E12.5
**Blocking**: E12.7 (Admin Settings), E12.8 (Unlock Request)

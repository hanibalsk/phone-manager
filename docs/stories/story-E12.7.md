# Story E12.7: Android Admin Settings Management

**Story ID**: E12.7
**Epic**: 12 - Settings Control
**Priority**: High
**Estimate**: 6 story points (2-3 days)
**Status**: Planned
**Created**: 2025-12-01
**PRD Reference**: PRD-user-management.md, SETTINGS_CONTROL_SPEC.md
**Dependencies**: E11.8 (Group Management), E12.6 (Settings with Locks), Backend E12.2-E12.3

---

## Story

As a group admin,
I want to view and manage settings for member devices,
so that I can ensure compliance and configure devices remotely.

## Acceptance Criteria

### AC E12.7.1: Device Settings List Screen
**Given** user is admin or owner
**When** they navigate to "Manage Devices" from group detail
**Then** they should see:
  - List of all member devices in group
  - Device name, owner name, online status
  - "Configure" button per device
  - Filter by online/offline status
  - Search by device or owner name

### AC E12.7.2: Device Settings Detail Screen
**Given** admin selects a device to configure
**When** viewing device settings
**Then** they should see:
  - All configurable settings with current values
  - Lock toggle next to each setting
  - "Apply Changes" button
  - "Reset to Defaults" button
  - Settings grouped by category (Tracking, Trip Detection, Display)
  - Last synced timestamp

### AC E12.7.3: View Remote Settings
**Given** admin opens device settings
**When** screen loads
**Then** the system should:
  - Call `GET /devices/{deviceId}/settings` endpoint
  - Display current setting values
  - Show lock status for each setting
  - Indicate if settings are out of sync
  - Show who last modified each setting

### AC E12.7.4: Modify Remote Settings
**Given** admin wants to change a setting
**When** they modify a value and tap "Apply Changes"
**Then** the system should:
  - Validate input (e.g., interval 10-3600 seconds)
  - Call `PUT /devices/{deviceId}/settings` endpoint with changes
  - Show confirmation dialog before applying
  - Display success/error feedback
  - Trigger push notification to device
  - Update local view with new values

### AC E12.7.5: Lock/Unlock Settings
**Given** admin wants to lock a setting
**When** they toggle the lock switch
**Then** the system should:
  - Show confirmation dialog: "Lock prevents user modification"
  - Call `PUT /devices/{deviceId}/settings/locks` endpoint
  - Update lock icon immediately
  - Log lock action in audit log
  - Push notification to device owner

### AC E12.7.6: Bulk Settings Application
**Given** admin wants to apply same settings to multiple devices
**When** they select "Apply to Multiple Devices"
**Then** they should:
  - See device multi-select screen
  - Choose which settings to apply
  - Choose whether to lock settings
  - Confirm bulk action
  - See progress indicator for each device
  - View summary of successes/failures

### AC E12.7.7: Settings Templates
**Given** admin frequently applies same configuration
**When** they tap "Save as Template"
**Then** they should be able to:
  - Name the template
  - Select which settings to include
  - Save template for reuse
  - Load template when configuring devices
  - Share template with other admins

### AC E12.7.8: Audit Trail
**Given** admin views device settings
**When** they tap "View History"
**Then** they should see:
  - Chronological list of all setting changes
  - Who made each change (admin or user)
  - Timestamp of change
  - Old and new values
  - Lock/unlock actions

## Tasks / Subtasks

- [ ] Task 1: Create Admin Settings Models (AC: E12.7.2, E12.7.7)
  - [ ] Create MemberDeviceSettings data class
  - [ ] Create SettingChange data class (key, oldValue, newValue, changedBy, timestamp)
  - [ ] Create SettingsTemplate data class (id, name, settings, locks)
  - [ ] Add validation methods for setting values

- [ ] Task 2: Extend DeviceApiService (AC: E12.7.3-E12.7.5)
  - [ ] Add `GET /devices/{id}/settings` - Fetch device settings (admin endpoint)
  - [ ] Add `PUT /devices/{id}/settings` - Update settings with changes map
  - [ ] Add `PUT /devices/{id}/settings/locks` - Bulk lock/unlock settings
  - [ ] Add `GET /devices/{id}/settings/history` - Fetch audit trail
  - [ ] Add `POST /settings/templates` - Save template
  - [ ] Add `GET /settings/templates` - List templates
  - [ ] Add `POST /devices/bulk-update` - Apply settings to multiple devices

- [ ] Task 3: Create AdminSettingsRepository (AC: All)
  - [ ] Implement getDeviceSettings(deviceId) function
  - [ ] Implement updateDeviceSettings(deviceId, changes) function
  - [ ] Implement lockSettings(deviceId, settingKeys) function
  - [ ] Implement unlockSettings(deviceId, settingKeys) function
  - [ ] Implement getSettingsHistory(deviceId) function
  - [ ] Implement saveTemplate(template) function
  - [ ] Implement getTemplates() function
  - [ ] Implement bulkUpdateDevices(deviceIds, settings) function

- [ ] Task 4: Create MemberDevicesViewModel (AC: E12.7.1)
  - [ ] Create MemberDevicesViewModel with Hilt
  - [ ] Inject GroupRepository, AdminSettingsRepository
  - [ ] Add StateFlow<List<MemberDevice>> devices
  - [ ] Add filterByStatus(online/offline) function
  - [ ] Add searchDevices(query) function
  - [ ] Load devices for current group

- [ ] Task 5: Create DeviceSettingsViewModel (AC: E12.7.2-E12.7.5, E12.7.8)
  - [ ] Create DeviceSettingsViewModel with Hilt
  - [ ] Add StateFlow<MemberDeviceSettings> settings
  - [ ] Add StateFlow<List<SettingChange>> history
  - [ ] Add loadDeviceSettings(deviceId) function
  - [ ] Add updateSetting(key, value) function
  - [ ] Add lockSetting(key) function
  - [ ] Add unlockSetting(key) function
  - [ ] Add applyChanges() function
  - [ ] Add loadHistory() function

- [ ] Task 6: Create SettingsTemplateViewModel (AC: E12.7.7)
  - [ ] Create SettingsTemplateViewModel with Hilt
  - [ ] Add StateFlow<List<SettingsTemplate>> templates
  - [ ] Add saveTemplate(name, settings) function
  - [ ] Add loadTemplate(templateId) function
  - [ ] Add deleteTemplate(templateId) function
  - [ ] Apply template to device

- [ ] Task 7: Create MemberDevicesScreen (AC: E12.7.1)
  - [ ] Create MemberDevicesScreen composable
  - [ ] Display LazyColumn of device cards
  - [ ] Show device name, owner, online status indicator
  - [ ] Add "Configure" button per device
  - [ ] Add filter chips (All, Online, Offline)
  - [ ] Add search bar
  - [ ] Navigate to DeviceSettingsScreen on configure

- [ ] Task 8: Create DeviceSettingsScreen (AC: E12.7.2-E12.7.5)
  - [ ] Create DeviceSettingsScreen composable
  - [ ] Group settings by category (collapsible sections)
  - [ ] Show setting name, current value, lock toggle
  - [ ] Add setting editors (Switch, TextField, Slider)
  - [ ] Add "Apply Changes" button (enabled when dirty)
  - [ ] Add "Reset to Defaults" button
  - [ ] Show last synced timestamp
  - [ ] Add "View History" button

- [ ] Task 9: Create BulkSettingsScreen (AC: E12.7.6)
  - [ ] Create BulkSettingsScreen composable
  - [ ] Show device multi-select with checkboxes
  - [ ] Add setting selection (which settings to apply)
  - [ ] Show preview of changes per device
  - [ ] Add progress indicator during bulk update
  - [ ] Show summary card with successes/failures

- [ ] Task 10: Create SettingsTemplateDialog (AC: E12.7.7)
  - [ ] Create SaveTemplateDialog composable
  - [ ] Add template name input
  - [ ] Add setting multi-select checklist
  - [ ] Show preview of template
  - [ ] Create LoadTemplateDialog for applying templates

- [ ] Task 11: Create SettingsHistoryScreen (AC: E12.7.8)
  - [ ] Create SettingsHistoryScreen composable
  - [ ] Display timeline of changes
  - [ ] Show change details (who, when, old/new values)
  - [ ] Add filter by setting or user
  - [ ] Add date range filter
  - [ ] Show lock/unlock actions with different styling

- [ ] Task 12: Create Confirmation Dialogs (AC: E12.7.4, E12.7.5)
  - [ ] Create ApplyChangesDialog with change summary
  - [ ] Create LockSettingDialog with warning
  - [ ] Create BulkUpdateConfirmDialog
  - [ ] Handle confirmations/cancellations

- [ ] Task 13: Update Navigation (AC: E12.7.1, E12.7.2)
  - [ ] Add Screen.MemberDevices(groupId) to sealed class
  - [ ] Add Screen.DeviceSettings(deviceId) to sealed class
  - [ ] Add Screen.BulkSettings(groupId) to sealed class
  - [ ] Add Screen.SettingsHistory(deviceId) to sealed class
  - [ ] Add composable routes in NavHost

- [ ] Task 14: Testing (All ACs)
  - [ ] Write unit tests for AdminSettingsRepository
  - [ ] Write unit tests for DeviceSettingsViewModel
  - [ ] Write unit tests for bulk update logic
  - [ ] Write UI tests for DeviceSettingsScreen
  - [ ] Test setting validation
  - [ ] Test lock/unlock flows
  - [ ] Test bulk application
  - [ ] Test template save/load

## Dev Notes

### Dependencies

**Backend Requirements:**
- Depends on backend E12.2-E12.3 (Settings update, lock management)
- Requires: Admin settings endpoints, bulk update, audit trail

### API Contracts

**Get Device Settings (Admin):**
```kotlin
GET /devices/{deviceId}/settings
Headers: Authorization: Bearer {token}
Response 200: {
  deviceId: "device-123",
  ownerName: "John Smith",
  settings: {
    tracking_enabled: true,
    tracking_interval_seconds: 60,
    secret_mode_enabled: false
  },
  locks: {
    tracking_enabled: {
      isLocked: true,
      lockedBy: "admin@example.com",
      lockedAt: "2025-12-01T10:00:00Z"
    }
  },
  lastSyncedAt: "2025-12-01T12:00:00Z",
  lastModifiedBy: "admin@example.com"
}
```

**Update Device Settings (Admin):**
```kotlin
PUT /devices/{deviceId}/settings
Body: {
  changes: {
    tracking_interval_seconds: 120
  },
  notifyUser: true
}
Response 200: {
  success: true,
  appliedSettings: {...},
  pushSent: true
}
```

**Lock Settings:**
```kotlin
PUT /devices/{deviceId}/settings/locks
Body: {
  settingKeys: ["tracking_enabled", "secret_mode_enabled"],
  lock: true
}
Response 200: { success: true, lockedCount: 2 }
```

**Bulk Update:**
```kotlin
POST /devices/bulk-update
Body: {
  deviceIds: ["device-1", "device-2", "device-3"],
  settings: {
    tracking_interval_seconds: 120
  },
  locks: ["tracking_interval_seconds"]
}
Response 200: {
  successful: ["device-1", "device-2"],
  failed: [
    { deviceId: "device-3", error: "Device offline" }
  ]
}
```

### Validation Rules

- `tracking_interval_seconds`: 10-3600
- `trip_minimum_duration_minutes`: 1-60
- `trip_minimum_distance_meters`: 10-10000
- Boolean settings: true/false only

### UI/UX Considerations

1. **Grouped Settings**: Use expandable categories for organization
2. **Change Highlighting**: Highlight modified settings before applying
3. **Validation Feedback**: Real-time validation with error messages
4. **Bulk Progress**: Show per-device progress during bulk update
5. **History Timeline**: Chronological timeline with color coding

### Security Considerations

1. **Admin Verification**: Backend validates admin role before allowing changes
2. **Audit Logging**: All changes logged with admin identity
3. **Rate Limiting**: Limit bulk operations to prevent abuse
4. **Notification**: Always notify device owner of admin changes

### Files to Create/Modify

**New Files:**
- `app/src/main/java/three/two/bit/phonemanager/data/model/MemberDeviceSettings.kt`
- `app/src/main/java/three/two/bit/phonemanager/data/model/SettingChange.kt`
- `app/src/main/java/three/two/bit/phonemanager/data/model/SettingsTemplate.kt`
- `app/src/main/java/three/two/bit/phonemanager/data/repository/AdminSettingsRepository.kt`
- `app/src/main/java/three/two/bit/phonemanager/ui/admin/MemberDevicesScreen.kt`
- `app/src/main/java/three/two/bit/phonemanager/ui/admin/DeviceSettingsScreen.kt`
- `app/src/main/java/three/two/bit/phonemanager/ui/admin/BulkSettingsScreen.kt`
- `app/src/main/java/three/two/bit/phonemanager/ui/admin/SettingsHistoryScreen.kt`
- `app/src/main/java/three/two/bit/phonemanager/ui/admin/MemberDevicesViewModel.kt`
- `app/src/main/java/three/two/bit/phonemanager/ui/admin/DeviceSettingsViewModel.kt`
- `app/src/main/java/three/two/bit/phonemanager/ui/admin/SettingsTemplateViewModel.kt`
- `app/src/main/java/three/two/bit/phonemanager/ui/admin/SettingsTemplateDialog.kt`

**Modified Files:**
- `app/src/main/java/three/two/bit/phonemanager/network/DeviceApiService.kt`
- `app/src/main/java/three/two/bit/phonemanager/ui/navigation/NavGraph.kt`
- `app/src/main/java/three/two/bit/phonemanager/ui/navigation/Screen.kt`
- `app/src/main/java/three/two/bit/phonemanager/ui/groups/GroupDetailScreen.kt` (add "Manage Devices" button)

### References

- [Source: PRD-user-management.md - Epic 12: Settings Control]
- [Source: SETTINGS_CONTROL_SPEC.md - Admin Settings Management]
- [Source: UI_SCREENS_SPEC.md - Admin UI Designs]

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
**Dependencies**: E11.8 (Group Management), E12.6 (Settings with Locks), Backend E12.2-E12.3
**Blocking**: None (admin-only feature)

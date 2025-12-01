# Story E10.6: Android Device Management UI

**Story ID**: E10.6
**Epic**: 10 - User-Device Binding
**Priority**: High
**Estimate**: 6 story points (2-3 days)
**Status**: Planned
**Created**: 2025-12-01
**PRD Reference**: PRD-user-management.md, DEVICE_MANAGEMENT_SPEC.md
**Dependencies**: E9.11 (Auth UI), Backend E10.1-E10.5

---

## Story

As a user,
I want to manage my linked devices in the Android app,
so that I can view, link, unlink, and transfer ownership of devices associated with my account.

## Acceptance Criteria

### AC E10.6.1: Device List Screen
**Given** a user is authenticated
**When** they navigate to "My Devices" from Settings
**Then** they should see:
  - List of all devices linked to their account
  - Device display name
  - Device ID (partial, e.g., "abc123...xyz789")
  - Last seen timestamp
  - Current device indicator (highlighted)
  - "Link New Device" button
  - Pull-to-refresh functionality

### AC E10.6.2: Device Link Flow
**Given** a user wants to link current device to their account
**When** they tap "Link This Device"
**Then** the system should:
  - Show device link confirmation dialog
  - Call `POST /users/{userId}/devices/{deviceId}/link` endpoint
  - Display success/error message
  - Refresh device list on success
  - Update Settings screen to show account ownership

### AC E10.6.3: Device Detail View
**Given** a user views a device in the list
**When** they tap on a device
**Then** they should see:
  - Full device ID
  - Display name (editable)
  - Group membership
  - Last seen location (optional, privacy toggle)
  - Last activity timestamp
  - "Unlink Device" button
  - "Transfer Ownership" button (if user is owner)

### AC E10.6.4: Device Unlink
**Given** a user wants to remove a device from their account
**When** they tap "Unlink Device" and confirm
**Then** the system should:
  - Show confirmation dialog with warning
  - Call `DELETE /users/{userId}/devices/{deviceId}/unlink` endpoint
  - Remove device from list on success
  - If unlinking current device, clear authentication tokens and return to setup
  - Display error message on failure

### AC E10.6.5: Transfer Ownership
**Given** a user is the device owner
**When** they initiate transfer ownership
**Then** they should:
  - Enter target user email
  - Confirm transfer action
  - Call `POST /devices/{deviceId}/transfer` endpoint
  - See success/error feedback
  - Update ownership indicator if still has access

### AC E10.6.6: Registration Flow Integration
**Given** a user completes authentication
**When** the app checks device link status
**Then** it should:
  - Auto-link current device if not linked
  - Show link prompt if auto-link fails
  - Allow user to skip linking (continue with X-API-Key)
  - Remember skip choice for 30 days

### AC E10.6.7: Backward Compatibility
**Given** existing devices use X-API-Key without accounts
**When** device operates normally
**Then** it should:
  - Show "Sign In to Link Device" option in Settings
  - Not block any functionality
  - Maintain all existing features
  - Show device management UI only after sign-in

## Tasks / Subtasks

- [ ] Task 1: Update Device Models (AC: E10.6.1)
  - [ ] Add `ownerId: String?` to Device model
  - [ ] Add `ownerEmail: String?` to Device model
  - [ ] Add `isLinkedToAccount: Boolean` to Device model
  - [ ] Add `isCurrentDevice: Boolean` helper
  - [ ] Update DeviceRepository to support ownership queries

- [ ] Task 2: Create DeviceApiService Extensions (AC: E10.6.2, E10.6.4, E10.6.5)
  - [ ] Add `linkDevice(userId: String, deviceId: String): Result<Unit>`
  - [ ] Add `unlinkDevice(userId: String, deviceId: String): Result<Unit>`
  - [ ] Add `transferDevice(deviceId: String, targetEmail: String): Result<Unit>`
  - [ ] Add `getUserDevices(userId: String): Result<List<Device>>`
  - [ ] Handle 401/403/404 error responses

- [ ] Task 3: Create DeviceManagementViewModel (AC: All)
  - [ ] Create DeviceManagementViewModel with Hilt
  - [ ] Create DeviceUiState sealed class (Loading, Success, Error, Empty)
  - [ ] Inject DeviceRepository and AuthRepository
  - [ ] Add StateFlow<List<Device>> devices
  - [ ] Add linkCurrentDevice() function
  - [ ] Add unlinkDevice(deviceId) function
  - [ ] Add transferDevice(deviceId, email) function
  - [ ] Add refreshDevices() function
  - [ ] Handle authentication state changes

- [ ] Task 4: Create DeviceListScreen UI (AC: E10.6.1)
  - [ ] Create DeviceListScreen composable
  - [ ] Display LazyColumn of devices with cards
  - [ ] Show device name, partial ID, last seen
  - [ ] Highlight current device with different style
  - [ ] Add "Link This Device" FloatingActionButton
  - [ ] Implement pull-to-refresh (SwipeRefresh)
  - [ ] Show empty state when no devices
  - [ ] Display loading indicator during fetch

- [ ] Task 5: Create DeviceDetailScreen UI (AC: E10.6.3, E10.6.4, E10.6.5)
  - [ ] Create DeviceDetailScreen composable
  - [ ] Show full device information in sections
  - [ ] Add editable display name field
  - [ ] Add "Unlink Device" button with confirmation dialog
  - [ ] Add "Transfer Ownership" button with email input dialog
  - [ ] Show last location on mini map (privacy toggle)
  - [ ] Implement confirmation dialogs for destructive actions

- [ ] Task 6: Create Device Link Dialog (AC: E10.6.2)
  - [ ] Create LinkDeviceDialog composable
  - [ ] Show device info being linked
  - [ ] Explain what linking means (privacy, management)
  - [ ] Add "Link Device" and "Cancel" buttons
  - [ ] Call viewModel.linkCurrentDevice() on confirm
  - [ ] Show progress indicator during linking

- [ ] Task 7: Update SettingsScreen (AC: E10.6.6, E10.6.7)
  - [ ] Show "My Devices" navigation item if authenticated
  - [ ] Show "Link This Device" button if authenticated but not linked
  - [ ] Show device link status indicator
  - [ ] Add "Manage Devices" section for authenticated users
  - [ ] Keep existing functionality for non-authenticated users

- [ ] Task 8: Update Registration/Login Flow (AC: E10.6.6)
  - [ ] After successful auth, check if device is linked
  - [ ] Auto-link device if not linked (call API)
  - [ ] Show link prompt dialog if auto-link fails
  - [ ] Allow user to skip with "Remind me later" option
  - [ ] Store skip choice with 30-day expiry in DataStore
  - [ ] Check skip choice on app startup if authenticated

- [ ] Task 9: Update Navigation (AC: E10.6.1, E10.6.3)
  - [ ] Add Screen.DeviceList to sealed class
  - [ ] Add Screen.DeviceDetail to sealed class
  - [ ] Add composable routes in NavHost
  - [ ] Add navigation from Settings → My Devices
  - [ ] Add navigation DeviceList → DeviceDetail
  - [ ] Handle deep links for device management

- [ ] Task 10: Testing (All ACs)
  - [ ] Write unit tests for DeviceManagementViewModel
  - [ ] Write unit tests for device link/unlink/transfer logic
  - [ ] Write UI tests for DeviceListScreen
  - [ ] Write UI tests for DeviceDetailScreen
  - [ ] Test auto-link flow after authentication
  - [ ] Test backward compatibility (unauthenticated users)
  - [ ] Test unlink current device (clears auth)
  - [ ] Test error handling for network failures

## Dev Notes

### Dependencies

**Backend Requirements:**
- Story depends on backend Epic 10 stories (E10.1-E10.5) being implemented
- Specifically requires: `POST /users/{id}/devices/{id}/link`, `DELETE .../unlink`, `POST /devices/{id}/transfer`, `GET /users/{id}/devices`
- Backend must support device ownership fields and transfer logic

**Frontend Requirements:**
- Story depends on E9.11 (Auth UI) for authentication state
- Requires authenticated user session with valid JWT tokens

### API Contracts

**Link Device Request:**
```kotlin
POST /users/{userId}/devices/{deviceId}/link
Headers: Authorization: Bearer {accessToken}
Response 200: { success: true }
Response 403: { error: "Device already linked to another user" }
Response 404: { error: "User or device not found" }
```

**Unlink Device Request:**
```kotlin
DELETE /users/{userId}/devices/{deviceId}/unlink
Headers: Authorization: Bearer {accessToken}
Response 200: { success: true }
Response 403: { error: "User does not own device" }
```

**Transfer Device Request:**
```kotlin
POST /devices/{deviceId}/transfer
Body: { targetEmail: "user@example.com" }
Headers: Authorization: Bearer {accessToken}
Response 200: { success: true, newOwnerEmail: "user@example.com" }
Response 404: { error: "Target user not found" }
Response 403: { error: "Only owner can transfer device" }
```

**Get User Devices:**
```kotlin
GET /users/{userId}/devices
Headers: Authorization: Bearer {accessToken}
Response 200: {
  devices: [
    {
      deviceId: "abc123-...",
      displayName: "My Phone",
      ownerId: "user-123",
      ownerEmail: "user@example.com",
      groupId: "family",
      lastSeenAt: "2025-12-01T10:00:00Z",
      platform: "android"
    }
  ]
}
```

### UI/UX Considerations

1. **Current Device Highlighting**: Use distinct color/border for current device
2. **Confirmation Dialogs**: Always confirm destructive actions (unlink, transfer)
3. **Privacy**: Don't show last location by default (opt-in toggle)
4. **Empty States**: Show helpful empty state when no devices linked
5. **Error Messages**: User-friendly, actionable error messages
6. **Loading States**: Show progress indicators during API calls

### Security Considerations

1. **Token Validation**: Always validate JWT before device operations
2. **Ownership Checks**: Backend enforces ownership for unlink/transfer
3. **Current Device Protection**: Warn user when unlinking current device
4. **Transfer Confirmation**: Require email + confirmation for transfers

### Testing Strategy

1. **Unit Tests**: ViewModel logic, device link/unlink/transfer flows
2. **Integration Tests**: API service with mock backend
3. **UI Tests**: Device list, detail screens with Compose Testing
4. **E2E Tests**: Complete link/unlink/transfer flows with staging backend
5. **Backward Compat Tests**: Verify unauthenticated users unaffected

### Files to Create/Modify

**New Files:**
- `app/src/main/java/three/two/bit/phonemanager/ui/devices/DeviceListScreen.kt`
- `app/src/main/java/three/two/bit/phonemanager/ui/devices/DeviceDetailScreen.kt`
- `app/src/main/java/three/two/bit/phonemanager/ui/devices/DeviceManagementViewModel.kt`
- `app/src/main/java/three/two/bit/phonemanager/ui/devices/DeviceUiState.kt`
- `app/src/main/java/three/two/bit/phonemanager/ui/devices/LinkDeviceDialog.kt`

**Modified Files:**
- `app/src/main/java/three/two/bit/phonemanager/data/model/Device.kt`
- `app/src/main/java/three/two/bit/phonemanager/network/DeviceApiService.kt`
- `app/src/main/java/three/two/bit/phonemanager/data/repository/DeviceRepository.kt`
- `app/src/main/java/three/two/bit/phonemanager/ui/navigation/NavGraph.kt`
- `app/src/main/java/three/two/bit/phonemanager/ui/navigation/Screen.kt`
- `app/src/main/java/three/two/bit/phonemanager/ui/settings/SettingsScreen.kt`
- `app/src/main/java/three/two/bit/phonemanager/ui/auth/AuthViewModel.kt` (add auto-link logic)

### References

- [Source: PRD-user-management.md - Epic 10: User-Device Binding]
- [Source: DEVICE_MANAGEMENT_SPEC.md - Device Linking Implementation]
- [Source: UI_SCREENS_SPEC.md - Device Management UI Designs]

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
| 2025-12-01 | Martin (PM) | Story created from Epic 10 specification |

---

**Last Updated**: 2025-12-01
**Status**: Planned
**Dependencies**: E9.11 (Auth UI), Backend E10.1-E10.5 must be implemented first
**Blocking**: Epic 11 Group Management UI cannot proceed without device-user binding

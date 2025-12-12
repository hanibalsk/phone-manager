# Story E12.8: Android Unlock Request UI

**Story ID**: E12.8
**Epic**: 12 - Settings Control
**Priority**: High
**Estimate**: 4 story points (1-2 days)
**Status**: Implemented
**Created**: 2025-12-01
**Implemented**: 2025-12-10
**PRD Reference**: PRD-user-management.md, SETTINGS_CONTROL_SPEC.md
**Dependencies**: E12.6 (Settings with Locks), Backend E12.4

---

## Story

As a user,
I want to request unlock for locked settings,
so that I can regain control of my device configuration when needed.

## Acceptance Criteria

### AC E12.8.1: Request Unlock from Locked Setting
**Given** user taps on a locked setting
**When** "Request Unlock" dialog appears
**Then** they should see:
  - Setting name
  - Current value
  - "Reason for request" text field
  - Character count (max 200)
  - "Send Request" button
  - "Cancel" button

### AC E12.8.2: Submit Unlock Request
**Given** user fills reason and taps "Send Request"
**When** request is submitted
**Then** the system should:
  - Validate reason is not empty
  - Call `POST /devices/{deviceId}/settings/unlock-requests` endpoint
  - Show success message: "Request sent to admin"
  - Close dialog
  - Add request to "My Requests" list
  - Send push notification to group admins

### AC E12.8.3: View My Unlock Requests
**Given** user has submitted unlock requests
**When** they navigate to "Unlock Requests" from Settings
**Then** they should see:
  - List of all requests (pending, approved, denied)
  - Setting name per request
  - Request reason
  - Status badge (pending, approved, denied)
  - Submitted timestamp
  - Admin response (if any)
  - "Withdraw Request" button for pending requests

### AC E12.8.4: Withdraw Unlock Request
**Given** user has a pending request
**When** they tap "Withdraw Request"
**Then** the system should:
  - Show confirmation dialog
  - Call `DELETE /unlock-requests/{requestId}` endpoint
  - Remove request from list
  - Show success message

### AC E12.8.5: Request Status Notifications
**Given** admin approves or denies request
**When** push notification is received
**Then** the app should:
  - Show notification: "Request for {setting} {approved/denied}"
  - Update request status in list
  - If approved, unlock setting immediately
  - Show admin's response message if provided

### AC E12.8.6: Admin Response Display
**Given** request was approved or denied
**When** user views request detail
**Then** they should see:
  - Admin's decision (approved/denied)
  - Admin's response message
  - Decision timestamp
  - Admin name
  - If approved: setting is now unlocked indicator

### AC E12.8.7: Request History
**Given** user views unlock requests
**Then** they should be able to:
  - Filter by status (all, pending, approved, denied)
  - Sort by date (newest first by default)
  - View request history for last 30 days
  - See which admin responded

### AC E12.8.8: Setting Auto-Unlock on Approval
**Given** admin approves unlock request
**When** app receives approval notification
**Then** the system should:
  - Update local lock state (remove lock)
  - Refresh Settings screen if open
  - Allow setting modification immediately
  - Show snackbar: "{Setting} unlocked by {admin}"

## Tasks / Subtasks

- [x] Task 1: Create Unlock Request Models (AC: E12.8.1-E12.8.3)
  - [x] Create UnlockRequest data class (id, deviceId, settingKey, reason, status, requestedBy, respondedBy, response, createdAt, respondedAt)
  - [x] Create UnlockRequestStatus enum (PENDING, APPROVED, DENIED, WITHDRAWN)
  - [x] Add helper methods: isPending(), canWithdraw()

- [x] Task 2: Extend DeviceApiService (AC: E12.8.2, E12.8.4)
  - [x] Add `POST /devices/{id}/settings/unlock-requests` - Create request
  - [x] Add `GET /devices/{id}/settings/unlock-requests` - List user's requests
  - [x] Add `DELETE /unlock-requests/{id}` - Withdraw request
  - [x] Handle validation errors (empty reason)

- [x] Task 3: Create UnlockRequestRepository (AC: All)
  - [x] Implement createUnlockRequest(settingKey, reason) function
  - [x] Implement getUserUnlockRequests() function
  - [x] Implement withdrawRequest(requestId) function
  - [x] Implement getRequestStatus(requestId) function
  - [x] Cache requests locally (Room database)

- [x] Task 4: Create UnlockRequestViewModel (AC: E12.8.3-E12.8.7)
  - [x] Create UnlockRequestViewModel with Hilt
  - [x] Create UnlockRequestUiState sealed class
  - [x] Add StateFlow<List<UnlockRequest>> requests
  - [x] Add createRequest(settingKey, reason) function
  - [x] Add withdrawRequest(requestId) function
  - [x] Add filterByStatus(status) function
  - [x] Add sortByDate() function
  - [x] Load user's requests on init

- [x] Task 5: Create RequestUnlockDialog (AC: E12.8.1, E12.8.2)
  - [x] Create RequestUnlockDialog composable
  - [x] Show setting name prominently
  - [x] Show current setting value
  - [x] Add multiline TextField for reason
  - [x] Show character count (200 max)
  - [x] Add "Send Request" button (disabled if empty)
  - [x] Add "Cancel" button
  - [x] Call viewModel.createRequest()

- [x] Task 6: Create UnlockRequestsScreen (AC: E12.8.3, E12.8.4)
  - [x] Create UnlockRequestsScreen composable
  - [x] Display LazyColumn of request cards
  - [x] Show setting name, status badge, timestamp
  - [x] Add expandable detail with reason/response
  - [x] Add "Withdraw" button for pending requests
  - [x] Add filter chips (All, Pending, Approved, Denied)
  - [x] Show empty state if no requests

- [x] Task 7: Create RequestDetailCard (AC: E12.8.6)
  - [x] Create RequestDetailCard composable
  - [x] Show request info (setting, reason, timestamp)
  - [x] Show admin response section if decided
  - [x] Show decision (approved/denied) with color coding
  - [x] Show admin name and response message
  - [x] Show decision timestamp

- [x] Task 8: Implement FCM Handler for Responses (AC: E12.8.5, E12.8.8)
  - [x] Extend FirebaseMessagingService
  - [x] Parse "unlock_request_response" message type
  - [x] Extract request ID, status, admin response
  - [x] Update UnlockRequestRepository
  - [x] If approved, call SettingsSyncRepository to unlock
  - [x] Show notification with status
  - [x] Update UI if UnlockRequestsScreen is open

- [x] Task 9: Integrate with SettingLockedDialog (AC: E12.8.1)
  - [x] Update SettingLockedDialog from E12.6
  - [x] Add "Request Unlock" button
  - [x] Navigate to RequestUnlockDialog on tap
  - [x] Pass setting key and current value

- [x] Task 10: Update SettingsScreen Navigation (AC: E12.8.3)
  - [x] Add "Unlock Requests" menu item in Settings
  - [x] Show badge with pending request count
  - [x] Navigate to UnlockRequestsScreen

- [ ] Task 11: Testing (All ACs)
  - [x] Write unit tests for UnlockRequestRepository
  - [x] Write unit tests for UnlockRequestViewModel
  - [ ] Write UI tests for RequestUnlockDialog
  - [ ] Write UI tests for UnlockRequestsScreen
  - [ ] Test request creation flow
  - [ ] Test request withdrawal
  - [ ] Test FCM response handling
  - [ ] Test auto-unlock on approval

## Dev Notes

### Dependencies

**Backend Requirements:**
- Depends on backend E12.4 (Unlock request workflow)
- Requires: Request CRUD, approval/denial endpoints, push notifications to admins

**FCM Setup:**
- Handle "unlock_request_response" message type
- Parse approval/denial with admin response

### API Contracts

**Create Unlock Request:**
```kotlin
POST /devices/{deviceId}/settings/unlock-requests
Body: {
  settingKey: "tracking_enabled",
  reason: "Need to disable tracking while at private event"
}
Response 201: {
  id: "request-123",
  deviceId: "device-456",
  settingKey: "tracking_enabled",
  reason: "Need to disable tracking while at private event",
  status: "pending",
  requestedBy: "user@example.com",
  createdAt: "2025-12-01T10:00:00Z"
}
```

**Get User's Requests:**
```kotlin
GET /devices/{deviceId}/settings/unlock-requests
Response 200: {
  requests: [
    {
      id: "request-123",
      settingKey: "tracking_enabled",
      reason: "...",
      status: "approved",
      requestedAt: "2025-12-01T10:00:00Z",
      respondedBy: "admin@example.com",
      response: "Approved for this weekend",
      respondedAt: "2025-12-01T11:00:00Z"
    }
  ]
}
```

**Withdraw Request:**
```kotlin
DELETE /unlock-requests/{requestId}
Response 200: { success: true }
Response 404: { error: "Request not found or already processed" }
```

**FCM Push Payload (Response):**
```json
{
  "message_type": "unlock_request_response",
  "request_id": "request-123",
  "setting_key": "tracking_enabled",
  "status": "approved",
  "admin_name": "John Admin",
  "response_message": "Approved for this weekend",
  "responded_at": "2025-12-01T11:00:00Z"
}
```

### UI/UX Considerations

1. **Status Colors**: Pending (yellow), Approved (green), Denied (red), Withdrawn (gray)
2. **Character Limit**: 200 characters for reason (enforce with counter)
3. **Admin Response**: Show prominently with distinct styling
4. **Empty State**: "No unlock requests yet"
5. **Badge**: Show pending count on Settings menu item

### Security Considerations

1. **Request Validation**: Backend validates user owns device
2. **Rate Limiting**: Limit requests per user (e.g., 10 per day)
3. **Spam Prevention**: Minimum 5 characters for reason
4. **Audit Trail**: Log all request actions

### Files to Create/Modify

**New Files:**
- `app/src/main/java/three/two/bit/phonemanager/data/model/UnlockRequest.kt`
- `app/src/main/java/three/two/bit/phonemanager/data/model/UnlockRequestStatus.kt`
- `app/src/main/java/three/two/bit/phonemanager/data/repository/UnlockRequestRepository.kt`
- `app/src/main/java/three/two/bit/phonemanager/ui/settings/UnlockRequestsScreen.kt`
- `app/src/main/java/three/two/bit/phonemanager/ui/settings/RequestUnlockDialog.kt`
- `app/src/main/java/three/two/bit/phonemanager/ui/settings/RequestDetailCard.kt`
- `app/src/main/java/three/two/bit/phonemanager/ui/settings/UnlockRequestViewModel.kt`
- `app/src/main/java/three/two/bit/phonemanager/fcm/UnlockRequestResponseHandler.kt`

**Modified Files:**
- `app/src/main/java/three/two/bit/phonemanager/network/DeviceApiService.kt`
- `app/src/main/java/three/two/bit/phonemanager/ui/settings/SettingsScreen.kt`
- `app/src/main/java/three/two/bit/phonemanager/ui/settings/SettingLockedDialog.kt`
- `app/src/main/java/three/two/bit/phonemanager/ui/navigation/NavGraph.kt`
- `app/src/main/java/three/two/bit/phonemanager/ui/navigation/Screen.kt`
- `app/src/main/java/three/two/bit/phonemanager/data/repository/SettingsSyncRepository.kt` (unlock on approval)

### References

- [Source: PRD-user-management.md - Epic 12: Settings Control]
- [Source: SETTINGS_CONTROL_SPEC.md - Unlock Request Implementation]
- [Source: UI_SCREENS_SPEC.md - Unlock Request UI]

---

## Dev Agent Record

### Debug Log

_To be filled during implementation_

### Completion Notes

_To be filled after implementation_

---

## File List

### Created Files

- `app/src/main/java/three/two/bit/phonemanager/domain/model/UnlockRequestModels.kt`
- `app/src/main/java/three/two/bit/phonemanager/network/models/UnlockRequestApiModels.kt`
- `app/src/main/java/three/two/bit/phonemanager/data/repository/UnlockRequestRepository.kt`
- `app/src/main/java/three/two/bit/phonemanager/ui/unlock/UnlockRequestViewModel.kt`
- `app/src/main/java/three/two/bit/phonemanager/ui/unlock/UnlockRequestsScreen.kt`
- `app/src/main/java/three/two/bit/phonemanager/ui/unlock/RequestUnlockDialog.kt`
- `app/src/test/java/three/two/bit/phonemanager/data/repository/UnlockRequestRepositoryTest.kt`
- `app/src/test/java/three/two/bit/phonemanager/ui/unlock/UnlockRequestViewModelTest.kt`

### Modified Files

- `app/src/main/java/three/two/bit/phonemanager/network/DeviceApiService.kt`
- `app/src/main/java/three/two/bit/phonemanager/ui/settings/SettingsScreen.kt`
- `app/src/main/java/three/two/bit/phonemanager/ui/settings/SettingLockedDialog.kt`
- `app/src/main/java/three/two/bit/phonemanager/ui/navigation/NavGraph.kt`
- `app/src/main/java/three/two/bit/phonemanager/ui/navigation/Screen.kt`
- `app/src/main/java/three/two/bit/phonemanager/data/repository/SettingsSyncRepository.kt`
- `app/src/main/java/three/two/bit/phonemanager/service/SettingsMessagingService.kt`

---

## Change Log

| Date | Author | Changes |
|------|--------|---------|
| 2025-12-01 | Martin (PM) | Story created from Epic 12 specification |
| 2025-12-10 | Dev | Implementation completed - unlock request UI and FCM handler |

---

**Last Updated**: 2025-12-10
**Status**: Implemented
**Dependencies**: E12.6 (Settings with Locks), Backend E12.4
**Blocking**: None (user feature, complements admin settings)

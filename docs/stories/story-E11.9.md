# Story E11.9: Android Invite Sharing UI

**Story ID**: E11.9
**Epic**: 11 - Group Management
**Priority**: High
**Estimate**: 5 story points (2 days)
**Status**: Planned
**Created**: 2025-12-01
**PRD Reference**: PRD-user-management.md, GROUP_MANAGEMENT_SPEC.md
**Dependencies**: E11.8 (Group List/Detail), Backend E11.5-E11.6

---

## Story

As a group admin,
I want to invite others to join my group,
so that I can easily onboard new members with secure invite codes.

## Acceptance Criteria

### AC E11.9.1: Generate Invite Code
**Given** user is admin or owner
**When** they tap "Invite Members" in group detail
**Then** the system should:
  - Call `POST /groups/{groupId}/invites` endpoint
  - Generate unique 8-character invite code
  - Set expiry time (default: 7 days)
  - Display invite code prominently
  - Show QR code representation
  - Provide share options

### AC E11.9.2: Invite Code Display
**Given** an invite code is generated
**When** viewing the invite screen
**Then** it should show:
  - Large, easy-to-read invite code
  - QR code for scanning
  - "Copy Code" button
  - "Share via..." button (SMS, email, messaging apps)
  - Expiry countdown timer
  - "Revoke Invite" button
  - List of pending invites

### AC E11.9.3: Share Invite
**Given** user wants to share invite
**When** they tap "Share via..."
**Then** Android share sheet should open with:
  - Pre-formatted message: "Join my Phone Manager group '{groupName}' with code: {code}"
  - Deep link: phonemanager://join/{code}
  - Available sharing apps (SMS, WhatsApp, Email, etc.)

### AC E11.9.4: Join with Invite Code
**Given** user receives an invite code
**When** they tap "Join Group" in app
**Then** they should see:
  - Code input field (8 characters, uppercase)
  - QR code scanner button
  - "Join" button
  - Group preview (name, member count) after code validation
  - Join confirmation dialog

### AC E11.9.5: QR Code Scanning
**Given** user has invite QR code
**When** they tap QR scanner button
**Then** the system should:
  - Request camera permission if needed
  - Open camera viewfinder
  - Detect and parse QR code
  - Auto-fill invite code field
  - Validate code immediately
  - Show group preview on success

### AC E11.9.6: Pending Invites List
**Given** group has active invites
**When** admin views invite management
**Then** they should see:
  - List of pending invites with codes
  - Created date and expiry time
  - Number of uses (if multi-use enabled)
  - "Revoke" button per invite
  - "Create New Invite" button

### AC E11.9.7: Revoke Invite
**Given** admin wants to invalidate invite
**When** they tap "Revoke" on an invite
**Then** the system should:
  - Show confirmation dialog
  - Call `DELETE /groups/{groupId}/invites/{inviteId}` endpoint
  - Remove invite from list
  - Display success message

### AC E11.9.8: Deep Link Handling
**Given** user clicks phonemanager://join/{code} link
**When** the app opens
**Then** it should:
  - Navigate to Join Group screen
  - Pre-fill invite code
  - Validate code automatically
  - Show group preview if valid
  - Prompt sign-in if not authenticated

## Tasks / Subtasks

- [ ] Task 1: Create Invite Models (AC: E11.9.1, E11.9.6)
  - [ ] Create GroupInvite data class (id, groupId, code, createdBy, expiresAt, usesRemaining)
  - [ ] Add invite status enum (ACTIVE, EXPIRED, REVOKED, USED)
  - [ ] Add helper methods: isExpired(), isValid()

- [ ] Task 2: Extend GroupApiService (AC: All)
  - [ ] Add `POST /groups/{id}/invites` - Create invite
  - [ ] Add `GET /groups/{id}/invites` - List invites
  - [ ] Add `DELETE /groups/{id}/invites/{inviteId}` - Revoke invite
  - [ ] Add `POST /invites/{code}/validate` - Validate code
  - [ ] Add `POST /invites/{code}/join` - Join with code

- [ ] Task 3: Extend GroupRepository (AC: All)
  - [ ] Implement createInvite(groupId, expiryDays) function
  - [ ] Implement getGroupInvites(groupId) function
  - [ ] Implement revokeInvite(inviteId) function
  - [ ] Implement validateInviteCode(code) function
  - [ ] Implement joinWithInvite(code) function

- [ ] Task 4: Create InviteViewModel (AC: E11.9.1-E11.9.3, E11.9.6-E11.9.7)
  - [ ] Create InviteViewModel with Hilt
  - [ ] Create InviteUiState sealed class
  - [ ] Add StateFlow<List<GroupInvite>> invites
  - [ ] Add createInvite(groupId) function
  - [ ] Add revokeInvite(inviteId) function
  - [ ] Add shareInvite(code, groupName) function
  - [ ] Handle expiry countdown timer

- [ ] Task 5: Create JoinGroupViewModel (AC: E11.9.4, E11.9.5, E11.9.8)
  - [ ] Create JoinGroupViewModel with Hilt
  - [ ] Add StateFlow<String> inviteCode
  - [ ] Add StateFlow<Group?> groupPreview
  - [ ] Add validateCode(code) function
  - [ ] Add joinGroup() function
  - [ ] Handle QR code parsing
  - [ ] Handle deep link navigation

- [ ] Task 6: Create InviteMembersScreen (AC: E11.9.1, E11.9.2, E11.9.3)
  - [ ] Create InviteMembersScreen composable
  - [ ] Show generated invite code in large font
  - [ ] Generate and display QR code (use QR library)
  - [ ] Add "Copy Code" button with clipboard API
  - [ ] Add "Share" button triggering Android share sheet
  - [ ] Show expiry countdown timer
  - [ ] Add "Generate New Code" button

- [ ] Task 7: Create PendingInvitesScreen (AC: E11.9.6, E11.9.7)
  - [ ] Create PendingInvitesScreen composable
  - [ ] Display LazyColumn of invite cards
  - [ ] Show code, created date, expiry, uses remaining
  - [ ] Add "Revoke" button per invite
  - [ ] Add "Create New Invite" FAB
  - [ ] Handle revocation confirmation

- [ ] Task 8: Create JoinGroupScreen (AC: E11.9.4, E11.9.5)
  - [ ] Create JoinGroupScreen composable
  - [ ] Add invite code TextField (8 chars, auto-uppercase)
  - [ ] Add QR scanner button
  - [ ] Show group preview card when code valid
  - [ ] Add "Join Group" button
  - [ ] Show error for invalid codes
  - [ ] Handle authentication requirement

- [ ] Task 9: Implement QR Code Generation (AC: E11.9.2)
  - [ ] Add ZXing dependency for QR generation
  - [ ] Create QRCodeGenerator utility
  - [ ] Generate QR with phonemanager://join/{code} format
  - [ ] Display QR as Bitmap in Image composable
  - [ ] Handle QR generation errors

- [ ] Task 10: Implement QR Code Scanning (AC: E11.9.5)
  - [ ] Add CameraX and ML Kit Barcode dependencies
  - [ ] Create QRScannerScreen with camera preview
  - [ ] Request camera permission
  - [ ] Detect and parse QR codes
  - [ ] Extract invite code from deep link
  - [ ] Navigate back with code on success

- [ ] Task 11: Implement Share Functionality (AC: E11.9.3)
  - [ ] Create ShareHelper utility
  - [ ] Format share message with group name and code
  - [ ] Create Android Intent.ACTION_SEND
  - [ ] Handle share cancellation
  - [ ] Log share events for analytics

- [ ] Task 12: Implement Deep Link Handling (AC: E11.9.8)
  - [ ] Add deep link intent filter to AndroidManifest
  - [ ] Handle phonemanager://join/{code} scheme
  - [ ] Parse code from URI in MainActivity
  - [ ] Navigate to JoinGroupScreen with code
  - [ ] Handle unauthenticated users (prompt sign-in)

- [ ] Task 13: Update Navigation (AC: E11.9.1, E11.9.4, E11.9.6)
  - [ ] Add Screen.InviteMembers(groupId) to sealed class
  - [ ] Add Screen.PendingInvites(groupId) to sealed class
  - [ ] Add Screen.JoinGroup(code?) to sealed class
  - [ ] Add Screen.QRScanner to sealed class
  - [ ] Add composable routes in NavHost
  - [ ] Handle deep link navigation

- [ ] Task 14: Testing (All ACs)
  - [ ] Write unit tests for InviteViewModel
  - [ ] Write unit tests for JoinGroupViewModel
  - [ ] Write unit tests for QRCodeGenerator
  - [ ] Write UI tests for InviteMembersScreen
  - [ ] Write UI tests for JoinGroupScreen
  - [ ] Test QR scanning flow
  - [ ] Test share functionality
  - [ ] Test deep link handling

## Dev Notes

### Dependencies

**Gradle Dependencies to Add:**
```kotlin
// QR Code generation
implementation("com.google.zxing:core:3.5.2")

// QR Code scanning
implementation("androidx.camera:camera-camera2:1.3.0")
implementation("androidx.camera:camera-lifecycle:1.3.0")
implementation("androidx.camera:camera-view:1.3.0")
implementation("com.google.mlkit:barcode-scanning:17.2.0")
```

**Backend Requirements:**
- Depends on backend E11.5-E11.6 (Invite CRUD, Join with code)
- Requires: Invite generation, validation, expiry, multi-use support

### API Contracts

**Create Invite:**
```kotlin
POST /groups/{groupId}/invites
Body: {
  expiryDays: 7,
  maxUses: 1  // -1 for unlimited
}
Response 201: {
  id: "invite-123",
  code: "ABC12XYZ",
  groupId: "group-456",
  expiresAt: "2025-12-08T10:00:00Z",
  usesRemaining: 1
}
```

**Validate Invite Code:**
```kotlin
POST /invites/{code}/validate
Response 200: {
  valid: true,
  group: {
    id: "group-456",
    name: "Smith Family",
    memberCount: 4
  }
}
Response 404: { valid: false, error: "Invalid or expired code" }
```

**Join with Invite:**
```kotlin
POST /invites/{code}/join
Headers: Authorization: Bearer {token}
Response 200: {
  groupId: "group-456",
  role: "member",
  joinedAt: "2025-12-01T10:00:00Z"
}
Response 403: { error: "Already a member" }
```

### UI/UX Considerations

1. **QR Code Size**: Generate 300x300px QR code for easy scanning
2. **Code Input**: Auto-format to uppercase, auto-advance on 8 chars
3. **Share Message**: Pre-format message for easy copying
4. **Camera Permission**: Handle denial gracefully with manual input fallback
5. **Expiry Indicator**: Show visual countdown (< 24h: yellow, < 1h: red)

### Security Considerations

1. **Code Uniqueness**: Backend ensures globally unique codes
2. **Expiry Enforcement**: Backend validates expiry on join attempt
3. **Rate Limiting**: Limit invite creation per user/group
4. **Code Entropy**: 8-character alphanumeric = ~2.8 trillion combinations

### Files to Create/Modify

**New Files:**
- `app/src/main/java/three/two/bit/phonemanager/data/model/GroupInvite.kt`
- `app/src/main/java/three/two/bit/phonemanager/ui/groups/InviteMembersScreen.kt`
- `app/src/main/java/three/two/bit/phonemanager/ui/groups/PendingInvitesScreen.kt`
- `app/src/main/java/three/two/bit/phonemanager/ui/groups/JoinGroupScreen.kt`
- `app/src/main/java/three/two/bit/phonemanager/ui/groups/QRScannerScreen.kt`
- `app/src/main/java/three/two/bit/phonemanager/ui/groups/InviteViewModel.kt`
- `app/src/main/java/three/two/bit/phonemanager/ui/groups/JoinGroupViewModel.kt`
- `app/src/main/java/three/two/bit/phonemanager/util/QRCodeGenerator.kt`
- `app/src/main/java/three/two/bit/phonemanager/util/ShareHelper.kt`

**Modified Files:**
- `app/src/main/java/three/two/bit/phonemanager/network/GroupApiService.kt`
- `app/src/main/java/three/two/bit/phonemanager/data/repository/GroupRepository.kt`
- `app/src/main/java/three/two/bit/phonemanager/ui/navigation/NavGraph.kt`
- `app/src/main/java/three/two/bit/phonemanager/ui/navigation/Screen.kt`
- `AndroidManifest.xml` (deep link intent filter)

### References

- [Source: PRD-user-management.md - Epic 11: Group Management]
- [Source: GROUP_MANAGEMENT_SPEC.md - Invite Sharing Implementation]
- [Source: UI_SCREENS_SPEC.md - Invite UI Designs]

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
| 2025-12-01 | Martin (PM) | Story created from Epic 11 specification |

---

**Last Updated**: 2025-12-01
**Status**: Planned
**Dependencies**: E11.8 (Group List/Detail), Backend E11.5-E11.6
**Blocking**: None (optional feature)

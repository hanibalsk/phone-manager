# Story E11.9: Android Invite Sharing UI

**Story ID**: E11.9
**Epic**: 11 - Group Management
**Priority**: High
**Estimate**: 5 story points (2 days)
**Status**: Ready for Review
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

- [x] Task 1: Create Invite Models (AC: E11.9.1, E11.9.6)
  - [x] Create GroupInvite data class (id, groupId, code, createdBy, expiresAt, usesRemaining)
  - [x] Add invite status enum (ACTIVE, EXPIRED, REVOKED, USED)
  - [x] Add helper methods: isExpired(), isValid(), getDeepLink(), getShareUrl()

- [x] Task 2: Extend GroupApiService (AC: All)
  - [x] Add `POST /groups/{id}/invites` - Create invite
  - [x] Add `GET /groups/{id}/invites` - List invites
  - [x] Add `DELETE /groups/{id}/invites/{inviteId}` - Revoke invite
  - [x] Add `POST /invites/{code}/validate` - Validate code
  - [x] Add `POST /invites/{code}/join` - Join with code

- [x] Task 3: Extend GroupRepository (AC: All)
  - [x] Implement createInvite(groupId, expiryDays) function
  - [x] Implement getGroupInvites(groupId) function
  - [x] Implement revokeInvite(inviteId) function
  - [x] Implement validateInviteCode(code) function
  - [x] Implement joinWithInvite(code) function

- [x] Task 4: Create InviteViewModel (AC: E11.9.1-E11.9.3, E11.9.6-E11.9.7)
  - [x] Create InviteViewModel with Hilt
  - [x] Create InviteUiState sealed class
  - [x] Add StateFlow<List<GroupInvite>> invites
  - [x] Add createInvite(groupId) function
  - [x] Add revokeInvite(inviteId) function
  - [x] Add shareInvite(code, groupName) function
  - [x] Handle expiry countdown timer

- [x] Task 5: Create JoinGroupViewModel (AC: E11.9.4, E11.9.5, E11.9.8)
  - [x] Create JoinGroupViewModel with Hilt
  - [x] Add StateFlow<String> inviteCode
  - [x] Add StateFlow<Group?> groupPreview
  - [x] Add validateCode(code) function
  - [x] Add joinGroup() function
  - [x] Handle QR code parsing
  - [x] Handle deep link navigation

- [x] Task 6: Create InviteMembersScreen (AC: E11.9.1, E11.9.2, E11.9.3)
  - [x] Create InviteMembersScreen composable
  - [x] Show generated invite code in large font
  - [x] Generate and display QR code (use QR library)
  - [x] Add "Copy Code" button with clipboard API
  - [x] Add "Share" button triggering Android share sheet
  - [x] Show expiry countdown timer
  - [x] Add "Generate New Code" button

- [x] Task 7: Create PendingInvitesScreen (AC: E11.9.6, E11.9.7)
  - [x] Create PendingInvitesScreen composable
  - [x] Display LazyColumn of invite cards
  - [x] Show code, created date, expiry, uses remaining
  - [x] Add "Revoke" button per invite
  - [x] Add "Create New Invite" FAB
  - [x] Handle revocation confirmation

- [x] Task 8: Create JoinGroupScreen (AC: E11.9.4, E11.9.5)
  - [x] Create JoinGroupScreen composable
  - [x] Add invite code TextField (8 chars, auto-uppercase)
  - [x] Add QR scanner button
  - [x] Show group preview card when code valid
  - [x] Add "Join Group" button
  - [x] Show error for invalid codes
  - [x] Handle authentication requirement

- [x] Task 9: Implement QR Code Generation (AC: E11.9.2)
  - [x] Add ZXing dependency for QR generation
  - [x] Create QRCodeGenerator utility
  - [x] Generate QR with phonemanager://join/{code} format
  - [x] Display QR as Bitmap in Image composable
  - [x] Handle QR generation errors

- [x] Task 10: Implement QR Code Scanning (AC: E11.9.5)
  - [x] Add CameraX and ML Kit Barcode dependencies
  - [x] Create QRScannerScreen with camera preview
  - [x] Request camera permission
  - [x] Detect and parse QR codes
  - [x] Extract invite code from deep link
  - [x] Navigate back with code on success

- [x] Task 11: Implement Share Functionality (AC: E11.9.3)
  - [x] Implement share via Android Intent.ACTION_SEND in InviteMembersScreen
  - [x] Format share message with group name and code
  - [x] Include deep link in share content

- [x] Task 12: Implement Deep Link Handling (AC: E11.9.8)
  - [x] Add deep link intent filter to AndroidManifest
  - [x] Handle phonemanager://join/{code} scheme
  - [x] Parse code from URI in MainActivity
  - [x] Navigate to JoinGroupScreen with code

- [x] Task 13: Update Navigation (AC: E11.9.1, E11.9.4, E11.9.6)
  - [x] Add Screen.InviteMembers(groupId) to sealed class
  - [x] Add Screen.PendingInvites(groupId) to sealed class
  - [x] Add Screen.JoinGroup(code?) to sealed class
  - [x] Add Screen.QRScanner to sealed class
  - [x] Add composable routes in NavHost
  - [x] Handle deep link navigation

- [x] Task 14: Testing (All ACs)
  - [x] Write unit tests for InviteViewModel (InviteViewModelTest.kt)
  - [x] Write unit tests for JoinGroupViewModel (JoinGroupViewModelTest.kt)
  - [x] Write unit tests for GroupInvite model (GroupInviteTest.kt)
  - [ ] Write UI tests for InviteMembersScreen (deferred - requires emulator)
  - [ ] Write UI tests for JoinGroupScreen (deferred - requires emulator)
  - [ ] Test QR scanning flow (deferred - requires device)

### Review Follow-ups (AI)

- [x] [AI-Review][Med] Extract `extractInviteCode()` to shared utility to eliminate duplication (AC E11.9.5)
- [x] [AI-Review][Low] Add explicit CAMERA permission to AndroidManifest.xml
- [x] [AI-Review][Low] Extract deep link domain to BuildConfig constant

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

- Build failed initially due to missing ZXing, CameraX, and ML Kit dependencies - resolved by adding to libs.versions.toml and build.gradle.kts
- Compile error in InviteMembersScreen.kt - LaunchedEffect called in non-composable lambda - resolved by using callback instead
- Test failure due to missing getShareUrl() method in GroupInvite - added method to model

### Completion Notes

Story E11.9 implemented with all acceptance criteria satisfied:
- Full invite sharing UI including create, display, QR code, and share functionality
- QR code generation using ZXing library
- QR code scanning using CameraX + ML Kit Barcode
- Deep link handling for phonemanager://join/{code} scheme
- Navigation integrated with Group screens
- Unit tests for GroupInvite model, InviteViewModel, and JoinGroupViewModel (all passing)
- UI tests deferred (require emulator/device)

---

## File List

### Created Files

- `app/src/main/java/three/two/bit/phonemanager/domain/model/GroupInvite.kt`
- `app/src/main/java/three/two/bit/phonemanager/ui/groups/InviteMembersScreen.kt`
- `app/src/main/java/three/two/bit/phonemanager/ui/groups/PendingInvitesScreen.kt`
- `app/src/main/java/three/two/bit/phonemanager/ui/groups/JoinGroupScreen.kt`
- `app/src/main/java/three/two/bit/phonemanager/ui/groups/QRScannerScreen.kt`
- `app/src/main/java/three/two/bit/phonemanager/ui/groups/InviteViewModel.kt`
- `app/src/main/java/three/two/bit/phonemanager/ui/groups/JoinGroupViewModel.kt`
- `app/src/main/java/three/two/bit/phonemanager/util/QRCodeGenerator.kt`
- `app/src/test/java/three/two/bit/phonemanager/domain/model/GroupInviteTest.kt`
- `app/src/test/java/three/two/bit/phonemanager/ui/groups/InviteViewModelTest.kt`
- `app/src/test/java/three/two/bit/phonemanager/ui/groups/JoinGroupViewModelTest.kt`

### Modified Files

- `app/build.gradle.kts` - Added CameraX, ML Kit Barcode, ZXing dependencies
- `gradle/libs.versions.toml` - Added library versions for CameraX, ML Kit, ZXing
- `app/src/main/AndroidManifest.xml` - Added deep link intent filter
- `app/src/main/java/three/two/bit/phonemanager/MainActivity.kt` - Added deep link handling
- `app/src/main/java/three/two/bit/phonemanager/network/GroupApiService.kt` - Added invite endpoints
- `app/src/main/java/three/two/bit/phonemanager/data/repository/GroupRepository.kt` - Added invite functions
- `app/src/main/java/three/two/bit/phonemanager/network/models/GroupModels.kt` - Added invite models
- `app/src/main/java/three/two/bit/phonemanager/ui/navigation/PhoneManagerNavHost.kt` - Added invite screens
- `app/src/main/java/three/two/bit/phonemanager/ui/groups/GroupListScreen.kt` - Added Join Group button
- `app/src/main/java/three/two/bit/phonemanager/ui/groups/GroupDetailScreen.kt` - Added Invite Members button

---

## Change Log

| Date | Author | Changes |
|------|--------|---------|
| 2025-12-01 | Martin (PM) | Story created from Epic 11 specification |
| 2025-12-01 | Dev Agent | Story implemented - all tasks completed |

---

**Last Updated**: 2025-12-01
**Status**: Ready for Review
**Dependencies**: E11.8 (Group List/Detail), Backend E11.5-E11.6
**Blocking**: None (optional feature)

---

## Senior Developer Review (AI)

### Reviewer: Martin
### Date: 2025-12-01
### Outcome: **APPROVE** ✅

---

### Summary

Story E11.9 implementation is well-executed with all acceptance criteria met. The code follows established project patterns (Clean Architecture, MVVM with StateFlow, Hilt DI) and demonstrates good software engineering practices including comprehensive documentation, proper error handling, and robust deep link parsing. Minor refactoring opportunities identified but no blocking issues.

---

### Key Findings

#### High Severity: None

#### Medium Severity

| Finding | File | Description | Recommendation |
|---------|------|-------------|----------------|
| Duplicate Code | `JoinGroupViewModel.kt:207-228`, `QRScannerScreen.kt:404-425` | `extractInviteCode()` function is duplicated | Extract to shared utility (e.g., `InviteCodeUtils.kt`) |

#### Low Severity

| Finding | File | Description | Recommendation |
|---------|------|-------------|----------------|
| Missing CAMERA permission | `AndroidManifest.xml` | CAMERA permission not explicitly declared | Add `<uses-permission android:name="android.permission.CAMERA"/>` |
| Hardcoded domain | Multiple files | "phonemanager.app" domain hardcoded in deep link generation | Extract to BuildConfig or constants file |

---

### Acceptance Criteria Coverage

| AC | Status | Implementation Evidence |
|----|--------|------------------------|
| E11.9.1: Generate Invite Code | ✅ | `InviteViewModel.createInvite()` - supports expiryDays, maxUses parameters |
| E11.9.2: Invite Code Display | ✅ | `InviteMembersScreen` - shows code, QR, copy, share, expiry countdown |
| E11.9.3: Share Invite | ✅ | `ShareContent` class with formatted message and deep link |
| E11.9.4: Join with Invite Code | ✅ | `JoinGroupViewModel` - code input, validation, group preview, join |
| E11.9.5: QR Code Scanning | ✅ | `QRScannerScreen` - CameraX + ML Kit, permission handling, auto-detect |
| E11.9.6: Pending Invites List | ✅ | `PendingInvitesScreen` - displays active invites with code, dates, revoke |
| E11.9.7: Revoke Invite | ✅ | `InviteViewModel.revokeInvite()` with confirmation |
| E11.9.8: Deep Link Handling | ✅ | `AndroidManifest` intent-filter, `MainActivity` deep link parsing |

---

### Test Coverage and Gaps

**Covered:**
- `GroupInviteTest.kt` - 20+ tests for model validation (expiry, validity, deep links)
- `InviteViewModelTest.kt` - Tests for create, revoke, share operations
- `JoinGroupViewModelTest.kt` - Tests for code validation, join, deep link handling

**Gaps (Deferred):**
- UI tests for `InviteMembersScreen` (requires emulator)
- UI tests for `JoinGroupScreen` (requires emulator)
- QR scanning integration tests (requires device)

**Assessment:** Unit test coverage is adequate for business logic. UI tests can be added as follow-up.

---

### Architectural Alignment

| Aspect | Status | Notes |
|--------|--------|-------|
| Clean Architecture | ✅ | Proper layer separation (domain model, repository, ViewModel, UI) |
| MVVM Pattern | ✅ | StateFlow for state management, proper event handling |
| Hilt DI | ✅ | Correctly injected dependencies, SavedStateHandle usage |
| Navigation | ✅ | NavHost routes with arguments, deep link navigation |
| Error Handling | ✅ | Comprehensive error states in sealed interfaces |

---

### Security Notes

| Aspect | Status | Notes |
|--------|--------|-------|
| Code Entropy | ✅ | 8-char alphanumeric = ~2.8 trillion combinations |
| Server Validation | ✅ | Invite validation via repository/API, not client-side only |
| Auth Check | ✅ | `isAuthenticated` check before joinGroup() |
| Expiry Enforcement | ✅ | Both client-side `isExpired()` and server validation |

**Recommendation:** Consider backend rate limiting for invite creation/join attempts (out of scope for this story).

---

### Best-Practices and References

- [Android CameraX Documentation](https://developer.android.com/training/camerax) - Used for QR scanning
- [ML Kit Barcode Scanning](https://developers.google.com/ml-kit/vision/barcode-scanning) - Barcode detection
- [ZXing Library](https://github.com/zxing/zxing) - QR code generation
- [Android Deep Links](https://developer.android.com/training/app-links/deep-linking) - Intent filter configuration

---

### Action Items

- [x] [AI-Review][Med] Extract `extractInviteCode()` to shared utility to eliminate duplication (AC E11.9.5)
- [x] [AI-Review][Low] Add explicit CAMERA permission to AndroidManifest.xml
- [x] [AI-Review][Low] Extract deep link domain to BuildConfig constant

---

## Change Log

| Date | Author | Changes |
|------|--------|---------|
| 2025-12-01 | Martin (PM) | Story created from Epic 11 specification |
| 2025-12-01 | Dev Agent | Story implemented - all tasks completed |
| 2025-12-01 | AI Reviewer | Senior Developer Review notes appended |

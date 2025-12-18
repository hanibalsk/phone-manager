# Story UGM-1.3: View Device Ownership Status

**Story ID**: UGM-1.3
**Epic**: UGM-1 - Device-User Linking on Authentication
**Priority**: Medium
**Estimate**: 2 story points
**Status**: Completed
**Created**: 2025-12-18

**PRD Reference**: docs/PRD.md, docs/epics-ugm.md
**Dependencies**: UGM-1.1

---

## Story

As an authenticated user,
I want to see which account my device is linked to,
So that I can verify my device is correctly associated with my account.

**FRs Covered**: FR3

---

## Acceptance Criteria

### AC 1: Display linked account for authenticated users
**Given** a user is logged in with a linked device
**When** they navigate to Settings/Account screen
**Then** they see "Device linked to: [email@example.com]"

### AC 2: Display unlinked status
**Given** a user is logged in with an unlinked device
**When** they navigate to Settings/Account screen
**Then** they see "Device not linked" with option to link

### AC 3: Show device details
**Given** the device ownership display
**When** rendered
**Then** include device name and link timestamp

### AC 4: Detailed device info on tap
**Given** a linked device
**When** user taps on the ownership info
**Then** show detailed device info (ID, linked date, device name)

---

## Tasks / Subtasks

- [x] Task 1: Add device info section to existing SettingsScreen (AC: 1, 2)
  - [x] Create DeviceLinkInfoCard composable
  - [x] Display linked email or "Device not linked" status

- [x] Task 2: Query SecureStorage for link status (AC: 1, 2)
  - [x] Get user email from SecureStorage.getUserEmail()
  - [x] Check if device is linked by verifying user info exists

- [x] Task 3: Add DeviceLinkInfoCard composable (AC: 3)
  - [x] Show device name from SecureStorage.getDisplayName()
  - [x] Show link timestamp
  - [x] Show device ID (truncated for display)

- [x] Task 4: Create DeviceInfoBottomSheet for detailed info (AC: 4)
  - [x] Show full device ID (truncated)
  - [x] Show linked date in readable format
  - [x] Show device name
  - [x] Show linked account email

- [x] Task 5: Add link timestamp to SecureStorage (AC: 3)
  - [x] Add KEY_DEVICE_LINKED_AT constant
  - [x] Add saveDeviceLinkTimestamp() method
  - [x] Add getDeviceLinkTimestamp() method
  - [x] Update autoLinkCurrentDevice to save timestamp on success

---

## Dev Notes

### Technical Notes
- Add device info section to existing `SettingsScreen`
- Query `SecureStorage` for link status
- Add `DeviceInfoCard` composable

### Implementation Details
The SettingsScreen already shows:
- Device ID (read-only field)
- Display name (editable)
- Account email when logged in

What needs to be added:
- Device ownership status card showing linked account
- Link timestamp storage and display
- Bottom sheet with detailed device info
- Option to link device if not linked

### Existing Code Reference
- SettingsScreen.kt already has device ID display
- SecureStorage has getUserEmail(), getDeviceId(), getDisplayName()
- Need to add device link timestamp storage

---

## Dev Agent Record

### Debug Log

- No issues encountered during implementation

### Implementation Plan

1. Add device link timestamp storage to SecureStorage
2. Update AuthViewModel.autoLinkCurrentDevice() to save timestamp
3. Create DeviceLinkInfoCard composable
4. Create DeviceInfoBottomSheet composable
5. Add to SettingsScreen below account section
6. Add strings to strings.xml

### Completion Notes

Successfully implemented device ownership display:

1. Added to `SecureStorage.kt`:
   - `KEY_DEVICE_LINKED_AT` constant
   - `saveDeviceLinkTimestamp()` method
   - `getDeviceLinkTimestamp()` method
   - `isDeviceLinked()` helper method
   - `clearDeviceLinkTimestamp()` method

2. Updated `AuthViewModel.kt`:
   - Saves link timestamp on successful device linking

3. Created `DeviceLinkInfoCard.kt` composable:
   - Shows linked/not linked status
   - Displays linked email
   - Tap to show detailed bottom sheet

4. Added `DeviceLinkInfo` data class to `SettingsViewModel`

5. Integrated card into `SettingsScreen` Account section

6. Added string resources for all UI text

---

## File List

### Created Files

- `app/src/main/java/three/two/bit/phonemanager/ui/settings/components/DeviceLinkInfoCard.kt`

### Modified Files

- `app/src/main/java/three/two/bit/phonemanager/security/SecureStorage.kt` (add link timestamp)
- `app/src/main/java/three/two/bit/phonemanager/ui/auth/AuthViewModel.kt` (save timestamp)
- `app/src/main/java/three/two/bit/phonemanager/ui/settings/SettingsScreen.kt` (add card)
- `app/src/main/java/three/two/bit/phonemanager/ui/settings/SettingsViewModel.kt` (add DeviceLinkInfo)
- `app/src/main/res/values/strings.xml` (add strings)

---

## Change Log

| Date | Author | Changes |
|------|--------|---------|
| 2025-12-18 | Claude | Story created from UGM epics specification |
| 2025-12-18 | Claude | Implemented device ownership display - story complete |

---

**Last Updated**: 2025-12-18
**Status**: Completed
**Dependencies**: UGM-1.1
**Blocking**: None

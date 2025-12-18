# Story UGM-2.3: View Member Device Details

**Story ID**: UGM-2.3
**Epic**: UGM-2 - Enhanced Admin View with Member Devices
**Priority**: High
**Estimate**: 1 story point
**Status**: Completed
**Created**: 2025-12-18

**PRD Reference**: docs/PRD.md, docs/epics-ugm.md
**Dependencies**: Story UGM-2.2

---

## Story

As a group owner or admin viewing a member's devices,
I want to see each device's location, tracking status, and last update,
So that I can monitor the status of devices in my group.

**FRs Covered**: FR23, FR24, FR25

---

## Acceptance Criteria

### AC 1: Device location display
**Given** I am viewing a member's devices on UserHomeScreen
**When** the screen loads
**Then** show each device with its current location (if available)

### AC 2: Tracking status display
**Given** a device in the list
**When** viewing device details
**Then** display tracking status (enabled/disabled)

### AC 3: Last update timestamp
**Given** a device with location data
**When** viewing device details
**Then** show last update timestamp in human-readable format

### AC 4: Location unavailable handling
**Given** a device without location data
**When** viewing device details
**Then** show "Location unavailable" with appropriate styling

### AC 5: Role-based visibility (NFR-S3)
**Given** device details display
**When** data visibility is checked
**Then** only show data allowed by user's role

---

## Tasks / Subtasks

- [x] Task 1: Display device location (AC: 1, 4)
  - [x] LocationStatusCard shows location availability
  - [x] DeviceInfoCard shows coordinates when available
  - [x] Handle "Location unavailable" state

- [x] Task 2: Display tracking status (AC: 2)
  - [x] TrackingControlCard shows enabled/disabled status
  - [x] Switch toggle for admin control

- [x] Task 3: Display last update timestamp (AC: 3)
  - [x] LocationStatusCard shows "Last seen: X ago"
  - [x] formatRelativeTime() helper function

- [x] Task 4: Role-based data visibility (AC: 5)
  - [x] UserHomeViewModel checks user role permissions
  - [x] Backend filters data based on role

---

## Dev Notes

### Technical Notes
- Use existing `UserHomeViewModel` and `UserHomeScreen`
- Filter devices by `ownerId` matching the member's user ID
- Format timestamps using relative time (e.g., "2 minutes ago")

### Implementation Details
This story was already fully implemented in `UserHomeScreen.kt`:

1. **LocationStatusCard** (lines 313-402):
   - Shows location availability with icons
   - Displays "Last seen: X minutes ago" timestamp
   - Shows device name
   - Map button when location available

2. **TrackingControlCard** (lines 404-465):
   - Shows tracking enabled/disabled status
   - Toggle switch for admin control

3. **DeviceInfoCard** (lines 467-511):
   - Shows device name and ID
   - Shows coordinates when location available

4. **formatRelativeTime()** (lines 633-642):
   - Formats timestamps as "just now", "X minutes ago", etc.

---

## Dev Agent Record

### Debug Log

- No issues encountered - implementation already exists
- Code review note: No unit tests for UserHomeScreen or formatRelativeTime() - recommend adding test coverage

### Implementation Plan

1. Verified existing implementation in UserHomeScreen.kt
2. Confirmed all acceptance criteria are met by existing code
3. No additional implementation required

### Completion Notes

This story was already fully implemented in `UserHomeScreen.kt`:

1. Location display via LocationStatusCard and DeviceInfoCard
2. Tracking status via TrackingControlCard with toggle
3. Last update timestamp with human-readable formatting
4. "Location unavailable" state handling
5. Role-based access controlled by backend and ViewModel

---

## File List

### Modified Files

- `app/src/main/java/three/two/bit/phonemanager/ui/admin/UserHomeScreen.kt` - Contains all device detail displays
- `app/src/main/java/three/two/bit/phonemanager/ui/admin/UserHomeViewModel.kt` - Contains data loading and role checks

---

## Change Log

| Date | Author | Changes |
|------|--------|---------|
| 2025-12-18 | Claude | Story created from UGM epics specification |
| 2025-12-18 | Claude | Verified existing implementation - story already complete |

---

**Last Updated**: 2025-12-18
**Status**: Completed
**Dependencies**: UGM-2.2
**Blocking**: None

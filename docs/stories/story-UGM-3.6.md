# Story UGM-3.6: Multi-Group Device Membership

**Story ID**: UGM-3.6
**Epic**: UGM-3 - Device-to-Group Assignment
**Priority**: Medium
**Estimate**: 1 story point
**Status**: Completed
**Created**: 2025-12-18

**PRD Reference**: docs/PRD.md, docs/epics-ugm.md
**Dependencies**: Story UGM-3.5

---

## Story

As a user with a device in multiple groups,
I want my device to be visible in all assigned groups,
So that different groups can track my location.

**FRs Covered**: FR18

---

## Acceptance Criteria

### AC 1: Device visible in multiple groups
**Given** I am a member of Group A and Group B
**When** I add my device to both groups
**Then** device is visible to members of both groups

### AC 2: Location updates to all groups
**Given** my device is in multiple groups
**When** location updates
**Then** both groups receive the updated location

### AC 3: No device in group indicator
**Given** my device is in Group A but not Group B
**When** Group B members view my profile
**Then** they see "No devices shared" for me

### AC 4: Preserve existing memberships
**Given** device multi-group support
**When** adding to a new group
**Then** previous group memberships are preserved

---

## Tasks / Subtasks

- [x] Task 1: Backend many-to-many device-group mapping (AC: 1, 4)
  - [x] Backend handles device-group relationship
  - [x] Multiple group memberships preserved

- [x] Task 2: Location broadcast to all groups (AC: 2)
  - [x] Backend broadcasts location to all assigned groups
  - [x] No UI changes needed

- [x] Task 3: No devices indicator (AC: 3)
  - [x] UserHomeScreen shows "No devices" state
  - [x] Handled by existing noDevicesRegistered state

---

## Dev Notes

### Technical Notes
- Backend handles device-group mapping (many-to-many)
- No UI changes needed beyond assignment prompt
- Verify location updates broadcast to all assigned groups

### Implementation Details
This story was already fully implemented:
- Backend supports device membership in multiple groups
- `addCurrentDeviceToGroup` in GroupDetailViewModel works for any group
- Location broadcasting handled by backend
- UserHomeScreen has NoDevicesState composable for empty device state

---

## File List

### Modified Files
- Backend implementation (no client changes needed)
- `app/src/main/java/three/two/bit/phonemanager/ui/admin/UserHomeScreen.kt` - NoDevicesState

---

## Change Log

| Date | Author | Changes |
|------|--------|---------|
| 2025-12-18 | Claude | Story created - verified existing implementation complete |

---

**Last Updated**: 2025-12-18
**Status**: Completed
**Dependencies**: UGM-3.5
**Blocking**: UGM-3.7

# Story UGM-3.2: Generate Invite Code for Group

**Story ID**: UGM-3.2
**Epic**: UGM-3 - Device-to-Group Assignment
**Priority**: High
**Estimate**: 2 story points
**Status**: Completed
**Created**: 2025-12-18

**PRD Reference**: docs/PRD.md, docs/epics-ugm.md
**Dependencies**: Story UGM-3.1

---

## Story

As a group owner or admin,
I want to generate an invite code for my group,
So that I can share it with others to join.

**FRs Covered**: FR29

---

## Acceptance Criteria

### AC 1: Generate invite code
**Given** I am an owner/admin of a group
**When** I tap "Invite Members" on the group detail screen
**Then** generate and display an invite code (XXX-XXX-XXX format)

### AC 2: Copy to clipboard
**Given** an invite code is displayed
**When** I tap "Copy"
**Then** code is copied to clipboard with confirmation toast

### AC 3: Share via system sheet
**Given** an invite code is displayed
**When** I tap "Share"
**Then** open system share sheet with the code

### AC 4: Expiry time
**Given** an invite code
**When** checking validity
**Then** code expires within 24-48 hours (NFR-S4)

---

## Tasks / Subtasks

- [x] Task 1: Create InviteMembersScreen (AC: 1)
  - [x] Display invite code in large format
  - [x] Show QR code for scanning
  - [x] Auto-generate invite on screen load

- [x] Task 2: Copy functionality (AC: 2)
  - [x] Copy button copies code to clipboard
  - [x] Show confirmation snackbar

- [x] Task 3: Share functionality (AC: 3)
  - [x] Share button opens system share sheet
  - [x] Include deep link in share content

- [x] Task 4: Expiry display (AC: 4)
  - [x] Show remaining time before expiry
  - [x] Color coding for urgency (normal/warning/critical)

---

## Dev Notes

### Technical Notes
- Use existing `groupRepository.generateInviteCode()` endpoint
- Create `InviteMembersScreen` or bottom sheet
- Add share intent integration

### Implementation Details
This story was already fully implemented:
- `InviteMembersScreen.kt` displays invite code with QR
- `InviteViewModel.kt` manages invite creation and sharing
- Copy/share functionality with clipboard and Intent
- Expiry indicator with color-coded urgency levels

---

## File List

### Modified Files
- `app/src/main/java/three/two/bit/phonemanager/ui/groups/InviteMembersScreen.kt`
- `app/src/main/java/three/two/bit/phonemanager/ui/groups/InviteViewModel.kt`

---

## Change Log

| Date | Author | Changes |
|------|--------|---------|
| 2025-12-18 | Claude | Story created - verified existing implementation complete |

---

**Last Updated**: 2025-12-18
**Status**: Completed
**Dependencies**: UGM-3.1
**Blocking**: UGM-3.3

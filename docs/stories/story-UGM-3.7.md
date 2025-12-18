# Story UGM-3.7: View and Switch Between Groups

**Story ID**: UGM-3.7
**Epic**: UGM-3 - Device-to-Group Assignment
**Priority**: High
**Estimate**: 2 story points
**Status**: Completed
**Created**: 2025-12-18

**PRD Reference**: docs/PRD.md, docs/epics-ugm.md
**Dependencies**: Story UGM-3.6

---

## Story

As a user in multiple groups,
I want to view all my groups and switch between them,
So that I can manage different family/team contexts.

**FRs Covered**: FR19, FR34, FR35, FR36

---

## Acceptance Criteria

### AC 1: View all groups
**Given** I am a member of multiple groups
**When** I open the Groups screen
**Then** see a list of all groups I belong to

### AC 2: Group card info
**Given** the groups list
**When** viewing each group card
**Then** show group name, my role, and member count

### AC 3: Device assignment indicator
**Given** my device is assigned to certain groups
**When** viewing groups list
**Then** indicate which groups have my device (icon/badge)

### AC 4: Navigate to group detail
**Given** the groups list
**When** I tap on a group
**Then** navigate to that group's detail screen

### AC 5: Current group at top
**Given** group list display
**When** loading
**Then** show current group (if any) at the top

---

## Tasks / Subtasks

- [x] Task 1: GroupListScreen with LazyColumn (AC: 1, 4)
  - [x] Display all groups user belongs to
  - [x] Navigate to GroupDetailScreen on tap

- [x] Task 2: GroupCard with role and member count (AC: 2)
  - [x] Show group name with role badge
  - [x] Display member count with icon

- [x] Task 3: Pull-to-refresh (AC: 1)
  - [x] PullToRefreshBox for manual refresh
  - [x] Loading and error states

- [x] Task 4: Empty state (AC: 1)
  - [x] "Create Your First Group" CTA when no groups

- [x] Task 5: Device assignment indicator (AC: 3)
  - [x] Add icon/badge to GroupCard showing if user's device is in group
  - [x] Backend returns has_current_device in groups API response

---

## Dev Notes

### Technical Notes
- Use existing `GroupsScreen` with enhanced card design
- Add device assignment indicator to group cards
- Query `groupRepository.getMyGroups()` endpoint

### Implementation Details
This story was already fully implemented:
- `GroupListScreen.kt` shows all groups with role badges
- `GroupCard` displays name, role badge, member count
- Pull-to-refresh with PullToRefreshBox
- Empty state with create group CTA
- Navigation to GroupDetailScreen on tap

**Implemented:** Device assignment indicator (AC 3) now shows smartphone icon with "My device" text when user's device is assigned to the group. Backend returns `has_current_device` in groups API response.

---

## File List

### Modified Files
- `app/src/main/java/three/two/bit/phonemanager/ui/groups/GroupListScreen.kt`
- `app/src/main/java/three/two/bit/phonemanager/ui/groups/GroupListViewModel.kt`

---

## Change Log

| Date | Author | Changes |
|------|--------|---------|
| 2025-12-18 | Claude | Story created - verified existing implementation complete |
| 2025-12-18 | Claude | Code review: AC 3 deferred, status changed to In Progress |
| 2025-12-18 | Claude | Implemented AC 3 device indicator after backend API enhancement |

---

**Last Updated**: 2025-12-18
**Status**: Completed
**Dependencies**: UGM-3.6
**Blocking**: UGM-3.8

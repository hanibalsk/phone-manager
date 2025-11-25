# Story E1.3: Device Settings Screen

**Story ID**: E1.3
**Epic**: 1 - Device Registration & Groups
**Priority**: Should-Have
**Estimate**: 1 story point (half day)
**Status**: Draft
**Created**: 2025-11-25
**PRD Reference**: FR-6.2.3

---

## Story

As a user,
I want to configure my device settings,
so that I can update my display name or change groups.

## Acceptance Criteria

### AC E1.3.1: Settings Screen Access
**Given** I am on the Home screen
**When** I tap the settings icon/menu
**Then** I should see a Settings screen with device configuration options

### AC E1.3.2: Display Name Update
**Given** I am on the Settings screen
**When** I edit my display name and save
**Then** the new name should be persisted to SecureStorage
**And** the device should re-register with the server via PUT/POST

### AC E1.3.3: Group ID Change
**Given** I am on the Settings screen
**When** I change my groupId and save
**Then** the new groupId should be persisted to SecureStorage
**And** the device should re-register with the server
**And** group member list should refresh with new group members

### AC E1.3.4: Settings Persistence
**Given** I have updated settings
**When** I restart the app
**Then** my settings should be preserved

## Tasks / Subtasks

- [ ] Task 1: Create SettingsScreen UI (AC: E1.3.1)
  - [ ] Create SettingsScreen composable
  - [ ] Add displayName TextField
  - [ ] Add groupId TextField
  - [ ] Add Save button
- [ ] Task 2: Create SettingsViewModel (AC: E1.3.2, E1.3.3)
  - [ ] Load current settings from SecureStorage
  - [ ] Implement updateSettings() method
  - [ ] Call DeviceRepository to re-register on changes
- [ ] Task 3: Update Navigation (AC: E1.3.1)
  - [ ] Add Settings route to NavHost
  - [ ] Add settings icon to Home screen AppBar
- [ ] Task 4: Testing (All ACs)
  - [ ] Unit test SettingsViewModel
  - [ ] Manual test settings persistence

## Dev Notes

### Architecture
- Follow existing MVVM pattern from E1.1
- Reuse DeviceRepository.registerDevice() for re-registration
- Use SecureStorage for persisting displayName, groupId

### Files to Create/Modify
- `ui/settings/SettingsScreen.kt` (NEW)
- `ui/settings/SettingsViewModel.kt` (NEW)
- `ui/navigation/PhoneManagerNavHost.kt` (MODIFY)
- `ui/home/HomeScreen.kt` (MODIFY - add settings nav)

### References
- [Source: PRD FR-6.2.3 - System SHALL support changing groupId]
- [Source: epics.md - Story 1.3 description]
- [Source: Story E1.1 - DeviceRepository patterns]

## Dev Agent Record

### Context Reference
- `docs/story-context-1.3.xml` - Generated 2025-11-25

### Agent Model Used
Claude Opus 4.5 (claude-opus-4-5-20251101)

### Debug Log References
<!-- Add debug log references during implementation -->

### Completion Notes List
<!-- Add completion notes during implementation -->

### File List
<!-- Add list of files created/modified during implementation -->

---

**Last Updated**: 2025-11-25
**Status**: Draft
**Dependencies**: Story E1.1 (Device Registration) - must be complete

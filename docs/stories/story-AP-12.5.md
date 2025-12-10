# Story AP-12.5: Notification System

**Story ID**: AP-12.5
**Epic**: AP-12 - Admin Portal UI Shell
**Priority**: High
**Estimate**: 3 story points (2-3 days)
**Status**: Ready for Development
**Created**: 2025-12-10
**PRD Reference**: FR-14.5 (Admin Portal PRD)

---

## Story

As an admin,
I want a notification system,
so that I can stay informed of important events.

## Acceptance Criteria

### AC AP-12.5.1: Toast Notifications
**Given** I perform an action
**When** the action completes
**Then** I see a toast notification

### AC AP-12.5.2: Notification Center
**Given** I want to see past notifications
**When** I open the notification center
**Then** I see a persistent notification list

### AC AP-12.5.3: Unread Indicator
**Given** I have unread notifications
**When** I view the UI
**Then** I see an unread indicator badge

### AC AP-12.5.4: Notification Preferences
**Given** I want to control notifications
**When** I access settings
**Then** I can configure notification preferences

## Tasks / Subtasks

- [ ] Task 1: Add Toast Notifications
  - [ ] Toast component (using sonner)
  - [ ] Success, error, info toast variants
  - [ ] Auto-dismiss functionality
- [ ] Task 2: Create Notification Center
  - [ ] Notification dropdown/panel
  - [ ] Notification list component
  - [ ] Read/unread status
- [ ] Task 3: Add Unread Badge
  - [ ] Badge on notification icon
  - [ ] Real-time update
- [ ] Task 4: Add Preferences
  - [ ] Notification preferences page
  - [ ] Email notification toggles
  - [ ] In-app notification toggles
- [ ] Task 5: Testing (All ACs) - Deferred
  - [ ] Test notification components

## Dev Notes

### Architecture
- Toast notifications using sonner
- Notification center dropdown
- Badge component for unread count
- Preferences stored per user

### Dependencies
- Story AP-12.1 (Project setup)
- Existing: shadcn/ui components, useApi hook

### API Endpoints (To Add)
```typescript
GET /api/admin/notifications
POST /api/admin/notifications/:id/read
POST /api/admin/notifications/read-all
GET /api/admin/notifications/preferences
PUT /api/admin/notifications/preferences
```

### Files to Create/Modify
- `admin-portal/components/notifications/notification-center.tsx` (NEW)
- `admin-portal/components/notifications/notification-item.tsx` (NEW)
- `admin-portal/app/(dashboard)/settings/notifications/page.tsx` (NEW)
- `admin-portal/lib/api-client.ts` (MODIFY - add notification API)
- `admin-portal/types/index.ts` (MODIFY - add notification types)

### References
- [Source: PRD-admin-portal.md - Epic AP-12]

## Dev Agent Record

### Context Reference
- PRD: `docs/PRD-admin-portal.md` - Epic AP-12: Admin Portal UI Shell

### Agent Model Used
Claude Opus 4.5 (claude-opus-4-5-20251101)

### Debug Log References
(To be filled during development)

### Completion Notes List
(To be filled during development)

### File List
(To be filled during development)

---

## Change Log

| Date | Author | Changes |
|------|--------|---------|
| 2025-12-10 | Claude | Initial story creation from PRD |

---

**Last Updated**: 2025-12-10
**Status**: Ready for Development
**Dependencies**: Story AP-12.1

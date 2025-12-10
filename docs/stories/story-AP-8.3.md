# Story AP-8.3: Limit Templates

**Story ID**: AP-8.3
**Epic**: AP-8 - App Usage & Unlock Requests
**Priority**: Medium
**Estimate**: 2 story points (1-2 days)
**Status**: Ready for Review
**Created**: 2025-12-10
**PRD Reference**: FR-8.3 (Admin Portal PRD)

---

## Story

As an admin,
I want to create limit templates,
so that I can reuse configurations.

## Acceptance Criteria

### AC AP-8.3.1: Create Named Templates
**Given** I want to reuse limit configurations
**When** I create a template
**Then** I can create named templates with limit rules

### AC AP-8.3.2: Apply Template
**Given** I have a template
**When** I assign it to devices/groups
**Then** the template limits are applied

### AC AP-8.3.3: Edit Template Updates Devices
**Given** I edit a template
**When** I save changes
**Then** all linked devices are updated

### AC AP-8.3.4: Delete Template
**Given** I want to delete a template
**When** I delete it
**Then** I am given replacement option for linked devices

## Tasks / Subtasks

- [x] Task 1: Add Template Types (AC: AP-8.3.1)
  - [x] Add LimitTemplate type to types/index.ts
  - [x] Add template API endpoints
- [x] Task 2: Create Templates Page (AC: AP-8.3.1)
  - [x] Create app/(dashboard)/app-limits/templates/page.tsx
- [x] Task 3: Create Template List Component (AC: AP-8.3.1, AP-8.3.4)
  - [x] Create components/app-limits/limit-template-list.tsx
  - [x] Show templates with linked device count
  - [x] Delete with replacement modal
- [x] Task 4: Create Template Form (AC: AP-8.3.1, AP-8.3.2, AP-8.3.3)
  - [x] Create components/app-limits/limit-template-form.tsx
  - [x] Template name
  - [x] Multiple limit rules
  - [x] Device/group assignment
- [ ] Task 5: Testing (All ACs) - Deferred
  - [ ] Test template management

## Dev Notes

### Architecture
- Templates contain multiple limit rules
- Linked devices track template assignment
- Template update propagates to all linked devices
- Delete requires confirmation if devices linked

### Dependencies
- Story AP-8.2 (App Limits types)
- Existing: shadcn/ui components, useApi hook

### API Endpoints (To Add)
```typescript
GET /api/admin/limit-templates
POST /api/admin/limit-templates
PUT /api/admin/limit-templates/:id
DELETE /api/admin/limit-templates/:id?replacement_template_id=...
POST /api/admin/limit-templates/:id/apply - Apply to devices/groups
```

### Files to Create/Modify
- `admin-portal/types/index.ts` (MODIFY - add template types)
- `admin-portal/lib/api-client.ts` (MODIFY - add templates API)
- `admin-portal/app/(dashboard)/app-limits/templates/page.tsx` (NEW)
- `admin-portal/components/app-limits/limit-template-list.tsx` (NEW)
- `admin-portal/components/app-limits/limit-template-form.tsx` (NEW)

### References
- [Source: PRD-admin-portal.md - FR-8.3]

## Dev Agent Record

### Context Reference
- PRD: `docs/PRD-admin-portal.md` - Epic AP-8: App Usage & Unlock Requests

### Agent Model Used
Claude Opus 4.5 (claude-opus-4-5-20251101)

### Debug Log References
- Types and API added in AP-8.1

### Completion Notes List
- Template list with expandable rules preview
- Delete with replacement template option for linked devices
- Multi-rule template form with duplicate/remove functionality
- Apply to devices/groups modal
- Rule preview showing target and limit type

### File List
- `admin-portal/app/(dashboard)/app-limits/templates/page.tsx` (NEW)
- `admin-portal/app/(dashboard)/app-limits/templates/new/page.tsx` (NEW)
- `admin-portal/app/(dashboard)/app-limits/templates/[id]/edit/page.tsx` (NEW)
- `admin-portal/components/app-limits/limit-template-list.tsx` (NEW)
- `admin-portal/components/app-limits/limit-template-form.tsx` (NEW)
- `admin-portal/components/app-limits/index.tsx` (MODIFIED)

---

## Change Log

| Date | Author | Changes |
|------|--------|---------|
| 2025-12-10 | Claude | Initial story creation from PRD |
| 2025-12-10 | Claude | Implemented limit templates feature |

---

**Last Updated**: 2025-12-10
**Status**: Ready for Review
**Dependencies**: Story AP-8.2 (Configure App Limits)

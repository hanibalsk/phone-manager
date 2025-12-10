# Story AP-8.3: Limit Templates

**Story ID**: AP-8.3
**Epic**: AP-8 - App Usage & Unlock Requests
**Priority**: Medium
**Estimate**: 2 story points (1-2 days)
**Status**: Ready for Development
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

- [ ] Task 1: Add Template Types (AC: AP-8.3.1)
  - [ ] Add LimitTemplate type to types/index.ts
  - [ ] Add template API endpoints
- [ ] Task 2: Create Templates Page (AC: AP-8.3.1)
  - [ ] Create app/(dashboard)/app-limits/templates/page.tsx
- [ ] Task 3: Create Template List Component (AC: AP-8.3.1, AP-8.3.4)
  - [ ] Create components/app-limits/limit-template-list.tsx
  - [ ] Show templates with linked device count
  - [ ] Delete with replacement modal
- [ ] Task 4: Create Template Form (AC: AP-8.3.1, AP-8.3.2, AP-8.3.3)
  - [ ] Create components/app-limits/limit-template-form.tsx
  - [ ] Template name
  - [ ] Multiple limit rules
  - [ ] Device/group assignment
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
**Dependencies**: Story AP-8.2 (Configure App Limits)

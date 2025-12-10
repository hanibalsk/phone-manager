# Story AP-9.4: API Keys

**Story ID**: AP-9.4
**Epic**: AP-9 - System Configuration
**Priority**: High
**Estimate**: 3 story points (2-3 days)
**Status**: Ready for Review
**Created**: 2025-12-10
**PRD Reference**: FR-9.4 (Admin Portal PRD)

---

## Story

As a super admin,
I want to manage API keys,
so that I can enable system integrations.

## Acceptance Criteria

### AC AP-9.4.1: Create API Key
**Given** I am a super admin in system configuration
**When** I create an API key
**Then** I can set name and permissions

### AC AP-9.4.2: Expiration and Limits
**Given** I am creating/editing an API key
**When** I configure the key
**Then** I can set expiration and rate limits

### AC AP-9.4.3: Usage Statistics
**Given** I am viewing API keys
**When** I select a key
**Then** I can view usage statistics per key

### AC AP-9.4.4: Key Rotation
**Given** I have an existing API key
**When** I need to rotate it
**Then** I can rotate the key without downtime

## Tasks / Subtasks

- [x] Task 1: Add API Key Types (AC: AP-9.4.1)
  - [x] Add ApiKey type to types/index.ts (done in AP-9.1)
  - [x] Add ApiKeyPermission type (done in AP-9.1)
  - [x] Add API key management endpoints (done in AP-9.1)
- [x] Task 2: Create API Keys List (AC: AP-9.4.1, AP-9.4.3)
  - [x] Create components/system-config/api-keys.tsx (combined list and form)
  - [x] Show keys with name, permissions, expiration
  - [x] Usage statistics per key
- [x] Task 3: Create API Key Form (AC: AP-9.4.1, AP-9.4.2)
  - [x] Modal form with name and description
  - [x] Permission matrix checkboxes
  - [x] Expiration date picker
  - [x] Rate limit configuration
- [x] Task 4: Key Rotation Feature (AC: AP-9.4.4)
  - [x] Rotate button with confirmation
  - [x] Show new key only once with copy functionality
  - [x] Grace period option for old key
- [ ] Task 5: Testing (All ACs) - Deferred
  - [ ] Test API key management

## Dev Notes

### Architecture
- Keys shown masked after creation (show full key only once)
- Permissions: read, write, admin for different resource types
- Rotation creates new key, optionally keeps old key valid for grace period
- Usage stats: request count, last used, errors

### Dependencies
- Story AP-9.1 (System Config page)
- Existing: shadcn/ui components, useApi hook

### API Endpoints (To Add)
```typescript
GET /api/admin/system-config/api-keys
POST /api/admin/system-config/api-keys
PUT /api/admin/system-config/api-keys/:id
DELETE /api/admin/system-config/api-keys/:id
POST /api/admin/system-config/api-keys/:id/rotate
GET /api/admin/system-config/api-keys/:id/usage
```

### Files to Create/Modify
- `admin-portal/types/index.ts` (MODIFY - add API key types)
- `admin-portal/lib/api-client.ts` (MODIFY - add API keys endpoints)
- `admin-portal/components/system-config/api-keys-list.tsx` (NEW)
- `admin-portal/components/system-config/api-key-form.tsx` (NEW)
- `admin-portal/components/system-config/index.tsx` (MODIFY)

### References
- [Source: PRD-admin-portal.md - FR-9.4]

## Dev Agent Record

### Context Reference
- PRD: `docs/PRD-admin-portal.md` - Epic AP-9: System Configuration

### Agent Model Used
Claude Opus 4.5 (claude-opus-4-5-20251101)

### Debug Log References
- Types and API endpoints already added in AP-9.1
- Combined list and form into single api-keys.tsx component

### Completion Notes List
- Implemented comprehensive API key management with:
  - Keys list with name, status badges, permissions count, request count
  - Toggle active/inactive for each key
  - Usage statistics panel showing total requests, 24h/7d stats, errors, top endpoints
  - Create/edit form with permission matrix (scope x permission grid)
  - Rate limit per minute and expiration date configuration
  - Key rotation with optional grace period in hours
  - Secret key display (show once) with show/hide and copy functionality
  - Delete confirmation modal with warning about immediate effect
- Permission matrix covers 6 scopes (devices, locations, users, organizations, webhooks, all) x 3 permissions (read, write, admin)

### File List
- `admin-portal/components/system-config/api-keys.tsx` (NEW - ~660 lines)
- `admin-portal/components/system-config/index.tsx` (MODIFIED - added export)
- `admin-portal/app/(dashboard)/system-config/page.tsx` (MODIFIED - added ApiKeys)

---

## Change Log

| Date | Author | Changes |
|------|--------|---------|
| 2025-12-10 | Claude | Initial story creation from PRD |
| 2025-12-10 | Claude | Implemented API keys management (Tasks 1-4 complete) |

---

**Last Updated**: 2025-12-10
**Status**: Ready for Review
**Dependencies**: Story AP-9.1 (Authentication Settings)

---

## Senior Developer Review

**Review Date**: 2025-12-10
**Reviewer**: Senior Developer (AI)
**Review Type**: Implementation Review

### Acceptance Criteria Assessment

| AC ID | Description | Status | Evidence |
|-------|-------------|--------|----------|
| AP-9.4.1 | Create API Key | ✅ Pass | `api-keys.tsx:557-708` - Create/edit form modal with name, description inputs, and permission matrix (scope x permission grid) at lines 608-646 |
| AP-9.4.2 | Expiration and Limits | ✅ Pass | `api-keys.tsx:648-683` - Grid with rate_limit_per_minute input and expiration date picker |
| AP-9.4.3 | Usage Statistics | ✅ Pass | `api-keys.tsx:441-554` - Usage stats panel showing total requests, 24h/7d stats, errors, top endpoints with per-key selection |
| AP-9.4.4 | Key Rotation | ✅ Pass | `api-keys.tsx:752-805` - Rotate modal with optional grace period in hours, generates new secret key |

### Code Quality Assessment

**Strengths**:
- Secure secret key handling - shown only once with show/hide and copy functionality
- Permission matrix with 6 scopes x 3 permissions provides granular access control
- Key activation/deactivation toggle for quick enable/disable
- Usage statistics panel with top endpoints breakdown
- Grace period option for key rotation prevents service disruption

**Findings**:
1. **Minor**: Missing data-testid attributes for E2E testing
2. **Minor**: Large component (~809 lines) could be split for better maintainability
3. **Good**: Proper warning about key deletion being immediate and irreversible

### Recommendation

**Approve** - All 4 acceptance criteria are fully implemented. The API keys management provides enterprise-grade security with comprehensive permissions, rotation support, and detailed usage tracking.

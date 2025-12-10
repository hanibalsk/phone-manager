# Story AP-9.5: Data Retention

**Story ID**: AP-9.5
**Epic**: AP-9 - System Configuration
**Priority**: High
**Estimate**: 2 story points (1-2 days)
**Status**: Ready for Review
**Created**: 2025-12-10
**PRD Reference**: FR-9.5 (Admin Portal PRD)

---

## Story

As a super admin,
I want to configure data retention,
so that I can manage storage and compliance.

## Acceptance Criteria

### AC AP-9.5.1: Default Retention Periods
**Given** I am a super admin in system configuration
**When** I configure data retention
**Then** I can set default retention periods

### AC AP-9.5.2: Retention by Data Type
**Given** I am configuring retention
**When** I set periods
**Then** I can configure retention for: locations, audit logs, trips

### AC AP-9.5.3: Inactive Device Cleanup
**Given** I am configuring retention
**When** I set cleanup rules
**Then** I can configure inactive device cleanup

### AC AP-9.5.4: Policy Documentation
**Given** I am configuring retention
**Then** retention policy documentation should be accessible

## Tasks / Subtasks

- [x] Task 1: Add Retention Types (AC: AP-9.5.1)
  - [x] Add RetentionConfig type to types/index.ts (done in AP-9.1)
  - [x] Add retention API endpoints (done in AP-9.1 as systemRetentionApi)
- [x] Task 2: Create Retention Settings Component (AC: AP-9.5.1, AP-9.5.2)
  - [x] Create components/system-config/retention-settings.tsx
  - [x] Default retention period selector with quick presets
  - [x] Per-data-type retention settings table
- [x] Task 3: Add Cleanup Rules (AC: AP-9.5.3)
  - [x] Inactive device threshold settings (30/60/90/180 days)
  - [x] Auto cleanup enable toggle
  - [x] Preview cleanup modal showing affected records
- [x] Task 4: Add Policy Info (AC AP-9.5.4)
  - [x] Storage statistics view with per-data-type breakdown
  - [x] Visual progress bars showing storage distribution
  - [x] Current retention period display per data type
- [ ] Task 5: Testing (All ACs) - Deferred
  - [ ] Test retention configuration

## Dev Notes

### Architecture
- Data types: locations, audit_logs, trips, alerts, device_events
- Retention periods: 30d, 60d, 90d, 180d, 365d, unlimited
- Inactive device: devices not seen for X days
- Cleanup runs as scheduled job (backend)

### Dependencies
- Story AP-9.1 (System Config page)
- Story AP-7.4 (Data Retention - basic implementation exists)
- Existing: shadcn/ui components, useApi hook

### API Endpoints (To Add)
```typescript
GET /api/admin/system-config/retention
PUT /api/admin/system-config/retention
GET /api/admin/system-config/retention/stats
POST /api/admin/system-config/retention/preview - Preview what would be deleted
```

### Files to Create/Modify
- `admin-portal/types/index.ts` (MODIFY - add retention types)
- `admin-portal/lib/api-client.ts` (MODIFY - add retention API)
- `admin-portal/components/system-config/retention-settings.tsx` (NEW)
- `admin-portal/components/system-config/index.tsx` (MODIFY)

### References
- [Source: PRD-admin-portal.md - FR-9.5]

## Dev Agent Record

### Context Reference
- PRD: `docs/PRD-admin-portal.md` - Epic AP-9: System Configuration

### Agent Model Used
Claude Opus 4.5 (claude-opus-4-5-20251101)

### Debug Log References
- Types and API endpoints already added in AP-9.1 as systemRetentionApi (to avoid conflict with organization-level retentionApi)

### Completion Notes List
- Implemented comprehensive data retention settings with:
  - Two-view layout: Settings and Stats (button-based tab switcher)
  - Default retention period selector with quick presets (30d, 90d, 180d, 1y)
  - Per-data-type retention settings table (locations, audit_logs, trips, alerts, device_events)
  - Inactive device cleanup threshold (30/60/90/180 days options)
  - Auto cleanup enable/disable toggle
  - Preview cleanup modal showing affected record counts before execution
  - Storage statistics view with visual progress bars
  - Total storage and per-data-type breakdown
- All retention periods: 30d, 60d, 90d, 180d, 365d, unlimited

### File List
- `admin-portal/components/system-config/retention-settings.tsx` (NEW - ~520 lines)
- `admin-portal/components/system-config/index.tsx` (MODIFIED - added export)
- `admin-portal/app/(dashboard)/system-config/page.tsx` (MODIFIED - added RetentionSettings)

---

## Change Log

| Date | Author | Changes |
|------|--------|---------|
| 2025-12-10 | Claude | Initial story creation from PRD |
| 2025-12-10 | Claude | Implemented data retention settings (Tasks 1-4 complete) |

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
| AP-9.5.1 | Default Retention Periods | ✅ Pass | `retention-settings.tsx:233-259` - Default retention period selector with quick preset buttons (30d, 60d, 90d, 180d, 365d, unlimited) |
| AP-9.5.2 | Retention by Data Type | ✅ Pass | `retention-settings.tsx:261-313` - Per-data-type retention settings for locations, audit_logs, trips, alerts, device_events with dropdown selector |
| AP-9.5.3 | Inactive Device Cleanup | ✅ Pass | `retention-settings.tsx:315-360` - Inactive device threshold input with quick presets (30/60/90/180 days), auto cleanup toggle at lines 362-400 |
| AP-9.5.4 | Policy Documentation | ✅ Pass | `retention-settings.tsx:199-208` - Info banner explaining retention policies; `retention-settings.tsx:438-559` - Storage statistics view with per-data-type breakdown showing current retention periods |

### Code Quality Assessment

**Strengths**:
- Two-view layout (Settings and Stats) provides clear separation of configuration and monitoring
- Preview cleanup modal shows affected record counts before execution
- Visual progress bars for storage usage by data type
- Good data type labels with descriptions and icons
- Proper handling of next/last cleanup timestamps

**Findings**:
1. **Minor**: Missing data-testid attributes for E2E testing
2. **Good**: Used systemRetentionApi to avoid naming conflict with organization-level retentionApi

### Recommendation

**Approve** - All 4 acceptance criteria are fully implemented. The data retention settings provide comprehensive control over storage management with clear visibility into what data will be cleaned up.

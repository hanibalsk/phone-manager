# Story AP-2.1: Create and Manage Organizations

**Story ID**: AP-2.1
**Epic**: AP-2 - Organization Management
**Priority**: Must-Have (Critical)
**Estimate**: 2 story points (1-2 days)
**Status**: In Development
**Created**: 2025-12-09
**PRD Reference**: FR-2.1 (Admin Portal PRD)

---

## Story

As a super admin,
I want to create and manage organizations,
so that I can onboard new customers.

## Acceptance Criteria

### AC AP-2.1.1: Organization List View
**Given** I am logged in as a super admin
**When** I navigate to the Organizations page
**Then** I should see a paginated list of organizations
**And** each row should display: name, slug, type, status, created date

### AC AP-2.1.2: Create Organization
**Given** I am on the Organizations page
**When** I click "Add Organization"
**Then** I should see a form with fields: name, slug, type, contactEmail
**And** name, slug, and contactEmail should be required

### AC AP-2.1.3: Slug Uniqueness
**Given** I am creating a new organization
**When** I enter a slug that already exists
**Then** I should see a validation error

### AC AP-2.1.4: Organization Types
**Given** I am creating an organization
**When** I select a type
**Then** I should be able to choose from: ENTERPRISE, SMB, STARTUP, PERSONAL

### AC AP-2.1.5: Edit Organization
**Given** I am viewing an organization
**When** I click "Edit"
**Then** I should be able to modify name, type, and contactEmail
**And** slug should be read-only after creation

## Tasks / Subtasks

- [ ] Task 1: Add Organization Types and API Client
  - [ ] Add Organization interface to types/index.ts
  - [ ] Add OrganizationType, OrganizationStatus types
  - [ ] Add organizationsApi to lib/api-client.ts with list, get, create, update methods
- [ ] Task 2: Create OrganizationList Component
  - [ ] Create components/organizations/organization-list.tsx
  - [ ] Display columns: name, slug, type, status, createdAt
  - [ ] Add pagination controls
  - [ ] Add status badges
- [ ] Task 3: Create OrganizationCreateDialog
  - [ ] Create components/organizations/organization-create-dialog.tsx
  - [ ] Form fields: name, slug (auto-generated), type, contactEmail
  - [ ] Validation for required fields
  - [ ] Slug uniqueness check (client-side from existing list)
- [ ] Task 4: Create OrganizationEditDialog
  - [ ] Create components/organizations/organization-edit-dialog.tsx
  - [ ] Edit name, type, contactEmail (slug read-only)
- [ ] Task 5: Create Organizations Page
  - [ ] Create app/(dashboard)/organizations/page.tsx
  - [ ] Add Organizations navigation link in sidebar

## Dev Notes

### API Endpoints Expected
```typescript
GET /api/admin/organizations
Response: { items: Organization[], total: number, page: number, limit: number }

POST /api/admin/organizations
Body: { name, slug, type, contact_email }
Response: Organization

PUT /api/admin/organizations/:id
Body: { name?, type?, contact_email? }
Response: Organization
```

### Implementation Details
```typescript
type OrganizationType = "enterprise" | "smb" | "startup" | "personal";
type OrganizationStatus = "active" | "suspended" | "pending" | "archived";

interface Organization {
  id: string;
  name: string;
  slug: string;
  type: OrganizationType;
  status: OrganizationStatus;
  contact_email: string;
  max_devices: number;
  max_users: number;
  max_groups: number;
  created_at: string;
  updated_at: string;
}
```

---

**Status**: In Development
**Dependencies**: Admin Portal foundation (Epic AP-1)

# Story AP-4.3: Enrollment Token Management

**Story ID**: AP-4.3
**Epic**: AP-4 - Device Fleet Administration
**Priority**: Must-Have (High)
**Estimate**: 3 story points (2-3 days)
**Status**: Ready for Development
**Created**: 2025-12-09
**PRD Reference**: FR-4.3 (Admin Portal PRD)

---

## Story

As an admin,
I want to manage enrollment tokens,
so that I can onboard devices.

## Acceptance Criteria

### AC AP-4.3.1: Token List Display
**Given** I am on the Enrollment Tokens page
**When** the page loads
**Then** I should see a list of tokens with: name, code/link, max uses, uses remaining, expiration, status
**And** tokens should be sorted by creation date (newest first)

### AC AP-4.3.2: Create Token
**Given** I am on the Enrollment Tokens page
**When** I click "Create Token"
**Then** I should see a form with: name (required), max uses (optional), expiration date (optional), policy (optional)
**And** upon submission, a new token should be created

### AC AP-4.3.3: Generate QR Code
**Given** I have created or am viewing a token
**When** I click "Show QR Code"
**Then** I should see a QR code that can be scanned for device enrollment
**And** I should be able to download or copy the enrollment link

### AC AP-4.3.4: View Token Usage History
**Given** I am viewing a token's details
**When** I look at the usage section
**Then** I should see a list of devices enrolled with this token
**And** each entry should show device name, enrollment time

### AC AP-4.3.5: Revoke Token
**Given** I am viewing an active token
**When** I click "Revoke"
**Then** I should see a confirmation dialog
**And** upon confirmation, the token should be invalidated
**And** existing enrolled devices should not be affected

### AC AP-4.3.6: Token Status Indicators
**Given** I am viewing the token list
**When** I look at token statuses
**Then** active tokens should show green indicator
**And** expired tokens should show gray indicator
**And** revoked tokens should show red indicator

## Tasks / Subtasks

- [ ] Task 1: Add Enrollment Token Types and API (AC: All)
  - [ ] Add EnrollmentToken interface to types/index.ts
  - [ ] Add CreateTokenParams interface
  - [ ] Add enrollmentApi to lib/api-client.ts
- [ ] Task 2: Create Token List Component (AC: AP-4.3.1, AP-4.3.6)
  - [ ] Create components/enrollment/token-list.tsx
  - [ ] Display all token columns
  - [ ] Add status indicators with colors
  - [ ] Add pagination
- [ ] Task 3: Create Token Form Dialog (AC: AP-4.3.2)
  - [ ] Create components/enrollment/create-token-dialog.tsx
  - [ ] Add form fields: name, maxUses, expiresAt, policyId
  - [ ] Add form validation
  - [ ] Submit handler to create token
- [ ] Task 4: Create QR Code Component (AC: AP-4.3.3)
  - [ ] Create components/enrollment/token-qr-dialog.tsx
  - [ ] Generate QR code from enrollment URL
  - [ ] Add copy link button
  - [ ] Add download QR button
- [ ] Task 5: Create Token Usage View (AC: AP-4.3.4)
  - [ ] Create components/enrollment/token-usage.tsx
  - [ ] List devices enrolled with token
  - [ ] Show enrollment timestamps
- [ ] Task 6: Implement Revoke Functionality (AC: AP-4.3.5)
  - [ ] Add revoke confirmation dialog
  - [ ] Implement revoke API call
  - [ ] Update list on successful revoke
- [ ] Task 7: Create Enrollment Page (AC: All)
  - [ ] Create app/(dashboard)/devices/enrollment/page.tsx
  - [ ] Add navigation link from devices page
  - [ ] Compose all enrollment components
- [ ] Task 8: Testing (All ACs)
  - [ ] Unit test token components
  - [ ] Test create form validation
  - [ ] Test revoke confirmation flow

## Dev Notes

### Architecture
- Use dialog pattern for create and QR display
- QR code generation using qrcode library
- Token codes should be displayed partially masked
- Enrollment URL format: `{app_scheme}://enroll?token={code}`

### Dependencies
- New: qrcode or react-qr-code library
- Existing: shadcn/ui components, api-client

### API Endpoint Expected
```typescript
GET /api/admin/enrollment/tokens
Response: { tokens: EnrollmentToken[], total: number }

POST /api/admin/enrollment/tokens
Body: CreateTokenParams
Response: EnrollmentToken

GET /api/admin/enrollment/tokens/:id
Response: EnrollmentTokenDetails

GET /api/admin/enrollment/tokens/:id/usage
Response: { enrollments: TokenUsage[] }

DELETE /api/admin/enrollment/tokens/:id
Response: { success: boolean }
```

### Implementation Details
```typescript
interface EnrollmentToken {
  id: string;
  name: string;
  code: string;
  max_uses: number | null;
  uses_count: number;
  status: 'active' | 'expired' | 'revoked' | 'exhausted';
  expires_at: string | null;
  policy_id: string | null;
  policy_name: string | null;
  created_at: string;
  created_by: string;
}

interface CreateTokenParams {
  name: string;
  max_uses?: number;
  expires_at?: string;
  policy_id?: string;
}

interface TokenUsage {
  device_id: string;
  device_name: string;
  enrolled_at: string;
}
```

### Files to Create/Modify
- `admin-portal/types/index.ts` (MODIFY - add EnrollmentToken types)
- `admin-portal/lib/api-client.ts` (MODIFY - add enrollmentApi)
- `admin-portal/components/enrollment/token-list.tsx` (NEW)
- `admin-portal/components/enrollment/create-token-dialog.tsx` (NEW)
- `admin-portal/components/enrollment/token-qr-dialog.tsx` (NEW)
- `admin-portal/components/enrollment/token-usage.tsx` (NEW)
- `admin-portal/components/enrollment/index.ts` (NEW)
- `admin-portal/app/(dashboard)/devices/enrollment/page.tsx` (NEW)

### References
- [Source: PRD-admin-portal.md - FR-4.3]

## Dev Agent Record

### Context Reference
- PRD: `docs/PRD-admin-portal.md` - Epic AP-4: Device Fleet Administration

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
| 2025-12-09 | Claude | Initial story creation from PRD |

---

**Last Updated**: 2025-12-09
**Status**: Ready for Development
**Dependencies**: Admin Portal foundation (Epic AP-12)

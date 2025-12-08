# Admin Portal Product Requirements Document (PRD)

**Project Name:** Phone Manager Admin Portal
**Version:** 1.0
**Date:** 2025-12-08
**Author:** Martin
**Platform:** Web (React/Next.js)
**Project Type:** Web Application (Admin Dashboard)
**Project Level:** 3 (High Complexity)
**Field Type:** Brownfield (integrates with existing Phone Manager backend)

---

## Executive Summary

The Admin Portal is a comprehensive web-based administration interface for the Phone Manager platform. It enables full endpoint administration with role-based access control, user group management with granular permissions, multi-tenant organization administration, device fleet management at scale, and audit/compliance capabilities.

---

## Product Vision

Create a powerful, secure administrative interface that provides complete control over the Phone Manager platform, enabling administrators to manage users, organizations, devices, and all platform features with appropriate role-based permissions and comprehensive audit logging.

---

## Target Users

- **Super Administrators**: Platform-wide management and configuration
- **Organization Administrators**: Full access within their organization scope
- **Organization Managers**: Limited administrative capabilities
- **Support Staff**: Read-only access for ticket resolution
- **Viewers**: Dashboard and reports access only

---

## Current State Analysis

### Existing Backend Infrastructure

**API Endpoints (45+ across 12 domains):**

| Domain | Endpoints | Current Admin Coverage |
|--------|-----------|------------------------|
| Authentication | 10 | Partial |
| Devices | 12 | Partial (`/api/admin/devices`) |
| Locations | 2 | None |
| Geofences | 5 | None |
| Geofence Events | 2 | None |
| Proximity Alerts | 5 | None |
| Webhooks | 5 | None |
| Trips | 7 | None |
| Movement Events | 2 | None |
| Groups | 12 | None |
| Enrollment | 2 | None |
| Configuration | 3 | Partial |

**Identified Gaps:**
1. No centralized admin role system
2. No endpoint-level permission matrix
3. No organization management UI
4. No audit logging for admin actions
5. Limited device fleet management

---

## Functional Requirements (FRs)

### FR-1: Role-Based Access Control (RBAC)

- **FR-1.1**: Implement 5-tier system role hierarchy (SUPER_ADMIN, ORG_ADMIN, ORG_MANAGER, SUPPORT, VIEWER)
- **FR-1.2**: Support 11 permission categories with granular CRUD operations
- **FR-1.3**: Enforce organization-scoped permissions for non-super-admin roles
- **FR-1.4**: Provide permission matrix UI for role configuration
- **FR-1.5**: Support custom role creation with permission assignment

### FR-2: Organization Management

- **FR-2.1**: CRUD operations for organizations with flat hierarchy
- **FR-2.2**: Organization types: ENTERPRISE, SMB, STARTUP, PERSONAL
- **FR-2.3**: Organization status management: ACTIVE, SUSPENDED, PENDING, ARCHIVED
- **FR-2.4**: Configurable limits per organization (maxDevices, maxUsers, maxGroups)
- **FR-2.5**: Feature flag management per organization
- **FR-2.6**: Organization statistics and usage dashboards

### FR-3: User Administration

- **FR-3.1**: User CRUD with role assignment across multiple organizations
- **FR-3.2**: User status management: ACTIVE, SUSPENDED, PENDING_VERIFICATION, LOCKED
- **FR-3.3**: Admin-initiated password reset functionality
- **FR-3.4**: MFA management (force enable, reset tokens)
- **FR-3.5**: Session management (view active sessions, revoke all)
- **FR-3.6**: User activity audit trail

### FR-4: Device Administration

- **FR-4.1**: Device fleet view with advanced filtering and sorting
- **FR-4.2**: Device detail view with ownership, status, and metrics
- **FR-4.3**: Bulk operations (suspend, reactivate, delete)
- **FR-4.4**: Enrollment token management with QR code generation
- **FR-4.5**: Device policy assignment and compliance tracking
- **FR-4.6**: Inactive device management and cleanup

### FR-5: Groups Administration

- **FR-5.1**: View all groups across organizations
- **FR-5.2**: Group membership management with role changes
- **FR-5.3**: Group ownership transfer
- **FR-5.4**: Invite management (view, revoke)
- **FR-5.5**: Group suspension and archival

### FR-6: Location & Geofence Administration

- **FR-6.1**: Location explorer with map-based visualization
- **FR-6.2**: Location history queries across devices/organizations
- **FR-6.3**: Location data export (CSV, JSON, GPX formats)
- **FR-6.4**: Geofence management for any device
- **FR-6.5**: Geofence event history and analytics
- **FR-6.6**: Proximity alert configuration and monitoring
- **FR-6.7**: Data retention policy configuration

### FR-7: Webhook Administration

- **FR-7.1**: View all webhooks across organizations
- **FR-7.2**: Webhook delivery log viewer
- **FR-7.3**: Failed delivery retry management
- **FR-7.4**: Webhook endpoint testing
- **FR-7.5**: Secret rotation capability

### FR-8: Trip Administration

- **FR-8.1**: Trip list view with filtering
- **FR-8.2**: Trip detail with interactive map visualization
- **FR-8.3**: Movement event timeline
- **FR-8.4**: Trip data export

### FR-9: App Usage & Limits

- **FR-9.1**: App usage dashboard with aggregated statistics
- **FR-9.2**: Per-device app usage breakdown
- **FR-9.3**: App limit configuration (daily/weekly)
- **FR-9.4**: Time window restrictions (allowed hours by day)
- **FR-9.5**: Limit templates for reuse

### FR-10: Unlock Requests

- **FR-10.1**: Pending request queue with priority view
- **FR-10.2**: Approve/deny workflow with notes
- **FR-10.3**: Request history per device/user
- **FR-10.4**: Auto-approval rule configuration
- **FR-10.5**: Emergency bulk unlock capability

### FR-11: System Configuration

- **FR-11.1**: Authentication settings (registration, OAuth providers, session timeout)
- **FR-11.2**: Feature flag management (global)
- **FR-11.3**: Rate limit configuration per endpoint category
- **FR-11.4**: Data retention policy settings
- **FR-11.5**: Email template customization
- **FR-11.6**: API key management (create, rotate, revoke)

### FR-12: Dashboard & Analytics

- **FR-12.1**: Overview dashboard with key metrics
- **FR-12.2**: User growth and activity analytics
- **FR-12.3**: Device distribution and activity charts
- **FR-12.4**: Location volume and coverage analytics
- **FR-12.5**: API usage and error rate monitoring
- **FR-12.6**: Custom report generation

### FR-13: Audit & Compliance

- **FR-13.1**: Comprehensive audit log with search and filters
- **FR-13.2**: User activity reports
- **FR-13.3**: Organization activity reports
- **FR-13.4**: Data access reports (GDPR compliance)
- **FR-13.5**: Audit log export (CSV, JSON)
- **FR-13.6**: Tamper-evident log storage

---

## Non-Functional Requirements (NFRs)

### NFR-1: Security

- **NFR-1.1**: MFA required for all admin roles
- **NFR-1.2**: Session timeout: 15 minutes idle, 8 hours maximum
- **NFR-1.3**: Password requirements: 12+ characters with complexity rules
- **NFR-1.4**: Account lockout after 5 failed attempts
- **NFR-1.5**: IP allowlisting for SUPER_ADMIN role
- **NFR-1.6**: All API endpoints require valid JWT with permission checks
- **NFR-1.7**: Audit logging for all write operations

### NFR-2: Performance

- **NFR-2.1**: Dashboard load time <2 seconds
- **NFR-2.2**: List views support pagination with 100+ items per page
- **NFR-2.3**: Search/filter operations <500ms response time
- **NFR-2.4**: Export operations support 100K+ records
- **NFR-2.5**: Real-time updates for critical alerts

### NFR-3: Compliance

- **NFR-3.1**: GDPR: Right to access (user data export)
- **NFR-3.2**: GDPR: Right to erasure (user data deletion)
- **NFR-3.3**: GDPR: Data portability (standard export formats)
- **NFR-3.4**: Audit log retention: 365 days minimum
- **NFR-3.5**: SOC2-ready audit capabilities

### NFR-4: Usability

- **NFR-4.1**: Responsive design for tablet support
- **NFR-4.2**: Keyboard navigation for power users
- **NFR-4.3**: Consistent UI patterns across all modules
- **NFR-4.4**: Clear error messages and validation feedback
- **NFR-4.5**: Breadcrumb navigation for deep hierarchies

---

## Epics and User Stories

### Epic AP-1: RBAC & Access Control Foundation

**Priority:** Critical (Foundation)
**Estimated Effort:** High
**Dependencies:** None

#### Stories:

1. **Story AP-1.1**: As a super admin, I want to define system roles with specific permissions so I can control access across the platform
   - **Acceptance Criteria:**
     - 5 system roles implemented (SUPER_ADMIN, ORG_ADMIN, ORG_MANAGER, SUPPORT, VIEWER)
     - Role-permission mapping stored in database
     - API middleware validates permissions on every request
     - Permission denied returns 403 with clear message

2. **Story AP-1.2**: As a super admin, I want to assign roles to users for specific organizations so they have appropriate access
   - **Acceptance Criteria:**
     - User can have different roles in different organizations
     - Role assignment UI with organization selector
     - Role changes take effect immediately
     - Audit log captures role changes

3. **Story AP-1.3**: As a super admin, I want to create custom roles with selected permissions so I can fine-tune access control
   - **Acceptance Criteria:**
     - Custom role creation form with permission checkboxes
     - Custom roles can be assigned like system roles
     - Custom roles can be edited or deleted
     - Cannot delete role while users are assigned

4. **Story AP-1.4**: As an admin, I want all API endpoints protected by permission checks so unauthorized access is prevented
   - **Acceptance Criteria:**
     - All 45+ endpoints have permission requirements defined
     - Permission middleware applied to all routes
     - Organization-scoped data isolation enforced
     - Comprehensive test coverage for permission checks

---

### Epic AP-2: Organization Management

**Priority:** Critical
**Estimated Effort:** High
**Dependencies:** Epic AP-1

#### Stories:

1. **Story AP-2.1**: As a super admin, I want to create and manage organizations so I can onboard new customers
   - **Acceptance Criteria:**
     - Organization CRUD operations via UI
     - Required fields: name, slug, type, contactEmail
     - Slug uniqueness validated
     - Organization creation logged in audit

2. **Story AP-2.2**: As a super admin, I want to configure organization limits so I can control resource usage
   - **Acceptance Criteria:**
     - Configurable limits: maxDevices, maxUsers, maxGroups
     - Limit enforcement on relevant operations
     - Warning displayed when approaching limits
     - Limit changes logged

3. **Story AP-2.3**: As a super admin, I want to manage organization status so I can suspend or archive customers
   - **Acceptance Criteria:**
     - Status transitions: ACTIVE ↔ SUSPENDED, ACTIVE → ARCHIVED
     - Suspended orgs: users cannot login, devices stop syncing
     - Archived orgs: read-only, can be restored
     - Status changes require confirmation

4. **Story AP-2.4**: As a super admin, I want to enable/disable features per organization so I can control feature access
   - **Acceptance Criteria:**
     - Feature flag toggle UI per organization
     - Flags: geofences, proximityAlerts, webhooks, trips, etc.
     - Feature checks enforced in API
     - Changes take effect immediately

5. **Story AP-2.5**: As an org admin, I want to view my organization's statistics so I can monitor usage
   - **Acceptance Criteria:**
     - Dashboard shows: users, devices, groups, storage used
     - Usage trends over time (7d, 30d, 90d)
     - Limit utilization percentages
     - Export statistics as CSV

---

### Epic AP-3: User Administration

**Priority:** Critical
**Estimated Effort:** High
**Dependencies:** Epic AP-1, Epic AP-2

#### Stories:

1. **Story AP-3.1**: As an admin, I want to view and search all users so I can find specific accounts
   - **Acceptance Criteria:**
     - User list with pagination (100/page)
     - Search by email, name, organization
     - Filter by status, role, organization
     - Sort by name, email, created date, last login

2. **Story AP-3.2**: As an admin, I want to create users and assign them to organizations so I can onboard new team members
   - **Acceptance Criteria:**
     - User creation form with required fields
     - Organization and role assignment during creation
     - Email verification sent automatically
     - Welcome email with setup instructions

3. **Story AP-3.3**: As an admin, I want to suspend and reactivate users so I can manage access
   - **Acceptance Criteria:**
     - Suspend action with reason input
     - Suspended users cannot login
     - Active sessions terminated on suspension
     - Reactivation restores previous access

4. **Story AP-3.4**: As an admin, I want to reset user passwords so I can help locked-out users
   - **Acceptance Criteria:**
     - Password reset triggers email to user
     - Option to force password change on next login
     - Reset action logged in audit
     - Cannot reset SUPER_ADMIN passwords (self-service only)

5. **Story AP-3.5**: As an admin, I want to manage user sessions so I can terminate suspicious activity
   - **Acceptance Criteria:**
     - View active sessions (device, IP, last activity)
     - Revoke individual session
     - Revoke all sessions for user
     - Session revocation is immediate

6. **Story AP-3.6**: As an admin, I want to manage user MFA settings so I can enforce security policies
   - **Acceptance Criteria:**
     - View MFA status (enabled/disabled)
     - Force MFA enrollment for user
     - Reset MFA (user must re-enroll)
     - MFA changes logged

---

### Epic AP-4: Device Fleet Administration

**Priority:** High
**Estimated Effort:** High
**Dependencies:** Epic AP-1, Epic AP-2

#### Stories:

1. **Story AP-4.1**: As an admin, I want to view all devices with filtering so I can manage the device fleet
   - **Acceptance Criteria:**
     - Device list with pagination
     - Filter by organization, group, status, platform
     - Search by device name, UUID, owner email
     - Sort by name, last seen, location count

2. **Story AP-4.2**: As an admin, I want to view device details so I can troubleshoot issues
   - **Acceptance Criteria:**
     - Device info: name, UUID, platform, owner, organization
     - Status: online/offline, last seen, enrollment status
     - Metrics: location count, trip count, last location
     - Policy compliance status

3. **Story AP-4.3**: As an admin, I want to manage enrollment tokens so I can onboard devices
   - **Acceptance Criteria:**
     - Create token with: name, max uses, expiration, policy
     - Generate QR code for token
     - View token usage history
     - Revoke active tokens

4. **Story AP-4.4**: As an admin, I want to perform bulk operations on devices so I can manage at scale
   - **Acceptance Criteria:**
     - Multi-select devices from list
     - Bulk actions: suspend, reactivate, delete
     - Confirmation dialog with affected count
     - Progress indicator for large batches

5. **Story AP-4.5**: As an admin, I want to manage inactive devices so I can clean up the fleet
   - **Acceptance Criteria:**
     - List devices inactive for X days (configurable)
     - Bulk delete inactive devices
     - Send notification before auto-cleanup
     - Deleted device data retention per policy

---

### Epic AP-5: Groups Administration

**Priority:** Medium
**Estimated Effort:** Medium
**Dependencies:** Epic AP-1, Epic AP-2

#### Stories:

1. **Story AP-5.1**: As an admin, I want to view all groups so I can manage group structures
   - **Acceptance Criteria:**
     - Group list with organization filter
     - Show: name, owner, member count, device count
     - Search by group name
     - Sort by name, member count, created date

2. **Story AP-5.2**: As an admin, I want to manage group membership so I can adjust team structures
   - **Acceptance Criteria:**
     - View all members with roles
     - Change member roles (ADMIN, MEMBER)
     - Remove members from group
     - Force-add users to group

3. **Story AP-5.3**: As an admin, I want to transfer group ownership so I can handle departures
   - **Acceptance Criteria:**
     - Select new owner from members
     - Previous owner becomes ADMIN
     - Ownership transfer logged
     - Notification sent to new owner

4. **Story AP-5.4**: As an admin, I want to manage group invites so I can control access
   - **Acceptance Criteria:**
     - View pending invites per group
     - Revoke individual invites
     - Bulk revoke all invites for group
     - See invite history (accepted, expired, revoked)

---

### Epic AP-6: Location & Geofence Administration

**Priority:** Medium
**Estimated Effort:** High
**Dependencies:** Epic AP-1, Epic AP-4

#### Stories:

1. **Story AP-6.1**: As an admin, I want to explore location data on a map so I can visualize device activity
   - **Acceptance Criteria:**
     - Map view with device location markers
     - Filter by device, organization, date range
     - Cluster markers for performance
     - Click marker to see device details

2. **Story AP-6.2**: As an admin, I want to query location history so I can investigate activity
   - **Acceptance Criteria:**
     - Query by device, date range, geographic bounds
     - Results shown on map and in list
     - Export results (CSV, JSON, GPX)
     - Maximum 10,000 results per query

3. **Story AP-6.3**: As an admin, I want to manage geofences so I can configure location-based triggers
   - **Acceptance Criteria:**
     - List all geofences with filters
     - Create/edit geofences for any device
     - Map-based geofence editor (circle, polygon)
     - Enable/disable geofences

4. **Story AP-6.4**: As an admin, I want to view geofence events so I can monitor activity
   - **Acceptance Criteria:**
     - Event list with device, geofence, type, time
     - Filter by event type (ENTER, EXIT, DWELL)
     - Event timeline visualization
     - Export events for analysis

5. **Story AP-6.5**: As an admin, I want to manage proximity alerts so I can configure device-to-device monitoring
   - **Acceptance Criteria:**
     - List all proximity alerts
     - Create/edit alerts for any device pair
     - Configure trigger distance and cooldown
     - View alert trigger history

6. **Story AP-6.6**: As an admin, I want to configure data retention so I can manage storage and compliance
   - **Acceptance Criteria:**
     - Set retention period per organization
     - Automatic deletion of expired data
     - Manual purge option with confirmation
     - Retention policy visible to org admins

---

### Epic AP-7: Webhooks & Trips Administration

**Priority:** Medium
**Estimated Effort:** Medium
**Dependencies:** Epic AP-1, Epic AP-4

#### Stories:

1. **Story AP-7.1**: As an admin, I want to view all webhooks so I can monitor integrations
   - **Acceptance Criteria:**
     - Webhook list with status indicators
     - Filter by organization, status, event types
     - Show delivery success/failure counts
     - Search by URL or name

2. **Story AP-7.2**: As an admin, I want to view webhook delivery logs so I can troubleshoot failures
   - **Acceptance Criteria:**
     - Delivery log with request/response details
     - Filter by status (success, failed)
     - View retry attempts and errors
     - Resend failed deliveries

3. **Story AP-7.3**: As an admin, I want to test webhooks so I can verify configurations
   - **Acceptance Criteria:**
     - Send test event to webhook
     - Show response status and body
     - Validate endpoint accessibility
     - Test does not count against rate limits

4. **Story AP-7.4**: As an admin, I want to view trip data so I can analyze movement patterns
   - **Acceptance Criteria:**
     - Trip list with device, status, duration, distance
     - Trip detail with interactive map
     - Path visualization with start/end markers
     - Movement event timeline

5. **Story AP-7.5**: As an admin, I want to export trip data so I can perform offline analysis
   - **Acceptance Criteria:**
     - Export by device, date range, organization
     - Formats: CSV, JSON
     - Include path coordinates option
     - Async export for large datasets

---

### Epic AP-8: App Usage & Unlock Requests

**Priority:** Medium
**Estimated Effort:** Medium
**Dependencies:** Epic AP-1, Epic AP-4

#### Stories:

1. **Story AP-8.1**: As an admin, I want to view app usage statistics so I can understand device activity
   - **Acceptance Criteria:**
     - Aggregated usage by app category
     - Per-device usage breakdown
     - Time-based usage charts
     - Top apps by usage time

2. **Story AP-8.2**: As an admin, I want to configure app limits so I can control device usage
   - **Acceptance Criteria:**
     - Set daily/weekly time limits per app
     - Configure allowed time windows
     - Block apps completely
     - Apply limits to device or group

3. **Story AP-8.3**: As an admin, I want to create limit templates so I can reuse configurations
   - **Acceptance Criteria:**
     - Create named templates with limit rules
     - Apply template to devices/groups
     - Edit template updates all linked devices
     - Delete template with replacement option

4. **Story AP-8.4**: As an admin, I want to manage unlock requests so I can approve device access
   - **Acceptance Criteria:**
     - Pending request queue sorted by time
     - View request details (device, user, reason)
     - Approve with duration or deny with note
     - Request history per device

5. **Story AP-8.5**: As an admin, I want to configure auto-approval rules so I can reduce manual work
   - **Acceptance Criteria:**
     - Rules based on time, user, device
     - Maximum auto-approval duration
     - Rule priority ordering
     - Audit log for auto-approvals

---

### Epic AP-9: System Configuration

**Priority:** High
**Estimated Effort:** Medium
**Dependencies:** Epic AP-1

#### Stories:

1. **Story AP-9.1**: As a super admin, I want to configure authentication settings so I can control platform security
   - **Acceptance Criteria:**
     - Toggle: registration enabled, invite only, OAuth only
     - Configure OAuth providers (Google, Apple)
     - Set session timeout and max attempts
     - Lockout duration configuration

2. **Story AP-9.2**: As a super admin, I want to manage feature flags so I can control platform capabilities
   - **Acceptance Criteria:**
     - Global feature toggle UI
     - Features: geofences, alerts, webhooks, trips, etc.
     - Changes require confirmation
     - Feature status visible in config

3. **Story AP-9.3**: As a super admin, I want to configure rate limits so I can protect platform stability
   - **Acceptance Criteria:**
     - Configure limits per endpoint category
     - Set requests per window (minute, hour, day)
     - Override limits per organization
     - Rate limit metrics visible in dashboard

4. **Story AP-9.4**: As a super admin, I want to manage API keys so I can enable system integrations
   - **Acceptance Criteria:**
     - Create API key with name and permissions
     - Set expiration and rate limits
     - View usage statistics per key
     - Rotate key without downtime

5. **Story AP-9.5**: As a super admin, I want to configure data retention so I can manage storage and compliance
   - **Acceptance Criteria:**
     - Set default retention periods
     - Retention for: locations, audit logs, trips
     - Configure inactive device cleanup
     - Retention policy documentation

---

### Epic AP-10: Dashboard & Analytics

**Priority:** High
**Estimated Effort:** Medium
**Dependencies:** Epic AP-1, Epic AP-2, Epic AP-3, Epic AP-4

#### Stories:

1. **Story AP-10.1**: As an admin, I want an overview dashboard so I can see platform health at a glance
   - **Acceptance Criteria:**
     - Key metrics: users, devices, organizations, groups
     - Activity counts: new today, active today
     - Alert indicators: pending requests, failed webhooks
     - Quick action buttons

2. **Story AP-10.2**: As an admin, I want user analytics so I can track adoption
   - **Acceptance Criteria:**
     - User growth chart (daily, weekly, monthly)
     - Active users over time
     - User retention metrics
     - New vs returning users

3. **Story AP-10.3**: As an admin, I want device analytics so I can monitor fleet health
   - **Acceptance Criteria:**
     - Device distribution by platform, status
     - Online/offline device counts over time
     - Location upload volume
     - Device activity heatmap

4. **Story AP-10.4**: As an admin, I want API analytics so I can monitor system usage
   - **Acceptance Criteria:**
     - Request volume by endpoint
     - Response time percentiles
     - Error rate tracking
     - Top consumers by request count

5. **Story AP-10.5**: As an admin, I want to generate custom reports so I can analyze specific metrics
   - **Acceptance Criteria:**
     - Report builder with metric selection
     - Date range and filter options
     - Export as PDF or CSV
     - Save report configurations

---

### Epic AP-11: Audit & Compliance

**Priority:** High
**Estimated Effort:** High
**Dependencies:** Epic AP-1

#### Stories:

1. **Story AP-11.1**: As an admin, I want comprehensive audit logging so I can track all changes
   - **Acceptance Criteria:**
     - Log: actor, action, resource, timestamp
     - Include: IP address, user agent
     - Capture: before/after state for changes
     - Log all admin write operations

2. **Story AP-11.2**: As an admin, I want to search audit logs so I can investigate incidents
   - **Acceptance Criteria:**
     - Search by actor, action, resource, date range
     - Filter by organization, action type
     - Results with full detail view
     - Export search results

3. **Story AP-11.3**: As an admin, I want user activity reports so I can review individual actions
   - **Acceptance Criteria:**
     - Activity timeline per user
     - Actions grouped by type
     - Date range filtering
     - Export as PDF or CSV

4. **Story AP-11.4**: As an admin, I want organization activity reports so I can monitor usage
   - **Acceptance Criteria:**
     - Activity summary per organization
     - User action counts
     - Resource changes summary
     - Anomaly highlighting

5. **Story AP-11.5**: As an admin, I want GDPR compliance reports so I can fulfill data requests
   - **Acceptance Criteria:**
     - User data export (all data types)
     - Data deletion with confirmation
     - Deletion verification report
     - Export in portable format

6. **Story AP-11.6**: As a super admin, I want tamper-evident audit storage so I can ensure log integrity
   - **Acceptance Criteria:**
     - Audit logs are append-only
     - Hash chain for integrity verification
     - Tampering detection alerts
     - Retention policy enforcement

---

### Epic AP-12: Admin Portal UI Shell

**Priority:** Critical (Foundation)
**Estimated Effort:** High
**Dependencies:** Epic AP-1

#### Stories:

1. **Story AP-12.1**: As a developer, I want to set up the admin portal project so I have a foundation
   - **Acceptance Criteria:**
     - Next.js 14+ with App Router
     - TypeScript configuration
     - Tailwind CSS with design system
     - Component library (shadcn/ui or similar)

2. **Story AP-12.2**: As an admin, I want a responsive navigation system so I can access all features
   - **Acceptance Criteria:**
     - Sidebar navigation with icons
     - Collapsible menu sections
     - Mobile-responsive drawer
     - Active state highlighting

3. **Story AP-12.3**: As an admin, I want permission-based UI so I only see features I can access
   - **Acceptance Criteria:**
     - Navigation items filtered by permissions
     - UI components check permissions
     - Disabled states for read-only access
     - Clear messaging for denied access

4. **Story AP-12.4**: As an admin, I want consistent list/detail patterns so I can navigate efficiently
   - **Acceptance Criteria:**
     - Reusable data table component
     - Standard pagination and filtering
     - Detail panel/page pattern
     - Consistent action button placement

5. **Story AP-12.5**: As an admin, I want a notification system so I can stay informed of important events
   - **Acceptance Criteria:**
     - Toast notifications for actions
     - Persistent notification center
     - Unread indicator badge
     - Notification preferences

---

## Admin Portal Navigation Structure

```
Dashboard
├── Overview
├── Activity Feed
└── Quick Actions

Users
├── All Users
├── By Organization
├── By Role
├── Suspended Users
└── Pending Verification

Organizations
├── All Organizations
├── By Type
├── By Status
└── Create New

Devices
├── Device Fleet
├── By Organization
├── By Status
├── Enrollment Tokens
└── Bulk Actions

Groups
├── All Groups
├── Group Invites
└── Suspended Groups

Locations
├── Location Explorer
├── Location History
└── Data Export

Geofences
├── All Geofences
├── Geofence Events
└── Proximity Alerts

Trips
├── All Trips
├── Movement Events
└── Trip Analytics

App Management
├── Usage Dashboard
├── App Limits
└── Limit Templates

Unlock Requests
├── Pending Queue
├── Request History
└── Auto-Rules

Webhooks
├── All Webhooks
├── Delivery Logs
└── Failed Deliveries

Settings
├── Authentication
├── Feature Flags
├── Rate Limits
├── Data Retention
└── API Keys

Audit
├── Activity Log
├── User Sessions
├── Compliance Reports
└── Export

Access Control
├── Roles
├── Permissions
└── Custom Roles
```

---

## Permission Catalog

| Permission Code | Description |
|-----------------|-------------|
| `USERS.CREATE` | Create new users |
| `USERS.READ` | View user details |
| `USERS.UPDATE` | Modify user profiles |
| `USERS.DELETE` | Delete users |
| `USERS.SUSPEND` | Suspend/activate users |
| `USERS.RESET_PASSWORD` | Reset user passwords |
| `ORGANIZATIONS.CREATE` | Create organizations |
| `ORGANIZATIONS.READ` | View organization details |
| `ORGANIZATIONS.UPDATE` | Modify organizations |
| `ORGANIZATIONS.DELETE` | Delete organizations |
| `ORGANIZATIONS.SUSPEND` | Suspend organizations |
| `DEVICES.CREATE` | Register devices |
| `DEVICES.READ` | View device details |
| `DEVICES.UPDATE` | Modify devices |
| `DEVICES.DELETE` | Delete devices |
| `DEVICES.ADMIN_READ` | Admin device view |
| `DEVICES.EXPORT` | Export device data |
| `LOCATIONS.READ` | View location data |
| `LOCATIONS.EXPORT` | Export location data |
| `LOCATIONS.DELETE` | Delete location data |
| `GEOFENCES.CREATE` | Create geofences |
| `GEOFENCES.READ` | View geofences |
| `GEOFENCES.UPDATE` | Modify geofences |
| `GEOFENCES.DELETE` | Delete geofences |
| `GEOFENCE_EVENTS.READ` | View geofence events |
| `ALERTS.CREATE` | Create proximity alerts |
| `ALERTS.READ` | View alerts |
| `ALERTS.UPDATE` | Modify alerts |
| `ALERTS.DELETE` | Delete alerts |
| `WEBHOOKS.CREATE` | Create webhooks |
| `WEBHOOKS.READ` | View webhooks |
| `WEBHOOKS.UPDATE` | Modify webhooks |
| `WEBHOOKS.DELETE` | Delete webhooks |
| `TRIPS.READ` | View trips |
| `TRIPS.EXPORT` | Export trip data |
| `GROUPS.CREATE` | Create groups |
| `GROUPS.READ` | View groups |
| `GROUPS.UPDATE` | Modify groups |
| `GROUPS.DELETE` | Delete groups |
| `GROUPS.ADMIN` | Admin group operations |
| `ENROLLMENT.CREATE` | Create enrollment tokens |
| `ENROLLMENT.READ` | View enrollment tokens |
| `ENROLLMENT.DELETE` | Revoke tokens |
| `APP_LIMITS.CREATE` | Create app limits |
| `APP_LIMITS.READ` | View app limits |
| `APP_LIMITS.UPDATE` | Modify app limits |
| `APP_LIMITS.DELETE` | Delete app limits |
| `UNLOCK.APPROVE` | Approve unlock requests |
| `UNLOCK.DENY` | Deny unlock requests |
| `AUDIT.READ` | View audit logs |
| `AUDIT.EXPORT` | Export audit logs |
| `CONFIG.READ` | View system config |
| `CONFIG.UPDATE` | Modify system config |
| `API_KEYS.CREATE` | Create API keys |
| `API_KEYS.READ` | View API keys |
| `API_KEYS.REVOKE` | Revoke API keys |
| `REPORTS.READ` | View reports |
| `REPORTS.EXPORT` | Export reports |

---

## Role Permission Matrix

| Role | USERS | ORGS | DEVICES | GROUPS | LOCATIONS | GEOFENCES | ALERTS | WEBHOOKS | TRIPS | REPORTS | SETTINGS |
|------|-------|------|---------|--------|-----------|-----------|--------|----------|-------|---------|----------|
| SUPER_ADMIN | CRUD | CRUD | CRUD | CRUD | CRUD | CRUD | CRUD | CRUD | CRUD | CRUD | CRUD |
| ORG_ADMIN | CRUD* | R* | CRUD* | CRUD* | CRUD* | CRUD* | CRUD* | CRUD* | CRUD* | CRUD* | CRU* |
| ORG_MANAGER | RU* | R* | RU* | RU* | R* | RU* | RU* | R* | R* | R* | R* |
| SUPPORT | R* | R* | R* | R* | R* | R* | R* | R* | R* | R* | - |
| VIEWER | - | - | R* | R* | - | - | - | - | - | R* | - |

*Scoped to assigned organization(s)

---

## Brownfield Integration Requirements

This is a **brownfield project** that must integrate with the existing Phone Manager backend infrastructure.

### Existing Systems to Leverage

| System | Current State | Integration Approach |
|--------|---------------|---------------------|
| **Authentication** | JWT + refresh tokens, Google/Apple OAuth | Extend with admin roles, add MFA |
| **Database** | PostgreSQL with existing schemas | Add new tables, extend existing entities |
| **API Layer** | NestJS REST API (45+ endpoints) | Add admin endpoints, enhance existing |
| **Authorization** | Group-level RBAC (OWNER/ADMIN/MEMBER) | Extend to system-level RBAC |
| **Device Management** | Basic admin endpoints exist | Expand with fleet management |
| **Configuration** | Public config endpoint exists | Extend with admin settings |

### Database Schema Extensions

**New Tables Required:**
- `system_roles` - System-level role definitions
- `user_system_roles` - User-to-system-role assignments
- `permissions` - Granular permission definitions
- `role_permissions` - Role-to-permission mappings
- `organizations` - Organization entity (if not exists)
- `audit_logs` - Admin action audit trail
- `api_keys` - System API key management

**Existing Tables to Extend:**
- `users` - Add `systemRole`, `mfaEnabled`, `status` fields
- `devices` - Add `organizationId`, `policyId`, `status` fields
- `groups` - Add `organizationId`, `status` fields

### API Integration Patterns

**Must Follow Existing Patterns:**
- NestJS controller/service/repository architecture
- Existing error response format
- Current authentication middleware
- Established validation patterns (class-validator)
- Existing pagination/filtering conventions

**New Admin Endpoints:**
- Mount under `/api/admin/*` namespace
- Reuse existing DTOs where applicable
- Extend existing services, don't duplicate

### Migration Considerations

1. **Data Migration**: Existing users/devices need default organization assignment
2. **Permission Migration**: Map existing group roles to new permission system
3. **Backward Compatibility**: Existing mobile app must continue working
4. **Rollout Strategy**: Feature flags for gradual admin portal enablement

---

## Technical Constraints

1. **Frontend**: Next.js 14+ with App Router, TypeScript, Tailwind CSS
2. **Authentication**: Extend existing JWT system, add MFA support
3. **Authorization**: Build on existing auth middleware, add permission checks
4. **API**: Follow existing NestJS patterns, extend existing services
5. **Database**: PostgreSQL - extend existing schema, use existing ORM (TypeORM/Prisma)
6. **State Management**: React Query for server state, Zustand for client state
7. **Forms**: React Hook Form with Zod validation
8. **Tables**: TanStack Table for data grids
9. **Maps**: Mapbox GL or Google Maps for location visualization
10. **Charts**: Recharts or Chart.js for analytics
11. **Testing**: Jest + React Testing Library, Playwright for E2E

---

## Out of Scope (Phase 1)

- Mobile admin app
- Real-time collaboration features
- Advanced reporting/BI tools
- White-labeling/custom branding
- Third-party SSO (SAML, LDAP)
- Billing and subscription management
- Multi-language support
- Advanced workflow automation

---

## Success Metrics

1. **Adoption**: 80%+ admin tasks completed through portal
2. **Efficiency**: 50% reduction in admin task completion time
3. **Reliability**: 99.9% uptime for admin portal
4. **Performance**: <2s page load, <500ms API response
5. **Security**: Zero unauthorized access incidents
6. **Compliance**: 100% audit log coverage for admin actions

---

## Risks and Mitigations

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| Permission system complexity | Medium | High | Start with core roles, iterate based on feedback |
| Performance with large datasets | Medium | Medium | Implement pagination, caching, lazy loading |
| Security vulnerabilities | Low | Critical | Security review, penetration testing, audit logging |
| Scope creep | High | Medium | Strict epic boundaries, phased delivery |
| Integration with existing API | Medium | Medium | API versioning, backward compatibility |

---

## Document Status

- [x] Goals and context validated
- [x] All functional requirements reviewed
- [x] Epics structured for phased delivery
- [x] Permission model defined
- [ ] Ready for architecture phase

---

## Approval

**Status**: Draft
**PRD Version**: 1.0
**Last Updated**: 2025-12-08
**Next Phase**: Solution Architecture

---

_This PRD was created as part of the BMAD solution architecture workflow. This is a standalone PRD for the Admin Portal with AP-prefixed numbering (Epic AP-1 to AP-12, Story AP-x.x, FR-1 to FR-13, NFR-1 to NFR-4)._

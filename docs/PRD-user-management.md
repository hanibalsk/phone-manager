# Phone Manager - User Management & Device Control PRD

**Author:** Martin
**Date:** 2025-12-01
**Project Level:** 3 (Full Product)
**Project Type:** Mobile Application Enhancement
**Target Scale:** ~50 stories across 6 epics
**Source:** Specification files (docs/specs/)

---

## Description, Context and Goals

### Description

Add comprehensive user management, authentication, group-based access control, and remote device settings management to Phone Manager. This feature transforms Phone Manager from a device-centric to a user-centric platform while maintaining full backward compatibility with existing device-only installations.

### Deployment Intent

- **Android App**: Google Play Store update
- **Backend**: Rust/Axum API extension
- **Admin Portal**: New Next.js web application
- **Target Audience**: B2C families, B2B enterprises

### Context

Phone Manager currently operates in a device-centric model where each device has a unique API key and belongs to a group identified by a simple group ID. There is no user authentication, ownership model, or administrative control over device settings.

**Current State:**
- Device registration via X-API-Key
- Simple group membership by groupId string
- All settings controlled locally on device
- No user accounts
- No administrative oversight

**Desired State:**
- User accounts with email/password and OAuth
- Devices owned by users
- Groups with role-based membership (owner, admin, member)
- Remote settings control by administrators
- Setting locks that prevent user modification
- B2B support for enterprise device fleets
- Full admin web portal

### Goals

1. **Enable User Identity** - Users can create accounts and authenticate across devices
2. **Establish Device Ownership** - Devices are linked to user accounts
3. **Implement Group Hierarchy** - Groups have owners and admins who can manage members
4. **Remote Settings Control** - Administrators can view and modify member device settings
5. **Setting Locks** - Administrators can lock settings to prevent user changes
6. **B2B Enterprise Support** - Companies can provision devices and manage fleets
7. **Maintain Backward Compatibility** - Existing devices continue working without accounts

---

## Requirements

### Functional Requirements

#### FR-9: Authentication
| ID | Requirement | Priority |
|----|-------------|----------|
| FR-9.1 | Users can register with email/password | Critical |
| FR-9.2 | Users can log in with email/password | Critical |
| FR-9.3 | Users can log in with Google OAuth | High |
| FR-9.4 | Users can log in with Apple Sign-In | High |
| FR-9.5 | JWT tokens are securely stored and auto-refreshed | Critical |
| FR-9.6 | Users can reset password via email | High |
| FR-9.7 | Users can log out (invalidate tokens) | High |
| FR-9.8 | Email verification is supported | Medium |

#### FR-10: User-Device Binding
| ID | Requirement | Priority |
|----|-------------|----------|
| FR-10.1 | Users can link devices to their account | Critical |
| FR-10.2 | Users can view all their linked devices | High |
| FR-10.3 | Users can unlink devices from their account | High |
| FR-10.4 | Users can transfer device ownership | Medium |
| FR-10.5 | Device registration works with/without authentication | Critical |

#### FR-11: Group Management
| ID | Requirement | Priority |
|----|-------------|----------|
| FR-11.1 | Users can create groups | Critical |
| FR-11.2 | Users can invite others via invite codes | Critical |
| FR-11.3 | Users can join groups with invite codes | Critical |
| FR-11.4 | Group owners can manage member roles | High |
| FR-11.5 | Admins can view group member devices | High |
| FR-11.6 | Group owners can delete groups | High |
| FR-11.7 | Users can belong to multiple groups | High |
| FR-11.8 | Users can leave groups (except owners) | High |

#### FR-12: Settings Control
| ID | Requirement | Priority |
|----|-------------|----------|
| FR-12.1 | Admins can view member device settings | Critical |
| FR-12.2 | Admins can modify member device settings | Critical |
| FR-12.3 | Admins can lock specific settings | Critical |
| FR-12.4 | Users see locked settings with lock indicator | High |
| FR-12.5 | Users can request setting unlocks | High |
| FR-12.6 | Admins can approve/deny unlock requests | High |
| FR-12.7 | Settings sync between server and device | Critical |
| FR-12.8 | Push notifications for settings changes | High |

#### FR-13: B2B Enterprise
| ID | Requirement | Priority |
|----|-------------|----------|
| FR-13.1 | Organizations can be created | High |
| FR-13.2 | Device policies can be defined | High |
| FR-13.3 | Devices can be enrolled via tokens/QR | High |
| FR-13.4 | Policies automatically applied to enrolled devices | High |
| FR-13.5 | Bulk device import supported | Medium |
| FR-13.6 | All admin actions are audited | High |

#### FR-14: Admin Portal
| ID | Requirement | Priority |
|----|-------------|----------|
| FR-14.1 | Web-based admin dashboard | High |
| FR-14.2 | Device fleet management | High |
| FR-14.3 | User and group management | High |
| FR-14.4 | Policy creation and assignment | High |
| FR-14.5 | Audit log viewing and export | High |

### Non-Functional Requirements

| Category | Requirement | Target |
|----------|-------------|--------|
| **Security** | Password hashing | Argon2id |
| **Security** | Token algorithm | RS256 JWT |
| **Security** | Access token lifetime | 1 hour |
| **Security** | Refresh token lifetime | 30 days |
| **Performance** | Auth endpoint latency | < 500ms |
| **Performance** | Settings sync latency | < 5 seconds |
| **Availability** | Backward compatibility | 100% for existing devices |
| **Scalability** | Concurrent users | 10,000+ |
| **Compliance** | Data protection | GDPR-ready |

---

## User Journeys

### Journey 1: New User Registration
```
1. User opens app → Sees "Sign In" option in settings
2. User taps "Create Account" → Registration form
3. User enters email, password, display name
4. System creates account → Issues JWT tokens
5. User's current device automatically linked to account
6. User can now see their device in "My Devices"
```

### Journey 2: Join a Family Group
```
1. Family admin creates group "Smith Family"
2. Admin generates invite code → Shares with family member
3. Family member opens app → Taps "Join Group"
4. Enters invite code → Joins group as member
5. Can now see family members' locations on map
```

### Journey 3: Admin Locks Setting
```
1. Parent (admin) opens group settings
2. Selects child's device → Views device settings
3. Toggles "tracking_enabled" lock ON
4. Child receives push notification: "Settings updated by admin"
5. Child sees lock icon on tracking setting → Cannot change
```

### Journey 4: User Requests Unlock
```
1. User sees locked setting they want to change
2. Taps "Request Unlock" → Enters reason
3. Admin receives notification of unlock request
4. Admin reviews and approves request
5. User receives notification → Setting now unlocked
```

### Journey 5: B2B Device Enrollment
```
1. IT admin logs into Admin Portal
2. Creates enrollment token for department
3. Generates QR code → Distributes to employees
4. Employee scans QR during app setup
5. Device automatically configured with company policy
6. IT admin sees device in fleet dashboard
```

---

## UX Design Principles

### 1. Progressive Disclosure
- Core functionality available without account
- Advanced features unlock with registration
- B2B features hidden from consumer users

### 2. Clear Permission Indicators
- Lock icons on locked settings
- "Managed by [admin]" labels where applicable
- Clear role badges in group member lists

### 3. Non-Intrusive Admin Control
- Push notifications for settings changes
- Ability to request unlocks
- Transparent about who changed what

### 4. Backward Compatibility First
- No forced migration prompts
- Existing devices work indefinitely
- Account creation is optional

---

## Epics

### Epic 9: Authentication Foundation

**Priority:** Critical
**Estimated Effort:** 3-4 weeks
**Dependencies:** None
**Stories:** 11

Implement user authentication with email/password and OAuth providers.

| Story | Title | Effort | Priority |
|-------|-------|--------|----------|
| E9.1 | Database schema for users and sessions | M | Critical |
| E9.2 | Password hashing with Argon2id | S | Critical |
| E9.3 | JWT infrastructure (RS256) | M | Critical |
| E9.4 | Register endpoint | M | Critical |
| E9.5 | Login endpoint | M | Critical |
| E9.6 | Logout and token invalidation | S | High |
| E9.7 | Refresh token rotation | M | Critical |
| E9.8 | Google OAuth integration | M | High |
| E9.9 | Apple Sign-In integration | M | High |
| E9.10 | Password reset flow | M | High |
| E9.11 | Android auth UI and token storage | L | Critical |

---

### Epic 10: User-Device Binding

**Priority:** High
**Estimated Effort:** 2 weeks
**Dependencies:** Epic 9
**Stories:** 6

Link devices to user accounts while maintaining backward compatibility.

| Story | Title | Effort | Priority |
|-------|-------|--------|----------|
| E10.1 | Device table migration (ownership fields) | S | Critical |
| E10.2 | Link device endpoint | M | Critical |
| E10.3 | Unlink device endpoint | S | High |
| E10.4 | Transfer device ownership | M | Medium |
| E10.5 | Device registration update (auth optional) | M | Critical |
| E10.6 | Android device management UI | M | High |

---

### Epic 11: Group Management

**Priority:** High
**Estimated Effort:** 3 weeks
**Dependencies:** Epic 10
**Stories:** 9

Full group lifecycle with role-based permissions.

| Story | Title | Effort | Priority |
|-------|-------|--------|----------|
| E11.1 | Database schema for groups and memberships | M | Critical |
| E11.2 | Group CRUD endpoints | M | Critical |
| E11.3 | Membership management endpoints | M | High |
| E11.4 | Role management (owner, admin, member) | M | High |
| E11.5 | Invite CRUD endpoints | M | Critical |
| E11.6 | Join group with code | M | Critical |
| E11.7 | Permission middleware (RBAC) | M | High |
| E11.8 | Android group list and detail screens | L | High |
| E11.9 | Android invite sharing UI | M | High |

---

### Epic 12: Settings Control

**Priority:** High
**Estimated Effort:** 2-3 weeks
**Dependencies:** Epic 11
**Stories:** 8

Remote device settings management with locking.

| Story | Title | Effort | Priority |
|-------|-------|--------|----------|
| E12.1 | Device settings table with locks | M | Critical |
| E12.2 | Get/Update settings endpoints | M | Critical |
| E12.3 | Lock/Unlock settings endpoints | M | Critical |
| E12.4 | Unlock request workflow | M | High |
| E12.5 | Settings sync push notifications | M | High |
| E12.6 | Android settings screen (lock indicators) | M | High |
| E12.7 | Android admin settings management | L | High |
| E12.8 | Android unlock request UI | M | High |

---

### Epic 13: B2B Enterprise Features

**Priority:** Medium
**Estimated Effort:** 4 weeks
**Dependencies:** Epic 12
**Stories:** 10

Organization management, policies, and device fleet control.

| Story | Title | Effort | Priority |
|-------|-------|--------|----------|
| E13.1 | Organizations table and CRUD | M | High |
| E13.2 | Organization users management | M | High |
| E13.3 | Device policies table and CRUD | M | High |
| E13.4 | Enrollment tokens management | M | High |
| E13.5 | Device enrollment flow | L | High |
| E13.6 | Policy engine (resolution algorithm) | L | High |
| E13.7 | Bulk device import | M | Medium |
| E13.8 | Audit logging system | M | High |
| E13.9 | Audit query and export | M | Medium |
| E13.10 | Android enrollment flow | M | High |

---

### Epic 14: Admin Web Portal

**Priority:** Medium
**Estimated Effort:** 4-5 weeks
**Dependencies:** Epic 13
**Stories:** 9

Web-based administration dashboard for enterprise customers.

| Story | Title | Effort | Priority |
|-------|-------|--------|----------|
| E14.1 | Portal project setup (Next.js) | M | High |
| E14.2 | Authentication and session management | M | High |
| E14.3 | Dashboard overview | M | High |
| E14.4 | Device fleet management | L | High |
| E14.5 | User management | M | High |
| E14.6 | Group management | M | High |
| E14.7 | Policy management | L | High |
| E14.8 | Audit log viewer | M | High |
| E14.9 | Reports and exports | M | Medium |

---

## Epic Summary

| Epic | Name | Stories | Effort | Dependencies |
|------|------|---------|--------|--------------|
| E9 | Authentication Foundation | 11 | 3-4 weeks | None |
| E10 | User-Device Binding | 6 | 2 weeks | E9 |
| E11 | Group Management | 9 | 3 weeks | E10 |
| E12 | Settings Control | 8 | 2-3 weeks | E11 |
| E13 | B2B Enterprise | 10 | 4 weeks | E12 |
| E14 | Admin Portal | 9 | 4-5 weeks | E13 |
| **Total** | | **53** | **18-22 weeks** | |

**Note:** Stories use new numbering (E9.x - E14.x) to avoid conflicts with existing stories (E1.x - E8.x).

---

## Out of Scope

The following are explicitly out of scope for this PRD:

1. **Multi-tenancy database isolation** - Single database with org_id filtering
2. **SSO/SAML integration** - OAuth only for initial release
3. **Two-factor authentication** - Planned for future release
4. **Mobile device management (MDM)** - Focus on app-level settings only
5. **Real-time collaboration** - Polling-based updates only
6. **White-label customization** - Standard branding only
7. **Offline-first architecture** - Requires connectivity for auth/sync

---

## Next Steps

1. **Architecture Review** - Review with Architect agent for technical feasibility
2. **Security Review** - Engage security review for auth/token design
3. **Epic Prioritization** - Confirm epic order with stakeholders
4. **Story Breakdown** - Create detailed story files for Epic 9 first
5. **API Design Review** - Validate API contracts with frontend team

---

## Related Specifications

All detailed specifications are in `/docs/specs/`:

| Document | Description |
|----------|-------------|
| `DATA_MODELS_SPEC.md` | Domain models, API contracts |
| `SECURITY_SPEC.md` | JWT design, password hashing, RBAC |
| `IMPLEMENTATION_ROADMAP.md` | Phased implementation plan |
| `DATABASE_SCHEMA_SPEC.md` | PostgreSQL schema design (backend) |
| `AUTH_API_SPEC.md` | Authentication endpoints (backend) |
| `USER_GROUP_API_SPEC.md` | User/Group endpoints (backend) |
| `DEVICE_SETTINGS_API_SPEC.md` | Settings endpoints (backend) |
| `B2B_ENTERPRISE_SPEC.md` | Enterprise features (backend) |
| `ADMIN_PORTAL_SPEC.md` | Admin dashboard (backend) |
| `USER_MANAGEMENT_SPEC.md` | Auth implementation (frontend) |
| `GROUP_MANAGEMENT_SPEC.md` | Group features (frontend) |
| `DEVICE_MANAGEMENT_SPEC.md` | Device features (frontend) |
| `SETTINGS_CONTROL_SPEC.md` | Settings sync (frontend) |
| `UI_SCREENS_SPEC.md` | Compose UI designs (frontend) |

---

## Document Status

- [x] Goals and context validated with stakeholders
- [x] All functional requirements reviewed
- [x] User journeys cover all major personas
- [x] Epic structure approved for phased delivery
- [x] Detailed specifications created
- [ ] Ready for architecture phase

---

## Version History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0.0 | 2025-12-01 | Martin | Initial PRD from specifications |

---

_This PRD covers Level 3 (Full Product) scope with 53 stories across 6 epics, estimated at 18-22 weeks with parallel work streams._

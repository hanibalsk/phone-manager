# Engineering Backlog

This backlog collects cross-cutting or future action items that emerge from reviews and planning.

Routing guidance:

- Use this file for non-urgent optimizations, refactors, or follow-ups that span multiple stories/epics.
- Must-fix items to ship a story belong in that story's `Tasks / Subtasks`.
- Same-epic improvements may also be captured under the epic Tech Spec `Post-Review Follow-ups` section.

## Critical Security Issues (Epic 14 - Admin Portal)

| Date | Story | Epic | Type | Severity | Owner | Status | Notes |
| ---- | ----- | ---- | ---- | -------- | ----- | ------ | ----- |
| 2025-12-02 | E14.* | 14 | Security | Critical | Backend | Open | Missing authentication - implement NextAuth.js |
| 2025-12-02 | E14.6 | 14 | Security | Critical | Backend | Open | PIN stored as plaintext - implement bcrypt hashing |
| 2025-12-02 | E14.* | 14 | Security | High | Backend | Open | No CSRF protection |
| 2025-12-02 | E14.* | 14 | Security | High | Backend | Open | Missing rate limiting |

## Infrastructure Issues (Epic 14 - Admin Portal)

| Date | Story | Epic | Type | Severity | Owner | Status | Notes |
| ---- | ----- | ---- | ---- | -------- | ----- | ------ | ----- |
| 2025-12-02 | E14.1 | 14 | Testing | Critical | Frontend | Open | No testing infrastructure (Jest/Playwright) |
| 2025-12-02 | E14.* | 14 | Error Handling | Critical | Frontend | Open | No error boundaries |
| 2025-12-02 | E14.* | 14 | Validation | High | Frontend | Open | Add Zod schema validation for all forms |
| 2025-12-02 | E14.* | 14 | Caching | Medium | Frontend | Open | Implement React Query for data caching |

## Accessibility Issues (Epic 14 - Admin Portal)

| Date | Story | Epic | Type | Severity | Owner | Status | Notes |
| ---- | ----- | ---- | ---- | -------- | ----- | ------ | ----- |
| 2025-12-02 | E14.* | 14 | A11y | Medium | Frontend | Open | Modal dialogs lack focus trap |
| 2025-12-02 | E14.* | 14 | A11y | Medium | Frontend | Open | Status badges missing ARIA labels |
| 2025-12-02 | E14.* | 14 | A11y | Low | Frontend | Open | No keyboard navigation for lists |

## UX Improvements (Epic 14 - Admin Portal)

| Date | Story | Epic | Type | Severity | Owner | Status | Notes |
| ---- | ----- | ---- | ---- | -------- | ----- | ------ | ----- |
| 2025-12-02 | E14.3 | 14 | UX | High | Frontend | Open | Add loading states for mutations |
| 2025-12-02 | E14.* | 14 | UX | Medium | Frontend | Open | Add toast notifications |
| 2025-12-02 | E14.2 | 14 | UX | Low | Frontend | Open | Add pagination for device lists |
| 2025-12-02 | E14.* | 14 | UX | Low | Frontend | Open | Add loading skeletons |

---

## Change Log

| Date | Author | Changes |
|------|--------|---------|
| 2025-12-02 | Code Quality Reviewer | Initial backlog from Epic 14 code review |

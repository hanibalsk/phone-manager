# Story E14.8: Portal Authentication

**Story ID**: E14.8
**Epic**: 14 - Admin Web Portal
**Priority**: High (Production Blocker)
**Estimate**: 5 story points (3-4 days)
**Status**: Ready for Review
**Created**: 2025-12-02
**Implemented**: 2025-12-02
**Dependencies**: E14.1

---

## Story

As an administrator,
I want to securely authenticate to the admin portal,
so that only authorized users can manage devices and settings.

## Backend Integration

The Rust backend (`phone-manager-backend`) already provides:

### Available Auth Endpoints
| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/v1/auth/register` | POST | Register new user (email, password, display_name) |
| `/api/v1/auth/login` | POST | Login with email/password → JWT tokens |
| `/api/v1/auth/oauth` | POST | OAuth login (Google, Apple) |
| `/api/v1/auth/refresh` | POST | Refresh access token |
| `/api/v1/auth/logout` | POST | Invalidate session |
| `/api/v1/auth/forgot-password` | POST | Request password reset |
| `/api/v1/auth/reset-password` | POST | Reset password with token |
| `/api/v1/auth/verify-email` | POST | Verify email with token |

### Token Structure
- **Access Token**: Short-lived JWT (Bearer token)
- **Refresh Token**: Long-lived token for getting new access tokens
- **Header**: `Authorization: Bearer <access_token>`

## Acceptance Criteria

### AC E14.8.1: Login Page
**Given** an unauthenticated user
**When** accessing the admin portal
**Then** they should:
  - Be redirected to `/login`
  - See email and password input fields
  - See a "Sign In" button
  - See a "Forgot Password?" link
  - See error messages for invalid credentials

### AC E14.8.2: Authentication Flow
**Given** valid credentials
**When** submitting the login form
**Then** the system should:
  - Call `POST /api/v1/auth/login`
  - Store access_token and refresh_token securely (httpOnly cookies or secure storage)
  - Redirect to dashboard (`/`)
  - Show user display name in header

### AC E14.8.3: Protected Routes
**Given** an unauthenticated user
**When** accessing any protected route (`/`, `/devices`, `/unlock-requests`, `/limits`, `/settings`)
**Then** they should be redirected to `/login` with return URL

### AC E14.8.4: Token Refresh
**Given** an expired access token
**When** making an API request
**Then** the system should:
  - Automatically call `/api/v1/auth/refresh`
  - Retry the original request with new token
  - Logout user if refresh fails

### AC E14.8.5: Logout
**Given** an authenticated user
**When** clicking "Sign Out"
**Then** the system should:
  - Call `POST /api/v1/auth/logout`
  - Clear stored tokens
  - Redirect to `/login`

### AC E14.8.6: Password Reset Flow
**Given** a user on the login page
**When** clicking "Forgot Password?"
**Then** they should:
  - See email input for reset request
  - Receive confirmation message (always, for security)
  - Be able to reset password via email link

### AC E14.8.7: Session Persistence
**Given** a logged-in user
**When** refreshing the page or returning later
**Then** they should:
  - Remain authenticated (if token valid)
  - Be redirected to login (if token expired and refresh fails)

## Tasks / Subtasks

- [x] Task 1: Create Auth Context and Provider (AC: E14.8.2, E14.8.7)
  - [x] Create `contexts/auth-context.tsx` with user state
  - [x] Implement token storage (localStorage)
  - [x] Add `useAuth` hook for components
  - [x] Handle token refresh logic

- [x] Task 2: Create Login Page (AC: E14.8.1)
  - [x] Create `/app/(auth)/login/page.tsx`
  - [x] Build login form with email/password
  - [x] Add Zod validation (extend schemas.ts)
  - [x] Handle loading/error states
  - [x] Add "Forgot Password?" link

- [x] Task 3: Implement API Auth Client (AC: E14.8.2, E14.8.4)
  - [x] Extend `lib/api-client.ts` with auth endpoints
  - [x] Add `authApi.login()`, `authApi.logout()`, `authApi.refresh()`
  - [x] Add automatic auth header injection
  - [x] Handle 401 responses

- [x] Task 4: Create Route Protection Middleware (AC: E14.8.3)
  - [x] Create `middleware.ts` for Next.js route protection
  - [x] Redirect unauthenticated users to `/login`
  - [x] Preserve return URL for post-login redirect
  - [x] Allow public routes (`/login`, `/forgot-password`, `/reset-password`)

- [x] Task 5: Update Layout with Auth (AC: E14.8.2, E14.8.5)
  - [x] Wrap app with AuthProvider
  - [x] Update header to show user info
  - [x] Add "Sign Out" button
  - [x] Show loading state during auth check
  - [x] Restructure to (auth) and (dashboard) route groups

- [x] Task 6: Create Password Reset Pages (AC: E14.8.6)
  - [x] Create `/app/(auth)/forgot-password/page.tsx`
  - [x] Create `/app/(auth)/reset-password/page.tsx`
  - [x] Add form validation
  - [x] Handle success/error states

- [x] Task 7: Add Auth Tests (AC: All)
  - [x] Test login schema validation
  - [x] Test forgot password schema validation
  - [x] Test reset password schema validation (12 new tests)

- [x] Task 8: Integration Testing (AC: All)
  - [x] Build verification passes
  - [x] TypeScript type check passes
  - [x] ESLint passes
  - [x] All 62 tests pass

## Dev Notes

### Technology Choices

**Auth Approach**: Client-side JWT storage with automatic refresh
- Store tokens in localStorage (simpler) or httpOnly cookies (more secure)
- Implement refresh token rotation on each refresh call
- Clear tokens on logout and auth errors

**Next.js Middleware**: Use `middleware.ts` for route protection
```typescript
// middleware.ts
import { NextResponse } from 'next/server'
import type { NextRequest } from 'next/server'

export function middleware(request: NextRequest) {
  const token = request.cookies.get('access_token')
  const isAuthPage = request.nextUrl.pathname.startsWith('/login')

  if (!token && !isAuthPage) {
    return NextResponse.redirect(new URL('/login', request.url))
  }

  return NextResponse.next()
}

export const config = {
  matcher: ['/((?!api|_next/static|_next/image|favicon.ico).*)'],
}
```

### API Response Types

```typescript
// types/auth.ts
interface LoginResponse {
  user: {
    id: string;
    email: string;
    display_name: string;
    avatar_url: string | null;
    email_verified: boolean;
    auth_provider: string;
    organization_id: string | null;
    created_at: string;
  };
  tokens: {
    access_token: string;
    refresh_token: string;
    token_type: string;
    expires_in: number;
  };
}

interface RefreshResponse {
  tokens: {
    access_token: string;
    refresh_token: string;
    token_type: string;
    expires_in: number;
  };
}
```

### Zod Schemas to Add

```typescript
// lib/schemas.ts additions
export const loginSchema = z.object({
  email: z.string().email("Invalid email format"),
  password: z.string().min(1, "Password is required"),
});

export const forgotPasswordSchema = z.object({
  email: z.string().email("Invalid email format"),
});

export const resetPasswordSchema = z.object({
  token: z.string().min(1, "Reset token is required"),
  new_password: z.string()
    .min(8, "Password must be at least 8 characters")
    .regex(/[A-Z]/, "Password must contain an uppercase letter")
    .regex(/[a-z]/, "Password must contain a lowercase letter")
    .regex(/[0-9]/, "Password must contain a number"),
});
```

### File Structure

```
admin-portal/
├── app/
│   ├── (auth)/              # Auth route group (no sidebar)
│   │   ├── login/
│   │   │   └── page.tsx
│   │   ├── forgot-password/
│   │   │   └── page.tsx
│   │   └── reset-password/
│   │       └── page.tsx
│   ├── (dashboard)/         # Protected routes (with sidebar)
│   │   ├── layout.tsx       # Dashboard layout with sidebar
│   │   ├── page.tsx         # Dashboard home (moved)
│   │   ├── devices/
│   │   ├── unlock-requests/
│   │   ├── limits/
│   │   └── settings/
│   └── layout.tsx           # Root layout (AuthProvider)
├── contexts/
│   └── auth-context.tsx     # Auth state management
├── lib/
│   └── api-client.ts        # Extended with auth endpoints
├── middleware.ts            # Route protection
└── types/
    └── auth.ts              # Auth-specific types
```

### Security Considerations

1. **Token Storage**: Consider httpOnly cookies for production
2. **HTTPS**: Required for secure cookie transmission
3. **CSRF**: Add CSRF protection if using cookies
4. **Token Refresh**: Implement sliding window refresh
5. **Logout**: Invalidate server-side session on logout

---

## File List

### Files Created

- `admin-portal/app/(auth)/login/page.tsx` - Login form with Suspense boundary
- `admin-portal/app/(auth)/forgot-password/page.tsx` - Forgot password request form
- `admin-portal/app/(auth)/reset-password/page.tsx` - Reset password form with token validation
- `admin-portal/app/(auth)/layout.tsx` - Auth pages layout (centered, no sidebar)
- `admin-portal/app/(dashboard)/layout.tsx` - Dashboard layout with auth protection
- `admin-portal/app/(dashboard)/page.tsx` - Dashboard home (moved)
- `admin-portal/app/(dashboard)/devices/page.tsx` - Devices page (moved)
- `admin-portal/app/(dashboard)/devices/[id]/usage/page.tsx` - Usage page (moved)
- `admin-portal/app/(dashboard)/unlock-requests/page.tsx` - Unlock requests (moved)
- `admin-portal/app/(dashboard)/limits/page.tsx` - App limits (moved)
- `admin-portal/app/(dashboard)/settings/page.tsx` - Settings (moved)
- `admin-portal/contexts/auth-context.tsx` - Auth context with useAuth hook
- `admin-portal/middleware.ts` - Route protection middleware
- `admin-portal/types/auth.ts` - Auth-related TypeScript types

### Files Modified

- `admin-portal/app/layout.tsx` - Wrapped with AuthProvider, removed Sidebar
- `admin-portal/lib/api-client.ts` - Added authApi endpoints and auth header injection
- `admin-portal/lib/schemas.ts` - Added loginSchema, forgotPasswordSchema, resetPasswordSchema
- `admin-portal/lib/__tests__/schemas.test.ts` - Added 12 auth schema tests
- `admin-portal/components/layout/header.tsx` - Added user display and logout button

### Files Removed

- `admin-portal/app/page.tsx` (moved to dashboard group)
- `admin-portal/app/devices/*` (moved to dashboard group)
- `admin-portal/app/unlock-requests/*` (moved to dashboard group)
- `admin-portal/app/limits/*` (moved to dashboard group)
- `admin-portal/app/settings/*` (moved to dashboard group)

---

## Change Log

| Date | Author | Changes |
|------|--------|---------|
| 2025-12-02 | Claude | Story created as placeholder |
| 2025-12-02 | Claude | Fleshed out with detailed ACs and tasks |
| 2025-12-02 | Claude | Implemented all 8 tasks |

---

**Last Updated**: 2025-12-02
**Status**: Ready for Review
**Backend**: phone-manager-backend (Rust/Axum) with JWT auth

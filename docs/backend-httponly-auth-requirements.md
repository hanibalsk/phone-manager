# Backend Requirements: httpOnly Cookie Authentication

This document describes the backend API changes required to support httpOnly cookie-based authentication for the admin-portal.

## Overview

The admin-portal frontend has been updated to support dual authentication modes:
- **localStorage mode** (default): Current behavior, tokens returned in response body
- **httpOnly mode**: Tokens stored in secure httpOnly cookies

To enable httpOnly mode, the backend must implement the changes described below.

---

## Required Backend Changes

### 1. Login Endpoint

**Endpoint**: `POST /api/v1/auth/login`

**Current Behavior**:
```json
// Response body
{
  "user": { ... },
  "tokens": {
    "access_token": "...",
    "refresh_token": "...",
    "token_type": "Bearer",
    "expires_in": 3600
  }
}
```

**New Behavior (httpOnly mode)**:
```json
// Response body (tokens moved to cookies)
{
  "user": { ... }
}
```

**Required Cookies** (set in response headers):
```
Set-Cookie: access_token=<token>; HttpOnly; Secure; SameSite=Strict; Path=/; Max-Age=3600
Set-Cookie: refresh_token=<token>; HttpOnly; Secure; SameSite=Strict; Path=/api/v1/auth; Max-Age=604800
```

---

### 2. Register Endpoint

**Endpoint**: `POST /api/v1/auth/register`

Same changes as login - set tokens as httpOnly cookies instead of returning in response body.

---

### 3. Refresh Token Endpoint

**Endpoint**: `POST /api/v1/auth/refresh`

**Current Behavior**:
- Accepts `refresh_token` in request body
- Returns new tokens in response body

**New Behavior (httpOnly mode)**:
- Read `refresh_token` from cookie (not request body)
- Set new tokens as httpOnly cookies in response
- Response body can be empty or contain success status

---

### 4. Logout Endpoint

**Endpoint**: `POST /api/v1/auth/logout`

**New Behavior**:
- Clear both cookies by setting `Max-Age=0`:
```
Set-Cookie: access_token=; HttpOnly; Secure; SameSite=Strict; Path=/; Max-Age=0
Set-Cookie: refresh_token=; HttpOnly; Secure; SameSite=Strict; Path=/api/v1/auth; Max-Age=0
```

---

### 5. All Authenticated Endpoints

**Current Behavior**:
- Read token from `Authorization: Bearer <token>` header

**New Behavior (httpOnly mode)**:
- Read `access_token` from cookie
- Keep header support for backward compatibility and API keys:
  1. Check cookie first
  2. Fall back to Authorization header if no cookie

---

### 6. CORS Configuration

Enable credentials in CORS headers:

```
Access-Control-Allow-Credentials: true
Access-Control-Allow-Origin: <specific-origin>  # NOT "*"
```

**Important**: When `Access-Control-Allow-Credentials` is `true`, you cannot use `*` for `Access-Control-Allow-Origin`. You must specify the exact origin.

---

## Cookie Configuration

### Access Token Cookie
```
Name: access_token
Value: <JWT token>
HttpOnly: true       # Prevents JavaScript access (XSS protection)
Secure: true         # HTTPS only
SameSite: Strict     # CSRF protection
Path: /              # Available for all endpoints
Max-Age: <expires_in> # Token expiration in seconds
Domain: <omit>       # Same-origin, or set for cross-domain
```

### Refresh Token Cookie
```
Name: refresh_token
Value: <refresh token>
HttpOnly: true       # Prevents JavaScript access
Secure: true         # HTTPS only
SameSite: Strict     # CSRF protection
Path: /api/v1/auth   # Only sent to auth endpoints (more secure)
Max-Age: 604800      # 7 days or your refresh token expiration
Domain: <omit>       # Same-origin, or set for cross-domain
```

---

## Security Considerations

### Why httpOnly Cookies?

1. **XSS Protection**: JavaScript cannot access httpOnly cookies, so even if malicious scripts execute on the page, they cannot steal authentication tokens.

2. **CSRF Protection**: `SameSite=Strict` prevents the browser from sending cookies with cross-site requests.

3. **Automatic Token Handling**: Browser automatically sends cookies with requests, simplifying frontend code.

### Additional Recommendations

1. **Token Rotation**: Consider implementing refresh token rotation - issue a new refresh token with each use and invalidate the old one.

2. **Short Access Token Lifetime**: Keep access tokens short-lived (e.g., 15-60 minutes) since they're automatically refreshed via cookies.

3. **Secure Flag**: Always use `Secure` flag in production to ensure cookies are only sent over HTTPS.

4. **Domain Configuration**: In production, you may need to set the `Domain` attribute if frontend and backend are on different subdomains.

---

## Testing Checklist

After implementing backend changes:

- [ ] Login sets httpOnly cookies (check DevTools > Application > Cookies)
- [ ] Cookies have correct flags: HttpOnly, Secure, SameSite=Strict
- [ ] Refresh endpoint reads token from cookie
- [ ] Logout clears both cookies
- [ ] Authenticated endpoints accept token from cookie
- [ ] CORS allows credentials from frontend origin
- [ ] Session persists across page refreshes
- [ ] New tabs maintain session

---

## Frontend Activation

Once backend changes are deployed, enable httpOnly mode in the frontend:

```bash
# In admin-portal/.env.local or deployment environment
NEXT_PUBLIC_AUTH_MODE=httpOnly
```

The frontend will automatically:
- Stop storing tokens in localStorage
- Stop sending Authorization headers (cookies sent automatically)
- Validate session via `/api/v1/auth/me` on page load

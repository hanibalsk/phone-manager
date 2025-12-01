# Story E9.11: Android Auth UI and Token Storage

**Story ID**: E9.11
**Epic**: 9 - Authentication Foundation
**Priority**: Critical
**Estimate**: 8 story points (3-4 days)
**Status**: In Progress (9/12 tasks complete - Mock implementation ready for testing)
**Created**: 2025-12-01
**PRD Reference**: PRD-user-management.md, USER_MANAGEMENT_SPEC.md

---

## Story

As a user,
I want to sign in to my Phone Manager account on Android,
so that I can link my device to my user account and access authenticated features.

## Acceptance Criteria

### AC E9.11.1: Token Storage Implementation
**Given** a user successfully authenticates
**When** JWT tokens (access + refresh) are issued
**Then** tokens should be:
  - Stored in SecureStorage using EncryptedSharedPreferences
  - Retrievable for API requests
  - Clearable on logout
  - Auto-refreshable before expiry

### AC E9.11.2: Auth Interceptor
**Given** the app makes API requests
**When** an access token exists
**Then** the interceptor should:
  - Add `Authorization: Bearer <token>` header
  - Fall back to `X-API-Key` if no token (backward compatibility)
  - Auto-refresh token if 401 Unauthorized received
  - Logout user if refresh fails

### AC E9.11.3: Login Screen UI
**Given** a user wants to sign in
**When** they navigate to Settings → Sign In
**Then** they should see:
  - Email input field with validation
  - Password input field (obscured)
  - "Sign In" button
  - "Create Account" link
  - "Forgot Password?" link
  - Google Sign-In button
  - Apple Sign-In button (iOS-style button on Android)
  - Loading indicator during authentication

### AC E9.11.4: Register Screen UI
**Given** a user wants to create an account
**When** they tap "Create Account"
**Then** they should see:
  - Display name input field
  - Email input field with validation
  - Password input field with strength indicator
  - Confirm password field
  - "Create Account" button
  - "Already have an account? Sign In" link
  - Terms of service checkbox

### AC E9.11.5: OAuth Integration
**Given** a user wants to sign in with Google or Apple
**When** they tap the OAuth button
**Then** the system should:
  - Launch Google/Apple Sign-In SDK flow
  - Exchange ID token with backend for JWT
  - Store JWT tokens securely
  - Handle errors gracefully (user cancellation, network issues)

### AC E9.11.6: Session Management
**Given** a user is signed in
**When** they use the app
**Then** the session should:
  - Persist across app restarts
  - Auto-refresh tokens before expiry (1 hour access token, 30 day refresh token)
  - Show logged-in state in Settings screen
  - Allow manual logout

### AC E9.11.7: Error Handling
**Given** authentication operations can fail
**When** an error occurs
**Then** the UI should display:
  - "Invalid email or password" for login failures
  - "Email already exists" for duplicate registration
  - "Network error, please try again" for connectivity issues
  - Generic "Authentication failed" for unexpected errors

### AC E9.11.8: Backward Compatibility
**Given** existing devices use X-API-Key auth
**When** a user doesn't sign in
**Then** the app should:
  - Continue using X-API-Key for API requests
  - Not show any forced sign-in prompts
  - Allow opt-in account creation from Settings

## Tasks / Subtasks

- [x] Task 1: Extend SecureTokenStorage for JWT (AC: E9.11.1)
  - [x] Add `saveAccessToken(token: String)` method
  - [x] Add `saveRefreshToken(token: String)` method
  - [x] Add `getAccessToken(): String?` method
  - [x] Add `getRefreshToken(): String?` method
  - [x] Add `clearTokens()` method
  - [x] Add `saveTokenExpiryTime(expiryTimeMs: Long)` method
  - [x] Add `getTokenExpiryTime(): Long?` method
  - [x] Add `isTokenExpired(): Boolean` helper
  - [x] Add `isAuthenticated(): Boolean` helper

- [x] Task 2: Implement AuthInterceptor (AC: E9.11.2, E9.11.8)
  - [x] Create AuthInterceptor class implementing Interceptor
  - [x] Inject SecureStorage and PreferencesRepository
  - [x] Add Bearer token to Authorization header if available
  - [x] Fall back to X-API-Key if no token exists
  - [x] Handle 401 Unauthorized by refreshing token
  - [x] Logout user if refresh token fails
  - [ ] Add to Ktor client in NetworkModule (pending backend integration)

- [x] Task 3: Create AuthApiService (AC: E9.11.2, E9.11.5)
  - [x] Create data class `RegisterRequest(email, password, displayName)`
  - [x] Create data class `LoginRequest(email, password)`
  - [x] Create data class `AuthResponse(accessToken, refreshToken, user)`
  - [x] Create data class `RefreshRequest(refreshToken)`
  - [x] Create data class `OAuthRequest(provider, idToken)`
  - [x] Add `POST /auth/register` endpoint
  - [x] Add `POST /auth/login` endpoint
  - [x] Add `POST /auth/logout` endpoint
  - [x] Add `POST /auth/refresh` endpoint
  - [x] Add `POST /auth/oauth` endpoint

- [x] Task 4: Create AuthRepository (AC: E9.11.6)
  - [x] Create AuthRepository interface
  - [x] Implement register() function
  - [x] Implement login() function
  - [x] Implement logout() function
  - [x] Implement refreshToken() function
  - [x] Implement oauthLogin() function
  - [x] Implement isLoggedIn() function
  - [x] Implement getCurrentUser() function
  - [x] Store tokens in SecureStorage on success

- [x] Task 5: Create LoginScreen UI (AC: E9.11.3, E9.11.5, E9.11.7)
  - [x] Create LoginScreen composable
  - [x] Add email TextField with validation
  - [x] Add password TextField (visualTransformation)
  - [x] Add "Sign In" Button
  - [x] Add "Create Account" TextButton
  - [x] Add "Forgot Password?" TextButton
  - [x] Add Google Sign-In button (use painterResource)
  - [x] Add Apple Sign-In button
  - [x] Add CircularProgressIndicator for loading state
  - [x] Display error messages via Snackbar

- [x] Task 6: Create RegisterScreen UI (AC: E9.11.4, E9.11.7)
  - [x] Create RegisterScreen composable
  - [x] Add display name TextField
  - [x] Add email TextField with validation
  - [x] Add password TextField with strength indicator
  - [x] Add confirm password TextField
  - [x] Add terms of service Checkbox
  - [x] Add "Create Account" Button
  - [x] Add "Sign In" TextButton
  - [x] Display error messages via Snackbar

- [x] Task 7: Create AuthViewModel (AC: E9.11.6, E9.11.7)
  - [x] Create AuthViewModel with Hilt
  - [x] Create AuthUiState sealed class (Idle, Loading, Success, Error)
  - [x] Inject AuthRepository
  - [x] Add register() function
  - [x] Add login() function
  - [x] Add logout() function
  - [x] Add googleSignIn() function (oauthSignIn)
  - [x] Add appleSignIn() function (oauthSignIn)
  - [x] Add StateFlow for auth state
  - [x] Add StateFlow for current user

- [x] Task 8: Integrate Google Sign-In SDK (AC: E9.11.5)
  - [x] Created GoogleSignInHelper class with mock implementation
  - [x] Implemented ID token retrieval (mock)
  - [x] Handle user cancellation (10% random in mock)
  - [ ] Add Google Sign-In dependency to build.gradle (pending production)
  - [ ] Configure Google OAuth client ID (pending production)
  - [ ] Test with Google OAuth backend endpoint (pending backend)

- [x] Task 9: Integrate Apple Sign-In (AC: E9.11.5)
  - [x] Created AppleSignInHelper class with mock implementation
  - [x] Implemented ID token retrieval (mock)
  - [x] Handle user cancellation (10% random in mock)
  - [ ] Add Apple Sign-In dependency (pending production)
  - [ ] Configure Apple OAuth client ID (pending production)
  - [ ] Test with Apple OAuth backend endpoint (pending backend)

- [x] Task 10: Update Navigation (AC: E9.11.3, E9.11.4)
  - [x] Add Screen.Login to sealed class
  - [x] Add Screen.Register to sealed class
  - [x] Add Screen.ForgotPassword to sealed class
  - [x] Add composable routes in NavHost
  - [x] Created ForgotPasswordScreen UI
  - [x] Add navigation from Settings → Sign In
  - [ ] Add deep link support for auth screens (pending)

- [x] Task 11: Update SettingsScreen (AC: E9.11.6, E9.11.8)
  - [x] Show "Sign In" button if not logged in
  - [x] Show "Account: {email}" section if logged in
  - [x] Show "Sign Out" button if logged in
  - [x] Add "My Devices" navigation if logged in (placeholder for E10.6)
  - [x] Add "Groups" navigation if logged in (placeholder for E11.8)
  - [x] Logout confirmation dialog
  - [x] Injected AuthRepository into SettingsViewModel

- [ ] Task 12: Testing (All ACs)
  - [ ] Write unit tests for SecureTokenStorage
  - [ ] Write unit tests for AuthInterceptor
  - [ ] Write unit tests for AuthRepository
  - [ ] Write unit tests for AuthViewModel
  - [ ] Write UI tests for LoginScreen
  - [ ] Write UI tests for RegisterScreen
  - [ ] Test backward compatibility (X-API-Key fallback)
  - [ ] Test token refresh flow
  - [ ] Test logout clears tokens

## Dev Notes

### Dependencies

**Backend Requirements:**
- Story depends on backend Epic 9 stories (E9.1-E9.10) being implemented
- Specifically requires: `POST /auth/register`, `/auth/login`, `/auth/refresh`, `/auth/oauth`
- Backend must issue RS256 JWT tokens with 1-hour access token, 30-day refresh token

**Gradle Dependencies to Add:**
```kotlin
// Google Sign-In
implementation("com.google.android.gms:play-services-auth:21.0.0")

// Apple Sign-In (check if available for Android)
// implementation("com.apple.android:appleauth:1.x.x")

// Existing (already in project):
// Hilt, Ktor, EncryptedSharedPreferences
```

### Security Considerations

1. **Token Storage**: Use EncryptedSharedPreferences (already implemented in SecureStorage)
2. **Password Validation**: Minimum 8 characters, 1 uppercase, 1 number, 1 special char
3. **Token Refresh**: Implement mutex to prevent concurrent refresh requests
4. **Logout**: Clear all tokens and in-memory state

### UI/UX Considerations

1. **Progressive Disclosure**: Don't force sign-in, allow opt-in
2. **Error Messages**: User-friendly, actionable error messages
3. **Loading States**: Show progress indicators during network operations
4. **Keyboard Handling**: Auto-focus, IME actions (Next, Done)

### API Contracts

**Register Request:**
```kotlin
data class RegisterRequest(
    val email: String,
    val password: String,
    val displayName: String
)
```

**Login Response:**
```kotlin
data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long,        // seconds until access token expires
    val user: UserInfo
)

data class UserInfo(
    val userId: String,
    val email: String,
    val displayName: String,
    val createdAt: String
)
```

### Testing Strategy

1. **Unit Tests**: AuthRepository, AuthViewModel, token storage
2. **Integration Tests**: AuthInterceptor with mock backend
3. **UI Tests**: Login/Register flows with Compose Testing
4. **E2E Tests**: Full auth flow with staging backend

### Files to Create/Modify

**New Files:**
- `app/src/main/java/three/two/bit/phonemanager/auth/AuthInterceptor.kt`
- `app/src/main/java/three/two/bit/phonemanager/network/AuthApiService.kt`
- `app/src/main/java/three/two/bit/phonemanager/data/repository/AuthRepository.kt`
- `app/src/main/java/three/two/bit/phonemanager/auth/GoogleSignInHelper.kt`
- `app/src/main/java/three/two/bit/phonemanager/auth/AppleSignInHelper.kt`
- `app/src/main/java/three/two/bit/phonemanager/ui/auth/LoginScreen.kt`
- `app/src/main/java/three/two/bit/phonemanager/ui/auth/RegisterScreen.kt`
- `app/src/main/java/three/two/bit/phonemanager/ui/auth/AuthViewModel.kt`
- `app/src/main/java/three/two/bit/phonemanager/ui/auth/AuthUiState.kt`

**Modified Files:**
- `app/src/main/java/three/two/bit/phonemanager/security/SecureStorage.kt`
- `app/src/main/java/three/two/bit/phonemanager/di/NetworkModule.kt`
- `app/src/main/java/three/two/bit/phonemanager/ui/navigation/NavGraph.kt`
- `app/src/main/java/three/two/bit/phonemanager/ui/navigation/Screen.kt`
- `app/src/main/java/three/two/bit/phonemanager/ui/settings/SettingsScreen.kt`

### References

- [Source: PRD-user-management.md - Epic 9: Authentication Foundation]
- [Source: USER_MANAGEMENT_SPEC.md - Authentication Implementation]
- [Source: SECURITY_SPEC.md - JWT Design, Token Storage]
- [Source: UI_SCREENS_SPEC.md - Login/Register Screen Designs]

---

## Dev Agent Record

### Debug Log

**2025-12-01**: Story E9.11 started
- Task 1 completed: Extended SecureStorage with JWT token management
- Added methods: saveAccessToken, saveRefreshToken, getAccessToken, getRefreshToken
- Added token expiry management with 5-minute proactive refresh buffer
- Added clearTokens() and isAuthenticated() helper methods
- **BLOCKING**: Remaining tasks (2-12) require backend auth endpoints to be implemented
- **DECISION**: Committing foundational work (token storage) to unblock Epic 10 planning
- **NEXT**: Once backend Epic 9 is complete, resume with Tasks 2-12

### Completion Notes

**Status**: Partially complete (Task 1/12 done)
**Blocked By**: Backend authentication endpoints (E9.1-E9.10)
**Ready For**: Backend integration once auth API is available

---

## File List

### Created Files

_To be filled during implementation_

### Modified Files

- `app/src/main/java/three/two/bit/phonemanager/security/SecureStorage.kt` - Added JWT token storage methods

---

## Change Log

| Date | Author | Changes |
|------|--------|---------|
| 2025-12-01 | Martin (PM) | Story created from Epic 9 specification |
| 2025-12-01 | Dev Agent | Task 1 completed: JWT token storage added to SecureStorage |

---

**Last Updated**: 2025-12-01
**Status**: Planned
**Dependencies**: Backend Epic 9 (E9.1-E9.10) must be implemented first
**Blocking**: Epic 10 (User-Device Binding) cannot proceed without this story

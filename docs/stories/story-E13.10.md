# Story E13.10: Android Enrollment Flow

**Story ID**: E13.10
**Epic**: 13 - B2B Enterprise Features
**Priority**: Medium
**Estimate**: 5 story points (2 days)
**Status**: Planned
**Created**: 2025-12-01
**PRD Reference**: PRD-user-management.md, B2B_ENTERPRISE_SPEC.md
**Dependencies**: E9.11 (Auth UI), Backend E13.1-E13.5

---

## Story

As an employee,
I want to enroll my device using a company-provided code or QR,
so that my device is automatically configured with company policies.

## Acceptance Criteria

### AC E13.10.1: Enrollment Entry Point
**Given** app is freshly installed or user is unauthenticated
**When** user reaches setup screen
**Then** they should see:
  - "Personal Use" option (standard sign-in)
  - "Company Enrollment" option (enterprise)
  - Clear explanation of each option
  - "Scan QR Code" button for enterprise

### AC E13.10.2: Enrollment Code Input
**Given** user selects "Company Enrollment"
**When** enrollment screen opens
**Then** they should see:
  - Enrollment code input field (alphanumeric, 16-20 chars)
  - "Scan QR Code" button
  - "Enroll" button
  - Help text explaining enrollment
  - "Back" button to return to setup

### AC E13.10.3: QR Code Scanning
**Given** user taps "Scan QR Code"
**When** camera opens
**Then** the system should:
  - Request camera permission if needed
  - Open camera viewfinder
  - Detect and parse enrollment QR code
  - Extract enrollment token
  - Auto-fill enrollment code field
  - Trigger enrollment automatically

### AC E13.10.4: Enroll Device
**Given** user enters enrollment code and taps "Enroll"
**When** enrollment is submitted
**Then** the system should:
  - Validate code format
  - Call `POST /enroll` endpoint with code and device info
  - Show loading indicator
  - Create user account if needed (backend)
  - Link device to organization (backend)
  - Receive device policies in response

### AC E13.10.5: Apply Device Policies
**Given** enrollment succeeds
**When** policies are received
**Then** the system should:
  - Save policies to local storage
  - Apply all policy settings immediately
  - Lock settings marked as locked
  - Set group membership from policy
  - Store organization context
  - Navigate to home screen

### AC E13.10.6: Enrollment Success Screen
**Given** enrollment completes successfully
**When** showing success screen
**Then** it should display:
  - "Device Enrolled Successfully" message
  - Organization name
  - Applied policies summary (locked settings count)
  - IT contact information
  - "Get Started" button

### AC E13.10.7: Enrollment Error Handling
**Given** enrollment fails
**When** error occurs
**Then** the system should show:
  - Invalid code: "Enrollment code not found or expired"
  - Already enrolled: "Device already enrolled in {org}"
  - Network error: "Cannot connect to enrollment server"
  - Policy error: "Failed to apply policies, contact IT"
  - "Try Again" button

### AC E13.10.8: Managed Device Indicator
**Given** device is enrolled
**When** user opens app
**Then** they should see:
  - "Managed by {org}" banner in Settings
  - Lock icons on policy-controlled settings
  - IT contact info in Settings
  - "Unenroll Device" option (with warning)

### AC E13.10.9: Unenroll Device
**Given** user wants to leave organization
**When** they tap "Unenroll Device" in Settings
**Then** the system should:
  - Show warning: "This will remove all company data and policies"
  - Require confirmation
  - Call `POST /devices/{id}/unenroll` endpoint
  - Clear policies and locks
  - Sign out user
  - Return to setup screen

### AC E13.10.10: Enrollment Deep Link
**Given** user receives enrollment link via email/SMS
**When** they tap phonemanager://enroll/{token}
**Then** the app should:
  - Open to enrollment screen
  - Pre-fill enrollment code
  - Trigger enrollment automatically
  - Handle app not installed (Play Store redirect)

## Tasks / Subtasks

- [ ] Task 1: Create Enrollment Models (AC: E13.10.4, E13.10.5)
  - [ ] Create EnrollmentToken data class
  - [ ] Create DevicePolicy data class (settings, locks, groupId, orgInfo)
  - [ ] Create OrganizationInfo data class (id, name, contactEmail, supportPhone)
  - [ ] Add helper methods: hasPolicy(key), getPolicyValue(key)

- [ ] Task 2: Create EnrollmentApiService (AC: E13.10.4, E13.10.9)
  - [ ] Add `POST /enroll` - Enroll device with token
  - [ ] Add `POST /devices/{id}/unenroll` - Unenroll device
  - [ ] Handle enrollment errors (invalid token, expired, already enrolled)

- [ ] Task 3: Create EnrollmentRepository (AC: All)
  - [ ] Implement enrollDevice(token, deviceInfo) function
  - [ ] Implement unenrollDevice() function
  - [ ] Implement savePolicies(policies) function
  - [ ] Implement applyPolicies(policies) function
  - [ ] Implement isEnrolled() function
  - [ ] Implement getOrganizationInfo() function
  - [ ] Store enrollment status in SecureStorage

- [ ] Task 4: Create EnrollmentViewModel (AC: E13.10.2-E13.10.7)
  - [ ] Create EnrollmentViewModel with Hilt
  - [ ] Create EnrollmentUiState sealed class (Idle, Loading, Success, Error)
  - [ ] Add StateFlow<String> enrollmentCode
  - [ ] Add enrollDevice() function
  - [ ] Add validateCode(code) function
  - [ ] Add parseQRCode(data) function
  - [ ] Handle enrollment responses

- [ ] Task 5: Create EnrollmentScreen (AC: E13.10.1, E13.10.2)
  - [ ] Create EnrollmentScreen composable
  - [ ] Add enrollment code TextField
  - [ ] Add "Scan QR Code" button
  - [ ] Add "Enroll" button (disabled until valid code)
  - [ ] Show help text explaining process
  - [ ] Add "Back" button
  - [ ] Display loading indicator during enrollment

- [ ] Task 6: Create Setup Selection Screen (AC: E13.10.1)
  - [ ] Create SetupScreen composable with two options
  - [ ] Add "Personal Use" card
  - [ ] Add "Company Enrollment" card
  - [ ] Show clear explanations of each
  - [ ] Navigate to appropriate flow on selection

- [ ] Task 7: Implement QR Scanner Integration (AC: E13.10.3)
  - [ ] Reuse QR scanner from E11.9
  - [ ] Parse enrollment token from QR data
  - [ ] Support format: phonemanager://enroll/{token}
  - [ ] Auto-trigger enrollment on scan

- [ ] Task 8: Create EnrollmentSuccessScreen (AC: E13.10.6)
  - [ ] Create EnrollmentSuccessScreen composable
  - [ ] Show success message with checkmark
  - [ ] Display organization name
  - [ ] Show policy summary (locked settings count)
  - [ ] Show IT contact info
  - [ ] Add "Get Started" button

- [ ] Task 9: Implement Policy Application Logic (AC: E13.10.5)
  - [ ] Create PolicyApplicator utility
  - [ ] Apply all settings from policy
  - [ ] Lock settings marked in policy
  - [ ] Set group membership
  - [ ] Store organization context
  - [ ] Trigger service restart if needed

- [ ] Task 10: Update SettingsScreen for Enrollment (AC: E13.10.8, E13.10.9)
  - [ ] Add "Managed by {org}" banner if enrolled
  - [ ] Show IT contact section
  - [ ] Add "Unenroll Device" button
  - [ ] Create UnenrollDialog with warning
  - [ ] Handle unenrollment flow

- [ ] Task 11: Implement Deep Link Handling (AC: E13.10.10)
  - [ ] Add enrollment deep link to AndroidManifest
  - [ ] Handle phonemanager://enroll/{token} scheme
  - [ ] Parse token from URI
  - [ ] Navigate to enrollment screen with token
  - [ ] Trigger auto-enrollment

- [ ] Task 12: Update Navigation (AC: E13.10.1, E13.10.2, E13.10.6)
  - [ ] Add Screen.Setup to sealed class
  - [ ] Add Screen.Enrollment to sealed class
  - [ ] Add Screen.EnrollmentSuccess to sealed class
  - [ ] Add routes in NavHost
  - [ ] Handle first-launch navigation

- [ ] Task 13: Testing (All ACs)
  - [ ] Write unit tests for EnrollmentRepository
  - [ ] Write unit tests for EnrollmentViewModel
  - [ ] Write unit tests for PolicyApplicator
  - [ ] Write UI tests for EnrollmentScreen
  - [ ] Test QR code enrollment
  - [ ] Test policy application
  - [ ] Test unenrollment flow
  - [ ] Test deep link handling

## Dev Notes

### Dependencies

**Gradle Dependencies:**
```kotlin
// QR scanning (reuse from E11.9)
implementation("androidx.camera:camera-camera2:1.3.0")
implementation("androidx.camera:camera-lifecycle:1.3.0")
implementation("androidx.camera:camera-view:1.3.0")
implementation("com.google.mlkit:barcode-scanning:17.2.0")
```

**Backend Requirements:**
- Depends on backend E13.1-E13.5 (Organizations, policies, enrollment)
- Requires: Enrollment token validation, automatic account creation, policy engine

### API Contracts

**Enroll Device:**
```kotlin
POST /enroll
Body: {
  enrollmentToken: "ABC123XYZ456DEF789",
  deviceInfo: {
    deviceId: "device-123",
    manufacturer: "Samsung",
    model: "Galaxy S21",
    osVersion: "Android 13",
    appVersion: "1.2.0"
  }
}
Response 200: {
  success: true,
  user: {
    userId: "user-456",
    email: "employee@company.com",
    accessToken: "jwt-token",
    refreshToken: "refresh-token"
  },
  organization: {
    id: "org-789",
    name: "Acme Corp",
    contactEmail: "it@acme.com",
    supportPhone: "+1-555-0100"
  },
  policy: {
    settings: {
      tracking_enabled: true,
      tracking_interval_seconds: 60,
      secret_mode_enabled: false
    },
    locks: ["tracking_enabled", "tracking_interval_seconds"],
    groupId: "acme-employees"
  }
}
Response 400: { error: "Invalid or expired enrollment token" }
Response 409: { error: "Device already enrolled" }
```

**Unenroll Device:**
```kotlin
POST /devices/{deviceId}/unenroll
Headers: Authorization: Bearer {token}
Response 200: { success: true }
Response 403: { error: "Unenrollment not allowed by policy" }
```

### Enrollment Token Format

- 16-20 alphanumeric characters
- Generated by IT admin in Admin Portal
- Can have expiry (default: 30 days)
- Can be single-use or multi-use
- QR code format: `phonemanager://enroll/{token}`

### UI/UX Considerations

1. **Clear Differentiation**: Personal vs Enterprise setup must be obvious
2. **Help Text**: Explain enrollment process clearly
3. **Progress Indicator**: Show enrollment stages (validating, configuring, applying policies)
4. **Success Feedback**: Celebrate successful enrollment
5. **IT Contact**: Always show IT contact for enrolled devices

### Security Considerations

1. **Token Validation**: Backend validates token before creating account
2. **Policy Enforcement**: All policy settings locked immediately
3. **Unenroll Protection**: Some policies may prevent unenrollment
4. **Data Wipe**: Unenrollment clears all organization data

### Testing Strategy

1. **Unit Tests**: EnrollmentRepository, PolicyApplicator
2. **Integration Tests**: Enrollment API with mock backend
3. **UI Tests**: Enrollment flow with Compose Testing
4. **E2E Tests**: Full enrollment from QR scan to policy application

### Files to Create/Modify

**New Files:**
- `app/src/main/java/three/two/bit/phonemanager/data/model/EnrollmentToken.kt`
- `app/src/main/java/three/two/bit/phonemanager/data/model/DevicePolicy.kt`
- `app/src/main/java/three/two/bit/phonemanager/data/model/OrganizationInfo.kt`
- `app/src/main/java/three/two/bit/phonemanager/network/EnrollmentApiService.kt`
- `app/src/main/java/three/two/bit/phonemanager/data/repository/EnrollmentRepository.kt`
- `app/src/main/java/three/two/bit/phonemanager/ui/enrollment/SetupScreen.kt`
- `app/src/main/java/three/two/bit/phonemanager/ui/enrollment/EnrollmentScreen.kt`
- `app/src/main/java/three/two/bit/phonemanager/ui/enrollment/EnrollmentSuccessScreen.kt`
- `app/src/main/java/three/two/bit/phonemanager/ui/enrollment/EnrollmentViewModel.kt`
- `app/src/main/java/three/two/bit/phonemanager/ui/enrollment/UnenrollDialog.kt`
- `app/src/main/java/three/two/bit/phonemanager/util/PolicyApplicator.kt`

**Modified Files:**
- `app/src/main/java/three/two/bit/phonemanager/ui/navigation/NavGraph.kt`
- `app/src/main/java/three/two/bit/phonemanager/ui/navigation/Screen.kt`
- `app/src/main/java/three/two/bit/phonemanager/ui/settings/SettingsScreen.kt`
- `app/src/main/java/three/two/bit/phonemanager/security/SecureStorage.kt` (enrollment status)
- `AndroidManifest.xml` (enrollment deep link)

### References

- [Source: PRD-user-management.md - Epic 13: B2B Enterprise Features]
- [Source: B2B_ENTERPRISE_SPEC.md - Enrollment Implementation]
- [Source: UI_SCREENS_SPEC.md - Enrollment UI Designs]

---

## Dev Agent Record

### Debug Log

_To be filled during implementation_

### Completion Notes

_To be filled after implementation_

---

## File List

### Created Files

_To be filled during implementation_

### Modified Files

_To be filled during implementation_

---

## Change Log

| Date | Author | Changes |
|------|--------|---------|
| 2025-12-01 | Martin (PM) | Story created from Epic 13 specification |

---

**Last Updated**: 2025-12-01
**Status**: Planned
**Dependencies**: E9.11 (Auth UI), Backend E13.1-E13.5
**Blocking**: None (B2B feature, optional for personal users)

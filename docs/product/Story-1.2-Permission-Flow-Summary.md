# Story 1.2: Permission Request Flow (Summary)

**Story ID**: 1.2
**Epic**: 1 - Location Tracking Core
**Priority**: Must-Have
**Estimate**: 8 story points
**Status**: Ready for Development
**Created**: 2025-01-11

> **Note**: This is a summary document. Full story documentation to be expanded based on BMAD requirements analysis completed 2025-01-11.

---

## User Story

```
AS A user
I WANT to understand why location permissions are needed and grant them confidently
SO THAT I can use location tracking features without privacy concerns
```

---

## Key Requirements (from BMAD Analysis)

### Acceptance Criteria Overview

1. **AC 1.2.1**: Request ACCESS_FINE_LOCATION on first launch with rationale
2. **AC 1.2.2**: Request ACCESS_BACKGROUND_LOCATION on Android 10+ (separately)
3. **AC 1.2.3**: Display clear rationale dialogs before system prompts
4. **AC 1.2.4**: Show permission status on main screen
5. **AC 1.2.5**: Deep link to settings when permanently denied
6. **AC 1.2.6**: Permission flow completes in < 60 seconds
7. **AC 1.2.7**: Zero crashes across Android 8-14

### Core Components

- **PermissionManager**: Abstracts permission checking/requesting
- **PermissionViewModel**: Manages permission state and flow
- **PermissionRationaleDialog**: Custom rationale before system prompt
- **PermissionStatusCard**: Displays current permission status

### Android Version Handling

- **API 24-28**: ACCESS_FINE_LOCATION only
- **API 29+**: ACCESS_FINE_LOCATION + ACCESS_BACKGROUND_LOCATION (separately)
- **API 30+**: Background must be requested after foreground granted
- **API 31+**: Approximate location option available (app doesn't use)

---

## Implementation Highlights

### Permission State Model

```kotlin
sealed class PermissionState {
    object NotRequested : PermissionState()
    object Granted : PermissionState()
    object Denied : PermissionState()
    object PermanentlyDenied : PermissionState()
    data class BackgroundRestricted(val foregroundGranted: Boolean) : PermissionState()
}
```

### Key Files

1. `PermissionManager.kt` - Permission abstraction layer
2. `PermissionViewModel.kt` - State management
3. `PermissionRationaleDialog.kt` - Rationale UI
4. `PermissionStatusCard.kt` - Status display

---

## Testing Focus

- Unit tests for all permission state transitions
- Integration tests for request flow end-to-end
- Manual testing on Android 8, 9, 10, 11, 12, 13, 14
- Rationale copy user testing (target: >70% grant rate)

---

## Compliance

- Google Play permission policy compliant
- Android permission best practices followed
- GDPR-compliant (clear consent, easy opt-out)

---

## Related Documentation

- **Full BMAD Analysis**: Session transcript 2025-01-11
- **Epic 1 Overview**: `docs/product/Epic-1-Location-Tracking-Core.md`
- **XML Context**: `docs/stories/story-1.2-context.xml` (to be created)

---

**Status**: ✅ Requirements analyzed, ready for full story expansion
**Next Step**: Expand to full story document format (similar to Story 1.1)

# Project Backlog

**Project**: Phone Manager
**Last Updated**: 2025-11-25

---

## Action Items from Story Reviews

| Date | Story | Epic | Type | Severity | Owner | Status | Notes |
|------|-------|------|------|----------|-------|--------|-------|
| 2025-11-25 | E3.3 | 3 | Enhancement | Medium | TBD | Open | Add Settings UI for polling interval configuration - Add dropdown/slider for 10, 15, 20, 30s selection. File: SettingsScreen.kt, AC: E3.3.5 |
| 2025-11-25 | E3.3 | 3 | Testing | Low | TBD | Open | Add unit tests for polling lifecycle - Test startPolling(), stopPolling(), interval execution. File: MapViewModelTest.kt |
| 2025-11-25 | E3.1 | 3 | Enhancement | Low | TBD | Open | Use camera animation for smooth transitions - Replace direct position with animate(). File: MapScreen.kt:72-79 |
| 2025-11-25 | E3.1 | 3 | Enhancement | Low | TBD | Open | Add retry button to error state - Add Button calling viewModel.refresh(). File: MapScreen.kt:108-112 |
| 2025-11-25 | E3.1 | 3 | Testing | Low | TBD | Open | Add tests for polling lifecycle - Test startPolling() and stopPolling(). File: MapViewModelTest.kt |
| 2025-11-25 | E2.2 | 2 | Performance | Low | TBD | Open | Cache secret mode state to avoid runBlocking - Add cached field updated by Flow. File: LocationTrackingService.kt:393-397 |
| 2025-11-25 | E2.2 | 2 | Testing | Low | TBD | Open | Add unit test for notification variants - Test createNotification() for secret vs normal mode. File: LocationTrackingServiceTest.kt (new) |
| 2025-11-25 | E2.1 | 2 | Enhancement | Low | TBD | Open | Verify long-press duration is 3 seconds - Consider awaitLongPressOrCancellation with 3000ms. File: HomeScreen.kt:79-85, AC: E2.1.2 |
| 2025-11-25 | E2.1 | 2 | Testing | Low | TBD | Open | Test gesture timing requirements - Add instrumented tests for 3s long-press and 5-tap timing. File: app/src/androidTest/ (new) |
| 2025-11-25 | E2.1 | 2 | Testing | Low | TBD | Open | Verify haptic feedback suppression - Manual test on multiple devices. AC: E2.1.4 |
| 2025-11-25 | E1.3 | 1 | Enhancement | Medium | Claude | Completed | Add comprehensive input validation matching RegistrationViewModel - Implemented 2-50 char limits and regex validation. File: SettingsViewModel.kt:127-167 |
| 2025-11-25 | E1.3 | 1 | Enhancement | Low | Claude | Completed | Add confirmation dialog for group ID changes - AlertDialog with warning implemented. File: SettingsScreen.kt:152-175, SettingsViewModel.kt:91-113 |
| 2025-11-25 | E1.3 | 1 | Enhancement | Low | Claude | Completed | Fix success message timing - Added 500ms delay before navigation. File: SettingsScreen.kt:48 |
| 2025-11-25 | E1.3 | 1 | Enhancement | Low | Claude | Completed | Add deviceId display for transparency - Read-only deviceId field added. File: SettingsScreen.kt:87-96 |
| 2025-11-25 | E1.3 | 1 | Testing | Low | Claude | Completed | Add tests for validation edge cases - 8 new tests added (16 total). File: SettingsViewModelTest.kt:216-419 |
| 2025-11-25 | E1.2 | 1 | Enhancement | Low | TBD | Open | Enhance error message specificity for NetworkException - Distinguish network errors with specific message per AC. File: GroupMembersViewModel.kt:53-60, AC: E1.2.5 |
| 2025-11-25 | E1.1 | 1 | Enhancement | Medium | TBD | Open | Add max length validation for groupId - Add 50-char limit check for consistency with displayName. File: RegistrationViewModel.kt:100, AC: E1.1.6 |
| 2025-11-25 | E1.1 | 1 | Enhancement | Medium | TBD | Open | Enhance error message mapping for server errors - Parse HTTP status codes and server responses for specific user messages. File: RegistrationViewModel.kt:62-65, AC: E1.1.4 |
| 2025-11-25 | E1.1 | 1 | Enhancement | Low | TBD | Open | Implement success toast notification - Show "Device registered successfully" before navigation. File: RegistrationScreen.kt:54-58, AC: E1.1.3 |
| 2025-11-25 | E1.1 | 1 | Enhancement | Low | TBD | Open | Add input trimming in update methods - Apply .trim() in updateDisplayName/updateGroupId for consistent validation. File: RegistrationViewModel.kt:28-38, AC: E1.1.6 |
| 2025-11-25 | E1.1 | 1 | Testing | Low | TBD | Open | Add integration test for registration flow - Create E2E test: launch app → registration → home. File: app/src/androidTest/ (new) |
| 2025-11-25 | 1.4 | 1 | TechDebt | Medium | TBD | Open | Document Alternative Approaches for isServiceRunning() - Add ADR for future migration when ActivityManager.getRunningServices() removed. File: LocationServiceController.kt:117-125 |
| 2025-11-25 | 1.4 | 1 | Testing | Medium | TBD | Open | Add Integration Tests for State Persistence - Implement ServiceStatePersistenceTest.kt for process death/reboot scenarios. File: app/src/androidTest/java/com/phonemanager/service/ServiceStatePersistenceTest.kt |
| 2025-11-25 | 1.4 | 1 | Performance | Low | TBD | Open | Add Performance Test for DataStore Writes - Verify <100ms write time for setServiceRunningState(). File: app/src/androidTest/java/com/phonemanager/data/preferences/PreferencesRepositoryPerformanceTest.kt |
| 2025-11-25 | 1.4 | 1 | Documentation | Low | TBD | Open | Document Repository Scope Lifecycle - Add KDoc explaining application-scoped repositoryScope design. File: LocationRepositoryImpl.kt:34 |

---

## Backlog Categories

### Technical Debt
Items related to code quality, architecture improvements, and modernization.

### Testing
Items related to test coverage, test quality, and testing infrastructure.

### Performance
Items related to optimization, benchmarking, and performance monitoring.

### Documentation
Items related to code documentation, architecture docs, and knowledge transfer.

### Bug
Items related to defects and incorrect behavior.

### Enhancement
Items related to new features and capability improvements.

---

## Notes

- Action items from Story 1.4 review added on 2025-11-25
- All items currently unassigned (Owner: TBD)
- Priority levels: High, Medium, Low based on impact and urgency

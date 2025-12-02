# Project Backlog

**Project**: Phone Manager
**Last Updated**: 2025-12-02

---

## Action Items from Story Reviews

| Date | Story | Epic | Type | Severity | Owner | Status | Notes |
|------|-------|------|------|----------|-------|--------|-------|
| 2025-12-01 | E11.9 | 11 | TechDebt | Medium | TBD | Open | Extract extractInviteCode() to shared utility to eliminate duplication. Files: JoinGroupViewModel.kt:207-228, QRScannerScreen.kt:404-425, AC: E11.9.5 |
| 2025-12-01 | E11.9 | 11 | Enhancement | Low | TBD | Open | Add explicit CAMERA permission to AndroidManifest.xml. File: AndroidManifest.xml, AC: E11.9.5 |
| 2025-12-01 | E11.9 | 11 | TechDebt | Low | TBD | Open | Extract deep link domain to BuildConfig constant. Files: GroupInvite.kt, extractInviteCode patterns, AC: E11.9.8 |
| 2025-11-26 | E5.1 | 5 | Enhancement | Medium | TBD | Open | Add radius validation to ProximityAlert domain model - Add init block: require(radiusMeters in 50..10_000). File: ProximityAlert.kt, AC: E5.1.2 |
| 2025-11-26 | E5.1 | 5 | Enhancement | Medium | TBD | Open | Implement server integration when backend ready - Implement AlertRepository, AlertApiService, and UI components (AlertsScreen, CreateAlertScreen, AlertsViewModel). Files: Multiple, AC: E5.1.4, E5.1.5, E5.1.6 |
| 2025-11-26 | E5.1 | 5 | Testing | Low | TBD | Open | Add unit tests for domain model and mappers - Test ProximityAlert instantiation, enum values, toDomain/toEntity functions. File: ProximityAlertTest.kt (new) |
| 2025-11-26 | E5.1 | 5 | Testing | Low | TBD | Open | Add DAO instrumented tests - Test CRUD operations, Flow queries, data integrity. File: ProximityAlertDaoTest.kt (new) |
| 2025-11-26 | E5.1 | 5 | Testing | Low | TBD | Open | Add Room migration test - Validate MIGRATION_3_4 creates proximity_alerts table correctly. File: AppDatabase migration test suite (new) |
| 2025-11-25 | E4.2 | 4 | Enhancement | Medium | TBD | Open | Implement server integration features when backend ready - Add getDeviceHistory() endpoint, device selector UI, server fetch logic. Files: DeviceApiService.kt, HistoryScreen.kt, HistoryViewModel.kt, AC: E4.2.1, E4.2.3, E4.2.5 |
| 2025-11-25 | E4.2 | 4 | Enhancement | Medium | TBD | Open | Update upload worker to mark records as synced - Set isSynced=true and syncedAt=timestamp after successful upload. File: Upload worker implementation (future), AC: E4.2.4 |
| 2025-11-25 | E4.2 | 4 | Testing | Low | TBD | Open | Add integration test for downsampling in HistoryViewModel - Test with >500 location points to verify downsampling triggers. File: HistoryViewModelTest.kt |
| 2025-11-25 | E4.2 | 4 | Testing | Low | TBD | Open | Add Room migration test - Create AppDatabaseMigrationTest with schema validation for MIGRATION_2_3. File: New test file for AppDatabase migrations |
| 2025-11-25 | E4.2 | 4 | Documentation | Low | TBD | Open | Update dev notes to reflect actual algorithm - Replace Douglas-Peucker reference with interval-based sampling description. File: story-E4.2.md:96-105 |
| 2025-11-25 | E4.1 | 4 | Enhancement | Medium | TBD | Open | Add Custom Date Range UI with date picker - Add DatePickerDialog for custom range. File: HistoryScreen.kt, AC: E4.1.5 |
| 2025-11-25 | E4.1 | 4 | Enhancement | Low | TBD | Open | Add retry button to error state - Add Button calling viewModel.setDateFilter(). File: HistoryScreen.kt:108-113 |
| 2025-11-25 | E4.1 | 4 | Enhancement | Low | TBD | Open | Use camera animation for filter changes - Replace direct position with animate(). File: HistoryScreen.kt:62-66 |
| 2025-11-25 | E4.1 | 4 | Testing | Low | TBD | Open | Add tests for date range calculations - Verify Today/Yesterday/Last7Days math. File: HistoryViewModelTest.kt |
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
| 2025-12-02 | E14.2 | 14 | Enhancement | Low | TBD | Open | Complete device details modal implementation - Currently placeholder. File: admin-portal/components/devices/device-details.tsx, AC: E14.2.2 |
| 2025-12-02 | E14.2 | 14 | Enhancement | Low | TBD | Open | Add pagination for large device lists. File: admin-portal/app/devices/page.tsx |
| 2025-12-02 | E14.2 | 14 | Testing | Low | TBD | Open | Add DeviceList component tests. File: admin-portal/components/devices/__tests__/ |
| 2025-12-02 | E14.4 | 14 | Enhancement | Low | TBD | Open | Complete usage analytics if needed for MVP - Currently placeholder. Files: admin-portal/components/usage/, AC: E14.4.1-3 |
| 2025-12-02 | E14.* | 14 | Security | Medium | TBD | Open | Implement authentication/authorization before production deployment - All admin routes unprotected. Recommend NextAuth.js. Files: All page routes, AC: E14.8 (future story) |
| 2025-12-02 | E14.* | 14 | Testing | Medium | TBD | Open | Add integration tests with MSW for API mocking - Current tests are unit-only. File: admin-portal/__tests__/integration/ |
| 2025-12-02 | E14.* | 14 | Testing | Low | TBD | Open | Add error boundary tests - Verify error recovery paths. File: admin-portal/app/__tests__/ |
| 2025-12-02 | E14.* | 14 | Security | Medium | TBD | Open | Add API response validation with Zod - Prevent type confusion from backend mismatches. File: admin-portal/lib/api-client.ts |
| 2025-12-02 | E14.8 | 14 | Security | Low | TBD | Open | Migrate token storage from localStorage to httpOnly cookies - Enhanced XSS protection for production. Files: admin-portal/contexts/auth-context.tsx, admin-portal/middleware.ts |
| 2025-12-02 | E14.8 | 14 | Enhancement | Low | TBD | Open | Add request queuing during token refresh - Prevent race conditions with concurrent requests. File: admin-portal/contexts/auth-context.tsx |

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

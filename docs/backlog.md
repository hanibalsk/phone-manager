# Project Backlog

**Project**: Phone Manager
**Last Updated**: 2025-11-25

---

## Action Items from Story Reviews

| Date | Story | Epic | Type | Severity | Owner | Status | Notes |
|------|-------|------|------|----------|-------|--------|-------|
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

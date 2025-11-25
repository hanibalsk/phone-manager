# Story E6.1: Geofence Definition

**Story ID**: E6.1
**Epic**: 6 - Geofencing with Webhooks
**Priority**: Must-Have
**Estimate**: 2 story points (1-2 days)
**Status**: ContextReadyDraft
**Created**: 2025-11-25
**PRD Reference**: Feature 5 (FR-5.1)

---

## Story

As a user,
I want to define geofence zones,
so that I get alerts for specific places.

## Acceptance Criteria

### AC E6.1.1: Geofence Entity
**Given** the data model is defined
**Then** Geofence entity should include:
  - id (UUID)
  - deviceId (owner device)
  - name (e.g., "Home", "Office")
  - latitude, longitude (center point)
  - radiusMeters
  - transitionTypes (ENTER, EXIT, DWELL set)
  - webhookId (optional link to webhook)
  - active (Boolean)
  - createdAt, updatedAt

### AC E6.1.2: Transition Types
**Given** I am creating a geofence
**When** I select transition types
**Then** I should be able to choose any combination of:
  - ENTER (trigger when entering zone)
  - EXIT (trigger when leaving zone)
  - DWELL (trigger after staying in zone for duration)

### AC E6.1.3: Android Geofencing API Registration
**Given** I create or enable a geofence
**When** the geofence is saved
**Then** it should be registered with Android Geofencing API
**And** use GeofencingClient.addGeofences()

### AC E6.1.4: Server Sync
**Given** I create a geofence
**When** I save
**Then** it should sync to server via `POST /api/geofences`
**And** persist across app reinstalls

### AC E6.1.5: Geofence Management UI
**Given** I am on the Geofences screen
**Then** I should be able to:
  - View list of my geofences
  - Create new geofence
  - Edit existing geofence
  - Delete geofence
  - Enable/disable geofence

### AC E6.1.6: Map-Based Location Selection
**Given** I am creating a geofence
**When** I set the location
**Then** I should be able to:
  - Select location by tapping on map
  - Enter coordinates manually
  - Use current location as center

## Tasks / Subtasks

- [ ] Task 1: Create Geofence Domain Model (AC: E6.1.1, E6.1.2)
  - [ ] Create Geofence data class
  - [ ] Create TransitionType enum (ENTER, EXIT, DWELL)
  - [ ] Add webhookId optional field
- [ ] Task 2: Create Geofence Room Entity (AC: E6.1.1)
  - [ ] Create GeofenceEntity with Room annotations
  - [ ] Create GeofenceDao
  - [ ] Add to AppDatabase
- [ ] Task 3: Create Network Models (AC: E6.1.4)
  - [ ] Create GeofenceDto for API
  - [ ] Add CRUD endpoints to GeofenceApiService
- [ ] Task 4: Create GeofenceRepository (AC: E6.1.4)
  - [ ] Implement local + remote sync
  - [ ] Add CRUD operations
- [ ] Task 5: Implement Android Geofencing Registration (AC: E6.1.3)
  - [ ] Create GeofenceManager wrapper
  - [ ] Implement addGeofence() with GeofencingClient
  - [ ] Implement removeGeofence()
  - [ ] Handle geofence registration errors
- [ ] Task 6: Create GeofencesScreen UI (AC: E6.1.5)
  - [ ] Create GeofencesScreen composable
  - [ ] Show list of geofences
  - [ ] Add enable/disable toggle per geofence
- [ ] Task 7: Create CreateGeofenceScreen (AC: E6.1.6)
  - [ ] Add name input field
  - [ ] Add map for location selection
  - [ ] Add radius slider (50-10,000m)
  - [ ] Add transition type checkboxes
  - [ ] Add webhook selector (optional)
- [ ] Task 8: Create GeofencesViewModel (AC: E6.1.5)
  - [ ] Load geofences from repository
  - [ ] Implement CRUD operations
  - [ ] Trigger Android geofence registration
- [ ] Task 9: Testing (All ACs)
  - [ ] Unit test GeofenceRepository
  - [ ] Manual test geofence creation
  - [ ] Test Android Geofencing registration

## Dev Notes

### Architecture
- GeofenceManager wraps Android GeofencingClient
- Room + Server sync for persistence
- UI for creation includes map-based selection

### Android Geofencing
```kotlin
class GeofenceManager(private val context: Context) {

    private val geofencingClient = LocationServices.getGeofencingClient(context)

    @SuppressLint("MissingPermission")
    suspend fun addGeofence(geofence: Geofence): Result<Unit> {
        val androidGeofence = com.google.android.gms.location.Geofence.Builder()
            .setRequestId(geofence.id)
            .setCircularRegion(
                geofence.latitude,
                geofence.longitude,
                geofence.radiusMeters.toFloat()
            )
            .setTransitionTypes(mapTransitionTypes(geofence.transitionTypes))
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .build()

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(androidGeofence)
            .build()

        val pendingIntent = createGeofencePendingIntent()

        return suspendCoroutine { cont ->
            geofencingClient.addGeofences(request, pendingIntent)
                .addOnSuccessListener { cont.resume(Result.success(Unit)) }
                .addOnFailureListener { cont.resume(Result.failure(it)) }
        }
    }

    private fun mapTransitionTypes(types: Set<TransitionType>): Int {
        var flags = 0
        if (TransitionType.ENTER in types) flags = flags or Geofence.GEOFENCE_TRANSITION_ENTER
        if (TransitionType.EXIT in types) flags = flags or Geofence.GEOFENCE_TRANSITION_EXIT
        if (TransitionType.DWELL in types) flags = flags or Geofence.GEOFENCE_TRANSITION_DWELL
        return flags
    }
}
```

### Files to Create
- `domain/model/Geofence.kt` (NEW)
- `data/model/GeofenceEntity.kt` (NEW)
- `data/database/dao/GeofenceDao.kt` (NEW)
- `network/GeofenceApiService.kt` (NEW)
- `data/repository/GeofenceRepository.kt` (NEW)
- `geofence/GeofenceManager.kt` (NEW)
- `ui/geofences/GeofencesScreen.kt` (NEW)
- `ui/geofences/GeofencesViewModel.kt` (NEW)
- `ui/geofences/CreateGeofenceScreen.kt` (NEW)

### Permissions Required
- `ACCESS_FINE_LOCATION`
- `ACCESS_BACKGROUND_LOCATION`

### References
- [Source: PRD FR-5.1.1-5.1.4 - Geofence Definition requirements]
- [Source: PRD Data Model: Geofence]
- [Source: PRD Section 6.2 - POST /api/geofences spec]
- [Source: epics.md - Story 6.1 description]

## Dev Agent Record

### Context Reference
- `/Users/martinjanci/cursor/phone-manager/docs/story-context-E6.1.xml` (Generated: 2025-11-25)

### Agent Model Used
Claude Opus 4.5 (claude-opus-4-5-20251101)

### Debug Log References
<!-- Add debug log references during implementation -->

### Completion Notes List
<!-- Add completion notes during implementation -->

### File List
<!-- Add list of files created/modified during implementation -->

---

**Last Updated**: 2025-11-25
**Status**: ContextReadyDraft
**Dependencies**: Story E3.1 (Google Maps - for location selection UI)

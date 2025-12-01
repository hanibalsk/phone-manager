# API Compatibility Report

**Generated**: 2024-11-30
**Backend Version**: phone-manager-backend (Epics 1-8 complete)
**Frontend Version**: phone-manager Android app

## Executive Summary

| Metric | Value |
|--------|-------|
| Total Backend Endpoints | 30+ |
| Implemented in Frontend | 17 (56%) |
| Missing in Frontend | 13+ (44%) |
| Critical Issues | 3 |
| Minor Issues | 8 |

---

## Endpoint Compatibility Matrix

### Legend
- ✅ **WORKING** - Fully implemented and compatible
- ❌ **MISSING** - Not implemented in frontend
- ⚠️ **PARTIAL** - Implemented but with issues
- ➖ **N/A** - Not applicable for mobile client

---

### Device Endpoints

| Endpoint | Method | Frontend Service | Status | Notes |
|----------|--------|------------------|--------|-------|
| `/api/v1/devices/register` | POST | `DeviceApiService.registerDevice()` | ✅ WORKING | |
| `/api/v1/devices` | GET | `DeviceApiService.getGroupMembers()` | ✅ WORKING | |
| `/api/v1/devices/:device_id` | DELETE | — | ❌ MISSING | **CRITICAL**: No delete capability |
| `/api/v1/devices/:device_id/locations` | GET | `DeviceApiService.getLocationHistory()` | ✅ WORKING | |
| `/api/v1/devices/:device_id/trips` | GET | — | ❌ MISSING | Trip listing not implemented |
| `/api/v1/devices/:device_id/movement-events` | GET | — | ❌ MISSING | Movement history not implemented |
| `/api/v1/devices/:device_id/data-export` | GET | — | ❌ MISSING | GDPR export (low priority) |
| `/api/v1/devices/:device_id/data` | DELETE | — | ❌ MISSING | GDPR deletion (low priority) |

### Location Endpoints

| Endpoint | Method | Frontend Service | Status | Notes |
|----------|--------|------------------|--------|-------|
| `/api/v1/locations` | POST | `LocationApiService.uploadLocation()` | ✅ WORKING | |
| `/api/v1/locations/batch` | POST | `LocationApiService.uploadLocations()` | ✅ WORKING | |

### Trip Endpoints

| Endpoint | Method | Frontend Service | Status | Notes |
|----------|--------|------------------|--------|-------|
| `/api/v1/trips` | POST | — | ❌ MISSING | **CRITICAL**: Trip creation missing |
| `/api/v1/trips/:trip_id` | PATCH | — | ❌ MISSING | **CRITICAL**: State updates missing |
| `/api/v1/trips/:trip_id/movement-events` | GET | — | ❌ MISSING | Trip events query missing |
| `/api/v1/trips/:trip_id/path` | GET | — | ❌ MISSING | Path retrieval missing |
| `/api/v1/trips/:trip_id/correct-path` | POST | — | ❌ MISSING | Path correction missing |

### Movement Event Endpoints

| Endpoint | Method | Frontend Service | Status | Notes |
|----------|--------|------------------|--------|-------|
| `/api/v1/movement-events` | POST | — | ❌ MISSING | **CRITICAL**: Event upload missing |
| `/api/v1/movement-events/batch` | POST | — | ❌ MISSING | **CRITICAL**: Batch upload missing |

### Geofence Endpoints

| Endpoint | Method | Frontend Service | Status | Notes |
|----------|--------|------------------|--------|-------|
| `/api/v1/geofences` | POST | `GeofenceApiService.createGeofence()` | ✅ WORKING | |
| `/api/v1/geofences` | GET | `GeofenceApiService.listGeofences()` | ✅ WORKING | |
| `/api/v1/geofences/:geofence_id` | GET | `GeofenceApiService.getGeofence()` | ✅ WORKING | |
| `/api/v1/geofences/:geofence_id` | PATCH | `GeofenceApiService.updateGeofence()` | ✅ WORKING | |
| `/api/v1/geofences/:geofence_id` | DELETE | `GeofenceApiService.deleteGeofence()` | ✅ WORKING | |

### Proximity Alert Endpoints

| Endpoint | Method | Frontend Service | Status | Notes |
|----------|--------|------------------|--------|-------|
| `/api/v1/proximity-alerts` | POST | `ProximityAlertApiService.createAlert()` | ✅ WORKING | |
| `/api/v1/proximity-alerts` | GET | `ProximityAlertApiService.listAlerts()` | ✅ WORKING | |
| `/api/v1/proximity-alerts/:alert_id` | GET | `ProximityAlertApiService.getAlert()` | ✅ WORKING | |
| `/api/v1/proximity-alerts/:alert_id` | PATCH | `ProximityAlertApiService.updateAlert()` | ✅ WORKING | |
| `/api/v1/proximity-alerts/:alert_id` | DELETE | `ProximityAlertApiService.deleteAlert()` | ✅ WORKING | |

### Admin Endpoints (Not Required for Mobile)

| Endpoint | Method | Status | Notes |
|----------|--------|--------|-------|
| `/api/v1/admin/stats` | GET | ➖ N/A | Admin only |
| `/api/v1/admin/devices/inactive` | DELETE | ➖ N/A | Admin only |
| `/api/v1/admin/devices/:device_id/reactivate` | POST | ➖ N/A | Admin only |

### Health Endpoints (Not Required for Mobile)

| Endpoint | Method | Status | Notes |
|----------|--------|--------|-------|
| `/api/health` | GET | ➖ N/A | Backend monitoring |
| `/api/health/live` | GET | ➖ N/A | K8s liveness probe |
| `/api/health/ready` | GET | ➖ N/A | K8s readiness probe |
| `/metrics` | GET | ➖ N/A | Prometheus metrics |

---

## Critical Issues

### 1. Missing Trip Management System

**Severity**: CRITICAL
**Impact**: Complete trip tracking feature unavailable in app

**Missing Implementation**:
- No `TripApiService` class exists
- Cannot create, update, or query trips
- Path correction feature inaccessible

**Required Frontend Changes**:
```kotlin
// app/src/main/java/three/two/bit/phonemanager/data/api/TripApiService.kt

interface TripApiService {
    suspend fun createTrip(request: CreateTripRequest): Result<TripResponse>
    suspend fun updateTripState(tripId: String, request: UpdateTripRequest): Result<TripResponse>
    suspend fun getDeviceTrips(deviceId: String, cursor: String? = null): Result<TripsListResponse>
    suspend fun getTripMovementEvents(tripId: String, order: String = "asc"): Result<TripEventsResponse>
    suspend fun getTripPath(tripId: String): Result<TripPathResponse>
    suspend fun triggerPathCorrection(tripId: String): Result<PathCorrectionResponse>
}
```

**Backend Endpoints**:
- `POST /api/v1/trips`
- `PATCH /api/v1/trips/:trip_id`
- `GET /api/v1/devices/:device_id/trips`
- `GET /api/v1/trips/:trip_id/movement-events`
- `GET /api/v1/trips/:trip_id/path`
- `POST /api/v1/trips/:trip_id/correct-path`

---

### 2. Missing Movement Events System

**Severity**: CRITICAL
**Impact**: Transportation mode detection and tracking unavailable

**Missing Implementation**:
- No `MovementEventApiService` class exists
- Cannot upload movement events to backend
- Cannot query movement history

**Required Frontend Changes**:
```kotlin
// app/src/main/java/three/two/bit/phonemanager/data/api/MovementEventApiService.kt

interface MovementEventApiService {
    suspend fun uploadEvent(request: CreateMovementEventRequest): Result<MovementEventResponse>
    suspend fun uploadEventsBatch(request: BatchMovementEventsRequest): Result<BatchResponse>
    suspend fun getDeviceEvents(
        deviceId: String,
        cursor: String? = null,
        from: Long? = null,
        to: Long? = null
    ): Result<MovementEventsResponse>
}
```

**Backend Endpoints**:
- `POST /api/v1/movement-events`
- `POST /api/v1/movement-events/batch`
- `GET /api/v1/devices/:device_id/movement-events`

---

### 3. Missing Device Deletion

**Severity**: CRITICAL
**Impact**: Users cannot remove devices from their group

**Missing Implementation**:
- `DeviceApiService` lacks `deleteDevice()` method

**Required Frontend Changes**:
```kotlin
// Add to DeviceApiService.kt

suspend fun deleteDevice(deviceId: String): Result<Unit>
```

**Backend Endpoint**:
- `DELETE /api/v1/devices/:device_id`

---

## Field-Level Compatibility

### Location Upload Request

| Field | Backend Type | Frontend Type | Status |
|-------|-------------|---------------|--------|
| `deviceId` | UUID (string) | String | ✅ Compatible |
| `timestamp` | i64 (milliseconds) | Long | ✅ Compatible |
| `latitude` | f64 | Double | ✅ Compatible |
| `longitude` | f64 | Double | ✅ Compatible |
| `accuracy` | f64 | Float | ⚠️ Minor mismatch |
| `altitude` | Option<f64> | Double? | ✅ Compatible |
| `bearing` | Option<f64> | Float? | ⚠️ Minor mismatch |
| `speed` | Option<f64> | Float? | ⚠️ Minor mismatch |
| `provider` | Option<String> | String? | ✅ Compatible |
| `batteryLevel` | Option<i32> | Int? | ✅ Compatible |
| `networkType` | Option<String> | String? | ✅ Compatible |
| `transportationMode` | Option<Enum> | — | ❌ **MISSING** |
| `detectionSource` | Option<Enum> | — | ❌ **MISSING** |
| `tripId` | Option<UUID> | — | ❌ **MISSING** |

**Action Required**: Add `transportationMode`, `detectionSource`, and `tripId` fields to `LocationPayload` class.

---

### Proximity Alert Response

| Field | Backend Type | Frontend Type | Status |
|-------|-------------|---------------|--------|
| `alertId` | UUID | String | ✅ Compatible |
| `sourceDeviceId` | UUID | String | ✅ Compatible |
| `targetDeviceId` | UUID | String | ✅ Compatible |
| `name` | Option<String> | String? | ✅ Compatible |
| `radiusMeters` | i32 | Int | ✅ Compatible |
| `isActive` | bool | Boolean | ✅ Compatible |
| `isTriggered` | bool | — | ⚠️ **MISSING** |
| `lastTriggeredAt` | Option<DateTime> | — | ⚠️ **MISSING** |
| `metadata` | Option<JSON> | Map<String, String>? | ⚠️ Type mismatch |
| `createdAt` | DateTime | String | ✅ Compatible |
| `updatedAt` | DateTime | String | ✅ Compatible |

**Action Required**: Add `isTriggered` and `lastTriggeredAt` fields to `ProximityAlertDto` class.

---

### Geofence Metadata

| Field | Backend Type | Frontend Type | Status |
|-------|-------------|---------------|--------|
| `metadata` | `Option<serde_json::Value>` | `Map<String, String>?` | ⚠️ Type mismatch |

**Issue**: Backend accepts any JSON object, but frontend only supports string maps.

**Example Backend Accepts**:
```json
{
  "metadata": {
    "color": "#FF0000",
    "priority": 1,
    "tags": ["home", "safe"]
  }
}
```

**Frontend Only Supports**:
```json
{
  "metadata": {
    "color": "#FF0000",
    "priority": "1"
  }
}
```

**Recommendation**: Either update frontend to use `JsonElement?` or keep as-is if string maps are sufficient.

---

## Authentication & Headers

| Aspect | Backend Requirement | Frontend Implementation | Status |
|--------|---------------------|------------------------|--------|
| Auth Header | `X-API-Key` | ✅ All services include header | ✅ CORRECT |
| Content-Type | `application/json` | ✅ Set on all POST/PATCH | ✅ CORRECT |
| Request ID | `X-Request-ID` (optional) | Not implemented | ⚠️ Optional |

---

## Recommendations

### Priority 1: Critical (Blocking Features)

1. **Create `TripApiService`**
   - Implement all 6 trip-related endpoints
   - Add corresponding DTOs for request/response
   - Integrate with trip tracking UI

2. **Create `MovementEventApiService`**
   - Implement event upload (single and batch)
   - Implement event history query
   - Add transportation mode enum

3. **Add `deleteDevice()` to `DeviceApiService`**
   - Simple addition to existing service
   - Add confirmation dialog in UI

### Priority 2: High (Data Completeness)

4. **Update `LocationPayload`** to include:
   - `transportationMode: String?`
   - `detectionSource: String?`
   - `tripId: String?`

5. **Update `ProximityAlertDto`** to include:
   - `isTriggered: Boolean`
   - `lastTriggeredAt: String?`

### Priority 3: Medium (Nice to Have)

6. **Add GDPR Privacy Endpoints** (if compliance required)
   - Data export endpoint
   - Data deletion endpoint

7. **Fix metadata type handling**
   - Use `JsonElement` instead of `Map<String, String>`

### Priority 4: Low (Optional)

8. **Add `X-Request-ID` header** for request tracing
9. **Standardize Float vs Double types** across all DTOs

---

## Testing Checklist

When implementing missing endpoints, verify:

- [ ] Trip creation with valid coordinates
- [ ] Trip state transitions (ACTIVE → COMPLETED, ACTIVE → CANCELLED)
- [ ] Trip listing with cursor pagination
- [ ] Movement event batch upload (up to 50 events)
- [ ] Movement event history with time filters
- [ ] Device deletion and group update
- [ ] Error handling for 404, 409, 429 responses
- [ ] Rate limiting on path correction (1 per hour per trip)

---

## Version History

| Date | Change | Author |
|------|--------|--------|
| 2024-11-30 | Initial compatibility analysis | Claude |

---

## Related Documents

- [API_REFERENCE.md](./API_REFERENCE.md) - Full API documentation
- [BACKEND_API_SPEC.md](./BACKEND_API_SPEC.md) - Backend specification
- [ARCHITECTURE.md](./ARCHITECTURE.md) - System architecture
- [DATA_MODELS.md](./DATA_MODELS.md) - Data model definitions

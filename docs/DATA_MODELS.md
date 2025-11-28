# Phone Manager Data Models

## Overview

Phone Manager uses a three-layer data model architecture:
- **Domain Models**: Pure Kotlin data classes representing business entities
- **Room Entities**: Database persistence with SQLite
- **API DTOs**: Network data transfer objects

## Domain Models

Domain models are located in `domain/model/` and represent the core business entities.

### Device

Represents a registered device in the system.

```kotlin
data class Device(
    val deviceId: String,          // Unique UUID
    val displayName: String,       // User-friendly name (2-50 chars)
    val lastLocation: DeviceLocation?,
    val lastSeenAt: Instant?,
)

data class DeviceLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float?,
    val timestamp: Instant,
)
```

### Geofence

Represents a geographic boundary with event triggers.

```kotlin
data class Geofence(
    val id: String,                        // Unique UUID
    val deviceId: String,                  // Owner device
    val name: String,                      // Display name
    val latitude: Double,                  // Center latitude
    val longitude: Double,                 // Center longitude
    val radiusMeters: Int,                 // Radius (50-10,000m)
    val transitionTypes: Set<TransitionType>,
    val webhookId: String?,                // Linked webhook (optional)
    val active: Boolean,                   // Enable/disable
    val createdAt: Instant,
    val updatedAt: Instant,
)

enum class TransitionType {
    ENTER,   // Device enters geofence
    EXIT,    // Device exits geofence
    DWELL,   // Device stays in geofence
}
```

### GeofenceEvent

Represents a triggered geofence event.

```kotlin
data class GeofenceEvent(
    val id: String,
    val deviceId: String,
    val geofenceId: String,
    val eventType: TransitionType,
    val timestamp: Instant,
    val latitude: Double,
    val longitude: Double,
    val webhookDelivered: Boolean?,        // Webhook delivery status
    val webhookResponseCode: Int?,         // HTTP response code
)
```

### Webhook

Represents a webhook configuration for external integrations.

```kotlin
data class Webhook(
    val id: String,
    val ownerDeviceId: String,
    val name: String,                      // Display name
    val targetUrl: String,                 // HTTPS URL
    val secret: String,                    // HMAC secret
    val enabled: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
)
```

### ProximityAlert

Represents a proximity alert between two devices.

```kotlin
data class ProximityAlert(
    val id: String,
    val ownerDeviceId: String,             // Alert owner
    val targetDeviceId: String,            // Device to monitor
    val radiusMeters: Int,                 // Alert radius (50-10,000m)
    val direction: AlertDirection,
    val active: Boolean,
    val lastState: ProximityState,
    val createdAt: Instant,
    val updatedAt: Instant,
    val lastTriggeredAt: Instant?,
)

enum class AlertDirection {
    ENTER,   // Alert when target enters range
    EXIT,    // Alert when target exits range
    BOTH,    // Alert on both transitions
}

enum class ProximityState {
    INSIDE,  // Target is within range
    OUTSIDE, // Target is outside range
}
```

---

## Room Entities

Room entities are located in `data/model/` and provide database persistence.

### LocationEntity

Stores GPS location records.

```kotlin
@Entity(tableName = "locations")
data class LocationEntity(
    @PrimaryKey val id: String,
    val deviceId: String,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float?,
    val altitude: Double?,
    val speed: Float?,
    val provider: String?,
    val timestamp: Long,                   // Epoch milliseconds
    val isSynced: Boolean = false,         // Server sync status
    val syncedAt: Long? = null,            // Sync timestamp
)
```

**Indexes:** `deviceId`, `timestamp`

### LocationQueueEntity

Upload queue for batch location sync.

```kotlin
@Entity(tableName = "location_queue")
data class LocationQueueEntity(
    @PrimaryKey val id: String,
    val locationId: String,
    val status: String,                    // PENDING, PROCESSING, FAILED
    val retryCount: Int = 0,
    val createdAt: Long,
    val updatedAt: Long,
)
```

### GeofenceEntity

Stores geofence definitions.

```kotlin
@Entity(tableName = "geofences")
data class GeofenceEntity(
    @PrimaryKey val id: String,
    val deviceId: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Int,
    val transitionTypes: String,           // Comma-separated: "ENTER,EXIT"
    val webhookId: String?,
    val active: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)
```

### GeofenceEventEntity

Stores geofence trigger events.

```kotlin
@Entity(tableName = "geofence_events")
data class GeofenceEventEntity(
    @PrimaryKey val id: String,
    val deviceId: String,
    val geofenceId: String,
    val eventType: String,                 // ENTER, EXIT, DWELL
    val timestamp: Long,
    val latitude: Double,
    val longitude: Double,
    val webhookDelivered: Boolean?,
    val webhookResponseCode: Int?,
)
```

### WebhookEntity

Stores webhook configurations.

```kotlin
@Entity(tableName = "webhooks")
data class WebhookEntity(
    @PrimaryKey val id: String,
    val ownerDeviceId: String,
    val name: String,
    val targetUrl: String,
    val secret: String,
    val enabled: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)
```

### ProximityAlertEntity

Stores proximity alert configurations.

```kotlin
@Entity(tableName = "proximity_alerts")
data class ProximityAlertEntity(
    @PrimaryKey val id: String,
    val ownerDeviceId: String,
    val targetDeviceId: String,
    val radiusMeters: Int,
    val direction: String,                 // ENTER, EXIT, BOTH
    val active: Boolean,
    val lastState: String,                 // INSIDE, OUTSIDE
    val createdAt: Long,
    val updatedAt: Long,
    val lastTriggeredAt: Long?,
)
```

---

## API DTOs

API data transfer objects are located in `network/models/`.

### Device DTOs

```kotlin
@Serializable
data class RegisterDeviceRequest(
    val deviceId: String,
    val displayName: String,
    val groupId: String,
)

@Serializable
data class RegisterDeviceResponse(
    val deviceId: String,
    val displayName: String,
    val groupId: String,
    val createdAt: String,                 // ISO 8601
)

@Serializable
data class DeviceDto(
    val deviceId: String,
    val displayName: String,
    val lastLocation: LocationDto?,
    val lastSeenAt: String?,
)

@Serializable
data class ListDevicesResponse(
    val devices: List<DeviceDto>,
    val total: Int,
)
```

### Location DTOs

```kotlin
@Serializable
data class LocationUploadRequest(
    val deviceId: String,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float?,
    val altitude: Double?,
    val speed: Float?,
    val provider: String?,
    val timestamp: String,                 // ISO 8601
)

@Serializable
data class BatchLocationRequest(
    val deviceId: String,
    val locations: List<LocationDto>,
)

@Serializable
data class LocationDto(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float?,
    val altitude: Double?,
    val speed: Float?,
    val timestamp: String,
)
```

### Geofence DTOs

```kotlin
@Serializable
data class CreateGeofenceRequest(
    val deviceId: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Double,
    val eventTypes: List<GeofenceEventType>,
    val active: Boolean = true,
)

@Serializable
data class UpdateGeofenceRequest(
    val name: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val radiusMeters: Double? = null,
    val eventTypes: List<GeofenceEventType>? = null,
    val active: Boolean? = null,
)

@Serializable
data class GeofenceDto(
    val geofenceId: String,
    val deviceId: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Double,
    val eventTypes: List<GeofenceEventType>,
    val active: Boolean,
    val metadata: Map<String, String>?,    // Contains webhookId
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class ListGeofencesResponse(
    val geofences: List<GeofenceDto>,
    val total: Int,
)

@Serializable
enum class GeofenceEventType {
    ENTER, EXIT, DWELL
}
```

### Geofence Event DTOs

```kotlin
@Serializable
data class ReportGeofenceEventRequest(
    val deviceId: String,
    val geofenceId: String,
    val eventType: GeofenceEventType,
    val timestamp: String,
    val latitude: Double,
    val longitude: Double,
)

@Serializable
data class GeofenceEventDto(
    val eventId: String,
    val deviceId: String,
    val geofenceId: String,
    val eventType: GeofenceEventType,
    val timestamp: String,
    val location: LocationDto?,
    val webhookDelivered: Boolean?,
    val webhookResponseCode: Int?,
)
```

### Webhook DTOs

```kotlin
@Serializable
data class CreateWebhookRequest(
    val ownerDeviceId: String,
    val name: String,
    val targetUrl: String,
    val secret: String,
    val enabled: Boolean = true,
)

@Serializable
data class UpdateWebhookRequest(
    val name: String? = null,
    val targetUrl: String? = null,
    val enabled: Boolean? = null,
)

@Serializable
data class WebhookDto(
    val webhookId: String,
    val ownerDeviceId: String,
    val name: String,
    val targetUrl: String,
    val enabled: Boolean,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class ListWebhooksResponse(
    val webhooks: List<WebhookDto>,
    val total: Int,
)
```

### Proximity Alert DTOs

```kotlin
@Serializable
data class CreateProximityAlertRequest(
    val ownerDeviceId: String,
    val targetDeviceId: String,
    val radiusMeters: Int,
    val direction: String,
    val active: Boolean = true,
)

@Serializable
data class ProximityAlertDto(
    val alertId: String,
    val ownerDeviceId: String,
    val targetDeviceId: String,
    val radiusMeters: Int,
    val direction: String,
    val active: Boolean,
    val lastState: String,
    val createdAt: String,
    val updatedAt: String,
    val lastTriggeredAt: String?,
)
```

---

## Entity Mappers

Mappers convert between domain models, entities, and DTOs.

### Example: Geofence Mappers

```kotlin
// Entity → Domain
fun GeofenceEntity.toDomain(): Geofence = Geofence(
    id = id,
    deviceId = deviceId,
    name = name,
    latitude = latitude,
    longitude = longitude,
    radiusMeters = radiusMeters,
    transitionTypes = transitionTypes
        .split(",")
        .map { TransitionType.valueOf(it) }
        .toSet(),
    webhookId = webhookId,
    active = active,
    createdAt = Instant.fromEpochMilliseconds(createdAt),
    updatedAt = Instant.fromEpochMilliseconds(updatedAt),
)

// Domain → Entity
fun Geofence.toEntity(): GeofenceEntity = GeofenceEntity(
    id = id,
    deviceId = deviceId,
    name = name,
    latitude = latitude,
    longitude = longitude,
    radiusMeters = radiusMeters,
    transitionTypes = transitionTypes.joinToString(",") { it.name },
    webhookId = webhookId,
    active = active,
    createdAt = createdAt.toEpochMilliseconds(),
    updatedAt = updatedAt.toEpochMilliseconds(),
)

// DTO → Domain
fun GeofenceDto.toDomain(): Geofence = Geofence(
    id = geofenceId,
    deviceId = deviceId,
    name = name,
    latitude = latitude,
    longitude = longitude,
    radiusMeters = radiusMeters.toInt(),
    transitionTypes = eventTypes.map { it.toTransitionType() }.toSet(),
    webhookId = metadata?.get("webhookId"),
    active = active,
    createdAt = Instant.parse(createdAt),
    updatedAt = Instant.parse(updatedAt),
)
```

---

## Database Schema

### Current Version: 7

| Table | Version Added | Primary Key | Indexes |
|-------|---------------|-------------|---------|
| locations | 1 | id | deviceId, timestamp |
| location_queue | 1 | id | status |
| proximity_alerts | 4 | id | ownerDeviceId |
| geofences | 5 | id | deviceId |
| geofence_events | 6 | id | deviceId, geofenceId |
| webhooks | 7 | id | ownerDeviceId |

### Migration History

| Migration | Changes |
|-----------|---------|
| 2 → 3 | Add `isSynced`, `syncedAt` to locations |
| 3 → 4 | Create `proximity_alerts` table |
| 4 → 5 | Create `geofences` table |
| 5 → 6 | Create `geofence_events` table |
| 6 → 7 | Create `webhooks` table |

---

## Validation Rules

### Device
- `displayName`: 2-50 characters, alphanumeric with spaces
- `groupId`: Required, non-empty

### Geofence
- `name`: Non-empty
- `latitude`: -90 to 90
- `longitude`: -180 to 180
- `radiusMeters`: 50 to 10,000

### Webhook
- `name`: Non-empty
- `targetUrl`: Valid HTTPS URL
- `secret`: Auto-generated UUID

### ProximityAlert
- `radiusMeters`: 50 to 10,000
- `ownerDeviceId`: Must differ from `targetDeviceId`

---

## Type Converters

Room type converters handle complex types:

```kotlin
class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Instant? =
        value?.let { Instant.fromEpochMilliseconds(it) }

    @TypeConverter
    fun toTimestamp(instant: Instant?): Long? =
        instant?.toEpochMilliseconds()

    @TypeConverter
    fun fromStringList(value: String?): List<String>? =
        value?.split(",")

    @TypeConverter
    fun toStringList(list: List<String>?): String? =
        list?.joinToString(",")
}
```

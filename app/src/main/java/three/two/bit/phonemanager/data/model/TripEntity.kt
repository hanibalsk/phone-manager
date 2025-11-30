package three.two.bit.phonemanager.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import three.two.bit.phonemanager.domain.model.LatLng
import three.two.bit.phonemanager.domain.model.Trip
import three.two.bit.phonemanager.domain.model.TripState
import three.two.bit.phonemanager.domain.model.TripTrigger
import three.two.bit.phonemanager.movement.TransportationMode

/**
 * Story E8.1: TripEntity - Database entity for storing trip data
 *
 * Represents an automatically detected trip with start/end locations,
 * duration, distance, and transportation mode breakdown.
 *
 * @see ANDROID_APP_SPEC.md Section 2.2
 */
@Entity(
    tableName = "trips",
    indices = [
        Index("startTime"),
        Index("state"),
        Index("isSynced"),
    ],
)
data class TripEntity(
    @PrimaryKey
    val id: String, // UUID generated client-side

    // State
    val state: String, // TripState: IDLE, ACTIVE, PENDING_END, COMPLETED

    // Timing
    val startTime: Long, // Unix timestamp (ms)
    val endTime: Long?, // Null if active

    // Start Location
    val startLatitude: Double,
    val startLongitude: Double,

    // End Location
    val endLatitude: Double?,
    val endLongitude: Double?,

    // Statistics
    val totalDistanceMeters: Double = 0.0,
    val locationCount: Int = 0,

    // Transportation Modes
    val dominantMode: String, // TransportationMode.name
    val modesUsedJson: String, // JSON array: ["WALKING", "IN_VEHICLE"]
    val modeBreakdownJson: String, // JSON object: {"WALKING": 300000, "IN_VEHICLE": 2400000} (ms)

    // Triggers
    val startTrigger: String, // TripTrigger: MODE_CHANGE, TIME, DISTANCE, MANUAL
    val endTrigger: String?,

    // Sync Status
    val isSynced: Boolean = false,
    val syncedAt: Long? = null,
    val serverId: String? = null, // Server-assigned ID

    // Timestamps
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

/**
 * Story E8.3: Convert TripEntity to Trip domain model (AC E8.3.7)
 */
fun TripEntity.toDomain(): Trip {
    // Parse modesUsed JSON array
    val modesUsed = try {
        Json.decodeFromString<List<String>>(modesUsedJson)
            .mapNotNull { modeName ->
                TransportationMode.entries.find { it.name == modeName }
            }
            .toSet()
    } catch (_: Exception) {
        setOf(TransportationMode.valueOf(dominantMode))
    }

    // Parse modeBreakdown JSON object
    val modeBreakdown = try {
        Json.decodeFromString<Map<String, Long>>(modeBreakdownJson)
            .mapNotNull { (key, value) ->
                TransportationMode.entries.find { it.name == key }?.let { it to value }
            }
            .toMap()
    } catch (_: Exception) {
        emptyMap()
    }

    return Trip(
        id = id,
        state = TripState.entries.find { it.name == state } ?: TripState.IDLE,
        startTime = Instant.fromEpochMilliseconds(startTime),
        endTime = endTime?.let { Instant.fromEpochMilliseconds(it) },
        startLocation = LatLng(startLatitude, startLongitude),
        endLocation = if (endLatitude != null && endLongitude != null) {
            LatLng(endLatitude, endLongitude)
        } else {
            null
        },
        totalDistanceMeters = totalDistanceMeters,
        locationCount = locationCount,
        dominantMode = TransportationMode.entries.find { it.name == dominantMode }
            ?: TransportationMode.UNKNOWN,
        modesUsed = modesUsed,
        modeBreakdown = modeBreakdown,
        startTrigger = TripTrigger.entries.find { it.name == startTrigger } ?: TripTrigger.MANUAL,
        endTrigger = endTrigger?.let { TripTrigger.entries.find { trigger -> trigger.name == it } },
        isSynced = isSynced,
        syncedAt = syncedAt?.let { Instant.fromEpochMilliseconds(it) },
        serverId = serverId,
        createdAt = Instant.fromEpochMilliseconds(createdAt),
        updatedAt = Instant.fromEpochMilliseconds(updatedAt),
    )
}

/**
 * Story E8.3: Convert Trip domain model to TripEntity (AC E8.3.7)
 */
fun Trip.toEntity(): TripEntity = TripEntity(
    id = id,
    state = state.name,
    startTime = startTime.toEpochMilliseconds(),
    endTime = endTime?.toEpochMilliseconds(),
    startLatitude = startLocation.latitude,
    startLongitude = startLocation.longitude,
    endLatitude = endLocation?.latitude,
    endLongitude = endLocation?.longitude,
    totalDistanceMeters = totalDistanceMeters,
    locationCount = locationCount,
    dominantMode = dominantMode.name,
    modesUsedJson = Json.encodeToString(modesUsed.map { it.name }),
    modeBreakdownJson = Json.encodeToString(modeBreakdown.map { (k, v) -> k.name to v }.toMap()),
    startTrigger = startTrigger.name,
    endTrigger = endTrigger?.name,
    isSynced = isSynced,
    syncedAt = syncedAt?.toEpochMilliseconds(),
    serverId = serverId,
    createdAt = createdAt.toEpochMilliseconds(),
    updatedAt = updatedAt.toEpochMilliseconds(),
)

package three.two.bit.phonemanager.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Epic 0.2.2/E4.2/E8.1: LocationEntity - Database entity for storing location data
 *
 * Story E4.2 additions: Sync tracking fields
 * Story E8.1 additions: Transportation mode context, trip association, backend corrections
 */
@Entity(
    tableName = "locations",
    indices = [
        Index("tripId"),
        Index("transportationMode"),
    ],
)
data class LocationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val timestamp: Long,
    val altitude: Double? = null,
    val bearing: Float? = null,
    val speed: Float? = null,
    val provider: String? = null,
    // Story E4.2: Sync tracking (AC E4.2.4)
    val isSynced: Boolean = false,
    val syncedAt: Long? = null,
    // Story E8.1: Transportation Mode Context (AC E8.1.3)
    val transportationMode: String? = null, // TransportationMode.name
    val detectionSource: String? = null, // DetectionSource.name
    val modeConfidence: Float? = null, // 0.0 - 1.0
    // Story E8.1: Trip Association (AC E8.1.3)
    val tripId: String? = null, // Associated trip ID
    // Story E8.1: Backend Corrections (AC E8.1.3)
    val correctedLatitude: Double? = null,
    val correctedLongitude: Double? = null,
    val correctionSource: String? = null, // ROAD_SNAP, INTERPOLATION, etc.
    val correctedAt: Long? = null,
)

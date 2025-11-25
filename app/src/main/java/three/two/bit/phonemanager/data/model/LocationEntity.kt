package three.two.bit.phonemanager.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Epic 0.2.2/E4.2: LocationEntity - Database entity for storing location data
 *
 * Story E4.2 additions: Sync tracking fields
 */
@Entity(tableName = "locations")
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
)

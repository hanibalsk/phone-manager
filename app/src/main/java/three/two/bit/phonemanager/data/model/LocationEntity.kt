package three.two.bit.phonemanager.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Epic 0.2.2: LocationEntity - Database entity for storing location data
 * Stub implementation for Epic 1 development
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
)

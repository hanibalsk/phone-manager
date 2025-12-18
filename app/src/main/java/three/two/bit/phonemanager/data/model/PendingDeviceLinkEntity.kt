package three.two.bit.phonemanager.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Story UGM-1.4: Pending device link entity for offline queue
 *
 * Stores device link operations that failed due to network issues
 * and need to be retried when connectivity is restored.
 */
@Entity(tableName = "pending_device_links")
data class PendingDeviceLinkEntity(
    @PrimaryKey
    val deviceId: String,
    val userId: String,
    val timestamp: Long,
    val retryCount: Int = 0,
)

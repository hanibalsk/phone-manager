package three.two.bit.phonemanager.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import three.two.bit.phonemanager.data.model.LocationEntity
import three.two.bit.phonemanager.data.model.LocationQueueEntity

/**
 * Story 0.2.3: AppDatabase - Room database for Phone Manager
 *
 * Version 2: Added LocationQueueEntity for upload queue management
 */
@Database(
    entities = [
        LocationEntity::class,
        LocationQueueEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun locationDao(): LocationDao
    abstract fun locationQueueDao(): LocationQueueDao

    companion object {
        const val DATABASE_NAME = "phone_manager_db"
    }
}

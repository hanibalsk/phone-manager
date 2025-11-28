package three.two.bit.phonemanager.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import three.two.bit.phonemanager.data.database.AppDatabase
import three.two.bit.phonemanager.data.database.GeofenceDao
import three.two.bit.phonemanager.data.database.GeofenceEventDao
import three.two.bit.phonemanager.data.database.LocationDao
import three.two.bit.phonemanager.data.database.LocationQueueDao
import three.two.bit.phonemanager.data.database.ProximityAlertDao
import three.two.bit.phonemanager.data.database.WebhookDao
import javax.inject.Singleton

/**
 * Story 0.2.3/E4.2/E5.1/E6.1/E6.2/E6.3: DatabaseModule - Provides Room database and DAOs
 *
 * Story E4.2: Add migration for sync tracking fields
 * Story E5.1: Add ProximityAlert table and DAO
 * Story E6.1: Add Geofence table and DAO
 * Story E6.2: Add GeofenceEvent table and DAO
 * Story E6.3: Add Webhook table and DAO
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        AppDatabase.DATABASE_NAME,
    )
        .addMigrations(
            AppDatabase.MIGRATION_2_3,
            AppDatabase.MIGRATION_3_4,
            AppDatabase.MIGRATION_4_5,
            AppDatabase.MIGRATION_5_6,
            AppDatabase.MIGRATION_6_7,
        )
        .fallbackToDestructiveMigration()
        .build()

    @Provides
    @Singleton
    fun provideLocationDao(database: AppDatabase): LocationDao = database.locationDao()

    @Provides
    @Singleton
    fun provideLocationQueueDao(database: AppDatabase): LocationQueueDao = database.locationQueueDao()

    @Provides
    @Singleton
    fun provideProximityAlertDao(database: AppDatabase): ProximityAlertDao = database.proximityAlertDao()

    @Provides
    @Singleton
    fun provideGeofenceDao(database: AppDatabase): GeofenceDao = database.geofenceDao()

    @Provides
    @Singleton
    fun provideGeofenceEventDao(database: AppDatabase): GeofenceEventDao = database.geofenceEventDao()

    /**
     * Story E6.3: Webhook DAO
     */
    @Provides
    @Singleton
    fun provideWebhookDao(database: AppDatabase): WebhookDao = database.webhookDao()
}

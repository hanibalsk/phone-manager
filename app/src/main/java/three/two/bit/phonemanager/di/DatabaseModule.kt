package three.two.bit.phonemanager.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import three.two.bit.phonemanager.data.database.AppDatabase
import three.two.bit.phonemanager.data.database.LocationDao
import three.two.bit.phonemanager.data.database.LocationQueueDao
import javax.inject.Singleton

/**
 * Story 0.2.3/E4.2: DatabaseModule - Provides Room database and DAOs
 *
 * Story E4.2: Add migration for sync tracking fields
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
        .addMigrations(AppDatabase.MIGRATION_2_3)
        .fallbackToDestructiveMigration()
        .build()

    @Provides
    @Singleton
    fun provideLocationDao(database: AppDatabase): LocationDao = database.locationDao()

    @Provides
    @Singleton
    fun provideLocationQueueDao(database: AppDatabase): LocationQueueDao = database.locationQueueDao()
}

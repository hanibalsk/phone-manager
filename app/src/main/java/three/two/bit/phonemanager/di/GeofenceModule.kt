package three.two.bit.phonemanager.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import three.two.bit.phonemanager.geofence.GeofenceManager
import three.two.bit.phonemanager.geofence.GeofenceManagerImpl
import javax.inject.Singleton

/**
 * Story E6.1: Hilt module for geofence-related dependencies
 *
 * Provides GeofenceManager for Android Geofencing API integration
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class GeofenceModule {

    @Binds
    @Singleton
    abstract fun bindGeofenceManager(impl: GeofenceManagerImpl): GeofenceManager
}

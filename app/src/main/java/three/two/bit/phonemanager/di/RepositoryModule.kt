package three.two.bit.phonemanager.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import three.two.bit.phonemanager.data.repository.AlertRepository
import three.two.bit.phonemanager.data.repository.AlertRepositoryImpl
import three.two.bit.phonemanager.data.repository.DeviceRepository
import three.two.bit.phonemanager.data.repository.DeviceRepositoryImpl
import three.two.bit.phonemanager.data.repository.GeofenceRepository
import three.two.bit.phonemanager.data.repository.GeofenceRepositoryImpl
import three.two.bit.phonemanager.data.repository.LocationRepository
import three.two.bit.phonemanager.data.repository.LocationRepositoryImpl
import javax.inject.Singleton

/**
 * Epic 0.2/1: RepositoryModule - Binds repository interfaces to implementations
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindLocationRepository(impl: LocationRepositoryImpl): LocationRepository

    @Binds
    @Singleton
    abstract fun bindDeviceRepository(impl: DeviceRepositoryImpl): DeviceRepository

    /**
     * Story E5.1: Alert Repository binding
     */
    @Binds
    @Singleton
    abstract fun bindAlertRepository(impl: AlertRepositoryImpl): AlertRepository

    /**
     * Story E6.1: Geofence Repository binding
     */
    @Binds
    @Singleton
    abstract fun bindGeofenceRepository(impl: GeofenceRepositoryImpl): GeofenceRepository
}

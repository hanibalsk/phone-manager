package three.two.bit.phonemanager.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import three.two.bit.phonemanager.trip.TripManager
import three.two.bit.phonemanager.trip.TripManagerImpl
import javax.inject.Singleton

/**
 * Story E8.4: TripModule - Hilt module for trip management dependencies
 *
 * AC E8.4.7: Provides TripManager singleton
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class TripModule {

    /**
     * Bind TripManager interface to TripManagerImpl implementation.
     */
    @Binds
    @Singleton
    abstract fun bindTripManager(impl: TripManagerImpl): TripManager
}

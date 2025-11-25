package three.two.bit.phonemanager.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
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
}

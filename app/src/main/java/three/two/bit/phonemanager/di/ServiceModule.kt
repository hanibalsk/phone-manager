package three.two.bit.phonemanager.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import three.two.bit.phonemanager.service.LocationServiceController
import three.two.bit.phonemanager.service.LocationServiceControllerImpl
import javax.inject.Singleton

/**
 * Story 1.1: ServiceModule - Provides service controller
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ServiceModule {

    @Binds
    @Singleton
    abstract fun bindLocationServiceController(impl: LocationServiceControllerImpl): LocationServiceController
}

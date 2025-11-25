package com.phonemanager.di

import com.phonemanager.service.LocationServiceController
import com.phonemanager.service.LocationServiceControllerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
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

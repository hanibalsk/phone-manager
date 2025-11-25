package three.two.bit.phonemanager.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import three.two.bit.phonemanager.permission.PermissionManager
import three.two.bit.phonemanager.permission.PermissionManagerImpl
import javax.inject.Singleton

/**
 * Story 1.2: PermissionModule - Provides permission management dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class PermissionModule {

    @Binds
    @Singleton
    abstract fun bindPermissionManager(impl: PermissionManagerImpl): PermissionManager
}

package three.two.bit.phonemanager.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import three.two.bit.phonemanager.data.preferences.PreferencesRepository
import three.two.bit.phonemanager.data.preferences.PreferencesRepositoryImpl
import javax.inject.Singleton

/**
 * Story 1.1: PreferencesModule - Provides preferences repository
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class PreferencesModule {

    @Binds
    @Singleton
    abstract fun bindPreferencesRepository(impl: PreferencesRepositoryImpl): PreferencesRepository
}

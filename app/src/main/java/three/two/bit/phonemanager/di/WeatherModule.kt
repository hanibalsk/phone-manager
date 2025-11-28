package three.two.bit.phonemanager.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import three.two.bit.phonemanager.data.cache.WeatherCache
import three.two.bit.phonemanager.data.cache.WeatherCacheImpl
import three.two.bit.phonemanager.data.repository.WeatherRepository
import three.two.bit.phonemanager.data.repository.WeatherRepositoryImpl
import three.two.bit.phonemanager.network.WeatherApiService
import three.two.bit.phonemanager.network.WeatherApiServiceImpl
import javax.inject.Singleton

/**
 * Story E7.1: WeatherModule - Hilt DI bindings for weather components
 *
 * AC E7.1.5: Provides all weather-related dependencies as singletons
 */
@Module
@InstallIn(SingletonComponent::class)
object WeatherModule {

    /**
     * AC E7.1.5: Provide WeatherApiService (singleton)
     * Uses Ktor HttpClient from NetworkModule
     */
    @Provides
    @Singleton
    fun provideWeatherApiService(httpClient: HttpClient): WeatherApiService = WeatherApiServiceImpl(httpClient)
}

/**
 * Story E7.1: WeatherModule - Abstract bindings for weather components
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class WeatherBindingsModule {

    /**
     * AC E7.1.5: Bind WeatherRepository (singleton)
     */
    @Binds
    @Singleton
    abstract fun bindWeatherRepository(impl: WeatherRepositoryImpl): WeatherRepository

    /**
     * AC E7.1.5: Bind WeatherCache (singleton)
     */
    @Binds
    @Singleton
    abstract fun bindWeatherCache(impl: WeatherCacheImpl): WeatherCache
}

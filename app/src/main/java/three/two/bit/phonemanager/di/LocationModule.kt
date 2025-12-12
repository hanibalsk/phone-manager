package three.two.bit.phonemanager.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import three.two.bit.phonemanager.location.GeocodingService
import three.two.bit.phonemanager.location.LocationManager
import javax.inject.Singleton

/**
 * Hilt module for location-related dependencies
 *
 * Story 0.2.1: Provides LocationManager singleton
 * Trip Geocoding Enhancement: Provides GeocodingService singleton
 */
@Module
@InstallIn(SingletonComponent::class)
object LocationModule {

    @Provides
    @Singleton
    fun provideLocationManager(@ApplicationContext context: Context): LocationManager = LocationManager(context)

    @Provides
    @Singleton
    fun provideGeocodingService(@ApplicationContext context: Context): GeocodingService = GeocodingService(context)
}

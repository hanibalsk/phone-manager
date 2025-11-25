package com.phonemanager.di

import com.phonemanager.BuildConfig
import com.phonemanager.analytics.Analytics
import com.phonemanager.analytics.DebugAnalytics
import com.phonemanager.analytics.NoOpAnalytics
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for analytics dependency injection
 *
 * Story 1.2, AC 1.2.12: Analytics tracking
 */
@Module
@InstallIn(SingletonComponent::class)
object AnalyticsModule {

    @Provides
    @Singleton
    fun provideAnalytics(): Analytics = if (BuildConfig.DEBUG) {
        // Use DebugAnalytics in debug builds for Timber logging
        DebugAnalytics()
    } else {
        // Use NoOpAnalytics in release builds until real analytics is configured
        // TODO: Replace with FirebaseAnalytics or other provider
        NoOpAnalytics()
    }
}

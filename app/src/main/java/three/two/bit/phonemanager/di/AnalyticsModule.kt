package three.two.bit.phonemanager.di

import android.content.Context
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import three.two.bit.phonemanager.BuildConfig
import three.two.bit.phonemanager.analytics.Analytics
import three.two.bit.phonemanager.analytics.DebugAnalytics
import three.two.bit.phonemanager.analytics.FirebaseAnalyticsImpl
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
    fun provideFirebaseAnalytics(@ApplicationContext context: Context): FirebaseAnalytics {
        return FirebaseAnalytics.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideAnalytics(firebaseAnalytics: FirebaseAnalytics): Analytics = if (BuildConfig.DEBUG) {
        // Use DebugAnalytics in debug builds for Timber logging
        DebugAnalytics()
    } else {
        // Use FirebaseAnalytics in release builds
        FirebaseAnalyticsImpl(firebaseAnalytics)
    }
}

package three.two.bit.phonemanager.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import three.two.bit.phonemanager.BuildConfig
import three.two.bit.phonemanager.network.ApiConfiguration
import three.two.bit.phonemanager.security.SecureStorage
import javax.inject.Singleton

/**
 * Module that provides API configuration.
 *
 * This module is separated from NetworkModule to allow tests to easily
 * replace it with a test configuration using @TestInstallIn.
 */
@Module
@InstallIn(SingletonComponent::class)
object ApiConfigurationModule {

    @Provides
    @Singleton
    fun provideApiConfiguration(
        @ApplicationContext context: Context,
        secureStorage: SecureStorage,
    ): ApiConfiguration {
        // Get base URL from secure storage - require HTTPS in production
        val baseUrl = secureStorage.getApiBaseUrl()
            ?: BuildConfig.API_BASE_URL.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException(
                "API base URL not configured. Set API_BASE_URL in build config or configure via SecureStorage.",
            )

        // Validate URL uses HTTPS in release builds
        if (!BuildConfig.DEBUG && !baseUrl.startsWith("https://")) {
            throw IllegalStateException(
                "API base URL must use HTTPS in production builds: $baseUrl",
            )
        }

        // Get API key from secure storage - require proper configuration
        val apiKey = secureStorage.getApiKey()
            ?: BuildConfig.API_KEY.takeIf { it.isNotBlank() && it != "default-api-key-change-me" }
            ?: throw IllegalStateException(
                "API key not configured. Set API_KEY in build config or configure via SecureStorage.",
            )

        return ApiConfiguration(
            baseUrl = baseUrl,
            apiKey = apiKey,
        )
    }
}

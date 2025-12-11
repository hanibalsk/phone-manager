package three.two.bit.phonemanager.di

import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import three.two.bit.phonemanager.network.ApiConfiguration
import javax.inject.Singleton

/**
 * Test module that provides fake API configuration for instrumented tests.
 *
 * This module replaces the ApiConfigurationModule, allowing tests to run
 * without requiring real API credentials.
 *
 * The baseUrl points to localhost:8080 which matches MockWebServerRule's default port.
 */
@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [ApiConfigurationModule::class],
)
object TestApiConfigurationModule {

    @Provides
    @Singleton
    fun provideApiConfiguration(): ApiConfiguration {
        // Use localhost with MockWebServer's default port
        return ApiConfiguration(
            baseUrl = "http://localhost:8080",
            apiKey = "test-api-key-for-instrumented-tests",
        )
    }
}

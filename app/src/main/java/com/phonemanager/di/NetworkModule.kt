package com.phonemanager.di

import android.content.Context
import com.phonemanager.BuildConfig
import com.phonemanager.network.ApiConfiguration
import com.phonemanager.network.LocationApiService
import com.phonemanager.network.LocationApiServiceImpl
import com.phonemanager.security.SecureStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Singleton

/**
 * Story 0.2.2: NetworkModule - Provides Ktor HttpClient and API services
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json {
        return Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
            prettyPrint = BuildConfig.DEBUG
        }
    }

    @Provides
    @Singleton
    fun provideHttpClient(json: Json): HttpClient {
        return HttpClient(Android) {
            // JSON serialization
            install(ContentNegotiation) {
                json(json)
            }

            // Logging (only in debug builds)
            if (BuildConfig.DEBUG) {
                install(Logging) {
                    logger = object : Logger {
                        override fun log(message: String) {
                            Timber.tag("Ktor").d(message)
                        }
                    }
                    level = LogLevel.HEADERS
                }
            }

            // Default request configuration
            defaultRequest {
                contentType(ContentType.Application.Json)
            }

            // Configure engine
            engine {
                connectTimeout = 30_000
                socketTimeout = 30_000
            }
        }
    }

    @Provides
    @Singleton
    fun provideApiConfiguration(
        @ApplicationContext context: Context,
        secureStorage: SecureStorage
    ): ApiConfiguration {
        // Get base URL from secure storage or use default
        val baseUrl = secureStorage.getApiBaseUrl() ?: "https://api.phonemanager.example.com"

        // Get API key from secure storage or use default
        val apiKey = secureStorage.getApiKey() ?: "default-api-key-change-me"

        return ApiConfiguration(
            baseUrl = baseUrl,
            apiKey = apiKey
        )
    }

    @Provides
    @Singleton
    fun provideLocationApiService(
        httpClient: HttpClient,
        apiConfig: ApiConfiguration
    ): LocationApiService {
        return LocationApiServiceImpl(httpClient, apiConfig)
    }
}

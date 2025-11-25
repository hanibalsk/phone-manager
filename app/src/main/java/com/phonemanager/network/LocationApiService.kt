package com.phonemanager.network

import com.phonemanager.network.models.LocationBatchPayload
import com.phonemanager.network.models.LocationPayload
import com.phonemanager.network.models.LocationUploadResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Story 0.2.2: LocationApiService - HTTP client for location transmission
 *
 * Provides methods to upload single or batched locations to backend API
 */
interface LocationApiService {
    suspend fun uploadLocation(location: LocationPayload): Result<LocationUploadResponse>
    suspend fun uploadLocations(batch: LocationBatchPayload): Result<LocationUploadResponse>
}

@Singleton
class LocationApiServiceImpl @Inject constructor(
    private val httpClient: HttpClient,
    private val apiConfig: ApiConfiguration,
) : LocationApiService {

    /**
     * Upload single location to backend
     */
    override suspend fun uploadLocation(location: LocationPayload): Result<LocationUploadResponse> = try {
        Timber.d("Uploading single location: lat=${location.latitude}, lon=${location.longitude}")

        val response: LocationUploadResponse = httpClient.post(apiConfig.uploadEndpoint) {
            contentType(ContentType.Application.Json)
            header("X-API-Key", apiConfig.apiKey)
            setBody(location)
        }.body()

        Timber.i("Location uploaded successfully: ${response.message}")
        Result.success(response)
    } catch (e: Exception) {
        Timber.e(e, "Failed to upload location")
        Result.failure(e)
    }

    /**
     * Upload batch of locations to backend
     */
    override suspend fun uploadLocations(batch: LocationBatchPayload): Result<LocationUploadResponse> = try {
        Timber.d("Uploading location batch: ${batch.locations.size} locations")

        val response: LocationUploadResponse = httpClient.post(apiConfig.batchUploadEndpoint) {
            contentType(ContentType.Application.Json)
            header("X-API-Key", apiConfig.apiKey)
            setBody(batch)
        }.body()

        Timber.i("Location batch uploaded successfully: processed=${response.processedCount}")
        Result.success(response)
    } catch (e: Exception) {
        Timber.e(e, "Failed to upload location batch")
        Result.failure(e)
    }
}

/**
 * API configuration holder
 */
data class ApiConfiguration(
    val baseUrl: String,
    val apiKey: String,
    val uploadEndpoint: String = "$baseUrl/api/locations",
    val batchUploadEndpoint: String = "$baseUrl/api/locations/batch",
    val connectionTimeout: Long = 30_000L,
    val requestTimeout: Long = 30_000L,
)

package three.two.bit.phonemanager.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import three.two.bit.phonemanager.domain.model.DeviceEnrollmentInfo
import three.two.bit.phonemanager.domain.model.EnrollmentResult
import three.two.bit.phonemanager.network.models.DeviceInfoDto
import three.two.bit.phonemanager.network.models.EnrollDeviceRequest
import three.two.bit.phonemanager.network.models.EnrollDeviceResponse
import three.two.bit.phonemanager.network.models.UnenrollDeviceResponse
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Story E13.10: Android Enrollment Flow - API Service
 *
 * Provides methods for device enrollment and unenrollment.
 * AC E13.10.4: Enroll device with token
 * AC E13.10.9: Unenroll device
 */
interface EnrollmentApiService {

    /**
     * Enroll a device with an enrollment token.
     * POST /enroll
     *
     * AC E13.10.4: Call POST /enroll endpoint with code and device info.
     *
     * @param enrollmentToken The enrollment token/code from IT admin
     * @param deviceInfo Device information to send
     * @return Result with EnrollmentResult on success
     */
    suspend fun enrollDevice(
        enrollmentToken: String,
        deviceInfo: DeviceEnrollmentInfo,
    ): Result<EnrollmentResult>

    /**
     * Unenroll a device from an organization.
     * POST /devices/{deviceId}/unenroll
     *
     * AC E13.10.9: Call POST /devices/{id}/unenroll endpoint.
     *
     * @param deviceId The device ID to unenroll
     * @param accessToken The user's access token
     * @return Result with Unit on success
     */
    suspend fun unenrollDevice(
        deviceId: String,
        accessToken: String,
    ): Result<Unit>
}

/**
 * Implementation of EnrollmentApiService using Ktor HttpClient.
 */
@Singleton
class EnrollmentApiServiceImpl @Inject constructor(
    private val httpClient: HttpClient,
    private val apiConfig: ApiConfiguration,
) : EnrollmentApiService {

    override suspend fun enrollDevice(
        enrollmentToken: String,
        deviceInfo: DeviceEnrollmentInfo,
    ): Result<EnrollmentResult> = runCatching {
        Timber.d("Enrolling device with token: ${enrollmentToken.take(4)}...")

        val request = EnrollDeviceRequest(
            enrollmentToken = enrollmentToken,
            deviceInfo = DeviceInfoDto.fromDomain(deviceInfo),
        )

        val response: EnrollDeviceResponse = httpClient.post("${apiConfig.baseUrl}/enroll") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

        Timber.i("Enrollment successful for org: ${response.organization.name}")
        response.toDomain()
    }.onFailure { e ->
        Timber.e(e, "Enrollment failed")
    }

    override suspend fun unenrollDevice(
        deviceId: String,
        accessToken: String,
    ): Result<Unit> = runCatching {
        Timber.d("Unenrolling device: $deviceId")

        val response: UnenrollDeviceResponse = httpClient.post(
            "${apiConfig.baseUrl}/devices/$deviceId/unenroll",
        ) {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $accessToken")
        }.body()

        if (response.success) {
            Timber.i("Unenrollment successful for device: $deviceId")
        } else {
            throw Exception(response.message ?: "Unenrollment failed")
        }
    }.onFailure { e ->
        Timber.e(e, "Unenrollment failed for device: $deviceId")
    }
}

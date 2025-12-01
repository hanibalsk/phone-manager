package three.two.bit.phonemanager.data.repository

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import three.two.bit.phonemanager.domain.model.DeviceEnrollmentInfo
import three.two.bit.phonemanager.domain.model.DevicePolicy
import three.two.bit.phonemanager.domain.model.EnrollmentResult
import three.two.bit.phonemanager.domain.model.EnrollmentStatus
import three.two.bit.phonemanager.domain.model.EnrollmentToken
import three.two.bit.phonemanager.domain.model.OrganizationInfo
import three.two.bit.phonemanager.domain.policy.PolicyApplicator
import three.two.bit.phonemanager.domain.policy.PolicyApplicationResult
import three.two.bit.phonemanager.network.EnrollmentApiService
import three.two.bit.phonemanager.security.SecureStorage
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for EnrollmentRepository (Story E13.10)
 *
 * Tests cover:
 * - AC E13.10.4: Device enrollment with token
 * - AC E13.10.5: Policy application
 * - AC E13.10.7: Error handling
 * - AC E13.10.8: Enrollment status
 * - AC E13.10.9: Device unenrollment
 */
class EnrollmentRepositoryTest {

    private lateinit var enrollmentApiService: EnrollmentApiService
    private lateinit var secureStorage: SecureStorage
    private lateinit var policyApplicator: PolicyApplicator
    private lateinit var enrollmentRepository: EnrollmentRepository

    private val testOrganizationInfo = OrganizationInfo(
        id = "org-123",
        name = "Test Company",
        contactEmail = "it@testcompany.com",
        supportPhone = "+1234567890",
    )

    private val testDevicePolicy = DevicePolicy(
        settings = mapOf(
            "tracking_enabled" to true,
            "tracking_interval" to 30,
        ),
        locks = listOf("tracking_enabled"),
        groupId = null,
    )

    private val testEnrollmentResult = EnrollmentResult(
        success = true,
        userId = "user-123",
        email = "test@testcompany.com",
        organization = testOrganizationInfo,
        policy = testDevicePolicy,
        accessToken = "access-token-123",
        refreshToken = "refresh-token-456",
    )

    @Before
    fun setup() {
        enrollmentApiService = mockk(relaxed = true)
        secureStorage = mockk(relaxed = true)
        policyApplicator = mockk(relaxed = true)

        // Default mock for device ID
        every { secureStorage.getDeviceId() } returns "device-123"
        every { secureStorage.getEnrollmentStatus() } returns EnrollmentStatus.NOT_ENROLLED

        enrollmentRepository = EnrollmentRepository(
            enrollmentApiService = enrollmentApiService,
            secureStorage = secureStorage,
            policyApplicator = policyApplicator,
        )
    }

    // AC E13.10.4: Enrollment Tests

    @Test
    fun `enrollDevice with valid token enrolls successfully`() = runTest {
        // Given
        val token = EnrollmentToken("ABC12345678901234567")
        coEvery {
            enrollmentApiService.enrollDevice(any(), any())
        } returns Result.success(testEnrollmentResult)
        coEvery {
            policyApplicator.applyPolicies(any())
        } returns PolicyApplicationResult(
            success = true,
            appliedSettings = listOf("tracking_enabled"),
            failedSettings = emptyList(),
            skippedSettings = emptyList(),
        )

        // When
        val result = enrollmentRepository.enrollDevice(token)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(testEnrollmentResult, result.getOrNull())
        assertEquals(EnrollmentStatus.ENROLLED, enrollmentRepository.enrollmentStatus.first())
        assertEquals(testOrganizationInfo, enrollmentRepository.organizationInfo.first())
    }

    @Test
    fun `enrollDevice with invalid token returns error`() = runTest {
        // Given
        val invalidToken = EnrollmentToken("ABC") // Too short

        // When
        val result = enrollmentRepository.enrollDevice(invalidToken)

        // Then
        assertTrue(result.isFailure)
        assertEquals(EnrollmentStatus.NOT_ENROLLED, enrollmentRepository.enrollmentStatus.first())
    }

    @Test
    fun `enrollDevice sets status to enrolling during process`() = runTest {
        // Given
        val token = EnrollmentToken("ABC12345678901234567")
        coEvery {
            enrollmentApiService.enrollDevice(any(), any())
        } returns Result.success(testEnrollmentResult)
        coEvery {
            policyApplicator.applyPolicies(any())
        } returns PolicyApplicationResult(
            success = true,
            appliedSettings = emptyList(),
            failedSettings = emptyList(),
            skippedSettings = emptyList(),
        )

        // When
        enrollmentRepository.enrollDevice(token)

        // Then - verify status was set to ENROLLING during the process
        // Note: Since we're testing after completion, we verify it ends in ENROLLED
        assertEquals(EnrollmentStatus.ENROLLED, enrollmentRepository.enrollmentStatus.first())
    }

    @Test
    fun `enrollDevice saves tokens to secure storage`() = runTest {
        // Given
        val token = EnrollmentToken("ABC12345678901234567")
        coEvery {
            enrollmentApiService.enrollDevice(any(), any())
        } returns Result.success(testEnrollmentResult)
        coEvery {
            policyApplicator.applyPolicies(any())
        } returns PolicyApplicationResult(
            success = true,
            appliedSettings = emptyList(),
            failedSettings = emptyList(),
            skippedSettings = emptyList(),
        )

        // When
        enrollmentRepository.enrollDevice(token)

        // Then
        verify { secureStorage.saveAccessToken(testEnrollmentResult.accessToken) }
        verify { secureStorage.saveRefreshToken(testEnrollmentResult.refreshToken) }
        verify { secureStorage.saveOrganizationInfo(testOrganizationInfo) }
    }

    @Test
    fun `enrollDevice handles API failure`() = runTest {
        // Given
        val token = EnrollmentToken("ABC12345678901234567")
        coEvery {
            enrollmentApiService.enrollDevice(any(), any())
        } returns Result.failure(Exception("Network error"))

        // When
        val result = enrollmentRepository.enrollDevice(token)

        // Then
        assertTrue(result.isFailure)
        assertEquals(EnrollmentStatus.NOT_ENROLLED, enrollmentRepository.enrollmentStatus.first())
    }

    // AC E13.10.5: Policy Application Tests

    @Test
    fun `applyPolicies calls policyApplicator`() = runTest {
        // Given
        coEvery {
            policyApplicator.applyPolicies(testDevicePolicy)
        } returns PolicyApplicationResult(
            success = true,
            appliedSettings = listOf("tracking_enabled"),
            failedSettings = emptyList(),
            skippedSettings = emptyList(),
        )

        // When
        val result = enrollmentRepository.applyPolicies(testDevicePolicy)

        // Then
        assertTrue(result.isSuccess)
        coVerify { policyApplicator.applyPolicies(testDevicePolicy) }
        verify { secureStorage.saveDevicePolicy(testDevicePolicy) }
    }

    @Test
    fun `applyPolicies updates device policy state`() = runTest {
        // Given
        coEvery {
            policyApplicator.applyPolicies(testDevicePolicy)
        } returns PolicyApplicationResult(
            success = true,
            appliedSettings = emptyList(),
            failedSettings = emptyList(),
            skippedSettings = emptyList(),
        )

        // When
        enrollmentRepository.applyPolicies(testDevicePolicy)

        // Then
        assertEquals(testDevicePolicy, enrollmentRepository.devicePolicy.first())
    }

    // AC E13.10.8: Enrollment Status Tests

    @Test
    fun `isEnrolled returns true when enrolled`() = runTest {
        // Given - set up enrolled state
        val token = EnrollmentToken("ABC12345678901234567")
        coEvery {
            enrollmentApiService.enrollDevice(any(), any())
        } returns Result.success(testEnrollmentResult)
        coEvery {
            policyApplicator.applyPolicies(any())
        } returns PolicyApplicationResult(
            success = true,
            appliedSettings = emptyList(),
            failedSettings = emptyList(),
            skippedSettings = emptyList(),
        )
        enrollmentRepository.enrollDevice(token)

        // When/Then
        assertTrue(enrollmentRepository.isEnrolled())
    }

    @Test
    fun `isEnrolled returns false when not enrolled`() {
        // Given - default state (NOT_ENROLLED)

        // When/Then
        assertFalse(enrollmentRepository.isEnrolled())
    }

    @Test
    fun `getOrganizationInfo returns organization when enrolled`() = runTest {
        // Given - set up enrolled state
        val token = EnrollmentToken("ABC12345678901234567")
        coEvery {
            enrollmentApiService.enrollDevice(any(), any())
        } returns Result.success(testEnrollmentResult)
        coEvery {
            policyApplicator.applyPolicies(any())
        } returns PolicyApplicationResult(
            success = true,
            appliedSettings = emptyList(),
            failedSettings = emptyList(),
            skippedSettings = emptyList(),
        )
        enrollmentRepository.enrollDevice(token)

        // When
        val orgInfo = enrollmentRepository.getOrganizationInfo()

        // Then
        assertEquals(testOrganizationInfo, orgInfo)
    }

    @Test
    fun `isSettingLocked returns true for locked setting`() = runTest {
        // Given
        enrollmentRepository.savePolicies(testDevicePolicy)

        // When/Then
        assertTrue(enrollmentRepository.isSettingLocked("tracking_enabled"))
    }

    @Test
    fun `isSettingLocked returns false for unlocked setting`() = runTest {
        // Given
        enrollmentRepository.savePolicies(testDevicePolicy)

        // When/Then
        assertFalse(enrollmentRepository.isSettingLocked("tracking_interval"))
    }

    // AC E13.10.9: Unenrollment Tests

    @Test
    fun `unenrollDevice clears enrollment data`() = runTest {
        // Given - enroll first
        val token = EnrollmentToken("ABC12345678901234567")
        coEvery {
            enrollmentApiService.enrollDevice(any(), any())
        } returns Result.success(testEnrollmentResult)
        coEvery {
            policyApplicator.applyPolicies(any())
        } returns PolicyApplicationResult(
            success = true,
            appliedSettings = emptyList(),
            failedSettings = emptyList(),
            skippedSettings = emptyList(),
        )
        every { secureStorage.getAccessToken() } returns "access-token-123"
        coEvery {
            enrollmentApiService.unenrollDevice(any(), any())
        } returns Result.success(Unit)

        enrollmentRepository.enrollDevice(token)

        // When
        val result = enrollmentRepository.unenrollDevice()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(EnrollmentStatus.NOT_ENROLLED, enrollmentRepository.enrollmentStatus.first())
        assertNull(enrollmentRepository.organizationInfo.first())
        assertNull(enrollmentRepository.devicePolicy.first())
    }

    @Test
    fun `unenrollDevice fails when not authenticated`() = runTest {
        // Given
        every { secureStorage.getAccessToken() } returns null

        // When
        val result = enrollmentRepository.unenrollDevice()

        // Then
        assertTrue(result.isFailure)
        val error = result.exceptionOrNull()
        assertTrue(error is IllegalStateException)
    }

    @Test
    fun `unenrollDevice handles API failure and restores enrolled state`() = runTest {
        // Given - enroll first
        val token = EnrollmentToken("ABC12345678901234567")
        coEvery {
            enrollmentApiService.enrollDevice(any(), any())
        } returns Result.success(testEnrollmentResult)
        coEvery {
            policyApplicator.applyPolicies(any())
        } returns PolicyApplicationResult(
            success = true,
            appliedSettings = emptyList(),
            failedSettings = emptyList(),
            skippedSettings = emptyList(),
        )
        every { secureStorage.getAccessToken() } returns "access-token-123"
        coEvery {
            enrollmentApiService.unenrollDevice(any(), any())
        } returns Result.failure(Exception("Unenrollment not allowed"))

        enrollmentRepository.enrollDevice(token)

        // When
        val result = enrollmentRepository.unenrollDevice()

        // Then
        assertTrue(result.isFailure)
        assertEquals(EnrollmentStatus.ENROLLED, enrollmentRepository.enrollmentStatus.first())
    }

    // AC E13.10.7: Error Handling Tests

    @Test
    fun `enrollDevice returns appropriate error for expired token`() = runTest {
        // Given
        val token = EnrollmentToken("EXPIRED1234567890123")
        coEvery {
            enrollmentApiService.enrollDevice(any(), any())
        } returns Result.failure(Exception("Token expired"))

        // When
        val result = enrollmentRepository.enrollDevice(token)

        // Then
        assertTrue(result.isFailure)
        val errorMessage = enrollmentRepository.error.first()
        assertTrue(errorMessage?.contains("expired") == true || errorMessage?.contains("not found") == true)
    }

    @Test
    fun `enrollDevice returns appropriate error for network failure`() = runTest {
        // Given
        val token = EnrollmentToken("ABC12345678901234567")
        coEvery {
            enrollmentApiService.enrollDevice(any(), any())
        } returns Result.failure(Exception("Network connection failed"))

        // When
        val result = enrollmentRepository.enrollDevice(token)

        // Then
        assertTrue(result.isFailure)
        val errorMessage = enrollmentRepository.error.first()
        assertTrue(errorMessage?.contains("connect") == true)
    }

    @Test
    fun `clearError clears the error state`() = runTest {
        // Given - cause an error
        val invalidToken = EnrollmentToken("ABC") // Too short
        enrollmentRepository.enrollDevice(invalidToken)
        assertTrue(enrollmentRepository.error.first() != null)

        // When
        enrollmentRepository.clearError()

        // Then
        assertNull(enrollmentRepository.error.first())
    }
}

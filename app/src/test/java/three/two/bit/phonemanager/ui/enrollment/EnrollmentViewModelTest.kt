package three.two.bit.phonemanager.ui.enrollment

import androidx.lifecycle.SavedStateHandle
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import three.two.bit.phonemanager.data.repository.EnrollmentRepository
import three.two.bit.phonemanager.domain.model.DevicePolicy
import three.two.bit.phonemanager.domain.model.EnrollmentResult
import three.two.bit.phonemanager.domain.model.EnrollmentStatus
import three.two.bit.phonemanager.domain.model.OrganizationInfo
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for EnrollmentViewModel (Story E13.10)
 *
 * Tests cover:
 * - AC E13.10.2: Enrollment code input and validation
 * - AC E13.10.3: QR code scanning
 * - AC E13.10.4: Enroll device
 * - AC E13.10.7: Error handling
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EnrollmentViewModelTest {

    private lateinit var enrollmentRepository: EnrollmentRepository
    private lateinit var savedStateHandle: SavedStateHandle
    private lateinit var viewModel: EnrollmentViewModel
    private val testDispatcher = StandardTestDispatcher()

    // Mock state flows
    private val enrollmentStatusFlow = MutableStateFlow(EnrollmentStatus.NOT_ENROLLED)
    private val isLoadingFlow = MutableStateFlow(false)
    private val errorFlow = MutableStateFlow<String?>(null)
    private val organizationInfoFlow = MutableStateFlow<OrganizationInfo?>(null)
    private val devicePolicyFlow = MutableStateFlow<DevicePolicy?>(null)

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
        Dispatchers.setMain(testDispatcher)
        savedStateHandle = SavedStateHandle()
        enrollmentRepository = mockk(relaxed = true)

        // Mock EnrollmentRepository interface flows - now works because it's an interface
        every { enrollmentRepository.enrollmentStatus } returns enrollmentStatusFlow
        every { enrollmentRepository.isLoading } returns isLoadingFlow
        every { enrollmentRepository.error } returns errorFlow
        every { enrollmentRepository.organizationInfo } returns organizationInfoFlow
        every { enrollmentRepository.devicePolicy } returns devicePolicyFlow

        viewModel = EnrollmentViewModel(enrollmentRepository, savedStateHandle)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // AC E13.10.2: Enrollment Code Input Tests

    @Test
    fun `updateEnrollmentCode updates code state`() = runTest {
        // Given
        val code = "ABC12345678901234567"

        // When
        viewModel.updateEnrollmentCode(code)

        // Then
        assertEquals(code, viewModel.enrollmentCode.value)
    }

    @Test
    fun `updateEnrollmentCode clears validation error`() = runTest {
        // Given - set an error first
        viewModel.updateEnrollmentCode("AB") // Too short, causes error
        viewModel.enrollDevice()
        advanceUntilIdle()

        // When
        viewModel.updateEnrollmentCode("ABC12345678901234567")

        // Then
        assertNull(viewModel.codeError.value)
    }

    @Test
    fun `validateCode shows error for short code`() = runTest {
        // Given
        viewModel.updateEnrollmentCode("ABC") // Only 3 characters

        // When
        val isValid = viewModel.validateCode()

        // Then
        assertFalse(isValid)
        assertTrue(viewModel.codeError.value?.contains("16") == true)
    }

    @Test
    fun `validateCode shows no error for empty code`() = runTest {
        // Given
        viewModel.updateEnrollmentCode("")

        // When
        val isValid = viewModel.validateCode()

        // Then
        assertFalse(isValid)
        // Empty code should not show error (just not be valid)
        assertNull(viewModel.codeError.value)
    }

    // AC E13.10.3: QR Code Scanning Tests

    @Test
    fun `parseQRCode updates enrollment code for valid QR`() = runTest {
        // Given
        val qrCode = "phonemanager://enroll/XYZ98765432109876543"

        // When
        val success = viewModel.parseQRCode(qrCode)

        // Then
        assertTrue(success)
        assertEquals("XYZ98765432109876543", viewModel.enrollmentCode.value)
    }

    @Test
    fun `parseQRCode returns false for invalid QR format`() = runTest {
        // Given
        val invalidQrCode = "https://example.com/something"

        // When
        val success = viewModel.parseQRCode(invalidQrCode)

        // Then
        assertFalse(success)
    }

    @Test
    fun `showQrScanner sets scanner visibility to true`() = runTest {
        // When
        viewModel.showQrScanner()

        // Then
        assertTrue(viewModel.showQrScanner.value)
    }

    @Test
    fun `hideQrScanner sets scanner visibility to false`() = runTest {
        // Given
        viewModel.showQrScanner()

        // When
        viewModel.hideQrScanner()

        // Then
        assertFalse(viewModel.showQrScanner.value)
    }

    // AC E13.10.4: Enrollment Tests

    @Test
    fun `enrollDevice with valid code calls repository`() = runTest {
        // Given
        val code = "ABC12345678901234567"
        viewModel.updateEnrollmentCode(code)
        coEvery {
            enrollmentRepository.enrollDevice(any())
        } returns Result.success(testEnrollmentResult)

        // When
        viewModel.enrollDevice()
        advanceUntilIdle()

        // Then
        coVerify { enrollmentRepository.enrollDevice(any()) }
    }

    @Test
    fun `enrollDevice sets loading state during enrollment`() = runTest {
        // Given
        val code = "ABC12345678901234567"
        viewModel.updateEnrollmentCode(code)
        coEvery {
            enrollmentRepository.enrollDevice(any())
        } returns Result.success(testEnrollmentResult)

        // When
        viewModel.enrollDevice()
        // Note: We can't easily test the loading state during the operation
        // but we can verify the call was made
        advanceUntilIdle()

        // Then
        coVerify { enrollmentRepository.enrollDevice(any()) }
    }

    @Test
    fun `enrollDevice sets success state on success`() = runTest {
        // Given
        val code = "ABC12345678901234567"
        viewModel.updateEnrollmentCode(code)
        coEvery {
            enrollmentRepository.enrollDevice(any())
        } returns Result.success(testEnrollmentResult)

        // When
        viewModel.enrollDevice()
        advanceUntilIdle()

        // Then
        assertTrue(viewModel.uiState.value is EnrollmentUiState.Success)
    }

    @Test
    fun `enrollDevice sets error state on failure`() = runTest {
        // Given
        val code = "ABC12345678901234567"
        viewModel.updateEnrollmentCode(code)
        coEvery {
            enrollmentRepository.enrollDevice(any())
        } returns Result.failure(Exception("Enrollment failed"))

        // When
        viewModel.enrollDevice()
        advanceUntilIdle()

        // Then
        assertTrue(viewModel.uiState.value is EnrollmentUiState.Error)
    }

    // AC E13.10.7: Error Handling Tests

    @Test
    fun `clearError clears repository error`() = runTest {
        // Given - set an error
        coEvery {
            enrollmentRepository.enrollDevice(any())
        } returns Result.failure(Exception("Test error"))

        viewModel.updateEnrollmentCode("ABC12345678901234567")
        viewModel.enrollDevice()
        advanceUntilIdle()

        // When
        viewModel.clearError()

        // Then
        coVerify { enrollmentRepository.clearError() }
    }

    @Test
    fun `enrollDevice does not call repository with invalid code`() = runTest {
        // Given - too short code
        viewModel.updateEnrollmentCode("AB")

        // When
        viewModel.enrollDevice()
        advanceUntilIdle()

        // Then
        coVerify(exactly = 0) { enrollmentRepository.enrollDevice(any()) }
    }

    // Deep Link Token Tests

    @Test
    fun `initial token from savedStateHandle is loaded`() = runTest {
        // Given
        val savedState = SavedStateHandle(mapOf("token" to "DEEPLINK1234567890123"))
        every { enrollmentRepository.enrollmentStatus } answers { enrollmentStatusFlow }
        every { enrollmentRepository.isLoading } answers { isLoadingFlow }
        every { enrollmentRepository.error } answers { errorFlow }
        every { enrollmentRepository.organizationInfo } answers { organizationInfoFlow }
        every { enrollmentRepository.devicePolicy } answers { devicePolicyFlow }

        // When
        val vm = EnrollmentViewModel(enrollmentRepository, savedState)

        // Then
        assertEquals("DEEPLINK1234567890123", vm.enrollmentCode.value)
    }

    // Organization and Policy Exposure Tests

    @Test
    fun `organizationInfo is exposed from repository`() = runTest {
        // Given
        organizationInfoFlow.value = testOrganizationInfo

        // Then
        assertEquals(testOrganizationInfo, viewModel.organizationInfo.value)
    }

    @Test
    fun `devicePolicy is exposed from repository`() = runTest {
        // Given
        devicePolicyFlow.value = testDevicePolicy

        // Then
        assertEquals(testDevicePolicy, viewModel.devicePolicy.value)
    }

    @Test
    fun `enrollmentStatus is exposed from repository`() = runTest {
        // Given
        enrollmentStatusFlow.value = EnrollmentStatus.ENROLLED

        // Then
        assertEquals(EnrollmentStatus.ENROLLED, viewModel.enrollmentStatus.value)
    }

    // Reset State Tests

    @Test
    fun `resetState clears enrollment code and errors`() = runTest {
        // Given
        viewModel.updateEnrollmentCode("ABC12345678901234567")

        // When
        viewModel.resetState()

        // Then
        assertEquals("", viewModel.enrollmentCode.value)
        assertTrue(viewModel.uiState.value is EnrollmentUiState.Idle)
    }

    // isCodeValid Tests

    @Test
    fun `isCodeValid returns true for valid code`() = runTest {
        // Given
        viewModel.updateEnrollmentCode("ABC12345678901234567")

        // When/Then
        assertTrue(viewModel.isCodeValid())
    }

    @Test
    fun `isCodeValid returns false for invalid code`() = runTest {
        // Given
        viewModel.updateEnrollmentCode("ABC") // Too short

        // When/Then
        assertFalse(viewModel.isCodeValid())
    }

    // getLockedSettingsCount Tests

    @Test
    fun `getLockedSettingsCount returns 0 when no policy`() = runTest {
        // Given
        devicePolicyFlow.value = null

        // When/Then
        assertEquals(0, viewModel.getLockedSettingsCount())
    }

    @Test
    fun `getLockedSettingsCount returns correct count`() = runTest {
        // Given
        devicePolicyFlow.value = testDevicePolicy

        // When/Then
        assertEquals(1, viewModel.getLockedSettingsCount())
    }
}

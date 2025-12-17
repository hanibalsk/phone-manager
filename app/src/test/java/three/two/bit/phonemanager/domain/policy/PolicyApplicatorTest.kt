package three.two.bit.phonemanager.domain.policy

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import three.two.bit.phonemanager.data.preferences.PreferencesRepository
import three.two.bit.phonemanager.domain.model.DevicePolicy
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for PolicyApplicator (Story E13.10)
 *
 * Tests cover:
 * - AC E13.10.5: Apply device policies to local preferences
 * - Policy key validation
 * - Error handling for invalid policy values
 */
class PolicyApplicatorTest {

    private lateinit var preferencesRepository: PreferencesRepository
    private lateinit var policyApplicator: PolicyApplicator

    @Before
    fun setup() {
        preferencesRepository = mockk(relaxed = true)
        policyApplicator = PolicyApplicator(preferencesRepository)
    }

    // AC E13.10.5: Apply Policies Tests

    @Test
    fun `applyPolicies applies tracking_enabled setting`() = runTest {
        // Given
        val policy = DevicePolicy(
            settings = mapOf("tracking_enabled" to true),
            locks = emptyList(),
            groupId = null,
        )

        // When
        val result = policyApplicator.applyPolicies(policy)

        // Then
        assertTrue(result.success)
        assertEquals(1, result.appliedSettings.size)
        assertEquals("tracking_enabled", result.appliedSettings[0])
        coVerify { preferencesRepository.setTrackingEnabled(true) }
    }

    @Test
    fun `applyPolicies applies tracking_interval setting`() = runTest {
        // Given
        val policy = DevicePolicy(
            settings = mapOf("tracking_interval" to 30),
            locks = emptyList(),
            groupId = null,
        )

        // When
        val result = policyApplicator.applyPolicies(policy)

        // Then
        assertTrue(result.success)
        coVerify { preferencesRepository.setTrackingInterval(30) }
    }

    @Test
    fun `applyPolicies applies secret_mode_enabled setting`() = runTest {
        // Given
        val policy = DevicePolicy(
            settings = mapOf("secret_mode_enabled" to true),
            locks = emptyList(),
            groupId = null,
        )

        // When
        val result = policyApplicator.applyPolicies(policy)

        // Then
        assertTrue(result.success)
        coVerify { preferencesRepository.setSecretModeEnabled(true) }
    }

    @Test
    fun `applyPolicies applies movement_detection_enabled setting`() = runTest {
        // Given
        val policy = DevicePolicy(
            settings = mapOf("movement_detection_enabled" to true),
            locks = emptyList(),
            groupId = null,
        )

        // When
        val result = policyApplicator.applyPolicies(policy)

        // Then
        assertTrue(result.success)
        coVerify { preferencesRepository.setMovementDetectionEnabled(true) }
    }

    @Test
    fun `applyPolicies applies trip_detection_enabled setting`() = runTest {
        // Given
        val policy = DevicePolicy(
            settings = mapOf("trip_detection_enabled" to true),
            locks = emptyList(),
            groupId = null,
        )

        // When
        val result = policyApplicator.applyPolicies(policy)

        // Then
        assertTrue(result.success)
        coVerify { preferencesRepository.setTripDetectionEnabled(true) }
    }

    @Test
    fun `applyPolicies applies show_weather_in_notification setting`() = runTest {
        // Given
        val policy = DevicePolicy(
            settings = mapOf("show_weather_in_notification" to false),
            locks = emptyList(),
            groupId = null,
        )

        // When
        val result = policyApplicator.applyPolicies(policy)

        // Then
        assertTrue(result.success)
        coVerify { preferencesRepository.setShowWeatherInNotification(false) }
    }

    @Test
    fun `applyPolicies applies map_polling_interval setting`() = runTest {
        // Given
        val policy = DevicePolicy(
            settings = mapOf("map_polling_interval" to 15),
            locks = emptyList(),
            groupId = null,
        )

        // When
        val result = policyApplicator.applyPolicies(policy)

        // Then
        assertTrue(result.success)
        coVerify { preferencesRepository.setMapPollingIntervalSeconds(15) }
    }

    @Test
    fun `applyPolicies applies multiple settings`() = runTest {
        // Given
        val policy = DevicePolicy(
            settings = mapOf(
                "tracking_enabled" to true,
                "tracking_interval" to 60,
                "secret_mode_enabled" to false,
                "movement_detection_enabled" to true,
            ),
            locks = listOf("tracking_enabled", "tracking_interval"),
            groupId = null,
        )

        // When
        val result = policyApplicator.applyPolicies(policy)

        // Then
        assertTrue(result.success)
        assertEquals(4, result.appliedSettings.size)
        coVerify { preferencesRepository.setTrackingEnabled(true) }
        coVerify { preferencesRepository.setTrackingInterval(60) }
        coVerify { preferencesRepository.setSecretModeEnabled(false) }
        coVerify { preferencesRepository.setMovementDetectionEnabled(true) }
    }

    @Test
    fun `applyPolicies skips unknown settings`() = runTest {
        // Given
        val policy = DevicePolicy(
            settings = mapOf(
                "tracking_enabled" to true,
                "unknown_setting" to "value",
            ),
            locks = emptyList(),
            groupId = null,
        )

        // When
        val result = policyApplicator.applyPolicies(policy)

        // Then
        assertTrue(result.success)
        assertEquals(1, result.appliedSettings.size)
        assertEquals(1, result.skippedSettings.size)
    }

    @Test
    fun `applyPolicies handles empty policy`() = runTest {
        // Given
        val policy = DevicePolicy(
            settings = emptyMap(),
            locks = emptyList(),
            groupId = null,
        )

        // When
        val result = policyApplicator.applyPolicies(policy)

        // Then
        assertTrue(result.success)
        assertTrue(result.appliedSettings.isEmpty())
    }

    @Test
    fun `applyPolicies handles exception and reports failure`() = runTest {
        // Given
        val policy = DevicePolicy(
            settings = mapOf("tracking_enabled" to true),
            locks = emptyList(),
            groupId = null,
        )
        coEvery { preferencesRepository.setTrackingEnabled(any()) } throws RuntimeException("Storage error")

        // When
        val result = policyApplicator.applyPolicies(policy)

        // Then
        assertFalse(result.success)
        assertEquals(1, result.failedSettings.size)
        assertEquals("tracking_enabled", result.failedSettings[0].settingKey)
        assertTrue(result.failedSettings[0].error.contains("Storage error"))
    }

    // Policy Validation Tests - using ALL_POLICY_KEYS companion object

    @Test
    fun `ALL_POLICY_KEYS contains known keys`() {
        assertTrue(PolicyApplicator.ALL_POLICY_KEYS.contains("tracking_enabled"))
        assertTrue(PolicyApplicator.ALL_POLICY_KEYS.contains("tracking_interval_minutes"))
        assertTrue(PolicyApplicator.ALL_POLICY_KEYS.contains("secret_mode_enabled"))
        assertTrue(PolicyApplicator.ALL_POLICY_KEYS.contains("movement_detection_enabled"))
        assertTrue(PolicyApplicator.ALL_POLICY_KEYS.contains("trip_detection_enabled"))
        assertTrue(PolicyApplicator.ALL_POLICY_KEYS.contains("show_weather_in_notification"))
        assertTrue(PolicyApplicator.ALL_POLICY_KEYS.contains("map_polling_interval"))
    }

    @Test
    fun `ALL_POLICY_KEYS does not contain unknown keys`() {
        assertFalse(PolicyApplicator.ALL_POLICY_KEYS.contains("unknown_key"))
        assertFalse(PolicyApplicator.ALL_POLICY_KEYS.contains(""))
        assertFalse(PolicyApplicator.ALL_POLICY_KEYS.contains("TRACKING_ENABLED")) // Case sensitive
    }

    @Test
    fun `getSettingDisplayName returns human readable names`() {
        assertEquals("Location Tracking", policyApplicator.getSettingDisplayName("tracking_enabled"))
        assertEquals("Tracking Interval", policyApplicator.getSettingDisplayName("tracking_interval"))
        assertEquals("Secret Mode", policyApplicator.getSettingDisplayName("secret_mode_enabled"))
    }

    @Test
    fun `getSettingDisplayName returns formatted key for unknown settings`() {
        val unknownKey = "some_unknown_key"
        val result = policyApplicator.getSettingDisplayName(unknownKey)
        // Should format as "Some Unknown Key"
        assertTrue(result.contains("Some") || result.contains("Unknown") || result.contains("Key"))
    }

    @Test
    fun `getPolicyValue returns value from policy`() {
        // Given
        val policy = DevicePolicy(
            settings = mapOf("tracking_enabled" to true),
            locks = emptyList(),
            groupId = null,
        )

        // When
        val value = policyApplicator.getPolicyValue(policy, "tracking_enabled")

        // Then
        assertEquals(true, value)
    }

    @Test
    fun `getPolicyValue returns null for missing key`() {
        // Given
        val policy = DevicePolicy(
            settings = mapOf("tracking_enabled" to true),
            locks = emptyList(),
            groupId = null,
        )

        // When
        val value = policyApplicator.getPolicyValue(policy, "missing_key")

        // Then
        assertEquals(null, value)
    }

    @Test
    fun `getPolicyValue returns null for null policy`() {
        // When
        val value = policyApplicator.getPolicyValue(null, "tracking_enabled")

        // Then
        assertEquals(null, value)
    }

    // Activity Recognition Policy Tests

    @Test
    fun `applyPolicies applies activity_recognition_enabled setting`() = runTest {
        // Given
        val policy = DevicePolicy(
            settings = mapOf("activity_recognition_enabled" to true),
            locks = emptyList(),
            groupId = null,
        )

        // When
        val result = policyApplicator.applyPolicies(policy)

        // Then
        assertTrue(result.success)
        assertEquals(1, result.appliedSettings.size)
        coVerify { preferencesRepository.setActivityRecognitionEnabled(true) }
    }

    // isSettingLocked Tests

    @Test
    fun `isSettingLocked returns true for locked setting`() {
        // Given
        val policy = DevicePolicy(
            settings = mapOf("tracking_enabled" to true),
            locks = listOf("tracking_enabled"),
            groupId = null,
        )

        // When/Then
        assertTrue(policyApplicator.isSettingLocked(policy, "tracking_enabled"))
    }

    @Test
    fun `isSettingLocked returns false for unlocked setting`() {
        // Given
        val policy = DevicePolicy(
            settings = mapOf("tracking_enabled" to true),
            locks = emptyList(),
            groupId = null,
        )

        // When/Then
        assertFalse(policyApplicator.isSettingLocked(policy, "tracking_enabled"))
    }

    @Test
    fun `isSettingLocked returns false for null policy`() {
        // When/Then
        assertFalse(policyApplicator.isSettingLocked(null, "tracking_enabled"))
    }
}

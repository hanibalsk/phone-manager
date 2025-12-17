@file:Suppress("DEPRECATION")

package three.two.bit.phonemanager.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for SecureStorage JWT token management (Story E9.11, Task 1)
 *
 * Tests cover:
 * - Access token storage and retrieval
 * - Refresh token storage and retrieval
 * - Token expiry time management
 * - Token expiration detection with 5-minute buffer
 * - Token clearing
 * - Authentication status validation
 */
class SecureStorageTest {

    private lateinit var context: Context
    private lateinit var encryptedPrefs: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var secureStorage: SecureStorage

    @Before
    fun setup() {
        // Mock Android context and EncryptedSharedPreferences
        context = mockk(relaxed = true)
        encryptedPrefs = mockk(relaxed = true)
        editor = mockk(relaxed = true)

        // Mock EncryptedSharedPreferences.create() to return our mock
        mockkStatic("androidx.security.crypto.EncryptedSharedPreferences")
        every {
            EncryptedSharedPreferences.create(
                any<Context>(),
                any<String>(),
                any(),
                any(),
                any(),
            )
        } returns encryptedPrefs

        // Mock editor behavior
        every { encryptedPrefs.edit() } returns editor
        every { editor.putString(any<String>(), any<String>()) } returns editor
        every { editor.putLong(any<String>(), any<Long>()) } returns editor
        every { editor.remove(any<String>()) } returns editor
        every { editor.apply() } just Runs

        // Create SecureStorage instance
        secureStorage = SecureStorage(context)
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    // Access Token Tests

    @Test
    fun `saveAccessToken stores token successfully`() {
        // Given
        val testToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test.token"

        // When
        secureStorage.saveAccessToken(testToken)

        // Then
        verify {
            editor.putString("access_token", testToken)
            editor.apply()
        }
    }

    @Test
    fun `getAccessToken retrieves stored token`() {
        // Given
        val testToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test.token"
        every { encryptedPrefs.getString("access_token", null) } returns testToken

        // When
        val result = secureStorage.getAccessToken()

        // Then
        assertEquals(testToken, result)
        verify { encryptedPrefs.getString("access_token", null) }
    }

    @Test
    fun `getAccessToken returns null when no token stored`() {
        // Given
        every { encryptedPrefs.getString("access_token", null) } returns null

        // When
        val result = secureStorage.getAccessToken()

        // Then
        assertNull(result)
    }

    // Refresh Token Tests

    @Test
    fun `saveRefreshToken stores token successfully`() {
        // Given
        val testToken = "refresh.token.value"

        // When
        secureStorage.saveRefreshToken(testToken)

        // Then
        verify {
            editor.putString("refresh_token", testToken)
            editor.apply()
        }
    }

    @Test
    fun `getRefreshToken retrieves stored token`() {
        // Given
        val testToken = "refresh.token.value"
        every { encryptedPrefs.getString("refresh_token", null) } returns testToken

        // When
        val result = secureStorage.getRefreshToken()

        // Then
        assertEquals(testToken, result)
        verify { encryptedPrefs.getString("refresh_token", null) }
    }

    @Test
    fun `getRefreshToken returns null when no token stored`() {
        // Given
        every { encryptedPrefs.getString("refresh_token", null) } returns null

        // When
        val result = secureStorage.getRefreshToken()

        // Then
        assertNull(result)
    }

    // Token Expiry Tests

    @Test
    fun `saveTokenExpiryTime stores expiry time successfully`() {
        // Given
        val expiryTime = System.currentTimeMillis() + 3600000L // 1 hour from now

        // When
        secureStorage.saveTokenExpiryTime(expiryTime)

        // Then
        verify {
            editor.putLong("token_expiry_time", expiryTime)
            editor.apply()
        }
    }

    @Test
    fun `getTokenExpiryTime retrieves stored expiry time`() {
        // Given
        val expiryTime = System.currentTimeMillis() + 3600000L
        every { encryptedPrefs.getLong("token_expiry_time", -1L) } returns expiryTime

        // When
        val result = secureStorage.getTokenExpiryTime()

        // Then
        assertEquals(expiryTime, result)
        verify { encryptedPrefs.getLong("token_expiry_time", -1L) }
    }

    @Test
    fun `getTokenExpiryTime returns null when no expiry stored`() {
        // Given
        every { encryptedPrefs.getLong("token_expiry_time", -1L) } returns -1L

        // When
        val result = secureStorage.getTokenExpiryTime()

        // Then
        assertNull(result)
    }

    // Token Expiration Detection Tests

    @Test
    fun `isTokenExpired returns true when no expiry time stored`() {
        // Given
        every { encryptedPrefs.getLong("token_expiry_time", -1L) } returns -1L

        // When
        val result = secureStorage.isTokenExpired()

        // Then
        assertTrue(result)
    }

    @Test
    fun `isTokenExpired returns true when token already expired`() {
        // Given - token expired 1 hour ago
        val expiredTime = System.currentTimeMillis() - 3600000L
        every { encryptedPrefs.getLong("token_expiry_time", -1L) } returns expiredTime

        // When
        val result = secureStorage.isTokenExpired()

        // Then
        assertTrue(result)
    }

    @Test
    fun `isTokenExpired returns true when within 5-minute buffer`() {
        // Given - token expires in 4 minutes (within 5-minute buffer)
        val expiryTime = System.currentTimeMillis() + (4 * 60 * 1000L)
        every { encryptedPrefs.getLong("token_expiry_time", -1L) } returns expiryTime

        // When
        val result = secureStorage.isTokenExpired()

        // Then
        assertTrue(result, "Token should be considered expired within 5-minute buffer")
    }

    @Test
    fun `isTokenExpired returns false when token valid beyond buffer`() {
        // Given - token expires in 10 minutes (beyond 5-minute buffer)
        val expiryTime = System.currentTimeMillis() + (10 * 60 * 1000L)
        every { encryptedPrefs.getLong("token_expiry_time", -1L) } returns expiryTime

        // When
        val result = secureStorage.isTokenExpired()

        // Then
        assertFalse(result, "Token should be valid when beyond 5-minute buffer")
    }

    // Clear Tokens Tests

    @Test
    fun `clearTokens removes all token data`() {
        // When
        secureStorage.clearTokens()

        // Then
        verify {
            editor.remove("access_token")
            editor.remove("refresh_token")
            editor.remove("token_expiry_time")
            editor.apply()
        }
    }

    // Authentication Status Tests

    @Test
    fun `isAuthenticated returns true when tokens present and not expired`() {
        // Given
        every { encryptedPrefs.getString("access_token", null) } returns "access.token"
        every { encryptedPrefs.getString("refresh_token", null) } returns "refresh.token"
        every { encryptedPrefs.getLong("token_expiry_time", -1L) } returns
            System.currentTimeMillis() + (10 * 60 * 1000L) // 10 minutes from now

        // When
        val result = secureStorage.isAuthenticated()

        // Then
        assertTrue(result)
    }

    @Test
    fun `isAuthenticated returns true when access token missing but refresh token exists`() {
        // Given - no access token but refresh token exists (can be refreshed)
        every { encryptedPrefs.getString("access_token", null) } returns null
        every { encryptedPrefs.getString("refresh_token", null) } returns "refresh.token"

        // When
        val result = secureStorage.isAuthenticated()

        // Then - should be true because refresh token can be used to get new access token
        assertTrue(result, "Should be authenticated when refresh token exists")
    }

    @Test
    fun `isAuthenticated returns true when refresh token missing but access token valid`() {
        // Given - no refresh token but access token is valid (not expired)
        every { encryptedPrefs.getString("access_token", null) } returns "access.token"
        every { encryptedPrefs.getString("refresh_token", null) } returns null
        every { encryptedPrefs.getLong("token_expiry_time", -1L) } returns
            System.currentTimeMillis() + (10 * 60 * 1000L)

        // When
        val result = secureStorage.isAuthenticated()

        // Then - valid access token means authenticated
        assertTrue(result, "Should be authenticated with valid access token even without refresh token")
    }

    @Test
    fun `isAuthenticated returns true when access token expired but refresh token exists`() {
        // Given - access token expired but refresh token exists
        every { encryptedPrefs.getString("access_token", null) } returns "access.token"
        every { encryptedPrefs.getString("refresh_token", null) } returns "refresh.token"
        every { encryptedPrefs.getLong("token_expiry_time", -1L) } returns
            System.currentTimeMillis() - 1000L // Expired 1 second ago

        // When
        val result = secureStorage.isAuthenticated()

        // Then - should be true because refresh token can be used to get new access token
        assertTrue(result, "Should be authenticated when refresh token exists even if access token expired")
    }

    @Test
    fun `isAuthenticated returns true when within expiry buffer but refresh token exists`() {
        // Given - access token expires in 3 minutes (within 5-minute buffer) but refresh token exists
        every { encryptedPrefs.getString("access_token", null) } returns "access.token"
        every { encryptedPrefs.getString("refresh_token", null) } returns "refresh.token"
        every { encryptedPrefs.getLong("token_expiry_time", -1L) } returns
            System.currentTimeMillis() + (3 * 60 * 1000L)

        // When
        val result = secureStorage.isAuthenticated()

        // Then - should be true because refresh token exists
        assertTrue(result, "Should be authenticated when refresh token exists even within expiry buffer")
    }

    @Test
    fun `isAuthenticated returns false when no refresh token and access token expired`() {
        // Given - no refresh token and access token expired
        every { encryptedPrefs.getString("access_token", null) } returns "access.token"
        every { encryptedPrefs.getString("refresh_token", null) } returns null
        every { encryptedPrefs.getLong("token_expiry_time", -1L) } returns
            System.currentTimeMillis() - 1000L // Expired 1 second ago

        // When
        val result = secureStorage.isAuthenticated()

        // Then - should be false because no refresh token and access token expired
        assertFalse(result, "Should not be authenticated without refresh token when access token expired")
    }

    // Integration Tests

    @Test
    fun `complete JWT flow - save, retrieve, and validate tokens`() {
        // Given
        val accessToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.access"
        val refreshToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.refresh"
        val expiryTime = System.currentTimeMillis() + (60 * 60 * 1000L) // 1 hour

        every { encryptedPrefs.getString("access_token", null) } returns accessToken
        every { encryptedPrefs.getString("refresh_token", null) } returns refreshToken
        every { encryptedPrefs.getLong("token_expiry_time", -1L) } returns expiryTime

        // When - Save tokens
        secureStorage.saveAccessToken(accessToken)
        secureStorage.saveRefreshToken(refreshToken)
        secureStorage.saveTokenExpiryTime(expiryTime)

        // Then - Verify storage
        verify {
            editor.putString("access_token", accessToken)
            editor.putString("refresh_token", refreshToken)
            editor.putLong("token_expiry_time", expiryTime)
        }

        // And - Verify retrieval
        assertEquals(accessToken, secureStorage.getAccessToken())
        assertEquals(refreshToken, secureStorage.getRefreshToken())
        assertEquals(expiryTime, secureStorage.getTokenExpiryTime())

        // And - Verify authentication status
        assertTrue(secureStorage.isAuthenticated())
        assertFalse(secureStorage.isTokenExpired())
    }

    @Test
    fun `complete logout flow - clear all tokens`() {
        // Given - tokens stored initially
        every { encryptedPrefs.getString("access_token", null) } returns "access.token" andThen null
        every { encryptedPrefs.getString("refresh_token", null) } returns "refresh.token" andThen null
        every { encryptedPrefs.getLong("token_expiry_time", -1L) } returns
            System.currentTimeMillis() + 3600000L andThen -1L

        // Verify initially authenticated
        assertTrue(secureStorage.isAuthenticated())

        // When - Clear tokens
        secureStorage.clearTokens()

        // Then - Verify all tokens removed
        verify {
            editor.remove("access_token")
            editor.remove("refresh_token")
            editor.remove("token_expiry_time")
            editor.apply()
        }

        // And - Verify not authenticated after clearing
        assertFalse(secureStorage.isAuthenticated())
    }
}

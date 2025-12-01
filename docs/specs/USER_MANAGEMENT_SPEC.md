# User Management Specification

## Phone Manager Android - User Authentication & Profile

**Version:** 1.0.0
**Status:** Design Specification
**Last Updated:** 2025-12-01

---

## 1. Overview

This document defines the Android implementation for user authentication, profile management, and session handling including login, registration, OAuth, and token management.

---

## 2. Authentication Flows

### 2.1 Email/Password Registration

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                      REGISTRATION FLOW                                       │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌─────────────────┐                                                        │
│  │  Register Screen │                                                        │
│  │  - Email input   │                                                        │
│  │  - Password      │                                                        │
│  │  - Confirm pass  │                                                        │
│  │  - Display name  │                                                        │
│  │  [Create Account]│                                                        │
│  └────────┬────────┘                                                        │
│           │                                                                  │
│           ▼                                                                  │
│  ┌─────────────────┐     ┌─────────────────┐                               │
│  │  AuthRepository │────►│  POST /auth/    │                               │
│  │  .register()    │     │  register       │                               │
│  └────────┬────────┘     └─────────────────┘                               │
│           │                                                                  │
│           ▼                                                                  │
│  ┌─────────────────┐                                                        │
│  │SecureTokenStorage                                                        │
│  │ .saveTokens()   │                                                        │
│  │ .saveUserId()   │                                                        │
│  └────────┬────────┘                                                        │
│           │                                                                  │
│           ▼                                                                  │
│  ┌─────────────────┐                                                        │
│  │  Link Device?   │────► DeviceRepository.linkDevice()                     │
│  └────────┬────────┘                                                        │
│           │                                                                  │
│           ▼                                                                  │
│  ┌─────────────────┐                                                        │
│  │  Home Screen    │                                                        │
│  └─────────────────┘                                                        │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

### 2.2 Email/Password Login

```kotlin
// AuthRepository.kt
suspend fun login(
    email: String,
    password: String,
    deviceId: String,
    deviceName: String
): Result<LoginResponse> {
    return try {
        val response = authApi.login(
            LoginRequest(
                email = email,
                password = password,
                deviceId = deviceId,
                deviceName = deviceName
            )
        )

        // Store tokens securely
        tokenStorage.saveTokens(
            accessToken = response.tokens.accessToken,
            refreshToken = response.tokens.refreshToken,
            expiresIn = response.tokens.expiresIn
        )
        tokenStorage.saveUserId(response.user.id)

        Result.success(response)
    } catch (e: HttpException) {
        when (e.code()) {
            401 -> Result.failure(AuthException.InvalidCredentials)
            403 -> Result.failure(AuthException.AccountDisabled)
            423 -> Result.failure(AuthException.AccountLocked)
            else -> Result.failure(e)
        }
    }
}
```

### 2.3 OAuth (Google/Apple)

```kotlin
// GoogleSignInHandler.kt
class GoogleSignInHandler @Inject constructor(
    private val authRepository: AuthRepository,
    private val context: Context
) {
    private val googleSignInClient: GoogleSignInClient by lazy {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(BuildConfig.GOOGLE_CLIENT_ID)
            .requestEmail()
            .build()
        GoogleSignIn.getClient(context, gso)
    }

    fun getSignInIntent(): Intent = googleSignInClient.signInIntent

    suspend fun handleSignInResult(data: Intent?): Result<User> {
        return try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)
            val idToken = account.idToken ?: throw AuthException.MissingIdToken

            authRepository.loginWithGoogle(idToken)
        } catch (e: ApiException) {
            Result.failure(AuthException.GoogleSignInFailed(e.statusCode))
        }
    }
}
```

### 2.4 Token Refresh

```kotlin
// AuthInterceptor.kt
class AuthInterceptor @Inject constructor(
    private val tokenStorage: SecureTokenStorage,
    private val authApi: AuthApi
) : Interceptor {

    private val tokenMutex = Mutex()

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        // Skip auth for public endpoints
        if (isPublicEndpoint(request.url.encodedPath)) {
            return chain.proceed(request)
        }

        // Get valid access token
        val token = runBlocking {
            tokenMutex.withLock {
                ensureValidAccessToken()
            }
        } ?: return chain.proceed(request)

        // Add Authorization header
        val authenticatedRequest = request.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()

        return chain.proceed(authenticatedRequest)
    }

    private suspend fun ensureValidAccessToken(): String? {
        // Check if access token is valid
        if (!tokenStorage.isAccessTokenExpired()) {
            return tokenStorage.getAccessToken()
        }

        // Try to refresh
        val refreshToken = tokenStorage.getRefreshToken() ?: return null

        return try {
            val response = authApi.refreshToken(
                RefreshTokenRequest(refreshToken = refreshToken)
            )

            tokenStorage.saveTokens(
                accessToken = response.accessToken,
                refreshToken = response.refreshToken,
                expiresIn = response.expiresIn
            )

            response.accessToken
        } catch (e: Exception) {
            // Refresh failed - clear tokens
            tokenStorage.clearTokens()
            null
        }
    }

    private fun isPublicEndpoint(path: String): Boolean {
        return path.contains("/auth/login") ||
               path.contains("/auth/register") ||
               path.contains("/auth/oauth") ||
               path.contains("/auth/forgot-password")
    }
}
```

---

## 3. Secure Token Storage

### 3.1 SecureTokenStorage Implementation

```kotlin
// SecureTokenStorage.kt
@Singleton
class SecureTokenStorage @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREFS_FILE_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private const val PREFS_FILE_NAME = "secure_auth_tokens"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_TOKEN_EXPIRY = "token_expiry"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_DEVICE_TOKEN = "device_token"  // B2B
    }

    fun saveTokens(accessToken: String, refreshToken: String, expiresIn: Long) {
        val expiryTime = System.currentTimeMillis() + (expiresIn * 1000)
        sharedPreferences.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .putLong(KEY_TOKEN_EXPIRY, expiryTime)
            .apply()
    }

    fun getAccessToken(): String? =
        sharedPreferences.getString(KEY_ACCESS_TOKEN, null)

    fun getRefreshToken(): String? =
        sharedPreferences.getString(KEY_REFRESH_TOKEN, null)

    fun isAccessTokenExpired(): Boolean {
        val expiry = sharedPreferences.getLong(KEY_TOKEN_EXPIRY, 0)
        // Consider expired if within 60 seconds of expiry
        return System.currentTimeMillis() >= expiry - 60_000
    }

    fun saveUserId(userId: String) {
        sharedPreferences.edit()
            .putString(KEY_USER_ID, userId)
            .apply()
    }

    fun getUserId(): String? =
        sharedPreferences.getString(KEY_USER_ID, null)

    fun isLoggedIn(): Boolean =
        getAccessToken() != null && getUserId() != null

    fun clearTokens() {
        sharedPreferences.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_TOKEN_EXPIRY)
            .remove(KEY_USER_ID)
            .apply()
    }

    // B2B: Device token for managed devices
    fun saveDeviceToken(token: String) {
        sharedPreferences.edit()
            .putString(KEY_DEVICE_TOKEN, token)
            .apply()
    }

    fun getDeviceToken(): String? =
        sharedPreferences.getString(KEY_DEVICE_TOKEN, null)
}
```

---

## 4. Session Management

### 4.1 SessionManager

```kotlin
// SessionManager.kt
@Singleton
class SessionManager @Inject constructor(
    private val tokenStorage: SecureTokenStorage,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) {
    private val _sessionState = MutableStateFlow<SessionState>(SessionState.Loading)
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    init {
        checkSession()
    }

    private fun checkSession() {
        viewModelScope.launch {
            if (tokenStorage.isLoggedIn()) {
                try {
                    val user = userRepository.getCurrentUser()
                    _currentUser.value = user
                    _sessionState.value = SessionState.Authenticated(user)
                } catch (e: Exception) {
                    // Token invalid, clear session
                    logout()
                }
            } else {
                _sessionState.value = SessionState.Unauthenticated
            }
        }
    }

    suspend fun login(email: String, password: String): Result<User> {
        val result = authRepository.login(email, password)
        result.onSuccess { response ->
            _currentUser.value = response.user
            _sessionState.value = SessionState.Authenticated(response.user)
        }
        return result.map { it.user }
    }

    suspend fun loginWithGoogle(idToken: String): Result<User> {
        val result = authRepository.loginWithGoogle(idToken)
        result.onSuccess { response ->
            _currentUser.value = response.user
            _sessionState.value = SessionState.Authenticated(response.user)
        }
        return result.map { it.user }
    }

    suspend fun logout() {
        authRepository.logout()
        tokenStorage.clearTokens()
        _currentUser.value = null
        _sessionState.value = SessionState.Unauthenticated
    }

    fun isLoggedIn(): Boolean = tokenStorage.isLoggedIn()
}

sealed class SessionState {
    object Loading : SessionState()
    object Unauthenticated : SessionState()
    data class Authenticated(val user: User) : SessionState()
}
```

---

## 5. Profile Management

### 5.1 UserRepository

```kotlin
// UserRepository.kt
interface UserRepository {
    suspend fun getCurrentUser(): User
    suspend fun updateProfile(displayName: String?, avatarUrl: String?): User
    suspend fun changePassword(currentPassword: String, newPassword: String)
    suspend fun deleteAccount()
}

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val userApi: UserApi,
    private val tokenStorage: SecureTokenStorage
) : UserRepository {

    override suspend fun getCurrentUser(): User {
        return userApi.getCurrentUser()
    }

    override suspend fun updateProfile(
        displayName: String?,
        avatarUrl: String?
    ): User {
        return userApi.updateProfile(
            UpdateProfileRequest(
                displayName = displayName,
                avatarUrl = avatarUrl
            )
        )
    }

    override suspend fun changePassword(
        currentPassword: String,
        newPassword: String
    ) {
        userApi.changePassword(
            ChangePasswordRequest(
                currentPassword = currentPassword,
                newPassword = newPassword
            )
        )
    }

    override suspend fun deleteAccount() {
        userApi.deleteAccount()
        tokenStorage.clearTokens()
    }
}
```

---

## 6. ViewModels

### 6.1 LoginViewModel

```kotlin
// LoginViewModel.kt
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email, emailError = null) }
    }

    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(password = password, passwordError = null) }
    }

    fun login() {
        val state = _uiState.value

        // Validate
        if (!isValidEmail(state.email)) {
            _uiState.update { it.copy(emailError = "Invalid email") }
            return
        }
        if (state.password.isEmpty()) {
            _uiState.update { it.copy(passwordError = "Password required") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            sessionManager.login(state.email, state.password)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, loginSuccess = true) }
                }
                .onFailure { error ->
                    val message = when (error) {
                        is AuthException.InvalidCredentials -> "Invalid email or password"
                        is AuthException.AccountDisabled -> "Account is disabled"
                        is AuthException.AccountLocked -> "Account locked. Try again later"
                        else -> "Login failed. Please try again"
                    }
                    _uiState.update { it.copy(isLoading = false, error = message) }
                }
        }
    }

    fun loginWithGoogle(idToken: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            sessionManager.loginWithGoogle(idToken)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, loginSuccess = true) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoading = false, error = "Google sign-in failed")
                    }
                }
        }
    }
}

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val loginSuccess: Boolean = false
)
```

### 6.2 ProfileViewModel

```kotlin
// ProfileViewModel.kt
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                val user = userRepository.getCurrentUser()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        user = user,
                        displayName = user.displayName
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = "Failed to load profile")
                }
            }
        }
    }

    fun updateProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }

            try {
                val user = userRepository.updateProfile(
                    displayName = _uiState.value.displayName
                )
                _uiState.update {
                    it.copy(isSaving = false, user = user, saveSuccess = true)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isSaving = false, error = "Failed to save")
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            sessionManager.logout()
        }
    }
}

data class ProfileUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val user: User? = null,
    val displayName: String = "",
    val error: String? = null,
    val saveSuccess: Boolean = false
)
```

---

## 7. API Services

### 7.1 AuthApi

```kotlin
// AuthApi.kt
interface AuthApi {
    @POST("api/v1/auth/register")
    suspend fun register(@Body request: RegisterRequest): RegisterResponse

    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @POST("api/v1/auth/logout")
    suspend fun logout()

    @POST("api/v1/auth/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): RefreshTokenResponse

    @POST("api/v1/auth/oauth/google")
    suspend fun loginWithGoogle(@Body request: GoogleOAuthRequest): LoginResponse

    @POST("api/v1/auth/oauth/apple")
    suspend fun loginWithApple(@Body request: AppleOAuthRequest): LoginResponse

    @POST("api/v1/auth/forgot-password")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequest)

    @POST("api/v1/auth/reset-password")
    suspend fun resetPassword(@Body request: ResetPasswordRequest)
}
```

### 7.2 UserApi

```kotlin
// UserApi.kt
interface UserApi {
    @GET("api/v1/auth/me")
    suspend fun getCurrentUser(): User

    @PUT("api/v1/auth/me")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): User

    @PUT("api/v1/auth/me/password")
    suspend fun changePassword(@Body request: ChangePasswordRequest)

    @DELETE("api/v1/auth/me")
    suspend fun deleteAccount()
}
```

---

## 8. Backward Compatibility

### 8.1 Dual Authentication Support

```kotlin
// AuthInterceptor.kt - Extended
class AuthInterceptor @Inject constructor(
    private val tokenStorage: SecureTokenStorage,
    private val secureStorage: SecureStorage,  // Legacy storage
    private val authApi: AuthApi
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        if (isPublicEndpoint(request.url.encodedPath)) {
            return chain.proceed(request)
        }

        // Priority 1: User auth (JWT)
        if (tokenStorage.isLoggedIn()) {
            val token = runBlocking { ensureValidAccessToken() }
            if (token != null) {
                return chain.proceed(
                    request.newBuilder()
                        .header("Authorization", "Bearer $token")
                        .build()
                )
            }
        }

        // Priority 2: Device token (B2B managed)
        val deviceToken = tokenStorage.getDeviceToken()
        if (deviceToken != null) {
            return chain.proceed(
                request.newBuilder()
                    .header("Authorization", "Bearer $deviceToken")
                    .build()
            )
        }

        // Priority 3: Legacy API key (backward compatibility)
        val apiKey = secureStorage.getApiKey()
        if (apiKey != null) {
            return chain.proceed(
                request.newBuilder()
                    .header("X-API-Key", apiKey)
                    .build()
            )
        }

        // No auth available
        return chain.proceed(request)
    }
}
```

---

## 9. Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0.0 | 2025-12-01 | Initial specification |

package three.two.bit.phonemanager.ui.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import three.two.bit.phonemanager.data.repository.AuthRepository
import three.two.bit.phonemanager.data.repository.ConfigRepository
import three.two.bit.phonemanager.data.repository.SettingsSyncRepository
import three.two.bit.phonemanager.network.DeviceApiService
import three.two.bit.phonemanager.security.SecureStorage
import three.two.bit.phonemanager.worker.SettingsSyncWorker
import timber.log.Timber
import javax.inject.Inject

/**
 * Story E9.11, Task 7: Authentication ViewModel
 *
 * Handles authentication operations for Login and Register screens.
 *
 * AC E9.11.3: Email/password login
 * AC E9.11.4: User registration
 * AC E9.11.5: OAuth sign-in
 * AC E9.11.6: User logout
 * AC E9.11.7: Input validation and error handling
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authRepository: AuthRepository,
    private val configRepository: ConfigRepository,
    private val deviceApiService: DeviceApiService,
    private val secureStorage: SecureStorage,
    private val settingsSyncRepository: SettingsSyncRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    // Feature flag states for UI visibility
    // Default to permissive values (true) until config explicitly disables features
    // This ensures OAuth buttons remain visible during loading/failures
    val isGoogleSignInEnabled: StateFlow<Boolean> = configRepository.config
        .map { it?.auth?.googleEnabled ?: true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isAppleSignInEnabled: StateFlow<Boolean> = configRepository.config
        .map { it?.auth?.appleEnabled ?: true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isRegistrationEnabled: StateFlow<Boolean> = configRepository.config
        .map { it?.auth?.registrationEnabled ?: true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isOAuthOnly: StateFlow<Boolean> = configRepository.config
        .map { it?.auth?.oauthOnly ?: false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isConfigLoaded: StateFlow<Boolean> = configRepository.config
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Config loading state for UI feedback
    private val _isConfigLoading = MutableStateFlow(true)
    val isConfigLoading: StateFlow<Boolean> = _isConfigLoading.asStateFlow()

    init {
        // Fetch public config on initialization with retry
        fetchPublicConfigWithRetry()
    }

    /**
     * Fetch public configuration from server with retry logic
     */
    private fun fetchPublicConfigWithRetry(
        maxRetries: Int = 3,
        initialDelayMs: Long = 1000L
    ) {
        viewModelScope.launch {
            _isConfigLoading.value = true
            var currentDelay = initialDelayMs
            var attempt = 0

            while (attempt < maxRetries) {
                val result = configRepository.fetchConfig()
                if (result.isSuccess) {
                    _isConfigLoading.value = false
                    return@launch
                }

                attempt++
                if (attempt < maxRetries) {
                    Timber.w("Config fetch attempt $attempt failed, retrying in ${currentDelay}ms")
                    kotlinx.coroutines.delay(currentDelay)
                    currentDelay *= 2 // Exponential backoff
                }
            }

            Timber.e("Failed to fetch public config after $maxRetries attempts")
            _isConfigLoading.value = false
        }
    }

    /**
     * Manual refresh of public configuration (for retry after failure)
     */
    fun refreshConfig() {
        fetchPublicConfigWithRetry()
    }

    // Form validation state
    private val _emailError = MutableStateFlow<String?>(null)
    val emailError: StateFlow<String?> = _emailError.asStateFlow()

    private val _passwordError = MutableStateFlow<String?>(null)
    val passwordError: StateFlow<String?> = _passwordError.asStateFlow()

    private val _displayNameError = MutableStateFlow<String?>(null)
    val displayNameError: StateFlow<String?> = _displayNameError.asStateFlow()

    // Story E10.6: Device linking state (AC E10.6.6)
    private val _deviceLinkState = MutableStateFlow<DeviceLinkState>(DeviceLinkState.Idle)
    val deviceLinkState: StateFlow<DeviceLinkState> = _deviceLinkState.asStateFlow()

    /**
     * AC E9.11.3: Login with email and password
     *
     * @param email User email address
     * @param password User password
     */
    fun login(email: String, password: String) {
        // Clear previous errors
        clearErrors()

        // Validate input (AC E9.11.7)
        if (!validateLoginInput(email, password)) {
            return
        }

        _uiState.value = AuthUiState.Loading

        viewModelScope.launch {
            val result = authRepository.login(email, password)

            if (result.isSuccess) {
                Timber.i("Login successful")
                val user = result.getOrThrow()
                _uiState.value = AuthUiState.Success(user)

                // AC E10.6.6: Auto-link device after successful login
                autoLinkCurrentDevice(user.userId)

                // Sync settings from server to apply admin/group-managed settings
                syncSettingsAfterAuth()
            } else {
                val exception = result.exceptionOrNull()
                Timber.e(exception, "Login failed")
                _uiState.value = AuthUiState.Error(
                    message = getErrorMessage(exception),
                    errorCode = "login_failed"
                )
            }
        }
    }

    /**
     * AC E9.11.4: Register new user account
     *
     * @param email User email address
     * @param password User password
     * @param displayName User display name
     */
    fun register(email: String, password: String, displayName: String) {
        // Clear previous errors
        clearErrors()

        // Validate input (AC E9.11.7)
        if (!validateRegisterInput(email, password, displayName)) {
            return
        }

        _uiState.value = AuthUiState.Loading

        viewModelScope.launch {
            val result = authRepository.register(email, password, displayName)

            if (result.isSuccess) {
                Timber.i("Registration successful")
                val user = result.getOrThrow()
                _uiState.value = AuthUiState.Success(user)

                // AC E10.6.6: Auto-link device after successful registration
                autoLinkCurrentDevice(user.userId)

                // Sync settings from server to apply admin/group-managed settings
                syncSettingsAfterAuth()
            } else {
                val exception = result.exceptionOrNull()
                Timber.e(exception, "Registration failed")
                _uiState.value = AuthUiState.Error(
                    message = getErrorMessage(exception),
                    errorCode = "registration_failed"
                )
            }
        }
    }

    /**
     * AC E9.11.5: OAuth Sign-In
     *
     * @param provider OAuth provider (google, apple)
     * @param idToken ID token from OAuth provider
     */
    fun oauthSignIn(provider: String, idToken: String) {
        _uiState.value = AuthUiState.Loading

        viewModelScope.launch {
            val result = authRepository.oauthLogin(provider, idToken)

            if (result.isSuccess) {
                Timber.i("OAuth sign-in successful: $provider")
                val user = result.getOrThrow()
                _uiState.value = AuthUiState.Success(user)

                // AC E10.6.6: Auto-link device after successful OAuth
                autoLinkCurrentDevice(user.userId)

                // Sync settings from server to apply admin/group-managed settings
                syncSettingsAfterAuth()
            } else {
                val exception = result.exceptionOrNull()
                Timber.e(exception, "OAuth sign-in failed: $provider")
                _uiState.value = AuthUiState.Error(
                    message = getErrorMessage(exception),
                    errorCode = "oauth_failed"
                )
            }
        }
    }

    /**
     * AC E9.11.6: Logout current user
     */
    fun logout() {
        // Cancel periodic settings sync worker
        SettingsSyncWorker.cancel(context)

        viewModelScope.launch {
            authRepository.logout()
            _uiState.value = AuthUiState.Idle
            Timber.i("User logged out")
        }
    }

    /**
     * Reset UI state to idle
     */
    fun resetState() {
        _uiState.value = AuthUiState.Idle
        clearErrors()
    }

    /**
     * AC E9.11.7: Validate login input
     *
     * @return true if input is valid
     */
    private fun validateLoginInput(email: String, password: String): Boolean {
        var isValid = true

        if (!isValidEmail(email)) {
            _emailError.value = "Please enter a valid email address"
            isValid = false
        }

        if (password.isBlank()) {
            _passwordError.value = "Password is required"
            isValid = false
        }

        return isValid
    }

    /**
     * AC E9.11.7: Validate registration input
     *
     * @return true if input is valid
     */
    private fun validateRegisterInput(
        email: String,
        password: String,
        displayName: String
    ): Boolean {
        var isValid = true

        if (!isValidEmail(email)) {
            _emailError.value = "Please enter a valid email address"
            isValid = false
        }

        if (displayName.isBlank() || displayName.length < 2) {
            _displayNameError.value = "Display name must be at least 2 characters"
            isValid = false
        }

        if (!isValidPassword(password)) {
            _passwordError.value = "Password must be at least 8 characters with 1 uppercase, 1 number, and 1 special character"
            isValid = false
        }

        return isValid
    }

    /**
     * Validate email format
     *
     * @param email Email address to validate
     * @return true if email format is valid
     */
    private fun isValidEmail(email: String): Boolean {
        return email.matches(Regex("[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"))
    }

    /**
     * Validate password strength
     *
     * Requirements: Minimum 8 characters, 1 uppercase, 1 number, 1 special char
     *
     * @param password Password to validate
     * @return true if password meets requirements
     */
    private fun isValidPassword(password: String): Boolean {
        if (password.length < 8) return false
        if (!password.any { it.isUpperCase() }) return false
        if (!password.any { it.isDigit() }) return false
        if (!password.any { !it.isLetterOrDigit() }) return false
        return true
    }

    /**
     * Clear all validation errors
     */
    private fun clearErrors() {
        _emailError.value = null
        _passwordError.value = null
        _displayNameError.value = null
    }

    /**
     * Convert exception to user-friendly error message
     *
     * @param exception Exception from repository
     * @return User-friendly error message
     */
    private fun getErrorMessage(exception: Throwable?): String {
        return when (exception?.message) {
            "email_already_exists" -> "An account with this email already exists"
            "invalid_credentials" -> "Invalid email or password"
            "weak_password" -> "Password does not meet security requirements"
            "account_locked" -> "Your account has been locked. Please contact support."
            "account_disabled" -> "Your account has been disabled. Please contact support."
            "network_error" -> "Unable to connect to server. Please check your internet connection."
            "oauth_failed" -> "Sign-in with this provider failed. Please try again."
            else -> exception?.message ?: "An error occurred. Please try again."
        }
    }

    /**
     * Story E10.6 Task 8: Auto-link device after successful authentication
     *
     * AC E10.6.6: Registration flow integration
     * - Auto-link current device if not linked
     * - Show link prompt if auto-link fails
     * - Allow user to skip linking
     *
     * @param userId The authenticated user's ID
     */
    private fun autoLinkCurrentDevice(userId: String) {
        val accessToken = secureStorage.getAccessToken() ?: return
        val deviceId = secureStorage.getDeviceId()

        _deviceLinkState.value = DeviceLinkState.Linking

        viewModelScope.launch {
            val result = deviceApiService.linkDevice(
                userId = userId,
                deviceId = deviceId,
                displayName = null, // Will use device's current name
                isPrimary = false,
                accessToken = accessToken,
            )

            _deviceLinkState.value = result.fold(
                onSuccess = {
                    Timber.i("Device auto-linked successfully: $deviceId")
                    DeviceLinkState.Linked
                },
                onFailure = { exception ->
                    val message = exception.message ?: ""
                    when {
                        message.contains("409") || message.contains("conflict", ignoreCase = true) -> {
                            // Device already linked to this or another user
                            Timber.i("Device already linked: $deviceId")
                            DeviceLinkState.AlreadyLinked
                        }
                        else -> {
                            Timber.e(exception, "Failed to auto-link device")
                            DeviceLinkState.Failed(
                                message = "Could not link device. You can try again from Settings."
                            )
                        }
                    }
                },
            )
        }
    }

    /**
     * Sync settings from server after successful authentication.
     * This ensures the device adopts admin/group-managed settings.
     * Also schedules periodic settings sync worker.
     */
    private fun syncSettingsAfterAuth() {
        // Schedule periodic settings sync worker
        SettingsSyncWorker.schedule(context)

        // Perform immediate sync
        viewModelScope.launch {
            settingsSyncRepository.syncAllSettings()
                .onSuccess {
                    Timber.i("Settings synced after authentication")
                }
                .onFailure { error ->
                    Timber.w(error, "Failed to sync settings after authentication")
                }
        }
    }

    /**
     * Dismiss the device link prompt
     */
    fun dismissDeviceLinkPrompt() {
        _deviceLinkState.value = DeviceLinkState.Skipped
    }

    /**
     * Clear device link state
     */
    fun clearDeviceLinkState() {
        _deviceLinkState.value = DeviceLinkState.Idle
    }
}

/**
 * Story E10.6: Device linking state for post-authentication flow
 */
sealed interface DeviceLinkState {
    /** No linking operation in progress */
    data object Idle : DeviceLinkState

    /** Currently attempting to link device */
    data object Linking : DeviceLinkState

    /** Device successfully linked */
    data object Linked : DeviceLinkState

    /** Device was already linked (to this user or another) */
    data object AlreadyLinked : DeviceLinkState

    /** User chose to skip linking */
    data object Skipped : DeviceLinkState

    /** Failed to link device */
    data class Failed(val message: String) : DeviceLinkState
}

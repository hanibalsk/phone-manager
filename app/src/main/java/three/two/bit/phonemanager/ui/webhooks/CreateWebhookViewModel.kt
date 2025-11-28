package three.two.bit.phonemanager.ui.webhooks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import three.two.bit.phonemanager.data.repository.WebhookRepository
import timber.log.Timber
import java.net.URI
import java.util.UUID
import javax.inject.Inject

/**
 * Story E6.3: CreateWebhookViewModel - State management for webhook creation
 *
 * AC E6.3.1: Create webhook with auto-generated secret
 * AC E6.3.6: URL validation (HTTPS required)
 */
@HiltViewModel
class CreateWebhookViewModel @Inject constructor(
    private val webhookRepository: WebhookRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateWebhookUiState())
    val uiState: StateFlow<CreateWebhookUiState> = _uiState.asStateFlow()

    init {
        // Auto-generate secret on init
        generateSecret()
    }

    fun updateName(name: String) {
        _uiState.update {
            it.copy(
                name = name,
                nameError = validateName(name),
            )
        }
    }

    fun updateTargetUrl(url: String) {
        _uiState.update {
            it.copy(
                targetUrl = url,
                urlError = validateUrl(url),
            )
        }
    }

    fun generateSecret() {
        val secret = UUID.randomUUID().toString().replace("-", "")
        _uiState.update { it.copy(secret = secret) }
    }

    fun createWebhook() {
        val state = _uiState.value

        // Validate all fields
        val nameError = validateName(state.name)
        val urlError = validateUrl(state.targetUrl)

        if (nameError != null || urlError != null) {
            _uiState.update {
                it.copy(
                    nameError = nameError,
                    urlError = urlError,
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isCreating = true) }

            webhookRepository.createWebhook(
                name = state.name.trim(),
                targetUrl = state.targetUrl.trim(),
                secret = state.secret,
            ).fold(
                onSuccess = {
                    Timber.i("Webhook created successfully")
                    _uiState.update { it.copy(isCreating = false, isCreated = true) }
                },
                onFailure = { error ->
                    Timber.e(error, "Failed to create webhook")
                    _uiState.update {
                        it.copy(
                            isCreating = false,
                            error = error.message ?: "Failed to create webhook",
                        )
                    }
                },
            )
        }
    }

    private fun validateName(name: String): String? {
        return when {
            name.isBlank() -> "Name is required"
            name.length < 2 -> "Name must be at least 2 characters"
            name.length > 50 -> "Name must be at most 50 characters"
            else -> null
        }
    }

    /**
     * Validate URL format and enforce HTTPS (AC E6.3.6)
     */
    private fun validateUrl(url: String): String? {
        if (url.isBlank()) {
            return "URL is required"
        }

        return try {
            val uri = URI(url.trim())

            when {
                uri.scheme == null -> "Invalid URL format"
                uri.scheme.lowercase() != "https" -> "URL must use HTTPS"
                uri.host == null || uri.host.isBlank() -> "Invalid host in URL"
                else -> null
            }
        } catch (e: Exception) {
            "Invalid URL format"
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

/**
 * UI state for create webhook screen
 */
data class CreateWebhookUiState(
    val name: String = "",
    val targetUrl: String = "",
    val secret: String = "",
    val nameError: String? = null,
    val urlError: String? = null,
    val isCreating: Boolean = false,
    val isCreated: Boolean = false,
    val error: String? = null,
)

package three.two.bit.phonemanager.ui.webhooks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import three.two.bit.phonemanager.data.repository.WebhookRepository
import three.two.bit.phonemanager.domain.model.Webhook
import timber.log.Timber
import javax.inject.Inject

/**
 * Story E6.3: WebhooksViewModel - State management for webhooks screen
 *
 * AC E6.3.5: Webhook CRUD operations
 */
@HiltViewModel
class WebhooksViewModel @Inject constructor(
    private val webhookRepository: WebhookRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WebhooksUiState())
    val uiState: StateFlow<WebhooksUiState> = _uiState.asStateFlow()

    init {
        loadWebhooks()
        syncFromServer()
    }

    private fun loadWebhooks() {
        viewModelScope.launch {
            webhookRepository.observeWebhooks()
                .catch { error ->
                    Timber.e(error, "Error observing webhooks")
                    _uiState.update { it.copy(error = error.message) }
                }
                .collect { webhooks ->
                    _uiState.update { it.copy(webhooks = webhooks, isLoading = false) }
                }
        }
    }

    fun syncFromServer() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }

            webhookRepository.syncFromServer().fold(
                onSuccess = {
                    _uiState.update { it.copy(isRefreshing = false, error = null) }
                },
                onFailure = { error ->
                    Timber.w(error, "Failed to sync webhooks from server")
                    _uiState.update { it.copy(isRefreshing = false) }
                    // Don't show error - local data is still available
                },
            )
        }
    }

    fun toggleWebhook(webhookId: String) {
        viewModelScope.launch {
            webhookRepository.toggleWebhook(webhookId).fold(
                onSuccess = {
                    Timber.d("Webhook toggled: $webhookId")
                },
                onFailure = { error ->
                    Timber.e(error, "Failed to toggle webhook: $webhookId")
                    _uiState.update { it.copy(error = "Failed to toggle webhook") }
                },
            )
        }
    }

    fun deleteWebhook(webhookId: String) {
        viewModelScope.launch {
            webhookRepository.deleteWebhook(webhookId).fold(
                onSuccess = {
                    Timber.d("Webhook deleted: $webhookId")
                },
                onFailure = { error ->
                    Timber.e(error, "Failed to delete webhook: $webhookId")
                    _uiState.update { it.copy(error = "Failed to delete webhook") }
                },
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

/**
 * UI state for webhooks screen
 */
data class WebhooksUiState(
    val webhooks: List<Webhook> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
)

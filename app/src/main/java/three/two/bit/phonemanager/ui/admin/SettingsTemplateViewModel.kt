package three.two.bit.phonemanager.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import three.two.bit.phonemanager.data.repository.AdminSettingsRepository
import three.two.bit.phonemanager.domain.model.BulkSettingsResult
import three.two.bit.phonemanager.domain.model.SettingDefinition
import three.two.bit.phonemanager.domain.model.SettingsTemplate
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import kotlin.time.Clock

/**
 * Story E12.7: ViewModel for Settings Templates
 *
 * Manages settings templates for bulk application to devices.
 *
 * AC E12.7.7: Settings Templates
 * - List templates
 * - Create/edit templates
 * - Delete templates
 * - Apply templates to devices
 */
@HiltViewModel
class SettingsTemplateViewModel @Inject constructor(private val adminSettingsRepository: AdminSettingsRepository) :
    ViewModel() {

    private val _uiState = MutableStateFlow(SettingsTemplateUiState())
    val uiState: StateFlow<SettingsTemplateUiState> = _uiState.asStateFlow()

    init {
        loadTemplates()
    }

    /**
     * Load all templates.
     */
    fun loadTemplates() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            adminSettingsRepository.getTemplates().fold(
                onSuccess = { templates ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            templates = templates,
                        )
                    }
                    Timber.i("Loaded ${templates.size} templates")
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Failed to load templates",
                        )
                    }
                    Timber.e(error, "Failed to load templates")
                },
            )
        }
    }

    /**
     * Start creating a new template.
     */
    fun startCreateTemplate() {
        // Note: user info will be populated by server on save
        _uiState.update {
            it.copy(
                isEditing = true,
                editingTemplate = SettingsTemplate(
                    id = UUID.randomUUID().toString(),
                    name = "",
                    settings = emptyMap(),
                    lockedSettings = emptySet(),
                    createdBy = "",
                    createdByName = "Current User",
                    createdAt = Clock.System.now(),
                ),
            )
        }
    }

    /**
     * Start editing an existing template.
     */
    fun startEditTemplate(template: SettingsTemplate) {
        _uiState.update {
            it.copy(
                isEditing = true,
                editingTemplate = template,
            )
        }
    }

    /**
     * Update template name.
     */
    fun updateTemplateName(name: String) {
        _uiState.update { state ->
            state.editingTemplate?.let { template ->
                state.copy(editingTemplate = template.copy(name = name))
            } ?: state
        }
    }

    /**
     * Update template description.
     */
    fun updateTemplateDescription(description: String) {
        _uiState.update { state ->
            state.editingTemplate?.let { template ->
                state.copy(editingTemplate = template.copy(description = description.ifBlank { null }))
            } ?: state
        }
    }

    /**
     * Update a setting value in the template.
     */
    fun updateTemplateSetting(key: String, value: Any) {
        _uiState.update { state ->
            state.editingTemplate?.let { template ->
                state.copy(
                    editingTemplate = template.copy(
                        settings = template.settings + (key to value),
                    ),
                )
            } ?: state
        }
    }

    /**
     * Remove a setting from the template.
     */
    fun removeTemplateSetting(key: String) {
        _uiState.update { state ->
            state.editingTemplate?.let { template ->
                state.copy(
                    editingTemplate = template.copy(
                        settings = template.settings - key,
                        lockedSettings = template.lockedSettings - key,
                    ),
                )
            } ?: state
        }
    }

    /**
     * Toggle whether a setting should be locked when applying the template.
     */
    fun toggleSettingLock(key: String) {
        _uiState.update { state ->
            state.editingTemplate?.let { template ->
                val newLocked = if (template.lockedSettings.contains(key)) {
                    template.lockedSettings - key
                } else {
                    template.lockedSettings + key
                }
                state.copy(editingTemplate = template.copy(lockedSettings = newLocked))
            } ?: state
        }
    }

    /**
     * Toggle whether the template is shared.
     */
    fun toggleTemplateShared() {
        _uiState.update { state ->
            state.editingTemplate?.let { template ->
                state.copy(editingTemplate = template.copy(isShared = !template.isShared))
            } ?: state
        }
    }

    /**
     * Save the current template.
     */
    fun saveTemplate() {
        val template = _uiState.value.editingTemplate ?: return

        if (template.name.isBlank()) {
            _uiState.update { it.copy(error = "Template name is required") }
            return
        }

        if (template.settings.isEmpty()) {
            _uiState.update { it.copy(error = "Template must have at least one setting") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }

            adminSettingsRepository.saveTemplate(template).fold(
                onSuccess = { savedTemplate ->
                    _uiState.update { state ->
                        val templates = state.templates.toMutableList()
                        val existingIndex = templates.indexOfFirst { it.id == savedTemplate.id }
                        if (existingIndex >= 0) {
                            templates[existingIndex] = savedTemplate
                        } else {
                            templates.add(savedTemplate)
                        }
                        state.copy(
                            isSaving = false,
                            isEditing = false,
                            editingTemplate = null,
                            templates = templates,
                            successMessage = "Template saved successfully",
                        )
                    }
                    Timber.i("Saved template: ${savedTemplate.name}")
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            error = error.message ?: "Failed to save template",
                        )
                    }
                    Timber.e(error, "Failed to save template")
                },
            )
        }
    }

    /**
     * Cancel editing.
     */
    fun cancelEditing() {
        _uiState.update {
            it.copy(
                isEditing = false,
                editingTemplate = null,
            )
        }
    }

    /**
     * Show delete confirmation for a template.
     */
    fun showDeleteConfirmation(template: SettingsTemplate) {
        _uiState.update { it.copy(confirmDeleteTemplate = template) }
    }

    /**
     * Hide delete confirmation.
     */
    fun hideDeleteConfirmation() {
        _uiState.update { it.copy(confirmDeleteTemplate = null) }
    }

    /**
     * Delete a template.
     */
    fun deleteTemplate(templateId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true, error = null) }

            adminSettingsRepository.deleteTemplate(templateId).fold(
                onSuccess = {
                    _uiState.update { state ->
                        state.copy(
                            isDeleting = false,
                            confirmDeleteTemplate = null,
                            templates = state.templates.filter { it.id != templateId },
                            successMessage = "Template deleted",
                        )
                    }
                    Timber.i("Deleted template: $templateId")
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isDeleting = false,
                            error = error.message ?: "Failed to delete template",
                        )
                    }
                    Timber.e(error, "Failed to delete template")
                },
            )
        }
    }

    /**
     * Show apply template dialog.
     */
    fun showApplyTemplate(template: SettingsTemplate) {
        _uiState.update { it.copy(applyingTemplate = template) }
    }

    /**
     * Hide apply template dialog.
     */
    fun hideApplyTemplate() {
        _uiState.update {
            it.copy(
                applyingTemplate = null,
                applyResult = null,
            )
        }
    }

    /**
     * Apply template to devices.
     */
    fun applyTemplateToDevices(templateId: String, deviceIds: List<String>, notifyUsers: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isApplying = true, error = null) }

            adminSettingsRepository.applyTemplate(templateId, deviceIds, notifyUsers).fold(
                onSuccess = { result ->
                    _uiState.update {
                        it.copy(
                            isApplying = false,
                            applyResult = result,
                            successMessage = "Applied to ${result.successCount} devices" +
                                if (result.failureCount > 0) " (${result.failureCount} failed)" else "",
                        )
                    }
                    Timber.i(
                        "Applied template $templateId: ${result.successCount} success, " +
                            "${result.failureCount} failed",
                    )
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isApplying = false,
                            error = error.message ?: "Failed to apply template",
                        )
                    }
                    Timber.e(error, "Failed to apply template")
                },
            )
        }
    }

    /**
     * Clear error message.
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Clear success message.
     */
    fun clearSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }

    /**
     * Get available settings for template creation.
     */
    fun getAvailableSettings(): List<SettingDefinition> = SettingDefinition.ALL_SETTINGS
}

/**
 * UI state for Settings Templates screen.
 */
data class SettingsTemplateUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isDeleting: Boolean = false,
    val isApplying: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val templates: List<SettingsTemplate> = emptyList(),
    val isEditing: Boolean = false,
    val editingTemplate: SettingsTemplate? = null,
    val confirmDeleteTemplate: SettingsTemplate? = null,
    val applyingTemplate: SettingsTemplate? = null,
    val applyResult: BulkSettingsResult? = null,
) {
    val hasTemplates: Boolean get() = templates.isNotEmpty()
    val isConfirmingDelete: Boolean get() = confirmDeleteTemplate != null
    val isApplyDialogOpen: Boolean get() = applyingTemplate != null
}

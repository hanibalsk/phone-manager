@file:Suppress("DEPRECATION") // hiltViewModel() deprecation - using stable API

package three.two.bit.phonemanager.ui.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import three.two.bit.phonemanager.R
import three.two.bit.phonemanager.domain.model.SettingDefinition
import three.two.bit.phonemanager.domain.model.SettingType
import three.two.bit.phonemanager.domain.model.SettingValidation
import three.two.bit.phonemanager.domain.model.SettingsTemplate
import java.text.NumberFormat

/**
 * Story E12.7: Settings Template Screen
 *
 * Displays and manages settings templates.
 *
 * AC E12.7.7: Settings Templates
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTemplateScreen(
    onNavigateBack: () -> Unit,
    onApplyTemplate: (templateId: String) -> Unit,
    viewModel: SettingsTemplateViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSuccessMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.admin_settings_templates)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.startCreateTemplate() }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.admin_create_template))
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.templates.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outline,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.admin_no_templates_yet),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.admin_create_template_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(uiState.templates, key = { it.id }) { template ->
                    TemplateCard(
                        template = template,
                        onEdit = { viewModel.startEditTemplate(template) },
                        onDelete = { viewModel.showDeleteConfirmation(template) },
                        onApply = { onApplyTemplate(template.id) },
                    )
                }
            }
        }
    }

    // Template editor sheet
    if (uiState.isEditing) {
        TemplateEditorSheet(
            template = uiState.editingTemplate!!,
            isSaving = uiState.isSaving,
            availableSettings = viewModel.getAvailableSettings(),
            onNameChange = { viewModel.updateTemplateName(it) },
            onDescriptionChange = { viewModel.updateTemplateDescription(it) },
            onSettingChange = { key, value -> viewModel.updateTemplateSetting(key, value) },
            onRemoveSetting = { viewModel.removeTemplateSetting(it) },
            onToggleLock = { viewModel.toggleSettingLock(it) },
            onToggleShared = { viewModel.toggleTemplateShared() },
            onSave = { viewModel.saveTemplate() },
            onDismiss = { viewModel.cancelEditing() },
        )
    }

    // Delete confirmation
    if (uiState.isConfirmingDelete) {
        DeleteTemplateDialog(
            templateName = uiState.confirmDeleteTemplate?.name ?: "",
            isDeleting = uiState.isDeleting,
            onConfirm = { viewModel.deleteTemplate(uiState.confirmDeleteTemplate!!.id) },
            onDismiss = { viewModel.hideDeleteConfirmation() },
        )
    }
}

@Composable
private fun TemplateCard(
    template: SettingsTemplate,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onApply: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = template.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (template.isShared) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.People,
                                contentDescription = stringResource(R.string.admin_shared),
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    template.description?.let { desc ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Settings summary
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = pluralStringResource(
                        R.plurals.admin_template_settings_count,
                        template.settings.size,
                        template.settings.size,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (template.lockedSettings.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = pluralStringResource(
                            R.plurals.admin_template_locked_count,
                            template.lockedSettings.size,
                            template.lockedSettings.size,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(R.string.delete),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit))
                }
                IconButton(onClick = onApply) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = stringResource(R.string.apply),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TemplateEditorSheet(
    template: SettingsTemplate,
    isSaving: Boolean,
    availableSettings: List<SettingDefinition>,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onSettingChange: (String, Any) -> Unit,
    onRemoveSetting: (String) -> Unit,
    onToggleLock: (String) -> Unit,
    onToggleShared: () -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text(
                text = stringResource(
                    if (template.name.isEmpty()) R.string.admin_new_template else R.string.admin_edit_template,
                ),
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = template.name,
                onValueChange = onNameChange,
                label = { Text(stringResource(R.string.admin_template_name)) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving,
                singleLine = true,
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = template.description ?: "",
                onValueChange = onDescriptionChange,
                label = { Text(stringResource(R.string.admin_template_description)) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving,
                maxLines = 2,
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !isSaving) { onToggleShared() }
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(stringResource(R.string.admin_share_admins))
                    Text(
                        text = stringResource(R.string.admin_share_admins_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = template.isShared,
                    onCheckedChange = { onToggleShared() },
                    enabled = !isSaving,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.admin_settings_section_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Settings list
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(availableSettings, key = { it.key }) { definition ->
                    val isIncluded = template.settings.containsKey(definition.key)
                    val value = template.settings[definition.key] ?: definition.defaultValue
                    val isLocked = template.lockedSettings.contains(definition.key)

                    TemplateSettingItem(
                        definition = definition,
                        isIncluded = isIncluded,
                        value = value,
                        isLocked = isLocked,
                        isSaving = isSaving,
                        onToggleInclude = {
                            if (isIncluded) {
                                onRemoveSetting(definition.key)
                            } else {
                                onSettingChange(definition.key, definition.defaultValue)
                            }
                        },
                        onValueChange = { onSettingChange(definition.key, it) },
                        onToggleLock = { onToggleLock(definition.key) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onDismiss, enabled = !isSaving) {
                    Text(stringResource(R.string.cancel))
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(
                    onClick = onSave,
                    enabled = !isSaving && template.name.isNotBlank() && template.settings.isNotEmpty(),
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(stringResource(R.string.save))
                }
            }
        }
    }
}

@Composable
private fun TemplateSettingItem(
    definition: SettingDefinition,
    isIncluded: Boolean,
    value: Any,
    isLocked: Boolean,
    isSaving: Boolean,
    onToggleInclude: () -> Unit,
    onValueChange: (Any) -> Unit,
    onToggleLock: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isIncluded) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = isIncluded,
                    onCheckedChange = { onToggleInclude() },
                    enabled = !isSaving,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = definition.displayName,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = definition.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (isIncluded) {
                    IconButton(onClick = onToggleLock, enabled = !isSaving) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = if (isLocked) "Will lock" else "Won't lock",
                            tint = if (isLocked) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outline
                            },
                        )
                    }
                }
            }

            if (isIncluded) {
                Spacer(modifier = Modifier.height(8.dp))
                when (definition.type) {
                    SettingType.BOOLEAN -> {
                        Switch(
                            checked = value as? Boolean ?: false,
                            onCheckedChange = { onValueChange(it) },
                            enabled = !isSaving,
                        )
                    }
                    SettingType.INTEGER -> {
                        val validation = definition.validation as? SettingValidation.IntRange
                        val min = validation?.min ?: 0
                        val max = validation?.max ?: 100
                        val intValue = (value as? Number)?.toInt() ?: min

                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(min.toString(), style = MaterialTheme.typography.labelSmall)
                                Text(
                                    intValue.toString(),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(max.toString(), style = MaterialTheme.typography.labelSmall)
                            }
                            Slider(
                                value = intValue.toFloat(),
                                onValueChange = { onValueChange(it.toInt()) },
                                valueRange = min.toFloat()..max.toFloat(),
                                enabled = !isSaving,
                            )
                        }
                    }
                    SettingType.FLOAT -> {
                        val validation = definition.validation as? SettingValidation.FloatRange
                        val min = validation?.min ?: 0f
                        val max = validation?.max ?: 100f
                        val floatValue = (value as? Number)?.toFloat() ?: min
                        val numberFormat = remember {
                            NumberFormat.getNumberInstance().apply {
                                maximumFractionDigits = 1
                                minimumFractionDigits = 0
                            }
                        }

                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(numberFormat.format(min), style = MaterialTheme.typography.labelSmall)
                                Text(
                                    numberFormat.format(floatValue),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(numberFormat.format(max), style = MaterialTheme.typography.labelSmall)
                            }
                            Slider(
                                value = floatValue,
                                onValueChange = { onValueChange(it) },
                                valueRange = min..max,
                                enabled = !isSaving,
                            )
                        }
                    }
                    SettingType.STRING -> {
                        OutlinedTextField(
                            value = value.toString(),
                            onValueChange = { onValueChange(it) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isSaving,
                            singleLine = true,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DeleteTemplateDialog(
    templateName: String,
    isDeleting: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.admin_delete_template)) },
        text = {
            Text(stringResource(R.string.admin_delete_template_confirm, templateName))
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = !isDeleting,
            ) {
                if (isDeleting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isDeleting) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

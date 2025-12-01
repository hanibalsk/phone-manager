package three.two.bit.phonemanager.ui.admin

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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import three.two.bit.phonemanager.domain.model.BulkSettingsResult
import three.two.bit.phonemanager.domain.model.SettingCategory
import three.two.bit.phonemanager.domain.model.SettingDefinition
import three.two.bit.phonemanager.domain.model.SettingType
import three.two.bit.phonemanager.domain.model.SettingValidation

/**
 * Story E12.7: Bulk Settings Screen
 *
 * Allows admins to apply settings to multiple devices at once.
 *
 * AC E12.7.6: Bulk Settings Application
 * - Select settings to change
 * - Set values for each setting
 * - Choose whether to lock settings
 * - Apply to selected devices
 * - Show results summary
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BulkSettingsScreen(
    deviceIds: List<String>,
    onNavigateBack: () -> Unit,
    onComplete: () -> Unit,
    viewModel: BulkSettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(deviceIds) {
        viewModel.setSelectedDevices(deviceIds)
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(uiState.applyComplete) {
        if (uiState.applyComplete) {
            // Show results then allow navigation
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Bulk Settings Update")
                        Text(
                            text = "${deviceIds.size} devices selected",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                actions = {
                    IconToggleButton(
                        checked = uiState.notifyUsers,
                        onCheckedChange = { viewModel.setNotifyUsers(it) },
                    ) {
                        Icon(
                            imageVector = if (uiState.notifyUsers) {
                                Icons.Default.Notifications
                            } else {
                                Icons.Default.NotificationsOff
                            },
                            contentDescription = if (uiState.notifyUsers) {
                                "Notifications enabled"
                            } else {
                                "Notifications disabled"
                            },
                        )
                    }
                },
            )
        },
        bottomBar = {
            BulkSettingsBottomBar(
                selectedSettingsCount = uiState.selectedSettings.size,
                isApplying = uiState.isApplying,
                onApply = { viewModel.applySettings() },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        if (uiState.isApplying) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Applying settings to ${deviceIds.size} devices...")
                }
            }
        } else if (uiState.result != null) {
            BulkResultsView(
                result = uiState.result!!,
                onDone = onComplete,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Group settings by category
                SettingDefinition.byCategory().forEach { (category, definitions) ->
                    item(key = category.name) {
                        BulkSettingsCategoryCard(
                            category = category,
                            definitions = definitions,
                            selectedSettings = uiState.selectedSettings,
                            settingValues = uiState.settingValues,
                            lockedSettings = uiState.lockedSettings,
                            onToggleSetting = { key -> viewModel.toggleSetting(key) },
                            onValueChange = { key, value -> viewModel.setSettingValue(key, value) },
                            onToggleLock = { key -> viewModel.toggleLock(key) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BulkSettingsBottomBar(
    selectedSettingsCount: Int,
    isApplying: Boolean,
    onApply: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "$selectedSettingsCount settings selected",
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(
                onClick = onApply,
                enabled = selectedSettingsCount > 0 && !isApplying,
            ) {
                if (isApplying) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Apply Settings")
            }
        }
    }
}

@Composable
private fun BulkSettingsCategoryCard(
    category: SettingCategory,
    definitions: List<SettingDefinition>,
    selectedSettings: Set<String>,
    settingValues: Map<String, Any>,
    lockedSettings: Set<String>,
    onToggleSetting: (String) -> Unit,
    onValueChange: (String, Any) -> Unit,
    onToggleLock: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = category.displayName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(12.dp))

            definitions.forEach { definition ->
                val isSelected = selectedSettings.contains(definition.key)
                val value = settingValues[definition.key] ?: definition.defaultValue
                val isLocked = lockedSettings.contains(definition.key)

                BulkSettingItem(
                    definition = definition,
                    isSelected = isSelected,
                    value = value,
                    isLocked = isLocked,
                    onToggleSelect = { onToggleSetting(definition.key) },
                    onValueChange = { onValueChange(definition.key, it) },
                    onToggleLock = { onToggleLock(definition.key) },
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun BulkSettingItem(
    definition: SettingDefinition,
    isSelected: Boolean,
    value: Any,
    isLocked: Boolean,
    onToggleSelect: () -> Unit,
    onValueChange: (Any) -> Unit,
    onToggleLock: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggleSelect() },
            )
            Spacer(modifier = Modifier.width(8.dp))
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
            if (isSelected) {
                IconButton(onClick = onToggleLock) {
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

        if (isSelected) {
            Spacer(modifier = Modifier.height(8.dp))
            when (definition.type) {
                SettingType.BOOLEAN -> {
                    Switch(
                        checked = value as? Boolean ?: false,
                        onCheckedChange = { onValueChange(it) },
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
                            Text("$min")
                            Text("$intValue", fontWeight = FontWeight.Bold)
                            Text("$max")
                        }
                        Slider(
                            value = intValue.toFloat(),
                            onValueChange = { onValueChange(it.toInt()) },
                            valueRange = min.toFloat()..max.toFloat(),
                        )
                    }
                }
                SettingType.FLOAT -> {
                    val validation = definition.validation as? SettingValidation.FloatRange
                    val min = validation?.min ?: 0f
                    val max = validation?.max ?: 100f
                    val floatValue = (value as? Number)?.toFloat() ?: min

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("$min")
                            Text("%.1f".format(floatValue), fontWeight = FontWeight.Bold)
                            Text("$max")
                        }
                        Slider(
                            value = floatValue,
                            onValueChange = { onValueChange(it) },
                            valueRange = min..max,
                        )
                    }
                }
                SettingType.STRING -> {
                    // For bulk operations, strings are less common
                    Text(
                        text = value.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun BulkResultsView(
    result: BulkSettingsResult,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = if (result.isAllSuccessful) Icons.Default.Check else Icons.Default.Close,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = if (result.isAllSuccessful) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.error
            },
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (result.isAllSuccessful) {
                "All settings applied successfully!"
            } else {
                "Settings applied with some failures"
            },
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "${result.successCount} successful, ${result.failureCount} failed",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (result.failed.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Failed Devices:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(result.failed) { device ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                        ),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = device.deviceName,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                device.error?.let {
                                    Text(
                                        text = it,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onDone) {
            Text("Done")
        }
    }
}

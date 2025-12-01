package three.two.bit.phonemanager.ui.admin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import three.two.bit.phonemanager.domain.model.SettingCategory
import three.two.bit.phonemanager.domain.model.SettingType
import three.two.bit.phonemanager.domain.model.SettingValidation

/**
 * Story E12.7: Device Settings Screen
 *
 * Displays and allows modification of device settings for admins.
 *
 * AC E12.7.2: Settings View (grouped by category)
 * AC E12.7.3: View Remote Settings
 * AC E12.7.4: Modify Remote Settings
 * AC E12.7.5: Lock/Unlock Settings
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceSettingsScreen(
    onNavigateBack: () -> Unit,
    onViewHistory: (deviceId: String) -> Unit,
    viewModel: DeviceSettingsViewModel = hiltViewModel(),
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
                title = {
                    Column {
                        Text(
                            text = uiState.deviceName.ifEmpty { "Device Settings" },
                            style = MaterialTheme.typography.titleMedium,
                        )
                        if (uiState.ownerName.isNotEmpty()) {
                            Text(
                                text = uiState.ownerName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
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
                    // Notify toggle
                    IconToggleButton(
                        checked = uiState.notifyUserOnChange,
                        onCheckedChange = { viewModel.setNotifyUserOnChange(it) },
                    ) {
                        Icon(
                            imageVector = if (uiState.notifyUserOnChange) {
                                Icons.Default.Notifications
                            } else {
                                Icons.Default.NotificationsOff
                            },
                            contentDescription = if (uiState.notifyUserOnChange) {
                                "Notifications enabled"
                            } else {
                                "Notifications disabled"
                            },
                        )
                    }
                    // History button
                    IconButton(onClick = {
                        uiState.deviceSettings?.let { onViewHistory(it.deviceId) }
                    }) {
                        Icon(Icons.Default.History, contentDescription = "View History")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = uiState.isLoading,
            onRefresh = { viewModel.loadDeviceSettings() },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            if (uiState.isLoading && uiState.deviceSettings == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // Device status header
                    item {
                        DeviceStatusHeader(
                            isOnline = uiState.isOnline,
                            lockedCount = uiState.lockedCount,
                        )
                    }

                    // Settings by category
                    uiState.settingsByCategory.forEach { (category, settings) ->
                        item(key = category.name) {
                            SettingsCategorySection(
                                category = category,
                                settings = settings,
                                isExpanded = uiState.expandedCategories.contains(category),
                                isSaving = uiState.isSaving,
                                onToggleExpand = { viewModel.toggleCategory(category) },
                                onSettingChange = { key, value ->
                                    viewModel.updateSetting(key, value)
                                },
                                onToggleLock = { key ->
                                    viewModel.toggleLock(key)
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    // Lock confirmation dialog
    if (uiState.isConfirmingLock) {
        LockConfirmationDialog(
            settingKey = uiState.confirmLockSettingKey ?: "",
            isLocking = uiState.confirmLockAction ?: true,
            onConfirm = {
                val key = uiState.confirmLockSettingKey
                val lock = uiState.confirmLockAction
                if (key != null && lock != null) {
                    if (lock) viewModel.lockSetting(key) else viewModel.unlockSetting(key)
                }
                viewModel.hideLockConfirmation()
            },
            onDismiss = { viewModel.hideLockConfirmation() },
        )
    }
}

@Composable
private fun DeviceStatusHeader(
    isOnline: Boolean,
    lockedCount: Int,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(
                            if (isOnline) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outline
                            },
                        ),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isOnline) "Online" else "Offline",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (lockedCount > 0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$lockedCount locked",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsCategorySection(
    category: SettingCategory,
    settings: List<SettingWithValue>,
    isExpanded: Boolean,
    isSaving: Boolean,
    onToggleExpand: () -> Unit,
    onSettingChange: (key: String, value: Any) -> Unit,
    onToggleLock: (key: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column {
            // Category header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpand)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = category.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Icon(
                    imageVector = if (isExpanded) {
                        Icons.Default.KeyboardArrowUp
                    } else {
                        Icons.Default.KeyboardArrowDown
                    },
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                )
            }

            // Settings
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    settings.forEach { setting ->
                        SettingItem(
                            setting = setting,
                            isSaving = isSaving,
                            onValueChange = { value ->
                                onSettingChange(setting.definition.key, value)
                            },
                            onToggleLock = {
                                onToggleLock(setting.definition.key)
                            },
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun SettingItem(
    setting: SettingWithValue,
    isSaving: Boolean,
    onValueChange: (Any) -> Unit,
    onToggleLock: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = setting.definition.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = setting.definition.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (setting.isLocked && setting.lockedBy != null) {
                    Text(
                        text = "Locked by ${setting.lockedBy}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            // Lock toggle button
            IconButton(onClick = onToggleLock, enabled = !isSaving) {
                Icon(
                    imageVector = if (setting.isLocked) {
                        Icons.Default.Lock
                    } else {
                        Icons.Default.LockOpen
                    },
                    contentDescription = if (setting.isLocked) "Unlock" else "Lock",
                    tint = if (setting.isLocked) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outline
                    },
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Value control
        when (setting.definition.type) {
            SettingType.BOOLEAN -> {
                Switch(
                    checked = setting.value as? Boolean ?: false,
                    onCheckedChange = { onValueChange(it) },
                    enabled = !isSaving,
                )
            }
            SettingType.INTEGER -> {
                IntegerSettingControl(
                    value = (setting.value as? Number)?.toInt() ?: 0,
                    validation = setting.definition.validation as? SettingValidation.IntRange,
                    onValueChange = { onValueChange(it) },
                    enabled = !isSaving,
                )
            }
            SettingType.FLOAT -> {
                FloatSettingControl(
                    value = (setting.value as? Number)?.toFloat() ?: 0f,
                    validation = setting.definition.validation as? SettingValidation.FloatRange,
                    onValueChange = { onValueChange(it) },
                    enabled = !isSaving,
                )
            }
            SettingType.STRING -> {
                var textValue by remember { mutableStateOf(setting.value.toString()) }
                OutlinedTextField(
                    value = textValue,
                    onValueChange = {
                        textValue = it
                        onValueChange(it)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving,
                    singleLine = true,
                )
            }
        }
    }
}

@Composable
private fun IntegerSettingControl(
    value: Int,
    validation: SettingValidation.IntRange?,
    onValueChange: (Int) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val min = validation?.min ?: 0
    val max = validation?.max ?: 100

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("$min")
            Text("$value", fontWeight = FontWeight.Bold)
            Text("$max")
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = min.toFloat()..max.toFloat(),
            enabled = enabled,
        )
    }
}

@Composable
private fun FloatSettingControl(
    value: Float,
    validation: SettingValidation.FloatRange?,
    onValueChange: (Float) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val min = validation?.min ?: 0f
    val max = validation?.max ?: 100f

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("$min")
            Text("%.1f".format(value), fontWeight = FontWeight.Bold)
            Text("$max")
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = min..max,
            enabled = enabled,
        )
    }
}

@Composable
private fun LockConfirmationDialog(
    settingKey: String,
    isLocking: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (isLocking) "Lock Setting" else "Unlock Setting")
        },
        text = {
            Text(
                if (isLocking) {
                    "Are you sure you want to lock this setting? The user will not be able to change it."
                } else {
                    "Are you sure you want to unlock this setting? The user will be able to change it."
                },
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(if (isLocking) "Lock" else "Unlock")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

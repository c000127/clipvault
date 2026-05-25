package com.clipvault.app.ui.settings

import android.app.Activity
import com.clipvault.app.ui.theme.ThemeMode
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onAiSettings: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val exportState by viewModel.exportState.collectAsState()
    val importState by viewModel.importState.collectAsState()
    val message by viewModel.message.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showImportDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    LaunchedEffect(exportState) {
        when (val state = exportState) {
            is ExportState.Success -> snackbarHostState.showSnackbar(state.message)
            is ExportState.Error -> snackbarHostState.showSnackbar(state.message)
            else -> {}
        }
    }

    LaunchedEffect(importState) {
        when (val state = importState) {
            is ImportState.Success -> snackbarHostState.showSnackbar(state.message)
            is ImportState.Error -> snackbarHostState.showSnackbar(state.message)
            else -> {}
        }
    }

    // Export launcher
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { viewModel.exportData(it) }
    }

    // Import launcher
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { showImportDialog = true }
    }

    if (showImportDialog) {
        ImportModeDialog(
            onModeSelected = { mode ->
                showImportDialog = false
                // Get the last URI from the launcher result
                importLauncher.launch(arrayOf("application/json"))
            },
            onDismiss = { showImportDialog = false }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            if (com.clipvault.app.data.local.AppDatabase.migrationFailed) {
                Card(
                    colors = androidx.compose.material3.CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Warning",
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Database Migration Failed",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "There was an issue migrating your local database. Pre-existing clips and tags were preserved, but we recommend exporting a backup JSON immediately and reinstalling the app if issues persist.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
                androidx.compose.material3.HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
            }

            val themeMode by viewModel.themeMode.collectAsState()
            var showThemeDialog by remember { mutableStateOf(false) }

            // Theme Settings
            SettingsItem(
                icon = Icons.Default.Palette,
                title = "Theme Mode",
                subtitle = when (themeMode) {
                    ThemeMode.LIGHT -> "Light"
                    ThemeMode.DARK -> "Dark"
                    ThemeMode.FOLLOW_SYSTEM -> "Follow System"
                },
                onClick = { showThemeDialog = true }
            )

            if (showThemeDialog) {
                ThemeSelectionDialog(
                    currentMode = themeMode,
                    onModeSelected = { mode ->
                        viewModel.setThemeMode(mode)
                        showThemeDialog = false
                    },
                    onDismiss = { showThemeDialog = false }
                )
            }

            androidx.compose.material3.HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

            // AI Settings
            SettingsItem(
                icon = Icons.Default.SmartToy,
                title = "AI Settings",
                subtitle = "Configure AI providers",
                onClick = onAiSettings
            )

            androidx.compose.material3.HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

            // Export
            SettingsItem(
                icon = Icons.Default.CloudUpload,
                title = "Export Data",
                subtitle = when (exportState) {
                    is ExportState.Loading -> "Exporting..."
                    is ExportState.Success -> "Export complete"
                    is ExportState.Error -> "Export failed"
                    else -> "Export clips and tags as JSON"
                },
                onClick = { exportLauncher.launch("clipvault_export.json") },
                enabled = exportState !is ExportState.Loading
            )

            androidx.compose.material3.HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

            // Import
            SettingsItem(
                icon = Icons.Default.CloudDownload,
                title = "Import Data",
                subtitle = when (importState) {
                    is ImportState.Loading -> "Importing..."
                    is ImportState.Success -> "Import complete"
                    is ImportState.Error -> "Import failed"
                    else -> "Import from JSON file"
                },
                onClick = { importLauncher.launch(arrayOf("application/json")) },
                enabled = importState !is ImportState.Loading
            )

            androidx.compose.material3.HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

            Spacer(modifier = Modifier.height(16.dp))

            // About
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "ClipVault",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Version 1.0.0",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "A powerful clipboard and note-taking app for collecting and organizing content from any source.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ImportModeDialog(
    onModeSelected: (ImportMode) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedMode by remember { mutableStateOf<ImportMode>(ImportMode.Overwrite) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import Mode") },
        text = {
            Column {
                Text("Choose how to import the data:")
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedMode = ImportMode.Overwrite }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedMode is ImportMode.Overwrite,
                        onClick = { selectedMode = ImportMode.Overwrite }
                    )
                    Column {
                        Text("Overwrite", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Clear all existing data and replace with imported data",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedMode = ImportMode.Merge }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedMode is ImportMode.Merge,
                        onClick = { selectedMode = ImportMode.Merge }
                    )
                    Column {
                        Text("Merge", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Add imported data to existing data, deduplicating tags",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onModeSelected(selectedMode) }) {
                Text("Import")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ThemeSelectionDialog(
    currentMode: ThemeMode,
    onModeSelected: (ThemeMode) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Theme") },
        text = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onModeSelected(ThemeMode.FOLLOW_SYSTEM) }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = currentMode == ThemeMode.FOLLOW_SYSTEM,
                        onClick = { onModeSelected(ThemeMode.FOLLOW_SYSTEM) }
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Follow System")
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onModeSelected(ThemeMode.LIGHT) }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = currentMode == ThemeMode.LIGHT,
                        onClick = { onModeSelected(ThemeMode.LIGHT) }
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Light")
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onModeSelected(ThemeMode.DARK) }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = currentMode == ThemeMode.DARK,
                        onClick = { onModeSelected(ThemeMode.DARK) }
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Dark")
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

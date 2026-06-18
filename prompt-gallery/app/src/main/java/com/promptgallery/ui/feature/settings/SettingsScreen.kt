package com.promptgallery.ui.feature.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.promptgallery.domain.model.GalleryView
import com.promptgallery.domain.model.SortOption
import com.promptgallery.domain.model.ThemeMode
import com.promptgallery.domain.repository.ExportFormat
import com.promptgallery.ui.feature.importflow.ImportViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
    importViewModel: ImportViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val settings = state.settings

    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let { importViewModel.restoreBackup(it) } }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { Text("Settings") },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SettingsSection("Appearance") {
                Text("Theme", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = settings.themeMode == mode,
                            onClick = { viewModel.setTheme(mode) },
                            label = { Text(mode.displayName) },
                        )
                    }
                }
                SwitchRow(
                    title = "Dynamic color",
                    subtitle = "Use Material You colors from your wallpaper (Android 12+)",
                    checked = settings.dynamicColor,
                    onCheckedChange = viewModel::setDynamicColor,
                )
            }

            SettingsSection("Gallery defaults") {
                Text("Default view", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(GalleryView.MASONRY, GalleryView.GRID, GalleryView.TIMELINE, GalleryView.FAVORITES)
                        .forEach { view ->
                            FilterChip(
                                selected = settings.defaultGalleryView == view,
                                onClick = { viewModel.setDefaultView(view) },
                                label = { Text(view.displayName) },
                            )
                        }
                }
                Text("Grid columns: ${settings.gridColumns}", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (2..6).forEach { columns ->
                        FilterChip(
                            selected = settings.gridColumns == columns,
                            onClick = { viewModel.setColumns(columns) },
                            label = { Text("$columns") },
                        )
                    }
                }
                Text("Default sort", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SortOption.entries.forEach { sort ->
                        FilterChip(
                            selected = settings.defaultSort == sort,
                            onClick = { viewModel.setSort(sort) },
                            label = { Text(sort.displayName) },
                        )
                    }
                }
            }

            SettingsSection("Backup & export") {
                Text("${state.totalImages} images in your library", style = MaterialTheme.typography.bodyMedium)
                Text("Export library", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExportButton(ExportFormat.JSON, "application/json", "prompt-gallery.json", importViewModel)
                    ExportButton(ExportFormat.CSV, "text/csv", "prompt-gallery.csv", importViewModel)
                    ExportButton(ExportFormat.MARKDOWN, "text/markdown", "prompt-gallery.md", importViewModel)
                    ExportButton(ExportFormat.ZIP, "application/zip", "prompt-gallery.zip", importViewModel)
                    ExportButton(ExportFormat.BACKUP, "application/zip", "prompt-gallery-backup.zip", importViewModel)
                }
                Button(
                    onClick = { restoreLauncher.launch(arrayOf("application/zip")) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Restore from backup") }
            }

            SettingsSection("Security") {
                SwitchRow(
                    title = "Encrypt database",
                    subtitle = "Protect your prompt library at rest with SQLCipher. " +
                        "Applies after the app restarts.",
                    checked = settings.encryptionEnabled,
                    onCheckedChange = viewModel::setEncryption,
                )
                Text(
                    "All data stays on this device. No account or network connection is required.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ExportButton(
    format: ExportFormat,
    mime: String,
    suggestedName: String,
    importViewModel: ImportViewModel,
) {
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(mime),
    ) { uri -> uri?.let { importViewModel.export(format, it) } }
    OutlinedButton(onClick = { launcher.launch(suggestedName) }) { Text(format.name) }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        content()
        HorizontalDivider()
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.padding(end = 16.dp).fillMaxWidth(0.8f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

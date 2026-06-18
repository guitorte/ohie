package com.promptgallery.ui.feature.gallery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Label
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.ViewQuilt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.promptgallery.domain.model.ColorLabel
import com.promptgallery.domain.model.GalleryView
import com.promptgallery.domain.model.SortOption
import com.promptgallery.ui.components.ColorLabelPicker
import com.promptgallery.ui.components.ConfirmDialog
import com.promptgallery.ui.components.RatingStars
import com.promptgallery.ui.feature.importflow.ImportUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryTopBar(
    view: GalleryView,
    sort: SortOption,
    totalCount: Int,
    onViewChange: (GalleryView) -> Unit,
    onSortChange: (SortOption) -> Unit,
    onImportFolder: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    var viewMenu by remember { mutableStateOf(false) }
    var sortMenu by remember { mutableStateOf(false) }
    var overflow by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            Column {
                Text("Gallery")
                if (totalCount > 0) {
                    Text(
                        "$totalCount image${if (totalCount == 1) "" else "s"} · ${view.displayName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        actions = {
            // View switcher.
            IconButton(onClick = { viewMenu = true }) {
                Icon(
                    if (view == GalleryView.GRID) Icons.Outlined.GridView else Icons.Outlined.ViewQuilt,
                    contentDescription = "Change view",
                )
            }
            DropdownMenu(expanded = viewMenu, onDismissRequest = { viewMenu = false }) {
                listOf(
                    GalleryView.MASONRY,
                    GalleryView.GRID,
                    GalleryView.TIMELINE,
                    GalleryView.FAVORITES,
                ).forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.displayName) },
                        onClick = { onViewChange(option); viewMenu = false },
                    )
                }
            }

            // Sort.
            IconButton(onClick = { sortMenu = true }) {
                Icon(Icons.AutoMirrored.Outlined.Sort, contentDescription = "Sort")
            }
            DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                SortOption.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.displayName) },
                        onClick = { onSortChange(option); sortMenu = false },
                    )
                }
            }

            IconButton(onClick = { overflow = true }) {
                Icon(Icons.Outlined.MoreVert, contentDescription = "More")
            }
            DropdownMenu(expanded = overflow, onDismissRequest = { overflow = false }) {
                DropdownMenuItem(
                    text = { Text("Import folder") },
                    onClick = { overflow = false; onImportFolder() },
                )
                DropdownMenuItem(
                    text = { Text("Settings") },
                    leadingIcon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
                    onClick = { overflow = false; onOpenSettings() },
                )
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GallerySelectionBar(
    selectedCount: Int,
    onClose: () -> Unit,
    onFavorite: () -> Unit,
    onDelete: () -> Unit,
    onTag: (String) -> Unit,
    onColorLabel: (ColorLabel) -> Unit,
    onRating: (Int) -> Unit,
) {
    var showTagDialog by remember { mutableStateOf(false) }
    var showColorMenu by remember { mutableStateOf(false) }
    var showRatingMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    TopAppBar(
        navigationIcon = {
            IconButton(onClick = onClose) { Icon(Icons.Outlined.Close, contentDescription = "Close") }
        },
        title = { Text("$selectedCount selected") },
        actions = {
            IconButton(onClick = onFavorite) { Icon(Icons.Outlined.Favorite, contentDescription = "Favorite") }
            IconButton(onClick = { showTagDialog = true }) { Icon(Icons.Outlined.Label, contentDescription = "Tag") }
            IconButton(onClick = { showColorMenu = true }) { Icon(Icons.Outlined.Palette, contentDescription = "Color label") }
            IconButton(onClick = { showRatingMenu = true }) { Icon(Icons.Outlined.Star, contentDescription = "Rate") }
            IconButton(onClick = { showDeleteConfirm = true }) { Icon(Icons.Outlined.Delete, contentDescription = "Delete") }
        },
    )

    if (showTagDialog) {
        var tagText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showTagDialog = false },
            title = { Text("Add tag to $selectedCount image(s)") },
            text = {
                OutlinedTextField(
                    value = tagText,
                    onValueChange = { tagText = it },
                    label = { Text("Tag name") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (tagText.isNotBlank()) onTag(tagText.trim())
                        showTagDialog = false
                    },
                ) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { showTagDialog = false }) { Text("Cancel") } },
        )
    }

    if (showColorMenu) {
        AlertDialog(
            onDismissRequest = { showColorMenu = false },
            title = { Text("Color label") },
            text = {
                ColorLabelPicker(
                    selected = ColorLabel.NONE,
                    onSelect = { onColorLabel(it); showColorMenu = false },
                )
            },
            confirmButton = { TextButton(onClick = { showColorMenu = false }) { Text("Done") } },
        )
    }

    if (showRatingMenu) {
        AlertDialog(
            onDismissRequest = { showRatingMenu = false },
            title = { Text("Set rating") },
            text = {
                RatingStars(rating = 0, onRatingChange = { onRating(it); showRatingMenu = false })
            },
            confirmButton = { TextButton(onClick = { showRatingMenu = false }) { Text("Done") } },
        )
    }

    if (showDeleteConfirm) {
        ConfirmDialog(
            title = "Delete $selectedCount image(s)?",
            message = "The image files and their prompts will be permanently removed.",
            confirmLabel = "Delete",
            onConfirm = { showDeleteConfirm = false; onDelete() },
            onDismiss = { showDeleteConfirm = false },
        )
    }
}

@Composable
fun ImportProgressBanner(importState: ImportUiState, modifier: Modifier = Modifier) {
    val progress = importState.progress ?: return
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        tonalElevation = 6.dp,
        shadowElevation = 6.dp,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Importing…", style = MaterialTheme.typography.titleSmall)
                Text(
                    "${progress.processed}/${progress.total}",
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            LinearProgressIndicator(
                progress = {
                    if (progress.total == 0) 0f else progress.processed.toFloat() / progress.total
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            )
            if (progress.currentName.isNotBlank()) {
                Text(
                    progress.currentName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

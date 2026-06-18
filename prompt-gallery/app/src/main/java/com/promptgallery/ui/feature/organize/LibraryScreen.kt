package com.promptgallery.ui.feature.organize

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.promptgallery.domain.model.Collection
import com.promptgallery.domain.model.Folder
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LibraryScreen(
    onOpenCollection: (String) -> Unit,
    onOpenFolder: (String) -> Unit,
    onOpenTag: (String) -> Unit,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showCreateCollection by remember { mutableStateOf(false) }
    var showCreateFolder by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Library") }) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item {
                SectionHeader("Collections", onAdd = { showCreateCollection = true })
            }
            item {
                if (state.collections.isEmpty()) {
                    EmptyHint("Group images into collections like \"Best Prompts\" or \"Portraits\".")
                } else {
                    LazyHorizontalGrid(
                        rows = GridCells.Fixed(1),
                        modifier = Modifier.fillMaxWidth().height(150.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(state.collections.size) { index ->
                            CollectionCard(
                                collection = state.collections[index],
                                onClick = { onOpenCollection(state.collections[index].id) },
                            )
                        }
                    }
                }
            }

            item { SectionHeader("Folders", onAdd = { showCreateFolder = true }) }
            if (state.rootFolders.isEmpty()) {
                item { EmptyHint("Create nested folders to mirror your own filing system.") }
            } else {
                items(state.rootFolders.size) { index ->
                    FolderRow(folder = state.rootFolders[index], onClick = { onOpenFolder(state.rootFolders[index].id) })
                }
            }

            item { SectionHeader("Tags", onAdd = null) }
            item {
                if (state.tags.isEmpty()) {
                    EmptyHint("Tags you add to images appear here for quick filtering.")
                } else {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.tags.forEach { tag ->
                            AssistChip(
                                onClick = { onOpenTag(tag.id) },
                                label = { Text("${tag.name} (${tag.imageCount})") },
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreateCollection) {
        CreateCollectionDialog(
            onCreate = { name -> viewModel.createCollection(name); showCreateCollection = false },
            onCreateSmart = { name, rating, fav ->
                viewModel.createSmartCollection(name, rating, fav); showCreateCollection = false
            },
            onDismiss = { showCreateCollection = false },
        )
    }

    if (showCreateFolder) {
        CreateNameDialog(
            title = "New folder",
            label = "Folder name",
            onConfirm = { viewModel.createFolder(it); showCreateFolder = false },
            onDismiss = { showCreateFolder = false },
        )
    }
}

@Composable
private fun SectionHeader(title: String, onAdd: (() -> Unit)?) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        if (onAdd != null) {
            IconButton(onClick = onAdd) { Icon(Icons.Outlined.Add, contentDescription = "Add $title") }
        }
    }
}

@Composable
private fun EmptyHint(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun CollectionCard(collection: Collection, onClick: () -> Unit) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .size(width = 160.dp, height = 150.dp)
            .clickable(onClick = onClick),
    ) {
        Column {
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.6f)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                if (collection.coverThumbnailPath != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(File(collection.coverThumbnailPath)).build(),
                        contentDescription = collection.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Icon(
                        if (collection.isSmartCollection) Icons.Outlined.AutoAwesome else Icons.Outlined.Collections,
                        contentDescription = null,
                    )
                }
            }
            Column(Modifier.padding(8.dp)) {
                Text(collection.name, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                Text(
                    "${collection.imageCount} image${if (collection.imageCount == 1) "" else "s"}" +
                        if (collection.isSmartCollection) " · smart" else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun FolderRow(folder: Folder, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(folder.name) },
        supportingContent = {
            Text("${folder.imageCount} images · ${folder.childCount} subfolders")
        },
        leadingContent = { Icon(Icons.Outlined.Folder, contentDescription = null) },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
private fun CreateCollectionDialog(
    onCreate: (String) -> Unit,
    onCreateSmart: (String, Int, Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var smart by remember { mutableStateOf(false) }
    var favoritesOnly by remember { mutableStateOf(false) }
    var minRating by remember { mutableStateOf(0) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New collection") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = smart, onCheckedChange = { smart = it })
                    Text("Smart collection", Modifier.padding(start = 8.dp))
                }
                if (smart) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = favoritesOnly, onCheckedChange = { favoritesOnly = it })
                        Text("Favorites only", Modifier.padding(start = 8.dp))
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(3, 4, 5).forEach { r ->
                            FilterChip(
                                selected = minRating == r,
                                onClick = { minRating = if (minRating == r) 0 else r },
                                label = { Text("$r★+") },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (smart) onCreateSmart(name, minRating, favoritesOnly) else onCreate(name)
            }) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
fun CreateNameDialog(
    title: String,
    label: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(label) },
                singleLine = true,
            )
        },
        confirmButton = { TextButton(onClick = { onConfirm(name) }) { Text("Create") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

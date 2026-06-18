package com.promptgallery.ui.feature.detail

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.promptgallery.domain.model.Image
import com.promptgallery.ui.components.ColorLabelPicker
import com.promptgallery.ui.components.ConfirmDialog
import com.promptgallery.ui.components.LoadingIndicator
import com.promptgallery.ui.components.PromptCard
import com.promptgallery.ui.components.RatingStars
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    onBack: () -> Unit,
    onOpenImage: (String) -> Unit,
    viewModel: DetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showEdit by remember { mutableStateOf(false) }
    var showVersions by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var showTagEditor by remember { mutableStateOf(false) }
    var showCollectionEditor by remember { mutableStateOf(false) }

    val image = state.image

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { Text(image?.title?.ifBlank { image.fileName } ?: "") },
                actions = {
                    if (image != null) {
                        IconButton(onClick = viewModel::toggleFavorite) {
                            Icon(
                                if (image.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = "Favorite",
                            )
                        }
                        IconButton(onClick = { showVersions = true }) {
                            Icon(Icons.Outlined.History, contentDescription = "Version history")
                        }
                        IconButton(onClick = { showDelete = true }) {
                            Icon(Icons.Outlined.Delete, contentDescription = "Delete")
                        }
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.loading -> LoadingIndicator(Modifier.padding(padding))
            image == null -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { Text("Image not found") }

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(File(image.filePath))
                            .crossfade(true)
                            .build(),
                        contentDescription = image.title,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 480.dp),
                    )
                }
                item {
                    PromptCard(
                        image = image,
                        onEdit = { showEdit = true },
                        onDuplicate = { viewModel.duplicate(onOpenImage) },
                        onShare = { shareImage(context, image) },
                    )
                }
                item { RatingRow(image = image, onRating = viewModel::setRating, onColor = viewModel::setColorLabel) }
                item {
                    TagsRow(
                        image = image,
                        onEditTags = { showTagEditor = true },
                        onEditCollections = { showCollectionEditor = true },
                    )
                }
                item { MetadataSection(image = image) }
            }
        }
    }

    if (showEdit && image != null) {
        EditPromptDialog(
            initialPrompt = image.prompt,
            initialNegative = image.negativePrompt,
            onConfirm = { p, n -> viewModel.updatePrompt(p, n); showEdit = false },
            onDismiss = { showEdit = false },
        )
    }

    if (showVersions && image != null) {
        VersionHistorySheet(
            versions = state.versions,
            onRestore = { viewModel.restoreVersion(it); showVersions = false },
            onDismiss = { showVersions = false },
        )
    }

    if (showTagEditor && image != null) {
        TagEditorDialog(
            allTags = state.allTags,
            selectedTagNames = image.tags.map { it.name },
            onConfirm = { viewModel.setTags(it); showTagEditor = false },
            onDismiss = { showTagEditor = false },
        )
    }

    if (showCollectionEditor && image != null) {
        CollectionEditorDialog(
            allCollections = state.allCollections,
            selectedIds = image.collectionIds,
            onConfirm = { viewModel.setCollections(it); showCollectionEditor = false },
            onDismiss = { showCollectionEditor = false },
        )
    }

    if (showDelete && image != null) {
        ConfirmDialog(
            title = "Delete image?",
            message = "This permanently removes the image and its prompt.",
            confirmLabel = "Delete",
            onConfirm = { showDelete = false; viewModel.delete(onBack) },
            onDismiss = { showDelete = false },
        )
    }
}

@Composable
private fun RatingRow(image: Image, onRating: (Int) -> Unit, onColor: (com.promptgallery.domain.model.ColorLabel) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Rating", style = MaterialTheme.typography.titleMedium)
        RatingStars(rating = image.rating, onRatingChange = onRating)
        Text("Color label", style = MaterialTheme.typography.titleMedium)
        ColorLabelPicker(selected = image.colorLabel, onSelect = onColor)
    }
}

@Composable
private fun TagsRow(image: Image, onEditTags: () -> Unit, onEditCollections: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Tags", style = MaterialTheme.typography.titleMedium)
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            image.tags.forEach { tag -> AssistChip(onClick = onEditTags, label = { Text(tag.name) }) }
            AssistChip(onClick = onEditTags, label = { Text(if (image.tags.isEmpty()) "Add tags" else "Edit") })
        }
        AssistChip(onClick = onEditCollections, label = { Text("Collections (${image.collectionIds.size})") })
    }
}

/** Shares the image file and its prompt via the system sheet. */
private fun shareImage(context: android.content.Context, image: Image) {
    val file = File(image.filePath)
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = image.mimeType.ifBlank { "image/*" }
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TEXT, buildString {
            if (image.prompt.isNotBlank()) appendLine(image.prompt)
            if (image.negativePrompt.isNotBlank()) {
                appendLine()
                appendLine("Negative prompt: ${image.negativePrompt}")
            }
        }.trim())
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share image"))
}

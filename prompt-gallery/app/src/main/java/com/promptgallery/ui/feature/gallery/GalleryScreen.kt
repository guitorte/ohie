package com.promptgallery.ui.feature.gallery

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.AutoAwesomeMosaic
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.promptgallery.domain.model.GalleryView
import com.promptgallery.domain.model.Image
import com.promptgallery.ui.components.EmptyState
import com.promptgallery.ui.components.ImageCell
import com.promptgallery.ui.components.LoadingIndicator
import com.promptgallery.ui.feature.importflow.ImportViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    onOpenImage: (String) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GalleryViewModel = hiltViewModel(),
    importViewModel: ImportViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val importState by importViewModel.state.collectAsStateWithLifecycle()
    val images = viewModel.images.collectAsLazyPagingItems()
    val snackbar = remember { SnackbarHostState() }

    val pickImages = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris -> if (uris.isNotEmpty()) importViewModel.importImages(uris) }

    val pickFolder = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri -> uri?.let { importViewModel.importFolder(it) } }

    LaunchedEffect(importState.lastMessage) {
        importState.lastMessage?.let {
            snackbar.showSnackbar(it)
            importViewModel.consumeMessage()
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            if (state.selectionMode) {
                GallerySelectionBar(
                    selectedCount = state.selectedCount,
                    onClose = viewModel::clearSelection,
                    onFavorite = { viewModel.bulkFavorite(true) },
                    onDelete = viewModel::bulkDelete,
                    onTag = { tag -> viewModel.bulkAddTag(tag) },
                    onColorLabel = viewModel::bulkColorLabel,
                    onRating = viewModel::bulkRating,
                )
            } else {
                GalleryTopBar(
                    view = state.view,
                    sort = state.sort,
                    totalCount = state.totalCount,
                    onViewChange = viewModel::setView,
                    onSortChange = viewModel::setSort,
                    onImportFolder = { pickFolder.launch(null) },
                    onOpenSettings = onOpenSettings,
                )
            }
        },
        floatingActionButton = {
            if (!state.selectionMode) {
                ExtendedFloatingActionButton(
                    onClick = { pickImages.launch(arrayOf("image/*")) },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("Import") },
                )
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                images.loadState.refresh is LoadState.Loading && images.itemCount == 0 ->
                    LoadingIndicator()

                images.itemCount == 0 -> EmptyState(
                    icon = Icons.Outlined.PhotoLibrary,
                    title = "Your gallery is empty",
                    message = "Import AI-generated images to start building your prompt library.",
                    action = {
                        ExtendedFloatingActionButton(
                            onClick = { pickImages.launch(arrayOf("image/*")) },
                            icon = { Icon(Icons.Outlined.AutoAwesomeMosaic, contentDescription = null) },
                            text = { Text("Import images") },
                        )
                    },
                )

                else -> GalleryContent(
                    view = state.view,
                    columns = state.gridColumns,
                    images = images,
                    selectionMode = state.selectionMode,
                    selectedIds = state.selectedIds,
                    onClick = { image ->
                        if (state.selectionMode) viewModel.toggleSelection(image.id)
                        else onOpenImage(image.id)
                    },
                    onLongClick = { image -> viewModel.enterSelection(image.id) },
                )
            }

            if (importState.running) {
                ImportProgressBanner(importState = importState)
            }
        }
    }
}

@Composable
private fun GalleryContent(
    view: GalleryView,
    columns: Int,
    images: androidx.paging.compose.LazyPagingItems<Image>,
    selectionMode: Boolean,
    selectedIds: Set<String>,
    onClick: (Image) -> Unit,
    onLongClick: (Image) -> Unit,
) {
    val contentPadding = PaddingValues(12.dp)
    val spacing = 12.dp

    when (view) {
        GalleryView.GRID -> LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            contentPadding = contentPadding,
            horizontalArrangement = Arrangement.spacedBy(spacing),
            verticalArrangement = Arrangement.spacedBy(spacing),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(count = images.itemCount, key = images.itemKey { it.id }) { index ->
                images[index]?.let { image ->
                    ImageCell(
                        image = image,
                        onClick = { onClick(image) },
                        onLongClick = { onLongClick(image) },
                        selectionMode = selectionMode,
                        selected = image.id in selectedIds,
                        modifier = Modifier.aspectRatio(1f),
                    )
                }
            }
        }

        else -> {
            // Masonry, Timeline and Favorites all use an adaptive staggered grid;
            // they differ only in the underlying paged query (handled in the VM).
            val staggeredColumns = if (view == GalleryView.TIMELINE) 2 else columns
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(staggeredColumns),
                contentPadding = contentPadding,
                horizontalArrangement = Arrangement.spacedBy(spacing),
                verticalItemSpacing = spacing,
                modifier = Modifier.fillMaxSize(),
            ) {
                items(count = images.itemCount, key = images.itemKey { it.id }) { index ->
                    images[index]?.let { image ->
                        ImageCell(
                            image = image,
                            onClick = { onClick(image) },
                            onLongClick = { onLongClick(image) },
                            selectionMode = selectionMode,
                            selected = image.id in selectedIds,
                            modifier = Modifier.aspectRatio(image.aspectRatioValue.coerceIn(0.5f, 2f)),
                        )
                    }
                }
            }
        }
    }
}

package com.promptgallery.ui.feature.organize

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import androidx.compose.foundation.lazy.staggeredgrid.items
import com.promptgallery.ui.components.EmptyState
import com.promptgallery.ui.components.ImageCell

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageListScreen(
    onBack: () -> Unit,
    onOpenImage: (String) -> Unit,
    viewModel: ImageListViewModel = hiltViewModel(),
) {
    val title by viewModel.title.collectAsStateWithLifecycle()
    val images = viewModel.images.collectAsLazyPagingItems()

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { Text(title) },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (images.itemCount == 0) {
                EmptyState(
                    icon = Icons.Outlined.PhotoLibrary,
                    title = "Nothing here yet",
                    message = "Images you add to this will appear here.",
                )
            } else {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Adaptive(160.dp),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalItemSpacing = 12.dp,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(count = images.itemCount, key = images.itemKey { it.id }) { index ->
                        images[index]?.let { image ->
                            ImageCell(
                                image = image,
                                onClick = { onOpenImage(image.id) },
                                onLongClick = { onOpenImage(image.id) },
                                modifier = Modifier.aspectRatio(image.aspectRatioValue.coerceIn(0.5f, 2f)),
                            )
                        }
                    }
                }
            }
        }
    }
}

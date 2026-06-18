package com.promptgallery.ui.feature.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.promptgallery.ui.components.EmptyState
import com.promptgallery.ui.components.ImageCell

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    onOpenImage: (String) -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = state.filters.query,
            onValueChange = viewModel::setQuery,
            placeholder = { Text("Search prompts, tags, models…") },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            trailingIcon = {
                when {
                    state.searching -> CircularProgressIndicator(Modifier.padding(8.dp))
                    state.filters.query.isNotEmpty() -> IconButton(onClick = { viewModel.setQuery("") }) {
                        Icon(Icons.Outlined.Close, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
        )

        FilterBar(state = state, viewModel = viewModel)

        Box(Modifier.fillMaxSize()) {
            when {
                !state.hasSearched -> EmptyState(
                    icon = Icons.Outlined.Search,
                    title = "Search your library",
                    message = "Find images by prompt text, negative prompt, tags, model, title or notes. " +
                        "Search is typo-tolerant.",
                )

                state.results.isEmpty() && !state.searching -> EmptyState(
                    icon = Icons.Outlined.SearchOff,
                    title = "No matches",
                    message = "Try fewer words or remove some filters.",
                )

                else -> LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Adaptive(160.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalItemSpacing = 12.dp,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(items = state.results, key = { it.image.id }) { result ->
                        ImageCell(
                            image = result.image,
                            onClick = { onOpenImage(result.image.id) },
                            onLongClick = { onOpenImage(result.image.id) },
                            modifier = Modifier.aspectRatio(result.image.aspectRatioValue.coerceIn(0.5f, 2f)),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterBar(state: SearchUiState, viewModel: SearchViewModel) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = state.filters.favoritesOnly,
            onClick = viewModel::toggleFavoritesOnly,
            label = { Text("Favorites") },
        )
        listOf(5, 4, 3).forEach { rating ->
            FilterChip(
                selected = state.filters.minRating == rating,
                onClick = { viewModel.setMinRating(rating) },
                label = { Text("$rating★+") },
            )
        }
        state.availableModels.take(12).forEach { model ->
            FilterChip(
                selected = model in state.filters.aiModels,
                onClick = { viewModel.toggleModel(model) },
                label = { Text(model) },
            )
        }
    }
}

package com.promptgallery.ui.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.promptgallery.domain.model.SearchFilters
import com.promptgallery.domain.model.SearchResult
import com.promptgallery.domain.model.Tag
import com.promptgallery.domain.repository.ImageRepository
import com.promptgallery.domain.repository.TagRepository
import com.promptgallery.domain.usecase.SearchImagesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class SearchUiState(
    val filters: SearchFilters = SearchFilters(),
    val results: List<SearchResult> = emptyList(),
    val searching: Boolean = false,
    val availableModels: List<String> = emptyList(),
    val availableTags: List<Tag> = emptyList(),
    val hasSearched: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchImages: SearchImagesUseCase,
    imageRepository: ImageRepository,
    tagRepository: TagRepository,
) : ViewModel() {

    private val _filters = MutableStateFlow(SearchFilters())
    private val _searching = MutableStateFlow(false)

    private val results: StateFlow<List<SearchResult>> = _filters
        .debounce { if (it.isEmpty) 0L else DEBOUNCE_MS }
        .distinctUntilChanged()
        .flatMapLatest { filters ->
            flow {
                if (filters.isEmpty) {
                    emit(emptyList())
                } else {
                    _searching.value = true
                    emit(searchImages(filters))
                    _searching.value = false
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val uiState: StateFlow<SearchUiState> = combine(
        _filters,
        results,
        _searching,
        imageRepository.observeModels(),
        tagRepository.observeAll(),
    ) { filters, res, searching, models, tags ->
        SearchUiState(
            filters = filters,
            results = res,
            searching = searching,
            availableModels = models,
            availableTags = tags,
            hasSearched = !filters.isEmpty,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchUiState())

    fun setQuery(query: String) = _filters.update { it.copy(query = query) }

    fun toggleFavoritesOnly() = _filters.update { it.copy(favoritesOnly = !it.favoritesOnly) }

    fun setMinRating(rating: Int) = _filters.update {
        it.copy(minRating = if (it.minRating == rating) 0 else rating)
    }

    fun toggleModel(model: String) = _filters.update {
        val next = if (model in it.aiModels) it.aiModels - model else it.aiModels + model
        it.copy(aiModels = next)
    }

    fun toggleTag(tagId: String) = _filters.update {
        val next = if (tagId in it.tagIds) it.tagIds - tagId else it.tagIds + tagId
        it.copy(tagIds = next)
    }

    fun clearFilters() = _filters.update { SearchFilters(query = it.query) }

    companion object {
        const val DEBOUNCE_MS = 180L
    }
}

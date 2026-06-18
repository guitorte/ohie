package com.promptgallery.ui.feature.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.promptgallery.domain.model.ColorLabel
import com.promptgallery.domain.model.GalleryView
import com.promptgallery.domain.model.Image
import com.promptgallery.domain.model.SortOption
import com.promptgallery.domain.repository.CollectionRepository
import com.promptgallery.domain.repository.ImageRepository
import com.promptgallery.domain.repository.SettingsRepository
import com.promptgallery.domain.repository.TagRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GalleryUiState(
    val view: GalleryView = GalleryView.MASONRY,
    val sort: SortOption = SortOption.IMPORT_DATE_DESC,
    val gridColumns: Int = 3,
    val totalCount: Int = 0,
    val selectionMode: Boolean = false,
    val selectedIds: Set<String> = emptySet(),
    // Whether the user overrode persisted defaults during this session.
    val userChangedView: Boolean = false,
    val userChangedSort: Boolean = false,
) {
    val selectedCount: Int get() = selectedIds.size
}

/** The (view, sort) pair that determines which paged query is active. */
private data class GalleryQuery(val view: GalleryView, val sort: SortOption)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class GalleryViewModel @Inject constructor(
    private val imageRepository: ImageRepository,
    private val tagRepository: TagRepository,
    private val collectionRepository: CollectionRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(GalleryUiState())
    val uiState = _uiState.asStateFlow()

    val images: Flow<PagingData<Image>> = _uiState
        .map { GalleryQuery(it.view, it.sort) }
        .distinctUntilChanged()
        .flatMapLatest { query ->
            val favoritesOnly = query.view == GalleryView.FAVORITES
            imageRepository.pagedLibrary(
                folderId = null,
                favoritesOnly = favoritesOnly,
                minRating = 0,
                sort = query.sort,
            )
        }
        .cachedIn(viewModelScope)

    init {
        settingsRepository.settings.onEach { settings ->
            _uiState.update {
                it.copy(
                    view = if (it.userChangedView) it.view else settings.defaultGalleryView,
                    sort = if (it.userChangedSort) it.sort else settings.defaultSort,
                    gridColumns = settings.gridColumns,
                )
            }
        }.launchIn(viewModelScope)

        imageRepository.observeTotalCount().onEach { count ->
            _uiState.update { it.copy(totalCount = count) }
        }.launchIn(viewModelScope)
    }

    fun setView(view: GalleryView) = _uiState.update { it.copy(view = view, userChangedView = true) }

    fun setSort(sort: SortOption) = _uiState.update { it.copy(sort = sort, userChangedSort = true) }

    // ---- Selection -------------------------------------------------------

    fun enterSelection(imageId: String) = _uiState.update {
        it.copy(selectionMode = true, selectedIds = it.selectedIds + imageId)
    }

    fun toggleSelection(imageId: String) = _uiState.update {
        val next = if (imageId in it.selectedIds) it.selectedIds - imageId else it.selectedIds + imageId
        it.copy(selectedIds = next, selectionMode = next.isNotEmpty())
    }

    fun clearSelection() = _uiState.update { it.copy(selectionMode = false, selectedIds = emptySet()) }

    // ---- Bulk operations -------------------------------------------------

    fun bulkFavorite(favorite: Boolean) = withSelection { imageRepository.setFavorite(it, favorite) }

    fun bulkRating(rating: Int) = withSelection { imageRepository.setRating(it, rating) }

    fun bulkColorLabel(label: ColorLabel) = withSelection { imageRepository.setColorLabel(it, label) }

    fun bulkDelete() = withSelection { imageRepository.delete(it) }

    fun bulkMoveToFolder(folderId: String?) = withSelection { imageRepository.moveToFolder(it, folderId) }

    fun bulkAddToCollection(collectionId: String) =
        withSelection { collectionRepository.addImages(collectionId, it) }

    fun bulkAddTag(tagName: String) = withSelection { ids ->
        val tag = tagRepository.getOrCreate(tagName)
        tagRepository.addTagToImages(tag.id, ids)
    }

    private fun withSelection(block: suspend (List<String>) -> Unit) {
        val ids = _uiState.value.selectedIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            block(ids)
            clearSelection()
        }
    }
}
